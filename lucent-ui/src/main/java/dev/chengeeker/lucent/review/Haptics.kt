// Adapted from Review, Copyright 2026 Chengeeker, MIT.
package dev.chengeeker.lucent.review

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role

object SilentComposeHaptics : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) = Unit
}

val LocalReviewHapticsEnabled = staticCompositionLocalOf<() -> Boolean> { { false } }

@Composable
fun reviewPulse(): () -> Unit {
    val view = LocalView.current
    val enabled = LocalReviewHapticsEnabled.current
    return remember(view, enabled) {
        { if (enabled()) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) }
    }
}

@Composable
fun hapticClick(action: () -> Unit): () -> Unit {
    val pulse = reviewPulse()
    return { pulse(); action() }
}

@Composable
fun <T> hapticChange(action: (T) -> Unit): (T) -> Unit {
    val pulse = reviewPulse()
    return { value -> pulse(); action(value) }
}

@Composable
fun Modifier.reviewClickable(enabled: Boolean = true, onClickLabel: String? = null, role: Role? = null, onClick: () -> Unit): Modifier =
    clickable(enabled = enabled, onClickLabel = onClickLabel, role = role, onClick = hapticClick(onClick))

@Composable
fun Modifier.reviewToggleable(value: Boolean, enabled: Boolean = true, change: (Boolean) -> Unit): Modifier =
    toggleable(value = value, enabled = enabled, role = Role.Switch, onValueChange = hapticChange(change))
