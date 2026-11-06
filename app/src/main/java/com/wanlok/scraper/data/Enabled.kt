package com.wanlok.scraper.data

/** The `enabled` field of a task: true (the default) to run it, false to skip it. */
object Enabled {
    /**
     * Reads the value sent for `enabled`. Left out (null) means true. Anything but a JSON boolean
     * throws [IllegalArgumentException] with a message that can be sent back to the caller.
     */
    fun fromJson(value: Any?): Boolean = when (value) {
        null -> true
        is Boolean -> value
        else -> throw IllegalArgumentException("enabled must be true or false")
    }
}