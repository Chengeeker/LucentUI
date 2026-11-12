// Adapted from Review, Copyright 2026 Chengeeker, MIT.
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package dev.chengeeker.lucent.review

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.os.Build
import android.widget.ImageView
import coil.compose.SubcomposeAsyncImageContent
import androidx.core.graphics.drawable.toBitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.MotionPhotosOn
import androidx.compose.material.icons.rounded.PauseCircleOutline
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material3.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlin.math.abs
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified

val LocalGalleryOpenFrom = staticCompositionLocalOf<((List<GalleryMedia>, Int, Map<Int, GalleryOrigin>) -> Unit)?> { null }
internal fun GalleryMedia.galleryPreviewUrl(): String = (if (long || (width > 0 && height.toFloat() / width > 2f)) url else preview.ifBlank { url })
    .let { if (gif) it else it }
internal fun galleryDoubleTapScale(scale: Float, long: Boolean): Float = when {
    scale <= 1.01f -> if (long) 3.5f else 2.5f
    scale <= 3.6f -> 6f
    else -> 1f
}

private val galleryCurve = CubicBezierEasing(.215f, .61f, .355f, 1f)

@Composable
fun ReviewGallery(media: List<GalleryMedia>, start: Int, sources: Map<Int, GalleryOrigin> = emptyMap(), onClose: () -> Unit, saving: Boolean = false, onSave: (GalleryMedia) -> Unit) {
    require(media.isNotEmpty())
    val initial = start.coerceIn(media.indices)
    val pager = rememberPagerState(initial) { media.size }
    val scope = rememberCoroutineScope()
    val origin = remember { sources[initial] }
    val openingAnchor = remember { origin?.anchor() }
    val openingImage = remember { origin?.bitmap?.takeUnless(Bitmap::isRecycled) }
    val motion = remember { GalleryOpeningMotion(scope, if (openingAnchor != null && openingImage != null) 0f else 1f) }
    var flight by remember { mutableStateOf<GalleryFlight?>(null) }
    var returnGeneration by remember { mutableIntStateOf(0) }
    var viewport by remember { mutableStateOf(Rect.Zero) }
    val pagePlanes = remember { mutableMapOf<Int, () -> GalleryPlane?>() }
    val closing = motion.returning
    val preparingFlight = openingAnchor != null && openingImage != null && motion.position == 0f && !closing
    val morphing = preparingFlight || (flight != null && (motion.position < 1f || closing))
    DisposableEffect(flight?.source) {
        val source = flight?.source
        source?.covered = true
        onDispose { source?.covered = false }
    }
    LaunchedEffect(motion.position == 1f, closing) {
        if (motion.position == 1f && !closing) {
            withFrameNanos {}
            if (!motion.returning && motion.position == 1f) flight = null
        }
    }
    fun close() {
        if (motion.returning) return
        if (flight == null || motion.position >= 1f) {
            val page = pager.currentPage
            val source = sources[page]
            val anchor = source?.anchor()
            val plane = pagePlanes[page]?.invoke()
            flight = if (source != null && anchor != null && plane != null) GalleryFlight(source, anchor, plane) else null
            pager.requestScrollToPage(page)
        }
        val generation = ++returnGeneration
        motion.returnToSource {
            flight?.source?.covered = false
            scope.launch {
                withFrameNanos {}
                snapshotFlow { !motion.holdingGesture || !motion.returning || generation != returnGeneration }.first { it }
                if (motion.returning && generation == returnGeneration) onClose()
            }
        }
    }
    @Composable fun Content(inDialog: Boolean) {
        val pulse = reviewPulse()
        val item = media[pager.currentPage]
        val context = LocalContext.current
        var zoomed by remember { mutableStateOf(false) }
        var dragY by remember(pager.currentPage) { mutableFloatStateOf(0f) }
        var dragFraction by remember(pager.currentPage) { mutableFloatStateOf(0f) }

        var showControls by remember { mutableStateOf(true) }




        var thumbnailScrolling by remember { mutableStateOf(false) }
        val mainDragging by pager.interactionSource.collectIsDraggedAsState()

        val density = LocalDensity.current
        val landscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val view = LocalView.current
        val window = if (inDialog) (view.parent as? DialogWindowProvider)?.window else LocalActivity.current?.window
        DisposableEffect(window, inDialog) {
            if (inDialog) window?.apply {
                setWindowAnimations(0)
                setDimAmount(0f)
                setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                androidx.core.view.WindowCompat.setDecorFitsSystemWindows(this, false)
                androidx.core.view.WindowCompat.getInsetsController(this, view).apply { isAppearanceLightStatusBars = false; isAppearanceLightNavigationBars = false }
            }
            val bars = window?.let { androidx.core.view.WindowCompat.getInsetsController(it, view) }
            val lightStatus = bars?.isAppearanceLightStatusBars
            val lightNavigation = bars?.isAppearanceLightNavigationBars
            bars?.apply { isAppearanceLightStatusBars = false; isAppearanceLightNavigationBars = false }
            val status = androidx.core.view.WindowInsetsCompat.Type.statusBars()
            onDispose {
                if (!inDialog) bars?.apply {
                    show(status)
                    isAppearanceLightStatusBars = lightStatus ?: false
                    isAppearanceLightNavigationBars = lightNavigation ?: false
                }
            }
        }
        LaunchedEffect(window, showControls) {
            window?.let {
                val bars = androidx.core.view.WindowCompat.getInsetsController(it, view)
                val status = androidx.core.view.WindowInsetsCompat.Type.statusBars()
                if (showControls) bars.show(status) else bars.hide(status)
            }
        }
        val hasHdr = item.hdr
        var hdrActive by remember(window) { mutableStateOf(false) }
        LaunchedEffect(hasHdr, motion.position == 1f, closing) {
            if (!hasHdr) hdrActive = false
            else if (motion.position == 1f && !closing) hdrActive = true
        }
        DisposableEffect(window, hdrActive) {
            val original = window?.colorMode
            val changed = Build.VERSION.SDK_INT >= 34 && hdrActive && view.display?.isHdr == true && original != ActivityInfo.COLOR_MODE_HDR
            if (changed) {
                window?.colorMode = ActivityInfo.COLOR_MODE_HDR
                if (Build.VERSION.SDK_INT >= 35) window?.setDesiredHdrHeadroom(0f)
            }
            onDispose { if (changed && original != null) window?.colorMode = original }
        }
        BackHandler { close() }
        fun save(target: GalleryMedia) = onSave(target)
        val mediaLayer = androidx.compose.ui.graphics.rememberGraphicsLayer()
        val mediaBackdrop = remember(mediaLayer) { ReviewBackdrop(mediaLayer) }
        CompositionLocalProvider(LocalBackdrop provides mediaBackdrop) {
        Box(Modifier.fillMaxSize().onGloballyPositioned { coordinates ->
            val position = coordinates.positionOnScreen()
            if (!position.isSpecified) return@onGloballyPositioned
            viewport = Rect(position, Size(coordinates.size.width.toFloat(), coordinates.size.height.toFloat()))
            if (motion.position == 0f && !motion.returning && flight == null && origin != null && openingAnchor != null && openingImage != null) {
                val destination = galleryImagePlane(viewport, Size(openingImage.width.toFloat(), openingImage.height.toFloat()), crop = false)
                flight = GalleryFlight(origin, openingAnchor, GalleryPlane(openingImage, destination, viewport))
                motion.open()
            }
        }.galleryOverlayInput(
            target = { if (motion.returning) flight?.anchor?.visible?.translate(-viewport.topLeft) else null },
            canSwipe = { flight != null && media.size > 1 && !motion.returning },
            hold = { motion.holdingGesture = it },
            reopen = { if (motion.returning) { returnGeneration++; flight?.source?.covered = true; pulse(); motion.open() } },
            swipe = { direction ->
                val index = pager.currentPage + if (direction < 0) 1 else -1
                if (index in media.indices) {
                    motion.finishOpening(); flight = null
                    scope.launch { pager.scrollToPage(index) }
                }
            },
        ).graphicsLayer { alpha = if (closing && !morphing) motion.position else 1f }
            .background(Color.Black.copy(alpha = motion.position * (1f - dragFraction).coerceIn(0f, 1f)))) {
            HorizontalPager(pager, userScrollEnabled = !zoomed && !closing && !morphing,
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = if (morphing) 0f else 1f }.recordBackdrop(mediaBackdrop), beyondViewportPageCount = 0) { page ->
                val picture = media[page]
                run {
                    val firstBitmap = remember(page) { if (page == initial) openingImage else sources[page]?.bitmap }
                    var scale by remember(page) { mutableFloatStateOf(1f) }
                    var pan by remember(page) { mutableStateOf(Offset.Zero) }
                    var displayedBitmap by remember(page) { mutableStateOf(firstBitmap) }
                    var zoomAnimation by remember(page) { mutableStateOf<kotlinx.coroutines.Job?>(null) }
                    DisposableEffect(page) {
                        pagePlanes[page] = {
                            displayedBitmap?.takeUnless(Bitmap::isRecycled)?.let { bitmap ->
                                val plane = galleryImagePlane(viewport, Size(bitmap.width.toFloat(), bitmap.height.toFloat()), crop = false)
                                val center = viewport.center
                                GalleryPlane(bitmap, Rect(center + (plane.topLeft - center) * scale + pan + Offset(0f, dragY),
                                    center + (plane.bottomRight - center) * scale + pan + Offset(0f, dragY)), viewport)
                            }
                        }
                        onDispose { pagePlanes.remove(page) }
                    }
                    LaunchedEffect(pager.currentPage) { zoomAnimation?.cancel(); scale = 1f; pan = Offset.Zero; zoomed = false }
                    BoxWithConstraints(Modifier.fillMaxSize()) {
                        val density = LocalDensity.current
                        val pageWidth = maxWidth; val pageHeight = maxHeight
                        val width = with(density) { pageWidth.toPx() }; val height = with(density) { pageHeight.toPx() }
                        val gesture = rememberTransformableState { zoom, offset, _ ->
                            zoomAnimation?.cancel()
                            scale = (scale * zoom).coerceIn(1f, 8f)
                            pan = Offset((pan.x + offset.x).coerceIn(-width * (scale - 1) / 2, width * (scale - 1) / 2),
                                (pan.y + offset.y).coerceIn(-height * (scale - 1) / 2, height * (scale - 1) / 2)); zoomed = scale > 1.01f
                        }
                        Box(Modifier.fillMaxSize().transformable(gesture, canPan = { scale > 1f })
                            .pointerInput(page, width, height) { detectTapGestures(onTap = { tap ->
                                if (tap.x >= width / 3 && tap.x < width * 2 / 3) showControls = !showControls else close()
                            }, onDoubleTap = { tap ->
                                pulse()
                                zoomAnimation?.cancel()
                                val target = galleryDoubleTapScale(scale, picture.long)
                                val from = pan
                                val focus = Offset(tap.x - width / 2, tap.y - height / 2)
                                val destination = focus - (focus - from) * (target / scale)
                                val boundX = width * (target - 1) / 2
                                val boundY = height * (target - 1) / 2
                                val to = Offset(destination.x.coerceIn(-boundX, boundX), destination.y.coerceIn(-boundY, boundY))
                                zoomed = true
                                zoomAnimation = scope.launch {
                                    val initialScale = scale
                                    Animatable(0f).animateTo(1f, tween(220, easing = galleryCurve)) {
                                        scale = initialScale + (target - initialScale) * value
                                        pan = from + (to - from) * value
                                    }
                                    zoomed = scale > 1.01f
                                }
                            }) }
                            .pointerInput(page, zoomed, closing, height) { if (!zoomed && !closing) detectVerticalDragGestures(onDragEnd = {
                                if (abs(dragY) > height * .18f) { pulse(); close() }
                                else scope.launch { Animatable(dragY).animateTo(0f, tween(200, easing = galleryCurve)) { dragY = value; dragFraction = abs(value) / (height * .5f) } }
                            }, onDragCancel = { dragY = 0f; dragFraction = 0f }) { _, delta ->
                                dragY += delta; dragFraction = abs(dragY) / (height * .5f)
                            } }, contentAlignment = Alignment.Center) {
                            Box(Modifier.fillMaxSize().graphicsLayer { scaleX = scale; scaleY = scale; translationX = pan.x; translationY = pan.y + dragY }, contentAlignment = Alignment.Center) {
                                OriginalImage(picture, Modifier.fillMaxSize(),
                                    onBitmap = { displayedBitmap = it }, showImage = !morphing)

                            }
                        }
                    }
                }
            }
            flight?.let { GalleryFlightImage(it, viewport.topLeft, motion.position) }
            val chrome = showControls && !morphing && !closing && dragFraction == 0f
            val desiredThumbnailBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 32.dp
            var thumbnailBottom by remember { mutableStateOf(desiredThumbnailBottom) }
            LaunchedEffect(desiredThumbnailBottom, thumbnailScrolling) { if (!thumbnailScrolling) thumbnailBottom = desiredThumbnailBottom }
            if (chrome) Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                GalleryGlassButton(Icons.AutoMirrored.Outlined.ArrowBack, "返回", click = ::close)
                Spacer(Modifier.weight(1f))
                    Text("${pager.currentPage + 1} / ${media.size}",
                        Modifier.background(Color.Black.copy(alpha = .45f), RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 6.dp),
                        color = Color.White, fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                GalleryGlassButton(Icons.Rounded.Download, "下载", enabled = !saving, busy = saving) { save(item) }
            }
            if (chrome && hasHdr) HdrBadge(Modifier.align(Alignment.BottomEnd).navigationBarsPadding()
                .padding(end = 16.dp, bottom = if (media.size > 1) 110.dp else 24.dp))
            if (chrome && media.size > 1) GalleryThumbnails(media, pager,
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = thumbnailBottom),
                onScrolling = { thumbnailScrolling = it }, onUserDrag = {})

        }
        }
    }
    if (sources.isEmpty()) {
        Dialog(onDismissRequest = ::close, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) { Content(true) }
    } else Content(false)
}

