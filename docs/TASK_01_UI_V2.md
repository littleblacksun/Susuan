# 任务书 01（V2 全新重构版）：《行测速算》晴空蓝白全新 UI 架构设计与 `imagegen` 生图任务书（交 Codex 执行）

> **阶段定位**：Phase 1-V2（**彻底摒弃旧微信参考图与旧版粉色稿**，从零全新设计 **Apple HIG × Google Material 3 晴空蓝白** 现代信息架构与组件排布）  
> **输出目录**：`/home/bubu/资料分析apk/design/ui_v2/`（共 **9 张** 9:16 高保真纯平设计图 + `MANIFEST.md`）  
> **核心铁律**：
> 1. **严禁参考** `design/reference/ref_home.jpg`、旧版 `design/ui/` 或 `design/ui_v1_backup/` 中的任何旧图！生成母版 `01_home.png` 必须为 **100% 纯文本全新生成（Text-Only）**。
> 2. 生成 `02` ~ `09` 时，**仅将刚刚通过自检的新版晴空蓝白 `design/ui_v2/01_home.png` 作为唯一视觉风格锚点**，确保全套 9 张图色温、圆角、字重、图标线宽完全一致。
> 3. **本阶段仅调用 `imagegen` 技能生图并撰写 `design/ui_v2/MANIFEST.md`，严禁修改或新增任何代码文件**。

---

## 1. 全新信息架构与组件排布思路（为什么远胜旧版参考图）

旧版参考图把 18 个大小一致的方块、键盘、触控笔、题量、开始、导出、历史、N-back、`+` 全部硬塞在一页里，拥挤且缺乏层级。V2 全新架构按 **Apple iOS / Google Material 3 现代学习工具** 重新梳理为 **3 大主 Tab**：

1. **Tab 1 · `速算`（首页工作台）**：
   - **顶部极简栏**：左侧标题 `行测速算`，右侧 2 枚纯白圆钮（`导出练习卷`、`偏好设置`）。
   - **今日速算 Hero 看板（晴空蓝渐变卡）**：展示今日已练题数、正确率、平均单题速度，右侧提供 `继续上次 ›` 一键直达。
   - **题型分类横向胶囊**：`除法估算`（默认选中，直击行测资料分析核心）| `加减基础` | `乘法速算` | `全部(17)`，彻底消灭 18 宫格文字墙！
   - **2 列宽敞纯白 Bento 题型卡片**：每张卡片带精致线性图标 + 题型名称 + **等宽算式示例**（如 `54321 ÷ 213`、`534 ÷ 4218 → %`）+ `估` 轻角标，一眼识别练什么；第 6 张为轻巧的 `+ 自定义出题` 入口卡。
   - **底部紧凑「参数胶囊 + 一键开始」操作区**：将 `题量 10题`、`误差 ±2%`、`正序键盘`、`草稿板 开`、`N-back 关` 收纳为清爽可点胶囊芯片，紧贴通栏晴空蓝 `开始练习` 主按钮，大拇指单手秒操作。
2. **Tab 2 · `公式`（独立一级「公式本」模块）**：
   - 顶部分段自由切换 **`我的公式`** 与 **`百化分速查`**。
   - 支持保存、分类、搜索、收藏、新增/编辑个人行测资料分析计算公式（带快捷数学符号输入栏与速算口诀备注）。
3. **Tab 3 · `统计`（成长统计与练习历史）**：
   - 顶部三列成长指标卡 + 精确/估算筛选胶囊 + 按日期分组练习记录列表。

---

## 2. 「晴空蓝白 · Clear Sky & Pure White」统一视觉规范

