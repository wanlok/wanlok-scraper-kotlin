package com.wanlok.scraper.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class EnabledTest {
    @Test
    fun leftOutMeansEnabled() {
        assertTrue(Enabled.fromJson(null))
    }

    @Test
    fun trueAndFalseAreKept() {
        assertTrue(Enabled.fromJson(true))
        assertFalse(Enabled.fromJson(false))
    }

    @Test
    fun anythingButABooleanIsRejected() {
        listOf("false", "true", 0, 1, emptyList<Any>()).forEach { value ->
            try {
                Enabled.fromJson(value)
                fail("$value should be rejected")
            } catch (e: IllegalArgumentException) {
                assertEquals("enabled must be true or false", e.message)
            }
        }
    }
}
