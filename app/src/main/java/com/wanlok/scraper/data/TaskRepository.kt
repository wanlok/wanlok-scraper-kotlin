package com.wanlok.scraper.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * The saved tasks. Kept in memory for the UI and saved to tasks.json so they survive restarts.
 * Whatever adds tasks later (the Add form, a web server) goes through [add].
 */
object TaskRepository {
    private const val FILE_NAME = "tasks.json"

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    private var file: File? = null

    @Synchronized
    fun load(context: Context) {
        if (file != null) return
        val tasksFile = File(context.applicationContext.filesDir, FILE_NAME)
        file = tasksFile
        if (tasksFile.exists()) {
            _tasks.value = parse(tasksFile.readText())
        }
    }

    @Synchronized
    fun add(context: Context, name: String, url: String, selector: String): Task {
        load(context)
        val task = Task(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            url = url.trim(),
            selector = selector.trim()
        )
        val updated = _tasks.value + task
        _tasks.value = updated
        file?.writeText(toJson(updated))
        return task
    }

    private fun toJson(tasks: List<Task>): String {
        val array = JSONArray()
        tasks.forEach {
            array.put(
                JSONObject()
                    .put("id", it.id)
                    .put("name", it.name)
                    .put("url", it.url)
                    .put("selector", it.selector)
            )
        }
        return array.toString()
    }

    private fun parse(text: String): List<Task> = try {
        val array = JSONArray(text)
        List(array.length()) { index ->
            val item = array.getJSONObject(index)
            Task(
                id = item.getString("id"),
                name = item.getString("name"),
                url = item.getString("url"),
                selector = item.getString("selector")
            )
        }
    } catch (e: JSONException) {
        emptyList()
    }
}
