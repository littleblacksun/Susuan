package com.xingce.speedmath.domain.engine

import com.xingce.speedmath.domain.model.CustomOperation
import com.xingce.speedmath.domain.model.CustomQuizConfig
import com.xingce.speedmath.domain.model.MathModule
import com.xingce.speedmath.domain.model.Question
import com.xingce.speedmath.domain.model.ToleranceLevel
import com.xingce.speedmath.domain.model.questionExactDisplay
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.random.Random
import kotlin.random.nextInt

class QuestionGenerator(private val random: Random = Random.Default) {
    private val problems = Problems { random.nextInt(it) }

    fun generateBatch(
        module: MathModule,
        count: Int,
        tolerance: ToleranceLevel,
        customConfig: CustomQuizConfig = CustomQuizConfig(),
    ): List<Question> = buildList {
        repeat(count.coerceAtLeast(1)) { index ->
            add(nextDistinct(index, module, tolerance, customConfig, lastOrNull()?.expression))
        }
    }

    fun generateOne(
        index: Int,
        module: MathModule,
        tolerance: ToleranceLevel,
        customConfig: CustomQuizConfig = CustomQuizConfig(),
    ): Question = problems.question(index, module, tolerance, customConfig)

    private fun nextDistinct(
        index: Int,
        module: MathModule,
        tolerance: ToleranceLevel,
        customConfig: CustomQuizConfig,
        previousExpression: String?,
    ): Question {
        repeat(7) {
            val candidate = generateOne(index, module, tolerance, customConfig)
            if (candidate.expression != previousExpression) return candidate
        }
        // The final two attempts choose opposite ends of the ranges, producing distinct problems.
        val low = lowestChoices.question(index, module, tolerance, customConfig)
        return if (low.expression != previousExpression) low else {
            highestChoices.question(index, module, tolerance, customConfig)
        }
    }

    private companion object {
        val lowestChoices = Problems { it.first }
        val highestChoices = Problems { it.last }
    }
}

private data class Problem(
    val expression: String,
    val value: BigDecimal,
    val terms: List<String> = emptyList(),
)

private data class SignedTerm(val operand: Int, val subtract: Boolean) {
    val value: Int get() = if (subtract) -operand else operand
    val text: String get() = if (subtract) "- $operand" else "+ $operand"
}

private class Problems(private val pick: (IntRange) -> Int) {
    fun question(
        index: Int,
        module: MathModule,
        tolerance: ToleranceLevel,
        customConfig: CustomQuizConfig,
    ): Question {
        val custom = if (module == MathModule.CUSTOM) customConfig.normalized() else null
        val effectiveTolerance = when {
            custom != null -> custom.tolerance
            !module.isEstimation -> ToleranceLevel.EXACT
            tolerance == ToleranceLevel.EXACT -> ToleranceLevel.STANDARD
            else -> tolerance
        }
        val isPercent = custom?.let { it.operation == CustomOperation.DIV && it.percentAnswer }
            ?: module.isPercentAnswer
        val problem = if (custom == null) preset(module) else customProblem(custom)
        val value = if (isPercent) problem.value.multiply(100.toBigDecimal()) else problem.value
        return Question(
            index = index,
            module = module,
            expression = problem.expression,
            exactValue = value,
            exactDisplay = questionExactDisplay(module, value),
            isEstimation = effectiveTolerance != ToleranceLevel.EXACT,
            isPercentAnswer = isPercent,
            tolerance = effectiveTolerance,
            terms = problem.terms,
        )
    }

