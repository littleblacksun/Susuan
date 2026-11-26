# 《行测速算》Android App — 终极开发任务书（Master Engineering Plan）

> **项目路径**：`/home/bubu/资料分析apk`  
> **视觉定稿基准**：`/home/bubu/资料分析apk/design/ui_v2/01_home.png` ~ `09_export_sheet.png`（Apple HIG × Google Material 3 晴空蓝白体系）  
> **应用图标素材**：`/home/bubu/资料分析apk/app封面/app封面.jpg`  
> **协作机制**：全项目拆分为 **6 个原子化任务包（Pack 1 ~ Pack 6）**。每次只下发 1 个任务包给 Codex 执行；每包完成后须通过自动化测试/编译与架构师人工代码审查，合格后再解锁下一包。

---

## 一、V2 UI 视觉定稿审核结论

已逐张审查 `design/ui_v2/` 下全部 **9 张**晴空蓝白设计图及 `MANIFEST.md`，**正式定稿通过**：
- **视觉水准**：晴空蓝白配色极其清透锐利，3-Tab 架构（`速算` / `公式` / `统计`）、首页今日看板 + 2 列 Bento 题型卡片、沉浸式答题页与悬浮白键帽键盘、蓝点阵草稿画布、独立「公式本」与新建公式快捷符号栏、百化分速查表、统计页与导出卷弹窗高度统一、清晰美观。
- **开发阶段自动修正的 3 处图稿微细节**：
  1. 数学与舍入：引擎使用 `BigDecimal` 精确计算真实值与相对误差（标准 `HALF_UP` 四舍五入保留 1 位小数，杜绝图稿演示中的舍入误差）。
  2. 图标一致性：底栏 3-Tab 图标与全站线框图标统一使用 Material Icons Rounded / 自定义矢量 Vector，保证跨页 100% 像素统一。
  3. 旧代码清理：Pack 1 第一步彻底清理早期残留在 `app/src/` 中的旧绿色半成品文件，基于晴空蓝白 V2 设计规范全新干净落地。

---

## 二、总体技术架构锁定

### 2.1 运行与构建环境
- **宿主实测环境**：OpenJDK 21 + Android SDK 34（`/home/bubu/Android/Sdk`，`platforms/android-34`，`build-tools/34.0.0`）
- **应用标识**：包名 `com.xingce.speedmath` · 应用名 `行测速算` · `minSdk = 26` · `compileSdk = 34` · `targetSdk = 34` · 竖屏锁定 + Edge-to-Edge 安全区适配
- **核心依赖版本**：
  - Gradle `8.7` · AGP `8.5.2` · Kotlin `2.0.20` · KSP `2.0.20-1.0.25`
  - Jetpack Compose BOM `2024.09.00` · Material 3 · `material-icons-extended`
  - Navigation Compose `2.8.0` · Lifecycle ViewModel Compose `2.8.5`
  - Room `2.6.1`（`room-runtime`, `room-ktx`, `room-compiler` via KSP）— 本地 SQLite 离线持久化
  - DataStore Preferences `1.1.1` — 用户偏好与练习默认配置
  - 原生 `android.graphics.pdf.PdfDocument` + `Canvas/Bitmap` + `MediaStore` + `FileProvider` — 零冗余依赖实现真实 PDF / 高清长图 / 纯文本导出
  - JUnit 4 + Kotlin Coroutines Test — 核心引擎与数据层全覆盖单元测试

### 2.2 清晰分层目录架构

