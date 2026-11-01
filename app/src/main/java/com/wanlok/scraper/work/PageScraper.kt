package com.wanlok.scraper.work

import android.content.Context
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.wanlok.scraper.data.DataSelector
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
 * reads the text of the elements a task's data selector describes.
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
                waitForMatches(webView, dataSelector)
            } finally {
                webView.destroy()
            }
        }

    /** Queries until something matches. Returns an empty list if nothing matched in time. */
    private suspend fun waitForMatches(
        webView: WebView,
        dataSelector: DataSelector
    ): List<ScrapedItem> {
        var items = query(webView, dataSelector)
        withTimeoutOrNull(MATCH_TIMEOUT_MS) {
            while (items.isEmpty()) {
                delay(POLL_MS)
                items = query(webView, dataSelector)
            }
        }
        return items
    }

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
        val script = """
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

        // evaluateJavascript hands back the script's result as JSON, so a string comes back quoted.
        val raw = suspendCancellableCoroutine<String?> { continuation ->
            webView.evaluateJavascript(script) { continuation.resume(it) }
        }
        val text = raw?.let { JSONTokener(it).nextValue() as? String }
            ?: throw ScrapeException("could not read the page")

        val result = JSONObject(text)
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
}
