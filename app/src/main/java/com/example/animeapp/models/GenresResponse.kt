package com.example.animeapp.models

import kotlinx.serialization.Serializable

@Serializable
data class GenresResponse(
    val data: List<Genre>,
)