private fun galleryRatio(media: GalleryMedia): Float = if (media.width > 0 && media.height > 0) media.width.toFloat() / media.height else 1f

@Composable
internal fun GalleryThumbnails(media: List<GalleryMedia>, pager: androidx.compose.foundation.pager.PagerState, modifier: Modifier, onScrolling: (Boolean) -> Unit, onUserDrag: () -> Unit) {
    val list = rememberLazyListState(pager.currentPage)
    val dragged by list.interactionSource.collectIsDraggedAsState()
    val mainDragged by pager.interactionSource.collectIsDraggedAsState()
    var ownsSelection by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val tick = reviewPulse()
    LaunchedEffect(dragged) { if (dragged) onUserDrag() }
    LaunchedEffect(list.isScrollInProgress) { onScrolling(list.isScrollInProgress) }
    LaunchedEffect(dragged, list.isScrollInProgress, mainDragged) {
        if (mainDragged) { ownsSelection = false; list.stopScroll() }
        else if (dragged) ownsSelection = true
        else if (!list.isScrollInProgress) ownsSelection = false
    }
    LaunchedEffect(ownsSelection) {
        if (!ownsSelection) return@LaunchedEffect
        snapshotFlow {
            val layout = list.layoutInfo
            val center = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
            layout.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - center) }?.index
        }.collectLatest { index -> if (index != null && index != pager.currentPage) { tick(); pager.scrollToPage(index) } }
    }
    LaunchedEffect(pager.currentPage, ownsSelection, pager.isScrollInProgress) {
        if (!ownsSelection && !pager.isScrollInProgress) list.scrollToItem(pager.currentPage)
    }
    BoxWithConstraints(modifier) {
        val cell = maxWidth * .16f
        LazyRow(state = list, contentPadding = PaddingValues(horizontal = (maxWidth - cell) / 2), modifier = Modifier.height(64.dp)) {
            items(media.size) { index ->
                val item = media[index]
                val selected = pager.currentPage == index
                val outline = RoundedCornerShape(8.dp)
                Box(Modifier.width(cell).fillMaxHeight().padding(horizontal = 3.dp, vertical = 2.dp)
                    .then(if (selected) Modifier.border(2.dp, Color.White, outline) else Modifier)
                    .semantics { this.selected = selected }
                    .reviewClickable { scope.launch { list.scrollToItem(index); pager.scrollToPage(index) } }) {
                    Box(Modifier.fillMaxSize().padding(2.dp).clip(RoundedCornerShape(6.dp))) {
                        val image = Modifier.fillMaxSize()
                        GalleryMediaImage(item.copy(url = item.preview.ifBlank { item.url }), "第 ${index + 1} 张图片", image)
                        if (!selected) Box(Modifier.matchParentSize().background(Color.White.copy(alpha = .5f)))
                        if (item.hdr) HdrBadge(Modifier.align(Alignment.BottomEnd).padding(3.dp),
                            description = "第 ${index + 1} 张 HDR 图片", compact = true)
                    }
                }
            }
        }
    }
}


