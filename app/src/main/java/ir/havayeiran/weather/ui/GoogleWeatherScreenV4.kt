package ir.havayeiran.weather.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.havayeiran.weather.data.CitySearchResult
import ir.havayeiran.weather.data.DailyWeather
import ir.havayeiran.weather.data.HourlyWeather
import ir.havayeiran.weather.data.WeatherKind
import ir.havayeiran.weather.data.weatherDescription
import ir.havayeiran.weather.data.weatherKind
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

private data class V4Palette(
    val background: Color,
    val panel: Color,
    val selected: Color,
    val text: Color,
    val muted: Color,
    val outline: Color,
    val chart: Color
)

private fun v4Palette(dark: Boolean) = if (dark) {
    V4Palette(
        background = Color(0xFF202124),
        panel = Color(0xFF292A2D),
        selected = Color(0xFF303134),
        text = Color(0xFFF1F3F4),
        muted = Color(0xFF9AA0A6),
        outline = Color(0xFF3C4043),
        chart = Color(0xFFFFC107)
    )
} else {
    V4Palette(
        background = Color(0xFFF7F9FC),
        panel = Color.White,
        selected = Color(0xFFE8F0FE),
        text = Color(0xFF202124),
        muted = Color(0xFF5F6368),
        outline = Color(0xFFDADCE0),
        chart = Color(0xFFF9AB00)
    )
}

private val V4Sun = Color(0xFFFFA726)
private val V4SunDeep = Color(0xFFFF8F00)
private val V4Rain = Color(0xFF1A73E8)
private val V4RainLight = Color(0xFF64B5F6)
private val V4Cloud = Color(0xFFF1F3F4)
private val V4CloudShade = Color(0xFFBDC1C6)
private val V4Storm = Color(0xFF8F969F)
private val V4Lightning = Color(0xFFFFD54F)

private enum class V4ForecastMode { WEATHER, RAIN, WIND }

@Composable
fun GoogleWeatherScreenV4(
    state: WeatherUiState,
    onSearchChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSearchResult: (CitySearchResult) -> Unit,
    onRefresh: () -> Unit,
    onLocate: () -> Unit,
    onToggleTheme: () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val palette = remember(state.darkMode) { v4Palette(state.darkMode) }
        var searchOpen by rememberSaveable { mutableStateOf(false) }
        var selectedDayIndex by remember(state.selectedLocation.latitude, state.selectedLocation.longitude) { mutableStateOf(0) }
        var mode by rememberSaveable { mutableStateOf(V4ForecastMode.WEATHER) }

        val bundle = state.weather
        val safeIndex = selectedDayIndex.coerceIn(0, max(bundle?.daily?.lastIndex ?: 0, 0))
        val day = bundle?.daily?.getOrNull(safeIndex)
        val hours = remember(bundle?.hourly, day?.date) {
            val date = day?.date
            if (bundle == null || date == null) emptyList() else bundle.hourly.filter { it.time.startsWith(date) }
        }

        Box(Modifier.fillMaxSize().background(palette.background)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
                contentPadding = PaddingValues(bottom = 26.dp)
            ) {
                item {
                    V4Header(
                        state = state,
                        palette = palette,
                        searchOpen = searchOpen,
                        onSearch = {
                            searchOpen = !searchOpen
                            if (!searchOpen) onClearSearch()
                        },
                        onLocate = onLocate,
                        onRefresh = onRefresh,
                        onToggleTheme = onToggleTheme
                    )
                }

                if (searchOpen) {
                    item {
                        V4Search(
                            state = state,
                            palette = palette,
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
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 7.dp),
                            colors = CardDefaults.cardColors(containerColor = if (state.darkMode) Color(0xFF3A2929) else Color(0xFFFFECEC)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                state.errorMessage.orEmpty(),
                                Modifier.padding(12.dp),
                                color = if (state.darkMode) Color(0xFFFFD3D3) else Color(0xFF8C1D18),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                if (state.isLoading && bundle == null) {
                    item {
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = 88.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = palette.chart, strokeWidth = 3.dp)
                            Spacer(Modifier.height(12.dp))
                            Text("در حال دریافت اطلاعات هوا…", color = palette.muted)
                        }
                    }
                } else if (bundle != null && day != null) {
                    item { V4Summary(state, day, safeIndex, palette) }
                    item { V4Tabs(mode, palette) { mode = it } }
                    item { V4Chart(hours, mode, palette) }
                    item {
                        V4Days(
                            days = bundle.daily.take(8),
                            selectedIndex = safeIndex,
                            palette = palette,
                            onSelect = { selectedDayIndex = it }
                        )
                    }
                    item { V4Hourly(day, hours, palette) }
                    item {
                        Text(
                            "داده‌های هواشناسی: Open-Meteo",
                            Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 4.dp),
                            color = palette.muted.copy(alpha = .72f),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                item { V4SocialFooter(palette, state.darkMode, onToggleTheme) }
            }
        }
    }
}

@Composable
private fun V4Header(
    state: WeatherUiState,
    palette: V4Palette,
    searchOpen: Boolean,
    onSearch: () -> Unit,
    onLocate: () -> Unit,
    onRefresh: () -> Unit,
    onToggleTheme: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.LocationOn, null, tint = palette.text, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(5.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (state.selectedLocation.province.isBlank()) state.selectedLocation.name
                else "${state.selectedLocation.name}، استان ${state.selectedLocation.province}",
                color = palette.text,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text("ایران · انتخاب منطقه", color = Color(0xFF4285F4), style = MaterialTheme.typography.bodySmall)
        }
        V4HeaderIcon(if (searchOpen) Icons.Rounded.Close else Icons.Rounded.Search, palette.text, onSearch)
        V4HeaderIcon(Icons.Rounded.MyLocation, palette.text, onLocate)
        V4HeaderIcon(Icons.Rounded.Refresh, palette.text, onRefresh)
        V4HeaderIcon(if (state.darkMode) Icons.Rounded.LightMode else Icons.Rounded.DarkMode, palette.muted, onToggleTheme)
    }
}

