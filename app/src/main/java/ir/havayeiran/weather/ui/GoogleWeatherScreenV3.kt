package ir.havayeiran.weather.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import ir.havayeiran.weather.data.WeatherLocation
import ir.havayeiran.weather.data.weatherDescription
import ir.havayeiran.weather.data.weatherKind
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

private val V3Bg = Color(0xFF202124)
private val V3Panel = Color(0xFF292A2D)
private val V3Selected = Color(0xFF303134)
private val V3Text = Color(0xFFF1F3F4)
private val V3Muted = Color(0xFF9AA0A6)
private val V3Gold = Color(0xFFFFC107)
private val V3Sun = Color(0xFFFFA726)
private val V3SunDeep = Color(0xFFFF8F00)
private val V3Rain = Color(0xFF1A73E8)
private val V3RainLight = Color(0xFF64B5F6)
private val V3Cloud = Color(0xFFF1F3F4)
private val V3CloudShade = Color(0xFFBDC1C6)
private val V3Storm = Color(0xFF8F969F)

@Composable
fun GoogleWeatherScreenV3(
    state: WeatherUiState,
    onSearchChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSearchResult: (CitySearchResult) -> Unit,
    onRefresh: () -> Unit,
    onLocate: () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        var searchOpen by remember { mutableStateOf(false) }
        var selectedDay by remember(state.selectedLocation.latitude, state.selectedLocation.longitude) { mutableStateOf(0) }

        Box(Modifier.fillMaxSize().background(V3Bg)) {
            LazyColumn(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
                contentPadding = PaddingValues(bottom = 28.dp)
            ) {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.LocationOn, null, tint = V3Text, modifier = Modifier.size(21.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (state.selectedLocation.province.isBlank()) state.selectedLocation.name else "${state.selectedLocation.name}، استان ${state.selectedLocation.province}",
                            Modifier.weight(1f), V3Text, fontWeight = FontWeight.Bold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text("انتخاب منطقه", color = V3RainLight, style = MaterialTheme.typography.bodySmall)
                        IconButton(onClick = { searchOpen = !searchOpen; if (!searchOpen) onClearSearch() }) {
                            Icon(if (searchOpen) Icons.Rounded.Close else Icons.Rounded.Search, null, tint = V3Text)
                        }
                        IconButton(onClick = onLocate) { Icon(Icons.Rounded.MyLocation, null, tint = V3Text) }
                        IconButton(onClick = onRefresh) { Icon(Icons.Rounded.Refresh, null, tint = V3Text) }
                    }
                }

                if (searchOpen) item {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp)) {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = onSearchChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("جستجوی شهر؛ مثلاً رامسر، رشت یا تهران") },
                            leadingIcon = { Icon(Icons.Rounded.Search, null) },
                            trailingIcon = { if (state.searchQuery.isNotEmpty()) IconButton(onClick = onClearSearch) { Icon(Icons.Rounded.Close, null) } },
                            shape = RoundedCornerShape(18.dp), singleLine = true
                        )
                        if (state.searchQuery.trim().length >= 2) {
                            Card(
                                Modifier.fillMaxWidth().padding(top = 6.dp),
                                colors = CardDefaults.cardColors(containerColor = V3Panel),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(Modifier.padding(6.dp)) {
                                    if (state.isSearching) {
                                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.Center) {
                                            CircularProgressIndicator(Modifier.size(18.dp), color = V3Gold, strokeWidth = 2.dp)
                                        }
                                    } else state.searchResults.take(7).forEach { result ->
                                        Row(
                                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable {
                                                selectedDay = 0; onSearchResult(result); searchOpen = false
                                            }.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Rounded.LocationOn, null, tint = V3RainLight, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(result.name, color = V3Text, fontWeight = FontWeight.Bold)
                                            Spacer(Modifier.width(6.dp))
                                            Text(result.province.ifBlank { "ایران" }, color = V3Muted, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (state.isLoading && state.weather == null) item {
                    Column(Modifier.fillMaxWidth().padding(top = 90.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = V3Gold)
                        Spacer(Modifier.height(12.dp)); Text("در حال دریافت اطلاعات هوا…", color = V3Muted)
                    }
                } else state.weather?.let { bundle ->
                    val dayIndex = selectedDay.coerceIn(0, max(bundle.daily.lastIndex, 0))
                    val day = bundle.daily.getOrNull(dayIndex)
                    val hours = day?.let { d -> bundle.hourly.filter { it.time.startsWith(d.date) } }.orEmpty()
                    if (day != null) {
                        item {
                            val code = if (dayIndex == 0) bundle.current.weatherCode else day.weatherCode
                            val temp = if (dayIndex == 0) bundle.current.temperature else day.maxTemperature
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(.8f)) {
                                    Text("آب‌وهوا", color = V3Text, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                                    Text(forecastDayName(day.date, dayIndex), color = V3Muted, fontSize = 15.sp)
                                    Text(weatherDescription(code), color = V3Muted, fontSize = 16.sp)
                                }
                                Column(Modifier.weight(1.2f), horizontalAlignment = Alignment.End) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(temp.fa(), color = V3Text, fontFamily = Vazirmatn, fontSize = 68.sp, lineHeight = 68.sp, fontWeight = FontWeight.Light)
                                            Text("°C | °F", color = V3Muted, style = MaterialTheme.typography.bodySmall)
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        GoogleLikeColorIcon(code, true, Modifier.size(108.dp))
                                    }
                                    Text("بارش: ${day.precipitationProbability.fa()}٪", color = V3Muted)
                                    Text("رطوبت: ${bundle.current.humidity.fa()}٪", color = V3Muted)
                                    Text("باد: ${day.maxWindSpeed.fa()} km/h", color = V3Muted)
                                }
                            }
                        }

                        item { TemperatureCurve(hours) }

                        item {
                            LazyRow(
                                Modifier.fillMaxWidth().padding(top = 16.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                itemsIndexed(bundle.daily.take(8)) { index, d ->
                                    val active = index == dayIndex
                                    Column(
                                        Modifier.width(102.dp).clip(RoundedCornerShape(16.dp))
                                            .background(if (active) V3Selected else Color.Transparent)
                                            .clickable { selectedDay = index }.padding(vertical = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(forecastDayName(d.date, index), color = V3Text, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.height(4.dp))
                                        GoogleLikeColorIcon(d.weatherCode, true, Modifier.size(66.dp))
                                        Text(weatherDescription(d.weatherCode), color = V3Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                        Row { Text("${d.minTemperature.fa()}°", color = V3Muted); Spacer(Modifier.width(5.dp)); Text("${d.maxTemperature.fa()}°", color = V3Text, fontWeight = FontWeight.Bold) }
                                    }
                                }
                            }
                        }

                        item {
                            Column(Modifier.fillMaxWidth().padding(top = 24.dp)) {
                                Text("پیش‌بینی ساعتی ${shortPersianDate(day.date)}", Modifier.padding(horizontal = 18.dp), color = V3Text, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(10.dp))
                                LazyRow(contentPadding = PaddingValues(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    items(hours) { h ->
                                        Card(
                                            Modifier.width(94.dp),
                                            colors = CardDefaults.cardColors(containerColor = V3Panel),
                                            shape = RoundedCornerShape(18.dp)
                                        ) {
                                            Column(Modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(formatTime(h.time), color = V3Muted, style = MaterialTheme.typography.bodySmall)
                                                GoogleLikeColorIcon(h.weatherCode, hourIsDayV3(h.time), Modifier.size(58.dp))
                                                Text("${h.temperature.fa()}°", color = V3Text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                                if (h.precipitationProbability > 0) Text("${h.precipitationProbability.fa()}٪", color = V3RainLight, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TemperatureCurve(hours: List<HourlyWeather>) {
    if (hours.isEmpty()) return
    val idx = listOf(1,4,7,10,13,16,19,22).filter { it < hours.size }
    val sample = if (idx.isEmpty()) hours.take(8) else idx.map { hours[it] }
    val values = sample.map { it.temperature }
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
            values.forEach { Text("${it.fa()}°", Modifier.weight(1f), color = V3Text.copy(alpha=.8f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall) }
        }
        Canvas(Modifier.fillMaxWidth().height(112.dp).padding(horizontal = 18.dp, vertical = 7.dp)) {
            if (values.size < 2) return@Canvas
            val minV = (values.minOrNull() ?: 0.0) - 2
            val maxV = (values.maxOrNull() ?: 1.0) + 2
            val span = max(maxV - minV, 1.0)
            val pts = values.mapIndexed { i, v -> Offset(i * size.width / values.lastIndex, size.height - (((v-minV)/span).toFloat()) * size.height * .68f - size.height * .10f) }
            val line = Path().apply { moveTo(pts.first().x, pts.first().y); for (i in 1 until pts.size) { val p=pts[i-1]; val c=pts[i]; val mx=(p.x+c.x)/2; cubicTo(mx,p.y,mx,c.y,c.x,c.y) } }
            val area = Path().apply { moveTo(pts.first().x,size.height); lineTo(pts.first().x,pts.first().y); for(i in 1 until pts.size){val p=pts[i-1];val c=pts[i];val mx=(p.x+c.x)/2;cubicTo(mx,p.y,mx,c.y,c.x,c.y)}; lineTo(pts.last().x,size.height); close() }
            drawPath(area, Brush.verticalGradient(listOf(V3Gold.copy(alpha=.30f), V3Gold.copy(alpha=.03f))))
            drawPath(line, V3Gold, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) { sample.forEach { Text(formatTime(it.time), Modifier.weight(1f), color = V3Muted, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall) } }
    }
}

@Composable
private fun GoogleLikeColorIcon(code: Int, isDay: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        when (weatherKind(code)) {
            WeatherKind.CLEAR -> drawSunOrMoonV3(isDay)
            WeatherKind.PARTLY_CLOUDY -> { drawSmallSunV3(isDay); drawCloudV3(Offset(size.width*.58f,size.height*.61f),size.width*.66f,false) }
            WeatherKind.CLOUDY -> { drawCloudV3(Offset(size.width*.42f,size.height*.45f),size.width*.55f,true); drawCloudV3(Offset(size.width*.58f,size.height*.61f),size.width*.68f,false) }
            WeatherKind.FOG -> { drawCloudV3(Offset(size.width*.52f,size.height*.43f),size.width*.66f,false); repeat(3){i-> val y=size.height*(.70f+i*.08f); drawLine(V3RainLight.copy(alpha=.75f),Offset(size.width*.18f,y),Offset(size.width*.82f,y),4f,cap=StrokeCap.Round)} }
            WeatherKind.RAIN -> { drawCloudV3(Offset(size.width*.52f,size.height*.41f),size.width*.70f,false); drawDropV3(.32f,.78f,.058f); drawDropV3(.51f,.83f,.070f); drawDropV3(.70f,.77f,.058f) }
            WeatherKind.STORM -> { drawCloudV3(Offset(size.width*.52f,size.height*.39f),size.width*.70f,true); drawDropV3(.30f,.78f,.05f); drawDropV3(.70f,.78f,.05f); val b=Path().apply{moveTo(size.width*.51f,size.height*.54f);lineTo(size.width*.40f,size.height*.73f);lineTo(size.width*.50f,size.height*.73f);lineTo(size.width*.43f,size.height*.92f);lineTo(size.width*.65f,size.height*.66f);lineTo(size.width*.54f,size.height*.66f);close()};drawPath(b,Color(0xFFFFD54F)) }
            WeatherKind.SNOW -> { drawCloudV3(Offset(size.width*.52f,size.height*.41f),size.width*.70f,false); listOf(.32f,.51f,.70f).forEachIndexed{i,x->drawCircle(V3RainLight, size.width*.035f, Offset(size.width*x,size.height*(.78f+(i%2)*.04f)));drawCircle(Color.White,size.width*.016f,Offset(size.width*x,size.height*(.78f+(i%2)*.04f)))} }
        }
    }
}

private fun DrawScope.drawSunOrMoonV3(isDay:Boolean){ if(!isDay){val c=Offset(size.width*.53f,size.height*.48f);val r=size.minDimension*.21f;drawCircle(Color(0xFFDCE6FF),r,c);drawCircle(V3Bg,r*.82f,Offset(c.x-r*.40f,c.y-r*.20f));return}; val c=Offset(size.width*.50f,size.height*.48f); val r=size.minDimension*.19f; repeat(8){i->val a=Math.toRadians(i*45.0);drawLine(V3Sun,Offset(c.x+cos(a).toFloat()*r*1.45f,c.y+sin(a).toFloat()*r*1.45f),Offset(c.x+cos(a).toFloat()*r*2.15f,c.y+sin(a).toFloat()*r*2.15f),r*.28f,cap=StrokeCap.Round)};drawCircle(V3Sun.copy(alpha=.18f),r*1.55f,c);drawCircle(Brush.radialGradient(listOf(Color(0xFFFFD54F),V3Sun,V3SunDeep),c,r*1.2f),r,c) }
private fun DrawScope.drawSmallSunV3(isDay:Boolean){ if(!isDay){drawSunOrMoonV3(false);return}; val c=Offset(size.width*.35f,size.height*.34f);val r=size.minDimension*.14f;repeat(8){i->val a=Math.toRadians(i*45.0);drawLine(V3Sun,Offset(c.x+cos(a).toFloat()*r*1.35f,c.y+sin(a).toFloat()*r*1.35f),Offset(c.x+cos(a).toFloat()*r*1.95f,c.y+sin(a).toFloat()*r*1.95f),r*.24f,cap=StrokeCap.Round)};drawCircle(V3Sun,r,c) }
private fun DrawScope.drawCloudV3(center:Offset,width:Float,dark:Boolean){val front=if(dark)V3Storm else V3Cloud;val shadow=if(dark)Color(0xFF747B84) else V3CloudShade;val h=width*.40f;drawRoundRect(shadow,Offset(center.x-width*.47f,center.y+h*.07f),Size(width*.94f,h*.62f),CornerRadius(h*.34f));drawRoundRect(front,Offset(center.x-width*.50f,center.y-h*.04f),Size(width,h*.62f),CornerRadius(h*.34f));drawCircle(front,width*.19f,Offset(center.x-width*.22f,center.y-h*.06f));drawCircle(front,width*.25f,Offset(center.x+width*.05f,center.y-h*.19f));drawCircle(front,width*.17f,Offset(center.x+width*.29f,center.y-h*.01f));drawArc(Color.White.copy(alpha=if(dark).14f else .34f),190f,88f,false,Offset(center.x-width*.37f,center.y-h*.31f),Size(width*.59f,h*.70f),style=Stroke(width=2.4f,cap=StrokeCap.Round))}
private fun DrawScope.drawDropV3(x:Float,y:Float,r:Float){val cx=size.width*x;val cy=size.height*y;val rr=size.width*r;val p=Path().apply{moveTo(cx,cy-rr*1.55f);cubicTo(cx-rr*1.05f,cy-rr*.25f,cx-rr*1.05f,cy+rr*.90f,cx,cy+rr*1.10f);cubicTo(cx+rr*1.05f,cy+rr*.90f,cx+rr*1.05f,cy-rr*.25f,cx,cy-rr*1.55f);close()};drawPath(p,V3Rain);drawCircle(V3RainLight.copy(alpha=.85f),rr*.28f,Offset(cx-rr*.18f,cy-rr*.43f))}
private fun hourIsDayV3(time:String):Boolean=(time.substringAfter('T',"12:00").take(2).toIntOrNull()?:12) in 7..18
