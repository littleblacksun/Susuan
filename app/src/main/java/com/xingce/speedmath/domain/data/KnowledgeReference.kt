package com.xingce.speedmath.domain.data

import com.xingce.speedmath.domain.model.FormulaCategory

data class BaifenReference(val fraction: String, val symbol: String, val percent: String, val note: String)

data class ExtendedBaifenReference(val group: String, val reference: BaifenReference)

data class ReferenceArticle(val title: String, val paragraphs: List<String>)

data class CagrCaliber(
    val title: String,
    val example: String,
    val start: String,
    val end: String,
    val yearDifference: String,
    val note: String,
)

data class CagrPowerRow(val rate: String, val powers: List<String>)

data class SquareReference(val base: Int, val result: Int)

data class TruncationRule(
    val title: String,
    val choiceRule: String,
    val choiceGap: String,
    val error: String,
    val appTolerance: String,
    val instruction: String,
)

data class Pitfall(val title: String, val explanation: String)

// Transcribed from docs/DATA_ANALYSIS_KNOWLEDGE.md, with Markdown formatting removed.
object KnowledgeReference {
    val formulas = listOf(
        FormulaSeed(
            "基期量精确公式", FormulaCategory.BASE_PERIOD,
            "基期 = 现期 ÷ (1 + r)",
            "一步除法只截分母；选项差距大截两位，差距小截三位", isFavorite = true,
        ),
        FormulaSeed(
            "基期量化除为乘", FormulaCategory.BASE_PERIOD,
            "基期 ≈ 现期 × (1 − r)（适用 |r| ≤ 5%）",
            "化除为乘误差极小，注意符号变号：分母+r变乘(1−r)", isFavorite = true,
        ),
        FormulaSeed(
            "现期与增长量求基期", FormulaCategory.BASE_PERIOD,
            "基期 = 现期 − 增长量",
            "若为减少量则变加法：基期 = 现期 + 减少量，防看反符号", isFavorite = false,
        ),
        FormulaSeed(
            "基期和差", FormulaCategory.BASE_PERIOD,
            "基期和差 = A ÷ (1 + a) ± B ÷ (1 + b)",
            "先算现期和差排坑，再看增速正负放缩，紧选项分步截位算", isFavorite = false,
        ),
        FormulaSeed(
            "间隔基期量", FormulaCategory.BASE_PERIOD,
            "间隔基期 = 现期 ÷ (1 + R)，R = r₁ + r₂ + r₁ × r₂",
            "先求隔年总增速 R 再直除；|R| ≤ 5% 可继续化除为乘", isFavorite = false,
        ),
        FormulaSeed(
            "一般增长率（现期与基期）", FormulaCategory.GROWTH_RATE,
            "r = (现期 − 基期) ÷ 基期 = 现期 ÷ 基期 − 1",
            "增长超100%时直接算倍数再减1，比先做减法更快不易错", isFavorite = true,
        ),
        FormulaSeed(
            "一般增长率（现期与增量）", FormulaCategory.GROWTH_RATE,
            "r = 增长量 ÷ (现期 − 增长量)",
            "分母是基期不是现期！先减出基期再直除，勿直接除现期", isFavorite = false,
        ),
        FormulaSeed(
            "增长量基本公式", FormulaCategory.GROWTH_RATE,
            "增长量 = 现期 − 基期 = 现期 ÷ (1 + r) × r",
            "比较大小口诀：大大则大，一大一小近似看“现期 × r”", isFavorite = true,
        ),
        FormulaSeed(
            "百化分求增长量（正增长）", FormulaCategory.GROWTH_RATE,
            "r ≈ 1/n（r > 0）时，增长量 ≈ 现期 ÷ (n + 1)",
            "正增长加一除；r 放大则算出的增长量偏大，选略小项", isFavorite = true,
        ),
        FormulaSeed(
            "百化分求减少量（负增长）", FormulaCategory.GROWTH_RATE,
            "|r| ≈ 1/n（r < 0）时，减少量 ≈ 现期 ÷ (n − 1)",
            "负增长减一除！最易误用 n+1；|r| 放大则结果偏大", isFavorite = true,
        ),
        FormulaSeed(
            "间隔（隔年）增长率", FormulaCategory.GROWTH_RATE,
            "R = r₁ + r₂ + r₁ × r₂",
            "隔两年才用；两增速绝对值均<10%时，乘积项常可秒估", isFavorite = true,
        ),
        FormulaSeed(
            "混合增长率（居中+线段法）", FormulaCategory.GROWTH_RATE,
            "r小 < R < r大；A₀ ÷ B₀ = (r大 − R) ÷ (R − r小)",
            "整体居中偏大基期；距离与基期成反比，常用现期近似替代", isFavorite = true,
        ),
        FormulaSeed(
            "乘积增长率", FormulaCategory.GROWTH_RATE,
            "若 S = A × B，则 r_S = a + b + a × b",
            "总额=单价×数量、总产=亩产×面积等乘积关系直接套用", isFavorite = true,
        ),
        FormulaSeed(
            "年均增长量", FormulaCategory.GROWTH_RATE,
            "年均增长量 = (末期 − 初期) ÷ n",
            "国考普通年份 n=末−初；五年规划期及江苏省考常前推1年", isFavorite = false,
        ),
        FormulaSeed(
            "年均增长率（精确与上限）", FormulaCategory.GROWTH_RATE,
            "(1 + r)ⁿ = 末期 ÷ 初期；上限 r < (末期 ÷ 初期 − 1) ÷ n",
            "正增长时上限估算必偏大，选略小项；选项紧用居中代入", isFavorite = true,
        ),
        FormulaSeed(
            "现期比重", FormulaCategory.PROPORTION,
            "现期比重 = 部分 A ÷ 整体 B",
            "“占”字前为分子、后为分母；比重必在 0%~100% 之间", isFavorite = false,
        ),
        FormulaSeed(
            "基期比重", FormulaCategory.PROPORTION,
            "基期比重 = (A ÷ B) × (1 + b) ÷ (1 + a)",
            "先算现期比重 A/B，再看 (1+b)/(1+a) 大于或小于1定方向", isFavorite = true,
        ),
        FormulaSeed(
            "两期比重差与升降判定", FormulaCategory.PROPORTION,
            "比重差 = (A ÷ B) × (a − b) ÷ (1 + a)",
            "a > b 比重上升，a < b 下降；结果单位为“百分点”非%", isFavorite = true,
        ),
        FormulaSeed(
            "两期比重变化大小快捷法", FormulaCategory.PROPORTION,
            "a > 0 且 A < B 时，|比重差| < |a − b|",
            "先定升降排两项，再用小于 |a−b| 秒杀；a < 0 时慎用", isFavorite = true,
        ),
        FormulaSeed(
            "现期平均数", FormulaCategory.AVERAGE,
            "现期平均数 = 总量 A ÷ 份数 B",
            "“每/均/单位”后作分母（后除前），务必先统一量级单位", isFavorite = false,
        ),
        FormulaSeed(
            "基期平均数", FormulaCategory.AVERAGE,
            "基期平均数 = (A ÷ B) × (1 + b) ÷ (1 + a)",
            "结构同基期比重，先截位算现期平均数再根据增速比放缩", isFavorite = false,
        ),
        FormulaSeed(
            "两期平均数变化量", FormulaCategory.AVERAGE,
            "平均数变化量 = (A ÷ B) × (a − b) ÷ (1 + a)",
            "结果带具体单位（如元/吨），区别于两期比重差的百分点", isFavorite = false,
        ),
        FormulaSeed(
            "平均数增长率", FormulaCategory.AVERAGE,
            "r_平均 = (a − b) ÷ (1 + b)",
            "分母是 1+b（非 1+a）且结果是%，区别于两期比重差！", isFavorite = true,
        ),
        FormulaSeed(
            "现期倍数", FormulaCategory.OTHER_HIGH_FREQ,
            "现期倍数 = A ÷ B",
            "“A 是 B 的几倍”直接相除；注意问题是否含“约/多”字", isFavorite = false,
        ),
        FormulaSeed(
            "基期倍数", FormulaCategory.OTHER_HIGH_FREQ,
            "基期倍数 = (A ÷ B) × (1 + b) ÷ (1 + a)",
            "与基期比重/平均数同构，先算 A/B 再乘 (1+b)/(1+a)", isFavorite = false,
        ),
        FormulaSeed(
            "是几倍 vs 多几倍", FormulaCategory.OTHER_HIGH_FREQ,
            "多几倍 = 增长率 = A ÷ B − 1 = 是几倍 − 1",
            "“多几倍”本质即增长率，算出“是几倍”后切记减 1！", isFavorite = true,
        ),
        FormulaSeed(
            "翻番公式", FormulaCategory.OTHER_HIGH_FREQ,
            "翻 n 番 = 原值 × 2ⁿ（翻1番=×2，翻2番=×4，翻3番=×8）",
            "翻番是 2 的幂次不是倍数：翻 2 番是 4 倍，非 2 倍！", isFavorite = false,
        ),
        FormulaSeed(
            "拉动增长率", FormulaCategory.OTHER_HIGH_FREQ,
            "拉动增长率 = 部分增长量 ÷ 整体基期量 × 100%",
            "单位为“百分点”；各部分拉动增长率之和等于整体增速", isFavorite = false,
        ),
        FormulaSeed(
            "增长贡献率", FormulaCategory.OTHER_HIGH_FREQ,
            "增长贡献率 = 部分增长量 ÷ 整体增长量 × 100%",
            "分母是整体增长量（非基期），各部分贡献率之和为 100%", isFavorite = false,
        ),
        FormulaSeed(
            "顺差与逆差", FormulaCategory.OTHER_HIGH_FREQ,
            "顺差 = 出口 − 进口；逆差 = 进口 − 出口",
            "出口>进口为顺差；进出口总额=出口+进口，易混减法方向", isFavorite = false,
        ),
    )

