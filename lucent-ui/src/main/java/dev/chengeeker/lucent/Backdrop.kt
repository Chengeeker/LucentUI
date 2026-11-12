package dev.chengeeker.lucent

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.*

class LucentBackdrop(val layer: GraphicsLayer) {
    var origin = Offset.Zero
}

val LocalBackdrop = staticCompositionLocalOf<LucentBackdrop?> { null }

@Composable
fun BackdropHost(
    modifier: Modifier = Modifier,
    overlay: @Composable BoxScope.() -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val layer = rememberGraphicsLayer()
    val backdrop = remember(layer) { LucentBackdrop(layer) }
    CompositionLocalProvider(LocalBackdrop provides backdrop) {
        Box(modifier.fillMaxSize()) {
            // Background, content, comments and replies must all belong to this same opaque
            // capture.
            Box(
                Modifier.fillMaxSize()
                    .onGloballyPositioned { backdrop.origin = it.positionInRoot() }
                    .drawWithContent {
                        layer.record { this@drawWithContent.drawContent() }
                        drawLayer(layer)
                    }
                    .background(MaterialTheme.colorScheme.background),
                content = content,
            )
            overlay()
        }
    }
}

@Composable
fun Modifier.frostedBackdrop(
    color: Color,
    panel: Boolean = false,
    blur: Dp = 20.dp,
    panelAlpha: Float = .82f,
    showBorder: Boolean = true,
): Modifier {
    val source = LocalBackdrop.current
    val sample = rememberGraphicsLayer()
    var position by remember { mutableStateOf(Offset.Zero) }
    val outline = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .24f)
    val enabled = LocalLucentStyle.current.glass
    return onGloballyPositioned { position = it.positionInRoot() }
        .clipToBounds()
        .drawWithContent {
            if (Build.VERSION.SDK_INT >= 31 && enabled && source != null) {
                sample.renderEffect =
                    BlurEffect(
                        blur.toPx().coerceAtLeast(.1f),
                        blur.toPx().coerceAtLeast(.1f),
                        TileMode.Clamp,
                    )
                sample.record {
                    drawRect(color.copy(alpha = 1f))
                    translate(source.origin.x - position.x, source.origin.y - position.y) {
                        drawLayer(source.layer)
                    }
                }
                drawLayer(sample)
                drawRect(color.copy(alpha = if (panel) panelAlpha else .45f))
                if (!panel)
                    drawRect(
                        Brush.verticalGradient(
                            listOf(color.copy(alpha = .95f), color.copy(alpha = 0f))
                        )
                    )
            } else {
                // shortcut: API 24–30 uses an opaque surface; add a profiled GLES backend before
                // promising legacy optical blur.
                drawRect(color.copy(alpha = 1f))
            }
            if (showBorder)
                drawLine(
                    outline,
                    Offset(0f, size.height - .5.dp.toPx()),
                    Offset(size.width, size.height - .5.dp.toPx()),
                    .5.dp.toPx(),
                )
            drawContent()
        }
}
