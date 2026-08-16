package ir.havayeiran.weather.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.havayeiran.weather.data.CitySearchResult
import ir.havayeiran.weather.data.QuickCities
import ir.havayeiran.weather.data.WeatherBundle
import ir.havayeiran.weather.data.WeatherLocation
import ir.havayeiran.weather.data.aqiLabel
import ir.havayeiran.weather.data.weatherDescription
import kotlin.math.abs

private val GoogleBg = Color(0xFF202328)
private val GooglePanel = Color(0xFF25292F)
private val GooglePanelSelected = Color(0xFF2B2F36)
private val GoogleGold = Color(0xFFFFD000)
private val GoogleText = Color(0xFFF1F3F4)
private val GoogleMuted = Color(0xFFADB2BA)
private val GoogleBlue = Color(0xFF72A9FF)

enum class ForecastMode { WEATHER, RAIN, WIND }

@Composable
fun WeatherScreen(
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
        var searchOpen by rememberSaveable { mutableStateOf(false) }
        var detailsOpen by rememberSaveable { mutableStateOf(false) }
        var forecastMode by rememberSaveable { mutableStateOf(ForecastMode.WEATHER) }

        val background = if (state.darkMode) GoogleBg else MaterialTheme.colorScheme.background
        val mainText = if (state.darkMode) GoogleText else MaterialTheme.colorScheme.onBackground
        val muted = if (state.darkMode) GoogleMuted else MaterialTheme.colorScheme.onSurfaceVariant

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                item {
                    HeaderBar(
                        state = state,
                        searchOpen = searchOpen,
                        onSearchToggle = {
                            searchOpen = !searchOpen
                            if (!searchOpen) onClearSearch()
                        },
                        onLocate = onLocate,
                        onRefresh = onRefresh,
                        textColor = mainText,
                        mutedColor = muted
                    )
                }

                if (searchOpen) {
                    item {
                        SearchArea(
                            state = state,
                            onSearchChange = onSearchChange,
                            onClearSearch = onClearSearch,
                            onSearchResult = { result ->
                                onSearchResult(result)
                                searchOpen = false
                            }
                        )
                    }
                }

                if (!state.errorMessage.isNullOrBlank()) {
                    item {
                        ErrorStrip(state.errorMessage, onRefresh)
                    }
                }

                if (state.isLoading && state.weather == null) {
                    item { LoadingGoogleCard() }
                } else {
                    state.weather?.let { bundle ->
                        item {
                            MainWeatherOverview(
                                bundle = bundle,
                                refreshing = state.isRefreshing,
                                isFavorite = isFavorite,
                                onToggleFavorite = onToggleFavorite,
                                textColor = mainText,
                                mutedColor = muted
                            )
                        }

                        item {
                            ForecastTabs(
                                selected = forecastMode,
                                onSelect = { forecastMode = it },
                                textColor = mainText,
                                mutedColor = muted
                            )
                        }

                        item {
                            HourlyGraph(
                                bundle = bundle,
                                mode = forecastMode,
                                textColor = mainText,
                                mutedColor = muted
                            )
                        }

                        item {
                            DailyForecastStrip(
                                bundle = bundle,
                                textColor = mainText,
                                mutedColor = muted
                            )
                        }

                        item {
                            MoreDetailsSection(
                                bundle = bundle,
                                expanded = detailsOpen,
                                onToggle = { detailsOpen = !detailsOpen },
                                state = state,
                                onToggleTheme = onToggleTheme,
                                textColor = mainText,
                                mutedColor = muted
                            )
                        }

                        item {
                            QuickCitiesStrip(
                                selected = state.selectedLocation,
                                favorites = state.favorites,
                                onSelectLocation = onSelectLocation,
                                textColor = mainText,
                                mutedColor = muted
                            )
                        }

                        item {
                            Text(
                                text = "داده‌های هواشناسی: Open-Meteo",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 22.dp, bottom = 12.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodySmall,
                                color = muted.copy(alpha = .7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderBar(
    state: WeatherUiState,
    searchOpen: Boolean,
    onSearchToggle: () -> Unit,
    onLocate: () -> Unit,
    onRefresh: () -> Unit,
    textColor: Color,
    mutedColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Rounded.LocationOn,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(21.dp)
        )
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = if (state.selectedLocation.province.isBlank()) {
                    state.selectedLocation.name
                } else {
                    "${state.selectedLocation.name}، ${state.selectedLocation.province}"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "ایران · انتخاب منطقه",
                style = MaterialTheme.typography.bodySmall,
                color = GoogleBlue
            )
        }
        CompactAction(Icons.Rounded.Search, if (searchOpen) "بستن جستجو" else "جستجو", onSearchToggle, textColor)
        CompactAction(Icons.Rounded.MyLocation, "موقعیت من", onLocate, textColor)
        CompactAction(Icons.Rounded.Refresh, "به‌روزرسانی", onRefresh, textColor)
        Icon(
            Icons.Rounded.MoreVert,
            contentDescription = null,
            tint = mutedColor,
            modifier = Modifier.size(21.dp)
        )
    }
}

@Composable
private fun CompactAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    tint: Color
) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        Icon(icon, description, tint = tint.copy(alpha = .84f), modifier = Modifier.size(19.dp))
    }
}

