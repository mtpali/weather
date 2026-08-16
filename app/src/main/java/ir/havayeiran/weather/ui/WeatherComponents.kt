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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import ir.havayeiran.weather.data.DailyWeather
import ir.havayeiran.weather.data.HourlyWeather
import ir.havayeiran.weather.data.WeatherKind
import ir.havayeiran.weather.data.WeatherLocation
import ir.havayeiran.weather.data.weatherDescription
import ir.havayeiran.weather.data.weatherKind
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun WeatherArtwork(
    kind: WeatherKind,
    isDay: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "weather-art")
    val cloudShift by transition.animateFloat(
        initialValue = -8f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(5500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cloud-shift"
    )
    val fall by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fall"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        if (isDay && kind in listOf(WeatherKind.CLEAR, WeatherKind.PARTLY_CLOUDY)) {
            drawSun(Offset(w * .67f, h * .31f), w * .12f)
        } else if (!isDay && kind in listOf(WeatherKind.CLEAR, WeatherKind.PARTLY_CLOUDY)) {
            drawMoon(Offset(w * .68f, h * .29f), w * .115f)
        }

        when (kind) {
            WeatherKind.CLEAR -> Unit
            WeatherKind.PARTLY_CLOUDY -> drawCloud(Offset(w * .47f + cloudShift, h * .56f), w * .28f, Color(0xFFF1F5FA))
            WeatherKind.CLOUDY -> {
                drawCloud(Offset(w * .58f + cloudShift * .5f, h * .43f), w * .25f, Color(0xFFCBD5E2).copy(alpha = .72f))
                drawCloud(Offset(w * .42f + cloudShift, h * .59f), w * .31f, Color(0xFFE7EDF5))
            }
            WeatherKind.FOG -> {
                drawCloud(Offset(w * .48f + cloudShift * .4f, h * .42f), w * .28f, Color(0xFFD9E0E7).copy(alpha = .75f))
                repeat(3) { i ->
                    val y = h * (.62f + i * .1f)
                    drawLine(
                        color = Color.White.copy(alpha = .35f - i * .06f),
                        start = Offset(w * .22f, y),
                        end = Offset(w * .78f, y),
                        strokeWidth = 5f,
                        cap = StrokeCap.Round
                    )
                }
            }
            WeatherKind.RAIN, WeatherKind.STORM -> {
                drawCloud(Offset(w * .48f + cloudShift * .4f, h * .42f), w * .31f, Color(0xFFD9E2EC))
                repeat(7) { i ->
                    val x = w * (.25f + i * .08f)
                    val y = h * (.61f + ((i * .17f + fall) % 1f) * .23f)
                    drawLine(
                        color = Color(0xFF63B9FF).copy(alpha = .88f),
                        start = Offset(x, y),
                        end = Offset(x - 8f, y + 22f),
                        strokeWidth = 5f,
                        cap = StrokeCap.Round
                    )
                }
                if (kind == WeatherKind.STORM) {
                    val bolt = androidx.compose.ui.graphics.Path().apply {
                        moveTo(w * .50f, h * .57f)
                        lineTo(w * .43f, h * .72f)
                        lineTo(w * .51f, h * .72f)
                        lineTo(w * .45f, h * .88f)
                        lineTo(w * .61f, h * .67f)
                        lineTo(w * .53f, h * .67f)
                        close()
                    }
                    drawPath(bolt, Color(0xFFFFD85D))
                }
            }
            WeatherKind.SNOW -> {
                drawCloud(Offset(w * .48f + cloudShift * .35f, h * .41f), w * .31f, Color(0xFFE7EDF3))
                repeat(8) { i ->
                    val x = w * (.22f + i * .075f)
                    val y = h * (.59f + ((i * .13f + fall) % 1f) * .28f)
                    drawCircle(Color.White.copy(alpha = .85f), radius = 5f, center = Offset(x, y))
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
        if (isDay && kind in listOf(WeatherKind.CLEAR, WeatherKind.PARTLY_CLOUDY)) {
            drawSun(Offset(w * .64f, h * .35f), w * .16f)
        } else if (!isDay && kind in listOf(WeatherKind.CLEAR, WeatherKind.PARTLY_CLOUDY)) {
            drawMoon(Offset(w * .64f, h * .33f), w * .15f)
        }
        when (kind) {
            WeatherKind.CLEAR -> Unit
            WeatherKind.PARTLY_CLOUDY -> drawCloud(Offset(w * .46f, h * .58f), w * .34f, Color(0xFFE8EEF5))
            WeatherKind.CLOUDY -> drawCloud(Offset(w * .50f, h * .55f), w * .36f, Color(0xFFD7DFE9))
            WeatherKind.FOG -> {
                drawCloud(Offset(w * .50f, h * .45f), w * .34f, Color(0xFFD7DFE9))
                repeat(2) { i ->
                    drawLine(Color(0xFFB8C5D4), Offset(w * .2f, h * (.72f + i * .1f)), Offset(w * .8f, h * (.72f + i * .1f)), 3f, cap = StrokeCap.Round)
                }
            }
            WeatherKind.RAIN, WeatherKind.STORM -> {
                drawCloud(Offset(w * .50f, h * .45f), w * .35f, Color(0xFFD7DFE9))
                repeat(3) { i ->
                    val x = w * (.34f + i * .17f)
                    drawLine(Color(0xFF5FB8FF), Offset(x, h * .67f), Offset(x - 3f, h * .82f), 4f, cap = StrokeCap.Round)
                }
            }
            WeatherKind.SNOW -> {
                drawCloud(Offset(w * .50f, h * .43f), w * .35f, Color(0xFFE7EDF3))
                repeat(3) { i -> drawCircle(Color.White, 3.5f, Offset(w * (.34f + i * .17f), h * .77f)) }
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
            WeatherGlyph(hour.weatherCode, modifier = Modifier.size(45.dp).padding(top = 5.dp))
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
        WeatherGlyph(day.weatherCode, modifier = Modifier.size(46.dp))
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

private fun DrawScope.drawSun(center: Offset, radius: Float) {
    repeat(8) { i ->
        val angle = (i * 45.0) * PI / 180.0
        val start = Offset(center.x + cos(angle).toFloat() * radius * 1.35f, center.y + sin(angle).toFloat() * radius * 1.35f)
        val end = Offset(center.x + cos(angle).toFloat() * radius * 1.75f, center.y + sin(angle).toFloat() * radius * 1.75f)
        drawLine(Color(0xFFFFD867), start, end, radius * .11f, cap = StrokeCap.Round)
    }
    drawCircle(
        brush = Brush.radialGradient(listOf(Color(0xFFFFF1A7), Color(0xFFFFC84D)), center = center, radius = radius * 1.4f),
        radius = radius,
        center = center
    )
}

private fun DrawScope.drawMoon(center: Offset, radius: Float) {
    drawCircle(Color(0xFFE8EEFF), radius, center)
    drawCircle(Color(0xFF243A5D), radius * .88f, Offset(center.x - radius * .38f, center.y - radius * .22f))
}

private fun DrawScope.drawCloud(center: Offset, width: Float, color: Color) {
    val height = width * .38f
    drawRoundRect(
        color = color,
        topLeft = Offset(center.x - width * .5f, center.y - height * .08f),
        size = Size(width, height * .72f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(height * .35f, height * .35f)
    )
    drawCircle(color, width * .19f, Offset(center.x - width * .20f, center.y - height * .04f))
    drawCircle(color, width * .24f, Offset(center.x + width * .08f, center.y - height * .17f))
    drawCircle(color.copy(alpha = .96f), width * .16f, Offset(center.x + width * .28f, center.y + height * .01f))
    drawArc(
        color = Color.White.copy(alpha = .10f),
        startAngle = 190f,
        sweepAngle = 95f,
        useCenter = false,
        topLeft = Offset(center.x - width * .38f, center.y - height * .30f),
        size = Size(width * .62f, height * .7f),
        style = Stroke(width = 2.5f, cap = StrokeCap.Round)
    )
}
