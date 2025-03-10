package com.example.animeapp.utils

import org.ocpsoft.prettytime.PrettyTime
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.util.*
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

object DateUtils {
    fun formatDateToAgo(dateString: String): String {
        val prettyTime = PrettyTime(Locale.getDefault())
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
        val date: Date? = sdf.parse(dateString)
        return prettyTime.format(date)
    }

    fun formatDate(year: Int, month: Int, dayOfMonth: Int): String {
        return String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth)
    }

    fun isEpisodeAreUpToDate(
        broadcastTime: String?,
        broadcastTimezone: String?,
        broadcastDay: String?,
        lastEpisodeUpdatedAt: Long?
    ): Boolean {
        if (broadcastTime == null || broadcastTimezone == null || broadcastDay == null || lastEpisodeUpdatedAt == null) {
            return false
        }

        try {
            val broadcastLocalTime = LocalTime.parse(broadcastTime)
            val broadcastZone = ZoneId.of(broadcastTimezone)
            val userZone = ZoneId.systemDefault()

            val singularDay = broadcastDay.removeSuffix("s").uppercase(Locale.ENGLISH)
            val dayOfWeek = DayOfWeek.valueOf(singularDay)

            val todayInBroadcastZone = LocalDate.now(broadcastZone)
            val firstDayOfWeek =
                todayInBroadcastZone.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val broadcastDateThisWeek = firstDayOfWeek.with(TemporalAdjusters.nextOrSame(dayOfWeek))

            val broadcastDateTime =
                ZonedDateTime.of(broadcastDateThisWeek, broadcastLocalTime, broadcastZone)
            val thisWeekBroadcastDateTime = broadcastDateTime.withZoneSameInstant(userZone)

            val lastUpdateDateTime = Instant.ofEpochSecond(lastEpisodeUpdatedAt).atZone(userZone)

            println("lastUpdateDateTime: $lastUpdateDateTime")
            println("thisWeekBroadcastDateTime: $thisWeekBroadcastDateTime")
            val isBroadcastThisWeekPassed = lastUpdateDateTime.isAfter(thisWeekBroadcastDateTime)
            println("isBroadcastThisWeekPassed: $isBroadcastThisWeekPassed")

            if (isBroadcastThisWeekPassed) {
                return true
            } else {
                val lastDayOfWeek =
                    firstDayOfWeek.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                        .atTime(LocalTime.MAX).atZone(broadcastZone).withZoneSameInstant(userZone)
                println("lastDayOfWeek: $lastDayOfWeek")
                val isWithinThisWeek =
                    lastUpdateDateTime.isAfter(thisWeekBroadcastDateTime) && lastUpdateDateTime.isBefore(
                        lastDayOfWeek
                    )
                println("isWithinThisWeek: $isWithinThisWeek")
                return !isWithinThisWeek
            }

        } catch (e: Exception) {
            println("Error parsing broadcast time: ${e.message}")
            return false
        }
    }
}