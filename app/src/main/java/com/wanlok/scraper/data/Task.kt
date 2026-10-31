package com.wanlok.scraper.data

/**
 * A scraping task: fetch [url] and extract the text of the element matched by [selector]
 * (a CSS selector, e.g. copied from the browser's DevTools).
 */
data class Task(
    val id: String,
    val name: String,
    val url: String,
    val selector: String
)