@Composable
private fun SearchArea(
    state: WeatherUiState,
    onSearchChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSearchResult: (CitySearchResult) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("جستجوی شهر در ایران؛ مثلاً رامسر، رشت، تهران…") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(onClick = onClearSearch) {
                        Icon(Icons.Rounded.Close, "پاک کردن")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(18.dp)
        )

        if (state.searchQuery.trim().length >= 2) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 7.dp),
                colors = CardDefaults.cardColors(containerColor = GooglePanel),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = .06f))
            ) {
                Column(Modifier.padding(7.dp)) {
                    if (state.isSearching) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp, color = GoogleGold)
                            Spacer(Modifier.width(8.dp))
                            Text("در حال جستجو…", color = GoogleMuted)
                        }
                    } else if (state.searchResults.isEmpty()) {
                        Text("شهری پیدا نشد.", modifier = Modifier.padding(12.dp), color = GoogleMuted)
                    } else {
                        state.searchResults.take(7).forEach { result ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(13.dp))
                                    .clickable { onSearchResult(result) }
                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.LocationOn, null, tint = GoogleBlue, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(result.name, color = GoogleText, style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.width(7.dp))
                                Text(result.province.ifBlank { "ایران" }, color = GoogleMuted, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MainWeatherOverview(
    bundle: WeatherBundle,
    refreshing: Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    textColor: Color,
    mutedColor: Color
) {
    val current = bundle.current
    val today = bundle.daily.firstOrNull()
    val dayAndTime = "${formatPersianDate(current.time).substringBefore('،')} ${formatTime(current.time)}"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(.9f),
                horizontalAlignment = Alignment.Start
            ) {
                Text("آب و هوا", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(5.dp))
                Text(dayAndTime, color = mutedColor, fontSize = 16.sp)
                Spacer(Modifier.height(3.dp))
                Text(weatherDescription(current.weatherCode), color = mutedColor, fontSize = 16.sp)
            }

            Row(
                modifier = Modifier.weight(1.35f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = current.temperature.fa(),
                            color = textColor,
                            fontFamily = Vazirmatn,
                            fontWeight = FontWeight.Light,
                            fontSize = 64.sp,
                            lineHeight = 68.sp
                        )
                        Text("°C", color = textColor, fontSize = 22.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                    Text(
                        text = "°C  |  °F",
                        color = mutedColor,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(Modifier.width(8.dp))
                WeatherArtwork(
                    kind = ir.havayeiran.weather.data.weatherKind(current.weatherCode),
                    isDay = current.isDay,
                    modifier = Modifier.size(88.dp)
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text("بارش: ${today?.precipitationProbability?.fa() ?: "—"}٪", color = mutedColor, style = MaterialTheme.typography.bodyMedium)
                Text("رطوبت: ${current.humidity.fa()}٪", color = mutedColor, style = MaterialTheme.typography.bodyMedium)
                Text("باد: ${current.windSpeed.fa()} km/h", color = mutedColor, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.width(16.dp))
            IconButton(onClick = onToggleFavorite, modifier = Modifier.size(38.dp)) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "علاقه‌مندی",
                    tint = if (isFavorite) Color(0xFFFF758E) else mutedColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (refreshing) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = GoogleGold)
            }
        }
    }
}

@Composable
private fun ForecastTabs(
    selected: ForecastMode,
    onSelect: (ForecastMode) -> Unit,
    textColor: Color,
    mutedColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.End
    ) {
        ForecastTab("هوا", ForecastMode.WEATHER, selected, onSelect, textColor, mutedColor)
        ForecastTab("بارش", ForecastMode.RAIN, selected, onSelect, textColor, mutedColor)
        ForecastTab("باد", ForecastMode.WIND, selected, onSelect, textColor, mutedColor)
    }
}

