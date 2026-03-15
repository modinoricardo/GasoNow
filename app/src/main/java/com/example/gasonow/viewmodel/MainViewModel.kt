package com.example.gasonow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gasonow.repository.GasStationRepository
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val repository = GasStationRepository()

    init {
        viewModelScope.launch {
            try {
                repository.prefetch()
            } catch (e: Exception) {
                // Silently ignore — si falla, la búsqueda lo reintentará
            }
        }
    }
}
