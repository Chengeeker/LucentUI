// Adapted from Review, Copyright 2026 Chengeeker, MIT.
package dev.chengeeker.lucent.review

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.Role
import dev.chengeeker.lucent.review.liquidBackdrop
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import kotlin.math.abs

// Same 48dp button resistance and anchored stretch as LiquidStretch (MIT, bundled library).
fun glassButtonStretch(drag: Offset): FloatArray {
    val offset = drag / (1f + drag.getDistance() * .01f) * .8f
    val x = abs(offset.x) / 48f; val y = abs(offset.y) / 48f
    fun pivot(value: Float): Float {
        val t = (abs(value) / 20f).coerceIn(0f, 1f)
        return .5f - kotlin.math.sign(value) * t * t * (3f - 2f * t) * .5f
    }
    return floatArrayOf((1f + (x * .8f).coerceAtMost(.3f)) * (1f - (y * .3f).coerceAtMost(.15f)),
        (1f + (y * .8f).coerceAtMost(.3f)) * (1f - (x * .3f).coerceAtMost(.15f)),
        pivot(offset.x), pivot(offset.y), offset.x * .15f, offset.y * .15f)
}

@Composable
fun GalleryGlassButton(icon: ImageVector, label: String, enabled: Boolean = true, busy: Boolean = false, click: () -> Unit) {
    val liquid = dev.chengeeker.lucent.LocalLucentStyle.current.glass
    if (!liquid) {
        PlainGalleryControl(label, enabled, hapticClick(click)) {
            if (busy) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
            else Icon(icon, null, Modifier.size(22.dp), tint = Color.White.copy(alpha = if (enabled) 1f else .38f))
        }
        return
    }
    val density = LocalDensity.current.density
    var pressed by remember { mutableStateOf(false) }
    var drag by remember { mutableStateOf(Offset.Zero) }
    val invoke by rememberUpdatedState(hapticClick(click))
    val growth by animateFloatAsState(if (pressed && liquid) 1f else 0f, spring(dampingRatio = .65f, stiffness = 450f), label = "gallery-glass-press")
    val x by animateFloatAsState(drag.x, spring(dampingRatio = .8f, stiffness = 550f), label = "gallery-glass-x")
    val y by animateFloatAsState(drag.y, spring(dampingRatio = .8f, stiffness = 550f), label = "gallery-glass-y")
    Box(Modifier.size(48.dp).semantics {
        contentDescription = label; role = Role.Button
        if (!enabled) disabled()
        onClick { if (enabled) invoke(); enabled }
    }.onKeyEvent { event ->
        if (enabled && event.key in listOf(Key.Enter, Key.Spacebar, Key.DirectionCenter)) {
            if (event.type == KeyEventType.KeyUp) invoke()
            true
        } else false
    }.focusable(enabled).pointerInput(enabled, liquid, density) {
        if (!enabled) return@pointerInput
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            down.consume(); pressed = true
            var moved = false; var cancelled = false
            try {
                while (true) {
                    val event = awaitPointerEvent()
                    val pointer = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (event.changes.any { it.id != down.id && it.pressed }) cancelled = true
                    val delta = pointer.position - down.position
                    moved = moved || delta.getDistance() > viewConfiguration.touchSlop
                    if (liquid) drag = delta / density
                    pointer.consume()
                    if (!pointer.pressed) {
                        if (!cancelled && !moved && (!liquid || pointer.uptimeMillis - down.uptimeMillis < viewConfiguration.longPressTimeoutMillis)) {
                            invoke()
                        }
                        break
                    }
                }
            } finally { pressed = false; drag = Offset.Zero }
        }
    }, contentAlignment = Alignment.Center) {
        Box(Modifier.size(48.dp).graphicsLayer {
            val stretch = glassButtonStretch(Offset(x, y))
            val scale = 1f + 17f / 48f * growth
            scaleX = scale * stretch[0]; scaleY = scale * stretch[1]
            transformOrigin = TransformOrigin(stretch[2], stretch[3])
            translationX = stretch[4] * density; translationY = stretch[5] * density
        }.liquidBackdrop(Color(0x66000000), pressed = growth, button = true), contentAlignment = Alignment.Center) {
            if (busy) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
            else Icon(icon, null, Modifier.size(22.dp), tint = Color.White)
        }
    }
}


/** Keep the original-file action visually consistent with the gallery's two top controls. */
internal fun galleryOriginalPillBottomMargin(hasStrip: Boolean, stripBottom: androidx.compose.ui.unit.Dp) =
    if (hasStrip) stripBottom + 64.dp + 8.dp else 24.dp

@Composable
internal fun GalleryOriginalPill(state: GalleryHiResState?, hdr: Boolean) {
    val liquid = dev.chengeeker.lucent.LocalLucentStyle.current.glass
    val phase = state?.phase ?: GalleryHiResPhase.IDLE
    val enabled = state != null && (phase == GalleryHiResPhase.IDLE || phase == GalleryHiResPhase.FAILED)
    val caption = when (phase) {
        GalleryHiResPhase.IDLE -> "加载原图"
        GalleryHiResPhase.LOADING -> {
            val total = state?.total
            val loaded = state?.downloaded ?: 0L
            if (total != null && total > 0L)
                "原图 " + (loaded * 100 / total).coerceIn(0, 100) + "%"
            else "正在加载原图…"
        }
        GalleryHiResPhase.READY -> when {
            hdr -> "HDR 原图已缓存"
            state?.tileIssue != null -> "原图已缓存·不支持区域解码"
            else -> "原图已缓存"
        }
        GalleryHiResPhase.FAILED -> "重试原图"
    }
    val action = hapticClick {
        if (phase == GalleryHiResPhase.FAILED) state?.retry()
        else if (phase == GalleryHiResPhase.IDLE) state?.request()
    }
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        if (pressed && liquid) 1f else 0f,
        spring(dampingRatio = .65f, stiffness = 450f),
        label = "gallery-original-glass-press",
    )
    Box(
        Modifier.widthIn(min = 96.dp, max = 258.dp).height(48.dp)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = action,
            )
            .semantics { contentDescription = caption },
        contentAlignment = Alignment.Center,
    ) {
        val pill = CircleShape
        Row(
            Modifier.height(36.dp)
                .then(
                    if (liquid) Modifier.liquidBackdrop(Color(0x66000000), pressed = press, button = true)
                    else Modifier.clip(pill)
                        .background(if (pressed && enabled) Color.Black.copy(alpha = .28f) else Color.Black.copy(alpha = .40f), pill)
                        .border(.5.dp, Color.White.copy(alpha = .24f), pill)
                )
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (phase == GalleryHiResPhase.LOADING) {
                CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 1.5.dp, color = Color.White)
                Spacer(Modifier.width(7.dp))
            }
            Text(
                caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.White.copy(alpha = if (enabled) 1f else .85f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
