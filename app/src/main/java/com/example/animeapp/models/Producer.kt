package com.example.animeapp.models

import kotlinx.serialization.Serializable

@Serializable
data class Producer(
    val mal_id: Int,
    val url: String?,
    val titles: List<Title>?,
    val images: ProducerImage?,
    val favorites: Int,
    val established: String?,
    val about: String?,
    val count: Int
)

@Serializable
data class ProducerImage(
    val jpg: JpgImage?
)
@Serializable
data class JpgImage(
    val image_url: String?
)