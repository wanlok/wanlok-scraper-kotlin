package com.wanlok.scraper.data

import org.json.JSONObject

/**
 * How to find the pages of a list that is spread over several pages. In JSON (the `page_selector`
 * property) it is an object with one or more pairs. The key is a CSS selector for the pagination
 * links, and the value is the name of the query parameter that holds the page number:
 *
 *     "page_selector": { ".pagination a": "page" }
 */
data class PageSelector(val pairs: Map<String, String>) {
    companion object {
        private const val FORMAT = "page_selector must be an object like { \".pagination a\": \"page\" }"

        /**
         * Reads the JSON value of `page_selector`. Throws [IllegalArgumentException] with a message
         * for the API user if it is not valid.
         */
        fun fromJson(value: Any?): PageSelector {
            if (value == null || value == JSONObject.NULL) {
                throw IllegalArgumentException("page_selector is required")
            }
            val json = value as? JSONObject ?: throw IllegalArgumentException(FORMAT)
            require(json.length() > 0) { FORMAT }

            // The order the pairs are written in is the order of their pages.
            val pairs = LinkedHashMap<String, String>()
            for (selector in json.keys()) {
                require(selector.isNotBlank()) { "a selector in page_selector must not be blank" }
                val param = json.opt(selector) as? String
                require(!param.isNullOrBlank()) {
                    "the page parameter of \"$selector\" must be a non-blank string"
                }
                pairs[selector.trim()] = param.trim()
            }
            return PageSelector(pairs)
        }
    }
}