package com.example.gasonow.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gasonow.repository.GasStationRepository
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GasStationRepository(application)

    /** Llamar explícitamente para asegurar que el ViewModel se crea */
    fun warmUp() = Unit
}
