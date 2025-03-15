package com.example.animeapp.models

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.time.Instant

@Entity(
    tableName = "anime_detail_complement",
    primaryKeys = ["id"]
)

@Parcelize
@Serializable
data class AnimeDetailComplement(
    @ColumnInfo(name = "id") private var _id: String,
    val malId: Int,
    val isFavorite: Boolean = false,
    val eps: Int? = null,
    val sub: Int? = null,
    val dub: Int? = null,
    val lastEpisodeWatched: Int? = null,
    val episodes: List<Episode>,
    val lastEpisodeUpdatedAt: Long = Instant.now().epochSecond,
    val createdAt: Long = Instant.now().epochSecond,
    var updatedAt: Long = Instant.now().epochSecond
) : Parcelable {
    var id: String
        get() = _id
        set(value) {
            _id = value.substringBefore("?").trim()
        }
}