    private fun preset(module: MathModule): Problem = when (module) {
        MathModule.FIVE_DIV_THREE -> {
            val dividend = pick(10000..99999)
            val divisor = if (chance(80)) nonRound(101..999) else pick(101..999)
            division(dividend, divisor)
        }
        MathModule.THREE_DIV_FOUR -> {
            val dividend = pick(101..999)
            val minimum = maxOf(1001, (dividend * 100 + 94) / 95)
            val maximum = minOf(9999, dividend * 100 / 3)
            division(dividend, pick(minimum..maximum))
        }
        MathModule.THREE_DIV_TWO -> division(pick(101..999), nonRound(12..99))
        MathModule.THREE_DIV_ONE -> division(pick(101..999), pick(3..9))
        MathModule.MUL_ESTIMATION -> multiplicationEstimation()
        MathModule.TWO_ADD_SUB -> {
            val first = pick(11..99)
            if (chance(50)) addition(first, pick(11..99)) else subtraction(first, pick(11..first))
        }
        MathModule.MAKE_HUNDRED -> makeHundred()
        MathModule.THREE_ADD -> threeDigitAddition()
        MathModule.THREE_SUB -> threeDigitSubtraction()
        MathModule.THREE_ADD_SUB -> if (chance(50)) threeDigitAddition() else threeDigitSubtraction()
        MathModule.MULTI_ADD -> {
            val head = pick(105..985)
            val remainingCount = if (chance(70)) 3 else 4
            sum(head, List(remainingCount) { SignedTerm(pick(105..985), subtract = false) })
        }
        MathModule.MIXED_ADD_SUB -> mixedPreset()
        MathModule.TWO_MUL_ONE -> multiplication(nonRound(12..99), pick(3..9))
        MathModule.THREE_MUL_ONE -> multiplication(pick(105..998), pick(3..9))
        MathModule.TWO_MUL_11 -> multiplication(pick(12..99), 11)
        MathModule.TWO_MUL_15 -> multiplication(pick(12..98), 15)
        MathModule.TWO_MUL_TWO -> multiplication(nonRound(12..98), nonRound(12..98))
        MathModule.CUSTOM -> error("Custom questions require a configuration")
    }

    private fun multiplicationEstimation(): Problem {
        if (!chance(60)) return multiplication(nonRound(112..989), nonRound(18..98))
        val first = pick(150..950)
        val percent = BigDecimal.valueOf(pick(112..898).toLong(), 1)
        return Problem(
            "$first × ${percent.toPlainString()}%",
            first.toBigDecimal().multiply(percent).movePointLeft(2),
        )
    }

    private fun makeHundred(): Problem {
        val target = if (chance(60)) 100 else pick(2..9) * 100
        val complement = nonRound(1..99)
        return Problem("${target - complement} + ? = $target", complement.toBigDecimal())
    }

    private fun threeDigitAddition(): Problem = addition(pick(111..989), pick(111..989))

    private fun threeDigitSubtraction(): Problem {
        val first = pick(200..999)
        return subtraction(first, pick(101..(first - 11)))
    }

    private fun mixedPreset(): Problem {
        val head = mixedOperand()
        val subtractSecond = chance(50)
        val tail = buildList {
            add(SignedTerm(mixedOperand(), subtractSecond))
            add(SignedTerm(mixedOperand(), !subtractSecond))
            if (chance(50)) add(SignedTerm(mixedOperand(), chance(50)))
        }
        val signedTail = if (head + tail.sumOf { it.value } < 0) {
            tail.map { it.copy(subtract = !it.subtract) }
        } else {
            tail
        }
        return sum(head, signedTail)
    }

    private fun customProblem(config: CustomQuizConfig): Problem {
        val first = digitRange(config.firstDigits)
        val second = digitRange(config.secondDigits)
        return when (config.operation) {
            CustomOperation.ADD -> addition(pick(first), pick(second))
            CustomOperation.SUB -> customSubtraction(first, second, config.allowNegative)
            CustomOperation.MUL -> multiplication(pick(first), pick(second))
            CustomOperation.DIV -> {
                if (config.tolerance == ToleranceLevel.EXACT) exactDivision(config) else {
                    division(pick(first), pick(second))
                }
            }
            CustomOperation.MIXED_ADD_SUB -> customMixed(first, second, config.allowNegative)
        }
    }

