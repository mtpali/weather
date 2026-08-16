package ir.havayeiran.weather.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.ImageVector
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
import ir.havayeiran.weather.internal.EndpointCodec
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

private data class V6Palette(
    val bg: Color,
    val panel: Color,
    val selected: Color,
    val text: Color,
    val muted: Color,
    val outline: Color,
    val chart: Color,
    val cloud: Color,
    val cloudShade: Color,
    val storm: Color
)

private fun v6Palette(dark: Boolean) = if (dark) {
    V6Palette(
        bg = Color(0xFF202124), panel = Color(0xFF292A2D), selected = Color(0xFF303134),
        text = Color(0xFFF1F3F4), muted = Color(0xFFB7BBC1), outline = Color(0xFF3C4043),
        chart = Color(0xFFFFC400), cloud = Color(0xFFF1F3F4), cloudShade = Color(0xFFB7BDC5), storm = Color(0xFF7F8791)
    )
} else {
    V6Palette(
        bg = Color(0xFFF7F9FC), panel = Color(0xFFFFFFFF), selected = Color(0xFFE8F0FE),
        text = Color(0xFF17191C), muted = Color(0xFF4D535A), outline = Color(0xFFD2D7DE),
        chart = Color(0xFFF9AB00), cloud = Color(0xFFE1E6EC), cloudShade = Color(0xFF9FAAB6), storm = Color(0xFF65707C)
    )
}

private val V6Sun = Color(0xFFFFA726)
private val V6SunCore = Color(0xFFFFD54F)
private val V6SunDeep = Color(0xFFF57C00)
private val V6Rain = Color(0xFF1A73E8)
private val V6RainLight = Color(0xFF64B5F6)
private val V6Lightning = Color(0xFFFFD54F)
private enum class V6Mode { WEATHER, RAIN, WIND }

@Composable
fun GoogleWeatherScreenV6(
    state: WeatherUiState,
    onSearchChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSearchResult: (CitySearchResult) -> Unit,
    onRefresh: () -> Unit,
    onLocate: () -> Unit,
    onToggleTheme: () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val p = remember(state.darkMode) { v6Palette(state.darkMode) }
        var searchOpen by rememberSaveable { mutableStateOf(false) }
        var selectedDay by remember(state.selectedLocation.latitude, state.selectedLocation.longitude) { mutableStateOf(0) }
        var mode by rememberSaveable { mutableStateOf(V6Mode.WEATHER) }
        val bundle = state.weather
        val index = selectedDay.coerceIn(0, max(bundle?.daily?.lastIndex ?: 0, 0))
        val day = bundle?.daily?.getOrNull(index)
        val hours = remember(bundle?.hourly, day?.date) {
            val date = day?.date
            if (bundle == null || date == null) emptyList() else bundle.hourly.filter { it.time.startsWith(date) }
        }

        Box(Modifier.fillMaxSize().background(p.bg)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
                contentPadding = PaddingValues(bottom = 28.dp)
            ) {
                item {
                    V6Header(state, p, searchOpen, onSearch = {
                        searchOpen = !searchOpen
                        if (!searchOpen) onClearSearch()
                    }, onLocate = onLocate, onRefresh = onRefresh, onToggleTheme = onToggleTheme)
                }

                if (searchOpen) {
                    item {
                        V6Search(state, p, onSearchChange, onClearSearch) {
                            selectedDay = 0
                            searchOpen = false
                            onSearchResult(it)
                        }
                    }
                }

                if (!state.errorMessage.isNullOrBlank()) {
                    item {
                        Card(
                            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 7.dp),
                            colors = CardDefaults.cardColors(containerColor = if (state.darkMode) Color(0xFF3A2929) else Color(0xFFFFECEC)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                state.errorMessage.orEmpty(), Modifier.padding(12.dp),
                                color = if (state.darkMode) Color(0xFFFFD3D3) else Color(0xFF8C1D18)
                            )
                        }
                    }
                }

                if (state.isLoading && bundle == null) {
                    item {
                        Column(Modifier.fillMaxWidth().padding(vertical = 90.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = p.chart, strokeWidth = 3.dp)
                            Spacer(Modifier.height(12.dp))
                            Text("در حال دریافت تازه‌ترین پیش‌بینی…", color = p.muted)
                        }
                    }
                } else if (bundle != null && day != null) {
                    val representativeCode = remember(day, hours) { v6RepresentativeCode(day, hours) }
                    item { V6Summary(state, day, index, representativeCode, p) }
                    item { V6Tabs(mode, p) { mode = it } }
                    item { V6Chart(hours, mode, p) }
                    item {
                        V6Days(
                            days = bundle.daily.take(8),
                            allHours = bundle.hourly,
                            selectedIndex = index,
                            p = p,
                            onSelect = { selectedDay = it }
                        )
                    }
                    item { V6Hourly(day, hours, p) }
                    item {
                        Text(
                            "پیش‌بینی مدل Best Match · Open-Meteo",
                            Modifier.fillMaxWidth().padding(top = 22.dp),
                            color = p.muted.copy(alpha = .8f),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                item { V6Footer(p, state.darkMode, onToggleTheme) }
            }
        }
    }
}

