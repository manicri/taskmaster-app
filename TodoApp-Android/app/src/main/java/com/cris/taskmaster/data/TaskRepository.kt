package com.cris.taskmaster.data

import android.content.Context
import android.content.SharedPreferences
import com.cris.taskmaster.model.NoteItem
import com.cris.taskmaster.model.TaskCategory
import com.cris.taskmaster.model.TaskItem
import com.cris.taskmaster.model.TaskPriority
import com.cris.taskmaster.model.UserAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TaskRepository(context: Context) {
    private val dbHelper = TaskDatabaseHelper(context.applicationContext)
    private val apiClient = TaskApiClient()
    private val prefs: SharedPreferences = context.getSharedPreferences("taskmaster_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val PREF_SERVER_URL = "server_url"
        private const val PREF_USER_ID = "user_id"
        private const val PREF_USER_NAME = "user_name"
        private const val PREF_USER_EMAIL = "user_email"
        const val DEFAULT_SERVER_URL = "http://10.0.2.2:3000"
    }

    fun getServerUrl(): String {
        return prefs.getString(PREF_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
    }

    fun setServerUrl(url: String) {
        prefs.edit().putString(PREF_SERVER_URL, url.trim()).apply()
    }

    // ==========================================
    // USER SESSION MANAGEMENT
    // ==========================================
    fun getCurrentUser(): UserAccount? {
        val id = prefs.getString(PREF_USER_ID, null) ?: return null
        val name = prefs.getString(PREF_USER_NAME, "") ?: ""
        val email = prefs.getString(PREF_USER_EMAIL, "") ?: ""
        return UserAccount(id = id, name = name, email = email)
    }

    fun saveCurrentUser(user: UserAccount?) {
        if (user != null) {
            prefs.edit()
                .putString(PREF_USER_ID, user.id)
                .putString(PREF_USER_NAME, user.name)
                .putString(PREF_USER_EMAIL, user.email)
                .apply()
        } else {
            prefs.edit()
                .remove(PREF_USER_ID)
                .remove(PREF_USER_NAME)
                .remove(PREF_USER_EMAIL)
                .apply()
        }
    }

    suspend fun login(email: String, pass: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val serverUrl = getServerUrl()
        val (success, result) = apiClient.login(serverUrl, email, pass)
        if (success && result is UserAccount) {
            saveCurrentUser(result)
            return@withContext Pair(true, "¡Bienvenido, ${result.name}!")
        } else {
            return@withContext Pair(false, result.toString())
        }
    }

    suspend fun register(name: String, email: String, pass: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val serverUrl = getServerUrl()
        val (success, result) = apiClient.register(serverUrl, name, email, pass)
        if (success && result is UserAccount) {
            saveCurrentUser(result)
            return@withContext Pair(true, "Cuenta creada exitosamente. ¡Bienvenido!")
        } else {
            return@withContext Pair(false, result.toString())
        }
    }

    fun logout() {
        saveCurrentUser(null)
    }

    private fun getActiveUserId(): String {
        return getCurrentUser()?.id ?: "default"
    }

    // ==========================================
    // TASK METHODS
    // ==========================================
    suspend fun getTasks(): List<TaskItem> = withContext(Dispatchers.IO) {
        val userId = getActiveUserId()
        val tasks = dbHelper.getTasksForUser(userId)
        if (tasks.isEmpty() && getCurrentUser() == null) {
            val initialTasks = listOf(
                TaskItem(
                    id = 1L,
                    title = "¡Bienvenido a TaskMaster!",
                    description = "Inicia sesión para sincronizar tus tareas y notas con la web.",
                    dueDateMillis = System.currentTimeMillis() + 86400000L,
                    priority = TaskPriority.HIGH,
                    category = TaskCategory.PERSONAL,
                    isCompleted = false
                )
            )
            for (t in initialTasks) {
                dbHelper.insertTask(t, userId)
            }
            return@withContext initialTasks
        }
        tasks
    }

    suspend fun addTask(task: TaskItem) = withContext(Dispatchers.IO) {
        dbHelper.insertTask(task, getActiveUserId())
    }

    suspend fun updateTask(task: TaskItem) = withContext(Dispatchers.IO) {
        dbHelper.updateTask(task, getActiveUserId())
    }

    suspend fun deleteTask(taskId: Long) = withContext(Dispatchers.IO) {
        dbHelper.deleteTask(taskId, getActiveUserId())
    }

    // ==========================================
    // NOTE METHODS
    // ==========================================
    suspend fun getNotes(): List<NoteItem> = withContext(Dispatchers.IO) {
        val userId = getActiveUserId()
        val notes = dbHelper.getNotesForUser(userId)
        if (notes.isEmpty() && getCurrentUser() == null) {
            val initialNotes = listOf(
                NoteItem(
                    id = 101L,
                    title = "💡 Nota de bienvenida",
                    content = "Crea tu cuenta para que tus notas estén sincronizadas en todos tus dispositivos.",
                    colorHex = "#FEF9C3",
                    isPinned = true
                )
            )
            for (n in initialNotes) {
                dbHelper.insertNote(n, userId)
            }
            return@withContext initialNotes
        }
        notes
    }

    suspend fun addNote(note: NoteItem) = withContext(Dispatchers.IO) {
        dbHelper.insertNote(note, getActiveUserId())
    }

    suspend fun updateNote(note: NoteItem) = withContext(Dispatchers.IO) {
        dbHelper.updateNote(note, getActiveUserId())
    }

    suspend fun deleteNote(noteId: Long) = withContext(Dispatchers.IO) {
        dbHelper.deleteNote(noteId, getActiveUserId())
    }

    // ==========================================
    // SYNCHRONIZATION
    // ==========================================
    suspend fun syncWithServer(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val user = getCurrentUser() ?: return@withContext Pair(false, "Inicia sesión para sincronizar")
        val serverUrl = getServerUrl()
        val userId = user.id

        val localTasks = dbHelper.getTasksForUser(userId)
        val localNotes = dbHelper.getNotesForUser(userId)

        val remoteTasks = apiClient.syncTasks(serverUrl, userId, localTasks)
        val remoteNotes = apiClient.syncNotes(serverUrl, userId, localNotes)

        var syncedItemsCount = 0

        if (remoteTasks != null) {
            remoteTasks.forEach { task -> dbHelper.insertTask(task, userId) }
            syncedItemsCount += remoteTasks.size
        }

        if (remoteNotes != null) {
            remoteNotes.forEach { note -> dbHelper.insertNote(note, userId) }
            syncedItemsCount += remoteNotes.size
        }

        if (remoteTasks != null || remoteNotes != null) {
            return@withContext Pair(true, "Sincronizado ($syncedItemsCount elementos)")
        } else {
            return@withContext Pair(false, "No se pudo conectar a $serverUrl")
        }
    }
}
