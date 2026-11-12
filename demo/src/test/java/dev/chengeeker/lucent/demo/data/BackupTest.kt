package dev.chengeeker.lucent.demo.data

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class BackupTest {
    private fun data() =
        JSONObject()
            .put(
                "settings",
                DemoDefaults.fresh()
                    .put("account", "demo-account")
                    .put("webdavUrl", "https://example.com/dav/")
                    .put("webdavUser", "test-user")
                    .put("font", 1.2)
                    .put("draft", "保留草稿")
                    .put("downloadTree", "content://old-device/tree"),
            )
            .put("secret", "test-only-password")

    @Test
    fun safeBackupExcludesSecretsAndRetainsDraft() {
        val bytes = BackupCodec.export(data(), null)
        val text = bytes.toString(Charsets.UTF_8)
        assertFalse(text.contains("test-only-password"))
        assertFalse(text.contains("demo-account"))
        assertFalse(text.contains("test-user"))
        val restored = BackupCodec.decode(bytes, null)
        assertEquals("保留草稿", restored.getJSONObject("settings").getString("draft"))
        assertEquals(1.2, restored.getJSONObject("settings").getDouble("font"), .001)
    }

    @Test
    fun fullBackupUsesPortableEncryptionAndClearsOldSafGrant() {
        val bytes = BackupCodec.export(data(), "sample-backup-password")
        assertFalse(bytes.toString(Charsets.UTF_8).contains("test-only-password"))
        val restored = BackupCodec.decode(bytes, "sample-backup-password")
        assertEquals("test-only-password", restored.getString("secret"))
        assertEquals("demo-account", restored.getJSONObject("settings").getString("account"))
        assertEquals("", restored.getJSONObject("settings").getString("downloadTree"))
    }

    @Test
    fun wrongPasswordAndTamperAreRejected() {
        val bytes = BackupCodec.export(data(), "sample-backup-password")
        assertThrows(Exception::class.java) { BackupCodec.decode(bytes, "incorrect-password") }
        val changed =
            JSONObject(bytes.toString(Charsets.UTF_8))
                .put("iv", "AAAAAAAAAAAAAAAA")
                .toString()
                .toByteArray()
        assertThrows(Exception::class.java) {
            BackupCodec.decode(changed, "sample-backup-password")
        }
    }

    @Test
    fun rejectsUnknownSchemaAndInvalidSettingsBeforeMutation() {
        val original = data()
        val before = original.toString()
        val bytes = BackupCodec.export(original, null)
        val changed =
            JSONObject(bytes.toString(Charsets.UTF_8)).put("schema", 99).toString().toByteArray()
        assertThrows(Exception::class.java) { BackupCodec.decode(changed, null) }
        assertEquals(before, original.toString())
        assertThrows(Exception::class.java) {
            DemoDefaults.validate(DemoDefaults.fresh().put("font", .1))
        }
        assertThrows(Exception::class.java) {
            DemoDefaults.validate(DemoDefaults.fresh().put("unknown", true))
        }
        assertThrows(Exception::class.java) {
            DemoDefaults.validate(DemoDefaults.fresh().put("glass", "true"))
        }
    }

    @Test
    fun rejectsEmptyOversizeAndShortPassword() {
        assertThrows(Exception::class.java) { BackupCodec.decode(byteArrayOf(), null) }
        assertThrows(Exception::class.java) {
            BackupCodec.decode(ByteArray(BackupCodec.LIMIT + 1), null)
        }
        assertThrows(Exception::class.java) { BackupCodec.export(data(), "short") }
    }

    @Test
    fun legacyPbkdf2MatchesProviderIncludingUnicode() {
        for (password in listOf("password", "备份密码示例")) {
            val salt = ByteArray(16) { it.toByte() }
            val expected =
                javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(
                        javax.crypto.spec.PBEKeySpec(password.toCharArray(), salt, 2, 256)
                    )
                    .encoded
            assertArrayEquals(expected, BackupCodec.legacyPbkdf2(password, salt, 2))
        }
    }

    @Test
    fun plaintextBackupCannotInjectCredentials() {
        val safe =
            JSONObject(BackupCodec.export(data(), null).toString(Charsets.UTF_8))
                .put("secret", "injected")
        assertThrows(Exception::class.java) {
            BackupCodec.decode(safe.toString().toByteArray(), null)
        }
    }

    @Test
    fun unreadableLocalSecretIsReportedInsteadOfBlank() {
        assertThrows(SecretReadFailure::class.java) { LocalSecrets().decrypt("%%%") }
    }

    @Test
    fun freshDefaultFontIsExactly100Percent() {
        assertEquals(1.0, DemoDefaults.fresh().getDouble("font"), 0.0)
    }
}
