// js/controllers.js
import { db } from './config.js';

// Global App States
export let state = {
  allUsers: [],
  allItems: [],
  allTxns: [],
  categories: [],
  pendingDelete: { col: null, id: null }
};

// --- UTILS ---
export function esc(s) {
  const d = document.createElement('div');
  d.textContent = String(s);
  return d.innerHTML;
}

export function fmtDate(d) {
  return d.toLocaleDateString('id-ID', { day: '2-digit', month: 'short', year: 'numeric' });
}

export function showToast(msg) {
  const t = document.getElementById('toast');
  t.textContent = msg;
  t.classList.add('show');
  setTimeout(() => t.classList.remove('show'), 2800);
}

// --- POPUP/MODAL SYSTEM ---
export function confirmDelete(col, id, msg) {
  state.pendingDelete = { col, id };
  document.getElementById('modal-body').textContent = msg;
  document.getElementById('confirm-modal').classList.add('open');
}

export function closeModal() {
  document.getElementById('confirm-modal').classList.remove('open');
}

export function executeDelete() {
  if (!state.pendingDelete.col) return;
  db.collection(state.pendingDelete.col).doc(state.pendingDelete.id).delete()
    .then(() => {
      showToast('Data berhasil dihapus');
      closeModal();
    }).catch(err => showToast('Gagal menghapus data: ' + err.message));
}

// --- CONTROLLER: DASHBOARD ---
export function updateDashStats() {
  document.getElementById('stat-users').textContent = state.allUsers.length;
  document.getElementById('stat-items').textContent = state.allItems.length;
  document.getElementById('stat-txn').textContent = state.allTxns.length;
  document.getElementById('stat-pending').textContent = state.allTxns.filter(t => t.status === 'Menunggu Konfirmasi').length;
}

export function renderRecentUsers() {
  const el = document.getElementById('dash-recent-users');
  const recent = state.allUsers.slice(0, 5);
  if (!recent.length) { el.innerHTML = '<div class="empty-state"><p>Belum ada pengguna</p></div>'; return; }
  el.innerHTML = `<table>${recent.map(u => {
    const init = (u.name || u.email || '?').substring(0, 2).toUpperCase();
    const rc = { renter: 'badge-blue', owner: 'badge-green', admin: 'badge-red' }[u.role] || 'badge-gray';
    return `<tr><td><div class="user-cell"><div class="avatar avatar-green">${init}</div><div><div class="fw500" style="font-size:13px">${esc(u.name || '—')}</div><div class="text-muted text-sm">${esc(u.email || '—')}</div></div></div></td><td><span class="badge ${rc}">${esc(u.role || '—')}</span></td></tr>`;
  }).join('')}</table>`;
}

export function renderRecentTxns() {
  const el = document.getElementById('dash-recent-txn');
  const recent = state.allTxns.slice(0, 5);
  if (!recent.length) { el.innerHTML = '<div class="empty-state"><p>Belum ada transaksi</p></div>'; return; }
  el.innerHTML = `<table>${recent.map(t => {
    const scMap = { 'Menunggu Konfirmasi': 'badge-gold', 'Disewa': 'badge-blue', 'Selesai': 'badge-green' };
    const sc = scMap[t.status] || 'badge-gray';
    const total = typeof t.total_price === 'number' ? 'Rp ' + t.total_price.toLocaleString('id') : '—';
    return `<tr><td style="font-size:12px;color:var(--text-muted)">${esc((t.transaction_id || t.id || '').substring(0, 14))}...</td><td class="fw500" style="font-size:13px">${total}</td><td><span class="badge ${sc}">${esc(t.status || '—')}</span></td></tr>`;
  }).join('')}</table>`;
}

