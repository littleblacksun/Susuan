package com.xingce.speedmath.ui.practice

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.withResumed
import com.xingce.speedmath.domain.engine.NBackPhase
import com.xingce.speedmath.domain.engine.NBackStepState
import com.xingce.speedmath.domain.model.KeypadOrder
import com.xingce.speedmath.ui.components.GradeBadge
import com.xingce.speedmath.ui.components.InkWidth
import com.xingce.speedmath.ui.components.MathKeypad
import com.xingce.speedmath.ui.components.ScratchpadCanvas
import com.xingce.speedmath.ui.components.ScratchpadState
import com.xingce.speedmath.ui.components.SkyCard
import com.xingce.speedmath.ui.components.SkyCircleIconButton
import com.xingce.speedmath.ui.components.SkyPrimaryButton
import com.xingce.speedmath.ui.components.SkySegmentedPill
import com.xingce.speedmath.ui.components.rememberScratchpadState
import com.xingce.speedmath.ui.theme.BorderSky
import com.xingce.speedmath.ui.theme.PrimarySky
import com.xingce.speedmath.ui.theme.PrimarySkyGradient
import com.xingce.speedmath.ui.theme.PrimarySkyText
import com.xingce.speedmath.ui.theme.StatusError
import com.xingce.speedmath.ui.theme.SurfaceIceBlue
import com.xingce.speedmath.ui.theme.SurfaceSkyTint
import com.xingce.speedmath.ui.theme.SurfaceWhite
import com.xingce.speedmath.ui.theme.TabularTextStyle
import com.xingce.speedmath.ui.theme.TextMuted
import com.xingce.speedmath.ui.theme.TextPrimary
import com.xingce.speedmath.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
internal fun PracticeRoute(viewModel: PracticeViewModel, onExit: () -> Unit, onCompleted: (Long) -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val complete by rememberUpdatedState(onCompleted)
    val snackbar = remember { SnackbarHostState() }

    LifecycleResumeEffect(viewModel) {
        viewModel.setForeground(true)
        onPauseOrDispose { viewModel.setForeground(false) }
    }
    LaunchedEffect(lifecycle, state.savedSessionId) {
        state.savedSessionId?.let { id -> lifecycle.withResumed { complete(id) } }
    }
    LaunchedEffect(state.preferenceError) {
        if (state.preferenceError) {
            snackbar.showSnackbar("暂时无法保存键盘偏好")
            viewModel.dismissPreferenceError()
        }
    }
    val requestExit = {
        if (state.step == null && state.savedSessionId == null) onExit() else viewModel.setExitConfirmation(true)
    }
    BackHandler(onBack = requestExit)

    val step = state.step
    if (step == null) {
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing), contentAlignment = Alignment.Center) {
            val error = state.loadError
            if (error != null) {
                Column(
                    Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(error, color = TextSecondary, textAlign = TextAlign.Center)
                    SkyPrimaryButton("返回", onExit)
                }
            } else {
                CircularProgressIndicator(color = PrimarySky)
            }
        }
    } else {
        PracticeScreen(state, step, viewModel, requestExit, snackbar)
    }

    if (state.confirmExit) {
        AlertDialog(
            onDismissRequest = { viewModel.setExitConfirmation(false) },
            containerColor = SurfaceWhite,
            title = { Text("退出本次练习？") },
            text = { Text("当前作答进度将不会保存。") },
            dismissButton = {
                TextButton(onClick = { viewModel.setExitConfirmation(false) }) { Text("继续练习") }
            },
            confirmButton = {
                TextButton(onClick = onExit) { Text("退出练习", color = StatusError) }
            },
        )
    }
    if (state.saveError) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = SurfaceWhite,
            title = { Text("练习记录保存失败") },
            text = { Text("当前作答还在，可以重试保存。") },
            dismissButton = { TextButton(onClick = onExit) { Text("退出练习", color = StatusError) } },
            confirmButton = { TextButton(onClick = viewModel::saveCompleted) { Text("重试保存") } },
        )
    }
}

@Composable
private fun PracticeScreen(
    state: PracticeUiState,
    step: NBackStepState,
    viewModel: PracticeViewModel,
    onBack: () -> Unit,
    snackbar: SnackbarHostState,
) {
    val scratchpad = rememberScratchpadState(step.stepIndex)
    var inkWidth by rememberSaveable { mutableStateOf(InkWidth.FINE) }

    BoxWithConstraints(
        Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
    ) {
        val compact = maxHeight < 650.dp
        val keyHeight = (maxHeight * 0.074f).coerceIn(40.dp, if (state.scratchpadOpen) 52.dp else 56.dp)
        Column(Modifier.fillMaxSize()) {
            PracticeHeader(state, step, onBack)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (state.scratchpadOpen) {
                    ExpandedPractice(
                        state, step, scratchpad, inkWidth,
                        onWidthChange = { inkWidth = inkWidth.next() },
                        onCollapse = { viewModel.setScratchpadOpen(false) },
                        onOrderChange = viewModel::setKeypadOrder,
                        compact = compact,
                    )
                } else {
                    StandardPractice(
                        state, step, compact,
                        onExpand = { viewModel.setScratchpadOpen(true) },
                        onOrderChange = viewModel::setKeypadOrder,
                        onNext = viewModel::advance,
                    )
                }
                SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
            }
            MathKeypad(
                digits = state.digits,
                nextLabel = if (state.saving) "保存中" else state.actionLabel,
                rowHeight = keyHeight,
                inputEnabled = state.canType,
                nextEnabled = !state.saving && !state.saveError && !state.confirmExit && state.savedSessionId == null,
                hapticEnabled = state.hapticEnabled,
                onKey = viewModel::type,
                onNext = viewModel::advance,
            )
        }
    }
}

