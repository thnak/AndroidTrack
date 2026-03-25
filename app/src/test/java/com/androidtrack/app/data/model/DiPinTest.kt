package com.androidtrack.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DiPinTest {

    // --- defaults ------------------------------------------------------------------

    @Test
    fun `default DiPin has expected field values`() {
        val pin = DiPin(pinNumber = "01", mode = PinMode.MANUAL)
        assertEquals(0, pin.id)
        assertEquals("01", pin.pinNumber)
        assertEquals(PinMode.MANUAL, pin.mode)
        assertEquals(5000L, pin.timerMs)
        assertEquals(0L, pin.shootCount)
        assertEquals(1000L, pin.pulseTime)
    }

    // --- PinMode ------------------------------------------------------------------

    @Test
    fun `PinMode has exactly two values MANUAL and AUTO`() {
        val values = PinMode.values()
        assertEquals(2, values.size)
        assertEquals(PinMode.MANUAL, values[0])
        assertEquals(PinMode.AUTO, values[1])
    }

    @Test
    fun `PinMode valueOf works correctly`() {
        assertEquals(PinMode.MANUAL, PinMode.valueOf("MANUAL"))
        assertEquals(PinMode.AUTO, PinMode.valueOf("AUTO"))
    }

    // --- equality ------------------------------------------------------------------

    @Test
    fun `two DiPins with identical fields are equal`() {
        val a = DiPin(id = 1, pinNumber = "01", mode = PinMode.AUTO, timerMs = 3000L, shootCount = 10L, pulseTime = 500L)
        val b = DiPin(id = 1, pinNumber = "01", mode = PinMode.AUTO, timerMs = 3000L, shootCount = 10L, pulseTime = 500L)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `DiPins differing in shootCount are not equal`() {
        val a = DiPin(pinNumber = "01", mode = PinMode.MANUAL, shootCount = 5L)
        val b = DiPin(pinNumber = "01", mode = PinMode.MANUAL, shootCount = 6L)
        assertNotEquals(a, b)
    }

    @Test
    fun `DiPins differing in mode are not equal`() {
        val a = DiPin(pinNumber = "01", mode = PinMode.MANUAL)
        val b = DiPin(pinNumber = "01", mode = PinMode.AUTO)
        assertNotEquals(a, b)
    }

    // --- copy ---------------------------------------------------------------------

    @Test
    fun `copy increments shootCount correctly`() {
        val original = DiPin(pinNumber = "01", mode = PinMode.MANUAL, shootCount = 42L)
        val updated = original.copy(shootCount = original.shootCount + 1)
        assertEquals(43L, updated.shootCount)
        // other fields unchanged
        assertEquals("01", updated.pinNumber)
        assertEquals(PinMode.MANUAL, updated.mode)
    }

    @Test
    fun `copy with new mode preserves other fields`() {
        val original = DiPin(id = 5, pinNumber = "02", mode = PinMode.MANUAL, timerMs = 2000L)
        val updated = original.copy(mode = PinMode.AUTO)
        assertEquals(5, updated.id)
        assertEquals("02", updated.pinNumber)
        assertEquals(PinMode.AUTO, updated.mode)
        assertEquals(2000L, updated.timerMs)
    }
}