    private fun customSubtraction(first: IntRange, second: IntRange, allowNegative: Boolean): Problem {
        if (allowNegative && first.last < second.first) {
            return subtraction(pick(first), pick(second))
        }
        if (allowNegative && first.first < second.last && chance(40)) {
            val left = pick(first.first..minOf(first.last, second.last - 1))
            return subtraction(left, pick(maxOf(second.first, left + 1)..second.last))
        }
        // Disjoint widths make nonnegative subtraction impossible without a wider minuend.
        val feasibleFirst = if (first.last < second.first) second else first
        val left = pick(maxOf(feasibleFirst.first, second.first)..feasibleFirst.last)
        return subtraction(left, pick(second.first..minOf(second.last, left)))
    }

    private fun customMixed(first: IntRange, second: IntRange, allowNegative: Boolean): Problem {
        val largestNegativeHead = second.last - second.first - 1
        val negative = allowNegative && first.first <= largestNegativeHead && chance(40)
        val head = pick(if (negative) first.first..minOf(first.last, largestNegativeHead) else first)
        val positive = pick(if (negative) second.first..(second.last - head - 1) else second)
        val negativeRange = if (negative) {
            (head + positive + 1)..second.last
        } else {
            second.first..minOf(second.last, head + positive)
        }
        val plus = SignedTerm(positive, subtract = false)
        val minus = SignedTerm(pick(negativeRange), subtract = true)
        return sum(head, if (chance(50)) listOf(plus, minus) else listOf(minus, plus))
    }

    private fun exactDivision(config: CustomQuizConfig): Problem {
        // A nonzero quotient with at most one decimal place requires dividend >= divisor / 10.
        val dividends = digitRange(maxOf(config.firstDigits, config.secondDigits - 1))
        val divisors = digitRange(config.secondDigits)
        val candidate = pick(divisors)
        val candidateStep = tenthsStep(candidate)
        val candidateMultiples = multiplesIn(dividends, candidateStep)
        if (!candidateMultiples.isEmpty()) {
            return division(pick(candidateMultiples) * candidateStep, candidate)
        }

        // A multiple-of-ten divisor leaves a feasible grid with the requested divisor width.
        val step = pick((divisors.first / 10)..(divisors.last / 10))
        return division(pick(multiplesIn(dividends, step)) * step, step * 10)
    }

    private fun tenthsStep(divisor: Int): Int {
        val commonFactor = when {
            divisor % 10 == 0 -> 10
            divisor % 5 == 0 -> 5
            divisor % 2 == 0 -> 2
            else -> 1
        }
        return divisor / commonFactor
    }

    private fun multiplesIn(range: IntRange, step: Int): IntRange =
        ((range.first + step - 1) / step)..(range.last / step)

    private fun chance(percent: Int): Boolean = pick(0..99) < percent

    private fun mixedOperand(): Int = pick(if (chance(50)) 101..699 else 21..99)

    private fun nonRound(range: IntRange): Int {
        val candidate = pick(range)
        return if (candidate % 10 != 0) candidate else minOf(candidate + 1, range.last)
    }

    private fun digitRange(digits: Int): IntRange = if (digits == 1) 2..9 else {
        val minimum = powersOfTen[digits - 1]
        minimum..(minimum * 10 - 1)
    }

    private fun addition(a: Int, b: Int): Problem = Problem("$a + $b", (a + b).toBigDecimal())

    private fun subtraction(a: Int, b: Int): Problem = Problem("$a - $b", (a - b).toBigDecimal())

    private fun multiplication(a: Int, b: Int): Problem =
        Problem("$a × $b", (a.toLong() * b).toBigDecimal())

    private fun division(a: Int, b: Int): Problem =
        Problem("$a ÷ $b", a.toBigDecimal().divide(b.toBigDecimal(), 8, RoundingMode.HALF_UP))

    private fun sum(head: Int, tail: List<SignedTerm>): Problem {
        val terms = listOf(head.toString()) + tail.map { it.text }
        return Problem(terms.joinToString(" "), (head + tail.sumOf { it.value }).toBigDecimal(), terms)
    }

    private companion object {
        val powersOfTen = intArrayOf(1, 10, 100, 1000, 10000)
    }
}
