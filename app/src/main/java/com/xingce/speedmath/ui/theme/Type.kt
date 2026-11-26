package com.xingce.speedmath.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun skyText(size: Int, lineHeight: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        letterSpacing = 0.sp,
    )

val SpeedMathTypography = Typography(
    displayLarge = skyText(48, 58, FontWeight.Bold),
    displayMedium = skyText(40, 50, FontWeight.Bold),
    displaySmall = skyText(34, 44, FontWeight.Bold),
    headlineLarge = skyText(32, 42, FontWeight.Bold),
    headlineMedium = skyText(28, 38, FontWeight.Bold),
    headlineSmall = skyText(24, 34, FontWeight.Bold),
    titleLarge = skyText(22, 30, FontWeight.Bold),
    titleMedium = skyText(17, 25, FontWeight.SemiBold),
    titleSmall = skyText(15, 22, FontWeight.SemiBold),
    bodyLarge = skyText(17, 26),
    bodyMedium = skyText(15, 23),
    bodySmall = skyText(13, 20),
    labelLarge = skyText(15, 22, FontWeight.SemiBold),
    labelMedium = skyText(13, 20, FontWeight.Medium),
    labelSmall = skyText(11, 17, FontWeight.Medium),
)

val TextStyle.TabularTextStyle: TextStyle
    get() = copy(fontFeatureSettings = "tnum")
