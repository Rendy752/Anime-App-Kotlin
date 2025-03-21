package com.example.animeapp.models

import kotlinx.serialization.Serializable

@Serializable
data class Genre(
    val mal_id: Int,
    val name: String,
    val url: String,
    val count: Int
)