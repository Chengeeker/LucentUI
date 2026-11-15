// Adapted from Review, Copyright 2026 Chengeeker, MIT.
package dev.chengeeker.lucent.review

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import dev.chengeeker.lucent.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp

@Composable
internal fun PlainGalleryControl(label: String?, enabled: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(Modifier.size(48.dp).clip(CircleShape)
        .clickable(interactionSource = null, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
        .semantics { if (label != null) contentDescription = label }, contentAlignment = Alignment.Center) {
        FlutterGallerySurface()
        content()
    }
}

@Composable
internal fun FlutterGallerySurface() {
    // shortcut: draw the Flutter tint and rim without extra image capture; compare device optics before changing the glass backend.
    val tint = remember { Brush.linearGradient(listOf(Color(0xffeeeeee).copy(alpha = .30f),
        Color(0xff9e9e9e).copy(alpha = .20f), Color(0xff424242).copy(alpha = .36f))) }
    Box(Modifier.size(44.dp).background(tint, CircleShape).border(1.dp, Color.White.copy(alpha = .34f), CircleShape))
}

private val redNoteIconFont = FontFamily(Font(R.font.rednote_material_icons))

@Composable
internal fun PlainGalleryGlyph(codepoint: Int, tint: Color) {
    val size = with(LocalDensity.current) { 24.dp.toSp() }
    Text(String(Character.toChars(codepoint)), Modifier.size(24.dp), color = tint,
        fontFamily = redNoteIconFont, fontSize = size, lineHeight = size)
}