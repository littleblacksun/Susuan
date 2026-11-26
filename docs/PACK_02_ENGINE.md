# 任务包 Pack 2：核心算术引擎（17 题型 + 自定义出题 + 相对误差判题 + N-back 状态机）

> **所属规划**：`docs/MASTER_PLAN.md` — Pack 2 / 6  
> **工作目录**：`/home/bubu/资料分析apk`  
> **前置基础**：Pack 1 已通过审查（`domain/model/MathModule.kt`、`ToleranceLevel.kt`、`PracticeConfig.kt` 已就位）  
> **本包边界**：本包专注 **纯 Kotlin `domain/` 核心引擎层（零 Android Framework 依赖）+ 全覆盖 JVM 单元测试**。**严禁**在本包提前修改 `ui/` 业务页面或实现导出模块；同时保持 Pack 1 已有公开模型签名兼容（不破坏 Pack 1 单测）。

---

## 一、新增 Domain 模型（`app/src/main/java/com/xingce/speedmath/domain/model/`）

### 1. `Question.kt`
```kotlin
data class Question(
    val index: Int,                       // 0-based 题号
    val module: MathModule,
    val expression: String,               // 展示算式，如 "54321 ÷ 213"、"534 ÷ 4218"、"64 + ? = 100"
    val exactValue: BigDecimal,           // 精确数值；当 isPercentAnswer=true 时直接存百分数数值（如 12.6600... 对应 12.66%）
    val exactDisplay: String,             // 规范化展示参考答案（整数如 "115"，小数/估算保留1位小数如 "255.0" 或 "12.7"，不含 % 号）
    val isEstimation: Boolean,            // true=估算（相对误差判题）；false=精确匹配
    val isPercentAnswer: Boolean,         // true=作答与展示带 % 后缀（如三位数除四位数）
    val tolerance: ToleranceLevel,        // 本题生效的容差档位
    val terms: List<String> = emptyList(),// 多数相加/混合加减的拆项列表，便于 Pack 4 多行优雅排版
)
```

### 2. `GradeResult.kt`
```kotlin
enum class GradeStatus { EXACT, PASS, FAIL }

data class GradeResult(
    val status: GradeStatus,
    val isCorrect: Boolean,               // EXACT 或 PASS 为 true，FAIL 为 false
    val normalizedUserInput: String,      // 清洗后的用户输入字符串
    val parsedUserValue: BigDecimal?,     // 解析失败或未作答时为 null
    val exactDisplay: String,             // 与 Question.exactDisplay 一致
    val signedErrorPercent: Double?,      // 估算且成功解析时为带符号百分比（HALF_UP 保留 1 位小数，如 +1.0 / -0.8 / 0.0，避免 -0.0）；精确题或无效输入为 null（精确命中可为 0.0）
    val feedbackText: String,             // 简洁中文反馈，供答题页状态胶囊直接展示
)
```

### 3. `PracticeSummary.kt`
- 定义单题已作答记录 `AnsweredItem(val question: Question, val grade: GradeResult, val durationMs: Long)`。
- 定义汇总结果 `PracticeSummary(val totalCount: Int, val correctCount: Int, val accuracyPercent: Int, val totalDurationMs: Long, val averageDurationSec: Double, val averageAbsErrorPercent: Double?)`。

---

## 二、核心出题引擎（`domain/engine/QuestionGenerator.kt`）

设计纯 Kotlin 类 `class QuestionGenerator(private val random: Random = Random.Default)`：
- 对外核心方法：
  - `fun generateBatch(module: MathModule, count: Int, tolerance: ToleranceLevel, customConfig: CustomQuizConfig = CustomQuizConfig()): List<Question>`
  - `fun generateOne(index: Int, module: MathModule, tolerance: ToleranceLevel, customConfig: CustomQuizConfig = CustomQuizConfig()): Question`
- **通用出题质量约束**：
  1. `count` 安全约束至少 `1` 题；同一批次内相邻两题 `expression` 不得完全相同（最多重试 8 次去重）。
  2. 精确题型（`!module.isEstimation` 且非 `CUSTOM`）强制生效 `tolerance = ToleranceLevel.EXACT`、`isEstimation = false`；估算题型（`module.isEstimation`）若传入 `ToleranceLevel.EXACT` 则兜底使用 `ToleranceLevel.STANDARD`（±2%），确保行为与 UI 一致。
  3. 当 `module == MathModule.CUSTOM` 时，完全按 `customConfig.normalized()` 决定：`isEstimation = (customConfig.tolerance != ToleranceLevel.EXACT)`、`tolerance = customConfig.tolerance`。
  4. **除法内部精度**：全部用 `BigDecimal.divide(..., 8, RoundingMode.HALF_UP)` 计算 `exactValue`，避免 `ArithmeticException: Non-terminating decimal expansion`。
  5. **符号规范**：算式统一使用清晰数学符号：`+`、`-`（或 `−`，统一在所有题型保持一致，推荐半角 `+` / `-` 配两侧空格，乘除用 `×` / `÷`）。

