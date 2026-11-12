@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package dev.chengeeker.lucent.demo
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.core.view.WindowCompat
import dev.chengeeker.lucent.*
import dev.chengeeker.lucent.review.PageStack
import dev.chengeeker.lucent.review.ReviewRoute
import dev.chengeeker.lucent.review.ReviewAlertDialog
import dev.chengeeker.lucent.review.reviewWeight
import dev.chengeeker.lucent.review.hapticClick
import dev.chengeeker.lucent.demo.data.DemoModel
import kotlinx.coroutines.launch

data class DemoRoute(val id: String, val title: String, val detail: String)

val settingRoutes =
    listOf(
        DemoRoute("appearance", "个性化", "主题、动态取色、字号字重、玻璃与动效"),
        DemoRoute("reading", "阅读与内容", "卡片布局、回复预览、过滤与浏览记录"),
        DemoRoute("media", "图片与视频", "播放偏好、画质、倍速与流量控制"),
        DemoRoute("storage", "存储与下载", "保存位置、下载任务与缓存清理"),
        DemoRoute("webdav", "WebDAV 备份", "连接测试、安全备份与加密全量恢复"),
        DemoRoute("local", "本地备份与迁移", "导入、导出、校验与恢复确认"),
        DemoRoute("notifications", "通知与后台", "系统授权与通知预览"),
        DemoRoute("network", "网络", "超时、HTTPS 连接诊断"),
        DemoRoute("privacy", "隐私与数据", "截图保护、历史记录与重置"),
        DemoRoute("account", "账号管理", "登录、凭据迁移与退出的演示流程"),
        DemoRoute("about", "关于与规范", "设计来源、组件契约与许可"),
    )
val patternRoutes =
    listOf(
        DemoRoute("article", "阅读与评论", "长内容、磨砂顶栏、回复、收藏与编辑"),
        DemoRoute("images", "图片浏览器", "共享位置过渡、缩放、长图与动图"),
        DemoRoute("video", "普通视频", "真实本地播放、画质、倍速和手势"),
        DemoRoute("music", "音乐与歌词", "队列、播放模式、迷你播放器与展开"),
        DemoRoute("drama", "剧集播放器", "定制模式：竖屏视频与选集"),
        DemoRoute("search", "搜索与消息", "搜索建议、结果、未读与会话"),
        DemoRoute("devices", "设备与传输", "设备卡片、连接、任务进度与取消"),
        DemoRoute("plugins", "插件与键盘", "启停、配置、导入流程与键盘预览"),
    )


val LocalDemoContentTop = staticCompositionLocalOf { 0.dp }

