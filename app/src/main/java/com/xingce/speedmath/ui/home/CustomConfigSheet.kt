package com.xingce.speedmath.ui.home

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xingce.speedmath.domain.model.CustomOperation
import com.xingce.speedmath.domain.model.CustomQuizConfig
import com.xingce.speedmath.domain.model.ToleranceLevel
import com.xingce.speedmath.ui.components.SkyPrimaryButton
import com.xingce.speedmath.ui.theme.SurfaceIceBlue

@Composable
internal fun CustomConfigSheet(
    initialConfig: CustomQuizConfig,
    onDismiss: () -> Unit,
    onSave: (CustomQuizConfig) -> Unit,
) {
    var draft by rememberSaveable(stateSaver = CustomConfigSaver) {
        mutableStateOf(initialConfig.normalizedForHome())
    }

    HomeBottomSheet("自定义出题配置", onDismiss) {
        HomeChoicePills("运算类型", CustomOperation.entries, draft.operation, CustomOperation::label) {
            draft = draft.copy(operation = it).normalizedForHome()
        }
        HomeChoicePills("第一数位数", (1..5).toList(), draft.firstDigits, { "${it}位" }) {
            draft = draft.copy(firstDigits = it)
        }
        HomeChoicePills("第二数位数", (1..4).toList(), draft.secondDigits, { "${it}位" }) {
            draft = draft.copy(secondDigits = it)
        }
        Column(
            modifier = Modifier.fillMaxWidth()
                .background(SurfaceIceBlue, RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            HomeSwitchRow(
                label = "允许负数结果",
                checked = draft.allowNegative,
                enabled = draft.operation.supportsNegative,
                supportingText = if (draft.operation.supportsNegative) null else "仅减法和混合加减可开启",
                onCheckedChange = { draft = draft.copy(allowNegative = it) },
            )
            HomeSwitchRow(
                label = "以百分比(%)作答",
                checked = draft.percentAnswer,
                enabled = draft.operation.supportsPercent,
                supportingText = if (draft.operation.supportsPercent) null else "仅除法可开启",
                onCheckedChange = { draft = draft.copy(percentAnswer = it) },
            )
        }
        HomeChoicePills("误差标准", ToleranceLevel.entries, draft.tolerance, {
            if (it == ToleranceLevel.EXACT) it.label else it.shortLabel
        }) {
            draft = draft.copy(tolerance = it)
        }
        SkyPrimaryButton(
            text = "保存并选用",
            onClick = { onSave(draft.normalizedForHome()) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
        )
    }
}

private val CustomConfigSaver = Saver<CustomQuizConfig, Bundle>(
    save = { config ->
        Bundle().apply {
            putString("operation", config.operation.name)
            putInt("firstDigits", config.firstDigits)
            putInt("secondDigits", config.secondDigits)
            putBoolean("allowNegative", config.allowNegative)
            putBoolean("percentAnswer", config.percentAnswer)
            putString("tolerance", config.tolerance.name)
        }
    },
    restore = {
        CustomQuizConfig(
            operation = CustomOperation.valueOf(requireNotNull(it.getString("operation"))),
            firstDigits = it.getInt("firstDigits"),
            secondDigits = it.getInt("secondDigits"),
            allowNegative = it.getBoolean("allowNegative"),
            percentAnswer = it.getBoolean("percentAnswer"),
            tolerance = ToleranceLevel.fromName(it.getString("tolerance").orEmpty()),
        )
    },
)
