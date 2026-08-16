package ir.havayeiran.weather.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider
import ir.havayeiran.weather.data.AirQuality
import ir.havayeiran.weather.data.CitySearchResult
import ir.havayeiran.weather.data.MarineWeather
import ir.havayeiran.weather.data.QuickCities
import ir.havayeiran.weather.data.WeatherBundle
import ir.havayeiran.weather.data.WeatherKind
import ir.havayeiran.weather.data.WeatherLocation
import ir.havayeiran.weather.data.aqiLabel
import ir.havayeiran.weather.data.weatherDescription
import ir.havayeiran.weather.data.weatherKind

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
        val background = if (state.darkMode) {
            Brush.verticalGradient(listOf(Color(0xFF050C16), Color(0xFF071321), Color(0xFF0A1625)))
        } else {
            Brush.verticalGradient(listOf(Color(0xFFF4F8FD), Color(0xFFEAF2FA), Color(0xFFF7FAFD)))
        }
        Box(Modifier.fillMaxSize().background(background)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().statusBarsPadding(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 15.dp, end = 15.dp, top = 10.dp, bottom = 34.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    AppHeader(
                        state = state,
                        onSearchChange = onSearchChange,
                        onClearSearch = onClearSearch,
                        onRefresh = onRefresh,
                        onLocate = onLocate,
                        onToggleTheme = onToggleTheme
                    )
                }

                if (state.searchQuery.trim().length >= 2) {
                    item {
                        SearchResultsCard(
                            state = state,
                            onSelect = onSearchResult
                        )
                    }
                }

                item {
                    val allCities = (state.favorites + QuickCities).distinctBy { "${it.latitude}-${it.longitude}" }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        items(allCities) { city ->
                            val selected = near(city, state.selectedLocation)
                            val favorite = state.favorites.any { near(it, city) }
                            CityChip(
                                location = city,
                                selected = selected,
                                favorite = favorite,
                                onClick = { onSelectLocation(city) }
                            )
                        }
                    }
                }

                if (!state.errorMessage.isNullOrBlank()) {
                    item { ErrorCard(state.errorMessage, onRefresh) }
                }

                if (state.isLoading && state.weather == null) {
                    item { LoadingCard() }
                } else {
                    state.weather?.let { bundle ->
                        item {
                            HeroWeatherCard(
                                bundle = bundle,
                                isFavorite = isFavorite,
                                refreshing = state.isRefreshing,
                                onToggleFavorite = onToggleFavorite,
                                onRefresh = onRefresh
                            )
                        }
                        item {
                            SectionTitle("پیش‌بینی ساعتی", "۲۴ ساعت آینده")
                            Spacer(Modifier.height(9.dp))
                            val upcoming = bundle.hourly
                                .filter { it.time >= bundle.current.time.take(13) + ":00" }
                                .take(24)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(upcoming) { hour ->
                                    HourlyItem(hour, isNow = hour == upcoming.firstOrNull())
                                }
                            }
                        }
                        item { DetailsSection(bundle) }
                        item { AirQualityCard(bundle.airQuality) }
                        bundle.marine?.let { marine -> item { MarineCard(marine) } }
                        item { SunAndUvCard(bundle) }
                        item {
                            SectionTitle("پیش‌بینی ۱۰ روزه", "کمینه، بیشینه و بارش")
                            Spacer(Modifier.height(9.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                bundle.daily.forEachIndexed { index, day -> DailyRow(day, index) }
                            }
                        }
                        item { TodayAdviceCard(bundle) }
                        item { DataSourceFooter() }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppHeader(
    state: WeatherUiState,
    onSearchChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onRefresh: () -> Unit,
    onLocate: () -> Unit,
    onToggleTheme: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(45.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF347CF4), Color(0xFF47C7E0)))),
                contentAlignment = Alignment.Center
            ) {
                Text("☁", fontSize = 22.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("هوای ایران", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("پیش‌بینی دقیق شهرهای ایران", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            RoundAction(icon = if (state.darkMode) Icons.Rounded.LightMode else Icons.Rounded.DarkMode, description = "تغییر تم", onClick = onToggleTheme)
            Spacer(Modifier.width(5.dp))
            RoundAction(icon = Icons.Rounded.MyLocation, description = "موقعیت من", onClick = onLocate)
            Spacer(Modifier.width(5.dp))
            RoundAction(icon = Icons.Rounded.Refresh, description = "به‌روزرسانی", onClick = onRefresh)
        }
        TextField(
            value = state.searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("نام شهر را جستجو کنید؛ مثلاً رامسر، تهران، شیراز…", style = MaterialTheme.typography.bodyMedium) },
            leadingIcon = { Icon(Icons.Rounded.Search, null, modifier = Modifier.size(19.dp)) },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(onClick = onClearSearch) { Icon(Icons.Rounded.Close, "پاک کردن", modifier = Modifier.size(18.dp)) }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = .72f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = .60f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun SearchResultsCard(state: WeatherUiState, onSelect: (CitySearchResult) -> Unit) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .92f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .25f))
    ) {
        Column(Modifier.fillMaxWidth().padding(8.dp)) {
            if (state.isSearching) {
                Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("در حال جستجوی شهرهای ایران…", style = MaterialTheme.typography.bodyMedium)
                }
            } else if (state.searchResults.isEmpty()) {
                Text("شهری با این نام پیدا نشد.", modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                state.searchResults.take(8).forEachIndexed { index, result ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelect(result) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.LocationOn, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text(result.name, style = MaterialTheme.typography.titleMedium)
                            Text(result.province.ifBlank { "ایران" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (index != state.searchResults.take(8).lastIndex) {
                        Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = .12f)))
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroWeatherCard(
    bundle: WeatherBundle,
    isFavorite: Boolean,
    refreshing: Boolean,
    onToggleFavorite: () -> Unit,
    onRefresh: () -> Unit
) {
    val current = bundle.current
    val today = bundle.daily.firstOrNull()
    val kind = weatherKind(current.weatherCode)
    val brush = heroBrush(kind, current.isDay)

    Card(
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .10f))
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(brush)
                .padding(21.dp)
        ) {
            Column(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text("اکنون در", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = .64f))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.LocationOn, null, tint = Color.White.copy(alpha = .88f), modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(bundle.location.name, style = MaterialTheme.typography.headlineMedium, color = Color.White)
                            if (bundle.location.province.isNotBlank()) {
                                Text("، ${bundle.location.province}", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = .68f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Text(formatPersianDate(current.time), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = .63f))
                    }
                    FavoriteButton(isFavorite, onToggleFavorite)
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("${current.temperature.fa()}°", fontFamily = Vazirmatn, fontWeight = FontWeight.Light, fontSize = 70.sp, lineHeight = 74.sp, color = Color.White)
                        Text(weatherDescription(current.weatherCode), style = MaterialTheme.typography.titleLarge, color = Color.White)
                        Text("دمای احساسی ${current.apparentTemperature.fa()}°", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = .68f))
                    }
                    WeatherArtwork(kind, current.isDay, Modifier.size(145.dp))
                }

                Text(
                    weatherSentence(current, today),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = .80f),
                    modifier = Modifier.padding(top = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    HeroPill("↕", "${today?.minTemperature?.fa() ?: "—"}° / ${today?.maxTemperature?.fa() ?: "—"}°")
                    HeroPill("💧", "بارش ${today?.precipitationProbability?.fa() ?: "—"}٪")
                    HeroPill("💨", "باد ${current.windSpeed.fa()} km/h")
                    HeroPill("☀", "UV ${today?.uvIndex?.fa(1) ?: "—"}")
                }
            }
            if (refreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.BottomStart).size(18.dp).clickable(onClick = onRefresh),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