| Token 名称 | Hex 色值 | 用途与质感 |
| :--- | :--- | :--- |
| `BgSkyWhite` | `#F4F8FD` | 极浅冰蓝白画布，清澈通透、绝不发灰 |
| `SurfaceWhite` | `#FFFFFF` | 纯雪白卡片，配清晰浅天蓝描边 `#D5E6FA`（1.5px）+ 柔和冷蓝微影，轮廓清脆立体 |
| `SurfaceIceBlue` | `#E8F3FF` | 浅天蓝雾表面（次级按钮、分段槽、参数芯片、公式高亮框、键盘底盘、进度条底槽） |
| `SurfaceSkyTint` | `#D6EAFF` | 浅天蓝轻强调（角标 `估`、`±2%`、分类标签、图标圆底） |
| `PrimarySky` | `#3B93F6`（柔和微渐变 `#52A6FF → #2E84EC`） | 主晴空蓝（Hero 看板、主按钮、选中分类/卡片勾标、进度条、环形进度弧、Tab 激活色） |
| `PrimarySkyDeep` | `#2277DF` | 浅蓝底上的清晰强调蓝字/数字 |
| `TextPrimary` | `#162436` | 深藏青炭黑主文字，锐利高对比、久看不累 |
| `TextSecondary` | `#5D7188` | 清朗石板蓝灰辅助文字 |
| `StatusExact` | `#179B68`（底 `#E6F8F0`） | 语义色：精准（翡翠绿） |
| `StatusPass` | `#148CA8`（底 `#E5F6FA`） | 语义色：估算通过（晴空青） |
| `StatusError` | `#E04B5A`（底 `#FDECEF`） | 语义色：偏差过大（珊瑚红，仅用于错题徽章） |

- **画幅与规范**：全部为 **9:16 竖屏纯平 2D 手机 UI 设计稿**（推荐 `1024×1792` 或 `936×1664` 统一尺寸，铺满画布四边，无手机边框、无 3D 透视、无微信胶囊、无粉色/绿色背景残留、无水印）。
- **字体**：现代简洁无衬线中文 UI 字体（苹方 / 思源黑体风格）+ 等宽清晰数字，**严禁任何错别字、乱码或糊字**。

---

## 3. 交付清单（输出至 `/home/bubu/资料分析apk/design/ui_v2/`）

| 文件名 | 页面说明 |
| :--- | :--- |
| `01_home.png` | 首页：速算工作台（晴空蓝今日看板 + 分类胶囊 + 2列 Bento 题型卡 + 参数条 + 开始练习 + 3-Tab 底栏） |
| `02_practice_normal.png` | 答题练习页：标准估算作答态（`54321 ÷ 213` + 估算通过反馈 + 正序悬浮白键帽键盘） |
| `03_practice_stylus.png` | 答题练习页：触控笔手写草稿板（`534 ÷ 4218 ≈ 12.7%` + 蓝点阵手账画布直除笔迹 + 倒序键盘） |
| `04_result.png` | 练习报告页：90% 晴空蓝环形仪表盘 + 三项统计 + 4 条分级复盘卡片 + 底部双按钮 |
| `05_formulas.png` | 公式本主页：我的公式库（`我的公式 | 百化分速查` 分段 + 分类胶囊 + 4 张高颜值公式卡 + 3-Tab `公式`高亮） |
| `06_formula_editor.png` | 新增/编辑公式弹窗：标题、分类胶囊、公式输入区、快捷数学符号栏、口诀备注、保存公式 |
| `07_baifen.png` | 百化分速查页：公式本 `百化分速查` Tab 视图（16 组精准百化分表 + 误差档位口诀提示卡 + 3-Tab `公式`高亮） |
| `08_history.png` | 统计与历史主页：三列成长指标卡 + 筛选胶囊 + 按日分组记录 + 3-Tab `统计`高亮 |
| `09_export_sheet.png` | 导出练习卷弹窗：题型、题量、PDF/文本/长图三卡、参考答案开关、3 题纸张预览 |

---

## 4. 可直接用于 `imagegen` 的全新完整 Prompt 集

### 4.1 母版 `01_home.png`（100% 纯文本全新生成，禁止传入任何历史旧图！）

