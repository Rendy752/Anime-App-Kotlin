package com.example.animeapp.repository

import com.example.animeapp.data.remote.api.AnimeAPI
import com.example.animeapp.utils.ResponseHandler.safeApiCall

class AnimeRecommendationsRepository(
    private val jikanAPI: AnimeAPI
) {
    suspend fun getAnimeRecommendations(page: Int = 1) =
        safeApiCall { jikanAPI.getAnimeRecommendations(page) }
}