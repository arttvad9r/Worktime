package com.worktime.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.worktime.app.ui.format.sanitizeMoneyInput

/**
 * Shared visual chrome for every compact inline numeric editor. The border is the only
 * animated part: a short color transition gives error/focus feedback without moving layout.
 */
@Composable
fun CompactInputChrome(
    isError: Boolean,
    modifier: Modifier = Modifier,
    isFocused: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val borderColor by animateColorAsState(
        targetValue = when {
            isError -> MaterialTheme.colorScheme.error
            isFocused -> MaterialTheme.colorScheme.primary.copy(alpha = 0.86f)
            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)
        },
        animationSpec = tween(AppDimens.feedbackAnimationMillis),
        label = "compact-input-border",
    )

    Surface(
        modifier = modifier
            .width(AppDimens.compactFieldWidth)
            .height(AppDimens.compactFieldHeight),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(
            width = 1.dp,
            color = borderColor,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

/**
 * Compact money input that fits settings rows instead of dominating them.
 * Owns the editor-value plumbing: sanitization, trailing-cursor placement,
 * select-all on refocusing a bare `0`.
 */
@Composable
fun CompactMoneyField(
    text: String,
    onTextChange: (String) -> Unit,
    isError: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier,
    autoFocus: Boolean = false,
    onLostFocus: (() -> Unit)? = null,
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    var hadFocus by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    var fieldValue by remember { mutableStateOf(TextFieldValue(text, TextRange(text.length))) }
    LaunchedEffect(text) {
        if (text != fieldValue.text) {
            fieldValue = TextFieldValue(text, TextRange(text.length))
        }
    }
    LaunchedEffect(autoFocus) {
        if (autoFocus) focusRequester.requestFocus()
    }
    CompactInputChrome(isError = isError, isFocused = isFocused, modifier = modifier) {
        BasicTextField(
            value = fieldValue,
            onValueChange = { updated ->
                val sanitized = sanitizeMoneyInput(updated.text)
                fieldValue = TextFieldValue(
                    text = sanitized,
                    selection = TextRange(sanitized.length),
                )
                onTextChange(sanitized)
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                    if (focusState.isFocused) {
                        hadFocus = true
                        if (fieldValue.text == "0") {
                            fieldValue = fieldValue.copy(selection = TextRange(0, 1))
                        }
                    }
                    if (onLostFocus != null && hadFocus && !focusState.isFocused && !focusState.hasFocus) {
                        onLostFocus()
                    }
                }
                .semantics { this.contentDescription = contentDescription },
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = MaterialTheme.typography.bodyLarge.fontSize,
            ),
            singleLine = true,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        )
    }
}
