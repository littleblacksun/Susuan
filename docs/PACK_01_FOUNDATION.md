# 任务包 Pack 1：工程清理、晴空蓝白 Design System、App 图标与 Room/DataStore 数据层

> **所属规划**：`docs/MASTER_PLAN.md` — Pack 1 / 6  
> **工作目录**：`/home/bubu/资料分析apk`  
> **视觉基准**：`design/ui_v2/01_home.png`（晴空蓝白体系）  
> **图标素材**：`app封面/app封面.jpg`  
> **本包边界**：仅完成「旧代码清理 + Gradle 依赖升级 + 启动图标 + 晴空蓝白主题/基础组件 + Domain 基础模型与常量 + Room/DataStore 数据层 + 最小可启动骨架与单测」。**严禁**在本包提前实现 Pack 2 的完整出题/判题算法、Pack 3~6 的完整业务页面或导出引擎。

---

## 一、必须清理的旧版残留文件

项目早期残留了一批旧绿色主题半成品代码，**请先删除以下全部旧文件**，确保代码库 100% 干净无污染：
- `app/src/main/java/com/xingce/speedmath/ui/components/InfoDialogs.kt`
- `app/src/main/java/com/xingce/speedmath/ui/components/MathKeypad.kt`
- `app/src/main/java/com/xingce/speedmath/ui/components/ScratchpadCanvas.kt`
- `app/src/main/java/com/xingce/speedmath/ui/components/SegmentedControl.kt`
- `app/src/main/java/com/xingce/speedmath/ui/components/UiComponents.kt`
- `app/src/main/java/com/xingce/speedmath/ui/model/MockData.kt`
- `app/src/main/java/com/xingce/speedmath/ui/model/PracticeUiState.kt`
- `app/src/main/java/com/xingce/speedmath/ui/model/UiModels.kt`
- `app/src/main/java/com/xingce/speedmath/ui/screens/CustomConfigSheet.kt`
- `app/src/main/java/com/xingce/speedmath/ui/screens/ExportSheet.kt`
- `app/src/main/java/com/xingce/speedmath/ui/screens/HomeScreen.kt`
- `app/src/main/java/com/xingce/speedmath/ui/theme/Color.kt`
- `app/src/main/java/com/xingce/speedmath/ui/theme/Theme.kt`
- `app/src/main/java/com/xingce/speedmath/ui/theme/Type.kt`

---

## 二、Gradle 依赖与工程配置

保持宿主环境锁定（JDK 21、`compileSdk = 34`、`targetSdk = 34`、`minSdk = 26`、`buildToolsVersion = "34.0.0"`、`local.properties` 指向 `/home/bubu/Android/Sdk`、`gradle.properties` 不动）：

1. **根目录 `build.gradle.kts`**：
   - 保留 AGP `8.5.2`、Kotlin `2.0.20`、Compose Plugin `2.0.20`。
   - 新增 KSP 插件：`id("com.google.devtools.ksp") version "2.0.20-1.0.25" apply false`。
2. **`app/build.gradle.kts`**：
   - 应用 `com.google.devtools.ksp`。
   - `versionCode = 1`、`versionName = "1.0.0"`。
   - 依赖清单：
     - Compose BOM `2024.09.00` + `activity-compose:1.9.2` + `foundation` + `ui` + `ui-tooling-preview` + `material3` + `material-icons-extended`
     - `androidx.navigation:navigation-compose:2.8.0`
     - `androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.5`
     - `androidx.lifecycle:lifecycle-viewmodel-compose:2.8.5`
     - `androidx.lifecycle:lifecycle-runtime-compose:2.8.5`
     - `androidx.room:room-runtime:2.6.1`、`androidx.room:room-ktx:2.6.1`、`ksp("androidx.room:room-compiler:2.6.1")`
     - `androidx.datastore:datastore-preferences:1.1.1`
     - `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1`
     - `testImplementation("junit:junit:4.13.2")`
     - `testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")`
   - 添加 `ksp { arg("room.generateKotlin", "true") }`。
