package dev.chengeeker.lucent.demo.data

import org.junit.Assert.*
import org.junit.Test

class SavedTitlesTest {
    @Test fun togglingOnePostPreservesOtherPostsAndLegacyArticle() {
        val original = "流动的边界\n山间来信 · 动态 1"
        val added = toggleSavedTitle(original, "生活记录 · 动态 2")
        assertEquals("$original\n生活记录 · 动态 2", added)
        assertEquals(original, toggleSavedTitle(added, "生活记录 · 动态 2"))
        assertEquals("山间来信 · 动态 1", toggleSavedTitle(original, "流动的边界"))
    }
    @Test fun exactTitlesDoNotMatchOtherPostsWithSimilarNames() {
        assertEquals("动态 11\n动态 1", toggleSavedTitle("动态 11", "动态 1"))
        assertEquals("", toggleSavedTitle("动态 1\n动态 1", "动态 1"))
    }
    @Test(expected = IllegalArgumentException::class) fun rejectMultilineIdentity() {
        toggleSavedTitle("保留原收藏", "不合法\n标题")
    }
}
