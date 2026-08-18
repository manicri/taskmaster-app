package com.cris.taskmaster.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TaskItem(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val description: String = "",
    val dueDateMillis: Long? = null,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val category: TaskCategory = TaskCategory.PERSONAL,
    val isCompleted: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    fun getFormattedDueDate(): String {
        return if (dueDateMillis != null && dueDateMillis > 0) {
            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("es", "ES"))
            sdf.format(Date(dueDateMillis))
        } else {
            "Sin fecha límite"
        }
    }

    fun isOverdue(): Boolean {
        return if (dueDateMillis != null && !isCompleted) {
            System.currentTimeMillis() > dueDateMillis
        } else {
            false
        }
    }
}
