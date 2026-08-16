package ir.havayeiran.weather.ui

import ir.havayeiran.weather.data.DailyWeather
import ir.havayeiran.weather.data.HourlyWeather

internal inline fun <T> remember(
    day: DailyWeather,
    hours: List<HourlyWeather>,
    calculation: () -> T
): T = calculation()
