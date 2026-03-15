package com.example.gasonow.repository

import android.location.Location
import com.example.gasonow.model.GasStation
import com.example.gasonow.model.GasStationRaw
import com.example.gasonow.model.SearchFilters
import com.example.gasonow.model.SortBy
import com.example.gasonow.network.RetrofitClient
import com.example.gasonow.utils.ScheduleHelper

class GasStationRepository {

    private val api = RetrofitClient.apiService

    companion object {
        @Volatile private var cachedStations: List<GasStationRaw>? = null
        @Volatile private var cacheTimestamp: Long = 0
        private const val CACHE_TTL_MS = 30 * 60 * 1000L // 30 minutos

        private fun isCacheValid() =
            cachedStations != null &&
            System.currentTimeMillis() - cacheTimestamp < CACHE_TTL_MS
    }

    /** Precarga los datos en background. Silencioso, no lanza excepciones al caller si ya hay caché. */
    suspend fun prefetch() {
        if (isCacheValid()) return
        fetchAndCache()
    }

    suspend fun searchStations(
        filters: SearchFilters,
        onProgress: ((String) -> Unit)? = null
    ): List<GasStation> {

        onProgress?.invoke("Descargando gasolineras…")
        val rawStations = if (isCacheValid()) cachedStations!! else fetchAndCache()

        onProgress?.invoke("Calculando distancias…")
        val mapped = rawStations.map { raw ->
            val station = raw.toGasStation()
            station.estaAbierta = ScheduleHelper.isOpen(station.horario)
            station.distanciaKm = calculateDistance(
                filters.userLat, filters.userLon,
                station.latitud, station.longitud
            )
            station
        }

        onProgress?.invoke("Aplicando filtros…")
        return mapped
            .filter { station ->
                station.distanciaKm <= filters.maxDistanceKm &&
                (!filters.onlyOpen || station.estaAbierta == true) &&
                (!filters.onlyWithPrice || station.getPrecio(filters.fuelType) != null)
            }
            .let { list ->
                when (filters.sortBy) {
                    SortBy.PRICE -> list.sortedWith(
                        compareBy<GasStation, Double?>(nullsLast()) { it.getPrecio(filters.fuelType) }
                            .thenBy { it.distanciaKm }
                    )
                    SortBy.DISTANCE -> list.sortedBy { it.distanciaKm }
                }
            }
    }

    private suspend fun fetchAndCache(): List<GasStationRaw> {
        val response = api.getAllStations()
        return if (response.resultado == "OK") {
            val stations = response.stations.filter {
                it.latitud.isNotBlank() && it.longitud.isNotBlank()
            }
            cachedStations = stations
            cacheTimestamp = System.currentTimeMillis()
            stations
        } else {
            emptyList()
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0] / 1000.0
    }
}
