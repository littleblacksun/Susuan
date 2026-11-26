# 任务书 01B：8 张 UI 设计稿「晴空蓝白」重调色任务与 `imagegen` 完整 Prompt（交 Codex 执行）

> **阶段定位**：Phase 1B（基于现有 8 张已定稿布局图，彻底切换为 **Apple HIG × Google Material 3 晴空蓝白色系**，大幅提升视觉清晰度与清透感）  
> **源图片备份目录**：`/home/bubu/资料分析apk/design/ui_v1_backup/`（若尚未备份，先将当前 `design/ui/01_home.png` ~ `08_baifen_sheet.png` 复制至此目录）  
> **最终输出目录**：覆盖输出至 `/home/bubu/资料分析apk/design/ui/`（保持 8 个文件名不变）  
> **核心铁律**：
> 1. **必须调用 `imagegen` 技能**，严格使用本文第 4 节提供的完整 Prompt 进行图生图重调色。
> 2. **彻底清除所有粉色/玫瑰红/玫红/紫粉残留**：包括背景、按钮、图标、描边、投影以及 BottomSheet 顶部半透明模糊背景！
> 3. **100% 锁死原版布局与精准文案**：所有 18 个题型（含第 18 格 **`公式`**）、算式、手写竖式、16 组百化分及数字一字不改。
> 4. **本阶段只出图与更新 `design/ui/MANIFEST.md`，严禁修改或新增任何代码文件**。

---

## 1. 「晴空蓝白 · Clear Sky & Pure White」设计规范

融合 **Apple iOS 清爽系统蓝白美学** 与 **Google Material 3 Sky Blue 色彩层级**，主打**极致清澈、高对比易读、清爽醒脑、久刷不累眼**：

| Token 名称 | 精确 Hex 色值 | 视觉质感与用途说明 |
| :--- | :--- | :--- |
| `BgSkyWhite` | **`#F4F8FD`** | **晴空极浅冰蓝白画布**：极清爽通透的浅蓝白底色，让上方的纯雪白卡片**边界分明、一眼跳出** |
| `SurfacePureWhite` | **`#FFFFFF`** | **纯净雪白卡片**：搭配清晰利落的淡天蓝描边 **`#D5E6FA`**（1.5px 清晰度）+ 极轻清澈冷调微影（`rgba(45, 125, 230, 0.08)`） |
| `SurfaceIceBlue` | **`#E8F3FF`** | **清透浅天蓝雾**：用于次级按钮（`导出题目`/`历史记录`/`错题重练`）、分段控制槽、键盘底盘、进度条底槽、弹窗开关组背景 |
| `SurfaceSkyTint` | **`#D6EAFF`** | **晴空浅蓝胶囊底**：用于 `估` 小角标、`±2%` 标签、历史页圆形算术图标底色、顶部齿轮按钮底圈 |
| `PrimarySkyBlue` | **`#3B93F6`**（清透晴空微渐变 `#52A6FF` → `#2F84EC`） | **主晴空蓝**：选中题型卡片（`两位数加减`）、主 CTA 按钮（`开始练习`/`下一题`/`再来一组`/`保存并选用`/`生成导出`）、Switch 开启态、环形正确率进度弧、右下 `+` 悬浮按钮 |
| `PrimarySkyText` | **`#2277DF`** | 浅蓝底上的清晰蓝色文字与高亮数字（如 `±2%`、`92%`、`估` 角标文字） |
| `BorderSky` | **`#CDE1F8`** | **清晰浅天蓝细边框**：输入框、表格分割线、卡片边框，清清楚楚不发虚 |
| `TextPrimary` | **`#162436`** | **高清深藏青炭黑**：极高清晰度与锐利感，数字与中文一目了然 |
| `TextSecondary` | **`#5D7188`** | **清朗石板蓝灰**：副标题、时间、题量清晰易读 |
| `TextOnBlue` | **`#FFFFFF`** | 晴空蓝主按钮上的纯白清晰粗体文字 |
| `StatusExact` | `#179B68`（底 `#E6F8F0`） | 语义色：`精准` 徽章（清透翡翠绿） |
| `StatusPass` | `#148CA8`（底 `#E5F6FA`） | 语义色：`估算通过` 提示与徽章（清透湖青蓝） |
| `StatusError` | `#E04B5A`（底 `#FDECEF`） | 语义色：`偏差过大` 徽章（仅限错题徽章小面积使用，其余页面绝不出现红粉色） |

