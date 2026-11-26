package com.xingce.speedmath.ui.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.xingce.speedmath.domain.engine.QuestionGenerator
import com.xingce.speedmath.domain.model.CustomQuizConfig
import com.xingce.speedmath.domain.model.MathModule
import com.xingce.speedmath.domain.model.Question
import com.xingce.speedmath.domain.model.ToleranceLevel
import com.xingce.speedmath.export.ExportFormat
import com.xingce.speedmath.export.ExportOutcome
import com.xingce.speedmath.export.ExportRequest
import com.xingce.speedmath.export.WorksheetBuilder
import com.xingce.speedmath.export.WorksheetExporter
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExportUiState(
    val module: MathModule = MathModule.FIVE_DIV_THREE,
    val count: Int = 20,
    val format: ExportFormat = ExportFormat.PDF,
    val includeAnswers: Boolean = true,
    val tolerance: ToleranceLevel = ToleranceLevel.STANDARD,
    val customConfig: CustomQuizConfig = CustomQuizConfig(),
    val previewQuestions: List<Question> = emptyList(),
    val exporting: Boolean = false,
    val errorMessage: String? = null,
)

class ExportViewModel(
    private val exporter: WorksheetExporter,
    private val generator: QuestionGenerator = QuestionGenerator(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    private val outcomeChannel = Channel<ExportOutcome>(Channel.BUFFERED)
    val outcomes = outcomeChannel.receiveAsFlow()

    fun configure(
        initialModule: MathModule,
        tolerance: ToleranceLevel,
        customConfig: CustomQuizConfig,
    ) {
        val preview = generatePreview(initialModule, tolerance, customConfig)
        _uiState.value = ExportUiState(
            module = initialModule,
            count = 20,
            format = ExportFormat.PDF,
            includeAnswers = true,
            tolerance = tolerance,
            customConfig = customConfig,
            previewQuestions = preview,
            exporting = false,
        )
    }

    fun selectModule(module: MathModule) {
        _uiState.update { state ->
            state.copy(
                module = module,
                previewQuestions = generatePreview(module, state.tolerance, state.customConfig),
                errorMessage = null,
            )
        }
    }

    fun selectCount(count: Int) {
        _uiState.update { it.copy(count = count, errorMessage = null) }
    }

    fun selectFormat(format: ExportFormat) {
        _uiState.update { it.copy(format = format, errorMessage = null) }
    }

    fun setIncludeAnswers(include: Boolean) {
        _uiState.update { it.copy(includeAnswers = include, errorMessage = null) }
    }

    fun refreshPreview() {
        _uiState.update { state ->
            state.copy(
                previewQuestions = generatePreview(state.module, state.tolerance, state.customConfig),
            )
        }
    }

    fun generateExport() {
        val current = _uiState.value
        if (current.exporting) return
        _uiState.update { it.copy(exporting = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val request = ExportRequest(
                    module = current.module,
                    count = current.count,
                    tolerance = current.tolerance,
                    customConfig = current.customConfig,
                    format = current.format,
                    includeAnswers = current.includeAnswers,
                )
                val outcome = exporter.export(request)
                if (outcome is ExportOutcome.Failed) {
                    _uiState.update { it.copy(errorMessage = outcome.message) }
                } else {
                    outcomeChannel.send(outcome)
                }
            } finally {
                _uiState.update { it.copy(exporting = false) }
            }
        }
    }

    private fun generatePreview(
        module: MathModule,
        tolerance: ToleranceLevel,
        customConfig: CustomQuizConfig,
    ): List<Question> {
        val effectiveTolerance = WorksheetBuilder.resolveTolerance(module, tolerance, customConfig)
        return generator.generateBatch(
            module = module,
            count = 3,
            tolerance = effectiveTolerance,
            customConfig = customConfig,
        )
    }

    companion object {
        fun factory(exporter: WorksheetExporter): ViewModelProvider.Factory = viewModelFactory {
            initializer { ExportViewModel(exporter) }
        }
    }
}