@Composable
private fun V6Header(
    state: WeatherUiState,
    p: V6Palette,
    searchOpen: Boolean,
    onSearch: () -> Unit,
    onLocate: () -> Unit,
    onRefresh: () -> Unit,
    onToggleTheme: () -> Unit
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.LocationOn, null, tint = p.text, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(5.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (state.selectedLocation.province.isBlank()) state.selectedLocation.name else "${state.selectedLocation.name}، استان ${state.selectedLocation.province}",
                color = p.text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text("ایران · انتخاب منطقه", color = Color(0xFF4285F4), style = MaterialTheme.typography.bodySmall)
        }
        V6IconButton(if (searchOpen) Icons.Rounded.Close else Icons.Rounded.Search, p.text, onSearch)
        V6IconButton(Icons.Rounded.MyLocation, p.text, onLocate)
        V6IconButton(Icons.Rounded.Refresh, p.text, onRefresh)
        V6IconButton(if (state.darkMode) Icons.Rounded.LightMode else Icons.Rounded.DarkMode, p.text, onToggleTheme)
    }
}

@Composable
private fun V6Search(
    state: WeatherUiState,
    p: V6Palette,
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
            trailingIcon = { if (state.searchQuery.isNotEmpty()) IconButton(onClick = onClearSearch) { Icon(Icons.Rounded.Close, null) } },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = p.text, unfocusedTextColor = p.text,
                focusedPlaceholderColor = p.muted, unfocusedPlaceholderColor = p.muted,
                focusedLeadingIconColor = p.text, unfocusedLeadingIconColor = p.muted,
                focusedTrailingIconColor = p.text, unfocusedTrailingIconColor = p.muted,
                focusedBorderColor = Color(0xFF4285F4), unfocusedBorderColor = p.outline,
                cursorColor = Color(0xFF4285F4), focusedContainerColor = p.panel, unfocusedContainerColor = p.panel
            ),
            singleLine = true,
            shape = RoundedCornerShape(18.dp)
        )
        if (state.searchQuery.trim().length >= 2) {
            Card(
                Modifier.fillMaxWidth().padding(top = 6.dp),
                colors = CardDefaults.cardColors(containerColor = p.panel),
                border = BorderStroke(1.dp, p.outline),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(6.dp)) {
                    when {
                        state.isSearching -> Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.Center) {
                            CircularProgressIndicator(Modifier.size(18.dp), color = p.chart, strokeWidth = 2.dp)
                        }
                        state.searchResults.isEmpty() -> Text("شهری پیدا نشد.", Modifier.padding(12.dp), color = p.muted)
                        else -> state.searchResults.take(7).forEach { result ->
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onSelect(result) }.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.LocationOn, null, tint = Color(0xFF4285F4), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(result.name, color = p.text, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(6.dp))
                                Text(result.province.ifBlank { "ایران" }, color = p.muted, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun V6Summary(state: WeatherUiState, day: DailyWeather, dayIndex: Int, representativeCode: Int, p: V6Palette) {
    val bundle = state.weather ?: return
    val today = dayIndex == 0
    val code = if (today) bundle.current.weatherCode else representativeCode
    val temp = if (today) bundle.current.temperature else day.maxTemperature
    val isDay = if (today) bundle.current.isDay else true

    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(.72f)) {
            Text("آب‌وهوا", color = p.text, fontSize = 20.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(3.dp))
            Text(forecastDayName(day.date, dayIndex), color = p.muted, fontSize = 15.sp)
            Text(weatherDescription(code), color = p.muted, fontSize = 16.sp)
        }
        Column(Modifier.weight(1.28f), horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 5.dp)) {
                    Text("بارش: ${day.precipitationProbability.fa()}٪", color = p.muted, style = MaterialTheme.typography.bodySmall)
                    if (today) Text("رطوبت: ${bundle.current.humidity.fa()}٪", color = p.muted, style = MaterialTheme.typography.bodySmall)
                    Text("باد: ${day.maxWindSpeed.fa()} km/h", color = p.muted, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(temp.fa(), color = p.text, fontFamily = Vazirmatn, fontSize = 64.sp, lineHeight = 64.sp, fontWeight = FontWeight.Light)
                        Text("°C", color = p.text, fontSize = 20.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                    Text("°C  |  °F", color = p.muted, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.width(7.dp))
                V6WeatherIcon(code, isDay, p, p.bg, Modifier.size(104.dp))
            }
        }
    }
}

@Composable
private fun V6Tabs(selected: V6Mode, p: V6Palette, onSelect: (V6Mode) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalArrangement = Arrangement.End) {
        V6Tab("هوا", V6Mode.WEATHER, selected, p, onSelect)
        V6Tab("بارش", V6Mode.RAIN, selected, p, onSelect)
        V6Tab("باد", V6Mode.WIND, selected, p, onSelect)
    }
}

