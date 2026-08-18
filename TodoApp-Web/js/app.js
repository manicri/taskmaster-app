/**
 * TASKMASTER WEB - FIREBASE REALTIME CLOUD SYNC & LOCAL FALLBACK ENGINE
 */

class TaskMasterApp {
  constructor() {
    this.currentUser = null;
    this.currentView = "tasks";
    this.tasks = [];
    this.notes = [];
    
    // Task filters
    this.currentCategory = "todas";
    this.currentStatus = "todas";
    this.searchQuery = "";
    this.currentSort = "dueDate";
    this.editingTaskId = null;

    // Note filters & state
    this.searchNotesQuery = "";
    this.editingNoteId = null;
    this.selectedNoteColor = "#fef9c3";

    // Auth state
    this.authMode = "login";

    this.initElements();
    this.initTheme();
    this.initFirebase();
    this.initEvents();
  }

  initFirebase() {
    this.isFirebaseReady = false;

    try {
      if (window.firebase && window.firebaseConfig && window.firebaseConfig.apiKey !== "TU_API_KEY") {
        if (!firebase.apps.length) {
          firebase.initializeApp(window.firebaseConfig);
        }
        this.auth = firebase.auth();
        this.db = firebase.firestore();

        // Enable offline persistence
        this.db.enablePersistence({ synchronizeTabs: true }).catch((err) => {
          console.warn("Firestore offline persistence:", err.code);
        });

        this.isFirebaseReady = true;
        this.updateSyncBadge(true, "🔥 Firebase Conectado 24/7");

        // Auth state listener
        this.auth.onAuthStateChanged((user) => {
          if (user) {
            this.currentUser = {
              id: user.uid,
              name: user.displayName || user.email.split("@")[0],
              email: user.email
            };
            this.setupFirestoreListeners();
          } else {
            this.currentUser = null;
            this.detachListeners();
            this.loadLocalData();
          }
          this.updateUserUI();
          this.render();
        });

        return;
      }
    } catch (e) {
      console.warn("Firebase no configurado aún o error:", e);
    }

    // Fallback if Firebase not configured yet
    this.loadLocalData();
    this.updateUserUI();
    this.render();
    this.updateSyncBadge(false, "Modo Local (Configura Firebase para 24/7)");
  }

  setupFirestoreListeners() {
    if (!this.isFirebaseReady || !this.currentUser) return;
    const userId = this.currentUser.id;

    // Tasks realtime listener
    this.tasksUnsubscribe = this.db
      .collection("users")
      .doc(userId)
      .collection("tasks")
      .orderBy("createdAt", "desc")
      .onSnapshot(
        (snapshot) => {
          this.tasks = [];
          snapshot.forEach((doc) => {
            this.tasks.push({ id: Number(doc.id) || doc.id, ...doc.data() });
          });
          this.saveLocalTasks();
          this.renderTasks();
        },
        (error) => {
          console.error("Error al escuchar tareas Firestore:", error);
        }
      );

    // Notes realtime listener
    this.notesUnsubscribe = this.db
      .collection("users")
      .doc(userId)
      .collection("notes")
      .orderBy("updatedAt", "desc")
      .onSnapshot(
        (snapshot) => {
          this.notes = [];
          snapshot.forEach((doc) => {
            this.notes.push({ id: Number(doc.id) || doc.id, ...doc.data() });
          });
          this.saveLocalNotes();
          this.renderNotes();
        },
        (error) => {
          console.error("Error al escuchar notas Firestore:", error);
        }
      );
  }

  detachListeners() {
    if (this.tasksUnsubscribe) this.tasksUnsubscribe();
    if (this.notesUnsubscribe) this.notesUnsubscribe();
  }

  loadLocalData() {
    const savedUser = localStorage.getItem("taskmaster_user");
    if (savedUser) {
      try { this.currentUser = JSON.parse(savedUser); } catch (e) {}
    }
    const prefix = this.currentUser ? `taskmaster_${this.currentUser.id}_` : 'taskmaster_guest_';
    
    try {
      this.tasks = JSON.parse(localStorage.getItem(`${prefix}tasks`)) || [];
      this.notes = JSON.parse(localStorage.getItem(`${prefix}notes`)) || [];
    } catch (e) {
      this.tasks = [];
      this.notes = [];
    }
  }

  saveLocalTasks() {
    const prefix = this.currentUser ? `taskmaster_${this.currentUser.id}_` : 'taskmaster_guest_';
    localStorage.setItem(`${prefix}tasks`, JSON.stringify(this.tasks));
  }