### 全部 17 种预设题型 + 自定义题型的精确数学规格

| 序号 | 枚举 | 数值范围与行测实战约束 | 精确/估算 | 答案格式 |
| :---: | :--- | :--- | :---: | :--- |
| 1 | `FIVE_DIV_THREE` | 被除数 `10000..99999`，除数 `101..999`（≥80% 避开整百/整十） | 估算 | 数值（`exactDisplay` 保留 1 位小数，如 `"255.0"`） |
| 2 | `THREE_DIV_FOUR` | 被除数 `101..999`，除数 `1001..9999`，且保证商占比落在 `3.0%..95.0%`；`exactValue = (a / b) * 100` | 估算 | **百分比 `%`**（`isPercentAnswer = true`，如 `"12.7"`） |
| 3 | `THREE_DIV_TWO` | 被除数 `101..999`，除数 `12..99`（尽量避开 `20, 50` 等平凡数） | 估算 | 数值（保留 1 位小数） |
| 4 | `THREE_DIV_ONE` | 被除数 `101..999`，除数 `3..9`（禁止 `1, 2`） | 估算 | 数值（整除显示整数，非整除保留 1 位小数） |
| 5 | `MUL_ESTIMATION` | 60% 为行测部分量型：`150..950 × 11.2%..89.8%`（1位小数百分数，如 `428 × 19.6%`，真实值 `a * p / 100`）；40% 为大数放缩型：`112..989 × 18..98`（避开整十） | 估算 | 数值（保留 1 位小数；整数则直接显示整数） |
| 6 | `TWO_ADD_SUB` | 两位数 `11..99` 加或减（各 50%），减法确保 `a >= b`（非负） | 精确 | 整数 |
| 7 | `MAKE_HUNDRED` | 凑整百补数训练：60% 目标 `100`（如 `64 + ? = 100`，答案 `36`），40% 目标 `200..900` 整百（如 `264 + ? = 300`，答案 `36`），加数末尾不为 `0` 且补数在 `1..99` | 精确 | 整数 |
| 8 | `THREE_ADD` | `111..989 + 111..989` | 精确 | 整数 |
| 9 | `THREE_SUB` | `200..999 - 101..(a - 11)`，保证结果 `> 10` | 精确 | 整数 |
| 10 | `THREE_ADD_SUB` | 随机 50% 三位数加法、50% 三位数减法（结果非负） | 精确 | 整数 |
| 11 | `MULTI_ADD` | 4 项（70%）或 5 项（30%）三位数（`105..985`）连加，填充 `terms` | 精确 | 整数 |
| 12 | `MIXED_ADD_SUB` | 3 项或 4 项两/三位数（`101..699` / `21..99`）混合加减，至少含 1 个 `+` 和 1 个 `-`，保证最终结果 `>= 0`，填充 `terms` | 精确 | 整数 |
| 13 | `TWO_MUL_ONE` | `12..99 × 3..9`（避开 `1, 2` 及整十） | 精确 | 整数 |
| 14 | `THREE_MUL_ONE` | `105..998 × 3..9` | 精确 | 整数 |
| 15 | `TWO_MUL_11` | `12..99 × 11` | 精确 | 整数 |
| 16 | `TWO_MUL_15` | `12..98 × 15` | 精确 | 整数 |
| 17 | `TWO_MUL_TWO` | `12..98 × 12..98`（避开整十） | 精确 | 整数 |
| 18 | `CUSTOM` | 严格遵循 `CustomQuizConfig.normalized()`：<br>• `1` 位取 `2..9`，`k` 位（`k=2..5`）取 `10^(k-1)..10^k-1`<br>• `SUB` / `MIXED_ADD_SUB` 当 `allowNegative=false` 保证结果 `>= 0`；`allowNegative=true` 时允许产生负数（约 40% 概率出现负结果）<br>• `DIV` 除数恒 `>= 2`；**若 `DIV` 且 `tolerance == EXACT`，必须生成可整除或有限1位小数题**（例如反推 `dividend = divisor * quotient`，并尽可能满足目标位数，避免无限循环小数无法精确作答）<br>• `percentAnswer=true` 且为 `DIV` 时，`exactValue = (a / b) * 100` 且 `isPercentAnswer = true` | 跟随配置 | 跟随配置 |

---

## 三、高精度判题与统计引擎（`domain/engine/AnswerGrader.kt` & `PracticeSummaryCalculator.kt`）

