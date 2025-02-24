package com.example.animeapp.modules

import android.content.Context
import com.example.animeapp.data.local.dao.AnimeDetailDao
import com.example.animeapp.data.local.database.AnimeDetailDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAnimeDetailDatabase(@ApplicationContext context: Context): AnimeDetailDatabase {
        return AnimeDetailDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideAnimeDetailDao(database: AnimeDetailDatabase): AnimeDetailDao {
        return database.getAnimeDetailDao()
    }
}