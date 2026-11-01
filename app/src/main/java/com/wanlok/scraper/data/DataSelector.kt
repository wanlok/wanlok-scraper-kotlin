package com.wanlok.scraper.data

import org.json.JSONObject

/**
 * What a task reads from its page. In JSON (the `data_selector` property) it is either a string
 * or an object:
 *
 *     "data_selector": "p.name"
 *     "data_selector": { "div.card": { "name": "h6.card-title", "price": ".price" } }
 */
sealed interface DataSelector {
    /** One CSS selector. Each matched element gives one text. */
    data class Text(val selector: String) : DataSelector

    /**
     * Each element matching [container] gives one item. [fields] maps a field name to a CSS
     * selector that is searched inside that element.
     */
    data class Fields(val container: String, val fields: Map<String, String>) : DataSelector

    /** A short description for the screen. */
    val label: String
        get() = when (this) {
            is Text -> selector
            is Fields -> "$container { ${fields.keys.joinToString(", ")} }"
        }

    /** The JSON value: a String or a JSONObject, the same as it was received. */
    fun toJson(): Any = when (this) {
        is Text -> selector
        is Fields -> JSONObject().put(container, JSONObject(fields))
    }

    companion object {
        /**
         * Reads the JSON value of `data_selector`. Throws [IllegalArgumentException] with a message
         * for the API user if it is not valid.
         */
        fun fromJson(value: Any?): DataSelector = when (value) {
            is String -> {
                require(value.isNotBlank()) { "data_selector is required" }
                Text(value.trim())
            }
            is JSONObject -> fieldsFromJson(value)
            else -> throw IllegalArgumentException("data_selector is required")
        }

        private fun fieldsFromJson(json: JSONObject): Fields {
            require(json.length() == 1) {
                "data_selector must be a string, or an object with exactly one container selector"
            }
            val container = json.keys().next()
            require(container.isNotBlank()) { "the container selector must not be blank" }
            val fieldsJson = json.opt(container) as? JSONObject
                ?: throw IllegalArgumentException("the fields of \"$container\" must be an object")
            require(fieldsJson.length() > 0) {
                "\"$container\" has no fields; use the string form for a plain list"
            }

            val fields = LinkedHashMap<String, String>()
            for (name in fieldsJson.keys()) {
                require(name.isNotBlank()) { "a field name must not be blank" }
                val selector = fieldsJson.opt(name) as? String
                require(!selector.isNullOrBlank()) { "the selector of field \"$name\" must be a non-blank string" }
                fields[name] = selector.trim()
            }
            return Fields(container.trim(), fields)
        }
    }
}