@Composable
private fun ForecastTab(
    label: String,
    mode: ForecastMode,
    selected: ForecastMode,
    onSelect: (ForecastMode) -> Unit,
    textColor: Color,
    mutedColor: Color
) {
    Column(
        modifier = Modifier
            .clickable { onSelect(mode) }
            .padding(horizontal = 11.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            color = if (selected == mode) textColor else mutedColor,
            fontWeight = if (selected == mode) FontWeight.Bold else FontWeight.Normal,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(7.dp))
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(3.dp)
                .background(if (selected == mode) GoogleGold else Color.Transparent)
        )
    }
}

@Composable
private fun HourlyGraph(
    bundle: WeatherBundle,
    mode: ForecastMode,
    textColor: Color,
    mutedColor: Color
) {
    val upcoming = bundle.hourly.filter { it.time >= bundle.current.time.take(13) + ":00" }
    val spaced = remember(upcoming) {
        val source = if (upcoming.size >= 9) upcoming else bundle.hourly.take(9)
        (0 until 9).mapNotNull { i -> source.getOrNull(i * 3) ?: source.getOrNull(i) }.distinctBy { it.time }.take(9)
    }
    if (spaced.isEmpty()) return

    val values = spaced.map {
        when (mode) {
            ForecastMode.WEATHER -> it.temperature
            ForecastMode.RAIN -> it.precipitationProbability.toDouble()
            ForecastMode.WIND -> it.windSpeed
        }
    }

    val suffix = when (mode) {
        ForecastMode.WEATHER -> "°"
        ForecastMode.RAIN -> "٪"
        ForecastMode.WIND -> ""
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 28.dp, bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            values.forEach { value ->
                Text(
                    text = "${value.fa()}$suffix",
                    modifier = Modifier.weight(1f),
                    color = textColor.copy(alpha = .78f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            if (values.size < 2) return@Canvas
            val rawMin = values.minOrNull() ?: 0.0
            val rawMax = values.maxOrNull() ?: 1.0
            val minValue = when (mode) {
                ForecastMode.RAIN -> 0.0
                ForecastMode.WIND -> 0.0
                ForecastMode.WEATHER -> rawMin - 2.0
            }
            val maxValue = when (mode) {
                ForecastMode.RAIN -> 100.0
                ForecastMode.WIND -> (rawMax + 5.0).coerceAtLeast(10.0)
                ForecastMode.WEATHER -> rawMax + 2.0
            }
            val span = (maxValue - minValue).coerceAtLeast(1.0)
            val points = values.mapIndexed { index, value ->
                val x = index * size.width / (values.lastIndex.coerceAtLeast(1))
                val normalized = ((value - minValue) / span).toFloat().coerceIn(0f, 1f)
                val y = size.height - normalized * (size.height * .78f) - size.height * .07f
                Offset(x, y)
            }

            val area = Path().apply {
                moveTo(points.first().x, size.height)
                lineTo(points.first().x, points.first().y)
                for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
                lineTo(points.last().x, size.height)
                close()
            }
            val line = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
            }

            drawPath(
                path = area,
                brush = Brush.verticalGradient(
                    colors = listOf(GoogleGold.copy(alpha = .31f), GoogleGold.copy(alpha = .08f)),
                    startY = 0f,
                    endY = size.height
                )
            )
            drawPath(
                path = line,
                color = GoogleGold,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
            points.forEach { point ->
                drawCircle(color = GoogleGold, radius = 2.7.dp.toPx(), center = point)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            spaced.forEachIndexed { index, hour ->
                Text(
                    text = if (index == 0) "اکنون" else formatTime(hour.time),
                    modifier = Modifier.weight(1f),
                    color = mutedColor,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun DailyForecastStrip(
    bundle: WeatherBundle,
    textColor: Color,
    mutedColor: Color
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        items(bundle.daily.take(8)) { day ->
            val index = bundle.daily.indexOf(day)
            val active = index == 0
            Column(
                modifier = Modifier
                    .width(92.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (active) GooglePanelSelected else Color.Transparent)
                    .border(
                        1.dp,
                        if (active) Color.White.copy(alpha = .035f) else Color.Transparent,
                        RoundedCornerShape(14.dp)
                    )
                    .padding(vertical = 12.dp, horizontal = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    forecastDayName(day.date, index),
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                WeatherGlyph(day.weatherCode, modifier = Modifier.size(48.dp).padding(top = 4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${day.maxTemperature.fa()}°", color = textColor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(5.dp))
                    Text("${day.minTemperature.fa()}°", color = mutedColor, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun MoreDetailsSection(
    bundle: WeatherBundle,
    expanded: Boolean,
    onToggle: () -> Unit,
    state: WeatherUiState,
    onToggleTheme: () -> Unit,
    textColor: Color,
    mutedColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable { onToggle() }
                .padding(horizontal = 9.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("جزئیات بیشتر", modifier = Modifier.weight(1f), color = textColor, fontWeight = FontWeight.Medium)
            Icon(
                if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = mutedColor
            )
        }

        if (expanded) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniMetric("رطوبت", "${bundle.current.humidity.fa()}٪", Modifier.weight(1f), textColor, mutedColor)
                MiniMetric("فشار", "${bundle.current.pressure.fa()} hPa", Modifier.weight(1f), textColor, mutedColor)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniMetric("دید افقی", "${bundle.current.visibilityKm.fa(1)} km", Modifier.weight(1f), textColor, mutedColor)
                MiniMetric("جهت باد", compassLabel(bundle.current.windDirection), Modifier.weight(1f), textColor, mutedColor)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val today = bundle.daily.firstOrNull()
                MiniMetric("UV", "${today?.uvIndex?.fa(1) ?: "—"} · ${uvLabel(today?.uvIndex ?: 0.0)}", Modifier.weight(1f), textColor, mutedColor)
                MiniMetric("تندباد", "${bundle.current.windGust.fa()} km/h", Modifier.weight(1f), textColor, mutedColor)
            }

            bundle.airQuality?.let { aqi ->
                Spacer(Modifier.height(8.dp))
                MiniMetric(
                    "کیفیت هوا",
                    "AQI ${aqi.usAqi.fa()} · ${aqiLabel(aqi.usAqi)}",
                    Modifier.fillMaxWidth(),
                    textColor,
                    mutedColor,
                    "PM2.5 ${aqi.pm25.fa(1)}  ·  PM10 ${aqi.pm10.fa(1)}"
                )
            }

            bundle.marine?.let { marine ->
                if (marine.seaSurfaceTemperature != null || marine.waveHeight != null) {
                    Spacer(Modifier.height(8.dp))
                    MiniMetric(
                        "دریای خزر",
                        "دمای آب ${marine.seaSurfaceTemperature?.fa(1) ?: "—"}°  ·  موج ${marine.waveHeight?.fa(1) ?: "—"} m",
                        Modifier.fillMaxWidth(),
                        textColor,
                        mutedColor
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = GooglePanel.copy(alpha = .72f)),
                shape = RoundedCornerShape(17.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("ظاهر برنامه", color = textColor, style = MaterialTheme.typography.titleMedium)
                        Text(if (state.darkMode) "حالت تیره فعال است" else "حالت روشن فعال است", color = mutedColor, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            if (state.darkMode) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                            contentDescription = "تغییر تم",
                            tint = GoogleGold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniMetric(
    label: String,
    value: String,
    modifier: Modifier,
    textColor: Color,
    mutedColor: Color,
    note: String? = null
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = GooglePanel.copy(alpha = .72f)),
        shape = RoundedCornerShape(17.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .035f))
    ) {
        Column(Modifier.padding(13.dp)) {
            Text(label, color = mutedColor, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(4.dp))
            Text(value, color = textColor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            if (!note.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(note, color = mutedColor, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun QuickCitiesStrip(
    selected: WeatherLocation,
    favorites: List<WeatherLocation>,
    onSelectLocation: (WeatherLocation) -> Unit,
    textColor: Color,
    mutedColor: Color
) {
    val cities = (favorites + QuickCities).distinctBy { "${it.latitude}-${it.longitude}" }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text("شهرهای منتخب", color = textColor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            cities.forEach { city ->
                val active = near(city, selected)
                val favorite = favorites.any { near(it, city) }
                CityChip(city, active, favorite, { onSelectLocation(city) })
            }
        }
    }
}

@Composable
private fun ErrorStrip(message: String, onRefresh: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF3A2929)),
        shape = RoundedCornerShape(15.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, modifier = Modifier.weight(1f), color = Color(0xFFFFD1D1), style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onRefresh) { Text("تلاش دوباره", color = GoogleGold) }
        }
    }
}

@Composable
private fun LoadingGoogleCard() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 90.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = GoogleGold, strokeWidth = 3.dp)
        Spacer(Modifier.height(12.dp))
        Text("در حال دریافت هوای رامسر…", color = GoogleMuted)
    }
}

private fun near(a: WeatherLocation, b: WeatherLocation): Boolean =
    abs(a.latitude - b.latitude) < .01 && abs(a.longitude - b.longitude) < .01
