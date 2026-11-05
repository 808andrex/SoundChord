package com.soundchord.features.radio.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soundchord.features.radio.data.RadioBrowserRepository
import com.soundchord.features.radio.data.RadioRepository
import com.soundchord.features.radio.data.RadioStation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
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

class RadioViewModel(
    private val repository: RadioRepository = RadioBrowserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<RadioUiState>(RadioUiState.Loading)
    val uiState: StateFlow<RadioUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _countryCode = MutableStateFlow<String?>(null)
    val countryCode: StateFlow<String?> = _countryCode.asStateFlow()

    private var requestJob: Job? = null

    init {
        observeFilters()
    }

    fun fetchTopStations() {
        _searchQuery.value = ""
        _countryCode.value = null
    }

    fun fetchStationsByCountry(countryCode: String) {
        _countryCode.value = countryCode.trim().takeIf(String::isNotEmpty)
    }

    fun onCountryCodeChanged(countryCode: String?) {
        _countryCode.value = countryCode?.trim()?.takeIf(String::isNotEmpty)
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    @OptIn(FlowPreview::class)
    private fun observeFilters() {
        viewModelScope.launch {
            combine(
                _searchQuery.debounce(350).distinctUntilChanged(),
                _countryCode
            ) { query, country -> query to country }
                .collect { (query, country) -> loadStations(query, country) }
        }
    }

    private fun loadStations(query: String, country: String?) {
        requestJob?.cancel()
        requestJob = viewModelScope.launch {
            _uiState.value = RadioUiState.Loading
            try {
                val stations = repository.getStations(query, country, limit = 40)
                _uiState.value = RadioUiState.Success(stations)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _uiState.value = RadioUiState.Error(
                    error.localizedMessage ?: "No se pudieron cargar las emisoras"
                )
            }
        }
    }
}
