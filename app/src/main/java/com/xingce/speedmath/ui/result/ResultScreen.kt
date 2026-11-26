package com.xingce.speedmath.ui.result

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.withResumed
import com.xingce.speedmath.data.db.PracticeItemEntity
import com.xingce.speedmath.data.db.PracticeSessionDetails
import com.xingce.speedmath.domain.model.GradeStatus
import com.xingce.speedmath.domain.model.MathModule
import com.xingce.speedmath.ui.components.AccuracyRing
import com.xingce.speedmath.ui.components.GradeBadge
import com.xingce.speedmath.ui.components.SkyCard
import com.xingce.speedmath.ui.components.SkyCircleIconButton
import com.xingce.speedmath.ui.components.SkyPrimaryButton
import com.xingce.speedmath.ui.components.SkySecondaryButton
import com.xingce.speedmath.ui.home.PracticeLaunchConfig
import com.xingce.speedmath.ui.practice.exactDisplay
import com.xingce.speedmath.ui.practice.formatPracticeSeconds
import com.xingce.speedmath.ui.practice.formatPracticeTime
import com.xingce.speedmath.ui.theme.BorderSkySubtle
import com.xingce.speedmath.ui.theme.PrimarySky
import com.xingce.speedmath.ui.theme.PrimarySkyText
import com.xingce.speedmath.ui.theme.SurfaceSkyTint
import com.xingce.speedmath.ui.theme.TabularTextStyle
import com.xingce.speedmath.ui.theme.TextPrimary
import com.xingce.speedmath.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.roundToInt

@Composable
internal fun ResultRoute(
    viewModel: ResultViewModel,
    fromHistory: Boolean,
    onHome: () -> Unit,
    onRetry: (Long) -> Unit,
    onNewPractice: (PracticeLaunchConfig) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val newPractice by rememberUpdatedState(onNewPractice)
    val home by rememberUpdatedState(onHome)
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.newPractice.collect { newPractice(it) }
        }
    }
    LaunchedEffect(lifecycle, state.missing) {
        if (state.missing) lifecycle.withResumed { home() }
    }
    LaunchedEffect(state.preferencesError) {
        if (state.preferencesError) {
            snackbar.showSnackbar("暂时无法读取练习偏好，请重试")
            viewModel.dismissPreferencesError()
        }
    }

    val details = state.details
    if (details != null) {
        ResultScreen(details, state.launching, fromHistory, onHome, { onRetry(viewModel.sessionId) }, viewModel::playAgain, snackbar)
    } else {
        Box(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.Center,
        ) {
            if (state.readError) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("暂时无法读取练习报告", color = TextSecondary)
                    SkyPrimaryButton("重新加载", viewModel::load)
                    SkySecondaryButton(if (fromHistory) "返回统计与记录" else "返回首页", onHome)
                }
            } else {
                CircularProgressIndicator(color = PrimarySky)
            }
        }
    }
}

@Composable
private fun ResultScreen(
    details: PracticeSessionDetails,
    launching: Boolean,
    fromHistory: Boolean,
    onHome: () -> Unit,
    onRetry: () -> Unit,
    onAgain: () -> Unit,
    snackbar: SnackbarHostState,
) {
    val session = details.session
    val hasFailures = details.items.any { it.gradeStatus == GradeStatus.FAIL.name }
    val module = MathModule.fromId(session.moduleId)
    Column(
        Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
    ) {
        ResultHeader(session.moduleTitle, fromHistory, onHome)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { ResultSummary(details) }
                items(details.items, key = { it.id }) { item ->
                    ResultItem(item, module, session.isEstimation)
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            SkySecondaryButton(
                if (hasFailures) "错题重练" else "全部答对",
                onRetry, Modifier.weight(1f).heightIn(min = 50.dp), enabled = hasFailures && !launching,
            )
            SkyPrimaryButton("再来一组", onAgain, Modifier.weight(1f).heightIn(min = 50.dp), enabled = !launching)
        }
    }
}

