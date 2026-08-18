package com.cris.taskmaster.model

enum class TaskCategory(val label: String, val iconName: String) {
    ALL("Todas", "FormatListBulleted"),
    WORK("Trabajo", "Work"),
    PERSONAL("Personal", "Person"),
    STUDY("Estudio", "School"),
    HEALTH("Salud", "Favorite"),
    OTHER("Otros", "Bookmark");

    companion object {
        fun fromString(value: String): TaskCategory {
            return entries.find { it.name.equals(value, ignoreCase = true) || it.label.equals(value, ignoreCase = true) }
                ?: PERSONAL
        }
    }
}
