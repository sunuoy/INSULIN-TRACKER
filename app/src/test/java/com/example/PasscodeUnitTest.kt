package com.example

import org.junit.Assert.*
import org.junit.Test

class PasscodeUnitTest {

    private fun isValidPin(pin: String): Boolean {
        return pin.length == 4 && pin.all { it.isDigit() }
    }

    @Test
    fun testPasscodeValidation() {
        assertTrue(isValidPin("1234"))
        assertTrue(isValidPin("0000"))
        assertTrue(isValidPin("9876"))

        assertFalse(isValidPin("123"))     // Too short
        assertFalse(isValidPin("12345"))   // Too long
        assertFalse(isValidPin("12a4"))    // Non-numeric
        assertFalse(isValidPin("    "))    // Whitespace
        assertFalse(isValidPin(""))        // Empty
    }

    @Test
    fun testPasscodeMatching() {
        val configuredPin = "4321"
        assertTrue(configuredPin == "4321")
        assertFalse(configuredPin == "1234")
        assertFalse(configuredPin == "432")
    }
}
