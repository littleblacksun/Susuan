package com.xingce.speedmath.ui

import com.xingce.speedmath.domain.model.KeypadOrder
import com.xingce.speedmath.ui.practice.KeypadAction
import com.xingce.speedmath.ui.practice.keypadDigitsFor
import com.xingce.speedmath.ui.practice.reduceKeypadInput
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class KeypadInputReducerTest {
    @Test
    fun digitsFoldMeaninglessLeadingZeroButPreserveDecimalZero() {
        assertEquals("5", reduceKeypadInput("", KeypadAction.Digit(5)))
        assertEquals("25", reduceKeypadInput("2", KeypadAction.Digit(5)))
        assertEquals("5", reduceKeypadInput("0", KeypadAction.Digit(5)))
        assertEquals("-5", reduceKeypadInput("-0", KeypadAction.Digit(5)))
        assertEquals("0.5", reduceKeypadInput("0.", KeypadAction.Digit(5)))
        assertEquals("-0.5", reduceKeypadInput("-0.", KeypadAction.Digit(5)))
    }

    @Test
    fun decimalInsertionIsUniqueAndAddsAnIntegerPartWhenNeeded() {
        assertEquals("0.", reduceKeypadInput("", KeypadAction.Decimal))
        assertEquals("-0.", reduceKeypadInput("-", KeypadAction.Decimal))
        assertEquals("12.", reduceKeypadInput("12", KeypadAction.Decimal))
        assertEquals("12.3", reduceKeypadInput("12.3", KeypadAction.Decimal))
    }

    @Test
    fun signBackspaceAndClearPreserveEditableIntermediateValues() {
        assertEquals("-", reduceKeypadInput("", KeypadAction.ToggleMinus))
        assertEquals("12", reduceKeypadInput("-12", KeypadAction.ToggleMinus))
        assertEquals("-12", reduceKeypadInput("12", KeypadAction.ToggleMinus))
        assertEquals("12.", reduceKeypadInput("12.3", KeypadAction.Backspace))
        assertEquals("", reduceKeypadInput("", KeypadAction.Backspace))
        assertEquals("", reduceKeypadInput("-12.3", KeypadAction.Clear))
    }

    @Test
    fun lengthLimitIncludesDecimalAndSign() {
        val max = "123456789012"
        assertEquals(max, reduceKeypadInput(max.dropLast(1), KeypadAction.Digit(2)))
        for (action in listOf(KeypadAction.Digit(1), KeypadAction.Decimal, KeypadAction.ToggleMinus)) {
            assertEquals(max, reduceKeypadInput(max, action))
        }
        assertEquals(max.dropLast(1), reduceKeypadInput(max, KeypadAction.Backspace))
    }

    @Test
    fun keypadOrdersHaveSpecifiedRowsAndEachShuffleHasAllNineDigits() {
        val ascending = (1..9).toList()
        assertEquals(ascending, keypadDigitsFor(KeypadOrder.ASCENDING))
        assertEquals(listOf(7, 8, 9, 4, 5, 6, 1, 2, 3), keypadDigitsFor(KeypadOrder.DESCENDING))

        val random = Random(44)
        val first = keypadDigitsFor(KeypadOrder.SHUFFLED, random)
        val second = keypadDigitsFor(KeypadOrder.SHUFFLED, random)
        assertEquals(ascending, first.sorted())
        assertEquals(ascending, second.sorted())
        assertNotEquals(first, second)
    }
}
