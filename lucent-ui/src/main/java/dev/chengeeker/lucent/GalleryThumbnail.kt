package dev.chengeeker.lucent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dev.chengeeker.lucent.review.*

/** Keep this source mounted underneath the gallery until its return flight completes. */
@Composable
fun GalleryThumbnail(media: GalleryMedia, source: GalleryOrigin, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val context = LocalContext.current
    val request = remember(context, media.url) {
        ImageRequest.Builder(context).data(media.url).allowHardware(false).build()
    }
    Box(modifier.onGloballyPositioned { source.coordinates = it }
        .clip(RoundedCornerShape(source.radius))
        // Hide only the drawing; the source must still consume taps during a fast return/reopen.
        .graphicsLayer { alpha = if (source.covered) 0f else 1f }
        .lucentClick(onClick = onClick)) {
        AsyncImage(request, media.description, Modifier.fillMaxSize(),
            contentScale = if (source.crop) ContentScale.Crop else ContentScale.Fit,
            onSuccess = { source.bitmap = it.result.drawable.galleryBitmap() })
        if (media.video.isNotBlank()) {
            Icon(Icons.Rounded.PlayCircle, "播放视频", Modifier.align(Alignment.Center).size(44.dp), tint = androidx.compose.ui.graphics.Color.White)
            if (media.duration.isNotBlank()) Text(media.duration,
                Modifier.align(Alignment.BottomEnd).padding(4.dp)
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = .72f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp), color = androidx.compose.ui.graphics.Color.White, fontSize = 10.sp)
        }
        if (media.hdr) HdrBadge(Modifier.align(Alignment.BottomEnd).padding(6.dp))
    }
}
