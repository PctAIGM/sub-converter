package com.subconverter.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

private val iOS_Blue = Color(0xFF0A84FF)
private val iOS_Blue_Dark = Color(0xFF64D2FF)
private val iOS_Green = Color(0xFF30D158)
private val iOS_Orange = Color(0xFFFF9F0A)
private val iOS_Red = Color(0xFFFF453A)
private val iOS_Purple = Color(0xFFBF5AF2)
private val iOS_Teal = Color(0xFF64D2FF)

private val iOS_LightBG = Color(0xFFF2F2F7)
private val iOS_LightCard = Color(0xFFFFFFFF)
private val iOS_LightGroupedBG = Color(0xFFFFFFFF)
private val iOS_LightSeparator = Color(0xFFC6C6C8)
private val iOS_LightSecondaryBG = Color(0xFFFFFFFF)

private val iOS_DarkBG = Color(0xFF000000)
private val iOS_DarkCard = Color(0xFF1C1C1E)
private val iOS_DarkElevated = Color(0xFF2C2C2E)
private val iOS_DarkGroupedBG = Color(0xFF1C1C1E)
private val iOS_DarkSeparator = Color(0xFF38383A)
private val iOS_DarkSecondaryBG = Color(0xFF2C2C2E)

private val LightScheme = lightColorScheme(
    primary = iOS_Blue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0EDFF),
    onPrimaryContainer = Color(0xFF003E80),
    secondary = iOS_Green,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCFCE7),
    onSecondaryContainer = Color(0xFF0A5C1E),
    tertiary = iOS_Orange,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE8C2),
    onTertiaryContainer = Color(0xFF5F3A00),
    background = iOS_LightBG,
    onBackground = Color(0xFF1C1C1E),
    surface = iOS_LightCard,
    onSurface = Color(0xFF1C1C1E),
    surfaceVariant = iOS_LightSecondaryBG,
    onSurfaceVariant = Color(0xFF8E8E93),
    surfaceContainer = Color(0xFFE5E5EA),
    surfaceContainerHigh = Color(0xFFEFEFF4),
    surfaceContainerLow = iOS_LightBG,
    surfaceContainerLowest = Color.White,
    outline = iOS_LightSeparator,
    outlineVariant = Color(0xFFE5E5EA),
    error = iOS_Red,
    onError = Color.White,
    errorContainer = Color(0xFFFFECEA),
    onErrorContainer = Color(0xFF68001D),
    inverseSurface = Color(0xFF2C2C2E),
    inverseOnSurface = Color(0xFFF2F2F7),
    inversePrimary = iOS_Blue_Dark,
)

private val DarkScheme = darkColorScheme(
    primary = iOS_Blue_Dark,
    onPrimary = Color(0xFF003E80),
    primaryContainer = Color(0xFF004E9E),
    onPrimaryContainer = Color(0xFFD6E9FF),
    secondary = iOS_Green,
    onSecondary = Color(0xFF00390D),
    secondaryContainer = Color(0xFF005317),
    onSecondaryContainer = Color(0xFFB8F1C5),
    tertiary = iOS_Orange,
    onTertiary = Color(0xFF4A2800),
    tertiaryContainer = Color(0xFF6D3C00),
    onTertiaryContainer = Color(0xFFFFDFA6),
    background = iOS_DarkBG,
    onBackground = Color(0xFFE5E5EA),
    surface = iOS_DarkCard,
    onSurface = Color(0xFFE5E5EA),
    surfaceVariant = iOS_DarkSecondaryBG,
    onSurfaceVariant = Color(0xFF8E8E93),
    surfaceContainer = iOS_DarkSeparator,
    surfaceContainerHigh = iOS_DarkElevated,
    surfaceContainerLow = iOS_DarkBG,
    surfaceContainerLowest = Color(0xFF141414),
    outline = iOS_DarkSeparator,
    outlineVariant = Color(0xFF48484A),
    error = Color(0xFFFF6961),
    onError = Color(0xFF68001D),
    errorContainer = Color(0xFF92000A),
    onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = Color(0xFFE5E5EA),
    inverseOnSurface = Color(0xFF1C1C1E),
    inversePrimary = iOS_Blue,
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val AppTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.sp,
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp,
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.sp,
    ),
)

@Composable
fun SubConverterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkScheme else LightScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        @Suppress("DEPRECATION")
        window.statusBarColor = Color.Transparent.toArgb()
        @Suppress("DEPRECATION")
        window.navigationBarColor = Color.Transparent.toArgb()
        val insetsController = WindowCompat.getInsetsController(window, view)
        insetsController.isAppearanceLightStatusBars = !darkTheme
        insetsController.isAppearanceLightNavigationBars = !darkTheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = AppShapes,
        typography = AppTypography,
        content = content,
    )
}
