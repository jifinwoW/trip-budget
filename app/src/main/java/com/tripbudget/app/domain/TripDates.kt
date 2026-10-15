package com.tripbudget.app.domain

import com.tripbudget.app.data.entity.TripEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object TripDates {
    private val shortFmt = DateTimeFormatter.ofPattern("dd MMM")
    private val fullFmt = DateTimeFormatter.ofPattern("dd MMM yyyy")

    fun formatRange(trip: TripEntity): String? {
        val start = trip.startDateEpochDay?.let { LocalDate.ofEpochDay(it) } ?: return null
        val end = trip.endDateEpochDay?.let { LocalDate.ofEpochDay(it) } ?: return null
        return if (start.year == end.year) {
            "${start.format(shortFmt)} – ${end.format(fullFmt)}"
        } else {
            "${start.format(fullFmt)} – ${end.format(fullFmt)}"
        }
    }
}