```text
Use case: ui-mockup. Generate a brand-new, Apple Design Award caliber native Android mobile UI design screenshot for the Chinese Civil Service Exam mental arithmetic app “行测速算”. This is SCREEN 01 (Home Workbench), the visual master. TEXT-ONLY generation; do NOT copy any old grid layout or use any input image.

FORMAT & AESTHETIC:
- Single 9:16 vertical full-bleed 2D flat UI screenshot (1080×1920 proportions), directly facing the screen pixels, filling all four edges cleanly. No phone frame, no 3D mockup, no perspective, no hand, no WeChat menu capsule, no watermark, no pink or green anywhere.
- Style: Refined Apple HIG breathing room and crisp visual hierarchy combined with Google Material 3 Expressive rounded geometry. Ultra-clear SKY BLUE & PURE WHITE palette:
  - Canvas background: crystal-clear pale sky-ice white #F4F8FD.
  - Cards: pure snow white #FFFFFF with crisp 1.5px pale sky-blue border #D5E6FA and a clean delicate cool shadow so every card stands out sharply.
  - Secondary surfaces & chips: airy ice blue #E8F3FF and pale sky tint #D6EAFF.
  - Primary accent: luminous fresh sky blue #3B93F6 (smooth subtle gradient #52A6FF to #2E84EC), deep sky text #2277DF on pale blue surfaces, pure white semibold text on sky-blue buttons.
  - Typography: razor-sharp deep navy-charcoal #162436 primary text, clean slate blue-gray #5D7188 secondary text, PingFang / Noto Sans SC modern sans Simplified Chinese with crisp aligned tabular numerals. Uniform minimal rounded monoline icons.

TOP-TO-BOTTOM LAYOUT (with generous margins and balanced vertical rhythm):
1. Minimal native status bar “9:41”, signal, Wi-Fi, battery.
2. Clean header row: left bold title “行测速算” in #162436; right two neat pure-white circular icon buttons with crisp #D5E6FA outlines (a document-export icon and a gear settings icon).
3. Top HERO SUMMARY CARD (22dp rounded rectangle, luminous smooth sky-blue gradient #52A6FF to #2E84EC with soft sky shadow):
   - Left side in crisp white: small label “今日速算”, large bold “30 题”, beneath it clean row “正确率 93% · 均速 6.2s”.
   - Right side: translucent frosted-white pill button reading “继续上次 ›” with subtle small caption underneath “五位数除三位数”.
4. Category filter pill row (horizontal single line):
   - Selected solid sky-blue pill #3B93F6 with white text “除法估算”;
   - Unselected pure-white pills with #D5E6FA hairline border and #162436 text: “加减基础”, “乘法速算”, “全部(17)”.
5. Spacious 2-COLUMN × 3-ROW BENTO MODULE GRID (6 clean 16dp rounded cards with generous gutters, airy and readable):
   - Card 1 (SELECTED state: ultra-pale ice-blue fill #EDF6FF, crisp 2px sky-blue border #3B93F6, tiny solid sky-blue circle checkmark “✓” top-right): bold title “五位数除三位数”, subtle slate tabular subtitle “54321 ÷ 213”, tiny pale sky tag “估”.
   - Card 2 (pure white #FFFFFF, #D5E6FA border): title “三位数除四位数”, subtitle “534 ÷ 4218 → %”, tiny pale sky tag “估”.
   - Card 3 (pure white): title “三位数除两位数”, subtitle “736 ÷ 42”, tiny pale sky tag “估”.
   - Card 4 (pure white): title “三位数除一位数”, subtitle “845 ÷ 7”, tiny pale sky tag “估”.
   - Card 5 (pure white): title “乘法估算”, subtitle “428 × 19.6%”, tiny pale sky tag “估”.
   - Card 6 (light ice-blue dashed-border card #F0F7FF): crisp sky-blue “+” icon + title “自定义出题”, subtitle “自由设位数与误差”.
6. Compact QUICK PRACTICE CONSOLE white card (18dp rounded, pure white #FFFFFF with #D5E6FA outline):
   - Top row: four neat compact pale ice-blue #E8F3FF pill chips with sharp #162436/#2277DF text: “10题 ▾”, “±2% ▾”, “正序键盘”, “草稿板 开”.
   - Below it inside the card (or tightly paired right below): full-width luminous sky-blue #3B93F6 rounded primary CTA button (14dp) with crisp white bold text “开始练习”.
7. Clean sleek 3-TAB BOTTOM NAVIGATION BAR anchored to bottom (pure white #FFFFFF with crisp #E3EEFA top hairline):
   - Tab 1 (ACTIVE in sky blue #2E84EC): calculator/lightning icon + “速算”
   - Tab 2 (inactive slate #7A8FA6): formula/book icon + “公式”
   - Tab 3 (inactive slate #7A8FA6): bar-chart icon + “统计”

Every quoted Chinese character and numeral must be rendered literally, razor-sharp, correctly drawn, with zero typos, zero extra filler text, and effortless Apple/Google visual polish.
```

