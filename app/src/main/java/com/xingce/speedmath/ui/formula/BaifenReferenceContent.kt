package com.xingce.speedmath.ui.formula

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xingce.speedmath.domain.data.BaifenReference
import com.xingce.speedmath.domain.data.BaifenTable
import com.xingce.speedmath.domain.data.KnowledgeReference
import com.xingce.speedmath.ui.components.SkyCard
import com.xingce.speedmath.ui.components.SkyChoicePill
import com.xingce.speedmath.ui.theme.PrimarySkyText
import com.xingce.speedmath.ui.theme.SurfaceIceBlue
import com.xingce.speedmath.ui.theme.SurfaceWhite
import com.xingce.speedmath.ui.theme.TabularTextStyle
import com.xingce.speedmath.ui.theme.TextPrimary
import com.xingce.speedmath.ui.theme.TextSecondary

private enum class ReferenceSection(val label: String) {
    BAIFEN("常用百化分"),
    CAGR("年均 (1+r)ⁿ"),
    SQUARES("平方与截位"),
    PITFALLS("易错概念"),
}

private val tableLine = Color(0xFFDCEAFB)
private val tableStripe = Color(0xFFF5F9FE)
private val tableShape = RoundedCornerShape(13.dp)
private val quickValues = BaifenTable.entries.map { "${it.fraction} ${it.symbol} ${it.percent}" }
    .chunked(8).let { (left, right) -> left.zip(right) }

@Composable
internal fun BaifenReferenceContent(onCopy: (String) -> Unit) {
    var section by rememberSaveable { mutableStateOf(ReferenceSection.BAIFEN) }
    Column(Modifier.fillMaxSize()) {
        LazyRow(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(ReferenceSection.entries) { item ->
                SkyChoicePill(item.label, section == item, { section = item }, compact = true)
            }
        }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val preferredHeight = (maxWidth / 8).coerceIn(42.dp, 54.dp)
            val rowHeight = ((maxHeight - 84.dp) / 8).coerceIn(38.dp, preferredHeight)
            key(section) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    when (section) {
                        ReferenceSection.BAIFEN -> baifenItems(rowHeight, onCopy)
                        ReferenceSection.CAGR -> cagrItems()
                        ReferenceSection.SQUARES -> squareAndTruncationItems()
                        ReferenceSection.PITFALLS -> items(KnowledgeReference.pitfalls) { pitfall ->
                            ReferenceCard(pitfall.title) { ReferenceText(pitfall.explanation) }
                        }
                    }
                }
            }
        }
    }
}

private fun LazyListScope.baifenItems(rowHeight: Dp, onCopy: (String) -> Unit) {
    item {
        Column(Modifier.fillMaxWidth().clip(tableShape).border(1.dp, tableLine, tableShape)) {
            quickValues.forEachIndexed { index, (left, right) ->
                if (index > 0) HorizontalDivider(color = tableLine)
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).background(stripe(index))) {
                    QuickCell(left, rowHeight, Modifier.weight(1f), onCopy)
                    VerticalDivider(Modifier.fillMaxHeight(), color = tableLine)
                    QuickCell(right, rowHeight, Modifier.weight(1f), onCopy)
                }
            }
        }
    }
    item {
        ReferenceCallout("速算提示：实战默认 ±2% 误差，截三位直除建议切至 ±1% 精算档", compact = true)
    }
    item {
        ReferenceCard("移位联动口诀") {
            ReferenceText(KnowledgeReference.baifenShiftRule)
        }
    }
    item {
        ReferenceCard("基础百化分补全") {
            KnowledgeReference.baseBaifen.filter { it.fraction in setOf("1/10", "1/19", "1/20") }
                .forEach { BaifenDetail(it, onCopy) }
        }
    }
    item {
        ReferenceCard("常用分数的移位变形") {
            val quickFractions = BaifenTable.entries.map { it.fraction }.toSet()
            KnowledgeReference.baseBaifen.filter { it.fraction in quickFractions }
                .forEach { BaifenDetail(it, onCopy) }
        }
    }
    item { ReferenceCallout("分子不为 1 的增长量通式\n${KnowledgeReference.baifenGrowthRule}") }
    KnowledgeReference.extendedBaifen.groupBy { it.group }.forEach { (group, members) ->
        item {
            ReferenceCard(group) {
                members.forEach { BaifenDetail(it.reference, onCopy) }
            }
        }
    }
    items(KnowledgeReference.baifenTricks) { trick ->
        ReferenceCard(trick.title) { trick.paragraphs.forEach { ReferenceText(it) } }
    }
}

