package dev.chengeeker.lucent.demo

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.chengeeker.lucent.*
import dev.chengeeker.lucent.demo.data.*
import java.io.File
import kotlinx.coroutines.*
import okhttp3.HttpUrl.Companion.toHttpUrl

@Composable
private fun Toggle(model: DemoModel, key: String, title: String, subtitle: String? = null) =
    LucentToggle(title, model.bool(key), { model.set(key, it) }, subtitle)

@Composable
private fun Choice(model: DemoModel, key: String, title: String, options: List<String>) {
    var show by remember { mutableStateOf(false) }
    LucentRow(title, model.str(key), onClick = { show = true })
    if (show)
        AlertDialog(
            onDismissRequest = { show = false },
            title = { Text(title) },
            text = {
                Column {
                    options.forEach { v ->
                        LucentRow(
                            v,
                            onClick = {
                                model.set(key, v)
                                show = false
                            },
                            trailing = { RadioButton(model.str(key) == v, null) },
                        )
                    }
                }
            },
            confirmButton = { LucentAction("取消", { show = false }, primary = false) },
        )
}

@Composable
fun SettingsPage(page: String, model: DemoModel, open: (String) -> Unit) {
    when (page) {
        "appearance" -> Appearance(model)
        "webdav",
        "local" -> BackupPage(page == "webdav", model)
        "storage" -> StoragePage(model)
        "notifications" -> NotificationPage(model)
        "network" -> NetworkPage(model)
        "account" -> AccountPage(model)
        "about" -> AboutPage()
        else ->
            PageList(bottom = 24) {
                when (page) {
                    "reading" -> {
                        item {
                            LucentSection("内容布局") {
                                Choice(model, "layout", "卡片密度", listOf("舒适", "紧凑"))
                                Toggle(model, "avatar", "显示头像")
                                Toggle(model, "time", "显示时间")
                                Toggle(model, "preview", "显示回复预览")
                                Choice(model, "bodySpacing", "正文行距", listOf("1.3", "1.5", "1.8"))
                                Toggle(model, "imageRound", "图片圆角")
                                Toggle(model, "relativeTime", "相对时间")
                            }
                        }
                        item {
                            LucentSection("内容管理") {
                                var filter by
                                    remember(model.str("filter")) {
                                        mutableStateOf(model.str("filter"))
                                    }
                                OutlinedTextField(
                                    filter,
                                    { filter = it },
                                    label = { Text("屏蔽词（逗号分隔）") },
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                )
                                LucentAction(
                                    "保存过滤规则",
                                    {
                                        model.set("filter", filter)
                                        model.report("已保存", "阅读与评论示例将应用这些过滤词。")
                                    },
                                    Modifier.padding(16.dp),
                                )
                                LucentRow("预览阅读页", onClick = { open("article") })
                                LucentRow("收藏", model.str("bookmarks").ifBlank { "暂无收藏" })
                                LucentRow("浏览记录", model.str("historyItems").ifBlank { "暂无记录" })
                            }
                        }
                    }
                    "media" -> {
                        item {
                            LucentSection("播放偏好") {
                                Toggle(model, "autoplay", "自动播放")
                                Toggle(
                                    model,
                                    "dataSaver",
                                    "节省流量",
                                    "示例播放器默认选择 480p；真实网络播放由接入方执行网络策略。",
                                )
                                Choice(model, "quality", "默认画质", listOf("自动", "720p", "480p"))
                                Choice(
                                    model,
                                    "speed",
                                    "默认倍速",
                                    listOf("0.5", "0.75", "1.0", "1.25", "1.5", "2.0", "3.0"),
                                )
                                Toggle(model, "loop", "循环播放")
                                Toggle(model, "lyricsCenter", "歌词居中")
                                Toggle(model, "lyricsBlur", "非当前歌词模糊")
                                Choice(
                                    model,
                                    "lyricsSize",
                                    "歌词字号（sp）",
                                    listOf("18", "22", "26", "30"),
                                )
                            }
                        }
                        item {
                            LucentSection("体验预览") {
                                LucentRow("图片浏览器", onClick = { open("images") })
                                LucentRow("普通视频播放器", onClick = { open("video") })
                                LucentRow("专用剧集播放器", "保留独立交互规范", onClick = { open("drama") })
                            }
                        }
                    }
                    "privacy" -> {
                        item {
                            LucentSection("隐私") {
                                Toggle(model, "private", "保护屏幕内容", "阻止系统截图，并隐藏最近任务缩略图。")
                                Toggle(model, "history", "保存 Demo 浏览记录")
                                LucentRow(
                                    "清空浏览记录",
                                    onClick = {
                                        model.set("historyItems", "")
                                        model.report("已清空", "只清除了 Demo 的浏览记录。")
                                    },
                                )
                            }
                        }
                        item {
                            var confirm by remember { mutableStateOf(false) }
                            LucentSection("重置") {
                                LucentRow(
                                    "重置 Demo 数据",
                                    "清除设置、草稿、收藏和本机连接凭据；不删除云端文件。",
                                    onClick = { confirm = true },
                                )
                            }
                            if (confirm)
                                Confirm("重置 Demo？", "此操作仅影响本应用。建议先导出备份。", { confirm = false }) {
                                    confirm = false
                                    model.reset()
                                }
                        }
                    }
                }
            }
    }
}

