package com.wanlok.scraper.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.text.ParseException

/**
 * The snapshots of each task, one file per task (snapshots/<task id>.jsonl) with one JSON object
 * per run. Only ids of tasks that exist should be passed in, since the id becomes a file name.
 */
object SnapshotRepository {
    private const val DIR_NAME = "snapshots"

    @Synchronized
    fun append(context: Context, taskId: String, snapshot: Snapshot) {
        fileFor(context, taskId).appendText(toJson(snapshot).toString() + "\n")
    }

    /** The most recent [limit] snapshots of the task, newest first. */
    @Synchronized
    fun latest(context: Context, taskId: String, limit: Int): List<Snapshot> {
        val file = fileFor(context, taskId)
        if (!file.exists()) return emptyList()
        return file.readLines().mapNotNull(::parse).takeLast(limit).reversed()
    }

    @Synchronized
    fun delete(context: Context, taskId: String) {
        fileFor(context, taskId).delete()
    }

    fun toJson(snapshot: Snapshot): JSONObject {
        val json = JSONObject()
            .put("date", IsoDate.format(snapshot.date))
            .put("status", snapshot.status.name)
        snapshot.data?.let { items ->
            val array = JSONArray()
            items.forEach { array.put(itemToJson(it)) }
            json.put("data", array)
        }
        snapshot.error?.let { json.put("error", it) }
        return json
    }

    // A text item is a JSON string; an item with fields is a JSON object, with null for a missing field.
    private fun itemToJson(item: ScrapedItem): Any = when (item) {
        is ScrapedItem.Text -> item.value
        is ScrapedItem.Fields -> JSONObject().apply {
            item.values.forEach { (name, value) -> put(name, value ?: JSONObject.NULL) }
        }
    }

    private fun itemFromJson(value: Any): ScrapedItem = when (value) {
        is String -> ScrapedItem.Text(value)
        is JSONObject -> ScrapedItem.Fields(
            value.keys().asSequence().associateWith { if (value.isNull(it)) null else value.getString(it) }
        )
        else -> throw JSONException("Unknown item")
    }

    // Skips lines that aren't valid snapshots.
    private fun parse(line: String): Snapshot? = try {
        val json = JSONObject(line)
        Snapshot(
            date = IsoDate.parse(json.getString("date")),
            status = LogStatus.valueOf(json.getString("status")),
            data = if (json.has("data")) {
                val array = json.getJSONArray("data")
                List(array.length()) { itemFromJson(array.get(it)) }
            } else {
                null
            },
            error = if (json.has("error")) json.getString("error") else null
        )
    } catch (e: JSONException) {
        null
    } catch (e: ParseException) {
        null
    } catch (e: IllegalArgumentException) {
        // A status name this version doesn't know.
        null
    }

    private fun fileFor(context: Context, taskId: String): File {
        val dir = File(context.applicationContext.filesDir, DIR_NAME).apply { mkdirs() }
        return File(dir, "$taskId.jsonl")
    }
}