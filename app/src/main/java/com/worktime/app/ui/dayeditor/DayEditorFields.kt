package com.worktime.app.ui.dayeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.worktime.app.R
import com.worktime.app.ui.components.AppDimens
import com.worktime.app.ui.components.AppFieldValueSlot
import com.worktime.app.ui.components.CompactInputChrome

internal enum class NumericField(val labelTextRes: Int) {
    Duration(R.string.worked),
    Rate(R.string.at_rate),
    Bonus(R.string.bonus),
    Penalty(R.string.penalty),
}

private enum class AdjustmentPresentation {
    Add,
    Value,
    Active,
}

@Composable
internal fun NumericEditorSection(
    durationState: TextFieldState,
    rateState: TextFieldState,
    bonusState: TextFieldState,
    penaltyState: TextFieldState,
    editorState: TextFieldState,
    activeField: NumericField,
    bonusVisible: Boolean,
    penaltyVisible: Boolean,
    durationInputTransformation: InputTransformation,
    moneyInputTransformation: InputTransformation,
    numericKeyboardOptions: KeyboardOptions,
    durationHasError: Boolean,
    rateHasError: Boolean,
    bonusHasError: Boolean,
    penaltyHasError: Boolean,
    onActivateField: (NumericField) -> Unit,
    onShowBonus: () -> Unit,
    onShowPenalty: () -> Unit,
    onNext: () -> Unit,
    editorFocusRequester: FocusRequester,
    onEditorFocusChanged: (Boolean) -> Unit,
) {
    // The 40 dp numeric field sits inside a 52 dp row, leaving a deliberate 6 dp inset
    // above and below so focused/error outlines stay visually separate from the section frame.
    val rowHeight = 52.dp
    val rateY = rowHeight
    val adjustmentTop = rateY + rowHeight
    val penaltyTop = adjustmentTop + rowHeight
    val sectionHeight = penaltyTop + rowHeight
    val editorY = when (activeField) {
        NumericField.Duration -> 0.dp
        NumericField.Rate -> rateY
        NumericField.Bonus -> adjustmentTop
        NumericField.Penalty -> penaltyTop
    }
    val bonusPresentation = when {
        activeField == NumericField.Bonus -> AdjustmentPresentation.Active
        bonusVisible -> AdjustmentPresentation.Value
        else -> AdjustmentPresentation.Add
    }
    val penaltyPresentation = when {
        activeField == NumericField.Penalty -> AdjustmentPresentation.Active
        penaltyVisible -> AdjustmentPresentation.Value
        else -> AdjustmentPresentation.Add
    }
    val sectionShape = MaterialTheme.shapes.medium

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(sectionHeight)
            .clip(sectionShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = sectionShape,
            ),
    ) {
        if (activeField != NumericField.Duration) {
            EditorValueRow(
                label = stringResource(NumericField.Duration.labelTextRes),
                valueText = durationState.text.toString().ifBlank { null },
                placeholderText = stringResource(R.string.duration_placeholder),
                isError = durationHasError,
                onClick = { onActivateField(NumericField.Duration) },
                modifier = Modifier
                    .offset(y = 0.dp)
                    .testTag("day-editor-row-duration")
                    .fillMaxWidth()
                    .height(rowHeight),
            )
        }
        if (activeField != NumericField.Rate) {
            EditorValueRow(
                label = stringResource(NumericField.Rate.labelTextRes),
                valueText = rateState.text.toString().ifBlank { null },
                placeholderText = "—",
                isError = rateHasError,
                onClick = { onActivateField(NumericField.Rate) },
                modifier = Modifier
                    .offset(y = rateY)
                    .testTag("day-editor-row-rate")
                    .fillMaxWidth()
                    .height(rowHeight),
            )
        }

        AdjustmentSlot(
            presentation = bonusPresentation,
            label = stringResource(NumericField.Bonus.labelTextRes),
            valueText = bonusState.text.toString().ifBlank { null },
            isError = bonusHasError,
            onClick = { onActivateField(NumericField.Bonus) },
            onAdd = onShowBonus,
            testTag = "day-editor-row-bonus",
            modifier = Modifier
                .offset(y = adjustmentTop)
                .fillMaxWidth()
                .height(rowHeight),
        )
        AdjustmentSlot(
            presentation = penaltyPresentation,
            label = stringResource(NumericField.Penalty.labelTextRes),
            valueText = penaltyState.text.toString().ifBlank { null },
            isError = penaltyHasError,
            onClick = { onActivateField(NumericField.Penalty) },
            onAdd = onShowPenalty,
            testTag = "day-editor-row-penalty",
            modifier = Modifier
                .offset(y = penaltyTop)
                .fillMaxWidth()
                .height(rowHeight),
        )

        PersistentNumericEditor(
            state = editorState,
            activeField = activeField,
            durationInputTransformation = durationInputTransformation,
            moneyInputTransformation = moneyInputTransformation,
            durationHasError = durationHasError,
            rateHasError = rateHasError,
            bonusHasError = bonusHasError,
            penaltyHasError = penaltyHasError,
            keyboardOptions = numericKeyboardOptions,
            onNext = onNext,
            editorFocusRequester = editorFocusRequester,
            onEditorFocusChanged = onEditorFocusChanged,
            modifier = Modifier
                .offset { IntOffset(0, editorY.roundToPx()) }
                .testTag("day-editor-active-field")
                .fillMaxWidth()
                .height(rowHeight),
        )
    }
}

