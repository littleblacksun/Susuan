package com.xingce.speedmath.domain.data

import com.xingce.speedmath.domain.model.FormulaCategory

data class BaifenEntry(val fraction: String, val symbol: String, val percent: String)

data class FormulaSeed(
    val title: String,
    val category: FormulaCategory,
    val expression: String,
    val note: String,
    val isFavorite: Boolean,
)

object BaifenTable {
    val entries: List<BaifenEntry> = listOf(
        BaifenEntry("1/2", "=", "50%"),
        BaifenEntry("1/3", "≈", "33.3%"),
        BaifenEntry("1/4", "=", "25%"),
        BaifenEntry("1/5", "=", "20%"),
        BaifenEntry("1/6", "≈", "16.7%"),
        BaifenEntry("1/7", "≈", "14.3%"),
        BaifenEntry("1/8", "=", "12.5%"),
        BaifenEntry("1/9", "≈", "11.1%"),
        BaifenEntry("1/11", "≈", "9.1%"),
        BaifenEntry("1/12", "≈", "8.3%"),
        BaifenEntry("1/13", "≈", "7.7%"),
        BaifenEntry("1/14", "≈", "7.1%"),
        BaifenEntry("1/15", "≈", "6.7%"),
        BaifenEntry("1/16", "=", "6.25%"),
        BaifenEntry("1/17", "≈", "5.9%"),
        BaifenEntry("1/18", "≈", "5.6%"),
    )

    val defaultFormulas: List<FormulaSeed> get() = KnowledgeReference.formulas
}
