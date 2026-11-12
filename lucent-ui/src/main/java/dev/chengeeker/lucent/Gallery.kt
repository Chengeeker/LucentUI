package dev.chengeeker.lucent

import androidx.compose.runtime.Composable
import dev.chengeeker.lucent.review.GalleryMedia
import dev.chengeeker.lucent.review.GalleryOrigin
import dev.chengeeker.lucent.review.ReviewGallery

data class GalleryItem(val uri: String, val description: String, val hdr: Boolean = false)

@Composable
fun LucentGallery(items: List<GalleryItem>, start: Int, sources: Map<Int, GalleryOrigin> = emptyMap(),
    onDismiss: () -> Unit, onSave: (GalleryItem) -> Unit) {
    if (items.isEmpty()) return
    ReviewGallery(items.map { GalleryMedia(it.uri, long = it.uri.endsWith("/long.jpg"), gif = it.uri.endsWith(".gif", true), hdr = it.hdr, description = it.description) }, start, sources, onDismiss,
        onSave = { media -> items.firstOrNull { it.uri == media.url }?.let(onSave) })
}