3. **`.gitignore`**：若缺失，创建标准 Android `.gitignore`（忽略 `local.properties`、`.gradle/`、`build/`、`app/build/`、`.idea/`、`*.iml`）。

---

## 三、App 启动图标与 Manifest 配置

1. **基于 `app封面/app封面.jpg` 生成全套高质量启动图标**：
   - 生成 `mipmap-mdpi`(48px)、`mipmap-hdpi`(72px)、`mipmap-xhdpi`(96px)、`mipmap-xxhdpi`(144px)、`mipmap-xxxhdpi`(192px) 的 `ic_launcher.png` 与 `ic_launcher_round.png`。
   - 同时生成 Adaptive Icon 前景图 `ic_launcher_foreground.png`（各密度 `108dp` 对应像素：108/162/216/324/432px），将原图居中缩放至约 68%~72% 安全区并衬以干净浅色边缘，防止圆形/圆角矩形桌面遮罩裁切小熊耳朵！
   - 创建 `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` 与 `ic_launcher_round.xml`（背景色 `#F4F8FD` 或 `#FFFFFF`，前景 `@mipmap/ic_launcher_foreground`）。
2. **`AndroidManifest.xml`**：
   - 注册 `android:name=".SpeedMathApp"`、`android:label="@string/app_name"`（`行测速算`）、`android:icon="@mipmap/ic_launcher"`、`android:roundIcon="@mipmap/ic_launcher_round"`。
   - 注册 `MainActivity` 为 `LAUNCHER`，锁定竖屏 `android:screenOrientation="portrait"`。
3. **`res/values/strings.xml` 与 `res/values/themes.xml`**：
   - `app_name` = `行测速算`。
   - `themes.xml` 采用简洁浅色系统窗体背景 `#F4F8FD`，无原生 ActionBar。

---

## 四、Domain 基础模型与常量（`domain/model/` & `domain/data/`）

创建以下纯 Kotlin 模型（不依赖 Android Context，为 Pack 2~6 提供稳定契约）：

1. **`domain/model/MathModule.kt`**：
   - `enum class ModuleCategory(val displayName: String)`：
     - `DIVISION_ESTIMATION("除法估算")`
     - `ADD_SUB("加减基础")`
     - `MULTIPLICATION("乘法速算")`
   - `enum class MathModule(...)`：包含完整 **17 个预设速算题型** + `CUSTOM`（共 18 项），字段含 `id: String`, `title: String`, `sampleExpression: String`, `category: ModuleCategory?`, `isEstimation: Boolean`, `isPercentAnswer: Boolean`, `operatorSymbol: String`：
     - **除法估算（5 项，`isEstimation = true`）**：
       1. `FIVE_DIV_THREE`：`五位数除三位数` · `54321 ÷ 213` · `÷`
       2. `THREE_DIV_FOUR`：`三位数除四位数` · `534 ÷ 4218 → %` · `÷` · `isPercentAnswer = true`
       3. `THREE_DIV_TWO`：`三位数除两位数` · `736 ÷ 42` · `÷`
       4. `THREE_DIV_ONE`：`三位数除一位数` · `845 ÷ 7` · `÷`
       5. `MUL_ESTIMATION`：`乘法估算` · `428 × 19.6%` · `×`
     - **加减基础（7 项，`isEstimation = false`）**：
       6. `TWO_ADD_SUB`：`两位数加减` · `68 + 47` · `±`
       7. `MAKE_HUNDRED`：`凑整百练习` · `64 + 36` · `+`
       8. `THREE_ADD`：`三位数加法` · `458 + 376` · `+`
       9. `THREE_SUB`：`三位数减法` · `842 - 367` · `-`
       10. `THREE_ADD_SUB`：`三位数加减` · `529 + 384` · `±`
       11. `MULTI_ADD`：`多数相加` · `142 + 238 + 315` · `+`
       12. `MIXED_ADD_SUB`：`混合加减` · `426 - 189 + 254` · `±`
     - **乘法速算（5 项，`isEstimation = false`）**：
       13. `TWO_MUL_ONE`：`两位数乘一位数` · `67 × 8` · `×`
       14. `THREE_MUL_ONE`：`三位数乘一位数` · `246 × 7` · `×`
       15. `TWO_MUL_11`：`两位数乘11` · `48 × 11` · `×`
       16. `TWO_MUL_15`：`两位数乘15` · `64 × 15` · `×`
       17. `TWO_MUL_TWO`：`两位数乘两位数` · `36 × 47` · `×`
     - **自定义（1 项）**：
       18. `CUSTOM`：`自定义出题` · `自由设位数与误差` · `category = null`
   - 提供便捷伴生属性：`val presets: List<MathModule>`（仅 17 项预设）、`fun byCategory(category: ModuleCategory)`、`fun fromId(id: String): MathModule`。

