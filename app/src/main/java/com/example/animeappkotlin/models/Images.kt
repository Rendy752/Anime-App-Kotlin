package com.example.animeappkotlin.models

import kotlinx.serialization.Serializable

@Serializable
data class Images(
    val jpg: ImageUrl,
    val webp: ImageUrl
)
