package dev.chengeeker.lucent.demo.data

import android.app.Application
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import android.util.Base64
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.chengeeker.lucent.LucentStyle
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject

object DemoDefaults {
    val values =
        linkedMapOf<String, Any>(
            "mode" to "system",
            "dynamic" to false,
            "seed" to 0,
            "black" to false,
            "font" to 1.0,
            "weight" to 0,
            "weightCustom" to false,
            "floating" to true,
            "glass" to false,
            "translucent" to false,
            "motion" to true,
            "haptics" to true,
            "refresh" to "系统默认",
            "icon" to "圆润",
            "layout" to "舒适",
            "avatar" to true,
            "time" to true,
            "preview" to true,
            "filter" to "",
            "autoplay" to false,
            "dataSaver" to false,
            "quality" to "自动",
            "speed" to "1.0",
            "loop" to false,
            "downloadWifi" to true,
            "downloadTree" to "",
            "notify" to false,
            "private" to false,
            "history" to true,
            "networkTimeout" to "30",
            "webdavUrl" to "",
            "webdavUser" to "",
            "webdavDirectory" to "",
            "account" to "",
            "draft" to "",
            "bookmarks" to "",
            "historyItems" to "",
            "plugin" to false,
            "keyboardHeight" to "240",
            "networkUrl" to "https://example.com",
            "lyricsCenter" to true,
            "lyricsBlur" to false,
            "lyricsSize" to "22",
            "proxyMode" to "系统",
            "proxyHost" to "127.0.0.1",
            "proxyPort" to "8080",
            "cacheLimit" to "关闭",
            "downloadRate" to "0",
            "downloadNaming" to "时间戳",
            "bodySpacing" to "1.5",
            "imageRound" to true,
            "relativeTime" to true,
            "browser" to true,
        )

    fun fresh() = JSONObject(values)

    fun validate(input: JSONObject): JSONObject {
        require(input.length() <= values.size) { "包含未知设置" }
        val out = fresh()
        for (key in input.keys()) {
            val expected = values[key] ?: error("不支持的设置项")
            val v = input.get(key)
            require(
                when (expected) {
                    is Boolean -> v is Boolean
                    is Number -> v is Number
                    else -> v is String && (v as String).length <= 32_000
                }
            ) {
                "设置类型不正确"
            }
            out.put(key, v)
        }
        require(out.getDouble("font").isFinite() && out.getDouble("font") in .85..1.5)
        require(out.getInt("weight") in -3..3 && out.getInt("seed") in 0..7)
        require(out.getString("mode") in setOf("system", "light", "dark"))
        require(out.getString("speed") in setOf("0.5", "0.75", "1.0", "1.25", "1.5", "2.0", "3.0"))
        require(out.getString("networkTimeout") in setOf("15", "30", "60"))
        require(out.getString("keyboardHeight").toIntOrNull() in 180..360)
        require(out.getString("proxyMode") in setOf("系统", "直连", "HTTP 代理"))
        require(out.getString("proxyPort").toIntOrNull() in 1..65535)
        require(
            out.getString("proxyHost").length in 1..253 &&
                out.getString("proxyHost").none { it.isWhitespace() || it == '/' || it == '@' }
        )
        require(out.getString("cacheLimit") in setOf("关闭", "16 MiB", "64 MiB", "256 MiB"))
        require(out.getString("downloadRate") in setOf("0", "128", "512", "2048"))
        require(out.getString("bodySpacing") in setOf("1.3", "1.5", "1.8"))
        require(out.getString("lyricsSize") in setOf("18", "22", "26", "30"))
        validateDavDirectory(out.getString("webdavDirectory"))
        return out
    }
}

object BackupCodec {
    const val LIMIT = 1_048_576

    private fun b64(b: ByteArray) = Base64.encodeToString(b, Base64.NO_WRAP)

    private fun un64(s: String) = Base64.decode(s, Base64.NO_WRAP)

