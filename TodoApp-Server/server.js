const express = require('express');
const cors = require('cors');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const app = express();
const PORT = process.env.PORT || 3000;
const DATA_DIR = path.join(__dirname, 'data');
const USERS_FILE = path.join(DATA_DIR, 'users.json');
const TASKS_FILE = path.join(DATA_DIR, 'tasks.json');
const NOTES_FILE = path.join(DATA_DIR, 'notes.json');

// Ensure data directory exists
if (!fs.existsSync(DATA_DIR)) {
  fs.mkdirSync(DATA_DIR, { recursive: true });
}

// Middleware
app.use(cors());
app.use(express.json());

// Serve Web application static files
const webAppPath = path.join(__dirname, '..', 'TodoApp-Web');
app.use(express.static(webAppPath));

// Helper functions for reading & writing JSON files
function readJSON(file, defaultData = []) {
  try {
    if (!fs.existsSync(file)) {
      writeJSON(file, defaultData);
      return defaultData;
    }
    const raw = fs.readFileSync(file, 'utf8');
    return JSON.parse(raw);
  } catch (error) {
    console.error(`Error reading ${file}:`, error);
    return defaultData;
  }
}

function writeJSON(file, data) {
  try {
    fs.writeFileSync(file, JSON.stringify(data, null, 2), 'utf8');
  } catch (error) {
    console.error(`Error writing ${file}:`, error);
  }
}

function hashPassword(password) {
  return crypto.createHash('sha256').update(password).digest('hex');
}

// Health Check
app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', timestamp: Date.now(), message: 'Servidor TaskMaster en línea' });
});

// ==========================================
// AUTHENTICATION ENDPOINTS
// ==========================================
app.post('/api/auth/register', (req, res) => {
  const { name, email, password } = req.body;
  if (!name || !email || !password) {
    return res.status(400).json({ error: 'Nombre, correo y contraseña son obligatorios' });
  }

  const cleanEmail = email.trim().toLowerCase();
  const users = readJSON(USERS_FILE, []);

  const exists = users.find(u => u.email === cleanEmail);
  if (exists) {
    return res.status(409).json({ error: 'Ya existe una cuenta con este correo electrónico' });
  }

  const newUser = {
    id: 'user_' + Date.now() + '_' + Math.random().toString(36).substr(2, 6),
    name: name.trim(),
    email: cleanEmail,
    passwordHash: hashPassword(password),
    createdAt: Date.now()
  };

  users.push(newUser);
  writeJSON(USERS_FILE, users);

  // Return safe user object (without password hash)
  const safeUser = { id: newUser.id, name: newUser.name, email: newUser.email };
  res.status(201).json({ message: 'Usuario registrado exitosamente', user: safeUser });
});

app.post('/api/auth/login', (req, res) => {
  const { email, password } = req.body;
  if (!email || !password) {
    return res.status(400).json({ error: 'Correo y contraseña son obligatorios' });
  }

  const cleanEmail = email.trim().toLowerCase();
  const users = readJSON(USERS_FILE, []);
  const user = users.find(u => u.email === cleanEmail && u.passwordHash === hashPassword(password));

  if (!user) {
    return res.status(401).json({ error: 'Credenciales inválidas. Verifica tu correo y contraseña.' });
  }

  const safeUser = { id: user.id, name: user.name, email: user.email };
  res.json({ message: 'Inicio de sesión exitoso', user: safeUser });
});

// ==========================================
// TASKS REST ENDPOINTS (SCOPED BY USER)
// ==========================================
app.get('/api/tasks', (req, res) => {
  const userId = req.query.userId || req.headers['x-user-id'];
  const allTasks = readJSON(TASKS_FILE, []);
  if (!userId) return res.json(allTasks);
  const userTasks = allTasks.filter(t => t.userId === userId || !t.userId);
  res.json(userTasks);
});

app.post('/api/tasks', (req, res) => {
  const userId = req.body.userId || req.headers['x-user-id'] || 'default';
  const allTasks = readJSON(TASKS_FILE, []);
  const newTask = {
    id: req.body.id || Date.now(),
    userId: userId,
    title: req.body.title || 'Sin título',
    description: req.body.description || '',
    category: req.body.category || 'personal',
    priority: req.body.priority || 'media',
    dueDate: req.body.dueDate || '',
    dueDateMillis: req.body.dueDateMillis || null,
    isCompleted: Boolean(req.body.isCompleted),
    createdAt: req.body.createdAt || Date.now(),
    updatedAt: Date.now()
  };

  const existingIndex = allTasks.findIndex(t => t.id === newTask.id);
  if (existingIndex >= 0) {
    allTasks[existingIndex] = newTask;
  } else {
    allTasks.unshift(newTask);
  }

  writeJSON(TASKS_FILE, allTasks);
  res.status(201).json(newTask);
});

app.put('/api/tasks/:id', (req, res) => {
  const taskId = Number(req.params.id);
  const allTasks = readJSON(TASKS_FILE, []);
  const index = allTasks.findIndex(t => t.id === taskId);

  if (index === -1) {
    return res.status(404).json({ error: 'Tarea no encontrada' });
  }

  const updatedTask = {
    ...allTasks[index],
    ...req.body,
    id: taskId,
    updatedAt: Date.now()
  };

  allTasks[index] = updatedTask;
  writeJSON(TASKS_FILE, allTasks);
  res.json(updatedTask);
});

app.delete('/api/tasks/:id', (req, res) => {
  const taskId = Number(req.params.id);
  let allTasks = readJSON(TASKS_FILE, []);
  const initialLength = allTasks.length;
  allTasks = allTasks.filter(t => t.id !== taskId);

  if (allTasks.length === initialLength) {
    return res.status(404).json({ error: 'Tarea no encontrada' });
  }

  writeJSON(TASKS_FILE, allTasks);
  res.json({ message: 'Tarea eliminada exitosamente', id: taskId });
});

