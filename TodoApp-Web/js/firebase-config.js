/**
 * FIREBASE CONFIGURATION
 * 
 * Para conectar con tu proyecto gratuito de Firebase:
 * 1. Entra a https://console.firebase.google.com
 * 2. Crea un proyecto gratis y añade una App Web (</>).
 * 3. Pega aquí los datos de tu objeto 'firebaseConfig'.
 */

const firebaseConfig = {
  apiKey: "TU_API_KEY",
  authDomain: "tu-proyecto.firebaseapp.com",
  projectId: "tu-proyecto",
  storageBucket: "tu-proyecto.appspot.com",
  messagingSenderId: "1234567890",
  appId: "1:1234567890:web:abcdef123456"
};

// Export config
window.firebaseConfig = firebaseConfig;
