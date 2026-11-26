package com.xingce.speedmath.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xingce.speedmath.ui.theme.BorderSky
import com.xingce.speedmath.ui.theme.SurfaceSelectedCard
import com.xingce.speedmath.ui.theme.SurfaceWhite
import com.xingce.speedmath.ui.theme.TextMuted
import com.xingce.speedmath.ui.theme.TextPrimary

internal enum class InkWidth(val label: String, val width: Dp) {
    FINE("细", 2.5.dp), MEDIUM("中", 4.dp), BOLD("粗", 6.dp);

    fun next(): InkWidth = entries[(ordinal + 1) % entries.size]
}

internal class ScratchpadState {
    val strokes = mutableStateListOf<ScratchStroke>()
}

@Composable
internal fun rememberScratchpadState(clearTriggerKey: Any): ScratchpadState =
    remember(clearTriggerKey) { ScratchpadState() }

internal class ScratchStroke(val points: List<Offset>, val width: Dp, val path: Path)

// Extend one path as events arrive. Committed ink can be redrawn without rebuilding its geometry.
private class LiveInk {
    var revision by mutableIntStateOf(0)
        private set
    val points = mutableListOf<Offset>()
    var path = Path()
        private set
    var width = InkWidth.FINE.width
        private set

    fun start(point: Offset, width: Dp) {
        this.width = width
        path = Path().apply { moveTo(point.x, point.y) }
        points.clear()
        points.add(point)
        revision++
    }

    fun append(point: Offset) {
        val previous = points.last()
        if (point == previous) return
        val midpoint = (previous + point) / 2f
        path.quadraticTo(previous.x, previous.y, midpoint.x, midpoint.y)
        points.add(point)
        revision++
    }

    fun finish(): ScratchStroke {
        val last = points.last()
        path.lineTo(last.x, last.y)
        return ScratchStroke(points.toList(), width, path)
    }

    fun clear() {
        points.clear()
        revision++
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun ScratchpadCanvas(
    state: ScratchpadState,
    width: InkWidth,
    onWidthChange: () -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
    controls: @Composable () -> Unit = {},
) {
    val shape = RoundedCornerShape(18.dp)
    val live = remember(state) { LiveInk() }
    val currentWidth by rememberUpdatedState(width)

    Box(modifier.clip(shape).background(SurfaceWhite).border(1.dp, BorderSky, shape)) {
        Spacer(
            Modifier.matchParentSize()
                .semantics { contentDescription = "蓝点阵手写草稿板" }
                .pointerInput(state) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        if (down.type != PointerType.Touch && down.type != PointerType.Stylus) {
                            return@awaitEachGesture
                        }
                        down.consume()
                        live.start(down.position, currentWidth.width)
                        try {
                            do {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (change.isConsumed) break
                                change.historical.forEach { live.append(it.position) }
                                live.append(change.position)
                                change.consume()
                                if (!change.pressed) state.strokes.add(live.finish())
                            } while (change.pressed)
                        } finally {
                            live.clear()
                        }
                    }
                }
                .drawWithCache {
                    val step = 18.dp.toPx()
                    val canvasSize = size
                    val dots = buildList {
                        var y = step
                        while (y < canvasSize.height) {
                            var x = step
                            while (x < canvasSize.width) {
                                add(Offset(x, y))
                                x += step
                            }
                            y += step
                        }
                    }
                    onDrawBehind {
                        drawPoints(dots, PointMode.Points, BorderSky, 2.8.dp.toPx(), StrokeCap.Round)
                        state.strokes.forEach { drawInk(it.path, it.points, it.width, liveStroke = false) }
                        // Reading the paint revision in the draw phase keeps input off the layout path.
                        if (live.revision != 0 && live.points.isNotEmpty()) {
                            drawInk(live.path, live.points, live.width, liveStroke = true)
                        }
                    }
                },
        )
        Row(
            Modifier.align(Alignment.TopEnd).padding(8.dp)
                .clip(CircleShape)
                .background(SurfaceSelectedCard.copy(alpha = 0.94f))
                .border(1.dp, BorderSky, CircleShape)
                .padding(horizontal = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            ScratchTool(Icons.AutoMirrored.Rounded.Undo, "撤销笔迹", state.strokes.isNotEmpty()) {
                state.strokes.removeLastOrNull()
            }
            ScratchTool(Icons.Rounded.DeleteOutline, "清空草稿", state.strokes.isNotEmpty()) {
                state.strokes.clear()
            }
            ScratchTool(Icons.Rounded.Edit, "笔触粗细 ${width.label}", onClick = onWidthChange)
            ScratchTool(Icons.Rounded.KeyboardArrowDown, "收起草稿板", onClick = onCollapse)
        }
        Box(Modifier.align(Alignment.BottomStart).padding(7.dp)) { controls() }
    }
}

private fun DrawScope.drawInk(path: Path, points: List<Offset>, width: Dp, liveStroke: Boolean) {
    val strokeWidth = width.toPx()
    if (points.size == 1) {
        drawCircle(TextPrimary, strokeWidth / 2, points[0])
    } else {
        drawPath(path, TextPrimary, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        if (liveStroke) {
            val last = points.last()
            val midpoint = (points[points.lastIndex - 1] + last) / 2f
            drawLine(TextPrimary, midpoint, last, strokeWidth, StrokeCap.Round)
        }
    }
}

@Composable
private fun ScratchTool(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        Modifier.size(37.dp).clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, label, Modifier.size(21.dp), tint = if (enabled) TextPrimary else TextMuted)
    }
}
