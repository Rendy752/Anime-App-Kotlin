package com.example.animeapp.repository

import com.example.animeapp.data.remote.api.AnimeAPI

class AnimeStreamingRepository(private val runwayAPI: AnimeAPI) {
    suspend fun getAnimeAniwatchSearch(keyword: String) = runwayAPI.getAnimeAniwatchSearch(keyword)

    suspend fun getEpisodes(id: String) = runwayAPI.getEpisodes(id)

    suspend fun getEpisodeServers(episodeId: String) = runwayAPI.getEpisodeServers(episodeId)

    suspend fun getEpisodeSources(episodeId: String, server: String, category: String) =
        runwayAPI.getEpisodeSources(episodeId, server, category)
}