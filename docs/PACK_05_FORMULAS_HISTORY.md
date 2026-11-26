# 任务包 Pack 5：「公式本」独立模块（接入完整资料分析知识库）+ 多维速查表 + 「统计与记录」主页

> **所属规划**：`docs/MASTER_PLAN.md` — Pack 5 / 6  
> **工作目录**：`/home/bubu/资料分析apk`  
> **权威内容数据源（用户已审核定稿）**：`/home/bubu/资料分析apk/docs/DATA_ANALYSIS_KNOWLEDGE.md`  
> **像素级视觉基准**：
> - `/home/bubu/资料分析apk/design/ui_v2/05_formulas.png`（公式本 · 我的公式）
> - `/home/bubu/资料分析apk/design/ui_v2/06_formula_editor.png`（新建/编辑公式弹窗 + 快捷数学符号栏）
> - `/home/bubu/资料分析apk/design/ui_v2/07_baifen.png`（公式本 · 百化分速查视图）
> - `/home/bubu/资料分析apk/design/ui_v2/08_history.png`（统计与历史记录主页）  
> **前置基础**：Pack 1 ~ Pack 4 均已验收通过。  
> **本包边界**：完整实现底部 Tab 2「公式」与 Tab 3「统计」全部真实业务功能、接入 `DATA_ANALYSIS_KNOWLEDGE.md` 全部干货、打通历史记录点击跳转 `ResultScreen` 回看详情。**严禁**提前实现 Pack 6 的真实 PDF/长图/纯文本导出引擎。

---

## 一、接入完整权威资料分析知识库（`domain/` & `data/` 增量升级）

严格以用户已审核通过的 `docs/DATA_ANALYSIS_KNOWLEDGE.md` 为唯一数据源，**数值、公式表达式与备注必须逐字核准，零 LaTeX 源码**：

### 1. 扩展 `FormulaCategory`（`domain/model/PracticeConfig.kt`）
在保持既有枚举名不变的前提下，补齐 `OTHER_HIGH_FREQ("其他高频")`：
```kotlin
enum class FormulaCategory(val label: String) {
    GROWTH_RATE("增长率"),
    BASE_PERIOD("基期"),
    PROPORTION("比重"),
    AVERAGE("平均数"),
    OTHER_HIGH_FREQ("其他高频"),
    CUSTOM("自定义"),
}
```
*(请确认 `FormulaCategory` 解析逻辑对未知字符串安全兜底至 `CUSTOM`。)*

### 2. 建立完整知识常量仓（`domain/data/KnowledgeReference.kt`）
保留 `BaifenTable.entries`（16 项）不变以兼容已有契约，并将 `BaifenTable.defaultFormulas` 升级为 **`docs/DATA_ANALYSIS_KNOWLEDGE.md` 模块 1 的全部 30 条公式**（5 条基期 + 10 条增长率 + 4 条比重 + 4 条平均数 + 7 条其他高频，默认收藏严格对应表格中标记为“是”的 15 条！），同时在 `KnowledgeReference.kt` 中结构化沉淀以下 4 组只读速查数据：
1. **百化分完整体系**：
   - `baseBaifen`：`1/2` ~ `1/20` 共 **19 组**（含分数、符号 `=`/`≈`、百分数、移位变形说明）。
   - `extendedBaifen`：模块 2.2 的全部 **30 组** 考场高频拓展分数（含分组、分数、符号、百分数、速记规律）。
   - `baifenTricks`：模块 2.3 的 **3 大非标准百分数反推技巧**（中值法、倒数反推+放缩单调性、拆两项/移位法）。
2. **年均增长率（CAGR）专题**：
   - `cagrCalibers`：模块 3.1 的 **4 种年份差 `n` 口径辨析**（国考常规、五年规划期、江苏省考、年初—年末）。
   - `cagrMethods`：模块 3.2 的 **3 大实战速算方法**（上限估算、二项式展开含 `n=2..5`、选项居中代入法）。
   - `cagrPowerMatrix`：模块 3.3 的完整 **8 行 × 4 列 `(1+r)ⁿ` 速查矩阵**（`r = 5%, 8%, 10%, 12%, 15%, 20%, 25%, 30%` × `n = 2, 3, 4, 5` 共 32 个精确验算值 + 72 法则翻倍锚点）。
