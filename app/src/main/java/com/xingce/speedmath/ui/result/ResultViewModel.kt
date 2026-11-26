package com.xingce.speedmath.ui.result

import android.database.SQLException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.xingce.speedmath.data.db.PracticeSessionDetails
import com.xingce.speedmath.data.prefs.UserPreferencesStore
import com.xingce.speedmath.data.repository.PracticeRepository
import com.xingce.speedmath.domain.model.MathModule
import com.xingce.speedmath.ui.home.PracticeLaunchConfig
import com.xingce.speedmath.ui.home.practiceLaunchConfig
import java.io.IOException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ResultUiState(
    val details: PracticeSessionDetails? = null,
    val missing: Boolean = false,
    val readError: Boolean = false,
    val launching: Boolean = false,
    val preferencesError: Boolean = false,
)

class ResultViewModel(
    val sessionId: Long,
    private val repository: PracticeRepository,
    private val preferencesStore: UserPreferencesStore,
) : ViewModel() {
    private val state = MutableStateFlow(ResultUiState())
    val uiState = state.asStateFlow()
    private val launches = Channel<PracticeLaunchConfig>(Channel.BUFFERED)
    val newPractice = launches.receiveAsFlow()

    init {
        load()
    }

    fun load() {
        state.update { it.copy(readError = false) }
        viewModelScope.launch {
            try {
                val details = repository.getSessionDetails(sessionId)
                state.update { it.copy(details = details, missing = details == null) }
            } catch (_: SQLException) {
                state.update { it.copy(readError = true) }
            }
        }
    }

    fun playAgain() {
        val current = state.value
        val session = current.details?.session ?: return
        if (current.launching) return
        state.update { it.copy(launching = true, preferencesError = false) }
        viewModelScope.launch {
            try {
                val preferences = preferencesStore.preferences.first()
                launches.send(preferences.practiceLaunchConfig(MathModule.fromId(session.moduleId)))
            } catch (_: IOException) {
                state.update { it.copy(launching = false, preferencesError = true) }
            }
        }
    }

    fun dismissPreferencesError() {
        state.update { it.copy(preferencesError = false) }
    }

    companion object {
        fun factory(
            sessionId: Long,
            repository: PracticeRepository,
            preferencesStore: UserPreferencesStore,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { ResultViewModel(sessionId, repository, preferencesStore) }
        }
    }
}
