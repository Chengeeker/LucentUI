package dev.chengeeker.lucent.review

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect as AndroidRect
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import coil.Coil
import coil.annotation.ExperimentalCoilApi
import coil.request.CachePolicy
import coil.request.ImageRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min

/**
 * App-specific original-file source. Must return a LOCAL, COMPLETE and immutable
 * file, ideally keyed by the remote object's version; no downscaled image bytes.
 * progress(loaded,total) is best-effort and can have a null total.
 */
typealias GalleryOriginalLoader = suspend (GalleryMedia, (Long, Long?) -> Unit) -> File

enum class GalleryHiResPhase { IDLE, LOADING, READY, FAILED }

@Stable
class GalleryHiResState internal constructor(val url: String) {
    var phase by mutableStateOf(GalleryHiResPhase.IDLE)
        internal set
    var downloaded by mutableLongStateOf(0L)
        internal set
    var total by mutableStateOf<Long?>(null)
        internal set
    var error by mutableStateOf<String?>(null)
        internal set
    var tileIssue by mutableStateOf<String?>(null)
        internal set
    var requested by mutableStateOf(false)
        internal set
    internal var file by mutableStateOf<File?>(null)
    internal var generation by mutableIntStateOf(0)
    fun request() { requested = true }
    fun retry() { file = null; phase = GalleryHiResPhase.IDLE; error = null; requested = true; generation++ }
}

@Composable
fun rememberGalleryHiResState(url: String): GalleryHiResState = remember(url) { GalleryHiResState(url) }

internal data class GalleryRegionPlan(
    val rect: AndroidRect,
    val sampleSize: Int,
    val fittedLeft: Float,
    val fittedTop: Float,
    val fittedWidth: Float,
    val fittedHeight: Float,
    val originalWidth: Int,
    val originalHeight: Int,
)

/** Maps visible viewport back into source pixels instead of enlarging a preview bitmap. */
internal fun galleryRegionPlan(
    imageWidth: Int, imageHeight: Int,
    viewport: Size, zoom: Float, pan: Offset,
): GalleryRegionPlan? {
    if (imageWidth <= 0 || imageHeight <= 0 || viewport.width <= 0f || viewport.height <= 0f ||
        !zoom.isFinite() || zoom < 1f || !pan.x.isFinite() || !pan.y.isFinite()) return null
    val fit = min(viewport.width / imageWidth, viewport.height / imageHeight)
    if (fit <= 0f) return null
    val fittedW = imageWidth * fit
    val fittedH = imageHeight * fit
    val left = (viewport.width - fittedW) * .5f
    val top = (viewport.height - fittedH) * .5f
    val margin = 48f
    val visibleLeft = ( -margin - viewport.width * .5f - pan.x) / zoom + viewport.width * .5f
    val visibleTop = ( -margin - viewport.height * .5f - pan.y) / zoom + viewport.height * .5f
    val visibleRight = (viewport.width + margin - viewport.width * .5f - pan.x) / zoom + viewport.width * .5f
    val visibleBottom = (viewport.height + margin - viewport.height * .5f - pan.y) / zoom + viewport.height * .5f
    val sourceLeft = floor((visibleLeft - left) / fit).toInt().coerceIn(0, imageWidth)
    val sourceTop = floor((visibleTop - top) / fit).toInt().coerceIn(0, imageHeight)
    val sourceRight = ceil((visibleRight - left) / fit).toInt().coerceIn(0, imageWidth)
    val sourceBottom = ceil((visibleBottom - top) / fit).toInt().coerceIn(0, imageHeight)
    if (sourceRight <= sourceLeft || sourceBottom <= sourceTop) return null
    val regionWidth = sourceRight - sourceLeft
    val regionHeight = sourceBottom - sourceTop
    var sample = 1
    // Keep the decoded tile below ~4 million ARGB pixels. Avoid full-image allocations.
    while (regionWidth.toLong() * regionHeight > 4_194_304L * sample * sample ||
        regionWidth / sample > 2560 || regionHeight / sample > 2560) {
        sample *= 2
    }
    val pixelPerScreen = 1f / (fit * zoom)
    while (sample * 2 <= pixelPerScreen && sample < 32) sample *= 2
    return GalleryRegionPlan(
        AndroidRect(sourceLeft, sourceTop, sourceRight, sourceBottom), sample,
        left, top, fittedW, fittedH, imageWidth, imageHeight,
    )
}

