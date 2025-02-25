package com.example.animeapp.repository

import com.example.animeapp.data.remote.api.AnimeAPI

class AnimeRecommendationsRepository(
    private val api: AnimeAPI
) {
    suspend fun getAnimeRecommendations(page: Int = 1) =
        api.getAnimeRecommendations(page)
}