```text
app/src/main/java/com/xingce/speedmath/
├── SpeedMathApp.kt                    # Application 级轻量依赖容器（DI Container）
├── MainActivity.kt                    # 入口 Activity（Edge-to-Edge、主题挂载）
├── domain/                            # 纯 Kotlin 业务引擎（零 Android 依赖，可跑纯 JVM 单测）
│   ├── model/
│   │   ├── MathModule.kt              # 17种预设题型 + 自定义题型元数据、分类、算式示例
│   │   ├── ToleranceLevel.kt          # 精确(0%) / 精算(±1%) / 实战(±2%) / 宽松(±5%)
│   │   ├── CustomQuizConfig.kt        # 自定义出题参数模型
│   │   ├── Question.kt                # 题目模型（算式、BigDecimal精确值、展示单位、估算提示）
│   │   ├── GradeResult.kt             # 判题结果（EXACT / PASS / FAIL、带符号相对误差%）
│   │   └── PracticeConfig.kt          # 题量、键盘顺序、草稿开关、N-back档位
│   ├── engine/
│   │   ├── QuestionGenerator.kt       # 17种题型 + 自定义题型高质量随机出题器
│   │   ├── AnswerGrader.kt            # BigDecimal 精确与相对误差判题引擎
│   │   └── NBackController.kt         # N-back（0/1/2）队列状态机（含记忆期与收尾补答）
│   └── data/
│       └── BaifenTable.kt             # 16组常用百化分常量表 + 预置行测高频公式种子数据
├── data/                              # 本地持久化层（Room + DataStore）
│   ├── db/
│   │   ├── AppDatabase.kt             # Room Database（v1，含首次预置公式回调）
│   │   ├── PracticeEntities.kt        # PracticeSessionEntity, PracticeItemEntity
│   │   ├── FormulaEntity.kt           # FormulaEntity（标题、分类、表达式、备注、收藏、时间）
│   │   ├── PracticeDao.kt             # 历史查询、今日统计聚合、总览聚合、删除
│   │   └── FormulaDao.kt              # 公式 CRUD、搜索、分类/收藏筛选、置顶排序
│   ├── prefs/
│   │   └── UserPreferencesStore.kt    # DataStore（上次题型、默认题量/误差/键盘/草稿/N-back/震动/自定义配置）
│   └── repository/
│       ├── PracticeRepository.kt
│       └── FormulaRepository.kt
├── export/                            # 真实导出引擎
│   ├── WorksheetExporter.kt           # 统一导出协调器
│   ├── PdfWorksheetGenerator.kt       # 原生 PdfDocument A4 打印卷（2列排版+可选答案页）
│   ├── ImageWorksheetGenerator.kt     # Canvas 高清晴空蓝白长图 + MediaStore 保存相册
│   └── TextWorksheetFormatter.kt      # 整齐纯文本题卷格式化 + 剪贴板复制
└── ui/                                # Jetpack Compose 晴空蓝白 UI 层（严格对齐 design/ui_v2/）
    ├── theme/                         # Color.kt, Type.kt, Theme.kt（精确对齐 V2 晴空蓝白色板）
    ├── components/                    # SkyCard, SkyPrimaryButton, SegmentedPill, MathKeypad, ScratchpadCanvas, AccuracyRing, BottomNavBar
    ├── navigation/                    # AppNavGraph.kt（3-Tab 主壳 + 练习/报告子路由）
    ├── home/                          # HomeScreen, HomeViewModel, CustomConfigSheet, SettingsSheet
    ├── practice/                      # PracticeScreen, PracticeViewModel
    ├── result/                        # ResultScreen, ResultViewModel（支持实时结算 & 历史详情回看）
    ├── formula/                       # FormulaScreen, FormulaViewModel, FormulaEditorSheet, BaifenTabContent
    ├── history/                       # HistoryScreen, HistoryViewModel
    └── export/                        # ExportSheet, ExportViewModel
```

---

## 三、6 大分步执行任务包总览（逐包交付 · 逐包审查）

### Pack 1：工程清理、晴空蓝白 Design System、App 启动图标与 Room/DataStore 数据层
- **核心目标**：
  1. 彻底清理 `app/src/` 下早期残留的旧绿色半成品代码，升级 `build.gradle.kts` 接入 KSP、Room 2.6.1、DataStore Preferences、ViewModel Compose、JUnit 4。
  2. 以 `/home/bubu/资料分析apk/app封面/app封面.jpg` 生成多分辨率 Android 启动图标（`mipmap-mdpi`~`xxxhdpi` + Adaptive Icon XML）。
  3. 严格按 `design/ui_v2/01_home.png` 搭建 **晴空蓝白主题**（`#F4F8FD` 画布、`#FFFFFF` 纯白卡片 + `#D5E6FA` 边框、`#E8F3FF` 冰蓝雾、`#3B93F6` 晴空蓝主色、`#162436` 深藏青炭黑文字、等宽数字排版）与基础复用组件。
  4. 搭建 Room 数据库（`PracticeSessionEntity`、`PracticeItemEntity`、`FormulaEntity` + 首次启动预置 10 条行测高频资料分析公式）、DataStore 偏好存储、Repository 与依赖容器 `SpeedMathApp`。