data class GalleryMedia(val url: String, val preview: String = "", val width: Int = 0, val height: Int = 0,
    val long: Boolean = false, val gif: Boolean = false, val hdr: Boolean = false, val description: String = "图片")

@Composable private fun GalleryMediaImage(media: GalleryMedia, description: String, modifier: Modifier) {
    coil.compose.AsyncImage(media.url, description, modifier, contentScale = androidx.compose.ui.layout.ContentScale.Crop)
}

@Composable private fun OriginalImage(media: GalleryMedia, modifier: Modifier, onBitmap: (Bitmap) -> Unit, showImage: Boolean) {
    var attempt by remember(media.url) { mutableIntStateOf(0) }
    val request = coil.request.ImageRequest.Builder(LocalContext.current).data(media.url).allowHardware(false).build()
    key(media.url, attempt) {
        coil.compose.SubcomposeAsyncImage(request, media.description, modifier.graphicsLayer { alpha = if (showImage) 1f else 0f },
            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
            onSuccess = { state -> onBitmap(state.result.drawable.galleryBitmap()) },
            success = { state ->
                val bitmap = (state.result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                if (bitmap == null) SubcomposeAsyncImageContent()
                else AndroidView(
                    factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.FIT_CENTER } },
                    modifier = Modifier.fillMaxSize(),
                    update = { it.setImageBitmap(bitmap); it.contentDescription = media.description },
                    onRelease = { it.setImageDrawable(null) },
                )
            },
            loading = { Box(contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp), color = Color.White) } },
            error = { Box(contentAlignment = Alignment.Center) { TextButton(onClick = hapticClick { attempt++ }) { Text("图片加载失败，点击重试", color = Color.White) } } })
    }
}

@Composable
fun HdrBadge(modifier: Modifier = Modifier, description: String = "HDR 图片", compact: Boolean = false) {
    Text("HDR", modifier.semantics { contentDescription = description }
        .background(Color.Black.copy(alpha = .4f), RoundedCornerShape(4.dp))
        .padding(horizontal = if (compact) 3.dp else 6.dp, vertical = if (compact) 1.dp else 2.dp),
        color = Color.White, fontSize = if (compact) 8.sp else 10.sp, lineHeight = if (compact) 9.sp else 12.sp,
        fontWeight = FontWeight.Normal)
}

internal fun android.graphics.drawable.Drawable.galleryBitmap(): Bitmap {
    if (this is android.graphics.drawable.BitmapDrawable) return bitmap
    val width = intrinsicWidth.coerceAtLeast(1)
    val height = intrinsicHeight.coerceAtLeast(1)
    val scale = minOf(1f, 1024f / maxOf(width, height))
    return toBitmap((width * scale).toInt().coerceAtLeast(1), (height * scale).toInt().coerceAtLeast(1))
}
