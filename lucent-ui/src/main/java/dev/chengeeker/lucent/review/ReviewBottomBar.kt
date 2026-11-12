// Adapted from Review, Copyright 2026 Chengeeker, MIT.
package dev.chengeeker.lucent.review
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
@Composable
fun ReviewBottomBar(selected: Int, floating: Boolean, glass: Boolean, translucent: Boolean, names: List<String>, inactive: List<ImageVector>, active: List<ImageVector>, select: (Int) -> Unit) {
    val icons = inactive.mapIndexed { i, icon -> if (selected == i) active[i] else icon }
    if (!floating) {
        NavigationBar(Modifier.height(68.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()), windowInsets = WindowInsets.navigationBars) { names.forEachIndexed { i, name -> NavigationBarItem(selected == i, hapticClick { select(i) }, icon = { Icon(icons[i], null) }, label = { Text(name) }) } }
    } else BoxWithConstraints(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = if (WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() > 0.dp) 6.dp else 14.dp), contentAlignment = Alignment.Center) {
        val width = minOf(440.dp, maxWidth - 40.dp) * (2f / 3f)
        if (glass) {
            GlassNavigation(selected, translucent, width, inactive, active, names, select)
            return@BoxWithConstraints
        }
        Surface(Modifier.width(width).height(56.dp).dropShadow(RoundedCornerShape(32.dp), Shadow(radius = 16.dp, color = Color.Black.copy(alpha = if (MaterialTheme.colorScheme.background.luminance() < .5f) .35f else .12f), offset = androidx.compose.ui.unit.DpOffset(0.dp, 4.dp))), color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = RoundedCornerShape(32.dp)) {
            Box(Modifier.padding(4.dp)) {
                val cell = (width - 8.dp) / 3
                val x by animateDpAsState(cell * selected, tween(240, easing = CubicBezierEasing(.215f, .61f, .355f, 1f)), label = "navigation")
                Box(Modifier.offset(x = x).width(cell).fillMaxHeight().clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.secondaryContainer))
                Row(Modifier.fillMaxSize()) { names.forEachIndexed { i, name ->
                    val tint by animateColorAsState(if (selected == i) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, tween(150), label = "navigation-label")
                    val weight by animateIntAsState(if (selected == i) 600 else 400, tween(150), label = "navigation-weight")
                    Column(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(28.dp)).reviewClickable { select(i) }, Arrangement.Center, Alignment.CenterHorizontally) {
                        AnimatedContent(icons[i], transitionSpec = { fadeIn(tween(150)).togetherWith(fadeOut(tween(150))) }, label = "navigation-icon") { icon -> Icon(icon, null, Modifier.size(22.dp), tint = tint) }
                        Spacer(Modifier.height(2.dp))
                        Text(name, fontSize = 11.5.sp, color = tint, fontWeight = reviewWeight(FontWeight(weight)))
                    }
                } }
            }
        }
    }
}

