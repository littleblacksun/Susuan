package com.xingce.speedmath.domain

import com.xingce.speedmath.domain.engine.AnswerGrader
import com.xingce.speedmath.domain.engine.PracticeSummaryCalculator
import com.xingce.speedmath.domain.model.AnsweredItem
import com.xingce.speedmath.domain.model.GradeStatus
import com.xingce.speedmath.domain.model.MathModule
import com.xingce.speedmath.domain.model.PracticeSummary
import com.xingce.speedmath.domain.model.Question
import com.xingce.speedmath.domain.model.ToleranceLevel
import java.math.BigDecimal
import java.math.RoundingMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerGraderTest {
    @Test
    fun exactModeMatchesNumericValueRatherThanStringScale() {
        val question = question("115", ToleranceLevel.EXACT)
        for (input in listOf("115", "115.0", "  115.000  ")) {
            val result = AnswerGrader.grade(question, input)
            assertEquals(GradeStatus.EXACT, result.status)
            assertTrue(result.isCorrect)
            assertEquals(0.0, result.signedErrorPercent!!, 0.0)
            assertEquals("回答正确", result.feedbackText)
            assertEquals("115", result.exactDisplay)
        }
        val fail = AnswerGrader.grade(question, "116")
        assertEquals(GradeStatus.FAIL, fail.status)
        assertFalse(fail.isCorrect)
        assertNull(fail.signedErrorPercent)
        assertEquals("回答错误 · 精确 115", fail.feedbackText)
    }

    @Test
    fun normalizationAcceptsUnicodeMinusAndTrailingPercentSigns() {
        val question = question("-115", ToleranceLevel.EXACT)
        for (minus in listOf("-", "−", "–", "－")) {
            val rawInput = "  ${minus}115.0 %％  "
            val result = AnswerGrader.grade(question, rawInput)
            assertEquals(GradeStatus.EXACT, result.status)
            assertEquals("-115.0", result.normalizedUserInput)
            assertEquals(BigDecimal("-115.0"), result.parsedUserValue)
            assertEquals(BigDecimal("-115.0"), AnswerGrader.parseDecimal(rawInput))
        }
        assertEquals(BigDecimal("0.5"), AnswerGrader.parseDecimal(".5"))
        assertEquals(BigDecimal("115"), AnswerGrader.parseDecimal("115."))
    }

    @Test
    fun exactToleranceAndExactQuestionFlagEachSelectExactMode() {
        val estimateWithExactTolerance = question("100", ToleranceLevel.EXACT).copy(isEstimation = true)
        val exactWithEstimateTolerance = question("100").copy(isEstimation = false)
        for (question in listOf(estimateWithExactTolerance, exactWithEstimateTolerance)) {
            val result = AnswerGrader.grade(question, "100.1")
            assertEquals(GradeStatus.FAIL, result.status)
            assertEquals("回答错误 · 精确 100", result.feedbackText)
            assertNull(result.signedErrorPercent)
        }
    }

    @Test
    fun estimationThresholdsUseUnroundedRelativeErrors() {
        val cases = listOf(
            Triple(ToleranceLevel.STRICT, "100.5", GradeStatus.EXACT),
            Triple(ToleranceLevel.STRICT, "100.51", GradeStatus.PASS),
            Triple(ToleranceLevel.STRICT, "101.0", GradeStatus.PASS),
            Triple(ToleranceLevel.STRICT, "101.01", GradeStatus.FAIL),
            Triple(ToleranceLevel.STANDARD, "102.00", GradeStatus.PASS),
            Triple(ToleranceLevel.STANDARD, "102.04", GradeStatus.FAIL),
            Triple(ToleranceLevel.STANDARD, "98.00", GradeStatus.PASS),
            Triple(ToleranceLevel.STANDARD, "97.96", GradeStatus.FAIL),
            Triple(ToleranceLevel.RELAXED, "105.0", GradeStatus.PASS),
            Triple(ToleranceLevel.RELAXED, "105.01", GradeStatus.FAIL),
        )
        for ((tolerance, input, status) in cases) {
            val result = AnswerGrader.grade(question("100", tolerance), input)
            assertEquals("$tolerance, answer $input", status, result.status)
            assertEquals(status != GradeStatus.FAIL, result.isCorrect)
        }
        val displayedAsTwo = AnswerGrader.grade(question("100"), "102.04")
        assertEquals(GradeStatus.FAIL, displayedAsTwo.status)
        assertEquals(2.0, displayedAsTwo.signedErrorPercent!!, 0.0)
        assertEquals("偏差过大 · 精确 100 · 误差 +2.0%", displayedAsTwo.feedbackText)

        val barelyOver = "102." + "0".repeat(40) + "1"
        assertEquals(GradeStatus.FAIL, AnswerGrader.grade(question("100"), barelyOver).status)
    }

    @Test
    fun solvedDivisionRoundsSignedErrorToPositiveOnePointZeroPercent() {
        val exact = BigDecimal("38412").divide(BigDecimal("192"), 8, RoundingMode.HALF_UP)
        val question = question("200.1").copy(expression = "38412 ÷ 192", exactValue = exact)
        val result = AnswerGrader.grade(question, "202")
        assertEquals(GradeStatus.PASS, result.status)
        assertEquals(1.0, result.signedErrorPercent!!, 0.0)
        assertEquals("估算通过 · 精确 200.1 · 误差 +1.0%", result.feedbackText)
    }

    @Test
    fun signedErrorUsesAbsoluteDenominatorAndNormalizesNegativeZero() {
        val belowNegativeExact = AnswerGrader.grade(question("-100"), "−100.8")
        assertEquals(-0.8, belowNegativeExact.signedErrorPercent!!, 0.0)
        assertTrue(belowNegativeExact.feedbackText.endsWith("误差 -0.8%"))

        val negativeNearZero = AnswerGrader.grade(question("100"), "99.99")
        assertEquals(GradeStatus.EXACT, negativeNearZero.status)
        assertEquals(0.0.toBits(), negativeNearZero.signedErrorPercent!!.toBits())
        assertEquals("精准命中 · 精确 100 · 误差 0.0%", negativeNearZero.feedbackText)

        val negativeTie = AnswerGrader.grade(question("100"), "99.95")
        assertEquals(-0.1, negativeTie.signedErrorPercent!!, 0.0)
        val positiveTie = AnswerGrader.grade(question("100"), "100.05")
        assertEquals(0.1, positiveTie.signedErrorPercent!!, 0.0)
    }

    @Test
    fun zeroReferenceRequiresZeroAndAvoidsDivisionByZero() {
        val question = question("0")
        val matched = AnswerGrader.grade(question, "-0.00")
        assertEquals(GradeStatus.EXACT, matched.status)
        assertEquals(0.0, matched.signedErrorPercent!!, 0.0)
        val fail = AnswerGrader.grade(question, "0.01")
        assertEquals(GradeStatus.FAIL, fail.status)
        assertFalse(fail.isCorrect)
        assertNull(fail.signedErrorPercent)
        assertEquals("偏差过大 · 精确 0", fail.feedbackText)
    }

    @Test
    fun percentQuestionsIncludeUnitInFeedbackAndKeepNumericalScale() {
        val question = question("12.7", percent = true)
        val result = AnswerGrader.grade(question, " 12.7% ")
        assertEquals(GradeStatus.EXACT, result.status)
        assertEquals("12.7", result.normalizedUserInput)
        assertEquals(BigDecimal("12.7"), result.parsedUserValue)
        assertEquals("12.7", result.exactDisplay)
        assertEquals("精准命中 · 精确 12.7% · 误差 0.0%", result.feedbackText)
    }

    @Test
    fun exactPercentQuestionIncludesPercentInWrongAnswerFeedback() {
        val question = question("12.7", ToleranceLevel.EXACT, percent = true)
        val result = AnswerGrader.grade(question, "12.6")

        assertEquals(GradeStatus.FAIL, result.status)
        assertEquals("回答错误 · 精确 12.7%", result.feedbackText)
    }

    @Test
    fun malformedAndBlankInputsAreSafelyRejected() {
        val invalidInputs = listOf("", "   ", "-", ".", "12.3.4", "abc", "12%7", "1e2", "--1", "1 2")
        for (question in listOf(question("12.7", percent = true), question("115", ToleranceLevel.EXACT))) {
            val expectedDisplay = question.exactDisplay + if (question.isPercentAnswer) "%" else ""
            for (input in invalidInputs) {
                assertNull(AnswerGrader.parseDecimal(input))
                val result = AnswerGrader.grade(question, input)
                assertEquals(GradeStatus.FAIL, result.status)
                assertFalse(result.isCorrect)
                assertNull(result.parsedUserValue)
                assertNull(result.signedErrorPercent)
                assertEquals("格式无效 · 精确 $expectedDisplay", result.feedbackText)
            }
        }
    }

    @Test
    fun summaryUsesRoundedAccuracyDurationAndAbsoluteErrorsFromParsedEstimatesOnly() {
        val estimate = question("100")
        val exact = question("115", ToleranceLevel.EXACT)
        val items = listOf(
            answered(estimate, "101", 1000),
            answered(estimate, "98.5", 1100),
            answered(estimate, "102.04", 1200),
            answered(estimate, "abc", 1300),
            answered(exact, "115", 1400),
            answered(estimate, "100", 900),
        )
        assertEquals(PracticeSummary(6, 4, 67, 6900, 1.2, 1.1), PracticeSummaryCalculator.calculate(items))
        val positiveAndNegative = items.take(2)
        val result = PracticeSummaryCalculator.calculate(positiveAndNegative)
        assertEquals(1.3, result.averageAbsErrorPercent!!, 0.0)
    }

    @Test
    fun summaryHasNoAverageErrorWithoutParsedEstimates() {
        assertEquals(PracticeSummary(0, 0, 0, 0, 0.0, null), PracticeSummaryCalculator.calculate(emptyList()))
        val exact = question("115", ToleranceLevel.EXACT)
        val items = listOf(
            answered(exact, "115", 1000),
            answered(exact.copy(isEstimation = true), "115", 1000),
            answered(question("1"), "-", 1000),
        )
        assertEquals(PracticeSummary(3, 2, 67, 3000, 1.0, null), PracticeSummaryCalculator.calculate(items))
    }

    private fun question(
        exact: String,
        tolerance: ToleranceLevel = ToleranceLevel.STANDARD,
        percent: Boolean = false,
    ): Question = Question(
        index = 0,
        module = MathModule.CUSTOM,
        expression = "test",
        exactValue = BigDecimal(exact),
        exactDisplay = exact,
        isEstimation = tolerance != ToleranceLevel.EXACT,
        isPercentAnswer = percent,
        tolerance = tolerance,
    )

    private fun answered(question: Question, input: String, duration: Long): AnsweredItem =
        AnsweredItem(question, AnswerGrader.grade(question, input), duration)
}
