package com.moontir.app.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private var currentBaseUrl: String? = null
    private var currentService: MoontirApiService? = null

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    fun getService(baseUrl: String): MoontirApiService {
        val sanitized = if (!baseUrl.endsWith("/")) "$baseUrl/" else baseUrl
        if (currentService == null || currentBaseUrl != sanitized) {
            currentBaseUrl = sanitized
            val retrofit = Retrofit.Builder()
                .baseUrl(sanitized)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            currentService = retrofit.create(MoontirApiService::class.java)
        }
        return currentService!!
    }
}
