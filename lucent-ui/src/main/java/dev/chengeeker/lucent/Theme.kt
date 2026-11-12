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
    val headerHeight = 56.dp
    const val fontMin = .85f
    const val fontMax = 1.5f
    const val fontSteps = 12
    const val cardContrast = 1.12f
    const val cardOverlay = .10f
    const val enterMs = 320
    const val exitMs = 220
    const val pressMs = 150
}

@Immutable
data class LucentStyle(
    val mode: String = "system",
    val dynamic: Boolean = false,
    val seed: Int = 0,
    val black: Boolean = false,
    val font: Float = 1f,
    val weight: Int = 0,
    val glass: Boolean = false,
    val translucent: Boolean = false,
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
 val dark=style.mode=="dark" || (style.mode=="system" && isSystemInDarkTheme())
 val haptics=remember(style.haptics) { { style.haptics } }
 CompositionLocalProvider(LocalLucentStyle provides style, LocalRippleConfiguration provides null,
  dev.chengeeker.lucent.review.LocalReviewHapticsEnabled provides haptics,
  androidx.compose.ui.platform.LocalHapticFeedback provides dev.chengeeker.lucent.review.SilentComposeHaptics) {
  dev.chengeeker.lucent.review.ReviewFontWeight(style.customWeight, style.weight*100) {
   dev.chengeeker.lucent.review.ReviewTheme(dark,style.dynamic,normalizeFont(style.font),style.seed,style.black,content)
  }
 }
}
