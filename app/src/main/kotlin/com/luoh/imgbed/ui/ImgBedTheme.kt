package com.luoh.imgbed.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightPalette = lightColorScheme(
    primary = Color(0xFF426654), onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E8D7), onPrimaryContainer = Color(0xFF233F30),
    secondary = Color(0xFF55665B), onSecondary = Color.White,
    secondaryContainer = Color(0xFFDFE8DE), onSecondaryContainer = Color(0xFF33483A),
    tertiary = Color(0xFF786248), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF2E3CC), onTertiaryContainer = Color(0xFF55432D),
    background = Color(0xFFF5F7F2), onBackground = Color(0xFF202722),
    surface = Color(0xFFF5F7F2), onSurface = Color(0xFF202722),
    surfaceVariant = Color(0xFFE3E9E1), onSurfaceVariant = Color(0xFF657168),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF0F3ED),
    surfaceContainer = Color(0xFFEAEFE7), surfaceContainerHigh = Color(0xFFE4EAE0),
    surfaceContainerHighest = Color(0xFFDDE5D9),
    outline = Color(0xFF778479), outlineVariant = Color(0xFFCDD7CB), surfaceTint = Color(0xFF426654),
)

private val DarkPalette = darkColorScheme(
    primary = Color(0xFFA9D1B6), onPrimary = Color(0xFF173827),
    primaryContainer = Color(0xFF2E503D), onPrimaryContainer = Color(0xFFCDEAD7),
    secondary = Color(0xFFBBCDBD), onSecondary = Color(0xFF27382D),
    secondaryContainer = Color(0xFF364B3C), onSecondaryContainer = Color(0xFFDCE7DB),
    tertiary = Color(0xFFE1C39A), onTertiary = Color(0xFF44341E),
    tertiaryContainer = Color(0xFF53432F), onTertiaryContainer = Color(0xFFF4E1C8),
    background = Color(0xFF111612), onBackground = Color(0xFFE1E8DE),
    surface = Color(0xFF111612), onSurface = Color(0xFFE1E8DE),
    surfaceVariant = Color(0xFF343E35), onSurfaceVariant = Color(0xFFABB8AC),
    surfaceContainerLowest = Color(0xFF0C110D), surfaceContainerLow = Color(0xFF181F19),
    surfaceContainer = Color(0xFF1D251E), surfaceContainerHigh = Color(0xFF273028),
    surfaceContainerHighest = Color(0xFF333D34),
    outline = Color(0xFF849186), outlineVariant = Color(0xFF3C483E), surfaceTint = Color(0xFFA9D1B6),
)

@Composable
internal fun ImgBedTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkPalette else LightPalette,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(5.dp), small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(24.dp), extraLarge = RoundedCornerShape(32.dp),
        ),
        typography = Typography(
            headlineLarge = TextStyle(fontSize = 34.sp, lineHeight = 43.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.8).sp),
            headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold),
            headlineSmall = TextStyle(fontSize = 23.sp, lineHeight = 31.sp, fontWeight = FontWeight.SemiBold),
            titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium),
            titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 23.sp, fontWeight = FontWeight.Medium),
            bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
            bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 20.sp),
            bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
            labelLarge = TextStyle(fontSize = 13.sp, lineHeight = 19.sp, fontWeight = FontWeight.Medium),
            labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
        ),
        content = content,
    )
}
