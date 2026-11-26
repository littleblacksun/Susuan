package com.xingce.speedmath

import com.xingce.speedmath.domain.model.CustomOperation
import com.xingce.speedmath.domain.model.CustomQuizConfig
import com.xingce.speedmath.domain.model.MathModule
import com.xingce.speedmath.domain.model.ToleranceLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class ConfigModelTest {
    @Test
    fun normalizingClipsBothDigitRangesAndKeepsOtherOptions() {
        val config = CustomQuizConfig(
            operation = CustomOperation.SUB,
            firstDigits = 0,
            secondDigits = 9,
            allowNegative = true,
            percentAnswer = true,
            tolerance = ToleranceLevel.STRICT,
        )
        assertEquals(config.copy(firstDigits = 1, secondDigits = 4), config.normalized())
        val reversed = config.copy(firstDigits = 6, secondDigits = -2)
        assertEquals(reversed.copy(firstDigits = 5, secondDigits = 1), reversed.normalized())
        assertEquals(CustomQuizConfig(), CustomQuizConfig().normalized())
    }

    @Test
    fun toleranceLookupFallsBackToStandardAndPreservesExact() {
        for (level in ToleranceLevel.entries) {
            assertEquals(level, ToleranceLevel.fromName(level.name))
        }
        assertEquals(ToleranceLevel.STRICT, ToleranceLevel.fromName(" strict "))
        assertEquals(ToleranceLevel.STANDARD, ToleranceLevel.fromName("unknown"))
        assertEquals(ToleranceLevel.STANDARD, ToleranceLevel.fromName(""))
        assertEquals(listOf(0.01, 0.02, 0.05), ToleranceLevel.estimationOptions.map { it.ratio })
    }

    @Test
    fun moduleLookupAcceptsCatalogIdsAndFallsBackToDefaultModule() {
        for (module in MathModule.entries) {
            assertEquals(module, MathModule.fromId(module.id))
        }
        assertEquals(MathModule.THREE_DIV_FOUR, MathModule.fromId(" THREE_DIV_FOUR "))
        assertEquals(MathModule.FIVE_DIV_THREE, MathModule.fromId("missing_module"))
        assertEquals(MathModule.FIVE_DIV_THREE, MathModule.fromId(""))
    }
}
