package com.example.animeapp.data.remote.api

import com.example.animeapp.utils.Const.Companion.BASE_URL
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RetrofitInstance @Inject constructor(okHttpClient: OkHttpClient) {

    private var retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: AnimeAPI by lazy { retrofit.create(AnimeAPI::class.java) }
}