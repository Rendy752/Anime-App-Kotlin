package com.example.animeapp.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.animeapp.data.local.dao.EpisodeDetailComplementDao
import com.example.animeapp.data.local.entities.EpisodeDetailComplementConverter
import com.example.animeapp.models.EpisodeDetailComplement

@Database(entities = [EpisodeDetailComplement::class], version = 2, exportSchema = false)
@TypeConverters(EpisodeDetailComplementConverter::class)
abstract class EpisodeDetailComplementDatabase : RoomDatabase() {

    abstract fun getEpisodeDetailComplementDao(): EpisodeDetailComplementDao

    companion object {
        @Volatile
        private var INSTANCE: EpisodeDetailComplementDatabase? = null

        fun getDatabase(context: Context): EpisodeDetailComplementDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EpisodeDetailComplementDatabase::class.java,
                    "episode_detail_complement.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}