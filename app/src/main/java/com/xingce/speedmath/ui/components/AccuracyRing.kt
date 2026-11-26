package com.xingce.speedmath.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.xingce.speedmath.ui.theme.PrimarySky
import com.xingce.speedmath.ui.theme.SurfaceIceBlue
import com.xingce.speedmath.ui.theme.TabularTextStyle
import com.xingce.speedmath.ui.theme.TextPrimary
import com.xingce.speedmath.ui.theme.TextSecondary

@Composable
internal fun AccuracyRing(percent: Int) {
    val clamped = percent.coerceIn(0, 100)
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val sweep by animateFloatAsState(
        if (started) clamped * 3.6f else 0f,
        animationSpec = tween(650),
        label = "accuracy arc",
    )
    Box(Modifier.size(116.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val width = 12.dp.toPx()
            val radius = (size.minDimension - width) / 2
            drawCircle(SurfaceIceBlue, radius, style = Stroke(width))
            if (sweep >= 360f) {
                drawCircle(PrimarySky, radius, style = Stroke(width))
            } else if (sweep > 0f) {
                drawArc(
                    color = PrimarySky,
                    startAngle = -90f, sweepAngle = sweep, useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${clamped}%", style = MaterialTheme.typography.headlineMedium.TabularTextStyle, color = TextPrimary)
            Text("正确率", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        }
    }
}
