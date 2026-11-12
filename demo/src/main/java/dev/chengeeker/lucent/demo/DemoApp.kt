package dev.chengeeker.lucent.demo

import android.content.res.Configuration
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.chengeeker.lucent.*
import dev.chengeeker.lucent.demo.data.DemoModel

data class DemoRoute(val id: String, val title: String, val detail: String)

val settingRoutes =
    listOf(
        DemoRoute("appearance", "个性化", "主题、动态取色、字号字重、玻璃与动效"),
        DemoRoute("reading", "阅读与内容", "卡片布局、回复预览、过滤与浏览记录"),
        DemoRoute("media", "图片与视频", "播放偏好、画质、倍速与流量控制"),
        DemoRoute("storage", "存储与下载", "保存位置、下载任务与缓存清理"),
        DemoRoute("webdav", "WebDAV 备份", "连接测试、安全备份与加密全量恢复"),
        DemoRoute("local", "本地备份与迁移", "导入、导出、校验与恢复确认"),
        DemoRoute("notifications", "通知与后台", "系统授权与通知预览"),
        DemoRoute("network", "网络", "超时、HTTPS 连接诊断"),
        DemoRoute("privacy", "隐私与数据", "截图保护、历史记录与重置"),
        DemoRoute("account", "账号管理", "登录、凭据迁移与退出的演示流程"),
        DemoRoute("about", "关于与规范", "设计来源、组件契约与许可"),
    )
val patternRoutes =
    listOf(
        DemoRoute("article", "阅读与评论", "长内容、磨砂顶栏、回复、收藏与编辑"),
        DemoRoute("images", "图片浏览器", "共享位置过渡、缩放、长图与动图"),
        DemoRoute("video", "普通视频", "真实本地播放、画质、倍速和手势"),
        DemoRoute("music", "音乐与歌词", "队列、播放模式、迷你播放器与展开"),
        DemoRoute("drama", "剧集播放器", "定制模式：竖屏视频与选集"),
        DemoRoute("search", "搜索与消息", "搜索建议、结果、未读与会话"),
        DemoRoute("devices", "设备与传输", "设备卡片、连接、任务进度与取消"),
        DemoRoute("plugins", "插件与键盘", "启停、配置、导入流程与键盘预览"),
    )

