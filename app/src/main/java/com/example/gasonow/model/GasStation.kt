package com.example.gasonow.model

data class GasStation(
    val id: String,
    val nombre: String,
    val direccion: String,
    val municipio: String,
    val provincia: String,
    val codigoPostal: String,
    val latitud: Double,
    val longitud: Double,
    val horario: String,
    val precios: Map<FuelType, Double>,
    var distanciaKm: Double = 0.0,
    var estaAbierta: Boolean? = null  // null = unknown
) {
    fun getPrecio(fuelType: FuelType): Double? = precios[fuelType]

    fun getPrecioDisplay(fuelType: FuelType): String {
        val precio = getPrecio(fuelType) ?: return "—"
        return String.format("%.3f €/L", precio)
    }

    fun getDistanciaDisplay(): String = String.format("%.1f km", distanciaKm)

    fun getDireccionCompleta(): String = buildString {
        append(direccion)
        if (municipio.isNotBlank()) append(", $municipio")
        if (codigoPostal.isNotBlank()) append(" ($codigoPostal)")
    }
}
