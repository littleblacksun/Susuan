package com.xingce.speedmath.ui.home

import android.database.SQLException
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.xingce.speedmath.data.db.StatsAggregate
import com.xingce.speedmath.data.prefs.UserPreferences
import com.xingce.speedmath.data.prefs.UserPreferencesStore
import com.xingce.speedmath.data.repository.PracticeRepository
import com.xingce.speedmath.domain.model.CustomQuizConfig
import com.xingce.speedmath.domain.model.KeypadOrder
import com.xingce.speedmath.domain.model.MathModule
import com.xingce.speedmath.domain.model.NBackLevel
import com.xingce.speedmath.domain.model.ToleranceLevel
import java.io.IOException
import java.time.Duration
import java.time.ZonedDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class HomeSheet { COUNT, TOLERANCE, CUSTOM, SETTINGS, EXPORT }

data class HomeUiState(
    val today: TodayBannerModel = formatTodaySummary(StatsAggregate(0, 0, 0)),
    val preferences: UserPreferences = UserPreferences(),
    val category: HomeCategoryFilter = HomeCategoryFilter.DIVISION_ESTIMATION,
    val selectedModule: MathModule = MathModule.FIVE_DIV_THREE,
    val sheet: HomeSheet? = null,
    val preferencesLoaded: Boolean = false,
    val launching: Boolean = false,
) {
    val tolerance: ToleranceLevel
        get() = effectiveToleranceFor(selectedModule, preferences.tolerance, preferences.customQuizConfig)

    val customSubtitle: String
        get() = if (preferences.customQuizConfigured || selectedModule == MathModule.CUSTOM) {
            customConfigSummary(preferences.customQuizConfig)
        } else {
            MathModule.CUSTOM.sampleExpression
        }
}

data class PracticeLaunchConfig(
    val module: MathModule,
    val count: Int,
    val tolerance: ToleranceLevel,
    val nBack: NBackLevel,
    val keypad: KeypadOrder,
    val stylus: Boolean,
    val haptic: Boolean,
    val custom: CustomQuizConfig?,
)

sealed interface HomeEffect {
    data class StartPractice(val config: PracticeLaunchConfig) : HomeEffect
    data class Message(val text: String) : HomeEffect
}

