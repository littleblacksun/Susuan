# 任务包 Pack 4：沉浸式答题练习页、蓝点阵手写草稿板、三态键盘与练习报告页

> **所属规划**：`docs/MASTER_PLAN.md` — Pack 4 / 6  
> **工作目录**：`/home/bubu/资料分析apk`  
> **像素级视觉基准**：
> - `/home/bubu/资料分析apk/design/ui_v2/02_practice_normal.png`（标准估算答题 + 正序悬浮白键帽键盘）
> - `/home/bubu/资料分析apk/design/ui_v2/03_practice_stylus.png`（蓝点阵手写草稿板 + 百分比除法 + 倒序键盘）
> - `/home/bubu/资料分析apk/design/ui_v2/04_result.png`（练习报告 + 晴空蓝环形仪表盘 + 逐题复盘）  
> **前置基础**：Pack 1（Design System + Room + DataStore）、Pack 2（核心算术/判题/N-back 引擎）、Pack 3（3-Tab 主壳 + 首页工作台）均已验收通过。  
> **本包边界**：实现完整闭环的「答题练习页 + 蓝点阵手写草稿板 + 自定义三态键盘 + 练习完成自动落库 Room + 练习报告页 + 错题重练 / 再来一组」。**严禁**提前实现 Pack 5 的公式本/统计页或 Pack 6 的导出引擎。

---

## 一、顺手修复 Pack 2 遗留微细节（1 处）

- 在 `domain/engine/AnswerGrader.kt` 第 40 行，将精确模式答错反馈从 `"回答错误 · 精确 ${question.exactDisplay}"` 修正为 `"回答错误 · 精确 $displayWithUnit"`，使自定义「除法 + 精确(0%) + 百分比」答错时也能正确带 `%` 后缀，并补充对应单测断言。

---

## 二、自定义 4×4 悬浮白键帽键盘与输入状态规约

### 1. `ui/practice/KeypadInputReducer.kt`（纯 Kotlin，可单测）
封装安全的数字键输入处理函数 `reduceKeypadInput(current: String, action: KeypadAction): String`：
- `Digit(0..9)`：最大长度 `12` 字符防溢出；处理无意义前导零（`"0"` + `5` → `"5"`、`"-0"` + `5` → `"-5"`，但 `"0."` 后正常追加）。
- `Decimal`：已有 `.` 则忽略；空串 `""` 插入 `"0."`；`"-"` 插入 `"-0."`。
- `ToggleMinus`：切换负号前缀（`"" -> "-"`、`"-12" -> "12"`、`"12" -> "-12"`）。
- `Backspace`：删除最后 1 位。
- `Clear`：清空为 `""`。
- 同步提供 `fun keypadDigitsFor(order: KeypadOrder, random: Random = Random.Default): List<Int>`：
  - `ASCENDING` → `[1, 2, 3, 4, 5, 6, 7, 8, 9]`
  - `DESCENDING` → `[7, 8, 9, 4, 5, 6, 1, 2, 3]`
  - `SHUFFLED` → `(1..9).shuffled(random)`（每次切题重新洗牌，确保乱序真实生效）。

### 2. `ui/components/MathKeypad.kt`
严格 1:1 对齐 `02_practice_normal.png` 与 `03_practice_stylus.png`，**全程禁止调用 Android 系统输入法**：
- 整体托盘：清透冰蓝底 `#E8F3FF`，顶部圆角 `20.dp`，底部自动加 `navigationBarsPadding()`。
- 4 列 × 4 行等宽网格（按键间距 `8.dp`，内边距 `12.dp`，单行高度自适应约 `52.dp~56.dp`，保证小屏不裁切）：
  - **前 3 列**：纯净雪白 `#FFFFFF` 圆角键帽（`12.dp`）+ 极轻冷调投影 + 深藏青粗体等宽数字（22sp Bold `#162436`）；第 4 行为 `.`、`0`、`-`。
  - **第 4 列**：
    - 第 1 行：浅天蓝 `#D6EAFF` 退格键（`Icons.AutoMirrored.Rounded.Backspace`）
    - 第 2 行：浅天蓝 `#D6EAFF` 清空键（`C`）
    - 第 3~4 行合并跨两行大号按钮：晴空蓝渐变 `PrimarySkyGradient` + 纯白粗体字（记忆阶段显示 `继续`、最后一题显示 `完成`、其余显示 `下一题`）。
- 支持 `hapticEnabled: Boolean`：开启时每次按键触发 `LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.TextHandleMove)`。

---

## 三、蓝点阵手账手写草稿板（`ui/components/ScratchpadCanvas.kt`）

严格 1:1 对齐 `design/ui_v2/03_practice_stylus.png`：
- **纸张画布外观**：纯白 `#FFFFFF`、`RoundedCornerShape(18.dp)`、`1.dp` `#D5E6FA` 细边框；Canvas 底层绘制均匀细腻的淡天蓝点阵（`#D5E6FA`，点间距约 `18.dp`，点半径 `1.4.dp`）。
- **低延迟平滑手写**：
  - 使用 `pointerInput` 捕获手指与触控笔（Stylus）拖拽轨迹，采用二阶贝塞尔中点平滑连线，笔迹墨水色采用清晰深藏青炭黑 `#162436`（`StrokeCap.Round`, `StrokeJoin.Round`）。
  - 维护 `strokes: List<ScratchStroke>`（每条含点集与线宽），外部传入 `clearTriggerKey: Any`（如 `stepIndex`）变化时自动清空画布。
