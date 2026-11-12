package com.example.ecodriver

import com.example.ecodriver.data.Cue
import com.example.ecodriver.data.HARSH_BRAKE_MPH_PER_S
import com.example.ecodriver.data.HarshEvent
import com.example.ecodriver.data.elapsedText
import com.example.ecodriver.data.galPerHourFromMaf
import com.example.ecodriver.data.harshEvent
import com.example.ecodriver.data.instantMpg
import com.example.ecodriver.data.instantMpgText
import com.example.ecodriver.data.isDfco
import com.example.ecodriver.data.pickCue
import com.example.ecodriver.data.tripMpg
import com.example.ecodriver.data.tripMpgText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TripMathTest {

    @Test
    fun mpgFromMaf() {
        val mpg = instantMpg(42.0, galPerHourFromMaf(7.4))!!
        assertEquals(64.9, mpg, 0.05)
        assertEquals("65", instantMpgText(mpg))
    }

    @Test
    fun stoppedShowsDashes() {
        assertNull(instantMpg(0.0, galPerHourFromMaf(2.0)))
        assertEquals("--", instantMpgText(instantMpg(0.0, galPerHourFromMaf(2.0))))
    }

    @Test
    fun zeroMafWhileMovingShows99Plus() {
        assertEquals("99+", instantMpgText(instantMpg(40.0, galPerHourFromMaf(0.0))))
    }

    @Test
    fun harshBrakeAtThreshold() {
        // Exactly at the threshold is not harsh; just past it is.
        assertNull(harshEvent(30.0, 30.0 - HARSH_BRAKE_MPH_PER_S, 1.0))
        assertEquals(HarshEvent.Brake, harshEvent(30.0, 30.0 - HARSH_BRAKE_MPH_PER_S - 0.1, 1.0))
        assertEquals(HarshEvent.Brake, harshEvent(30.0, 27.9, 0.25)) // 8.4 mph/s over one 250 ms loop
        assertEquals(HarshEvent.Accel, harshEvent(20.0, 22.0, 0.25)) // 8 mph/s
    }

    @Test
    fun cuePriority() {
        assertEquals(Cue.Harsh(HarshEvent.Brake), pickCue(HarshEvent.Brake, 1_000, 120.0, true))
        assertEquals(Cue.Idling(120), pickCue(HarshEvent.Brake, 6_000, 120.0, true))
        assertEquals(Cue.Coasting, pickCue(null, 0, 59.0, true))
        assertEquals(Cue.Steady, pickCue(null, 0, 0.0, false))
    }

    @Test
    fun tripMpgNeedsDistance() {
        assertEquals("--", tripMpgText(tripMpg(0.1, 0.01)))
        assertEquals("29.6", tripMpgText(tripMpg(6.3, 6.3 / 29.6)))
    }

    @Test
    fun dfcoNeedsClosedThrottleAndNoFuel() {
        assertTrue(isDfco(40.0, 0.0, 0.0))
        assertFalse(isDfco(40.0, 20.0, 0.0))
        assertFalse(isDfco(4.0, 0.0, 0.0))
    }

    @Test
    fun elapsedFormat() {
        assertEquals("12:48", elapsedText(768_000))
    }
}
