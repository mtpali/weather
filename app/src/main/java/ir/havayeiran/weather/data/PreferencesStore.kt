package ir.havayeiran.weather.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class PreferencesStore(context: Context) {
    private val prefs = context.getSharedPreferences("havaye_iran", Context.MODE_PRIVATE)

    fun loadLocation(): WeatherLocation {
        val raw = prefs.getString(KEY_LOCATION, null) ?: return DefaultRamsar
        return runCatching {
            val json = JSONObject(raw)
            WeatherLocation(
                name = json.getString("name"),
                province = json.optString("province"),
                latitude = json.getDouble("latitude"),
                longitude = json.getDouble("longitude"),
                timezone = json.optString("timezone").ifBlank { "Asia/Tehran" }
            )
        }.getOrDefault(DefaultRamsar)
    }

    fun saveLocation(location: WeatherLocation) {
        val json = JSONObject()
            .put("name", location.name)
            .put("province", location.province)
            .put("latitude", location.latitude)
            .put("longitude", location.longitude)
            .put("timezone", location.timezone)
        prefs.edit().putString(KEY_LOCATION, json.toString()).apply()
    }

    fun loadFavorites(): List<WeatherLocation> {
        val raw = prefs.getString(KEY_FAVORITES, null) ?: return listOf(DefaultRamsar)
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val json = array.optJSONObject(i) ?: continue
                    add(
                        WeatherLocation(
                            name = json.optString("name"),
                            province = json.optString("province"),
                            latitude = json.optDouble("latitude"),
                            longitude = json.optDouble("longitude"),
                            timezone = json.optString("timezone").ifBlank { "Asia/Tehran" }
                        )
                    )
                }
            }.ifEmpty { listOf(DefaultRamsar) }
        }.getOrDefault(listOf(DefaultRamsar))
    }

    fun saveFavorites(items: List<WeatherLocation>) {
        val array = JSONArray()
        items.forEach { location ->
            array.put(
                JSONObject()
                    .put("name", location.name)
                    .put("province", location.province)
                    .put("latitude", location.latitude)
                    .put("longitude", location.longitude)
                    .put("timezone", location.timezone)
            )
        }
        prefs.edit().putString(KEY_FAVORITES, array.toString()).apply()
    }

    fun loadDarkMode(): Boolean = prefs.getBoolean(KEY_DARK_MODE, true)

    fun saveDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
    }

    companion object {
        private const val KEY_LOCATION = "location"
        private const val KEY_FAVORITES = "favorites"
        private const val KEY_DARK_MODE = "dark_mode"
    }
}