@Composable
private fun QuickCell(value: String, rowHeight: Dp, modifier: Modifier, onCopy: (String) -> Unit) {
    Box(
        modifier.fillMaxHeight().heightIn(min = rowHeight)
            .clickable(role = Role.Button, onClick = { onCopy(value) })
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium.TabularTextStyle, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BaifenDetail(reference: BaifenReference, onCopy: (String) -> Unit) {
    val line = "${reference.fraction} ${reference.symbol} ${reference.percent}"
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = { onCopy(line) })
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(line, color = PrimarySkyText, style = MaterialTheme.typography.titleSmall.TabularTextStyle)
        ReferenceText(reference.note)
    }
}

private fun LazyListScope.cagrItems() {
    item {
        ReferenceCard("常用 (1+r)ⁿ 速查矩阵") {
            Column(Modifier.fillMaxWidth().clip(tableShape).border(1.dp, tableLine, tableShape)) {
                MatrixRow(listOf("增速 r", "n = 2", "n = 3", "n = 4", "n = 5"), SurfaceIceBlue)
                KnowledgeReference.cagrPowerMatrix.forEachIndexed { index, row ->
                    HorizontalDivider(color = tableLine)
                    MatrixRow(listOf(row.rate) + row.powers, stripe(index))
                }
            }
        }
    }
    item { ReferenceCallout("翻倍速记锚点（72 法则辅助）\n${KnowledgeReference.cagrDoublingAnchors}") }
    item { ReferenceCard("年均增长率公式") { ReferenceText(KnowledgeReference.cagrFormula) } }
    items(KnowledgeReference.cagrCalibers) { caliber ->
        ReferenceCard(caliber.title) {
            ReferenceText(caliber.example)
            ReferenceText("初期 ${caliber.start} · 末期 ${caliber.end}")
            Text("n 取 ${caliber.yearDifference}", color = PrimarySkyText, style = MaterialTheme.typography.titleSmall)
            ReferenceText(caliber.note)
        }
    }
    item { ReferenceCallout(KnowledgeReference.cagrPeriodWarning) }
    items(KnowledgeReference.cagrMethods) { method ->
        ReferenceCard(method.title) { method.paragraphs.forEach { ReferenceText(it) } }
    }
}

@Composable
private fun MatrixRow(cells: List<String>, color: Color) {
    Row(Modifier.fillMaxWidth().background(color), verticalAlignment = Alignment.CenterVertically) {
        cells.forEachIndexed { index, cell ->
            val extra = cell.indexOf('（')
            Column(
                Modifier.weight(if (index == 0) 0.78f else 1f).padding(horizontal = 2.dp, vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    if (extra == -1) cell else cell.substring(0, extra),
                    style = MaterialTheme.typography.labelMedium.TabularTextStyle,
                    color = if (index == 0) PrimarySkyText else TextPrimary,
                    textAlign = TextAlign.Center,
                )
                if (extra != -1) {
                    Text(
                        cell.substring(extra), color = TextSecondary, textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall.TabularTextStyle,
                    )
                }
            }
        }
    }
}

private fun LazyListScope.squareAndTruncationItems() {
    item {
        ReferenceCard("常用平方数") {
            KnowledgeReference.squares.forEachIndexed { index, row ->
                MatrixRow(row.map { "${it.base}² = ${it.result}" }, stripe(index))
            }
            ReferenceText(KnowledgeReference.squareUsage)
        }
    }
    item { ReferenceCallout("平方数心算规律\n${KnowledgeReference.squareMnemonic}") }
    item {
        ReferenceCard("截位直除与误差档位") {
            KnowledgeReference.truncationRules.forEachIndexed { index, rule ->
                if (index > 0) HorizontalDivider(color = tableLine)
                Text(rule.title, color = PrimarySkyText, style = MaterialTheme.typography.titleSmall)
                ReferenceText("选项判定　${rule.choiceRule}")
                ReferenceText("相对差距 ${rule.choiceGap} · 理论误差 ${rule.error}")
                ReferenceText("App 档位　${rule.appTolerance}")
                ReferenceText(rule.instruction)
            }
        }
    }
    item {
        ReferenceCard("四大实战口诀") {
            KnowledgeReference.truncationMnemonics.forEach { ReferenceText(it) }
        }
    }
}

@Composable
private fun ReferenceCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    SkyCard(Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleSmall)
            content()
        }
    }
}

@Composable
private fun ReferenceCallout(text: String, compact: Boolean = false) {
    Box(
        Modifier.fillMaxWidth().background(SurfaceIceBlue, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text, color = TextPrimary,
            style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun ReferenceText(text: String) {
    Text(text, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
}

private fun stripe(index: Int) = if (index % 2 == 0) tableStripe else SurfaceWhite
