# 🔥 Guía Paso a Paso: Firebase 100% Gratis y 24/7

Con Google Firebase tendrás **sincronización en tiempo real las 24 horas del día** entre tu Web y tu teléfono Android, **sin pagar nada y sin necesidad de tarjeta de crédito**.

---

## ⚡ Paso 1: Crear tu Proyecto en Firebase (1 minuto)

1. Entra a [**console.firebase.google.com**](https://console.firebase.google.com) con tu cuenta de Google.
2. Haz clic en **"Agregar proyecto"** (o *Crear un proyecto*).
3. Escribe el nombre: `TaskMaster` y presiona **Continuar**.
4. Desactiva Google Analytics (opcional) y haz clic en **"Crear proyecto"**.

---

## 🔐 Paso 2: Activar Inicio de Sesión (Authentication)

1. En el menú de la izquierda, entra a **Compilación > Authentication**.
2. Haz clic en **"Comenzar"**.
3. En los métodos de acceso, selecciona **"Correo electrónico/contraseña"**.
4. Activa la primera casilla (**Habilitar**) y presiona **"Guardar"**.

---

## 🗄️ Paso 3: Activar la Base de Datos en Tiempo Real (Cloud Firestore)

1. En el menú de la izquierda, entra a **Compilación > Firestore Database**.
2. Haz clic en **"Crear base de datos"**.
3. Selecciona la ubicación más cercana (ej: `us-central` o la sugerida) y presiona **Siguiente**.
4. Selecciona **"Iniciar en modo de prueba"** y presiona **Habilitar**.

---

## 🌐 Paso 4: Conectar la Web (Netlify o Local)

1. En la página principal del proyecto Firebase, haz clic en el icono Web **`</>`** (para agregar una app).
2. Ponle de nombre `TaskMaster-Web` y presiona **Registrar app**.
3. Te aparecerá un bloque de código como este:

```javascript
const firebaseConfig = {
  apiKey: "AIzaSy...",
  authDomain: "taskmaster-xxxx.firebaseapp.com",
  projectId: "taskmaster-xxxx",
  storageBucket: "taskmaster-xxxx.appspot.com",
  messagingSenderId: "123456789",
  appId: "1:123456789:web:abcdef"
};
```

4. Abre el archivo [`TodoApp-Web/js/firebase-config.js`](file:///c:/Users/crisd/OneDrive/Escritorio/MIO%20PROGRAMAS/TodoApp-Web/js/firebase-config.js) en tu computadora y pega tus claves allí.
5. ¡Listo! Ya puedes subirlo a Netlify o GitHub y tu Web estará sincronizada en tiempo real 24/7.

---

## 📱 Paso 5: Conectar la App Android

1. En Firebase, haz clic en el icono de **Android 🤖** (Agregar app).
2. En *Nombre del paquete de Android*, escribe: `com.cris.taskmaster`
3. Haz clic en **Registrar app** y descarga el archivo **`google-services.json`**.
4. Coloca ese archivo `google-services.json` dentro de la carpeta:
   `TodoApp-Android/app/`
5. ¡Listo! Al abrir la app en tu teléfono, tus tareas y notas se sincronizarán directamente con la nube de Google Firebase.