- **右上角悬浮极简毛玻璃工具条**（半透明白底 + `#D5E6FA` 细边胶囊，严格 4 枚圆润线性图标）：
  1. 撤销（`Icons.AutoMirrored.Rounded.Undo`，无笔迹时置灰）
  2. 一键清空（`Icons.Rounded.DeleteOutline`）
  3. 笔触粗细切换（`Icons.Rounded.Edit`，循环切换 细 `2.5dp` / 中 `4.dp` / 粗 `6.dp`，并可带细小点/提示）
  4. 收起草稿板（`Icons.Rounded.KeyboardArrowDown` / `KeyboardArrowUp`，回调切换回标准模式）

---

## 四、答题练习页与 ViewModel（`ui/practice/PracticeScreen.kt` & `PracticeViewModel.kt`）

### 1. 路由与启动模式（更新 `ui/navigation/AppNavGraph.kt`）
替换 Pack 3 的 `PracticePlaceholder`，正式接入 `PracticeScreen` 与 `ResultScreen`：
- **标准启动**：接收 Pack 3 既有的 `PracticeLaunchConfig` 路由参数，调用 `QuestionGenerator.generateBatch(...)` 生成题目。
- **错题重练启动**：新增可选参数 `retrySessionId: Long = -1L`（或独立路由 `practice/retry/{sessionId}`）：
  - 从 `PracticeRepository.getSessionDetails(sessionId)` 读取当次所有 `gradeStatus == "FAIL"` 的错题，通过纯函数 `QuestionReconstructor.fromFailedItems(details)` 重建 `List<Question>`（保留原题型、算式、精确值、是否百分比、原误差档位）发起专项重练！
- **结果页路由**：`result/{sessionId}`，完成练习落库后跳转 `result/{savedSessionId}` 并弹出 `practice` 路由（避免从结果页按返回键退回已完成练习页）。

### 2. 练习生命周期与交互细节（`PracticeViewModel.kt`）
- 持有 `NBackController` 实例驱动全流程，暴露单一 `StateFlow<PracticeUiState>`：
  - 实时计时：区分**整局累计用时**（顶栏 `mm:ss`）与**单题用时**（精确记录每题从开始到提交的毫秒数）；弹出退出确认框时暂停计时。
  - 切题键盘顺序：支持答题页内实时切换 `正序 / 倒序 / 乱序`，同步更新本地键盘并写回 DataStore；`乱序` 模式在每次进入新步时刷新 `1..9` 排列。
  - **提交与即时反馈体验**：
    - `MEMORIZE` 期：点“继续”直接推进 `NBackController.advanceMemorize()`。
    - 答题期：提交输入后调用 `NBackController.submitAnswer(input, itemDurationMs)` 获得 `GradeResult`，立即展示反馈胶囊（约 `750ms` 自动切下一题，若用户在反馈期间再次点击“下一题”则立即跳过等待进入下一题，兼顾刷题节奏与反馈查看！）。
    - `COMPLETED` 期：立即汇总 `PracticeSummaryCalculator.calculate(items)`，通过 `PracticeRepository.saveSessionWithItems(...)` 事务写入 Room，拿到 `sessionId` 后发送一次性导航事件跳转 `result/{sessionId}`（防重复提交/重复保存）。
  - 防误触返回：拦截物理返回键（`BackHandler`）与顶栏返回按钮，弹出二次确认 `AlertDialog`（“退出本次练习？当前作答进度将不会保存”）。

### 3. 两种页面布局精确还原（严格对齐 `02_practice_normal.png` & `03_practice_stylus.png`）
- **顶栏**（两种模式共用）：
  - 左：纯白圆钮 `<`；中：题型标题（错题重练可显示 `{原题型} · 错题`）+ 浅天蓝胶囊（`±2%` / `精确`）；右：等宽计时 `00:18` + 细竖线 + 进度 `{current}/{total}`。
  - 下方：纤细圆头晴空蓝进度条（平滑动画 `animateFloatAsState`）。
  - 若 N-back 开启：紧接进度条下方展示清透浅蓝提示条 `bannerText`。