    val baseBaifen = listOf(
        BaifenReference("1/2", "=", "50%", "1/20 = 5%、1/200 = 0.5%、1/0.2 = 500%"),
        BaifenReference("1/3", "≈", "33.3%（33.33%）", "1/30 ≈ 3.33%、1/300 ≈ 0.33%"),
        BaifenReference("1/4", "=", "25%", "1/40 = 2.5%、1/400 = 0.25%"),
        BaifenReference("1/5", "=", "20%", "1/50 = 2%、1/500 = 0.2%"),
        BaifenReference("1/6", "≈", "16.7%（16.67%）", "1/60 ≈ 1.67%、1/600 ≈ 0.17%"),
        BaifenReference("1/7", "≈", "14.3%（14.29%）", "1/70 ≈ 1.43%、1/700 ≈ 0.14%"),
        BaifenReference("1/8", "=", "12.5%", "1/80 = 1.25%、1/800 = 0.125%"),
        BaifenReference("1/9", "≈", "11.1%（11.11%）", "1/90 ≈ 1.11%；与 1/11 互换记忆（9↔11）"),
        BaifenReference("1/10", "=", "10%", "1/100 = 1%、1/1000 = 0.1%"),
        BaifenReference("1/11", "≈", "9.1%（9.09%）", "1/110 ≈ 0.91%；与 1/9 互换记忆（11↔9）"),
        BaifenReference("1/12", "≈", "8.3%（8.33%）", "1/120 ≈ 0.83%、1/1.2 ≈ 83.3%（即 5/6）"),
        BaifenReference("1/13", "≈", "7.7%（7.69%）", "1/130 ≈ 0.77%、1/1.3 ≈ 76.9%"),
        BaifenReference("1/14", "≈", "7.1%（7.14%）", "1/140 ≈ 0.71%、1/1.4 ≈ 71.4%（即 5/7）"),
        BaifenReference("1/15", "≈", "6.7%（6.67%）", "1/150 ≈ 0.67%、1/1.5 ≈ 66.7%（即 2/3）"),
        BaifenReference("1/16", "=", "6.25%", "1/160 = 0.625%、1/1.6 = 62.5%（即 5/8）"),
        BaifenReference("1/17", "≈", "5.9%（5.88%）", "1/170 ≈ 0.59%、1/1.7 ≈ 58.8%"),
        BaifenReference("1/18", "≈", "5.6%（5.56%）", "1/180 ≈ 0.56%、1/1.8 ≈ 55.6%（即 5/9）"),
        BaifenReference("1/19", "≈", "5.3%（5.26%）", "1/190 ≈ 0.53%、1/1.9 ≈ 52.6%"),
        BaifenReference("1/20", "=", "5%", "1/200 = 0.5%、1/0.2 = 500%"),
    )

