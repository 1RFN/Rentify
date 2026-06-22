// js/auth.js
import { auth, db } from './config.js';
import { showToast, initDataListeners } from './controllers.js';

export function handleAuthState() {
  auth.onAuthStateChanged(user => {
    if (user) {
      db.collection('users').doc(user.uid).get().then(doc => {
        const role = doc.exists ? doc.data().role : '';
        if (role !== 'admin') {
          showToast('Akun ini bukan admin!');
          auth.signOut();
          return;
        }
        document.getElementById('login-screen').style.display = 'none';
        document.getElementById('app').style.display = 'flex';
        document.getElementById('admin-email-display').textContent = user.email;
        initDataListeners(); // Memulai sinkronisasi data Firestore setelah terverifikasi
      });
    } else {
      document.getElementById('login-screen').style.display = 'flex';
      document.getElementById('app').style.display = 'none';
    }
  });
}

export function doLogin() {
  const email = document.getElementById('login-email').value;
  const pass = document.getElementById('login-password').value;
  const err = document.getElementById('login-error');
  err.style.display = 'none';

  auth.signInWithEmailAndPassword(email, pass).catch(e => {
    err.textContent = 'Email atau password salah.';
    err.style.display = 'block';
  });
}

export function doLogout() {
  auth.signOut();
}