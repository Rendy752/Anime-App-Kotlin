package com.example.animeapp.repository

import com.example.animeapp.data.remote.api.AnimeAPI

class AnimeRecommendationsRepository(
    private val jikanAPI: AnimeAPI
) {
    suspend fun getAnimeRecommendations(page: Int = 1) =
        jikanAPI.getAnimeRecommendations(page)
}