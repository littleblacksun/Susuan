package com.xingce.speedmath.domain

import com.xingce.speedmath.domain.engine.AnswerGrader
import com.xingce.speedmath.domain.engine.QuestionGenerator
import com.xingce.speedmath.domain.model.CustomOperation
import com.xingce.speedmath.domain.model.CustomQuizConfig
import com.xingce.speedmath.domain.model.GradeStatus
import com.xingce.speedmath.domain.model.MathModule
import com.xingce.speedmath.domain.model.Question
import com.xingce.speedmath.domain.model.ToleranceLevel
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionGeneratorTest {
    @Test
    fun allPresetsProduceOneHundredValidQuestions() {
        val generator = QuestionGenerator(Random(2026))
        for (module in MathModule.presets) {
            val questions = generator.generateBatch(module, 100, ToleranceLevel.RELAXED)
            assertEquals((0 until 100).toList(), questions.map { it.index })
            assertNoAdjacentDuplicates(questions)
            for (question in questions) {
                assertTrue(question.expression.isNotBlank())
                assertEquals(module, question.module)
                assertEquals(module.isEstimation, question.isEstimation)
                assertEquals(module == MathModule.THREE_DIV_FOUR, question.isPercentAnswer)
                assertEquals(
                    if (module.isEstimation) ToleranceLevel.RELAXED else ToleranceLevel.EXACT,
                    question.tolerance,
                )
                assertPresetOperands(question)
                assertReferenceDisplay(question)
            }
            assertPresetVariety(module, questions)
        }
    }

    @Test
    fun estimationPresetWithExactRequestedUsesStandardAndNonpositiveCountsProduceOneQuestion() {
        val generator = QuestionGenerator(Random(2026))
        for (module in MathModule.presets.filter { it.isEstimation }) {
            for (count in listOf(0, -5)) {
                val question = generator.generateBatch(module, count, ToleranceLevel.EXACT).single()
                assertEquals(0, question.index)
                assertTrue(question.isEstimation)
                assertEquals(ToleranceLevel.STANDARD, question.tolerance)
            }
        }
    }

    @Test
    fun customOperationsRespectOperandWidthsToleranceAndPercentMode() {
        val generator = QuestionGenerator(Random(2026))
        for (operation in CustomOperation.entries) {
            for (tolerance in ToleranceLevel.entries) {
                val config = CustomQuizConfig(
                    operation = operation,
                    firstDigits = 3,
                    secondDigits = 2,
                    percentAnswer = true,
                    tolerance = tolerance,
                )
                val questions = generator.generateBatch(MathModule.CUSTOM, 30, ToleranceLevel.STRICT, config)
                assertNoAdjacentDuplicates(questions)
                for (question in questions) {
                    assertEquals(MathModule.CUSTOM, question.module)
                    assertEquals(config.tolerance, question.tolerance)
                    assertEquals(tolerance != ToleranceLevel.EXACT, question.isEstimation)
                    assertEquals(operation == CustomOperation.DIV, question.isPercentAnswer)
                    assertCustomWidths(question, 3, 2)
                    assertTrue(question.exactValue.signum() >= 0)
                    val words = question.expression.split(' ')
                    val expected = when (operation) {
                        CustomOperation.ADD -> arithmetic(words, "+")
                        CustomOperation.SUB -> arithmetic(words, "-")
                        CustomOperation.MUL -> arithmetic(words, "×")
                        CustomOperation.DIV -> arithmetic(words, "÷").movePointRight(2)
                        CustomOperation.MIXED_ADD_SUB -> assertMixedTerms(question)
                    }
                    assertValue(expected, question)
                    assertReferenceDisplay(question)
                }
            }
        }
    }

    @Test
    fun customSubtractionAndMixedArithmeticGenerateNegativeResultsWhenEnabled() {
        val generator = QuestionGenerator(Random(2026))
        for (operation in listOf(CustomOperation.SUB, CustomOperation.MIXED_ADD_SUB)) {
            val configuration = CustomQuizConfig(operation = operation, firstDigits = 2, secondDigits = 2)
            val nonnegative = generator.generateBatch(MathModule.CUSTOM, 300, ToleranceLevel.EXACT, configuration)
            val signed = generator.generateBatch(
                MathModule.CUSTOM, 300, ToleranceLevel.EXACT, configuration.copy(allowNegative = true),
            )
            assertTrue(nonnegative.all { it.exactValue.signum() >= 0 })
            assertTrue(signed.count { it.exactValue.signum() < 0 } in 90..150)
            (nonnegative + signed).forEach { assertCustomWidths(it, 2, 2) }
        }
    }

    @Test
    fun customSubtractionUsesSmallestFeasibleWidthForNonnegativeResults() {
        val generator = QuestionGenerator(Random(2026))
        val config = CustomQuizConfig(operation = CustomOperation.SUB, firstDigits = 1, secondDigits = 4)
        val nonnegative = generator.generateBatch(MathModule.CUSTOM, 30, ToleranceLevel.EXACT, config)
        val signed = generator.generateBatch(
            MathModule.CUSTOM, 30, ToleranceLevel.EXACT, config.copy(allowNegative = true),
        )
        nonnegative.forEach {
            assertCustomWidths(it, 4, 4)
            assertTrue(it.exactValue.signum() >= 0)
        }
        signed.forEach {
            assertCustomWidths(it, 1, 4)
            assertTrue(it.exactValue.signum() < 0)
        }
    }

    @Test
    fun exactCustomDivisionAcrossEveryDigitPairCanBeAnsweredFromItsReferenceDisplay() {
        val generator = QuestionGenerator(Random(2026))
        for (firstDigits in 1..5) for (secondDigits in 1..4) for (percent in listOf(false, true)) {
            val config = CustomQuizConfig(
                operation = CustomOperation.DIV,
                firstDigits = firstDigits,
                secondDigits = secondDigits,
                percentAnswer = percent,
                tolerance = ToleranceLevel.EXACT,
            )
            val questions = generator.generateBatch(MathModule.CUSTOM, 20, ToleranceLevel.RELAXED, config)
            for (question in questions) {
                assertCustomWidths(question, maxOf(firstDigits, secondDigits - 1), secondDigits)
                val words = question.expression.split(' ')
                val quotient = words[0].toBigDecimal().divide(words[2].toBigDecimal(), 1, RoundingMode.UNNECESSARY)
                assertValue(if (percent) quotient.movePointRight(2) else quotient, question)
                val answer = question.exactDisplay + if (percent) "%" else ""
                assertEquals(GradeStatus.EXACT, AnswerGrader.grade(question, answer).status)
            }
        }
    }

    @Test
    fun customDigitLimitsAreNormalizedInGenerateOne() {
        val generator = QuestionGenerator(Random(2026))
        val question = generator.generateOne(
            index = 12,
            module = MathModule.CUSTOM,
            tolerance = ToleranceLevel.EXACT,
            customConfig = CustomQuizConfig(operation = CustomOperation.ADD, firstDigits = 12, secondDigits = 0),
        )
        assertEquals(12, question.index)
        assertCustomWidths(question, 5, 1)
    }

    @Test
    fun exhaustingRandomRetriesStillProducesDistinctAdjacentQuestions() {
        val repeatedDraw = object : Random() {
            override fun nextBits(bitCount: Int): Int = 0
        }
        val generator = QuestionGenerator(repeatedDraw)
        for (module in MathModule.presets) {
            val questions = generator.generateBatch(module, 5, ToleranceLevel.STANDARD)
            assertNoAdjacentDuplicates(questions)
            questions.forEach(::assertPresetOperands)
        }
        for (operation in CustomOperation.entries) for (firstDigits in 1..5) for (secondDigits in 1..4) {
            val config = CustomQuizConfig(
                operation = operation,
                firstDigits = firstDigits,
                secondDigits = secondDigits,
                tolerance = ToleranceLevel.EXACT,
            )
            assertNoAdjacentDuplicates(generator.generateBatch(MathModule.CUSTOM, 5, ToleranceLevel.EXACT, config))
        }
    }

    private fun assertPresetOperands(question: Question) {
        val words = question.expression.split(' ')
        val left = words.first().toInt()
        val right = words[2].toIntOrNull()

        fun binary(operator: String, leftRange: IntRange, rightRange: IntRange) {
            assertTrue(question.expression, left in leftRange)
            assertTrue(question.expression, right != null && right in rightRange)
            assertValue(arithmetic(words, operator), question)
        }

        when (question.module) {
            MathModule.FIVE_DIV_THREE -> binary("÷", 10000..99999, 101..999)
            MathModule.THREE_DIV_FOUR -> {
                assertEquals(3, words.size)
                assertEquals("÷", words[1])
                assertTrue(left in 101..999)
                assertTrue(right!! in 1001..9999)
                assertTrue(question.exactValue in BigDecimal("3")..BigDecimal("95"))
                assertValue(arithmetic(words, "÷").movePointRight(2), question)
            }
            MathModule.THREE_DIV_TWO -> {
                binary("÷", 101..999, 12..99)
                assertTrue(right!! % 10 != 0)
            }
            MathModule.THREE_DIV_ONE -> binary("÷", 101..999, 3..9)
            MathModule.MUL_ESTIMATION -> if (words.last().endsWith("%")) {
                assertEquals(3, words.size)
                assertEquals("×", words[1])
                assertTrue(left in 150..950)
                val percent = words.last().removeSuffix("%").toBigDecimal()
                assertEquals(1, percent.scale())
                assertTrue(percent in BigDecimal("11.2")..BigDecimal("89.8"))
                assertValue(left.toBigDecimal().multiply(percent).movePointLeft(2), question)
            } else {
                binary("×", 112..989, 18..98)
                assertTrue(left % 10 != 0 && right!! % 10 != 0)
            }
            MathModule.TWO_ADD_SUB -> if (words[1] == "+") {
                binary("+", 11..99, 11..99)
            } else {
                binary("-", 11..99, 11..left)
            }
            MathModule.MAKE_HUNDRED -> {
                val target = words.last().toInt()
                assertEquals(listOf("+", "?", "="), words.subList(1, 4))
                assertTrue(target in 100..900 && target % 100 == 0)
                assertTrue(left % 10 != 0 && target - left in 1..99)
                assertValue((target - left).toBigDecimal(), question)
            }
            MathModule.THREE_ADD -> binary("+", 111..989, 111..989)
            MathModule.THREE_SUB -> binary("-", 200..999, 101..(left - 11))
            MathModule.THREE_ADD_SUB -> if (words[1] == "+") {
                binary("+", 111..989, 111..989)
            } else {
                binary("-", 200..999, 101..(left - 11))
            }
            MathModule.MULTI_ADD -> {
                assertTrue(question.terms.size in 4..5)
                assertEquals(question.expression, question.terms.joinToString(" "))
                assertTrue(words.filterIndexed { i, _ -> i % 2 == 1 }.all { it == "+" })
                val operands = operands(question)
                assertEquals(question.terms.size, operands.size)
                assertTrue(operands.all { it in 105..985 })
                assertValue(operands.sum().toBigDecimal(), question)
            }
            MathModule.MIXED_ADD_SUB -> {
                assertTrue(question.terms.size in 3..4)
                assertTrue(operands(question).all { it in 101..699 || it in 21..99 })
                assertTrue(question.exactValue.signum() >= 0)
                assertValue(assertMixedTerms(question), question)
            }
            MathModule.TWO_MUL_ONE -> {
                binary("×", 12..99, 3..9)
                assertTrue(left % 10 != 0)
            }
            MathModule.THREE_MUL_ONE -> binary("×", 105..998, 3..9)
            MathModule.TWO_MUL_11 -> binary("×", 12..99, 11..11)
            MathModule.TWO_MUL_15 -> binary("×", 12..98, 15..15)
            MathModule.TWO_MUL_TWO -> {
                binary("×", 12..98, 12..98)
                assertTrue(left % 10 != 0 && right!! % 10 != 0)
            }
            MathModule.CUSTOM -> error("Expected a preset")
        }
    }

    private fun assertPresetVariety(module: MathModule, questions: List<Question>) {
        when (module) {
            MathModule.FIVE_DIV_THREE -> assertTrue(questions.count { operands(it).last() % 10 != 0 } >= 80)
            MathModule.MUL_ESTIMATION -> assertTrue(questions.count { it.expression.endsWith("%") } in 45..75)
            MathModule.TWO_ADD_SUB, MathModule.THREE_ADD_SUB ->
                assertTrue(questions.count { " + " in it.expression } in 35..65)
            MathModule.MAKE_HUNDRED -> assertTrue(questions.count { it.expression.endsWith("= 100") } in 45..75)
            MathModule.MULTI_ADD -> assertTrue(questions.count { it.terms.size == 4 } in 55..85)
            MathModule.MIXED_ADD_SUB -> assertEquals(setOf(3, 4), questions.map { it.terms.size }.toSet())
            else -> Unit
        }
    }

    private fun arithmetic(words: List<String>, operator: String): BigDecimal {
        assertEquals(3, words.size)
        assertEquals(operator, words[1])
        val a = words[0].toBigDecimal()
        val b = words[2].toBigDecimal()
        return when (operator) {
            "+" -> a.add(b)
            "-" -> a.subtract(b)
            "×" -> a.multiply(b)
            "÷" -> a.divide(b, 8, RoundingMode.HALF_UP)
            else -> error("Unexpected operator $operator")
        }
    }

    private fun assertMixedTerms(question: Question): BigDecimal {
        val words = question.expression.split(' ')
        val operators = words.filterIndexed { i, _ -> i % 2 == 1 }
        assertEquals(setOf("+", "-"), operators.toSet())
        assertEquals(operators.size + 1, question.terms.size)
        assertEquals(question.expression, question.terms.joinToString(" "))
        val numbers = operands(question)
        return operators.zip(numbers.drop(1)).fold(numbers.first()) { total, (operator, operand) ->
            if (operator == "+") total + operand else total - operand
        }.toBigDecimal()
    }

    private fun assertCustomWidths(question: Question, firstDigits: Int, secondDigits: Int) {
        val numbers = operands(question)
        assertEquals(firstDigits, numbers.first().toString().length)
        assertTrue(numbers.drop(1).all { it.toString().length == secondDigits })
        assertTrue(numbers.all { it >= 2 })
    }

    private fun operands(question: Question): List<Int> =
        question.expression.split(' ').filterIndexed { i, _ -> i % 2 == 0 }.map { it.toInt() }

    private fun assertReferenceDisplay(question: Question) {
        val alwaysDecimal = question.module in listOf(
            MathModule.FIVE_DIV_THREE, MathModule.THREE_DIV_FOUR, MathModule.THREE_DIV_TWO,
        )
        val exact = question.exactValue.stripTrailingZeros()
        val expected = if (alwaysDecimal || exact.scale() > 0) {
            exact.setScale(1, RoundingMode.HALF_UP).toPlainString()
        } else {
            exact.toPlainString()
        }
        assertEquals(expected, question.exactDisplay)
    }

    private fun assertValue(expected: BigDecimal, question: Question) {
        assertEquals(question.expression, 0, expected.compareTo(question.exactValue))
    }

    private fun assertNoAdjacentDuplicates(questions: List<Question>) {
        questions.zipWithNext().forEach { (a, b) ->
            assertFalse("Consecutive duplicate: ${a.expression}", a.expression == b.expression)
        }
    }
}
