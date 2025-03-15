package com.example.animeapp.ui.animeDetail

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.animeapp.models.AnimeAniwatchSearchResponse
import com.example.animeapp.models.AnimeDetail
import com.example.animeapp.models.AnimeDetailComplement
import com.example.animeapp.models.AnimeDetailResponse
import com.example.animeapp.models.EpisodeDetailComplement
import com.example.animeapp.models.EpisodeServersResponse
import com.example.animeapp.models.EpisodeSourcesResponse
import com.example.animeapp.models.EpisodesResponse
import com.example.animeapp.repository.AnimeDetailRepository
import com.example.animeapp.repository.AnimeStreamingRepository
import com.example.animeapp.utils.FindAnimeTitle
import com.example.animeapp.utils.Resource
import com.example.animeapp.utils.ResponseHandler
import com.example.animeapp.utils.StreamingUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class AnimeDetailViewModel @Inject constructor(
    private val animeDetailRepository: AnimeDetailRepository,
    private val animeStreamingRepository: AnimeStreamingRepository
) : ViewModel() {
    val animeDetail: MutableLiveData<Resource<AnimeDetailResponse>?> = MutableLiveData()
    val animeDetailComplement: MutableLiveData<Resource<AnimeDetailComplement?>> = MutableLiveData()
    val defaultEpisode: MutableLiveData<EpisodeDetailComplement?> = MutableLiveData()

    fun handleAnimeDetail(id: Int) = viewModelScope.launch {
        animeDetail.postValue(Resource.Loading())
        animeDetail.postValue(getAnimeDetail(id))
    }

    suspend fun getAnimeDetail(id: Int): Resource<AnimeDetailResponse> {
        return ResponseHandler.handleCommonResponse(animeDetailRepository.getAnimeDetail(id))
    }

    fun handleEpisodes() = viewModelScope.launch {
        animeDetailComplement.postValue(Resource.Loading())
        val detailData =
            animeDetail.value?.data?.data
                ?: return@launch animeDetailComplement.postValue(Resource.Error("Anime data not available"))
        if (handleCachedAnimeDetailComplement(detailData)) return@launch

        if (detailData.type == "Music") return@launch animeDetailComplement.postValue(
            Resource.Error("Anime is a music, no episodes available")
        )
        if (detailData.status == "Not yet aired") return@launch animeDetailComplement.postValue(
            Resource.Error("Anime not yet aired")
        )
        val title = detailData.title
        val englishTitle = animeDetail.value?.data?.data?.title_english ?: ""
        val searchTitle = when {
            englishTitle.isNotEmpty() -> englishTitle.lowercase()
            else -> title.lowercase()
        }
        val response = animeStreamingRepository.getAnimeAniwatchSearch(searchTitle)
        if (!response.isSuccessful) {
            return@launch animeDetailComplement.postValue(
                Resource.Error(
                    response.errorBody()?.string() ?: "Unknown error"
                )
            )
        }
        handleValidEpisode(response)
    }

    private suspend fun handleCachedAnimeDetailComplement(detailData: AnimeDetail): Boolean {
        val cachedAnimeDetailComplement =
            animeDetailRepository.getCachedAnimeDetailComplementByMalId(detailData.mal_id)

        cachedAnimeDetailComplement?.let { cachedAnimeDetail ->
            val updatedAnimeDetail = animeDetailRepository.updateAnimeDetailComplementWithEpisodes(
                detailData,
                cachedAnimeDetail
            )

            if (updatedAnimeDetail == null) {
                animeDetailComplement.postValue(Resource.Error("Failed to fetch or update episodes"))
            } else {
                animeDetailComplement.postValue(Resource.Success(updatedAnimeDetail))
            }

            cachedAnimeDetail.episodes.firstOrNull()?.episodeId?.let { episodeId ->
                val cachedEpisodeDetailComplement =
                    animeDetailRepository.getCachedEpisodeDetailComplement(episodeId)
                defaultEpisode.postValue(cachedEpisodeDetailComplement)
            }

            return true
        }
        return false
    }

    private fun handleValidEpisode(response: Response<AnimeAniwatchSearchResponse>) =
        viewModelScope.launch {
            if (!response.isSuccessful) {
                animeDetailComplement.postValue(
                    Resource.Error(
                        response.errorBody()?.string() ?: "Unknown error"
                    )
                )
                return@launch
            }

            val resultResponse = response.body() ?: run {
                animeDetailComplement.postValue(
                    Resource.Error(
                        response.errorBody()?.string() ?: "Unknown error"
                    )
                )
                return@launch
            }

            animeDetail.value?.data?.data?.let { animeDetail ->
                val animes = FindAnimeTitle.findClosestAnimes(resultResponse, animeDetail)

                if (animes.isEmpty()) {
                    animeDetailComplement.postValue(Resource.Error("No matching anime found"))
                    return@launch
                }

                for (anime in animes) {
                    val animeId = anime.id.substringBefore("?").trim()
                    val episodesResponse = getEpisodes(animeId)

                    if (episodesResponse !is Resource.Success) {
                        continue
                    }

                    val defaultEpisodeServersResponse =
                        getDefaultEpisodeServers(episodesResponse.data?.episodes?.firstOrNull()?.episodeId)

                    if (defaultEpisodeServersResponse !is Resource.Success) {
                        continue
                    }

                    val defaultEpisodeSourcesResponse =
                        StreamingUtils.getEpisodeSources(
                            defaultEpisodeServersResponse,
                            animeStreamingRepository
                        )

                    if (defaultEpisodeSourcesResponse is Resource.Success && checkEpisodeSourceMalId(
                            defaultEpisodeSourcesResponse
                        )
                    ) {
                        val cachedAnimeDetailComplement = AnimeDetailComplement(
                            _id = anime.id,
                            malId = animeDetail.mal_id,
                            episodes = episodesResponse.data?.episodes ?: emptyList(),
                            eps = anime.episodes?.eps,
                            sub = anime.episodes?.sub,
                            dub = anime.episodes?.dub,
                        )

                        cachedAnimeDetailComplement.let {
                            animeDetailRepository.insertCachedAnimeDetailComplement(it)
                        }
                        animeDetailComplement.postValue(
                            Resource.Success(cachedAnimeDetailComplement)
                        )

                        defaultEpisodeServersResponse.data?.let { servers ->
                            defaultEpisodeSourcesResponse.data?.let { sources ->
                                val cachedEpisodeDetailComplement =
                                    StreamingUtils.getEpisodeQuery(
                                        Resource.Success(servers),
                                        servers.episodeId
                                    )?.let { query ->
                                        EpisodeDetailComplement(
                                            id = servers.episodeId,
                                            title = animeDetail.title,
                                            imageUrl = animeDetail.images.jpg.image_url,
                                            servers = servers,
                                            sources = sources,
                                            sourcesQuery = query
                                        )
                                    }
                                cachedEpisodeDetailComplement?.let {
                                    animeDetailRepository.insertCachedEpisodeDetailComplement(it)
                                    defaultEpisode.postValue(cachedEpisodeDetailComplement)
                                }
                            }
                        }
                        return@launch
                    }
                }

                animeDetailComplement.postValue(Resource.Error("No matching anime found"))
            }
        }

    private suspend fun getEpisodes(animeId: String): Resource<EpisodesResponse> =
        viewModelScope.async {
            ResponseHandler.handleCommonResponse(animeStreamingRepository.getEpisodes(animeId))
        }.await()

    private suspend fun getDefaultEpisodeServers(defaultEpisodeId: String?): Resource<EpisodeServersResponse> =
        viewModelScope.async {
            defaultEpisodeId ?: return@async Resource.Error("No default episode found")
            ResponseHandler.handleCommonResponse(
                animeStreamingRepository.getEpisodeServers(
                    defaultEpisodeId
                )
            )
        }.await()

    private fun checkEpisodeSourceMalId(response: Resource<EpisodeSourcesResponse>): Boolean =
        animeDetail.value?.data?.data?.mal_id == response.data?.malID
}