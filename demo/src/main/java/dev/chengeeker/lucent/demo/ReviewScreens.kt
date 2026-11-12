@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package dev.chengeeker.lucent.demo

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import dev.chengeeker.lucent.*
import dev.chengeeker.lucent.review.SettingsCard
import dev.chengeeker.lucent.review.SettingsSection
import dev.chengeeker.lucent.review.SettingsTile
import dev.chengeeker.lucent.review.reviewWeight
import dev.chengeeker.lucent.review.reviewToggleable
import dev.chengeeker.lucent.review.ReviewDivider
import dev.chengeeker.lucent.demo.data.DemoModel

// Adapted from Review SettingsScreens.kt; only routes and app-specific labels differ.
@Composable
fun ReviewSettingsRoot(model:DemoModel,open:(String)->Unit,embedded:Boolean=false) {
 val top=if(embedded) LocalDemoContentTop.current else WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start=16.dp,end=16.dp,top=top+20.dp,bottom=104.dp)) {
  ReferenceGroup("偏好与功能") {
   LucentRow("个性化",icon=Icons.Outlined.Palette,onClick={open("appearance")})
   LucentRow("内容样式",icon=Icons.Outlined.Style,onClick={open("reading")})
   LucentRow("订阅消息提醒",icon=Icons.Outlined.NotificationsActive,onClick={open("notifications")})
  }
  ReferenceGroup("存储与备份") {
   LucentRow("存储设置",icon=Icons.Outlined.FolderOpen,onClick={open("storage")})
   LucentRow("缓存管理",icon=Icons.Outlined.CleaningServices,onClick={open("cache")})
   LucentRow("WebDAV备份",icon=Icons.Outlined.CloudSync,onClick={open("webdav")})
  }
  ReferenceGroup("账号设置") {
   LucentRow(if(model.str("account").isBlank())"登录账号" else "账号管理",icon=Icons.Outlined.Login,onClick={open("account")})
  }
  ReferenceGroup("关于与支持") {
   LucentRow("关于 Lucent UI","版本 ${BuildConfig.VERSION_NAME}",Icons.Outlined.Info,onClick={open("about")})
  }
 }
}
@Composable private fun ReferenceGroup(title:String,content:@Composable ColumnScope.()->Unit) {
 Text(title,Modifier.padding(horizontal=16.dp).padding(bottom=12.dp),style=MaterialTheme.typography.titleSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
 SettingsCard(content);Spacer(Modifier.height(32.dp))
}
@Composable private fun ReferenceToggle(title:String,summary:String,icon:androidx.compose.ui.graphics.vector.ImageVector,value:Boolean,change:(Boolean)->Unit) {
 SettingsTile(headlineContent={Text(title,fontSize=16.sp,fontWeight=reviewWeight(FontWeight.SemiBold))},
 supportingContent=summary.takeIf{it.isNotBlank()}?.let{{Text(it,fontSize=14.sp,lineHeight=19.6.sp)}},
 leadingContent={Icon(icon,null,tint=MaterialTheme.colorScheme.primary)},trailingContent={Switch(value,null,Modifier.padding(horizontal=4.dp))},
 modifier=Modifier.reviewToggleable(value=value,change=change).padding(vertical=3.dp))
}
@Composable
fun ReviewAppearance(model:DemoModel,open:(String)->Unit) {
 val modes=listOf("system","light","dark")
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start=16.dp,end=16.dp,top=LocalDemoContentTop.current+12.dp,bottom=24.dp)) {
  dev.chengeeker.lucent.review.AppearanceModes(modes.indexOf(model.str("mode")).coerceAtLeast(0),{model.set("mode",modes[it])},model.bool("black"),{model.set("black",it)})
  Spacer(Modifier.height(16.dp))
  dev.chengeeker.lucent.review.AppearancePalette(model.bool("dynamic"),{model.set("dynamic",it)},model.number("seed").toInt(),{model.set("seed",it)})
  Spacer(Modifier.height(16.dp))
  dev.chengeeker.lucent.review.FontWeightSetting(model.bool("weightCustom"),model.number("weight").toInt()*100,"Lucent UI",hapticsEnabled=model.bool("haptics"),onReset={model.set("weightCustom",false)},onCommit={model.saveSettings(mapOf("weight" to it/100, "weightCustom" to true))})
  Spacer(Modifier.height(16.dp))
  dev.chengeeker.lucent.review.FontSizeSetting(model.number("font").toFloat(),onCommit={model.set("font",it.toDouble())})
  Spacer(Modifier.height(16.dp))
  SettingsSection("底部导航") {ReferenceToggle("使用悬浮底栏","居中显示悬浮式导航栏",Icons.Outlined.Dock,model.bool("floating")){model.set("floating",it)}}
  SettingsSection("显示效果") {
   ReferenceToggle("使用液态玻璃效果","控制液态玻璃控件；当前应用于悬浮底栏",Icons.Outlined.BlurOn,model.bool("glass")){model.set("glass",it)}
   if(model.bool("glass") && model.bool("floating")) dev.chengeeker.lucent.review.NavigationGlassChoice(model.bool("translucent"),hapticsEnabled=model.bool("haptics")){model.set("translucent",it)}
  }
  SettingsSection("应用图标") {LucentRow("自定义应用图标",model.str("icon"),Icons.Outlined.AppShortcut,onClick={open("icons")})}
  SettingsSection("屏幕显示") {LucentRow("屏幕帧率设置",model.str("refresh"),Icons.Outlined.Speed,onClick={open("refresh")})}
  SettingsSection("触感与震动") {ReferenceToggle("触感与震动反馈","点击、长按和完成指定手势时轻振一次",Icons.Outlined.Vibration,model.bool("haptics")){model.set("haptics",it)}}
 }
}