  saveLocalNotes() {
    const prefix = this.currentUser ? `taskmaster_${this.currentUser.id}_` : 'taskmaster_guest_';
    localStorage.setItem(`${prefix}notes`, JSON.stringify(this.notes));
  }

  updateSyncBadge(online, customMsg = null) {
    if (!this.syncBadge) return;
    if (online) {
      this.syncBadge.innerHTML = "🔥 Firebase 24/7";
      this.syncBadge.style.background = "rgba(16, 185, 129, 0.15)";
      this.syncBadge.style.color = "#10b981";
      this.syncBadge.title = "Conectado a Google Firebase. Sincronización en tiempo real 24/7.";
    } else {
      this.syncBadge.innerHTML = customMsg ? `🟡 ${customMsg}` : "🟡 Modo Local";
      this.syncBadge.style.background = "rgba(245, 158, 11, 0.15)";
      this.syncBadge.style.color = "#f59e0b";
      this.syncBadge.title = "Guardando en almacenamiento local.";
    }
  }

  updateUserUI() {
    if (this.currentUser) {
      this.userProfilePill.style.display = "flex";
      this.btnOpenAuth.style.display = "none";
      this.userNameText.textContent = this.currentUser.name;
      this.userAvatar.textContent = this.currentUser.name.charAt(0).toUpperCase();
    } else {
      this.userProfilePill.style.display = "none";
      this.btnOpenAuth.style.display = "inline-flex";
    }
  }

  initElements() {
    // Navigation Tabs
    this.tabTasks = document.getElementById("tabTasks");
    this.tabNotes = document.getElementById("tabNotes");
    this.tasksView = document.getElementById("tasksView");
    this.notesView = document.getElementById("notesView");
    this.btnMainAction = document.getElementById("btnMainAction");
    this.btnMainActionText = document.getElementById("btnMainActionText");

    // User Profile Elements
    this.userProfilePill = document.getElementById("userProfilePill");
    this.userAvatar = document.getElementById("userAvatar");
    this.userNameText = document.getElementById("userNameText");
    this.btnLogout = document.getElementById("btnLogout");
    this.btnOpenAuth = document.getElementById("btnOpenAuth");

    // Auth Modal Elements
    this.authModal = document.getElementById("authModal");
    this.authForm = document.getElementById("authForm");
    this.authModalTitle = document.getElementById("authModalTitle");
    this.tabAuthLogin = document.getElementById("tabAuthLogin");
    this.tabAuthRegister = document.getElementById("tabAuthRegister");
    this.authNameGroup = document.getElementById("authNameGroup");
    this.authNameInput = document.getElementById("authName");
    this.authEmailInput = document.getElementById("authEmail");
    this.authPasswordInput = document.getElementById("authPassword");
    this.authErrorMsg = document.getElementById("authErrorMsg");
    this.btnAuthSubmit = document.getElementById("btnAuthSubmit");

    // Task Elements
    this.taskModalBackdrop = document.getElementById("taskModal");
    this.taskForm = document.getElementById("taskForm");
    this.modalTitle = document.getElementById("modalTitle");
    this.taskTitleInput = document.getElementById("taskTitle");
    this.taskDescInput = document.getElementById("taskDesc");
    this.taskCategoryInput = document.getElementById("taskCategory");
    this.taskDueDateInput = document.getElementById("taskDueDate");
    this.priorityButtons = document.querySelectorAll(".priority-btn");
    this.selectedPriority = "media";

    this.searchInput = document.getElementById("searchInput");
    this.sortSelect = document.getElementById("sortSelect");
    this.categoryChips = document.querySelectorAll(".chip[data-category]");
    this.statusChips = document.querySelectorAll(".chip[data-status]");
    this.taskList = document.getElementById("taskList");
    this.emptyState = document.getElementById("emptyState");
    this.syncBadge = document.getElementById("syncStatusBadge");

    // Stats
    this.statTotal = document.getElementById("statTotal");
    this.statPending = document.getElementById("statPending");
    this.statCompleted = document.getElementById("statCompleted");
    this.statRate = document.getElementById("statRate");
    this.progressFill = document.getElementById("progressFill");
    this.progressText = document.getElementById("progressText");

    // Note Elements
    this.noteModalBackdrop = document.getElementById("noteModal");
    this.noteForm = document.getElementById("noteForm");
    this.noteModalTitle = document.getElementById("noteModalTitle");
    this.noteTitleInput = document.getElementById("noteTitle");
    this.noteContentInput = document.getElementById("noteContent");
    this.notePinnedInput = document.getElementById("notePinned");
    this.searchNotesInput = document.getElementById("searchNotesInput");
    this.notesGrid = document.getElementById("notesGrid");
    this.emptyNotesState = document.getElementById("emptyNotesState");
    this.colorOptions = document.querySelectorAll(".color-option");

    // Theme Toggle
    this.themeToggleBtn = document.getElementById("themeToggleBtn");
  }

