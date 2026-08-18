package com.cris.taskmaster.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cris.taskmaster.data.TaskRepository
import com.cris.taskmaster.model.NoteItem
import com.cris.taskmaster.model.TaskCategory
import com.cris.taskmaster.model.TaskItem
import com.cris.taskmaster.model.UserAccount
import com.cris.taskmaster.ui.components.StatusFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TaskRepository(application)

    // User Account State
    private val _currentUser = MutableStateFlow<UserAccount?>(repository.getCurrentUser())
    val currentUser: StateFlow<UserAccount?> = _currentUser.asStateFlow()

    // Current Tab: 0 = Tareas, 1 = Notas Rápidas
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Tasks State
    private val _tasks = MutableStateFlow<List<TaskItem>>(emptyList())
    val tasks: StateFlow<List<TaskItem>> = _tasks.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow(TaskCategory.ALL)
    val selectedCategory: StateFlow<TaskCategory> = _selectedCategory.asStateFlow()

    private val _selectedStatus = MutableStateFlow(StatusFilter.ALL)
    val selectedStatus: StateFlow<StatusFilter> = _selectedStatus.asStateFlow()

    // Notes State
    private val _notes = MutableStateFlow<List<NoteItem>>(emptyList())
    val notes: StateFlow<List<NoteItem>> = _notes.asStateFlow()

    private val _searchNotesQuery = MutableStateFlow("")
    val searchNotesQuery: StateFlow<String> = _searchNotesQuery.asStateFlow()

    // Sync & Loading State
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    val serverUrl: String get() = repository.getServerUrl()

    // Tareas filtradas reactivas
    val filteredTasks: StateFlow<List<TaskItem>> = combine(
        _tasks,
        _searchQuery,
        _selectedCategory,
        _selectedStatus
    ) { taskList, query, category, status ->
        taskList.filter { task ->
            val matchesQuery = query.isBlank() ||
                    task.title.contains(query, ignoreCase = true) ||
                    task.description.contains(query, ignoreCase = true)

            val matchesCategory = category == TaskCategory.ALL || task.category == category

            val matchesStatus = when (status) {
                StatusFilter.ALL -> true
                StatusFilter.PENDING -> !task.isCompleted
                StatusFilter.COMPLETED -> task.isCompleted
            }

            matchesQuery && matchesCategory && matchesStatus
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Notas filtradas reactivas
    val filteredNotes: StateFlow<List<NoteItem>> = combine(
        _notes,
        _searchNotesQuery
    ) { noteList, query ->
        noteList.filter { note ->
            query.isBlank() ||
                    note.title.contains(query, ignoreCase = true) ||
                    note.content.contains(query, ignoreCase = true)
        }.sortedWith(
            compareByDescending<NoteItem> { it.isPinned }
                .thenByDescending { it.updatedAtMillis }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        loadData()
        if (_currentUser.value != null) {
            syncWithServer()
        }
    }

    // ==========================================
    // AUTH ACTIONS
    // ==========================================
    fun login(email: String, pass: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val (success, msg) = repository.login(email, pass)
            if (success) {
                _currentUser.value = repository.getCurrentUser()
                loadData()
                syncWithServer()
            }
            onResult(success, msg)
        }
    }

    fun register(name: String, email: String, pass: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val (success, msg) = repository.register(name, email, pass)
            if (success) {
                _currentUser.value = repository.getCurrentUser()
                loadData()
                syncWithServer()
            }
            onResult(success, msg)
        }
    }

    fun logout() {
        repository.logout()
        _currentUser.value = null
        loadData()
    }

    fun setSelectedTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            _tasks.value = repository.getTasks()
            _notes.value = repository.getNotes()
            _isLoading.value = false
        }
    }

    fun updateServerUrl(newUrl: String) {
        repository.setServerUrl(newUrl)
    }

    fun syncWithServer() {
        if (_currentUser.value == null) return

        viewModelScope.launch {
            _isSyncing.value = true
            val (success, message) = repository.syncWithServer()
            _tasks.value = repository.getTasks()
            _notes.value = repository.getNotes()
            _isSyncing.value = false
            _syncMessage.value = message
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    // ==========================================
    // TASK ACTIONS
    // ==========================================
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: TaskCategory) {
        _selectedCategory.value = category
    }

    fun setSelectedStatus(status: StatusFilter) {
        _selectedStatus.value = status
    }

    fun addTask(task: TaskItem) {
        viewModelScope.launch {
            repository.addTask(task)
            _tasks.value = repository.getTasks()
            syncWithServer()
        }
    }

    fun updateTask(task: TaskItem) {
        viewModelScope.launch {
            repository.updateTask(task)
            _tasks.value = repository.getTasks()
            syncWithServer()
        }
    }

    fun toggleTaskCompleted(task: TaskItem) {
        viewModelScope.launch {
            val updated = task.copy(isCompleted = !task.isCompleted)
            repository.updateTask(updated)
            _tasks.value = repository.getTasks()
            syncWithServer()
        }
    }

    fun deleteTask(task: TaskItem) {
        viewModelScope.launch {
            repository.deleteTask(task.id)
            _tasks.value = repository.getTasks()
            syncWithServer()
        }
    }

    // ==========================================
    // NOTE ACTIONS
    // ==========================================
    fun setSearchNotesQuery(query: String) {
        _searchNotesQuery.value = query
    }

    fun addNote(note: NoteItem) {
        viewModelScope.launch {
            repository.addNote(note)
            _notes.value = repository.getNotes()
            syncWithServer()
        }
    }

    fun updateNote(note: NoteItem) {
        viewModelScope.launch {
            repository.updateNote(note)
            _notes.value = repository.getNotes()
            syncWithServer()
        }
    }

    fun togglePinNote(note: NoteItem) {
        viewModelScope.launch {
            val updated = note.copy(isPinned = !note.isPinned, updatedAtMillis = System.currentTimeMillis())
            repository.updateNote(updated)
            _notes.value = repository.getNotes()
            syncWithServer()
        }
    }

    fun deleteNote(note: NoteItem) {
        viewModelScope.launch {
            repository.deleteNote(note.id)
            _notes.value = repository.getNotes()
            syncWithServer()
        }
    }
}
