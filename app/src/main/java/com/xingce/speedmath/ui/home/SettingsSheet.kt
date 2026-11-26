package com.xingce.speedmath.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.xingce.speedmath.data.prefs.UserPreferences
import com.xingce.speedmath.domain.model.NBackLevel
import com.xingce.speedmath.ui.components.SkyCard
import com.xingce.speedmath.ui.components.SkySegmentedPill
import com.xingce.speedmath.ui.theme.BorderSky
import com.xingce.speedmath.ui.theme.PrimarySkyText
import com.xingce.speedmath.ui.theme.StatusError
import com.xingce.speedmath.ui.theme.SurfaceIceBlue
import com.xingce.speedmath.ui.theme.SurfaceWhite
import com.xingce.speedmath.ui.theme.TextMuted
import com.xingce.speedmath.ui.theme.TextPrimary
import com.xingce.speedmath.ui.theme.TextSecondary

@Composable
internal fun SettingsSheet(
    preferences: UserPreferences,
    onDismiss: () -> Unit,
    onNBackChange: (NBackLevel) -> Unit,
    onHapticChange: (Boolean) -> Unit,
    onStylusChange: (Boolean) -> Unit,
    onClearRecords: () -> Unit,
) {
    var confirmClear by rememberSaveable { mutableStateOf(false) }

    HomeBottomSheet("偏好设置", onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("N-back 工作记忆训练", style = MaterialTheme.typography.titleSmall)
            SkySegmentedPill(
                labels = NBackLevel.entries.map(NBackLevel::label),
                selectedIndex = preferences.nBackLevel.ordinal,
                onSelect = { onNBackChange(NBackLevel.entries[it]) },
            )
            Text(
                "看当前新题、答前 N 题答案，训练行测考场抗干扰与短时记忆（前 N 题为记忆期，末尾自动补答）。",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("练习体验", style = MaterialTheme.typography.titleSmall)
            Column(
                Modifier.fillMaxWidth()
                    .background(SurfaceIceBlue, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                HomeSwitchRow("按键震动反馈", preferences.hapticEnabled, onHapticChange)
                HomeSwitchRow(
                    label = "默认开启触控笔草稿板",
                    checked = preferences.stylusEnabled,
                    onCheckedChange = onStylusChange,
                    supportingText = "开启后可在练习中用手指或触控笔演算。",
                )
            }
        }

        SkyCard(Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("误差标准说明", style = MaterialTheme.typography.titleSmall)
                Text(
                    "精算 ±1%（截三位直除）\n实战 ±2%（默认推荐）\n宽松 ±5%（截两位快估）\n精确题型按 0 误差匹配。",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Column {
            Text("数据管理", style = MaterialTheme.typography.titleSmall)
            TextButton(onClick = { confirmClear = true }) {
                Text("清空全部练习记录", color = StatusError)
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            containerColor = SurfaceWhite,
            title = { Text("清空全部练习记录？") },
            text = { Text("删除后，所有练习会话及逐题记录将无法恢复。") },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("取消", color = PrimarySkyText) }
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    onClearRecords()
                }) { Text("确认清空", color = StatusError) }
            },
        )
    }
}

@Composable
internal fun HomeSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    supportingText: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                label,
                color = if (enabled) TextPrimary else TextMuted,
                style = MaterialTheme.typography.labelLarge,
            )
            supportingText?.let {
                Text(it, color = if (enabled) TextSecondary else TextMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = SurfaceWhite,
                uncheckedBorderColor = BorderSky,
                disabledUncheckedThumbColor = TextMuted.copy(alpha = 0.45f),
                disabledUncheckedTrackColor = SurfaceWhite.copy(alpha = 0.55f),
                disabledUncheckedBorderColor = BorderSky,
            ),
        )
    }
}
