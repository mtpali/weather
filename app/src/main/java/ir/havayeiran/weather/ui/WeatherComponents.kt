package ir.havayeiran.weather.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.havayeiran.weather.data.DailyWeather
import ir.havayeiran.weather.data.HourlyWeather
import ir.havayeiran.weather.data.WeatherKind
import ir.havayeiran.weather.data.WeatherLocation
import ir.havayeiran.weather.data.weatherDescription
import ir.havayeiran.weather.data.weatherKind
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val SunOrange = Color(0xFFFF8A00)
private val SunYellow = Color(0xFFFFC928)
private val CloudTop = Color(0xFFF2F3F5)
private val CloudMid = Color(0xFFD9DCE1)
private val CloudRain = Color(0xFFC8CCD2)
private val RainBlue = Color(0xFF0B73E0)

@Composable
fun WeatherArtwork(
    kind: WeatherKind,
    isDay: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "weather-art")
    val cloudShift by transition.animateFloat(
        initialValue = -4f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(5200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cloud-shift"
    )
    val dropPulse by transition.animateFloat(
        initialValue = .92f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drop-pulse"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        when (kind) {
            WeatherKind.CLEAR -> {
                if (isDay) drawGoogleSun(Offset(w * .52f, h * .50f), w * .24f, rays = true)
                else drawMoon(Offset(w * .54f, h * .48f), w * .23f)
            }

            WeatherKind.PARTLY_CLOUDY -> {
                if (isDay) drawGoogleSun(Offset(w * .58f, h * .36f), w * .21f, rays = false)
                else drawMoon(Offset(w * .61f, h * .34f), w * .19f)
                drawGoogleCloud(
                    center = Offset(w * .46f + cloudShift, h * .62f),
                    width = w * .58f,
                    topColor = CloudTop,
                    bottomColor = CloudMid
                )
            }

            WeatherKind.CLOUDY -> {
                drawGoogleCloud(
                    center = Offset(w * .50f + cloudShift * .25f, h * .55f),
                    width = w * .67f,
                    topColor = CloudMid,
                    bottomColor = Color(0xFFBFC3C9)
                )
            }

            WeatherKind.FOG -> {
                drawGoogleCloud(
                    center = Offset(w * .50f + cloudShift * .2f, h * .42f),
                    width = w * .61f,
                    topColor = CloudMid,
                    bottomColor = Color(0xFFBEC4CB)
                )
                repeat(3) { i ->
                    val y = h * (.68f + i * .09f)
                    drawLine(
                        color = Color.White.copy(alpha = .42f - i * .07f),
                        start = Offset(w * .20f, y),
                        end = Offset(w * .80f, y),
                        strokeWidth = w * .035f,
                        cap = StrokeCap.Round
                    )
                }
            }

            WeatherKind.RAIN -> {
                drawGoogleCloud(
                    center = Offset(w * .50f + cloudShift * .18f, h * .44f),
                    width = w * .66f,
                    topColor = CloudMid,
                    bottomColor = CloudRain
                )
                drawDrop(
                    center = Offset(w * .58f, h * .79f),
                    radius = w * .115f * dropPulse,
                    color = RainBlue
                )
            }

            WeatherKind.STORM -> {
                drawGoogleCloud(
                    center = Offset(w * .50f + cloudShift * .18f, h * .42f),
                    width = w * .66f,
                    topColor = CloudMid,
                    bottomColor = CloudRain
                )
                drawDrop(Offset(w * .68f, h * .78f), w * .085f, RainBlue)
                val bolt = Path().apply {
                    moveTo(w * .46f, h * .60f)
                    lineTo(w * .36f, h * .77f)
                    lineTo(w * .47f, h * .77f)
                    lineTo(w * .38f, h * .94f)
                    lineTo(w * .59f, h * .70f)
                    lineTo(w * .48f, h * .70f)
                    close()
                }
                drawPath(bolt, SunYellow)
            }

            WeatherKind.SNOW -> {
                drawGoogleCloud(
                    center = Offset(w * .50f + cloudShift * .18f, h * .42f),
                    width = w * .64f,
                    topColor = CloudTop,
                    bottomColor = CloudMid
                )
                repeat(3) { i ->
                    val x = w * (.34f + i * .16f)
                    drawSnowflake(Offset(x, h * .78f), w * .055f)
                }
            }
        }
    }
}

