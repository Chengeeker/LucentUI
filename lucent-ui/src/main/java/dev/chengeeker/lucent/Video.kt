@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package dev.chengeeker.lucent

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsControllerCompat
import dev.chengeeker.lucent.review.reviewPulse
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.*
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlin.math.abs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class VideoSource(val label: String, val uri: String)

fun manualQualities(sources: List<VideoSource>) =
    sources
        .filter { it.label.isNotBlank() && it.label != "未知" && it.uri.isNotBlank() }
        .distinctBy { it.uri }
        .distinctBy { it.label }

fun Context.activity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.activity()
        else -> null
    }

@Composable
fun LucentVideo(
    sources: List<VideoSource>,
    modifier: Modifier = Modifier,
    autoplay: Boolean = false,
    initialSpeed: Float = 1f,
    loop: Boolean = false,
    onSpeed: (Float) -> Unit = {},
    onExitFullscreen: () -> Unit = {},
    onControlsHeight: (Int) -> Unit = {},
) {
    val valid = remember(sources) { manualQualities(sources) }
    if (valid.isEmpty()) {
        Box(modifier.background(Color.Black), contentAlignment = Alignment.Center) {
            Text("暂无可播放的视频", color = Color.White)
        }
        return
    }
    val context = LocalContext.current
    val activity = context.activity()
    val lifecycle = LocalLifecycleOwner.current
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true,
            )
            setHandleAudioBecomingNoisy(true)
        }
    }
    var quality by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }
    var position by remember { mutableLongStateOf(0) }
    var duration by remember { mutableLongStateOf(0) }
    var speed by remember { mutableFloatStateOf(initialSpeed) }
    var fullscreen by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf("") }
    var controls by remember { mutableStateOf(true) }
    var hint by remember { mutableStateOf("") }
    var speedCue by remember { mutableStateOf("") }
    var lockCue by remember { mutableStateOf("") }
    var boostLocked by remember { mutableStateOf(false) }
    val latestBoostLocked by rememberUpdatedState(boostLocked)
    val latestSpeed by rememberUpdatedState(speed)
    val gestureScope = rememberCoroutineScope()
    var pendingTap by remember { mutableStateOf<Job?>(null) }
    var pendingTapPosition by remember { mutableStateOf<androidx.compose.ui.geometry.Offset?>(null) }
    var pendingTapTime by remember { mutableLongStateOf(0L) }
    val audio = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val pulse by rememberUpdatedState(reviewPulse())
    val initialBrightness = remember { activity?.window?.attributes?.screenBrightness ?: -1f }
    LaunchedEffect(valid) {
        quality = quality.coerceIn(valid.indices)
        player.setMediaItem(MediaItem.fromUri(valid[quality].uri))
        player.prepare()
        player.playWhenReady = autoplay
        player.setPlaybackSpeed(initialSpeed)
    }
    LaunchedEffect(loop) {
        player.repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }
    DisposableEffect(player, lifecycle) {
        val listener =
            object : Player.Listener {
                override fun onIsPlayingChanged(value: Boolean) {
                    playing = value
                }

                override fun onPlaybackStateChanged(state: Int) {
                    loading = state == Player.STATE_BUFFERING
                    duration = player.duration.coerceAtLeast(0)
                }

                override fun onPlayerError(e: PlaybackException) {
                    error = true
                    loading = false
                }
            }
        player.addListener(listener)
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) player.pause()
        }
        lifecycle.lifecycle.addObserver(observer)
        onDispose {
            lifecycle.lifecycle.removeObserver(observer)
            player.removeListener(listener)
            player.release()
            activity?.window?.let { w ->
                w.attributes = w.attributes.apply { screenBrightness = initialBrightness }
                w.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
    DisposableEffect(playing) {
        if (playing)
            activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else
            activity
                ?.window
                ?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity
                ?.window
                ?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    LaunchedEffect(player) {
        while (true) {
            position = player.currentPosition.coerceAtLeast(0)
            duration = player.duration.coerceAtLeast(0)
            delay(if (playing) 250 else 1000)
        }
    }
    LaunchedEffect(hint) { if (hint.isNotBlank()) { delay(900); hint = "" } }
    LaunchedEffect(speedCue) { if (speedCue.isNotBlank()) { delay(900); speedCue = "" } }
    LaunchedEffect(lockCue) { if (lockCue.isNotBlank()) { delay(900); lockCue = "" } }
    DisposableEffect(activity, fullscreen) {
        val window = activity?.window
        if (!fullscreen || window == null) {
            onDispose {}
        } else {
            val orientation = activity.requestedOrientation
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            val behavior = controller.systemBarsBehavior
            val insets = ViewCompat.getRootWindowInsets(window.decorView)
            val status = WindowInsetsCompat.Type.statusBars()
            val navigation = WindowInsetsCompat.Type.navigationBars()
            val statusVisible = insets?.isVisible(status) != false
            val navigationVisible = insets?.isVisible(navigation) != false
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(status or navigation)
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            onDispose {
                activity.requestedOrientation = orientation
                controller.systemBarsBehavior = behavior
                if (statusVisible) controller.show(status) else controller.hide(status)
                if (navigationVisible) controller.show(navigation) else controller.hide(navigation)
            }
        }
    }
    fun setFull(value: Boolean) {
        fullscreen = value
        controls = true
        if (!value) {
            menu = ""
            onExitFullscreen()
        }
    }
    BackHandler(fullscreen) { setFull(false) }
    BoxWithConstraints(modifier.background(Color.Black)) {
        val showChrome = controls && (!fullscreen || (landscape && maxWidth > maxHeight))
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    useController = false
                    this.player = player
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier.fillMaxSize()
                .pointerInput(player, speed) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val origin = down.position
                        val downAt = down.uptimeMillis
                        val startPosition = player.currentPosition
                        val left = origin.x < size.width / 2
                        val startVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
                        val maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                        val startBrightness = activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0 } ?: .5f
                        var axis = 0
                        var held = false
                        var completed = false
                        var lockOnRelease = false
                        var unlockOnRelease = false
                        var seekTarget = startPosition
                        try {
                            while (true) {
                                val elapsed = (SystemClock.uptimeMillis() - downAt).coerceAtLeast(0L)
                                val wait = (viewConfiguration.longPressTimeoutMillis - elapsed).coerceAtLeast(0L)
                                val event = if (!held && axis == 0) {
                                    withTimeoutOrNull(wait) { awaitPointerEvent(PointerEventPass.Initial) }
                                } else awaitPointerEvent(PointerEventPass.Initial)
                                if (event == null) {
                                    held = true
                                    pendingTapPosition = null
                                    pendingTap?.cancel()
                                    pulse()
                                    speedCue = if (latestBoostLocked) "2× 倍速已锁定" else "2× 临时倍速"
                                    lockCue = ""
                                    player.setPlaybackSpeed(2f)
                                    continue
                                }
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                val offset = change.position - origin
                                if (!held && axis == 0 && change.uptimeMillis - downAt >= viewConfiguration.longPressTimeoutMillis && offset.getDistance() <= viewConfiguration.touchSlop) {
                                    held = true
                                    pendingTapPosition = null
                                    pendingTap?.cancel()
                                    pulse()
                                    speedCue = if (latestBoostLocked) "2× 倍速已锁定" else "2× 临时倍速"
                                    lockCue = ""
                                    player.setPlaybackSpeed(2f)
                                }
                                if (change.pressed) {
                                    if (held) {
                                        val threshold = 48.dp.toPx()
                                        lockOnRelease = !latestBoostLocked && offset.y >= threshold
                                        unlockOnRelease = latestBoostLocked && offset.y <= -threshold
                                        lockCue = when {
                                            lockOnRelease -> "松开锁定倍速"
                                            unlockOnRelease -> "松开恢复原速"
                                            else -> ""
                                        }
                                        change.consume()
                                    } else {
                                        if (axis == 0 && offset.getDistance() > viewConfiguration.touchSlop) {
                                            axis = if (abs(offset.x) > abs(offset.y)) 1 else 2
                                            pendingTapPosition = null
                                        }
                                        when (axis) {
                                            1 -> {
                                                seekTarget = (startPosition + offset.x / size.width * 60_000)
                                                    .toLong().coerceIn(0, player.duration.coerceAtLeast(0))
                                                hint = "定位 ${seekTarget / 1000} 秒"
                                                change.consume()
                                            }
                                            2 -> {
                                                if (left) {
                                                    val value = (startBrightness - offset.y / size.height).coerceIn(.05f, 1f)
                                                    activity?.window?.let { window -> window.attributes = window.attributes.apply { screenBrightness = value } }
                                                    hint = "亮度 ${(value * 100).toInt()}%"
                                                } else {
                                                    val value = (startVolume - offset.y / size.height * maxVolume).toInt().coerceIn(0, maxVolume)
                                                    audio.setStreamVolume(AudioManager.STREAM_MUSIC, value, 0)
                                                    hint = "音量 ${(value * 100 / maxVolume)}%"
                                                }
                                                change.consume()
                                            }
                                        }
                                    }
                                } else {
                                    completed = true
                                    if (held) {
                                        if (unlockOnRelease) boostLocked = false
                                        else if (lockOnRelease) boostLocked = true
                                        player.setPlaybackSpeed(if (boostLocked) 2f else latestSpeed)
                                        speedCue = ""
                                        lockCue = when {
                                            lockOnRelease -> "2× 倍速已锁定"
                                            unlockOnRelease -> "已恢复原速"
                                            else -> ""
                                        }
                                        change.consume()
                                    } else if (axis == 1) {
                                        player.seekTo(seekTarget)
                                        change.consume()
                                    } else if (axis == 2) change.consume()
                                    hint = ""
                                    if (!held && axis == 0) {
                                        val previous = pendingTapPosition
                                        val doubleTap = previous != null &&
                                            change.uptimeMillis - pendingTapTime in viewConfiguration.doubleTapMinTimeMillis..viewConfiguration.doubleTapTimeoutMillis &&
                                            (change.position - previous).getDistance() <= android.view.ViewConfiguration.get(context).scaledDoubleTapSlop
                                        if (doubleTap) {
                                            pendingTap?.cancel()
                                            pendingTap = null
                                            pendingTapPosition = null
                                            pulse()
                                            if (player.isPlaying) player.pause() else player.play()
                                        } else {
                                            pendingTap?.cancel()
                                            pendingTapPosition = change.position
                                            pendingTapTime = change.uptimeMillis
                                            pendingTap = gestureScope.launch {
                                                delay(viewConfiguration.doubleTapTimeoutMillis)
                                                if (pendingTapPosition != null) {
                                                    pulse()
                                                    controls = !controls
                                                    pendingTapPosition = null
                                                }
                                            }
                                        }
                                    }
                                    break
                                }
                            }
                        } finally {
                            if (held) {
                                if (!completed) {
                                    player.setPlaybackSpeed(if (latestBoostLocked) 2f else latestSpeed)
                                    speedCue = ""
                                    lockCue = ""
                                }
                            }
                            if (!completed) hint = ""
                        }
                    }
                }
        )
        if (loading)
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
        if (error)
            Column(
                Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("播放失败", color = Color.White)
                LucentAction(
                    "重试",
                    {
                        error = false
                        player.prepare()
                        player.play()
                    },
                )
            }
        val hud = lockCue.ifEmpty { speedCue.ifEmpty { hint } }
        val levelHud = lockCue.isEmpty() && speedCue.isEmpty() && (hint.startsWith("亮度") || hint.startsWith("音量"))
        if (hud.isNotBlank()) LucentVideoHintOverlay(player, Modifier.fillMaxSize()) {
            Row(
                Modifier.shadow(12.dp, RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = .87f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (levelHud) {
                    val brightnessHint = hint.startsWith("亮度")
                    val value = hint.substringAfter(' ').removeSuffix("%").toFloatOrNull()?.div(100f) ?: 0f
                    Icon(
                        if (brightnessHint) Icons.Rounded.Brightness6 else Icons.Rounded.VolumeUp,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    LinearProgressIndicator(
                        progress = { value.coerceIn(0f, 1f) },
                        modifier = Modifier.width(96.dp).height(5.dp),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = .25f),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("${(value * 100).toInt()}%", color = Color.White, style = MaterialTheme.typography.labelLarge)
                } else {
                    Icon(Icons.Rounded.FastForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(hud, color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        if (showChrome)
            LucentVideoBottom(
                Modifier.align(Alignment.BottomCenter).onSizeChanged { onControlsHeight(it.height) },
                position,
                duration,
                player.bufferedPosition,
                playing,
                speed,
                fullscreen,
                quality = valid.takeIf { it.size > 1 }?.get(quality)?.label,
                onPlay = {
                    if (playing) player.pause()
                    else {
                        if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
                        player.play()
                    }
                },
                onSpeed = { menu = "speed" },
                onQuality = { menu = "quality" },
                onFullscreen = { setFull(!fullscreen) },
                onSeek = { target ->
                    player.seekTo(target)
                    position = target
                },
            )
        if (showChrome && fullscreen)
            LucentVideoTop(
                modifier = Modifier.align(Alignment.TopCenter),
                title = "Lucent · Mountain light",
                fullscreen = true,
                close = { setFull(false) },
            )
        if (menu.isNotBlank())
            LucentVideoChoices(
                if (menu == "quality") "清晰度选择" else "播放倍速",
                if (menu == "quality") valid.map { it.label }
                else standardVideoSpeeds.map { "${it}X" },
                if (menu == "quality") valid[quality].label else "${speed}X",
                dismiss = { menu = "" },
            ) { label ->
                if (menu == "quality") {
                    val at = player.currentPosition
                    val resume = player.playWhenReady
                    quality = valid.indexOfFirst { it.label == label }.coerceAtLeast(0)
                    player.setMediaItem(MediaItem.fromUri(valid[quality].uri), at)
                    player.prepare()
                    player.playWhenReady = resume
                } else {
                    speed = label.removeSuffix("X").toFloat()
                    player.setPlaybackSpeed(speed)
                    onSpeed(speed)
                }
            }
    }
}

private fun time(ms: Long) = "${ms/60000}:${(ms/1000%60).toString().padStart(2,'0')}"

@Composable
private fun LucentVideoHintOverlay(
    player: androidx.media3.common.Player,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    var ratio by remember(player) { mutableFloatStateOf(0f) }
    DisposableEffect(player) {
        fun update(size: VideoSize) {
            ratio = if (size.height > 0) size.width * size.pixelWidthHeightRatio / size.height else 0f
        }
        update(player.videoSize)
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) = update(videoSize)
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    BoxWithConstraints(modifier) {
        val topSpace = if (ratio > 0f) ((maxHeight - maxWidth / ratio).coerceAtLeast(0.dp) / 2) else 0.dp
        val slot = if (topSpace >= 144.dp) {
            Modifier.align(Alignment.TopCenter).height(topSpace).padding(top = 56.dp)
        } else Modifier.align(BiasAlignment(0f, -.5f))
        Box(slot, contentAlignment = Alignment.Center) { content() }
    }
}