@Composable
private fun V6Tab(title: String, value: V6Mode, selected: V6Mode, p: V6Palette, onSelect: (V6Mode) -> Unit) {
    Column(Modifier.clickable { onSelect(value) }.padding(horizontal = 13.dp, vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = if (selected == value) p.text else p.muted, fontWeight = if (selected == value) FontWeight.Bold else FontWeight.Normal)
        Spacer(Modifier.height(7.dp))
        Box(Modifier.width(28.dp).height(3.dp).background(if (selected == value) p.chart else Color.Transparent))
    }
}

@Composable
private fun V6Chart(hours: List<HourlyWeather>, mode: V6Mode, p: V6Palette) {
    if (hours.isEmpty()) return
    val samples = remember(hours) {
        listOf(1, 4, 7, 10, 13, 16, 19, 22).filter { it < hours.size }.map { hours[it] }.ifEmpty { hours.take(8) }
    }
    val values = remember(samples, mode) {
        samples.map {
            when (mode) {
                V6Mode.WEATHER -> it.temperature
                V6Mode.RAIN -> it.precipitationProbability.toDouble()
                V6Mode.WIND -> it.windSpeed
            }
        }
    }
    val suffix = when (mode) { V6Mode.WEATHER -> "°"; V6Mode.RAIN -> "٪"; V6Mode.WIND -> "" }

    Column(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 7.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
            values.forEach { Text("${it.fa()}$suffix", Modifier.weight(1f), color = p.text.copy(alpha = .78f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall) }
        }
        Canvas(Modifier.fillMaxWidth().height(112.dp).padding(horizontal = 18.dp, vertical = 7.dp)) {
            if (values.size < 2) return@Canvas
            val low = if (mode == V6Mode.WEATHER) (values.minOrNull() ?: 0.0) - 2.0 else 0.0
            val high = when (mode) {
                V6Mode.WEATHER -> (values.maxOrNull() ?: 1.0) + 2.0
                V6Mode.RAIN -> 100.0
                V6Mode.WIND -> ((values.maxOrNull() ?: 1.0) + 5.0).coerceAtLeast(10.0)
            }
            val span = (high - low).coerceAtLeast(1.0)
            val points = values.mapIndexed { i, v ->
                val x = i * size.width / values.lastIndex
                val n = ((v - low) / span).toFloat().coerceIn(0f, 1f)
                Offset(x, size.height - n * size.height * .68f - size.height * .10f)
            }
            drawPath(v6Area(points, size.height), Brush.verticalGradient(listOf(p.chart.copy(alpha = .30f), p.chart.copy(alpha = .035f))))
            drawPath(v6Line(points), p.chart, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            samples.forEach { Text(formatTime(it.time), Modifier.weight(1f), color = p.muted, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun V6Days(days: List<DailyWeather>, allHours: List<HourlyWeather>, selectedIndex: Int, p: V6Palette, onSelect: (Int) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        contentPadding = PaddingValues(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        itemsIndexed(days, key = { _, item -> item.date }) { index, item ->
            val active = index == selectedIndex
            val dayHours = remember(allHours, item.date) { allHours.filter { it.time.startsWith(item.date) } }
            val code = remember(item, dayHours) { v6RepresentativeCode(item, dayHours) }
            Column(
                Modifier.width(102.dp).clip(RoundedCornerShape(16.dp))
                    .background(if (active) p.selected else Color.Transparent)
                    .clickable { onSelect(index) }.padding(horizontal = 5.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(forecastDayName(item.date, index), color = p.text, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                V6WeatherIcon(code, true, p, if (active) p.selected else p.bg, Modifier.size(68.dp))
                Text(weatherDescription(code), color = p.muted, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                Row {
                    Text("${item.minTemperature.fa()}°", color = p.muted)
                    Spacer(Modifier.width(5.dp))
                    Text("${item.maxTemperature.fa()}°", color = p.text, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun V6Hourly(day: DailyWeather, hours: List<HourlyWeather>, p: V6Palette) {
    if (hours.isEmpty()) return
    Column(Modifier.fillMaxWidth().padding(top = 25.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("پیش‌بینی ساعتی ${shortPersianDate(day.date)}", Modifier.weight(1f), color = p.text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("ساعت‌به‌ساعت", color = p.muted, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            items(hours, key = { it.time }) { hour ->
                val code = v6EffectiveCode(hour)
                Card(
                    Modifier.width(96.dp),
                    colors = CardDefaults.cardColors(containerColor = p.panel),
                    border = BorderStroke(1.dp, p.outline),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(Modifier.padding(horizontal = 5.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(formatTime(hour.time), color = p.muted, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(2.dp))
                        V6WeatherIcon(code, hour.isDay, p, p.panel, Modifier.size(60.dp))
                        Text("${hour.temperature.fa()}°", color = p.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(weatherDescription(code), color = p.muted, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        if (hour.precipitationProbability >= 10) {
                            Text("${hour.precipitationProbability.fa()}٪", color = V6Rain, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun V6Footer(p: V6Palette, dark: Boolean, onToggleTheme: () -> Unit) {
    val uri = LocalUriHandler.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("تنظیمات و ارتباط", color = p.text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = p.panel), border = BorderStroke(1.dp, p.outline), shape = RoundedCornerShape(18.dp)) {
            Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("حالت نمایش", Modifier.weight(1f), color = p.text, fontWeight = FontWeight.Medium)
                V6ThemeChip("روشن", !dark, p, Icons.Rounded.LightMode) { if (dark) onToggleTheme() }
                Spacer(Modifier.width(6.dp))
                V6ThemeChip("تاریک", dark, p, Icons.Rounded.DarkMode) { if (!dark) onToggleTheme() }
            }
        }
        Spacer(Modifier.height(12.dp))
        V6GradientButton("ایستاگرام موبایل تینا", if (expanded) "برای بستن دوباره لمس کنید" else "صفحه‌های اینستاگرام", "IG", listOf(Color(0xFF833AB4), Color(0xFFE1306C), Color(0xFFFCAF45))) { expanded = !expanded }
        AnimatedVisibility(expanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                V6Instagram(EndpointCodec.instagramOneLabel(), Modifier.weight(1f)) { runCatching { uri.openUri(EndpointCodec.instagramOneUrl()) } }
                V6Instagram(EndpointCodec.instagramTwoLabel(), Modifier.weight(1f)) { runCatching { uri.openUri(EndpointCodec.instagramTwoUrl()) } }
                V6Instagram(EndpointCodec.instagramThreeLabel(), Modifier.weight(1f)) { runCatching { uri.openUri(EndpointCodec.instagramThreeUrl()) } }
            }
        }
        Spacer(Modifier.height(10.dp))
        V6GradientButton("توسعه دهنده برنامه", null, "TG", listOf(Color(0xFF168ACD), Color(0xFF229ED9), Color(0xFF5BC0EB))) { runCatching { uri.openUri(EndpointCodec.developerUrl()) } }
        Spacer(Modifier.height(14.dp))
        Text("weather · Mobile Tina", color = p.muted.copy(alpha = .8f), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun V6ThemeChip(title: String, selected: Boolean, p: V6Palette, icon: ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(if (selected) p.selected else Color.Transparent).clickable(onClick = onClick).padding(horizontal = 9.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (selected) Color(0xFF4285F4) else p.muted, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(title, color = if (selected) p.text else p.muted, style = MaterialTheme.typography.bodySmall, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun V6GradientButton(title: String, subtitle: String?, badge: String, gradient: List<Color>, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(19.dp)).background(Brush.horizontalGradient(gradient)).clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = if (subtitle == null) 15.dp else 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(42.dp).background(Color.White.copy(alpha = .18f), CircleShape), contentAlignment = Alignment.Center) {
            Text(badge, color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            if (!subtitle.isNullOrBlank()) Text(subtitle, color = Color.White.copy(alpha = .86f), style = MaterialTheme.typography.bodySmall)
        }
        Text("‹", color = Color.White, fontSize = 27.sp)
    }
}

@Composable
private fun V6Instagram(handle: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(Color(0xFF6A33A8), Color(0xFFC13584), Color(0xFFE6683C))))
            .clickable(onClick = onClick).padding(horizontal = 4.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(handle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, maxLines = 1)
    }
}

@Composable
private fun V6IconButton(icon: ImageVector, tint: Color, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) { Icon(icon, null, tint = tint, modifier = Modifier.size(19.dp)) }
}

@Composable
private fun V6WeatherIcon(code: Int, isDay: Boolean, p: V6Palette, background: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        when (weatherKind(code)) {
            WeatherKind.CLEAR -> v6SunMoon(isDay, background)
            WeatherKind.PARTLY_CLOUDY -> {
                v6SmallSunMoon(isDay, background)
                v6Cloud(Offset(size.width * .59f, size.height * .62f), size.width * .68f, false, p)
            }
            WeatherKind.CLOUDY -> {
                v6Cloud(Offset(size.width * .42f, size.height * .46f), size.width * .55f, true, p)
                v6Cloud(Offset(size.width * .59f, size.height * .62f), size.width * .69f, false, p)
            }
            WeatherKind.FOG -> {
                v6Cloud(Offset(size.width * .52f, size.height * .42f), size.width * .66f, false, p)
                repeat(3) { i ->
                    val y = size.height * (.71f + i * .075f)
                    drawLine(p.cloudShade, Offset(size.width * .18f, y), Offset(size.width * .82f, y), strokeWidth = 3.5f, cap = StrokeCap.Round)
                }
            }
            WeatherKind.RAIN -> {
                v6Cloud(Offset(size.width * .52f, size.height * .40f), size.width * .70f, false, p)
                v6Drop(.31f, .77f, .052f); v6Drop(.51f, .83f, .062f); v6Drop(.71f, .77f, .052f)
            }
            WeatherKind.STORM -> {
                v6Cloud(Offset(size.width * .52f, size.height * .38f), size.width * .70f, true, p)
                v6Drop(.28f, .79f, .045f); v6Drop(.72f, .79f, .045f)
                val bolt = Path().apply {
                    moveTo(size.width * .51f, size.height * .54f); lineTo(size.width * .40f, size.height * .73f)
                    lineTo(size.width * .50f, size.height * .73f); lineTo(size.width * .43f, size.height * .93f)
                    lineTo(size.width * .65f, size.height * .66f); lineTo(size.width * .54f, size.height * .66f); close()
                }
                drawPath(bolt, V6Lightning)
            }
            WeatherKind.SNOW -> {
                v6Cloud(Offset(size.width * .52f, size.height * .40f), size.width * .70f, false, p)
                listOf(.31f, .51f, .71f).forEachIndexed { i, x -> v6Snowflake(Offset(size.width * x, size.height * (.79f + (i % 2) * .04f)), size.width * .045f) }
            }
        }
    }
}

private fun DrawScope.v6SunMoon(day: Boolean, background: Color) {
    if (!day) {
        val c = Offset(size.width * .52f, size.height * .48f); val r = size.minDimension * .21f
        drawCircle(Color(0xFFDCE6FF), radius = r, center = c)
        drawCircle(background, radius = r * .82f, center = Offset(c.x - r * .40f, c.y - r * .20f))
        drawCircle(Color(0xFF8AB4F8), radius = r * .08f, center = Offset(c.x + r * 1.6f, c.y - r * 1.15f))
        return
    }
    val c = Offset(size.width * .50f, size.height * .48f); val r = size.minDimension * .18f
    repeat(8) { i ->
        val a = Math.toRadians(i * 45.0)
        val inner = Offset(c.x + cos(a).toFloat() * r * 1.55f, c.y + sin(a).toFloat() * r * 1.55f)
        val outer = Offset(c.x + cos(a).toFloat() * r * 2.18f, c.y + sin(a).toFloat() * r * 2.18f)
        drawLine(V6Sun, inner, outer, strokeWidth = r * .13f, cap = StrokeCap.Round)
    }
    drawCircle(V6Sun.copy(alpha = .16f), radius = r * 1.34f, center = c)
    drawCircle(Brush.radialGradient(listOf(V6SunCore, V6Sun, V6SunDeep), center = c, radius = r * 1.1f), radius = r, center = c)
}

private fun DrawScope.v6SmallSunMoon(day: Boolean, background: Color) {
    val c = Offset(size.width * .34f, size.height * .33f); val r = size.minDimension * .135f
    if (!day) {
        drawCircle(Color(0xFFDCE6FF), radius = r, center = c)
        drawCircle(background, radius = r * .80f, center = Offset(c.x - r * .38f, c.y - r * .18f))
        return
    }
    repeat(8) { i ->
        val a = Math.toRadians(i * 45.0)
        drawLine(
            V6Sun,
            Offset(c.x + cos(a).toFloat() * r * 1.50f, c.y + sin(a).toFloat() * r * 1.50f),
            Offset(c.x + cos(a).toFloat() * r * 2.05f, c.y + sin(a).toFloat() * r * 2.05f),
            strokeWidth = r * .12f,
            cap = StrokeCap.Round
        )
    }
    drawCircle(Brush.radialGradient(listOf(V6SunCore, V6Sun, V6SunDeep), center = c, radius = r * 1.1f), radius = r, center = c)
}

private fun DrawScope.v6Cloud(center: Offset, width: Float, dark: Boolean, p: V6Palette) {
    val front = if (dark) p.storm else p.cloud
    val shade = if (dark) p.storm.copy(alpha = .70f) else p.cloudShade
    val h = width * .40f
    drawRoundRect(Color.Black.copy(alpha = if (dark) .16f else .10f), Offset(center.x - width * .43f, center.y + h * .15f), Size(width * .92f, h * .54f), CornerRadius(h * .30f))
    drawRoundRect(shade, Offset(center.x - width * .47f, center.y + h * .07f), Size(width * .94f, h * .62f), CornerRadius(h * .34f))
    drawRoundRect(front, Offset(center.x - width * .50f, center.y - h * .04f), Size(width, h * .62f), CornerRadius(h * .34f))
    drawCircle(front, radius = width * .19f, center = Offset(center.x - width * .22f, center.y - h * .06f))
    drawCircle(front, radius = width * .25f, center = Offset(center.x + width * .05f, center.y - h * .19f))
    drawCircle(front, radius = width * .17f, center = Offset(center.x + width * .29f, center.y - h * .01f))
    drawArc(Color.White.copy(alpha = if (dark) .14f else .58f), 198f, 72f, false, Offset(center.x - width * .37f, center.y - h * .31f), Size(width * .58f, h * .70f), style = Stroke(width = 2.2f, cap = StrokeCap.Round))
}

private fun DrawScope.v6Drop(x: Float, y: Float, fraction: Float) {
    val cx = size.width * x; val cy = size.height * y; val r = size.width * fraction
    val path = Path().apply {
        moveTo(cx, cy - r * 1.55f)
        cubicTo(cx - r * 1.02f, cy - r * .22f, cx - r * 1.02f, cy + r * .88f, cx, cy + r * 1.10f)
        cubicTo(cx + r * 1.02f, cy + r * .88f, cx + r * 1.02f, cy - r * .22f, cx, cy - r * 1.55f)
        close()
    }
    drawPath(path, Brush.verticalGradient(listOf(V6RainLight, V6Rain), startY = cy - r * 1.4f, endY = cy + r * 1.1f))
}

private fun DrawScope.v6Snowflake(center: Offset, r: Float) {
    repeat(3) { i ->
        val a = Math.toRadians(i * 60.0)
        val dx = cos(a).toFloat() * r
        val dy = sin(a).toFloat() * r
        drawLine(V6RainLight, Offset(center.x - dx, center.y - dy), Offset(center.x + dx, center.y + dy), strokeWidth = max(1.5f, r * .18f), cap = StrokeCap.Round)
    }
}

private fun v6EffectiveCode(hour: HourlyWeather): Int = when {
    hour.snowfall >= 0.05 -> 71
    (hour.rain + hour.showers >= 0.08 || hour.precipitation >= 0.10) && hour.precipitationProbability >= 20 -> 61
    else -> hour.weatherCode
}

private fun v6RepresentativeCode(day: DailyWeather, hours: List<HourlyWeather>): Int {
    if (hours.isEmpty()) return day.weatherCode
    val daylight = hours.filter { it.isDay }
    val sample = if (daylight.isNotEmpty()) daylight else hours
    val effective = sample.map { it to v6EffectiveCode(it) }
    if (effective.any { (_, code) -> weatherKind(code) == WeatherKind.STORM }) return 95
    val snowHours = effective.count { (h, code) -> weatherKind(code) == WeatherKind.SNOW && (h.snowfall >= .05 || h.precipitationProbability >= 25) }
    if (snowHours >= 2 || day.snowfallSum >= .4) return 71
    val rainHours = effective.count { (h, code) -> weatherKind(code) == WeatherKind.RAIN && (h.precipitation >= .08 || h.precipitationProbability >= 30) }
    if (rainHours >= 2 || ((day.rainSum + day.showersSum) >= .5 && day.precipitationProbability >= 40)) return 61
    val avgCloud = sample.map { it.cloudCover }.average().takeUnless { it.isNaN() } ?: 50.0
    val cloudy = effective.count { weatherKind(it.second) == WeatherKind.CLOUDY }
    val partly = effective.count { weatherKind(it.second) == WeatherKind.PARTLY_CLOUDY }
    return when {
        cloudy >= sample.size / 2 || avgCloud >= 72 -> 3
        partly + cloudy >= sample.size / 2 || avgCloud >= 32 -> 2
        else -> 1
    }
}

private fun v6Line(points: List<Offset>) = Path().apply {
    if (points.isEmpty()) return@apply
    moveTo(points.first().x, points.first().y)
    for (i in 1 until points.size) {
        val a = points[i - 1]; val b = points[i]; val middle = (a.x + b.x) / 2f
        cubicTo(middle, a.y, middle, b.y, b.x, b.y)
    }
}

private fun v6Area(points: List<Offset>, bottom: Float) = Path().apply {
    if (points.isEmpty()) return@apply
    moveTo(points.first().x, bottom)
    lineTo(points.first().x, points.first().y)
    for (i in 1 until points.size) {
        val a = points[i - 1]; val b = points[i]; val middle = (a.x + b.x) / 2f
        cubicTo(middle, a.y, middle, b.y, b.x, b.y)
    }
    lineTo(points.last().x, bottom)
    close()
}
