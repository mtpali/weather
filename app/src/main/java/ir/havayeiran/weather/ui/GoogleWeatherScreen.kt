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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MoreVert
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
import ir.havayeiran.weather.data.aqiLabel
import ir.havayeiran.weather.data.weatherDescription
import ir.havayeiran.weather.data.weatherKind
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private val GBg = Color(0xFF202124)
private val GPanel = Color(0xFF292A2D)
private val GSelected = Color(0xFF2B2D31)
private val GText = Color(0xFFF1F3F4)
private val GMuted = Color(0xFF9AA0A6)
private val GGold = Color(0xFFFFC800)
private val GBlue = Color(0xFF8AB4F8)
private val GRain = Color(0xFF1A73E8)
private val GSun = Color(0xFFFF9800)
private val GCloud = Color(0xFFDADCE0)
private val GCloudDark = Color(0xFFB7BBC1)

private enum class GoogleMode { WEATHER, RAIN, WIND }

@Composable
fun GoogleWeatherScreen(
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
        var searchOpen by remember { mutableStateOf(false) }
        var detailsOpen by remember { mutableStateOf(false) }
        var selectedDayIndex by remember(state.selectedLocation.latitude, state.selectedLocation.longitude) { mutableStateOf(0) }
        var mode by remember { mutableStateOf(GoogleMode.WEATHER) }

        val background = if (state.darkMode) GBg else MaterialTheme.colorScheme.background
        val textColor = if (state.darkMode) GText else MaterialTheme.colorScheme.onBackground
        val mutedColor = if (state.darkMode) GMuted else MaterialTheme.colorScheme.onSurfaceVariant

        Box(Modifier.fillMaxSize().background(background)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    GoogleHeader(
                        location = state.selectedLocation,
                        textColor = textColor,
                        mutedColor = mutedColor,
                        searchOpen = searchOpen,
                        onSearchToggle = {
                            searchOpen = !searchOpen
                            if (!searchOpen) onClearSearch()
                        },
                        onLocate = onLocate,
                        onRefresh = onRefresh
                    )
                }

                if (searchOpen) {
                    item {
                        GoogleSearch(
                            state = state,
                            onSearchChange = onSearchChange,
                            onClearSearch = onClearSearch,
                            onSearchResult = {
                                selectedDayIndex = 0
                                onSearchResult(it)
                                searchOpen = false
                            }
                        )
                    }
                }

                if (!state.errorMessage.isNullOrBlank()) {
                    item { GoogleError(state.errorMessage, onRefresh) }
                }

                if (state.isLoading && state.weather == null) {
                    item { GoogleLoading() }
                } else {
                    state.weather?.let { bundle ->
                        val safeIndex = selectedDayIndex.coerceIn(0, bundle.daily.lastIndex.coerceAtLeast(0))
                        val selectedDay = bundle.daily.getOrNull(safeIndex)
                        val selectedHours = selectedDay?.let { day -> bundle.hourly.filter { it.time.startsWith(day.date) } }.orEmpty()

                        item {
                            SelectedDayOverview(
                                bundle = bundle,
                                day = selectedDay,
                                dayIndex = safeIndex,
                                textColor = textColor,
                                mutedColor = mutedColor
                            )
                        }

                        item {
                            GoogleTabs(mode, { mode = it }, textColor, mutedColor)
                        }

                        item {
                            SelectedDayChart(
                                hours = selectedHours,
                                mode = mode,
                                textColor = textColor,
                                mutedColor = mutedColor
                            )
                        }

                        item {
                            DayPicker(
                                days = bundle.daily.take(8),
                                selectedIndex = safeIndex,
                                onSelect = { selectedDayIndex = it },
                                textColor = textColor,
                                mutedColor = mutedColor
                            )
                        }

                        item {
                            HourByHourSection(
                                day = selectedDay,
                                hours = selectedHours,
                                textColor = textColor,
                                mutedColor = mutedColor
                            )
                        }

                        item {
                            GoogleMoreDetails(
                                bundle = bundle,
                                expanded = detailsOpen,
                                onToggle = { detailsOpen = !detailsOpen },
                                state = state,
                                onToggleTheme = onToggleTheme,
                                textColor = textColor,
                                mutedColor = mutedColor
                            )
                        }

                        item {
                            GoogleQuickCities(
                                selected = state.selectedLocation,
                                favorites = state.favorites,
                                onSelect = {
                                    selectedDayIndex = 0
                                    onSelectLocation(it)
                                },
                                textColor = textColor,
                                mutedColor = mutedColor
                            )
                        }

                        item {
                            Text(
                                "داده‌های هواشناسی: Open-Meteo",
                                modifier = Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 10.dp),
                                color = mutedColor.copy(alpha = .68f),
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
private fun GoogleHeader(
    location: WeatherLocation,
    textColor: Color,
    mutedColor: Color,
    searchOpen: Boolean,
    onSearchToggle: () -> Unit,
    onLocate: () -> Unit,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.LocationOn, null, tint = textColor, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            text = if (location.province.isBlank()) location.name else "${location.name}، استان ${location.province}",
            modifier = Modifier.weight(1f),
            color = textColor,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text("· انتخاب منطقه", color = GBlue, style = MaterialTheme.typography.bodySmall)
        IconButton(onClick = onSearchToggle, modifier = Modifier.size(36.dp)) {
            Icon(if (searchOpen) Icons.Rounded.Close else Icons.Rounded.Search, "جستجو", tint = textColor.copy(alpha = .86f), modifier = Modifier.size(19.dp))
        }
        IconButton(onClick = onLocate, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Rounded.MyLocation, "موقعیت من", tint = textColor.copy(alpha = .82f), modifier = Modifier.size(19.dp))
        }
        IconButton(onClick = onRefresh, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Rounded.Refresh, "به‌روزرسانی", tint = textColor.copy(alpha = .82f), modifier = Modifier.size(19.dp))
        }
        Icon(Icons.Rounded.MoreVert, null, tint = mutedColor, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun GoogleSearch(
    state: WeatherUiState,
    onSearchChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSearchResult: (CitySearchResult) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("نام شهر را وارد کنید؛ مثلاً رامسر، رشت یا تهران") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) IconButton(onClick = onClearSearch) { Icon(Icons.Rounded.Close, "پاک کردن") }
            },
            shape = RoundedCornerShape(18.dp),
            singleLine = true
        )
        if (state.searchQuery.trim().length >= 2) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                colors = CardDefaults.cardColors(containerColor = GPanel),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(6.dp)) {
                    if (state.isSearching) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(17.dp), color = GGold, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("در حال جستجو…", color = GMuted)
                        }
                    } else if (state.searchResults.isEmpty()) {
                        Text("شهری پیدا نشد.", Modifier.padding(12.dp), color = GMuted)
                    } else {
                        state.searchResults.take(8).forEach { result ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onSearchResult(result) }.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.LocationOn, null, tint = GBlue, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(result.name, color = GText, style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.width(6.dp))
                                Text(result.province.ifBlank { "ایران" }, color = GMuted, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedDayOverview(
    bundle: WeatherBundle,
    day: DailyWeather?,
    dayIndex: Int,
    textColor: Color,
    mutedColor: Color
) {
    if (day == null) return
    val isToday = dayIndex == 0
    val code = if (isToday) bundle.current.weatherCode else day.weatherCode
    val displayTemp = if (isToday) bundle.current.temperature else day.maxTemperature
    val title = if (isToday) "آب و هوا" else forecastDayName(day.date, dayIndex)
    val subtitle = if (isToday) "${formatPersianDate(bundle.current.time).substringBefore('،')}  ${formatTime(bundle.current.time)}" else shortPersianDate(day.date)

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(.8f), horizontalAlignment = Alignment.Start) {
            Text(title, color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = mutedColor, fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            Text(weatherDescription(code), color = mutedColor, fontSize = 16.sp)
            if (!isToday) {
                Spacer(Modifier.height(5.dp))
                Text("کمینه ${day.minTemperature.fa()}°  ·  بیشینه ${day.maxTemperature.fa()}°", color = mutedColor, style = MaterialTheme.typography.bodySmall)
            }
        }

        Column(Modifier.weight(1.2f), horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(displayTemp.fa(), color = textColor, fontFamily = Vazirmatn, fontWeight = FontWeight.Light, fontSize = 66.sp, lineHeight = 68.sp)
                        Text("°C", color = textColor, fontSize = 21.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                    Text("°C  |  °F", color = mutedColor, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.width(12.dp))
                GoogleWeatherSymbol(code = code, isDay = true, modifier = Modifier.size(92.dp))
            }
            Spacer(Modifier.height(3.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text("بارش: ${day.precipitationProbability.fa()}٪", color = mutedColor, style = MaterialTheme.typography.bodyMedium)
                Text("باد: ${day.maxWindSpeed.fa()} km/h", color = mutedColor, style = MaterialTheme.typography.bodyMedium)
                if (isToday) Text("رطوبت: ${bundle.current.humidity.fa()}٪", color = mutedColor, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun GoogleTabs(selected: GoogleMode, onSelect: (GoogleMode) -> Unit, textColor: Color, mutedColor: Color) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp), horizontalArrangement = Arrangement.End) {
        GoogleTab("هوا", GoogleMode.WEATHER, selected, onSelect, textColor, mutedColor)
        GoogleTab("بارش", GoogleMode.RAIN, selected, onSelect, textColor, mutedColor)
        GoogleTab("باد", GoogleMode.WIND, selected, onSelect, textColor, mutedColor)
    }
}

@Composable
private fun GoogleTab(label: String, value: GoogleMode, selected: GoogleMode, onSelect: (GoogleMode) -> Unit, textColor: Color, mutedColor: Color) {
    Column(
        modifier = Modifier.clickable { onSelect(value) }.padding(horizontal = 13.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = if (selected == value) textColor else mutedColor, fontWeight = if (selected == value) FontWeight.Bold else FontWeight.Normal)
        Spacer(Modifier.height(7.dp))
        Box(Modifier.width(30.dp).height(3.dp).background(if (selected == value) GGold else Color.Transparent))
    }
}

@Composable
private fun SelectedDayChart(hours: List<HourlyWeather>, mode: GoogleMode, textColor: Color, mutedColor: Color) {
    if (hours.isEmpty()) return
    val samples = remember(hours, mode) {
        val indexes = listOf(1, 4, 7, 10, 13, 16, 19, 22).filter { it < hours.size }
        if (indexes.isEmpty()) hours.take(8) else indexes.map { hours[it] }
    }
    val values = samples.map {
        when (mode) {
            GoogleMode.WEATHER -> it.temperature
            GoogleMode.RAIN -> it.precipitationProbability.toDouble()
            GoogleMode.WIND -> it.windSpeed
        }
    }
    val suffix = when (mode) { GoogleMode.WEATHER -> "°"; GoogleMode.RAIN -> "٪"; GoogleMode.WIND -> "" }

    Column(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 6.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            values.forEach { v ->
                Text("${v.fa()}$suffix", Modifier.weight(1f), color = textColor.copy(alpha = .76f), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
        }

        Canvas(Modifier.fillMaxWidth().height(112.dp).padding(horizontal = 20.dp, vertical = 6.dp)) {
            if (values.size < 2) return@Canvas
            val rawMin = values.minOrNull() ?: 0.0
            val rawMax = values.maxOrNull() ?: 1.0
            val minV = when (mode) { GoogleMode.RAIN, GoogleMode.WIND -> 0.0; GoogleMode.WEATHER -> rawMin - 2.0 }
            val maxV = when (mode) { GoogleMode.RAIN -> 100.0; GoogleMode.WIND -> (rawMax + 5.0).coerceAtLeast(10.0); GoogleMode.WEATHER -> rawMax + 2.0 }
            val span = (maxV - minV).coerceAtLeast(1.0)
            val points = values.mapIndexed { index, value ->
                val x = index * size.width / values.lastIndex
                val n = ((value - minV) / span).toFloat().coerceIn(0f, 1f)
                Offset(x, size.height - n * size.height * .7f - size.height * .08f)
            }
            val line = smoothPath(points)
            val area = smoothAreaPath(points, size.height)
            drawPath(area, Brush.verticalGradient(listOf(GGold.copy(alpha = .30f), GGold.copy(alpha = .08f))))
            drawPath(line, GGold, style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round))
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
            samples.forEach { hour ->
                Text(formatTime(hour.time), Modifier.weight(1f), color = mutedColor, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun DayPicker(
    days: List<DailyWeather>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    textColor: Color,
    mutedColor: Color
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        itemsIndexed(days) { index, day ->
            val selected = index == selectedIndex
            Column(
                modifier = Modifier
                    .width(98.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(if (selected) GSelected else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(horizontal = 7.dp, vertical = 11.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(forecastDayName(day.date, index), color = textColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                GoogleWeatherSymbol(day.weatherCode, isDay = true, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(2.dp))
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
private fun HourByHourSection(day: DailyWeather?, hours: List<HourlyWeather>, textColor: Color, mutedColor: Color) {
    if (day == null || hours.isEmpty()) return
    Column(Modifier.fillMaxWidth().padding(top = 24.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("وضعیت ساعتی ${forecastDayName(day.date, 2)}", modifier = Modifier.weight(1f), color = textColor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Text("برای دیدن ساعت‌ها ورق بزنید", color = mutedColor, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(hours) { hour ->
                HourConditionCard(hour, textColor, mutedColor)
            }
        }
    }
}

@Composable
private fun HourConditionCard(hour: HourlyWeather, textColor: Color, mutedColor: Color) {
    Card(
        modifier = Modifier.width(84.dp),
        colors = CardDefaults.cardColors(containerColor = GPanel.copy(alpha = .78f)),
        shape = RoundedCornerShape(17.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .035f))
    ) {
        Column(Modifier.padding(horizontal = 7.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(formatTime(hour.time), color = mutedColor, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(4.dp))
            GoogleWeatherSymbol(hour.weatherCode, isDay = hourIsDay(hour.time), modifier = Modifier.size(48.dp))
            Text("${hour.temperature.fa()}°", color = textColor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(weatherDescription(hour.weatherCode), color = mutedColor, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            if (hour.precipitationProbability > 0) Text("${hour.precipitationProbability.fa()}٪", color = GBlue, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun GoogleMoreDetails(
    bundle: WeatherBundle,
    expanded: Boolean,
    onToggle: () -> Unit,
    state: WeatherUiState,
    onToggleTheme: () -> Unit,
    textColor: Color,
    mutedColor: Color
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onToggle() }.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("جزئیات بیشتر", modifier = Modifier.weight(1f), color = textColor, fontWeight = FontWeight.Medium)
            Icon(if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown, null, tint = mutedColor)
        }
        if (expanded) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GoogleMetric("رطوبت", "${bundle.current.humidity.fa()}٪", Modifier.weight(1f), textColor, mutedColor)
                GoogleMetric("فشار", "${bundle.current.pressure.fa()} hPa", Modifier.weight(1f), textColor, mutedColor)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GoogleMetric("دید", "${bundle.current.visibilityKm.fa(1)} km", Modifier.weight(1f), textColor, mutedColor)
                GoogleMetric("جهت باد", compassLabel(bundle.current.windDirection), Modifier.weight(1f), textColor, mutedColor)
            }
            bundle.airQuality?.let { aq ->
                Spacer(Modifier.height(8.dp))
                GoogleMetric("کیفیت هوا", "AQI ${aq.usAqi.fa()} · ${aqiLabel(aq.usAqi)}", Modifier.fillMaxWidth(), textColor, mutedColor, "PM2.5 ${aq.pm25.fa(1)} · PM10 ${aq.pm10.fa(1)}")
            }
            bundle.marine?.let { marine ->
                if (marine.seaSurfaceTemperature != null || marine.waveHeight != null) {
                    Spacer(Modifier.height(8.dp))
                    GoogleMetric("دریای خزر", "آب ${marine.seaSurfaceTemperature?.fa(1) ?: "—"}° · موج ${marine.waveHeight?.fa(1) ?: "—"} m", Modifier.fillMaxWidth(), textColor, mutedColor)
                }
            }
            Spacer(Modifier.height(8.dp))
            Card(colors = CardDefaults.cardColors(containerColor = GPanel.copy(alpha = .78f)), shape = RoundedCornerShape(17.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("ظاهر برنامه", color = textColor, style = MaterialTheme.typography.titleMedium)
                        Text(if (state.darkMode) "حالت تیره" else "حالت روشن", color = mutedColor, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = onToggleTheme) {
                        Icon(if (state.darkMode) Icons.Rounded.LightMode else Icons.Rounded.DarkMode, "تغییر تم", tint = GGold)
                    }
                }
            }
        }
    }
}

@Composable
private fun GoogleMetric(label: String, value: String, modifier: Modifier, textColor: Color, mutedColor: Color, note: String? = null) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = GPanel.copy(alpha = .78f)), shape = RoundedCornerShape(17.dp)) {
        Column(Modifier.padding(13.dp)) {
            Text(label, color = mutedColor, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(4.dp))
            Text(value, color = textColor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            if (!note.isNullOrBlank()) Text(note, color = mutedColor, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun GoogleQuickCities(selected: WeatherLocation, favorites: List<WeatherLocation>, onSelect: (WeatherLocation) -> Unit, textColor: Color, mutedColor: Color) {
    val cities = (favorites + QuickCities).distinctBy { "${it.latitude}-${it.longitude}" }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text("شهرهای منتخب", color = textColor, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            cities.forEach { city ->
                val active = nearLocation(city, selected)
                Box(
                    Modifier.clip(RoundedCornerShape(50)).background(if (active) GSelected else GPanel.copy(alpha = .7f)).clickable { onSelect(city) }.padding(horizontal = 13.dp, vertical = 8.dp)
                ) {
                    Text(city.name, color = if (active) textColor else mutedColor, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun GoogleError(message: String, onRefresh: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF3B2929)), shape = RoundedCornerShape(15.dp)) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, Modifier.weight(1f), color = Color(0xFFFFD3D3), style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onRefresh) { Text("تلاش دوباره", color = GGold) }
        }
    }
}

@Composable
private fun GoogleLoading() {
    Column(Modifier.fillMaxWidth().padding(top = 90.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = GGold, strokeWidth = 3.dp)
        Spacer(Modifier.height(12.dp))
        Text("در حال دریافت اطلاعات هوا…", color = GMuted)
    }
}

@Composable
private fun GoogleWeatherSymbol(code: Int, isDay: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val kind = weatherKind(code)
        val w = size.width
        val h = size.height
        when (kind) {
            WeatherKind.CLEAR -> {
                if (isDay) drawGoogleSun(Offset(w * .50f, h * .49f), w * .22f)
                else drawGoogleMoon(Offset(w * .50f, h * .48f), w * .22f)
            }
            WeatherKind.PARTLY_CLOUDY -> {
                if (isDay) drawGoogleSun(Offset(w * .38f, h * .35f), w * .20f) else drawGoogleMoon(Offset(w * .38f, h * .34f), w * .19f)
                drawGoogleCloud(Offset(w * .58f, h * .60f), w * .66f, GCloud)
            }
            WeatherKind.CLOUDY -> {
                drawGoogleCloud(Offset(w * .43f, h * .47f), w * .58f, GCloudDark)
                drawGoogleCloud(Offset(w * .58f, h * .61f), w * .70f, GCloud)
            }
            WeatherKind.FOG -> {
                drawGoogleCloud(Offset(w * .52f, h * .45f), w * .67f, GCloud)
                repeat(2) { i ->
                    drawLine(GMuted, Offset(w * .20f, h * (.72f + i * .10f)), Offset(w * .80f, h * (.72f + i * .10f)), 4f, cap = StrokeCap.Round)
                }
            }
            WeatherKind.RAIN -> {
                drawGoogleCloud(Offset(w * .50f, h * .43f), w * .70f, GCloud)
                drawRaindrop(Offset(w * .53f, h * .78f), w * .095f, GRain)
            }
            WeatherKind.STORM -> {
                drawGoogleCloud(Offset(w * .50f, h * .40f), w * .70f, GCloudDark)
                val bolt = Path().apply {
                    moveTo(w * .50f, h * .57f); lineTo(w * .40f, h * .76f); lineTo(w * .50f, h * .76f); lineTo(w * .43f, h * .92f); lineTo(w * .65f, h * .68f); lineTo(w * .54f, h * .68f); close()
                }
                drawPath(bolt, GGold)
            }
            WeatherKind.SNOW -> {
                drawGoogleCloud(Offset(w * .50f, h * .42f), w * .70f, GCloud)
                repeat(3) { i -> drawCircle(Color.White, radius = w * .045f, center = Offset(w * (.34f + i * .17f), h * .79f)) }
            }
        }
    }
}

private fun DrawScope.drawGoogleSun(center: Offset, radius: Float) {
    drawCircle(GSun.copy(alpha = .18f), radius * 1.28f, center)
    drawCircle(GSun, radius, center)
}

private fun DrawScope.drawGoogleMoon(center: Offset, radius: Float) {
    drawCircle(Color(0xFFE2E5EA), radius, center)
    drawCircle(GBg, radius * .84f, Offset(center.x - radius * .42f, center.y - radius * .22f))
}

private fun DrawScope.drawGoogleCloud(center: Offset, width: Float, color: Color) {
    val height = width * .40f
    drawRoundRect(color, Offset(center.x - width * .50f, center.y - height * .05f), Size(width, height * .66f), androidx.compose.ui.geometry.CornerRadius(height * .34f))
    drawCircle(color, width * .19f, Offset(center.x - width * .22f, center.y - height * .05f))
    drawCircle(color, width * .25f, Offset(center.x + width * .06f, center.y - height * .18f))
    drawCircle(color, width * .17f, Offset(center.x + width * .29f, center.y))
    drawArc(Color.White.copy(alpha = .16f), 190f, 92f, false, Offset(center.x - width * .38f, center.y - height * .30f), Size(width * .60f, height * .72f), style = Stroke(width = 2.3f, cap = StrokeCap.Round))
}

private fun DrawScope.drawRaindrop(center: Offset, radius: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius * 1.55f)
        cubicTo(center.x - radius * 1.15f, center.y - radius * .25f, center.x - radius * 1.10f, center.y + radius * .95f, center.x, center.y + radius * 1.10f)
        cubicTo(center.x + radius * 1.10f, center.y + radius * .95f, center.x + radius * 1.15f, center.y - radius * .25f, center.x, center.y - radius * 1.55f)
        close()
    }
    drawPath(path, color)
}

private fun hourIsDay(time: String): Boolean {
    val hour = time.substringAfter('T', "12:00").take(2).toIntOrNull() ?: 12
    return hour in 7..18
}

private fun smoothPath(points: List<Offset>): Path = Path().apply {
    if (points.isEmpty()) return@apply
    moveTo(points.first().x, points.first().y)
    for (i in 1 until points.size) {
        val prev = points[i - 1]
        val current = points[i]
        val midX = (prev.x + current.x) / 2f
        cubicTo(midX, prev.y, midX, current.y, current.x, current.y)
    }
}

private fun smoothAreaPath(points: List<Offset>, bottom: Float): Path = Path().apply {
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

private fun nearLocation(a: WeatherLocation, b: WeatherLocation): Boolean = abs(a.latitude - b.latitude) < .01 && abs(a.longitude - b.longitude) < .01