@Composable
private fun ResultHeader(moduleTitle: String, fromHistory: Boolean, onHome: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            SkyCircleIconButton(
                if (fromHistory) Icons.AutoMirrored.Rounded.ArrowBack else Icons.Rounded.Home,
                if (fromHistory) "返回统计与记录" else "返回首页",
                onHome,
                Modifier.height(40.dp).width(40.dp),
            )
        }
        Text(
            "练习报告", Modifier.semantics { heading() }.padding(horizontal = 4.dp),
            style = MaterialTheme.typography.headlineSmall, color = TextPrimary,
        )
        Text(
            moduleTitle, Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelMedium, color = TextSecondary, textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun ResultSummary(details: PracticeSessionDetails) {
    val session = details.session
    val count = session.questionCount
    val correct = if (count > 0) (session.correctCount * 100.0 / count).roundToInt() else 0
    val answerTime = if (details.items.isEmpty()) session.totalDurationMs else details.items.sumOf { it.durationMs }
    val error = when {
        !session.isEstimation -> "精确匹配"
        session.averageErrorPercent == null -> "—"
        else -> "±" + String.format(Locale.ROOT, "%.1f%%", session.averageErrorPercent)
    }

    SkyCard(Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(0.45f), contentAlignment = Alignment.Center) { AccuracyRing(correct) }
            Box(Modifier.height(106.dp).width(1.dp).background(BorderSkySubtle))
            Column(Modifier.weight(0.55f)) {
                ResultMetric("总用时", formatPracticeTime(session.totalDurationMs))
                HorizontalDivider(color = BorderSkySubtle)
                ResultMetric("均速", "${formatPracticeSeconds(answerTime, count)}秒/题")
                HorizontalDivider(color = BorderSkySubtle)
                ResultMetric("平均误差", error)
            }
        }
    }
}

@Composable
private fun ResultMetric(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 34.dp).padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f).padding(end = 3.dp), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.titleSmall.TabularTextStyle, color = TextPrimary, textAlign = TextAlign.End)
    }
}

@Composable
private fun ResultItem(item: PracticeItemEntity, module: MathModule, isEstimation: Boolean) {
    val status = GradeStatus.entries.firstOrNull { it.name == item.gradeStatus } ?: GradeStatus.FAIL
    val error = item.signedErrorPercent?.takeIf { isEstimation && it.isFinite() }?.let {
        val sign = if (it > 0) "+" else ""
        sign + String.format(Locale.ROOT, "%.1f%%", it)
    }
    val tag = when (status) {
        GradeStatus.EXACT -> "精准"
        GradeStatus.PASS -> if (error != null) "通过 $error" else "通过"
        GradeStatus.FAIL -> when {
            item.userAnswer.isBlank() -> "未作答"
            error != null -> "偏差 $error"
            else -> "错误"
        }
    }
    val unit = if (item.isPercent) "%" else ""
    val userAnswer = item.userAnswer.trim().removeSuffix("%").ifBlank { "—" }
    val userWithUnit = if (userAnswer == "—") userAnswer else userAnswer + unit
    val exact = item.exactDisplay(module) + unit

    SkyCard(Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "题 ${item.questionIndex + 1}",
                    Modifier.background(SurfaceSkyTint, CircleShape).padding(horizontal = 10.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelMedium.TabularTextStyle, color = PrimarySkyText,
                )
                Text("${formatPracticeSeconds(item.durationMs)}s", style = MaterialTheme.typography.labelMedium.TabularTextStyle, color = TextSecondary)
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(item.expression, style = MaterialTheme.typography.titleMedium.TabularTextStyle, color = TextPrimary)
                    Text(
                        "你的 $userWithUnit · 精确 $exact", style = MaterialTheme.typography.bodySmall.TabularTextStyle,
                        color = TextSecondary,
                    )
                }
                GradeBadge(status, tag)
            }
        }
    }
}
