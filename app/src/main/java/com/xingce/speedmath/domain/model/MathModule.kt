package com.xingce.speedmath.domain.model

enum class ModuleCategory(val displayName: String) {
    DIVISION_ESTIMATION("除法估算"),
    ADD_SUB("加减基础"),
    MULTIPLICATION("乘法速算"),
}

enum class MathModule(
    val id: String,
    val title: String,
    val sampleExpression: String,
    val category: ModuleCategory?,
    val operatorSymbol: String,
    val isEstimation: Boolean = false,
    val isPercentAnswer: Boolean = false,
) {
    FIVE_DIV_THREE(
        "five_div_three", "五位数除三位数", "54321 ÷ 213",
        ModuleCategory.DIVISION_ESTIMATION, "÷", isEstimation = true,
    ),
    THREE_DIV_FOUR(
        "three_div_four", "三位数除四位数", "534 ÷ 4218 → %",
        ModuleCategory.DIVISION_ESTIMATION, "÷", isEstimation = true, isPercentAnswer = true,
    ),
    THREE_DIV_TWO(
        "three_div_two", "三位数除两位数", "736 ÷ 42",
        ModuleCategory.DIVISION_ESTIMATION, "÷", isEstimation = true,
    ),
    THREE_DIV_ONE(
        "three_div_one", "三位数除一位数", "845 ÷ 7",
        ModuleCategory.DIVISION_ESTIMATION, "÷", isEstimation = true,
    ),
    MUL_ESTIMATION(
        "mul_estimation", "乘法估算", "428 × 19.6%",
        ModuleCategory.DIVISION_ESTIMATION, "×", isEstimation = true,
    ),
    TWO_ADD_SUB("two_add_sub", "两位数加减", "68 + 47", ModuleCategory.ADD_SUB, "±"),
    MAKE_HUNDRED("make_hundred", "凑整百练习", "64 + 36", ModuleCategory.ADD_SUB, "+"),
    THREE_ADD("three_add", "三位数加法", "458 + 376", ModuleCategory.ADD_SUB, "+"),
    THREE_SUB("three_sub", "三位数减法", "842 - 367", ModuleCategory.ADD_SUB, "-"),
    THREE_ADD_SUB("three_add_sub", "三位数加减", "529 + 384", ModuleCategory.ADD_SUB, "±"),
    MULTI_ADD("multi_add", "多数相加", "142 + 238 + 315", ModuleCategory.ADD_SUB, "+"),
    MIXED_ADD_SUB("mixed_add_sub", "混合加减", "426 - 189 + 254", ModuleCategory.ADD_SUB, "±"),
    TWO_MUL_ONE("two_mul_one", "两位数乘一位数", "67 × 8", ModuleCategory.MULTIPLICATION, "×"),
    THREE_MUL_ONE("three_mul_one", "三位数乘一位数", "246 × 7", ModuleCategory.MULTIPLICATION, "×"),
    TWO_MUL_11("two_mul_11", "两位数乘11", "48 × 11", ModuleCategory.MULTIPLICATION, "×"),
    TWO_MUL_15("two_mul_15", "两位数乘15", "64 × 15", ModuleCategory.MULTIPLICATION, "×"),
    TWO_MUL_TWO("two_mul_two", "两位数乘两位数", "36 × 47", ModuleCategory.MULTIPLICATION, "×"),
    CUSTOM("custom", "自定义出题", "自由设位数与误差", null, "");

    companion object {
        val presets: List<MathModule> = entries.filter { it.category != null }

        fun byCategory(category: ModuleCategory): List<MathModule> =
            presets.filter { it.category == category }

        fun fromId(id: String): MathModule {
            val normalizedId = id.trim()
            return entries.firstOrNull { it.id.equals(normalizedId, ignoreCase = true) }
                ?: FIVE_DIV_THREE
        }
    }
}