@Composable
fun ReviewCatalog(open:(String)->Unit) {
 PageList {
  item { SettingsSection("页面与交互") {
   patternRoutes.forEach {r->LucentRow(r.title,icon=when(r.id){"images"->Icons.Outlined.Image;"video"->Icons.Outlined.PlayCircle;"music"->Icons.Outlined.MusicNote;"article"->Icons.Outlined.Article;else->Icons.Outlined.Widgets},onClick={open(r.id)})}
   LucentRow("基础组件",icon=Icons.Outlined.Widgets,onClick={open("controls")})
   LucentRow("玻璃与动画",icon=Icons.Outlined.BlurOn,onClick={open("glass")})
  }}
  item { SettingsSection("通用功能扩展") {
   LucentRow("图片与视频设置",icon=Icons.Outlined.VideoSettings,onClick={open("media")})
   LucentRow("本地备份与迁移",icon=Icons.Outlined.Save,onClick={open("local")})
   LucentRow("网络设置",icon=Icons.Outlined.Wifi,onClick={open("network")})
   LucentRow("隐私与数据",icon=Icons.Outlined.Security,onClick={open("privacy")})
  }}
 }
}

// Review PostCard geometry, metadata and action row; the selected post owns every action.
@Composable
private fun ReferencePost(post: DemoPost, model: DemoModel, open: (String) -> Unit, onShare: () -> Unit, onMore: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val liked = model.postLikes[post.id] == true
    val tight = model.str("layout") == "紧凑"
    Surface(color = colors.surfaceContainerLowest, shape = RoundedCornerShape(22.dp), shadowElevation = 1.dp,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).fillMaxWidth()) {
        Column(Modifier.lucentClick { open("article:${post.id}") }
            .padding(horizontal = 14.dp, vertical = if (tight) 8.dp else 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (model.bool("avatar")) {
                    Box(Modifier.size(42.dp).clip(CircleShape).background(colors.primaryContainer), contentAlignment = Alignment.Center) {
                        Text(post.author.take(1), fontSize = 18.sp, color = colors.onPrimaryContainer)
                    }
                    Spacer(Modifier.width(10.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(post.author, fontSize = 15.sp, fontWeight = reviewWeight(FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (model.bool("time")) Text(post.dateLabel(model.bool("relativeTime")), fontSize = 11.5.sp, color = colors.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(post.body, fontSize = 16.sp, lineHeight = (16 * model.str("bodySpacing").toFloat()).sp,
                color = colors.onSurface, maxLines = 6, overflow = TextOverflow.Ellipsis)
            if (post.kind != PostKind.Text) {
                Spacer(Modifier.height(10.dp))
                PostMedia(post.kind, model.bool("imageRound"), onVideo = { open("video") })
            }
            Spacer(Modifier.height(12.dp))
            ReviewDivider()
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                ReferencePostAction(Icons.Rounded.Repeat, "转发", 0, colors.onSurfaceVariant, onShare)
                ReferencePostAction(Icons.Rounded.ChatBubbleOutline, "评论", post.seedComments + model.commentsFor(post.id).size, colors.onSurfaceVariant) {
                    open("comments:${post.id}")
                }
                ReferencePostAction(if (liked) Icons.Rounded.ThumbUp else Icons.Outlined.ThumbUp,
                    if (liked) "取消点赞" else "点赞", (if (post.isLocal) 0 else 12) + (if (liked) 1 else 0),
                    if (liked) colors.primary else colors.onSurfaceVariant) { model.postLikes[post.id] = !liked }
                ReferencePostAction(Icons.Rounded.MoreHoriz, "更多", 0, colors.onSurfaceVariant, onMore)
            }
        }
    }
}

@Composable
internal fun PostMedia(kind: PostKind, rounded: Boolean, onVideo: () -> Unit) {
    when (kind) {
        PostKind.Video -> Box(Modifier.fillMaxWidth().aspectRatio(16f/9f)
            .clip(RoundedCornerShape(if (rounded) 12.dp else 0.dp)).lucentClick(onClick = onVideo)) {
            AsyncImage("file:///android_asset/media/landscape-1.jpg", null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Icon(Icons.Rounded.PlayCircle, "播放视频", Modifier.align(Alignment.Center).size(48.dp), tint = Color.White)
        }
        PostKind.Images -> {
            val media = remember { listOf(galleryItems[0], galleryItems.last(), galleryItems[2]).map { it.media() } }
            val radius = if (rounded) 8.dp else 0.dp
            val origins = remember(radius) { media.indices.associateWith { dev.chengeeker.lucent.review.GalleryOrigin(radius) } }
            val openGallery = dev.chengeeker.lucent.review.LocalGalleryOpenFrom.current
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                media.forEachIndexed { i, item ->
                    GalleryThumbnail(item, origins.getValue(i), Modifier.weight(1f).aspectRatio(1f)) {
                        openGallery?.invoke(media, i, origins)
                    }
                }
            }
        }
        PostKind.Text -> Unit
    }
}

@Composable private fun ReferencePostAction(icon:androidx.compose.ui.graphics.vector.ImageVector,label:String,count:Int,color:Color,action:()->Unit) {
 Row(Modifier.clip(RoundedCornerShape(12.dp)).lucentClick(onClick=action).padding(horizontal=8.dp,vertical=9.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center) {
  Icon(icon,label,Modifier.size(18.dp),tint=color);Spacer(Modifier.width(5.dp));Text(if(count>0)count.toString() else label,fontSize=12.sp,color=color)
 }
}
@Composable
fun ReviewFeed(model: DemoModel, group: FeedGroup, list: LazyListState, open: (String) -> Unit) {
 var refresh by remember {mutableStateOf(false)}
 var selectedPost by remember { mutableStateOf<DemoPost?>(null) }
 val context = androidx.compose.ui.platform.LocalContext.current
 fun share(post: DemoPost) {
  val intent = android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain")
   .putExtra(android.content.Intent.EXTRA_TEXT, post.author + "\n" + post.body)
  runCatching { context.startActivity(android.content.Intent.createChooser(intent, "分享动态")) }
   .onFailure { model.report("无法分享", "当前设备没有可用的分享应用。") }
 }
 LaunchedEffect(refresh){if(refresh){kotlinx.coroutines.delay(500);refresh=false}}
 LucentRefresh(refresh,{refresh=true}) {
  val blocked = model.filterWords()
  val posts = model.posts.filter { group.matches(it) && blocked.none { word -> it.body.contains(word, true) } }
  LazyColumn(Modifier.fillMaxSize(), state = list, contentPadding = PaddingValues(top = LocalDemoContentTop.current + 4.dp, bottom = 104.dp)) {
   if (posts.isEmpty()) item { LucentEmpty("没有符合条件的动态", "可以切换分组或修改屏蔽词。") }
   items(posts, key = { it.id }) { post ->
    ReferencePost(post, model, open, onShare = { share(post) }, onMore = { selectedPost = post })
   }
  }
 }
 selectedPost?.let { post ->
  val title = post.title
  val bookmarked = model.isBookmarked(post.bookmarkKey)
  ModalBottomSheet(onDismissRequest={selectedPost=null}) {
   Column(Modifier.fillMaxWidth().padding(horizontal=16.dp).padding(bottom=24.dp)) {
    Text(title, Modifier.padding(16.dp), style=MaterialTheme.typography.titleMedium)
    LucentRow(if(bookmarked) "取消收藏" else "收藏", icon=if(bookmarked) Icons.Outlined.BookmarkRemove else Icons.Outlined.BookmarkBorder,
     onClick={selectedPost=null;model.toggleBookmark(post.bookmarkKey)})
    LucentRow("复制正文", icon=Icons.Outlined.ContentCopy, onClick={
     val clipboard=context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
     clipboard.setPrimaryClip(android.content.ClipData.newPlainText(title,post.body))
     selectedPost=null
     android.widget.Toast.makeText(context,"正文已复制",android.widget.Toast.LENGTH_SHORT).show()
    })
    LucentRow("分享", icon=Icons.Outlined.Share, onClick={selectedPost=null;share(post)})
   }
  }
 }
}