    const val baifenShiftRule = "分母小数点右移一位，百分数左移一位（缩小 10 倍）；分母左移一位，百分数右移一位（扩大 10 倍）。例如 1/16 = 6.25% ↔ 1/1.6 = 62.5% ↔ 1/160 = 0.625%。"

    val extendedBaifen = listOf(
        ExtendedBaifenReference("延伸小分数", BaifenReference("1/21", "≈", "4.8%（4.76%）", "1/2.1 ≈ 47.6%（1/7 除以 3）")),
        ExtendedBaifenReference("延伸小分数", BaifenReference("1/22", "≈", "4.5%（4.55%）", "1/2.2 ≈ 45.5%（1/11 一半）")),
        ExtendedBaifenReference("延伸小分数", BaifenReference("1/23", "≈", "4.3%（4.35%）", "1/2.3 ≈ 43.5%")),
        ExtendedBaifenReference("延伸小分数", BaifenReference("1/24", "≈", "4.2%（4.17%）", "1/2.4 ≈ 41.7%（即 5/12，极高频！）")),
        ExtendedBaifenReference("延伸小分数", BaifenReference("1/25", "=", "4%", "1/2.5 = 40%（即 2/5）、1/250 = 0.4%")),
        ExtendedBaifenReference("延伸小分数", BaifenReference("1/30", "≈", "3.3%（3.33%）", "1/3 缩小 10 倍")),
        ExtendedBaifenReference("常规高频", BaifenReference("2/3", "≈", "66.7%（66.67%）", "= 1/1.5，正增长量 ≈ 现期 × 2 ÷ 5")),
        ExtendedBaifenReference("常规高频", BaifenReference("3/4", "=", "75%", "正增长量 = 现期 × 3 ÷ 7")),
        ExtendedBaifenReference("五分系", BaifenReference("2/5", "=", "40%", "= 1/2.5，正增长量 = 现期 × 2 ÷ 7")),
        ExtendedBaifenReference("五分系", BaifenReference("3/5", "=", "60%", "正增长量 = 现期 × 3 ÷ 8")),
        ExtendedBaifenReference("五分系", BaifenReference("4/5", "=", "80%", "= 1/1.25，正增长量 = 现期 × 4 ÷ 9")),
        ExtendedBaifenReference("六分系", BaifenReference("5/6", "≈", "83.3%（83.33%）", "= 1/1.2，正增长量 ≈ 现期 × 5 ÷ 11")),
        ExtendedBaifenReference("八分系", BaifenReference("3/8", "=", "37.5%", "每增加 1/8 加 12.5%；正增量 × 3 ÷ 11")),
        ExtendedBaifenReference("八分系", BaifenReference("5/8", "=", "62.5%", "= 1/1.6；正增量 × 5 ÷ 13")),
        ExtendedBaifenReference("八分系", BaifenReference("7/8", "=", "87.5%", "= 1 − 1/8；正增量 × 7 ÷ 15")),
        ExtendedBaifenReference("七分系（走马灯）", BaifenReference("2/7", "≈", "28.6%（28.57%）", "142857 循环排序：2/7 → 28.57%")),
        ExtendedBaifenReference("七分系（走马灯）", BaifenReference("3/7", "≈", "42.9%（42.86%）", "142857 循环排序：3/7 → 42.86%")),
        ExtendedBaifenReference("七分系（走马灯）", BaifenReference("4/7", "≈", "57.1%（57.14%）", "142857 循环排序：4/7 → 57.14%")),
        ExtendedBaifenReference("七分系（走马灯）", BaifenReference("5/7", "≈", "71.4%（71.43%）", "= 1/1.4，142857 循环排序")),
        ExtendedBaifenReference("七分系（走马灯）", BaifenReference("6/7", "≈", "85.7%（85.71%）", "142857 循环排序：6/7 → 85.71%")),
        ExtendedBaifenReference("九分系", BaifenReference("2/9", "≈", "22.2%（22.22%）", "规律：m/9 ≈ 11.11% × m（叠数）")),
        ExtendedBaifenReference("九分系", BaifenReference("4/9", "≈", "44.4%（44.44%）", "= 4 × (1/9)，叠数 44.44%")),
        ExtendedBaifenReference("九分系", BaifenReference("5/9", "≈", "55.6%（55.56%）", "= 1/1.8 = 5 × (1/9)，约 55.56%")),
        ExtendedBaifenReference("九分系", BaifenReference("7/9", "≈", "77.8%（77.78%）", "= 7 × (1/9)，约 77.78%")),
        ExtendedBaifenReference("十一分系", BaifenReference("2/11", "≈", "18.2%（18.18%）", "规律：m/11 ≈ 9.09% × m（9 的倍数循环）")),
        ExtendedBaifenReference("十一分系", BaifenReference("3/11", "≈", "27.3%（27.27%）", "3 × 9.09% = 27.27%")),
        ExtendedBaifenReference("十一分系", BaifenReference("4/11", "≈", "36.4%（36.36%）", "4 × 9.09% = 36.36%")),
        ExtendedBaifenReference("十二分系", BaifenReference("5/12", "≈", "41.7%（41.67%）", "= 1/2.4，考场最高频的 40%+ 过渡分数")),
        ExtendedBaifenReference("十二分系", BaifenReference("7/12", "≈", "58.3%（58.33%）", "= 50% + 8.33%（即 1/2 + 1/12）")),
        ExtendedBaifenReference("十二分系", BaifenReference("11/12", "≈", "91.7%（91.67%）", "= 100% − 8.33%（即 1 − 1/12）")),
    )

