package com.xingce.speedmath.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.xingce.speedmath.domain.model.MathModule
import com.xingce.speedmath.domain.model.NBackLevel
import com.xingce.speedmath.domain.model.ToleranceLevel
import com.xingce.speedmath.ui.components.SkyCard
import com.xingce.speedmath.ui.components.SkyCircleIconButton
import com.xingce.speedmath.ui.components.SkyPrimaryButton
import com.xingce.speedmath.ui.export.ExportSheet
import com.xingce.speedmath.ui.export.ExportViewModel
import com.xingce.speedmath.ui.theme.BorderSky
import com.xingce.speedmath.ui.theme.PrimarySky
import com.xingce.speedmath.ui.theme.PrimarySkyDeep
import com.xingce.speedmath.ui.theme.PrimarySkyGradient
import com.xingce.speedmath.ui.theme.PrimarySkyLight
import com.xingce.speedmath.ui.theme.PrimarySkyText
import com.xingce.speedmath.ui.theme.SurfaceIceBlue
import com.xingce.speedmath.ui.theme.SurfaceSelectedCard
import com.xingce.speedmath.ui.theme.SurfaceSkyTint
import com.xingce.speedmath.ui.theme.SurfaceWhite
import com.xingce.speedmath.ui.theme.TabularTextStyle
import com.xingce.speedmath.ui.theme.TextPrimary
import com.xingce.speedmath.ui.theme.TextSecondary
import kotlinx.coroutines.launch

private val heroShape = RoundedCornerShape(22.dp)
private val heroGradient = Brush.linearGradient(listOf(PrimarySkyLight, PrimarySkyDeep))
private val moduleShape = RoundedCornerShape(16.dp)
private val customBackground = Color(0xFFF0F7FF)
private val customDash = Color(0xFF98C3F6)
private val questionCounts = listOf(10, 20, 30, 50)

@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    exportViewModel: ExportViewModel,
    onStartPractice: (PracticeLaunchConfig) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val hostScope = rememberCoroutineScope()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val navigate by rememberUpdatedState(onStartPractice)

    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is HomeEffect.StartPractice -> navigate(effect.config)
                    is HomeEffect.Message -> launch {
                        snackbar.currentSnackbarData?.dismiss()
                        snackbar.showSnackbar(effect.text)
                    }
                }
            }
        }
    }

    HomeScreen(state, viewModel, snackbar)
    val dismiss = { viewModel.showSheet(null) }
    when (state.sheet) {
        HomeSheet.COUNT -> HomeOptionsSheet(
            "选择题量", questionCounts, state.preferences.questionCount, { "${it}题" },
            viewModel::setQuestionCount, dismiss,
        )
        HomeSheet.TOLERANCE -> HomeOptionsSheet(
            "选择误差档位", ToleranceLevel.estimationOptions, state.tolerance, ToleranceLevel::label,
            viewModel::setTolerance, dismiss,
        )
        HomeSheet.CUSTOM -> CustomConfigSheet(
            state.preferences.customQuizConfig, dismiss, viewModel::saveCustomConfig,
        )
        HomeSheet.SETTINGS -> SettingsSheet(
            preferences = state.preferences,
            onDismiss = dismiss,
            onNBackChange = viewModel::setNBackLevel,
            onHapticChange = viewModel::setHapticEnabled,
            onStylusChange = viewModel::setStylusEnabled,
            onClearRecords = viewModel::clearPracticeRecords,
        )
        HomeSheet.EXPORT -> ExportSheet(
            viewModel = exportViewModel,
            initialModule = state.selectedModule,
            tolerance = state.preferences.tolerance,
            customConfig = state.preferences.customQuizConfig,
            snackbarHostState = snackbar,
            hostScope = hostScope,
            onDismiss = dismiss,
        )
        null -> Unit
    }
}

