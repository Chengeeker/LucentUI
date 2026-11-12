package dev.chengeeker.lucent
import dev.chengeeker.lucent.review.*
import androidx.compose.material.icons.outlined.*

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
fun Modifier.lucentClick(enabled:Boolean=true,role:Role?=null,onClick:()->Unit):Modifier = reviewClickable(enabled=enabled,role=role,onClick=onClick)

@Composable
fun LucentCard(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit) {
 Box(modifier.fillMaxWidth()) { SettingsCard(content) }
}
@Composable
fun LucentSection(title:String,content:@Composable ColumnScope.()->Unit) { SettingsSection(title,content) }
@Composable
fun LucentRow(title:String,subtitle:String?=null,icon:ImageVector?=null,enabled:Boolean=true,onClick:(()->Unit)?=null,trailing:(@Composable ()->Unit)?=null) {
 val colors=MaterialTheme.colorScheme
 SettingsTile(headlineContent={Text(title,fontSize=15.sp,lineHeight=20.25.sp,color=colors.onSurface.copy(alpha=if(enabled)1f else .38f))},
 supportingContent=subtitle?.takeIf{it.isNotBlank()}?.let { {Text(it,fontSize=12.5.sp,lineHeight=17.5.sp)} },
 leadingContent=icon?.let { {Icon(it,null,tint=colors.onSurfaceVariant)} },
 trailingContent=trailing ?: onClick?.let { {Icon(Icons.Outlined.ChevronRight,null,tint=colors.onSurfaceVariant)} },
 modifier=if(onClick!=null) Modifier.lucentClick(enabled,onClick=onClick) else Modifier)
}
@Composable
fun LucentToggle(title:String,value:Boolean,onChange:(Boolean)->Unit,subtitle:String?=null,enabled:Boolean=true) {
 SettingsTile(headlineContent={Text(title,fontSize=16.sp,fontWeight=reviewWeight(FontWeight.SemiBold))},
 supportingContent=subtitle?.takeIf{it.isNotBlank()}?.let{{Text(it,fontSize=14.sp,lineHeight=19.6.sp)}},
 trailingContent={Switch(value,null,modifier=Modifier.padding(horizontal=4.dp),enabled=enabled)},
 modifier=Modifier.reviewToggleable(value,enabled,onChange).padding(vertical=3.dp))
}
@Composable
fun LucentAction(text:String,onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,primary:Boolean=true,icon:ImageVector?=null) {
 val click=hapticClick(onClick)
 val body:@Composable RowScope.()->Unit={ if(icon!=null){Icon(icon,null,Modifier.size(18.dp));Spacer(Modifier.width(8.dp))};Text(text) }
 if(primary) Button(onClick=click,modifier=modifier,enabled=enabled,content=body)
 else TextButton(onClick=click,modifier=modifier,enabled=enabled,content=body)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LucentHeader(title:String,onBack:(()->Unit)?=null,actions:@Composable RowScope.()->Unit={}) {
 Box(Modifier.frostedBackdrop(MaterialTheme.colorScheme.background)) {
 TopAppBar(title={Text(title,Modifier.padding(start=16.dp),fontSize=18.sp,fontWeight=reviewWeight(FontWeight.Bold))},
 navigationIcon={if(onBack!=null)IconButton(onClick=hapticClick(onBack)){Icon(Icons.AutoMirrored.Rounded.ArrowBack,"返回")}},
 actions=actions,expandedHeight=56.dp,colors=TopAppBarDefaults.topAppBarColors(containerColor=Color.Transparent))
 }
}

@Composable
fun LucentResult(title: String, message: String, onDismiss: () -> Unit) {
    ReviewAlertDialog(
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
        ReviewAlertDialog(
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