---

## 2. 执行步骤（双锚定防串色流程）

1. **步骤 1 — 备份原稿**：检查 `/home/bubu/资料分析apk/design/ui_v1_backup/` 是否已有 8 张原图；若没有，先把当前 `design/ui/01_home.png` ~ `08_baifen_sheet.png` 完整复制进去作为结构与文案基准。
2. **步骤 2 — 先重调母版 `01_home.png`**：
   - 输入图像：`design/ui_v1_backup/01_home.png`（仅保留其布局与全部中文文案，彻底替换色彩系统）。
   - 使用第 4.1 节的 **母版 Prompt** 生成新版 `design/ui/01_home.png`。
   - **严格自检**：确认零粉色残留、晴空蓝清澈不发暗、纯白卡片与 `#F4F8FD` 背景边界清脆分明、18 个题型（含第 18 格 `公式`）与全部文字 100% 精准。不达标必须重生成！
3. **步骤 3 — 双图锚定重调 `02` ~ `08`**：
   - 每张图同时提供 **Input Image 1 = `design/ui_v1_backup/<对应文件名>`**（锁定页面布局、算式、手写笔迹、表格与文案）+ **Input Image 2 = 新版晴空蓝白 `design/ui/01_home.png`**（锁定全套晴空蓝白色板、清晰卡片边框与弹窗顶部模糊背景色调！）。
   - 使用「第 4.2 节共用前缀 + 对应页面 Prompt」依次生成并覆盖至 `design/ui/`。
4. **步骤 4 — 逐图复核与更新清单**：逐张检查无粉色残留、无中文错漏、清晰度极佳，更新 `design/ui/MANIFEST.md` 记录 v2 晴空蓝白版本交付信息。

---

## 3. 审核通过标准

- [ ] `design/ui_v1_backup/` 完整保留原 8 张参考结构图。
- [ ] `design/ui/` 下 8 张图全部为清澈高级的 **晴空蓝白色系**，**零粉色/玫瑰红/暗紫残留**（包括 3 张 BottomSheet 顶部模糊背景！）。
- [ ] 纯白卡片在 `#F4F8FD` 浅冰蓝白画布上轮廓清晰分明，主文字 `#162436` 与数字极其锐利清透。
- [ ] 8 张图布局、18 宫格（含 `公式`）、算式、手写直除竖式、16 组百化分表 100% 准确无错字。
- [ ] 严格未改动或新增任何代码文件。

---

## 4. 可直接用于 `imagegen` 的完整 Prompt 集

### 4.1 母版 `01_home.png` Prompt（Input Image 1 = `design/ui_v1_backup/01_home.png`）

```text
Use case: ui-mockup recolor and clarity upgrade. Transform Input Image 1 into an ultra-crisp, refreshing SKY BLUE & PURE WHITE native Android mobile UI design screenshot for the Chinese mental arithmetic app “行测速算” (Apple HIG × Google Material 3 aesthetic).

CRITICAL COLOR REPLACEMENT RULES:
- COMPLETELY REMOVE every trace of pink, rose, blush, magenta, warm plum, or reddish tint from Input Image 1. Replace the entire palette with a crystal-clear, airy, high-clarity Sky Blue & Pure White theme.
- Page canvas background: ultra-clean crisp pale sky-ice white #F4F8FD (cool, bright, airy, never gray or muddy).
- Unselected cards & grouped settings card: pure snow white #FFFFFF with a distinct, crisp light sky-blue hairline border #D5E6FA and a clean subtle cool blue shadow so every card pops crisply off the #F4F8FD canvas.
- Selected card (“两位数加减”), primary button (“开始练习”), active “触控笔” toggle track, and bottom-right “+” FAB: vibrant, fresh, clear sky blue #3B93F6 (subtle smooth vertical gradient #52A6FF to #2F84EC) with crisp pure white semibold text #FFFFFF and a soft airy sky-blue glow.
- Secondary surfaces (keyboard segmented track, “导出题目” and “历史记录” buttons): clean translucent ice blue #E8F3FF (zero grayness).
- Small badges & icon circles (“估” tags, “实战 ±2%” pill, top-right gear button circle, “?” icon circles): crisp pale sky blue #D6EAFF with sharp sky-blue text/icons #2277DF.
- Primary typography & icons: razor-sharp deep navy-charcoal #162436 for maximum clarity; secondary labels in clean slate blue-gray #5D7188.

STRICT LAYOUT & TEXT PRESERVATION:
- Preserve 100% of Input Image 1’s exact 9:16 flat full-bleed screenshot composition, card grid geometry, alignment, spacing, and EVERY SINGLE Simplified Chinese character and numeral verbatim:
  - Status bar 9:41, title “行测速算”, right gear icon.
  - Row: “键盘” + segmented “正序 | 倒序 | 乱序” (white raised active thumb on 正序) + “触控笔” + sky-blue ON switch + “?”.
  - 3×6 grid verbatim:
    Row 1: “两位数加减” (selected sky blue) | “凑整百练习” | “三位数加法”
    Row 2: “三位数减法” | “三位数加减” | “多数相加”
    Row 3: “混合加减” | “两位数乘一位数” | “三位数乘一位数”
    Row 4: “两位数乘11” | “两位数乘15” | “两位数乘两位数”
    Row 5: “三位数除一位数” (估) | “三位数除两位数” (估) | “乘法估算” (估)
    Row 6: “五位数除三位数” (估) | “三位数除四位数” (估) | “公式”
  - Grouped white settings card: “题量” / “快速(10题) >” and “误差标准” / sky-blue pill “实战 ±2%” / “>”.
  - Primary sky-blue button “开始练习”; twin ice-blue buttons “导出题目” and “历史记录”.
  - Bottom left white pill “N-back 关闭” + pale sky-blue “?”; bottom right sky-blue circular “+” FAB.
- Pure 2D flat Figma-grade 9:16 UI screenshot, bleeding to all four edges. No phone frame, no perspective, no WeChat capsule, no pink anywhere, no watermark, ultra-sharp Simplified Chinese glyphs.
```

