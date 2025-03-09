package com.example.animeapp.repository

import com.example.animeapp.data.remote.api.AnimeAPI
import com.example.animeapp.models.AnimeSearchQueryState
import com.example.animeapp.models.ProducersSearchQueryState

class AnimeSearchRepository(
    private val jikanAPI: AnimeAPI
) {
    suspend fun searchAnime(
        queryState: AnimeSearchQueryState
    ) = jikanAPI.getAnimeSearch(
        q = queryState.query,
        page = queryState.page,
        limit = queryState.limit,
        type = queryState.type,
        score = queryState.score,
        minScore = queryState.minScore,
        maxScore = queryState.maxScore,
        status = queryState.status,
        rating = queryState.rating,
        sfw = queryState.sfw,
        unapproved = queryState.unapproved,
        genres = queryState.genres,
        genresExclude = queryState.genresExclude,
        orderBy = queryState.orderBy,
        sort = queryState.sort,
        letter = queryState.letter,
        producers = queryState.producers,
        startDate = queryState.startDate,
        endDate = queryState.endDate
    )

    suspend fun getRandomAnime() = jikanAPI.getRandomAnime()

    suspend fun getGenres() = jikanAPI.getGenres()

    suspend fun getProducers(
        queryState: ProducersSearchQueryState
    ) = jikanAPI.getProducers(
        page = queryState.page,
        limit = queryState.limit,
        q = queryState.query,
        orderBy = queryState.orderBy,
        sort = queryState.sort,
        letter = queryState.letter
    )
}