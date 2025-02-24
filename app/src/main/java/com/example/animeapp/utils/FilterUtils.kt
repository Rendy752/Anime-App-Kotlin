package com.example.animeapp.utils

import com.example.animeapp.databinding.FragmentBottomSheetFilterBinding
import com.example.animeapp.models.AnimeSearchQueryState

object FilterUtils {

    val TYPE_OPTIONS =
        listOf("Any", "TV", "Movie", "OVA", "Special", "ONA", "Music", "CM", "PV", "TV Special")
    val STATUS_OPTIONS = listOf("Any", "Airing", "Complete", "Upcoming")
    val RATING_OPTIONS = listOf("Any", "G", "PG", "PG13", "R17", "R", "Rx")
    private val RATING_DESCRIPTIONS = mapOf(
        "G" to "All Ages",
        "PG" to "Children",
        "PG13" to "Teens 13 or older",
        "R17" to "17+ (violence & profanity)",
        "R" to "Mild Nudity",
        "Rx" to "Hentai"
    )
    val ORDER_BY_OPTIONS = listOf(
        "Any", "mal_id", "title", "start_date", "end_date", "episodes", "score",
        "scored_by", "rank", "popularity", "members", "favorites"
    )
    val SORT_OPTIONS = listOf("Any", "desc", "asc")

    fun collectFilterValues(
        currentState: AnimeSearchQueryState,
        binding: FragmentBottomSheetFilterBinding
    ): AnimeSearchQueryState {
        binding.apply {
            val enableDateRange = binding.enableDateRangeCheckBox.isChecked

            val startDate = if (!enableDateRange) {
                null
            } else {
                DateUtils.formatDate(
                    binding.startDatePicker.year,
                    binding.startDatePicker.month,
                    binding.startDatePicker.dayOfMonth
                )
            }

            val endDate = if (!enableDateRange) {
                null
            } else {
                DateUtils.formatDate(
                    binding.endDatePicker.year,
                    binding.endDatePicker.month,
                    binding.endDatePicker.dayOfMonth
                )
            }

            val minScore = minScoreEditText.text.toString().toDoubleOrNull()
            val maxScore = maxScoreEditText.text.toString().toDoubleOrNull()

            return currentState.copy(
                page = 1,
                limit = 10,
                type = typeSpinner.text.toString().takeIf { it != "Any" },
                score = if (minScore != null || maxScore != null) {
                    null
                } else {
                    scoreEditText.text.toString().toDoubleOrNull()
                },
                minScore = minScore,
                maxScore = maxScore,
                status = statusSpinner.text.toString().takeIf { it != "Any" },
                rating = RATING_DESCRIPTIONS.entries.firstOrNull {
                    it.value == ratingSpinner.text.toString()
                }?.key?.takeIf { it != "Any" },
                sfw = sfwCheckBox.isChecked.takeIf { it },
                unapproved = unapprovedCheckBox.isChecked.takeIf { it },
                orderBy = orderBySpinner.text.toString().takeIf { it != "Any" },
                sort = sortSpinner.text.toString().takeIf { it != "Any" },
                startDate = startDate,
                endDate = endDate
            )
        }
    }

    fun getRatingDescription(ratingCode: String): String {
        return RATING_DESCRIPTIONS[ratingCode] ?: ratingCode
    }
}