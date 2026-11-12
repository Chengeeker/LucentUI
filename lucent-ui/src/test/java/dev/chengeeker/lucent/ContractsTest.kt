package dev.chengeeker.lucent

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import org.junit.Assert.*
import org.junit.Test

class ContractsTest {
    @Test
    fun allStaticPalettesMeetTextContrast() {
        for (pair in dev.chengeeker.lucent.review.ReviewPalettes) for (c in listOf(pair.first, pair.second)) {
            assertTrue(contrast(c.primary, c.onPrimary) >= 4.5f)
            assertTrue(contrast(c.onSurface, c.background) >= 4.5f)
            assertTrue(contrast(c.onSurfaceVariant, c.cardColor()) >= 4.5f)
        }
    }

    @Test
    fun fontsAreFiniteClampedAndStepped() {
        assertEquals(1f, normalizeFont(Float.NaN), 0f)
        assertEquals(.85f, normalizeFont(.2f), .001f)
        assertEquals(1.5f, normalizeFont(8f), .001f)
        assertEquals(1.15f, normalizeFont(1.13f), .001f)
    }

    @Test
    fun cardsRemainOpaqueWhenDynamicPaletteCollapses() {
        for (base in listOf(Color.White, Color.Black, Color(0xff202020))) {
            val scheme =
                if (base == Color.White)
                    lightColorScheme(background = base, surfaceContainerHigh = base)
                else darkColorScheme(background = base, surfaceContainerHigh = base)
            val card = scheme.cardColor()
            assertEquals(1f, card.alpha, 0f)
            assertTrue(contrast(card, base) > 1.1f)
        }
    }

    @Test
    fun onlyDistinctKnownManualSourcesEnableQualityMenu() {
        val list =
            listOf(
                VideoSource("", "x"),
                VideoSource("未知", "u"),
                VideoSource("720p", "a"),
                VideoSource("720p duplicate", "a"),
                VideoSource("480p", "b"),
                VideoSource("480p", "another-address"),
            )
        assertEquals(listOf("a", "b"), manualQualities(list).map { it.uri })
        assertEquals(
            1,
            manualQualities(listOf(VideoSource("720p", "a"), VideoSource("duplicate", "a"))).size,
        )
    }

    @Test fun referenceGalleryZoomAndFlightKeepTheirContracts() {
        assertEquals(2.5f, dev.chengeeker.lucent.review.galleryDoubleTapScale(1f, false), 0f)
        assertEquals(3.5f, dev.chengeeker.lucent.review.galleryDoubleTapScale(1f, true), 0f)
        assertEquals(6f, dev.chengeeker.lucent.review.galleryDoubleTapScale(2.5f, false), 0f)
        assertEquals(1f, dev.chengeeker.lucent.review.galleryDoubleTapScale(6f, false), 0f)
        val viewport=androidx.compose.ui.geometry.Rect(0f,0f,400f,800f)
        assertEquals(androidx.compose.ui.geometry.Rect(0f,300f,400f,500f),dev.chengeeker.lucent.review.galleryImagePlane(viewport,androidx.compose.ui.geometry.Size(800f,400f),false))
        assertEquals(androidx.compose.ui.geometry.Rect(-600f,0f,1000f,800f),dev.chengeeker.lucent.review.galleryImagePlane(viewport,androidx.compose.ui.geometry.Size(800f,400f),true))
    }
}
