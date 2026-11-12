package dev.chengeeker.lucent

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*

object LucentTokens {
    val pageInset = 16.dp
    val cardRadius = 24.dp
    val controlTarget = 48.dp
    val galleryCircle = 44.dp
    val thumbnailStrip = 64.dp
    val headerHeight = 64.dp
    const val fontMin = .85f
    const val fontMax = 1.5f
    const val fontSteps = 12
    const val cardContrast = 1.12f
    const val cardOverlay = .10f
    const val enterMs = 350
    const val exitMs = 240
    const val pressMs = 150
}

@Immutable
data class LucentStyle(
    val mode: String = "system",
    val dynamic: Boolean = true,
    val seed: Int = 0,
    val black: Boolean = false,
    val font: Float = 1f,
    val weight: Int = 0,
    val glass: Boolean = true,
    val translucent: Boolean = true,
    val motion: Boolean = true,
    val haptics: Boolean = true,
    val customWeight: Boolean = false,
    val floating: Boolean = true,
)

val LocalLucentStyle = staticCompositionLocalOf { LucentStyle() }

fun contrast(a: Color, b: Color): Float =
    (maxOf(a.luminance(), b.luminance()) + .05f) / (minOf(a.luminance(), b.luminance()) + .05f)

fun ColorScheme.cardColor(page: Color = background): Color =
    if (contrast(surfaceContainerHigh, page) >= LucentTokens.cardContrast) surfaceContainerHigh
    else onSurface.copy(alpha = .10f).compositeOver(page)

fun normalizeFont(value: Float) =
    if (value.isFinite()) (kotlin.math.round(value.coerceIn(.85f, 1.5f) * 20) / 20) else 1f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LucentTheme(style: LucentStyle = LucentStyle(), content: @Composable () -> Unit) {
    val dark = style.mode == "dark" || (style.mode == "system" && isSystemInDarkTheme())
    val context = LocalContext.current
    val scheme =
        if (style.dynamic && Build.VERSION.SDK_INT >= 31) {
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else
            tonalScheme(appearanceSeeds[style.seed.coerceIn(appearanceSeeds.indices)].toInt(), dark)
    val colors =
        if (dark && style.black) scheme.copy(background = Color.Black, surface = Color.Black)
        else scheme
    val base = Typography()
    fun androidx.compose.ui.text.TextStyle.weighted() =
        copy(
            fontWeight =
                FontWeight(
                    ((fontWeight?.weight ?: 400) +
                            (if (style.customWeight) style.weight * 100 else 0))
                        .coerceIn(100, 900)
                )
        )
    val type =
        base.copy(
            displayLarge = base.displayLarge.weighted(),
            headlineLarge = base.headlineLarge.weighted(),
            headlineMedium = base.headlineMedium.weighted(),
            headlineSmall = base.headlineSmall.weighted(),
            titleLarge = base.titleLarge.weighted(),
            titleMedium = base.titleMedium.weighted(),
            titleSmall = base.titleSmall.weighted(),
            bodyLarge = base.bodyLarge.weighted(),
            bodyMedium = base.bodyMedium.weighted(),
            bodySmall = base.bodySmall.weighted(),
            labelLarge = base.labelLarge.weighted(),
            labelMedium = base.labelMedium.weighted(),
            labelSmall = base.labelSmall.weighted(),
        )
    val config = androidx.compose.ui.platform.LocalConfiguration.current
    val fontContext =
        remember(context, config, style.customWeight) {
            if (style.customWeight && Build.VERSION.SDK_INT >= 31)
                context.createConfigurationContext(
                    android.content.res.Configuration(config).apply { fontWeightAdjustment = 0 }
                )
            else context
        }
    val resolver =
        remember(fontContext) {
            androidx.compose.ui.text.font.createFontFamilyResolver(fontContext)
        }
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalLucentStyle provides style,
        LocalRippleConfiguration provides null,
        androidx.compose.ui.platform.LocalFontFamilyResolver provides resolver,
        LocalDensity provides
            Density(density.density, density.fontScale * normalizeFont(style.font)),
    ) {
        MaterialTheme(colorScheme = colors, typography = type) {
            CompositionLocalProvider(LocalContentColor provides colors.onSurface, content = content)
        }
    }
}

fun tonalScheme(seed: Int, dark: Boolean): ColorScheme {
    val hue = com.google.android.material.color.utilities.Hct.fromInt(seed).hue
    fun palette(h: Double, c: Double) =
        com.google.android.material.color.utilities.TonalPalette.fromHueAndChroma(h, c)
    val p = palette(hue, 36.0)
    val s = palette(hue, 16.0)
    val t = palette((hue + 60) % 360, 24.0)
    val n = palette(hue, 6.0)
    val v = palette(hue, 8.0)
    fun com.google.android.material.color.utilities.TonalPalette.c(light: Int, night: Int) =
        Color(tone(if (dark) night else light))
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = p.c(40, 80),
        onPrimary = p.c(100, 20),
        primaryContainer = p.c(90, 30),
        onPrimaryContainer = p.c(10, 90),
        secondary = s.c(40, 80),
        onSecondary = s.c(100, 20),
        secondaryContainer = s.c(90, 30),
        onSecondaryContainer = s.c(10, 90),
        tertiary = t.c(40, 80),
        onTertiary = t.c(100, 20),
        tertiaryContainer = t.c(90, 30),
        onTertiaryContainer = t.c(10, 90),
        background = n.c(98, 6),
        onBackground = n.c(10, 90),
        surface = n.c(98, 6),
        onSurface = n.c(10, 90),
        surfaceVariant = v.c(90, 30),
        onSurfaceVariant = v.c(30, 80),
        outline = v.c(50, 60),
        outlineVariant = v.c(80, 30),
        surfaceContainerLowest = n.c(100, 4),
        surfaceContainerLow = n.c(96, 10),
        surfaceContainer = n.c(94, 12),
        surfaceContainerHigh = n.c(92, 17),
        surfaceContainerHighest = n.c(90, 22),
        inverseSurface = n.c(20, 90),
        inverseOnSurface = n.c(95, 20),
        inversePrimary = p.c(80, 40),
        surfaceTint = p.c(40, 80),
    )
}
