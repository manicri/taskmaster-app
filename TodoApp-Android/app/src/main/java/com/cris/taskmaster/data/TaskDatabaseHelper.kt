package com.cris.taskmaster.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.cris.taskmaster.model.NoteItem
import com.cris.taskmaster.model.TaskCategory
import com.cris.taskmaster.model.TaskItem
import com.cris.taskmaster.model.TaskPriority

class TaskDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "taskmaster.db"
        private const val DATABASE_VERSION = 3

        // Table Tasks
        const val TABLE_TASKS = "tasks"
        const val COLUMN_ID = "id"
        const val COLUMN_USER_ID = "user_id"
        const val COLUMN_TITLE = "title"
        const val COLUMN_DESCRIPTION = "description"
        const val COLUMN_DUE_DATE = "due_date"
        const val COLUMN_PRIORITY = "priority"
        const val COLUMN_CATEGORY = "category"
        const val COLUMN_IS_COMPLETED = "is_completed"
        const val COLUMN_CREATED_AT = "created_at"

        // Table Notes
        const val TABLE_NOTES = "notes"
        const val COLUMN_NOTE_ID = "id"
        const val COLUMN_NOTE_USER_ID = "user_id"
        const val COLUMN_NOTE_TITLE = "title"
        const val COLUMN_NOTE_CONTENT = "content"
        const val COLUMN_NOTE_COLOR = "color"
        const val COLUMN_NOTE_IS_PINNED = "is_pinned"
        const val COLUMN_NOTE_CREATED_AT = "created_at"
        const val COLUMN_NOTE_UPDATED_AT = "updated_at"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTasksTable = """
            CREATE TABLE $TABLE_TASKS (
                $COLUMN_ID INTEGER PRIMARY KEY,
                $COLUMN_USER_ID TEXT NOT NULL DEFAULT 'default',
                $COLUMN_TITLE TEXT NOT NULL,
                $COLUMN_DESCRIPTION TEXT,
                $COLUMN_DUE_DATE INTEGER,
                $COLUMN_PRIORITY TEXT NOT NULL,
                $COLUMN_CATEGORY TEXT NOT NULL,
                $COLUMN_IS_COMPLETED INTEGER NOT NULL DEFAULT 0,
                $COLUMN_CREATED_AT INTEGER NOT NULL
            )
        """.trimIndent()
        db.execSQL(createTasksTable)

        val createNotesTable = """
            CREATE TABLE $TABLE_NOTES (
                $COLUMN_NOTE_ID INTEGER PRIMARY KEY,
                $COLUMN_NOTE_USER_ID TEXT NOT NULL DEFAULT 'default',
                $COLUMN_NOTE_TITLE TEXT,
                $COLUMN_NOTE_CONTENT TEXT NOT NULL,
                $COLUMN_NOTE_COLOR TEXT NOT NULL DEFAULT '#FEF9C3',
                $COLUMN_NOTE_IS_PINNED INTEGER NOT NULL DEFAULT 0,
                $COLUMN_NOTE_CREATED_AT INTEGER NOT NULL,
                $COLUMN_NOTE_UPDATED_AT INTEGER NOT NULL
            )
        """.trimIndent()
        db.execSQL(createNotesTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 3) {
            try {
                db.execSQL("ALTER TABLE $TABLE_TASKS ADD COLUMN $COLUMN_USER_ID TEXT NOT NULL DEFAULT 'default'")
            } catch (_: Exception) {}
            try {
                db.execSQL("ALTER TABLE $TABLE_NOTES ADD COLUMN $COLUMN_NOTE_USER_ID TEXT NOT NULL DEFAULT 'default'")
            } catch (_: Exception) {}
        }
    }

    // ==========================================
    // TASK DATABASE METHODS
    // ==========================================
    fun insertTask(task: TaskItem, userId: String = "default"): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ID, task.id)
            put(COLUMN_USER_ID, userId)
            put(COLUMN_TITLE, task.title)
            put(COLUMN_DESCRIPTION, task.description)
            put(COLUMN_DUE_DATE, task.dueDateMillis)
            put(COLUMN_PRIORITY, task.priority.name)
            put(COLUMN_CATEGORY, task.category.name)
            put(COLUMN_IS_COMPLETED, if (task.isCompleted) 1 else 0)
            put(COLUMN_CREATED_AT, task.createdAtMillis)
        }
        return db.insertWithOnConflict(TABLE_TASKS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun updateTask(task: TaskItem, userId: String = "default"): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE, task.title)
            put(COLUMN_DESCRIPTION, task.description)
            put(COLUMN_DUE_DATE, task.dueDateMillis)
            put(COLUMN_PRIORITY, task.priority.name)
            put(COLUMN_CATEGORY, task.category.name)
            put(COLUMN_IS_COMPLETED, if (task.isCompleted) 1 else 0)
        }
        return db.update(TABLE_TASKS, values, "$COLUMN_ID = ? AND $COLUMN_USER_ID = ?", arrayOf(task.id.toString(), userId))
    }

    fun deleteTask(taskId: Long, userId: String = "default"): Int {
        val db = writableDatabase
        return db.delete(TABLE_TASKS, "$COLUMN_ID = ? AND $COLUMN_USER_ID = ?", arrayOf(taskId.toString(), userId))
    }

    fun getTasksForUser(userId: String = "default"): List<TaskItem> {
        val taskList = mutableListOf<TaskItem>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_TASKS,
            null,
            "$COLUMN_USER_ID = ?",
            arrayOf(userId),
            null,
            null,
            "$COLUMN_IS_COMPLETED ASC, $COLUMN_DUE_DATE ASC, $COLUMN_CREATED_AT DESC"
        )

        cursor.use {
            if (it.moveToFirst()) {
                do {
                    val id = it.getLong(it.getColumnIndexOrThrow(COLUMN_ID))
                    val title = it.getString(it.getColumnIndexOrThrow(COLUMN_TITLE))
                    val description = it.getString(it.getColumnIndexOrThrow(COLUMN_DESCRIPTION)) ?: ""
                    val dueDateIndex = it.getColumnIndexOrThrow(COLUMN_DUE_DATE)
                    val dueDate = if (it.isNull(dueDateIndex)) null else it.getLong(dueDateIndex)
                    val priorityStr = it.getString(it.getColumnIndexOrThrow(COLUMN_PRIORITY))
                    val categoryStr = it.getString(it.getColumnIndexOrThrow(COLUMN_CATEGORY))
                    val isCompleted = it.getInt(it.getColumnIndexOrThrow(COLUMN_IS_COMPLETED)) == 1
                    val createdAt = it.getLong(it.getColumnIndexOrThrow(COLUMN_CREATED_AT))

                    taskList.add(
                        TaskItem(
                            id = id,
                            title = title,
                            description = description,
                            dueDateMillis = dueDate,
                            priority = TaskPriority.fromString(priorityStr),
                            category = TaskCategory.fromString(categoryStr),
                            isCompleted = isCompleted,
                            createdAtMillis = createdAt
                        )
                    )
                } while (it.moveToNext())
            }
        }
        return taskList
    }

    // ==========================================
    // NOTE DATABASE METHODS
    // ==========================================
    fun insertNote(note: NoteItem, userId: String = "default"): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NOTE_ID, note.id)
            put(COLUMN_NOTE_USER_ID, userId)
            put(COLUMN_NOTE_TITLE, note.title)
            put(COLUMN_NOTE_CONTENT, note.content)
            put(COLUMN_NOTE_COLOR, note.colorHex)
            put(COLUMN_NOTE_IS_PINNED, if (note.isPinned) 1 else 0)
            put(COLUMN_NOTE_CREATED_AT, note.createdAtMillis)
            put(COLUMN_NOTE_UPDATED_AT, note.updatedAtMillis)
        }
        return db.insertWithOnConflict(TABLE_NOTES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun updateNote(note: NoteItem, userId: String = "default"): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NOTE_TITLE, note.title)
            put(COLUMN_NOTE_CONTENT, note.content)
            put(COLUMN_NOTE_COLOR, note.colorHex)
            put(COLUMN_NOTE_IS_PINNED, if (note.isPinned) 1 else 0)
            put(COLUMN_NOTE_UPDATED_AT, note.updatedAtMillis)
        }
        return db.update(TABLE_NOTES, values, "$COLUMN_NOTE_ID = ? AND $COLUMN_NOTE_USER_ID = ?", arrayOf(note.id.toString(), userId))
    }

    fun deleteNote(noteId: Long, userId: String = "default"): Int {
        val db = writableDatabase
        return db.delete(TABLE_NOTES, "$COLUMN_NOTE_ID = ? AND $COLUMN_NOTE_USER_ID = ?", arrayOf(noteId.toString(), userId))
    }

    fun getNotesForUser(userId: String = "default"): List<NoteItem> {
        val noteList = mutableListOf<NoteItem>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NOTES,
            null,
            "$COLUMN_NOTE_USER_ID = ?",
            arrayOf(userId),
            null,
            null,
            "$COLUMN_NOTE_IS_PINNED DESC, $COLUMN_NOTE_UPDATED_AT DESC"
        )

        cursor.use {
            if (it.moveToFirst()) {
                do {
                    val id = it.getLong(it.getColumnIndexOrThrow(COLUMN_NOTE_ID))
                    val title = it.getString(it.getColumnIndexOrThrow(COLUMN_NOTE_TITLE)) ?: ""
                    val content = it.getString(it.getColumnIndexOrThrow(COLUMN_NOTE_CONTENT))
                    val colorHex = it.getString(it.getColumnIndexOrThrow(COLUMN_NOTE_COLOR)) ?: "#FEF9C3"
                    val isPinned = it.getInt(it.getColumnIndexOrThrow(COLUMN_NOTE_IS_PINNED)) == 1
                    val createdAt = it.getLong(it.getColumnIndexOrThrow(COLUMN_NOTE_CREATED_AT))
                    val updatedAt = it.getLong(it.getColumnIndexOrThrow(COLUMN_NOTE_UPDATED_AT))

                    noteList.add(
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
                } while (it.moveToNext())
            }
        }
        return noteList
    }
}
