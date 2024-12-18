package com.example.animeappkotlin.ui.providerfactories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.animeappkotlin.repository.AnimeRecommendationsRepository
import com.example.animeappkotlin.ui.viewmodels.AnimeRecommendationsViewModel

class AnimeRecommendationsViewModelProviderFactory(
    private val animeRecommendationsRepository: AnimeRecommendationsRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnimeRecommendationsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AnimeRecommendationsViewModel(animeRecommendationsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}