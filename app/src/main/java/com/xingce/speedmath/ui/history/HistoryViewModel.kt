package com.xingce.speedmath.ui.history

import android.database.SQLException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.xingce.speedmath.data.db.PracticeSessionEntity
import com.xingce.speedmath.data.db.StatsAggregate
import com.xingce.speedmath.data.prefs.UserPreferences
import com.xingce.speedmath.data.prefs.UserPreferencesStore
import com.xingce.speedmath.data.repository.PracticeRepository
import java.io.IOException
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val emptyStats = formatHistoryOverview(StatsAggregate(0, 0, 0))

internal data class HistoryClock(val date: LocalDate, val zone: ZoneId)

internal data class HistoryUiState(
    val overview: HistoryOverview = emptyStats,
    val sessions: List<PracticeSessionEntity>? = null,
    val clock: HistoryClock = ZonedDateTime.now().let { HistoryClock(it.toLocalDate(), it.zone) },
    val loadError: Boolean = false,
)

internal class HistoryViewModel(
    private val repository: PracticeRepository,
    private val preferencesStore: UserPreferencesStore,
) : ViewModel() {
    private val feedback = Channel<String>(Channel.BUFFERED)
    val effects = feedback.receiveAsFlow()
    private val refresh = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState = refresh.flatMapLatest {
        combine(repository.observeOverallStats(), repository.observeAllSessions(), historyClock()) { stats, sessions, clock ->
            HistoryUiState(formatHistoryOverview(stats), sessions, clock)
        }.catch { error ->
            if (error !is SQLException) throw error
            emit(HistoryUiState(loadError = true))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    val preferences = preferencesStore.preferences.catch { error ->
        if (error !is IOException) throw error
        feedback.send("暂时无法读取练习设置")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun reload() = refresh.update { it + 1 }

    fun updatePreferences(change: (UserPreferences) -> UserPreferences) {
        viewModelScope.launch {
            try {
                preferencesStore.updatePracticePreferences(change)
            } catch (_: IOException) {
                feedback.send("暂时无法保存设置，请重试")
            }
        }
    }

    fun deleteSession(id: Long) = changeRecords("已删除练习记录", "暂时无法删除练习记录，请重试") {
        repository.deleteSessionById(id)
    }

    fun clearAllSessions() = changeRecords("已清空全部练习记录", "暂时无法清空练习记录，请重试") {
        repository.clearAllSessions()
    }

    private fun changeRecords(success: String, error: String, operation: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                operation()
                feedback.send(success)
            } catch (_: SQLException) {
                feedback.send(error)
            }
        }
    }

    companion object {
        fun factory(
            repository: PracticeRepository,
            preferencesStore: UserPreferencesStore,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { HistoryViewModel(repository, preferencesStore) }
        }
    }
}

private fun historyClock() = flow {
    while (true) {
        val now = ZonedDateTime.now()
        val date = now.toLocalDate()
        emit(HistoryClock(date, now.zone))
        val nextDay = date.plusDays(1).atStartOfDay(now.zone)
        delay(Duration.between(now, nextDay).toMillis().coerceIn(1, 60_000))
    }
}.distinctUntilChanged()
