package com.worktime.app.ui.dayeditor

import com.worktime.app.domain.model.WorkEntry
import java.time.DayOfWeek
import java.time.LocalDate

internal data class DurationSample(val date: LocalDate, val minutes: Int)

internal const val MaxDurationSuggestions = 4
private val DefaultSuggestionMinutes = listOf(8 * 60, 12 * 60, 10 * 60, 6 * 60)

internal fun durationSamples(entries: Collection<WorkEntry>): List<DurationSample> =
    entries.filter { it.workedMinutes > 0 }.map { DurationSample(it.date, it.workedMinutes) }

/**
 * Up to [MaxDurationSuggestions] durations, ascending: the most frequent one for this weekday,
 * then the most frequent overall, then fixed defaults. Ties prefer the more recent sample.
 */
internal fun suggestDurations(
    dayOfWeek: DayOfWeek,
    samples: List<DurationSample>,
): List<Int> {
    fun rank(source: List<DurationSample>): List<Int> = source
        .groupBy { it.minutes }
        .entries
        .sortedWith(
            compareByDescending<Map.Entry<Int, List<DurationSample>>> { it.value.size }
                .thenByDescending { entry -> entry.value.maxOf { it.date } },
        )
        .map { it.key }

    val result = LinkedHashSet<Int>()
    rank(samples.filter { it.date.dayOfWeek == dayOfWeek }).firstOrNull()?.let(result::add)
    for (minutes in rank(samples)) {
        if (result.size >= MaxDurationSuggestions - 1) break
        result.add(minutes)
    }
    for (minutes in DefaultSuggestionMinutes) {
        if (result.size >= MaxDurationSuggestions) break
        result.add(minutes)
    }
    return result.sorted()
}
