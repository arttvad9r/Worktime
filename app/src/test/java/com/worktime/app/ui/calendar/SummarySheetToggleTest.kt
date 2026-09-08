package com.worktime.app.ui.calendar

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalMaterial3Api::class)
class SummarySheetToggleTest {
    @Test
    fun hiddenTargetRequestsExpansion() {
        assertTrue(shouldExpandSummaryAfterToggle(SheetValue.Hidden))
    }

    @Test
    fun expandedTargetRequestsHide() {
        assertFalse(shouldExpandSummaryAfterToggle(SheetValue.Expanded))
    }

    @Test
    fun closingAnimationCanBeReversedImmediately() {
        // During closing, currentValue may still be Expanded while targetValue is already Hidden.
        // The next tap must follow the latest target and reopen instead of issuing hide() again.
        assertTrue(shouldExpandSummaryAfterToggle(SheetValue.Hidden))
    }

    @Test
    fun handleStaysVisibleWhileExpandedSheetIsBeingDraggedDown() {
        // This is the regression from the physical-device recording: target switches to Hidden
        // before the sheet has actually left the screen. Removing the handle at that moment
        // makes the top of the sheet jump/disappear under the finger.
        assertTrue(
            shouldShowSummaryHandle(
                currentValue = SheetValue.Expanded,
                targetValue = SheetValue.Hidden,
            ),
        )
    }

    @Test
    fun handleStaysVisibleWhileSheetIsOpening() {
        assertTrue(
            shouldShowSummaryHandle(
                currentValue = SheetValue.Hidden,
                targetValue = SheetValue.Expanded,
            ),
        )
    }

    @Test
    fun handleCanDisappearOnlyAfterSheetIsFullyHidden() {
        assertFalse(
            shouldShowSummaryHandle(
                currentValue = SheetValue.Hidden,
                targetValue = SheetValue.Hidden,
            ),
        )
    }
}