---

### 4.2 `02` ~ `09` 共用风格前缀（Input Image 1 = 已定稿的新版 `design/ui_v2/01_home.png`）

```text
Use case: ui-mockup. Design a shippable native Android mobile UI screen for the same Chinese mental arithmetic app “行测速算”. Input Image 1 is the approved SKY BLUE & PURE WHITE Home visual master; match its EXACT canvas color #F4F8FD, pure white card brightness #FFFFFF, crisp #D5E6FA borders, airy ice-blue #E8F3FF secondary fills, luminous sky-blue #3B93F6 (#52A6FF to #2E84EC) primary accent, deep navy-charcoal #162436 typography, slate #5D7188 secondary text, uniform monoline icon weight, status bar “9:41”, and Apple HIG × Material 3 spacing rhythm. Single 9:16 flat full-bleed 2D UI screenshot matching Input Image 1’s aspect ratio. No phone frame, no perspective, no WeChat capsule, no pink anywhere (except a single semantic error badge when requested), no watermark. Render every quoted Simplified Chinese label and numeral verbatim, ultra-sharp, and completely typo-free.
```

---

### 4.3 各页面专项 Prompt（拼接在 4.2 共用前缀之后）

#### 图 2：`02_practice_normal.png`（标准估算答题页 · 正序键盘）
```text
SCREEN 02 — STANDARD PRACTICE SCREEN (replace all Home content below status bar; no bottom navigation bar):
1. Top header: left pure-white circle back button “<”, center title “五位数除三位数” + pale sky pill “±2%” (#D6EAFF / #2277DF), right tabular timer “00:18” and “3/10”. Immediately beneath: slim pill progress bar 30% filled in sky blue #3B93F6 over ice-blue track #E8F3FF.
2. Generous central pure-white #FFFFFF card (22dp rounded, crisp #D5E6FA border, clean shadow):
   - Large centered crisp tabular expression “54321 ÷ 213 = ?” in #162436.
   - Centered spacious white input box with crisp 2px sky-blue border #3B93F6 showing “255” and a thin sky-blue vertical cursor on the right.
   - Clean sea-teal status pill (#E5F6FA fill, #148CA8 text) reading verbatim: “✓ 估算通过 · 精确 255.0 · 误差 0.0%”.
3. Compact toolbar above keypad: left pale ice-blue pill “草稿板 · 关” with fine pen icon; right ice-blue segmented pill “正序 | 倒序 | 乱序” with raised white thumb on “正序”.
4. Bottom 4×4 custom in-app numeric keypad on airy ice-blue tray #E8F3FF (NO system OS keyboard):
   - Columns 1–3: pure white #FFFFFF rounded keycaps with subtle cool shadow and sharp #162436 tabular numerals in ASCENDING rows: “1” “2” “3” / “4” “5” “6” / “7” “8” “9” / “.” “0” “-”.
   - Column 4: pale sky-blue #D6EAFF utility keys “⌫” (row 1) and “C” (row 2), plus one tall luminous sky-blue #3B93F6 button spanning rows 3–4 with white bold text “下一题”.
```

