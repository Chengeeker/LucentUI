package dev.chengeeker.lucent.demo

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dev.chengeeker.lucent.GalleryItem
import dev.chengeeker.lucent.demo.data.DemoModel
import dev.chengeeker.lucent.review.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal fun GalleryItem.media() = GalleryMedia(uri, long = uri.endsWith("/long.jpg"),
    gif = uri.endsWith(".gif", true), hdr = hdr, description = description)

private data class GalleryRequest(val media: List<GalleryMedia>, val index: Int, val origins: Map<Int, GalleryOrigin>)

@Composable
internal fun GalleryHost(model: DemoModel, content: @Composable () -> Unit) {
    var request by remember { mutableStateOf<GalleryRequest?>(null) }
    var export by remember { mutableStateOf<GalleryMedia?>(null) }
    val context = LocalContext.current
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/*")) { uri ->
        val item = export
        export = null
        if (uri != null && item != null) model.run("保存图片") {
            withContext(Dispatchers.IO) {
                require(item.url.startsWith("file:///android_asset/media/")) { "不支持的示例图片" }
                context.assets.open(item.url.substringAfter("android_asset/")).use { input ->
                    context.contentResolver.openOutputStream(uri)?.use { input.copyTo(it) } ?: error("无法保存")
                }
            }
            "图片已保存。"
        }
    }
    CompositionLocalProvider(LocalGalleryOpenFrom provides { media, index, sources ->
        if (media.isNotEmpty() && request == null) request = GalleryRequest(media, index, sources)
    }) {
        Box(Modifier.fillMaxSize()) {
            content()
            // Above the page stack and its glass chrome, with the original source still mounted.
            request?.let { current ->
                ReviewGallery(current.media, current.index, current.origins, onClose = { request = null },
                    saving = model.busy != null, onSave = { export = it; save.launch(it.url.substringAfterLast('/')) })
            }
        }
    }
}
