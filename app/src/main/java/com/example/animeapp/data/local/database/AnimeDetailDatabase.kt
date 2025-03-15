package com.example.animeapp.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.animeapp.data.local.dao.AnimeDetailDao
import com.example.animeapp.data.local.entities.AnimeDetailConverter
import com.example.animeapp.models.AnimeDetail

@Database(entities = [AnimeDetail::class], version = 15, exportSchema = false)
@TypeConverters(AnimeDetailConverter::class)
abstract class AnimeDetailDatabase : RoomDatabase() {

    abstract fun getAnimeDetailDao(): AnimeDetailDao

    companion object {
        @Volatile
        private var INSTANCE: AnimeDetailDatabase? = null

        fun getDatabase(context: Context): AnimeDetailDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AnimeDetailDatabase::class.java,
                    "anime_detail.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}