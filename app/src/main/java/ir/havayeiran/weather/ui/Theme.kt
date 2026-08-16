package ir.havayeiran.weather.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import ir.havayeiran.weather.R

private val GoogleFontsProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val VazirmatnName = GoogleFont("Vazirmatn", bestEffort = true)

val Vazirmatn = FontFamily(
    androidx.compose.ui.text.googlefonts.Font(
        googleFont = VazirmatnName,
        fontProvider = GoogleFontsProvider,
        weight = FontWeight.Light
    ),
    androidx.compose.ui.text.googlefonts.Font(
        googleFont = VazirmatnName,
        fontProvider = GoogleFontsProvider,
        weight = FontWeight.Normal
    ),
    androidx.compose.ui.text.googlefonts.Font(
        googleFont = VazirmatnName,
        fontProvider = GoogleFontsProvider,
        weight = FontWeight.Medium
    ),
    androidx.compose.ui.text.googlefonts.Font(
        googleFont = VazirmatnName,
        fontProvider = GoogleFontsProvider,
        weight = FontWeight.Bold
    )
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFD000),
    onPrimary = Color(0xFF262100),
    secondary = Color(0xFF72A9FF),
    background = Color(0xFF202328),
    onBackground = Color(0xFFF1F3F4),
    surface = Color(0xFF25292F),
    onSurface = Color(0xFFF1F3F4),
    surfaceVariant = Color(0xFF2B2F36),
    onSurfaceVariant = Color(0xFFADB2BA),
    outline = Color(0xFF3B4048),
    error = Color(0xFFFF9A9A)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFFB58500),
    onPrimary = Color.White,
    secondary = Color(0xFF2866B2),
    background = Color(0xFFF4F6F8),
    onBackground = Color(0xFF202328),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF202328),
    surfaceVariant = Color(0xFFE9EDF1),
    onSurfaceVariant = Color(0xFF626870),
    outline = Color(0xFFD0D5DB),
    error = Color(0xFFB3261E)
)

private val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = Vazirmatn, fontWeight = FontWeight.Light, fontSize = 64.sp, lineHeight = 70.sp),
    displayMedium = TextStyle(fontFamily = Vazirmatn, fontWeight = FontWeight.Light, fontSize = 52.sp, lineHeight = 58.sp),
    headlineLarge = TextStyle(fontFamily = Vazirmatn, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = Vazirmatn, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = Vazirmatn, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = Vazirmatn, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 23.sp),
    bodyLarge = TextStyle(fontFamily = Vazirmatn, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = Vazirmatn, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Vazirmatn, fontWeight = FontWeight.Normal, fontSize = 10.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = Vazirmatn, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = Vazirmatn, fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 16.sp)
)

@Composable
fun HavayeIranTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
