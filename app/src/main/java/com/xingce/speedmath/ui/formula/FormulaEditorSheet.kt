package com.xingce.speedmath.ui.formula

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.xingce.speedmath.data.db.FormulaEntity
import com.xingce.speedmath.domain.model.FormulaCategory
import com.xingce.speedmath.ui.components.SkyChoicePill
import com.xingce.speedmath.ui.components.SkyCircleIconButton
import com.xingce.speedmath.ui.components.SkyPrimaryButton
import com.xingce.speedmath.ui.theme.BorderSky
import com.xingce.speedmath.ui.theme.PrimarySky
import com.xingce.speedmath.ui.theme.PrimarySkyText
import com.xingce.speedmath.ui.theme.StatusError
import com.xingce.speedmath.ui.theme.SurfaceIceBlue
import com.xingce.speedmath.ui.theme.SurfaceWhite
import com.xingce.speedmath.ui.theme.TextMuted
import com.xingce.speedmath.ui.theme.TextPrimary
import kotlinx.coroutines.launch

private val mathSymbols = listOf("+", "−", "×", "÷", "≈", "%", "()", "r₁", "r₂", "≤", "ⁿ", "²", "| |")
private val expressionBackground = Color(0xFFF2F8FF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FormulaEditorSheet(
    formula: FormulaEntity?,
    busy: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (FormulaEntity) -> Unit,
    onDelete: (Long) -> Unit,
) {
    var title by rememberSaveable(formula?.id, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(atEnd(formula?.title.orEmpty()))
    }
    var expression by rememberSaveable(formula?.id, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(atEnd(formula?.expression.orEmpty()))
    }
    var note by rememberSaveable(formula?.id, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(atEnd(formula?.note.orEmpty()))
    }
    var category by rememberSaveable(formula?.id) {
        mutableStateOf(formula?.let { FormulaCategory.fromName(it.category) } ?: FormulaCategory.CUSTOM)
    }
    var confirmDelete by rememberSaveable(formula?.id) { mutableStateOf(false) }
    val expressionFocus = remember { FocusRequester() }
    val canDismiss by rememberUpdatedState(!busy)
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    ModalBottomSheet(
        onDismissRequest = { if (!busy) onDismiss() },
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { it != SheetValue.Hidden || canDismiss },
        ),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = SurfaceWhite,
        contentColor = TextPrimary,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextMuted.copy(alpha = 0.68f)) },
    ) {
        Box(Modifier.fillMaxWidth().imePadding()) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (formula == null) "新建公式" else "编辑公式",
                        Modifier.weight(1f).semantics { heading() },
                        style = MaterialTheme.typography.titleLarge,
                    )
                    SkyCircleIconButton(
                        Icons.Rounded.Close, "关闭公式编辑器", onDismiss, Modifier.size(34.dp),
                        enabled = !busy, tinted = true,
                    )
                }

                EditorField(
                    "公式名称", title, { title = it }, "请输入公式名称（必填）",
                    enabled = !busy, singleLine = true,
                    error = "请输入非空公式名称".takeIf { title.text.isNotEmpty() && title.text.isBlank() },
                )
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("所属分类", style = MaterialTheme.typography.labelLarge)
                    LazyRow(
                        Modifier.fillMaxWidth().selectableGroup(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(FormulaCategory.entries) { item ->
                            SkyChoicePill(item.label, item == category, { if (!busy) category = item }, compact = true)
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    EditorField(
                        "公式表达式", expression, { expression = it }, "请输入公式表达式（必填）",
                        fieldModifier = Modifier.focusRequester(expressionFocus),
                        emphasized = true,
                        enabled = !busy,
                        error = "请输入非空公式表达式".takeIf {
                            expression.text.isNotEmpty() && expression.text.isBlank()
                        },
                    )
                    LazyRow(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(mathSymbols) { symbol ->
                            Box(
                                Modifier.widthIn(min = 37.dp)
                                    .clip(RoundedCornerShape(8.dp)).background(SurfaceIceBlue)
                                    .clickable(enabled = !busy, role = Role.Button, onClick = {
                                        expression = insertMathSymbol(expression, symbol)
                                        expressionFocus.requestFocus()
                                    })
                                    .heightIn(min = 32.dp).padding(horizontal = 6.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(symbol, color = PrimarySkyText, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
                EditorField("记忆口诀 / 备注", note, { note = it }, "选填", enabled = !busy)
                if (formula != null) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(
                            enabled = !busy && expression.text.isNotBlank(),
                            onClick = {
                                clipboard.setText(AnnotatedString(expression.text))
                                scope.launch {
                                    snackbar.currentSnackbarData?.dismiss()
                                    snackbar.showSnackbar("已复制公式")
                                }
                            },
                        ) { Text("复制公式", color = PrimarySkyText) }
                        TextButton(onClick = { confirmDelete = true }, enabled = !busy) {
                            Text("删除公式", color = StatusError)
                        }
                    }
                }
                error?.let { Text(it, color = StatusError, style = MaterialTheme.typography.bodySmall) }
                SkyPrimaryButton(
                    text = if (busy) "处理中…" else "保存公式",
                    onClick = {
                        val original = formula ?: FormulaEntity(
                            title = "", category = category.name, expression = "", note = "",
                            isFavorite = false, isBuiltIn = false, createdAt = 0, updatedAt = 0,
                        )
                        onSave(original.copy(
                            title = title.text.trim(),
                            category = category.name,
                            expression = expression.text.trim(),
                            note = note.text.trim(),
                        ))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy && title.text.isNotBlank() && expression.text.isNotBlank(),
                )
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
    }
    if (confirmDelete && formula != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = SurfaceWhite,
            title = { Text("删除公式？") },
            text = { Text("确认删除「${formula.title}」？") },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("取消") }
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete(formula.id)
                }) { Text("删除", color = StatusError) }
            },
        )
    }
}

private fun atEnd(text: String) = TextFieldValue(text, selection = TextRange(text.length))

@Composable
private fun EditorField(
    label: String,
    value: TextFieldValue,
    onChange: (TextFieldValue) -> Unit,
    placeholder: String,
    fieldModifier: Modifier = Modifier,
    emphasized: Boolean = false,
    enabled: Boolean,
    singleLine: Boolean = false,
    error: String? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val borderInk = when {
        error != null -> StatusError
        focused || emphasized -> PrimarySky
        else -> BorderSky
    }
    val shape = RoundedCornerShape(10.dp)
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            modifier = fieldModifier.fillMaxWidth()
                .onFocusChanged { focused = it.isFocused }
                .semantics {
                    contentDescription = label
                    if (error != null) error(error)
                },
            enabled = enabled,
            singleLine = singleLine,
            maxLines = if (singleLine) 1 else 4,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
            cursorBrush = SolidColor(PrimarySky),
        ) { field ->
            Box(
                Modifier.fillMaxWidth().heightIn(min = if (emphasized) 48.dp else 42.dp)
                    .background(if (emphasized) expressionBackground else SurfaceWhite, shape)
                    .border(1.dp, borderInk, shape).padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.text.isEmpty()) {
                    Text(placeholder, color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                }
                field()
            }
        }
        if (error != null) Text(error, color = StatusError, style = MaterialTheme.typography.bodySmall)
    }
}
