package dev.chengeeker.lucent

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import kotlin.math.abs
import kotlinx.coroutines.launch

data class GalleryItem(val uri: String, val description: String)

@Composable
fun LucentGallery(
    items: List<GalleryItem>,
    initial: Int = 0,
    origin: Rect? = null,
    onDismiss: () -> Unit,
    onSave: (GalleryItem) -> Unit,
    onShare: (GalleryItem) -> Unit,
) {
    if (items.isEmpty()) {
        LucentEmpty("还没有图片", "关闭后重新选择图片。", action = { LucentAction("关闭", onDismiss) })
        return
    }
    val pager = rememberPagerState(initialPage = initial.coerceIn(items.indices)) { items.size }
    val scope = rememberCoroutineScope()
    val style = LocalLucentStyle.current
    val flight = remember { Animatable(if (style.motion) 0f else 1f) }
    var closing by remember { mutableStateOf(false) }
    var controls by remember { mutableStateOf(true) }
    var zoomed by remember { mutableStateOf(false) }
    var viewport by remember { mutableStateOf(IntSize(1, 1)) }
    var dismissOffset by remember { mutableFloatStateOf(0f) }
    fun close() {
        if (!closing) {
            closing = true
            scope.launch {
                flight.animateTo(0f, if (style.motion) tween(240) else snap())
                onDismiss()
            }
        }
    }
    LaunchedEffect(Unit) {
        flight.animateTo(1f, if (style.motion) tween(350, easing = FastOutSlowInEasing) else snap())
    }
    BackHandler { close() }
    Box(
        Modifier.fillMaxSize()
            .onSizeChanged { viewport = it }
            .background(
                Color.Black.copy(
                    alpha =
                        flight.value * (1 - abs(dismissOffset) / viewport.height).coerceIn(.3f, 1f)
                )
            )
    ) {
        HorizontalPager(pager, Modifier.fillMaxSize(), userScrollEnabled = !zoomed && !closing) {
            page ->
            var loading by remember { mutableStateOf(true) }
            var failed by remember { mutableStateOf(false) }
            var retry by remember { mutableIntStateOf(0) }
            var scale by remember { mutableFloatStateOf(1f) }
            var pan by remember { mutableStateOf(Offset.Zero) }
            LaunchedEffect(pager.currentPage) {
                if (page != pager.currentPage) {
                    scale = 1f
                    pan = Offset.Zero
                } else zoomed = scale > 1.01f
            }
            Box(
                Modifier.fillMaxSize()
                    .graphicsLayer {
                        val start = if (page == initial) origin else null
                        val initialScale = start?.let { it.width / viewport.width } ?: .92f
                        scaleX = initialScale + (1 - initialScale) * flight.value
                        scaleY = scaleX
                        translationX =
                            (start?.center?.x?.minus(viewport.width / 2f) ?: 0f) *
                                (1 - flight.value)
                        translationY =
                            (start?.center?.y?.minus(viewport.height / 2f) ?: 0f) *
                                (1 - flight.value) + dismissOffset
                        alpha = if (start == null) flight.value else 1f
                    }
                    .pointerInput(page, scale > 1f) {
                        detectTapGestures(
                            onTap = { controls = !controls },
                            onDoubleTap = { tap ->
                                scale = if (scale > 1f) 1f else 2.5f
                                pan =
                                    if (scale == 1f) Offset.Zero
                                    else
                                        Offset(
                                            (size.width / 2 - tap.x) * (scale - 1),
                                            (size.height / 2 - tap.y) * (scale - 1),
                                        )
                                zoomed = scale > 1f
                            },
                        )
                    }
                    .pointerInput(page) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            do {
                                val event = awaitPointerEvent()
                                if (event.changes.count { it.pressed } > 1 || scale > 1.01f) {
                                    scale = (scale * event.calculateZoom()).coerceIn(1f, 5f)
                                    val delta = event.calculatePan()
                                    val boundX = size.width * (scale - 1) / 2
                                    val boundY = size.height * (scale - 1) / 2
                                    pan =
                                        Offset(
                                            (pan.x + delta.x).coerceIn(-boundX, boundX),
                                            (pan.y + delta.y).coerceIn(-boundY, boundY),
                                        )
                                    zoomed = scale > 1.01f
                                    event.changes.forEach { it.consume() }
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    }
                    .pointerInput(page, zoomed) {
                        if (!zoomed)
                            detectVerticalDragGestures(
                                onVerticalDrag = { change, dy ->
                                    change.consume()
                                    dismissOffset += dy
                                },
                                onDragCancel = { dismissOffset = 0f },
                                onDragEnd = {
                                    if (abs(dismissOffset) > size.height * .14f) close()
                                    else dismissOffset = 0f
                                },
                            )
                    }
            ) {
                key(retry) {
                    AsyncImage(
                        items[page].uri,
                        items[page].description,
                        Modifier.fillMaxSize().graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = pan.x
                            translationY = pan.y
                        },
                        contentScale = ContentScale.Fit,
                        onLoading = {
                            loading = true
                            failed = false
                        },
                        onSuccess = {
                            loading = false
                            failed = false
                        },
                        onError = {
                            loading = false
                            failed = true
                        },
                    )
                }
                if (loading)
                    CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
                if (failed)
                    Column(
                        Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("图片加载失败", color = Color.White)
                        LucentAction(
                            "重试",
                            {
                                failed = false
                                loading = true
                                retry++
                            },
                        )
                    }
            }
        }
        if (controls) {
            Row(
                Modifier.align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(12.dp)
                    .graphicsLayer { alpha = flight.value },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GalleryControl(Icons.Rounded.Close, "关闭图片") { close() }
                Spacer(Modifier.weight(1f))
                Text(
                    "${pager.currentPage+1} / ${items.size}",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.weight(1f))
                GalleryControl(Icons.Rounded.Download, "保存图片") { onSave(items[pager.currentPage]) }
                GalleryControl(Icons.Rounded.Share, "分享图片") { onShare(items[pager.currentPage]) }
            }
            LazyRow(
                Modifier.align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(64.dp)
                    .background(Color.Black.copy(alpha = .45f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(items) { index, item ->
                    AsyncImage(
                        item.uri,
                        item.description,
                        Modifier.size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                if (index == pager.currentPage) 2.dp else 0.dp,
                                if (index == pager.currentPage) Color.White else Color.Transparent,
                                RoundedCornerShape(8.dp),
                            )
                            .lucentClick {
                                scope.launch {
                                    if (style.motion) pager.animateScrollToPage(index)
                                    else pager.scrollToPage(index)
                                }
                            },
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }
    }
}
