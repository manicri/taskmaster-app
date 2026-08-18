# ☁️ Guía de Sincronización en la Nube 24/7 (Siempre Encendido)

Para que la sincronización entre tu teléfono Android y la Web funcione **a todas horas, en cualquier lugar del mundo y aunque apagues tu laptop**, tu servidor backend debe estar alojado en la nube (en un servicio gratuito en internet).

El servidor ya está 100% preparado y optimizado para funcionar en plataformas gratuitas en la nube como **Render.com** o **Railway.app**.

---

## 🚀 Opción Recomendada: Despliegue Gratis en Render.com (En 3 minutos)

**Render** ofrece un plan gratuito que mantiene tu servidor y tu página web activos las 24 horas del día.

### Pasos Sencillos:

1. **Subir tu proyecto a GitHub**:
   - Crea un repositorio en [GitHub.com](https://github.com) (puedes llamarlo `taskmaster-app`).
   - Sube la carpeta del proyecto a tu repositorio.

2. **Crear el servicio en Render**:
   - Entra a [Render.com](https://render.com) e inicia sesión con tu cuenta de GitHub.
   - Haz clic en el botón **New +** y selecciona **Web Service**.
   - Conecta tu repositorio de GitHub.
   - Configura estos 3 campos (Render los detecta automáticamente):
     - **Root Directory**: `TodoApp-Server`
     - **Build Command**: `npm install`
     - **Start Command**: `node server.js`
   - Elige el plan **Free** ($0/mes) y haz clic en **Deploy Web Service**.

3. **Obtener tu enlace permanente**:
   - En pocos segundos, Render te dará un enlace público seguro como:
     `https://taskmaster-xxxx.onrender.com`
   - ¡Esa dirección estará **activa 24/7 en internet**!

---

## 📱 Cómo conectar tu App Android y tu Web a tu Servidor en la Nube

Una vez tengas tu URL de la nube (ejemplo: `https://mi-taskmaster.onrender.com`):

### 1. En tu teléfono Android:
1. Abre la app **TaskMaster**.
2. Toca el icono de engranaje **⚙️ (Ajustes de Servidor)** en la parte superior derecha.
3. Borra `http://10.0.2.2:3000` y pega tu enlace de la nube:
   `https://mi-taskmaster.onrender.com`
4. Presiona **Guardar**.
5. ¡Listo! Tu teléfono se sincronizará automáticamente desde cualquier lugar (con datos móviles o WiFi), sin importar si tu computadora está apagada.

### 2. En la Web:
- Puedes abrir directamente tu enlace en cualquier navegador (computadora, tablet u otro móvil):
  `https://mi-taskmaster.onrender.com`
- Tu página web cargará instantáneamente desde los servidores en la nube y podrás iniciar sesión con tu cuenta.

---

## 🛡️ ¿Qué pasa si estás sin internet en tu teléfono?
La aplicación cuenta con arquitectura **Offline-First**:
- Podrás seguir creando y completando tareas y notas normalmente.
- En cuanto tu teléfono vuelva a tener conexión a internet, se sincronizará automáticamente con la nube sin perder ningún dato.
