package dev.chengeeker.lucent.demo

import org.junit.Assert.assertEquals
import org.junit.Test

class DemoPostsTest {
    @Test
    fun sampleTimelineCoversEveryGridCountAndKeepsCopyAndSavedKeysDistinct() {
        val mediaCounts = samplePosts.filter { it.kind == PostKind.Images }.map { it.media.size }.toSet()
        val mixedPost = samplePosts.single { post -> post.media.any { it.video.isNotBlank() } }
        val mixedVideo = mixedPost.media.single { it.video.isNotBlank() }

        assertEquals((1..9).toSet() + 12, mediaCounts)
        assertEquals(5, mixedPost.media.size)
        assertEquals(2, mixedPost.media.indexOf(mixedVideo))
        assertEquals(1, mixedPost.media.count { it.video.isNotBlank() })
        assertEquals(samplePosts.size, samplePosts.map { it.body }.toSet().size)
        assertEquals(
            listOf(
                "山间来信 · 动态 1", "生活记录 · 动态 2", "Lucent Design · 动态 3", "山间来信 · 动态 4",
                "生活记录 · 动态 5", "Lucent Design · 动态 6", "山间来信 · 动态 7", "生活记录 · 动态 8",
            ),
            samplePosts.take(8).map { it.bookmarkKey },
        )
    }
}
