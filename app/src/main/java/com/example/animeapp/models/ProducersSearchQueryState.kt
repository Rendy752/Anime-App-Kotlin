package com.example.animeapp.models

import com.example.animeapp.utils.Limit

data class ProducersSearchQueryState(
    val query: String = "",
    val page: Int = 1,
    val limit: Int? = 25,

    val orderBy: String? = null,
    val sort: String? = null,
    val letter: String? = null,
) {
    fun isDefault(): Boolean {
        return query.isBlank() &&
                orderBy == null &&
                sort == null &&
                letter == null
    }

    fun defaultLimitAndPage(): ProducersSearchQueryState {
        return copy(page = 1, limit = Limit.DEFAULT_LIMIT)
    }

    fun resetProducers(): ProducersSearchQueryState {
        return defaultLimitAndPage().copy(
            query = "",
            orderBy = null,
            sort = null,
        )
    }
}