package com.xingce.speedmath.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.xingce.speedmath.ui.theme.BorderSkySubtle
import com.xingce.speedmath.ui.theme.PrimarySkyDeep
import com.xingce.speedmath.ui.theme.SurfaceWhite
import com.xingce.speedmath.ui.theme.TextMuted

enum class PrimaryTab(val route: String, val label: String, val icon: ImageVector) {
    HOME("home", "速算", Icons.Rounded.Calculate),
    FORMULAS("formulas", "公式", Icons.AutoMirrored.Rounded.MenuBook),
    HISTORY("history", "统计", Icons.Rounded.BarChart),
}

@Composable
fun BottomNavBar(selectedTab: PrimaryTab, onSelect: (PrimaryTab) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().background(SurfaceWhite)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(BorderSkySubtle))
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .selectableGroup(),
        ) {
            PrimaryTab.entries.forEach { tab ->
                val selected = tab == selectedTab
                val tint = if (selected) PrimarySkyDeep else TextMuted
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 54.dp)
                        .selectable(
                            selected = selected,
                            role = Role.Tab,
                            onClick = { onSelect(tab) },
                        )
                        .padding(vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(tab.icon, contentDescription = null, Modifier.size(25.dp), tint = tint)
                    Text(tab.label, style = MaterialTheme.typography.labelSmall, color = tint)
                }
            }
        }
    }
}
