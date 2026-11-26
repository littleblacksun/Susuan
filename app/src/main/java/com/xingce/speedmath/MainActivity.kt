package com.xingce.speedmath

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.xingce.speedmath.ui.navigation.AppNavGraph
import com.xingce.speedmath.ui.theme.BgSkyWhite
import com.xingce.speedmath.ui.theme.SpeedMathTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.Transparent.toArgb(), BgSkyWhite.toArgb()),
            navigationBarStyle = SystemBarStyle.light(Color.Transparent.toArgb(), BgSkyWhite.toArgb()),
        )
        val container = (application as SpeedMathApp).container
        setContent {
            SpeedMathTheme {
                AppNavGraph(container)
            }
        }
    }
}
