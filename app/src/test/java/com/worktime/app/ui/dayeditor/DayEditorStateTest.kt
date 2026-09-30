package com.worktime.app.ui.dayeditor

import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DayEditorStateTest {
    private val date = LocalDate.of(2026, 8, 21)

    private fun evaluate(
        duration: String = "",
        rate: String = "",
        bonus: String = "",
        penalty: String = "",
    ) = evaluateDayEditor(date, DayEditorInput(duration, rate, bonus, penalty))

    @Test
    fun `duration parsing accepts hours and minutes and rejects out of range`() {
        assertEquals(0, parseDurationMinutes(""))
        assertEquals(8 * 60, parseDurationMinutes("8"))
        assertEquals(8 * 60 + 30, parseDurationMinutes("8:30"))
        assertEquals(24 * 60, parseDurationMinutes("24"))
        assertNull(parseDurationMinutes("24:01"))
        assertNull(parseDurationMinutes("25"))
        assertNull(parseDurationMinutes("8:60"))
        assertNull(parseDurationMinutes("1:2:3"))
    }

    @Test
    fun `empty form cannot be saved and has no errors`() {
        val state = evaluate()

        assertFalse(state.canSave)
        assertFalse(state.durationError || state.rateError || state.bonusError || state.penaltyError)
    }

    @Test
    fun `worked time requires a positive rate`() {
        val state = evaluate(duration = "8")

        assertTrue(state.rateError)
        assertFalse(state.canSave)
    }

    @Test
    fun `valid duration and rate produce an entry with total`() {
        val state = evaluate(duration = "8:30", rate = "100")

        assertTrue(state.canSave)
        assertEquals(510, state.entry!!.workedMinutes)
        assertEquals(850_000_000L, state.totalMicros)
    }

    @Test
    fun `bonus alone is a valid entry without rate`() {
        val state = evaluate(bonus = "500")

        assertTrue(state.canSave)
        assertEquals(0, state.entry!!.workedMinutes)
        assertEquals(500_000_000L, state.totalMicros)
    }

    @Test
    fun `penalty alone yields a negative total`() {
        val state = evaluate(penalty = "200")

        assertNotNull(state.entry)
        assertEquals(-200_000_000L, state.totalMicros)
    }

    @Test
    fun `invalid duration and oversized amounts are flagged`() {
        val state = evaluate(duration = "25", rate = "100", bonus = "999999999999")

        assertTrue(state.durationError)
        assertTrue(state.bonusError)
        assertFalse(state.canSave)
    }

    @Test
    fun `comma decimal separator is accepted`() {
        val state = evaluate(duration = "1", rate = "10,5")

        assertEquals(10_500_000L, state.entry!!.hourlyRateMicros)
    }
}
