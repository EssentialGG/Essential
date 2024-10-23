/*
 * Copyright (c) 2024 ModCore Inc. All rights reserved.
 *
 * This code is part of ModCore Inc.'s Essential Mod repository and is protected
 * under copyright registration # TX0009138511. For the full license, see:
 * https://github.com/EssentialGG/Essential/blob/main/LICENSE
 *
 * You may not use, copy, reproduce, modify, sell, license, distribute,
 * commercialize, or otherwise exploit, or create derivative works based
 * upon, this file or any other in this repository, all of which is reserved by Essential.
 */
package gg.essential.util

import gg.essential.config.EssentialConfig
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import java.util.*
import java.util.concurrent.TimeUnit

const val DATE_FORMAT = "MMM dd, yyyy"
const val DATE_FORMAT_NO_YEAR = "MMM dd"

fun getTimeFormat(includeSeconds: Boolean): String {
    val seconds = if (includeSeconds) ":ss" else ""
    return if (EssentialConfig.timeFormat == 0) "h:mm$seconds a" else "HH:mm$seconds"
}

fun formatDate(date: LocalDate, displayYear: Boolean = true) =
    date.formatter(if (displayYear) DATE_FORMAT else DATE_FORMAT_NO_YEAR)

fun formatTime(time: TemporalAccessor, includeSeconds: Boolean) =
    time.formatter(getTimeFormat(includeSeconds))

fun formatDateAndTime(date: TemporalAccessor) =
    date.formatter("$DATE_FORMAT @ ${getTimeFormat(includeSeconds = false)}")

fun TemporalAccessor.formatter(pattern: String): String {
    val format = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH).withZone(ZoneId.systemDefault())
    return format.format(this)
}

private val weeksTimeMap = TreeMap(
    mutableMapOf(
        TimeUnit.DAYS.toMillis(7) to "w",
        TimeUnit.DAYS.toMillis(1) to "d",
        TimeUnit.HOURS.toMillis(1) to "h",
        TimeUnit.MINUTES.toMillis(1) to "m",
        TimeUnit.SECONDS.toMillis(1) to "s"
    )
)

private val daysTimeMap = TreeMap(
    mutableMapOf(
        TimeUnit.DAYS.toMillis(1) to "d",
        TimeUnit.HOURS.toMillis(1) to "h",
        TimeUnit.MINUTES.toMillis(1) to "m",
        TimeUnit.SECONDS.toMillis(1) to "s"
    )
)

fun Instant.toCosmeticOptionTime(): String {
    return Duration.between(Instant.now(), this).toShortString()
}

fun Duration.toShortString(weeks: Boolean = true, expiredText: String = "Expired"): String {
    val delta = toMillis()
    val timeMap = if (weeks) weeksTimeMap else daysTimeMap

    val ceilEntry = timeMap.floorEntry(delta) ?: return expiredText
    val floorEntry = timeMap.floorEntry(ceilEntry.key - 1)
    if (ceilEntry === floorEntry || floorEntry == null) {
        return (delta / ceilEntry.key).toString() + ceilEntry.value
    }
    val ceilUnits = delta / ceilEntry.key
    val floorUnits = (delta - ceilUnits * ceilEntry.key) / floorEntry.key

    return ceilUnits.toString() + ceilEntry.value + " " + floorUnits.toString() + floorEntry.value
}