// --- CONTROLLER: USERS ---
export function renderUsers(users) {
  document.getElementById('user-count').textContent = users.length + ' pengguna';
  const tbody = document.getElementById('users-tbody');
  if (!users.length) { tbody.innerHTML = '<tr><td colspan="5"><div class="empty-state"><p>Tidak ada pengguna ditemukan</p></div></td></tr>'; return; }
  tbody.innerHTML = users.map(u => {
    const initials = (u.name || u.email || '?').substring(0, 2).toUpperCase();
    const roleMap = { renter: 'badge-blue', owner: 'badge-green', admin: 'badge-red' };
    const rc = roleMap[u.role] || 'badge-gray';
    return `<tr>
      <td><div class="user-cell"><div class="avatar avatar-green">${initials}</div><div><div class="fw500">${esc(u.name || '—')}</div><div class="text-muted text-sm">${esc(u.email || '—')}</div></div></div></td>
      <td>${esc(u.phone || '—')}</td>
      <td><span class="badge ${rc}">${esc(u.role || '—')}</span></td>
      <td class="text-muted">${esc(u.address || '—')}</td>
      <td><div class="gap4">
        <button class="btn btn-sm btn-gold change-role-btn" data-id="${u.id}" data-role="${u.role}">Ubah Role</button>
        <button class="btn btn-sm btn-danger delete-user-btn" data-id="${u.id}" data-name="${esc(u.name || u.email)}">Hapus</button>
      </div></td>
    </tr>`;
  }).join('');
  
  // Re-attach dynamically rendered listeners
  document.querySelectorAll('.change-role-btn').forEach(btn => {
    btn.onclick = () => changeRole(btn.dataset.id, btn.dataset.role);
  });
  document.querySelectorAll('.delete-user-btn').forEach(btn => {
    btn.onclick = () => confirmDelete('users', btn.dataset.id, `Hapus pengguna ${btn.dataset.name}?`);
  });
}

export function filterUsers() {
  const q = document.getElementById('user-search').value.toLowerCase();
  const r = document.getElementById('user-role-filter').value;
  let filtered = state.allUsers.filter(u => {
    const matchQ = !q || (u.name || '').toLowerCase().includes(q) || (u.email || '').toLowerCase().includes(q);
    const matchR = !r || u.role === r;
    return matchQ && matchR;
  });
  renderUsers(filtered);
}

function changeRole(uid, currentRole) {
  const roles = ['renter', 'owner', 'admin'];
  const next = roles[(roles.indexOf(currentRole) + 1) % roles.length];
  if (!confirm(`Ubah role menjadi "${next}"?`)) return;
  db.collection('users').doc(uid).update({ role: next }).then(() => showToast('Role berhasil diubah ke ' + next));
}

// --- CONTROLLER: ITEMS ---
export function renderItems(items) {
  document.getElementById('item-count').textContent = items.length + ' barang';
  const tbody = document.getElementById('items-tbody');
  if (!items.length) { tbody.innerHTML = '<tr><td colspan="6"><div class="empty-state"><p>Tidak ada barang</p></div></td></tr>'; return; }
  tbody.innerHTML = items.map(i => {
    const sc = i.status === 'Tersedia' ? 'badge-green' : 'badge-gold';
    const price = typeof i.price_per_day === 'number' ? 'Rp ' + i.price_per_day.toLocaleString('id') : '—';
    return `<tr>
      <td><div class="fw500">${esc(i.title || '—')}</div><div class="text-muted text-sm">Owner: ${esc(i.owner_id || '—').substring(0, 12)}...</div></td>
      <td><span class="badge badge-gray">${esc(i.category_name || '—')}</span></td>
      <td>${price}/hari</td>
      <td>${i.stock || 0}</td>
      <td><span class="badge ${sc}">${esc(i.status || '—')}</span></td>
      <td><button class="btn btn-sm btn-danger delete-item-btn" data-id="${i.id}" data-title="${esc(i.title)}">Hapus</button></td>
    </tr>`;
  }).join('');

  document.querySelectorAll('.delete-item-btn').forEach(btn => {
    btn.onclick = () => confirmDelete('items', btn.dataset.id, `Hapus barang "${btn.dataset.title}"?`);
  });
}

export function filterItems() {
  const q = document.getElementById('item-search').value.toLowerCase();
  const s = document.getElementById('item-status-filter').value;
  let filtered = state.allItems.filter(i => {
    const matchQ = !q || (i.title || '').toLowerCase().includes(q);
    const matchS = !s || i.status === s;
    return matchQ && matchS;
  });
  renderItems(filtered);
}

// --- CONTROLLER: TRANSACTIONS ---
export function renderTxns(txns) {
  document.getElementById('txn-count').textContent = txns.length + ' transaksi';
  const tbody = document.getElementById('txns-tbody');
  if (!txns.length) { tbody.innerHTML = '<tr><td colspan="6"><div class="empty-state"><p>Tidak ada transaksi</p></div></td></tr>'; return; }
  tbody.innerHTML = txns.map(t => {
    const scMap = { 'Menunggu Konfirmasi': 'badge-gold', 'Disewa': 'badge-blue', 'Selesai': 'badge-green' };
    const sc = scMap[t.status] || 'badge-gray';
    const start = t.start_date?.toDate ? fmtDate(t.start_date.toDate()) : '—';
    const end = t.end_date?.toDate ? fmtDate(t.end_date.toDate()) : '—';
    const total = typeof t.total_price === 'number' ? 'Rp ' + t.total_price.toLocaleString('id') : '—';
    const shortId = t.transaction_id || t.id || '';
    return `<tr>
      <td class="text-sm text-muted">${esc(shortId.substring(0, 12))}...</td>
      <td class="text-sm">${esc(t.item_id || '—').substring(0, 14)}...</td>
      <td class="text-sm text-muted">${esc(t.renter_id || '—').substring(0, 12)}...</td>
      <td class="text-sm">${start} → ${end}</td>
      <td class="fw500">${total}</td>
      <td><span class="badge ${sc}">${esc(t.status || '—')}</span></td>
    </tr>`;
  }).join('');
}