---

### 4.2 `02` ~ `08` 共用视觉前缀（双图输入：Input Image 1 = 对应原图 `ui_v1_backup/<file>`，Input Image 2 = 新晴空蓝白 `design/ui/01_home.png`）

```text
Use case: ui-mockup recolor and clarity upgrade. Recolor Input Image 1 so it matches the exact SKY BLUE & PURE WHITE visual design system of Input Image 2 (the approved sky-blue Home master for “行测速算”).

STRICT RULES:
1. Keep 100% of Input Image 1’s exact layout, component positions, typography hierarchy, formulas, numerals, handwriting, tables, and Simplified Chinese copy VERBATIM — do not drop, alter, or paraphrase a single character or digit.
2. COMPLETELY REMOVE all pink, rose, blush, magenta, and warm plum tones from Input Image 1 (including any blurred background behind bottom sheets, borders, shadows, progress bars, cursors, pills, switches, and trays).
3. Match Input Image 2’s crisp Apple HIG × Material 3 Sky Blue & Pure White palette exactly: canvas #F4F8FD, pure white cards #FFFFFF with crisp #D5E6FA hairline borders, clear ice-blue secondary fills #E8F3FF, pale sky-blue badges #D6EAFF with #2277DF text, vibrant clear sky-blue primary #3B93F6 (#52A6FF to #2F84EC gradient) with pure white text, razor-sharp deep navy-charcoal text #162436, slate blue-gray secondary text #5D7188.
4. Pure flat 2D 9:16 full-bleed mobile UI screenshot, Figma-level sharpness, zero phone frame, zero perspective, zero watermark.
```

---

### 4.3 各页面追加指令（拼接在 4.2 共用前缀之后）

#### 图 2：`02_practice_normal.png`
```text
SCREEN 02 (Standard Practice Screen):
- Top bar: back chevron in pale sky-blue circle #E8F3FF, “五位数除三位数”, pale sky-blue pill “±2%” (#D6EAFF / #2277DF text), “00:18”, “3/10”; slim 30% sky-blue #3B93F6 progress bar on ice-blue #E8F3FF track.
- Central pure white #FFFFFF card with crisp #D5E6FA outline: “54321 ÷ 213 = ?” in sharp #162436 tabular numerals; white input box with crisp sky-blue #3B93F6 border, “255”, and thin sky-blue vertical cursor; clean sea-teal status pill (#E5F6FA fill, #148CA8 text) reading “✓ 估算通过 · 精确 255.0 · 误差 0.0%”.
- Mid toolbar: ice-blue pill “草稿板 · 关” with fine pen icon; ice-blue segmented pill “正序 | 倒序 | 乱序” with raised white thumb on “正序”.
- Bottom 4×4 custom keypad on clean ice-blue tray #E8F3FF: crisp pure white #FFFFFF keycaps with clean subtle cool shadows and #162436 numerals (rows “1 2 3”, “4 5 6”, “7 8 9”, “. 0 -”); 4th column pale sky-blue “⌫” and “C” keys, and tall sky-blue #3B93F6 button spanning rows 3–4 labeled “下一题” in pure white. Zero pink anywhere.
```