2. **`domain/model/ToleranceLevel.kt`**：
   - `EXACT("精确(0%)", "精确", 0.0)`
   - `STRICT("精算 ±1%", "±1%", 0.01)`
   - `STANDARD("实战 ±2%", "±2%", 0.02)`（估算默认）
   - `RELAXED("宽松 ±5%", "±5%", 0.05)`
   - 提供 `estimationOptions = listOf(STRICT, STANDARD, RELAXED)` 与 `fromName(name: String)`。

3. **`domain/model/PracticeConfig.kt`**：
   - `enum class KeypadOrder(val label: String)`：`ASCENDING("正序")`, `DESCENDING("倒序")`, `SHUFFLED("乱序")`
   - `enum class NBackLevel(val label: String, val steps: Int)`：`OFF("关闭", 0)`, `ONE("1-back", 1)`, `TWO("2-back", 2)`
   - `enum class CustomOperation(val label: String)`：`ADD("加法")`, `SUB("减法")`, `MUL("乘法")`, `DIV("除法")`, `MIXED_ADD_SUB("混合加减")`
   - `data class CustomQuizConfig(val operation: CustomOperation = CustomOperation.DIV, val firstDigits: Int = 5, val secondDigits: Int = 3, val allowNegative: Boolean = false, val percentAnswer: Boolean = false, val tolerance: ToleranceLevel = ToleranceLevel.STANDARD)`（提供 `normalized()` 方法确保 `firstDigits in 1..5`、`secondDigits in 1..4`）。
   - `enum class FormulaCategory(val label: String)`：`GROWTH_RATE("增长率")`, `BASE_PERIOD("基期")`, `PROPORTION("比重")`, `AVERAGE("平均数")`, `CUSTOM("自定义")`。

4. **`domain/data/BaifenTable.kt`**：
   - `data class BaifenEntry(val fraction: String, val symbol: String, val percent: String)`，严格包含 `design/ui_v2/07_baifen.png` 中的全部 **16 组**：
     - `1/2 = 50%`、`1/3 ≈ 33.3%`、`1/4 = 25%`、`1/5 = 20%`、`1/6 ≈ 16.7%`、`1/7 ≈ 14.3%`、`1/8 = 12.5%`、`1/9 ≈ 11.1%`
     - `1/11 ≈ 9.1%`、`1/12 ≈ 8.3%`、`1/13 ≈ 7.7%`、`1/14 ≈ 7.1%`、`1/15 ≈ 6.7%`、`1/16 = 6.25%`、`1/17 ≈ 5.9%`、`1/18 ≈ 5.6%`
   - `data class FormulaSeed(val title: String, val category: FormulaCategory, val expression: String, val note: String, val isFavorite: Boolean)`：提供 **10 条预置行测资料分析高频公式**（前 4 条严格对齐 `05_formulas.png`：间隔增长率★、两期比重差★、基期量估算、平均数增长率，再补 6 条实战高频：混合增长率、增长量百化分、现期比重、基期比重、现期倍数、年均增长率估算）。

---

## 五、晴空蓝白 Design System 与复用组件（`ui/theme/` & `ui/components/`）

严格对照 `design/ui_v2/01_home.png` 精准落地色彩、字体与基础组件：

