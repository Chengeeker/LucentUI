// Adapted from Review, Copyright 2026 Chengeeker, MIT.
@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package dev.chengeeker.lucent.review

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(title, Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    SettingsCard(content)
    Spacer(Modifier.height(16.dp))
}
@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.settingsCardColor()) { Column(content = content) }
}

@Composable
fun SettingsTile(headlineContent: @Composable () -> Unit, modifier: Modifier = Modifier,
    supportingContent: (@Composable () -> Unit)? = null, leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null) {
    val colors = MaterialTheme.colorScheme
    Row(modifier.fillMaxWidth().padding(horizontal = 16.dp).heightIn(min = if (supportingContent == null) 56.dp else 72.dp), verticalAlignment = Alignment.CenterVertically) {
        if (leadingContent != null) { leadingContent(); Spacer(Modifier.width(16.dp)) }
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            ProvideTextStyle(MaterialTheme.typography.bodyLarge.copy(fontWeight = reviewWeight(FontWeight.SemiBold), lineHeight = 21.6.sp, color = colors.onSurface), headlineContent)
            if (supportingContent != null) ProvideTextStyle(MaterialTheme.typography.bodyMedium.copy(lineHeight = 19.6.sp, color = colors.onSurfaceVariant), supportingContent)
        }
        if (trailingContent != null) { Spacer(Modifier.width(16.dp)); trailingContent() }
    }
}

