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
        for (seed in appearanceSeeds) for (dark in listOf(false, true)) {
            val c = tonalScheme(seed.toInt(), dark)
            assertTrue(contrast(c.primary, c.onPrimary) >= 4.5f)
            assertTrue(contrast(mediaAccent(c), Color(0xFF212121)) >= 4.5f)
            assertTrue(contrast(c.onSurface, c.background) >= 4.5f)
            assertTrue(contrast(c.onSurfaceVariant, c.cardColor()) >= 4.5f)
            for (backdrop in listOf(Color.Black, Color.White)) {
                assertTrue(
                    contrast(
                        c.onSurfaceVariant,
                        c.surface.copy(alpha = NavigationSurfaceAlpha).compositeOver(backdrop),
                    ) >= 4.5f
                )
                assertTrue(
                    contrast(
                        c.onPrimaryContainer,
                        c.primaryContainer
                            .copy(alpha = NavigationSelectionAlpha)
                            .compositeOver(backdrop),
                    ) >= 4.5f
                )
            }
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

    @Test
    fun cancelledNavigationDoesNotSelect() {
        assertNull(glassReleaseTarget(1, 2, 0f, true, true, false, true))
        assertNull(glassReleaseTarget(1, 1, 0f, true, false, false, false))
        assertEquals(2, glassReleaseTarget(1, 0, 1500f, false, true, false, false))
    }
}
