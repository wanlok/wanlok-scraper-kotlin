package com.wanlok.scraper.work

/**
 * Works out the URLs of all the pages of a paginated list from the links in its pagination bar.
 *
 * The bar only links to some pages (2, 3 and the last, say), so the other pages are generated
 * from one of the links, by changing the page number in it.
 */
object PageUrls {
    /**
     * The URL of every page in order: [startUrl] as page 1, then page 2 up to the largest page
     * number found in [links] (as given by `a.href`, so absolute). Only links that have the query
     * parameter [param] with a number of 1 or more count; any other links, and `page=0`, are
     * ignored. The other parts of the link, such as other parameters, are kept.
     *
     * Returns null if no link has the parameter.
     */
    fun build(startUrl: String, param: String, links: List<String>): List<String>? {
        val pattern = Regex("([?&])" + Regex.escape(param) + "=(\\d+)(?=&|$)")

        var prefix: String? = null
        var suffix = ""
        var last = 0
        for (link in links) {
            // A fragment (#...) is not part of the page's address.
            val url = link.substringBefore('#')
            val match = pattern.find(url) ?: continue
            val page = match.groupValues[2].toIntOrNull() ?: continue
            if (page < 1) continue

            if (prefix == null) {
                // The text before and after the page number, to build the other pages from.
                val number = match.groups[2]!!.range
                prefix = url.substring(0, number.first)
                suffix = url.substring(number.last + 1)
            }
            if (page > last) last = page
        }
        if (prefix == null) return null

        return listOf(startUrl) + (2..last).map { "$prefix$it$suffix" }
    }
}