package com.xingce.speedmath.ui

import com.xingce.speedmath.data.db.StatsAggregate
import com.xingce.speedmath.domain.model.CustomOperation
import com.xingce.speedmath.domain.model.CustomQuizConfig
import com.xingce.speedmath.domain.model.KeypadOrder
import com.xingce.speedmath.domain.model.MathModule
import com.xingce.speedmath.domain.model.ToleranceLevel
import com.xingce.speedmath.ui.home.HomeCategoryFilter
import com.xingce.speedmath.ui.home.TodayBannerModel
import com.xingce.speedmath.ui.home.customConfigSummary
import com.xingce.speedmath.ui.home.effectiveToleranceFor
import com.xingce.speedmath.ui.home.formatTodaySummary
import com.xingce.speedmath.ui.home.next
import com.xingce.speedmath.ui.home.normalizedForHome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLogicTest {
    @Test
    fun emptyStatsShowFirstPracticePrompt() {
        val today = formatTodaySummary(StatsAggregate(0, 0, 0))

        assertEquals(TodayBannerModel(0, 0, "0.0"), today)
        assertEquals("开始今天的第一组速算吧", today.subtitle)
    }

    @Test
    fun todayStatsRoundAccuracyAndKeepOneDecimalForAverageSeconds() {
        val today = formatTodaySummary(StatsAggregate(30, 28, 185_500))

        assertEquals(30L, today.questionCount)
        assertEquals(93, today.accuracyPercent)
        assertEquals("6.2", today.averageSeconds)
        assertEquals("正确率 93% · 均速 6.2s", today.subtitle)
        assertEquals(
            TodayBannerModel(8, 63, "1.3"),
            formatTodaySummary(StatsAggregate(8, 5, 10_000)),
        )
    }

    @Test
    fun effectiveToleranceFollowsModuleAndFallsBackToStandardForEstimation() {
        val custom = CustomQuizConfig(tolerance = ToleranceLevel.STRICT)

        assertEquals(
            ToleranceLevel.EXACT,
            effectiveToleranceFor(MathModule.THREE_ADD, ToleranceLevel.RELAXED, custom),
        )
        assertEquals(
            ToleranceLevel.RELAXED,
            effectiveToleranceFor(MathModule.THREE_DIV_ONE, ToleranceLevel.RELAXED, custom),
        )
        assertEquals(
            ToleranceLevel.STANDARD,
            effectiveToleranceFor(MathModule.MUL_ESTIMATION, ToleranceLevel.EXACT, custom),
        )
        assertEquals(
            ToleranceLevel.STRICT,
            effectiveToleranceFor(MathModule.CUSTOM, ToleranceLevel.RELAXED, custom),
        )
        assertEquals(
            ToleranceLevel.EXACT,
            effectiveToleranceFor(
                MathModule.CUSTOM, ToleranceLevel.RELAXED, custom.copy(tolerance = ToleranceLevel.EXACT),
            ),
        )
    }

    @Test
    fun categoryFiltersIncludeEveryPresetAndExcludeTheCustomCard() {
        assertEquals(listOf(5, 7, 5, 17), HomeCategoryFilter.entries.map { it.modules.size })
        for (filter in HomeCategoryFilter.entries) {
            assertFalse(MathModule.CUSTOM in filter.modules)
        }
        assertEquals(
            MathModule.presets.toSet(),
            HomeCategoryFilter.entries.dropLast(1).flatMap { it.modules }.toSet(),
        )
    }

    @Test
    fun keypadOrderCyclesThroughAllThreeOrders() {
        assertEquals(KeypadOrder.DESCENDING, KeypadOrder.ASCENDING.next())
        assertEquals(KeypadOrder.SHUFFLED, KeypadOrder.DESCENDING.next())
        assertEquals(KeypadOrder.ASCENDING, KeypadOrder.SHUFFLED.next())
    }

    @Test
    fun customConfigSummaryReflectsValidOptionsOnly() {
        val division = CustomQuizConfig(firstDigits = 8, allowNegative = true, percentAnswer = true)
        val subtraction = division.copy(operation = CustomOperation.SUB)

        assertEquals("除法 · 5位÷3位 · ±2%", customConfigSummary(CustomQuizConfig()))
        assertEquals("除法 · 5位÷3位 · ±2% · 百分比作答", customConfigSummary(division))
        assertFalse(division.normalizedForHome().allowNegative)
        assertFalse(subtraction.normalizedForHome().percentAnswer)
        assertTrue(subtraction.normalizedForHome().allowNegative)
    }
}
