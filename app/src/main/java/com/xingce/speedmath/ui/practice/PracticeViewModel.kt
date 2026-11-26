package com.xingce.speedmath.ui.practice

import android.database.SQLException
import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.xingce.speedmath.data.db.PracticeSessionDetails
import com.xingce.speedmath.data.prefs.UserPreferencesStore
import com.xingce.speedmath.data.repository.PracticeRepository
import com.xingce.speedmath.domain.engine.NBackController
import com.xingce.speedmath.domain.engine.NBackPhase
import com.xingce.speedmath.domain.engine.NBackStepState
import com.xingce.speedmath.domain.engine.QuestionGenerator
import com.xingce.speedmath.domain.model.CustomQuizConfig
import com.xingce.speedmath.domain.model.GradeResult
import com.xingce.speedmath.domain.model.KeypadOrder
import com.xingce.speedmath.domain.model.NBackLevel
import com.xingce.speedmath.domain.model.Question
import com.xingce.speedmath.domain.model.ToleranceLevel
import com.xingce.speedmath.ui.home.PracticeLaunchConfig
import com.xingce.speedmath.ui.home.practiceLaunchConfig
import java.io.IOException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface PracticeStart {
    data class New(val config: PracticeLaunchConfig) : PracticeStart
    data class Retry(val sessionId: Long) : PracticeStart
}

data class PracticeUiState(
    val title: String = "",
    val tolerance: ToleranceLevel = ToleranceLevel.STANDARD,
    val step: NBackStepState? = null,
    val input: String = "",
    val feedback: GradeResult? = null,
    val keypadOrder: KeypadOrder = KeypadOrder.ASCENDING,
    val digits: List<Int> = keypadDigitsFor(KeypadOrder.ASCENDING),
    val scratchpadOpen: Boolean = false,
    val hapticEnabled: Boolean = true,
    val elapsedMs: Long = 0,
    val confirmExit: Boolean = false,
    val loadError: String? = null,
    val preferenceError: Boolean = false,
    val saving: Boolean = false,
    val saveError: Boolean = false,
    val savedSessionId: Long? = null,
) {
    val canType: Boolean
        get() = step?.targetQuestion != null && feedback == null && !confirmExit && !saving && !saveError

    val actionLabel: String
        get() = when {
            step?.phase == NBackPhase.MEMORIZE -> "继续"
            step != null && step.stepIndex == step.totalSteps - 1 -> "完成"
            else -> "下一题"
        }
}

