package com.xingce.speedmath.ui

import com.xingce.speedmath.domain.engine.AnswerGrader
import com.xingce.speedmath.domain.model.AnsweredItem
import com.xingce.speedmath.domain.model.GradeStatus
import com.xingce.speedmath.domain.model.MathModule
import com.xingce.speedmath.domain.model.Question
import com.xingce.speedmath.domain.model.ToleranceLevel
import com.xingce.speedmath.ui.practice.QuestionReconstructor
import com.xingce.speedmath.ui.practice.formatPracticeSeconds
import com.xingce.speedmath.ui.practice.formatPracticeTime
import com.xingce.speedmath.ui.practice.mapPracticeSession
import com.xingce.speedmath.ui.practice.toPracticeItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeSessionMapperTest {
    @Test
    fun answeredItemPersistsFullPrecisionAndGradingMetadata() {
        val question = percentQuestion(2, "12.65908013")
        val grade = AnswerGrader.grade(question, " 13.7% ")
        val item = AnsweredItem(question, grade, 5850).toPracticeItem()

        assertEquals(2, item.questionIndex)
        assertEquals(question.expression, item.expression)
        assertEquals("13.7", item.userAnswer)
        assertEquals("12.65908013", item.exactAnswer)
        assertTrue(item.isPercent)
        assertEquals(GradeStatus.FAIL.name, item.gradeStatus)
        assertEquals(grade.signedErrorPercent, item.signedErrorPercent)
        assertEquals(5850L, item.durationMs)
    }

    @Test
    fun sessionStoresWholeRunTimeIncludingMemorizationSeparatelyFromItemTimes() {
        val question = percentQuestion(0, "12.65908013")
        val item = AnsweredItem(question, AnswerGrader.grade(question, "12.7"), 5850)
        val record = mapPracticeSession(listOf(item), 7810, 1, 1234)

        assertEquals(question.module.id, record.session.moduleId)
        assertEquals(question.module.title, record.session.moduleTitle)
        assertEquals(ToleranceLevel.STRICT.name, record.session.toleranceName)
        assertEquals(1, record.session.correctCount)
        assertEquals(1, record.session.questionCount)
        assertEquals(7810L, record.session.totalDurationMs)
        assertEquals(5850L, record.items.single().durationMs)
        assertEquals(1, record.session.nBackSteps)
        assertEquals(1234L, record.session.createdAt)
    }

    @Test
    fun reconstructedFailuresKeepUnroundedValuesUnitsToleranceAndOriginalOrder() {
        val first = percentQuestion(1, "12.65908013")
        val accepted = percentQuestion(3, "12.65908013")
        val last = percentQuestion(4, "12.74000000").copy(expression = "547 ÷ 4294")
        val recorded = listOf(
            AnsweredItem(last, AnswerGrader.grade(last, ""), 3100L),
            AnsweredItem(accepted, AnswerGrader.grade(accepted, "12.7"), 3000L),
            AnsweredItem(first, AnswerGrader.grade(first, "14"), 6500L),
        )
        val details = mapPracticeSession(recorded, 12600, 0, 10)
        val retry = QuestionReconstructor.fromFailedItems(details)

        assertEquals(listOf(0, 1), retry.map { it.index })
        assertEquals(listOf(first.expression, last.expression), retry.map { it.expression })
        assertEquals(listOf(first.exactValue, last.exactValue), retry.map { it.exactValue })
        assertEquals(listOf("12.7", "12.7"), retry.map { it.exactDisplay })
        assertTrue(retry.all { it.isPercentAnswer && it.isEstimation && it.tolerance == ToleranceLevel.STRICT })
        assertEquals(GradeStatus.FAIL, AnswerGrader.grade(retry[0], "12.82").status)
    }

    @Test
    fun timeFormatsUseWholeSecondsAndOneDecimalWithHalfUpRounding() {
        assertEquals("00:00", formatPracticeTime(0))
        assertEquals("01:12", formatPracticeTime(72_999))
        assertEquals("61:01", formatPracticeTime(3_661_015))
        assertEquals("5.9", formatPracticeSeconds(5850))
        assertEquals("7.2", formatPracticeSeconds(72_000, 10))
        assertEquals("6.0", formatPracticeSeconds(6000))
    }

    private fun percentQuestion(index: Int, exact: String) = Question(
        index = index,
        module = MathModule.THREE_DIV_FOUR,
        expression = "534 ÷ 4218",
        exactValue = exact.toBigDecimal(),
        exactDisplay = "12.7",
        isEstimation = true,
        isPercentAnswer = true,
        tolerance = ToleranceLevel.STRICT,
    )
}
