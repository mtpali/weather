package ir.havayeiran.weather.ui

import androidx.compose.runtime.Composable
import ir.havayeiran.weather.data.CitySearchResult

@Composable
fun GoogleWeatherScreenV6(
    state: WeatherUiState,
    onSearchChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSearchResult: (CitySearchResult) -> Unit,
    onRefresh: () -> Unit,
    onLocate: () -> Unit,
    onToggleTheme: () -> Unit
) = GoogleWeatherScreenV7(
    state = state,
    onSearchChange = onSearchChange,
    onClearSearch = onClearSearch,
    onSearchResult = onSearchResult,
    onRefresh = onRefresh,
    onLocate = onLocate,
    onToggleTheme = onToggleTheme
)
