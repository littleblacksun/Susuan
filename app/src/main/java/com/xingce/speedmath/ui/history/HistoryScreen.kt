package com.xingce.speedmath.ui.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xingce.speedmath.data.db.PracticeSessionEntity
import com.xingce.speedmath.domain.model.CustomQuizConfig
import com.xingce.speedmath.domain.model.MathModule
import com.xingce.speedmath.domain.model.ToleranceLevel
import com.xingce.speedmath.ui.components.SkyCard
import com.xingce.speedmath.ui.components.SkyChoicePill
import com.xingce.speedmath.ui.components.SkyCircleIconButton
import com.xingce.speedmath.ui.components.SkyPageHeader
import com.xingce.speedmath.ui.components.SkyPrimaryButton
import com.xingce.speedmath.ui.export.ExportSheet
import com.xingce.speedmath.ui.export.ExportViewModel
import com.xingce.speedmath.ui.home.SettingsSheet
import com.xingce.speedmath.ui.practice.formatPracticeTime
import com.xingce.speedmath.ui.theme.BgSkyWhite
import com.xingce.speedmath.ui.theme.BorderSky
import com.xingce.speedmath.ui.theme.PrimarySky
import com.xingce.speedmath.ui.theme.PrimarySkyText
import com.xingce.speedmath.ui.theme.StatusError
import com.xingce.speedmath.ui.theme.SurfaceIceBlue
import com.xingce.speedmath.ui.theme.SurfaceWhite
import com.xingce.speedmath.ui.theme.TabularTextStyle
import com.xingce.speedmath.ui.theme.TextPrimary
import com.xingce.speedmath.ui.theme.TextSecondary
import java.time.ZoneId
import kotlinx.coroutines.launch

@Composable
internal fun HistoryRoute(
    viewModel: HistoryViewModel,
    exportViewModel: ExportViewModel,
    onOpenSession: (Long) -> Unit,
    onHome: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf(HistoryFilter.ALL) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showExport by rememberSaveable { mutableStateOf(false) }
    var deleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val hostScope = rememberCoroutineScope()
    fun message(text: String) {
        hostScope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(text)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { message(it) }
    }

    HistoryScreen(
        state, filter, { filter = it }, onOpenSession, { deleteId = it },
        onHome, viewModel::reload,
        onExport = { showExport = true },
        onSettings = { showSettings = true },
        settingsEnabled = preferences != null,
        snackbar = snackbar,
    )
    val loadedPreferences = preferences
    if (showExport) {
        ExportSheet(
            viewModel = exportViewModel,
            initialModule = loadedPreferences?.lastModule ?: MathModule.FIVE_DIV_THREE,
            tolerance = loadedPreferences?.tolerance ?: ToleranceLevel.STANDARD,
            customConfig = loadedPreferences?.customQuizConfig ?: CustomQuizConfig(),
            snackbarHostState = snackbar,
            hostScope = hostScope,
            onDismiss = { showExport = false },
        )
    }
    if (showSettings && loadedPreferences != null) {
        SettingsSheet(
            preferences = loadedPreferences,
            onDismiss = { showSettings = false },
            onNBackChange = { level -> viewModel.updatePreferences { it.copy(nBackLevel = level) } },
            onHapticChange = { enabled -> viewModel.updatePreferences { it.copy(hapticEnabled = enabled) } },
            onStylusChange = { enabled -> viewModel.updatePreferences { it.copy(stylusEnabled = enabled) } },
            onClearRecords = {
                showSettings = false
                viewModel.clearAllSessions()
            },
        )
    }
    val sessionToDelete = state.sessions?.firstOrNull { it.id == deleteId }
    if (sessionToDelete != null) {
        AlertDialog(
            onDismissRequest = { deleteId = null },
            containerColor = SurfaceWhite,
            title = { Text("删除练习记录？") },
            text = { Text("确认删除「${sessionToDelete.moduleTitle}」的本次练习及逐题记录？") },
            dismissButton = { TextButton(onClick = { deleteId = null }) { Text("取消") } },
            confirmButton = {
                TextButton(onClick = {
                    deleteId = null
                    viewModel.deleteSession(sessionToDelete.id)
                }) { Text("删除", color = StatusError) }
            },
        )
    }
}

