package com.example.animeapp.models

import com.example.animeapp.utils.Limit

data class AnimeSearchQueryState(
    val query: String = "",
    val page: Int = 1,
    val limit: Int? = Limit.DEFAULT_LIMIT,

    val type: String? = null,
    val score: Double? = null,
    val minScore: Double? = null,
    val maxScore: Double? = null,
    val status: String? = null,
    val rating: String? = null,
    val sfw: Boolean? = null,
    val unapproved: Boolean? = null,
    val genres: String? = null,
    val genresExclude: String? = null,
    val orderBy: String? = null,
    val sort: String? = null,
    val letter: String? = null,
    val producers: String? = null,
    val startDate: String? = null,
    val endDate: String? = null
) {
    fun isDefault(): Boolean {
        return query.isBlank() &&
                type == null &&
                score == null &&
                minScore == null &&
                maxScore == null &&
                status == null &&
                rating == null &&
                sfw == null &&
                unapproved == null &&
                orderBy == null &&
                sort == null &&
                letter == null &&
                startDate == null &&
                endDate == null
    }

    fun isGenresDefault(): Boolean {
        return genres == null && genresExclude == null
    }

    fun isProducersDefault(): Boolean {
        return producers == null
    }

    fun defaultLimitAndPage(): AnimeSearchQueryState {
        return copy(page = 1, limit = Limit.DEFAULT_LIMIT)
    }

    fun resetGenres(): AnimeSearchQueryState {
        return copy(
            page = 1, limit = Limit.DEFAULT_LIMIT, genres = null, genresExclude = null
        )
    }

    fun resetProducers(): AnimeSearchQueryState {
        return copy(
            page = 1, limit = Limit.DEFAULT_LIMIT, producers = null
        )
    }

    fun resetBottomSheetFilters(): AnimeSearchQueryState {
        return defaultLimitAndPage().copy(
            type = null,
            status = null,
            rating = null,
            score = null,
            minScore = null,
            maxScore = null,
            orderBy = null,
            sort = null,
            startDate = null,
            endDate = null,
            unapproved = null,
            sfw = null
        )
    }
}