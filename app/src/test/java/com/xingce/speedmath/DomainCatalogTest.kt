package com.xingce.speedmath

import com.xingce.speedmath.domain.data.BaifenTable
import com.xingce.speedmath.domain.data.KnowledgeReference
import com.xingce.speedmath.domain.model.FormulaCategory
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import com.xingce.speedmath.domain.model.MathModule
import com.xingce.speedmath.domain.model.ModuleCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainCatalogTest {
    @Test
    fun catalogContainsSeventeenUniquePresetsInThreeCategories() {
        val presets = MathModule.presets
        assertEquals(18, MathModule.entries.size)
        assertEquals(17, presets.size)
        assertFalse(MathModule.CUSTOM in presets)
        assertEquals(MathModule.entries.size, MathModule.entries.map { it.id }.toSet().size)
        assertEquals(MathModule.entries.size, MathModule.entries.map { it.title }.toSet().size)

        val estimation = MathModule.byCategory(ModuleCategory.DIVISION_ESTIMATION)
        val addition = MathModule.byCategory(ModuleCategory.ADD_SUB)
        val multiplication = MathModule.byCategory(ModuleCategory.MULTIPLICATION)
        assertEquals(5, estimation.size)
        assertTrue(estimation.all { it.isEstimation })
        assertEquals(7, addition.size)
        assertEquals(5, multiplication.size)
        assertTrue((addition + multiplication).none { it.isEstimation })
        assertEquals(listOf(MathModule.THREE_DIV_FOUR), presets.filter { it.isPercentAnswer })
    }

    @Test
    fun baifenTablePreservesAllSixteenValuesAndSymbols() {
        val expected = listOf(
            "1/2 = 50%", "1/3 ≈ 33.3%", "1/4 = 25%", "1/5 = 20%",
            "1/6 ≈ 16.7%", "1/7 ≈ 14.3%", "1/8 = 12.5%", "1/9 ≈ 11.1%",
            "1/11 ≈ 9.1%", "1/12 ≈ 8.3%", "1/13 ≈ 7.7%", "1/14 ≈ 7.1%",
            "1/15 ≈ 6.7%", "1/16 = 6.25%", "1/17 ≈ 5.9%", "1/18 ≈ 5.6%",
        )
        assertEquals(expected, BaifenTable.entries.map { "${it.fraction} ${it.symbol} ${it.percent}" })
    }

    @Test
    fun formulaSeedsMatchEveryFieldAndFavoriteFlagInTheSource() {
        val formulas = BaifenTable.defaultFormulas
        assertEquals(30, formulas.size)
        assertEquals(30, formulas.map { it.title }.distinct().size)
        assertEquals(
            mapOf("基期" to 5, "增长率" to 10, "比重" to 4, "平均数" to 4, "其他高频" to 7),
            formulas.groupingBy { it.category.label }.eachCount(),
        )
        assertEquals(15, formulas.count { it.isFavorite })
        assertEquals(
            sourceRows("## 模块 1").map { it.drop(1) },
            formulas.map {
                listOf(it.title, it.category.label, it.expression, it.note, if (it.isFavorite) "是" else "否")
            },
        )
        assertTrue(formulas.none { listOf(it.title, it.expression, it.note).any { text -> '\\' in text || '$' in text } })
        assertEquals(FormulaCategory.CUSTOM, FormulaCategory.fromName("future_category"))
    }

    @Test
    fun bothBaifenTablesMatchTheSourceAndRecalculate() {
        val base = KnowledgeReference.baseBaifen
        val extended = KnowledgeReference.extendedBaifen
        assertEquals((2..20).map { "1/$it" }, base.map { it.fraction })
        assertEquals(30, extended.size)
        assertEquals(sourceRows("### 2.1"), base.map { listOf(it.fraction, it.symbol, it.percent, it.note) })
        assertEquals(sourceRows("### 2.2"), extended.map { (group, data) ->
            listOf(group, data.fraction, data.symbol, data.percent, data.note)
        })
        (base + extended.map { it.reference }).forEach { data ->
            val (numerator, denominator) = data.fraction.split('/').map(::BigDecimal)
            val percentages = Regex("[0-9.]+(?=%)").findAll(data.percent).map { it.value.toBigDecimal() }.toList()
            percentages.forEach { value ->
                val exact = numerator.movePointRight(2)
                val recomputed = exact.divide(denominator, value.scale(), RoundingMode.HALF_UP)
                assertEquals(value, recomputed)
            }
            val exact = (percentages.first() * denominator).compareTo(numerator.movePointRight(2)) == 0
            assertEquals(if (exact) "=" else "≈", data.symbol)
        }
        assertEquals(3, KnowledgeReference.baifenTricks.size)
    }

    @Test
    fun powerMatrixMatchesAllThirtyTwoSourceCellsAndRecalculates() {
        val rows = KnowledgeReference.cagrPowerMatrix
        assertEquals(8, rows.size)
        assertTrue(rows.all { it.powers.size == 4 })
        assertEquals(sourceRows("### 3.3"), rows.map { listOf(it.rate) + it.powers })
        rows.forEach { row ->
            val base = BigDecimal.ONE + row.rate.removeSuffix("%").toBigDecimal().movePointLeft(2)
            row.powers.forEachIndexed { index, power ->
                val exact = base.pow(index + 2)
                Regex("([=≈]) ([0-9.]+)").findAll(power).forEach { match ->
                    val (symbol, number) = match.destructured
                    val value = number.toBigDecimal()
                    val recomputed = if (symbol == "=") exact else exact.setScale(value.scale(), RoundingMode.HALF_UP)
                    assertEquals(0, value.compareTo(recomputed))
                }
            }
        }
        assertEquals(4, KnowledgeReference.cagrCalibers.size)
        assertEquals(3, KnowledgeReference.cagrMethods.size)
    }

    @Test
    fun squaresTruncationAndPitfallsMatchTheSource() {
        val squares = KnowledgeReference.squares
        assertEquals((11..25).toList(), squares.flatten().map { it.base }.sorted())
        assertTrue(squares.flatten().all { it.base * it.base == it.result })
        assertEquals(sourceRows("### 4.1"), squares.map { row -> row.map { "${it.base}² = ${it.result}" } })
        assertEquals(sourceRows("### 4.2"), KnowledgeReference.truncationRules.map {
            listOf(it.title, it.choiceRule, it.choiceGap, it.error, it.appTolerance, it.instruction)
        })
        assertEquals(4, KnowledgeReference.truncationMnemonics.size)
        assertEquals(8, KnowledgeReference.pitfalls.size)
        assertEquals(
            sourceRows("### 4.3").map { it.drop(1) },
            KnowledgeReference.pitfalls.map { listOf(it.title, it.explanation) },
        )
    }

    private fun sourceRows(section: String): List<List<String>> {
        val source = generateSequence(File("").absoluteFile) { it.parentFile }
            .map { File(it, "docs/DATA_ANALYSIS_KNOWLEDGE.md") }
            .first(File::isFile)
        return source.readText().substringAfter(section).substringBefore("\n---")
            .lineSequence().filter { it.startsWith("|") }.drop(2)
            .map { row ->
                row.removeSurrounding("|").split(Regex("""(?<!\\)\|""")).map { field ->
                    field.trim().replace("**", "").replace("`", "").replace("\\|", "|")
                }
            }.toList()
    }
}