@Composable
private fun AdjustmentSlot(
    presentation: AdjustmentPresentation,
    label: String,
    valueText: String?,
    isError: Boolean,
    onClick: () -> Unit,
    onAdd: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    when (presentation) {
        AdjustmentPresentation.Add -> AdjustmentAddRow(
            label = label,
            onClick = onAdd,
            modifier = modifier.testTag(testTag),
        )
        AdjustmentPresentation.Value -> EditorValueRow(
            label = label,
            valueText = valueText,
            placeholderText = null,
            isError = isError,
            onClick = onClick,
            modifier = modifier.testTag(testTag),
        )
        AdjustmentPresentation.Active -> Box(modifier = modifier)
    }
}

@Composable
private fun EditorValueRow(
    label: String,
    valueText: String?,
    placeholderText: String?,
    isError: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
        AppFieldValueSlot {
            Text(
                text = valueText ?: placeholderText.orEmpty(),
                modifier = Modifier.padding(end = AppDimens.rowGap),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (valueText != null) FontWeight.Medium else FontWeight.Normal,
                color = when {
                    isError -> MaterialTheme.colorScheme.error
                    valueText != null -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun AdjustmentAddRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun PersistentNumericEditor(
    state: TextFieldState,
    activeField: NumericField,
    durationInputTransformation: InputTransformation,
    moneyInputTransformation: InputTransformation,
    durationHasError: Boolean,
    rateHasError: Boolean,
    bonusHasError: Boolean,
    penaltyHasError: Boolean,
    keyboardOptions: KeyboardOptions,
    onNext: () -> Unit,
    editorFocusRequester: FocusRequester,
    onEditorFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDuration = activeField == NumericField.Duration
    val label = if (activeField == NumericField.Rate) {
        stringResource(R.string.hourly_rate)
    } else {
        stringResource(activeField.labelTextRes)
    }
    val isError = when (activeField) {
        NumericField.Duration -> durationHasError
        NumericField.Rate -> rateHasError
        NumericField.Bonus -> bonusHasError
        NumericField.Penalty -> penaltyHasError
    }
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
        CompactInputChrome(
            isError = isError,
            focused = isFocused,
        ) {
            BasicTextField(
                state = state,
                inputTransformation = if (isDuration) durationInputTransformation else moneyInputTransformation,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    lineHeight = MaterialTheme.typography.bodyLarge.fontSize,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                lineLimits = TextFieldLineLimits.SingleLine,
                keyboardOptions = keyboardOptions,
                onKeyboardAction = { onNext() },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(editorFocusRequester)
                    .onFocusChanged {
                        isFocused = it.isFocused
                        onEditorFocusChanged(it.isFocused)
                    },
            )
            if (isDuration && state.text.isEmpty()) {
                Text(
                    text = stringResource(R.string.duration_placeholder),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.52f),
                    maxLines = 1,
                )
            }
        }
    }
}
