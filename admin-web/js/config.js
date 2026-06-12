// js/config.js
export const firebaseConfig = {
  apiKey: "AIzaSyD0EsKQwSm2JJ9FVoyfsdPaBRbB8C4c63E",
  authDomain: "rentify-834a9.firebaseapp.com",
  projectId: "rentify-834a9",
  storageBucket: "rentify-834a9.firebasestorage.app",
  messagingSenderId: "617779268466",
  appId: "1:617779268466:web:7a06ee2ed85efc98f26beb"
};

// Inisialisasi global via compat SDK instance
firebase.initializeApp(firebaseConfig);

export const auth = firebase.auth();
export const db = firebase.firestore();