@Composable
private fun DetailsSection(bundle: WeatherBundle) {
    SectionTitle("جزئیات اکنون", "اندازه‌گیری‌های محلی")
    Spacer(Modifier.height(9.dp))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("💧", "رطوبت", "${bundle.current.humidity.fa()}٪", Modifier.weight(1f))
            MetricCard("💨", "سرعت باد", "${bundle.current.windSpeed.fa()} km/h", Modifier.weight(1f), compassLabel(bundle.current.windDirection))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("◉", "فشار هوا", "${bundle.current.pressure.fa()} hPa", Modifier.weight(1f))
            MetricCard("👁", "دید افقی", "${bundle.current.visibilityKm.fa(1)} km", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("☁", "پوشش ابر", "${bundle.current.cloudCover.fa()}٪", Modifier.weight(1f))
            MetricCard("↗", "تندباد", "${bundle.current.windGust.fa()} km/h", Modifier.weight(1f))
        }
    }
}

@Composable
private fun AirQualityCard(air: AirQuality?) {
    SectionTitle("کیفیت هوا", "شاخص AQI و ذرات معلق")
    Spacer(Modifier.height(9.dp))
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .65f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .20f))
    ) {
        if (air == null) {
            Text("اطلاعات کیفیت هوا در حال حاضر در دسترس نیست.", Modifier.padding(18.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("شاخص کیفیت هوا", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(air.usAqi.fa(), style = MaterialTheme.typography.headlineLarge)
                            Spacer(Modifier.width(7.dp))
                            Text(aqiLabel(air.usAqi), style = MaterialTheme.typography.titleMedium, color = aqiColor(air.usAqi), modifier = Modifier.padding(bottom = 3.dp))
                        }
                    }
                    Text("AQI", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(10.dp))
                ProgressTrack(progress = (air.usAqi / 300f).coerceIn(0f, 1f), color = aqiColor(air.usAqi))
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AirMetric("PM2.5", "${air.pm25.fa(1)} µg/m³", Modifier.weight(1f))
                    AirMetric("PM10", "${air.pm10.fa(1)} µg/m³", Modifier.weight(1f))
                    AirMetric("O₃", "${air.ozone.fa()} µg/m³", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MarineCard(marine: MarineWeather) {
    SectionTitle("دریای خزر", "اطلاعات دریایی نزدیک ساحل")
    Spacer(Modifier.height(9.dp))
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C5473).copy(alpha = .28f)),
        border = BorderStroke(1.dp, Color(0xFF62C6E8).copy(alpha = .18f))
    ) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🌊", fontSize = 36.sp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("دمای سطح آب", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(marine.seaSurfaceTemperature?.let { "${it.fa(1)}°" } ?: "—", style = MaterialTheme.typography.headlineMedium)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("ارتفاع موج", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(marine.waveHeight?.let { "${it.fa(1)} m" } ?: "—", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Composable
private fun SunAndUvCard(bundle: WeatherBundle) {
    val today = bundle.daily.firstOrNull() ?: return
    SectionTitle("خورشید و فرابنفش", "برنامه امروز")
    Spacer(Modifier.height(9.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .62f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .20f))
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("🌅  طلوع", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatTime(today.sunrise), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(9.dp))
                Text("🌇  غروب", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatTime(today.sunset), style = MaterialTheme.typography.titleLarge)
            }
        }
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .62f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .20f))
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("☀  شاخص UV", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(today.uvIndex.fa(1), style = MaterialTheme.typography.headlineMedium)
                Text(uvLabel(today.uvIndex), style = MaterialTheme.typography.titleMedium, color = uvColor(today.uvIndex))
                Spacer(Modifier.height(9.dp))
                ProgressTrack((today.uvIndex / 12f).toFloat().coerceIn(0f, 1f), uvColor(today.uvIndex))
            }
        }
    }
}