@Composable
private fun PracticeHeader(state: PracticeUiState, step: NBackStepState, onBack: () -> Unit) {
    val fraction = (step.stepIndex + 1).toFloat() / step.totalSteps
    val progress by animateFloatAsState(fraction, label = "practice progress")
    Column(
        Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SkyCircleIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "返回", onBack, Modifier.size(38.dp))
            Row(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    state.title, Modifier.weight(1f, fill = false), style = MaterialTheme.typography.titleMedium,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, color = TextPrimary,
                )
                Text(
                    state.tolerance.shortLabel,
                    Modifier.background(SurfaceSkyTint, CircleShape).padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelMedium.TabularTextStyle, color = PrimarySkyText,
                    maxLines = 1,
                )
            }
            Text(
                formatPracticeTime(state.elapsedMs), style = MaterialTheme.typography.labelLarge.TabularTextStyle,
                color = TextPrimary,
            )
            Box(Modifier.height(15.dp).width(1.dp).background(BorderSky))
            Text(
                "${step.stepIndex + 1}/${step.totalSteps}",
                style = MaterialTheme.typography.labelLarge.TabularTextStyle, color = TextSecondary,
            )
        }
        Box(
            Modifier.fillMaxWidth().height(if (state.scratchpadOpen) 10.dp else 20.dp)
                .clip(CircleShape).background(SurfaceSkyTint)
                .semantics { progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f) },
        ) {
            Box(
                Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight()
                    .clip(CircleShape).background(PrimarySkyGradient),
                contentAlignment = Alignment.Center,
            ) {
                if (!state.scratchpadOpen && progress >= 0.17f) {
                    Text(
                        "${(fraction * 100).roundToInt()}%", color = SurfaceWhite,
                        style = MaterialTheme.typography.labelSmall.TabularTextStyle,
                    )
                }
            }
        }
        step.bannerText?.let { banner ->
            Text(
                banner,
                Modifier.fillMaxWidth().background(SurfaceIceBlue, RoundedCornerShape(10.dp))
                    .padding(horizontal = 9.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelSmall, color = PrimarySkyText, textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun StandardPractice(
    state: PracticeUiState,
    step: NBackStepState,
    compact: Boolean,
    onExpand: () -> Unit,
    onOrderChange: (KeypadOrder) -> Unit,
    onNext: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Column(
        Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = if (compact) 8.dp else 12.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 14.dp),
    ) {
        SkyCard(Modifier.weight(1f).fillMaxWidth(), cornerRadius = 22.dp) {
            Column(
                Modifier.align(Alignment.Center)
                    .fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = if (compact) 8.dp else 22.dp),
                verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    equation(step, inline = false),
                    style = MaterialTheme.typography.displaySmall.TabularTextStyle,
                    fontSize = if (compact) 24.sp else 36.sp,
                    lineHeight = if (compact) 29.sp else 42.sp,
                    color = TextPrimary, textAlign = TextAlign.Center,
                )
                if (step.phase == NBackPhase.MEMORIZE) {
                    SkyPrimaryButton(
                        "记住了，继续 ›",
                        {
                            if (state.hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onNext()
                        },
                        Modifier.fillMaxWidth(0.85f),
                    )
                } else {
                    AnswerDisplay(state, step, Modifier.fillMaxWidth(0.86f), compact, inline = false)
                    Box(
                        Modifier.heightIn(min = if (compact) 24.dp else 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        state.feedback?.let { GradeBadge(it.status, it.feedbackText, showIcon = true) }
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.weight(0.42f).clip(CircleShape).background(SurfaceIceBlue)
                    .clickable(role = Role.Button, onClick = onExpand)
                    .heightIn(min = if (compact) 36.dp else 42.dp)
                    .padding(horizontal = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Edit, null, Modifier.size(18.dp), tint = TextSecondary)
                Text("草稿板 · 关", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            }
            SkySegmentedPill(
                labels = KeypadOrder.entries.map(KeypadOrder::label),
                selectedIndex = state.keypadOrder.ordinal,
                onSelect = { onOrderChange(KeypadOrder.entries[it]) },
                modifier = Modifier.weight(0.58f),
                segmentMinHeight = if (compact) 29.dp else 34.dp,
            )
        }
    }
}

@Composable
private fun ExpandedPractice(
    state: PracticeUiState,
    step: NBackStepState,
    scratchpad: ScratchpadState,
    inkWidth: InkWidth,
    onWidthChange: () -> Unit,
    onCollapse: () -> Unit,
    onOrderChange: (KeypadOrder) -> Unit,
    compact: Boolean,
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = if (compact) 7.dp else 12.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 12.dp),
    ) {
        SkyCard(Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
            val memorize = step.phase == NBackPhase.MEMORIZE
            Row(
                Modifier.fillMaxWidth().heightIn(min = if (compact) 56.dp else 76.dp)
                    .padding(horizontal = 12.dp, vertical = if (compact) 5.dp else 10.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    equation(step, inline = !memorize),
                    Modifier.weight(0.56f),
                    style = MaterialTheme.typography.headlineSmall.TabularTextStyle,
                    fontSize = if (compact) 18.sp else 25.sp,
                    lineHeight = if (compact) 23.sp else 29.sp,
                    color = TextPrimary,
                    textAlign = if (memorize) TextAlign.Center else TextAlign.Start,
                )
                if (!memorize) {
                    AnswerDisplay(state, step, Modifier.weight(0.44f), compact, inline = true)
                }
            }
        }
        state.feedback?.let { GradeBadge(it.status, it.feedbackText, showIcon = true) }
        ScratchpadCanvas(
            state = scratchpad, width = inkWidth, onWidthChange = onWidthChange, onCollapse = onCollapse,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            ScratchKeypadMenu(state.keypadOrder, onOrderChange)
        }
    }
}

@Composable
private fun ScratchKeypadMenu(order: KeypadOrder, onOrderChange: (KeypadOrder) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(
        Modifier.clip(CircleShape).background(SurfaceIceBlue.copy(alpha = 0.94f))
            .clickable(role = Role.Button) { open = true }
            .padding(start = 8.dp, end = 4.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("${order.label}键盘", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Icon(Icons.Rounded.ArrowDropDown, null, Modifier.size(16.dp), tint = TextSecondary)
    }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        KeypadOrder.entries.forEach { choice ->
            DropdownMenuItem(
                text = { Text(choice.label, color = if (choice == order) PrimarySkyText else TextPrimary) },
                onClick = {
                    open = false
                    onOrderChange(choice)
                },
            )
        }
    }
}

@Composable
private fun AnswerDisplay(
    state: PracticeUiState,
    step: NBackStepState,
    modifier: Modifier,
    compact: Boolean,
    inline: Boolean,
) {
    val isPercent = step.targetQuestion?.isPercentAnswer == true
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val shape = RoundedCornerShape(if (inline) 10.dp else 14.dp)
        BoxWithConstraints(
            Modifier.weight(1f).heightIn(min = when {
                inline && compact -> 39.dp
                inline -> 46.dp
                compact -> 52.dp
                else -> 80.dp
            })
                .background(SurfaceWhite, shape).border(2.dp, PrimarySky, shape)
                .padding(horizontal = 6.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (state.input.isEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!inline) Text("输入答案", color = TextMuted, style = MaterialTheme.typography.labelMedium)
                    if (state.canType) AnswerCaret(if (inline) 21 else 26)
                }
            } else {
                val targetSize = if (inline) 28 else if (compact) 34 else 46
                val availableSp = with(LocalDensity.current) { (maxWidth - 8.dp).toSp().value }
                val fontSize = minOf(targetSize.toFloat(), availableSp / (0.66f * state.input.length))
                    .coerceAtLeast(8f)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        state.input, fontSize = fontSize.sp, lineHeight = (fontSize + 4).sp,
                        style = MaterialTheme.typography.displaySmall.TabularTextStyle,
                        fontWeight = FontWeight.Bold,
                        color = if (inline) PrimarySkyText else TextPrimary, maxLines = 1,
                    )
                    if (state.canType) AnswerCaret((fontSize * 0.87f).roundToInt())
                }
            }
        }
        if (isPercent) {
            Text("%", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
        }
    }
}

@Composable
private fun AnswerCaret(height: Int) {
    val transition = rememberInfiniteTransition(label = "answer cursor")
    val alpha = transition.animateFloat(
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1000
                1f at 490
                0f at 500
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "cursor visibility",
    )
    Spacer(Modifier.width(2.dp).height(height.dp)
        .graphicsLayer { this.alpha = alpha.value }.background(PrimarySky, CircleShape))
}

private fun equation(step: NBackStepState, inline: Boolean): String {
    val question = step.displayQuestion
        ?: return "回忆第 ${(step.targetQuestion?.index ?: 0) + 1} 题答案"
    if ('?' in question.expression) return question.expression
    val suffix = if (inline) {
        if (question.isEstimation) " ≈" else " ="
    } else {
        " = ?"
    }
    val terms = question.terms
    return if (terms.size > 3) {
        val split = (terms.size + 1) / 2
        terms.take(split).joinToString(" ") + "\n" + terms.drop(split).joinToString(" ") + suffix
    } else {
        question.expression + suffix
    }
}