#### 图 3：`03_practice_stylus.png`（触控笔草稿板 + 百分比除法 + 倒序键盘）
```text
SCREEN 03 — STYLUS SCRATCHPAD PRACTICE SCREEN (replace all Home content below status bar; no bottom navigation bar):
1. Top header: back circle “<”, “三位数除四位数”, pale sky pill “±2%”, right “00:34” and “6/10”; slim 60% sky-blue progress bar.
2. Compact horizontal pure-white #FFFFFF formula card (18dp rounded): left “534 ÷ 4218 ≈”, center crisp sky-blue bordered white input showing “12.7”, right large clear “%” outside the box.
3. Large rounded pure-white writing canvas (~36% of screen height, crisp #D5E6FA border) with subtle fine sky-blue dot grid (#D5E6FA dots). Natural dark navy-charcoal handwriting at upper left reading exactly “534 ÷ 42 ≈ 12.7%” with clean hand-written long division working directly below (“42 | 534”, “42”, “114”, “84”, “30”). At top-right inside canvas: translucent white pill overlay with exactly 4 fine monoline icons (undo, trash, pen stroke, collapse). Generous blank canvas space remaining.
4. Bottom 4×4 custom keypad on airy ice-blue tray #E8F3FF with pure-white #FFFFFF keycaps in DESCENDING calculator order: “7” “8” “9” / “4” “5” “6” / “1” “2” “3” / “.” “0” “-”; column 4 has “⌫”, “C”, and tall sky-blue #3B93F6 “下一题” spanning rows 3–4.
```

#### 图 4：`04_result.png`（练习报告与逐题复盘页）
```text
SCREEN 04 — PRACTICE REPORT SCREEN (replace all Home content below status bar; no bottom navigation bar):
1. Top bar: left pure-white circle home icon, centered title “练习报告”, right muted slate “五位数除三位数”.
2. Large pure-white #FFFFFF overview card (22dp rounded, #D5E6FA border):
   - Left: luminous sky-blue #3B93F6 90% circular progress ring over clean ice-blue #E8F3FF track, centered “90%” above “正确率”.
   - Right: three vertically stacked tabular metrics separated by fine #E6F0FA dividers: “总用时 01:12”, “均速 7.2秒/题”, “平均误差 ±0.8%”.
3. Four separate crisp pure-white #FFFFFF review cards in a vertical list:
   - Card 1: pale sky pill “题 1” · “54321 ÷ 213” · “你的 255 · 精确 255.0” · right “5.8s” & mint-green pill “精准” (#E6F8F0 / #179B68).
   - Card 2: pale sky pill “题 2” · “38412 ÷ 192” · “你的 202 · 精确 200.1” · right “6.4s” & cyan-blue pill “通过 +0.9%” (#E5F6FA / #148CA8).
   - Card 3: pale sky pill “题 3” · “71250 ÷ 345” · “你的 218 · 精确 206.5” · right “9.1s” & coral-red pill “偏差 +5.6%” (#FDECEF / #E04B5A).
   - Card 4: pale sky pill “题 4” · “46890 ÷ 231” · “你的 203 · 精确 203.0” · right “6.0s” & mint-green pill “精准” (#E6F8F0 / #179B68).
4. Anchored bottom twin equal buttons: left ice-blue #E8F3FF “错题重练” (#162436 text), right sky-blue #3B93F6 “再来一组” (white text).
```

#### 图 5：`05_formulas.png`（公式本主页 · 3-Tab `公式`高亮）
```text
SCREEN 05 — MY FORMULA BOOK SCREEN (primary Tab 2 screen, matching Input Image 1’s exact bottom navigation bar):
1. Header row: left bold title “公式本”; right pure-white circular search icon button and a luminous sky-blue #3B93F6 circular “+” button (for adding new formulas).
2. Full-width ice-blue #E8F3FF segmented pill control: left raised pure-white active segment “我的公式”, right inactive “百化分速查”.
3. Category filter pill row: sky-blue #3B93F6 selected white “全部”, pure-white #D5E6FA-bordered pills “收藏”, “增长率”, “比重”, “自定义”.
4. Four clean, spacious pure-white #FFFFFF formula cards (18dp rounded, crisp #D5E6FA border):
   - Card 1: top row bold “间隔增长率” + pale sky tag “增长率” + filled sky-blue star icon on right; middle pale ice-blue #EFF7FF rounded formula strip reading crisp centered “R = r₁ + r₂ + r₁ × r₂”; bottom quiet slate note “隔年增长率高频，乘积项通常可直接估算”.
   - Card 2: top row “两期比重差” + pale sky tag “比重” + filled sky-blue star icon; middle ice-blue formula strip “(A / B) × (a − b) / (1 + a)”; bottom note “a > b 上升，结果绝对值小于 |a − b|”.
   - Card 3: top row “基期量估算” + pale sky tag “基期” + outline star icon; middle ice-blue formula strip “基期 = 现期 / (1 + r)”; bottom note “|r| ≤ 5% 时可化除为乘：现期 × (1 − r)”.
   - Card 4: top row “平均数增长率” + pale sky tag “自定义” + outline star icon; middle ice-blue formula strip “r = (a − b) / (1 + b)”; bottom note “a 为总量增速，b 为份数增速”.
5. Bottom 3-Tab Navigation Bar identical to Input Image 1, except Tab 2 “公式” is ACTIVE in sky blue #2E84EC and Tabs “速算” & “统计” are inactive slate #7A8FA6.
```

