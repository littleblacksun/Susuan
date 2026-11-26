package com.xingce.speedmath.export

import com.xingce.speedmath.domain.model.Question
import com.xingce.speedmath.domain.model.ToleranceLevel
import java.math.BigDecimal
import java.math.RoundingMode

fun formatQuestionLine(number: Int, question: Question): String {
    val expr = question.expression
    return when {
        expr.contains('?') -> "$number. ${expr.replace("?", "(____)")}"
        question.isPercentAnswer -> "$number. $expr = ______ %"
        else -> "$number. $expr = ______"
    }
}

fun formatAnswerLine(number: Int, question: Question): String {
    val unit = if (question.isPercentAnswer) "%" else ""
    if (!question.isEstimation || question.tolerance == ToleranceLevel.EXACT) {
        return "$number. ${question.exactDisplay}$unit"
    }
    val ratio = BigDecimal.valueOf(question.tolerance.ratio)
    val delta = question.exactValue.abs().multiply(ratio)
    val low = question.exactValue.subtract(delta).setScale(1, RoundingMode.HALF_UP).toPlainString()
    val high = question.exactValue.add(delta).setScale(1, RoundingMode.HALF_UP).toPlainString()
    return "$number. ${question.exactDisplay}$unit（${question.tolerance.shortLabel} 区间：$low$unit ~ $high$unit）"
}

object TextWorksheetFormatter {
    private const val DIVIDER = "----------------------------------------"
    private const val ANSWER_DIVIDER = "========================================"

    fun format(document: WorksheetDocument): String = buildString {
        appendLine(document.title)
        appendLine(document.subtitle)
        appendLine("生成时间：${document.generatedAtLabel}")
        appendLine(DIVIDER)
        document.questions.forEachIndexed { index, question ->
            appendLine(formatQuestionLine(index + 1, question))
        }
        if (document.includeAnswers) {
            appendLine()
            appendLine(ANSWER_DIVIDER)
            appendLine("参考答案与误差区间")
            appendLine(DIVIDER)
            document.questions.forEachIndexed { index, question ->
                appendLine(formatAnswerLine(index + 1, question))
            }
        }
    }.trimEnd()
}