@Composable
private fun V4HeaderIcon(
    image: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        Icon(image, null, tint = tint, modifier = Modifier.size(19.dp))
    }
}

@Composable
private fun V4Search(
    state: WeatherUiState,
    palette: V4Palette,
    onSearchChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSelect: (CitySearchResult) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp)) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("جستجوی شهر؛ مثلاً رامسر، رشت یا تهران") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(onClick = onClearSearch) { Icon(Icons.Rounded.Close, null) }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(18.dp)
        )
        if (state.searchQuery.trim().length >= 2) {
            Card(
                Modifier.fillMaxWidth().padding(top = 6.dp),
                colors = CardDefaults.cardColors(containerColor = palette.panel),
                border = BorderStroke(1.dp, palette.outline.copy(alpha = .7f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(6.dp)) {
                    if (state.isSearching) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.Center) {
                            CircularProgressIndicator(Modifier.size(18.dp), color = palette.chart, strokeWidth = 2.dp)
                        }
                    } else if (state.searchResults.isEmpty()) {
                        Text("شهری پیدا نشد.", Modifier.padding(12.dp), color = palette.muted)
                    } else {
                        state.searchResults.take(7).forEach { result ->
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onSelect(result) }.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.LocationOn, null, tint = Color(0xFF4285F4), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(result.name, color = palette.text, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(6.dp))
                                Text(result.province.ifBlank { "ایران" }, color = palette.muted, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun V4Summary(state: WeatherUiState, day: DailyWeather, dayIndex: Int, palette: V4Palette) {
    val bundle = state.weather ?: return
    val isToday = dayIndex == 0
    val code = if (isToday) bundle.current.weatherCode else day.weatherCode
    val temp = if (isToday) bundle.current.temperature else day.maxTemperature
    val isDay = if (isToday) bundle.current.isDay else true

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(.78f)) {
            Text("آب‌وهوا", color = palette.text, fontSize = 20.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(3.dp))
            Text(forecastDayName(day.date, dayIndex), color = palette.muted, fontSize = 15.sp)
            Text(weatherDescription(code), color = palette.muted, fontSize = 16.sp)
        }
        Column(Modifier.weight(1.22f), horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(temp.fa(), color = palette.text, fontFamily = Vazirmatn, fontSize = 66.sp, lineHeight = 66.sp, fontWeight = FontWeight.Light)
                        Text("°C", color = palette.text, fontSize = 20.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                    Text("°C  |  °F", color = palette.muted, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.width(8.dp))
                V4WeatherIcon(code, isDay, palette.background, Modifier.size(106.dp))
            }
            Text("بارش: ${day.precipitationProbability.fa()}٪", color = palette.muted, style = MaterialTheme.typography.bodyMedium)
            if (isToday) Text("رطوبت: ${bundle.current.humidity.fa()}٪", color = palette.muted, style = MaterialTheme.typography.bodyMedium)
            Text("باد: ${day.maxWindSpeed.fa()} km/h", color = palette.muted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun V4Tabs(selected: V4ForecastMode, palette: V4Palette, onSelect: (V4ForecastMode) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalArrangement = Arrangement.End) {
        V4Tab("هوا", V4ForecastMode.WEATHER, selected, palette, onSelect)
        V4Tab("بارش", V4ForecastMode.RAIN, selected, palette, onSelect)
        V4Tab("باد", V4ForecastMode.WIND, selected, palette, onSelect)
    }
}

@Composable
private fun V4Tab(
    title: String,
    value: V4ForecastMode,
    selected: V4ForecastMode,
    palette: V4Palette,
    onSelect: (V4ForecastMode) -> Unit
) {
    Column(
        Modifier.clickable { onSelect(value) }.padding(horizontal = 13.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = if (selected == value) palette.text else palette.muted, fontWeight = if (selected == value) FontWeight.Bold else FontWeight.Normal)
        Spacer(Modifier.height(7.dp))
        Box(Modifier.width(28.dp).height(3.dp).background(if (selected == value) palette.chart else Color.Transparent))
    }
}

@Composable
private fun V4Chart(hours: List<HourlyWeather>, mode: V4ForecastMode, palette: V4Palette) {
    if (hours.isEmpty()) return
    val samples = remember(hours, mode) {
        val indexes = listOf(1, 4, 7, 10, 13, 16, 19, 22).filter { it < hours.size }
        if (indexes.isEmpty()) hours.take(8) else indexes.map { hours[it] }
    }
    val values = remember(samples, mode) {
        samples.map {
            when (mode) {
                V4ForecastMode.WEATHER -> it.temperature
                V4ForecastMode.RAIN -> it.precipitationProbability.toDouble()
                V4ForecastMode.WIND -> it.windSpeed
            }
        }
    }
    val suffix = when (mode) {
        V4ForecastMode.WEATHER -> "°"
        V4ForecastMode.RAIN -> "٪"
        V4ForecastMode.WIND -> ""
    }

    Column(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 7.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
            values.forEach { value ->
                Text("${value.fa()}$suffix", Modifier.weight(1f), color = palette.text.copy(alpha = .76f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
            }
        }
        Canvas(Modifier.fillMaxWidth().height(112.dp).padding(horizontal = 18.dp, vertical = 7.dp)) {
            if (values.size < 2) return@Canvas
            val rawMin = values.minOrNull() ?: 0.0
            val rawMax = values.maxOrNull() ?: 1.0
            val minValue = if (mode == V4ForecastMode.WEATHER) rawMin - 2.0 else 0.0
            val maxValue = when (mode) {
                V4ForecastMode.WEATHER -> rawMax + 2.0
                V4ForecastMode.RAIN -> 100.0
                V4ForecastMode.WIND -> (rawMax + 5.0).coerceAtLeast(10.0)
            }
            val span = (maxValue - minValue).coerceAtLeast(1.0)
            val points = values.mapIndexed { index, value ->
                val x = index * size.width / values.lastIndex
                val normalized = ((value - minValue) / span).toFloat().coerceIn(0f, 1f)
                Offset(x, size.height - normalized * size.height * .68f - size.height * .10f)
            }
            val line = v4SmoothLine(points)
            val area = v4SmoothArea(points, size.height)
            drawPath(area, Brush.verticalGradient(listOf(palette.chart.copy(alpha = .30f), palette.chart.copy(alpha = .035f))))
            drawPath(line, palette.chart, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            samples.forEach { hour ->
                Text(formatTime(hour.time), Modifier.weight(1f), color = palette.muted, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun V4Days(days: List<DailyWeather>, selectedIndex: Int, palette: V4Palette, onSelect: (Int) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        contentPadding = PaddingValues(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        itemsIndexed(days, key = { _, item -> item.date }) { index, item ->
            val active = index == selectedIndex
            Column(
                Modifier.width(102.dp).clip(RoundedCornerShape(16.dp))
                    .background(if (active) palette.selected else Color.Transparent)
                    .clickable { onSelect(index) }.padding(horizontal = 5.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(forecastDayName(item.date, index), color = palette.text, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                V4WeatherIcon(item.weatherCode, true, palette.background, Modifier.size(66.dp))
                Text(weatherDescription(item.weatherCode), color = palette.muted, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${item.minTemperature.fa()}°", color = palette.muted)
                    Spacer(Modifier.width(5.dp))
                    Text("${item.maxTemperature.fa()}°", color = palette.text, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun V4Hourly(day: DailyWeather, hours: List<HourlyWeather>, palette: V4Palette) {
    if (hours.isEmpty()) return
    Column(Modifier.fillMaxWidth().padding(top = 25.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("پیش‌بینی ساعتی ${shortPersianDate(day.date)}", Modifier.weight(1f), color = palette.text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("ساعت‌به‌ساعت", color = palette.muted, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            items(hours, key = { it.time }) { hour ->
                Card(
                    Modifier.width(94.dp),
                    colors = CardDefaults.cardColors(containerColor = palette.panel),
                    border = BorderStroke(1.dp, palette.outline.copy(alpha = .6f)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(Modifier.padding(horizontal = 5.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(formatTime(hour.time), color = palette.muted, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(2.dp))
                        V4WeatherIcon(hour.weatherCode, v4HourIsDay(hour.time), palette.panel, Modifier.size(58.dp))
                        Text("${hour.temperature.fa()}°", color = palette.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(weatherDescription(hour.weatherCode), color = palette.muted, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        if (hour.precipitationProbability > 0) {
                            Text("💧 ${hour.precipitationProbability.fa()}٪", color = V4Rain, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun V4SocialFooter(palette: V4Palette, darkMode: Boolean, onToggleTheme: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    var instagramExpanded by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 24.dp).animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("تنظیمات و ارتباط", color = palette.text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))

        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = palette.panel),
            border = BorderStroke(1.dp, palette.outline.copy(alpha = .7f)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("حالت نمایش", Modifier.weight(1f), color = palette.text, fontWeight = FontWeight.Medium)
                V4ThemeChip("روشن", !darkMode, palette, Icons.Rounded.LightMode) { if (darkMode) onToggleTheme() }
                Spacer(Modifier.width(6.dp))
                V4ThemeChip("تاریک", darkMode, palette, Icons.Rounded.DarkMode) { if (!darkMode) onToggleTheme() }
            }
        }

        Spacer(Modifier.height(12.dp))
        V4GradientButton(
            title = "ایستاگرام موبایل تینا",
            subtitle = if (instagramExpanded) "برای بستن دوباره لمس کنید" else "مشاهده صفحه‌های اینستاگرام",
            badge = "IG",
            gradient = listOf(Color(0xFF833AB4), Color(0xFFE1306C), Color(0xFFFCAF45))
        ) { instagramExpanded = !instagramExpanded }

        AnimatedVisibility(
            visible = instagramExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    V4InstagramButton("mobile.tina", Modifier.weight(1f)) { runCatching { uriHandler.openUri("https://www.instagram.com/mobile.tina/") } }
                    V4InstagramButton("mobile.tina2", Modifier.weight(1f)) { runCatching { uriHandler.openUri("https://www.instagram.com/mobile.tina2/") } }
                    V4InstagramButton("mobile.tinaa", Modifier.weight(1f)) { runCatching { uriHandler.openUri("https://www.instagram.com/mobile.tinaa/") } }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        V4GradientButton(
            title = "توسعه دهنده برنامه",
            subtitle = "Telegram · @vpn963",
            badge = "TG",
            gradient = listOf(Color(0xFF168ACD), Color(0xFF229ED9), Color(0xFF5BC0EB))
        ) { runCatching { uriHandler.openUri("https://t.me/vpn963") } }

        Spacer(Modifier.height(14.dp))
        Text("weather · Mobile Tina", color = palette.muted.copy(alpha = .72f), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun V4ThemeChip(
    title: String,
    selected: Boolean,
    palette: V4Palette,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(if (selected) palette.selected else Color.Transparent)
            .clickable(onClick = onClick).padding(horizontal = 9.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (selected) Color(0xFF4285F4) else palette.muted, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(title, color = if (selected) palette.text else palette.muted, style = MaterialTheme.typography.bodySmall, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun V4GradientButton(
    title: String,
    subtitle: String,
    badge: String,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(19.dp)).background(Brush.horizontalGradient(gradient))
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(42.dp).background(Color.White.copy(alpha = .18f), CircleShape), contentAlignment = Alignment.Center) {
            Text(badge, color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, color = Color.White.copy(alpha = .84f), style = MaterialTheme.typography.bodySmall)
        }
        Text("‹", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Light)
    }
}

@Composable
private fun V4InstagramButton(handle: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF6A33A8), Color(0xFFC13584), Color(0xFFE6683C))))
            .clickable(onClick = onClick).padding(horizontal = 4.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("@$handle", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, maxLines = 1)
    }
}

@Composable
private fun V4WeatherIcon(code: Int, isDay: Boolean, background: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        when (weatherKind(code)) {
            WeatherKind.CLEAR -> v4DrawSunOrMoon(isDay, background)
            WeatherKind.PARTLY_CLOUDY -> {
                v4DrawSmallSunOrMoon(isDay, background)
                v4DrawCloud(Offset(size.width * .58f, size.height * .61f), size.width * .66f, false)
            }
            WeatherKind.CLOUDY -> {
                v4DrawCloud(Offset(size.width * .42f, size.height * .45f), size.width * .55f, true)
                v4DrawCloud(Offset(size.width * .58f, size.height * .61f), size.width * .68f, false)
            }
            WeatherKind.FOG -> {
                v4DrawCloud(Offset(size.width * .52f, size.height * .43f), size.width * .66f, false)
                repeat(3) { index ->
                    val y = size.height * (.70f + index * .08f)
                    drawLine(V4RainLight.copy(alpha = .78f), Offset(size.width * .18f, y), Offset(size.width * .82f, y), 4f, cap = StrokeCap.Round)
                }
            }
            WeatherKind.RAIN -> {
                v4DrawCloud(Offset(size.width * .52f, size.height * .41f), size.width * .70f, false)
                v4DrawDrop(.32f, .78f, .058f); v4DrawDrop(.51f, .83f, .070f); v4DrawDrop(.70f, .77f, .058f)
            }
            WeatherKind.STORM -> {
                v4DrawCloud(Offset(size.width * .52f, size.height * .39f), size.width * .70f, true)
                v4DrawDrop(.30f, .78f, .05f); v4DrawDrop(.70f, .78f, .05f)
                val bolt = Path().apply {
                    moveTo(size.width * .51f, size.height * .54f); lineTo(size.width * .40f, size.height * .73f)
                    lineTo(size.width * .50f, size.height * .73f); lineTo(size.width * .43f, size.height * .92f)
                    lineTo(size.width * .65f, size.height * .66f); lineTo(size.width * .54f, size.height * .66f); close()
                }
                drawPath(bolt, V4Lightning)
            }
            WeatherKind.SNOW -> {
                v4DrawCloud(Offset(size.width * .52f, size.height * .41f), size.width * .70f, false)
                listOf(.32f, .51f, .70f).forEachIndexed { index, x ->
                    val center = Offset(size.width * x, size.height * (.78f + (index % 2) * .04f))
                    drawCircle(V4RainLight, size.width * .035f, center)
                    drawCircle(Color.White, size.width * .016f, center)
                }
            }
        }
    }
}

private fun DrawScope.v4DrawSunOrMoon(isDay: Boolean, background: Color) {
    if (!isDay) {
        val c = Offset(size.width * .53f, size.height * .48f); val r = size.minDimension * .21f
        drawCircle(Color(0xFFDCE6FF), r, c); drawCircle(background, r * .82f, Offset(c.x - r * .40f, c.y - r * .20f)); return
    }
    val c = Offset(size.width * .50f, size.height * .48f); val r = size.minDimension * .19f
    repeat(8) { index ->
        val angle = Math.toRadians(index * 45.0)
        drawLine(V4Sun, Offset(c.x + cos(angle).toFloat() * r * 1.45f, c.y + sin(angle).toFloat() * r * 1.45f), Offset(c.x + cos(angle).toFloat() * r * 2.15f, c.y + sin(angle).toFloat() * r * 2.15f), r * .28f, cap = StrokeCap.Round)
    }
    drawCircle(V4Sun.copy(alpha = .18f), r * 1.55f, c)
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFE082), V4Sun, V4SunDeep), c, r * 1.2f), r, c)
}

private fun DrawScope.v4DrawSmallSunOrMoon(isDay: Boolean, background: Color) {
    val c = Offset(size.width * .35f, size.height * .34f); val r = size.minDimension * .14f
    if (!isDay) {
        drawCircle(Color(0xFFDCE6FF), r, c); drawCircle(background, r * .80f, Offset(c.x - r * .38f, c.y - r * .18f)); return
    }
    repeat(8) { index ->
        val angle = Math.toRadians(index * 45.0)
        drawLine(V4Sun, Offset(c.x + cos(angle).toFloat() * r * 1.35f, c.y + sin(angle).toFloat() * r * 1.35f), Offset(c.x + cos(angle).toFloat() * r * 1.95f, c.y + sin(angle).toFloat() * r * 1.95f), r * .24f, cap = StrokeCap.Round)
    }
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFE082), V4Sun, V4SunDeep), c, r * 1.2f), r, c)
}

private fun DrawScope.v4DrawCloud(center: Offset, width: Float, dark: Boolean) {
    val front = if (dark) V4Storm else V4Cloud; val shadow = if (dark) Color(0xFF747B84) else V4CloudShade; val height = width * .40f
    drawRoundRect(shadow, Offset(center.x - width * .47f, center.y + height * .07f), Size(width * .94f, height * .62f), CornerRadius(height * .34f))
    drawRoundRect(front, Offset(center.x - width * .50f, center.y - height * .04f), Size(width, height * .62f), CornerRadius(height * .34f))
    drawCircle(front, width * .19f, Offset(center.x - width * .22f, center.y - height * .06f))
    drawCircle(front, width * .25f, Offset(center.x + width * .05f, center.y - height * .19f))
    drawCircle(front, width * .17f, Offset(center.x + width * .29f, center.y - height * .01f))
    drawArc(Color.White.copy(alpha = if (dark) .14f else .34f), 190f, 88f, false, Offset(center.x - width * .37f, center.y - height * .31f), Size(width * .59f, height * .70f), style = Stroke(width = 2.4f, cap = StrokeCap.Round))
}

private fun DrawScope.v4DrawDrop(x: Float, y: Float, fraction: Float) {
    val cx = size.width * x; val cy = size.height * y; val r = size.width * fraction
    val path = Path().apply {
        moveTo(cx, cy - r * 1.55f)
        cubicTo(cx - r * 1.05f, cy - r * .25f, cx - r * 1.05f, cy + r * .90f, cx, cy + r * 1.10f)
        cubicTo(cx + r * 1.05f, cy + r * .90f, cx + r * 1.05f, cy - r * .25f, cx, cy - r * 1.55f)
        close()
    }
    drawPath(path, V4Rain)
    drawCircle(V4RainLight.copy(alpha = .86f), r * .28f, Offset(cx - r * .18f, cy - r * .43f))
}

private fun v4SmoothLine(points: List<Offset>) = Path().apply {
    if (points.isEmpty()) return@apply
    moveTo(points.first().x, points.first().y)
    for (index in 1 until points.size) {
        val p = points[index - 1]; val c = points[index]; val mx = (p.x + c.x) / 2f
        cubicTo(mx, p.y, mx, c.y, c.x, c.y)
    }
}

private fun v4SmoothArea(points: List<Offset>, bottom: Float) = Path().apply {
    if (points.isEmpty()) return@apply
    moveTo(points.first().x, bottom); lineTo(points.first().x, points.first().y)
    for (index in 1 until points.size) {
        val p = points[index - 1]; val c = points[index]; val mx = (p.x + c.x) / 2f
        cubicTo(mx, p.y, mx, c.y, c.x, c.y)
    }
    lineTo(points.last().x, bottom); close()
}

private fun v4HourIsDay(time: String): Boolean =
    (time.substringAfter('T', "12:00").take(2).toIntOrNull() ?: 12) in 7..18
