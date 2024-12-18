package com.example.animeappkotlin.data.remote.api

import com.example.animeappkotlin.models.AnimeDetailResponse
import com.example.animeappkotlin.models.AnimeRecommendationResponse
import kotlinx.coroutines.runBlocking
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface AnimeAPI {
    @GET("v4/recommendations/anime")
    suspend fun getAnimeRecommendations(
        @Query("page") page: Int = 1,
    ): Response<AnimeRecommendationResponse>

    fun getAnimeRecommendationsWrapper(page: Int = 1): Response<AnimeRecommendationResponse> {
        return runBlocking { getAnimeRecommendations(page) }
    }

    @GET("v4/anime/{id}/full")
    suspend fun getAnimeDetail(
        @Path("id") id: Int
    ): Response<AnimeDetailResponse>
}