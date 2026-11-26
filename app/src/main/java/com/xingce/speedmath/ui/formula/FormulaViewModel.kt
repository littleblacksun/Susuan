package com.xingce.speedmath.ui.formula

import android.database.SQLException
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.xingce.speedmath.data.db.FormulaEntity
import com.xingce.speedmath.data.repository.FormulaRepository
import com.xingce.speedmath.domain.model.FormulaCategory
import java.io.IOException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal enum class FormulaFilter(private val title: String = "", val category: FormulaCategory? = null) {
    ALL("全部"),
    FAVORITE("收藏"),
    GROWTH_RATE(category = FormulaCategory.GROWTH_RATE),
    BASE_PERIOD(category = FormulaCategory.BASE_PERIOD),
    PROPORTION(category = FormulaCategory.PROPORTION),
    AVERAGE(category = FormulaCategory.AVERAGE),
    OTHER_HIGH_FREQ(category = FormulaCategory.OTHER_HIGH_FREQ),
    CUSTOM(category = FormulaCategory.CUSTOM);

    val label: String get() = category?.label ?: title
}

internal fun filterFormulas(
    formulas: List<FormulaEntity>,
    query: String,
    filter: FormulaFilter,
): List<FormulaEntity> {
    val keyword = query.trim()
    return formulas.filter { formula ->
        (filter != FormulaFilter.FAVORITE || formula.isFavorite) &&
            (filter.category == null || FormulaCategory.fromName(formula.category) == filter.category) &&
            (formula.title.contains(keyword, ignoreCase = true) ||
                formula.expression.contains(keyword, ignoreCase = true) ||
                formula.note.contains(keyword, ignoreCase = true))
    }
}

internal fun insertMathSymbol(value: TextFieldValue, symbol: String): TextFieldValue {
    val start = value.selection.min
    val text = value.text.replaceRange(start, value.selection.max, symbol)
    val offset = if (symbol == "()" || symbol == "| |") 1 else symbol.length
    return TextFieldValue(text, selection = TextRange(start + offset))
}

internal data class FormulaUiState(
    val formulas: List<FormulaEntity>? = null,
    val loadError: Boolean = false,
    val editingInProgress: Boolean = false,
    val editorError: String? = null,
)

internal data class FormulaFeedback(val message: String, val closeEditor: Boolean = false)

internal class FormulaViewModel(private val repository: FormulaRepository) : ViewModel() {
    private val state = MutableStateFlow(FormulaUiState())
    val uiState = state.asStateFlow()
    private val feedback = Channel<FormulaFeedback>(Channel.BUFFERED)
    val effects = feedback.receiveAsFlow()
    private var loadJob: Job? = null

    init { load() }

    fun load() {
        loadJob?.cancel()
        state.update { it.copy(loadError = false) }
        loadJob = viewModelScope.launch {
            if (!tryWrite {
                    repository.ensureSeeded()
                    repository.observeFormulas().collect { formulas ->
                        state.update { it.copy(formulas = formulas) }
                    }
                }
            ) state.update { it.copy(loadError = true) }
        }
    }

    fun toggleFavorite(formula: FormulaEntity) {
        viewModelScope.launch {
            if (!tryWrite { repository.toggleFavorite(formula.id, !formula.isFavorite) }) {
                feedback.send(FormulaFeedback("暂时无法更新收藏，请重试"))
            }
        }
    }

    fun clearEditorError() = state.update { it.copy(editorError = null) }

    fun save(formula: FormulaEntity) = edit("已保存公式", "暂时无法保存公式，请重试") {
        repository.saveFormula(formula)
    }

    fun delete(id: Long) = edit("已删除公式", "暂时无法删除公式，请重试") {
        repository.deleteFormula(id)
    }

    private fun edit(success: String, error: String, operation: suspend () -> Unit) {
        if (state.value.editingInProgress) return
        state.update { it.copy(editingInProgress = true, editorError = null) }
        viewModelScope.launch {
            val completed = tryWrite(operation)
            if (completed) {
                feedback.send(FormulaFeedback(success, closeEditor = true))
                if (state.value.loadError) load()
            }
            state.update { it.copy(editingInProgress = false, editorError = error.takeUnless { completed }) }
        }
    }

    private suspend fun tryWrite(operation: suspend () -> Unit): Boolean = try {
        operation()
        true
    } catch (_: SQLException) {
        false
    } catch (_: IOException) {
        false
    }

    companion object {
        fun factory(repository: FormulaRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { FormulaViewModel(repository) }
        }
    }
}
