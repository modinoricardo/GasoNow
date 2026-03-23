package com.example.gasonow.network

import android.content.Context
import okhttp3.Cache
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val BASE_URL =
        "https://sedeaplicaciones.minetur.gob.es/ServiciosRESTCarburantes/PreciosCarburantes/"

    // 30 minutos en segundos — igual que el TTL del caché en memoria
    private const val CACHE_MAX_AGE_SECONDS = 30 * 60

    private var httpCache: Cache? = null

    fun init(context: Context) {
        val cacheDir = File(context.cacheDir, "http_cache")
        httpCache = Cache(cacheDir, 30L * 1024L * 1024L) // 30 MB en disco
    }

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .cache(httpCache)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            // Fuerza el cacheo aunque el servidor no envíe Cache-Control
            .addNetworkInterceptor { chain ->
                chain.proceed(chain.request()).newBuilder()
                    .header("Cache-Control", "public, max-age=$CACHE_MAX_AGE_SECONDS")
                    .build()
            }
            .build()
    }

    val apiService: MineTurApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MineTurApiService::class.java)
    }
}
