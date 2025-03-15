package com.example.animeapp.repository

import com.example.animeapp.data.local.dao.EpisodeDetailComplementDao
import com.example.animeapp.data.remote.api.AnimeAPI
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.utils.ResponseHandler.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AnimeStreamingRepository(
    private val episodeDetailComplementDao: EpisodeDetailComplementDao,
    private val runwayAPI: AnimeAPI
) {
    suspend fun getAnimeAniwatchSearch(keyword: String) =
        safeApiCall { runwayAPI.getAnimeAniwatchSearch(keyword) }

    suspend fun getEpisodes(id: String) = safeApiCall { runwayAPI.getEpisodes(id) }

    suspend fun getEpisodeServers(episodeId: String) =
        safeApiCall { runwayAPI.getEpisodeServers(episodeId) }

    suspend fun getEpisodeSources(episodeId: String, server: String, category: String) =
        safeApiCall { runwayAPI.getEpisodeSources(episodeId, server, category) }

    suspend fun getCachedEpisodeDetailComplement(id: String): EpisodeDetailComplement? =
        withContext(Dispatchers.IO) {
            episodeDetailComplementDao.getEpisodeDetailComplementById(id)
        }

    suspend fun insertCachedEpisodeDetailComplement(episodeDetailComplement: EpisodeDetailComplement) =
        withContext(Dispatchers.IO) {
            episodeDetailComplementDao.insertEpisodeDetailComplement(episodeDetailComplement)
        }

    suspend fun updateEpisodeDetailComplement(episodeDetailComplement: EpisodeDetailComplement) =
        withContext(Dispatchers.IO) {
            episodeDetailComplementDao.updateEpisodeDetailComplement(episodeDetailComplement)
        }
}