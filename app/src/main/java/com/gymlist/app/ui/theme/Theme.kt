package com.gymlist.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// Verde "gimnasio": legible sobre fondo claro y oscuro sin depender del color dinámico del sistema,
// para que la app se vea igual en cualquier móvil.
private val GreenPrimary = Color(0xFF1F6B45)
private val GreenPrimaryDark = Color(0xFF8FDCAC)
private val LimeAccent = Color(0xFF5B8C1F)
private val LimeAccentDark = Color(0xFFBEE86B)
private val AmberRest = Color(0xFF8A5100)
private val AmberRestDark = Color(0xFFFFB95C)

private val LightColors = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBCF0CE),
    onPrimaryContainer = Color(0xFF00210F),
    secondary = LimeAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDF0B8),
    onSecondaryContainer = Color(0xFF192000),
    tertiary = AmberRest,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDDB6),
    onTertiaryContainer = Color(0xFF2B1700),
    background = Color(0xFFF7FBF6),
    onBackground = Color(0xFF181D19),
    surface = Color(0xFFF7FBF6),
    onSurface = Color(0xFF181D19),
    surfaceVariant = Color(0xFFDCE5DC),
    onSurfaceVariant = Color(0xFF414942),
    outline = Color(0xFF717972),
    outlineVariant = Color(0xFFC0C9C1),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B)
)

private val DarkColors = darkColorScheme(
    primary = GreenPrimaryDark,
    onPrimary = Color(0xFF003920),
    primaryContainer = Color(0xFF005230),
    onPrimaryContainer = Color(0xFFABF8C7),
    secondary = LimeAccentDark,
    onSecondary = Color(0xFF2A3700),
    secondaryContainer = Color(0xFF3E5000),
    onSecondaryContainer = Color(0xFFDAFF85),
    tertiary = AmberRestDark,
    onTertiary = Color(0xFF462A00),
    tertiaryContainer = Color(0xFF643E00),
    onTertiaryContainer = Color(0xFFFFDDB6),
    background = Color(0xFF0F1411),
    onBackground = Color(0xFFDFE4DE),
    surface = Color(0xFF0F1411),
    onSurface = Color(0xFFDFE4DE),
    surfaceVariant = Color(0xFF3E4A42),
    onSurfaceVariant = Color(0xFFBDCBC0),
    outline = Color(0xFF87958B),
    outlineVariant = Color(0xFF3E4A42),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val GymTypography = Typography().let { base ->
    base.copy(
        // El cronómetro de descanso necesita dígitos grandes y de ancho estable.
        displayMedium = base.displayMedium.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.sp
        ),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold)
    )
}

/** Estilo monoespaciado para los números del cronómetro, para que no "bailen" al contar. */
val TimerDigitsStyle: TextStyle
    @Composable get() = MaterialTheme.typography.displayMedium.copy(
        fontFeatureSettings = "tnum"
    )

@Composable
fun GymListTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = GymTypography,
        content = content
    )
}
