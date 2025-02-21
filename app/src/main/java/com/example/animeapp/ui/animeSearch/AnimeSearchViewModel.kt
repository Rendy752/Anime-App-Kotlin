package com.example.animeapp.ui.animeSearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.animeapp.models.AnimeDetailResponse
import com.example.animeapp.models.AnimeFilterState
import com.example.animeapp.models.AnimeSearchQueryState
import com.example.animeapp.models.AnimeSearchResponse
import com.example.animeapp.models.CompletePagination
import com.example.animeapp.models.Items
import com.example.animeapp.repository.AnimeSearchRepository
import com.example.animeapp.utils.Limit
import com.example.animeapp.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Response

class AnimeSearchViewModel(
    private val animeSearchRepository: AnimeSearchRepository
) : ViewModel() {

    private val _animeSearchResults =
        MutableStateFlow<Resource<AnimeSearchResponse>>(Resource.Loading())
    val animeSearchResults: StateFlow<Resource<AnimeSearchResponse>> =
        _animeSearchResults.asStateFlow()

    private val _queryState = MutableStateFlow(AnimeSearchQueryState())
    val queryState: StateFlow<AnimeSearchQueryState> = _queryState.asStateFlow()

    private val _filterState = MutableStateFlow(AnimeFilterState())
    val filterState: StateFlow<AnimeFilterState> = _filterState.asStateFlow()

    fun updateQuery(query: String) {
        _queryState.value = queryState.value.copy(query = query, page = 1)
        searchAnime()
    }

    fun updatePage(page: Int) {
        _queryState.value = queryState.value.copy(page = page)
        searchAnime()
    }

    fun updateLimit(limit: Int?) {
        if (_queryState.value.limit != limit) {
            _queryState.value = queryState.value.copy(limit = limit, page = 1)
            searchAnime()
        }
    }

    init {
        getRandomAnime()
    }

    fun searchAnime() = viewModelScope.launch {
        if (queryState.value.query.isBlank()) {
            getRandomAnime()
        } else {
            _animeSearchResults.value = Resource.Loading()
            val response = animeSearchRepository.searchAnime(
                queryState.value.query,
                queryState.value.page,
                queryState.value.limit ?: Limit.DEFAULT_LIMIT
            )
            _animeSearchResults.value = handleAnimeSearchResponse(response)
        }
    }

    fun applyFilters(filters: Map<String, Any?>) {
        _filterState.value = AnimeFilterState(
            type = filters["type"] as? String,
            score = filters["score"] as? Double,
            minScore = filters["minScore"] as? Double,
            maxScore = filters["maxScore"] as? Double,
            status = filters["status"] as? String,
            rating = filters["rating"] as? String,
            sfw = filters["sfw"] as? Boolean,
            unapproved = filters["unapproved"] as? Boolean,
            genres = filters["genres"] as? String,
            genresExclude = filters["genresExclude"] as? String,
            orderBy = filters["orderBy"] as? String,
            sort = filters["sort"] as? String,
            letter = filters["letter"] as? String,
            producers = filters["producers"] as? String,
            startDate = filters["startDate"] as? String,
            endDate = filters["endDate"] as? String
        )
        searchAnimeWithFilters()
    }

    private fun searchAnimeWithFilters() = viewModelScope.launch {
        _animeSearchResults.value = Resource.Loading()
        val response = animeSearchRepository.searchAnime(
            query = queryState.value.query,
            page = queryState.value.page,
            limit = queryState.value.limit?: Limit.DEFAULT_LIMIT,
            type = filterState.value.type,
            score = filterState.value.score,
            minScore = filterState.value.minScore,
            maxScore = filterState.value.maxScore,
            status = filterState.value.status,
            rating = filterState.value.rating,
            sfw = filterState.value.sfw,
            unapproved = filterState.value.unapproved,
            genres = filterState.value.genres,
            genresExclude = filterState.value.genresExclude,
            orderBy = filterState.value.orderBy,
            sort = filterState.value.sort,
            letter = filterState.value.letter,
            producers = filterState.value.producers,
            startDate = filterState.value.startDate,
            endDate = filterState.value.endDate
        )
        _animeSearchResults.value = handleAnimeSearchResponse(response)
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
                    pagination = CompletePagination(
                        last_visible_page = 1,
                        has_next_page = false,
                        current_page = 1,
                        items = Items(
                            count = 1,
                            total = 1,
                            per_page = 1
                        )
                    )
                )
                return Resource.Success(searchResponse)
            } ?: return Resource.Error("Response body is null")
        } else {
            return Resource.Error(response.message())
        }
    }
}