package com.xingce.speedmath.ui

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.xingce.speedmath.data.db.FormulaEntity
import com.xingce.speedmath.domain.model.FormulaCategory
import com.xingce.speedmath.ui.formula.FormulaFilter
import com.xingce.speedmath.ui.formula.filterFormulas
import com.xingce.speedmath.ui.formula.insertMathSymbol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormulaLogicTest {
    private val growth = FormulaEntity(
        id = 1, title = "增长率", category = FormulaCategory.GROWTH_RATE.name,
        expression = "R = A ÷ B", note = "记得先统一量级",
        isFavorite = true, isBuiltIn = true, createdAt = 0, updatedAt = 0,
    )
    private val base = growth.copy(
        id = 2, title = "基期", category = FormulaCategory.BASE_PERIOD.name,
        expression = "现期 ÷ (1 + r)", note = "先截位", isFavorite = false,
    )
    private val custom = growth.copy(
        id = 3, title = "手写公式", category = "unknown", expression = "A × 100%",
        note = "", isBuiltIn = false,
    )
    private val formulas = listOf(custom, growth, base)

    @Test
    fun keywordMatchesTitleExpressionOrNoteIgnoringCaseAndOuterSpaces() {
        assertEquals(listOf(base), filterFormulas(formulas, "基期", FormulaFilter.ALL))
        assertEquals(listOf(growth), filterFormulas(formulas, "  r = a  ", FormulaFilter.ALL))
        assertEquals(listOf(growth), filterFormulas(formulas, "统一量级", FormulaFilter.ALL))
        assertEquals(emptyList<FormulaEntity>(), filterFormulas(formulas, "无匹配", FormulaFilter.ALL))
    }

    @Test
    fun categoryAndFavoritesKeepRepositoryOrderAndCombineWithSearch() {
        assertEquals(formulas, filterFormulas(formulas, " ", FormulaFilter.ALL))
        assertEquals(listOf(custom, growth), filterFormulas(formulas, "", FormulaFilter.FAVORITE))
        assertEquals(listOf(custom), filterFormulas(formulas, "100%", FormulaFilter.CUSTOM))
        assertEquals(listOf(base), filterFormulas(formulas, "截位", FormulaFilter.BASE_PERIOD))
        assertEquals(emptyList<FormulaEntity>(), filterFormulas(formulas, "基期", FormulaFilter.FAVORITE))
        assertEquals(emptyList<FormulaEntity>(), filterFormulas(formulas, "增长率", FormulaFilter.BASE_PERIOD))
    }

    @Test
    fun symbolInsertsAtActualCursorAndAdvancesByItsLength() {
        val original = TextFieldValue("AB", TextRange(1))
        assertEquals(TextFieldValue("A×B", TextRange(2)), insertMathSymbol(original, "×"))
        assertEquals(TextFieldValue("Ar₁B", TextRange(3)), insertMathSymbol(original, "r₁"))
        assertEquals(TextFieldValue("²", TextRange(1)), insertMathSymbol(TextFieldValue(), "²"))
    }

    @Test
    fun pairedSymbolsPlaceCursorInsideDelimiters() {
        val original = TextFieldValue("AB", TextRange(1))
        assertEquals(TextFieldValue("A()B", TextRange(2)), insertMathSymbol(original, "()"))
        assertEquals(TextFieldValue("A| |B", TextRange(2)), insertMathSymbol(original, "| |"))
    }

    @Test
    fun symbolReplacesForwardOrReversedSelectionAndClearsComposition() {
        val forward = TextFieldValue("ABCDE", TextRange(1, 4), TextRange(1, 4))
        val reverse = forward.copy(selection = TextRange(4, 1))
        assertEquals(TextFieldValue("A÷E", TextRange(2)), insertMathSymbol(forward, "÷"))
        val result = insertMathSymbol(reverse, "()")
        assertEquals(TextFieldValue("A()E", TextRange(2)), result)
        assertNull(result.composition)
    }
}
