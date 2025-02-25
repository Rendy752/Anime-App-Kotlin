package com.example.animeapp.models

import kotlinx.serialization.Serializable

@Serializable
data class ProducersResponse(
    val pagination: CompletePagination,
    val data: List<Producer>,
)

