package com.example.gasonow.model

import com.google.gson.annotations.SerializedName

data class GasStationRaw(
    @SerializedName("IDEESS") val id: String = "",
    @SerializedName("Rótulo") val rotulo: String = "",
    @SerializedName("Dirección") val direccion: String = "",
    @SerializedName("Municipio") val municipio: String = "",
    @SerializedName("Provincia") val provincia: String = "",
    @SerializedName("C.P.") val codigoPostal: String = "",
    @SerializedName("Latitud") val latitud: String = "",
    @SerializedName("Longitud (WGS84)") val longitud: String = "",
    @SerializedName("Horario") val horario: String = "",
    @SerializedName("Precio Gasolina 95 E5") val precioGasolina95: String = "",
    @SerializedName("Precio Gasolina 98 E5") val precioGasolina98: String = "",
    @SerializedName("Precio Gasoleo A") val precioGasoleoA: String = "",
    @SerializedName("Precio Gasoleo B") val precioGasoleoB: String = "",
    @SerializedName("Precio Gasoleo Premium") val precioGasoleoPremium: String = "",
    @SerializedName("Precio Gas Natural Comprimido") val precioGNC: String = "",
    @SerializedName("Precio Gas Natural Licuado") val precioGNL: String = "",
    @SerializedName("Precio Gases licuados del petroleo") val precioGLP: String = "",
    @SerializedName("Precio Hidrogeno") val precioHidrogeno: String = "",
    @SerializedName("IDMunicipio") val idMunicipio: String = "",
    @SerializedName("IDProvincia") val idProvincia: String = "",
    @SerializedName("IDCCAA") val idCCAA: String = ""
) {
    fun toGasStation(): GasStation {
        val lat = latitud.replace(",", ".").toDoubleOrNull() ?: 0.0
        val lon = longitud.replace(",", ".").toDoubleOrNull() ?: 0.0

        fun parsePrice(raw: String): Double? =
            raw.trim().replace(",", ".").toDoubleOrNull()?.takeIf { it > 0 }

        val prices = mapOf(
            FuelType.GASOLINA_95 to parsePrice(precioGasolina95),
            FuelType.GASOLINA_98 to parsePrice(precioGasolina98),
            FuelType.GASOLEO_A to parsePrice(precioGasoleoA),
            FuelType.GASOLEO_PREMIUM to parsePrice(precioGasoleoPremium),
            FuelType.GLP to parsePrice(precioGLP),
            FuelType.GNC to parsePrice(precioGNC)
        ).filterValues { it != null }.mapValues { it.value!! }

        return GasStation(
            id = id,
            nombre = rotulo.trim().uppercase(),
            direccion = direccion.trim(),
            municipio = municipio.trim(),
            provincia = provincia.trim(),
            codigoPostal = codigoPostal.trim(),
            latitud = lat,
            longitud = lon,
            horario = horario.trim(),
            precios = prices
        )
    }
}
