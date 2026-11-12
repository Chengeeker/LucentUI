package dev.chengeeker.lucent

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chengeeker.lucent.review.liquidBackdrop as reviewLiquidBackdrop

// One renderer: keep the public entry point wired to the Review implementation.
@Composable
fun Modifier.liquidBackdrop(tint: Color, indicator: Boolean = false, pressed: Float = 0f,
    blur: Dp = if (indicator) 0.dp else 2.dp, opacity: Float = 1f,
    source: LucentBackdrop? = LocalBackdrop.current, button: Boolean = false, plainButton: Boolean = false): Modifier =
    reviewLiquidBackdrop(tint, indicator, pressed, blur, opacity, source, button, plainButton)
