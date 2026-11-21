package com.wanlok.scraper.data

/** The `scheduled` field of a task: true (the default) to run it with the periodic job, false to skip it there. */
object Scheduled {
    /**
     * Reads the value sent for `scheduled`. Left out (null) means true. Anything but a JSON boolean
     * throws [IllegalArgumentException] with a message that can be sent back to the caller.
     */
    fun fromJson(value: Any?): Boolean = when (value) {
        null -> true
        is Boolean -> value
        else -> throw IllegalArgumentException("scheduled must be true or false")
    }
}
