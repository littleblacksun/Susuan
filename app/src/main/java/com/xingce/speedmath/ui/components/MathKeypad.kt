package com.xingce.speedmath.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xingce.speedmath.ui.practice.KeypadAction
import com.xingce.speedmath.ui.theme.BorderSky
import com.xingce.speedmath.ui.theme.PrimarySkyGradient
import com.xingce.speedmath.ui.theme.SurfaceIceBlue
import com.xingce.speedmath.ui.theme.SurfaceSkyTint
import com.xingce.speedmath.ui.theme.SurfaceWhite
import com.xingce.speedmath.ui.theme.TabularTextStyle
import com.xingce.speedmath.ui.theme.TextMuted
import com.xingce.speedmath.ui.theme.TextPrimary

@Composable
internal fun MathKeypad(
    digits: List<Int>,
    nextLabel: String,
    rowHeight: Dp,
    inputEnabled: Boolean,
    nextEnabled: Boolean,
    hapticEnabled: Boolean,
    onKey: (KeypadAction) -> Unit,
    onNext: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val gap = 8.dp
    val radius = RoundedCornerShape(12.dp)
    fun press(action: KeypadAction) {
        if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onKey(action)
    }

    Row(
        modifier = Modifier.fillMaxWidth()
            .background(SurfaceIceBlue, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .navigationBarsPadding()
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(gap),
    ) {
        repeat(3) { column ->
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(gap)) {
                repeat(3) { row ->
                    val digit = digits[row * 3 + column]
                    Keycap(rowHeight, SurfaceWhite, inputEnabled, { press(KeypadAction.Digit(digit)) }) {
                        KeyLabel(digit.toString(), inputEnabled)
                    }
                }
                val (label, action) = when (column) {
                    0 -> "." to KeypadAction.Decimal
                    1 -> "0" to KeypadAction.Digit(0)
                    else -> "−" to KeypadAction.ToggleMinus
                }
                Keycap(rowHeight, SurfaceWhite, inputEnabled, { press(action) }) {
                    KeyLabel(label, inputEnabled)
                }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(gap)) {
            Keycap(rowHeight, SurfaceSkyTint, inputEnabled, { press(KeypadAction.Backspace) }) {
                Icon(
                    Icons.AutoMirrored.Rounded.Backspace, contentDescription = "退格",
                    Modifier.size(27.dp), tint = if (inputEnabled) TextPrimary else TextMuted,
                )
            }
            Keycap(rowHeight, SurfaceSkyTint, inputEnabled, { press(KeypadAction.Clear) }) {
                KeyLabel("C", inputEnabled)
            }
            Box(
                modifier = Modifier.fillMaxWidth().height(rowHeight * 2 + gap)
                    .clip(radius)
                    .then(
                        if (nextEnabled) Modifier.background(PrimarySkyGradient)
                        else Modifier.background(SurfaceSkyTint),
                    )
                    .clickable(enabled = nextEnabled, role = Role.Button) {
                        if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNext()
                    }
                    .padding(horizontal = 3.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    nextLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (nextEnabled) SurfaceWhite else TextMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun Keycap(
    height: Dp,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier.fillMaxWidth().height(height)
            .shadow(3.dp, shape, ambientColor = BorderSky, spotColor = BorderSky)
            .clip(shape)
            .background(color)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun KeyLabel(text: String, enabled: Boolean) {
    Text(
        text, style = MaterialTheme.typography.titleLarge.TabularTextStyle,
        color = if (enabled) TextPrimary else TextMuted,
    )
}
