package dev.chengeeker.lucent.review

import android.graphics.Rect
import android.media.ExifInterface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GalleryHighResolutionTest {
    @Test fun zoomMapsToSmallerOriginalPixelWindow() {
        val wide = Size(1080f, 2400f)
        val normal = requireNotNull(galleryRegionPlan(8000, 6000, wide, 1f, Offset.Zero))
        val zoomed = requireNotNull(galleryRegionPlan(8000, 6000, wide, 4f, Offset.Zero))
        assertEquals(8000, normal.rect.width())
        assertEquals(6000, normal.rect.height())
        assertTrue(zoomed.rect.width() < normal.rect.width())
        assertTrue(zoomed.rect.height() < normal.rect.height())
        assertTrue(zoomed.rect.left >= 0)
        assertTrue(zoomed.rect.right <= 8000)
    }

    @Test fun panningMovesVisibleSourceRect() {
        val viewport = Size(1080f, 2300f)
        val leftward = requireNotNull(galleryRegionPlan(8000, 6000, viewport, 4f, Offset(-160f, 0f)))
        val rightward = requireNotNull(galleryRegionPlan(8000, 6000, viewport, 4f, Offset(160f, 0f)))
        assertTrue(leftward.rect.left > rightward.rect.left)
    }

    @Test fun decodedTilesHaveBoundedMemoryEvenForGigapixelSource() {
        val plan = requireNotNull(galleryRegionPlan(100000, 80000, Size(1440f, 3200f), 2f, Offset.Zero))
        val w = plan.rect.width() / plan.sampleSize
        val h = plan.rect.height() / plan.sampleSize
        assertTrue(w <= 2560)
        assertTrue(h <= 2560)
        assertTrue(w.toLong() * h <= 4_194_304L)
    }

    @Test fun invalidViewportOrImageIsNotDecoded() {
        assertNull(galleryRegionPlan(0, 1000, Size(1080f, 2300f), 3f, Offset.Zero))
        assertNull(galleryRegionPlan(2000, 1000, Size.Zero, 3f, Offset.Zero))
        assertNull(galleryRegionPlan(2000, 1000, Size(1000f, 1000f), Float.NaN, Offset.Zero))
    }

    @Test fun exifRotationAndMirroringReturnValidEncodedCoordinates() {
        val area = Rect(100, 200, 400, 800)
        assertEquals(Rect(200, 7600, 800, 7900),
            galleryUnrotateRegion(area, 6000, 8000, ExifInterface.ORIENTATION_ROTATE_90))
        assertEquals(Rect(5600, 7200, 5900, 7800),
            galleryUnrotateRegion(area, 6000, 8000, ExifInterface.ORIENTATION_ROTATE_180))
        assertEquals(Rect(5600, 200, 5900, 800),
            galleryUnrotateRegion(area, 6000, 8000, ExifInterface.ORIENTATION_FLIP_HORIZONTAL))
        assertEquals(Rect(5200, 100, 5800, 400),
            galleryUnrotateRegion(area, 6000, 8000, ExifInterface.ORIENTATION_ROTATE_270))
    }

    @Test fun originalPillSitsAboveFilmstripAndRespectsSingleImageSpacing() {
        assertEquals(104.dp, galleryOriginalPillBottomMargin(true, 32.dp))
        assertEquals(24.dp, galleryOriginalPillBottomMargin(false, 32.dp))
        assertEquals(148.dp, galleryOriginalPillBottomMargin(true, 76.dp))
    }

    @Test fun explicitRetryStartsAnotherRequestGeneration() {
        val state = GalleryHiResState("test")
        assertFalse(state.requested)
        state.request()
        assertTrue(state.requested)
        state.retry()
        assertTrue(state.requested)
        assertEquals(1, state.generation)
    }
}
