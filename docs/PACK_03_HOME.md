# 任务包 Pack 3：3-Tab 主壳、首页工作台（对齐 `01_home.png`）、自定义出题与偏好设置

> **所属规划**：`docs/MASTER_PLAN.md` — Pack 3 / 6  
> **工作目录**：`/home/bubu/资料分析apk`  
> **像素级视觉基准**：`/home/bubu/资料分析apk/design/ui_v2/01_home.png`（晴空蓝白 Apple HIG × Material 3）  
> **前置基础**：Pack 1（Design System + Room + DataStore）与 Pack 2（核心算术引擎）均已验收通过  
> **本包边界**：实现「3-Tab 主壳与导航骨架 + 完整首页工作台 + 自定义出题 BottomSheet + 偏好设置 BottomSheet（含 N-back 与说明） + HomeViewModel 与单测」。**严禁**在本包提前实现 Pack 4 的真实答题页/草稿板/报告页、Pack 5 的完整公式本/历史页或 Pack 6 的真实导出引擎（相关路由与 Tab 仅放整洁晴空蓝白占位页即可，确保点击可跳转、可返回）。

---

## 一、导航骨架与 3-Tab 底部导航栏

### 1. `ui/components/BottomNavBar.kt`
严格对齐 `design/ui_v2/01_home.png`、`05_formulas.png`、`08_history.png`：
- 3 个一级 Tab：
  1. `速算`（route: `home`，图标 `Icons.Rounded.Calculate`）
  2. `公式`（route: `formulas`，图标 `Icons.Rounded.MenuBook`）
  3. `统计`（route: `history`，图标 `Icons.Rounded.BarChart`）
- 视觉样式：纯白底 `#FFFFFF` + 顶部 `1.dp` `#E3EEFA` 极细分割线 + `navigationBarsPadding()`；激活项晴空蓝 `#2E84EC`（图标 + 11sp Medium 文字），未激活 `#7A8FA6`。

### 2. `ui/navigation/AppNavGraph.kt` & `MainActivity.kt`
- 替换 Pack 1 的临时启动验证页，`MainActivity` 正式挂载 `SpeedMathTheme` + `AppNavGraph(container)`。
- 配置清晰的路由结构（为 Pack 4 / Pack 5 无缝替换做好接口）：
  - 一级 Tab 路由：`home`、`formulas`、`history`（底栏仅在这 3 个路由显示，切换使用 `launchSingleTop = true` + `restoreState = true`）。
  - 子路由：`practice/{moduleId}?count={count}&tolerance={tolerance}&nBack={nBack}&keypad={keypad}&stylus={stylus}`（Pack 3 内展示干净的「练习启动参数确认占位页」，清晰显示传入的题型、题量、误差档位、键盘顺序、草稿板开关、N-back 档位及自定义配置摘要，并提供“返回首页”按钮，证明导航传参 100% 打通；Pack 4 将直接替换为真实 `PracticeScreen`）。
  - `formulas` 与 `history` 在 Pack 3 中放干净的晴空蓝白占位卡片（可顺便展示 Room 实时公式数 / 历史会话数，验证数据库连接正常）。

---

## 二、首页工作台（`ui/home/HomeScreen.kt` & `HomeViewModel.kt`）

严格 1:1 还原 `design/ui_v2/01_home.png` 的清透呼吸感与组件布局：

### 1. 顶部栏（`statusBarsPadding()` 安全区适配）
- 左侧：大标题 `行测速算`（24sp Bold `#162436`）。
- 右侧：两枚 `42.dp` 纯白圆形按钮（`SkyCircleIconButton`，`#D5E6FA` 细边）：
  - 导出按钮（`Icons.Rounded.IosShare` 或 `Icons.Rounded.Description`）：点击弹出轻提示或 Pack 6 导出占位 Sheet（提示“导出练习卷将在 Pack 6 启用”）。
  - 设置按钮（`Icons.Rounded.Settings`）：点击打开 `SettingsSheet`。

### 2. 晴空蓝渐变「今日速算看板」Hero 卡
- 容器：`RoundedCornerShape(22.dp)`，晴空蓝渐变背景 `Brush.linearGradient(listOf(PrimarySkyLight, PrimarySkyDeep))` + 柔和晴空阴影，内边距 `horizontal = 20.dp, vertical = 18.dp`。
- 实时订阅 `PracticeRepository.observeTodayStats(startOfDayMillis)`（本地时区当日 00:00:00）：
  - 左侧：
    - 小标 `今日速算`（13sp 白 88%）
    - 大号等宽数字：`{todayCount} 题`（数字 34sp ExtraBold 白、`题` 18sp Medium 白）
    - 副行：若 `todayCount > 0` 显示 `正确率 {accuracy}% · 均速 {avgSec}s`；若今日为 `0` 题显示 `开始今天的第一组速算吧`
  - 右侧：
    - 轻透白色胶囊按钮 `继续上次 ›`（`Color.White.copy(alpha = 0.88f)`，文字 `#2277DF` Bold）：点击直接以 `preferences.lastModule` 与当前参数启动练习！
    - 胶囊正下方居中柔和白字（12sp）：显示 `preferences.lastModule.title`（如 `五位数除三位数`）。

