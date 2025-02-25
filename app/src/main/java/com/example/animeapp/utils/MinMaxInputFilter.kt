package com.example.animeapp.utils

import android.text.InputFilter
import android.text.Spanned

class MinMaxInputFilter(private val min: Double, private val max: Double) : InputFilter {
    override fun filter(
        source: CharSequence,
        start: Int,
        end: Int,
        dest: Spanned,
        dstart: Int,
        dend: Int,
    ): CharSequence? {
        val inputString = (dest.subSequence(0, dstart).toString() +
                source.subSequence(start, end) +
                dest.subSequence(dend, dest.length))

        if (inputString.isBlank()) {
            return null
        }

        try {
            val input = inputString.toDouble()

            if (input in min..max) {
                return null
            } else {
                return ""
            }
        } catch (e: NumberFormatException) {
            e.printStackTrace()
        }
        return ""
    }
}