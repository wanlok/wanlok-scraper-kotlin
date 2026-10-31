package com.wanlok.scraper.server

import android.content.Context
import com.wanlok.scraper.data.TaskRepository
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
import org.json.JSONException
import org.json.JSONObject
import java.net.URI
import java.net.URISyntaxException

/** The task API used by the desktop page. Everything is JSON. */
fun Route.taskRoutes(context: Context) {
    // Runs every task now, the same way the periodic job does. Handy for testing.
    post("/api/run") {
        TaskRunner.runAll(context)
        call.respond(HttpStatusCode.NoContent)
    }

    route("/api/tasks") {
        get {
            TaskRepository.load(context)
            call.respondText(
                TaskRepository.toJson(TaskRepository.tasks.value),
                ContentType.Application.Json
            )
        }

        post {
            val input = call.receiveTaskInput() ?: return@post
            val task = TaskRepository.add(context, input.name, input.url, input.selector)
            call.respondText(
                TaskRepository.toJson(task).toString(),
                ContentType.Application.Json,
                HttpStatusCode.Created
            )
        }

        put("{id}") {
            val input = call.receiveTaskInput() ?: return@put
            val id = call.parameters["id"].orEmpty()
            val task = TaskRepository.update(context, id, input.name, input.url, input.selector)
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
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respondError("No task with that id", HttpStatusCode.NotFound)
            }
        }
    }
}

private data class TaskInput(val name: String, val url: String, val selector: String)

/** Reads and validates a task from the request body. Responds 400 and returns null if invalid. */
private suspend fun ApplicationCall.receiveTaskInput(): TaskInput? {
    val body = try {
        JSONObject(receiveText())
    } catch (e: JSONException) {
        respondError("The body must be JSON")
        return null
    }
    val input = TaskInput(
        name = body.optString("name"),
        url = body.optString("url"),
        selector = body.optString("selector")
    )
    val error = validate(input)
    if (error != null) {
        respondError(error)
        return null
    }
    return input
}

private fun validate(input: TaskInput): String? = when {
    input.name.isBlank() -> "name is required"
    input.selector.isBlank() -> "selector is required"
    !isHttpUrl(input.url) -> "url must be an http:// or https:// address"
    else -> null
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