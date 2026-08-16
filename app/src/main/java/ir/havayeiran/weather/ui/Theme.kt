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
import androidx.compose.ui.text.googlefonts.R as GoogleFontsR

private val GoogleFontsProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = GoogleFontsR.array.com_google_android_gms_fonts_certs
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
    primary = Color(0xFF8BB8FF),
    onPrimary = Color(0xFF071A32),
    secondary = Color(0xFF74DCEF),
    background = Color(0xFF06101D),
    onBackground = Color(0xFFF3F7FF),
    surface = Color(0xFF0D1A2B),
    onSurface = Color(0xFFF3F7FF),
    surfaceVariant = Color(0xFF14243A),
    onSurfaceVariant = Color(0xFFB8C8DC),
    outline = Color(0xFF31455F),
    error = Color(0xFFFF8C8C)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF2866B2),
    onPrimary = Color.White,
    secondary = Color(0xFF047D91),
    background = Color(0xFFF3F7FC),
    onBackground = Color(0xFF0E1B2A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0E1B2A),
    surfaceVariant = Color(0xFFE8F0F8),
    onSurfaceVariant = Color(0xFF52657A),
    outline = Color(0xFFC7D4E2),
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