- **交付验证门禁**：`./gradlew testDebugUnitTest assembleDebug` 零报错；无旧绿色文件残留；图标资源完整。

---

### Pack 2：核心算术引擎（17 题型 + 自定义出题 + 相对误差判题 + N-back 状态机）
- **核心目标**（纯 Kotlin `domain/` 层，高覆盖单元测试）：
  1. **17 种速算题型出题器**（贴合行测数感，严禁无意义全 0/除数为 0/过于简单凑数）：
     - 加减基础（7 项）：`两位数加减`、`凑整百练习`（如 `68 + 32` 或 `268 + 132` / 补数填空或凑整和）、`三位数加法`、`三位数减法`、`三位数加减`、`多数相加`（4~5 个三位数连加）、`混合加减`（3~4 项加减混合）。
     - 乘法速算（5 项）：`两位数乘一位数`、`三位数乘一位数`、`两位数乘11`、`两位数乘15`、`两位数乘两位数`。
     - 除法估算（5 项）：`三位数除一位数`、`三位数除两位数`、`乘法估算`（如三位数×两位百分比或三位数×三位数，按行测放缩估算）、`五位数除三位数`、`三位数除四位数`（结果转为百分比 `%` 数值，如 `12.66%`）。
  2. **自定义出题器**：支持 `加法/减法/乘法/除法/混合加减`、第 1 数 `1~5` 位、第 2 数 `1~4` 位、是否允许负数、是否以 `%` 作答、自定义误差档位。
  3. **精确 & 相对误差判题引擎（`AnswerGrader`）**：
     - 全程 `BigDecimal` 运算；精确类题型严格 `0` 误差匹配。
     - 估算类题型计算带符号相对误差 $\delta = \frac{y_{\text{作答}} - y_{\text{精确}}}{|y_{\text{精确}}|}$，标准四舍五入保留 1 位小数展示（如 `+0.9%` / `-1.2%`）：
       - $|\delta| \le 0.5\%$ → `EXACT`（🟢精准）
       - $0.5\% < |\delta| \le \text{阈值（1\% / 2\% / 5\%）}$ → `PASS`（🔵估算通过）
       - $|\delta| > \text{阈值}$ → `FAIL`（🔴偏差过大）
     - 容错解析用户输入（空字符串、单负号、多余小数点安全拦截不崩溃）。
  4. **N-back 状态机（`NBackController`）**：完整支持 `OFF(0)` / `1-back` / `2-back`，清晰划分「记忆前置期（前 N 题）」→「常规作答期（看第 $i$ 题答第 $i-N$ 题）」→「收尾回忆期（补答最后 N 题）」。
- **交付验证门禁**：编写完整 JUnit 单元测试覆盖全部 17 题型约束（位数范围、非零除数、百分比范围）、自定义配置、误差边界（0.5%、1%、2%、5%、负数、非法输入）及 N-back 全生命周期，`./gradlew testDebugUnitTest` 100% 全绿通过。

---

