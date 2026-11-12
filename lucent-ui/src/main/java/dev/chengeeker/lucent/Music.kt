package dev.chengeeker.lucent

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.*
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

data class MusicTrack(
    val id: String,
    val title: String,
    val artist: String,
    val artwork: String,
    val uri: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LucentMusic(
    tracks: List<MusicTrack>,
    modifier: Modifier = Modifier,
    centerLyrics: Boolean = true,
    blurLyrics: Boolean = false,
    lyricSize: Float = 22f,
) {
    if (tracks.isEmpty()) {
        LucentEmpty("还没有音乐", "添加可播放曲目后显示播放器。")
        return
    }
    val context = LocalContext.current
    val life = LocalLifecycleOwner.current
    val style = LocalLucentStyle.current
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true,
            )
            setHandleAudioBecomingNoisy(true)
        }
    }
    var index by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf(false) }
    var position by remember { mutableLongStateOf(0) }
    var duration by remember { mutableLongStateOf(1) }
    var expanded by remember { mutableStateOf(false) }
    var queue by remember { mutableStateOf(false) }
    var mode by remember { mutableIntStateOf(0) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(tracks) {
        player.setMediaItems(
            tracks.map { MediaItem.Builder().setMediaId(it.id).setUri(it.uri).build() }
        )
        player.prepare()
    }
    DisposableEffect(player, life) {
        val listener =
            object : Player.Listener {
                override fun onIsPlayingChanged(value: Boolean) {
                    playing = value
                }

                override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                    index = player.currentMediaItemIndex.coerceIn(tracks.indices)
                }

                override fun onPlayerError(error: PlaybackException) {
                    failed = true
                }
            }
        player.addListener(listener)
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) player.pause()
        }
        life.lifecycle.addObserver(observer)
        onDispose {
            life.lifecycle.removeObserver(observer)
            player.removeListener(listener)
            player.release()
        }
    }
    LaunchedEffect(player) {
        while (true) {
            position = player.currentPosition.coerceAtLeast(0)
            duration = player.duration.coerceAtLeast(1)
            delay(if (playing) 150 else 500)
        }
    }
    val track = tracks[index]
    LucentCard(modifier.animateContentSize(if (style.motion) tween(300) else snap())) {
        Row(
            Modifier.fillMaxWidth().lucentClick { expanded = !expanded }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                track.artwork,
                null,
                Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
            )
            Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                Text(track.title, style = MaterialTheme.typography.titleMedium)
                Text(track.artist, style = MaterialTheme.typography.bodySmall)
            }
            LucentIcon(
                if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                if (playing) "暂停音乐" else "播放音乐",
            ) {
                if (playing) player.pause()
                else {
                    if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
                    player.play()
                }
            }
            LucentIcon(Icons.Rounded.QueueMusic, "播放队列") { queue = true }
        }
        if (expanded) {
            AsyncImage(
                track.artwork,
                track.title,
                Modifier.fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(28.dp)),
                contentScale = ContentScale.Crop,
            )
            var draft by remember { mutableStateOf<Float?>(null) }
            Slider(
                draft ?: position.toFloat(),
                { draft = it },
                Modifier.padding(horizontal = 20.dp),
                valueRange = 0f..duration.toFloat(),
                onValueChangeFinished = {
                    draft?.let { player.seekTo(it.toLong()) }
                    draft = null
                },
            )
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("${position/1000}s", style = MaterialTheme.typography.labelMedium)
                Text("${duration/1000}s", style = MaterialTheme.typography.labelMedium)
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LucentIcon(Icons.Rounded.SkipPrevious, "上一首") {
                    if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem()
                    else player.seekTo(0)
                }
                LucentAction(
                    if (playing) "暂停" else "播放",
                    { if (playing) player.pause() else player.play() },
                )
                LucentIcon(Icons.Rounded.SkipNext, "下一首") {
                    if (player.hasNextMediaItem()) player.seekToNextMediaItem()
                    else player.seekToDefaultPosition(0)
                }
            }
            LucentRow(
                "播放模式",
                listOf("顺序播放", "单曲循环", "随机播放")[mode],
                onClick = {
                    mode = (mode + 1) % 3
                    player.repeatMode =
                        if (mode == 1) Player.REPEAT_MODE_ONE
                        else if (mode == 2) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
                    player.shuffleModeEnabled = mode == 2
                },
            )
            val lines = listOf("光线越过山脊", "在安静的边界停留", "让每一次触碰", "都有清楚的回应")
            Column(
                Modifier.fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                lines.forEachIndexed { i, line ->
                    val active = (position / 3000).toInt().coerceIn(0, 3) == i
                    Text(
                        line,
                        Modifier.fillMaxWidth()
                            .then(if (blurLyrics && !active) Modifier.blur(1.dp) else Modifier),
                        color =
                            if (active) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = lyricSize.sp,
                        textAlign = if (centerLyrics) TextAlign.Center else TextAlign.Start,
                    )
                }
            }
        } else
            LinearProgressIndicator(
                progress = { position.toFloat() / duration },
                modifier = Modifier.fillMaxWidth(),
            )
        if (failed)
            LucentRow(
                "播放失败，点击重试",
                onClick = {
                    failed = false
                    player.prepare()
                    player.play()
                },
            )
    }
    if (queue)
        ModalBottomSheet(onDismissRequest = { queue = false }) {
            Column(Modifier.padding(16.dp)) {
                Text("播放队列", style = MaterialTheme.typography.titleLarge)
                tracks.forEachIndexed { i, t ->
                    LucentRow(
                        t.title,
                        t.artist,
                        onClick = {
                            player.seekToDefaultPosition(i)
                            player.play()
                            queue = false
                        },
                        trailing = { if (i == index) Icon(Icons.Rounded.Check, "当前曲目") },
                    )
                }
            }
        }
}