3. **常用平方数表**：`11² = 121` 至 `25² = 625` 共 **15 组** + 尾数 5 平方与相邻平方差口诀。
4. **截位直除口诀与 8 大易错概念**：
   - `truncationRules`：模块 4.2 的 3 档截位策略（截两位 / 截三位 / 截四位，明确标注与本 App `±5% / ±2% / ±1% / 0%` 档位对应关系）+ 四大实战口诀。
   - `pitfalls`：模块 4.3 的 **8 条高频易错概念辨析**（百分数 vs 百分点、同比 vs 环比、顺差 vs 逆差、成数 vs 翻番、累计 vs 当月、增长率 vs 降幅 vs 变化幅度、增长贡献率 vs 拉动增长率、恩格尔系数 vs 基尼系数）。

### 3. 升级公式库幂等增量预置（`FormulaRepository.kt` & `UserPreferencesStore.kt`）
- 在 `UserPreferencesStore` 中新增版本键 `formula_seed_version`（Int，默认 `0`；若旧版 `formulas_seeded == true` 视为版本 `1`）。
- 设当前目标种子版本 `CURRENT_FORMULA_SEED_VERSION = 2`：
  - 当本地版本 `< 2` 时，在 Room 事务内读取现有所有公式标题集合，将 30 条 `BaifenTable.defaultFormulas` 中**尚未存在**的内置公式补插进去（**严禁覆盖或删除用户已创建的自定义公式，也严禁重置用户对旧公式的收藏状态与编辑内容！**），完成后写入 `formula_seed_version = 2`。
  - 当本地版本已 `>= 2` 时直接跳过，尊重用户主动删除内置公式的操作，绝不反复回插。

---

## 二、「公式本」独立一级页面（`ui/formula/`）

替换 `AppNavGraph.kt` 中 `PrimaryTab.FORMULAS` 的占位页，严格 1:1 还原 `design/ui_v2/05_formulas.png`、`06_formula_editor.png` 与 `07_baifen.png`：

### 1. `FormulaScreen.kt` & `FormulaViewModel.kt`
- **顶栏（`statusBarsPadding()` + `displayCutout`）**：
  - 左侧粗体大标题 `公式本`（24sp Bold `#162436`）。
  - 右侧两枚圆形图标按钮：
    - 纯白圆形搜索按钮（`Icons.Rounded.Search`）：点击切换展开/收起实时搜索输入条（输入关键词即时过滤标题、公式表达式、备注口诀；搜索框带一键清空 `×`）。
    - 晴空蓝实底圆形 `+` 按钮（`PrimarySkyGradient` + 纯白 `Icons.Rounded.Add`）：点击打开 `FormulaEditorSheet` 新建公式。
- **顶部分段控制器（`SkySegmentedPill`）**：
  - 两段切换：`我的公式` | `百化分速查`。
- **视图 A：`我的公式`（严格对齐 `05_formulas.png`）**：
  - 横向可滚动分类胶囊栏（`LazyRow`）：`全部`（晴空蓝选中）、`收藏`、`增长率`、`基期`、`比重`、`平均数`、`其他高频`、`自定义`。
  - 列表（`LazyColumn`，卡片间距 `12.dp`）：每条公式渲染为高颜值纯白 `SkyCard`（`18.dp` 圆角）：
    - 顶部行：粗体标题（16sp Bold `#162436`）+ 浅天蓝 `#D6EAFF` 分类小胶囊（`#2277DF`）+ 右侧星标收藏图标按钮（已收藏实心晴空蓝 `Icons.Rounded.Star`，未收藏线框 `Icons.Rounded.StarOutline`，点击实时持久化并自动置顶排序）。
    - 中间高亮公式区：柔和冰蓝圆角条（`#EFF7FF`, `RoundedCornerShape(12.dp)`）居中展示清晰等宽感纯文本公式表达式（如 `R = r₁ + r₂ + r₁ × r₂`）。
    - 底部备注行：清朗石板蓝灰 `#5D7188` 展示速算口诀与避坑提示。
    - 交互：点击卡片打开 `FormulaEditorSheet` 进入编辑态（支持修改名称/分类/表达式/备注、复制公式或删除）；也可在卡片长按或编辑弹窗内快捷复制公式到剪贴板并弹 Snackbar 提示。
  - 空状态：当搜索无匹配或「收藏 / 自定义」为空时，展示友好晴空蓝白提示卡片 + `新建一条公式` 快捷按钮。

