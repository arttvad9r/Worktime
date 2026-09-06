package com.worktime.app.ui.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Small shared dimension contract for the WorkTime interface.
 * Screens should reach for these before inventing local values.
 */
object AppDimens {
    /** Horizontal padding of every full screen and modal sheet. */
    val screenHorizontalPadding: Dp = 16.dp

    /** Space above a semantic section (and around dividers between sections). */
    val sectionSpacing: Dp = 16.dp

    /** Vertical gap between rows and small blocks. */
    val rowGap: Dp = 8.dp

    /** Compact but accessible interactive row height used across screens and sheets. */
    val rowMinHeight: Dp = 48.dp

    /** Rows carrying a secondary explanatory line get a little more breathing room. */
    val rowWithSubtitleMinHeight: Dp = 56.dp

    /** Minimum interactive height for segmented controls. */
    val compactControlHeight: Dp = 48.dp

    /**
     * Visible chrome for inline numeric values. The host row remains 48–52 dp high;
     * only the painted input surface is compact so it stays proportional to body text.
     */
    val compactFieldWidth: Dp = 84.dp
    val compactFieldHeight: Dp = 32.dp

    /** Minimum height of primary actions. */
    val primaryButtonMinHeight: Dp = 52.dp

    /** Short, non-layout motion used only for visual feedback. */
    const val feedbackAnimationMillis: Int = 120
}