### Pack 3：首页工作台、自定义出题弹窗、偏好设置与 3-Tab 主壳（对齐 `01_home.png`）
- **核心目标**：
  1. 搭建主导航框架 `AppNavGraph` + 底部 3-Tab 导航栏（`速算` / `公式` / `统计`，风格严格对齐 `01/05/08`）。
  2. 1:1 高保真实现 **首页工作台（`HomeScreen`）**：
     - 顶栏（`行测速算` + 右侧纯白圆形 `导出` / `设置` 图标钮）。
     - 晴空蓝渐变 **今日速算看板 Hero 卡**：实时读取 Room 今日题数、今日正确率、今日均速；右侧 `继续上次 ›` 一键复用上次配置进入练习（首次无记录时展示引导文案）。
     - 4 分类胶囊：`除法估算`（默认选中）、`加减基础`、`乘法速算`、`全部(17)`。
     - 2 列宽敞纯白 Bento 题型卡片：带题型名、等宽算式示例、`估` 浅蓝角标、选中态晴空蓝 2px 边框 + 冰蓝底 + 右上蓝色勾选圆标；含 `+ 自定义出题` 虚线轻卡。
     - 底部紧凑白卡「参数控制台」：`题量(10/20/30/50题)`、`误差档位(精算±1% / 实战±2% / 宽松±5%，精确题型自动显示精确匹配)`、`键盘顺序(正序/倒序/乱序)`、`草稿板(开/关)` + 通栏晴空蓝 `开始练习` 主按钮。
  3. 实现 **自定义出题 BottomSheet**（实时保存至 DataStore 并选中）。
  4. 实现 **偏好设置 BottomSheet**：N-back 档位切换（`关闭 / 1-back / 2-back` + 玩法说明）、按键震动开关、误差标准说明、触控笔模式说明。
- **交付验证门禁**：`./gradlew assembleDebug` 通过；逐项对照 `design/ui_v2/01_home.png` 审核视觉还原度与交互流畅度。

---

### Pack 4：沉浸式答题练习页、蓝点阵手写草稿板、三态自定义键盘与练习报告页（对齐 `02`、`03`、`04`）
- **核心目标**：
  1. 1:1 实现 **答题练习页（`PracticeScreen`）**：
     - 顶栏（退出防误触确认弹窗、题型名 + 误差/精确标签、实时等宽计时器 `mm:ss`、进度 `3/10` + 圆头晴空蓝进度条）。
     - N-back 激活态顶部清晰提示胶囊（区分「记忆第 k 题，点击继续」与「请输入前 N 题答案」）。
     - 大字清晰算式白卡（`多数相加` 优雅多行排版、`三位数除四位数` 自动显示 `%` 后缀）+ 晴空蓝描边输入框（实时闪烁光标）+ 提交后分级反馈胶囊（`精准` / `估算通过 +误差%` / `偏差过大 +精确值`，停留短暂反馈后流畅切下一题）。
  2. **高流畅度手写草稿板（`ScratchpadCanvas`）**：
     - 纯白蓝点阵手账纸风格（对齐 `03_practice_stylus.png`），支持手指/触控笔平滑贝塞尔曲线绘制；
     - 右上角 4 键极简悬浮工具条：`撤销`、`一键清空`、`笔触粗细切换`、`收起/展开`；切题自动清空画布。
  3. **自定义 4×4 悬浮白键帽键盘（`MathKeypad`）**：
     - 全程禁用系统输入法，冰蓝底盘 + 纯白圆角键帽 + 跨两行晴空蓝 `下一题` 主按钮；
     - 真实支持 `正序(1-9)` / `倒序(7-9在上)` / `乱序(每题随机洗牌1-9)` 实时切换 + 可控触觉反馈（HapticFeedback）。
  4. 1:1 实现 **练习报告页（`ResultScreen`，对齐 `04_result.png`）**：
     - 练习结束自动持久化保存 `PracticeSessionEntity` + 全部 `PracticeItemEntity` 到 Room。
     - Canvas 自绘圆头晴空蓝正确率环形仪表盘 + `总用时` / `均速` / `平均误差`。
     - 逐题复盘列表（题号胶囊、算式、你的答案 vs 精确答案、单题耗时、三色语义状态徽章）。
     - 底部双按钮：`错题重练`（精准提取本次错题生成专项复盘练习；全对时置灰或提示“全对太棒了”）+ `再来一组`。
- **交付验证门禁**：`./gradlew testDebugUnitTest assembleDebug` 通过；全流程对照 `02`/`03`/`04` 逐项审查。

---

