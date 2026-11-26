package com.xingce.speedmath.ui.export

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xingce.speedmath.domain.model.CustomQuizConfig
import com.xingce.speedmath.domain.model.MathModule
import com.xingce.speedmath.domain.model.ModuleCategory
import com.xingce.speedmath.domain.model.Question
import com.xingce.speedmath.domain.model.ToleranceLevel
import com.xingce.speedmath.export.ExportFormat
import com.xingce.speedmath.export.ExportOutcome
import com.xingce.speedmath.export.WorksheetExporter
import com.xingce.speedmath.export.formatQuestionLine
import com.xingce.speedmath.ui.components.SkyPrimaryButton
import com.xingce.speedmath.ui.theme.BorderSky
import com.xingce.speedmath.ui.theme.PrimarySky
import com.xingce.speedmath.ui.theme.PrimarySkyDeep
import com.xingce.speedmath.ui.theme.PrimarySkyGradient
import com.xingce.speedmath.ui.theme.PrimarySkyText
import com.xingce.speedmath.ui.theme.StatusError
import com.xingce.speedmath.ui.theme.SurfaceIceBlue
import com.xingce.speedmath.ui.theme.SurfaceWhite
import com.xingce.speedmath.ui.theme.TabularTextStyle
import com.xingce.speedmath.ui.theme.TextPrimary
import com.xingce.speedmath.ui.theme.TextSecondary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private val exportCounts = listOf(10, 20, 30, 50)
private val dragHandleColor = Color(0xFFB5C4D6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportSheet(
    viewModel: ExportViewModel,
    initialModule: MathModule,
    tolerance: ToleranceLevel,
    customConfig: CustomQuizConfig,
    snackbarHostState: SnackbarHostState,
    hostScope: CoroutineScope,
    onDismiss: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showModulePicker by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(initialModule, tolerance, customConfig) {
        viewModel.configure(initialModule, tolerance, customConfig)
    }

    LaunchedEffect(viewModel) {
        viewModel.outcomes.collect { outcome ->
            handleExportOutcome(
                context = context,
                outcome = outcome,
                snackbarHostState = snackbarHostState,
                hostScope = hostScope,
                onDismiss = onDismiss,
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = { if (!state.exporting) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = SurfaceWhite,
        contentColor = TextPrimary,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(dragHandleColor),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 1. Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "导出练习卷",
                    modifier = Modifier.weight(1f).semantics { heading() },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
                Surface(
                    onClick = onDismiss,
                    enabled = !state.exporting,
                    modifier = Modifier.size(38.dp),
                    shape = CircleShape,
                    color = SurfaceIceBlue,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "关闭",
                            tint = PrimarySkyDeep,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            // 2. Module selector row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceIceBlue)
                    .clickable(enabled = !state.exporting, role = Role.Button) { showModulePicker = true }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "题型",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = state.module.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimarySkyText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = PrimarySkyText,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            // 3. Question count pills
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "导出题量",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    exportCounts.forEach { count ->
                        val active = state.count == count
                        val bg = if (active) {
                            Modifier.background(PrimarySkyGradient)
                        } else {
                            Modifier.background(SurfaceWhite).border(1.dp, BorderSky, CircleShape)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 42.dp)
                                .clip(CircleShape)
                                .then(bg)
                                .selectable(
                                    selected = active,
                                    enabled = !state.exporting,
                                    role = Role.RadioButton,
                                    onClick = { viewModel.selectCount(count) },
                                )
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "${count}题",
                                style = MaterialTheme.typography.labelLarge.TabularTextStyle,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                color = if (active) SurfaceWhite else TextPrimary,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }

            // 4. Export format cards
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "导出格式",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ExportFormatCard(
                        label = ExportFormat.PDF.label,
                        icon = Icons.Rounded.PictureAsPdf,
                        selected = state.format == ExportFormat.PDF,
                        enabled = !state.exporting,
                        onClick = { viewModel.selectFormat(ExportFormat.PDF) },
                        modifier = Modifier.weight(1f),
                    )
                    ExportFormatCard(
                        label = ExportFormat.TEXT.label,
                        icon = Icons.Rounded.ContentCopy,
                        selected = state.format == ExportFormat.TEXT,
                        enabled = !state.exporting,
                        onClick = { viewModel.selectFormat(ExportFormat.TEXT) },
                        modifier = Modifier.weight(1f),
                    )
                    ExportFormatCard(
                        label = ExportFormat.IMAGE.label,
                        icon = Icons.Rounded.Image,
                        selected = state.format == ExportFormat.IMAGE,
                        enabled = !state.exporting,
                        onClick = { viewModel.selectFormat(ExportFormat.IMAGE) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // 5. Reference answers switch row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceIceBlue)
                    .toggleable(
                        value = state.includeAnswers,
                        enabled = !state.exporting,
                        role = Role.Switch,
                        onValueChange = viewModel::setIncludeAnswers,
                    )
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "附带参考答案与误差区间",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
                Switch(
                    checked = state.includeAnswers,
                    onCheckedChange = null,
                    enabled = !state.exporting,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SurfaceWhite,
                        checkedTrackColor = PrimarySky,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = SurfaceWhite,
                        uncheckedBorderColor = BorderSky,
                    ),
                )
            }

            // 6. Paper-like 3-question preview card
            PreviewPaperCard(
                questions = state.previewQuestions,
                enabled = !state.exporting,
                onRefresh = viewModel::refreshPreview,
            )

            state.errorMessage?.let { message ->
                Text(
                    text = message,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    style = MaterialTheme.typography.bodySmall,
                    color = StatusError,
                )
            }

            // 7. Bottom primary CTA button
            SkyPrimaryButton(
                text = if (state.exporting) "生成中…" else "生成导出",
                onClick = viewModel::generateExport,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                enabled = !state.exporting,
            )
        }
    }

    if (showModulePicker) {
        ExportModulePickerDialog(
            selected = state.module,
            onSelect = { module ->
                viewModel.selectModule(module)
                showModulePicker = false
            },
            onDismiss = { showModulePicker = false },
        )
    }
}

@Composable
private fun ExportFormatCard(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        modifier = modifier
            .heightIn(min = 96.dp)
            .clip(shape)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        shape = shape,
        color = if (selected) SurfaceIceBlue else SurfaceWhite,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) PrimarySky else BorderSky,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) PrimarySkyDeep else TextPrimary,
                modifier = Modifier.size(30.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun PreviewPaperCard(
    questions: List<Question>,
    enabled: Boolean,
    onRefresh: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceWhite,
        border = BorderStroke(1.dp, BorderSky),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 42.dp, top = 14.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                questions.forEachIndexed { index, question ->
                    Text(
                        text = formatQuestionLine(index + 1, question),
                        style = MaterialTheme.typography.bodyMedium.TabularTextStyle.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                        ),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(
                onClick = onRefresh,
                enabled = enabled,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(34.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "换一批预览",
                    tint = PrimarySkyText,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun ExportModulePickerDialog(
    selected: MathModule,
    onSelect: (MathModule) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceWhite,
        title = {
            Text(
                text = "选择导出题型",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ModuleCategory.entries.forEach { category ->
                    item(key = "cat_${category.name}") {
                        Text(
                            text = category.displayName,
                            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                        )
                    }
                    items(MathModule.byCategory(category), key = { it.id }) { module ->
                        ModulePickerRow(
                            module = module,
                            selected = module == selected,
                            onClick = { onSelect(module) },
                        )
                    }
                }
                item(key = "cat_custom") {
                    Text(
                        text = "自由配置",
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )
                }
                item(key = MathModule.CUSTOM.id) {
                    ModulePickerRow(
                        module = MathModule.CUSTOM,
                        selected = selected == MathModule.CUSTOM,
                        onClick = { onSelect(MathModule.CUSTOM) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = PrimarySkyText)
            }
        },
    )
}

@Composable
private fun ModulePickerRow(
    module: MathModule,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clip(shape)
            .background(if (selected) SurfaceIceBlue else SurfaceWhite)
            .border(1.dp, if (selected) PrimarySky else BorderSky, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = module.title,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) PrimarySkyText else TextPrimary,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            )
            Text(
                text = module.sampleExpression,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = PrimarySkyText,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private fun handleExportOutcome(
    context: Context,
    outcome: ExportOutcome,
    snackbarHostState: SnackbarHostState,
    hostScope: CoroutineScope,
    onDismiss: () -> Unit,
) {
    when (outcome) {
        is ExportOutcome.CopiedText -> {
            onDismiss()
            hostScope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar("已复制 ${outcome.count} 道题到剪贴板")
            }
        }
        is ExportOutcome.PdfReady -> {
            onDismiss()
            val launched = WorksheetExporter.shareFile(
                context = context,
                uri = outcome.uri,
                mimeType = "application/pdf",
                chooserTitle = "分享或打印 PDF 练习卷",
            )
            hostScope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(
                    if (launched) "已生成 PDF 练习卷" else "已生成 PDF，但未找到可打开的应用",
                )
            }
        }
        is ExportOutcome.ImageSaved -> {
            onDismiss()
            if (outcome.savedToGallery) {
                hostScope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    val result = snackbarHostState.showSnackbar(
                        message = "已保存高清长图到相册",
                        actionLabel = "分享",
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        WorksheetExporter.shareFile(
                            context = context,
                            uri = outcome.uri,
                            mimeType = "image/png",
                            chooserTitle = "分享练习卷长图",
                        )
                    }
                }
            } else {
                val launched = WorksheetExporter.shareFile(
                    context = context,
                    uri = outcome.uri,
                    mimeType = "image/png",
                    chooserTitle = "保存或分享练习卷长图",
                )
                hostScope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(
                        if (launched) "已生成高清长图" else "已生成高清长图，但未找到可分享的应用",
                    )
                }
            }
        }
        is ExportOutcome.Failed -> Unit
    }
}