@Composable
private fun Appearance(model: DemoModel) {
    PageList(bottom = 24) {
        item {
            Spacer(Modifier.height(24.dp))
            LucentAppearanceModes(
                listOf("system", "light", "dark").indexOf(model.str("mode")),
                { model.set("mode", listOf("system", "light", "dark")[it]) },
                model.bool("black"),
                { model.set("black", it) },
            )
        }
        item {
            Spacer(Modifier.height(16.dp))
            LucentAppearancePalette(
                model.bool("dynamic"),
                { model.set("dynamic", it) },
                model.number("seed").toInt(),
                { model.set("seed", it) },
            )
        }
        item {
            Spacer(Modifier.height(16.dp))
            LucentFontSizeSetting(model.number("font").toFloat()) {
                model.set("font", it.toDouble())
            }
        }
        item {
            Spacer(Modifier.height(16.dp))
            LucentFontWeightSetting(
                model.bool("weightCustom"),
                model.number("weight").toInt() * 100,
                "Lucent UI",
                hapticsEnabled = model.bool("haptics"),
                onReset = {
                    model.set("weight", 0)
                    model.set("weightCustom", false)
                },
                onCommit = {
                    model.set("weight", it / 100)
                    model.set("weightCustom", true)
                },
            )
        }
        item {
            LucentSection("底部导航") {
                LucentAppearanceToggle(
                    "使用悬浮底栏",
                    "dock",
                    model.bool("floating"),
                    change = { model.set("floating", it) },
                )
                LucentAppearanceToggle(
                    "使用液态玻璃效果",
                    "blur_on",
                    model.bool("glass"),
                    change = { model.set("glass", it) },
                )
                LucentGlassChoice(model.bool("translucent")) { model.set("translucent", it) }
            }
        }
        item {
            LucentSection("交互与显示") {
                Toggle(model, "motion", "交互动画", "关闭后取消位移与弹性动画；同时尊重系统动画设置。")
                LucentAppearanceToggle(
                    "触感与震动反馈",
                    "vibration",
                    model.bool("haptics"),
                    change = { model.set("haptics", it) },
                )
                Choice(model, "refresh", "屏幕帧率设置", listOf("系统默认", "60 Hz", "90 Hz", "120 Hz"))
            }
        }
    }
}

@Composable
fun Confirm(title: String, body: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { LucentAction("确认", onConfirm) },
        dismissButton = { LucentAction("取消", onDismiss, primary = false) },
    )
}

