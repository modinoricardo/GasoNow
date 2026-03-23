package com.example.gasonow.utils

import android.content.Context
import android.location.Geocoder
import java.util.Locale

object ProvinceHelper {

    fun getProvinceId(context: Context, lat: Double, lon: Double): Int? {
        return try {
            val geocoder = Geocoder(context, Locale("es", "ES"))
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            val adminArea = addresses?.firstOrNull()?.adminArea ?: return null
            PROVINCE_MAP[normalize(adminArea)]
        } catch (e: Exception) {
            null
        }
    }

    private fun normalize(name: String): String =
        name.lowercase(Locale.ROOT)
            .replace("á", "a").replace("é", "e").replace("í", "i")
            .replace("ó", "o").replace("ú", "u").replace("ü", "u")
            .replace("ñ", "n").trim()

    private val PROVINCE_MAP = mapOf(
        "alava" to 1, "araba" to 1,
        "albacete" to 2,
        "alicante" to 3, "alacant" to 3,
        "almeria" to 4,
        "avila" to 5,
        "badajoz" to 6,
        "illes balears" to 7, "islas baleares" to 7, "balears" to 7, "baleares" to 7,
        "barcelona" to 8,
        "burgos" to 9,
        "caceres" to 10,
        "cadiz" to 11,
        "castellon" to 12, "castello" to 12,
        "ciudad real" to 13,
        "cordoba" to 14,
        "a coruna" to 15, "la coruna" to 15,
        "cuenca" to 16,
        "girona" to 17, "gerona" to 17,
        "granada" to 18,
        "guadalajara" to 19,
        "gipuzkoa" to 20, "guipuzcoa" to 20,
        "huelva" to 21,
        "huesca" to 22,
        "jaen" to 23,
        "leon" to 24,
        "lleida" to 25, "lerida" to 25,
        "la rioja" to 26, "rioja" to 26,
        "lugo" to 27,
        "madrid" to 28, "comunidad de madrid" to 28,
        "malaga" to 29,
        "murcia" to 30, "region de murcia" to 30,
        "navarra" to 31, "nafarroa" to 31,
        "ourense" to 32, "orense" to 32,
        "asturias" to 33, "principado de asturias" to 33,
        "palencia" to 34,
        "las palmas" to 35,
        "pontevedra" to 36,
        "salamanca" to 37,
        "santa cruz de tenerife" to 38,
        "cantabria" to 39,
        "segovia" to 40,
        "sevilla" to 41,
        "soria" to 42,
        "tarragona" to 43,
        "teruel" to 44,
        "toledo" to 45,
        "valencia" to 46, "comunitat valenciana" to 46, "comunidad valenciana" to 46,
        "valladolid" to 47,
        "bizkaia" to 48, "vizcaya" to 48,
        "zamora" to 49,
        "zaragoza" to 50,
        "ceuta" to 51,
        "melilla" to 52
    )
}