### 1. `AnswerGrader`（纯静态/单对象或无状态类）
- **输入解析 `parseDecimal(rawInput: String): BigDecimal?`**：
  - `trim()` 空白，将全角/Unicode 减号 `−`、`–` 统一替换为 `-`，去除末尾多余 `%` 符号。
  - 空串、纯 `-`、纯 `.`、含多个 `.` 或非数字字符 → 返回 `null`（**严禁抛异常**）。
- **精确模式判题（`!question.isEstimation || question.tolerance == ToleranceLevel.EXACT`）**：
  - `parsed.compareTo(question.exactValue) == 0` → `GradeStatus.EXACT`（`isCorrect = true`, `signedErrorPercent = 0.0`, `feedbackText = "回答正确"`）
  - 否则 → `GradeStatus.FAIL`（`isCorrect = false`, `signedErrorPercent = null`, `feedbackText = "回答错误 · 精确 ${question.exactDisplay}"`）
- **估算模式判题（`question.isEstimation && question.tolerance != ToleranceLevel.EXACT`）**：
  - 边界 `exactValue.compareTo(BigDecimal.ZERO) == 0`：仅 `parsed == 0` 为 `EXACT`，否则 `FAIL`。
  - 常规精确计算：
    - `signedRatio = parsed.subtract(question.exactValue).divide(question.exactValue.abs(), MathContext.DECIMAL128)`
    - `absRatio = signedRatio.abs()`
    - **严格使用未四舍五入的 `absRatio` 判定等级**（绝不允许 `2.04%` 绕过 `±2%` 阈值）：
      - `absRatio <= BigDecimal("0.005")` → `GradeStatus.EXACT`（`isCorrect = true`）
      - `absRatio <= BigDecimal.valueOf(question.tolerance.ratio)` → `GradeStatus.PASS`（`isCorrect = true`）
      - 否则 → `GradeStatus.FAIL`（`isCorrect = false`）
    - **展示用带符号误差百分比**：
      - `val rawPct = signedRatio.multiply(BigDecimal(100)).setScale(1, RoundingMode.HALF_UP).toDouble()`
      - 归零 `-0.0` → `if (rawPct == 0.0) 0.0 else rawPct`
      - 格式化符号：正数带 `+`（如 `+1.0%`），负数带 `-`（如 `-0.8%`），零显示 `0.0%`。
    - **中文反馈文案（对齐 `design/ui_v2/02_practice_normal.png`）**：
      - `EXACT`：`"精准命中 · 精确 ${displayWithUnit} · 误差 ${formattedErr}"`
      - `PASS`：`"估算通过 · 精确 ${displayWithUnit} · 误差 ${formattedErr}"`
      - `FAIL`：`"偏差过大 · 精确 ${displayWithUnit} · 误差 ${formattedErr}"`
      - 解析失败：`"格式无效 · 精确 ${displayWithUnit}"`
      *(注：若 `isPercentAnswer == true`，`displayWithUnit` 附带 `%`，如 `"12.7%"`。)*

### 2. `PracticeSummaryCalculator`
- 输入 `List<AnsweredItem>`，计算 `PracticeSummary`：
  - `accuracyPercent = if (total == 0) 0 else ((correct * 100.0) / total).roundToInt()`
  - `averageDurationSec`：总耗时毫秒转秒后除以题数，保留 1 位小数。
  - `averageAbsErrorPercent`：仅当存在估算题且至少有 1 题 `signedErrorPercent != null` 时，计算这些题 `abs(signedErrorPercent)` 的平均值并 `HALF_UP` 保留 1 位小数；否则为 `null`。

---

## 四、N-back 状态机（`domain/engine/NBackController.kt`）

设计不可变、纯函数式或易测试的状态控制器，彻底杜绝 UI 层下标越界与漏题 bug：

- **模式阶段枚举**：
  ```kotlin
  enum class NBackPhase {
      STANDARD,      // N = 0 普通模式：看本题答本题
      MEMORIZE,      // N > 0 前置记忆期（前 effectiveN 步）：仅看题记忆，不作答
      RECALL_ACTIVE, // N > 0 常规交叉期：看当前新题，作答前 effectiveN 题
      RECALL_TAIL,   // N > 0 收尾补答期：不再出新题，补答最后滞留的 effectiveN 题
      COMPLETED,     // 全部 Q 题作答完毕
  }
  ```
