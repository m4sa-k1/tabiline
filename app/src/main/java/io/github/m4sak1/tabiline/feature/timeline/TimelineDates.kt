package io.github.m4sak1.tabiline.feature.timeline

import io.github.m4sak1.tabiline.core.model.TransportLeg
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

internal fun TransportLeg.isVisibleOn(date: LocalDate): Boolean {
    val departureDate = departureLocal.toLocalDate()
    val arrivalDate = arrivalLocal.toLocalDate()
    return date in minOf(departureDate, arrivalDate)..maxOf(departureDate, arrivalDate)
}

internal fun relativeTimelineTime(time: LocalDateTime, referenceDate: LocalDate): String {
    val days = ChronoUnit.DAYS.between(referenceDate, time.toLocalDate())
    val prefix = when (days) {
        0L -> ""
        1L -> "翌"
        2L -> "翌々"
        -1L -> "前"
        -2L -> "前々"
        else -> if (days > 0) "${days}日後" else "${-days}日前"
    }
    return prefix + time.format(DateTimeFormatter.ofPattern("HH:mm"))
}
