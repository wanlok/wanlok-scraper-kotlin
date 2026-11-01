package com.wanlok.scraper.data

/**
 * A scraping task: load [url] and read what [dataSelector] describes (CSS selectors, e.g. copied
 * from the browser's DevTools). For a list spread over several pages, the optional [pageSelector]
 * finds the other pages of the list that starts at [url], and each page is read in turn. Without
 * it only [url] is read.
 */
data class Task(
    val id: String,
    val name: String,
    val url: String,
    val pageSelector: PageSelector?,
    val dataSelector: DataSelector
)