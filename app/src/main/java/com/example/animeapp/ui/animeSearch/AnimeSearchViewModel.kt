package com.example.animeapp.ui.animeSearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.animeapp.models.AnimeDetailResponse
import com.example.animeapp.models.AnimeSearchQueryState
import com.example.animeapp.models.AnimeSearchResponse
import com.example.animeapp.models.CompletePagination
import com.example.animeapp.models.GenresResponse
import com.example.animeapp.repository.AnimeSearchRepository
import com.example.animeapp.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class AnimeSearchViewModel @Inject constructor(
    private val animeSearchRepository: AnimeSearchRepository
) : ViewModel() {

    private val _animeSearchResults =
        MutableStateFlow<Resource<AnimeSearchResponse>>(Resource.Loading())
    val animeSearchResults: StateFlow<Resource<AnimeSearchResponse>> =
        _animeSearchResults.asStateFlow()

    private val _queryState = MutableStateFlow(AnimeSearchQueryState())
    val queryState: StateFlow<AnimeSearchQueryState> = _queryState.asStateFlow()

    private val _genres = MutableStateFlow<Resource<GenresResponse>>(Resource.Loading())
    val genres: StateFlow<Resource<GenresResponse>> = _genres.asStateFlow()

    init {
        getRandomAnime()
        fetchGenres()
    }

    private fun searchAnime() = viewModelScope.launch {
        if (queryState.value.isDefault()) {
            getRandomAnime()
        } else {
            _animeSearchResults.value = Resource.Loading()
            val response = animeSearchRepository.searchAnime(queryState.value)
            _animeSearchResults.value = handleAnimeSearchResponse(response)
        }
    }

    fun applyFilters(updatedQueryState: AnimeSearchQueryState) {
        _queryState.value = updatedQueryState
        searchAnime()
    }

    private fun getRandomAnime() = viewModelScope.launch {
        _animeSearchResults.value = Resource.Loading()
        val response = animeSearchRepository.getRandomAnime()
        _animeSearchResults.value = handleAnimeRandomResponse(response)
    }

    private fun handleAnimeSearchResponse(response: Response<AnimeSearchResponse>): Resource<AnimeSearchResponse> {
        return if (response.isSuccessful) {
            response.body()?.let { resultResponse ->
                Resource.Success(resultResponse)
            } ?: Resource.Error("Response body is null")
        } else {
            Resource.Error(response.message())
        }
    }

    private fun handleAnimeRandomResponse(response: Response<AnimeDetailResponse>): Resource<AnimeSearchResponse> {
        if (response.isSuccessful) {
            response.body()?.let { resultResponse ->
                val searchResponse = AnimeSearchResponse(
                    data = listOf(resultResponse.data),
                    pagination = CompletePagination.default()
                )
                return Resource.Success(searchResponse)
            } ?: return Resource.Error("Response body is null")
        } else {
            return Resource.Error(response.message())
        }
    }

    fun fetchGenres() = viewModelScope.launch {
        _genres.value = Resource.Loading()
        val response = animeSearchRepository.getGenres()
        _genres.value = handleGenresResponse(response)
    }

    private fun handleGenresResponse(response: Response<GenresResponse>): Resource<GenresResponse> {
        if (response.isSuccessful) {
            response.body()?.let { resultResponse ->
                return Resource.Success(resultResponse)
            }
        }
        return Resource.Error(response.message())
    }

    fun resetBottomSheetFilters() {
        _queryState.value = queryState.value.resetBottomSheetFilters()
        searchAnime()
    }
}