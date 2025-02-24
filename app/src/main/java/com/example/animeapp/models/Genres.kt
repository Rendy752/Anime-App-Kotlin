package com.example.animeapp.models

import kotlinx.serialization.Serializable

@Serializable
data class Genres(
    val mal_id: Int,
    val name: String,
    val url: String,
    val count: Int
)