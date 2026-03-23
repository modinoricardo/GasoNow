package com.example.gasonow.repository

import android.content.Context
import android.location.Location
import com.example.gasonow.model.GasStation
import com.example.gasonow.model.GasStationRaw
import com.example.gasonow.model.SearchFilters
import com.example.gasonow.model.SortBy
import com.example.gasonow.network.RetrofitClient
import com.example.gasonow.utils.ProvinceHelper
import com.example.gasonow.utils.ScheduleHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GasStationRepository(private val context: Context) {

    private val api = RetrofitClient.apiService

    companion object {
        @Volatile private var cachedStations: List<GasStationRaw>? = null
        @Volatile private var cacheTimestamp: Long = 0
        @Volatile private var cachedProvinceId: Int = -1 // -1 = todas las provincias
        private const val CACHE_TTL_MS = 30 * 60 * 1000L

        private fun isCacheValid(provinceId: Int) =
            cachedStations != null &&
            cachedProvinceId == provinceId &&
            System.currentTimeMillis() - cacheTimestamp < CACHE_TTL_MS
    }

    suspend fun searchStations(
        filters: SearchFilters,
        onProgress: ((String) -> Unit)? = null
    ): List<GasStation> = withContext(Dispatchers.IO) {

        onProgress?.invoke("Localizando provincia…")
        val provinceId = ProvinceHelper.getProvinceId(context, filters.userLat, filters.userLon) ?: -1

        onProgress?.invoke("Descargando gasolineras…")
        val rawStations = if (isCacheValid(provinceId)) cachedStations!! else fetchAndCache(provinceId)

        onProgress?.invoke("Calculando distancias…")
        val mapped = rawStations.mapNotNull { raw ->
            val station = raw.toGasStation()
            if (station.latitud == 0.0 || station.longitud == 0.0) return@mapNotNull null
            station.estaAbierta = ScheduleHelper.isOpen(station.horario)
            station.distanciaKm = calculateDistance(
                filters.userLat, filters.userLon,
                station.latitud, station.longitud
            )
            station
        }

        onProgress?.invoke("Aplicando filtros…")
        mapped
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

    private suspend fun fetchAndCache(provinceId: Int): List<GasStationRaw> {
        val response = if (provinceId != -1) {
            api.getStationsByProvince(provinceId)
        } else {
            api.getAllStations()
        }
        return if (response.resultado == "OK") {
            val stations = response.stations.filter {
                it.latitud.isNotBlank() && it.longitud.isNotBlank()
            }
            cachedStations = stations
            cacheTimestamp = System.currentTimeMillis()
            cachedProvinceId = provinceId
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
