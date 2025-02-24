package com.example.animeapp.utils

import org.ocpsoft.prettytime.PrettyTime
import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    fun formatDateToAgo(dateString: String): String {
        val prettyTime = PrettyTime(Locale.getDefault())
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
        val date: Date = sdf.parse(dateString)!!
        return prettyTime.format(date)
    }

    fun formatDate(year: Int, month: Int, dayOfMonth: Int): String {
        return String.format(Locale.getDefault(),"%04d-%02d-%02d", year, month + 1, dayOfMonth)
    }
}