    fun export(data: JSONObject, password: String?): ByteArray {
        val body =
            JSONObject()
                .put("format", "lucent-ui")
                .put("schema", 1)
                .put("settings", DemoDefaults.validate(data.getJSONObject("settings")))
        if (password == null)
            body
                .getJSONObject("settings")
                .put("account", "")
                .put("webdavUser", "").put("webdavDirectory", "")
                .put("webdavUrl", "")
                .put("downloadTree", "")
        else {
            require(password.length in 8..256) { "备份密码需要 8–256 个字符" }
            body.put("secret", data.optString("secret"))
        }
        val plain = body.toString().toByteArray()
        require(plain.size <= LIMIT) { "备份超过大小上限" }
        if (password == null) return plain
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(password, salt))
        cipher.updateAAD("lucent-ui:1".toByteArray())
        return JSONObject()
            .put("format", "lucent-ui-encrypted")
            .put("schema", 1)
            .put("salt", b64(salt))
            .put("iv", b64(cipher.iv))
            .put("body", b64(cipher.doFinal(plain)))
            .toString()
            .toByteArray()
    }

    fun decode(bytes: ByteArray, password: String?): JSONObject {
        require(bytes.isNotEmpty() && bytes.size <= LIMIT) { "文件为空或超过 1 MiB" }
        var obj = JSONObject(bytes.toString(Charsets.UTF_8))
        val encrypted = obj.optString("format") == "lucent-ui-encrypted"
        if (encrypted) {
            require(obj.getInt("schema") == 1 && password != null && password.length in 8..256) {
                "需要正确的备份密码"
            }
            val salt = un64(obj.getString("salt"))
            val iv = un64(obj.getString("iv"))
            require(salt.size == 16 && iv.size == 12)
            val c = Cipher.getInstance("AES/GCM/NoPadding")
            c.init(Cipher.DECRYPT_MODE, key(password, salt), GCMParameterSpec(128, iv))
            c.updateAAD("lucent-ui:1".toByteArray())
            obj = JSONObject(c.doFinal(un64(obj.getString("body"))).toString(Charsets.UTF_8))
        }
        require(obj.optString("format") == "lucent-ui" && obj.optInt("schema") == 1) {
            "不是受支持的 Lucent UI 备份"
        }
        val settings = DemoDefaults.validate(obj.getJSONObject("settings"))
        if (!encrypted) {
            require(!obj.has("secret")) { "安全备份不能包含明文凭据" }
            settings.put("account", "").put("webdavUrl", "").put("webdavUser", "").put("webdavDirectory", "")
        }
        require(!obj.has("secret") || obj.get("secret") is String)
        // SAF grants belong to the old installation and must be selected again on this device.
        settings.put("downloadTree", "")
        return JSONObject()
            .put("settings", settings)
            .put("secret", obj.optString("secret").also { require(it.length <= 8192) })
    }

    private fun key(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, 210_000, 256)
        return try {
            SecretKeySpec(
                try {
                    SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                        .generateSecret(spec)
                        .encoded
                } catch (_: java.security.NoSuchAlgorithmException) {
                    legacyPbkdf2(password, salt)
                },
                "AES",
            )
        } finally {
            spec.clearPassword()
        }
    }

    internal fun legacyPbkdf2(
        password: String,
        salt: ByteArray,
        iterations: Int = 210_000,
    ): ByteArray {
        require(iterations > 0)
        val mac =
            javax.crypto.Mac.getInstance("HmacSHA256").apply {
                init(SecretKeySpec(password.toByteArray(Charsets.UTF_8), "HmacSHA256"))
            }
        var u = mac.doFinal(salt + byteArrayOf(0, 0, 0, 1))
        val result = u.copyOf()
        repeat(iterations - 1) {
            u = mac.doFinal(u)
            for (i in result.indices) result[i] = (result[i].toInt() xor u[i].toInt()).toByte()
        }
        u.fill(0)
        return result
    }
}

class LocalSecrets {
    private val alias = "lucent.webdav.v1"

    private fun key(): javax.crypto.SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(alias, null) as? javax.crypto.SecretKey)?.let {
            return it
        }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            .apply {
                init(
                    KeyGenParameterSpec.Builder(
                            alias,
                            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                        )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .build()
                )
            }
            .generateKey()
    }

    fun encrypt(value: String): String {
        if (value.isEmpty()) return ""
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, key())
        return Base64.encodeToString(c.iv + c.doFinal(value.toByteArray()), Base64.NO_WRAP)
    }

    fun decrypt(value: String): String {
        if (value.isEmpty()) return ""
        return try {
            val b = Base64.decode(value, Base64.NO_WRAP)
            require(b.size >= 28)
            val c = Cipher.getInstance("AES/GCM/NoPadding")
            c.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, b.copyOfRange(0, 12)))
            c.doFinal(b.copyOfRange(12, b.size)).toString(Charsets.UTF_8)
        } catch (_: Exception) {
            throw SecretReadFailure("本机连接密码无法解密，原资料已保留；请重新输入授权密码后保存。")
        }
    }
}