- **模式 A（草稿板收起 — 对齐 `02_practice_normal.png`）**：
  - 中央宽敞 `SkyCard`（`22.dp` 圆角）：
    - 大号等宽算式（`32sp~36sp` Bold `#162436`）：注意 `MAKE_HUNDRED` 已自带 `= 100` 与 `?`，勿重复追加 `= ?`；`MULTI_ADD` / `MIXED_ADD_SUB` 超过 3 项时利用 `terms` 优雅双行居中排版不溢出；`RECALL_TAIL` 无新题时优雅显示 `"回忆第 ${targetIdx + 1} 题答案"`。
    - 晴空蓝 `2.dp` 描边纯白大号输入框（显示当前输入 + 周期闪烁晴空蓝竖线光标；百分比题型在输入框右侧醒目展示 `%`；记忆阶段改为居中提示与大号 `记住了，继续 ›` 按钮）。
    - 即时反馈胶囊：根据 `GradeStatus` 显示翡翠绿(`EXACT`) / 晴空青(`PASS`) / 珊瑚红(`FAIL`) 语义色胶囊与 `feedbackText`。
  - 键盘上方工具条：左浅冰蓝胶囊 `草稿板 · 关`（配笔图标，点击展开草稿板），右紧凑分段器 `正序 | 倒序 | 乱序`。
  - 底部 `MathKeypad`。
- **模式 B（草稿板展开 — 对齐 `03_practice_stylus.png`）**：
  - 顶部紧凑横向纯白算式卡（`18.dp` 圆角）：左侧算式（如 `534 ÷ 4218 ≈`），右侧晴空蓝描边输入盒 + `%` 后缀；有即时反馈时在其下方叠加轻薄反馈条。
  - 中段 `Modifier.weight(1f)` 完整呈现 `ScratchpadCanvas`（蓝点阵手账纸 + 4 键工具条）。
  - 底部 `MathKeypad`。

---

## 五、练习报告与逐题复盘页（`ui/result/ResultScreen.kt` & `ResultViewModel.kt`）

严格 1:1 对齐 `design/ui_v2/04_result.png`，并天然兼容 Pack 5 历史记录页点击跳转回看：

1. **数据加载**：通过 `sessionId` 从 `PracticeRepository.getSessionDetails(sessionId)` 异步加载，处理空/不存在异常兜底（返回首页）。
2. **顶栏**：左侧纯白圆钮首页/返回图标，居中标题 `练习报告`，右侧次要字 `session.moduleTitle`。
3. **成绩总览大白卡（`SkyCard`, `22.dp` 圆角）**：
   - 左侧：**自绘 Canvas `AccuracyRing`**（`ui/components/AccuracyRing.kt`）：直径 `116.dp`、线宽 `12.dp`、冰蓝底环 `#E8F3FF`、晴空蓝 `#3B93F6` 圆头进度弧（入场平滑动画）、环心大号等宽 `{accuracy}%` + 小字 `正确率`。
   - 右侧：竖排 3 项指标，以 `#E6F0FA` 细线分隔：
     - `总用时`：`mm:ss`（如 `01:12`）
     - `均速`：`{x.x}秒/题`
     - `平均误差`：估算题且有误差值时显示 `±{x.x}%`，精确题型显示 `精确匹配`
4. **逐题复盘列表（`LazyColumn`）**：
   - 每张纯白 `SkyCard`（`16.dp` 圆角）：
     - 上行：浅天蓝胶囊 `题 {item.questionIndex + 1}` + 粗体算式（若较长自动优雅换行）；右侧单题耗时 `{x.x}s`。
     - 下行：左侧 `你的 {userAnswerDisplay} · 精确 {exactWithUnit}`；右侧语义状态胶囊：
       - `EXACT` → `#E6F8F0` / `#179B68` `精准`
       - `PASS` → `#E5F6FA` / `#148CA8` `通过 {+x.x%}`
       - `FAIL` → `#FDECEF` / `#E04B5A`（估算且有误差显示 `偏差 {+x.x%}`，否则显示 `错误` / `未作答`）
5. **底部双按钮固定栏（`navigationBarsPadding()`）**：
   - 左 `SkySecondaryButton`：`错题重练`（存在错题时可点，跳转该 `sessionId` 的错题重练；全对时置灰显示 `全部答对`）。
   - 右 `SkyPrimaryButton`：`再来一组`（复用该会话题型与用户当前偏好直接新开一组练习）。

---

## 六、单元测试与强制验证标准

1. **新增 JVM 单元测试（`app/src/test/java/com/xingce/speedmath/ui/`）**：
   - `KeypadInputReducerTest.kt`：覆盖数字追加、前导零折叠、小数点唯一性与 `"" -> "0."`、负号切换、退格、清空、12 位长度限制，以及 `正序 / 倒序 / 乱序` 1..9 完整性校验。
   - `PracticeSessionMapperTest.kt`：覆盖 `AnsweredItem -> Room Entity` 映射、时长 `mm:ss` 与单题 `x.xs` 格式化、`QuestionReconstructor` 从失败条目重建错题（含百分比与估算档位保留）。
   - 更新 `AnswerGraderTest.kt` 覆盖精确百分比模式答错反馈含 `%`。
2. **强制命令验证**：
   - `./gradlew testDebugUnitTest` — 100% 全绿通过（含 Pack 1~4 全部测试）
   - `./gradlew assembleDebug` — 零错误生成 APK
3. **汇报内容**：列出新增/修改文件、与 `02`/`03`/`04` 视觉对齐要点、单测数量与构建输出，等待架构师审查后进入 **Pack 5（公式本完整知识库 CRUD + 百化分/年均幂次速查 + 统计与历史记录页）**。
