package dev.chengeeker.lucent

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import kotlin.math.PI

fun <T> glassSpring(duration: Int = 350, bounce: Float = .15f): SpringSpec<T> =
    spring(
        dampingRatio = if (bounce >= 0) 1 - bounce else 1 / (1 + bounce),
        stiffness = (4 * PI * PI / ((duration / 1000.0) * (duration / 1000.0))).toFloat(),
    )

internal const val NavigationSurfaceAlpha = .78f
internal const val NavigationSelectionAlpha = .86f

data class LucentDestination(val label: String, val icon: ImageVector)

@Composable
fun LucentNavigation(
    items: List<LucentDestination>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    require(items.size in 2..5)
    val style = LocalLucentStyle.current
    val colors = MaterialTheme.colorScheme
    var drag by remember { mutableStateOf<Float?>(null) }
    var width by remember { mutableFloatStateOf(1f) }
    val target = drag ?: selected.toFloat()
    val fraction by
        animateFloatAsState(
            target,
            if (style.motion) glassSpring(if (drag != null) 150 else 350) else snap(),
            label = "lens position",
        )
    val pressure by
        animateFloatAsState(
            if (drag != null) 1f else 0f,
            if (style.motion) glassSpring(240) else snap(),
            label = "lens pressure",
        )
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(
                if (style.floating) CircleShape
                else androidx.compose.foundation.shape.RoundedCornerShape(0.dp)
            )
            .then(
                if (style.glass)
                    Modifier.liquidBackdrop(
                        colors.surface.copy(
                            alpha = if (style.translucent) NavigationSurfaceAlpha else .90f
                        ),
                        blur = if (style.translucent) 2.dp else 20.dp,
                    )
                else Modifier.background(colors.cardColor())
            )
            .pointerInput(items.size) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        width = size.width.toFloat()
                        drag =
                            (it.x / width * items.size - .5f).coerceIn(
                                0f,
                                items.lastIndex.toFloat(),
                            )
                    },
                    onDragCancel = { drag = null },
                    onDragEnd = {
                        drag?.let { onSelect(kotlin.math.round(it).toInt()) }
                        drag = null
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        drag =
                            ((drag ?: selected.toFloat()) + amount.x / width * items.size).coerceIn(
                                0f,
                                items.lastIndex.toFloat(),
                            )
                    },
                )
            }
    ) {
        val slot = maxWidth / items.size
        Box(
            Modifier.offset(x = slot * fraction)
                .padding(5.dp)
                .width(slot - 10.dp)
                .matchParentHeightCompat()
                .graphicsLayer {
                    scaleX = 1f - pressure * .07f
                    scaleY = 1f + pressure * .04f
                }
                .clip(CircleShape)
                .then(
                    if (style.glass)
                        Modifier.liquidBackdrop(
                            colors.primaryContainer.copy(alpha = NavigationSelectionAlpha),
                            indicator = true,
                            pressed = pressure,
                        )
                    else Modifier.background(colors.primaryContainer)
                )
        )
        Row(
            Modifier.fillMaxWidth().heightIn(min = 72.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, item ->
                Column(
                    Modifier.weight(1f)
                        .heightIn(min = 72.dp)
                        .clip(CircleShape)
                        .lucentClick(role = Role.Tab) { onSelect(index) }
                        .semantics { this.selected = selected == index },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        item.icon,
                        null,
                        tint =
                            if (selected == index) colors.onPrimaryContainer
                            else colors.onSurfaceVariant,
                    )
                    Text(
                        item.label,
                        style = MaterialTheme.typography.labelMedium,
                        color =
                            if (selected == index) colors.onPrimaryContainer
                            else colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun Modifier.matchParentHeightCompat() = height(62.dp)
