package com.cris.taskmaster.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.cris.taskmaster.model.NoteItem

@Composable
fun AddNoteDialog(
    noteToEdit: NoteItem? = null,
    onDismiss: () -> Unit,
    onSaveNote: (NoteItem) -> Unit
) {
    var title by remember { mutableStateOf(noteToEdit?.title ?: "") }
    var content by remember { mutableStateOf(noteToEdit?.content ?: "") }
    var selectedColorHex by remember { mutableStateOf(noteToEdit?.colorHex ?: "#FEF9C3") }
    var isPinned by remember { mutableStateOf(noteToEdit?.isPinned ?: false) }
    var contentError by remember { mutableStateOf(false) }

    val colorPalette = listOf(
        "#FEF9C3" to Color(0xFFFEF9C3), // Amarillo
        "#DCFCE7" to Color(0xFFDCFCE7), // Menta
        "#E0F2FE" to Color(0xFFE0F2FE), // Azul cielo
        "#F3E8FF" to Color(0xFFF3E8FF), // Morado
        "#FCE7F3" to Color(0xFFFCE7F3), // Rosa
        "#F1F5F9" to Color(0xFFF1F5F9)  // Gris neutro
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (noteToEdit == null) "Nueva Nota Rápida" else "Editar Nota",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Campo Título
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título (opcional)") },
                    placeholder = { Text("Ej. Ideas para la semana") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Campo Contenido
                OutlinedTextField(
                    value = content,
                    onValueChange = {
                        content = it
                        if (it.isNotBlank()) contentError = false
                    },
                    label = { Text("Contenido de la nota *") },
                    placeholder = { Text("Escribe aquí lo que no quieres olvidar...") },
                    isError = contentError,
                    supportingText = {
                        if (contentError) Text("El contenido no puede estar vacío", color = Color(0xFFEF4444))
                    },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Selector de Color
                Text(
                    text = "Color de la Nota",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    colorPalette.forEach { (hex, color) ->
                        val isSelected = hex.equals(selectedColorHex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                    shape = CircleShape
                                )
                                .clickable { selectedColorHex = hex }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Checkbox Fijar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { isPinned = !isPinned }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = isPinned,
                        onCheckedChange = { isPinned = it }
                    )
                    Text(
                        text = "📌 Fijar nota al inicio",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = {
                            if (content.isBlank()) {
                                contentError = true
                                return@Button
                            }
                            val note = NoteItem(
                                id = noteToEdit?.id ?: System.currentTimeMillis(),
                                title = title.trim(),
                                content = content.trim(),
                                colorHex = selectedColorHex,
                                isPinned = isPinned,
                                createdAtMillis = noteToEdit?.createdAtMillis ?: System.currentTimeMillis(),
                                updatedAtMillis = System.currentTimeMillis()
                            )
                            onSaveNote(note)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (noteToEdit == null) "Guardar Nota" else "Actualizar")
                    }
                }
            }
        }
    }
}
