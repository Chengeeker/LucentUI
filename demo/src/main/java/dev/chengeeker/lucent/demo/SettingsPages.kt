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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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
        dev.chengeeker.lucent.review.ReviewAlertDialog(
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
        "appearance" -> ReviewAppearance(model, open)
        "webdav",
        "local" -> BackupPage(page == "webdav", model)
        "storage" -> StoragePage(model)
        "cache" -> CacheSettingsPage(model)
        "notifications" -> NotificationPage(model)
        "network" -> NetworkPage(model)
        "account" -> AccountPage(model)
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
                                        model.set("filter", filter) {
                                            model.report("已保存", "时间线和评论将应用这些屏蔽词。")
                                        }
                                    },
                                    Modifier.padding(16.dp),
                                )
                                LucentRow("预览阅读页", onClick = { open("article") })
                                LucentRow("收藏", onClick = { open("favorites") })
                                LucentRow("浏览记录", onClick = { open("history") })
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
                                        model.set("historyItems", "") {
                                            model.report("已清空", "只清除了 Demo 的浏览记录。")
                                        }
                                    },
                                )
                            }
                        }
                        item {
                            var confirm by remember { mutableStateOf(false) }
                            LucentSection("重置") {
                                LucentRow(
                                    "重置 Demo 数据",
                                    "清除设置、本地动态、草稿、收藏和本机连接凭据。",
                                    onClick = { confirm = true },
                                )
                            }
                            if (confirm)
                                Confirm("重置 Demo？", "本地动态和发帖草稿将永久清除。设置与收藏可先导出备份。", { confirm = false }) {
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
fun Confirm(title: String, body: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    dev.chengeeker.lucent.review.ReviewAlertDialog(
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
    var directory by remember(model.str("webdavDirectory")) { mutableStateOf(model.str("webdavDirectory")) }
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
    val colors = MaterialTheme.colorScheme
    val ready = model.busy == null && (savedCredential.isSuccess || authPassword.isNotEmpty())
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = LocalDemoContentTop.current + 14.dp, bottom = 24.dp)) {
        Surface(shape = RoundedCornerShape(16.dp), color = colors.primaryContainer.copy(alpha = .4f)) {
            Row(Modifier.fillMaxWidth().padding(14.dp)) {
                Icon(Icons.Outlined.CloudSync, null, tint = colors.primary)
                Spacer(Modifier.width(12.dp))
                Column { Text("备份内容", style = MaterialTheme.typography.titleMedium, color = colors.primary, fontWeight = FontWeight.Bold); Spacer(Modifier.height(4.dp)); Text("备份设置、草稿、收藏和浏览历史，不含图片和视频缓存。", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant) }
            }
        }
        Spacer(Modifier.height(16.dp))
        Surface(shape = RoundedCornerShape(24.dp), color = colors.surfaceContainerLowest) {
            Column(Modifier.selectableGroup()) {
                listOf(false, true).forEach { selected ->
                    Row(Modifier.fillMaxWidth().selectable(full == selected, interactionSource = null, indication = null, role = androidx.compose.ui.semantics.Role.RadioButton, onClick = dev.chengeeker.lucent.review.hapticClick { full = selected }).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(if(selected) "加密全量备份" else "全量安全备份", style = MaterialTheme.typography.titleMedium)
                            Text(if(selected) "含演示账号及 WebDAV 密码" else "不含账号及 WebDAV 密码", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                            if(selected) Text("使用独立密码加密，请妥善保管", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                        }
                        Spacer(Modifier.width(12.dp)); RadioButton(full == selected, null, Modifier.size(24.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        if(webdav) {
            Surface(shape = RoundedCornerShape(24.dp), color = colors.surfaceContainerLowest) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("WebDAV 服务器配置", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(url, { url = it }, Modifier.fillMaxWidth(), label = { Text("服务器 URL") }, placeholder = { Text("https://example.com/dav/") }, leadingIcon = { Icon(Icons.Outlined.Link, null) }, singleLine = true)
                    OutlinedTextField(user, { user = it }, Modifier.fillMaxWidth(), label = { Text("账号 / 邮箱") }, leadingIcon = { Icon(Icons.Outlined.PersonOutline, null) }, singleLine = true)
                    OutlinedTextField(authPassword, { authPassword = it }, Modifier.fillMaxWidth(), label = { Text("应用密码 / Token") }, leadingIcon = { Icon(Icons.Outlined.Lock, null) }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password, autoCorrectEnabled = false))
                    OutlinedTextField(directory, { directory = it }, Modifier.fillMaxWidth(), label = { Text("备份文件夹名称（可留空）") }, leadingIcon = { Icon(Icons.Outlined.Folder, null) }, singleLine = true)
                    OutlinedButton(onClick = dev.chengeeker.lucent.review.hapticClick { keyboard?.hide(); model.testDav(url, user, authPassword, directory) }, enabled = ready, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.WifiTethering, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("测试服务器连接")
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        Surface(shape = RoundedCornerShape(24.dp), color = colors.surfaceContainerLowest) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(if(webdav) "云端备份与还原操作" else "本地备份与还原操作", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text(if(full) "备份密码（至少 8 字符）" else "恢复加密备份时填写密码") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password, autoCorrectEnabled = false))
                if(full) Text("忘记密码无法恢复全量文件，请妥善保管文件和密码。", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = dev.chengeeker.lucent.review.hapticClick { keyboard?.hide(); confirmation = "export" }, enabled = ready && (!full || password.length >= 8) && (!webdav || url.isNotBlank()), modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)) { Icon(Icons.Outlined.CloudUpload, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(if(webdav) "上传备份" else "导出备份") }
                    FilledTonalButton(onClick = dev.chengeeker.lucent.review.hapticClick { keyboard?.hide(); if(webdav) confirmation = "restore" else importer.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }, enabled = ready && (!webdav || url.isNotBlank()), modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)) { Icon(Icons.Outlined.CloudDownload, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(if(webdav) "下载还原" else "导入备份") }
                }
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
                    if (webdav) model.upload(full, password, url, user, authPassword, directory)
                    else exporter.launch("LucentUI-backup.json")
                "restore" -> model.download(password, url, user, authPassword, directory)
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
private fun StoragePage(model: DemoModel, cacheOnly: Boolean = false) {
    val context = LocalContext.current
    var bytes by remember { mutableLongStateOf(0) }
    LaunchedEffect(Unit) { bytes = withContext(Dispatchers.IO) { context.cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() } } }
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
        if (!cacheOnly) item {
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
        if (!cacheOnly) item {
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
                                    if (manager.getNotificationChannel("demo")?.importance == NotificationManager.IMPORTANCE_NONE) {
                                        model.report("演示通知已关闭", "请在系统通知设置中开启演示通知类别。")
                                        return@runCatching
                                    }
                                    manager.notify(
                                        1,
                                        NotificationCompat.Builder(context, "demo")
                                            .setSmallIcon(
                                                dev.chengeeker.lucent.demo.R.drawable.ic_launcher
                                            )
                                            .setContentTitle("Lucent UI")
                                            .setContentText("这是 Lucent UI 的本地通知预览。")
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
                                model.saveSettings(mapOf("proxyHost" to host, "proxyPort" to port)) {
                                    model.report("已保存", "用于 WebDAV 和网络诊断请求。")
                                }
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
fun AboutPage(chapter: String, open: (String) -> Unit) {
    val context = LocalContext.current
    if (chapter.isNotEmpty()) {
        val text = remember(chapter) {
            runCatching { context.assets.open("spec/$chapter.md").bufferedReader().use { it.readText() } }
                .getOrDefault("文档未打包")
        }
        PageList(bottom = 24) {
            item { Text(text, Modifier.padding(vertical = 16.dp), style = MaterialTheme.typography.bodyMedium) }
        }
    } else PageList(bottom = 24) {
        item {
            LucentSection("Lucent UI") {
                LucentRow("Material 3 · Glass & Motion", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · Review 视觉基准与功能示例")
                LucentRow("开源许可", "MIT / Apache-2.0", onClick = { open("about:licenses") })
            }
        }
        item {
            LucentSection("离线设计规范") {
                specChapters.filterKeys { it != "licenses" }.forEach { (id, title) ->
                    LucentRow(title, onClick = { open("about:$id") })
                }
            }
        }
    }
}

@Composable private fun CacheSettingsPage(model: DemoModel) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    var bytes by remember { mutableLongStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var confirm by remember { mutableStateOf(false) }
    var revision by remember { mutableIntStateOf(0) }
    LaunchedEffect(revision) {
        loading = true
        try { bytes = withContext(Dispatchers.IO) { context.cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() } } }
        finally { loading = false }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start=16.dp,end=16.dp,top=LocalDemoContentTop.current+12.dp,bottom=32.dp)) {
        LucentCard {
            Column(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=24.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                Surface(Modifier.size(56.dp),shape=androidx.compose.foundation.shape.CircleShape,color=colors.primaryContainer) { Box(contentAlignment=Alignment.Center){ Icon(Icons.Outlined.CleaningServices,null,Modifier.size(30.dp),tint=colors.onPrimaryContainer) } }
                Spacer(Modifier.height(16.dp))
                if(loading) CircularProgressIndicator(Modifier.size(24.dp),strokeWidth=2.5.dp) else Text("${bytes/1024} KiB",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(4.dp)); Text("可管理的 Demo 临时缓存",color=colors.onSurfaceVariant)
                Spacer(Modifier.height(8.dp)); Text("不包含草稿、设置及已导出文件",style=MaterialTheme.typography.bodySmall,color=colors.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(16.dp))
        LucentCard { Button(onClick=dev.chengeeker.lucent.review.hapticClick{confirm=true},enabled=!loading&&model.busy==null,modifier=Modifier.padding(16.dp).fillMaxWidth().height(48.dp),shape=RoundedCornerShape(12.dp)){Icon(Icons.Outlined.DeleteSweep,null);Spacer(Modifier.width(8.dp));Text("清理临时缓存")} }
        Spacer(Modifier.height(24.dp));Text("自动清理规则",Modifier.padding(horizontal=16.dp),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        LucentCard { Choice(model,"cacheLimit","启动时自动清理阈值",listOf("关闭","16 MiB","64 MiB","256 MiB")) }
        Spacer(Modifier.height(24.dp))
        LucentCard { Text("仅清理本应用的临时缓存；已保存的图片、视频、设置、备份与演示账号资料会保留。",Modifier.padding(16.dp),color=colors.onSurfaceVariant) }
        Spacer(Modifier.height(16.dp));TextButton(onClick=dev.chengeeker.lucent.review.hapticClick{revision++},modifier=Modifier.align(Alignment.CenterHorizontally)){Icon(Icons.Outlined.Refresh,null);Text("刷新缓存占用")}
    }
    if(confirm) Confirm("清理临时缓存？","不会删除已保存的媒体及个人设置。",{confirm=false}) {
        confirm=false
        model.run("清理缓存") {
            withContext(Dispatchers.IO) { context.cacheDir.listFiles()?.forEach { check(it.deleteRecursively()) { "缓存文件正在使用，请稍后重试" } } }
            revision++; "已清理 Demo 临时缓存。"
        }
    }
}
