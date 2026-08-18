package com.cris.taskmaster.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cris.taskmaster.model.NoteItem
import com.cris.taskmaster.model.TaskItem
import com.cris.taskmaster.ui.components.AddNoteDialog
import com.cris.taskmaster.ui.components.AddTaskDialog
import com.cris.taskmaster.ui.components.CategoryFilterRow
import com.cris.taskmaster.ui.components.SearchBarView
import com.cris.taskmaster.ui.components.ServerSettingsDialog
import com.cris.taskmaster.ui.components.StatsHeader
import com.cris.taskmaster.ui.components.StatusFilterRow
import com.cris.taskmaster.ui.components.TaskCard
import com.cris.taskmaster.ui.components.UserProfileDialog
import com.cris.taskmaster.ui.theme.PrimaryBlue
import com.cris.taskmaster.ui.viewmodel.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: TaskViewModel
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val allTasks by viewModel.tasks.collectAsState()
    val filteredTasks by viewModel.filteredTasks.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedStatus by viewModel.selectedStatus.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()

    var showTaskDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<TaskItem?>(null) }

    var showNoteDialog by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<NoteItem?>(null) }

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(syncMessage) {
        syncMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSyncMessage()
        }
    }

    val totalTasks = allTasks.size
    val completedTasks = allTasks.count { it.isCompleted }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PrimaryBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (selectedTab == 0) Icons.Default.AssignmentTurnedIn else Icons.Default.Description,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = currentUser?.let { "Hola, ${it.name.split(" ").firstOrNull() ?: it.name}" } ?: "TaskMaster",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = if (selectedTab == 0) "Tareas Sincronizadas" else "Notas Rápidas",
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 11.sp,
                                color = PrimaryBlue
                            )
                        }
                    }
                },
                actions = {
                    // Botón Sincronizar
                    IconButton(
                        onClick = { viewModel.syncWithServer() },
                        enabled = !isSyncing
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = PrimaryBlue
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sincronizar ahora",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Botón Ajustes de Servidor
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configuración de servidor",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Avatar de Usuario
                    if (currentUser != null) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(PrimaryBlue)
                                .clickable { showProfileDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentUser?.name?.take(1)?.uppercase() ?: "U",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.size(8.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { viewModel.setSelectedTab(0) },
                    icon = { Icon(Icons.Default.FormatListBulleted, contentDescription = "Tareas") },
                    label = { Text("Tareas") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { viewModel.setSelectedTab(1) },
                    icon = { Icon(Icons.Default.Description, contentDescription = "Notas Rápidas") },
                    label = { Text("Notas Rápidas") }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) {
                        taskToEdit = null
                        showTaskDialog = true
                    } else {
                        noteToEdit = null
                        showNoteDialog = true
                    }
                },
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(60.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (selectedTab == 0) "Agregar Tarea" else "Agregar Nota",
                    modifier = Modifier.size(30.dp)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (selectedTab == 0) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp)
                ) {
                    Spacer(modifier = Modifier.height(10.dp))

                    StatsHeader(
                        totalTasks = totalTasks,
                        completedTasks = completedTasks
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    SearchBarView(
                        query = searchQuery,
                        onQueryChanged = { viewModel.setSearchQuery(it) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    CategoryFilterRow(
                        selectedCategory = selectedCategory,
                        onCategorySelected = { viewModel.setSelectedCategory(it) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    StatusFilterRow(
                        selectedStatus = selectedStatus,
                        onStatusSelected = { viewModel.setSelectedStatus(it) }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = PrimaryBlue)
                        }
                    } else if (filteredTasks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircleOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(72.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (searchQuery.isNotBlank()) "No se encontraron tareas con '$searchQuery'"
                                    else "¡No hay tareas pendientes en este filtro!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Presiona el botón '+' para registrar una nueva tarea.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(
                                items = filteredTasks,
                                key = { it.id }
                            ) { task ->
                                TaskCard(
                                    task = task,
                                    onToggleCompleted = { viewModel.toggleTaskCompleted(it) },
                                    onDelete = { viewModel.deleteTask(it) },
                                    onClick = {
                                        taskToEdit = task
                                        showTaskDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                NotesScreen(
                    viewModel = viewModel,
                    onEditNote = { note ->
                        noteToEdit = note
                        showNoteDialog = true
                    }
                )
            }
        }
    }

    if (showTaskDialog) {
        AddTaskDialog(
            taskToEdit = taskToEdit,
            onDismiss = { showTaskDialog = false },
            onSaveTask = { task ->
                if (taskToEdit == null) {
                    viewModel.addTask(task)
                } else {
                    viewModel.updateTask(task)
                }
                showTaskDialog = false
            }
        )
    }

    if (showNoteDialog) {
        AddNoteDialog(
            noteToEdit = noteToEdit,
            onDismiss = { showNoteDialog = false },
            onSaveNote = { note ->
                if (noteToEdit == null) {
                    viewModel.addNote(note)
                } else {
                    viewModel.updateNote(note)
                }
                showNoteDialog = false
            }
        )
    }

    if (showSettingsDialog) {
        ServerSettingsDialog(
            currentUrl = viewModel.serverUrl,
            onDismiss = { showSettingsDialog = false },
            onSaveUrl = { newUrl -> viewModel.updateServerUrl(newUrl) },
            onSyncNow = { viewModel.syncWithServer() }
        )
    }

    if (showProfileDialog && currentUser != null) {
        UserProfileDialog(
            user = currentUser!!,
            onDismiss = { showProfileDialog = false },
            onLogout = { viewModel.logout() }
        )
    }
}