    const val baifenGrowthRule = "若 r ≈ m/k，正增长量 ≈ 现期 × m ÷ (k + m)；负增长减少量 ≈ 现期 × m ÷ (k − m)。例如 r ≈ 37.5% = 3/8，正增长量 ≈ 现期 × 3 ÷ 11。"

    val baifenTricks = listOf(
        ReferenceArticle(
            "中值法（夹在两个相邻分数正中 → 取 .5）",
            listOf(
                "原理：若百分数恰好落在 1/k 与 1/(k + 1) 之间，直接取 n = k + 0.5。",
                "实战 13.4%：介于 1/7 ≈ 14.3% 与 1/8 = 12.5% 正中间 → 取 n = 7.5，即 13.4% ≈ 1/7.5 = 2/15（精确 1/7.5 ≈ 13.33%，相对误差仅 −0.5%）；求正增长量直接算 现期 ÷ 8.5（或 现期 × 2 ÷ 17）。",
            ),
        ),
        ReferenceArticle(
            "倒数反推 + 放缩误差修正法（n = 100 ÷ r%）",
            listOf(
                "原理：用 100 ÷ r 心算小数型 n，或先近似为最近标准分数再按单调性修正方向。",
                "单调性铁律：正增长时 r ÷ (1 + r) 随 r 增大而增大；负增长时 |r| ÷ (1 − |r|) 随 |r| 增大而增大。因此无论正负增长，把 |r| 放大，算出的增长量/减少量都偏大（应往略小选）；把 |r| 缩小，算出结果偏小（应往略大选）。",
                "实战 17.2%：100 ÷ 17.2 ≈ 5.81 → 精准取 1/5.8 ≈ 17.24%（误差 +0.2%），正增长量 ≈ 现期 ÷ 6.8；选项差距大时直接按 1/6 ≈ 16.7% 算 现期 ÷ 7，因 16.7% < 17.2%，选略大于 现期 ÷ 7 的选项。",
                "实战 23.5%：100 ÷ 23.5 ≈ 4.26 → 精准取 1/4.25 = 4/17 ≈ 23.53%（误差 +0.1%），正增长量 ≈ 现期 ÷ 5.25；若图快先按 1/4 = 25% 算 现期 ÷ 5，因 25% > 23.5%，选略小于 现期 ÷ 5 的选项。",
            ),
        ),
        ReferenceArticle(
            "拆两项 / 移位联想转化法",
            listOf(
                "原理：大于 30% 的非标准百分数优先联想 1/2.x（由 1/2x 右移小数点得到）或拆成两个整百化分之和/差。",
                "实战 41.5%：",
                "移位法：由 1/24 ≈ 4.17% 瞬间联想 1/2.4 = 5/12 ≈ 41.67%（误差 +0.4%），求正增长量 ≈ 现期 ÷ 3.4（或 现期 × 5 ÷ 17）；",
                "拆两项法：算乘法 A × 41.5% 时拆成 40% + 1.5%（A × 0.4 + A × 0.015）或 50% − 8.5%，心算极快。",
            ),
        ),
    )

