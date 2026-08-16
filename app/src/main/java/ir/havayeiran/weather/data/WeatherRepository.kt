package ir.havayeiran.weather.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class WeatherRepository {

    suspend fun load(location: WeatherLocation): WeatherBundle = coroutineScope {
        val weatherDeferred = async(Dispatchers.IO) { fetchWeather(location) }
        val airDeferred = async(Dispatchers.IO) { runCatching { fetchAirQuality(location) }.getOrNull() }
        val marineDeferred = async(Dispatchers.IO) {
            if (isCaspianArea(location)) runCatching { fetchMarine(location) }.getOrNull() else null
        }

        val core = weatherDeferred.await()
        core.copy(
            airQuality = airDeferred.await(),
            marine = marineDeferred.await()
        )
    }

    suspend fun searchIranCities(query: String): List<CitySearchResult> = withContext(Dispatchers.IO) {
        if (query.trim().length < 2) return@withContext emptyList()
        val encoded = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8.toString())
        val url = "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=10&language=fa&format=json&countryCode=IR"
        val root = fetchJson(url)
        val results = root.optJSONArray("results") ?: return@withContext emptyList()
        buildList {
            for (i in 0 until results.length()) {
                val item = results.optJSONObject(i) ?: continue
                val name = item.optString("name").trim()
                val lat = item.optDouble("latitude", Double.NaN)
                val lon = item.optDouble("longitude", Double.NaN)
                if (name.isBlank() || lat.isNaN() || lon.isNaN()) continue
                add(
                    CitySearchResult(
                        name = name,
                        province = item.optString("admin1").ifBlank { item.optString("admin2") },
                        latitude = lat,
                        longitude = lon,
                        timezone = item.optString("timezone").ifBlank { "Asia/Tehran" }
                    )
                )
            }
        }.distinctBy { "${it.name}-${it.province}" }
    }

    private fun fetchWeather(location: WeatherLocation): WeatherBundle {
        val url = buildString {
            append("https://api.open-meteo.com/v1/forecast")
            append("?latitude=${location.latitude}")
            append("&longitude=${location.longitude}")
            append("&timezone=auto")
            append("&forecast_days=10")
            append("&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,weather_code,cloud_cover,pressure_msl,wind_speed_10m,wind_direction_10m,wind_gusts_10m,visibility")
            append("&hourly=temperature_2m,apparent_temperature,precipitation_probability,weather_code,wind_speed_10m,relative_humidity_2m")
            append("&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,precipitation_probability_max,precipitation_sum,wind_speed_10m_max,uv_index_max")
        }
        val root = fetchJson(url)
        val currentJson = root.getJSONObject("current")
        val hourlyJson = root.getJSONObject("hourly")
        val dailyJson = root.getJSONObject("daily")

        val current = CurrentWeather(
            time = currentJson.optString("time"),
            temperature = currentJson.optDouble("temperature_2m"),
            apparentTemperature = currentJson.optDouble("apparent_temperature"),
            humidity = currentJson.optInt("relative_humidity_2m"),
            weatherCode = currentJson.optInt("weather_code"),
            isDay = currentJson.optInt("is_day", 1) == 1,
            precipitation = currentJson.optDouble("precipitation"),
            cloudCover = currentJson.optInt("cloud_cover"),
            pressure = currentJson.optDouble("pressure_msl"),
            windSpeed = currentJson.optDouble("wind_speed_10m"),
            windDirection = currentJson.optInt("wind_direction_10m"),
            windGust = currentJson.optDouble("wind_gusts_10m"),
            visibilityKm = currentJson.optDouble("visibility") / 1000.0
        )

        val hTime = hourlyJson.getJSONArray("time")
        val hTemp = hourlyJson.getJSONArray("temperature_2m")
        val hFeels = hourlyJson.getJSONArray("apparent_temperature")
        val hRain = hourlyJson.getJSONArray("precipitation_probability")
        val hCode = hourlyJson.getJSONArray("weather_code")
        val hWind = hourlyJson.getJSONArray("wind_speed_10m")
        val hHumidity = hourlyJson.getJSONArray("relative_humidity_2m")
        val hourly = buildList {
            for (i in 0 until hTime.length()) {
                add(
                    HourlyWeather(
                        time = hTime.optString(i),
                        temperature = hTemp.safeDouble(i),
                        apparentTemperature = hFeels.safeDouble(i),
                        precipitationProbability = hRain.safeInt(i),
                        weatherCode = hCode.safeInt(i),
                        windSpeed = hWind.safeDouble(i),
                        humidity = hHumidity.safeInt(i)
                    )
                )
            }
        }

        val dTime = dailyJson.getJSONArray("time")
        val dCode = dailyJson.getJSONArray("weather_code")
        val dMax = dailyJson.getJSONArray("temperature_2m_max")
        val dMin = dailyJson.getJSONArray("temperature_2m_min")
        val dSunrise = dailyJson.getJSONArray("sunrise")
        val dSunset = dailyJson.getJSONArray("sunset")
        val dRain = dailyJson.getJSONArray("precipitation_probability_max")
        val dRainSum = dailyJson.getJSONArray("precipitation_sum")
        val dWind = dailyJson.getJSONArray("wind_speed_10m_max")
        val dUv = dailyJson.getJSONArray("uv_index_max")
        val daily = buildList {
            for (i in 0 until dTime.length()) {
                add(
                    DailyWeather(
                        date = dTime.optString(i),
                        weatherCode = dCode.safeInt(i),
                        maxTemperature = dMax.safeDouble(i),
                        minTemperature = dMin.safeDouble(i),
                        sunrise = dSunrise.optString(i),
                        sunset = dSunset.optString(i),
                        precipitationProbability = dRain.safeInt(i),
                        precipitationSum = dRainSum.safeDouble(i),
                        maxWindSpeed = dWind.safeDouble(i),
                        uvIndex = dUv.safeDouble(i)
                    )
                )
            }
        }

        return WeatherBundle(
            location = location.copy(timezone = root.optString("timezone").ifBlank { location.timezone }),
            current = current,
            hourly = hourly,
            daily = daily,
            airQuality = null,
            marine = null
        )
    }

    private fun fetchAirQuality(location: WeatherLocation): AirQuality? {
        val url = buildString {
            append("https://air-quality-api.open-meteo.com/v1/air-quality")
            append("?latitude=${location.latitude}")
            append("&longitude=${location.longitude}")
            append("&timezone=auto")
            append("&current=us_aqi,pm2_5,pm10,carbon_monoxide,nitrogen_dioxide,ozone")
        }
        val current = fetchJson(url).optJSONObject("current") ?: return null
        return AirQuality(
            usAqi = current.optInt("us_aqi"),
            pm25 = current.optDouble("pm2_5"),
            pm10 = current.optDouble("pm10"),
            carbonMonoxide = current.optDouble("carbon_monoxide"),
            nitrogenDioxide = current.optDouble("nitrogen_dioxide"),
            ozone = current.optDouble("ozone")
        )
    }

    private fun fetchMarine(location: WeatherLocation): MarineWeather? {
        val url = buildString {
            append("https://marine-api.open-meteo.com/v1/marine")
            append("?latitude=${location.latitude}")
            append("&longitude=${location.longitude}")
            append("&timezone=auto")
            append("&current=sea_surface_temperature,wave_height")
        }
        val current = fetchJson(url).optJSONObject("current") ?: return null
        val sea = current.optNullableDouble("sea_surface_temperature")
        val wave = current.optNullableDouble("wave_height")
        if (sea == null && wave == null) return null
        return MarineWeather(seaSurfaceTemperature = sea, waveHeight = wave)
    }

    private fun isCaspianArea(location: WeatherLocation): Boolean =
        location.latitude in 35.5..39.8 && location.longitude in 48.0..55.5

    private fun fetchJson(url: String): JSONObject {
        val connection = (URI(url).toURL().openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "HavayeIran-Android/1.0")
        }
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = BufferedReader(stream.reader()).use { it.readText() }
            if (code !in 200..299) error("خطای سرویس هواشناسی ($code)")
            return JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun JSONArray.safeDouble(index: Int): Double = when (val value = opt(index)) {
        null, JSONObject.NULL -> 0.0
        is Number -> value.toDouble()
        else -> value.toString().toDoubleOrNull() ?: 0.0
    }

    private fun JSONArray.safeInt(index: Int): Int = when (val value = opt(index)) {
        null, JSONObject.NULL -> 0
        is Number -> value.toInt()
        else -> value.toString().toIntOrNull() ?: 0
    }

    private fun JSONObject.optNullableDouble(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        return optDouble(key, Double.NaN).takeUnless { it.isNaN() }
    }
}