app.post('/api/tasks/sync', (req, res) => {
  const userId = req.body.userId || req.headers['x-user-id'] || 'default';
  const clientTasks = (req.body.tasks || []).map(t => ({ ...t, userId }));
  let allTasks = readJSON(TASKS_FILE, []);

  // Filter tasks not belonging to this user so we don't overwrite other users' tasks
  const otherUsersTasks = allTasks.filter(t => t.userId && t.userId !== userId);
  const currentUserTasks = allTasks.filter(t => t.userId === userId || !t.userId);

  const taskMap = new Map();
  currentUserTasks.forEach(t => taskMap.set(t.id, t));

  clientTasks.forEach(clientTask => {
    const existing = taskMap.get(clientTask.id);
    if (!existing) {
      taskMap.set(clientTask.id, clientTask);
    } else {
      const clientTime = clientTask.updatedAt || clientTask.createdAt || 0;
      const serverTime = existing.updatedAt || existing.createdAt || 0;
      if (clientTime >= serverTime) {
        taskMap.set(clientTask.id, clientTask);
      }
    }
  });

  const mergedUserTasks = Array.from(taskMap.values());
  writeJSON(TASKS_FILE, [...otherUsersTasks, ...mergedUserTasks]);
  res.json({ tasks: mergedUserTasks, timestamp: Date.now() });
});

// ==========================================
// NOTES REST ENDPOINTS (SCOPED BY USER)
// ==========================================
app.get('/api/notes', (req, res) => {
  const userId = req.query.userId || req.headers['x-user-id'];
  const allNotes = readJSON(NOTES_FILE, []);
  if (!userId) return res.json(allNotes);
  const userNotes = allNotes.filter(n => n.userId === userId || !n.userId);
  res.json(userNotes);
});

app.post('/api/notes', (req, res) => {
  const userId = req.body.userId || req.headers['x-user-id'] || 'default';
  const allNotes = readJSON(NOTES_FILE, []);
  const newNote = {
    id: req.body.id || Date.now(),
    userId: userId,
    title: req.body.title || '',
    content: req.body.content || '',
    color: req.body.color || '#fef9c3',
    isPinned: Boolean(req.body.isPinned),
    createdAt: req.body.createdAt || Date.now(),
    updatedAt: Date.now()
  };

  const existingIndex = allNotes.findIndex(n => n.id === newNote.id);
  if (existingIndex >= 0) {
    allNotes[existingIndex] = newNote;
  } else {
    allNotes.unshift(newNote);
  }

  writeJSON(NOTES_FILE, allNotes);
  res.status(201).json(newNote);
});

app.put('/api/notes/:id', (req, res) => {
  const noteId = Number(req.params.id);
  const allNotes = readJSON(NOTES_FILE, []);
  const index = allNotes.findIndex(n => n.id === noteId);

  if (index === -1) {
    return res.status(404).json({ error: 'Nota no encontrada' });
  }

  const updatedNote = {
    ...allNotes[index],
    ...req.body,
    id: noteId,
    updatedAt: Date.now()
  };

  allNotes[index] = updatedNote;
  writeJSON(NOTES_FILE, allNotes);
  res.json(updatedNote);
});

app.delete('/api/notes/:id', (req, res) => {
  const noteId = Number(req.params.id);
  let allNotes = readJSON(NOTES_FILE, []);
  const initialLength = allNotes.length;
  allNotes = allNotes.filter(n => n.id !== noteId);

  if (allNotes.length === initialLength) {
    return res.status(404).json({ error: 'Nota no encontrada' });
  }

  writeJSON(NOTES_FILE, allNotes);
  res.json({ message: 'Nota eliminada exitosamente', id: noteId });
});

app.post('/api/notes/sync', (req, res) => {
  const userId = req.body.userId || req.headers['x-user-id'] || 'default';
  const clientNotes = (req.body.notes || []).map(n => ({ ...n, userId }));
  let allNotes = readJSON(NOTES_FILE, []);

  const otherUsersNotes = allNotes.filter(n => n.userId && n.userId !== userId);
  const currentUserNotes = allNotes.filter(n => n.userId === userId || !n.userId);

  const noteMap = new Map();
  currentUserNotes.forEach(n => noteMap.set(n.id, n));

  clientNotes.forEach(clientNote => {
    const existing = noteMap.get(clientNote.id);
    if (!existing) {
      noteMap.set(clientNote.id, clientNote);
    } else {
      const clientTime = clientNote.updatedAt || clientNote.createdAt || 0;
      const serverTime = existing.updatedAt || existing.createdAt || 0;
      if (clientTime >= serverTime) {
        noteMap.set(clientNote.id, clientNote);
      }
    }
  });

  const mergedUserNotes = Array.from(noteMap.values());
  writeJSON(NOTES_FILE, [...otherUsersNotes, ...mergedUserNotes]);
  res.json({ notes: mergedUserNotes, timestamp: Date.now() });
});

// Start Server
app.listen(PORT, '0.0.0.0', () => {
  console.log(`=======================================================`);
  console.log(`🚀 SERVIDOR TASKMASTER CON AUTENTICACIÓN MULTI-USUARIO`);
  console.log(`📡 Acceso Web Local: http://localhost:${PORT}`);
  console.log(`📱 Acceso Android Emulator: http://10.0.2.2:${PORT}`);
  console.log(`🔐 API Autenticación: http://localhost:${PORT}/api/auth/login`);
  console.log(`=======================================================`);
});