### Pack 5：「公式本」独立模块 + 百化分速查表 + 「统计与记录」主页（对齐 `05`、`06`、`07`、`08`）
- **核心目标**：
  1. 1:1 实现 **公式本主页（`FormulaScreen`，对齐 `05_formulas.png`）**：
     - 顶栏 `公式本` + 实时关键词搜索展开栏 + 晴空蓝圆形 `+` 新增公式按钮。
     - 顶部分段切换：`我的公式` | `百化分速查`。
     - 分类胶囊筛选：`全部`、`收藏`、`增长率`、`基期`、`比重`、`平均数`、`自定义`。
     - 纯白公式卡片：标题 + 分类浅蓝标 + 星标一键收藏（收藏自动置顶）+ 冰蓝高亮公式框 + 记忆口诀备注；支持点击卡片编辑、长按或菜单删除（带二次确认）。
  2. 1:1 实现 **新建/编辑公式弹窗（`FormulaEditorSheet`，对齐 `06_formula_editor.png`）**：
     - 公式名称、所属分类胶囊、冰蓝公式表达式输入框 + **可真实向光标处插入字符的快捷数学符号栏**（`+`, `−`, `×`, `÷`, `≈`, `%`, `()`, `r₁`, `r₂`, `≤`）、记忆口诀/备注、校验并写入 Room 持久化。
  3. 1:1 实现 **百化分速查表（`07_baifen.png`）**：
     - 2 列 × 8 行共 16 组清透蓝白斑马纹速查表（`1/2` 至 `1/18`）+ 点击可高亮/复制 + 底部误差速算提示卡。
  4. 1:1 实现 **统计与记录主页（`HistoryScreen`，对齐 `08_history.png`）**：
     - 实时读取 Room 聚合数据：累计题数、综合正确率（晴空蓝大字）、平均单题耗时；空状态友好引导。
     - `全部` / `估算题型` / `精确题型` 实时筛选；按 `今天` / `昨天` / `MM月dd日` 自动分组展示。
     - 点击任意记录跳转至 `ResultScreen` 完整回看当次逐题明细并支持发起错题重练；支持删除单条记录。
- **交付验证门禁**：`./gradlew testDebugUnitTest assembleDebug` 通过；公式 CRUD/收藏/搜索及历史详情联动经代码审查通过。

---

### Pack 6：真实练习卷导出（PDF / 纯文本 / 高清长图）、全局打磨与最终 APK 交付（对齐 `09_export_sheet.png`）
- **核心目标**：
  1. 1:1 实现 **导出练习卷弹窗（`ExportSheet`，对齐 `09_export_sheet.png`）** 并接入 **100% 真实导出引擎**：
     - 支持切换任意题型（17 种 + 自定义）、导出题量（`10 / 20 / 30 / 50 题`）、导出格式三选一（`PDF 打印卷` / `纯文本复制` / `高清长图`）、`附带参考答案与误差区间` 开关、实时生成的 3 道样题预览卡片。
     - **PDF 打印卷**：原生 `PdfDocument` 渲染标准 A4 清晰中文打印卷（自动分页、双列整齐算式、勾选答案时末页附参考答案与误差范围），配置 `FileProvider` 调起系统分享/打开。
     - **纯文本复制**：生成整齐纯文本练习卷写入系统剪贴板并弹出 Snackbar 确认。
     - **高清长图**：`Canvas` + `Bitmap` 绘制晴空蓝白高清晰长图练习卷，通过 `MediaStore` 无感保存至系统相册 `Pictures/行测速算/` 并支持一键分享。
  2. **全局回归打磨与最终构建验收**：
     - 全面核查 Edge-to-Edge 沉浸式状态栏/导航栏内边距、小屏滚动防截断、空数据状态、内存防泄漏、R8/ProGuard 规则。
     - 全量执行 `./gradlew testDebugUnitTest assembleDebug`，产出可真机直接安装的完整 APK，输出最终功能验收清单。

---

## 四、审查与推进规则

1. **你的确认节点**：审阅完本《终极开发任务书》后，告诉我是否需要调整任何功能点或任务包划分；
2. **逐包执行**：确认后，我会按 **Pack 1 → Pack 6** 的顺序，每次单独出具一份精准细化的执行任务包（如 `docs/PACK_01.md`）交由你发给 Codex；
3. **逐包验收**：每包完成后你通知我，我会直接检查 Codex 的代码改动、运行测试与编译审查；发现任何问题立即出具修订清单让 Codex 修复，验收 100% 合格再发下一包。
