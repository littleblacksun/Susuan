package com.xingce.speedmath.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xingce.speedmath.domain.model.GradeStatus
import com.xingce.speedmath.ui.theme.StatusError
import com.xingce.speedmath.ui.theme.StatusErrorBg
import com.xingce.speedmath.ui.theme.StatusExact
import com.xingce.speedmath.ui.theme.StatusExactBg
import com.xingce.speedmath.ui.theme.StatusPass
import com.xingce.speedmath.ui.theme.StatusPassBg

@Composable
internal fun GradeBadge(status: GradeStatus, label: String, showIcon: Boolean = false) {
    val (ink, background) = when (status) {
        GradeStatus.EXACT -> StatusExact to StatusExactBg
        GradeStatus.PASS -> StatusPass to StatusPassBg
        GradeStatus.FAIL -> StatusError to StatusErrorBg
    }
    Row(
        Modifier.background(background, CircleShape).padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showIcon) {
            Icon(
                if (status == GradeStatus.FAIL) Icons.Rounded.Close else Icons.Rounded.Check,
                null, Modifier.size(16.dp), tint = ink,
            )
        }
        Text(label, color = ink, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
    }
}