@Composable
fun WeatherGlyph(
    code: Int,
    isDay: Boolean = true,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val kind = weatherKind(code)
        val w = size.width
        val h = size.height

        when (kind) {
            WeatherKind.CLEAR -> {
                if (isDay) drawGoogleSun(Offset(w * .50f, h * .49f), w * .25f, rays = true)
                else drawMoon(Offset(w * .52f, h * .48f), w * .23f)
            }

            WeatherKind.PARTLY_CLOUDY -> {
                if (isDay) drawGoogleSun(Offset(w * .58f, h * .34f), w * .22f, rays = false)
                else drawMoon(Offset(w * .61f, h * .32f), w * .20f)
                drawGoogleCloud(
                    center = Offset(w * .45f, h * .62f),
                    width = w * .66f,
                    topColor = Color(0xFFF8F8F8),
                    bottomColor = Color(0xFFD5D8DC)
                )
            }

            WeatherKind.CLOUDY -> {
                drawGoogleCloud(
                    center = Offset(w * .50f, h * .54f),
                    width = w * .72f,
                    topColor = Color(0xFFE5E7EA),
                    bottomColor = Color(0xFFC6CAD0)
                )
            }

            WeatherKind.FOG -> {
                drawGoogleCloud(
                    center = Offset(w * .50f, h * .39f),
                    width = w * .67f,
                    topColor = Color(0xFFE0E3E7),
                    bottomColor = Color(0xFFC4C9D0)
                )
                repeat(2) { i ->
                    drawLine(
                        color = Color(0xFFB9BEC6),
                        start = Offset(w * .18f, h * (.70f + i * .12f)),
                        end = Offset(w * .82f, h * (.70f + i * .12f)),
                        strokeWidth = w * .045f,
                        cap = StrokeCap.Round
                    )
                }
            }

            WeatherKind.RAIN -> {
                drawGoogleCloud(
                    center = Offset(w * .50f, h * .42f),
                    width = w * .70f,
                    topColor = Color(0xFFD9DCE0),
                    bottomColor = Color(0xFFBEC3C9)
                )
                drawDrop(Offset(w * .57f, h * .82f), w * .12f, RainBlue)
            }

            WeatherKind.STORM -> {
                drawGoogleCloud(
                    center = Offset(w * .50f, h * .40f),
                    width = w * .70f,
                    topColor = Color(0xFFD5D8DD),
                    bottomColor = Color(0xFFB8BDC4)
                )
                val bolt = Path().apply {
                    moveTo(w * .48f, h * .58f)
                    lineTo(w * .36f, h * .75f)
                    lineTo(w * .47f, h * .75f)
                    lineTo(w * .39f, h * .92f)
                    lineTo(w * .60f, h * .69f)
                    lineTo(w * .49f, h * .69f)
                    close()
                }
                drawPath(bolt, SunYellow)
                drawDrop(Offset(w * .69f, h * .80f), w * .08f, RainBlue)
            }

            WeatherKind.SNOW -> {
                drawGoogleCloud(
                    center = Offset(w * .50f, h * .40f),
                    width = w * .69f,
                    topColor = CloudTop,
                    bottomColor = CloudMid
                )
                repeat(3) { i -> drawSnowflake(Offset(w * (.33f + i * .17f), h * .80f), w * .055f) }
            }
        }
    }
}

