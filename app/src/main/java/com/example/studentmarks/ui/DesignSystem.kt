package com.example.studentmarks.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val StudentMarksLightColors = lightColorScheme(
    primary = Color(0xFF176B5B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD2EEE5),
    onPrimaryContainer = Color(0xFF0B332B),
    secondary = Color(0xFF52665E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE8E1),
    onSecondaryContainer = Color(0xFF17251F),
    tertiary = Color(0xFF9A5B16),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDDB8),
    onTertiaryContainer = Color(0xFF321B00),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    background = Color(0xFFF5F7F4),
    onBackground = Color(0xFF18211D),
    surface = Color(0xFFF5F7F4),
    onSurface = Color(0xFF18211D),
    surfaceVariant = Color(0xFFE8EEEA),
    onSurfaceVariant = Color(0xFF4B5A53),
    outline = Color(0xFF78877F),
    outlineVariant = Color(0xFFD0D9D3),
)

private val StudentMarksDarkColors = darkColorScheme(
    primary = Color(0xFF8FD6C4),
    onPrimary = Color(0xFF00382D),
    primaryContainer = Color(0xFF165143),
    onPrimaryContainer = Color(0xFFB0F2E0),
    secondary = Color(0xFFB6CCC1),
    onSecondary = Color(0xFF22352D),
    secondaryContainer = Color(0xFF384B42),
    onSecondaryContainer = Color(0xFFD2E8DC),
    tertiary = Color(0xFFFFB96D),
    onTertiary = Color(0xFF512F00),
    tertiaryContainer = Color(0xFF714500),
    onTertiaryContainer = Color(0xFFFFDDB8),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    background = Color(0xFF111916),
    onBackground = Color(0xFFE0E8E2),
    surface = Color(0xFF111916),
    onSurface = Color(0xFFE0E8E2),
    surfaceVariant = Color(0xFF293630),
    onSurfaceVariant = Color(0xFFC0CCC4),
    outline = Color(0xFF89968D),
    outlineVariant = Color(0xFF3F4A43),
)

private val StudentMarksTypography = Typography().copy(
    displaySmall = TextStyle(fontSize = 36.sp, lineHeight = 42.sp, fontWeight = FontWeight.SemiBold),
    headlineLarge = TextStyle(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold),
    headlineMedium = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
    headlineSmall = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
)

object AppColors {
    val success = Color(0xFF24734E)
    val warning = Color(0xFF9A5B16)
    val divider = Color(0xFFD0D9D3)
}

object AppSpacing {
    val small = 8.dp
    val medium = 16.dp
    val large = 24.dp
    val screen = 20.dp
    val card = 16.dp
    val section = 24.dp
}

@Composable
fun StudentMarksTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) StudentMarksDarkColors else StudentMarksLightColors,
        typography = StudentMarksTypography,
        content = content,
    )
}

@Composable
fun BackIconButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
    }
}
