package dev.chengeeker.lucent.demo

import android.content.Intent
import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import dev.chengeeker.lucent.*
import dev.chengeeker.lucent.demo.data.DemoModel
import java.io.File
import kotlinx.coroutines.*

val galleryItems =
    (0..3).map {
        GalleryItem(
            "file:///android_asset/media/landscape-$it.jpg",
            listOf("蓝色山谷", "暮色群山", "绿色山峦", "暖色远山")[it],
        )
    } +
        listOf(
            GalleryItem("file:///android_asset/media/long.jpg", "长图示例"),
            GalleryItem("file:///android_asset/media/motion.gif", "动态山峦"),
        )

fun videoSources(model: DemoModel): List<VideoSource> {
    val all =
        listOf(
            VideoSource("720p", "asset:///media/lucent-720.mp4"),
            VideoSource("480p", "asset:///media/lucent-480.mp4"),
        )
    return if (model.bool("dataSaver") || model.str("quality") == "480p") all.reversed() else all
}

@Composable
fun PatternPage(page: String, model: DemoModel, open: (String) -> Unit) {
    when (page) {
        "images" -> ImagesPage(model)
        "video" -> VideoPage(model)
        "article" -> ArticlePage(model)
        "music" -> MusicPage(model)
        "drama" -> DramaPage(model)
        "search" -> SearchPage(model, open)
        "devices" -> DevicesPage(model)
        "plugins" -> PluginsPage(model)
    }
}

