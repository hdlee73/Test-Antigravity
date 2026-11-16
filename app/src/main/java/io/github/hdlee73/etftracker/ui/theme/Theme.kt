package io.github.hdlee73.etftracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 시안 5: Editorial Private Wealth (웜 크림 · 클래식 네이비 · 샴페인 골드 · 포레스트 에메랄드)
object WealthColors {
    val CreamBackground = Color(0xFFFAF7F2)
    val PureWhiteCard = Color(0xFFFFFFFF)
    val WarmBorder = Color(0xFFE6DFD3)
    val ClassicNavy = Color(0xFF14213D)
    val SoftNavy = Color(0xFF283A5E)
    val ChampagneGold = Color(0xFFB8924A)
    val ChampagneContainer = Color(0xFFF5EFE3)
    val ForestEmerald = Color(0xFF1B6B46)
    val EmeraldContainer = Color(0xFFE6F2EC)
    val DeepCrimson = Color(0xFFA63434)
    val CrimsonContainer = Color(0xFFF9EAE9)
    val WarmSlate = Color(0xFF5C6473)
    val MutedTaupe = Color(0xFF8C857B)
}

private val LightColors = lightColorScheme(
    primary = WealthColors.ClassicNavy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7ECF4),
    onPrimaryContainer = WealthColors.ClassicNavy,
    secondary = WealthColors.ChampagneGold,
    onSecondary = Color.White,
    secondaryContainer = WealthColors.ChampagneContainer,
    onSecondaryContainer = Color(0xFF5D461B),
    tertiary = WealthColors.ForestEmerald,
    onTertiary = Color.White,
    tertiaryContainer = WealthColors.EmeraldContainer,
    onTertiaryContainer = WealthColors.ForestEmerald,
    background = WealthColors.CreamBackground,
    onBackground = WealthColors.ClassicNavy,
    surface = WealthColors.CreamBackground,
    onSurface = WealthColors.ClassicNavy,
    surfaceVariant = Color(0xFFF1ECE2),
    onSurfaceVariant = WealthColors.WarmSlate,
    surfaceContainer = WealthColors.PureWhiteCard,
    surfaceContainerHigh = Color(0xFFF4EFE6),
    outline = WealthColors.MutedTaupe,
    outlineVariant = WealthColors.WarmBorder,
    error = WealthColors.DeepCrimson,
    errorContainer = WealthColors.CrimsonContainer,
    onErrorContainer = WealthColors.DeepCrimson,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFD6B97B),
    onPrimary = Color(0xFF1A1407),
    primaryContainer = Color(0xFF243047),
    onPrimaryContainer = Color(0xFFEAF0FA),
    secondary = Color(0xFF9FB3D9),
    onSecondary = Color(0xFF0E1729),
    secondaryContainer = Color(0xFF2B261C),
    onSecondaryContainer = Color(0xFFF1DFB6),
    tertiary = Color(0xFF58B98B),
    onTertiary = Color(0xFF082618),
    tertiaryContainer = Color(0xFF153526),
    onTertiaryContainer = Color(0xFFB5E8CE),
    background = Color(0xFF0F141F),
    onBackground = Color(0xFFF4F1EA),
    surface = Color(0xFF0F141F),
    onSurface = Color(0xFFF4F1EA),
    surfaceVariant = Color(0xFF1B2230),
    onSurfaceVariant = Color(0xFFA8B0BF),
    surfaceContainer = Color(0xFF161C29),
    surfaceContainerHigh = Color(0xFF1F2738),
    outline = Color(0xFF6B7384),
    outlineVariant = Color(0xFF293245),
    error = Color(0xFFF07B7B),
    errorContainer = Color(0xFF3E1818),
    onErrorContainer = Color(0xFFFFD1D1),
)

private val EditorialTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.4).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = 19.sp,
        lineHeight = 25.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 13.5.sp,
        lineHeight = 19.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 12.sp,
        lineHeight = 17.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
    ),
)

private val EditorialShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
)

@Composable
fun EtfTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = EditorialTypography,
        shapes = EditorialShapes,
        content = content,
    )
}
