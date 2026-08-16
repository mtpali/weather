package ir.havayeiran.weather.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.havayeiran.weather.data.CitySearchResult
import ir.havayeiran.weather.data.DailyWeather
import ir.havayeiran.weather.data.HourlyWeather
import ir.havayeiran.weather.data.QuickCities
import ir.havayeiran.weather.data.WeatherBundle
import ir.havayeiran.weather.data.WeatherKind
import ir.havayeiran.weather.data.WeatherLocation
import ir.havayeiran.weather.data.weatherDescription
import ir.havayeiran.weather.data.weatherKind
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private val V2Bg = Color(0xFF202124)
private val V2Panel = Color(0xFF292A2D)
private val V2Selected = Color(0xFF303134)
private val V2Text = Color(0xFFF1F3F4)
private val V2Muted = Color(0xFF9AA0A6)
private val V2Gold = Color(0xFFFFC107)
private val V2Blue = Color(0xFF4285F4)
private val V2RainBlue = Color(0xFF1A73E8)
private val V2SunOrange = Color(0xFFFB8C00)
private val V2SunYellow = Color(0xFFFFB300)
private val V2Cloud = Color(0xFFE3E6EA)
private val V2CloudShade = Color(0xFFB5BAC1)
private val V2StormCloud = Color(0xFF8F969F)

private enum class ForecastTabV2 { WEATHER, RAIN, WIND }