### 3. 横向分类胶囊栏（4 枚）
- 选项枚举 `HomeCategoryFilter`：
  1. `DIVISION_ESTIMATION("除法估算")` *(默认选中)*
  2. `ADD_SUB("加减基础")`
  3. `MULTIPLICATION("乘法速算")`
  4. `ALL("全部(17)")`
- 使用 `LazyRow`（间距 `8.dp`，左右 `16.dp` 内边距），防止小屏挤压换行：
  - 选中态：晴空蓝渐变或 `#3B93F6` 实底 + 纯白 Medium 文字。
  - 未选态：纯白 `#FFFFFF` + `1.dp` `#D5E6FA` 边框 + `#162436` 文字。

### 4. 2 列宽敞纯白 Bento 题型卡片区（可垂直滚动）
- 展示当前分类过滤后的预设题型，并在网格末尾始终附带 1 张 **`+ 自定义出题`** 轻卡：
  - `除法估算`：5 项预设 + `自定义出题` = 6 张（3 行 × 2 列，与 `01_home.png` 一模一样！）。
  - `加减基础`：7 项预设 + `自定义出题` = 8 张（4 行 × 2 列）。
  - `乘法速算`：5 项预设 + `自定义出题` = 6 张（3 行 × 2 列）。
  - `全部(17)`：17 项预设 + `自定义出题` = 18 张（9 行 × 2 列）。
- **单张预设卡片规格**：
  - 圆角 `16.dp`，最小高度 `98.dp`，内边距 `14.dp`。
  - **选中态**：背景 `#EDF6FF`、边框 `2.dp` `#3B93F6`、右上角 `22.dp` 实心晴空蓝圆圈配白色 `✓` 图标。
  - **未选态**：背景 `#FFFFFF`、边框 `1.dp` `#D5E6FA`、轻柔微影。
  - 内部排布：
    - 主标题：`module.title`（15sp Bold `#162436`，单行防溢出）
    - 算式预览：`module.sampleExpression`（13sp 等宽 `#5D7188`，如 `54321 ÷ 213`、`534 ÷ 4218 → %`）
    - 底部角标：当 `module.isEstimation == true` 时显示浅天蓝小胶囊 `#D6EAFF` + `#2277DF` 文字 `估`（精确题型留白保持清爽）。
- **`+ 自定义出题` 虚线轻卡**：
  - 若未选中：极浅冰蓝 `#F0F7FF` + 虚线天蓝边框（Canvas `PathEffect.dashPathEffect` 绘制圆角虚线）+ 蓝色 `+` 与标题 `自定义出题` + 副文（默认 `自由设位数与误差`，或显示当前自定义摘要如 `除法 · 5位÷3位 · ±2%`）。
  - 若已选中 `MathModule.CUSTOM`：切换为选中态蓝框 + 右上蓝勾 `✓`；点击卡片打开 `CustomConfigSheet` 供编辑（若已在其他题型上，点一次自定义卡直接弹出配置 Sheet）。

### 5. 底部紧凑「练习参数控制台」悬浮纯白卡片（固定在 3-Tab 底栏上方，不随列表滚动消失）
- 容器：纯白 `#FFFFFF`、`RoundedCornerShape(20.dp)`、`1.dp` `#D5E6FA` 边框 + 柔和顶阴影，内部两行紧凑排布：
  - **上排（4 枚冰蓝 `#E8F3FF` 参数芯片，横向均匀排布）**：
    1. **题量芯片**：`{count}题 ▾`（默认 `10题 ▾`），点击弹出轻量 `ModalBottomSheet` 选 `10题 / 20题 / 30题 / 50题`，持久化至 DataStore。
    2. **误差芯片**：
       - 若当前选中题型为**估算题型**（或自定义且非精确）：显示 `{tolerance.shortLabel} ▾`（如 `±2% ▾`），点击弹出误差档位 BottomSheet 选择 `精算 ±1%` / `实战 ±2%` / `宽松 ±5%` 并持久化。
       - 若当前选中题型为**精确题型**：显示 `精确`，点击弹出轻量 Snackbar 或提示“该题型为基本功精确匹配模式（0误差）”。
    3. **键盘顺序芯片**：显示 `{keypadOrder.label}键盘`（如 `正序键盘`），点击一键循环切换 `正序 → 倒序 → 乱序 → 正序` 并持久化。
    4. **草稿板芯片**：显示 `草稿板 开` / `草稿板 关`（开启时文字高亮 `#2277DF`），点击一键切换并持久化。
    - *(附加贴心细节：当 `nBackLevel != NBackLevel.OFF` 时，在参数行上方增加一条极纤细浅蓝提示条 `已开启 ${nBackLevel.label} 训练模式 · 点击调整`，点击打开 `SettingsSheet`，防止用户忘记开启了 N-back！)*
  - **下排**：通栏 `50.dp` `SkyPrimaryButton("开始练习")`，点击将当前选中模块保存为 `lastModule` 并导航至 `practice` 路由。

