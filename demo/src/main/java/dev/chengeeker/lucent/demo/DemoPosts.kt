package dev.chengeeker.lucent.demo

import dev.chengeeker.lucent.GalleryItem
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
    val media: List<GalleryItem> = emptyList(),
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

private fun sampleImages(count: Int, start: Int) = List(count) { index ->
    val item = galleryItems[(start + index * 3) % galleryItems.size]
    item.copy(description = item.description + " · 第 " + (index + 1) + " 张")
}

internal val samplePosts = listOf(
    DemoPost("sample-1", "山间来信", "沿着旧栈道走到观景台，云层刚好散开。三张照片从山脚拍到山顶，风比想象中大。",
        PostKind.Images, "山间来信 · 动态 1", bookmarkKey = "山间来信 · 动态 1", seedComments = 8, media = sampleImages(3, 4)),
    DemoPost("sample-2", "生活记录", "清晨的雾在河面上慢慢散开，留了一段短片。点开可体验播放、倍速和全屏。",
        PostKind.Video, "生活记录 · 动态 2", bookmarkKey = "生活记录 · 动态 2", seedComments = 17),
    DemoPost("sample-3", "Lucent Design", "最近重新开始步行通勤。路程没有变短，但注意到的店铺和树比以前多了。",
        PostKind.Text, "Lucent Design · 动态 3", bookmarkKey = "Lucent Design · 动态 3", seedComments = 2),
    DemoPost("sample-4", "山间来信", "天气转晴前后各拍了两张，颜色变化比预报里写的更快。",
        PostKind.Images, "山间来信 · 动态 4", bookmarkKey = "山间来信 · 动态 4", seedComments = 14, media = sampleImages(4, 0)),
    DemoPost("sample-5", "生活记录", "第一次把做早餐的过程剪成短片，成品比过程简单得多。",
        PostKind.Video, "生活记录 · 动态 5", bookmarkKey = "生活记录 · 动态 5", seedComments = 5),
    DemoPost("sample-6", "Lucent Design", "周末试做了一份新菜谱。火候还要练，记录下来下次少走弯路。",
        PostKind.Text, "Lucent Design · 动态 6", bookmarkKey = "Lucent Design · 动态 6", seedComments = 0),
    DemoPost("sample-7", "山间来信", "绕湖一圈遇到七种不同的光线，最后一张是回程时的晚霞。",
        PostKind.Images, "山间来信 · 动态 7", bookmarkKey = "山间来信 · 动态 7", seedComments = 23, media = sampleImages(7, 1)),
    DemoPost("sample-8", "生活记录", "雨点落在玻璃上的声音比想象中清楚，拍了一小段留作天气记录。",
        PostKind.Video, "生活记录 · 动态 8", bookmarkKey = "生活记录 · 动态 8", seedComments = 11),
    DemoPost("sample-9", "城市拾光", "雨停后巷口的招牌映在积水里，单张照片保留了当时的构图。",
        PostKind.Images, "雨后的巷口", seedComments = 3, media = sampleImages(1, 6)),
    DemoPost("sample-10", "周末手记", "从出门前到抵达公园，两张照片记下了这段短途散步。",
        PostKind.Images, "公园散步", seedComments = 6, media = sampleImages(2, 4)),
    DemoPost("sample-11", "生活记录", "整理书桌前后和收纳细节，五张图里也放进一张动图和一张长图。",
        PostKind.Images, "桌面整理", seedComments = 18, media = sampleImages(5, 3)),
    DemoPost("sample-12", "山间来信", "这次按路线顺序整理了六张照片，长图和动图也会显示对应标记。",
        PostKind.Images, "沿河步道", seedComments = 9, media = sampleImages(6, 2)),
    DemoPost("sample-13", "城市拾光", "从早市走到河边，八张照片按拍摄顺序组成一组。",
        PostKind.Images, "周末早市", seedComments = 4, media = sampleImages(8, 0)),
    DemoPost("sample-14", "周末手记", "一次短途旅行挑出九张照片，列表按三列排成完整九宫格。",
        PostKind.Images, "九张旅行记录", seedComments = 32, media = sampleImages(9, 1)),
    DemoPost("sample-15", "Lucent Design", "十二张图片组成一组，时间线显示前九张并提示剩余数量，点开可浏览完整相册。",
        PostKind.Images, "十二张图片与剩余提示", seedComments = 26, media = sampleImages(12, 2)),
    DemoPost("sample-16", "城市拾光", "傍晚沿河散步时拍了四张照片，也留了一段短片记录水面和沿途声音。",
        PostKind.Images, "照片与短片", seedComments = 12, media = sampleImages(2, 1) + GalleryItem(
            "file:///android_asset/media/landscape-1.jpg", "河面短片封面",
            video = "asset:///media/lucent-720.mp4", duration = "00:24") + sampleImages(2, 4)),
)

internal val sampleArticle = DemoPost(
    "reading-sample", "Lucent Design",
    List(5) { index ->
        (index + 1).toString() + ". 清晰优先于装饰。让重要的内容保持清楚，用空间和容器区分信息层级。玻璃在滚动时表达前后关系；动画负责解释位置变化。设置、阅读和媒体播放遵循同一套规则。"
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
        DemoPost(id, author, body, PostKind.Text, author + " · " + body.lineSequence().first().take(30),
            bookmarkKey = id, seedComments = 0, created = record.optLong("created"))
    }
}
