package com.wanlok.scraper.data

/**
 * A scraping task: fetch [url] and extract the text of the element matched by [dataSelector]
 * (a CSS selector, e.g. copied from the browser's DevTools). It is called the data selector
 * because a page selector, for sites with several pages, is planned.
 */
data class Task(
    val id: String,
    val name: String,
    val url: String,
    val dataSelector: String
)