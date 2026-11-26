package com.xingce.speedmath.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

data class Question(
    val index: Int,
    val module: MathModule,
    val expression: String,
    val exactValue: BigDecimal,
    val exactDisplay: String,
    val isEstimation: Boolean,
    val isPercentAnswer: Boolean,
    val tolerance: ToleranceLevel,
    val terms: List<String> = emptyList(),
)

private val decimalDisplayModules = setOf(
    MathModule.FIVE_DIV_THREE, MathModule.THREE_DIV_FOUR, MathModule.THREE_DIV_TWO,
)

internal fun questionExactDisplay(module: MathModule, value: BigDecimal): String {
    val stripped = value.stripTrailingZeros()
    return if (module in decimalDisplayModules || stripped.scale() > 0) {
        value.setScale(1, RoundingMode.HALF_UP).toPlainString()
    } else {
        stripped.toPlainString()
    }
}
