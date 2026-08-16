package ir.havayeiran.weather.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
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
import ir.havayeiran.weather.data.*
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

private data class V5P(
    val bg: Color, val panel: Color, val selected: Color, val text: Color,
    val muted: Color, val outline: Color, val chart: Color,
    val cloud: Color, val cloudShade: Color, val storm: Color
)

private fun v5p(dark: Boolean) = if (dark) V5P(
    Color(0xFF202124), Color(0xFF292A2D), Color(0xFF303134), Color(0xFFF1F3F4),
    Color(0xFFB0B4BA), Color(0xFF3C4043), Color(0xFFFFC107),
    Color(0xFFF1F3F4), Color(0xFFBDC1C6), Color(0xFF8F969F)
) else V5P(
    Color(0xFFF7F9FC), Color.White, Color(0xFFE8F0FE), Color(0xFF17191C),
    Color(0xFF4E545B), Color(0xFFD3D8DE), Color(0xFFF9AB00),
    Color(0xFFE3E7EC), Color(0xFFAAB3BD), Color(0xFF6F7883)
)

private val V5Sun = Color(0xFFFFA726)
private val V5SunDeep = Color(0xFFFF8F00)
private val V5Rain = Color(0xFF1A73E8)
private val V5RainLight = Color(0xFF64B5F6)
private val V5Lightning = Color(0xFFFFD54F)
private enum class V5Mode { WEATHER, RAIN, WIND }

