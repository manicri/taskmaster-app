package com.cris.taskmaster.data

import com.cris.taskmaster.model.NoteItem
import com.cris.taskmaster.model.TaskCategory
import com.cris.taskmaster.model.TaskItem
import com.cris.taskmaster.model.TaskPriority
import com.cris.taskmaster.model.UserAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class TaskApiClient {

    suspend fun checkHealth(serverUrl: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = serverUrl.trimEnd('/')
            val url = URL("$cleanUrl/api/health")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 3000
                readTimeout = 3000
            }
            connection.responseCode == 200
        } catch (e: Exception) {
            false
        }
    }

    // ==========================================
    // AUTHENTICATION
    // ==========================================
    suspend fun register(serverUrl: String, name: String, email: String, pass: String): Pair<Boolean, Any> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = serverUrl.trimEnd('/')
            val url = URL("$cleanUrl/api/auth/register")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 5000
                readTimeout = 5000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
            }

            val body = JSONObject().apply {
                put("name", name)
                put("email", email)
                put("password", pass)
            }

            OutputStreamWriter(connection.outputStream).use { it.write(body.toString()); it.flush() }

            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            val responseText = BufferedReader(InputStreamReader(stream)).use { it.readText() }
            val json = JSONObject(responseText)

            if (connection.responseCode in 200..299) {
                val userJson = json.getJSONObject("user")
                val user = UserAccount(
                    id = userJson.getString("id"),
                    name = userJson.getString("name"),
                    email = userJson.getString("email")
                )
                return@withContext Pair(true, user)
            } else {
                return@withContext Pair(false, json.optString("error", "Error al registrar cuenta"))
            }
        } catch (e: Exception) {
            return@withContext Pair(false, "No se pudo conectar al servidor: ${e.message}")
        }
    }

    suspend fun login(serverUrl: String, email: String, pass: String): Pair<Boolean, Any> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = serverUrl.trimEnd('/')
            val url = URL("$cleanUrl/api/auth/login")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 5000
                readTimeout = 5000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
            }

            val body = JSONObject().apply {
                put("email", email)
                put("password", pass)
            }

            OutputStreamWriter(connection.outputStream).use { it.write(body.toString()); it.flush() }

            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            val responseText = BufferedReader(InputStreamReader(stream)).use { it.readText() }
            val json = JSONObject(responseText)

            if (connection.responseCode in 200..299) {
                val userJson = json.getJSONObject("user")
                val user = UserAccount(
                    id = userJson.getString("id"),
                    name = userJson.getString("name"),
                    email = userJson.getString("email")
                )
                return@withContext Pair(true, user)
            } else {
                return@withContext Pair(false, json.optString("error", "Credenciales inválidas"))
            }
        } catch (e: Exception) {
            return@withContext Pair(false, "No se pudo conectar al servidor: ${e.message}")
        }
    }

    // ==========================================
    // SYNC SCOPED BY USER
    // ==========================================
    suspend fun syncTasks(serverUrl: String, userId: String, localTasks: List<TaskItem>): List<TaskItem>? = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = serverUrl.trimEnd('/')
            val url = URL("$cleanUrl/api/tasks/sync")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 5000
                readTimeout = 5000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
            }

            val rootJson = JSONObject()
            rootJson.put("userId", userId)
            val tasksArray = JSONArray()

            localTasks.forEach { task ->
                val taskJson = JSONObject().apply {
                    put("id", task.id)
                    put("userId", userId)
                    put("title", task.title)
                    put("description", task.description)
                    put("dueDateMillis", task.dueDateMillis ?: JSONObject.NULL)
                    put("priority", task.priority.name.lowercase())
                    put("category", task.category.name.lowercase())
                    put("isCompleted", task.isCompleted)
                    put("createdAt", task.createdAtMillis)
                    put("updatedAt", task.createdAtMillis)
                }
                tasksArray.put(taskJson)
            }
            rootJson.put("tasks", tasksArray)

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(rootJson.toString())
                writer.flush()
            }

            if (connection.responseCode in 200..299) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val responseText = reader.readText()
                reader.close()

                val responseJson = JSONObject(responseText)
                val serverTasksJson = responseJson.optJSONArray("tasks") ?: JSONArray()
                val mergedList = mutableListOf<TaskItem>()

                for (i in 0 until serverTasksJson.length()) {
                    val item = serverTasksJson.getJSONObject(i)
                    val id = item.optLong("id", System.currentTimeMillis())
                    val title = item.optString("title", "Sin título")
                    val description = item.optString("description", "")
                    val dueDateMillis = if (item.isNull("dueDateMillis")) null else item.optLong("dueDateMillis")
                    val priorityStr = item.optString("priority", "media")
                    val categoryStr = item.optString("category", "personal")
                    val isCompleted = item.optBoolean("isCompleted", false)
                    val createdAt = item.optLong("createdAt", System.currentTimeMillis())

                    mergedList.add(
                        TaskItem(
                            id = id,
                            title = title,
                            description = description,
                            dueDateMillis = dueDateMillis,
                            priority = TaskPriority.fromString(priorityStr),
                            category = TaskCategory.fromString(categoryStr),
                            isCompleted = isCompleted,
                            createdAtMillis = createdAt
                        )
                    )
                }
                return@withContext mergedList
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun syncNotes(serverUrl: String, userId: String, localNotes: List<NoteItem>): List<NoteItem>? = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = serverUrl.trimEnd('/')
            val url = URL("$cleanUrl/api/notes/sync")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 5000
                readTimeout = 5000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
            }

            val rootJson = JSONObject()
            rootJson.put("userId", userId)
            val notesArray = JSONArray()

            localNotes.forEach { note ->
                val noteJson = JSONObject().apply {
                    put("id", note.id)
                    put("userId", userId)
                    put("title", note.title)
                    put("content", note.content)
                    put("color", note.colorHex)
                    put("isPinned", note.isPinned)
                    put("createdAt", note.createdAtMillis)
                    put("updatedAt", note.updatedAtMillis)
                }
                notesArray.put(noteJson)
            }
            rootJson.put("notes", notesArray)

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(rootJson.toString())
                writer.flush()
            }

            if (connection.responseCode in 200..299) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val responseText = reader.readText()
                reader.close()

                val responseJson = JSONObject(responseText)
                val serverNotesJson = responseJson.optJSONArray("notes") ?: JSONArray()
                val mergedList = mutableListOf<NoteItem>()

                for (i in 0 until serverNotesJson.length()) {
                    val item = serverNotesJson.getJSONObject(i)
                    val id = item.optLong("id", System.currentTimeMillis())
                    val title = item.optString("title", "")
                    val content = item.optString("content", "")
                    val colorHex = item.optString("color", "#FEF9C3")
                    val isPinned = item.optBoolean("isPinned", false)
                    val createdAt = item.optLong("createdAt", System.currentTimeMillis())
                    val updatedAt = item.optLong("updatedAt", System.currentTimeMillis())

                    mergedList.add(
                        NoteItem(
                            id = id,
                            title = title,
                            content = content,
                            colorHex = colorHex,
                            isPinned = isPinned,
                            createdAtMillis = createdAt,
                            updatedAtMillis = updatedAt
                        )
                    )
                }
                return@withContext mergedList
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
