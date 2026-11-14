package dev.chengeeker.lucent.demo

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import dev.chengeeker.lucent.LucentStyle
import dev.chengeeker.lucent.LucentTheme
import dev.chengeeker.lucent.LucentVideoBottom
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class VideoControlsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun progressStaysCompactAndActionsUseOnePulse() {
        val enabled = mutableStateOf(true)
        val effects = mutableListOf<Int>()
        val actions = mutableListOf<String>()
        lateinit var view: View
        compose.runOnUiThread {
            view = object : View(compose.activity) {
                override fun performHapticFeedback(feedbackConstant: Int): Boolean {
                    effects.add(feedbackConstant)
                    return true
                }
            }
        }
        compose.setContent {
            CompositionLocalProvider(LocalView provides view) {
                LucentTheme(LucentStyle(haptics = enabled.value)) {
                    Box(Modifier.fillMaxSize()) {
                        LucentVideoBottom(
                            Modifier, 0, 12_000, 12_000, false, 1f,
                            quality = "720p",
                            onPlay = { actions.add("play") },
                            onSpeed = { actions.add("speed") },
                            onQuality = { actions.add("quality") },
                            onFullscreen = { actions.add("fullscreen") },
                            onSeek = { actions.add("seek:$it") },
                        )
                    }
                }
            }
        }
        val progress = compose.onNodeWithTag("video-progress")
        val density = compose.activity.resources.displayMetrics.density
        assertEquals(16f, progress.fetchSemanticsNode().boundsInRoot.height / density, .6f)
        listOf("播放", "播放倍速", "清晰度选择", "全屏").forEach {
            compose.onNodeWithContentDescription(it).performClick()
        }
        progress.performSemanticsAction(SemanticsActions.SetProgress) { it(6_000f) }
        compose.runOnIdle {
            assertEquals(listOf("play", "speed", "quality", "fullscreen", "seek:6000"), actions)
            assertEquals(List(5) { HapticFeedbackConstants.KEYBOARD_TAP }, effects)
            enabled.value = false
        }
        compose.onNodeWithContentDescription("播放倍速").performClick()
        compose.runOnIdle {
            assertEquals("speed", actions.last())
            assertEquals(5, effects.size)
        }
    }
}