class SecretReadFailure(message: String) : IllegalStateException(message)

class DemoModel(app: Application) : AndroidViewModel(app) {
    private val atomic = AtomicFile(File(app.filesDir, "demo-state.json"))
    private val mutex = Mutex()
    private val secrets = LocalSecrets()
    private var readFailure = false
    var result by mutableStateOf<Pair<String, String>?>(null)
        private set

    var document by
        mutableStateOf(
            runCatching {
                    JSONObject(atomic.openRead().bufferedReader().use { it.readText() }).also {
                        DemoDefaults.validate(it.getJSONObject("settings"))
                    }
                }
                .getOrElse {
                    if (atomic.baseFile.exists()) readFailure = true
                    JSONObject().put("settings", DemoDefaults.fresh()).put("secret", "")
                }
        )
        private set

    var busy by mutableStateOf<String?>(null)
        private set

    private var operation: Job? = null

    init {
        if (readFailure) result = "本机数据无法读取" to "原文件已保留。请先导出文件排查，或通过隐私页明确重置；不会自动覆盖。"
        viewModelScope.launch(Dispatchers.IO) {
            val limit = str("cacheLimit").substringBefore(' ').toLongOrNull()?.times(1024 * 1024)
            if (
                limit != null &&
                    app.cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() } > limit
            )
                app.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
        }
    }

    fun str(key: String) =
        document.getJSONObject("settings").optString(key, DemoDefaults.values[key].toString())

    fun bool(key: String) =
        document
            .getJSONObject("settings")
            .optBoolean(key, DemoDefaults.values[key] as? Boolean ?: false)

    fun number(key: String) =
        document
            .getJSONObject("settings")
            .optDouble(key, (DemoDefaults.values[key] as? Number)?.toDouble() ?: 0.0)

    fun style() =
        LucentStyle(
            str("mode"),
            bool("dynamic"),
            number("seed").toInt(),
            bool("black"),
            number("font").toFloat(),
            number("weight").toInt(),
            bool("glass"),
            bool("translucent"),
            bool("motion"),
            bool("haptics"),
            customWeight = bool("weightCustom"),
            floating = bool("floating"),
        )

    fun set(key: String, value: Any) {
        viewModelScope.launch {
            try {
                mutex.withLock {
                    val next = JSONObject(document.toString())
                    next.getJSONObject("settings").put(key, value)
                    DemoDefaults.validate(next.getJSONObject("settings"))
                    commit(next)
                }
            } catch (_: Exception) {
                report("保存失败", "原有设置已保留，请检查存储空间。")
            }
        }
    }

    private suspend fun commit(next: JSONObject) =
        withContext(NonCancellable) {
            check(!readFailure) { "本机数据无法读取，原文件已保留" }
            withContext(Dispatchers.IO) {
                val stream = atomic.startWrite()
                try {
                    stream.write(next.toString().toByteArray())
                    atomic.finishWrite(stream)
                } catch (e: Exception) {
                    atomic.failWrite(stream)
                    throw e
                }
            }
            document = next
        }

    fun report(title: String, message: String) {
        result = title to message
    }

    fun dismiss() {
        result = null
    }

    fun cancel() {
        operation?.cancel()
    }

    fun run(title: String, block: suspend () -> String) {
        if (busy != null) return
        busy = title
        operation =
            viewModelScope.launch {
                try {
                    report("$title · 完成", block())
                } catch (_: CancellationException) {
                    report("操作已取消", "若已进入提交阶段，本机数据或远端文件可能已经更新；请先核对当前状态再重试。")
                } catch (e: Exception) {
                    report(
                        "$title · 未完成",
                        when (e) {
                            is javax.crypto.AEADBadTagException -> "备份密码不正确或文件已损坏，原数据已保留。"
                            else -> webDavError(e)
                        },
                    )
                } finally {
                    busy = null
                    operation = null
                }
            }
    }

    private suspend fun saveWebDav(url: String, user: String, password: String, directory: String) {
        val target = davBaseUrl(url, "")
        validateDavDirectory(directory)
        mutex.withLock {
            val next = JSONObject(document.toString())
            next.getJSONObject("settings").put("webdavUrl", target.toString()).put("webdavUser", user).put("webdavDirectory", directory.trim())
            next.put("secret", withContext(Dispatchers.IO) { secrets.encrypt(password) })
            commit(next)
        }
    }

    fun savedPassword() = secrets.decrypt(document.optString("secret"))

    suspend fun backup(full: Boolean, password: String): ByteArray =
        withContext(Dispatchers.Default) {
            val snapshot = JSONObject(document.toString())
            if (full) snapshot.put("secret", secrets.decrypt(snapshot.optString("secret")))
            BackupCodec.export(snapshot, if (full) password else null)
        }

    suspend fun restore(bytes: ByteArray, password: String): String {
        val parsed =
            withContext(Dispatchers.Default) {
                BackupCodec.decode(bytes, password.takeIf { it.isNotEmpty() })
            }
        mutex.withLock {
            val next = JSONObject(parsed.toString())
            next.put(
                "secret",
                withContext(Dispatchers.IO) { secrets.encrypt(parsed.optString("secret")) },
            )
            // One atomic replacement: validation and encryption complete before touching current
            // state.
            commit(next)
        }
        return "已恢复设置、草稿和收藏。请重新选择下载目录；安全备份不携带连接凭据。"
    }

    fun httpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .apply {
                connectTimeout(str("networkTimeout").toLong(), TimeUnit.SECONDS)
                readTimeout(str("networkTimeout").toLong(), TimeUnit.SECONDS)
                if (str("proxyMode") == "直连") proxy(java.net.Proxy.NO_PROXY)
                if (str("proxyMode") == "HTTP 代理")
                    proxy(
                        java.net.Proxy(
                            java.net.Proxy.Type.HTTP,
                            java.net.InetSocketAddress.createUnresolved(
                                str("proxyHost"),
                                str("proxyPort").toInt(),
                            ),
                        )
                    )
            }
            .build()

    private fun transport() = WebDavTransport(httpClient(), BackupCodec.LIMIT.toLong())

    private fun base() = davBaseUrl(str("webdavUrl"), str("webdavDirectory"))
    private fun fileUrl() = base().newBuilder().addPathSegment("lucent-ui-backup.json").build()
    private fun auth() = Credentials.basic(str("webdavUser"), savedPassword())

    private suspend fun ensureDirectory() {
        if (str("webdavDirectory").isBlank()) return
        val response = transport().request(base(), "PROPFIND", auth())
        if (response.code == 404) transport().request(base(), "MKCOL", auth()).requireStatus("创建备份目录", setOf(201, 405))
        else response.requireStatus("访问备份目录", setOf(200, 207))
    }

    fun testDav(url: String, user: String, credential: String, directory: String) = run("连接测试") {
        saveWebDav(url, user, credential, directory)
        ensureDirectory()
        transport().request(base(), "PROPFIND", auth()).requireStatus("连接", setOf(200, 207))
        "服务器已确认目录可以访问，配置已加密保存在本机。"
    }

    fun upload(full: Boolean, password: String, url: String, user: String, credential: String, directory: String) = run("上传备份") {
        saveWebDav(url, user, credential, directory)
        ensureDirectory()
        transport().request(fileUrl(), "PUT", auth(), backup(full, password)).requireStatus("上传", setOf(200, 201, 204))
        "服务器已确认写入 lucent-ui-backup.json。"
    }

    fun download(password: String, url: String, user: String, credential: String, directory: String) = run("恢复备份") {
        saveWebDav(url, user, credential, directory)
        val response = transport().request(fileUrl(), "GET", auth()).requireStatus("下载", setOf(200))
        restore(response.bytes, password)
    }

    fun reset() =
        run("重置 Demo") {
            mutex.withLock {
                if (readFailure) {
                    withContext(Dispatchers.IO) {
                        atomic.baseFile.copyTo(
                            File(
                                getApplication<Application>().filesDir,
                                "demo-state-unreadable-${System.currentTimeMillis()}.json",
                            )
                        )
                    }
                    readFailure = false
                }
                commit(JSONObject().put("settings", DemoDefaults.fresh()).put("secret", ""))
            }
            "已重置 Demo 设置，不影响其他应用和云端文件。"
        }
}
