package com.example.animeapp.repository

import com.example.animeapp.data.local.dao.AnimeDetailDao
import com.example.animeapp.data.local.dao.AnimeDetailComplementDao
import com.example.animeapp.data.local.dao.EpisodeDetailComplementDao
import com.example.animeapp.data.remote.api.AnimeAPI
import com.example.animeapp.models.AnimeDetail
import com.example.animeapp.models.AnimeDetailComplement
import com.example.animeapp.models.AnimeDetailResponse
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.utils.DateUtils
import com.example.animeapp.utils.Resource
import com.example.animeapp.utils.ResponseHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.animeapp.utils.ResponseHandler.safeApiCall
import retrofit2.Response

class AnimeDetailRepository(
    private val animeDetailDao: AnimeDetailDao,
    private val animeDetailComplementDao: AnimeDetailComplementDao,
    private val episodeDetailComplementDao: EpisodeDetailComplementDao,
    private val jikanAPI: AnimeAPI,
    private val runwayAPI: AnimeAPI
) {
    suspend fun getAnimeDetail(id: Int): Response<AnimeDetailResponse> =
        withContext(Dispatchers.IO) {
            getCachedAnimeDetailResponse(id) ?: getRemoteAnimeDetail(id)
        }

    private suspend fun getCachedAnimeDetailResponse(id: Int): Response<AnimeDetailResponse>? {
        val cachedAnimeDetail = animeDetailDao.getAnimeDetailById(id)

        return cachedAnimeDetail?.let { cache ->
            if (isDataNeedUpdate(cache)) {
                val remoteData =
                    ResponseHandler.handleCommonResponse(jikanAPI.getAnimeDetail(cache.mal_id))

                if (remoteData is Resource.Success && remoteData.data?.data != cache) {
                    remoteData.data?.data?.let {
                        animeDetailDao.updateAnimeDetail(it)
                        Response.success(remoteData.data)
                    } ?: Response.success(AnimeDetailResponse(cache))
                } else {
                    Response.success(AnimeDetailResponse(cache))
                }
            } else {
                Response.success(AnimeDetailResponse(cache))
            }
        }
    }

    private suspend fun isDataNeedUpdate(data: AnimeDetail): Boolean {
        return data.airing && !DateUtils.isEpisodeAreUpToDate(
            data.broadcast.time,
            data.broadcast.timezone,
            data.broadcast.day,
            getCachedAnimeDetailComplementByMalId(data.mal_id)?.lastEpisodeUpdatedAt
        )
    }

    private suspend fun getRemoteAnimeDetail(id: Int): Response<AnimeDetailResponse> {
        return safeApiCall { jikanAPI.getAnimeDetail(id) }.let { response ->
            if (response.isSuccessful && response.body() != null) {
                response.body()?.data?.let {
                    animeDetailDao.insertAnimeDetail(it)
                }
            }
            response
        }
    }

    suspend fun getCachedAnimeDetailComplementByMalId(malId: Int): AnimeDetailComplement? =
        withContext(Dispatchers.IO) {
            animeDetailComplementDao.getAnimeDetailComplementByMalId(malId)
        }

    suspend fun insertCachedAnimeDetailComplement(animeDetailComplement: AnimeDetailComplement) =
        withContext(Dispatchers.IO) {
            animeDetailComplementDao.insertAnimeDetailComplement(animeDetailComplement)
        }

    suspend fun updateAnimeDetailComplementWithEpisodes(
        animeDetail: AnimeDetail,
        cachedAnimeDetailComplement: AnimeDetailComplement
    ): AnimeDetailComplement? = withContext(Dispatchers.IO) {
        if (isDataNeedUpdate(animeDetail)) {
            val episodesResponse = ResponseHandler.handleCommonResponse(
                runwayAPI.getEpisodes(cachedAnimeDetailComplement.id)
            )
            if (episodesResponse is Resource.Success) {
                val episodes = episodesResponse.data?.episodes ?: return@withContext null

                if (episodes != cachedAnimeDetailComplement.episodes) {
                    val updatedAnimeDetail = cachedAnimeDetailComplement.copy(episodes = episodes)
                    animeDetailComplementDao.updateEpisodeAnimeDetailComplement(updatedAnimeDetail)
                    return@withContext updatedAnimeDetail
                } else {
                    return@withContext cachedAnimeDetailComplement
                }
            }

            return@withContext null
        } else {
            return@withContext cachedAnimeDetailComplement
        }
    }

    suspend fun getCachedEpisodeDetailComplement(id: String): EpisodeDetailComplement? =
        withContext(Dispatchers.IO) {
            episodeDetailComplementDao.getEpisodeDetailComplementById(id)
        }

    suspend fun insertCachedEpisodeDetailComplement(episodeDetailComplement: EpisodeDetailComplement) =
        withContext(Dispatchers.IO) {
            episodeDetailComplementDao.insertEpisodeDetailComplement(episodeDetailComplement)
        }
}