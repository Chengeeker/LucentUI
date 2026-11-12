// Adapted from Review, Copyright 2026 Chengeeker, MIT.
package dev.chengeeker.lucent.review
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
@Composable
fun ReviewTheme(dark: Boolean, dynamic: Boolean = false, scale: Float = 1f, palette: Int = 0, pureBlack: Boolean = false, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val resources = androidx.compose.ui.platform.LocalResources.current
    val scheme = if (dynamic && Build.VERSION.SDK_INT >= 31) {
        remember(context, dark, resources, androidx.compose.ui.platform.LocalConfiguration.current) {
            val platform = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            val surface = resources.getColor(if (dark) android.R.color.system_neutral1_900 else android.R.color.system_neutral1_10, context.theme)
            val seed = resources.getColor(if (dark) android.R.color.system_accent1_200 else android.R.color.system_accent1_600, context.theme)
            reviewDynamicSurfaces(platform, dark, seed, surface)
        }
    } else ReviewPalettes[palette.coerceIn(0, 7)].let { if (dark) it.second else it.first }
    val effective = reviewColors(scheme, dark, pureBlack)
    fun TextStyle.relativeHeight(ratio: Float) = copy(lineHeight = ratio.em, lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None))
    val baseText = Typography().let { text -> text.copy(
        displayLarge = text.displayLarge.relativeHeight(1.12f), displayMedium = text.displayMedium.relativeHeight(1.16f), displaySmall = text.displaySmall.relativeHeight(1.22f),
        headlineLarge = text.headlineLarge.relativeHeight(1.25f), headlineMedium = text.headlineMedium.relativeHeight(1.29f), headlineSmall = text.headlineSmall.relativeHeight(1.33f),
        titleLarge = text.titleLarge.relativeHeight(1.27f), titleMedium = text.titleMedium.relativeHeight(1.5f), titleSmall = text.titleSmall.relativeHeight(1.43f),
        bodyLarge = text.bodyLarge.relativeHeight(1.5f), bodyMedium = text.bodyMedium.relativeHeight(1.43f), bodySmall = text.bodySmall.relativeHeight(1.33f),
        labelLarge = text.labelLarge.relativeHeight(1.43f), labelMedium = text.labelMedium.relativeHeight(1.33f), labelSmall = text.labelSmall.relativeHeight(1.45f)) }
    val text = baseText.copy(
        bodyLarge = baseText.bodyLarge.copy(letterSpacing = 0.sp),
        bodyMedium = baseText.bodyMedium.copy(letterSpacing = 0.sp),
        bodySmall = baseText.bodySmall.copy(letterSpacing = 0.sp, color = effective.onSurfaceVariant),
        titleLarge = baseText.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.sp),
        titleMedium = baseText.titleMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
        titleSmall = baseText.titleSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp, color = effective.onSurfaceVariant),
        labelLarge = baseText.labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
        labelMedium = baseText.labelMedium.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.sp),
    ).adjustWeight(LocalWeightAdjustment.current)
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, density.fontScale * scale), LocalContentColor provides effective.onSurface) {
        MaterialTheme(colorScheme = effective, typography = text) { ProvideTextStyle(text.bodyMedium, content) }
    }
}

// shortcut: palette helpers are restricted Material internals, revalidate when upgrading Material.
@android.annotation.SuppressLint("RestrictedApi")
internal fun reviewDynamicSurfaces(scheme: ColorScheme, dark: Boolean, seed: Int, surface: Int): ColorScheme {
    val colors = com.google.android.material.color.utilities.MaterialDynamicColors()
    val palette = com.google.android.material.color.utilities.SchemeTonalSpot(com.google.android.material.color.utilities.Hct.fromInt(seed), dark, 0.0)
    return scheme.copy(surface = Color(surface),
        surfaceContainerLowest = Color(colors.surfaceContainerLowest().getArgb(palette)),
        surfaceContainerLow = Color(colors.surfaceContainerLow().getArgb(palette)),
        surfaceContainer = Color(colors.surfaceContainer().getArgb(palette)),
        surfaceContainerHigh = Color(colors.surfaceContainerHigh().getArgb(palette)),
        surfaceContainerHighest = Color(colors.surfaceContainerHighest().getArgb(palette)))
}

fun reviewColors(scheme: ColorScheme, dark: Boolean, pureBlack: Boolean): ColorScheme = when {
    !dark -> scheme.copy(background = scheme.surface)
    pureBlack -> scheme.copy(background = Color.Black, surface = Color.Black,
        surfaceContainerLowest = Color(0xFF121212), surfaceContainer = Color(0xFF121212),
        surfaceContainerHigh = Color(0xFF181818), surfaceContainerHighest = Color(0xFF222222))
    else -> scheme.copy(background = Color(0xFF111215), surfaceContainerLowest = Color(0xFF1B1C20))
}

@Composable
fun ReviewDivider(modifier: Modifier = Modifier, thickness: androidx.compose.ui.unit.Dp = .5.dp, color: Color = Color.Unspecified) {
    val colors = MaterialTheme.colorScheme
    val line = if (color != Color.Unspecified) color else if (colors.background == Color.Black) Color.White.copy(alpha = .1f)
        else colors.outlineVariant.copy(alpha = if (colors.background.luminance() < .5f) .2f else .3f)
    Box(modifier.fillMaxWidth().height(maxOf(1.dp, thickness)), contentAlignment = Alignment.Center) {
        HorizontalDivider(thickness = thickness, color = line)
    }
}

@Composable
fun ReviewListItem(headlineContent: @Composable () -> Unit, modifier: Modifier = Modifier,
    supportingContent: (@Composable () -> Unit)? = null, leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    colors: ListItemColors = ListItemDefaults.colors(containerColor = Color.Transparent)) {
    ListItem(modifier = modifier.padding(vertical = 3.dp), colors = colors,
        headlineContent = { ProvideTextStyle(MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 1.35.em, fontWeight = reviewWeight(FontWeight.SemiBold)), headlineContent) },
        supportingContent = supportingContent?.let { content -> { ProvideTextStyle(MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp, lineHeight = 1.4.em, fontWeight = reviewWeight(FontWeight.Normal)), content) } },
        leadingContent = leadingContent, trailingContent = trailingContent)
}