### 2. `FormulaEditorSheet.kt`（严格对齐 `06_formula_editor.png`）
纯白 `24.dp` 顶圆角 `ModalBottomSheet`（同时服务「新建公式」与「编辑公式」）：
- 顶栏：左侧 `新建公式` / `编辑公式`，右侧浅天蓝圆形 `×` 关闭钮。
- 表单项（可垂直滚动，防止软键盘弹出遮挡）：
  1. **公式名称**：纯白圆角输入框（`#D5E6FA` 边框，聚焦晴空蓝 `#3B93F6`），必填非空提示。
  2. **所属分类**：横向可滚动单选胶囊 `增长率 / 基期 / 比重 / 平均数 / 其他高频 / 自定义`。
  3. **公式表达式**：冰蓝微底 `#F2F8FF` + 晴空蓝描边输入框，使用 `TextFieldValue` 精准维护光标与选区！
  4. **快捷数学符号条**（紧贴表达式输入框下方，严格对齐 `06_formula_editor.png`）：
     - 横向冰蓝 `#E8F3FF` 圆角芯片：`+`、`−`、`×`、`÷`、`≈`、`%`、`()`、`r₁`、`r₂`、`≤`、`ⁿ`、`²`、`| |`。
     - **纯函数光标插入逻辑 `insertMathSymbol(value: TextFieldValue, symbol: String): TextFieldValue`**：将符号精准插入当前光标/替换选区；插入 `()` 或 `| |` 时光标自动落在符号正中间，体验丝滑！
  5. **记忆口诀 / 备注**：纯白圆角输入框（可选）。
  6. **底部操作区**：
     - 编辑模式额外提供轻量双操作行：`复制公式`（写入剪贴板）+ 珊瑚红 `删除公式`（二次确认 `AlertDialog` 后调用 `FormulaRepository.deleteFormula(id)`）。
     - 通栏晴空蓝主按钮：`保存公式`（标题与表达式均非空时可点，保存至 Room 并弹 Snackbar 反馈）。

### 3. `BaifenReferenceContent.kt`（严格对齐 `07_baifen.png` + 完整多维速查干货）
当顶部分段切至 `百化分速查` 时展示：
- **二级轻胶囊栏**（紧凑优雅，不抢风头，默认选中第一项即 1:1 呈现 `07_baifen.png`）：
  1. `常用百化分` *(默认)*：
     - **首屏置顶**：严格 1:1 还原 `07_baifen.png` 的 **2 列 × 8 行清透斑马纹速查表**（`#F5F9FE` 与 `#FFFFFF` 交替、`#DCEAFB` 细线、`1/2 = 50%` ~ `1/18 ≈ 5.6%`，点击任意格一键复制并提示）+ 底部冰蓝速算提示条 `速算提示：实战默认 ±2% 误差，截三位直除建议切至 ±1% 精算档`。
     - **向下滚动延伸**：完整展开「移位联动口诀 + `1/10`、`1/19`、`1/20` 补全」、「30 组考场高频拓展分数（按五分/八分/七分走马灯/九分/十一分/十二分分组卡片）」及「3 招非标准百分数反推技巧（`13.4% / 17.2% / 23.5% / 41.5%`）」。
  2. `年均 (1+r)ⁿ`：
     - 完整呈现 8 行 × 4 列 `(1+r)ⁿ` 精确速查矩阵卡片（表格左右对齐、等宽数字、斑马纹清透易读）+ 72 法则翻倍锚点 + 4 种年份差 `n` 口径避坑卡 + 3 大实战速算法卡片。
  3. `平方与截位`：
     - `11²` ~ `25²` 3 列整齐平方数卡片 + 尾数 5 速算口诀 + 截位直除 3 档对照表（直接关联 App `±5% / ±2% / ±1%`）+ 四大实战口诀。
  4. `易错概念`：
     - 8 条高频易错概念辨析纯白卡片（如 百分数 vs 百分点、同比 vs 环比、贡献率 vs 拉动增长率等）。

---

## 三、「统计与记录」独立一级页面（`ui/history/`）

替换 `AppNavGraph.kt` 中 `PrimaryTab.HISTORY` 的占位页，严格 1:1 还原 `design/ui_v2/08_history.png`：

