package dev.chengeeker.lucent.demo

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.chengeeker.lucent.*
import dev.chengeeker.lucent.demo.data.DemoModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComponentsPage(model: DemoModel, open: (String) -> Unit) {
    var checked by remember { mutableStateOf(true) }
    var selected by remember { mutableIntStateOf(0) }
    var text by remember { mutableStateOf("") }
    var sheet by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf(false) }
    var state by remember { mutableStateOf("ready") }
    var menu by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    LaunchedEffect(state) {
        if (state == "loading") {
            delay(1800)
            state = "ready"
        }
    }
    PageList {
        item {
            LucentSection("操作与选择") {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LucentAction("主要操作", { model.report("操作完成", "所有结果都以当前页面可见的反馈呈现。") })
                    LucentAction("次要操作", { sheet = true }, primary = false)
                }
                Row(
                    Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LucentAction("不可用", {}, enabled = false)
                    Box {
                        LucentAction("更多", { menu = true }, primary = false)
                        DropdownMenu(menu, { menu = false }) {
                            DropdownMenuItem(
                                { Text("查看说明") },
                                {
                                    menu = false
                                    dialog = true
                                },
                            )
                            DropdownMenuItem(
                                { Text("打开设置") },
                                {
                                    menu = false
                                    open("settings")
                                },
                            )
                        }
                    }
                }
                LucentToggle("开关状态", checked, { checked = it })
                Row(
                    Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    listOf("全部", "图片", "视频").forEachIndexed { i, t ->
                        LucentAction(
                            if (selected == i) "✓ $t" else t,
                            { selected = i },
                            primary = selected == i,
                        )
                    }
                }
            }
        }
        item {
            LucentSection("输入与校验") {
                OutlinedTextField(
                    text,
                    { text = it },
                    Modifier.fillMaxWidth().padding(16.dp),
                    label = { Text("名称") },
                    isError = text.length > 24,
                    supportingText = {
                        Text(if (text.length > 24) "最多 24 个字符" else "${text.length} / 24")
                    },
                    singleLine = true,
                )
                LucentAction(
                    "提交",
                    { model.report("已提交", text) },
                    Modifier.padding(16.dp),
                    enabled = text.isNotBlank() && text.length <= 24,
                )
            }
        }
        item {
            LucentSection("状态与反馈") {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("loading" to "加载", "empty" to "空内容", "error" to "错误", "ready" to "内容")
                        .forEach { (s, t) -> LucentAction(t, { state = s }, primary = state == s) }
                }
                when (state) {
                    "loading" ->
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                            repeat(3) {
                                Box(
                                    Modifier.fillMaxWidth(if (it == 2) .6f else 1f)
                                        .height(20.dp)
                                        .background(
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = .08f),
                                            CircleShape,
                                        )
                                )
                            }
                        }
                    "empty" ->
                        LucentEmpty(
                            "还没有内容",
                            "新建内容后会显示在这里。",
                            action = { LucentAction("添加示例", { state = "ready" }) },
                        )
                    "error" ->
                        LucentEmpty("加载失败", "这是可重试的错误状态示例。", Icons.Rounded.CloudOff) {
                            LucentAction("重试", { state = "loading" })
                        }
                    else -> LucentRow("加载成功", "内容稳定显示，不使用额外的成功弹窗。", Icons.Rounded.CheckCircle)
                }
            }
        }
        item {
            LucentSection("弹窗与展开") {
                LucentRow("确认弹窗", onClick = { dialog = true })
                LucentRow("底部面板", onClick = { sheet = true })
                LucentRow("展开详情", onClick = { expanded = !expanded })
                AnimatedVisibility(
                    expanded,
                    enter =
                        if (LocalLucentStyle.current.motion) fadeIn() + expandVertically()
                        else EnterTransition.None,
                    exit =
                        if (LocalLucentStyle.current.motion) fadeOut() + shrinkVertically()
                        else ExitTransition.None,
                ) {
                    Text("展开区域与当前操作对象保持关联。关闭时恢复原有布局，不跳回列表顶部。", Modifier.padding(16.dp))
                }
                LucentRow("玻璃与弹性导航", onClick = { open("glass") })
            }
        }
        item {
            LucentSection("设计变量") {
                LucentRow("触摸区域", "最小 48 dp；图库圆面 44 dp")
                LucentRow("分组容器", "24 dp 圆角 · 16 dp 内边距")
                LucentRow("字体", "默认 100% · 85–150% · 每档 5%")
                LucentRow("动效", "320 ms 页面进入 · 220 ms 退出 · 240 ms 底栏切换")
            }
        }
    }
    if (dialog)
        Confirm("确认操作", "说明操作对象、后果以及是否能撤销。", { dialog = false }) {
            dialog = false
            model.report("操作已确认", "此示例未修改业务数据。")
        }
    if (sheet)
        ModalBottomSheet(
            onDismissRequest = { sheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(Modifier.fillMaxWidth().imePadding().padding(16.dp)) {
                Text("当前对象的操作", style = MaterialTheme.typography.titleLarge)
                LucentRow(
                    "查看详情",
                    onClick = {
                        sheet = false
                        expanded = true
                    },
                )
                LucentRow(
                    "编辑内容",
                    onClick = {
                        sheet = false
                        open("article")
                    },
                )
                LucentRow("取消", onClick = { sheet = false })
                Spacer(Modifier.navigationBarsPadding())
            }
        }
}

@Composable
fun GlassLab(model: DemoModel) {
    var selected by remember { mutableIntStateOf(0) }
    PageList(bottom = 24) {
        item {
            Text(
                "先滚动，再长按拖动。",
                Modifier.padding(vertical = 20.dp),
                style = MaterialTheme.typography.headlineSmall,
            )
        }
        item {
            BackdropHost(
                Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(28.dp)),
                overlay = {
                    Column(
                        Modifier.align(Alignment.BottomCenter).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        LucentNavigation(
                            listOf(
                                LucentDestination("山色", Icons.Rounded.Landscape),
                                LucentDestination("光线", Icons.Rounded.WbSunny),
                                LucentDestination("远方", Icons.Rounded.Explore),
                            ),
                            selected,
                            { selected = it },
                        )
                    }
                },
            ) {
                AsyncImage(
                    "file:///android_asset/media/landscape-$selected.jpg",
                    null,
                    Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        item {
            LucentSection("玻璃层级") {
                LucentToggle("光学效果", model.bool("glass"), { model.set("glass", it) })
                LucentToggle("通透底栏", model.bool("translucent"), { model.set("translucent", it) })
                LucentRow("兼容性", "Android 13+：Backdrop；Android 8–12：GLES 底栏。磨砂顶栏保留旧系统采样。")
            }
        }
        item {
            Text(
                "滚动检查：下面的正文和评论都属于同一背景采样层。进入顶栏后应一起模糊。",
                Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        items(24) { index ->
            LucentCard {
                LucentRow(
                    if (index < 8) "正文段落 ${index+1}" else "评论 ${index-7}",
                    "清晰内容穿过顶栏后变为磨砂背景。1234567890 ABCDEFGHIJ · 不应出现透明区域里的清晰原字。",
                    Icons.Rounded.ChatBubbleOutline,
                )
            }
        }
    }
}
