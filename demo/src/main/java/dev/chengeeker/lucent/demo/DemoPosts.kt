package dev.chengeeker.lucent.demo

import java.text.DateFormat
import java.util.Date
import org.json.JSONArray

internal const val PostBodyLimit = 4000
internal const val LocalPostLimit = 100

enum class PostKind { Images, Video, Text }

enum class FeedGroup(val title: String, private val kind: PostKind?) {
    Latest("最新动态", null),
    Images("图片动态", PostKind.Images),
    Video("视频动态", PostKind.Video),
    Text("文字动态", PostKind.Text);

    fun matches(post: DemoPost) = kind == null || kind == post.kind
}

data class DemoPost(
    val id: String,
    val author: String,
    val body: String,
    val kind: PostKind,
    val title: String,
    val bookmarkKey: String = title,
    val seedComments: Int = 8,
    val created: Long = 0L,
) {
    val isLocal get() = id.startsWith("local-")

    fun dateLabel(relative: Boolean): String {
        if (!isLocal) return if (relative) "刚刚 · Lucent UI" else "2026-11-12 · Lucent UI"
        val elapsed = (System.currentTimeMillis() - created).coerceAtLeast(0L) / 60_000
        return when {
            relative && elapsed == 0L -> "刚刚 · 本地发布"
            relative && elapsed < 60 -> "$elapsed 分钟前 · 本地发布"
            else -> DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(created))
        }
    }
}

data class DemoComment(val id: String, val author: String, val body: String, val parent: String? = null)

private val authors = listOf("山间来信", "生活记录", "Lucent Design")
private val bodies = listOf(
    "在路上，记录一些普通又值得记住的片刻。光线、山色和远处的风。",
    "一段短片，一次慢下来的机会。点开视频可以体验播放、画质、倍速和全屏。",
    "写下一点想法，再看看大家的回复。正文、评论与顶栏共享同一份背景。\n这是一条本地示例动态。",
)
internal val samplePosts = List(8) { index ->
    val type = index % 3
    DemoPost("sample-${index + 1}", authors[type], bodies[type], PostKind.entries[type],
        "${authors[type]} · 动态 ${index + 1}")
}

internal val sampleArticle = DemoPost(
    "reading-sample", "Lucent Design",
    List(5) { index ->
        "${index + 1}. 清晰优先于装饰。让重要的内容保持清楚，用空间和容器区分信息层级。玻璃在滚动时表达前后关系；动画负责解释位置变化。设置、阅读和媒体播放遵循同一套规则。"
    }.joinToString("\n\n"),
    PostKind.Images, "流动的边界", seedComments = 30,
)

internal fun readLocalPosts(records: JSONArray?): List<DemoPost> {
    if (records == null) return emptyList()
    return (records.length() - 1 downTo 0).mapNotNull { index ->
        val record = records.optJSONObject(index) ?: return@mapNotNull null
        val id = record.optString("id")
        val body = record.optString("body")
        if (!id.startsWith("local-") || body.isBlank()) return@mapNotNull null
        val author = record.optString("author").ifBlank { "我" }
        DemoPost(id, author, body, PostKind.Text, "$author · ${body.lineSequence().first().take(30)}",
            bookmarkKey = id, seedComments = 0, created = record.optLong("created"))
    }
}