    const val cagrFormula = "(1 + r)ⁿ = 末期量 ÷ 初期量，等价于 r = (末期量 ÷ 初期量)^(1/n) − 1"

    val cagrCalibers = listOf(
        CagrCaliber(
            "国考 / 联考 / 浙江常规", "2020—2024 年年均增长率",
            "2020 年", "2024 年", "2024 − 2020 = 4", "普通起止年份直接“末减初”，不前推",
        ),
        CagrCaliber(
            "五年规划期（国考 & 多数省考）", "“十三五”（2016—2020）/“十四五”（2021—2025）期间",
            "前推 1 年：2015 年 / 2020 年", "2020 年 / 2025 年", "5", "国家统计局标准口径；仅当材料完全未给前一年数据时才退化为 n = 4",
        ),
        CagrCaliber(
            "江苏省考（特色“翻旧账”）", "2020—2024 年年均增长率/量",
            "通常前推 1 年：2019 年", "2024 年", "5（末−初+1）", "江苏独有高频习惯！务必先看材料是否给出前一年数据，给了即前推",
        ),
        CagrCaliber(
            "含“年初—年末”表述", "2021 年初至 2024 年末",
            "2020 年末（=2021 年初）", "2024 年末", "2024 − 2020 = 4", "“某年初”等价于“上一年末”，勿误算成 3 年",
        ),
    )