class HomeViewModel(
    private val practiceRepository: PracticeRepository,
    private val preferencesStore: UserPreferencesStore,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private data class Selection(val category: HomeCategoryFilter, val module: MathModule)

    private data class LocalState(
        val selection: Selection? = null,
        val sheet: HomeSheet? = null,
        val launching: Boolean = false,
    )

    private val local = MutableStateFlow(
        LocalState(
            selection = savedStateHandle.get<MathModule>("homeModule")?.let { module ->
                Selection(
                    savedStateHandle["homeCategory"] ?: HomeCategoryFilter.forModule(module),
                    module,
                )
            },
            sheet = savedStateHandle["homeSheet"],
        ),
    )
    private val effectChannel = Channel<HomeEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val todayStats = localDayStarts().flatMapLatest(practiceRepository::observeTodayStats)

    val uiState: StateFlow<HomeUiState> = combine(
        todayStats,
        preferencesStore.preferences,
        local,
    ) { stats, preferences, state ->
        val selection = state.selection ?: Selection(
            HomeCategoryFilter.forModule(preferences.lastModule),
            preferences.lastModule,
        )
        HomeUiState(
            today = formatTodaySummary(stats),
            preferences = preferences,
            category = selection.category,
            selectedModule = selection.module,
            sheet = state.sheet,
            preferencesLoaded = true,
            launching = state.launching,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun selectCategory(category: HomeCategoryFilter) {
        val selected = local.value.selection?.module ?: uiState.value.selectedModule
        val module = if (selected == MathModule.CUSTOM || selected in category.modules) {
            selected
        } else {
            category.modules.first()
        }
        select(Selection(category, module))
    }

    fun selectModule(module: MathModule) {
        val category = local.value.selection?.category ?: uiState.value.category
        select(Selection(category, module))
    }

    private fun select(selection: Selection) {
        savedStateHandle["homeCategory"] = selection.category
        savedStateHandle["homeModule"] = selection.module
        local.update { it.copy(selection = selection) }
    }

    fun showSheet(sheet: HomeSheet?) {
        savedStateHandle["homeSheet"] = sheet
        local.update { it.copy(sheet = sheet) }
    }

    fun showTolerance() {
        if (uiState.value.tolerance == ToleranceLevel.EXACT) {
            message("该题型为基本功精确匹配模式（0误差）")
        } else {
            showSheet(HomeSheet.TOLERANCE)
        }
    }

    fun showExport() = showSheet(HomeSheet.EXPORT)

    fun setQuestionCount(count: Int) = updatePreferences({ it.copy(questionCount = count) }) {
        showSheet(null)
    }

    fun setTolerance(tolerance: ToleranceLevel) {
        val isCustom = uiState.value.selectedModule == MathModule.CUSTOM
        updatePreferences({ preferences ->
            if (isCustom) {
                preferences.copy(customQuizConfig = preferences.customQuizConfig.copy(tolerance = tolerance))
            } else {
                preferences.copy(tolerance = tolerance)
            }
        }) {
            showSheet(null)
        }
    }

    fun cycleKeypad() = updatePreferences({ it.copy(keypadOrder = it.keypadOrder.next()) })

    fun toggleStylus() = updatePreferences({ it.copy(stylusEnabled = !it.stylusEnabled) })

    fun setStylusEnabled(enabled: Boolean) = updatePreferences({ it.copy(stylusEnabled = enabled) })

    fun setHapticEnabled(enabled: Boolean) = updatePreferences({ it.copy(hapticEnabled = enabled) })

    fun setNBackLevel(level: NBackLevel) = updatePreferences({ it.copy(nBackLevel = level) })

    fun saveCustomConfig(config: CustomQuizConfig) {
        updatePreferences({
            it.copy(customQuizConfig = config.normalizedForHome(), customQuizConfigured = true)
        }) {
            selectModule(MathModule.CUSTOM)
            showSheet(null)
        }
    }

    fun startPractice(continueLast: Boolean = false) {
        if (!uiState.value.preferencesLoaded || local.value.launching) return
        val selected = local.value.selection?.module ?: uiState.value.selectedModule
        local.update { it.copy(launching = true) }
        viewModelScope.launch {
            try {
                val preferences = preferencesStore.updatePracticePreferences {
                    if (continueLast) it else it.copy(lastModule = selected)
                }
                effectChannel.send(HomeEffect.StartPractice(preferences.practiceLaunchConfig(preferences.lastModule)))
            } catch (_: IOException) {
                message("暂时无法保存练习设置，请重试")
            } finally {
                local.update { it.copy(launching = false) }
            }
        }
    }

    fun clearPracticeRecords() {
        showSheet(null)
        viewModelScope.launch {
            try {
                practiceRepository.clearAllSessions()
                message("已清空全部练习记录")
            } catch (_: SQLException) {
                message("暂时无法清空练习记录，请重试")
            }
        }
    }

    private fun updatePreferences(
        transform: (UserPreferences) -> UserPreferences,
        onSaved: () -> Unit = {},
    ) {
        viewModelScope.launch {
            try {
                preferencesStore.updatePracticePreferences(transform)
                onSaved()
            } catch (_: IOException) {
                message("暂时无法保存设置，请重试")
            }
        }
    }

    private fun message(text: String) {
        effectChannel.trySend(HomeEffect.Message(text))
    }

    companion object {
        fun factory(
            repository: PracticeRepository,
            preferencesStore: UserPreferencesStore,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { HomeViewModel(repository, preferencesStore, createSavedStateHandle()) }
        }
    }
}

private fun localDayStarts() = flow {
    while (true) {
        val now = ZonedDateTime.now()
        val today = now.toLocalDate().atStartOfDay(now.zone)
        val next = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
        emit(today.toInstant().toEpochMilli())
        // Recheck wall-clock and time-zone changes even while the screen stays open.
        delay(Duration.between(now, next).toMillis().coerceIn(1, 60_000))
    }
}.distinctUntilChanged()
