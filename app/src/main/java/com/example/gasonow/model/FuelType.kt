package com.example.gasonow.model

enum class FuelType(val displayName: String) {
    GASOLINA_95("Gasolina 95 E5"),
    GASOLINA_98("Gasolina 98 E5"),
    GASOLEO_A("Diésel"),
    GASOLEO_PREMIUM("Diésel Premium"),
    GLP("GLP"),
    GNC("Gas Natural (GNC)")
}

enum class SortBy { PRICE, DISTANCE }

data class SearchFilters(
    val maxDistanceKm: Int = 10,
    val fuelType: FuelType = FuelType.GASOLINA_95,
    val onlyOpen: Boolean = true,
    val onlyWithPrice: Boolean = true,
    val sortBy: SortBy = SortBy.PRICE,
    val userLat: Double = 0.0,
    val userLon: Double = 0.0
)
