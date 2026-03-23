package com.example.gasonow.network

import com.example.gasonow.model.ApiResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface MineTurApiService {

    @GET("EstacionesTerrestres/")
    suspend fun getAllStations(): ApiResponse

    @GET("EstacionesTerrestres/FiltroProvincia/{provinciaId}")
    suspend fun getStationsByProvince(@Path("provinciaId") provinciaId: Int): ApiResponse
}
