package com.wanlok.scraper.data

/**
 * A scraping task: load [url] and read what [dataSelector] describes (CSS selectors, e.g. copied
 * from the browser's DevTools). It is called the data selector because a page selector, for sites
 * with several pages, is planned.
 */
data class Task(
    val id: String,
    val name: String,
    val url: String,
    val dataSelector: DataSelector
)