package com.xingce.speedmath.domain.model

enum class KeypadOrder(val label: String) {
    ASCENDING("正序"),
    DESCENDING("倒序"),
    SHUFFLED("乱序"),
}

enum class NBackLevel(val label: String, val steps: Int) {
    OFF("关闭", 0),
    ONE("1-back", 1),
    TWO("2-back", 2),
}

enum class CustomOperation(val label: String) {
    ADD("加法"),
    SUB("减法"),
    MUL("乘法"),
    DIV("除法"),
    MIXED_ADD_SUB("混合加减"),
}

data class CustomQuizConfig(
    val operation: CustomOperation = CustomOperation.DIV,
    val firstDigits: Int = 5,
    val secondDigits: Int = 3,
    val allowNegative: Boolean = false,
    val percentAnswer: Boolean = false,
    val tolerance: ToleranceLevel = ToleranceLevel.STANDARD,
) {
    fun normalized(): CustomQuizConfig = copy(
        firstDigits = firstDigits.coerceIn(1, 5),
        secondDigits = secondDigits.coerceIn(1, 4),
    )
}

enum class FormulaCategory(val label: String) {
    GROWTH_RATE("增长率"),
    BASE_PERIOD("基期"),
    PROPORTION("比重"),
    AVERAGE("平均数"),
    OTHER_HIGH_FREQ("其他高频"),
    CUSTOM("自定义");

    companion object {
        fun fromName(name: String): FormulaCategory =
            entries.firstOrNull { it.name == name } ?: CUSTOM
    }
}
