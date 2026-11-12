@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package dev.chengeeker.lucent.demo

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkRemove
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chengeeker.lucent.*
import dev.chengeeker.lucent.demo.data.DemoModel
import dev.chengeeker.lucent.review.GalleryOrigin
import dev.chengeeker.lucent.review.LocalGalleryOpenFrom
import dev.chengeeker.lucent.review.LocalPageActive
import kotlinx.coroutines.delay

@Composable
internal fun PostComposer(model: DemoModel, close: (published: Boolean) -> Unit) {
    var text by rememberSaveable { mutableStateOf(model.postDraft) }
    var saving by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val full = model.posts.count { it.isLocal } >= LocalPostLimit
    fun submit(publish: Boolean) {
        if (saving) return
        keyboard?.hide()
        if (!publish && text == model.postDraft) {
            close(false)
            return
        }
        saving = true
        val finished: (Boolean) -> Unit = { ok ->
            saving = false
            if (ok) close(publish)
        }
        if (publish) model.publishPost(text, finished) else model.savePostDraft(text, finished)
    }
    BackHandler(LocalPageActive.current) { submit(false) }
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 56.dp
    BackdropHost(overlay = {
        LucentHeader("写帖子", { submit(false) }) {
            TextButton(onClick = dev.chengeeker.lucent.review.hapticClick { submit(true) },
                enabled = !saving && !full && text.isNotBlank()) { Text("发布") }
        }
    }) {
        CompositionLocalProvider(LocalDemoContentTop provides top) {
            PageList(bottom = 24) {
                item {
                    OutlinedTextField(
                        text, { text = it.take(PostBodyLimit) },
                        Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        enabled = !saving, label = { Text("分享新鲜事") }, minLines = 5,
                        supportingText = { Text(if (full) "本机已达到 $LocalPostLimit 条动态" else "${text.length} / $PostBodyLimit") },
                    )
                    Text("发布到本机时间线", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    LucentAction("保存草稿", { submit(false) }, enabled = !saving && (text.isNotBlank() || model.postDraft.isNotEmpty()), primary = false)
                }
            }
        }
    }
}

@Composable
internal fun SavedPostsPage(model: DemoModel, favorites: Boolean, open: (String) -> Unit) {
    val posts = (model.posts + sampleArticle).associateBy { it.bookmarkKey }
    val keys = model.str(if (favorites) "bookmarks" else "historyItems").lineSequence()
        .filter(String::isNotBlank).distinct().toList()
    PageList(bottom = 24) {
        if (keys.isEmpty()) item {
            LucentEmpty(if (favorites) "暂无收藏" else "暂无浏览记录",
                if (favorites) "在动态菜单或详情中选择收藏。" else "打开动态详情后会显示在这里。")
        }
        items(keys, key = { it }) { key ->
            val post = posts[key]
            LucentCard {
                LucentRow(
                    post?.title ?: "本机找不到这条动态",
                    post?.body?.lineSequence()?.first()?.take(60),
                    onClick = post?.let { { open("article:${it.id}") } },
                    trailing = if (favorites) {{
                        LucentIcon(Icons.Outlined.BookmarkRemove, "取消收藏") { model.toggleBookmark(key) }
                    }} else null,
                )
            }
        }
    }
}

@Composable
internal fun ArticlePage(model: DemoModel, post: DemoPost?, jumpToComments: Boolean, onVideo: () -> Unit) {
    if (post == null) {
        PageList { item { LucentEmpty("动态不可用", "返回时间线查看其他动态。") } }
        return
    }
    val list = rememberLazyListState()
    val sample = post.id == sampleArticle.id
    var limit by rememberSaveable { mutableIntStateOf(10) }
    var refreshing by remember { mutableStateOf(false) }
    var composerOpen by remember { mutableStateOf(false) }
    var target by remember { mutableStateOf<DemoComment?>(null) }
    val stored = model.commentsFor(post.id)
    val blocked = model.filterWords()
    val visibleSeeds = List(minOf(post.seedComments, limit)) { index ->
        DemoComment("seed-$index", "读者 ${index + 1}", if (sample)
            "评论也应进入磨砂背景采样，滚到顶栏时不会露出清晰原字。"
        else "关于「${post.title}」的本地示例评论。")
    }
    val comments = (stored.asReversed().filter { it.parent == null } + visibleSeeds)
        .filter { comment -> blocked.none { comment.body.contains(it, true) } }
    LaunchedEffect(post.id, jumpToComments) {
        model.recordVisit(post.bookmarkKey)
        if (jumpToComments) list.scrollToItem(2)
    }
    LaunchedEffect(refreshing) {
        if (refreshing) { delay(500); limit = 10; refreshing = false }
    }
    fun compose(reply: DemoComment?) { target = reply; composerOpen = true }
    LucentRefresh(refreshing, { refreshing = true }) {
        LazyColumn(Modifier.fillMaxSize().imePadding(), state = list,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = LocalDemoContentTop.current + 12.dp, bottom = 24.dp)) {
            item {
                Column(Modifier.padding(vertical = 12.dp)) {
                    Text(post.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    if (model.bool("avatar")) LucentRow(post.author, icon = Icons.Rounded.AccountCircle)
                    else Text(post.author, Modifier.padding(vertical = 8.dp))
                    if (model.bool("time")) Text(post.dateLabel(model.bool("relativeTime")), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    if (sample) {
                        val openGallery = LocalGalleryOpenFrom.current
                        val radius = if (model.bool("imageRound")) 24.dp else 0.dp
                        val origin = remember(radius) { GalleryOrigin(radius) }
                        val media = remember { listOf(galleryItems[0].media()) }
                        GalleryThumbnail(media[0], origin, Modifier.fillMaxWidth().height(220.dp)) {
                            openGallery?.invoke(media, 0, mapOf(0 to origin))
                        }
                    } else PostMedia(post.kind, model.bool("imageRound"), onVideo)
                    Text(post.body, Modifier.padding(vertical = 12.dp),
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = (16 * model.str("bodySpacing").toFloat()).sp))
                }
            }
            item {
                val bookmarked = model.isBookmarked(post.bookmarkKey)
                LucentAction(if (bookmarked) "取消收藏" else "收藏",
                    { model.toggleBookmark(post.bookmarkKey) }, primary = bookmarked)
            }
            item {
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("评论 ${post.seedComments + stored.size}", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    LucentAction("写评论", { compose(null) }, primary = false)
                }
            }
            if (comments.isEmpty()) item { LucentEmpty("暂无可见评论", "可以写下第一条评论。") }
            items(comments, key = { it.id }) { comment ->
                val replies = stored.filter { it.parent == comment.id && blocked.none { word -> it.body.contains(word, true) } }
                CommentRow(comment, replies, model.bool("preview"), model.bool("avatar")) { compose(comment) }
            }
            if (limit < post.seedComments) item {
                LucentAction("加载更多", { limit += 10 }, Modifier.fillMaxWidth(), primary = false)
            }
        }
    }
    if (composerOpen) {
        val parent = target
        val draftKey = if (parent == null) post.id else "${post.id}:${parent.id}"
        var draft by rememberSaveable(draftKey) { mutableStateOf(model.commentDraft(draftKey)) }
        fun saveAndClose() { model.saveCommentDraft(draftKey, draft); composerOpen = false }
        ModalBottomSheet(onDismissRequest = ::saveAndClose) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(16.dp)) {
                Text(if (parent == null) "发表评论" else "回复 ${parent.author}", style = MaterialTheme.typography.titleLarge)
                if (parent != null) {
                    Text(parent.body, Modifier.padding(vertical = 8.dp))
                    stored.filter { it.parent == parent.id && blocked.none { word -> it.body.contains(word, true) } }
                        .forEach { Text("${it.author}：${it.body}", Modifier.padding(vertical = 4.dp)) }
                }
                OutlinedTextField(draft, { draft = it.take(PostBodyLimit) },
                    Modifier.fillMaxWidth().padding(vertical = 12.dp), label = { Text("想说点什么") }, minLines = 2)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LucentAction("保存草稿", ::saveAndClose, primary = false)
                    LucentAction(if (parent == null) "发表评论" else "发送回复", {
                        model.addComment(post.id, draft, parent?.id)
                        model.saveCommentDraft(draftKey, "")
                        composerOpen = false
                    }, enabled = draft.isNotBlank())
                }
            }
        }
    }
}

@Composable
private fun CommentRow(comment: DemoComment, replies: List<DemoComment>, preview: Boolean, avatar: Boolean, reply: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var liked by rememberSaveable { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().lucentClick(onClick = reply).padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (avatar) Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(colors.primaryContainer), contentAlignment = Alignment.Center) {
                Text(comment.author.take(1), fontWeight = FontWeight.Bold)
            }
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(comment.author, Modifier.weight(1f), fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = dev.chengeeker.lucent.review.hapticClick { liked = !liked }) {
                    Icon(if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        if (liked) "取消赞评论" else "赞评论", tint = if (liked) Color(0xFFEC407A) else colors.onSurfaceVariant)
                }
            }
            Text(comment.body, fontSize = 14.sp, lineHeight = 19.6.sp)
            if (replies.isNotEmpty()) {
                if (preview) Text("${replies.last().author}：${replies.last().body}",
                    Modifier.fillMaxWidth().padding(top = 8.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                        .background(colors.surfaceContainerHighest.copy(alpha = .4f)).padding(8.dp), fontSize = 12.5.sp)
                Text("共${replies.size}条回复", Modifier.padding(vertical = 8.dp), color = colors.primary, fontSize = 12.5.sp)
            }
        }
    }
}
