package com.xingce.speedmath.ui.practice

import com.xingce.speedmath.domain.model.KeypadOrder
import kotlin.random.Random

sealed interface KeypadAction {
    data class Digit(val value: Int) : KeypadAction {
        init { require(value in 0..9) }
    }

    data object Decimal : KeypadAction
    data object ToggleMinus : KeypadAction
    data object Backspace : KeypadAction
    data object Clear : KeypadAction
}

fun reduceKeypadInput(current: String, action: KeypadAction): String {
    val next = when (action) {
        is KeypadAction.Digit -> when (current) {
            "0" -> action.value.toString()
            "-0" -> "-${action.value}"
            else -> current + action.value
        }
        KeypadAction.Decimal -> when {
            '.' in current -> current
            current.isEmpty() || current == "-" -> current + "0."
            else -> "$current."
        }
        KeypadAction.ToggleMinus -> if (current.startsWith('-')) current.drop(1) else "-$current"
        KeypadAction.Backspace -> current.dropLast(1)
        KeypadAction.Clear -> ""
    }
    return if (next.length <= 12) next else current
}

fun keypadDigitsFor(order: KeypadOrder, random: Random = Random.Default): List<Int> = when (order) {
    KeypadOrder.ASCENDING -> (1..9).toList()
    KeypadOrder.DESCENDING -> listOf(7, 8, 9, 4, 5, 6, 1, 2, 3)
    KeypadOrder.SHUFFLED -> (1..9).shuffled(random)
}
