package com.xingce.speedmath.ui.home

import com.xingce.speedmath.data.db.StatsAggregate
import com.xingce.speedmath.data.prefs.UserPreferences
import com.xingce.speedmath.domain.model.CustomOperation
import com.xingce.speedmath.domain.model.CustomQuizConfig
import com.xingce.speedmath.domain.model.KeypadOrder
import com.xingce.speedmath.domain.model.MathModule
import com.xingce.speedmath.domain.model.ModuleCategory
import com.xingce.speedmath.domain.model.ToleranceLevel
import java.math.BigDecimal
import java.math.RoundingMode

enum class HomeCategoryFilter(val label: String, val category: ModuleCategory?) {
    DIVISION_ESTIMATION("除法估算", ModuleCategory.DIVISION_ESTIMATION),
    ADD_SUB("加减基础", ModuleCategory.ADD_SUB),
    MULTIPLICATION("乘法速算", ModuleCategory.MULTIPLICATION),
    ALL("全部(17)", null);

    val modules: List<MathModule> = category?.let(MathModule::byCategory) ?: MathModule.presets

    companion object {
        fun forModule(module: MathModule): HomeCategoryFilter =
            entries.firstOrNull { it.category != null && it.category == module.category }
                ?: DIVISION_ESTIMATION
    }
}

data class TodayBannerModel(
    val questionCount: Long,
    val accuracyPercent: Int,
    val averageSeconds: String,
) {
    val subtitle: String
        get() = if (questionCount == 0L) {
            "开始今天的第一组速算吧"
        } else {
            "正确率 ${accuracyPercent}% · 均速 ${averageSeconds}s"
        }
}

fun formatTodaySummary(stats: StatsAggregate): TodayBannerModel {
    val count = stats.questionCount
    if (count <= 0L) return TodayBannerModel(0, 0, "0.0")

    val accuracy = BigDecimal.valueOf(stats.correctCount)
        .movePointRight(2)
        .divide(BigDecimal.valueOf(count), 0, RoundingMode.HALF_UP)
    val averageSeconds = BigDecimal.valueOf(stats.totalDurationMs, 3)
        .divide(BigDecimal.valueOf(count), 1, RoundingMode.HALF_UP)
    return TodayBannerModel(count, accuracy.toInt(), averageSeconds.toPlainString())
}

fun effectiveToleranceFor(
    module: MathModule,
    userTolerance: ToleranceLevel,
    customConfig: CustomQuizConfig,
): ToleranceLevel = when {
    module == MathModule.CUSTOM -> customConfig.tolerance
    !module.isEstimation -> ToleranceLevel.EXACT
    userTolerance == ToleranceLevel.EXACT -> ToleranceLevel.STANDARD
    else -> userTolerance
}

internal val CustomOperation.supportsNegative: Boolean
    get() = this == CustomOperation.SUB || this == CustomOperation.MIXED_ADD_SUB

internal val CustomOperation.supportsPercent: Boolean
    get() = this == CustomOperation.DIV

internal fun CustomQuizConfig.normalizedForHome(): CustomQuizConfig = normalized().copy(
    allowNegative = allowNegative && operation.supportsNegative,
    percentAnswer = percentAnswer && operation.supportsPercent,
)

fun customConfigSummary(config: CustomQuizConfig): String {
    val custom = config.normalizedForHome()
    val symbol = when (custom.operation) {
        CustomOperation.ADD -> "+"
        CustomOperation.SUB -> "−"
        CustomOperation.MUL -> "×"
        CustomOperation.DIV -> "÷"
        CustomOperation.MIXED_ADD_SUB -> "±"
    }
    return buildString {
        append("${custom.operation.label} · ${custom.firstDigits}位$symbol${custom.secondDigits}位")
        append(" · ${custom.tolerance.shortLabel}")
        if (custom.allowNegative) append(" · 允许负数")
        if (custom.percentAnswer) append(" · 百分比作答")
    }
}

fun KeypadOrder.next(): KeypadOrder = when (this) {
    KeypadOrder.ASCENDING -> KeypadOrder.DESCENDING
    KeypadOrder.DESCENDING -> KeypadOrder.SHUFFLED
    KeypadOrder.SHUFFLED -> KeypadOrder.ASCENDING
}

internal fun UserPreferences.practiceLaunchConfig(module: MathModule): PracticeLaunchConfig {
    val custom = customQuizConfig.normalizedForHome()
    return PracticeLaunchConfig(
        module = module,
        count = questionCount,
        tolerance = effectiveToleranceFor(module, tolerance, custom),
        nBack = nBackLevel,
        keypad = keypadOrder,
        stylus = stylusEnabled,
        haptic = hapticEnabled,
        custom = custom.takeIf { module == MathModule.CUSTOM },
    )
}
