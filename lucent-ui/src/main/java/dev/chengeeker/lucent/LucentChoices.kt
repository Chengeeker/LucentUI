package dev.chengeeker.lucent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Selectable settings are NEVER advanced by tapping the containing row.
 * Keep all options visible, mark the current value, and commit only after an
 * explicit choice. Boolean values instead use LucentToggle.
 */
data class LucentChoice<T>(
    val value: T,
    val label: String,
    val description: String? = null,
)

fun <T> lucentSelectedChoice(value: T, options: List<LucentChoice<T>>): LucentChoice<T>? =
    options.firstOrNull { it.value == value }

@Composable
fun LucentOptionRow(
    label: String,
    selected: Boolean,
    description: String? = null,
    enabled: Boolean = true,
    onSelect: () -> Unit,
) {
    // The radio itself deliberately has no click listener: the entire row
    // provides one large 48dp+ tap target with unified Lucent haptics.
    LucentRow(
        title = label,
        subtitle = description,
        enabled = enabled,
        onClick = onSelect,
        trailing = { RadioButton(selected = selected, onClick = null, enabled = enabled) },
    )
}

@Composable
fun <T> LucentChoiceRow(
    title: String,
    value: T,
    options: List<LucentChoice<T>>,
    onSelect: (T) -> Unit,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    supportingText: String? = null,
) {
    var selecting by remember(title) { mutableStateOf(false) }
    val selected = lucentSelectedChoice(value, options)
    val summary = selected?.label ?: "请选择"
    LucentRow(
        title = title,
        subtitle = if (supportingText == null) summary else "$summary · $supportingText",
        icon = icon,
        enabled = enabled && options.isNotEmpty(),
        onClick = { selecting = true },
    )
    if (selecting) {
        AlertDialog(
            onDismissRequest = { selecting = false },
            title = { Text(title) },
            text = {
                Column(
                    Modifier.fillMaxWidth().heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    options.forEach { option ->
                        LucentOptionRow(
                            label = option.label,
                            selected = value == option.value,
                            description = option.description,
                        ) {
                            onSelect(option.value)
                            selecting = false
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selecting = false }) { Text("取消") }
            },
        )
    }
}