1. **`ui/theme/Color.kt`**：
   - `BgSkyWhite = Color(0xFFF4F8FD)`
   - `SurfaceWhite = Color(0xFFFFFFFF)`
   - `SurfaceIceBlue = Color(0xFFE8F3FF)`
   - `SurfaceSkyTint = Color(0xFFD6EAFF)`
   - `SurfaceSelectedCard = Color(0xFFEDF6FF)`
   - `BorderSky = Color(0xFFD5E6FA)`
   - `BorderSkySubtle = Color(0xFFE3EEFA)`
   - `PrimarySky = Color(0xFF3B93F6)`
   - `PrimarySkyLight = Color(0xFF52A6FF)`
   - `PrimarySkyDeep = Color(0xFF2E84EC)`
   - `PrimarySkyText = Color(0xFF2277DF)`
   - `TextPrimary = Color(0xFF162436)`
   - `TextSecondary = Color(0xFF5D7188)`
   - `TextMuted = Color(0xFF7A8FA6)`
   - `StatusExact = Color(0xFF179B68)` / `StatusExactBg = Color(0xFFE6F8F0)`
   - `StatusPass = Color(0xFF148CA8)` / `StatusPassBg = Color(0xFFE5F6FA)`
   - `StatusError = Color(0xFFE04B5A)` / `StatusErrorBg = Color(0xFFFDECEF)`
   - `PrimarySkyGradient = Brush.verticalGradient(listOf(PrimarySkyLight, PrimarySkyDeep))`

2. **`ui/theme/Type.kt` & `Theme.kt`**：
   - 配置清晰中文层次排版体系 + `TabularTextStyle` 工具扩展（启用 `fontFeatureSettings = "tnum"`）。
   - `SpeedMathTheme`：挂载 Material 3 LightColorScheme，统一晴空蓝白基底。

3. **`ui/components/SkyComponents.kt`**：
   - `SkyCard`：纯白圆角卡片（默认 `16.dp` 或 `18.dp`，带 `BorderStroke(1.dp, BorderSky)` 与柔和冷调微影）。
   - `SkyPrimaryButton`：晴空蓝微渐变大圆角主按钮（`14.dp`，纯白加粗文字，支持 disabled 态）。
   - `SkySecondaryButton`：清透冰蓝 `#E8F3FF` 次级按钮（深藏青炭黑文字）。
   - `SkyCircleIconButton`：纯白或浅蓝圆形线框图标按钮（供顶栏导出/设置/搜索/返回复用）。
   - `SkySegmentedPill`：高质感冰蓝槽 + 纯白微影激活滑块分段控制器（供键盘模式、公式本分段复用）。

---

## 六、Room 数据库、DataStore 与 Repository（`data/`）

1. **Entities (`data/db/PracticeEntities.kt` & `FormulaEntity.kt`)**：
   - `PracticeSessionEntity`（表名 `practice_sessions`）：
     `id: Long (PK autoGenerate)`, `moduleId: String`, `moduleTitle: String`, `isEstimation: Boolean`, `toleranceName: String`, `questionCount: Int`, `correctCount: Int`, `totalDurationMs: Long`, `averageErrorPercent: Double?`, `nBackSteps: Int`, `createdAt: Long`
   - `PracticeItemEntity`（表名 `practice_items`，外键 `sessionId` 关联 `practice_sessions.id`，`onDelete = CASCADE`，加索引）：
     `id: Long (PK autoGenerate)`, `sessionId: Long`, `questionIndex: Int`, `expression: String`, `userAnswer: String`, `exactAnswer: String`, `isPercent: Boolean`, `gradeStatus: String` (`EXACT`/`PASS`/`FAIL`), `signedErrorPercent: Double?`, `durationMs: Long`
   - `FormulaEntity`（表名 `formulas`）：
     `id: Long (PK autoGenerate)`, `title: String`, `category: String`, `expression: String`, `note: String`, `isFavorite: Boolean`, `isBuiltIn: Boolean`, `createdAt: Long`, `updatedAt: Long`