@Composable
fun DemoApp(model: DemoModel) {
    var stack by rememberSaveable { mutableStateOf(listOf("explore")) }
    val route = stack.last()
    fun open(id: String) {
        if (route != id) stack = stack + id
    }
    fun back() {
        if (stack.size > 1) stack = stack.dropLast(1)
    }
    BackHandler(stack.size > 1) { back() }
    val stateHolder = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    val root = stack.size == 1
    val context = LocalContext.current
    val activity = context.activity()
    val style = LocalLucentStyle.current
    val lightBars = MaterialTheme.colorScheme.background.luminance() > .5f
    SideEffect {
        activity?.window?.let { w ->
            androidx.core.view.WindowCompat.getInsetsController(w, w.decorView).apply {
                isAppearanceLightStatusBars = lightBars
                isAppearanceLightNavigationBars = lightBars
            }
        }
    }
    DisposableEffect(model.bool("private")) {
        if (model.bool("private"))
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        else activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {}
    }
    LaunchedEffect(model.str("refresh")) {
        activity?.window?.let { w ->
            w.attributes =
                w.attributes.apply {
                    preferredRefreshRate =
                        model.str("refresh").filter { it.isDigit() }.toFloatOrNull() ?: 0f
                }
        }
    }
    val titles =
        (settingRoutes + patternRoutes).associate { it.id to it.title } +
            mapOf(
                "explore" to "Lucent UI",
                "components" to "组件与交互",
                "settings" to "设置",
                "glass" to "玻璃实验室",
            )
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    BackdropHost(
        overlay = {
            if (!(route == "video" && landscape))
                LucentHeader(titles[route] ?: route, if (root) null else ::back) {
                    if (root) LucentIcon(Icons.Rounded.Search, "搜索规范") { open("search") }
                }
            if (root)
                Box(
                    Modifier.align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(
                            horizontal = if (style.floating) 24.dp else 0.dp,
                            vertical = if (style.floating) 12.dp else 0.dp,
                        )
                        .widthIn(max = 600.dp)
                ) {
                    LucentNavigation(
                        listOf(
                            LucentDestination("探索", Icons.Rounded.AutoAwesome),
                            LucentDestination("组件", Icons.Rounded.Widgets),
                            LucentDestination("设置", Icons.Rounded.Tune),
                        ),
                        listOf("explore", "components", "settings").indexOf(route).coerceAtLeast(0),
                        { stack = listOf(listOf("explore", "components", "settings")[it]) },
                    )
                }
        }
    ) {
        AnimatedContent(
            route,
            transitionSpec = {
                if (style.motion)
                    (fadeIn(tween(220)) + slideInHorizontally(tween(300)) { it / 12 }) togetherWith
                        fadeOut(tween(120))
                else fadeIn(tween(0)) togetherWith fadeOut(tween(0))
            },
            label = "page",
        ) { page ->
            stateHolder.SaveableStateProvider(page) {
                when (page) {
                    "explore" -> Explore(::open)
                    "settings" ->
                        PageList {
                            item {
                                LucentSection("偏好与功能") {
                                    settingRoutes.take(3).forEach { r ->
                                        LucentRow(
                                            r.title,
                                            r.detail,
                                            Icons.Rounded.Tune,
                                            onClick = { open(r.id) },
                                        )
                                    }
                                }
                            }
                            item {
                                LucentSection("存储与备份") {
                                    settingRoutes.slice(3..5).forEach { r ->
                                        LucentRow(
                                            r.title,
                                            r.detail,
                                            Icons.Rounded.CloudQueue,
                                            onClick = { open(r.id) },
                                        )
                                    }
                                }
                            }
                            item {
                                LucentSection("系统与应用") {
                                    settingRoutes.drop(6).forEach { r ->
                                        LucentRow(
                                            r.title,
                                            r.detail,
                                            Icons.Rounded.Settings,
                                            onClick = { open(r.id) },
                                        )
                                    }
                                }
                            }
                        }
                    "components" -> ComponentsPage(model, ::open)
                    "glass" -> GlassLab(model)
                    "appearance",
                    "reading",
                    "media",
                    "storage",
                    "webdav",
                    "local",
                    "notifications",
                    "network",
                    "privacy",
                    "account",
                    "about" -> SettingsPage(page, model, ::open)
                    else -> PatternPage(page, model, ::open)
                }
            }
        }
    }
    model.busy?.let { title ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text(title) },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                    Text("正在处理，请稍候…")
                }
            },
            confirmButton = { LucentAction("取消", model::cancel, primary = false) },
        )
    }
    model.result?.let { (title, body) -> LucentResult(title, body, model::dismiss) }
}

@Composable
fun PageList(bottom: Int = 108, content: LazyListScope.() -> Unit) {
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 64.dp
    LazyColumn(
        Modifier.fillMaxSize().imePadding(),
        contentPadding =
            PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = top,
                bottom =
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                        bottom.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
private fun Explore(open: (String) -> Unit) {
    PageList {
        item {
            Column(Modifier.padding(top = 24.dp, bottom = 16.dp)) {
                Text(
                    "清晰的内容，\n流动的界面。",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Material 3 · Glass & Motion",
                    Modifier.padding(top = 12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
        item {
            Box(
                Modifier.fillMaxWidth()
                    .height(210.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(28.dp))
                    .lucentClick { open("glass") }
            ) {
                AsyncImage(
                    "file:///android_asset/media/landscape-0.jpg",
                    null,
                    Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                Box(
                    Modifier.fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = .65f))
                            )
                        )
                )
                Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                    Text(
                        "让玻璃拥有背景",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text("折射 · 磨砂 · 长按与拖动", color = Color.White.copy(alpha = .8f))
                    Spacer(Modifier.height(8.dp))
                    LucentAction("打开实验室", { open("glass") }, primary = false)
                }
            }
        }
        item {
            LucentSection("可交互页面") {
                patternRoutes.forEach { r ->
                    LucentRow(r.title, r.detail, Icons.Rounded.Layers, onClick = { open(r.id) })
                }
            }
        }
        item {
            LucentSection("同一套规则，覆盖每一个设置页") {
                LucentRow(
                    "功能界面",
                    "所有操作只作用于 Lucent Demo 自身的数据。",
                    Icons.Rounded.Tune,
                    onClick = { open("settings") },
                )
                LucentRow(
                    "设计规范",
                    "基础变量、状态、交互契约与接入示例",
                    Icons.Rounded.Description,
                    onClick = { open("about") },
                )
            }
        }
    }
}
