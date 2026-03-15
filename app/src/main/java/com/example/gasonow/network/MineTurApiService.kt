package com.example.gasonow.network

import com.example.gasonow.model.ApiResponse
import retrofit2.http.GET

interface MineTurApiService {
    @GET("EstacionesTerrestres/")
    suspend fun getAllStations(): ApiResponse
}