@Composable fun DemoApp(model:DemoModel) {
 val routes=remember{mutableStateListOf(ReviewRoute(0,"root"))}
 var nextKey by remember {mutableLongStateOf(1)}
 var tab by rememberSaveable {mutableIntStateOf(0)}
 fun open(name:String){if(routes.last().name!=name)routes.add(ReviewRoute(nextKey++,name))}
 fun back(){if(routes.size>1)routes.removeAt(routes.lastIndex)}
 val context=LocalContext.current;val activity=context.activity();val colors=MaterialTheme.colorScheme
 val drawer=rememberDrawerState(DrawerValue.Closed);val scope=rememberCoroutineScope()
 BackHandler(drawer.isOpen){scope.launch{drawer.close()}}
 PredictiveBackHandler(routes.size>1 && !drawer.isOpen){events->
  val route=routes.last()
  try { events.collect {if(routes.lastOrNull()===route)route.backProgress=it.progress};if(routes.lastOrNull()===route)back() }
  finally{route.backProgress=null}
 }
 val dark=colors.background.luminance()<.5f
 SideEffect {activity?.window?.let {w->WindowCompat.getInsetsController(w,w.decorView).apply{isAppearanceLightStatusBars=!dark;isAppearanceLightNavigationBars=!dark}}}
 DisposableEffect(model.bool("private")) {activity?.window?.let{if(model.bool("private"))it.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE) else it.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)};onDispose{}}
 LaunchedEffect(model.str("refresh")){activity?.window?.let{w->w.attributes=w.attributes.apply{preferredRefreshRate=model.str("refresh").filter{it.isDigit()}.toFloatOrNull()?:0f}}}
 LaunchedEffect(model.str("icon")) {
  val aliases=listOf("圆润" to "LauncherDefault","清浅" to "LauncherLight","深邃" to "LauncherDark")
  val selected=aliases.firstOrNull { it.first==model.str("icon") }?.second ?: "LauncherDefault"
  val pm=context.packageManager
  // Enable the new entry first so a failed update cannot remove every launcher entry.
  runCatching {
   val next=android.content.ComponentName(context,context.packageName+"."+selected)
   if(pm.getComponentEnabledSetting(next)!=android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
    pm.setComponentEnabledSetting(next,android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,android.content.pm.PackageManager.DONT_KILL_APP)
   aliases.filter{it.second!=selected}.forEach{(_,name)->
    val component=android.content.ComponentName(context,context.packageName+"."+name)
    if(pm.getComponentEnabledSetting(component)!=android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED)
     pm.setComponentEnabledSetting(component,android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,android.content.pm.PackageManager.DONT_KILL_APP)
   }
  }.onFailure { model.report("图标切换失败", "当前桌面不支持切换，请稍后重试。") }
 }
 val tabStates=rememberSaveableStateHolder()
 val titles=(settingRoutes+patternRoutes).associate{it.id to it.title}+mapOf("webdav" to "WebDAV云端备份与恢复","reading" to "内容样式","storage" to "存储设置","cache" to "缓存管理","notifications" to "订阅消息提醒","icons" to "自定义应用图标","refresh" to "屏幕帧率设置","controls" to "基础组件","glass" to "玻璃与动画")
 GalleryHost(model) {
 BoxWithConstraints {
 ModalNavigationDrawer(drawerState=drawer,gesturesEnabled=routes.size==1,drawerContent={
  ModalDrawerSheet(Modifier.width(minOf(maxWidth*.62f,280.dp)),drawerContainerColor=colors.background) {
   Column(Modifier.padding(20.dp).statusBarsPadding()) {
    Box(Modifier.size(56.dp).clip(CircleShape).background(colors.primaryContainer),contentAlignment=Alignment.Center){Text("L",color=colors.onPrimaryContainer,fontSize=24.sp,fontWeight=FontWeight.Bold)}
    Spacer(Modifier.height(12.dp));Text("Lucent UI",fontWeight=FontWeight.Bold,fontSize=18.sp);Text("本地演示",color=colors.onSurfaceVariant,fontSize=13.sp)
   }
   listOf("阅读与评论" to "article","收藏" to "reading","搜索" to "search","图片浏览器" to "images","普通视频" to "video","设计规范" to "about").forEach{(title,id)->NavigationDrawerItem(label={Text(title)},selected=false,onClick=hapticClick{scope.launch{drawer.close()};open(id)})}
  }
 }) {
 PageStack(routes.toList()){route->
  val root=route.name=="root"
  val landscape=LocalConfiguration.current.orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE
  val hideVideoBar=route.name=="video" && landscape
  val top=if(root && tab==2 || hideVideoBar)0.dp else WindowInsets.statusBars.asPaddingValues().calculateTopPadding()+56.dp
  CompositionLocalProvider(LocalDemoContentTop provides top,dev.chengeeker.lucent.review.LocalContentTop provides top) {
   BackdropHost(overlay={
    if(root){
     if(tab==0) ReferenceFeedHeader({scope.launch{drawer.open()}},{open("search")},{open("article")})
     else if(tab==1) LucentHeader("组件与功能")
     Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
      LucentNavigation(listOf(LucentDestination("时间线",Icons.Outlined.DynamicFeed,Icons.Rounded.DynamicFeed),LucentDestination("组件",Icons.Outlined.LocalFireDepartment,Icons.Rounded.LocalFireDepartment),LucentDestination("设置",Icons.Outlined.Settings,Icons.Rounded.Settings)),tab,{tab=it})
     }
    }else if(!hideVideoBar)LucentHeader(titles[route.name]?:route.name,::back)
   }) {
    Box(Modifier.fillMaxSize().then(if(!root && route.name!="video") Modifier.navigationBarsPadding() else Modifier).clipToBounds()) {
    if(root)tabStates.SaveableStateProvider(tab){when(tab){0->ReviewFeed(model,::open);1->ReviewCatalog(::open);else->ReviewSettingsRoot(model,::open)}}
    else when(route.name){
     "appearance"->ReviewAppearance(model,::open)
     "icons","refresh"->DevicePreviewSettings(route.name,model)
     "controls"->ComponentsPage(model,::open)
     "glass"->GlassLab(model)
     "cache"->SettingsPage("cache",model,::open)
     "reading","media","storage","webdav","local","notifications","network","privacy","account","about"->SettingsPage(route.name,model,::open)
     else->PatternPage(route.name,model,::open)
    }
    }
   }
  }
 }
 }
 }
 }
 model.busy?.let{title->ReviewAlertDialog(onDismissRequest={},title={Text(title)},text={Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)){CircularProgressIndicator(Modifier.size(24.dp));Text("正在处理，请稍候…")}},confirmButton={TextButton(onClick=hapticClick(model::cancel)){Text("取消")}})}
 model.result?.let{(title,body)->LucentResult(title,body,model::dismiss)}
}

