package com.example.animeapp.models

import android.os.Parcelable
import androidx.room.Entity
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Entity(
    tableName = "anime_detail",
    primaryKeys = ["mal_id"]
)

@Parcelize
@Serializable
data class AnimeDetail(
    val mal_id: Int,
    val url: String,
    val images: Images,
    val trailer: Trailer,
    val approved: Boolean,
    val titles: List<Title>,
    val title: String,
    val title_english: String?,
    val title_japanese: String?,
    val title_synonyms: Array<String>?,
    val type: String?,
    val source: String,
    val episodes: Int,
    val status: String,
    val airing: Boolean,
    val aired: Aired,
    val duration: String,
    val rating: String?,
    val score: Double?,
    val scored_by: Int?,
    val rank: Int?,
    val popularity: Int,
    val members: Int,
    val favorites: Int,
    val synopsis: String?,
    val background: String?,
    val season: String?,
    val year: Int?,
    val broadcast: Broadcast,
    val producers: List<CommonIdentity>?,
    val licensors: List<CommonIdentity>?,
    val studios: List<CommonIdentity>?,
    val genres: List<CommonIdentity>?,
    val explicit_genres: List<CommonIdentity>?,
    val themes: List<CommonIdentity>?,
    val demographics: List<CommonIdentity>?,
    val relations: List<Relation>?,
    val theme: Theme,
    val external: List<NameAndUrl>?,
    val streaming: List<NameAndUrl>?
): Parcelable