@Composable
private fun HistoryScreen(
    state: HistoryUiState,
    filter: HistoryFilter,
    onFilter: (HistoryFilter) -> Unit,
    onOpenSession: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onHome: () -> Unit,
    onReload: () -> Unit,
    onExport: () -> Unit,
    onSettings: () -> Unit,
    settingsEnabled: Boolean,
    snackbar: SnackbarHostState,
) {
    val groups = remember(state.sessions, filter, state.clock) {
        state.sessions?.let { sessions ->
            groupHistorySessions(filterHistorySessions(sessions, filter), state.clock.zone, state.clock.date)
        }
    }

    Box(Modifier.fillMaxSize().background(BgSkyWhite)) {
        Column(
            Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        ) {
            SkyPageHeader("统计与记录") {
                SkyCircleIconButton(Icons.Rounded.IosShare, "导出", onExport, Modifier.size(42.dp))
                SkyCircleIconButton(
                    Icons.Rounded.Settings, "设置", onSettings, Modifier.size(42.dp), enabled = settingsEnabled,
                )
            }
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { OverviewCard(state.overview) }
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp).selectableGroup(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        HistoryFilter.entries.forEach { option ->
                            SkyChoicePill(option.label, filter == option, { onFilter(option) }, Modifier.weight(1f))
                        }
                    }
                }
                when {
                    state.loadError -> item { HistoryPrompt("暂时无法读取练习记录", "重新加载", onReload) }
                    groups == null -> item {
                        Box(Modifier.fillMaxWidth().padding(22.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = PrimarySky)
                        }
                    }
                    state.sessions?.isEmpty() == true -> item {
                        HistoryPrompt("还没有练习记录，去完成第一组速算吧", "前往速算", onHome)
                    }
                    groups.isEmpty() -> item {
                        HistoryPrompt("还没有${filter.label}的练习记录", "查看全部") { onFilter(HistoryFilter.ALL) }
                    }
                    else -> groups.forEach { group ->
                        item(key = group.date.toString()) {
                            Text(
                                group.title,
                                Modifier.padding(start = 4.dp, top = 6.dp).semantics { heading() },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        items(group.sessions, key = { it.id }) { session ->
                            HistorySessionCard(
                                session, state.clock.zone, { onOpenSession(session.id) }, { onDelete(session.id) },
                            )
                        }
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun OverviewCard(overview: HistoryOverview) {
    SkyCard(Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(horizontal = 4.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OverviewMetric(overview.count, "累计题数", TextPrimary, Modifier.weight(1f))
            VerticalDivider(Modifier.fillMaxHeight().padding(vertical = 5.dp), color = BorderSky)
            OverviewMetric(overview.accuracy, "综合正确率", PrimarySkyText, Modifier.weight(1f))
            VerticalDivider(Modifier.fillMaxHeight().padding(vertical = 5.dp), color = BorderSky)
            OverviewMetric(overview.average, "平均单题", TextPrimary, Modifier.weight(1f))
        }
    }
}

@Composable
private fun OverviewMetric(value: String, label: String, tint: Color, modifier: Modifier) {
    Column(
        modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium.TabularTextStyle,
            color = tint,
            textAlign = TextAlign.Center,
        )
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary, textAlign = TextAlign.Center)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistorySessionCard(
    session: PracticeSessionEntity,
    zone: ZoneId,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    SkyCard(Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
        Row(
            Modifier.fillMaxWidth().combinedClickable(
                role = Role.Button,
                onClickLabel = "查看练习报告",
                onLongClickLabel = "删除练习记录",
                onLongClick = onDelete,
                onClick = onOpen,
            ).padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(44.dp).background(SurfaceIceBlue, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    MathModule.fromId(session.moduleId).operatorSymbol.ifBlank { "∑" },
                    color = PrimarySkyText,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(session.moduleTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(formatSessionMetadata(session, zone), color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    historyAccuracy(session.correctCount.toLong(), session.questionCount.toLong()),
                    style = MaterialTheme.typography.titleMedium.TabularTextStyle,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    formatPracticeTime(session.totalDurationMs),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall.TabularTextStyle,
                )
            }
        }
    }
}

@Composable
private fun HistoryPrompt(text: String, label: String, onClick: () -> Unit) {
    SkyCard(Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center)
            SkyPrimaryButton(label, onClick)
        }
    }
}