/** EXIF coordinates refer to encoded pixels; the preview is already orientation-correct. */
internal fun galleryUnrotateRegion(rect: AndroidRect, width: Int, height: Int, orientation: Int): AndroidRect = when (orientation) {
    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> AndroidRect(width - rect.right, rect.top, width - rect.left, rect.bottom)
    ExifInterface.ORIENTATION_ROTATE_180 -> AndroidRect(width - rect.right, height - rect.bottom, width - rect.left, height - rect.top)
    ExifInterface.ORIENTATION_FLIP_VERTICAL -> AndroidRect(rect.left, height - rect.bottom, rect.right, height - rect.top)
    ExifInterface.ORIENTATION_TRANSPOSE -> AndroidRect(rect.top, rect.left, rect.bottom, rect.right)
    ExifInterface.ORIENTATION_ROTATE_90 -> AndroidRect(rect.top, height - rect.right, rect.bottom, height - rect.left)
    ExifInterface.ORIENTATION_TRANSVERSE -> AndroidRect(width - rect.bottom, height - rect.right, width - rect.top, height - rect.left)
    ExifInterface.ORIENTATION_ROTATE_270 -> AndroidRect(width - rect.bottom, rect.left, width - rect.top, rect.right)
    else -> AndroidRect(rect)
}

internal fun galleryRotateTile(bitmap: Bitmap, orientation: Int): Bitmap {
    val matrix = Matrix().apply {
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> setValues(floatArrayOf(0f,1f,0f, 1f,0f,0f, 0f,0f,1f))
            ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> setValues(floatArrayOf(0f,-1f,0f, -1f,0f,0f, 0f,0f,1f))
            ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(270f)
            else -> return bitmap
        }
    }
    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    if (rotated !== bitmap) bitmap.recycle()
    return rotated
}

private data class GalleryDecodedTile(val bitmap: Bitmap, val plan: GalleryRegionPlan)

@OptIn(ExperimentalCoilApi::class)
private suspend fun originalFromCoil(context: Context, media: GalleryMedia, progress: (Long, Long?) -> Unit): File =
    withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "gallery-originals").apply { mkdirs() }
        val hash = MessageDigest.getInstance("SHA-256").digest(media.url.toByteArray(Charsets.UTF_8))
            .joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
        val result = File(dir, "$hash.original")
        if (result.isFile && result.length() > 0L) {
            progress(result.length(), result.length())
            return@withContext result
        }
        val loader = Coil.imageLoader(context)
        if (loader.diskCache?.openSnapshot(media.url)?.use { true } != true) {
            // Small decode, but the configured image loader fetches the source bytes.
            // This delegates authentication and network policies to the host app.
            val request = ImageRequest.Builder(context).data(media.url).size(64, 64)
                .memoryCachePolicy(CachePolicy.DISABLED).diskCachePolicy(CachePolicy.ENABLED).build()
            val response = loader.execute(request)
            if (response !is coil.request.SuccessResult) throw IOException("原图缓存失败，请检查网络")
        }
        val snapshot = loader.diskCache?.openSnapshot(media.url)
            ?: throw IOException("无法从图片缓存取得原始文件；请由应用提供原图加载器")
        snapshot.use {
            val src = it.data.toFile()
            val expected = src.length()
            val temp = File.createTempFile("origin-", ".tmp", dir)
            try {
                src.inputStream().buffered().use { input ->
                    temp.outputStream().buffered().use { output ->
                        val buffer = ByteArray(128 * 1024)
                        var loaded = 0L
                        while (true) {
                            coroutineContext.ensureActive()
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                            loaded += count
                            progress(loaded, expected)
                        }
                    }
                }
                if (temp.length() != expected || expected <= 0L) throw IOException("缓存原图不完整")
                if (!temp.renameTo(result)) throw IOException("无法写入原图缓存")
            } finally { temp.delete() }
        }
        result
    }

/**
 * Mounted INSIDE the same graphicsLayer transform as the preview image.
 * A tiny decoded tile replaces only pixels currently visible; zoom/pan drives
 * source-region re-decode. Never decode the full compressed image into RAM.
 */
