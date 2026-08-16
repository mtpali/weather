package ir.havayeiran.weather.ui

import ir.havayeiran.weather.data.CurrentWeather
import ir.havayeiran.weather.data.DailyWeather
import ir.havayeiran.weather.data.weatherDescription
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val PersianMonths = listOf(
    "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
    "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
)

fun Number.fa(decimals: Int = 0): String {
    val value = this.toDouble()
    val text = if (decimals == 0) value.roundToInt().toString() else String.format(Locale.US, "%.${decimals}f", value)
    return text.toFaDigits()
}

fun String.toFaDigits(): String = buildString(length) {
    this@toFaDigits.forEach { char ->
        append(
            when (char) {
                '0' -> '۰'; '1' -> '۱'; '2' -> '۲'; '3' -> '۳'; '4' -> '۴'
                '5' -> '۵'; '6' -> '۶'; '7' -> '۷'; '8' -> '۸'; '9' -> '۹'
                else -> char
            }
        )
    }
}

fun formatTime(iso: String): String = runCatching {
    LocalDateTime.parse(iso, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        .format(DateTimeFormatter.ofPattern("HH:mm"))
        .toFaDigits()
}.getOrElse {
    iso.substringAfter('T', "--:--").take(5).toFaDigits()
}

fun formatPersianDate(isoDateOrTime: String): String = runCatching {
    val date = LocalDate.parse(isoDateOrTime.take(10))
    val (jy, jm, jd) = gregorianToJalali(date.year, date.monthValue, date.dayOfMonth)
    "${persianWeekday(date.dayOfWeek)}، ${jd.fa()} ${PersianMonths[jm - 1]} ${jy.fa()}"
}.getOrElse { "امروز" }

fun shortPersianDate(isoDate: String): String = runCatching {
    val date = LocalDate.parse(isoDate.take(10))
    val (_, jm, jd) = gregorianToJalali(date.year, date.monthValue, date.dayOfMonth)
    "${jd.fa()} ${PersianMonths[jm - 1]}"
}.getOrElse { isoDate }

fun forecastDayName(isoDate: String, index: Int): String {
    if (index == 0) return "امروز"
    if (index == 1) return "فردا"
    return runCatching { persianWeekday(LocalDate.parse(isoDate.take(10)).dayOfWeek) }.getOrElse { "روز ${index.fa()}" }
}

fun compassLabel(degrees: Int): String {
    val dirs = listOf("شمال", "شمال‌شرق", "شرق", "جنوب‌شرق", "جنوب", "جنوب‌غرب", "غرب", "شمال‌غرب")
    return dirs[((degrees + 22) / 45) % 8]
}

fun uvLabel(uv: Double): String = when {
    uv < 3 -> "کم"
    uv < 6 -> "متوسط"
    uv < 8 -> "زیاد"
    uv < 11 -> "خیلی زیاد"
    else -> "بسیار شدید"
}

fun weatherSentence(current: CurrentWeather, today: DailyWeather?): String {
    val base = "هوا ${weatherDescription(current.weatherCode)} است و دمای احساسی حدود ${current.apparentTemperature.fa()} درجه است."
    val rain = today?.precipitationProbability ?: 0
    return when {
        rain >= 70 -> "$base احتمال بارش بالاست؛ چتر همراهتان باشد."
        rain >= 35 -> "$base احتمال بارش پراکنده وجود دارد."
        current.windSpeed >= 35 -> "$base وزش باد نسبتاً شدید است."
        current.humidity >= 80 -> "$base رطوبت هوا بالاست."
        else -> "$base شرایط کلی برای فعالیت روزانه مناسب به نظر می‌رسد."
    }
}

fun todayAdvice(current: CurrentWeather, today: DailyWeather?, aqi: Int?): String {
    val uv = today?.uvIndex ?: 0.0
    val rain = today?.precipitationProbability ?: 0
    return when {
        aqi != null && aqi > 150 -> "کیفیت هوا ناسالم است؛ فعالیت طولانی در فضای باز را کمتر کنید."
        uv >= 8 -> "تابش فرابنفش بالاست؛ ضدآفتاب، عینک و سایه را جدی بگیرید."
        rain >= 65 -> "امروز احتمال بارش زیاد است؛ برای بیرون رفتن چتر یا بارانی همراه داشته باشید."
        current.windSpeed >= 35 -> "باد نسبتاً شدید است؛ در فضای باز مراقب اجسام سبک و شاخه‌های درختان باشید."
        current.temperature >= 34 -> "هوا گرم است؛ آب کافی بنوشید و فعالیت سنگین را به ساعات خنک‌تر منتقل کنید."
        current.temperature <= 5 -> "هوا سرد است؛ پوشش گرم مناسب همراه داشته باشید."
        else -> "شرایط هوا متعادل است؛ زمان مناسبی برای برنامه‌های روزمره و فضای باز است."
    }
}

private fun persianWeekday(day: DayOfWeek): String = when (day) {
    DayOfWeek.SATURDAY -> "شنبه"
    DayOfWeek.SUNDAY -> "یکشنبه"
    DayOfWeek.MONDAY -> "دوشنبه"
    DayOfWeek.TUESDAY -> "سه‌شنبه"
    DayOfWeek.WEDNESDAY -> "چهارشنبه"
    DayOfWeek.THURSDAY -> "پنجشنبه"
    DayOfWeek.FRIDAY -> "جمعه"
}

private fun gregorianToJalali(gyInput: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
    val gdm = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
    var gy = gyInput
    val gy2 = if (gm > 2) gy + 1 else gy
    var days = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) + ((gy2 + 399) / 400) + gd + gdm[gm - 1]
    var jy = -1595 + 33 * (days / 12053)
    days %= 12053
    jy += 4 * (days / 1461)
    days %= 1461
    if (days > 365) {
        jy += (days - 1) / 365
        days = (days - 1) % 365
    }
    val jm: Int
    val jd: Int
    if (days < 186) {
        jm = 1 + days / 31
        jd = 1 + days % 31
    } else {
        jm = 7 + (days - 186) / 30
        jd = 1 + (days - 186) % 30
    }
    return Triple(jy, jm, jd)
}