@Composable
private fun ImagesPage(model: DemoModel) {
    var index by remember { mutableStateOf<Int?>(null) }
    var origin by remember { mutableStateOf<Rect?>(null) }
    val context = LocalContext.current
    var export by remember { mutableStateOf<GalleryItem?>(null) }
    val save =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/*")) { uri
            ->
            val item = export
            if (uri != null && item != null)
                model.run("保存图片") {
                    withContext(Dispatchers.IO) {
                        context.assets.open(item.uri.substringAfter("android_asset/")).use { input
                            ->
                            context.contentResolver.openOutputStream(uri)?.use { input.copyTo(it) }
                                ?: error("无法保存")
                        }
                    }
                    "图片已保存。"
                }
        }
    PageList(bottom = 24) {
        item {
            Text(
                "点击打开。双击或双指缩放，左右切换，原始倍率下上下拖动退出。",
                Modifier.padding(vertical = 20.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        itemsIndexed(galleryItems) { i, item ->
            var rect by remember { mutableStateOf<Rect?>(null) }
            LucentCard {
                AsyncImage(
                    item.uri,
                    item.description,
                    Modifier.fillMaxWidth()
                        .height(210.dp)
                        .onGloballyPositioned { rect = it.boundsInRoot() }
                        .lucentClick {
                            origin = rect
                            index = i
                        },
                    contentScale = ContentScale.Crop,
                )
                LucentRow(item.description)
            }
        }
    }
    index?.let { start ->
        Dialog(
            onDismissRequest = { index = null },
            properties =
                DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            LucentGallery(
                galleryItems,
                start,
                origin,
                onDismiss = { index = null },
                onSave = {
                    export = it
                    save.launch(it.uri.substringAfterLast('/'))
                },
                onShare = { item ->
                    model.run("准备分享") {
                        val file =
                            withContext(Dispatchers.IO) {
                                File(context.cacheDir, "shared")
                                    .apply { mkdirs() }
                                    .resolve(item.uri.substringAfterLast('/'))
                                    .also { f ->
                                        context.assets
                                            .open(item.uri.substringAfter("android_asset/"))
                                            .use { input ->
                                                f.outputStream().use { input.copyTo(it) }
                                            }
                                    }
                            }
                        val uri =
                            FileProvider.getUriForFile(
                                context,
                                context.packageName + ".files",
                                file,
                            )
                        context.startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND)
                                    .setType(
                                        if (file.extension == "gif") "image/gif" else "image/jpeg"
                                    )
                                    .putExtra(Intent.EXTRA_STREAM, uri)
                                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                                "分享图片",
                            )
                        )
                        "已交给系统分享面板。"
                    }
                },
            )
        }
    }
}

@Composable
private fun VideoPage(model: DemoModel) {
    var single by remember { mutableStateOf(false) }
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val sources = videoSources(model).let { if (single) it.take(1) else it }
    Column(
        Modifier.fillMaxSize()
            .then(
                if (landscape) Modifier
                else Modifier.statusBarsPadding().padding(top = 64.dp).navigationBarsPadding()
            )
    ) {
        // Keep the player at the same composition position across orientation changes.
        LucentVideo(
            sources,
            if (landscape) Modifier.fillMaxWidth().weight(1f)
            else Modifier.fillMaxWidth().aspectRatio(16f / 10),
            model.bool("autoplay"),
            model.str("speed").toFloat(),
            model.bool("loop"),
            { model.set("speed", it.toString()) },
        )
        if (!landscape)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
                LucentSection("播放控制") {
                    LucentToggle("仅提供一个有效画质", single, { single = it }, "开启后隐藏画质按钮，保留倍速。")
                    LucentRow("手势", "双击播放/暂停；横向拖动定位；左侧亮度、右侧音量；长按临时 2×。")
                    LucentRow("真实离线媒体", "两档独立编码的 H.264/AAC 示例，不需要网络。")
                    LucentRow("完整生命周期", "退出释放播放器；进入后台暂停；退出全屏还原方向和亮度。")
                }
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArticlePage(model: DemoModel) {
    var editor by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(model.str("draft")) }
    var comment by remember { mutableStateOf("") }
    var replies by remember { mutableStateOf(listOf<String>()) }
    var limit by remember { mutableIntStateOf(10) }
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val filter = model.str("filter").split(',', '，').map { it.trim() }.filter { it.isNotEmpty() }
    val bookmarked = model.str("bookmarks").contains("流动的边界")
    LaunchedEffect(Unit) { if (model.bool("history")) model.set("historyItems", "流动的边界") }
    LucentRefresh(
        refreshing,
        {
            scope.launch {
                refreshing = true
                delay(600)
                limit = 10
                refreshing = false
            }
        },
    ) {
        PageList(bottom = 24) {
            item {
                Column(Modifier.padding(vertical = 20.dp)) {
                    Text("流动的边界", style = MaterialTheme.typography.headlineLarge)
                    if (model.bool("avatar"))
                        LucentRow("Lucent Design", "演示作者", Icons.Rounded.AccountCircle)
                    if (model.bool("time"))
                        Text(
                            if (model.bool("relativeTime")) "今天 · 设计随笔" else "2026-11-12 · 设计随笔",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    AsyncImage(
                        galleryItems[0].uri,
                        "蓝色山谷",
                        Modifier.fillMaxWidth()
                            .padding(vertical = 16.dp)
                            .height(220.dp)
                            .clip(
                                RoundedCornerShape(if (model.bool("imageRound")) 24.dp else 0.dp)
                            ),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            items(5) { n ->
                Text(
                    "${n+1}. 清晰优先于装饰。让重要的内容保持清楚，用空间和容器区分信息层级。玻璃在滚动时表达前后关系；动画负责解释位置变化。设置、阅读和媒体播放遵循同一套规则。",
                    Modifier.padding(vertical = if (model.str("layout") == "紧凑") 4.dp else 12.dp),
                    style =
                        MaterialTheme.typography.bodyLarge.copy(
                            lineHeight =
                                androidx.compose.ui.unit.TextUnit(
                                    16f * model.str("bodySpacing").toFloat(),
                                    androidx.compose.ui.unit.TextUnitType.Sp,
                                )
                        ),
                )
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LucentAction(
                        if (bookmarked) "已收藏" else "收藏",
                        { model.set("bookmarks", if (bookmarked) "" else "流动的边界") },
                        primary = bookmarked,
                    )
                    LucentAction("写评论", { editor = true }, primary = false)
                    LucentAction(
                        if (refreshing) "刷新中" else "刷新",
                        {
                            scope.launch {
                                refreshing = true
                                delay(600)
                                limit = 10
                                refreshing = false
                            }
                        },
                        enabled = !refreshing,
                        primary = false,
                    )
                }
            }
            item {
                Text(
                    "评论",
                    Modifier.padding(top = 24.dp, bottom = 12.dp),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            items(
                (replies + List(limit) { "读者 ${it+1}：评论也应进入磨砂背景采样，滚到顶栏时不会露出清晰原字。" }).filter { t ->
                    filter.none { t.contains(it, true) }
                }
            ) { reply ->
                LucentCard {
                    LucentRow(
                        reply,
                        if (model.bool("preview")) "回复预览 · 点按打开编辑器" else null,
                        if (model.bool("avatar")) Icons.Rounded.AccountCircle else null,
                        onClick = { editor = true },
                    )
                }
            }
            item { LucentAction("加载更多", { limit += 10 }, Modifier.fillMaxWidth(), primary = false) }
        }
    }
    if (editor)
        ModalBottomSheet(
            onDismissRequest = {
                model.set("draft", draft)
                editor = false
            }
        ) {
            Column(Modifier.fillMaxWidth().imePadding().padding(16.dp)) {
                Text("发表评论", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    draft,
                    { draft = it.take(4000) },
                    Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    label = { Text("想说点什么") },
                    minLines = 3,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LucentAction(
                        "保存草稿",
                        {
                            model.set("draft", draft)
                            editor = false
                        },
                    )
                    LucentAction(
                        "发布到 Demo",
                        {
                            replies = listOf("我：$draft") + replies
                            draft = ""
                            model.set("draft", "")
                            editor = false
                        },
                        enabled = draft.isNotBlank(),
                        primary = false,
                    )
                }
            }
        }
}

@Composable
private fun MusicPage(model: DemoModel) {
    PageList(bottom = 24) {
        item {
            Text(
                "静谧之声",
                Modifier.padding(vertical = 20.dp),
                style = MaterialTheme.typography.headlineLarge,
            )
        }
        item { Text("原创合成音轨；四个队列条目共用离线音频。点击迷你播放器展开，歌词高亮跟随实际进度。", Modifier.padding(bottom = 16.dp)) }
        item {
            LucentMusic(
                (0..3).map {
                    MusicTrack(
                        "track-$it",
                        "山色 ${it+1}",
                        "Lucent Studio",
                        galleryItems[it].uri,
                        "asset:///media/lucent-audio.m4a",
                    )
                },
                centerLyrics = model.bool("lyricsCenter"),
                blurLyrics = model.bool("lyricsBlur"),
                lyricSize = model.str("lyricsSize").toFloat(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DramaPage(model: DemoModel) {
    var episode by remember { mutableIntStateOf(1) }
    var choose by remember { mutableStateOf(false) }
    var liked by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(top = 64.dp).navigationBarsPadding()
    ) {
        Box(Modifier.fillMaxWidth().weight(1f).background(Color.Black)) {
            key(episode) {
                LucentVideo(
                    videoSources(model),
                    Modifier.fillMaxSize(),
                    autoplay = false,
                    loop = model.bool("loop"),
                )
            }
            Column(Modifier.align(Alignment.TopStart).padding(16.dp)) {
                Text(
                    "山间来信 · 第 $episode 集",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "定制播放器模板 · 所有剧集使用同一离线片段",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            LucentAction(if (liked) "已喜欢" else "喜欢", { liked = !liked }, primary = liked)
            LucentAction("选集", { choose = true }, primary = false)
            LucentAction("下一集", { episode = episode % 12 + 1 }, primary = false)
        }
    }
    if (choose)
        ModalBottomSheet(onDismissRequest = { choose = false }) {
            Column(Modifier.padding(16.dp)) {
                Text("选择剧集", style = MaterialTheme.typography.titleLarge)
                repeat(3) { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        repeat(4) { col ->
                            val e = row * 4 + col + 1
                            LucentAction(
                                "$e",
                                {
                                    episode = e
                                    choose = false
                                },
                                primary = episode == e,
                            )
                        }
                    }
                }
            }
        }
}

@Composable
private fun SearchPage(model: DemoModel, open: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(false) }
    var unread by remember { mutableStateOf(true) }
    var chat by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    var lines by remember { mutableStateOf(listOf("欢迎使用 Lucent UI。这是本地会话示例。")) }
    PageList(bottom = 24) {
        item {
            OutlinedTextField(
                query,
                { query = it },
                Modifier.fillMaxWidth().padding(vertical = 16.dp),
                label = { Text("搜索页面或设置") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                singleLine = true,
            )
        }
        if (query.isNotBlank())
            items(
                (settingRoutes + patternRoutes).filter {
                    it.title.contains(query, true) || it.detail.contains(query, true)
                }
            ) { r ->
                LucentCard { LucentRow(r.title, r.detail, onClick = { open(r.id) }) }
            }
        else {
            item {
                LucentSection("快速入口") {
                    listOf("玻璃", "WebDAV", "字号").forEach { word ->
                        LucentRow(word, onClick = { query = word })
                    }
                }
            }
            item {
                LucentSection("消息示例") {
                    LucentRow(
                        "设计助手",
                        if (unread) "1 条未读 · 点击查看" else "已读",
                        Icons.Rounded.ChatBubble,
                        onClick = {
                            chat = true
                            unread = false
                        },
                    )
                    if (chat) {
                        lines.forEach { Text(it, Modifier.padding(16.dp)) }
                        OutlinedTextField(
                            draft,
                            { draft = it },
                            Modifier.fillMaxWidth().padding(16.dp),
                            label = { Text("消息内容") },
                        )
                        LucentAction(
                            "发送到本地示例",
                            {
                                lines = lines + "我：$draft"
                                draft = ""
                            },
                            Modifier.padding(16.dp),
                            enabled = draft.isNotBlank(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DevicesPage(model: DemoModel) {
    var connected by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var job by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    PageList(bottom = 24) {
        item { Text("设备与传输为本地模拟，用于验证界面状态和接入契约。不会扫描网络或操作其他设备。", Modifier.padding(vertical = 16.dp)) }
        item {
            LucentSection("我的设备") {
                LucentRow(
                    "Studio Desktop",
                    if (connected) "已连接 · 演示状态" else "等待连接",
                    Icons.Rounded.Computer,
                    onClick = { connected = !connected },
                )
                LucentRow("当前手机", "本机界面示例", Icons.Rounded.Smartphone)
            }
        }
        item {
            LucentSection("传输任务") {
                LucentRow(
                    "设计样片.mp4",
                    if (progress >= 1f) "传输完成（模拟）" else "${(progress*100).toInt()}%",
                    Icons.Rounded.InsertDriveFile,
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                )
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LucentAction(
                        if (job?.isActive == true) "传输中" else "开始示例",
                        {
                            job =
                                scope.launch {
                                    progress = 0f
                                    repeat(100) {
                                        delay(45)
                                        progress = (it + 1) / 100f
                                    }
                                }
                        },
                        enabled = connected && job?.isActive != true,
                    )
                    LucentAction(
                        "取消",
                        {
                            job?.cancel()
                            model.report("传输已取消", "演示任务已停止，没有实际发送文件。")
                        },
                        enabled = job?.isActive == true,
                        primary = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun PluginsPage(model: DemoModel) {
    var input by remember { mutableStateOf("") }
    var configuration by remember { mutableStateOf(false) }
    var nine by remember { mutableStateOf(false) }
    PageList(bottom = 24) {
        item {
            Text("插件和键盘是独立产品模板。这里展示交互与配置，不注册系统输入法，也不执行第三方插件代码。", Modifier.padding(vertical = 16.dp))
        }
        item {
            LucentSection("插件管理") {
                LucentToggle(
                    "示例词库",
                    model.bool("plugin"),
                    { model.set("plugin", it) },
                    "启用后在下方键盘展示候选词。",
                )
                LucentRow("插件配置", onClick = { configuration = !configuration })
                if (configuration) {
                    LucentRow("当前版本", "1.0.0 · 本地示例")
                    LucentAction(
                        "检查示例更新",
                        { model.report("已是最新版本", "这是本地状态示例；接入方需实现更新源校验。") },
                        Modifier.padding(16.dp),
                    )
                }
            }
        }
        item {
            LucentSection("键盘预览") {
                LucentToggle("九宫格布局", nine, { nine = it })
                OutlinedTextField(
                    input,
                    { input = it },
                    Modifier.fillMaxWidth().padding(16.dp),
                    label = { Text("预览输入") },
                )
                if (model.bool("plugin"))
                    Row(
                        Modifier.fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(8.dp)
                    ) {
                        listOf("清晰", "流动", "玻璃", "界面").forEach { word ->
                            LucentAction(word, { input += word }, primary = false)
                        }
                    }
                val rows =
                    if (nine) listOf("123", "456", "789")
                    else listOf("QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM")
                rows.forEach { line ->
                    Row(
                        Modifier.fillMaxWidth().padding(3.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        line.forEach { char ->
                            Box(
                                Modifier.weight(1f)
                                    .heightIn(min = 48.dp)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .lucentClick { input += char },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(char.toString())
                            }
                        }
                    }
                }
                Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LucentAction("空格", { input += " " }, Modifier.weight(1f), primary = false)
                    LucentIcon(Icons.Rounded.Backspace, "退格") { input = input.dropLast(1) }
                }
            }
        }
    }
}