@Composable
fun CityChip(
    location: WeatherLocation,
    selected: Boolean,
    favorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .16f) else MaterialTheme.colorScheme.surface.copy(alpha = .58f)
    val border = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .42f) else MaterialTheme.colorScheme.outline.copy(alpha = .35f)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (favorite) {
            Icon(Icons.Rounded.Favorite, null, tint = Color(0xFFFF7195), modifier = Modifier.size(13.dp))
        }
        Text(
            text = location.name,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MetricCard(
    emoji: String,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    note: String? = null
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .34f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .22f))
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = 15.sp)
                Spacer(Modifier.width(6.dp))
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
            if (!note.isNullOrBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun HourlyItem(hour: HourlyWeather, isNow: Boolean, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.width(86.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isNow) MaterialTheme.colorScheme.primary.copy(alpha = .14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .28f)
        ),
        border = BorderStroke(
            1.dp,
            if (isNow) MaterialTheme.colorScheme.primary.copy(alpha = .35f) else MaterialTheme.colorScheme.outline.copy(alpha = .18f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(if (isNow) "اکنون" else formatTime(hour.time), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            WeatherGlyph(hour.weatherCode, modifier = Modifier.size(48.dp).padding(top = 4.dp))
            Text("${hour.temperature.fa()}°", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            Text("💧 ${hour.precipitationProbability.fa()}٪", style = MaterialTheme.typography.bodySmall, color = Color(0xFF74BFFF))
        }
    }
}

@Composable
fun DailyRow(day: DailyWeather, index: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .24f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .16f), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1.15f)) {
            Text(forecastDayName(day.date, index), style = MaterialTheme.typography.titleMedium)
            Text(shortPersianDate(day.date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        WeatherGlyph(day.weatherCode, modifier = Modifier.size(50.dp))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(.9f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(weatherDescription(day.weatherCode), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Text("💧 ${day.precipitationProbability.fa()}٪", style = MaterialTheme.typography.bodySmall, color = Color(0xFF74BFFF))
        }
        Row(Modifier.weight(.8f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Text("${day.maxTemperature.fa()}°", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(7.dp))
            Text("${day.minTemperature.fa()}°", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun FavoriteButton(isFavorite: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = .12f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = if (isFavorite) "حذف از علاقه‌مندی" else "افزودن به علاقه‌مندی",
            tint = if (isFavorite) Color(0xFFFF7195) else Color.White.copy(alpha = .88f),
            modifier = Modifier.size(20.dp)
        )
    }
}

private fun DrawScope.drawGoogleSun(center: Offset, radius: Float, rays: Boolean) {
    if (rays) {
        repeat(8) { i ->
            val angle = (i * 45.0) * PI / 180.0
            val start = Offset(
                center.x + cos(angle).toFloat() * radius * 1.28f,
                center.y + sin(angle).toFloat() * radius * 1.28f
            )
            val end = Offset(
                center.x + cos(angle).toFloat() * radius * 1.55f,
                center.y + sin(angle).toFloat() * radius * 1.55f
            )
            drawLine(SunYellow, start, end, radius * .11f, cap = StrokeCap.Round)
        }
    }
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFD85B), SunOrange),
            center = Offset(center.x - radius * .24f, center.y - radius * .24f),
            radius = radius * 1.35f
        ),
        radius = radius,
        center = center
    )
}

private fun DrawScope.drawMoon(center: Offset, radius: Float) {
    drawCircle(Color(0xFFE7E9EC), radius, center)
    drawCircle(Color(0xFF8F949B).copy(alpha = .35f), radius * .12f, Offset(center.x - radius * .25f, center.y - radius * .20f))
    drawCircle(Color(0xFF8F949B).copy(alpha = .25f), radius * .08f, Offset(center.x + radius * .23f, center.y + radius * .12f))
}

private fun DrawScope.drawGoogleCloud(
    center: Offset,
    width: Float,
    topColor: Color,
    bottomColor: Color
) {
    val height = width * .43f
    val baseTop = center.y - height * .05f

    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(topColor, bottomColor),
            startY = center.y - height * .55f,
            endY = center.y + height * .48f
        ),
        topLeft = Offset(center.x - width * .50f, baseTop),
        size = Size(width, height * .66f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(height * .34f, height * .34f)
    )

    drawCircle(
        brush = Brush.verticalGradient(listOf(topColor, bottomColor)),
        radius = width * .20f,
        center = Offset(center.x - width * .20f, center.y - height * .10f)
    )
    drawCircle(
        brush = Brush.verticalGradient(listOf(topColor, bottomColor)),
        radius = width * .255f,
        center = Offset(center.x + width * .08f, center.y - height * .23f)
    )
    drawCircle(
        brush = Brush.verticalGradient(listOf(topColor, bottomColor)),
        radius = width * .17f,
        center = Offset(center.x + width * .30f, center.y - height * .02f)
    )

    drawArc(
        color = Color.White.copy(alpha = .22f),
        startAngle = 205f,
        sweepAngle = 88f,
        useCenter = false,
        topLeft = Offset(center.x - width * .38f, center.y - height * .42f),
        size = Size(width * .64f, height * .78f),
        style = Stroke(width = (width * .024f).coerceAtLeast(1.5f), cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawDrop(center: Offset, radius: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius * 1.35f)
        cubicTo(
            center.x - radius * .42f, center.y - radius * .62f,
            center.x - radius, center.y - radius * .08f,
            center.x - radius, center.y + radius * .38f
        )
        cubicTo(
            center.x - radius, center.y + radius * 1.05f,
            center.x - radius * .48f, center.y + radius * 1.38f,
            center.x, center.y + radius * 1.38f
        )
        cubicTo(
            center.x + radius * .48f, center.y + radius * 1.38f,
            center.x + radius, center.y + radius * 1.05f,
            center.x + radius, center.y + radius * .38f
        )
        cubicTo(
            center.x + radius, center.y - radius * .08f,
            center.x + radius * .42f, center.y - radius * .62f,
            center.x, center.y - radius * 1.35f
        )
        close()
    }
    drawPath(path, color)
    drawCircle(Color.White.copy(alpha = .24f), radius * .17f, Offset(center.x - radius * .31f, center.y - radius * .14f))
}

private fun DrawScope.drawSnowflake(center: Offset, radius: Float) {
    repeat(3) { i ->
        val angle = (i * 60.0) * PI / 180.0
        val dx = cos(angle).toFloat() * radius
        val dy = sin(angle).toFloat() * radius
        drawLine(Color.White, Offset(center.x - dx, center.y - dy), Offset(center.x + dx, center.y + dy), radius * .22f, cap = StrokeCap.Round)
    }
}
