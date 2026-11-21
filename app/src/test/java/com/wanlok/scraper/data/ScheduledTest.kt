package com.wanlok.scraper.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ScheduledTest {
    @Test
    fun leftOutMeansScheduled() {
        assertTrue(Scheduled.fromJson(null))
    }

    @Test
    fun trueAndFalseAreKept() {
        assertTrue(Scheduled.fromJson(true))
        assertFalse(Scheduled.fromJson(false))
    }

    @Test
    fun anythingButABooleanIsRejected() {
        listOf("false", "true", 0, 1, emptyList<Any>()).forEach { value ->
            try {
                Scheduled.fromJson(value)
                fail("$value should be rejected")
            } catch (e: IllegalArgumentException) {
                assertEquals("scheduled must be true or false", e.message)
            }
        }
    }
}
