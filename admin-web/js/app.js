// js/app.js
import { handleAuthState, doLogin, doLogout } from './auth.js';
import { 
  filterUsers, 
  filterItems, 
  filterTxns, 
  addCategory, 
  closeModal, 
  executeDelete 
} from './controllers.js';

const pageTitles = { 
  dashboard: 'Dashboard', 
  users: 'Manajemen Pengguna', 
  items: 'Moderasi Barang', 
  transactions: 'Monitoring Transaksi', 
  categories: 'Manajemen Kategori' 
};

// --- LOGIKA NAVIGASI PAGE ---
function goTo(page, btn) {
  document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
  document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
  
  document.getElementById('page-' + page).classList.add('active');
  if (btn) {
    btn.classList.add('active');
  } else {
    document.querySelector(`[data-page="${page}"]`)?.classList.add('active');
  }
  document.getElementById('topbar-title').textContent = pageTitles[page];
}

// --- ATTACH EVENT LISTENERS ON DOM LOAD ---
document.addEventListener('DOMContentLoaded', () => {
  // Jalankan pemantau Auth Firebase
  handleAuthState();

  // Tombol Aksi Autentikasi
  document.getElementById('login-submit-btn').onclick = doLogin;
  document.getElementById('logout-btn').onclick = doLogout;

  // Navigasi Sidebar
  document.querySelectorAll('.nav-item').forEach(btn => {
    btn.onclick = (e) => {
      const page = btn.getAttribute('data-page');
      goTo(page, btn);
    };
  });

  // Filter & Pencarian
  document.getElementById('user-search').oninput = filterUsers;
  document.getElementById('user-role-filter').onchange = filterUsers;
  document.getElementById('item-search').oninput = filterItems;
  document.getElementById('item-status-filter').onchange = filterItems;
  document.getElementById('txn-status-filter').onchange = filterTxns;

  // Kategori Baru
  document.getElementById('add-category-btn').onclick = addCategory;
  document.getElementById('new-category-input').onkeydown = (e) => {
    if (e.key === 'Enter') addCategory();
  };

  // Tombol Konfirmasi Global Modal Hapus
  document.getElementById('modal-cancel-btn').onclick = closeModal;
  document.getElementById('modal-confirm-btn').onclick = executeDelete;
});