    const val cagrPeriodWarning = "材料若注明“扣除价格因素（实际增速）”或“统计口径调整（如规上企业门槛变更）”，切忌用现价绝对值或跨口径数据硬套 (1 + r)ⁿ。"

    val cagrMethods = listOf(
        ReferenceArticle(
            "方法一：上限估算公式",
            listOf(
                "公式：r < (末期 ÷ 初期 − 1) ÷ n",
                "误差方向与适用场景：",
                "由伯努利不等式，只要 n ≥ 2 且 r ≠ 0（r > −1），恒有 (1 + r)ⁿ > 1 + n × r。",
                "正增长（r > 0）：算出的 (末期 ÷ 初期 − 1) ÷ n 严格偏大（即真实 r 的上限），必选略小于估算值的选项；当 r ≤ 5% 时非常精确可直接秒杀，当 r > 10% 时偏大显著，仅用于先排偏大项。",
                "负增长（r < 0）：不等式仍成立（如真实 r = −10%、n = 4 时，上限估算为 (0.6561 − 1) ÷ 4 ≈ −8.6% > −10%），即估算出的降幅偏小，应选降幅更大（更负）的选项。",
            ),
        ),
        ReferenceArticle(
            "方法二：二项式展开估算（适用于 r 在 5% ~ 20%）",
            listOf(
                "通用公式：(1 + r)ⁿ ≈ 1 + n × r + [n × (n − 1) ÷ 2] × r²",
                "高频展开速记：",
                "n = 2：(1 + r)² = 1 + 2r + r²（100% 精确）",
                "n = 3：(1 + r)³ ≈ 1 + 3r + 3r²（舍去 r³）",
                "n = 4：(1 + r)⁴ ≈ 1 + 4r + 6r²（舍去 4r³ + r⁴）",
                "n = 5：(1 + r)⁵ ≈ 1 + 5r + 10r²（舍去 10r³ + 5r⁴ + r⁵）",
                "误差方向（r > 0 且 n ≥ 3）：因舍去了正的高次项，二项式展开值略小于真实的 (1 + r)ⁿ。故代入某选项 r₀ 后，若展开值已 ≥ 末期 ÷ 初期，则真实 (1 + r₀)ⁿ 必更大，说明真实 r < r₀，直接往小选！",
            ),
        ),
        ReferenceArticle(
            "方法三：选项居中代入法（最稳实战）",
            listOf(
                "将 4 个选项按升序排列，优先挑选居中且好算的整数/整五百分数（如 10%、15%、20%、25%）直接对照下方幂次表或代入二项式，与 末期 ÷ 初期 比较大小，通常 1 次计算即可锁定唯一答案。",
            ),
        ),
    )

