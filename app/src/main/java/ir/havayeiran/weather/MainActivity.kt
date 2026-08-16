package ir.havayeiran.weather

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import ir.havayeiran.weather.data.PreferencesStore
import ir.havayeiran.weather.data.WeatherRepository
import ir.havayeiran.weather.ui.HavayeIranTheme
import ir.havayeiran.weather.ui.WeatherScreen
import ir.havayeiran.weather.ui.WeatherViewModel
import ir.havayeiran.weather.ui.WeatherViewModelFactory

class MainActivity : ComponentActivity() {

    private val repository by lazy { WeatherRepository() }
    private val preferences by lazy { PreferencesStore(applicationContext) }
    private val viewModel: WeatherViewModel by viewModels {
        WeatherViewModelFactory(repository, preferences)
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) loadCurrentLocation()
        else toast("بدون اجازه موقعیت، می‌توانید شهر را از جستجو انتخاب کنید.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            HavayeIranTheme(darkTheme = state.darkMode) {
                WeatherScreen(
                    state = state,
                    isFavorite = viewModel.isFavorite(),
                    onSearchChange = viewModel::updateSearchQuery,
                    onClearSearch = viewModel::clearSearch,
                    onSearchResult = viewModel::selectSearchResult,
                    onSelectLocation = viewModel::selectLocation,
                    onRefresh = { viewModel.refresh() },
                    onLocate = ::requestLocation,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onToggleTheme = { viewModel.setDarkMode(!state.darkMode) }
                )
            }
        }
    }

    private fun requestLocation() {
        if (hasLocationPermission()) {
            loadCurrentLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    @Suppress("MissingPermission")
    private fun loadCurrentLocation() {
        if (!hasLocationPermission()) return
        val manager = getSystemService(LOCATION_SERVICE) as LocationManager
        val provider = when {
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            else -> null
        }
        if (provider == null) {
            useLastKnownLocation(manager)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            manager.getCurrentLocation(provider, null, mainExecutor) { location ->
                if (location != null) consumeLocation(location) else useLastKnownLocation(manager)
            }
        } else {
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    manager.removeUpdates(this)
                    consumeLocation(location)
                }
                override fun onProviderEnabled(provider: String) = Unit
                override fun onProviderDisabled(provider: String) = Unit
                @Deprecated("Deprecated in Android API")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            }
            runCatching {
                manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
            }.onFailure {
                useLastKnownLocation(manager)
            }
        }
    }

    @Suppress("MissingPermission")
    private fun useLastKnownLocation(manager: LocationManager) {
        val location = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }
        if (location != null) consumeLocation(location)
        else toast("موقعیت فعلی پیدا نشد؛ GPS را روشن کنید یا شهر را جستجو کنید.")
    }

    private fun consumeLocation(location: Location) {
        if (!insideIran(location.latitude, location.longitude)) {
            toast("این برنامه برای شهرها و موقعیت‌های داخل ایران تنظیم شده است.")
            return
        }
        viewModel.useCurrentCoordinates(location.latitude, location.longitude)
    }

    private fun hasLocationPermission(): Boolean =
        checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun insideIran(latitude: Double, longitude: Double): Boolean =
        latitude in 24.0..40.0 && longitude in 44.0..64.0

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
