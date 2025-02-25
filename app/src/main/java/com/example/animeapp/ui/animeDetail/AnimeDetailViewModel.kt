package com.example.animeapp.ui.animeDetail

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.animeapp.models.AnimeDetailResponse
import com.example.animeapp.repository.AnimeDetailRepository
import com.example.animeapp.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class AnimeDetailViewModel @Inject constructor(
    private val animeDetailRepository: AnimeDetailRepository
) : ViewModel() {
    val animeDetail: MutableLiveData<Resource<AnimeDetailResponse>?> = MutableLiveData()

    fun getAnimeDetail(id: Int) = viewModelScope.launch {
        val cachedResponse = getCachedAnimeDetail(id)
        if (cachedResponse != null) {
            animeDetail.postValue(cachedResponse)
            return@launch
        }

        animeDetail.postValue(Resource.Loading())
        val response = animeDetailRepository.getAnimeDetail(id)
        animeDetail.postValue(handleAnimeDetailResponse(response))
    }

    private suspend fun handleAnimeDetailResponse(response: Response<AnimeDetailResponse>): Resource<AnimeDetailResponse> {
        if (response.isSuccessful) {
            response.body()?.let { resultResponse ->
                cacheAnimeDetail(resultResponse)
                return Resource.Success(resultResponse)
            }
        }
        return Resource.Error(response.message())
    }

    private suspend fun getCachedAnimeDetail(id: Int): Resource<AnimeDetailResponse>? {
        val cachedAnimeDetail = animeDetailRepository.getCachedAnimeDetail(id)
        return if (cachedAnimeDetail != null) {
            Resource.Success(cachedAnimeDetail)
        } else {
            null
        }
    }

    private suspend fun cacheAnimeDetail(animeDetailResponse: AnimeDetailResponse) {
        animeDetailRepository.cacheAnimeDetail(animeDetailResponse)
    }
}