@Composable
fun GoogleWeatherScreenV5(
    state: WeatherUiState,
    onSearchChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSearchResult: (CitySearchResult) -> Unit,
    onRefresh: () -> Unit,
    onLocate: () -> Unit,
    onToggleTheme: () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val p = remember(state.darkMode) { v5p(state.darkMode) }
        var searchOpen by rememberSaveable { mutableStateOf(false) }
        var selectedDay by remember(state.selectedLocation.latitude, state.selectedLocation.longitude) { mutableStateOf(0) }
        var mode by rememberSaveable { mutableStateOf(V5Mode.WEATHER) }
        val bundle = state.weather
        val index = selectedDay.coerceIn(0, max(bundle?.daily?.lastIndex ?: 0, 0))
        val day = bundle?.daily?.getOrNull(index)
        val hours = remember(bundle?.hourly, day?.date) {
            val d = day?.date
            if (bundle == null || d == null) emptyList() else bundle.hourly.filter { it.time.startsWith(d) }
        }

        Box(Modifier.fillMaxSize().background(p.bg)) {
            LazyColumn(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
                contentPadding = PaddingValues(bottom = 26.dp)
            ) {
                item {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.LocationOn, null, tint = p.text, modifier = Modifier.size(21.dp))
                        Spacer(Modifier.width(5.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (state.selectedLocation.province.isBlank()) state.selectedLocation.name else "${state.selectedLocation.name}، استان ${state.selectedLocation.province}",
                                color = p.text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            Text("ایران · انتخاب منطقه", color = Color(0xFF3367D6), style = MaterialTheme.typography.bodySmall)
                        }
                        V5IconButton(if (searchOpen) Icons.Rounded.Close else Icons.Rounded.Search, p.text) {
                            searchOpen = !searchOpen; if (!searchOpen) onClearSearch()
                        }
                        V5IconButton(Icons.Rounded.MyLocation, p.text, onLocate)
                        V5IconButton(Icons.Rounded.Refresh, p.text, onRefresh)
                        V5IconButton(if (state.darkMode) Icons.Rounded.LightMode else Icons.Rounded.DarkMode, p.text, onToggleTheme)
                    }
                }

                if (searchOpen) item {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp)) {
                        OutlinedTextField(
                            value = state.searchQuery, onValueChange = onSearchChange, modifier = Modifier.fillMaxWidth(),
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
                            singleLine = true, shape = RoundedCornerShape(18.dp)
                        )
                        if (state.searchQuery.trim().length >= 2) {
                            Card(
                                Modifier.fillMaxWidth().padding(top = 6.dp),
                                colors = CardDefaults.cardColors(containerColor = p.panel),
                                border = BorderStroke(1.dp, p.outline), shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(Modifier.padding(6.dp)) {
                                    when {
                                        state.isSearching -> Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.Center) {
                                            CircularProgressIndicator(Modifier.size(18.dp), color = p.chart, strokeWidth = 2.dp)
                                        }
                                        state.searchResults.isEmpty() -> Text("شهری پیدا نشد.", Modifier.padding(12.dp), color = p.muted)
                                        else -> state.searchResults.take(7).forEach { r ->
                                            Row(
                                                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable {
                                                    selectedDay = 0; searchOpen = false; onSearchResult(r)
                                                }.padding(10.dp), verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Rounded.LocationOn, null, tint = Color(0xFF4285F4), modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.width(8.dp)); Text(r.name, color = p.text, fontWeight = FontWeight.Bold)
                                                Spacer(Modifier.width(6.dp)); Text(r.province.ifBlank { "ایران" }, color = p.muted, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (!state.errorMessage.isNullOrBlank()) item {
                    Card(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 7.dp),
                        colors = CardDefaults.cardColors(containerColor = if (state.darkMode) Color(0xFF3A2929) else Color(0xFFFFECEC)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(state.errorMessage.orEmpty(), Modifier.padding(12.dp), color = if (state.darkMode) Color(0xFFFFD3D3) else Color(0xFF8C1D18))
                    }
                }

                if (state.isLoading && bundle == null) item {
                    Column(Modifier.fillMaxWidth().padding(vertical = 88.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = p.chart); Spacer(Modifier.height(12.dp)); Text("در حال دریافت اطلاعات هوا…", color = p.muted)
                    }
                } else if (bundle != null && day != null) {
                    item {
                        val today = index == 0
                        val code = if (today) bundle.current.weatherCode else day.weatherCode
                        val temp = if (today) bundle.current.temperature else day.maxTemperature
                        val isDay = if (today) bundle.current.isDay else true
                        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(.78f)) {
                                Text("آب‌وهوا", color = p.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Text(forecastDayName(day.date, index), color = p.muted, fontSize = 15.sp)
                                Text(weatherDescription(code), color = p.muted, fontSize = 16.sp)
                            }
                            Column(Modifier.weight(1.22f), horizontalAlignment = Alignment.End) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Row(verticalAlignment = Alignment.Top) {
                                            Text(temp.fa(), color = p.text, fontFamily = Vazirmatn, fontSize = 66.sp, lineHeight = 66.sp, fontWeight = FontWeight.Light)
                                            Text("°C", color = p.text, fontSize = 20.sp, modifier = Modifier.padding(top = 8.dp))
                                        }
                                        Text("°C  |  °F", color = p.muted, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Spacer(Modifier.width(8.dp)); V5WeatherIcon(code, isDay, p, p.bg, Modifier.size(106.dp))
                                }
                                Text("بارش: ${day.precipitationProbability.fa()}٪", color = p.muted)
                                if (today) Text("رطوبت: ${bundle.current.humidity.fa()}٪", color = p.muted)
                                Text("باد: ${day.maxWindSpeed.fa()} km/h", color = p.muted)
                            }
                        }
                    }

                    item {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalArrangement = Arrangement.End) {
                            V5Tab("هوا", V5Mode.WEATHER, mode, p) { mode = it }
                            V5Tab("بارش", V5Mode.RAIN, mode, p) { mode = it }
                            V5Tab("باد", V5Mode.WIND, mode, p) { mode = it }
                        }
                    }
                    item { V5Chart(hours, mode, p) }
                    item {
                        LazyRow(Modifier.fillMaxWidth().padding(top = 10.dp), contentPadding = PaddingValues(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            itemsIndexed(bundle.daily.take(8), key = { _, d -> d.date }) { i, d ->
                                val active = i == index
                                Column(
                                    Modifier.width(102.dp).clip(RoundedCornerShape(16.dp)).background(if (active) p.selected else Color.Transparent)
                                        .clickable { selectedDay = i }.padding(horizontal = 5.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(forecastDayName(d.date, i), color = p.text, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(4.dp)); V5WeatherIcon(d.weatherCode, true, p, if (active) p.selected else p.bg, Modifier.size(66.dp))
                                    Text(weatherDescription(d.weatherCode), color = p.muted, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                    Row { Text("${d.minTemperature.fa()}°", color = p.muted); Spacer(Modifier.width(5.dp)); Text("${d.maxTemperature.fa()}°", color = p.text, fontWeight = FontWeight.Bold) }
                                }
                            }
                        }
                    }
                    item {
                        Column(Modifier.fillMaxWidth().padding(top = 25.dp)) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("پیش‌بینی ساعتی ${shortPersianDate(day.date)}", Modifier.weight(1f), color = p.text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("ساعت‌به‌ساعت", color = p.muted, style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(Modifier.height(10.dp))
                            LazyRow(contentPadding = PaddingValues(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                items(hours, key = { it.time }) { h ->
                                    Card(Modifier.width(94.dp), colors = CardDefaults.cardColors(containerColor = p.panel), border = BorderStroke(1.dp, p.outline), shape = RoundedCornerShape(18.dp)) {
                                        Column(Modifier.padding(horizontal = 5.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(formatTime(h.time), color = p.muted, style = MaterialTheme.typography.bodySmall)
                                            V5WeatherIcon(h.weatherCode, v5IsDay(h.time), p, p.panel, Modifier.size(58.dp))
                                            Text("${h.temperature.fa()}°", color = p.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                            Text(weatherDescription(h.weatherCode), color = p.muted, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                            if (h.precipitationProbability > 0) Text("💧 ${h.precipitationProbability.fa()}٪", color = V5Rain, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item { Text("داده‌های هواشناسی: Open-Meteo", Modifier.fillMaxWidth().padding(top = 22.dp), color = p.muted, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center) }
                }

                item { V5Footer(p, state.darkMode, onToggleTheme) }
            }
        }
    }
}

@Composable private fun V5IconButton(icon: ImageVector, tint: Color, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) { Icon(icon, null, tint = tint, modifier = Modifier.size(19.dp)) }
}

@Composable private fun V5Tab(title: String, value: V5Mode, selected: V5Mode, p: V5P, onSelect: (V5Mode) -> Unit) {
    Column(Modifier.clickable { onSelect(value) }.padding(horizontal = 13.dp, vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = if (selected == value) p.text else p.muted, fontWeight = if (selected == value) FontWeight.Bold else FontWeight.Normal)
        Spacer(Modifier.height(7.dp)); Box(Modifier.width(28.dp).height(3.dp).background(if (selected == value) p.chart else Color.Transparent))
    }
}

@Composable private fun V5Chart(hours: List<HourlyWeather>, mode: V5Mode, p: V5P) {
    if (hours.isEmpty()) return
    val sample = remember(hours) { listOf(1,4,7,10,13,16,19,22).filter { it < hours.size }.map { hours[it] }.ifEmpty { hours.take(8) } }
    val values = remember(sample, mode) { sample.map { when (mode) { V5Mode.WEATHER -> it.temperature; V5Mode.RAIN -> it.precipitationProbability.toDouble(); V5Mode.WIND -> it.windSpeed } } }
    val suffix = if (mode == V5Mode.WEATHER) "°" else if (mode == V5Mode.RAIN) "٪" else ""
    Column(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 7.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) { values.forEach { Text("${it.fa()}$suffix", Modifier.weight(1f), color = p.text, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall) } }
        Canvas(Modifier.fillMaxWidth().height(112.dp).padding(horizontal = 18.dp, vertical = 7.dp)) {
            if (values.size < 2) return@Canvas
            val lo = if (mode == V5Mode.WEATHER) (values.minOrNull() ?: 0.0) - 2 else 0.0
            val hi = when(mode) { V5Mode.WEATHER -> (values.maxOrNull() ?: 1.0) + 2; V5Mode.RAIN -> 100.0; V5Mode.WIND -> ((values.maxOrNull() ?: 1.0) + 5).coerceAtLeast(10.0) }
            val span = (hi - lo).coerceAtLeast(1.0)
            val pts = values.mapIndexed { i, v -> Offset(i * size.width / values.lastIndex, size.height - (((v - lo) / span).toFloat().coerceIn(0f,1f)) * size.height * .68f - size.height * .10f) }
            drawPath(v5Area(pts, size.height), Brush.verticalGradient(listOf(p.chart.copy(alpha=.30f), p.chart.copy(alpha=.035f))))
            drawPath(v5Line(pts), p.chart, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) { sample.forEach { Text(formatTime(it.time), Modifier.weight(1f), color = p.muted, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall) } }
    }
}

@Composable private fun V5Footer(p: V5P, dark: Boolean, onToggleTheme: () -> Unit) {
    val uri = LocalUriHandler.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 24.dp).animateContentSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("تنظیمات و ارتباط", color = p.text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = p.panel), border = BorderStroke(1.dp,p.outline), shape = RoundedCornerShape(18.dp)) {
            Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("حالت نمایش", Modifier.weight(1f), color = p.text, fontWeight = FontWeight.Medium)
                V5ThemeChip("روشن", !dark, p, Icons.Rounded.LightMode) { if (dark) onToggleTheme() }; Spacer(Modifier.width(6.dp))
                V5ThemeChip("تاریک", dark, p, Icons.Rounded.DarkMode) { if (!dark) onToggleTheme() }
            }
        }
        Spacer(Modifier.height(12.dp))
        V5GradientButton("ایستاگرام موبایل تینا", if (expanded) "برای بستن دوباره لمس کنید" else "مشاهده صفحه‌های اینستاگرام", "IG", listOf(Color(0xFF833AB4),Color(0xFFE1306C),Color(0xFFFCAF45))) { expanded = !expanded }
        AnimatedVisibility(expanded, enter = fadeIn()+expandVertically(), exit = fadeOut()+shrinkVertically()) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                V5Instagram("mobile.tina", Modifier.weight(1f)) { runCatching { uri.openUri("https://www.instagram.com/mobile.tina/") } }
                V5Instagram("mobile.tina2", Modifier.weight(1f)) { runCatching { uri.openUri("https://www.instagram.com/mobile.tina2/") } }
                V5Instagram("mobile.tinaa", Modifier.weight(1f)) { runCatching { uri.openUri("https://www.instagram.com/mobile.tinaa/") } }
            }
        }
        Spacer(Modifier.height(10.dp))
        V5GradientButton("توسعه دهنده برنامه", null, "TG", listOf(Color(0xFF168ACD),Color(0xFF229ED9),Color(0xFF5BC0EB))) { runCatching { uri.openUri("https://t.me/vpn963") } }
        Spacer(Modifier.height(14.dp)); Text("weather · Mobile Tina", color = p.muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable private fun V5ThemeChip(title: String, selected: Boolean, p: V5P, icon: ImageVector, onClick: () -> Unit) {
    Row(Modifier.clip(RoundedCornerShape(50)).background(if(selected)p.selected else Color.Transparent).clickable(onClick=onClick).padding(horizontal=9.dp,vertical=7.dp), verticalAlignment=Alignment.CenterVertically) {
        Icon(icon,null,tint=if(selected)Color(0xFF3367D6) else p.muted,modifier=Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text(title,color=if(selected)p.text else p.muted,style=MaterialTheme.typography.bodySmall,fontWeight=if(selected)FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable private fun V5GradientButton(title:String, subtitle:String?, badge:String, gradient:List<Color>, onClick:()->Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(19.dp)).background(Brush.horizontalGradient(gradient)).clickable(onClick=onClick).padding(horizontal=14.dp,vertical=if(subtitle==null)15.dp else 13.dp), verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).background(Color.White.copy(alpha=.18f),CircleShape),contentAlignment=Alignment.Center){Text(badge,color=Color.White,fontWeight=FontWeight.Black,fontSize=13.sp)}
        Spacer(Modifier.width(11.dp)); Column(Modifier.weight(1f)){Text(title,color=Color.White,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium); if(!subtitle.isNullOrBlank())Text(subtitle,color=Color.White.copy(alpha=.84f),style=MaterialTheme.typography.bodySmall)}; Text("‹",color=Color.White,fontSize=27.sp)
    }
}

@Composable private fun V5Instagram(handle:String, modifier:Modifier=Modifier, onClick:()->Unit) {
    Box(modifier.clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(Color(0xFF6A33A8),Color(0xFFC13584),Color(0xFFE6683C)))).clickable(onClick=onClick).padding(horizontal=4.dp,vertical=10.dp),contentAlignment=Alignment.Center){Text(handle,color=Color.White,fontWeight=FontWeight.Bold,fontSize=10.sp,maxLines=1)}
}

@Composable private fun V5WeatherIcon(code:Int,isDay:Boolean,p:V5P,bg:Color,modifier:Modifier=Modifier){
    Canvas(modifier){when(weatherKind(code)){
        WeatherKind.CLEAR->v5SunMoon(isDay,bg)
        WeatherKind.PARTLY_CLOUDY->{v5SmallSunMoon(isDay,bg);v5Cloud(Offset(size.width*.58f,size.height*.61f),size.width*.66f,false,p)}
        WeatherKind.CLOUDY->{v5Cloud(Offset(size.width*.42f,size.height*.45f),size.width*.55f,true,p);v5Cloud(Offset(size.width*.58f,size.height*.61f),size.width*.68f,false,p)}
        WeatherKind.FOG->{v5Cloud(Offset(size.width*.52f,size.height*.43f),size.width*.66f,false,p);repeat(3){i->val y=size.height*(.70f+i*.08f);drawLine(V5RainLight,Offset(size.width*.18f,y),Offset(size.width*.82f,y),4f,cap=StrokeCap.Round)}}
        WeatherKind.RAIN->{v5Cloud(Offset(size.width*.52f,size.height*.41f),size.width*.70f,false,p);v5Drop(.32f,.78f,.058f);v5Drop(.51f,.83f,.070f);v5Drop(.70f,.77f,.058f)}
        WeatherKind.STORM->{v5Cloud(Offset(size.width*.52f,size.height*.39f),size.width*.70f,true,p);v5Drop(.30f,.78f,.05f);v5Drop(.70f,.78f,.05f);val b=Path().apply{moveTo(size.width*.51f,size.height*.54f);lineTo(size.width*.40f,size.height*.73f);lineTo(size.width*.50f,size.height*.73f);lineTo(size.width*.43f,size.height*.92f);lineTo(size.width*.65f,size.height*.66f);lineTo(size.width*.54f,size.height*.66f);close()};drawPath(b,V5Lightning)}
        WeatherKind.SNOW->{v5Cloud(Offset(size.width*.52f,size.height*.41f),size.width*.70f,false,p);listOf(.32f,.51f,.70f).forEachIndexed{i,x->val c=Offset(size.width*x,size.height*(.78f+(i%2)*.04f));drawCircle(V5RainLight,size.width*.035f,c);drawCircle(Color.White,size.width*.016f,c)}}
    }}
}

private fun DrawScope.v5SunMoon(day:Boolean,bg:Color){if(!day){val c=Offset(size.width*.53f,size.height*.48f);val r=size.minDimension*.21f;drawCircle(Color(0xFFDCE6FF),r,c);drawCircle(bg,r*.82f,Offset(c.x-r*.40f,c.y-r*.20f));return};val c=Offset(size.width*.50f,size.height*.48f);val r=size.minDimension*.19f;repeat(8){i->val a=Math.toRadians(i*45.0);drawLine(V5Sun,Offset(c.x+cos(a).toFloat()*r*1.45f,c.y+sin(a).toFloat()*r*1.45f),Offset(c.x+cos(a).toFloat()*r*2.15f,c.y+sin(a).toFloat()*r*2.15f),r*.28f,cap=StrokeCap.Round)};drawCircle(Brush.radialGradient(listOf(Color(0xFFFFE082),V5Sun,V5SunDeep),c,r*1.2f),r,c)}
private fun DrawScope.v5SmallSunMoon(day:Boolean,bg:Color){val c=Offset(size.width*.35f,size.height*.34f);val r=size.minDimension*.14f;if(!day){drawCircle(Color(0xFFDCE6FF),r,c);drawCircle(bg,r*.80f,Offset(c.x-r*.38f,c.y-r*.18f));return};repeat(8){i->val a=Math.toRadians(i*45.0);drawLine(V5Sun,Offset(c.x+cos(a).toFloat()*r*1.35f,c.y+sin(a).toFloat()*r*1.35f),Offset(c.x+cos(a).toFloat()*r*1.95f,c.y+sin(a).toFloat()*r*1.95f),r*.24f,cap=StrokeCap.Round)};drawCircle(V5Sun,r,c)}
private fun DrawScope.v5Cloud(c:Offset,w:Float,dark:Boolean,p:V5P){val f=if(dark)p.storm else p.cloud;val s=if(dark)p.storm.copy(alpha=.72f) else p.cloudShade;val h=w*.40f;drawRoundRect(s,Offset(c.x-w*.47f,c.y+h*.07f),Size(w*.94f,h*.62f),CornerRadius(h*.34f));drawRoundRect(f,Offset(c.x-w*.50f,c.y-h*.04f),Size(w,h*.62f),CornerRadius(h*.34f));drawCircle(f,w*.19f,Offset(c.x-w*.22f,c.y-h*.06f));drawCircle(f,w*.25f,Offset(c.x+w*.05f,c.y-h*.19f));drawCircle(f,w*.17f,Offset(c.x+w*.29f,c.y-h*.01f));drawArc(Color.White.copy(alpha=if(dark).14f else .62f),190f,88f,false,Offset(c.x-w*.37f,c.y-h*.31f),Size(w*.59f,h*.70f),style=Stroke(width=2.4f,cap=StrokeCap.Round))}
private fun DrawScope.v5Drop(x:Float,y:Float,k:Float){val cx=size.width*x;val cy=size.height*y;val r=size.width*k;val path=Path().apply{moveTo(cx,cy-r*1.55f);cubicTo(cx-r*1.05f,cy-r*.25f,cx-r*1.05f,cy+r*.90f,cx,cy+r*1.10f);cubicTo(cx+r*1.05f,cy+r*.90f,cx+r*1.05f,cy-r*.25f,cx,cy-r*1.55f);close()};drawPath(path,V5Rain);drawCircle(V5RainLight.copy(alpha=.86f),r*.28f,Offset(cx-r*.18f,cy-r*.43f))}
private fun v5Line(pts:List<Offset>)=Path().apply{if(pts.isEmpty())return@apply;moveTo(pts.first().x,pts.first().y);for(i in 1 until pts.size){val a=pts[i-1];val b=pts[i];val m=(a.x+b.x)/2;cubicTo(m,a.y,m,b.y,b.x,b.y)}}
private fun v5Area(pts:List<Offset>,bottom:Float)=Path().apply{if(pts.isEmpty())return@apply;moveTo(pts.first().x,bottom);lineTo(pts.first().x,pts.first().y);for(i in 1 until pts.size){val a=pts[i-1];val b=pts[i];val m=(a.x+b.x)/2;cubicTo(m,a.y,m,b.y,b.x,b.y)};lineTo(pts.last().x,bottom);close()}
private fun v5IsDay(time:String)=((time.substringAfter('T',"12:00").take(2).toIntOrNull()?:12) in 7..18)