@Composable
fun GalleryHighResolutionLayer(
    media: GalleryMedia,
    state: GalleryHiResState,
    zoom: Float,
    pan: Offset,
    viewport: Size,
    modifier: Modifier = Modifier,
    originalLoader: GalleryOriginalLoader? = null,
    preserveHdr: Boolean = false,
    active: Boolean = true,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(media.url, zoom, active, preserveHdr) {
        if (active && zoom >= 1.6f && !preserveHdr) state.request()
    }
    LaunchedEffect(media.url, state.requested, state.generation, originalLoader) {
        if (!state.requested || state.file != null) return@LaunchedEffect
        state.phase = GalleryHiResPhase.LOADING
        state.error = null
        try {
            val file = (originalLoader ?: { input: GalleryMedia, update: (Long, Long?) -> Unit ->
                originalFromCoil(context, input, update)
            })(media) { received, expected ->
                state.downloaded = received
                state.total = expected
            }
            coroutineContext.ensureActive()
            if (!file.isFile || file.length() <= 0L) throw IOException("原始图片文件不存在")
            state.file = file
            state.phase = GalleryHiResPhase.READY
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            state.error = error.message ?: "原图加载失败"
            state.phase = GalleryHiResPhase.FAILED
        }
    }

    val file = state.file
    var tile by remember(media.url) { mutableStateOf<GalleryDecodedTile?>(null) }
    var lastError by remember(media.url) { mutableStateOf(false) }
    LaunchedEffect(file, viewport, zoom, pan, preserveHdr, active) {
        if (file == null || !active || preserveHdr || lastError) return@LaunchedEffect
        delay(100) // Debounce gesture frames and double-tap zoom animations.
        try {
            val next = withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                val decoder = BitmapRegionDecoder.newInstance(file.absolutePath, false)
                try {
                    val orientation = runCatching {
                        ExifInterface(file.absolutePath).getAttributeInt(
                            ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL,
                        )
                    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
                    val sideways = orientation in ExifInterface.ORIENTATION_TRANSPOSE..ExifInterface.ORIENTATION_ROTATE_270
                    val orientedWidth = if (sideways) decoder.height else decoder.width
                    val orientedHeight = if (sideways) decoder.width else decoder.height
                    val plan = galleryRegionPlan(orientedWidth, orientedHeight, viewport, zoom, pan)
                        ?: return@withContext null
                    val rawRegion = galleryUnrotateRegion(plan.rect, decoder.width, decoder.height, orientation)
                    @Suppress("DEPRECATION")
                    val bitmap = decoder.decodeRegion(rawRegion, BitmapFactory.Options().apply {
                        inSampleSize = plan.sampleSize
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }) ?: throw IOException("高清区域解码失败")
                    GalleryDecodedTile(galleryRotateTile(bitmap, orientation), plan)
                } finally { decoder.recycle() }
            }
            coroutineContext.ensureActive()
            tile = next
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            // Keep the already displayed preview intact on unsupported formats.
            state.tileIssue = error.message ?: "暂不支持对该格式进行高清区域解码"
            lastError = true
        }
    }

    val current = tile?.takeIf { !it.bitmap.isRecycled && !preserveHdr && active }
    if (current != null) {
        DisposableEffect(current.bitmap) { onDispose { current.bitmap.recycle() } }
        Canvas(modifier.fillMaxSize()) {
            val p = current.plan
            val src = p.rect
            val dx = p.fittedLeft + src.left.toFloat() / p.originalWidth * p.fittedWidth
            val dy = p.fittedTop + src.top.toFloat() / p.originalHeight * p.fittedHeight
            val dw = src.width().toFloat() / p.originalWidth * p.fittedWidth
            val dh = src.height().toFloat() / p.originalHeight * p.fittedHeight
            drawImage(
                current.bitmap.asImageBitmap(),
                dstOffset = IntOffset(dx.toInt(), dy.toInt()),
                dstSize = IntSize(ceil(dw).toInt().coerceAtLeast(1), ceil(dh).toInt().coerceAtLeast(1)),
                filterQuality = FilterQuality.Medium,
            )
        }
    }
}
