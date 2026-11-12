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
fun ReviewSettingsRoot(model:DemoModel,open:(String)->Unit) {
 val top=WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
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
   LucentRow("关于 Lucent UI","版本 0.2.1",Icons.Outlined.Info,onClick={open("about")})
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
  dev.chengeeker.lucent.review.FontWeightSetting(model.bool("weightCustom"),model.number("weight").toInt()*100,"Lucent UI",hapticsEnabled=model.bool("haptics"),onReset={model.set("weightCustom",false)},onCommit={model.set("weight",it/100);model.set("weightCustom",true)})
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

// Review PostCard geometry, metadata and action row; body is an application-owned slot.
@Composable
fun ReferencePost(author:String,text:String,index:Int=0,detail:Boolean=false,onOpen:()->Unit,onVideo:()->Unit,onShare:()->Unit,onMore:()->Unit) {
 val colors=MaterialTheme.colorScheme
 val media = remember { listOf(galleryItems[0], galleryItems.last(), galleryItems[2]).map { it.media() } }
 val origins = remember { media.indices.associateWith { dev.chengeeker.lucent.review.GalleryOrigin(8.dp) } }
 val openGallery = dev.chengeeker.lucent.review.LocalGalleryOpenFrom.current
 var liked by remember { mutableStateOf(false) }
 Surface(color=colors.surfaceContainerLowest,shape=RoundedCornerShape(22.dp),shadowElevation=if(detail)0.dp else 1.dp,
  modifier=(if(detail)Modifier else Modifier.padding(horizontal=8.dp,vertical=4.dp)).fillMaxWidth()) {
  Column(Modifier.lucentClick(enabled=!detail,onClick=onOpen).padding(start=14.dp,end=14.dp,top=14.dp,bottom=10.dp)) {
   if(detail)Spacer(Modifier.height(24.dp))
   Row(verticalAlignment=Alignment.CenterVertically) {
    Box(Modifier.size(42.dp).clip(CircleShape).background(colors.primaryContainer),contentAlignment=Alignment.Center) {Text(author.take(1),fontSize=18.sp,color=colors.onPrimaryContainer)}
    Column(Modifier.weight(1f).padding(start=10.dp)) {
     Text(author,fontSize=15.sp,fontWeight=reviewWeight(FontWeight.Bold),maxLines=1,overflow=TextOverflow.Ellipsis)
     Spacer(Modifier.height(2.dp))
     Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {Text("刚刚",fontSize=11.5.sp,color=colors.onSurfaceVariant);Text("Lucent UI",fontSize=11.5.sp,color=colors.onSurfaceVariant)}
    }
   }
   Spacer(Modifier.height(10.dp))
   Text(text,fontSize=16.sp,lineHeight=24.sp,color=colors.onSurface)
   Spacer(Modifier.height(10.dp))
   if(index%3==1) {
    Box(Modifier.fillMaxWidth().aspectRatio(16f/9f).clip(RoundedCornerShape(12.dp)).lucentClick(onClick=onVideo)) {
     AsyncImage("file:///android_asset/media/landscape-1.jpg","播放视频",Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
     Icon(Icons.Rounded.PlayCircle,"播放视频",Modifier.align(Alignment.Center).size(48.dp),tint=Color.White)
    }
   } else if(index%3==0) {
    Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) { repeat(3) { i ->
     GalleryThumbnail(media[i], origins.getValue(i), Modifier.weight(1f).aspectRatio(1f)) {
      openGallery?.invoke(media, i, origins)
     }
    }}
   }
   if(!detail) {
    Spacer(Modifier.height(12.dp));ReviewDivider();Spacer(Modifier.height(4.dp))
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceAround,verticalAlignment=Alignment.CenterVertically) {
     ReferencePostAction(Icons.Rounded.Repeat,"转发",2,colors.onSurfaceVariant,onShare)
     ReferencePostAction(Icons.Rounded.ChatBubbleOutline,"评论",8,colors.onSurfaceVariant,onOpen)
     ReferencePostAction(if(liked)Icons.Rounded.ThumbUp else Icons.Outlined.ThumbUp,"点赞",if(liked)13 else 12,if(liked)colors.primary else colors.onSurfaceVariant){liked=!liked}
     ReferencePostAction(Icons.Rounded.MoreHoriz,"更多",0,colors.onSurfaceVariant,onMore)
    }
   }
  }
 }
}
@Composable private fun ReferencePostAction(icon:androidx.compose.ui.graphics.vector.ImageVector,label:String,count:Int,color:Color,action:()->Unit) {
 Row(Modifier.clip(RoundedCornerShape(12.dp)).lucentClick(onClick=action).padding(horizontal=8.dp,vertical=9.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center) {
  Icon(icon,label,Modifier.size(18.dp),tint=color);Spacer(Modifier.width(5.dp));Text(if(count>0)count.toString() else label,fontSize=12.sp,color=color)
 }
}
private val feedAuthors = listOf("山间来信", "生活记录", "Lucent Design")
private val feedBodies = listOf("在路上，记录一些普通又值得记住的片刻。光线、山色和远处的风。",
 "一段短片，一次慢下来的机会。点开视频可以体验播放、画质、倍速和全屏。",
 "写下一点想法，再看看大家的回复。正文、评论与顶栏共享同一份背景。\n这是一条本地示例动态。")

@Composable fun ReviewFeed(model:DemoModel,open:(String)->Unit) {
 var refresh by remember {mutableStateOf(false)}
 var selectedPost by remember { mutableStateOf<Int?>(null) }
 val context = androidx.compose.ui.platform.LocalContext.current
 fun share(index: Int) {
  val intent = android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain")
   .putExtra(android.content.Intent.EXTRA_TEXT, feedAuthors[index%3] + "\n" + feedBodies[index%3])
  runCatching { context.startActivity(android.content.Intent.createChooser(intent, "分享动态")) }
   .onFailure { model.report("无法分享", "当前设备没有可用的分享应用。") }
 }
 LaunchedEffect(refresh){if(refresh){kotlinx.coroutines.delay(500);refresh=false}}
 LucentRefresh(refresh,{refresh=true}) {
  LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(top=LocalDemoContentTop.current+4.dp,bottom=104.dp)) {
   items(8, key={it}) { i -> ReferencePost(feedAuthors[i%3], feedBodies[i%3], i,
    onOpen={open("article")}, onVideo={open("video")}, onShare={share(i)}, onMore={selectedPost=i}) }
  }
 }
 selectedPost?.let { index ->
  val title = "${feedAuthors[index%3]} · 动态 ${index+1}"
  val bookmarked = model.isBookmarked(title)
  ModalBottomSheet(onDismissRequest={selectedPost=null}) {
   Column(Modifier.fillMaxWidth().padding(horizontal=16.dp).padding(bottom=24.dp)) {
    Text(title, Modifier.padding(16.dp), style=MaterialTheme.typography.titleMedium)
    LucentRow(if(bookmarked) "取消收藏" else "收藏", icon=if(bookmarked) Icons.Outlined.BookmarkRemove else Icons.Outlined.BookmarkBorder,
     onClick={selectedPost=null;model.toggleBookmark(title)})
    LucentRow("复制正文", icon=Icons.Outlined.ContentCopy, onClick={
     val clipboard=context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
     clipboard.setPrimaryClip(android.content.ClipData.newPlainText(title,feedBodies[index%3]))
     selectedPost=null
     android.widget.Toast.makeText(context,"正文已复制",android.widget.Toast.LENGTH_SHORT).show()
    })
    LucentRow("分享", icon=Icons.Outlined.Share, onClick={selectedPost=null;share(index)})
   }
  }
 }
}
