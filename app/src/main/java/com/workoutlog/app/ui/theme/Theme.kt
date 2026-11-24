package com.workoutlog.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class AppTheme { MENS, WOMENS }

/** Extra theme info used by charts and the PDF (which can't read MaterialTheme). */
data class ThemeStyle(
    val theme: AppTheme,
    val chartLine: Color,
    val chartGoal: Color,
    val chartFill: Color,
    val headerTitle: (String) -> String,
)

val LocalThemeStyle = staticCompositionLocalOf {
    ThemeStyle(AppTheme.MENS, Color.Blue, Color.Red, Color.Blue, { it })
}

// ---------------- Men's: "Iron" – charcoal, steel blue, ember orange ----------------
private val MensDark = darkColorScheme(
    primary = Color(0xFF5AA9FF), onPrimary = Color(0xFF00203F),
    primaryContainer = Color(0xFF1D3A5C), onPrimaryContainer = Color(0xFFD3E6FF),
    secondary = Color(0xFFFF8A3D), onSecondary = Color(0xFF3A1500),
    secondaryContainer = Color(0xFF5A2A08), onSecondaryContainer = Color(0xFFFFDBC8),
    tertiary = Color(0xFF9FB4C8),
    background = Color(0xFF111418), onBackground = Color(0xFFE3E6EA),
    surface = Color(0xFF171B20), onSurface = Color(0xFFE3E6EA),
    surfaceVariant = Color(0xFF252B33), onSurfaceVariant = Color(0xFFB9C2CC),
    surfaceContainer = Color(0xFF1C2127), surfaceContainerHigh = Color(0xFF232930),
    surfaceContainerLow = Color(0xFF171B20), surfaceContainerHighest = Color(0xFF2A3038),
    outline = Color(0xFF5B6673), outlineVariant = Color(0xFF39414B),
)
private val MensLight = lightColorScheme(
    primary = Color(0xFF1F4F82), onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E4F8), onPrimaryContainer = Color(0xFF0A2340),
    secondary = Color(0xFFD9600F), onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDCC5), onSecondaryContainer = Color(0xFF3A1500),
    tertiary = Color(0xFF4B5D6E),
    background = Color(0xFFEEF1F4), onBackground = Color(0xFF15191E),
    surface = Color(0xFFF7F9FB), onSurface = Color(0xFF15191E),
    surfaceVariant = Color(0xFFDDE3EA), onSurfaceVariant = Color(0xFF40484F),
    surfaceContainer = Color(0xFFFFFFFF), surfaceContainerHigh = Color(0xFFF1F4F7),
    surfaceContainerLow = Color(0xFFF7F9FB), surfaceContainerHighest = Color(0xFFE6EBF0),
    outline = Color(0xFF75808B), outlineVariant = Color(0xFFC3CBD3),
)

// ---------------- Women's: "Bloom" – rose, plum, blush ----------------
private val WomensLight = lightColorScheme(
    primary = Color(0xFFC2185B), onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9E4), onPrimaryContainer = Color(0xFF3E0019),
    secondary = Color(0xFF8250C4), onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDDDFF), onSecondaryContainer = Color(0xFF2A0C52),
    tertiary = Color(0xFFB0697F),
    background = Color(0xFFFFF4F7), onBackground = Color(0xFF241A1D),
    surface = Color(0xFFFFFAFB), onSurface = Color(0xFF241A1D),
    surfaceVariant = Color(0xFFF5DDE4), onSurfaceVariant = Color(0xFF574046),
    surfaceContainer = Color(0xFFFFFFFF), surfaceContainerHigh = Color(0xFFFFF0F4),
    surfaceContainerLow = Color(0xFFFFFAFB), surfaceContainerHighest = Color(0xFFFBE6EC),
    outline = Color(0xFF9A7C84), outlineVariant = Color(0xFFE6C9D1),
)
private val WomensDark = darkColorScheme(
    primary = Color(0xFFFF8FB5), onPrimary = Color(0xFF5E0A2C),
    primaryContainer = Color(0xFF7D1E44), onPrimaryContainer = Color(0xFFFFD9E4),
    secondary = Color(0xFFCFB2FF), onSecondary = Color(0xFF3A1A6B),
    secondaryContainer = Color(0xFF52338A), onSecondaryContainer = Color(0xFFEDDDFF),
    tertiary = Color(0xFFE6B3C3),
    background = Color(0xFF1B1216), onBackground = Color(0xFFF2DEE3),
    surface = Color(0xFF22171B), onSurface = Color(0xFFF2DEE3),
    surfaceVariant = Color(0xFF3A2A30), onSurfaceVariant = Color(0xFFDCC0C8),
    surfaceContainer = Color(0xFF2A1D22), surfaceContainerHigh = Color(0xFF33242A),
    surfaceContainerLow = Color(0xFF22171B), surfaceContainerHighest = Color(0xFF3B2B31),
    outline = Color(0xFFA48A92), outlineVariant = Color(0xFF574046),
)

private fun typography(theme: AppTheme): Typography {
    val base = Typography()
    return if (theme == AppTheme.MENS) base.copy(
        titleLarge = TextStyle(fontWeight = FontWeight.Black, fontSize = 22.sp, letterSpacing = 1.5.sp),
        titleMedium = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, letterSpacing = 1.sp),
        labelSmall = base.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
    ) else base.copy(
        titleLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 23.sp),
        titleMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
        labelSmall = base.labelSmall.copy(fontWeight = FontWeight.Medium),
    )
}

private fun shapes(theme: AppTheme): Shapes = if (theme == AppTheme.MENS) Shapes(
    extraSmall = RoundedCornerShape(3.dp), small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp), large = RoundedCornerShape(8.dp), extraLarge = RoundedCornerShape(12.dp),
) else Shapes(
    extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp), large = RoundedCornerShape(24.dp), extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun WorkoutLogTheme(theme: AppTheme, darkMode: String, content: @Composable () -> Unit) {
    val dark = when (darkMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
    }
    val colors = when (theme) {
        AppTheme.MENS -> if (dark) MensDark else MensLight
        AppTheme.WOMENS -> if (dark) WomensDark else WomensLight
    }
    val style = ThemeStyle(
        theme = theme,
        chartLine = colors.primary,
        chartGoal = colors.secondary,
        chartFill = colors.primary.copy(alpha = 0.15f),
        headerTitle = { if (theme == AppTheme.MENS) it.uppercase() else it },
    )
    CompositionLocalProvider(LocalThemeStyle provides style) {
        MaterialTheme(colorScheme = colors, typography = typography(theme), shapes = shapes(theme), content = content)
    }
}
