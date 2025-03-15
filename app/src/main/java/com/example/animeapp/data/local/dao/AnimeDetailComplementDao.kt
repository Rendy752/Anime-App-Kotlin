package com.example.animeapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Update
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.animeapp.models.AnimeDetailComplement
import java.time.Instant

@Dao
interface AnimeDetailComplementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnimeDetailComplement(animeDetailComplement: AnimeDetailComplement)

    @Query("SELECT * FROM anime_detail_complement WHERE id = :id")
    fun getAnimeDetailComplementById(id: String): AnimeDetailComplement?

    @Query("SELECT * FROM anime_detail_complement WHERE malId = :malId")
    fun getAnimeDetailComplementByMalId(malId: Int): AnimeDetailComplement?

    @Delete
    suspend fun deleteAnimeDetailComplement(animeDetailComplement: AnimeDetailComplement)

    @Update
    suspend fun updateAnimeDetailComplement(animeDetail: AnimeDetailComplement)

    @Update
    suspend fun updateEpisodeAnimeDetailComplement(animeDetail: AnimeDetailComplement) {
        val updatedAnime = animeDetail.copy(
            lastEpisodeUpdatedAt = Instant.now().epochSecond,
            updatedAt = Instant.now().epochSecond
        )
        updateAnimeDetailComplement(updatedAnime)
    }
}