#### 图 3：`03_practice_stylus.png`
```text
SCREEN 03 (Stylus Scratchpad Practice Screen):
- Top bar: “<”, “三位数除四位数”, pale sky-blue “±2%”, “00:34”, “6/10”; slim 60% sky-blue #3B93F6 progress bar on ice-blue #E8F3FF track.
- Compact pure white #FFFFFF formula card: “534 ÷ 4218 ≈”, crisp sky-blue #3B93F6 bordered white input reading “12.7”, and “%” on right.
- Large rounded pure white writing canvas with crisp light sky-blue dot grid (#D5E6FA dots) and clean #D5E6FA border. Keep the EXACT dark navy-charcoal handwritten working “534 ÷ 42 ≈ 12.7%” and the exact long division (“42 | 534”, “42”, “114”, “84”, “30”) crisp and unaltered. Top-right translucent white pill toolbar with the 4 crisp monoline icons (undo, trash, stroke width, collapse).
- Bottom 4×4 custom keypad on clean ice-blue tray #E8F3FF with pure white #FFFFFF keycaps in DESCENDING calculator order (“7 8 9”, “4 5 6”, “1 2 3”, “. 0 -”), pale sky-blue “⌫” and “C”, and tall sky-blue #3B93F6 “下一题” button. Zero pink anywhere.
```

#### 图 4：`04_result.png`
```text
SCREEN 04 (Practice Report & Review Screen):
- Top bar: home icon, centered “练习报告”, right “五位数除三位数” on crisp #F4F8FD canvas.
- Top pure white #FFFFFF summary card with crisp #D5E6FA border: vibrant sky-blue #3B93F6 90% circular progress ring over clean ice-blue #E8F3FF track, centered “90%” / “正确率”; right metrics “总用时 01:12”, “均速 7.2秒/题”, “平均误差 ±0.8%” with crisp #E3EEFA horizontal hairlines.
- Four crisp pure white #FFFFFF review cards preserving EVERY number and expression verbatim:
  1. pale sky-blue pill “题 1” · “54321 ÷ 213” · “你的 255 · 精确 255.0” · “5.8s” · mint-green badge “精准” (#E6F8F0 / #179B68).
  2. pale sky-blue pill “题 2” · “38412 ÷ 192” · “你的 202 · 精确 200.1” · “6.4s” · clear cyan-blue badge “通过 +0.9%” (#E5F6FA / #148CA8).
  3. pale sky-blue pill “题 3” · “71250 ÷ 345” · “你的 218 · 精确 206.5” · “9.1s” · clear coral-red badge “偏差 +5.6%” (#FDECEF / #E04B5A).
  4. pale sky-blue pill “题 4” · “46890 ÷ 231” · “你的 203 · 精确 203.0” · “6.0s” · mint-green badge “精准” (#E6F8F0 / #179B68).
- Bottom twin buttons: left ice-blue #E8F3FF “错题重练” with #162436 text; right vibrant sky-blue #3B93F6 “再来一组” with white text. Zero pink anywhere outside the single error badge on 题 3.
```

#### 图 5：`05_history.png`
```text
SCREEN 05 (History & Statistics Screen):
- Crisp #F4F8FD canvas, top bar “<” and centered “历史记录”.
- Pure white #FFFFFF 3-column summary card with crisp #D5E6FA outline: “1,280” / “累计题数”, sky-blue #2277DF “92%” / “综合正确率”, “6.4秒” / “平均单题”.
- Three filter pills: sky-blue #3B93F6 selected white “全部”, ice-blue #E8F3FF “估算题型”, ice-blue #E8F3FF “精确题型”.
- Groups “今天” and “昨天” with 4 crisp pure white #FFFFFF history cards preserving every title, icon (inside pale sky-blue #E8F3FF circle with #2277DF math symbol), metadata, percentage, and duration verbatim:
  - 今天: “五位数除三位数” / “10题 · ±2% · 09:20” / “90%” / “01:12”; “三位数除四位数” / “10题 · ±2% · 08:46” / “100%” / “01:04”.
  - 昨天: “两位数乘两位数” / “10题 · 18:42” / “90%” / “01:08”; “五位数除三位数” / “10题 · ±2% · 12:15” / “100%” / “01:10”.
- Zero pink anywhere.
```

