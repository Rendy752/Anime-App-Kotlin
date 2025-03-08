package com.example.animeapp.models

import kotlinx.serialization.Serializable

@Serializable
data class AnimeAniwatchSearchResponse(
    val animes: List<AnimeAniwatch>
)

@Serializable
data class AnimeAniwatch(
    val id: String,
    val name: String,
    val img: String? = null,
    val episodes: EpisodeTypeNumber? = null,
    val duration: String? = null,
    val rated: Boolean? = null,
)

@Serializable
data class EpisodeTypeNumber(
    val eps: Int? = null,
    val sub: Int? = null,
    val dub: Int? = null
)