@Composable
private fun HomeScreen(state: HomeUiState, viewModel: HomeViewModel, snackbar: SnackbarHostState) {
    val settings = { viewModel.showSheet(HomeSheet.SETTINGS) }
    val moduleRows = remember(state.category) { (state.category.modules + MathModule.CUSTOM).chunked(2) }
    Column(
        Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
    ) {
        HomeHeader(state.preferencesLoaded, viewModel::showExport, settings)
        Box(Modifier.weight(1f)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                item(key = "today") {
                    TodayHero(
                        today = state.today,
                        lastModule = state.preferences.lastModule,
                        enabled = state.preferencesLoaded && !state.launching,
                        onContinue = { viewModel.startPractice(continueLast = true) },
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
                item(key = "categories") {
                    HomeCategories(state.category, viewModel::selectCategory)
                }
                items(moduleRows, key = { it.first().id }) { modules ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        modules.forEach { module ->
                            if (module == MathModule.CUSTOM) {
                                CustomModuleCard(
                                    subtitle = state.customSubtitle,
                                    selected = state.selectedModule == module,
                                    enabled = state.preferencesLoaded,
                                    onClick = { viewModel.showSheet(HomeSheet.CUSTOM) },
                                    modifier = Modifier.weight(1f),
                                )
                            } else {
                                ModuleCard(
                                    module = module,
                                    selected = state.selectedModule == module,
                                    onClick = { viewModel.selectModule(module) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
        PracticeConsole(state, viewModel, settings)
    }
}

@Composable
private fun HomeHeader(settingsEnabled: Boolean, onExport: () -> Unit, onSettings: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding()
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top))
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "行测速算",
            Modifier.weight(1f).semantics { heading() },
            style = MaterialTheme.typography.headlineSmall,
        )
        SkyCircleIconButton(Icons.Rounded.IosShare, "导出", onExport, Modifier.size(42.dp))
        SkyCircleIconButton(
            Icons.Rounded.Settings, "设置", onSettings, Modifier.size(42.dp), enabled = settingsEnabled,
        )
    }
}

@Composable
private fun TodayHero(
    today: TodayBannerModel,
    lastModule: MathModule,
    enabled: Boolean,
    onContinue: () -> Unit,
    modifier: Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth()
            .shadow(7.dp, heroShape, ambientColor = PrimarySkyLight, spotColor = PrimarySkyLight)
            .background(heroGradient, heroShape)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(0.64f)) {
            Text(
                "今日速算", color = SurfaceWhite.copy(alpha = 0.88f),
                style = MaterialTheme.typography.labelMedium,
            )
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    today.questionCount.toString(),
                    color = SurfaceWhite,
                    style = MaterialTheme.typography.displaySmall.TabularTextStyle,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 38.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Text(
                    "题", Modifier.padding(bottom = 3.dp), color = SurfaceWhite,
                    fontSize = 18.sp, fontWeight = FontWeight.Medium,
                )
            }
            Text(
                today.subtitle, color = SurfaceWhite,
                style = MaterialTheme.typography.bodySmall.TabularTextStyle,
            )
        }
        Column(
            modifier = Modifier.weight(0.36f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                Modifier.fillMaxWidth().heightIn(min = 36.dp).clip(CircleShape)
                    .background(SurfaceWhite.copy(alpha = 0.88f))
                    .clickable(enabled = enabled, role = Role.Button, onClick = onContinue)
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "继续上次 ›", color = PrimarySkyText, style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                )
            }
            Text(
                lastModule.title, color = SurfaceWhite.copy(alpha = 0.9f), fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun HomeCategories(selected: HomeCategoryFilter, onSelect: (HomeCategoryFilter) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val minPillWidth = ((maxWidth - 32.dp - 24.dp) / 4).coerceAtLeast(0.dp)
        LazyRow(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(HomeCategoryFilter.entries, key = { it.name }) { filter ->
                val active = selected == filter
                val background = if (active) {
                    Modifier.background(PrimarySkyGradient)
                } else {
                    Modifier.background(SurfaceWhite).border(1.dp, BorderSky, CircleShape)
                }
                Box(
                    modifier = Modifier
                        .widthIn(min = minPillWidth)
                        .heightIn(min = 34.dp)
                        .shadow(2.dp, CircleShape, ambientColor = BorderSky, spotColor = BorderSky)
                        .clip(CircleShape)
                        .then(background)
                        .selectable(active, role = Role.Tab, onClick = { onSelect(filter) })
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        filter.label, color = if (active) SurfaceWhite else TextPrimary,
                        style = MaterialTheme.typography.labelMedium, maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun ModuleCard(module: MathModule, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Box(
        modifier.heightIn(min = 98.dp)
            .shadow(3.dp, moduleShape, ambientColor = BorderSky, spotColor = BorderSky)
            .clip(moduleShape)
            .background(if (selected) SurfaceSelectedCard else SurfaceWhite)
            .border(if (selected) 2.dp else 1.dp, if (selected) PrimarySky else BorderSky, moduleShape)
            .selectable(selected, role = Role.RadioButton, onClick = onClick),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(
                module.title,
                modifier = Modifier.padding(end = if (selected) 19.dp else 0.dp),
                color = TextPrimary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                module.sampleExpression,
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall.TabularTextStyle,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (module.isEstimation) {
                Text(
                    "估", Modifier.padding(top = 7.dp)
                        .background(SurfaceSkyTint, RoundedCornerShape(7.dp))
                        .padding(horizontal = 8.dp, vertical = 1.dp),
                    color = PrimarySkyText,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        if (selected) SelectionCheck(Modifier.align(Alignment.TopEnd).padding(8.dp))
    }
}

@Composable
private fun CustomModuleCard(
    subtitle: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val border = if (selected) {
        Modifier.border(2.dp, PrimarySky, moduleShape)
    } else {
        Modifier.drawWithContent {
            drawContent()
            val strokeWidth = 1.dp.toPx()
            val inset = strokeWidth / 2
            drawRoundRect(
                color = customDash,
                topLeft = Offset(inset, inset),
                size = Size(size.width - strokeWidth, size.height - strokeWidth),
                cornerRadius = CornerRadius(16.dp.toPx() - inset),
                style = Stroke(
                    width = strokeWidth,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())),
                ),
            )
        }
    }

    Box(
        modifier = modifier.heightIn(min = 98.dp).clip(moduleShape)
            .background(if (selected) SurfaceSelectedCard else customBackground)
            .then(border)
            .selectable(selected, enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.CenterStart,
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(
                modifier = Modifier.padding(end = if (selected) 19.dp else 0.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Add, null, Modifier.size(22.dp), tint = PrimarySkyDeep)
                Text(
                    "自定义出题", color = TextPrimary, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
        }
        if (selected) SelectionCheck(Modifier.align(Alignment.TopEnd).padding(8.dp))
    }
}

@Composable
private fun SelectionCheck(modifier: Modifier) {
    Box(
        modifier.size(22.dp).background(PrimarySkyGradient, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Check, null, Modifier.size(16.dp), tint = SurfaceWhite)
    }
}

@Composable
private fun PracticeConsole(state: HomeUiState, viewModel: HomeViewModel, onSettings: () -> Unit) {
    val preferences = state.preferences
    val enabled = state.preferencesLoaded && !state.launching
    SkyCard(
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 12.dp),
        cornerRadius = 20.dp,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 14.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (preferences.nBackLevel != NBackLevel.OFF) {
                Text(
                    "已开启 ${preferences.nBackLevel.label} 训练模式 · 点击调整",
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(SurfaceIceBlue).clickable(role = Role.Button, onClick = onSettings)
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    color = PrimarySkyText, style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ParameterChip("${preferences.questionCount}题", enabled, Modifier.weight(1f), dropdown = true) {
                    viewModel.showSheet(HomeSheet.COUNT)
                }
                ParameterChip(
                    state.tolerance.shortLabel, enabled, Modifier.weight(1f),
                    dropdown = state.tolerance != ToleranceLevel.EXACT,
                    onClick = viewModel::showTolerance,
                )
                ParameterChip(
                    "${preferences.keypadOrder.label}键盘", enabled, Modifier.weight(1f),
                    onClick = viewModel::cycleKeypad,
                )
                ParameterChip(
                    if (preferences.stylusEnabled) "草稿板 开" else "草稿板 关", enabled, Modifier.weight(1f),
                    emphasized = preferences.stylusEnabled, onClick = viewModel::toggleStylus,
                )
            }
            SkyPrimaryButton(
                "开始练习", { viewModel.startPractice() },
                Modifier.fillMaxWidth().heightIn(min = 50.dp), enabled = enabled,
            )
        }
    }
}

@Composable
private fun ParameterChip(
    label: String,
    enabled: Boolean,
    modifier: Modifier,
    dropdown: Boolean = false,
    emphasized: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier.heightIn(min = 37.dp).clip(RoundedCornerShape(11.dp)).background(SurfaceIceBlue)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val tint = if (emphasized) PrimarySkyText else TextPrimary
        Text(
            label, Modifier.weight(1f, fill = false), color = tint,
            style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center,
        )
        if (dropdown) Icon(Icons.Rounded.ArrowDropDown, null, Modifier.size(15.dp), tint = tint)
    }
}
