package com.example.gasonow

import android.app.Application
import com.example.gasonow.network.RetrofitClient

class GasoNowApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RetrofitClient.init(this)
    }
}
