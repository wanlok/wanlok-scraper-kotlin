package com.wanlok.scraper.server

import android.content.Context
import com.wanlok.scraper.data.DataSelector
import com.wanlok.scraper.data.PageSelector
import com.wanlok.scraper.data.SnapshotRepository
import com.wanlok.scraper.data.TaskRepository
import com.wanlok.scraper.work.PageScraper
import com.wanlok.scraper.work.ScrapeException
import com.wanlok.scraper.work.TaskRunner
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.URI
import java.net.URISyntaxException

private const val DEFAULT_SNAPSHOTS = 100
private const val MAX_SNAPSHOTS = 1000

/** The task API used by the desktop page. Everything is JSON. */
fun Route.taskRoutes(context: Context) {
    // Runs every task now, the same way the periodic job does. Handy for testing.
    // The run takes as long as the pages take, so this returns at once. The outcome is in the Home log.
    post("/api/run") {
        if (TaskRunner.isRunning) {
            call.respondError("A run is already in progress", HttpStatusCode.Conflict)
        } else {

            TaskRunner.runAllInBackground(context)
            call.respond(HttpStatusCode.Accepted)
        }
    }

    // Works out the URLs of all the pages of a list from its pagination links. Handy for testing a
    // page_selector. Body: { "url": "...", "page_selector": { ".pagination a": "page" } }
    post("/api/pages") {
        val body = try {
            JSONObject(call.receiveText())
        } catch (e: JSONException) {
            return@post call.respondError("The body must be JSON")
        }
        val url = body.optString("url").trim()
        if (!isHttpUrl(url)) {
            return@post call.respondError("url must be an http:// or https:// address")
        }
        val pageSelector = try {
            PageSelector.fromJson(body.opt("page_selector"))
        } catch (e: IllegalArgumentException) {
            return@post call.respondError(e.message ?: "page_selector is not valid")
        }

        try {
            val pages = PageScraper.pageUrls(context, url, pageSelector)
            call.respondText(
                JSONObject().put("pages", JSONArray(pages)).toString(),
                ContentType.Application.Json
            )
        } catch (e: ScrapeException) {
            call.respondError(e.message ?: "could not read the page", HttpStatusCode.UnprocessableEntity)
        }
    }

    route("/api/tasks") {
        get {
            TaskRepository.load(context)
            call.respondText(
                TaskRepository.toJson(TaskRepository.tasks.value),
                ContentType.Application.Json
            )
        }

        // One task with its snapshots, newest first. ?limit= sets how many snapshots.
        get("{id}") {
            TaskRepository.load(context)
            val id = call.parameters["id"].orEmpty()
            val task = TaskRepository.tasks.value.firstOrNull { it.id == id }
                ?: return@get call.respondError("No task with that id", HttpStatusCode.NotFound)
            val limit = call.request.queryParameters["limit"]?.toIntOrNull()
                ?.coerceIn(1, MAX_SNAPSHOTS) ?: DEFAULT_SNAPSHOTS

            val snapshots = JSONArray()
            SnapshotRepository.latest(context, task.id, limit).forEach {
                snapshots.put(SnapshotRepository.toJson(it))
            }
            call.respondText(
                TaskRepository.toJson(task).put("snapshots", snapshots).toString(),
                ContentType.Application.Json
            )
        }

        post {
            val input = call.receiveTaskInput() ?: return@post
            val task = TaskRepository.add(
                context, input.name, input.url, input.pageSelector, input.dataSelector
            )
            call.respondText(
                TaskRepository.toJson(task).toString(),
                ContentType.Application.Json,
                HttpStatusCode.Created
            )
        }

        put("{id}") {
            val input = call.receiveTaskInput() ?: return@put
            val id = call.parameters["id"].orEmpty()
            val task = TaskRepository.update(
                context, id, input.name, input.url, input.pageSelector, input.dataSelector
            )
            if (task != null) {
                call.respondText(
                    TaskRepository.toJson(task).toString(),
                    ContentType.Application.Json
                )
            } else {
                call.respondError("No task with that id", HttpStatusCode.NotFound)
            }
        }

        delete("{id}") {
            val id = call.parameters["id"].orEmpty()
            if (TaskRepository.remove(context, id)) {
                SnapshotRepository.delete(context, id)
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respondError("No task with that id", HttpStatusCode.NotFound)
            }
        }
    }
}

private data class TaskInput(
    val name: String,
    val url: String,
    val pageSelector: PageSelector?,
    val dataSelector: DataSelector
)

/** Reads and validates a task from the request body. Responds 400 and returns null if invalid. */
private suspend fun ApplicationCall.receiveTaskInput(): TaskInput? {
    val body = try {
        JSONObject(receiveText())
    } catch (e: JSONException) {
        respondError("The body must be JSON")
        return null
    }
    val name = body.optString("name")
    val url = body.optString("url")
    val error = when {
        name.isBlank() -> "name is required"
        !isHttpUrl(url) -> "url must be an http:// or https:// address"
        else -> null
    }
    if (error != null) {
        respondError(error)
        return null
    }
    // The page selector is optional. Without it only the task's own page is read.
    val pageSelector = if (body.isNull("page_selector")) {
        null
    } else {
        try {
            PageSelector.fromJson(body.opt("page_selector"))
        } catch (e: IllegalArgumentException) {
            respondError(e.message ?: "page_selector is not valid")
            return null
        }
    }
    val dataSelector = try {
        DataSelector.fromJson(body.opt("data_selector"))
    } catch (e: IllegalArgumentException) {
        respondError(e.message ?: "data_selector is not valid")
        return null
    }
    return TaskInput(name, url, pageSelector, dataSelector)
}

private fun isHttpUrl(url: String): Boolean = try {
    val uri = URI(url.trim())
    (uri.scheme == "http" || uri.scheme == "https") && !uri.host.isNullOrBlank()
} catch (e: URISyntaxException) {
    false
}

private suspend fun ApplicationCall.respondError(
    message: String,
    status: HttpStatusCode = HttpStatusCode.BadRequest
) {
    respondText(
        JSONObject().put("error", message).toString(),
        ContentType.Application.Json,
        status
    )
}