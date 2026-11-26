# 任务包 Pack 6（收官包）：真实练习卷导出（PDF / 纯文本 / 高清长图）、全局打磨与最终 APK 交付

> **所属规划**：`docs/MASTER_PLAN.md` — Pack 6 / 6（最终收官任务包）  
> **工作目录**：`/home/bubu/资料分析apk`  
> **像素级视觉基准**：`/home/bubu/资料分析apk/design/ui_v2/09_export_sheet.png`  
> **前置基础**：Pack 1 ~ Pack 5 均已全部验收通过。  
> **本包目标**：打通 100% 真实的练习卷导出功能（PDF 打印卷 / 纯文本复制 / 高清长图），统一全站导出入口与视觉微细节，补齐导出单测与发布级配置，产出可直接安装到安卓手机的最终完整 APK。

---

## 一、真实练习卷导出引擎（`app/src/main/java/com/xingce/speedmath/export/`）

全部采用 Android 原生 SDK 能力实现，**零第三方重型 PDF/绘图库依赖**，兼顾体积轻量与渲染清晰度：

### 1. `WorksheetDocument.kt` & `TextWorksheetFormatter.kt`（纯 Kotlin，支持 JVM 单测）
- **核心模型**：
  - `enum class ExportFormat(val label: String)`：`PDF("PDF 打印卷")`, `TEXT("纯文本复制")`, `IMAGE("高清长图")`
  - `data class ExportRequest(val module: MathModule, val count: Int, val tolerance: ToleranceLevel, val customConfig: CustomQuizConfig, val format: ExportFormat, val includeAnswers: Boolean)`
  - `data class WorksheetDocument(val title: String, val subtitle: String, val questions: List<Question>, val includeAnswers: Boolean, val generatedAtLabel: String)`
- **题目与答案精确格式化函数（纯函数，全覆盖单测）**：
  - `formatQuestionLine(number: Int, question: Question): String`：
    - `MAKE_HUNDRED`（表达式含 `?`）：将 `?` 替换为 `(____)`，如 `"1. 64 + (____) = 100"`
    - `isPercentAnswer == true`：如 `"2. 534 ÷ 4218 ≈ ______ %"`
    - 其他估算题：`"1. 54321 ÷ 213 ≈ ______"`（或 `= ______`，对齐 `09_export_sheet.png` 推荐常规统一 `= ______`，百分比题型加 `%`）
  - `formatAnswerLine(number: Int, question: Question): String`：
    - 精确题型：`"1. 115"`（自定义精确百分比则加 `%`）
    - 估算题型：基于 `BigDecimal` 精确计算允许误差上下界 `low = exact * (1 - ratio)`、`high = exact * (1 + ratio)`（保留 1 位小数 `HALF_UP`），输出如 `"1. 255.0（±2% 区间：249.9 ~ 260.1）"`；百分比题型附 `%`（如 `"2. 12.7%（±2% 区间：12.4% ~ 12.9%）"`）。
  - `TextWorksheetFormatter.format(document: WorksheetDocument): String`：输出排版工整的纯文本题卷（含标题、生成时间、题量/误差说明、带序号题目区；若 `includeAnswers == true`，在分隔线后整齐列出参考答案与误差区间）。

### 2. `PdfWorksheetGenerator.kt`（原生 `android.graphics.pdf.PdfDocument`）
- **A4 标准排版**（`595 × 842` pt，清晰中文字体 `Typeface.DEFAULT` / `DEFAULT_BOLD`）：
  - 首页顶部晴空蓝题头栏：`行测速算 · {moduleTitle} 练习卷`，副行含 `题量：{count}题 · 误差标准：{tolerance} · 日期：{yyyy-MM-dd}` 及手写填写栏 `用时：____分____秒   正确率：____%`。
  - **2 列整齐算式区**（`MULTI_ADD` / 超长算式自动占满整行防止文字截断重叠），题间留出合理打草稿空间。
  - **自动多页分页**：支持 `10 / 20 / 30 / 50` 题全部工整排版，每页底部居中页码 `第 X / Y 页`。
  - 若 `includeAnswers == true`：自动另起一页打印 `参考答案与误差区间`（同样支持自动分页）。
- 文件输出：写入 `context.cacheDir/exports/xingce_${moduleId}_${timestamp}.pdf`，并在写入前清理超过 10 个的旧缓存文件防止缓存膨胀。

### 3. `ImageWorksheetGenerator.kt`（原生 `Canvas` + `Bitmap` + `MediaStore`）
- 绘制晴空蓝白风格高清长图（宽 `1080px`，按题量与是否含答案动态计算精确高度，使用 `Bitmap.Config.ARGB_8888`，并保证 50 题高清晰且不触发 OOM）：
  - 顶部晴空蓝渐变圆角卡片题头 + 纯白圆角题目纸张区（2 列或单列清爽排版，超长算式自动单列防重叠）+ 可选底部淡冰蓝参考答案卡片 + 页脚 `行测速算 · 资料分析速算训练`。
- **免危险权限保存策略**：
  - `API >= 29`（Android 10+）：通过 `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` 直接写入系统相册 `Pictures/行测速算/`（使用 `IS_PENDING` 原子写入，无需存储权限），同时也可将一份分享副本或返回的 `MediaStore` Uri 供分享。
  - `API 26..28`：写入 `context.cacheDir/exports/xingce_${moduleId}_${timestamp}.png` 并通过 `FileProvider` 调起系统分享/保存（无需危险权限）。