export function filterTxns() {
  const s = document.getElementById('txn-status-filter').value;
  let filtered = s ? state.allTxns.filter(t => t.status === s) : state.allTxns;
  renderTxns(filtered);
}

// --- CONTROLLER: CATEGORIES ---
export function renderCategoriesList() {
  const el = document.getElementById('categories-list');
  if (!state.categories.length) {
    el.innerHTML = '<p style="color:var(--text-muted);font-size:13px;padding:8px 0">Belum ada kategori. Tambahkan di atas.</p>';
    return;
  }
  el.innerHTML = `<div style="display:flex;flex-wrap:wrap;gap:8px">${
    state.categories.map(c => `<div style="display:flex;align-items:center;gap:6px;background:var(--bg);border:1px solid var(--border);border-radius:8px;padding:6px 12px;font-size:13px">
      <span>${esc(c.name || c.id)}</span>
      <button class="delete-cat-btn" data-id="${c.id}" style="background:none;border:none;cursor:pointer;color:var(--danger);font-size:16px;line-height:1;padding:0 0 0 4px">&times;</button>
    </div>`).join('')
  }</div>`;

  document.querySelectorAll('.delete-cat-btn').forEach(btn => {
    btn.onclick = () => deleteCategory(btn.dataset.id);
  });
}

export function renderCatStats() {
  const counts = {};
  state.allItems.forEach(i => { const c = i.category_name || 'Lainnya'; counts[c] = (counts[c] || 0) + 1; });
  const tbody = document.getElementById('cat-stats-tbody');
  const entries = Object.entries(counts).sort((a, b) => b[1] - a[1]);
  if (!entries.length) { tbody.innerHTML = '<tr><td colspan="2" class="text-muted" style="padding:16px">Belum ada data</td></tr>'; return; }
  tbody.innerHTML = entries.map(([name, count]) => `<tr><td>${esc(name)}</td><td><span class="badge badge-green">${count} barang</span></td></tr>`).join('');
}

export function addCategory() {
  const val = document.getElementById('new-category-input').value.trim();
  if (!val) { showToast('Nama kategori tidak boleh kosong'); return; }
  db.collection('categories').add({ name: val, createdAt: firebase.firestore.FieldValue.serverTimestamp() })
    .then(() => {
      document.getElementById('new-category-input').value = '';
      showToast('Kategori ditambahkan!');
    });
}

function deleteCategory(id) {
  if (!confirm('Hapus kategori ini?')) return;
  db.collection('categories').doc(id).delete().then(() => showToast('Kategori dihapus'));
}

// --- FIREBASE REALTIME LISTENERS INITIALIZERS ---
export function initDataListeners() {
  db.collection('users').onSnapshot(snap => {
    state.allUsers = snap.docs.map(d => ({ id: d.id, ...d.data() }));
    renderUsers(state.allUsers);
    updateDashStats();
    renderRecentUsers();
  });

  db.collection('items').onSnapshot(snap => {
    state.allItems = snap.docs.map(d => ({ id: d.id, ...d.data() }));
    renderItems(state.allItems);
    updateDashStats();
    renderCatStats();
  });

  const loadTxnFallback = () => {
    db.collection('transactions').onSnapshot(snap => {
      state.allTxns = snap.docs.map(d => ({ id: d.id, ...d.data() }));
      renderTxns(state.allTxns);
      updateDashStats();
      renderRecentTxns();
    });
  };

  db.collection('transactions').orderBy('start_date', 'desc').onSnapshot(snap => {
    state.allTxns = snap.docs.map(d => ({ id: d.id, ...d.data() }));
    renderTxns(state.allTxns);
    updateDashStats();
    renderRecentTxns();
  }, err => {
    console.warn("Index not ready yet, falling back to unordered query...", err);
    loadTxnFallback();
  });

  db.collection('categories').onSnapshot(snap => {
    state.categories = snap.docs.map(d => ({ id: d.id, ...d.data() }));
    renderCategoriesList();
  });
}