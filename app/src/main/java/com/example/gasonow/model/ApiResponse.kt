package com.example.gasonow.model

import com.google.gson.annotations.SerializedName

data class ApiResponse(
    @SerializedName("Fecha") val fecha: String = "",
    @SerializedName("ListaEESSPrecio") val stations: List<GasStationRaw> = emptyList(),
    @SerializedName("ResultadoConsulta") val resultado: String = ""
)
