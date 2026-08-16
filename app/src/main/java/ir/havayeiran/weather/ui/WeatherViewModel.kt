package ir.havayeiran.weather.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ir.havayeiran.weather.data.CitySearchResult
import ir.havayeiran.weather.data.PreferencesStore
import ir.havayeiran.weather.data.WeatherBundle
import ir.havayeiran.weather.data.WeatherLocation
import ir.havayeiran.weather.data.WeatherRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class WeatherUiState(
    val selectedLocation: WeatherLocation,
    val weather: WeatherBundle? = null,
    val favorites: List<WeatherLocation> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<CitySearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val darkMode: Boolean = true,
    val errorMessage: String? = null
)

class WeatherViewModel(
    private val repository: WeatherRepository,
    private val preferences: PreferencesStore
) : ViewModel() {

    private val initialLocation = preferences.loadLocation()
    private val _uiState = MutableStateFlow(
        WeatherUiState(
            selectedLocation = initialLocation,
            favorites = preferences.loadFavorites(),
            darkMode = preferences.loadDarkMode()
        )
    )
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var loadJob: Job? = null

    init {
        refresh(initial = true)
    }

    fun refresh(initial: Boolean = false) {
        val location = _uiState.value.selectedLocation
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = initial || it.weather == null,
                    isRefreshing = !initial && it.weather != null,
                    errorMessage = null
                )
            }
            runCatching { repository.load(location) }
                .onSuccess { bundle ->
                    _uiState.update {
                        it.copy(
                            selectedLocation = bundle.location,
                            weather = bundle,
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = null
                        )
                    }
                    preferences.saveLocation(bundle.location)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = error.message ?: "دریافت اطلاعات هواشناسی ناموفق بود."
                        )
                    }
                }
        }
    }

    fun selectLocation(location: WeatherLocation) {
        if (sameLocation(location, _uiState.value.selectedLocation) && _uiState.value.weather != null) return
        preferences.saveLocation(location)
        _uiState.update {
            it.copy(
                selectedLocation = location,
                searchQuery = "",
                searchResults = emptyList(),
                errorMessage = null
            )
        }
        refresh(initial = _uiState.value.weather == null)
    }

    fun useCurrentCoordinates(latitude: Double, longitude: Double) {
        selectLocation(
            WeatherLocation(
                name = "موقعیت من",
                province = "ایران",
                latitude = latitude,
                longitude = longitude
            )
        )
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        if (query.trim().length < 2) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(350)
            _uiState.update { it.copy(isSearching = true) }
            runCatching { repository.searchIranCities(query) }
                .onSuccess { results ->
                    if (_uiState.value.searchQuery == query) {
                        _uiState.update { it.copy(searchResults = results, isSearching = false) }
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
                }
        }
    }

    fun selectSearchResult(result: CitySearchResult) {
        selectLocation(result.toLocation())
    }

    fun clearSearch() {
        searchJob?.cancel()
        _uiState.update { it.copy(searchQuery = "", searchResults = emptyList(), isSearching = false) }
    }

    fun toggleFavorite() {
        val selected = _uiState.value.selectedLocation
        val current = _uiState.value.favorites
        val updated = if (current.any { sameLocation(it, selected) }) {
            current.filterNot { sameLocation(it, selected) }
        } else {
            (current + selected).takeLast(8)
        }
        preferences.saveFavorites(updated)
        _uiState.update { it.copy(favorites = updated) }
    }

    fun setDarkMode(enabled: Boolean) {
        preferences.saveDarkMode(enabled)
        _uiState.update { it.copy(darkMode = enabled) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun isFavorite(location: WeatherLocation = _uiState.value.selectedLocation): Boolean =
        _uiState.value.favorites.any { sameLocation(it, location) }

    private fun sameLocation(a: WeatherLocation, b: WeatherLocation): Boolean =
        kotlin.math.abs(a.latitude - b.latitude) < 0.01 && kotlin.math.abs(a.longitude - b.longitude) < 0.01
}

class WeatherViewModelFactory(
    private val repository: WeatherRepository,
    private val preferences: PreferencesStore
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WeatherViewModel::class.java)) {
            return WeatherViewModel(repository, preferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
