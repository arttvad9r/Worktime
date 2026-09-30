package com.worktime.app.ui.dayeditor

import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DurationSuggestionsTest {
    private fun sample(day: String, hours: Int) = DurationSample(LocalDate.parse(day), hours * 60)

    @Test
    fun `empty history offers fixed defaults`() {
        assertEquals(
            listOf(360, 480, 600, 720),
            suggestDurations(DayOfWeek.MONDAY, emptyList()),
        )
    }

    @Test
    fun `weekday and weekend habits both appear`() {
        val history = listOf(
            sample("2026-09-14", 13), // Mon
            sample("2026-09-15", 13), // Tue
            sample("2026-09-18", 15), // Fri
            sample("2026-09-19", 15), // Sat
            sample("2026-09-21", 13), // Mon
        )
        val expected = listOf(480, 720, 780, 900)
        assertEquals(expected, suggestDurations(DayOfWeek.MONDAY, history))
        assertEquals(expected, suggestDurations(DayOfWeek.FRIDAY, history))
    }

    @Test
    fun `weekday habit outranks a more common overall value`() {
        val history = listOf(
            sample("2026-09-14", 9),
            sample("2026-09-15", 9),
            sample("2026-09-16", 9),
            sample("2026-09-18", 15), // Fri
            sample("2026-09-11", 15), // Fri
        )
        val friday = suggestDurations(DayOfWeek.FRIDAY, history)
        assertEquals(true, 900 in friday && 540 in friday)
        assertEquals(4, friday.size)
    }

    @Test
    fun `never exceeds the limit and has no duplicates`() {
        val history = (1..9).map { sample("2026-09-%02d".format(it), it + 3) }
        val result = suggestDurations(DayOfWeek.MONDAY, history)
        assertEquals(MaxDurationSuggestions, result.size)
        assertEquals(result.distinct(), result)
        assertEquals(result.sorted(), result)
    }
}
