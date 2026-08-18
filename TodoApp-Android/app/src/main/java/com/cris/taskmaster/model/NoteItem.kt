package com.cris.taskmaster.model

import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class NoteItem(
    val id: Long = System.currentTimeMillis(),
    val title: String = "",
    val content: String,
    val colorHex: String = "#FEF9C3", // Default light yellow post-it
    val isPinned: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    fun getParsedColor(): Color {
        return try {
            val cleanHex = colorHex.removePrefix("#")
            val colorInt = cleanHex.toLong(16) or 0x00000000FF000000
            Color(colorInt)
        } catch (e: Exception) {
            Color(0xFFFEF9C3)
        }
    }

    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale("es", "ES"))
        return sdf.format(Date(updatedAtMillis))
    }
}
