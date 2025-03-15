package com.example.animeapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Update
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.animeapp.models.EpisodeDetailComplement
import java.time.Instant

@Dao
interface EpisodeDetailComplementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisodeDetailComplement(episodeDetailComplement: EpisodeDetailComplement)

    @Query("SELECT * FROM episode_detail_complement WHERE id = :id")
    fun getEpisodeDetailComplementById(id: String): EpisodeDetailComplement?

    @Delete
    suspend fun deleteEpisodeDetailComplement(episodeDetailComplement: EpisodeDetailComplement)

    @Update
    suspend fun updateEpisodeDetailComplement(episodeDetailComplement: EpisodeDetailComplement) {
        val updatedEpisode = episodeDetailComplement.copy(updatedAt = Instant.now().epochSecond)
        updateEpisodeDetailComplementWithoutUpdateTimestamp(updatedEpisode)
    }

    @Update
    suspend fun updateEpisodeDetailComplementWithoutUpdateTimestamp(episodeDetailComplement: EpisodeDetailComplement)
}