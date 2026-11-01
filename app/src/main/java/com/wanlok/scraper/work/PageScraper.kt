package com.wanlok.scraper.work

import android.content.Context
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
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
 * reads the text of the elements a CSS selector matches.
 */
object PageScraper {
    private const val LOAD_TIMEOUT_MS = 30_000L

    // After the page has loaded its scripts may still be building the content, so the selector is
    // tried every POLL_MS until something matches or MATCH_TIMEOUT_MS has passed.
    private const val MATCH_TIMEOUT_MS = 10_000L
    private const val POLL_MS = 250L

    /** The text of every element matching [dataSelector], in page order. Throws [ScrapeException]. */
    suspend fun scrape(context: Context, url: String, dataSelector: String): List<String> =
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
    private suspend fun waitForMatches(webView: WebView, dataSelector: String): List<String> {
        var items = query(webView, dataSelector)
        withTimeoutOrNull(MATCH_TIMEOUT_MS) {
            while (items.isEmpty()) {
                delay(POLL_MS)
                items = query(webView, dataSelector)
            }
        }
        return items
    }

    private suspend fun query(webView: WebView, dataSelector: String): List<String> {
        val script = """
            (function() {
                try {
                    var nodes = document.querySelectorAll(${JSONObject.quote(dataSelector)});
                    return JSON.stringify({ items: Array.from(nodes, function(node) {
                        return (node.textContent || '').replace(/\s+/g, ' ').trim();
                    }) });
                } catch (e) {
                    return JSON.stringify({ invalid: true });
                }
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
        val items = result.getJSONArray("items")
        return List(items.length()) { items.getString(it) }
    }
}
