package com.example.glassgallery.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class DaySection(
    val id: String,
    val title: String,
    val items: List<MediaItem>,
)

/** Groups items (already newest-first) into day sections with friendly titles. */
fun buildDaySections(items: List<MediaItem>): List<DaySection> {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val formatter = DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())

    return items
        .groupBy { Instant.ofEpochMilli(it.dateTaken).atZone(zone).toLocalDate() }
        .entries
        .sortedByDescending { it.key }
        .map { (date, list) ->
            val title = when (date) {
                today -> "Today"
                today.minusDays(1) -> "Yesterday"
                else -> date.format(formatter)
            }
            DaySection(id = date.toString(), title = title, items = list)
        }
}
