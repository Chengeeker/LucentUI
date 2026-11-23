package dev.chengeeker.lucent

import org.junit.Assert.*
import org.junit.Test

class LucentChoicesTest {
    @Test fun eachChoiceHasExplicitVisibleValue() {
        val modes = listOf(LucentChoice(false, "直线（默认）"), LucentChoice(true, "波浪线"))
        assertEquals("直线（默认）", lucentSelectedChoice(false, modes)?.label)
        assertEquals("波浪线", lucentSelectedChoice(true, modes)?.label)
        assertEquals(2, modes.size)
    }

    @Test fun selectionDoesNotImplicitlyAdvanceCurrentValue() {
        val choices = listOf(16, 18, 20, 24, 28, 32, 36, 40)
            .map { LucentChoice(it, "$it sp") }
        val current = 24
        repeat(5) { assertEquals("24 sp", lucentSelectedChoice(current, choices)?.label) }
        assertEquals("36 sp", lucentSelectedChoice(36, choices)?.label)
        assertNull(lucentSelectedChoice(99, choices))
    }
}
