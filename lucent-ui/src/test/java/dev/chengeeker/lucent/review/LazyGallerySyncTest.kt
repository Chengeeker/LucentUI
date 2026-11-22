package dev.chengeeker.lucent.review

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LazyGallerySyncTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun thumbnailsMoveDuringPartialMainPageScroll() {
        lateinit var pager: PagerState
        lateinit var scope: CoroutineScope
        compose.setContent {
            pager = rememberPagerState { 3 }
            scope = rememberCoroutineScope()
            Column {
                HorizontalPager(pager, Modifier.fillMaxWidth().height(120.dp)) { }
                GalleryThumbnails(List(3) { GalleryMedia("") }, pager, Modifier.fillMaxWidth()) { }
            }
        }
        val image = compose.onNodeWithContentDescription("第 1 张图片")
        val start = image.getUnclippedBoundsInRoot().left.value
        compose.runOnIdle { scope.launch { pager.scrollToPage(0, .3f) } }
        compose.waitForIdle()
        val end = image.getUnclippedBoundsInRoot().left.value
        assertTrue("Thumbnail should move before the page settles", start - end > 5f)
        compose.runOnIdle { scope.launch { pager.scrollToPage(1, -.3f) } }
        compose.waitForIdle()
        val beyondMidpoint = image.getUnclippedBoundsInRoot().left.value
        assertTrue("Thumbnail should keep moving across the page midpoint", end - beyondMidpoint > 5f)
    }

    @Test fun draggingThumbnailStripMovesMainPage() {
        lateinit var pager: PagerState
        compose.setContent {
            pager = rememberPagerState { 4 }
            Column {
                HorizontalPager(pager, Modifier.fillMaxWidth().height(120.dp)) { }
                GalleryThumbnails(List(4) { GalleryMedia("") }, pager, Modifier.fillMaxWidth()) { }
            }
        }
        compose.onNodeWithContentDescription("第 1 张图片").performTouchInput { swipeLeft() }
        compose.waitForIdle()
        compose.runOnIdle {
            assertTrue("Thumbnail scrolling should update the main page", pager.currentPage > 0)
            assertEquals("The strip should settle on a centered thumbnail", 0f, pager.currentPageOffsetFraction, .05f)
        }
    }
}
