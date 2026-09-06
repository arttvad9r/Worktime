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
     * Inline numeric editors are intentionally smaller than their host rows. They should
     * read as values being edited, not as a second card nested inside the row.
     */
    val compactFieldWidth: Dp = 96.dp
    val compactFieldHeight: Dp = 40.dp

    /** Minimum height of primary actions. */
    val primaryButtonMinHeight: Dp = 52.dp

    /** Short, non-layout motion used only for visual feedback. */
    const val feedbackAnimationMillis: Int = 120
}