@Composable
private fun TodayAdviceCard(bundle: WeatherBundle) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = .09f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .20f))
    ) {
        Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = .14f)),
                contentAlignment = Alignment.Center
            ) { Text("✨", fontSize = 19.sp) }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text("پیشنهاد امروز", style = MaterialTheme.typography.titleMedium)
                Text(
                    todayAdvice(bundle.current, bundle.daily.firstOrNull(), bundle.airQuality?.usAqi),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String, retry: () -> Unit) {
    Card(
        shape = RoundedCornerShape(19.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = .10f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = .20f))
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("⚠️", fontSize = 18.sp)
            Spacer(Modifier.width(9.dp))
            Text(message, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text("تلاش مجدد", modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = retry).padding(8.dp), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun LoadingCard() {
    Card(
        modifier = Modifier.fillMaxWidth().height(330.dp),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .55f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .18f))
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
            Spacer(Modifier.height(12.dp))
            Text("در حال دریافت هوای رامسر…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DataSourceFooter() {
    Column(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("داده‌های هوا، کیفیت هوا و دریا: Open‑Meteo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Text("هوای ایران · نسخه اندروید", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .65f))
    }
}

@Composable
private fun SectionTitle(title: String, note: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HeroPill(emoji: String, text: String) {
    Row(
        modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = .09f)).border(1.dp, Color.White.copy(alpha = .08f), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 11.sp)
        Spacer(Modifier.width(5.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = .90f))
    }
}

@Composable
private fun AirMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

@Composable
private fun ProgressTrack(progress: Float, color: Color) {
    Box(Modifier.fillMaxWidth().height(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outline.copy(alpha = .18f))) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(.02f, 1f)).height(7.dp).clip(CircleShape).background(color))
    }
}