- **安全边界**：`effectiveSteps = nBackLevel.steps.coerceAtMost((questions.size - 1).coerceAtLeast(0))`（例如错题重练仅剩 1 题时，即使开了 `2-back` 也自动安全降级为 `effectiveSteps = 0`，避免逻辑死锁）。
- **步骤快照 `NBackStepState`**：
  - `stepIndex: Int`、`totalSteps: Int`（`questions.size + effectiveSteps`）
  - `phase: NBackPhase`
  - `effectiveN: Int`
  - `displayQuestion: Question?`（当前屏幕展示给用户看的题目；`RECALL_TAIL` 与 `COMPLETED` 时为 `null`）
  - `targetQuestion: Question?`（当前用户需要作答并判题的目标题；`MEMORIZE` 与 `COMPLETED` 时为 `null`）
  - `answeredCount: Int`、`totalQuestionCount: Int`
  - `bannerText: String?`（`N=0` 时为 `null`；`MEMORIZE` 如 `"1-back 记忆阶段：记住第 1 题答案后点继续"`；`RECALL_ACTIVE` 如 `"1-back：记住本题，输入第 1 题答案"`；`RECALL_TAIL` 如 `"1-back 收尾：请输入第 10 题答案"`）
- **推进接口**：
  - `fun currentState(): NBackStepState`
  - `fun advanceMemorize(): NBackStepState`（仅 `MEMORIZE` 期推进）
  - `fun submitAnswer(rawInput: String, durationMs: Long): Pair<GradeResult, NBackStepState>`（对 `targetQuestion` 判题、记录 `AnsweredItem` 并推进下一步）
  - `fun answeredItems(): List<AnsweredItem>`（最终顺序严格对应 `questions[0..Q-1]`，保证 100% 闭环不丢题！）

---

## 五、全覆盖 JVM 单元测试要求（`app/src/test/java/com/xingce/speedmath/domain/`）

必须新增以下 3 个完整单测类，全部纯 JVM 高速运行：

1. **`QuestionGeneratorTest.kt`**：
   - 使用固定种子 `Random(2026)` 对**全部 17 种预设题型**各批量生成 100 题，逐一断言：
     - 表达式非空、无除数为 0、操作数位数范围严格合规；
     - `THREE_DIV_FOUR` 全部 `isPercentAnswer == true` 且 `exactValue` 落在 `3..95`；
     - `MAKE_HUNDRED` 表达式含 `?` 且补数在 `1..99` 且与左数之和为整百；
     - `MULTI_ADD` 均为 4~5 项且 `terms.size in 4..5`；
     - `MIXED_ADD_SUB` 同时含 `+` 和 `-` 且结果 `>= 0`；
     - 精确题型自动强制 `isEstimation == false` 与 `ToleranceLevel.EXACT`；
     - 批量去重：相邻两题表达式不重复。
   - 测试 `CUSTOM` 全部 5 种运算、`allowNegative = false/true`、`percentAnswer = true`、`DIV + ToleranceLevel.EXACT` 可精确匹配。
2. **`AnswerGraderTest.kt`**：
   - 精确模式：整数精确命中、尾随 `.0` 等价（`115.0 == 115`）、差 1 判错、负号与全角 `−` 兼容。
   - 估算模式：
     - 精确边界测试：`0.5%` → `EXACT`；`0.51%` → `PASS`；`1.0%` (`±1%`) → `PASS`；`1.01%` (`±1%`) → `FAIL`；`2.00%` (`±2%`) → `PASS`；**`2.04%` (`±2%`) → 必须 `FAIL`（验证不用四舍五入后的值误放行！）**；`5.0%` (`±5%`) → `PASS`。
     - 验证带符号误差四舍五入与 `-0.0` 归零（如 `38412 ÷ 192` 答 `202` 误差应精确四舍五入为 `+1.0%`，不是 `+0.9%`）。
     - 百分比题型反馈包含 `%`。
     - 非法输入测试：`""`, `"   "`, `"-"`, `"."`, `"12.3.4"`, `"abc"` 全部安全返回 `FAIL` 且不抛异常。
   - `PracticeSummaryCalculator` 汇总正确率、平均误差（忽略精确题与解析失败项）、平均耗时。
3. **`NBackControllerTest.kt`**：
   - 完整跑通 `OFF(0)`、`ONE(1)`、`TWO(2)` 在 `Q = 5` 题下的每一步 `phase`、`displayQuestion`、`targetQuestion`、步数（`5 / 6 / 7`），断言最终 `answeredItems()` 长度恒为 `5` 且题序与原始 `questions` 100% 一一对应。
   - 测试边界场景：`Q = 1` 开启 `2-back` 时自动安全降级为 `effectiveN = 0`；`Q = 2` 开启 `2-back` 时自动降级为 `effectiveN = 1`。

---

## 六、强制命令验证与汇报要求

1. 在 `/home/bubu/资料分析apk` 执行：
   - `./gradlew testDebugUnitTest`（Pack 1 + Pack 2 所有单元测试 100% 通过）
   - `./gradlew assembleDebug`（零编译报错）
2. 完成后汇报：新增文件清单、各题型生成规则关键实现要点、单元测试总数与执行结果，等待架构师逐项代码审查。