#### 图 6：`06_custom_config.png`
```text
SCREEN 06 (Custom Config BottomSheet):
- IMPORTANT: The softly blurred dimmed Home screen visible above the bottom sheet MUST match Input Image 2 (the new SKY BLUE & PURE WHITE Home screen — NO pink top button or pink switch in the blurred background!).
- Pure white #FFFFFF 24dp-top-rounded bottom sheet with centered muted slate-gray drag handle, title “自定义题型配置”, and pale sky-blue circular “×” close button.
- Selected pills in vibrant sky blue #3B93F6 with crisp white text: “除法”, “5位”, “3位”, “±2%”. Unselected pills pure white with crisp #CDE1F8 hairline borders and #162436 text.
- Switch panel in clean ice blue #E8F3FF with two rows “允许负数结果” and “以百分比(%)作答”, both switches cleanly OFF (cool light blue-gray track, white knob on left).
- Bottom full-width vibrant sky-blue #3B93F6 button “保存并选用” in white text. Zero pink anywhere.
```

#### 图 7：`07_export_sheet.png`
```text
SCREEN 07 (Export Worksheet BottomSheet):
- IMPORTANT: The softly blurred dimmed Home screen visible above the bottom sheet MUST match Input Image 2 (the new SKY BLUE & PURE WHITE Home screen — NO pink residue in the blurred background!).
- Pure white #FFFFFF 24dp-top-rounded bottom sheet, title “导出练习卷”, pale sky-blue circular “×”.
- Ice-blue #E8F3FF row: “题型” | “五位数除三位数 >”.
- “导出题量” pills: “10题”, sky-blue #3B93F6 selected “20题”, “30题”, “50题”.
- “导出格式” 3 cards: “PDF 打印卷” selected with ice-blue #E8F3FF fill and crisp sky-blue #3B93F6 border & icon; “纯文本复制” and “高清长图” pure white with #CDE1F8 border.
- Ice-blue #E8F3FF switch row “附带参考答案与误差区间” with sky-blue #3B93F6 ON switch.
- Pure white paper preview card with crisp #CDE1F8 border preserving exact 3 lines:
  “1. 54321 ÷ 213 = ______”
  “2. 38412 ÷ 192 = ______”
  “3. 62910 ÷ 318 = ______”
- Bottom full-width sky-blue #3B93F6 button “生成导出” in white text. Zero pink anywhere.
```

#### 图 8：`08_baifen_sheet.png`
```text
SCREEN 08 (Fraction-to-Percentage Cheat Sheet BottomSheet):
- IMPORTANT: The softly blurred dimmed Home screen visible above the bottom sheet MUST match Input Image 2 (the new SKY BLUE & PURE WHITE Home screen — NO pink residue in the blurred background!).
- Pure white #FFFFFF 24dp-top-rounded bottom sheet, top ice-blue #E8F3FF segmented pill with raised white active “百化分速查表” and inactive “误差标准说明”.
- Crisp 2-column × 8-row table with alternating ultra-light sky white #F5F9FE and pure white #FFFFFF rows, crisp #D5E6FA gridlines, and 100% VERBATIM sharp #162436 fractions & percentages:
  “1/2 = 50%” | “1/11 ≈ 9.1%”
  “1/3 ≈ 33.3%” | “1/12 ≈ 8.3%”
  “1/4 = 25%” | “1/13 ≈ 7.7%”
  “1/5 = 20%” | “1/14 ≈ 7.1%”
  “1/6 ≈ 16.7%” | “1/15 ≈ 6.7%”
  “1/7 ≈ 14.3%” | “1/16 = 6.25%”
  “1/8 = 12.5%” | “1/17 ≈ 5.9%”
  “1/9 ≈ 11.1%” | “1/18 ≈ 5.6%”
- Clean ice-blue #E8F3FF tip bar reading verbatim: “提示：实战默认 ±2% 误差，截三位直除建议切至 ±1% 精算档”.
- Bottom twin buttons: left ice-blue #E8F3FF “去新建自定义” (#162436 text), right vibrant sky-blue #3B93F6 “我知道了” (white text). Zero pink anywhere.
```
