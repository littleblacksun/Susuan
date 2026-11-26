package com.xingce.speedmath.domain

import com.xingce.speedmath.domain.engine.NBackController
import com.xingce.speedmath.domain.engine.NBackPhase
import com.xingce.speedmath.domain.engine.NBackPhase.COMPLETED
import com.xingce.speedmath.domain.engine.NBackPhase.MEMORIZE
import com.xingce.speedmath.domain.engine.NBackPhase.RECALL_ACTIVE
import com.xingce.speedmath.domain.engine.NBackPhase.RECALL_TAIL
import com.xingce.speedmath.domain.engine.NBackPhase.STANDARD
import com.xingce.speedmath.domain.model.GradeStatus
import com.xingce.speedmath.domain.model.MathModule
import com.xingce.speedmath.domain.model.NBackLevel
import com.xingce.speedmath.domain.model.Question
import com.xingce.speedmath.domain.model.ToleranceLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class NBackControllerTest {
    @Test
    fun standardModeDisplaysAndGradesEachOfFiveQuestionsInFiveSteps() {
        assertRun(
            questionCount = 5,
            level = NBackLevel.OFF,
            effectiveN = 0,
            expected = List(5) { ExpectedStep(STANDARD, it, it) },
        )
    }

    @Test
    fun oneBackCompletesFiveQuestionsInSixStepsIncludingOneTailAnswer() {
        assertRun(
            questionCount = 5,
            level = NBackLevel.ONE,
            effectiveN = 1,
            expected = listOf(
                ExpectedStep(MEMORIZE, 0, null),
                ExpectedStep(RECALL_ACTIVE, 1, 0),
                ExpectedStep(RECALL_ACTIVE, 2, 1),
                ExpectedStep(RECALL_ACTIVE, 3, 2),
                ExpectedStep(RECALL_ACTIVE, 4, 3),
                ExpectedStep(RECALL_TAIL, null, 4),
            ),
        )
    }

    @Test
    fun twoBackCompletesFiveQuestionsInSevenStepsIncludingBothTailAnswers() {
        assertRun(
            questionCount = 5,
            level = NBackLevel.TWO,
            effectiveN = 2,
            expected = listOf(
                ExpectedStep(MEMORIZE, 0, null),
                ExpectedStep(MEMORIZE, 1, null),
                ExpectedStep(RECALL_ACTIVE, 2, 0),
                ExpectedStep(RECALL_ACTIVE, 3, 1),
                ExpectedStep(RECALL_ACTIVE, 4, 2),
                ExpectedStep(RECALL_TAIL, null, 3),
                ExpectedStep(RECALL_TAIL, null, 4),
            ),
        )
    }

    @Test
    fun twoBackWithOnlyOneQuestionDowngradesToStandard() {
        assertRun(1, NBackLevel.TWO, 0, listOf(ExpectedStep(STANDARD, 0, 0)))
    }

    @Test
    fun twoBackWithTwoQuestionsDowngradesToOneBack() {
        assertRun(
            questionCount = 2,
            level = NBackLevel.TWO,
            effectiveN = 1,
            expected = listOf(
                ExpectedStep(MEMORIZE, 0, null),
                ExpectedStep(RECALL_ACTIVE, 1, 0),
                ExpectedStep(RECALL_TAIL, null, 1),
            ),
        )
    }

    @Test
    fun emptySessionStartsCompleted() {
        assertRun(0, NBackLevel.TWO, 0, emptyList())
    }

    @Test
    fun nonAnswerableStepsDoNotAcceptSubmissionsAndInvalidInputOnATargetIsRecorded() {
        val questions = questions(2)
        val controller = NBackController(questions, NBackLevel.ONE)
        val memorize = controller.currentState()
        assertThrows(IllegalStateException::class.java) { controller.submitAnswer("15", 0) }
        assertEquals(memorize, controller.currentState())
        assertTrue(controller.answeredItems().isEmpty())

        controller.advanceMemorize()
        val (failedGrade, tail) = controller.submitAnswer("-", 850)
        assertEquals(GradeStatus.FAIL, failedGrade.status)
        assertNull(failedGrade.parsedUserValue)
        assertEquals(RECALL_TAIL, tail.phase)
        assertEquals(questions.first(), controller.answeredItems().single().question)
        assertEquals(850L, controller.answeredItems().single().durationMs)

        val (_, completed) = controller.submitAnswer(questions.last().exactDisplay, 1100)
        assertEquals(COMPLETED, completed.phase)
        assertThrows(IllegalStateException::class.java) { controller.submitAnswer("16", 0) }
        assertEquals(completed, controller.currentState())
        assertEquals(questions, controller.answeredItems().map { it.question })
    }

    @Test
    fun questionOrderAndAnswerSnapshotsAreIndependentOfCallerListMutation() {
        val original = questions(2)
        val source = original.toMutableList()
        val controller = NBackController(source, NBackLevel.OFF)
        source.clear()

        controller.submitAnswer(original[0].exactDisplay, 300)
        val snapshot = controller.answeredItems()
        val (_, completed) = controller.submitAnswer(original[1].exactDisplay, 500)
        assertEquals(COMPLETED, completed.phase)
        assertEquals(listOf(original[0]), snapshot.map { it.question })
        assertEquals(original, controller.answeredItems().map { it.question })
    }

    private fun assertRun(
        questionCount: Int,
        level: NBackLevel,
        effectiveN: Int,
        expected: List<ExpectedStep>,
    ) {
        val questions = questions(questionCount)
        val controller = NBackController(questions, level)
        assertEquals(questionCount + effectiveN, expected.size)
        val durations = mutableListOf<Long>()
        expected.forEachIndexed { index, step ->
            val state = controller.currentState()
            assertEquals(index, state.stepIndex)
            assertEquals(expected.size, state.totalSteps)
            assertEquals(effectiveN, state.effectiveN)
            assertEquals(questionCount, state.totalQuestionCount)
            assertEquals(durations.size, state.answeredCount)
            assertEquals(step.phase, state.phase)
            assertEquals(step.display?.let { questions[it] }, state.displayQuestion)
            assertEquals(step.target?.let { questions[it] }, state.targetQuestion)
            val expectedBanner = when (step.phase) {
                MEMORIZE -> "$effectiveN-back 记忆阶段：记住第 ${step.display!! + 1} 题答案后点继续"
                RECALL_ACTIVE -> "$effectiveN-back：记住本题，输入第 ${step.target!! + 1} 题答案"
                RECALL_TAIL -> "$effectiveN-back 收尾：请输入第 ${step.target!! + 1} 题答案"
                STANDARD, COMPLETED -> null
            }
            assertEquals(expectedBanner, state.bannerText)
            if (step.target == null) {
                val next = controller.advanceMemorize()
                assertEquals(index + 1, next.stepIndex)
                assertEquals(durations.size, next.answeredCount)
            } else {
                assertEquals(state, controller.advanceMemorize())
                val duration = 1000L + index
                val (grade, next) = controller.submitAnswer(questions[step.target].exactDisplay, duration)
                assertEquals(GradeStatus.EXACT, grade.status)
                durations.add(duration)
                assertEquals(index + 1, next.stepIndex)
                assertEquals(durations.size, next.answeredCount)
            }
        }
        val completed = controller.currentState()
        assertEquals(COMPLETED, completed.phase)
        assertEquals(expected.size, completed.stepIndex)
        assertEquals(expected.size, completed.totalSteps)
        assertEquals(effectiveN, completed.effectiveN)
        assertEquals(questionCount, completed.answeredCount)
        assertEquals(questionCount, completed.totalQuestionCount)
        assertNull(completed.displayQuestion)
        assertNull(completed.targetQuestion)
        assertNull(completed.bannerText)
        assertEquals(completed, controller.advanceMemorize())
        assertEquals(questions, controller.answeredItems().map { it.question })
        assertEquals(durations, controller.answeredItems().map { it.durationMs })
    }

    private fun questions(count: Int): List<Question> = List(count) { position ->
        val second = position + 3
        val answer = 12 + second
        Question(
            index = position * 3,
            module = MathModule.TWO_ADD_SUB,
            expression = "12 + $second",
            exactValue = answer.toBigDecimal(),
            exactDisplay = answer.toString(),
            isEstimation = false,
            isPercentAnswer = false,
            tolerance = ToleranceLevel.EXACT,
        )
    }

    private data class ExpectedStep(val phase: NBackPhase, val display: Int?, val target: Int?)
}
