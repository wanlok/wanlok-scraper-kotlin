package com.wanlok.scraper.work

import android.content.Context
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.wanlok.scraper.data.DataSelector
import com.wanlok.scraper.data.PageSelector
import com.wanlok.scraper.data.ScrapedItem
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import org.json.JSONTokener
import kotlin.coroutines.resume

/** A page could not be scraped. The message is the reason shown in the log and the snapshot. */
class ScrapeException(message: String) : Exception(message)

/**
 * Loads a page in a WebView (the system browser engine, so JavaScript runs like in Chrome) and
 * reads from it: the text of the elements a task's data selector describes, or the URLs of the
 * pages of a list.
 */
object PageScraper {
    private const val LOAD_TIMEOUT_MS = 30_000L

    // After the page has loaded its scripts may still be building the content, so the selector is
    // tried every POLL_MS until something matches or MATCH_TIMEOUT_MS has passed.
    private const val MATCH_TIMEOUT_MS = 10_000L
    private const val POLL_MS = 250L

    /**
     * What [dataSelector] describes, one item per matching element in page order. Throws
     * [ScrapeException].
     */
    suspend fun scrape(context: Context, url: String, dataSelector: DataSelector): List<ScrapedItem> =
        withPage(context, url) { webView -> waitForMatches(webView, dataSelector) }

    /**
     * The URLs of all the pages of the list that starts at [url] (page 1), in order, worked out
     * from the pagination links on that page. The pairs of [pageSelector] are handled in turn and
     * their pages are joined, without duplicates. Throws [ScrapeException].
     */
    suspend fun pageUrls(context: Context, url: String, pageSelector: PageSelector): List<String> =
        withPage(context, url) { webView ->
            val pages = LinkedHashSet<String>()
            for ((selector, param) in pageSelector.pairs) {
                val links = pollUntilNotEmpty { queryLinks(webView, selector) }
                if (links.isEmpty()) throw ScrapeException("page_selector matched nothing: $selector")
                pages += PageUrls.build(url, param, links)
                    ?: throw ScrapeException("no links with page parameter \"$param\"")
            }
            pages.toList()
        }

    /** Loads [url] in a WebView and runs [block] on the loaded page. Then destroys the WebView. */
    private suspend fun <T> withPage(
        context: Context,
        url: String,
        block: suspend (WebView) -> T
    ): T =
        // A WebView must be created and used on the main thread.
        withContext(Dispatchers.Main) {
            val webView = WebView(context.applicationContext)
            try {
                webView.settings.javaScriptEnabled = true
                val loaded = CompletableDeferred<Unit>()
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        loaded.complete(Unit)
                    }

                    override fun onReceivedError(
                        view: WebView,
                        request: WebResourceRequest,
                        error: WebResourceError
                    ) {
                        if (request.isForMainFrame) {
                            loaded.completeExceptionally(ScrapeException(error.description.toString()))
                        }
                    }

                    override fun onReceivedHttpError(
                        view: WebView,
                        request: WebResourceRequest,
                        errorResponse: WebResourceResponse
                    ) {
                        if (request.isForMainFrame) {
                            loaded.completeExceptionally(ScrapeException("HTTP ${errorResponse.statusCode}"))
                        }
                    }
                }
                webView.loadUrl(url)
                withTimeoutOrNull(LOAD_TIMEOUT_MS) { loaded.await() }
                    ?: throw ScrapeException("page took too long to load")
                block(webView)
            } finally {
                webView.destroy()
            }
        }

    /** Reads until something comes back. Returns an empty list if nothing did in time. */
    private suspend fun <T> pollUntilNotEmpty(read: suspend () -> List<T>): List<T> {
        var items = read()
        withTimeoutOrNull(MATCH_TIMEOUT_MS) {
            while (items.isEmpty()) {
                delay(POLL_MS)
                items = read()
            }
        }
        return items
    }

    private suspend fun waitForMatches(
        webView: WebView,
        dataSelector: DataSelector
    ): List<ScrapedItem> = pollUntilNotEmpty { query(webView, dataSelector) }

    private suspend fun query(webView: WebView, dataSelector: DataSelector): List<ScrapedItem> {
        val container: String
        val fields: JSONObject?
        when (dataSelector) {
            is DataSelector.Text -> {
                container = dataSelector.selector
                fields = null
            }
            is DataSelector.Fields -> {
                container = dataSelector.container
                fields = JSONObject(dataSelector.fields)
            }
        }

        // Without fields each container gives its text; with fields each gives an object, with null
        // for a field that isn't found inside it.
        val result = evaluate(
            webView,
            """
            (function() {
                var fields = ${fields ?: "null"};
                function text(node) {
                    return (node.textContent || '').replace(/\s+/g, ' ').trim();
                }
                var nodes;
                try {
                    nodes = document.querySelectorAll(${JSONObject.quote(container)});
                } catch (e) {
                    return JSON.stringify({ invalid: true });
                }
                var items = [];
                for (var i = 0; i < nodes.length; i++) {
                    if (fields === null) {
                        items.push(text(nodes[i]));
                        continue;
                    }
                    var item = {};
                    for (var name in fields) {
                        try {
                            var found = nodes[i].querySelector(fields[name]);
                            item[name] = found ? text(found) : null;
                        } catch (e) {
                            return JSON.stringify({ invalidField: name });
                        }
                    }
                    items.push(item);
                }
                return JSON.stringify({ items: items });
            })()
            """.trimIndent()
        )
        if (result.optBoolean("invalid")) throw ScrapeException("invalid data selector")
        val invalidField = result.optString("invalidField")
        if (invalidField.isNotEmpty()) throw ScrapeException("invalid selector for field $invalidField")

        val items = result.getJSONArray("items")
        return List(items.length()) { index ->
            when (dataSelector) {
                is DataSelector.Text -> ScrapedItem.Text(items.getString(index))
                is DataSelector.Fields -> {
                    val item = items.getJSONObject(index)
                    // Keep the order of the task's fields.
                    ScrapedItem.Fields(
                        dataSelector.fields.keys.associateWith { name ->
                            if (item.isNull(name)) null else item.getString(name)
                        }
                    )
                }
            }
        }
    }

    /** The absolute URL of every element [selector] matches that has an href. */
    private suspend fun queryLinks(webView: WebView, selector: String): List<String> {
        // For a link, node.href is the full address, even if the page wrote a relative one.
        val result = evaluate(
            webView,
            """
            (function() {
                var nodes;
                try {
                    nodes = document.querySelectorAll(${JSONObject.quote(selector)});
                } catch (e) {
                    return JSON.stringify({ invalid: true });
                }
                return JSON.stringify({ links: Array.from(nodes, function(node) {
                    return typeof node.href === 'string' ? node.href : '';
                }) });
            })()
            """.trimIndent()
        )
        if (result.optBoolean("invalid")) throw ScrapeException("invalid page_selector")

        val links = result.getJSONArray("links")
        return List(links.length()) { links.getString(it) }.filter { it.isNotEmpty() }
    }

    /** Runs [script], which returns JSON text, in the page and parses what it returns. */
    private suspend fun evaluate(webView: WebView, script: String): JSONObject {
        // evaluateJavascript hands back the script's result as JSON, so a string comes back quoted.
        val raw = suspendCancellableCoroutine<String?> { continuation ->
            webView.evaluateJavascript(script) { continuation.resume(it) }
        }
        val text = raw?.let { JSONTokener(it).nextValue() as? String }
            ?: throw ScrapeException("could not read the page")
        return JSONObject(text)
    }
}