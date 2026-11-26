package com.xingce.speedmath.ui.formula

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xingce.speedmath.data.db.FormulaEntity
import com.xingce.speedmath.domain.model.FormulaCategory
import com.xingce.speedmath.ui.components.SkyCard
import com.xingce.speedmath.ui.components.SkyChoicePill
import com.xingce.speedmath.ui.components.SkyCircleIconButton
import com.xingce.speedmath.ui.components.SkyPageHeader
import com.xingce.speedmath.ui.components.SkyPrimaryButton
import com.xingce.speedmath.ui.components.SkySegmentedPill
import com.xingce.speedmath.ui.theme.BgSkyWhite
import com.xingce.speedmath.ui.theme.BorderSky
import com.xingce.speedmath.ui.theme.PrimarySky
import com.xingce.speedmath.ui.theme.PrimarySkyText
import com.xingce.speedmath.ui.theme.SurfaceSkyTint
import com.xingce.speedmath.ui.theme.SurfaceWhite
import com.xingce.speedmath.ui.theme.TabularTextStyle
import com.xingce.speedmath.ui.theme.TextPrimary
import com.xingce.speedmath.ui.theme.TextSecondary
import kotlinx.coroutines.launch

private const val NewFormulaId = 0L
private val formulaSegments = listOf("我的公式", "百化分速查")

@Composable
internal fun FormulaRoute(viewModel: FormulaViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var segment by rememberSaveable { mutableIntStateOf(0) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(FormulaFilter.ALL) }
    var editorId by rememberSaveable { mutableStateOf<Long?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    fun message(text: String) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(text)
        }
    }

    fun closeSearch() {
        keyboard?.hide()
        searching = false
        query = ""
    }

    fun openEditor(id: Long) {
        keyboard?.hide()
        viewModel.clearEditorError()
        editorId = id
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            if (effect.closeEditor) editorId = null
            message(effect.message)
        }
    }
    BackHandler(searching && editorId == null) { closeSearch() }

    Box(Modifier.fillMaxSize().background(BgSkyWhite)) {
        Column(
            Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        ) {
            SkyPageHeader("公式本") {
                SkyCircleIconButton(
                    Icons.Rounded.Search,
                    if (searching) "收起搜索" else "搜索公式",
                    onClick = {
                        if (searching) closeSearch() else {
                            segment = 0
                            searching = true
                        }
                    },
                    modifier = Modifier.size(38.dp),
                )
                SkyCircleIconButton(
                    Icons.Rounded.Add, "新建公式", { openEditor(NewFormulaId) },
                    Modifier.size(38.dp), primary = true,
                )
            }
            if (searching) FormulaSearchField(query, { query = it })
            SkySegmentedPill(
                labels = formulaSegments,
                selectedIndex = segment,
                onSelect = {
                    segment = it
                    if (it != 0) closeSearch()
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                segmentMinHeight = 36.dp,
            )

            if (segment == 0) {
                val formulas = state.formulas
                val displayed = remember(formulas, query, filter) {
                    formulas?.let { filterFormulas(it, query, filter) }
                }
                LazyRow(
                    modifier = Modifier.fillMaxWidth().selectableGroup(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    items(FormulaFilter.entries) { item ->
                        SkyChoicePill(item.label, filter == item, { filter = item })
                    }
                }
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    when {
                        state.loadError -> item {
                            FormulaEmptyCard("暂时无法读取公式", "重新加载", viewModel::load)
                        }
                        displayed == null -> item {
                            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = PrimarySky)
                            }
                        }
                        displayed.isEmpty() -> item {
                            val prompt = when {
                                query.isNotBlank() -> "没有找到匹配的公式"
                                filter == FormulaFilter.FAVORITE -> "还没有收藏公式"
                                filter == FormulaFilter.CUSTOM -> "还没有自定义公式"
                                else -> "这个分类还没有公式"
                            }
                            FormulaEmptyCard(prompt, "新建一条公式") { openEditor(NewFormulaId) }
                        }
                        else -> items(displayed, key = { it.id }) { formula ->
                            FormulaCard(
                                formula,
                                onOpen = { openEditor(formula.id) },
                                onFavorite = { viewModel.toggleFavorite(formula) },
                                onCopy = {
                                    clipboard.setText(AnnotatedString(formula.expression))
                                    message("已复制公式")
                                },
                            )
                        }
                    }
                }
            } else {
                BaifenReferenceContent { value ->
                    clipboard.setText(AnnotatedString(value))
                    message("已复制 $value")
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    val editing = state.formulas?.firstOrNull { it.id == editorId }
    if (editorId == NewFormulaId || editing != null) {
        FormulaEditorSheet(
            formula = editing,
            busy = state.editingInProgress,
            error = state.editorError,
            onDismiss = { editorId = null },
            onSave = viewModel::save,
            onDelete = viewModel::delete,
        )
    }
}

@Composable
private fun FormulaSearchField(query: String, onChange: (String) -> Unit) {
    val focusRequester = remember { FocusRequester() }
    OutlinedTextField(
        value = query,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
            .focusRequester(focusRequester),
        singleLine = true,
        placeholder = { Text("搜索名称、表达式或口诀") },
        leadingIcon = { Icon(Icons.Rounded.Search, null, tint = TextSecondary) },
        trailingIcon = if (query.isNotEmpty()) {
            { IconButton(onClick = { onChange("") }) { Icon(Icons.Rounded.Close, "清空搜索") } }
        } else null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SurfaceWhite,
            unfocusedContainerColor = SurfaceWhite,
            focusedBorderColor = PrimarySky,
            unfocusedBorderColor = BorderSky,
        ),
        shape = RoundedCornerShape(14.dp),
    )
    LaunchedEffect(focusRequester) { focusRequester.requestFocus() }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FormulaCard(
    formula: FormulaEntity,
    onOpen: () -> Unit,
    onFavorite: () -> Unit,
    onCopy: () -> Unit,
) {
    val category = FormulaCategory.fromName(formula.category)
    SkyCard(Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
        Column(
            Modifier.fillMaxWidth().combinedClickable(
                role = Role.Button,
                onClickLabel = "编辑公式",
                onLongClickLabel = "复制公式",
                onLongClick = onCopy,
                onClick = onOpen,
            ).padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 9.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        formula.title,
                        modifier = Modifier.weight(1f, fill = false),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                    )
                    Text(
                        category.label,
                        Modifier.background(SurfaceSkyTint, CircleShape).padding(horizontal = 8.dp, vertical = 1.dp),
                        color = PrimarySkyText,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                IconButton(onClick = onFavorite, modifier = Modifier.size(38.dp)) {
                    Icon(
                        if (formula.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                        if (formula.isFavorite) "取消收藏" else "收藏公式",
                        tint = if (formula.isFavorite) PrimarySky else TextSecondary,
                    )
                }
            }
            Text(
                formula.expression,
                Modifier.fillMaxWidth().background(Color(0xFFEFF7FF), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium.TabularTextStyle,
                textAlign = TextAlign.Center,
            )
            if (formula.note.isNotBlank()) {
                Text(formula.note, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun FormulaEmptyCard(prompt: String, action: String, onClick: () -> Unit) {
    SkyCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(prompt, color = TextSecondary, textAlign = TextAlign.Center)
            SkyPrimaryButton(action, onClick)
        }
    }
}
