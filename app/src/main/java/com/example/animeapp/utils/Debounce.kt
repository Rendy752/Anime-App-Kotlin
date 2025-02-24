package com.example.animeapp.utils

import com.example.animeapp.ui.animeSearch.AnimeSearchViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope

class Debounce(
    private val coroutineScope: CoroutineScope,
    private val delayMillis: Long = 1000L,
    private val onDebounced: (String) -> Unit,
    private val viewModel: AnimeSearchViewModel
) {

    private var searchJob: Job? = null

    fun query(text: String) {
        searchJob?.cancel()
        searchJob = coroutineScope.launch {
            delay(delayMillis)
            if (text != viewModel.queryState.value.query) {
                onDebounced(text)
            }
        }
    }
}