@Composable
private fun BackupPage(webdav: Boolean, model: DemoModel) {
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    var url by remember(model.str("webdavUrl")) { mutableStateOf(model.str("webdavUrl")) }
    var user by remember(model.str("webdavUser")) { mutableStateOf(model.str("webdavUser")) }
    val savedCredential = remember { runCatching { model.savedPassword() } }
    var authPassword by remember { mutableStateOf(savedCredential.getOrDefault("")) }
    LaunchedEffect(savedCredential) {
        if (savedCredential.isFailure) model.report("连接密码无法读取", "原有数据已保留，请重新输入授权密码后保存连接。")
    }
    var full by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var pendingBytes by remember { mutableStateOf<ByteArray?>(null) }
    val exporter =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/json")
        ) { uri ->
            if (uri != null)
                model.run("导出备份") {
                    val bytes = model.backup(full, password)
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes) }
                            ?: error("无法写入所选文件")
                    }
                    "备份已写入所选文件。"
                }
        }
    val importer =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null)
                model.run("检查备份") {
                    val bytes =
                        withContext(Dispatchers.IO) {
                            context.contentResolver.openInputStream(uri)?.use {
                                it.readBytesLimited(BackupCodec.LIMIT)
                            } ?: error("无法读取备份")
                        }
                    withContext(Dispatchers.Default) {
                        BackupCodec.decode(bytes, password.takeIf { it.isNotBlank() })
                    }
                    pendingBytes = bytes
                    "格式和密码校验通过。关闭此提示后请确认覆盖当前 Demo 数据。"
                }
        }
    LaunchedEffect(pendingBytes, model.result) {
        if (pendingBytes != null && model.result == null) confirmation = "import"
    }
    PageList(bottom = 24) {
        if (webdav)
            item {
                LucentSection("连接信息") {
                    OutlinedTextField(
                        url,
                        { url = it },
                        Modifier.fillMaxWidth().padding(16.dp),
                        label = { Text("服务器目录 URL") },
                        keyboardOptions =
                            androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Uri,
                                autoCorrectEnabled = false,
                            ),
                        placeholder = { Text("https://example.com/dav/") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        user,
                        { user = it },
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        label = { Text("账号") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        authPassword,
                        { authPassword = it },
                        Modifier.fillMaxWidth().padding(16.dp),
                        label = { Text("授权密码") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions =
                            androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
                                autoCorrectEnabled = false,
                            ),
                    )
                    LucentAction(
                        "保存连接",
                        {
                            keyboard?.hide()
                            model.saveWebDav(url, user, authPassword)
                        },
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        enabled =
                            model.busy == null &&
                                (savedCredential.isSuccess || authPassword.isNotEmpty()),
                    )
                    LucentRow(
                        "测试已保存的连接",
                        onClick = {
                            keyboard?.hide()
                            model.testDav()
                        },
                        enabled = model.str("webdavUrl").isNotBlank(),
                    )
                }
            }
        item {
            LucentSection("备份范围") {
                LucentRow(
                    "安全备份",
                    "设置、草稿和收藏；排除账号及连接凭据。",
                    onClick = { full = false },
                    trailing = { RadioButton(!full, null) },
                )
                LucentRow(
                    "加密全量备份",
                    "另含演示账号与 WebDAV 连接凭据。使用独立密码加密，可换机恢复。",
                    onClick = { full = true },
                    trailing = { RadioButton(full, null) },
                )
                OutlinedTextField(
                    password,
                    { password = it },
                    Modifier.fillMaxWidth().padding(16.dp),
                    label = { Text(if (full) "备份密码（至少 8 字符）" else "恢复加密备份时填写密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions =
                        androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
                            autoCorrectEnabled = false,
                        ),
                    singleLine = true,
                )
                Text(
                    "忘记备份密码无法恢复全量文件。不会备份缓存、安装包或旧设备的存储授权。",
                    Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        item {
            LucentSection(if (webdav) "云端操作" else "文件操作") {
                LucentRow(
                    if (webdav) "上传到云端" else "导出到文件",
                    if (webdav) "将覆盖同目录的 lucent-ui-backup.json" else "使用系统文件选择器保存。",
                    enabled =
                        (!full || password.length >= 8) &&
                            (!webdav || model.str("webdavUrl").isNotBlank()),
                    onClick = {
                        keyboard?.hide()
                        confirmation = "export"
                    },
                )
                LucentRow(
                    if (webdav) "从云端恢复" else "导入备份文件",
                    "校验通过后替换当前 Demo 数据。",
                    enabled = !webdav || model.str("webdavUrl").isNotBlank(),
                    onClick = {
                        keyboard?.hide()
                        if (webdav) confirmation = "restore"
                        else
                            importer.launch(
                                arrayOf(
                                    "application/json",
                                    "text/plain",
                                    "application/octet-stream",
                                )
                            )
                    },
                )
            }
        }
    }
    if (confirmation.isNotBlank())
        Confirm(
            if (confirmation == "export") "确认备份" else "确认恢复",
            if (confirmation == "export")
                if (full) "全量文件包含连接凭据，将使用独立密码加密。请妥善保管文件和密码。" else "仅保存非敏感 Demo 数据。已有同名云端文件会被替换。"
            else "恢复会覆盖当前 Demo 设置、草稿和收藏。建议先备份当前数据。",
            {
                confirmation = ""
                pendingBytes = null
            },
        ) {
            val action = confirmation
            confirmation = ""
            when (action) {
                "export" ->
                    if (webdav) model.upload(full, password)
                    else exporter.launch("LucentUI-backup.json")
                "restore" -> model.download(password)
                "import" -> {
                    val bytes = pendingBytes
                    pendingBytes = null
                    if (bytes != null) model.run("恢复本地备份") { model.restore(bytes, password) }
                }
            }
        }
}

internal fun java.io.InputStream.readBytesLimited(max: Int): ByteArray {
    val out = java.io.ByteArrayOutputStream()
    val block = ByteArray(8192)
    while (true) {
        val n = read(block)
        if (n < 0) break
        require(out.size() + n <= max)
        out.write(block, 0, n)
    }
    return out.toByteArray()
}

@Composable
private fun StoragePage(model: DemoModel) {
    val context = LocalContext.current
    var bytes by remember {
        mutableLongStateOf(
            context.cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        )
    }
    var progress by remember { mutableFloatStateOf(0f) }
    var clear by remember { mutableStateOf(false) }
    val choose =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null)
                runCatching {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                        )
                        model.set("downloadTree", uri.toString())
                    }
                    .onFailure { model.report("选择失败", "未获得目录写入权限，请重新选择。") }
        }
    PageList(bottom = 24) {
        item {
            LucentSection("存储位置") {
                LucentRow(
                    "图片与视频目录",
                    if (model.str("downloadTree").isBlank()) "应用专属目录" else "已选择系统授权目录",
                    onClick = { choose.launch(null) },
                )
                LucentRow("恢复默认目录", onClick = { model.set("downloadTree", "") })
                Toggle(model, "downloadWifi", "仅 Wi-Fi 下载", "提供网络下载接入策略；本页的离线样片保存不使用网络。")
                Choice(model, "downloadRate", "保存限速（KiB/s；0 不限）", listOf("0", "128", "512", "2048"))
                Choice(model, "downloadNaming", "文件命名", listOf("时间戳", "示例标题"))
            }
        }
        item {
            LucentSection("任务反馈") {
                LucentRow(
                    "保存离线样片",
                    "实际复制内置视频到所选目录，支持取消。",
                    onClick = {
                        model.run("保存视频") {
                            progress = 0f
                            withContext(Dispatchers.IO) {
                                val tree =
                                    model
                                        .str("downloadTree")
                                        .takeIf { it.isNotBlank() }
                                        ?.let(Uri::parse)
                                var uri: Uri? = null
                                var file: File? = null
                                val fileName =
                                    (if (model.str("downloadNaming") == "示例标题") "Lucent-Mountain-"
                                    else "Lucent-") + System.currentTimeMillis() + ".mp4"
                                try {
                                    val output =
                                        if (tree != null) {
                                            val parent =
                                                DocumentsContract.buildDocumentUriUsingTree(
                                                    tree,
                                                    DocumentsContract.getTreeDocumentId(tree),
                                                )
                                            uri =
                                                DocumentsContract.createDocument(
                                                    context.contentResolver,
                                                    parent,
                                                    "video/mp4",
                                                    fileName,
                                                ) ?: error("无法创建文件")
                                            context.contentResolver.openOutputStream(uri!!, "w")
                                                ?: error("无法写入文件")
                                        } else {
                                            file = File(context.getExternalFilesDir(null), fileName)
                                            file!!.outputStream()
                                        }
                                    context.assets.open("media/lucent-720.mp4").use { input ->
                                        val total = input.available().coerceAtLeast(1)
                                        var copied = 0
                                        output.use { stream ->
                                            val block = ByteArray(8192)
                                            while (true) {
                                                ensureActive()
                                                val n = input.read(block)
                                                if (n < 0) break
                                                stream.write(block, 0, n)
                                                copied += n
                                                withContext(Dispatchers.Main) {
                                                    progress =
                                                        (copied.toFloat() / total).coerceIn(0f, 1f)
                                                }
                                                val rate = model.str("downloadRate").toInt()
                                                if (rate > 0)
                                                    delay(
                                                        (n * 1000L / (rate * 1024)).coerceAtLeast(1)
                                                    )
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    file?.delete()
                                    uri?.let {
                                        runCatching {
                                            DocumentsContract.deleteDocument(
                                                context.contentResolver,
                                                it,
                                            )
                                        }
                                    }
                                    throw e
                                }
                            }
                            "视频已保存到指定目录。"
                        }
                    },
                )
                if (progress > 0f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    )
            }
        }
        item {
            LucentSection("缓存管理") {
                LucentRow("临时缓存", "${bytes/1024} KiB")
                Choice(
                    model,
                    "cacheLimit",
                    "启动时自动清理阈值",
                    listOf("关闭", "16 MiB", "64 MiB", "256 MiB"),
                )
                LucentRow("清理缓存", "只清除 Demo 的缓存，不清除草稿、设置或导出文件。", onClick = { clear = true })
            }
        }
    }
    if (clear)
        Confirm("清理临时缓存？", "下载目录和备份文件会保留。", { clear = false }) {
            clear = false
            model.run("清理缓存") {
                withContext(Dispatchers.IO) {
                    context.cacheDir.listFiles()?.forEach {
                        check(it.deleteRecursively()) { "缓存文件正在使用，请稍后重试" }
                    }
                }
                bytes = 0
                "已清理 Demo 临时缓存。"
            }
        }
}

@Composable
private fun NotificationPage(model: DemoModel) {
    val context = LocalContext.current
    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            model.set("notify", granted)
            model.report(
                if (granted) "已授权" else "未授权",
                if (granted) "可以发送本地演示通知。" else "可稍后在系统设置中开启。",
            )
        }
    PageList(bottom = 24) {
        item {
            LucentSection("通知权限") {
                LucentToggle(
                    "启用通知",
                    model.bool("notify"),
                    { value ->
                        if (
                            value &&
                                Build.VERSION.SDK_INT >= 33 &&
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS,
                                ) != PackageManager.PERMISSION_GRANTED
                        )
                            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else model.set("notify", value)
                    },
                )
                LucentRow(
                    "系统通知设置",
                    onClick = {
                        context.startActivity(
                            if (Build.VERSION.SDK_INT >= 26)
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            else
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse("package:${context.packageName}"),
                                )
                        )
                    },
                )
            }
        }
        item {
            LucentSection("本地示例") {
                LucentRow(
                    "发送预览通知",
                    "只发送到本机，不连接任何消息平台。",
                    onClick = {
                        if (
                            !model.bool("notify") ||
                                !NotificationManagerCompat.from(context).areNotificationsEnabled()
                        )
                            model.report("通知未开启", "请先授权并启用通知。")
                        else
                            runCatching {
                                    val manager =
                                        context.getSystemService(Context.NOTIFICATION_SERVICE)
                                            as NotificationManager
                                    if (Build.VERSION.SDK_INT >= 26)
                                        manager.createNotificationChannel(
                                            NotificationChannel(
                                                "demo",
                                                "演示通知",
                                                NotificationManager.IMPORTANCE_DEFAULT,
                                            )
                                        )
                                    val pending =
                                        PendingIntent.getActivity(
                                            context,
                                            1,
                                            Intent(context, MainActivity::class.java),
                                            PendingIntent.FLAG_IMMUTABLE or
                                                PendingIntent.FLAG_UPDATE_CURRENT,
                                        )
                                    manager.notify(
                                        1,
                                        NotificationCompat.Builder(context, "demo")
                                            .setSmallIcon(
                                                dev.chengeeker.lucent.demo.R.drawable.ic_launcher
                                            )
                                            .setContentTitle("Lucent UI")
                                            .setContentText("备份已完成 · 这是本地通知示例")
                                            .setAutoCancel(true)
                                            .setContentIntent(pending)
                                            .build(),
                                    )
                                    model.report("已发送", "可在通知栏查看预览。")
                                }
                                .onFailure { model.report("未发送", "请检查系统通知权限。") }
                    },
                )
            }
        }
    }
}

@Composable
private fun NetworkPage(model: DemoModel) {
    var url by remember { mutableStateOf(model.str("networkUrl")) }
    PageList(bottom = 24) {
        item {
            LucentSection("连接策略") {
                Choice(model, "networkTimeout", "超时（秒）", listOf("15", "30", "60"))
                Choice(model, "proxyMode", "连接方式", listOf("系统", "直连", "HTTP 代理"))
                if (model.str("proxyMode") == "HTTP 代理") {
                    var host by remember { mutableStateOf(model.str("proxyHost")) }
                    var port by remember { mutableStateOf(model.str("proxyPort")) }
                    OutlinedTextField(
                        host,
                        { host = it },
                        Modifier.fillMaxWidth().padding(16.dp),
                        label = { Text("HTTP 代理主机") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        port,
                        { port = it },
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        label = { Text("端口") },
                        singleLine = true,
                    )
                    LucentAction(
                        "保存代理",
                        {
                            if (
                                port.toIntOrNull() in 1..65535 &&
                                    host.isNotBlank() &&
                                    host.none { it.isWhitespace() || it == '/' || it == '@' }
                            ) {
                                model.set("proxyHost", host)
                                model.set("proxyPort", port)
                                model.report("已保存", "用于 WebDAV 和网络诊断请求。")
                            } else model.report("输入无效", "请填写有效主机和 1–65535 端口。")
                        },
                        Modifier.padding(16.dp),
                    )
                }
                OutlinedTextField(
                    url,
                    { url = it },
                    Modifier.fillMaxWidth().padding(16.dp),
                    label = { Text("HTTPS 测试地址") },
                    keyboardOptions =
                        androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Uri,
                            autoCorrectEnabled = false,
                        ),
                    singleLine = true,
                )
                LucentAction(
                    "测试连接",
                    {
                        model.run("网络诊断") {
                            val target = url.trim().toHttpUrl()
                            require(
                                target.isHttps &&
                                    target.username.isBlank() &&
                                    target.password.isBlank()
                            )
                            val start = android.os.SystemClock.elapsedRealtime()
                            val response =
                                WebDavTransport(model.httpClient(), 1024)
                                    .request(target, "HEAD", "")
                            model.set("networkUrl", url)
                            "服务器返回 HTTP ${response.code}，耗时 ${android.os.SystemClock.elapsedRealtime()-start} ms。此结果只表示连接，不代表账号登录成功。"
                        }
                    },
                    Modifier.fillMaxWidth().padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun AccountPage(model: DemoModel) {
    var name by remember { mutableStateOf("Demo Reader") }
    var confirm by remember { mutableStateOf(false) }
    PageList(bottom = 24) {
        item {
            LucentSection("演示账号") {
                Text(
                    "此页演示账号管理界面。不会登录任何真实平台，也不会读取其他应用凭据。",
                    Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (model.str("account").isBlank()) {
                    OutlinedTextField(
                        name,
                        { name = it.take(80) },
                        Modifier.fillMaxWidth().padding(16.dp),
                        label = { Text("演示昵称") },
                    )
                    LucentAction(
                        "建立演示会话",
                        {
                            model.set("account", name.ifBlank { "Demo Reader" })
                            model.report("演示登录成功", "本地示例会话已建立。")
                        },
                        Modifier.fillMaxWidth().padding(16.dp),
                    )
                } else {
                    LucentRow("当前演示账号", model.str("account"))
                    LucentRow(
                        "检查会话",
                        onClick = { model.report("演示会话有效", "这是本地样例状态；真实应用应调用自身认证后端。") },
                    )
                    LucentRow("退出演示会话", onClick = { confirm = true })
                }
            }
        }
    }
    if (confirm)
        Confirm("退出演示账号？", "草稿与偏好设置会保留。", { confirm = false }) {
            confirm = false
            model.set("account", "")
        }
}

@Composable
private fun AboutPage() {
    val context = LocalContext.current
    var selected by remember { mutableStateOf<String?>(null) }
    androidx.activity.compose.BackHandler(selected != null) { selected = null }
    val chapters =
        listOf(
            "foundations" to "基础与设计变量",
            "glass" to "玻璃与背景采样",
            "components" to "组件与状态",
            "motion" to "手势与动画",
            "media" to "媒体浏览",
            "settings" to "设置与备份",
            "patterns" to "页面模板",
            "integration" to "接入代码",
            "quality" to "兼容性与验收",
            "scope-and-sources" to "范围与来源",
        )
    if (selected != null) {
        val text =
            remember(selected) {
                runCatching {
                        context.assets.open("spec/${selected}.md").bufferedReader().use {
                            it.readText()
                        }
                    }
                    .getOrDefault("文档未打包")
            }
        PageList(bottom = 24) {
            item { LucentAction("返回文档目录", { selected = null }, primary = false) }
            item {
                Text(
                    text,
                    Modifier.padding(vertical = 16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    } else
        PageList(bottom = 24) {
            item {
                LucentSection("Lucent UI") {
                    LucentRow("Material 3 · Glass & Motion", "0.1.0 (1) · 可复用 Compose 组件与功能示例")
                    LucentRow("设计原则", "清晰内容、明确层级、克制反馈、可解释的动效。")
                    LucentRow(
                        "开源许可",
                        "MIT / Apache-2.0 · 查看作者署名与许可全文",
                        onClick = { selected = "licenses" },
                    )
                }
            }
            item {
                LucentSection("离线设计规范") {
                    chapters.forEach { (id, title) ->
                        LucentRow(title, onClick = { selected = id })
                    }
                }
            }
        }
}