  initTheme() {
    const savedTheme = localStorage.getItem("taskmaster_theme") || 
      (window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light");
    document.documentElement.setAttribute("data-theme", savedTheme);
    this.updateThemeIcon(savedTheme);
  }

  updateThemeIcon(theme) {
    if (!this.themeToggleBtn) return;
    this.themeToggleBtn.innerHTML = theme === "dark" 
      ? `<svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z"/></svg>`
      : `<svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z"/></svg>`;
  }

  toggleTheme() {
    const currentTheme = document.documentElement.getAttribute("data-theme") || "light";
    const newTheme = currentTheme === "dark" ? "light" : "dark";
    document.documentElement.setAttribute("data-theme", newTheme);
    localStorage.setItem("taskmaster_theme", newTheme);
    this.updateThemeIcon(newTheme);
    this.showToast(`Modo ${newTheme === "dark" ? "Oscuro" : "Claro"} activado`);
  }

  initEvents() {
    // Navigation Tabs
    this.tabTasks.addEventListener("click", () => this.switchView("tasks"));
    this.tabNotes.addEventListener("click", () => this.switchView("notes"));

    // Main action button in header
    this.btnMainAction.addEventListener("click", () => {
      if (this.currentView === "tasks") {
        this.openTaskModal();
      } else {
        this.openNoteModal();
      }
    });

    // Auth Events
    this.btnOpenAuth.addEventListener("click", () => this.openAuthModal());
    document.getElementById("btnAuthModalClose").addEventListener("click", () => this.closeAuthModal());
    document.getElementById("btnAuthCancel").addEventListener("click", () => this.closeAuthModal());
    this.authModal.addEventListener("click", (e) => {
      if (e.target === this.authModal) this.closeAuthModal();
    });

    this.tabAuthLogin.addEventListener("click", () => this.setAuthMode("login"));
    this.tabAuthRegister.addEventListener("click", () => this.setAuthMode("register"));

    this.authForm.addEventListener("submit", (e) => {
      e.preventDefault();
      this.handleAuthSubmit();
    });

    this.btnLogout.addEventListener("click", () => this.logout());

    // Theme Toggle
    this.themeToggleBtn.addEventListener("click", () => this.toggleTheme());

    // Task Modal Events
    document.getElementById("btnModalClose").addEventListener("click", () => this.closeTaskModal());
    document.getElementById("btnModalCancel").addEventListener("click", () => this.closeTaskModal());
    this.taskModalBackdrop.addEventListener("click", (e) => {
      if (e.target === this.taskModalBackdrop) this.closeTaskModal();
    });

    this.priorityButtons.forEach((btn) => {
      btn.addEventListener("click", () => {
        this.priorityButtons.forEach((b) => b.classList.remove("selected", "alta", "media", "baja"));
        const priority = btn.dataset.priority;
        btn.classList.add("selected", priority);
        this.selectedPriority = priority;
      });
    });

    this.taskForm.addEventListener("submit", (e) => {
      e.preventDefault();
      this.handleTaskFormSubmit();
    });

    this.searchInput.addEventListener("input", (e) => {
      this.searchQuery = e.target.value.toLowerCase().trim();
      this.renderTasks();
    });

    this.sortSelect.addEventListener("change", (e) => {
      this.currentSort = e.target.value;
      this.renderTasks();
    });

    this.categoryChips.forEach((chip) => {
      chip.addEventListener("click", () => {
        this.categoryChips.forEach((c) => c.classList.remove("active"));
        chip.classList.add("active");
        this.currentCategory = chip.dataset.category;
        this.renderTasks();
      });
    });

    this.statusChips.forEach((chip) => {
      chip.addEventListener("click", () => {
        this.statusChips.forEach((c) => c.classList.remove("active"));
        chip.classList.add("active");
        this.currentStatus = chip.dataset.status;
        this.renderTasks();
      });
    });

    // Note Modal Events
    document.getElementById("btnNoteModalClose").addEventListener("click", () => this.closeNoteModal());
    document.getElementById("btnNoteModalCancel").addEventListener("click", () => this.closeNoteModal());
    this.noteModalBackdrop.addEventListener("click", (e) => {
      if (e.target === this.noteModalBackdrop) this.closeNoteModal();
    });

    this.colorOptions.forEach((opt) => {
      opt.addEventListener("click", () => {
        this.colorOptions.forEach((o) => o.classList.remove("selected"));
        opt.classList.add("selected");
        this.selectedNoteColor = opt.dataset.color;
      });
    });

    this.noteForm.addEventListener("submit", (e) => {
      e.preventDefault();
      this.handleNoteFormSubmit();
    });

    this.searchNotesInput.addEventListener("input", (e) => {
      this.searchNotesQuery = e.target.value.toLowerCase().trim();
      this.renderNotes();
    });

    // Export / Import
    const exportBtn = document.getElementById("btnExportJSON");
    if (exportBtn) exportBtn.addEventListener("click", () => this.exportAllDataJSON());

    const importInput = document.getElementById("inputImportJSON");
    if (importInput) importInput.addEventListener("change", (e) => this.importAllDataJSON(e));
  }

  // ==========================================
  // AUTHENTICATION METHODS
  // ==========================================
  openAuthModal() {
    this.authErrorMsg.style.display = "none";
    this.authForm.reset();
    this.setAuthMode("login");
    this.authModal.classList.add("active");
  }

  closeAuthModal() {
    this.authModal.classList.remove("active");
  }

  setAuthMode(mode) {
    this.authMode = mode;
    this.authErrorMsg.style.display = "none";
    if (mode === "login") {
      this.tabAuthLogin.classList.add("active");
      this.tabAuthRegister.classList.remove("active");
      this.authModalTitle.textContent = "Iniciar Sesión";
      this.authNameGroup.style.display = "none";
      this.authNameInput.removeAttribute("required");
      this.btnAuthSubmit.textContent = "Ingresar";
    } else {
      this.tabAuthRegister.classList.add("active");
      this.tabAuthLogin.classList.remove("active");
      this.authModalTitle.textContent = "Crear Cuenta";
      this.authNameGroup.style.display = "flex";
      this.authNameInput.setAttribute("required", "true");
      this.btnAuthSubmit.textContent = "Registrarme";
    }
  }

  async handleAuthSubmit() {
    const email = this.authEmailInput.value.trim();
    const password = this.authPasswordInput.value;
    const name = this.authNameInput.value.trim();

    this.authErrorMsg.style.display = "none";

    try {
      this.btnAuthSubmit.disabled = true;
      this.btnAuthSubmit.textContent = "Conectando...";

      if (this.isFirebaseReady) {
        if (this.authMode === "login") {
          await this.auth.signInWithEmailAndPassword(email, password);
        } else {
          const userCred = await this.auth.createUserWithEmailAndPassword(email, password);
          if (name && userCred.user) {
            await userCred.user.updateProfile({ displayName: name });
          }
        }
      } else {
        // Fallback local account
        this.currentUser = {
          id: 'user_' + Date.now(),
          name: name || email.split('@')[0],
          email: email
        };
        localStorage.setItem("taskmaster_user", JSON.stringify(this.currentUser));
        this.loadLocalData();
        this.updateUserUI();
        this.render();
      }

      this.closeAuthModal();
      this.showToast(`¡Bienvenido! 🎉`);

    } catch (err) {
      this.authErrorMsg.textContent = err.message || "Error al autenticar";
      this.authErrorMsg.style.display = "block";
    } finally {
      this.btnAuthSubmit.disabled = false;
      this.btnAuthSubmit.textContent = this.authMode === "login" ? "Ingresar" : "Registrarme";
    }
  }

  async logout() {
    if (confirm("¿Deseas cerrar la sesión actual?")) {
      if (this.isFirebaseReady) {
        await this.auth.signOut();
      } else {
        this.currentUser = null;
        localStorage.removeItem("taskmaster_user");
        this.loadLocalData();
        this.updateUserUI();
        this.render();
      }
      this.showToast("Sesión cerrada");
    }
  }

  switchView(viewName) {
    this.currentView = viewName;
    if (viewName === "tasks") {
      this.tabTasks.classList.add("active");
      this.tabNotes.classList.remove("active");
      this.tasksView.style.display = "block";
      this.notesView.style.display = "none";
      this.btnMainActionText.textContent = "Nueva Tarea";
      this.renderTasks();
    } else {
      this.tabNotes.classList.add("active");
      this.tabTasks.classList.remove("active");
      this.tasksView.style.display = "none";
      this.notesView.style.display = "block";
      this.btnMainActionText.textContent = "Nueva Nota";
      this.renderNotes();
    }
  }

  // ==========================================
  // TASK METHODS
  // ==========================================
  openTaskModal(taskToEdit = null) {
    this.editingTaskId = taskToEdit ? taskToEdit.id : null;
    this.modalTitle.textContent = taskToEdit ? "Editar Tarea" : "Nueva Tarea";

    if (taskToEdit) {
      this.taskTitleInput.value = taskToEdit.title;
      this.taskDescInput.value = taskToEdit.description || "";
      this.taskCategoryInput.value = taskToEdit.category || "personal";
      this.taskDueDateInput.value = taskToEdit.dueDate || "";
      this.selectedPriority = taskToEdit.priority || "media";
    } else {
      this.taskForm.reset();
      this.taskCategoryInput.value = "personal";
      this.selectedPriority = "media";
    }

    this.priorityButtons.forEach((btn) => {
      btn.classList.remove("selected", "alta", "media", "baja");
      if (btn.dataset.priority === this.selectedPriority) {
        btn.classList.add("selected", this.selectedPriority);
      }
    });

    this.taskModalBackdrop.classList.add("active");
    this.taskTitleInput.focus();
  }

  closeTaskModal() {
    this.taskModalBackdrop.classList.remove("active");
    this.editingTaskId = null;
  }

  async handleTaskFormSubmit() {
    const title = this.taskTitleInput.value.trim();
    if (!title) return;

    const description = this.taskDescInput.value.trim();
    const category = this.taskCategoryInput.value;
    const dueDate = this.taskDueDateInput.value;
    const priority = this.selectedPriority;
    const dueDateMillis = dueDate ? new Date(dueDate).getTime() : null;

    if (this.editingTaskId) {
      const taskData = {
        title,
        description,
        category,
        dueDate,
        dueDateMillis,
        priority,
        updatedAt: Date.now()
      };

      if (this.isFirebaseReady && this.currentUser) {
        await this.db
          .collection("users")
          .doc(this.currentUser.id)
          .collection("tasks")
          .doc(String(this.editingTaskId))
          .update(taskData);
      } else {
        const index = this.tasks.findIndex((t) => t.id === this.editingTaskId);
        if (index !== -1) {
          this.tasks[index] = { ...this.tasks[index], ...taskData };
          this.saveLocalTasks();
          this.renderTasks();
        }
      }
      this.showToast("Tarea actualizada");
    } else {
      const taskId = Date.now();
      const newTask = {
        id: taskId,
        title,
        description,
        category,
        dueDate,
        dueDateMillis,
        priority,
        isCompleted: false,
        createdAt: Date.now(),
        updatedAt: Date.now()
      };

      if (this.isFirebaseReady && this.currentUser) {
        await this.db
          .collection("users")
          .doc(this.currentUser.id)
          .collection("tasks")
          .doc(String(taskId))
          .set(newTask);
      } else {
        this.tasks.unshift(newTask);
        this.saveLocalTasks();
        this.renderTasks();
      }
      this.showToast("¡Nueva tarea creada!");
    }

    this.closeTaskModal();
  }

  async toggleTask(taskId) {
    const task = this.tasks.find((t) => t.id === taskId);
    if (task) {
      const newStatus = !task.isCompleted;
      if (this.isFirebaseReady && this.currentUser) {
        await this.db
          .collection("users")
          .doc(this.currentUser.id)
          .collection("tasks")
          .doc(String(taskId))
          .update({ isCompleted: newStatus, updatedAt: Date.now() });
      } else {
        task.isCompleted = newStatus;
        task.updatedAt = Date.now();
        this.saveLocalTasks();
        this.renderTasks();
      }
      if (newStatus) this.showToast("¡Tarea completada! 🎉");
    }
  }

  async deleteTask(taskId) {
    const task = this.tasks.find((t) => t.id === taskId);
    if (!task) return;

    if (confirm(`¿Estás seguro de eliminar "${task.title}"?`)) {
      if (this.isFirebaseReady && this.currentUser) {
        await this.db
          .collection("users")
          .doc(this.currentUser.id)
          .collection("tasks")
          .doc(String(taskId))
          .delete();
      } else {
        this.tasks = this.tasks.filter((t) => t.id !== taskId);
        this.saveLocalTasks();
        this.renderTasks();
      }
      this.showToast("Tarea eliminada");
    }
  }

  getFilteredAndSortedTasks() {
    return this.tasks
      .filter((task) => {
        if (this.currentStatus === "activas" && task.isCompleted) return false;
        if (this.currentStatus === "completadas" && !task.isCompleted) return false;
        if (this.currentCategory !== "todas" && task.category !== this.currentCategory) return false;
        if (this.searchQuery) {
          const matchTitle = task.title.toLowerCase().includes(this.searchQuery);
          const matchDesc = (task.description || "").toLowerCase().includes(this.searchQuery);
          if (!matchTitle && !matchDesc) return false;
        }
        return true;
      })
      .sort((a, b) => {
        const priorityWeight = { alta: 3, media: 2, baja: 1 };
        switch (this.currentSort) {
          case "dueDate":
            if (!a.dueDate) return 1;
            if (!b.dueDate) return -1;
            return new Date(a.dueDate) - new Date(b.dueDate);
          case "priority":
            return (priorityWeight[b.priority] || 0) - (priorityWeight[a.priority] || 0);
          case "alphabetical":
            return a.title.localeCompare(b.title);
          case "created":
          default:
            return (b.createdAt || 0) - (a.createdAt || 0);
        }
      });
  }

  formatDateBadge(dueDateStr, isCompleted) {
    if (!dueDateStr) return "";
    const dueDate = new Date(dueDateStr);
    const now = new Date();
    const isOverdue = !isCompleted && dueDate < now;
    const options = { month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" };
    const dateFormatted = dueDate.toLocaleDateString("es-ES", options);

    const diffHours = (dueDate - now) / (1000 * 60 * 60);
    let relativeLabel = "";
    if (isOverdue) relativeLabel = "⚠️ Vencida: ";
    else if (diffHours >= 0 && diffHours <= 24) relativeLabel = "⏰ Hoy: ";
    else if (diffHours > 24 && diffHours <= 48) relativeLabel = "📅 Mañana: ";

    const badgeClass = isOverdue ? "badge-date overdue" : (diffHours >= 0 && diffHours <= 24 ? "badge-date today" : "badge-date");

    return `
      <span class="badge ${badgeClass}">
        <svg style="width:12px;height:12px;" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/></svg>
        ${relativeLabel}${dateFormatted}
      </span>
    `;
  }

  renderTasks() {
    const total = this.tasks.length;
    const completed = this.tasks.filter((t) => t.isCompleted).length;
    const pending = total - completed;
    const rate = total > 0 ? Math.round((completed / total) * 100) : 0;

    this.statTotal.textContent = total;
    this.statPending.textContent = pending;
    this.statCompleted.textContent = completed;
    this.statRate.textContent = `${rate}%`;
    this.progressFill.style.width = `${rate}%`;
    this.progressText.textContent = `${rate}% completado (${completed} de ${total})`;

    const filtered = this.getFilteredAndSortedTasks();
    this.taskList.innerHTML = "";

    if (filtered.length === 0) {
      this.emptyState.style.display = "flex";
      this.taskList.style.display = "none";
    } else {
      this.emptyState.style.display = "none";
      this.taskList.style.display = "flex";

      filtered.forEach((task) => {
        const itemEl = document.createElement("div");
        itemEl.className = `task-item ${task.isCompleted ? "completed" : ""}`;
        itemEl.dataset.id = task.id;

        const categoryLabels = {
          trabajo: "Trabajo", personal: "Personal", estudio: "Estudio",
          salud: "Salud", finanzas: "Finanzas", otros: "Otros"
        };
        const priorityLabels = { alta: "Alta", media: "Media", baja: "Baja" };

        itemEl.innerHTML = `
          <div class="task-checkbox-container">
            <div class="custom-checkbox" title="${task.isCompleted ? 'Desmarcar' : 'Completar'}">
              <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7"/></svg>
            </div>
          </div>
          <div class="task-content">
            <div class="task-title-row">
              <span class="task-title">${this.escapeHTML(task.title)}</span>
            </div>
            ${task.description ? `<p class="task-desc">${this.escapeHTML(task.description)}</p>` : ""}
            <div class="task-badges">
              <span class="badge badge-priority-${task.priority}">
                ${priorityLabels[task.priority] || task.priority}
              </span>
              <span class="badge badge-cat-${task.category}">
                ${categoryLabels[task.category] || task.category}
              </span>
              ${this.formatDateBadge(task.dueDate, task.isCompleted)}
            </div>
          </div>
          <div class="task-actions">
            <button class="btn-action-icon edit" title="Editar tarea">
              <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/></svg>
            </button>
            <button class="btn-action-icon delete" title="Eliminar tarea">
              <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/></svg>
            </button>
          </div>
        `;

        itemEl.querySelector(".custom-checkbox").addEventListener("click", () => this.toggleTask(task.id));
        itemEl.querySelector(".btn-action-icon.edit").addEventListener("click", () => this.openTaskModal(task));
        itemEl.querySelector(".btn-action-icon.delete").addEventListener("click", () => this.deleteTask(task.id));

        this.taskList.appendChild(itemEl);
      });
    }
  }

  // ==========================================
  // NOTE METHODS
  // ==========================================
  openNoteModal(noteToEdit = null) {
    this.editingNoteId = noteToEdit ? noteToEdit.id : null;
    this.noteModalTitle.textContent = noteToEdit ? "Editar Nota Rápida" : "Nueva Nota Rápida";

    if (noteToEdit) {
      this.noteTitleInput.value = noteToEdit.title || "";
      this.noteContentInput.value = noteToEdit.content || "";
      this.notePinnedInput.checked = Boolean(noteToEdit.isPinned);
      this.selectedNoteColor = noteToEdit.color || "#fef9c3";
    } else {
      this.noteForm.reset();
      this.selectedNoteColor = "#fef9c3";
      this.notePinnedInput.checked = false;
    }

    this.colorOptions.forEach((opt) => {
      opt.classList.remove("selected");
      if (opt.dataset.color === this.selectedNoteColor) opt.classList.add("selected");
    });

    this.noteModalBackdrop.classList.add("active");
    this.noteContentInput.focus();
  }

  closeNoteModal() {
    this.noteModalBackdrop.classList.remove("active");
    this.editingNoteId = null;
  }

  async handleNoteFormSubmit() {
    const content = this.noteContentInput.value.trim();
    if (!content) return;

    const title = this.noteTitleInput.value.trim();
    const color = this.selectedNoteColor;
    const isPinned = this.notePinnedInput.checked;

    if (this.editingNoteId) {
      const noteData = {
        title,
        content,
        color,
        isPinned,
        updatedAt: Date.now()
      };

      if (this.isFirebaseReady && this.currentUser) {
        await this.db
          .collection("users")
          .doc(this.currentUser.id)
          .collection("notes")
          .doc(String(this.editingNoteId))
          .update(noteData);
      } else {
        const index = this.notes.findIndex((n) => n.id === this.editingNoteId);
        if (index !== -1) {
          this.notes[index] = { ...this.notes[index], ...noteData };
          this.saveLocalNotes();
          this.renderNotes();
        }
      }
      this.showToast("Nota actualizada");
    } else {
      const noteId = Date.now();
      const newNote = {
        id: noteId,
        title,
        content,
        color,
        isPinned,
        createdAt: Date.now(),
        updatedAt: Date.now()
      };

      if (this.isFirebaseReady && this.currentUser) {
        await this.db
          .collection("users")
          .doc(this.currentUser.id)
          .collection("notes")
          .doc(String(noteId))
          .set(newNote);
      } else {
        this.notes.unshift(newNote);
        this.saveLocalNotes();
        this.renderNotes();
      }
      this.showToast("¡Nota rápida guardada!");
    }

    this.closeNoteModal();
  }

  async togglePinNote(noteId) {
    const note = this.notes.find((n) => n.id === noteId);
    if (note) {
      const newPin = !note.isPinned;
      if (this.isFirebaseReady && this.currentUser) {
        await this.db
          .collection("users")
          .doc(this.currentUser.id)
          .collection("notes")
          .doc(String(noteId))
          .update({ isPinned: newPin, updatedAt: Date.now() });
      } else {
        note.isPinned = newPin;
        note.updatedAt = Date.now();
        this.saveLocalNotes();
        this.renderNotes();
      }
      this.showToast(newPin ? "Nota fijada al inicio 📌" : "Nota desfijada");
    }
  }

  async deleteNote(noteId) {
    const note = this.notes.find((n) => n.id === noteId);
    if (!note) return;

    if (confirm("¿Estás seguro de eliminar esta nota?")) {
      if (this.isFirebaseReady && this.currentUser) {
        await this.db
          .collection("users")
          .doc(this.currentUser.id)
          .collection("notes")
          .doc(String(noteId))
          .delete();
      } else {
        this.notes = this.notes.filter((n) => n.id !== noteId);
        this.saveLocalNotes();
        this.renderNotes();
      }
      this.showToast("Nota eliminada");
    }
  }

  getFilteredNotes() {
    return this.notes
      .filter((note) => {
        if (!this.searchNotesQuery) return true;
        const matchTitle = (note.title || "").toLowerCase().includes(this.searchNotesQuery);
        const matchContent = (note.content || "").toLowerCase().includes(this.searchNotesQuery);
        return matchTitle || matchContent;
      })
      .sort((a, b) => {
        if (a.isPinned && !b.isPinned) return -1;
        if (!a.isPinned && b.isPinned) return 1;
        return (b.updatedAt || b.createdAt || 0) - (a.updatedAt || a.createdAt || 0);
      });
  }

  renderNotes() {
    const filtered = this.getFilteredNotes();
    this.notesGrid.innerHTML = "";

    if (filtered.length === 0) {
      this.emptyNotesState.style.display = "flex";
      this.notesGrid.style.display = "none";
    } else {
      this.emptyNotesState.style.display = "none";
      this.notesGrid.style.display = "grid";

      filtered.forEach((note) => {
        const cardEl = document.createElement("div");
        cardEl.className = "note-card";
        cardEl.style.backgroundColor = note.color || "#fef9c3";

        const dateStr = new Date(note.updatedAt || note.createdAt || Date.now()).toLocaleDateString("es-ES", {
          month: "short", day: "numeric", hour: "2-digit", minute: "2-digit"
        });

        cardEl.innerHTML = `
          <div class="note-card-header">
            ${note.title ? `<div class="note-card-title">${this.escapeHTML(note.title)}</div>` : '<div></div>'}
            <button class="btn-action-icon pin ${note.isPinned ? 'active' : ''}" title="${note.isPinned ? 'Desfijar' : 'Fijar al inicio'}">
              <svg fill="${note.isPinned ? 'currentColor' : 'none'}" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z"/></svg>
            </button>
          </div>
          <div class="note-card-content">${this.escapeHTML(note.content)}</div>
          <div class="note-card-footer">
            <span class="note-date">${dateStr}</span>
            <div class="task-actions">
              <button class="btn-action-icon edit" title="Editar nota">
                <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/></svg>
              </button>
              <button class="btn-action-icon delete" title="Eliminar nota">
                <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/></svg>
              </button>
            </div>
          </div>
        `;

        cardEl.querySelector(".btn-action-icon.pin").addEventListener("click", () => this.togglePinNote(note.id));
        cardEl.querySelector(".btn-action-icon.edit").addEventListener("click", () => this.openNoteModal(note));
        cardEl.querySelector(".btn-action-icon.delete").addEventListener("click", () => this.deleteNote(note.id));

        this.notesGrid.appendChild(cardEl);
      });
    }
  }

  render() {
    this.renderTasks();
    this.renderNotes();
  }

  escapeHTML(str) {
    if (!str) return "";
    const div = document.createElement("div");
    div.textContent = str;
    return div.innerHTML;
  }

  showToast(message) {
    const container = document.getElementById("toastContainer");
    if (!container) return;

    const toast = document.createElement("div");
    toast.className = "toast";
    toast.innerHTML = `
      <svg style="width:18px;height:18px;color:var(--primary);" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
      <span>${message}</span>
    `;

    container.appendChild(toast);
    setTimeout(() => {
      toast.style.opacity = "0";
      toast.style.transform = "translateY(10px)";
      toast.style.transition = "all 0.3s ease";
      setTimeout(() => toast.remove(), 300);
    }, 3000);
  }

  exportAllDataJSON() {
    const backup = { user: this.currentUser, tasks: this.tasks, notes: this.notes, exportedAt: new Date().toISOString() };
    const dataStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(backup, null, 2));
    const downloadAnchor = document.createElement("a");
    downloadAnchor.setAttribute("href", dataStr);
    downloadAnchor.setAttribute("download", `taskmaster-${this.currentUser ? this.currentUser.name : 'backup'}-${new Date().toISOString().slice(0, 10)}.json`);
    document.body.appendChild(downloadAnchor);
    downloadAnchor.click();
    downloadAnchor.remove();
    this.showToast("Copia completa exportada");
  }

  importAllDataJSON(event) {
    const file = event.target.files[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (e) => {
      try {
        const imported = JSON.parse(e.target.result);
        if (imported.tasks && Array.isArray(imported.tasks)) this.tasks = imported.tasks;
        if (imported.notes && Array.isArray(imported.notes)) this.notes = imported.notes;
        if (Array.isArray(imported)) this.tasks = imported;
        this.saveLocalTasks();
        this.saveLocalNotes();
        this.render();
        this.showToast("Datos importados con éxito");
      } catch (err) {
        alert("Error al leer el archivo JSON: " + err.message);
      }
    };
    reader.readAsText(file);
    event.target.value = "";
  }
}

document.addEventListener("DOMContentLoaded", () => {
  window.app = new TaskMasterApp();
});
