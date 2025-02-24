package com.example.animeapp.modules

import com.example.animeapp.data.local.dao.AnimeDetailDao
import com.example.animeapp.data.remote.api.AnimeAPI
import com.example.animeapp.repository.AnimeDetailRepository
import com.example.animeapp.repository.AnimeRecommendationsRepository
import com.example.animeapp.repository.AnimeSearchRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
object RepositoryModule {

    @Provides
    fun provideAnimeDetailRepository(
        animeDetailDao: AnimeDetailDao,
        animeAPI: AnimeAPI
    ): AnimeDetailRepository {
        return AnimeDetailRepository(animeDetailDao, animeAPI)
    }

    @Provides
    fun provideAnimeRecommendationsRepository(animeAPI: AnimeAPI): AnimeRecommendationsRepository {
        return AnimeRecommendationsRepository(animeAPI)
    }

    @Provides
    fun provideAnimeSearchRepository(animeAPI: AnimeAPI): AnimeSearchRepository {
        return AnimeSearchRepository(animeAPI)
    }
}