class PracticeViewModel(
    start: PracticeStart,
    private val repository: PracticeRepository,
    private val preferenceStore: UserPreferencesStore,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val state = MutableStateFlow(
        PracticeUiState(savedSessionId = savedState["completedSessionId"]),
    )
    val uiState = state.asStateFlow()

    private var controller: NBackController? = null
    private var foreground = false
    private var runningSince: Long? = null
    private var accumulatedMs = 0L
    private var stepStartMs = 0L
    private var clockJob: Job? = null
    private var feedbackJob: Job? = null
    private var pendingRecord: PracticeSessionDetails? = null

    init {
        if (state.value.savedSessionId == null) {
            when (start) {
                is PracticeStart.New -> {
                    val config = start.config
                    initialize(
                        config,
                        QuestionGenerator().generateBatch(
                            config.module, config.count, config.tolerance, config.custom ?: CustomQuizConfig(),
                        ),
                    )
                }
                is PracticeStart.Retry -> loadRetry(start.sessionId)
            }
        }
    }

    private fun loadRetry(sessionId: Long) {
        viewModelScope.launch {
            try {
                val details = repository.getSessionDetails(sessionId)
                val questions = details?.let(QuestionReconstructor::fromFailedItems).orEmpty()
                if (details == null || questions.isEmpty()) {
                    failToLoad("这组练习没有可重练的错题")
                    return@launch
                }
                val first = questions.first()
                val nBack = NBackLevel.entries.firstOrNull { it.steps == details.session.nBackSteps }
                    ?: NBackLevel.OFF
                val config = preferenceStore.preferences.first().practiceLaunchConfig(first.module)
                    .copy(tolerance = first.tolerance, nBack = nBack)
                initialize(config, questions, retry = true)
            } catch (_: SQLException) {
                failToLoad("暂时无法读取错题，请返回后重试")
            } catch (_: IOException) {
                failToLoad("暂时无法读取练习偏好，请返回后重试")
            } catch (_: NumberFormatException) {
                failToLoad("这组错题的答案记录无法读取")
            }
        }
    }

    private fun failToLoad(message: String) {
        state.update { it.copy(loadError = message) }
    }

    private fun initialize(config: PracticeLaunchConfig, questions: List<Question>, retry: Boolean = false) {
        controller = NBackController(questions, config.nBack)
        state.update {
            it.copy(
                title = config.module.title + if (retry) " · 错题" else "",
                tolerance = questions.first().tolerance,
                keypadOrder = config.keypad,
                scratchpadOpen = config.stylus,
                hapticEnabled = config.haptic,
            )
        }
        showNextStep()
    }

    fun setForeground(resumed: Boolean) {
        foreground = resumed
        synchronizeClock()
        scheduleFeedback()
    }

    fun setScratchpadOpen(open: Boolean) {
        state.update { it.copy(scratchpadOpen = open) }
    }

    fun setKeypadOrder(order: KeypadOrder) {
        if (order == state.value.keypadOrder) return
        state.update { it.copy(keypadOrder = order, digits = keypadDigitsFor(order)) }
        viewModelScope.launch {
            try {
                preferenceStore.updatePracticePreferences { it.copy(keypadOrder = order) }
            } catch (_: IOException) {
                state.update { it.copy(preferenceError = true) }
            }
        }
    }

    fun dismissPreferenceError() {
        state.update { it.copy(preferenceError = false) }
    }

    fun type(action: KeypadAction) {
        state.update {
            if (it.canType) it.copy(input = reduceKeypadInput(it.input, action)) else it
        }
    }

    fun advance() {
        val current = state.value
        val engine = controller ?: return
        if (current.confirmExit || current.saving || current.saveError || current.savedSessionId != null) return
        val step = current.step ?: return
        if (current.feedback != null) {
            showNextStep()
        } else if (step.phase == NBackPhase.MEMORIZE) {
            stopClock()
            engine.advanceMemorize()
            showNextStep()
        } else if (step.targetQuestion != null) {
            stopClock()
            val (grade, _) = engine.submitAnswer(current.input, accumulatedMs - stepStartMs)
            state.update { it.copy(feedback = grade) }
            scheduleFeedback()
        }
    }

    fun setExitConfirmation(show: Boolean) {
        if (state.value.saving || state.value.savedSessionId != null) return
        state.update { it.copy(confirmExit = show) }
        synchronizeClock()
        scheduleFeedback()
    }

    private fun scheduleFeedback() {
        feedbackJob?.cancel()
        val current = state.value
        if (foreground && current.feedback != null && !current.confirmExit &&
            !current.saving && !current.saveError && current.savedSessionId == null
        ) {
            feedbackJob = viewModelScope.launch {
                delay(750)
                feedbackJob = null
                showNextStep()
            }
        }
    }

    private fun showNextStep() {
        feedbackJob?.cancel()
        feedbackJob = null
        val engine = controller ?: return
        val step = engine.currentState()
        if (step.phase == NBackPhase.COMPLETED) {
            pendingRecord = mapPracticeSession(
                engine.answeredItems(), accumulatedMs, step.effectiveN, System.currentTimeMillis(),
            )
            saveCompleted()
        } else {
            stepStartMs = accumulatedMs
            state.update {
                it.copy(step = step, input = "", feedback = null, digits = keypadDigitsFor(it.keypadOrder))
            }
            synchronizeClock()
        }
    }

    fun saveCompleted() {
        val record = pendingRecord ?: return
        if (state.value.saving || state.value.savedSessionId != null) return
        state.update { it.copy(saving = true, saveError = false, confirmExit = false) }
        viewModelScope.launch {
            try {
                val sessionId = repository.saveSessionWithItems(record.session, record.items)
                savedState["completedSessionId"] = sessionId
                pendingRecord = null
                state.update { it.copy(saving = false, savedSessionId = sessionId) }
            } catch (_: SQLException) {
                state.update { it.copy(saving = false, saveError = true) }
            }
        }
    }

    private fun synchronizeClock() {
        val current = state.value
        val canRun = foreground && current.step != null && current.feedback == null &&
            !current.confirmExit && !current.saving && !current.saveError && current.savedSessionId == null
        if (!canRun) {
            stopClock()
        } else if (runningSince == null) {
            runningSince = SystemClock.elapsedRealtime()
            clockJob = viewModelScope.launch {
                while (true) {
                    val started = runningSince ?: return@launch
                    val elapsed = accumulatedMs + SystemClock.elapsedRealtime() - started
                    state.update { it.copy(elapsedMs = elapsed) }
                    delay(200)
                }
            }
        }
    }

    private fun stopClock() {
        val started = runningSince ?: return
        runningSince = null
        clockJob?.cancel()
        accumulatedMs += SystemClock.elapsedRealtime() - started
        state.update { it.copy(elapsedMs = accumulatedMs) }
    }

    companion object {
        fun factory(
            start: PracticeStart,
            repository: PracticeRepository,
            preferences: UserPreferencesStore,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { PracticeViewModel(start, repository, preferences, createSavedStateHandle()) }
        }
    }
}