---

## 三、自定义出题弹窗（`ui/home/CustomConfigSheet.kt`）

纯白 `24.dp` 顶圆角 `ModalBottomSheet`，参考本应用晴空蓝白美学（简洁清晰）：
1. 顶栏：左侧 `自定义出题配置` + 右上浅蓝圆形 `×` 关闭钮。
2. `运算类型`：单选胶囊组 `加法 / 减法 / 乘法 / 除法 / 混合加减`（选中晴空蓝白字）。
3. `第一数位数`：`1位 ~ 5位` 等宽胶囊。
4. `第二数位数`：`1位 ~ 4位` 等宽胶囊。
5. 冰蓝 `#E8F3FF` 圆角分组开关面板：
   - `允许负数结果` Switch（仅 `减法` / `混合加减` 可启用，其他运算自动关闭并置灰提示）。
   - `以百分比(%)作答` Switch（仅 `除法` 可启用，其他运算自动关闭并置灰提示）。
6. `误差标准`：4 枚胶囊 `精确(0%) / ±1% / ±2% / ±5%`。
7. 底部通栏晴空蓝按钮 `保存并选用`：归一化配置写入 DataStore，选中 `MathModule.CUSTOM` 并关闭弹窗。

---

## 四、偏好设置弹窗（`ui/home/SettingsSheet.kt`）

纯白 `24.dp` 顶圆角 `ModalBottomSheet`：
1. 顶栏：`偏好设置` + 右上关闭 `×`。
2. **N-back 工作记忆训练**：
   - 使用 `SkySegmentedPill` 提供 `关闭 | 1-back | 2-back` 实时切换并写入 DataStore。
   - 下方浅灰蓝说明文案：`看当前新题、答前 N 题答案，训练行测考场抗干扰与短时记忆（前 N 题为记忆期，末尾自动补答）。`
3. **练习体验分组卡片**（冰蓝圆角分组）：
   - `按键震动反馈` Switch（绑定 `hapticEnabled`）
   - `默认开启触控笔草稿板` Switch（绑定 `stylusEnabled`，附简短说明）
4. **误差标准说明卡片**（纯白 `#D5E6FA` 边框小卡）：
   - 简洁说明 `精算 ±1%（截三位直除）`、`实战 ±2%（默认推荐）`、`宽松 ±5%（截两位快估）` 及精确题型 `0` 误差原则。
5. **数据管理**：
   - `清空全部练习记录` 按钮（珊瑚红文字 `#E04B5A`），点击弹出二次确认 `AlertDialog`，确认后调用 `PracticeRepository.clearAll()` 并给出反馈。

---

## 五、ViewModel、状态格式化与单元测试

1. **`ui/home/HomeViewModel.kt`**：
   - 使用 `ViewModelProvider.Factory` 注入 `PracticeRepository` 与 `UserPreferencesStore`。
   - 暴露单一 `StateFlow<HomeUiState>`（组合今日统计、用户偏好、当前分类、当前选中模块、弹窗状态），无副作用、状态清晰。
   - **纯函数格式化工具**（方便 JVM 单测）：
     - `formatTodaySummary(stats: StatsAggregate): TodayBannerModel`（计算今日题数、四舍五入正确率%、均速保留 1 位小数如 `6.2s`）
     - `effectiveToleranceFor(module: MathModule, userTolerance: ToleranceLevel, customConfig: CustomQuizConfig): ToleranceLevel`
     - `customConfigSummary(config: CustomQuizConfig): String`（如 `"除法 · 5位÷3位 · ±2%"`）
2. **新增单元测试（`app/src/test/java/com/xingce/speedmath/ui/HomeLogicTest.kt`）**：
   - 验证 `formatTodaySummary` 在 0 题与正常刷题下的计算与格式化；
   - 验证 `effectiveToleranceFor` 对精确题型、估算题型（含 `EXACT` 兜底）、自定义题型的判断；
   - 验证 `HomeCategoryFilter` 各分类返回的题型数量（5 / 7 / 5 / 17）；
   - 验证 `KeypadOrder` 循环切换顺序（`ASCENDING -> DESCENDING -> SHUFFLED -> ASCENDING`）。

---

## 六、强制验证与交付汇报

1. 执行 `./gradlew testDebugUnitTest`（Pack 1 + Pack 2 + Pack 3 全部单测 100% 通过）。
2. 执行 `./gradlew assembleDebug`（0 错误生成 APK）。
3. 完成后汇报：新增/修改文件清单、与 `01_home.png` 视觉对齐要点、单测与构建结果，等待架构师逐行审核后再下发 **Pack 4（答题练习页 + 蓝点阵手写草稿板 + 三态键盘 + 练习报告页）**。
