package dev.chengeeker.lucent

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*

@Composable
fun Modifier.lucentClick(
    enabled: Boolean = true,
    role: Role = Role.Button,
    onClick: () -> Unit,
): Modifier {
    val style = LocalLucentStyle.current
    val haptic = LocalHapticFeedback.current
    return clickable(
        remember { MutableInteractionSource() },
        null,
        enabled = enabled,
        role = role,
    ) {
        if (style.haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onClick()
    }
}

@Composable
fun LucentCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.cardColor()),
        content = content,
    )
}

@Composable
fun LucentSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            title,
            Modifier.padding(start = 16.dp, top = 24.dp, bottom = 12.dp),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleSmall,
        )
        LucentCard(content = content)
    }
}

@Composable
fun LucentRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val click = if (onClick != null) Modifier.lucentClick(enabled, onClick = onClick) else Modifier
    Row(
        Modifier.fillMaxWidth()
            .then(click)
            .heightIn(min = if (subtitle == null) 56.dp else 72.dp)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight =
                    FontWeight(
                        (600 +
                                (if (LocalLucentStyle.current.customWeight)
                                    LocalLucentStyle.current.weight * 100
                                else 0))
                            .coerceIn(100, 900)
                    ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else .38f),
            )
            if (subtitle != null)
                Text(
                    subtitle,
                    Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        } else if (onClick != null) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null)
    }
}

@Composable
fun LucentToggle(
    title: String,
    value: Boolean,
    onChange: (Boolean) -> Unit,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    Row(
        Modifier.fillMaxWidth()
            .lucentClick(enabled, Role.Switch) { onChange(!value) }
            .semantics {
                stateDescription = if (value) "已开启" else "已关闭"
                toggleableState =
                    if (value) androidx.compose.ui.state.ToggleableState.On
                    else androidx.compose.ui.state.ToggleableState.Off
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight =
                    FontWeight(
                        (600 +
                                (if (LocalLucentStyle.current.customWeight)
                                    LocalLucentStyle.current.weight * 100
                                else 0))
                            .coerceIn(100, 900)
                    ),
            )
            if (subtitle != null)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
        }
        Spacer(Modifier.width(12.dp))
        Switch(value, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
fun LucentAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = true,
    icon: ImageVector? = null,
) {
    val c = MaterialTheme.colorScheme
    Row(
        modifier
            .heightIn(min = 48.dp)
            .clip(CircleShape)
            .background(
                if (!enabled) c.onSurface.copy(alpha = .12f)
                else if (primary) c.primary else c.secondaryContainer
            )
            .lucentClick(enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val color =
            if (!enabled) c.onSurface.copy(alpha = .38f)
            else if (primary) c.onPrimary else c.onSecondaryContainer
        if (icon != null) {
            Icon(icon, null, Modifier.size(20.dp), tint = color)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = color, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun LucentIcon(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier.size(48.dp).clip(CircleShape).lucentClick(onClick = onClick).semantics {
            contentDescription = label
        },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null)
    }
}

@Composable
fun LucentHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(Modifier.fillMaxWidth().frostedBackdrop(MaterialTheme.colorScheme.background)) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null)
                LucentIcon(Icons.AutoMirrored.Rounded.ArrowBack, "返回", onClick = onBack)
            else Spacer(Modifier.width(8.dp))
            Text(
                title,
                Modifier.weight(1f).padding(horizontal = 8.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight =
                    FontWeight(
                        (600 +
                                (if (LocalLucentStyle.current.customWeight)
                                    LocalLucentStyle.current.weight * 100
                                else 0))
                            .coerceIn(100, 900)
                    ),
            )
            actions()
        }
    }
}

@Composable
fun LucentResult(title: String, message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { LucentAction("知道了", onDismiss) },
    )
}

@Composable
fun LucentEmpty(
    title: String,
    description: String,
    icon: ImageVector = Icons.Rounded.Inbox,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            description,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        action?.invoke()
    }
}

@Composable
fun GalleryControl(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).lucentClick(onClick = onClick).semantics {
            contentDescription = label
        },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(44.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xffeeeeee).copy(alpha = .30f),
                            Color(0xff9e9e9e).copy(alpha = .20f),
                            Color(0xff424242).copy(alpha = .36f),
                        )
                    )
                )
                .border(1.dp, Color.White.copy(alpha = .34f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = Color.White)
        }
    }
}

@Composable
internal fun lucentFeedbackAction(action: () -> Unit): () -> Unit {
    val style = LocalLucentStyle.current
    val haptic = LocalHapticFeedback.current
    return {
        if (style.haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        action()
    }
}

@Composable
fun LucentGlassChoice(translucent: Boolean, onSelect: (Boolean) -> Unit) {
    var open by remember { mutableStateOf(false) }
    LucentRow(
        "底栏玻璃样式",
        if (translucent) "经典玻璃" else "通透磨砂",
        Icons.Rounded.BlurOn,
        onClick = { open = true },
    )
    if (open)
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text("底栏玻璃样式") },
            text = {
                Column {
                    listOf(true to "经典玻璃", false to "通透磨砂").forEach { (value, title) ->
                        LucentRow(
                            title,
                            onClick = {
                                onSelect(value)
                                open = false
                            },
                            trailing = { RadioButton(value == translucent, null) },
                        )
                    }
                }
            },
            confirmButton = { LucentAction("取消", { open = false }, primary = false) },
        )
}
