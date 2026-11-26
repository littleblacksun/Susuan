package com.xingce.speedmath.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xingce.speedmath.ui.theme.BorderSky
import com.xingce.speedmath.ui.theme.PrimarySkyGradient
import com.xingce.speedmath.ui.theme.PrimarySkyText
import com.xingce.speedmath.ui.theme.SurfaceIceBlue
import com.xingce.speedmath.ui.theme.SurfaceWhite
import com.xingce.speedmath.ui.theme.TextMuted
import com.xingce.speedmath.ui.theme.TextPrimary

@Composable
fun SkyCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 18.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Surface(
        modifier = modifier.shadow(3.dp, shape, ambientColor = BorderSky, spotColor = BorderSky),
        shape = shape,
        color = SurfaceWhite,
        contentColor = TextPrimary,
        border = BorderStroke(1.dp, BorderSky),
    ) {
        Box(content = content)
    }
}

@Composable
fun SkyPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val background = if (enabled) {
        Modifier.background(PrimarySkyGradient)
    } else {
        Modifier.background(SurfaceIceBlue)
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .then(background)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .defaultMinSize(minWidth = 64.dp, minHeight = 48.dp)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (enabled) SurfaceWhite else TextMuted,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun SkySecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = SurfaceIceBlue,
            contentColor = TextPrimary,
            disabledContainerColor = SurfaceIceBlue,
            disabledContentColor = TextMuted,
        ),
    ) {
        Text(text = text, textAlign = TextAlign.Center)
    }
}

@Composable
fun SkyCircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tinted: Boolean = false,
    primary: Boolean = false,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .semantics { role = Role.Button }
            .shadow(2.dp, CircleShape, ambientColor = BorderSky, spotColor = BorderSky),
        enabled = enabled,
        shape = CircleShape,
        color = if (tinted) SurfaceIceBlue else SurfaceWhite,
        contentColor = if (primary) SurfaceWhite else if (enabled) TextPrimary else TextMuted,
        border = if (primary) null else BorderStroke(1.dp, BorderSky),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().then(
                if (primary) Modifier.background(PrimarySkyGradient) else Modifier,
            ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = contentDescription, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
internal fun SkyPageHeader(title: String, actions: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding()
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top))
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            Modifier.weight(1f).semantics { heading() },
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
        )
        actions()
    }
}

@Composable
internal fun SkyChoicePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = SurfaceWhite,
        border = if (selected) null else BorderStroke(1.dp, BorderSky),
        shadowElevation = if (compact) 0.dp else 1.dp,
    ) {
        Box(
            Modifier.then(if (selected) Modifier.background(PrimarySkyGradient) else Modifier)
                .selectable(selected, role = Role.RadioButton, onClick = onClick)
                .heightIn(min = if (compact) 30.dp else 34.dp)
                .padding(horizontal = if (compact) 10.dp else 14.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) SurfaceWhite else TextPrimary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun SkySegmentedPill(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    segmentMinHeight: Dp = 48.dp,
) {
    if (labels.isEmpty()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(SurfaceIceBlue)
            .selectableGroup()
            .padding(4.dp),
    ) {
        labels.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val color by animateColorAsState(if (selected) SurfaceWhite else Color.Transparent, label = "segment")
            val elevation by animateDpAsState(if (selected) 2.dp else 0.dp, label = "segment shadow")
            Box(
                modifier = Modifier
                    .weight(1f)
                    .shadow(elevation, CircleShape, ambientColor = BorderSky, spotColor = BorderSky)
                    .clip(CircleShape)
                    .background(color)
                    .selectable(
                        selected = selected,
                        enabled = enabled,
                        role = Role.Tab,
                        onClick = { onSelect(index) },
                    )
                    .heightIn(min = segmentMinHeight)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = when {
                        !enabled -> TextMuted
                        selected -> PrimarySkyText
                        else -> TextPrimary
                    },
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
