package com.xingce.speedmath.domain.model

enum class ToleranceLevel(val label: String, val shortLabel: String, val ratio: Double) {
    EXACT("精确(0%)", "精确", 0.0),
    STRICT("精算 ±1%", "±1%", 0.01),
    STANDARD("实战 ±2%", "±2%", 0.02),
    RELAXED("宽松 ±5%", "±5%", 0.05);

    companion object {
        val estimationOptions: List<ToleranceLevel> = listOf(STRICT, STANDARD, RELAXED)

        fun fromName(name: String): ToleranceLevel {
            val normalizedName = name.trim()
            return entries.firstOrNull { it.name.equals(normalizedName, ignoreCase = true) }
                ?: STANDARD
        }
    }
}