@Composable
fun GoogleWeatherScreenV2(
    state: WeatherUiState,
    isFavorite: Boolean,
    onSearchChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSearchResult: (CitySearchResult) -> Unit,
    onSelectLocation: (WeatherLocation) -> Unit,
    onRefresh: () -> Unit,
    onLocate: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleTheme: () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        var selectedDayIndex by remember(state.selectedLocation.latitude, state.selectedLocation.longitude) { mutableStateOf(0) }
        var selectedTab by remember { mutableStateOf(ForecastTabV2.WEATHER) }
        var searchOpen by remember { mutableStateOf(false) }

        val bg = if (state.darkMode) V2Bg else MaterialTheme.colorScheme.background
        val text = if (state.darkMode) V2Text else MaterialTheme.colorScheme.onBackground
        val muted = if (state.darkMode) V2Muted else MaterialTheme.colorScheme.onSurfaceVariant

        Box(Modifier.fillMaxSize().background(bg)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    TopWeatherBarV2(
                        location = state.selectedLocation,
                        searchOpen = searchOpen,
                        onSearch = {
                            searchOpen = !searchOpen
                            if (!searchOpen) onClearSearch()
                        },
                        onLocate = onLocate,
                        onRefresh = onRefresh,
                        onTheme = onToggleTheme,
                        darkMode = state.darkMode,
                        textColor = text,
                        mutedColor = muted
                    )
                }

                if (searchOpen) {
                    item {
                        SearchPanelV2(
                            state = state,
                            onSearchChange = onSearchChange,
                            onClearSearch = onClearSearch,
                            onSelect = {
                                selectedDayIndex = 0
                                searchOpen = false
                                onSearchResult(it)
                            }
                        )
                    }
                }

                if (!state.errorMessage.isNullOrBlank()) {
                    item { ErrorBarV2(state.errorMessage, onRefresh) }
                }

                if (state.isLoading && state.weather == null) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 100.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = V2Gold)
                            Spacer(Modifier.height(12.dp))
                            Text("در حال دریافت اطلاعات هوا…", color = muted)
                        }
                    }
                } else {
                    state.weather?.let { bundle ->
                        val safeIndex = selectedDayIndex.coerceIn(0, bundle.daily.lastIndex.coerceAtLeast(0))
                        val day = bundle.daily.getOrNull(safeIndex)
                        val hours = day?.let { d -> bundle.hourly.filter { it.time.startsWith(d.date) } }.orEmpty()

                        item {
                            MainSummaryV2(
                                bundle = bundle,
                                day = day,
                                dayIndex = safeIndex,
                                isFavorite = isFavorite,
                                onFavorite = onToggleFavorite,
                                textColor = text,
                                mutedColor = muted
                            )
                        }

                        item { TabsV2(selectedTab, { selectedTab = it }, text, muted) }
                        item { DayChartV2(hours, selectedTab, text, muted) }

                        item {
                            DailyChooserV2(
                                days = bundle.daily.take(8),
                                selectedIndex = safeIndex,
                                onSelect = { selectedDayIndex = it },
                                textColor = text,
                                mutedColor = muted
                            )
                        }

                        item {
                            HourlyForecastV2(
                                day = day,
                                hours = hours,
                                textColor = text,
                                mutedColor = muted
                            )
                        }

                        item {
                            QuickCitiesV2(
                                selected = state.selectedLocation,
                                favorites = state.favorites,
                                onSelect = {
                                    selectedDayIndex = 0
                                    onSelectLocation(it)
                                },
                                textColor = text,
                                mutedColor = muted
                            )
                        }

                        item {
                            Text(
                                "داده‌های هواشناسی: Open-Meteo",
                                modifier = Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 8.dp),
                                color = muted.copy(alpha = .65f),
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopWeatherBarV2(
    location: WeatherLocation,
    searchOpen: Boolean,
    onSearch: () -> Unit,
    onLocate: () -> Unit,
    onRefresh: () -> Unit,
    onTheme: () -> Unit,
    darkMode: Boolean,
    textColor: Color,
    mutedColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.LocationOn, null, tint = textColor, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(5.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (location.province.isBlank()) location.name else "${location.name}، استان ${location.province}",
                color = textColor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text("ایران · انتخاب منطقه", color = V2Blue, style = MaterialTheme.typography.bodySmall)
        }
        SmallIconButtonV2(if (searchOpen) Icons.Rounded.Close else Icons.Rounded.Search, "جستجو", onSearch, textColor)
        SmallIconButtonV2(Icons.Rounded.MyLocation, "موقعیت من", onLocate, textColor)
        SmallIconButtonV2(Icons.Rounded.Refresh, "به‌روزرسانی", onRefresh, textColor)
        SmallIconButtonV2(if (darkMode) Icons.Rounded.LightMode else Icons.Rounded.DarkMode, "تغییر تم", onTheme, mutedColor)
    }
}

@Composable
private fun SmallIconButtonV2(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    tint: Color
) {
    IconButton(onClick = onClick, modifier = Modifier.size(34.dp)) {
        Icon(icon, description, tint = tint.copy(alpha = .88f), modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SearchPanelV2(
    state: WeatherUiState,
    onSearchChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSelect: (CitySearchResult) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp)) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("جستجوی شهر در ایران") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) IconButton(onClick = onClearSearch) { Icon(Icons.Rounded.Close, "پاک کردن") }
            },
            singleLine = true,
            shape = RoundedCornerShape(18.dp)
        )
        if (state.searchQuery.trim().length >= 2) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                colors = CardDefaults.cardColors(containerColor = V2Panel),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(6.dp)) {
                    if (state.isSearching) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.Center) {
                            CircularProgressIndicator(Modifier.size(18.dp), color = V2Gold, strokeWidth = 2.dp)
                        }
                    } else if (state.searchResults.isEmpty()) {
                        Text("شهری پیدا نشد.", Modifier.padding(12.dp), color = V2Muted)
                    } else {
                        state.searchResults.take(8).forEach { result ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSelect(result) }
                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.LocationOn, null, tint = V2Blue, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(result.name, color = V2Text, style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.width(7.dp))
                                Text(result.province.ifBlank { "ایران" }, color = V2Muted, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MainSummaryV2(
    bundle: WeatherBundle,
    day: DailyWeather?,
    dayIndex: Int,
    isFavorite: Boolean,
    onFavorite: () -> Unit,
    textColor: Color,
    mutedColor: Color
) {
    if (day == null) return
    val today = dayIndex == 0
    val code = if (today) bundle.current.weatherCode else day.weatherCode
    val temp = if (today) bundle.current.temperature else day.maxTemperature
    val dayLabel = if (today) "آب و هوا" else forecastDayName(day.date, dayIndex)
    val timeLabel = if (today) "${formatPersianDate(bundle.current.time).substringBefore('،')}  ${formatTime(bundle.current.time)}" else shortPersianDate(day.date)

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 26.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(.82f)) {
            Text(dayLabel, color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text(timeLabel, color = mutedColor, fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            Text(weatherDescription(code), color = mutedColor, fontSize = 16.sp)
            if (!today) {
                Spacer(Modifier.height(5.dp))
                Text("${day.minTemperature.fa()}° / ${day.maxTemperature.fa()}°", color = mutedColor, style = MaterialTheme.typography.bodySmall)
            }
        }

        Column(Modifier.weight(1.18f), horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(temp.fa(), color = textColor, fontFamily = Vazirmatn, fontWeight = FontWeight.Light, fontSize = 67.sp, lineHeight = 68.sp)
                        Text("°C", color = textColor, fontSize = 21.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("°C  |  °F", color = mutedColor, style = MaterialTheme.typography.bodySmall)
                        IconButton(onClick = onFavorite, modifier = Modifier.size(30.dp)) {
                            Icon(
                                if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                "علاقه‌مندی",
                                tint = if (isFavorite) Color(0xFFFF6B86) else mutedColor,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.width(9.dp))
                VividWeatherIcon(code, isDay = if (today) bundle.current.isDay else true, modifier = Modifier.size(100.dp))
            }
            Spacer(Modifier.height(4.dp))
            Text("بارش: ${day.precipitationProbability.fa()}٪", color = mutedColor, style = MaterialTheme.typography.bodyMedium)
            if (today) Text("رطوبت: ${bundle.current.humidity.fa()}٪", color = mutedColor, style = MaterialTheme.typography.bodyMedium)
            Text("باد: ${day.maxWindSpeed.fa()} km/h", color = mutedColor, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun TabsV2(selected: ForecastTabV2, onSelect: (ForecastTabV2) -> Unit, textColor: Color, mutedColor: Color) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.End) {
        TabButtonV2("هوا", ForecastTabV2.WEATHER, selected, onSelect, textColor, mutedColor)
        TabButtonV2("بارش", ForecastTabV2.RAIN, selected, onSelect, textColor, mutedColor)
        TabButtonV2("باد", ForecastTabV2.WIND, selected, onSelect, textColor, mutedColor)
    }
}

@Composable
private fun TabButtonV2(label: String, value: ForecastTabV2, selected: ForecastTabV2, onSelect: (ForecastTabV2) -> Unit, textColor: Color, mutedColor: Color) {
    Column(
        modifier = Modifier.clickable { onSelect(value) }.padding(horizontal = 13.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = if (selected == value) textColor else mutedColor, fontWeight = if (selected == value) FontWeight.Bold else FontWeight.Normal)
        Spacer(Modifier.height(7.dp))
        Box(Modifier.width(28.dp).height(3.dp).background(if (selected == value) V2Gold else Color.Transparent))
    }
}

@Composable
private fun DayChartV2(hours: List<HourlyWeather>, tab: ForecastTabV2, textColor: Color, mutedColor: Color) {
    if (hours.isEmpty()) return
    val samples = remember(hours, tab) {
        val indexes = listOf(1, 4, 7, 10, 13, 16, 19, 22).filter { it < hours.size }
        if (indexes.isEmpty()) hours.take(8) else indexes.map { hours[it] }
    }
    val values = samples.map {
        when (tab) {
            ForecastTabV2.WEATHER -> it.temperature
            ForecastTabV2.RAIN -> it.precipitationProbability.toDouble()
            ForecastTabV2.WIND -> it.windSpeed
        }
    }
    val suffix = when (tab) {
        ForecastTabV2.WEATHER -> "°"
        ForecastTabV2.RAIN -> "٪"
        ForecastTabV2.WIND -> ""
    }

    Column(Modifier.fillMaxWidth().padding(top = 23.dp, bottom = 7.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
            values.forEach { value ->
                Text(
                    "${value.fa()}$suffix",
                    modifier = Modifier.weight(1f),
                    color = textColor.copy(alpha = .78f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Canvas(Modifier.fillMaxWidth().height(112.dp).padding(horizontal = 18.dp, vertical = 6.dp)) {
            if (values.size < 2) return@Canvas
            val rawMin = values.minOrNull() ?: 0.0
            val rawMax = values.maxOrNull() ?: 1.0
            val minV = when (tab) {
                ForecastTabV2.RAIN, ForecastTabV2.WIND -> 0.0
                ForecastTabV2.WEATHER -> rawMin - 2.0
            }
            val maxV = when (tab) {
                ForecastTabV2.RAIN -> 100.0
                ForecastTabV2.WIND -> (rawMax + 5.0).coerceAtLeast(10.0)
                ForecastTabV2.WEATHER -> rawMax + 2.0
            }
            val span = (maxV - minV).coerceAtLeast(1.0)
            val points = values.mapIndexed { i, value ->
                val x = i * size.width / values.lastIndex
                val norm = ((value - minV) / span).toFloat().coerceIn(0f, 1f)
                Offset(x, size.height - norm * size.height * .70f - size.height * .08f)
            }
            val line = smoothLineV2(points)
            val area = smoothAreaV2(points, size.height)
            drawPath(area, Brush.verticalGradient(listOf(V2Gold.copy(alpha = .31f), V2Gold.copy(alpha = .08f))))
            drawPath(line, V2Gold, style = Stroke(width = 2.7.dp.toPx(), cap = StrokeCap.Round))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            samples.forEach { hour ->
                Text(formatTime(hour.time), Modifier.weight(1f), color = mutedColor, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun DailyChooserV2(
    days: List<DailyWeather>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    textColor: Color,
    mutedColor: Color
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        contentPadding = PaddingValues(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        itemsIndexed(days) { index, day ->
            val selected = index == selectedIndex
            Column(
                modifier = Modifier
                    .width(102.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(if (selected) V2Selected else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(horizontal = 6.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(forecastDayName(day.date, index), color = textColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(3.dp))
                VividWeatherIcon(day.weatherCode, true, Modifier.size(64.dp))
                Spacer(Modifier.height(1.dp))
                Text(weatherDescription(day.weatherCode), color = mutedColor, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${day.minTemperature.fa()}°", color = mutedColor, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.width(5.dp))
                    Text("${day.maxTemperature.fa()}°", color = textColor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun HourlyForecastV2(day: DailyWeather?, hours: List<HourlyWeather>, textColor: Color, mutedColor: Color) {
    if (day == null || hours.isEmpty()) return
    Column(Modifier.fillMaxWidth().padding(top = 25.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("پیش‌بینی ساعتی ${shortPersianDate(day.date)}", modifier = Modifier.weight(1f), color = textColor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Text("برای دیدن همه ساعت‌ها ورق بزنید", color = mutedColor, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(hours) { hour ->
                HourCardV2(hour, textColor, mutedColor)
            }
        }
    }
}

@Composable
private fun HourCardV2(hour: HourlyWeather, textColor: Color, mutedColor: Color) {
    Card(
        modifier = Modifier.width(90.dp),
        colors = CardDefaults.cardColors(containerColor = V2Panel.copy(alpha = .88f)),
        shape = RoundedCornerShape(17.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .04f))
    ) {
        Column(Modifier.padding(horizontal = 7.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(formatTime(hour.time), color = mutedColor, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(4.dp))
            VividWeatherIcon(hour.weatherCode, hourIsDayV2(hour.time), Modifier.size(56.dp))
            Text("${hour.temperature.fa()}°", color = textColor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(weatherDescription(hour.weatherCode), color = mutedColor, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            if (hour.precipitationProbability > 0) {
                Text("💧 ${hour.precipitationProbability.fa()}٪", color = V2RainBlue, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun QuickCitiesV2(
    selected: WeatherLocation,
    favorites: List<WeatherLocation>,
    onSelect: (WeatherLocation) -> Unit,
    textColor: Color,
    mutedColor: Color
) {
    val cities = (favorites + QuickCities).distinctBy { "${it.latitude}-${it.longitude}" }
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 22.dp)) {
        Text("شهرهای منتخب", color = textColor, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            cities.forEach { city ->
                val active = nearV2(city, selected)
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (active) V2Selected else V2Panel.copy(alpha = .75f))
                        .clickable { onSelect(city) }
                        .padding(horizontal = 13.dp, vertical = 8.dp)
                ) {
                    Text(city.name, color = if (active) textColor else mutedColor, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun ErrorBarV2(message: String, onRefresh: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF3B2929)),
        shape = RoundedCornerShape(15.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, Modifier.weight(1f), color = Color(0xFFFFD3D3), style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onRefresh) { Text("تلاش دوباره", color = V2Gold) }
        }
    }
}

@Composable
private fun VividWeatherIcon(code: Int, isDay: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val kind = weatherKind(code)
        val w = size.width
        val h = size.height
        when (kind) {
            WeatherKind.CLEAR -> {
                if (isDay) drawVividSun(Offset(w * .50f, h * .50f), w * .22f)
                else drawVividMoon(Offset(w * .50f, h * .50f), w * .22f)
            }
            WeatherKind.PARTLY_CLOUDY -> {
                if (isDay) drawVividSun(Offset(w * .34f, h * .34f), w * .20f)
                else drawVividMoon(Offset(w * .34f, h * .34f), w * .19f)
                drawVividCloud(Offset(w * .59f, h * .61f), w * .68f, V2Cloud, V2CloudShade)
            }
            WeatherKind.CLOUDY -> {
                drawVividCloud(Offset(w * .40f, h * .44f), w * .56f, V2CloudShade, Color(0xFF969CA4))
                drawVividCloud(Offset(w * .58f, h * .62f), w * .70f, V2Cloud, V2CloudShade)
            }
            WeatherKind.FOG -> {
                drawVividCloud(Offset(w * .51f, h * .42f), w * .67f, V2Cloud, V2CloudShade)
                repeat(2) { i ->
                    drawLine(
                        Color(0xFFB8BDC4),
                        Offset(w * .18f, h * (.72f + i * .11f)),
                        Offset(w * .82f, h * (.72f + i * .11f)),
                        4.5f,
                        cap = StrokeCap.Round
                    )
                }
            }
            WeatherKind.RAIN -> {
                drawVividCloud(Offset(w * .50f, h * .40f), w * .70f, V2Cloud, V2CloudShade)
                drawDropV2(Offset(w * .33f, h * .78f), w * .060f, V2RainBlue)
                drawDropV2(Offset(w * .51f, h * .82f), w * .070f, Color(0xFF0D6EFD))
                drawDropV2(Offset(w * .69f, h * .76f), w * .058f, V2Blue)
            }
            WeatherKind.STORM -> {
                drawVividCloud(Offset(w * .50f, h * .38f), w * .71f, V2StormCloud, Color(0xFF6F7680))
                drawDropV2(Offset(w * .30f, h * .76f), w * .055f, V2RainBlue)
                drawDropV2(Offset(w * .70f, h * .76f), w * .055f, V2Blue)
                val bolt = Path().apply {
                    moveTo(w * .51f, h * .55f)
                    lineTo(w * .40f, h * .73f)
                    lineTo(w * .50f, h * .73f)
                    lineTo(w * .43f, h * .91f)
                    lineTo(w * .65f, h * .66f)
                    lineTo(w * .54f, h * .66f)
                    close()
                }
                drawPath(bolt, Color(0xFFFFD54F))
            }
            WeatherKind.SNOW -> {
                drawVividCloud(Offset(w * .50f, h * .40f), w * .70f, V2Cloud, V2CloudShade)
                repeat(3) { i ->
                    val cx = w * (.32f + i * .18f)
                    val cy = h * (.78f + (i % 2) * .04f)
                    drawSnowflakeV2(Offset(cx, cy), w * .055f)
                }
            }
        }
    }
}

private fun DrawScope.drawVividSun(center: Offset, radius: Float) {
    repeat(8) { i ->
        val angle = i * 45.0 * PI / 180.0
        val start = Offset(
            center.x + cos(angle).toFloat() * radius * 1.38f,
            center.y + sin(angle).toFloat() * radius * 1.38f
        )
        val end = Offset(
            center.x + cos(angle).toFloat() * radius * 1.72f,
            center.y + sin(angle).toFloat() * radius * 1.72f
        )
        drawLine(V2SunOrange, start, end, strokeWidth = radius * .13f, cap = StrokeCap.Round)
    }
    drawCircle(V2SunOrange.copy(alpha = .18f), radius * 1.24f, center)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFD54F), V2SunYellow, V2SunOrange),
            center = center,
            radius = radius * 1.2f
        ),
        radius = radius,
        center = center
    )
}

private fun DrawScope.drawVividMoon(center: Offset, radius: Float) {
    drawCircle(Color(0xFFDCE6FF), radius, center)
    drawCircle(V2Bg, radius * .82f, Offset(center.x - radius * .42f, center.y - radius * .20f))
}

private fun DrawScope.drawVividCloud(center: Offset, width: Float, front: Color, shadow: Color) {
    val height = width * .40f
    drawRoundRect(
        shadow,
        topLeft = Offset(center.x - width * .49f, center.y + height * .07f),
        size = Size(width * .96f, height * .62f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(height * .34f)
    )
    drawRoundRect(
        front,
        topLeft = Offset(center.x - width * .50f, center.y - height * .04f),
        size = Size(width, height * .63f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(height * .34f)
    )
    drawCircle(front, width * .19f, Offset(center.x - width * .22f, center.y - height * .06f))
    drawCircle(front, width * .25f, Offset(center.x + width * .05f, center.y - height * .19f))
    drawCircle(front, width * .17f, Offset(center.x + width * .29f, center.y - height * .01f))
    drawArc(
        Color.White.copy(alpha = .34f),
        startAngle = 190f,
        sweepAngle = 88f,
        useCenter = false,
        topLeft = Offset(center.x - width * .37f, center.y - height * .31f),
        size = Size(width * .59f, height * .70f),
        style = Stroke(width = 2.5f, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawDropV2(center: Offset, radius: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius * 1.55f)
        cubicTo(
            center.x - radius * 1.05f, center.y - radius * .25f,
            center.x - radius * 1.05f, center.y + radius * .90f,
            center.x, center.y + radius * 1.10f
        )
        cubicTo(
            center.x + radius * 1.05f, center.y + radius * .90f,
            center.x + radius * 1.05f, center.y - radius * .25f,
            center.x, center.y - radius * 1.55f
        )
        close()
    }
    drawPath(path, color)
    drawCircle(Color.White.copy(alpha = .24f), radius * .22f, Offset(center.x - radius * .30f, center.y - radius * .25f))
}

private fun DrawScope.drawSnowflakeV2(center: Offset, radius: Float) {
    repeat(3) { i ->
        val angle = i * 60.0 * PI / 180.0
        val dx = cos(angle).toFloat() * radius
        val dy = sin(angle).toFloat() * radius
        drawLine(Color(0xFFBBDEFB), Offset(center.x - dx, center.y - dy), Offset(center.x + dx, center.y + dy), 3f, cap = StrokeCap.Round)
    }
}

private fun hourIsDayV2(time: String): Boolean {
    val hour = time.substringAfter('T', "12:00").take(2).toIntOrNull() ?: 12
    return hour in 7..18
}

private fun smoothLineV2(points: List<Offset>): Path = Path().apply {
    if (points.isEmpty()) return@apply
    moveTo(points.first().x, points.first().y)
    for (i in 1 until points.size) {
        val prev = points[i - 1]
        val current = points[i]
        val midX = (prev.x + current.x) / 2f
        cubicTo(midX, prev.y, midX, current.y, current.x, current.y)
    }
}

private fun smoothAreaV2(points: List<Offset>, bottom: Float): Path = Path().apply {
    if (points.isEmpty()) return@apply
    moveTo(points.first().x, bottom)
    lineTo(points.first().x, points.first().y)
    for (i in 1 until points.size) {
        val prev = points[i - 1]
        val current = points[i]
        val midX = (prev.x + current.x) / 2f
        cubicTo(midX, prev.y, midX, current.y, current.x, current.y)
    }
    lineTo(points.last().x, bottom)
    close()
}

private fun nearV2(a: WeatherLocation, b: WeatherLocation): Boolean =
    abs(a.latitude - b.latitude) < .01 && abs(a.longitude - b.longitude) < .01
