package ir.havayeiran.weather.data

data class WeatherLocation(
    val name: String,
    val province: String = "",
    val latitude: Double,
    val longitude: Double,
    val timezone: String = "Asia/Tehran"
) {
    val subtitle: String get() = if (province.isBlank()) "ایران" else province
}

data class CurrentWeather(
    val time: String,
    val temperature: Double,
    val apparentTemperature: Double,
    val humidity: Int,
    val weatherCode: Int,
    val isDay: Boolean,
    val precipitation: Double,
    val cloudCover: Int,
    val pressure: Double,
    val windSpeed: Double,
    val windDirection: Int,
    val windGust: Double,
    val visibilityKm: Double
)

data class HourlyWeather(
    val time: String,
    val temperature: Double,
    val apparentTemperature: Double,
    val precipitationProbability: Int,
    val weatherCode: Int,
    val windSpeed: Double,
    val humidity: Int
)

data class DailyWeather(
    val date: String,
    val weatherCode: Int,
    val maxTemperature: Double,
    val minTemperature: Double,
    val sunrise: String,
    val sunset: String,
    val precipitationProbability: Int,
    val precipitationSum: Double,
    val maxWindSpeed: Double,
    val uvIndex: Double
)

data class AirQuality(
    val usAqi: Int,
    val pm25: Double,
    val pm10: Double,
    val carbonMonoxide: Double,
    val nitrogenDioxide: Double,
    val ozone: Double
)

data class MarineWeather(
    val seaSurfaceTemperature: Double?,
    val waveHeight: Double?
)

data class WeatherBundle(
    val location: WeatherLocation,
    val current: CurrentWeather,
    val hourly: List<HourlyWeather>,
    val daily: List<DailyWeather>,
    val airQuality: AirQuality?,
    val marine: MarineWeather?
)

data class CitySearchResult(
    val name: String,
    val province: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: String
) {
    fun toLocation() = WeatherLocation(
        name = name,
        province = province,
        latitude = latitude,
        longitude = longitude,
        timezone = timezone
    )
}

enum class WeatherKind { CLEAR, PARTLY_CLOUDY, CLOUDY, FOG, RAIN, SNOW, STORM }

fun weatherKind(code: Int): WeatherKind = when (code) {
    0, 1 -> WeatherKind.CLEAR
    2 -> WeatherKind.PARTLY_CLOUDY
    3 -> WeatherKind.CLOUDY
    45, 48 -> WeatherKind.FOG
    51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> WeatherKind.RAIN
    71, 73, 75, 77, 85, 86 -> WeatherKind.SNOW
    95, 96, 99 -> WeatherKind.STORM
    else -> WeatherKind.CLOUDY
}

fun weatherDescription(code: Int): String = when (code) {
    0 -> "صاف"
    1 -> "عمدتاً صاف"
    2 -> "کمی ابری"
    3 -> "ابری"
    45 -> "مه‌آلود"
    48 -> "مه یخ‌زن"
    51, 53 -> "نم‌نم باران"
    55 -> "نم‌نم باران شدید"
    56, 57 -> "باران یخ‌زن"
    61 -> "باران سبک"
    63 -> "بارانی"
    65 -> "باران شدید"
    66, 67 -> "باران یخ‌زن"
    71 -> "برف سبک"
    73 -> "برفی"
    75 -> "برف شدید"
    77 -> "دانه‌های برف"
    80 -> "رگبار سبک"
    81 -> "رگبار"
    82 -> "رگبار شدید"
    85, 86 -> "رگبار برف"
    95 -> "رعدوبرق"
    96, 99 -> "رعدوبرق و تگرگ"
    else -> "وضعیت نامشخص"
}

fun aqiLabel(aqi: Int): String = when {
    aqi <= 50 -> "پاک"
    aqi <= 100 -> "متوسط"
    aqi <= 150 -> "ناسالم برای گروه‌های حساس"
    aqi <= 200 -> "ناسالم"
    aqi <= 300 -> "بسیار ناسالم"
    else -> "خطرناک"
}

val DefaultRamsar = WeatherLocation(
    name = "رامسر",
    province = "مازندران",
    latitude = 36.9031,
    longitude = 50.6581,
    timezone = "Asia/Tehran"
)

val QuickCities = listOf(
    DefaultRamsar,
    WeatherLocation("تهران", "تهران", 35.6892, 51.3890),
    WeatherLocation("رشت", "گیلان", 37.2808, 49.5832),
    WeatherLocation("مشهد", "خراسان رضوی", 36.2605, 59.6168),
    WeatherLocation("اصفهان", "اصفهان", 32.6546, 51.6680),
    WeatherLocation("شیراز", "فارس", 29.5918, 52.5837),
    WeatherLocation("تبریز", "آذربایجان شرقی", 38.0800, 46.2919),
    WeatherLocation("کیش", "هرمزگان", 26.5325, 53.9820)
)
