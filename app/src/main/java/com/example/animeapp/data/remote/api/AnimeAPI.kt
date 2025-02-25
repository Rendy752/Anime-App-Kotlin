package com.example.animeapp.data.remote.api

import com.example.animeapp.models.AnimeDetailResponse
import com.example.animeapp.models.AnimeRecommendationResponse
import com.example.animeapp.models.AnimeSearchResponse
import com.example.animeapp.models.GenresResponse
import com.example.animeapp.models.ProducersResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface AnimeAPI {
    @GET("v4/recommendations/anime")
    suspend fun getAnimeRecommendations(
        @Query("page") page: Int = 1,
    ): Response<AnimeRecommendationResponse>

    @GET("v4/anime/{id}/full")
    suspend fun getAnimeDetail(
        @Path("id") id: Int
    ): Response<AnimeDetailResponse>

    @GET("/v4/random/anime")
    suspend fun getRandomAnime(): Response<AnimeDetailResponse>

    @GET("v4/anime")
    suspend fun getAnimeSearch(
        @Query("q") q: String,
        @Query("unapproved") unapproved: Boolean? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("type") type: String? = null,
        @Query("score") score: Double? = null,
        @Query("min_score") minScore: Double? = null,
        @Query("max_score") maxScore: Double? = null,
        @Query("status") status: String? = null,
        @Query("rating") rating: String? = null,
        @Query("sfw") sfw: Boolean? = null,
        @Query("genres") genres: String? = null,
        @Query("genres_exclude") genresExclude: String? = null,
        @Query("order_by") orderBy: String? = null,
        @Query("sort") sort: String? = null,
        @Query("letter") letter: String? = null,
        @Query("producers") producers: String? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
    ): Response<AnimeSearchResponse>

    @GET("v4/genres/anime")
    suspend fun getGenres(): Response<GenresResponse>

    @GET("v4/producers")
    suspend fun getProducers(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("q") q: String,
        @Query("order_by") orderBy: String? = null,
        @Query("sort") sort: String? = null,
        @Query("letter") letter: String? = null
    ): Response<ProducersResponse>
}