2. **DAOs (`data/db/PracticeDao.kt` & `FormulaDao.kt`)**：
   - `PracticeDao`：
     - `insertSession(session): Long`、`insertItems(items)`、`@Transaction suspend fun saveSessionWithItems(session, items): Long`
     - `observeAllSessions(): Flow<List<PracticeSessionEntity>>`（按 `createdAt DESC`）
     - `observeSessionsByType(isEstimation: Boolean): Flow<List<PracticeSessionEntity>>`
     - `observeTodayStats(startOfDayMillis: Long): Flow<StatsAggregate>`（聚合 `questionCount`、`correctCount`、`totalDurationMs`）
     - `observeOverallStats(): Flow<StatsAggregate>`
     - `getSessionById(sessionId: Long): PracticeSessionEntity?`、`getItemsBySessionId(sessionId: Long): List<PracticeItemEntity>`
     - `deleteSessionById(sessionId: Long)`、`clearAllSessions()`
   - `FormulaDao`：
     - `observeAllFormulas(): Flow<List<FormulaEntity>>`（`ORDER BY isFavorite DESC, updatedAt DESC`）
     - `count(): Int`
     - `upsertFormula(formula): Long`、`insertAll(formulas)`、`setFavorite(id: Long, isFavorite: Boolean, updatedAt: Long)`、`deleteById(id: Long)`

3. **Database & Repositories**：
   - `AppDatabase`（Room `version = 1`, `exportSchema = false`）。
   - `FormulaRepository`：提供 `ensureSeeded()`（幂等检查：当且仅当首次未初始化且表为空时插入 10 条 `BaifenTable.defaultFormulas`，并用 DataStore 标记 `formulas_seeded = true`，避免用户主动删光公式后重启又被重新插回！）、`observeFormulas()`、`saveFormula()`、`toggleFavorite()`、`deleteFormula()`。
   - `PracticeRepository`：封装会话保存、今日统计、总览统计、历史列表、单次详情查询与删除。
   - `UserPreferencesStore`（DataStore Preferences）：持久化并暴露 `Flow<UserPreferences>`，字段包含：`lastModule: MathModule`（默认 `FIVE_DIV_THREE`）、`questionCount: Int`（默认 `10`）、`tolerance: ToleranceLevel`（默认 `STANDARD`）、`keypadOrder: KeypadOrder`（默认 `ASCENDING`）、`stylusEnabled: Boolean`（默认 `true`）、`nBackLevel: NBackLevel`（默认 `OFF`）、`hapticEnabled: Boolean`（默认 `true`）、`customQuizConfig: CustomQuizConfig`、`formulasSeeded: Boolean`。

4. **Application 与最小可启动 `MainActivity.kt`**：
   - `SpeedMathApp : Application()` 持有 `val container: AppContainer`，并在启动协程中调用 `formulaRepository.ensureSeeded()`。
   - `MainActivity.kt` 启用 `enableEdgeToEdge()`，使用 `SpeedMathTheme` 展示简洁的晴空蓝白启动验证页（实时读取 DataStore 偏好与 Room 预置公式条数），确认整个依赖注入、Room KSP 与 Compose 主题可真机启动无崩溃（Pack 3 将替换为正式 3-Tab `AppNavGraph`）。

---

## 七、单元测试与强制验收标准

1. **JVM 单元测试（`app/src/test/java/com/xingce/speedmath/`）**：
   - `DomainCatalogTest.kt`：校验 `MathModule.presets` 严格为 17 项（5 除法估算 + 7 加减基础 + 5 乘法速算），`id` 与 `title` 无重复；`BaifenTable.entries` 严格为 16 项且数值精准；`BaifenTable.defaultFormulas` 为 10 条且标题/公式非空。
   - `ConfigModelTest.kt`：校验 `CustomQuizConfig.normalized()` 越界裁剪、`ToleranceLevel` / `MathModule.fromId` 兜底容错。
2. **强制命令验证（全部 0 错误）**：
   - `./gradlew testDebugUnitTest` — 全部通过
   - `./gradlew assembleDebug` — 成功生成 APK，无编译错误与 KSP 报错
3. **审查交付汇报**：完成后汇报清理文件清单、新增文件清单、图标生成尺寸、单测与构建命令输出，等待架构师审查通过后再进入 Pack 2。