@Composable
private fun RoundAction(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = .62f)).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .22f), RoundedCornerShape(14.dp))
    ) {
        Icon(icon, description, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun heroBrush(kind: WeatherKind, isDay: Boolean): Brush {
    val colors = when {
        !isDay -> listOf(Color(0xFF172B55), Color(0xFF09162D), Color(0xFF101B37))
        kind == WeatherKind.CLEAR -> listOf(Color(0xFF2766A6), Color(0xFF173D6A), Color(0xFF163052))
        kind == WeatherKind.PARTLY_CLOUDY -> listOf(Color(0xFF315F8A), Color(0xFF1B3E62), Color(0xFF172D48))
        kind == WeatherKind.RAIN -> listOf(Color(0xFF344F68), Color(0xFF1A3047), Color(0xFF122337))
        kind == WeatherKind.STORM -> listOf(Color(0xFF303E5A), Color(0xFF171E32), Color(0xFF111A2A))
        kind == WeatherKind.SNOW -> listOf(Color(0xFF58748B), Color(0xFF2C465D), Color(0xFF1B3145))
        kind == WeatherKind.FOG -> listOf(Color(0xFF687985), Color(0xFF3B5363), Color(0xFF293E4D))
        else -> listOf(Color(0xFF405D78), Color(0xFF203A55), Color(0xFF172B41))
    }
    return Brush.linearGradient(colors)
}

private fun aqiColor(aqi: Int): Color = when {
    aqi <= 50 -> Color(0xFF59D39A)
    aqi <= 100 -> Color(0xFFFFD15C)
    aqi <= 150 -> Color(0xFFFFA158)
    aqi <= 200 -> Color(0xFFFF6E6E)
    aqi <= 300 -> Color(0xFFC987E8)
    else -> Color(0xFFA9586B)
}

private fun uvColor(uv: Double): Color = when {
    uv < 3 -> Color(0xFF57D49A)
    uv < 6 -> Color(0xFFFFD05C)
    uv < 8 -> Color(0xFFFF9D57)
    uv < 11 -> Color(0xFFFF6969)
    else -> Color(0xFFB77BE3)
}

private fun near(a: WeatherLocation, b: WeatherLocation): Boolean =
    kotlin.math.abs(a.latitude - b.latitude) < .01 && kotlin.math.abs(a.longitude - b.longitude) < .01
