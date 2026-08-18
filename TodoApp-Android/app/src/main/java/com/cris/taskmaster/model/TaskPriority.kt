package com.cris.taskmaster.model

import androidx.compose.ui.graphics.Color

enum class TaskPriority(val label: String, val colorValue: Long) {
    LOW("Baja", 0xFF10B981),      // Verde esmeralda
    MEDIUM("Media", 0xFFF59E0B),  // Ámbar
    HIGH("Alta", 0xFFEF4444);     // Rojo coral

    fun getColor(): Color = Color(colorValue)

    companion object {
        fun fromString(value: String): TaskPriority {
            return entries.find { it.name.equals(value, ignoreCase = true) || it.label.equals(value, ignoreCase = true) }
                ?: MEDIUM
        }
    }
}
