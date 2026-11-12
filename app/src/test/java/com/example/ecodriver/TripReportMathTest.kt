package com.example.ecodriver

import com.example.ecodriver.data.cToF
import com.example.ecodriver.data.consistency
import com.example.ecodriver.data.csvRow
import com.example.ecodriver.data.kmhToMph
import com.example.ecodriver.data.kpaToPsi
import com.example.ecodriver.data.parseDtcs
import com.example.ecodriver.data.savingsUsd
import com.example.ecodriver.data.smoothnessScore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TripReportMathTest {

    @Test
    fun smoothnessScoreFormulaAndClamp() {
        assertEquals(100.0, smoothnessScore(0.0, 0.0), 1e-9)
        assertEquals(75.0, smoothnessScore(2.0, 10.0), 1e-9) // 100 - 20 - 5
        assertEquals(0.0, smoothnessScore(50.0, 0.0), 1e-9)
        assertEquals(100.0, smoothnessScore(-1.0, 0.0), 1e-9)
    }

    @Test
    fun consistencyConstantSpeedIs100() {
        assertEquals(100.0, consistency(List(30) { 45.0 })!!, 1e-9)
    }

    @Test
    fun consistencyNeedsThirtyMovingSamples() {
        assertNull(consistency(List(29) { 45.0 }))
        assertNull(consistency(List(29) { 45.0 } + List(10) { 2.0 })) // stopped samples don't count
    }

    @Test
    fun consistencyDropsWithVariation() {
        val c = consistency(List(40) { if (it % 2 == 0) 30.0 else 50.0 })!! // mean 40, sd 10
        assertEquals(75.0, c, 1e-9)
    }

    @Test
    fun savingsPositiveAndNegative() {
        // 30 mi at 25 MPG baseline = 1.2 gal. Used 1.0 gal at $3.50.
        assertEquals(0.70, savingsUsd(30.0, 1.0, 25.0, 3.50), 1e-9)
        assertEquals(-1.05, savingsUsd(30.0, 1.5, 25.0, 3.50), 1e-9)
    }

    @Test
    fun unitHelpers() {
        assertEquals(62.1, kmhToMph(100.0), 0.05)
        assertEquals(194.0, cToF(90.0), 1e-9)
        assertEquals(14.5, kpaToPsi(100.0), 0.05)
    }

    @Test
    fun parseDtcsCases() {
        assertEquals(emptyList<String>(), parseDtcs(""))
        assertEquals(emptyList<String>(), parseDtcs(null))
        assertEquals(listOf("P0300"), parseDtcs("P0300"))
        assertEquals(listOf("P0300", "P0171", "C1234"), parseDtcs("P0300,P0171, C1234"))
    }

    @Test
    fun csvRowQuotesCommasAndQuotes() {
        assertEquals("1,2014 Honda Civic,25.5,", csvRow(listOf(1, "2014 Honda Civic", 25.5, null)))
        assertEquals("1,\"Civic, LX\",3", csvRow(listOf(1, "Civic, LX", 3)))
        assertEquals("\"say \"\"hi\"\"\"", csvRow(listOf("say \"hi\"")))
    }
}
