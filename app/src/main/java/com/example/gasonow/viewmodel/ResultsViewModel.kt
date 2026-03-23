package com.example.gasonow.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.gasonow.model.GasStation
import com.example.gasonow.model.SearchFilters
import com.example.gasonow.model.SortBy
import com.example.gasonow.repository.GasStationRepository
import kotlinx.coroutines.launch

sealed class ResultsUiState {
    data class Loading(val message: String = "Buscando gasolineras…") : ResultsUiState()
    data class Success(val stations: List<GasStation>, val totalCount: Int) : ResultsUiState()
    data class Error(val message: String) : ResultsUiState()
    object Empty : ResultsUiState()
}

class ResultsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GasStationRepository(application)

    private val _uiState = MutableLiveData<ResultsUiState>(ResultsUiState.Loading())
    val uiState: LiveData<ResultsUiState> = _uiState

    private var currentFilters: SearchFilters? = null
    private var allStations: List<GasStation> = emptyList()

    fun loadStations(filters: SearchFilters) {
        currentFilters = filters
        _uiState.value = ResultsUiState.Loading("Buscando gasolineras…")
        viewModelScope.launch {
            try {
                val stations = repository.searchStations(filters) { message ->
                    _uiState.postValue(ResultsUiState.Loading(message))
                }
                allStations = stations
                postResult(stations)
            } catch (e: Exception) {
                _uiState.postValue(ResultsUiState.Error(e.message ?: "Error desconocido"))
            }
        }
    }

    fun changeSortOrder(sortBy: SortBy) {
        val filters = currentFilters ?: return
        currentFilters = filters.copy(sortBy = sortBy)
        val sorted = when (sortBy) {
            SortBy.PRICE -> allStations.sortedWith(
                compareBy<GasStation, Double?>(nullsLast()) { it.getPrecio(filters.fuelType) }
                    .thenBy { it.distanciaKm }
            )
            SortBy.DISTANCE -> allStations.sortedBy { it.distanciaKm }
        }
        postResult(sorted)
    }

    fun getCurrentFilters(): SearchFilters? = currentFilters

    private fun postResult(stations: List<GasStation>) {
        if (stations.isEmpty()) {
            _uiState.postValue(ResultsUiState.Empty)
        } else {
            _uiState.postValue(ResultsUiState.Success(stations, stations.size))
        }
    }
}
