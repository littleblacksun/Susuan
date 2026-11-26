package com.xingce.speedmath.ui.practice

import com.xingce.speedmath.data.db.PracticeSessionDetails
import com.xingce.speedmath.domain.model.GradeStatus
import com.xingce.speedmath.domain.model.MathModule
import com.xingce.speedmath.domain.model.Question
import com.xingce.speedmath.domain.model.ToleranceLevel

object QuestionReconstructor {
    private val termBoundary = Regex(" (?=[+-] )")

    fun fromFailedItems(details: PracticeSessionDetails): List<Question> {
        val module = MathModule.fromId(details.session.moduleId)
        val tolerance = details.session.tolerance
        return details.items.asSequence()
            .filter { it.gradeStatus == GradeStatus.FAIL.name }
            .sortedBy { it.questionIndex }
            .mapIndexed { index, item ->
                Question(
                    index = index,
                    module = module,
                    expression = item.expression,
                    exactValue = item.exactWithoutUnit().toBigDecimal(),
                    exactDisplay = item.exactDisplay(module),
                    isEstimation = tolerance != ToleranceLevel.EXACT,
                    isPercentAnswer = item.isPercent,
                    tolerance = tolerance,
                    terms = item.expression.split(termBoundary).takeIf { it.size > 3 }.orEmpty(),
                )
            }
            .toList()
    }
}