### 1. `HistoryScreen.kt` & `HistoryViewModel.kt`
- **顶栏**：左侧粗体大标题 `统计与记录`（24sp Bold `#162436`），右侧 2 枚 `42.dp` 纯白圆形按钮（与 `08_history.png` 一致：导出按钮触发 Pack 6 占位提示、设置按钮可复用 `SettingsSheet` 或统一回调）。
- **顶部 3 列成长统计白卡（`SkyCard`, `20.dp` 圆角）**：
  - 实时订阅 `PracticeRepository.observeOverallStats()`：
    - 第 1 列：千分位格式化大字 `{1,280}`（0 时显示 `0`）+ `累计题数`
    - 第 2 列：晴空蓝 `#2277DF` 大字 `{92%}`（0 题显示 `—`）+ `综合正确率`
    - 第 3 列：大字 `{6.4秒}`（0 题显示 `—`）+ `平均单题`
- **3 枚横向筛选胶囊**：
  - `全部`（晴空蓝选中白字）、`估算题型`（纯白 `#D5E6FA` 边框）、`精确题型`。
- **按日分组练习历史列表（`LazyColumn`）**：
  - 纯函数 `groupHistorySessions(sessions, zoneId, today)`：按本地日期分组标题 `今天`、`昨天`、当年 `M月d日`、跨年 `yyyy年M月d日`。
  - 单条历史卡片（纯白 `SkyCard`, `16.dp` 圆角）：
    - 左侧：`44.dp` 冰蓝圆形图标 `#E8F3FF` 内居中 `#2277DF` 清晰运算符号（`MathModule.fromId(session.moduleId).operatorSymbol`，空时用 `∑`）。
    - 中间：粗体题型名 `session.moduleTitle` + 次行元数据 `{questionCount}题`（估算题拼接 ` · {toleranceShortLabel}`，开启 N-back 拼接 ` · {nBack}back`）` · {HH:mm}`。
    - 右侧：粗体正确率 `{accuracy}%` + 下方等宽总用时 `{mm:ss}`。
    - 交互：
      - **点击卡片** → 导航至 `result/{session.id}`（直接复用 Pack 4 的 `ResultScreen` 完整回看当次逐题复盘并可一键发起错题重练！）。
      - **删除记录** → 卡片右侧轻巧更多/删除操作或长按触发二次确认 `AlertDialog`，调用 `PracticeRepository.deleteSessionById(session.id)`。
  - 空状态：暂无记录时展示清爽晴空蓝白引导卡（文案 `还没有练习记录，去完成第一组速算吧` + `前往速算` 主按钮跳转 `PrimaryTab.HOME`）。

### 2. 优化 `ResultScreen` 返回栈行为（`ui/navigation/AppNavGraph.kt`）
- 在 `AppNavGraph` 中，将 `ResultRoute` 的 `onHome` 调整为优先安全返回上一页（若来自 `HistoryScreen` 则回到 `HistoryScreen`，若来自 `PracticeScreen` 因练习页已 `inclusive = true` 弹出，直接返回 `HomeScreen`；若返回栈无上一页则兜底导航到 `PrimaryTab.HOME.route`）。

---

## 四、单元测试与强制验收标准

1. **更新与新增 JVM 单元测试（`app/src/test/java/com/xingce/speedmath/`）**：
   - 更新 `DomainCatalogTest.kt`：校验 `BaifenTable.defaultFormulas` 严格为 **30 条**（5 分类计数 5/10/4/4/7、15 条默认收藏、零 LaTeX 符号 `\frac`）、`KnowledgeReference` 19 组基础百化分 + 30 组拓展分数 + 32 组 `(1+r)ⁿ` 幂次 + 15 组平方数（`11²..25²`）+ 8 条易错概念完整准确。
   - 新增 `ui/FormulaLogicTest.kt`：覆盖关键词搜索（标题/表达式/口诀）、分类与收藏筛选、`insertMathSymbol` 光标插入与 `()`/`| |` 光标居中定位、选区替换。
   - 新增 `ui/HistoryLogicTest.kt`：覆盖三列总览格式化（0 题 & 千分位）、全部/估算/精确筛选、按 `今天 / 昨天 / M月d日 / yyyy年M月d日` 分组与 `HH:mm` 格式化。
2. **强制命令验证**：
   - `./gradlew testDebugUnitTest` — Pack 1 ~ Pack 5 全部单元测试 100% 通过
   - `./gradlew assembleDebug` — 零报错生成 APK
3. **汇报内容**：列出新增/修改文件清单、与 `05`/`06`/`07`/`08` 视觉对齐要点、30 条公式与速查表接入核对结果、单测与构建结果，等待架构师审查通过后发布收官任务包 **Pack 6（真实 PDF/长图/纯文本导出 + 全局打磨 + 最终 APK 交付）**。
