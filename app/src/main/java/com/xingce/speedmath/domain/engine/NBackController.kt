package com.xingce.speedmath.domain.engine

import com.xingce.speedmath.domain.model.AnsweredItem
import com.xingce.speedmath.domain.model.GradeResult
import com.xingce.speedmath.domain.model.NBackLevel
import com.xingce.speedmath.domain.model.Question

enum class NBackPhase { STANDARD, MEMORIZE, RECALL_ACTIVE, RECALL_TAIL, COMPLETED }

data class NBackStepState(
    val stepIndex: Int,
    val totalSteps: Int,
    val phase: NBackPhase,
    val effectiveN: Int,
    val displayQuestion: Question?,
    val targetQuestion: Question?,
    val answeredCount: Int,
    val totalQuestionCount: Int,
    val bannerText: String?,
)

class NBackController(questions: List<Question>, nBackLevel: NBackLevel) {
    private val questions = questions.toList()
    private val effectiveSteps = nBackLevel.steps.coerceAtMost((this.questions.size - 1).coerceAtLeast(0))
    private val totalSteps = this.questions.size + effectiveSteps
    private val answers = mutableListOf<AnsweredItem>()
    private var stepIndex = 0

    fun currentState(): NBackStepState {
        val phase = when {
            stepIndex >= totalSteps -> NBackPhase.COMPLETED
            effectiveSteps == 0 -> NBackPhase.STANDARD
            stepIndex < effectiveSteps -> NBackPhase.MEMORIZE
            stepIndex < questions.size -> NBackPhase.RECALL_ACTIVE
            else -> NBackPhase.RECALL_TAIL
        }
        val targetIndex = stepIndex - effectiveSteps
        val banner = when (phase) {
            NBackPhase.MEMORIZE -> "$effectiveSteps-back 记忆阶段：记住第 ${stepIndex + 1} 题答案后点继续"
            NBackPhase.RECALL_ACTIVE -> "$effectiveSteps-back：记住本题，输入第 ${targetIndex + 1} 题答案"
            NBackPhase.RECALL_TAIL -> "$effectiveSteps-back 收尾：请输入第 ${targetIndex + 1} 题答案"
            NBackPhase.STANDARD, NBackPhase.COMPLETED -> null
        }
        return NBackStepState(
            stepIndex = stepIndex,
            totalSteps = totalSteps,
            phase = phase,
            effectiveN = effectiveSteps,
            displayQuestion = questions.getOrNull(stepIndex),
            targetQuestion = questions.getOrNull(targetIndex),
            answeredCount = answers.size,
            totalQuestionCount = questions.size,
            bannerText = banner,
        )
    }

    fun advanceMemorize(): NBackStepState {
        val state = currentState()
        if (state.phase != NBackPhase.MEMORIZE) return state
        stepIndex += 1
        return currentState()
    }

    fun submitAnswer(rawInput: String, durationMs: Long): Pair<GradeResult, NBackStepState> {
        val target = checkNotNull(currentState().targetQuestion) {
            "There is no question to answer at step $stepIndex"
        }
        val grade = AnswerGrader.grade(target, rawInput)
        answers.add(AnsweredItem(target, grade, durationMs))
        stepIndex += 1
        return grade to currentState()
    }

    fun answeredItems(): List<AnsweredItem> = answers.toList()
}