#### 图 6：`06_formula_editor.png`（新增/编辑个人公式弹窗）
```text
SCREEN 06 — ADD / EDIT FORMULA BOTTOM SHEET:
- Upper ~22% shows a softly blurred, cool dimmed Formula Book screen in the exact Sky Blue & Pure White palette (no pink).
- Lower ~78% is a full-width pure-white #FFFFFF bottom sheet with 24dp rounded top corners and centered muted slate drag handle.
- Sheet top header: left bold “新建公式”, right pale sky-blue circle close button “×”.
- Form sections cleanly spaced:
  1. Label “公式名称”: crisp rounded white input (#D5E6FA border) filled with “混合增长率（十字交叉）”.
  2. Label “所属分类”: single row of pills “增长率” (selected sky blue #3B93F6 white text), “基期”, “比重”, “平均数”, “自定义”.
  3. Label “公式表达式”: spacious ice-blue tinted #F2F8FF rounded formula input box with crisp sky-blue #3B93F6 border showing “r₁ < r总 < r₂ ，距离与基期成反比”.
  4. Directly under expression box: neat horizontal quick-symbol chip row in pale ice blue #E8F3FF: “+” “−” “×” “÷” “≈” “%” “( )” “r₁”.
  5. Label “记忆口诀 / 备注”: clean white rounded text box (#D5E6FA border) with placeholder/text “偏向基期量大的一侧，常用于快速排除选项”.
  6. Bottom full-width luminous sky-blue #3B93F6 primary button with crisp white bold text “保存公式”.
```

#### 图 7：`07_baifen.png`（公式本 · 百化分速查视图）
```text
SCREEN 07 — FRACTION-TO-PERCENTAGE CHEAT SHEET SCREEN (second segment of Formula Book, Tab 2 “公式” active):
1. Header row identical to Formula Book: left bold “公式本”, right white circular search button and sky-blue “+” button.
2. Full-width ice-blue #E8F3FF segmented pill control: left inactive “我的公式”, right raised pure-white active segment “百化分速查”.
3. Crisp, spacious pure-white #FFFFFF rounded table card (18dp rounded, #D5E6FA border) of exactly 2 columns × 8 rows, alternating ultra-light sky white #F5F9FE and pure white #FFFFFF rows, fine #DCEAFB gridlines, sharp #162436 tabular numerals, NO header row, verbatim values:
   “1/2 = 50%” | “1/11 ≈ 9.1%”
   “1/3 ≈ 33.3%” | “1/12 ≈ 8.3%”
   “1/4 = 25%” | “1/13 ≈ 7.7%”
   “1/5 = 20%” | “1/14 ≈ 7.1%”
   “1/6 ≈ 16.7%” | “1/15 ≈ 6.7%”
   “1/7 ≈ 14.3%” | “1/16 = 6.25%”
   “1/8 = 12.5%” | “1/17 ≈ 5.9%”
   “1/9 ≈ 11.1%” | “1/18 ≈ 5.6%”
4. Below table: clean pale ice-blue #E8F3FF rounded tip card reading verbatim: “速算提示：实战默认 ±2% 误差，截三位直除建议切至 ±1% 精算档”.
5. Bottom 3-Tab Navigation Bar identical to Input Image 1 with Tab 2 “公式” ACTIVE in sky blue #2E84EC.
```

