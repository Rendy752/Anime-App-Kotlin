package com.example.animeapp.repository

import com.example.animeapp.data.remote.api.AnimeAPI
import com.example.animeapp.utils.ResponseHandler.safeApiCall

class AnimeStreamingRepository(private val runwayAPI: AnimeAPI) {
    suspend fun getAnimeAniwatchSearch(keyword: String) =
        safeApiCall { runwayAPI.getAnimeAniwatchSearch(keyword) }

    suspend fun getEpisodes(id: String) = safeApiCall { runwayAPI.getEpisodes(id) }

    suspend fun getEpisodeServers(episodeId: String) =
        safeApiCall { runwayAPI.getEpisodeServers(episodeId) }

    suspend fun getEpisodeSources(episodeId: String, server: String, category: String) =
        safeApiCall { runwayAPI.getEpisodeSources(episodeId, server, category) }
}