@Composable private fun ReferenceFeedHeader(drawer:()->Unit,search:()->Unit,publish:()->Unit) {
 Box(Modifier.frostedBackdrop(MaterialTheme.colorScheme.background)) {
 TopAppBar(title={Row(Modifier.padding(horizontal=8.dp,vertical=4.dp),verticalAlignment=Alignment.CenterVertically){Text("最新动态",fontSize=18.sp,fontWeight=reviewWeight(FontWeight.Bold));Spacer(Modifier.width(4.dp));IconButton(onClick=hapticClick(search),modifier=Modifier.size(32.dp)){Icon(Icons.Rounded.ArrowDropDown,"浏览内容",Modifier.size(24.dp))}}},
 navigationIcon={Box(Modifier.size(48.dp).padding(start=12.dp),contentAlignment=Alignment.Center){Box(Modifier.size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer).lucentClick(onClick=drawer),contentAlignment=Alignment.Center){Icon(Icons.Rounded.Person,"打开侧栏",Modifier.size(20.dp),tint=MaterialTheme.colorScheme.onPrimaryContainer)}}},
 actions={IconButton(onClick=hapticClick(publish)){Icon(Icons.Outlined.Edit,"编辑示例")};IconButton(onClick=hapticClick(search)){Icon(Icons.Rounded.Search,"搜索")}},expandedHeight=56.dp,colors=TopAppBarDefaults.topAppBarColors(containerColor=Color.Transparent))
 }
}

@Composable fun PageList(bottom:Int=104,content:LazyListScope.()->Unit) {
 LazyColumn(Modifier.fillMaxSize().imePadding(),contentPadding=PaddingValues(start=16.dp,end=16.dp,top=LocalDemoContentTop.current+12.dp,bottom=WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()+bottom.dp),verticalArrangement=Arrangement.spacedBy(0.dp),content=content)
}

@Composable private fun DevicePreviewSettings(page:String,model:DemoModel) {
 PageList(bottom=26) {
  item {dev.chengeeker.lucent.review.SettingsCard {
   val choices=if(page=="icons")listOf("圆润","清浅","深邃") else listOf("系统默认","60 Hz","90 Hz","120 Hz")
   val key=if(page=="icons")"icon" else "refresh"
   choices.forEachIndexed{index,value->
    dev.chengeeker.lucent.review.SettingsTile(headlineContent={Text(value,fontSize=16.sp,fontWeight=reviewWeight(FontWeight.SemiBold))},
     leadingContent=if(page=="icons"){{androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(listOf(R.drawable.ic_launcher,R.drawable.ic_launcher_light,R.drawable.ic_launcher_dark)[index]),null,Modifier.size(48.dp))}}else null,
     trailingContent={RadioButton(model.str(key)==value,null)},modifier=Modifier.lucentClick{model.set(key,value)})
   }
  }}
  if(page=="icons") item {Text("选择后会更新系统桌面图标，部分桌面需要稍候刷新。",Modifier.padding(16.dp),color=MaterialTheme.colorScheme.onSurfaceVariant)}
 }
}