#### 图 8：`08_history.png`（统计与历史记录主页 · 3-Tab `统计`高亮）
```text
SCREEN 08 — STATISTICS & HISTORY SCREEN (primary Tab 3 screen):
1. Header row: left bold title “统计与记录”.
2. Pure-white #FFFFFF 3-column summary card (20dp rounded, crisp #D5E6FA border):
   - Col 1: bold “1,280” above “累计题数”
   - Col 2: luminous sky-blue #2277DF bold “92%” above “综合正确率”
   - Col 3: bold “6.4秒” above “平均单题”
3. Three filter pills in a single line: sky-blue #3B93F6 selected white “全部”, pure-white #D5E6FA-bordered “估算题型”, “精确题型”.
4. Date groups “今天” and “昨天” with 4 spacious pure-white #FFFFFF cards (16dp rounded, #D5E6FA border), each with a pale sky-blue #E8F3FF circle containing a clean #2277DF math icon (÷ or ×), bold title, slate metadata, and right-aligned bold percentage above duration:
   - 今天: “五位数除三位数” / “10题 · ±2% · 09:20” / “90%” / “01:12”; “三位数除四位数” / “10题 · ±2% · 08:46” / “100%” / “01:04”.
   - 昨天: “两位数乘两位数” / “10题 · 18:42” / “90%” / “01:08”; “五位数除三位数” / “10题 · ±2% · 12:15” / “100%” / “01:10”.
5. Bottom 3-Tab Navigation Bar identical to Input Image 1 with Tab 3 “统计” ACTIVE in sky blue #2E84EC.
```

#### 图 9：`09_export_sheet.png`（导出练习卷弹窗）
```text
SCREEN 09 — EXPORT WORKSHEET BOTTOM SHEET:
- Upper ~26% shows a softly blurred cool dimmed version of Input Image 1 (Sky Blue & Pure White Home Workbench).
- Lower ~74% is a full-width pure-white #FFFFFF 24dp-top-rounded bottom sheet with centered slate drag handle.
- Header: left bold “导出练习卷”, right pale sky-blue circle “×”.
- Content top-to-bottom:
  1. Ice-blue #E8F3FF rounded row: left “题型”, right “五位数除三位数 ›”.
  2. Label “导出题量” + 4 pills: “10题”, sky-blue #3B93F6 selected white “20题”, “30题”, “50题”.
  3. Label “导出格式” + 3 equal cards with clean monoline icons: “PDF 打印卷” (selected ice-blue #E8F3FF fill + crisp sky-blue #3B93F6 border), “纯文本复制” (white, #D5E6FA border), “高清长图” (white, #D5E6FA border).
  4. Ice-blue #E8F3FF switch row: “附带参考答案与误差区间” + sky-blue #3B93F6 ON switch.
  5. Crisp white paper preview card (#D5E6FA border) with exact 3 tabular lines:
     “1. 54321 ÷ 213 = ______”
     “2. 38412 ÷ 192 = ______”
     “3. 62910 ÷ 318 = ______”
  6. Bottom full-width luminous sky-blue #3B93F6 rounded button “生成导出” in white bold text.
```

---

## 5. 自检与审核通过标准

1. **零旧图污染**：母版 `01_home.png` 必须是全新的晴空蓝白 Bento 现代架构（今日看板 + 分类胶囊 + 2 列带算式卡片 + 参数芯片 + 3-Tab 导航），彻底告别旧 18 宫格与粉色/绿色。
2. **全套 9 张视觉统一**：`02`~`09` 的背景色、卡片边框、晴空蓝主色、底栏 Tab 样式（`01`/`05`/`07`/`08`）高度一致。
3. **文案与数学 100% 精准**：重点检查 `05_formulas.png` 四个行测公式表达式、`03_practice_stylus.png` 手写长除竖式、`07_baifen.png` 全部 16 组百化分数值，任何错字或乱码必须重生成。
4. **交付清单**：生成 `/home/bubu/资料分析apk/design/ui_v2/MANIFEST.md` 记录全部 9 张图尺寸与自检说明。严禁改动任何代码文件。
