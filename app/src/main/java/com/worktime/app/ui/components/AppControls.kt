package com.worktime.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/**
 * The one segmented presentation for mutually exclusive options (theme, period).
 * The selected pill is the only moving surface. A quiet container behind it keeps the
 * control visually stable while the framework rectangular press indication stays disabled.
 *
 * Position uses a critically damped spring rather than a duration-based tween. This keeps
 * the capsule interruptible and equally responsive when the display switches between
 * 120/90/60 Hz. [onIndicatorSettled] is useful when an external state change should happen
 * only after the visual selection has physically reached its destination (theme palette).
 */
@Composable
fun AppSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onIndicatorSettled: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (options.isEmpty()) return

    val haptics = LocalHapticFeedback.current
    val largeFont = LocalDensity.current.fontScale >= 1.3f
    val controlHeight = if (largeFont) 56.dp else AppDimens.compactControlHeight

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(controlHeight)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f),
                shape = MaterialTheme.shapes.small,
            ),
    ) {
        val safeIndex = selectedIndex.coerceIn(options.indices)
        val segmentWidth = maxWidth / options.size
        val indicatorOffset by animateDpAsState(
            targetValue = segmentWidth * safeIndex,
            animationSpec = spring(
                dampingRatio = AppMotion.NoBounceDampingRatio,
                stiffness = AppMotion.ControlStiffness,
            ),
            finishedListener = { onIndicatorSettled?.invoke(safeIndex) },
            label = "segmented indicator",
        )

        Box(
            modifier = Modifier
                .offset { IntOffset(indicatorOffset.roundToPx(), 0) }
                .width(segmentWidth)
                .fillMaxHeight()
                .padding(3.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.primaryContainer),
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .selectableGroup(),
        ) {
            options.forEachIndexed { index, option ->
                val selected = safeIndex == index
                val interactionSource = remember(index) { MutableInteractionSource() }
                val contentColor by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    animationSpec = tween(
                        durationMillis = AppMotion.FastMillis,
                        easing = AppMotion.StandardEasing,
                    ),
                    label = "segmented content",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(
                            selected = selected,
                            interactionSource = interactionSource,
                            indication = null,
                            role = Role.RadioButton,
                            onClick = {
                                if (index != safeIndex) {
                                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                    onSelect(index)
                                }
                            },
                        )
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = option,
                        style = if (largeFont) {
                            MaterialTheme.typography.labelMedium
                        } else {
                            MaterialTheme.typography.bodyMedium
                        },
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Full-width primary action with the shared height contract. */
@Composable
fun AppPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = AppDimens.primaryButtonMinHeight),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** Full-width destructive text action ("Удалить запись"). */
@Composable
fun AppDestructiveAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = AppDimens.rowMinHeight),
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
    }
}
