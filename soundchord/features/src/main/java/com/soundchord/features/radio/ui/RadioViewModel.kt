package com.soundchord.features.radio.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soundchord.features.radio.data.RadioBrowserApi
import com.soundchord.features.radio.data.RadioStation
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

sealed interface RadioUiState {
    data object Loading : RadioUiState
    data class Success(val stations: List<RadioStation>) : RadioUiState
    data class Error(val message: String) : RadioUiState
}

class RadioViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<RadioUiState>(RadioUiState.Loading)
    val uiState: StateFlow<RadioUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        // Carga inicial: Emisoras más populares
        fetchTopStations()
        observeSearchQuery()
    }

    fun fetchTopStations() {
        viewModelScope.launch {
            _uiState.value = RadioUiState.Loading
            runCatching {
                RadioBrowserApi.getTopStations(limit = 40)
            }.onSuccess { stations ->
                _uiState.value = RadioUiState.Success(stations)
            }.onFailure { error ->
                _uiState.value = RadioUiState.Error(error.localizedMessage ?: "Error de conexión")
            }
        }
    }

    fun fetchStationsByCountry(countryCode: String) {
        viewModelScope.launch {
            _uiState.value = RadioUiState.Loading
            runCatching {
                RadioBrowserApi.getStationsByCountry(countryCode = countryCode, limit = 40)
            }.onSuccess { stations ->
                _uiState.value = RadioUiState.Success(stations)
            }.onFailure { error ->
                _uiState.value = RadioUiState.Error(error.localizedMessage ?: "Error al cargar país")
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    @OptIn(FlowPreview::class)
    private fun observeSearchQuery() {
        viewModelScope.launch {
            _searchQuery
                .debounce(350)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.isNotBlank()) {
                        _uiState.value = RadioUiState.Loading
                        runCatching {
                            RadioBrowserApi.searchStations(query.trim(), limit = 40)
                        }.onSuccess { stations ->
                            _uiState.value = RadioUiState.Success(stations)
                        }.onFailure { error ->
                            _uiState.value = RadioUiState.Error(error.localizedMessage ?: "Error en la búsqueda")
                        }
                    } else if (_uiState.value !is RadioUiState.Success) {
                        fetchTopStations()
                    }
                }
        }
    }
}