    val cagrPowerMatrix = listOf(
        CagrPowerRow("5%", listOf("= 1.1025（≈ 1.103）", "≈ 1.158", "≈ 1.216", "≈ 1.276")),
        CagrPowerRow("8%", listOf("= 1.1664（≈ 1.166）", "≈ 1.260", "≈ 1.360", "≈ 1.469")),
        CagrPowerRow("10%", listOf("= 1.21", "= 1.331", "≈ 1.464（= 1.4641）", "≈ 1.611")),
        CagrPowerRow("12%", listOf("= 1.2544（≈ 1.254）", "≈ 1.405", "≈ 1.574", "≈ 1.762")),
        CagrPowerRow("15%", listOf("= 1.3225（≈ 1.323）", "≈ 1.521", "≈ 1.749", "≈ 2.011")),
        CagrPowerRow("20%", listOf("= 1.44", "= 1.728", "≈ 2.074（= 2.0736）", "≈ 2.488")),
        CagrPowerRow("25%", listOf("= 1.5625（≈ 1.563）", "≈ 1.953", "≈ 2.441", "≈ 3.052")),
        CagrPowerRow("30%", listOf("= 1.69", "= 2.197", "≈ 2.856（= 2.8561）", "≈ 3.713")),
    )

    const val cagrDoublingAnchors = "1.15⁵ ≈ 2.011（15% 增速 5 年翻一倍）、1.20⁴ ≈ 2.074（20% 增速 4 年略超两倍）、1.25³ ≈ 1.953（25% 增速 3 年近两倍）、1.25⁵ ≈ 3.052（25% 增速 5 年略超三倍）。"

    val squares = listOf(
        listOf(SquareReference(11, 121), SquareReference(16, 256), SquareReference(21, 441)),
        listOf(SquareReference(12, 144), SquareReference(17, 289), SquareReference(22, 484)),
        listOf(SquareReference(13, 169), SquareReference(18, 324), SquareReference(23, 529)),
        listOf(SquareReference(14, 196), SquareReference(19, 361), SquareReference(24, 576)),
        listOf(SquareReference(15, 225), SquareReference(20, 400), SquareReference(25, 625)),
    )

    const val squareUsage = "快速计算 n = 2 的年均增长率 (1 + r)²（如 1.17² = 1.3689）、间隔/乘积增长率乘积项及平方差估算。"

