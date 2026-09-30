package com.worktime.app.ui.calendar

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CalendarAmountVisibilityTest {
    @Test
    fun `day amount is hidden when total is zero or unavailable`() {
        assertFalse(shouldShowDayAmount(null))
        assertFalse(shouldShowDayAmount(0L))
    }

    @Test
    fun `day amount is shown for non-zero totals`() {
        assertTrue(shouldShowDayAmount(1L))
        assertTrue(shouldShowDayAmount(-1L))
    }

    @Test
    fun `empty day description contains only the date`() {
        val description = buildDayCellDescription(
            dateLabel = "24 August 2026",
            todayLabel = null,
            selectedLabel = null,
            entryLabel = null,
            durationText = null,
            amountText = null,
            bonusText = null,
            penaltyText = null,
        )

        assertEquals("24 August 2026", description)
    }

    @Test
    fun `entry day description appends state details after the date`() {
        val description = buildDayCellDescription(
            dateLabel = "24 August 2026",
            todayLabel = "Today",
            selectedLabel = "selected",
            entryLabel = "entry recorded",
            durationText = "8 h",
            amountText = "1200",
            bonusText = null,
            penaltyText = null,
        )

        assertEquals("24 August 2026, Today, selected, entry recorded, 8 h, 1200", description)
    }

    @Test
    fun `bonus only and penalty only days have distinct descriptions`() {
        val bonusOnly = buildDayCellDescription(
            dateLabel = "24 August 2026",
            todayLabel = null,
            selectedLabel = null,
            entryLabel = "entry recorded",
            durationText = null,
            amountText = null,
            bonusText = "bonus added",
            penaltyText = null,
        )
        val penaltyOnly = buildDayCellDescription(
            dateLabel = "24 August 2026",
            todayLabel = null,
            selectedLabel = null,
            entryLabel = "entry recorded",
            durationText = null,
            amountText = null,
            bonusText = null,
            penaltyText = "penalty added",
        )

        assertEquals("24 August 2026, entry recorded, bonus added", bonusOnly)
        assertEquals("24 August 2026, entry recorded, penalty added", penaltyOnly)
        assertTrue(bonusOnly != penaltyOnly)
    }

    @Test
    fun `expanded total uses error color only when final total is negative`() {
        assertTrue(shouldUseErrorColorForTotal(-1L))
        assertFalse(shouldUseErrorColorForTotal(0L))
        assertFalse(shouldUseErrorColorForTotal(1L))
    }
}