### 4. `FileProvider` 与 `WorksheetExporter.kt`
- 在 `app/src/main/res/xml/file_paths.xml` 配置 `<cache-path name="exports" path="exports/" />`。
- 在 `AndroidManifest.xml` 注册 `androidx.core.content.FileProvider`（`authorities="${applicationId}.fileprovider"`，`exported="false"`，`grantUriPermissions="true"`）。
- `WorksheetExporter`：
  - 所有生成与 IO 均在 `Dispatchers.IO` 执行，确保主线程零卡顿。
  - 返回密封结果 `ExportOutcome`：`CopiedText(count)`、`PdfReady(uri, fileName)`、`ImageSaved(uri, savedToGallery, fileName)`、`Failed(message)`。
  - 调起系统分享 `Intent.ACTION_SEND` 时务必加 `Intent.FLAG_GRANT_READ_URI_PERMISSION`，通过 `Intent.createChooser` 启动并用 `runCatching` 兜底无可用目标应用异常。

---

## 二、导出练习卷弹窗（`ui/export/ExportSheet.kt` & `ExportViewModel.kt`）

严格 1:1 还原 `design/ui_v2/09_export_sheet.png`：
1. **顶栏**：左侧 `导出练习卷`，右侧浅天蓝圆形关闭按钮 `×`。
2. **题型选择条**（冰蓝 `#E8F3FF` 圆角行）：左侧 `题型`，右侧晴空蓝文字 `{selectedModule.title} ›`；点击弹出简洁题型选择弹窗/列表，支持从全部 **17 预设题型 + 自定义出题** 任选（打开 `ExportSheet` 时默认带入当前首页选中题型或上次练习题型）。
3. **导出题量**：4 枚等宽圆角胶囊 `10题`、`20题`（默认选中晴空蓝）、`30题`、`50题`。
4. **导出格式**：3 张等宽纯白圆角卡片（配清晰圆润线性图标，严格对齐 `09_export_sheet.png`）：
   - `PDF 打印卷`（默认选中：冰蓝底 `#E8F3FF` + 晴空蓝边框 `#3B93F6` + 蓝色图标）
   - `纯文本复制`
   - `高清长图`
5. **参考答案开关行**（冰蓝 `#E8F3FF` 圆角行）：`附带参考答案与误差区间` + 晴空蓝 Switch（默认开启）。
6. **纸张感 3 题实时预览卡片**（纯白 `#FFFFFF` + `#D5E6FA` 边框）：
   - 实时展示 3 道由 `QuestionGenerator` 生成的真实样题（等宽字体、与最终导出格式一致），切换题型时自动刷新，并附一个轻量小图标按钮可手动“换一批预览”。
7. **底部主按钮**：通栏晴空蓝 `SkyPrimaryButton("生成导出")`（导出中显示 `生成中…` 并防重复点击）：
   - `纯文本复制`：直接写入系统剪贴板并弹 Snackbar `已复制 {count} 道题到剪贴板`，关闭弹窗。
   - `PDF 打印卷`：生成 PDF 后直接调起系统分享/打印面板并给出成功提示。
   - `高清长图`：保存至系统相册（API 29+）并弹 Snackbar（附 `分享` 按钮可一键分享图片），低版本直接唤起系统分享保存。

---

## 三、全站接线与收官视觉/工程打磨

1. **接通全部导出入口**：
   - 移除 `HomeViewModel.kt` 和 `HistoryScreen.kt` 中所有“导出练习卷将在 Pack 6 启用”的占位文案！
   - 将 **首页工作台 (`HomeScreen`)** 与 **统计与记录页 (`HistoryScreen`)** 顶栏的导出按钮统一图标风格（统一使用 `Icons.Rounded.IosShare`），点击均打开真实的 `ExportSheet`！
2. **全站背景色 100% 统一**：
   - 将 `FormulaScreen.kt` 与 `HistoryScreen.kt` 的根画布底色从 `SurfaceSelectedCard` (`#EDF6FF`) 统一为标准晴空画布色 `BgSkyWhite` (`#F4F8FD`)，确保 3 个一级 Tab 切换时背景零色差跳变！
3. **发布级构建健全性检查**：
   - 检查 `app/proguard-rules.pro`（如缺失则补充标准 Room/DataStore/Compose 安全规则），确保应用可稳定构建 Release/Debug APK。
   - 检查全项目无残留 `TODO`、无未使用的占位页代码、无硬编码死链接。

---

## 四、单元测试与最终收官验收门禁

1. **新增 JVM 单元测试（`app/src/test/java/com/xingce/speedmath/export/WorksheetExportTest.kt`）**：
   - 校验 `formatQuestionLine` 对常规四则运算、百分比除法（`三位数除四位数`）、凑整百填空（`MAKE_HUNDRED`）及自定义题型的输出格式。
   - 校验 `formatAnswerLine` 对精确题、估算题（验证 `±1% / ±2% / ±5%` 允许区间上下界 `HALF_UP` 计算准确）及百分比题型的输出格式。
   - 校验 `TextWorksheetFormatter` 在 `includeAnswers = false` 与 `includeAnswers = true` 时的完整文本结构，以及 10/20/30/50 题卷生成数量与题号连续性。
2. **最终全量验收命令（全部必须 0 错误）**：
   - `./gradlew testDebugUnitTest --rerun-tasks` — 全项目 Pack 1 ~ Pack 6 全部单测 100% 通过！
   - `./gradlew assembleDebug` — 生成最终可真机安装的 APK（`app/build/outputs/apk/debug/app-debug.apk`）。
   - `./gradlew lintDebug` — 检查确认无 Error 级别阻断问题（若有 Error 必须修复）。
3. **收官交付汇报**：汇报新增/修改文件清单、全部单元测试通过总数、Lint 检查结果、最终 APK 路径与文件大小，等待架构师进行最终验收！
