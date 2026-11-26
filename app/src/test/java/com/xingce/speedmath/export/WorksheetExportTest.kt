package com.xingce.speedmath.export

import com.xingce.speedmath.domain.engine.QuestionGenerator
import com.xingce.speedmath.domain.model.CustomOperation
import com.xingce.speedmath.domain.model.CustomQuizConfig
import com.xingce.speedmath.domain.model.MathModule
import com.xingce.speedmath.domain.model.Question
import com.xingce.speedmath.domain.model.ToleranceLevel
import java.math.BigDecimal
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorksheetExportTest {

    @Test
    fun formatQuestionLine_handlesStandardPercentMakeHundredAndCustom() {
        val division = Question(
            index = 0,
            module = MathModule.FIVE_DIV_THREE,
            expression = "54321 ÷ 213",
            exactValue = BigDecimal("255.02816901"),
            exactDisplay = "255.0",
            isEstimation = true,
            isPercentAnswer = false,
            tolerance = ToleranceLevel.STANDARD,
        )
        assertEquals("1. 54321 ÷ 213 = ______", formatQuestionLine(1, division))

        val percentDivision = Question(
            index = 1,
            module = MathModule.THREE_DIV_FOUR,
            expression = "534 ÷ 4218",
            exactValue = BigDecimal("12.66002845"),
            exactDisplay = "12.7",
            isEstimation = true,
            isPercentAnswer = true,
            tolerance = ToleranceLevel.STANDARD,
        )
        assertEquals("2. 534 ÷ 4218 = ______ %", formatQuestionLine(2, percentDivision))

        val makeHundred = Question(
            index = 2,
            module = MathModule.MAKE_HUNDRED,
            expression = "64 + ? = 100",
            exactValue = BigDecimal("36"),
            exactDisplay = "36",
            isEstimation = false,
            isPercentAnswer = false,
            tolerance = ToleranceLevel.EXACT,
        )
        assertEquals("3. 64 + (____) = 100", formatQuestionLine(3, makeHundred))

        val customExact = Question(
            index = 3,
            module = MathModule.CUSTOM,
            expression = "48 × 12",
            exactValue = BigDecimal("576"),
            exactDisplay = "576",
            isEstimation = false,
            isPercentAnswer = false,
            tolerance = ToleranceLevel.EXACT,
        )
        assertEquals("4. 48 × 12 = ______", formatQuestionLine(4, customExact))
    }

    @Test
    fun formatAnswerLine_handlesExactAndAllToleranceLevelsWithHalfUp() {
        val exactQuestion = Question(
            index = 0,
            module = MathModule.TWO_ADD_SUB,
            expression = "68 + 47",
            exactValue = BigDecimal("115"),
            exactDisplay = "115",
            isEstimation = false,
            isPercentAnswer = false,
            tolerance = ToleranceLevel.EXACT,
        )
        assertEquals("1. 115", formatAnswerLine(1, exactQuestion))

        val customExactPercent = Question(
            index = 1,
            module = MathModule.CUSTOM,
            expression = "25 ÷ 100",
            exactValue = BigDecimal("25"),
            exactDisplay = "25",
            isEstimation = false,
            isPercentAnswer = true,
            tolerance = ToleranceLevel.EXACT,
        )
        assertEquals("2. 25%", formatAnswerLine(2, customExactPercent))

        // ±1% STRICT: 205.0 * 0.01 = 2.05 -> low = 202.95 (HALF_UP -> 203.0), high = 207.05 (HALF_UP -> 207.1)
        val strictQuestion = Question(
            index = 2,
            module = MathModule.FIVE_DIV_THREE,
            expression = "41000 ÷ 200",
            exactValue = BigDecimal("205.0"),
            exactDisplay = "205.0",
            isEstimation = true,
            isPercentAnswer = false,
            tolerance = ToleranceLevel.STRICT,
        )
        assertEquals("3. 205.0（±1% 区间：203.0 ~ 207.1）", formatAnswerLine(3, strictQuestion))

        // ±2% STANDARD: 255.0 * 0.02 = 5.1 -> low = 249.9, high = 260.1
        val standardQuestion = Question(
            index = 3,
            module = MathModule.FIVE_DIV_THREE,
            expression = "54315 ÷ 213",
            exactValue = BigDecimal("255.0"),
            exactDisplay = "255.0",
            isEstimation = true,
            isPercentAnswer = false,
            tolerance = ToleranceLevel.STANDARD,
        )
        assertEquals("4. 255.0（±2% 区间：249.9 ~ 260.1）", formatAnswerLine(4, standardQuestion))

        // ±5% RELAXED: 100.0 * 0.05 = 5.0 -> low = 95.0, high = 105.0
        val relaxedQuestion = Question(
            index = 4,
            module = MathModule.THREE_DIV_TWO,
            expression = "4200 ÷ 42",
            exactValue = BigDecimal("100.0"),
            exactDisplay = "100.0",
            isEstimation = true,
            isPercentAnswer = false,
            tolerance = ToleranceLevel.RELAXED,
        )
        assertEquals("5. 100.0（±5% 区间：95.0 ~ 105.0）", formatAnswerLine(5, relaxedQuestion))

        // Percent estimation: 12.66002845 * 0.98 = 12.4068 -> 12.4%, * 1.02 = 12.9132 -> 12.9%
        val percentEstimation = Question(
            index = 5,
            module = MathModule.THREE_DIV_FOUR,
            expression = "534 ÷ 4218",
            exactValue = BigDecimal("12.66002845"),
            exactDisplay = "12.7",
            isEstimation = true,
            isPercentAnswer = true,
            tolerance = ToleranceLevel.STANDARD,
        )
        assertEquals("6. 12.7%（±2% 区间：12.4% ~ 12.9%）", formatAnswerLine(6, percentEstimation))
    }

    @Test
    fun textWorksheetFormatter_respectsIncludeAnswersFlag() {
        val fixedTime = ZonedDateTime.of(2026, 11, 26, 14, 30, 0, 0, ZoneId.of("Asia/Shanghai"))
        val baseRequest = ExportRequest(
            module = MathModule.FIVE_DIV_THREE,
            count = 10,
            tolerance = ToleranceLevel.STANDARD,
            customConfig = CustomQuizConfig(),
            format = ExportFormat.TEXT,
            includeAnswers = false,
        )

        val withoutAnswersDoc = WorksheetBuilder.build(baseRequest, QuestionGenerator(Random(42)), fixedTime)
        val withoutAnswersText = TextWorksheetFormatter.format(withoutAnswersDoc)
        assertTrue(withoutAnswersText.contains("行测速算 · 五位数除三位数 练习卷"))
        assertTrue(withoutAnswersText.contains("题量：10题 · 误差标准：实战 ±2% · 日期：2026-11-26"))
        assertTrue(withoutAnswersText.contains("生成时间：2026-11-26 14:30"))
        assertFalse(withoutAnswersText.contains("参考答案与误差区间"))

        val withAnswersDoc = WorksheetBuilder.build(
            baseRequest.copy(includeAnswers = true),
            QuestionGenerator(Random(42)),
            fixedTime,
        )
        val withAnswersText = TextWorksheetFormatter.format(withAnswersDoc)
        assertTrue(withAnswersText.contains("参考答案与误差区间"))
        assertTrue(withAnswersText.contains("±2% 区间："))
    }

    @Test
    fun worksheetBuilder_generatesExactCountsAndContinuousNumberingFor10_20_30_50() {
        val counts = listOf(10, 20, 30, 50)
        val fixedTime = ZonedDateTime.of(2026, 11, 26, 15, 0, 0, 0, ZoneId.of("Asia/Shanghai"))

        counts.forEach { count ->
            val request = ExportRequest(
                module = MathModule.THREE_DIV_FOUR,
                count = count,
                tolerance = ToleranceLevel.STRICT,
                customConfig = CustomQuizConfig(),
                format = ExportFormat.PDF,
                includeAnswers = true,
            )
            val doc = WorksheetBuilder.build(request, QuestionGenerator(Random(count)), fixedTime)
            assertEquals(count, doc.questions.size)

            val text = TextWorksheetFormatter.format(doc)
            val sections = text.split("参考答案与误差区间")
            assertEquals(2, sections.size)

            val questionLines = sections[0].lines().filter { it.matches(Regex("^\\d+\\. .*")) }
            val answerLines = sections[1].lines().filter { it.matches(Regex("^\\d+\\. .*")) }
            assertEquals(count, questionLines.size)
            assertEquals(count, answerLines.size)

            for (number in 1..count) {
                assertTrue(
                    "Missing question #$number for count=$count",
                    questionLines[number - 1].startsWith("$number. "),
                )
                assertTrue(
                    "Missing answer #$number for count=$count",
                    answerLines[number - 1].startsWith("$number. "),
                )
            }
        }
    }

    @Test
    fun worksheetBuilder_resolvesToleranceForExactEstimationAndCustomModules() {
        val customConfig = CustomQuizConfig(
            operation = CustomOperation.DIV,
            firstDigits = 4,
            secondDigits = 2,
            percentAnswer = true,
            tolerance = ToleranceLevel.RELAXED,
        )
        assertEquals(
            ToleranceLevel.EXACT,
            WorksheetBuilder.resolveTolerance(MathModule.MAKE_HUNDRED, ToleranceLevel.STANDARD, customConfig),
        )
        assertEquals(
            ToleranceLevel.STANDARD,
            WorksheetBuilder.resolveTolerance(MathModule.FIVE_DIV_THREE, ToleranceLevel.EXACT, customConfig),
        )
        assertEquals(
            ToleranceLevel.STRICT,
            WorksheetBuilder.resolveTolerance(MathModule.FIVE_DIV_THREE, ToleranceLevel.STRICT, customConfig),
        )
        assertEquals(
            ToleranceLevel.RELAXED,
            WorksheetBuilder.resolveTolerance(MathModule.CUSTOM, ToleranceLevel.STRICT, customConfig),
        )
    }
}