    const val squareMnemonic = "尾数为 5 的两位数平方 = 十位 × (十位 + 1) 后拼 25（如 25² → 2×3 = 6 → 625）；相邻平方差：n² − (n − 1)² = 2n − 1。"

    val truncationRules = listOf(
        TruncationRule(
            "截两位（左二截）", "选项首位各不相同；或首位相同但最接近两项次位差 > 首位",
            "> 10%", "一般 ±1% ~ ±3%，极端最坏约 ±4.8%",
            "宽松 ±5%（大差距快估） / 实战 ±2%（中大差距）", "一步除法只截分母；分母第三位四舍五入，直除商首位或前两位即选",
        ),
        TruncationRule(
            "截三位（留三位）", "选项首位相同，且最接近两项次位差 ≤ 首位",
            "2% ~ 10%", "单步最大 ±0.5%，多步叠加通常 ≤ ±1%",
            "实战 ±2%（常规考场主力） / 精算 ±1%（紧密选项）", "分母第四位四舍五入；多步乘除上下同截三位并先约分",
        ),
        TruncationRule(
            "截四位 / 精确直除", "选项前两位完全相同，或处于临界比较题",
            "< 2%", "< ±0.1%",
            "精算 ±1% / 精确 0%", "不可盲目截两位，直接保留四位或精确列竖式/差分法比较",
        ),
    )

    val truncationMnemonics = listOf(
        "看选项定截位：差得远截两位，挨得近截三位，前二同精算。",
        "定对象：一步除法只截分母（分子不动）；多步乘除分子分母同时截位、先约分再除。",
        "定误差方向：分母放大，商偏小（选略大）；分母缩小，商偏大（选略小）。",
        "定量级：若选项呈 10 倍、100 倍差距，切记同步估算小数点与单位换算（亿/万/千、元/万元）。",
    )

    val pitfalls = listOf(
        Pitfall(
            "百分数（%） vs 百分点",
            "百分数表示相对比例或增速；百分点用于表示两个百分数直接作差（如增速从 8% 升至 10%，上升 2 个百分点，不是上升 2%）。",
        ),
        Pitfall(
            "同比 vs 环比",
            "同比 = 与上年同期比（防周期季节波动）；环比 = 与紧邻上一统计周期比（特别注意：1 月环比看上年 12 月，一季度环比看上年四季度）。",
        ),
        Pitfall(
            "顺差 vs 逆差",
            "出口 > 进口为贸易顺差（顺差 = 出口 − 进口）；进口 > 出口为贸易逆差（逆差 = 进口 − 出口）；进出口总额 = 出口 + 进口。",
        ),
        Pitfall(
            "成数 vs 翻番",
            "成数：一成 = 10%，三成五 = 35%；翻番：翻 n 番 = 原值 × 2ⁿ（翻一番是 2 倍，翻两番是 4 倍，切忌把翻两番当成 2 倍）。",
        ),
        Pitfall(
            "累计 vs 当月",
            "统计公报常给“1—N 月累计值”，求“N 月当月值”需用 (1—N 月累计) − (1—(N−1) 月累计)；累计增速是各月混合增速，非当月增速。",
        ),
        Pitfall(
            "增长率 vs 降幅 vs 变化幅度",
            "增长率/增速/增幅带正负号比大小（−2% > −5%）；降幅前提为下降，比较降幅大小看绝对值（下降 5% 的降幅大于下降 2%）；变化幅度不论涨跌一律比 |r|。",
        ),
        Pitfall(
            "增长贡献率 vs 拉动增长率",
            "分子均为“部分增长量”；贡献率除以“整体增长量”（单位 %），拉动增长率除以“整体基期量”（单位 百分点）。",
        ),
        Pitfall(
            "恩格尔系数 vs 基尼系数",
            "恩格尔系数 = 食品支出 ÷ 消费总支出（越低越富裕）；基尼系数衡量收入分配差距（0 ~ 1，越高越悬殊，国际警戒线 0.4）。",
        ),
    )
}
