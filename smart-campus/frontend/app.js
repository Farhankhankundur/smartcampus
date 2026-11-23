// If the frontend is hosted separately (e.g. Vercel), put the backend URL here, e.g.
// const API_BASE = 'https://smart-campus-xxxx.onrender.com';
// If you use a vercel.json rewrite, or serve this from Spring Boot's static folder, leave it empty.
const API_BASE = '';

/* ---------- state ---------- */
const TOKEN_KEY = 'campus_token';
const store = {
  get() { try { return localStorage.getItem(TOKEN_KEY); } catch (e) { return null; } },
  set(v) { try { v ? localStorage.setItem(TOKEN_KEY, v) : localStorage.removeItem(TOKEN_KEY); } catch (e) { /* storage blocked */ } }
};
let token = store.get(), user = null, navBuilt = false;
let current = null, editingId = null, rowsCache = [], authMode = 'login';
const isAdmin = () => !!user && user.role === 'ADMIN';

// Each field: [jsonField, label, inputType, selectOptions?]
const ENTITIES = {
  students:  { label: 'Students',  one: 'student',  sub: 'Everyone enrolled on campus',
               fields: [['name','Name','text'], ['email','Email','email'], ['department','Department','text'], ['semester','Semester','number']] },
  faculty:   { label: 'Faculty',   one: 'faculty member', sub: 'Teaching staff and their departments',
               fields: [['name','Name','text'], ['email','Email','email'], ['department','Department','text']] },
  courses:   { label: 'Courses',   one: 'course',   sub: 'Subjects offered, with the faculty member in charge',
               fields: [['code','Code','text'], ['title','Title','text'], ['credits','Credits','number'], ['facultyId','Faculty ID','number']] },
  books:     { label: 'Library',   one: 'book',     sub: 'Browse the catalogue and borrow a book',
               fields: [['title','Title','text'], ['author','Author','text'], ['category','Category','text'], ['copies','Copies','number']],
               extra: [['available', 'Available']] },
  resources: { label: 'Resources', one: 'resource', sub: 'Classrooms, labs and equipment you can book',
               fields: [['name','Name','text'], ['type','Type','select',['Classroom','Lab','Equipment']], ['capacity','Capacity','number']] },
  events:    { label: 'Events',    one: 'event',    sub: 'Workshops, lectures and campus activities',
               fields: [['title','Title','text'], ['eventDate','Date','date'], ['venue','Venue','text']] }
};
const VIEWS = ['dashboard', 'students', 'faculty', 'courses', 'books', 'resources', 'events', 'booking', 'loans'];
const LABELS = { dashboard: 'Dashboard', booking: 'Booking', loans: 'Loans' };
const labelOf = (v) => LABELS[v] || ENTITIES[v].label;
const TONES = { students: 'var(--c1)', faculty: 'var(--c2)', courses: 'var(--c3)', books: '#0b7285', resources: 'var(--c4)',
                events: 'var(--c5)', bookings: 'var(--c6)', loans: '#9c36b5' };
const STAT_LABELS = { loans: 'Books on loan' };
const SUBS = { booking: 'Reserve a classroom, lab or equipment for a time slot', loans: 'Books that are out, when they are due, and any fines' };

const ICONS = {
  dashboard: '<rect x="3" y="3" width="7" height="7" rx="1.5"/><rect x="14" y="3" width="7" height="7" rx="1.5"/><rect x="3" y="14" width="7" height="7" rx="1.5"/><rect x="14" y="14" width="7" height="7" rx="1.5"/>',
  students:  '<path d="M22 9 12 4 2 9l10 5 10-5Z"/><path d="M6 11.5V16c0 1.5 2.7 3 6 3s6-1.5 6-3v-4.5"/>',
  faculty:   '<circle cx="12" cy="8" r="4"/><path d="M4 21c0-4 3.6-6 8-6s8 2 8 6"/>',
  courses:   '<path d="M4 5a2 2 0 0 1 2-2h13v15H6a2 2 0 0 0-2 2V5Z"/><path d="M4 20a2 2 0 0 0 2 1h13"/>',
  books:     '<path d="M12 6c-1.5-1.2-3.5-2-6-2H3v14h3c2.5 0 4.5.8 6 2 1.5-1.2 3.5-2 6-2h3V4h-3c-2.5 0-4.5.8-6 2Z"/><path d="M12 6v14"/>',
  resources: '<rect x="4" y="3" width="16" height="18" rx="2"/><path d="M9 8h2M13 8h2M9 12h2M13 12h2M10 21v-4h4v4"/>',
  events:    '<rect x="3" y="5" width="18" height="16" rx="2"/><path d="M3 10h18M8 3v4M16 3v4"/>',
  booking:   '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
  bookings:  '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
  loans:     '<path d="M4 8h14l-3-3"/><path d="M20 16H6l3 3"/>'
};
const icon = (k) => `<svg viewBox="0 0 24 24" aria-hidden="true">${ICONS[k]}</svg>`;

const $ = (sel) => document.querySelector(sel);
const esc = (v) => String(v ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const initials = (n) => String(n || '?').replace(/^(Dr|Prof|Mr|Ms|Mrs)\.?\s+/i, '').split(/\s+/).slice(0, 2).map((w) => w[0]).join('').toUpperCase();
const hue = (s) => { let h = 0; for (const c of String(s)) h = (h * 31 + c.charCodeAt(0)) % 360; return h; };

/* ---------- talking to the backend ---------- */
async function api(path, method = 'GET', body) {
  const headers = {};
  if (body) headers['Content-Type'] = 'application/json';
  if (token) headers['Authorization'] = 'Bearer ' + token;
  const res = await fetch(API_BASE + path, { method, headers, body: body ? JSON.stringify(body) : undefined });
  const text = await res.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch (e) { /* not JSON, e.g. server still waking up */ }
  if (res.status === 401 && !path.startsWith('/api/auth/')) {
    showLogin('Your session has ended. Please sign in again.');
    throw new Error('Please sign in again.');
  }
  if (!res.ok) throw new Error((data && data.error) || `Request failed (${res.status}). If the server was idle, wait a minute and try again.`);
  return data;
}

let toastTimer;
function toast(msg, bad = false) {
  const t = $('#toast');
  t.textContent = msg;
  t.className = 'show' + (bad ? ' bad' : '');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => (t.className = ''), 3200);
}

/* ---------- sign in / create account ---------- */
function setMode(mode) {
  authMode = mode;
  document.querySelectorAll('.tabs button').forEach((b) => b.classList.toggle('active', b.dataset.mode === mode));
  $('#nameField').hidden = mode !== 'register';
  $('#aName').required = mode === 'register';
  $('#aPass').autocomplete = mode === 'register' ? 'new-password' : 'current-password';
  $('#authBtn').textContent = mode === 'register' ? 'Create account' : 'Sign in';
  $('#authMsg').textContent = '';
}
document.querySelector('.tabs').onclick = (e) => { if (e.target.dataset.mode) setMode(e.target.dataset.mode); };

function showLogin(message = '') {
  token = null; user = null; store.set(null);
  document.body.classList.remove('authed', 'booting');
  setMode('login');
  $('#authMsg').textContent = message;
}

$('#authForm').onsubmit = async (e) => {
  e.preventDefault();
  $('#authMsg').textContent = '';
  const body = { username: $('#aUser').value, password: $('#aPass').value };
  if (authMode === 'register') body.fullName = $('#aName').value;
  try {
    const r = await api(authMode === 'register' ? '/api/auth/register' : '/api/auth/login', 'POST', body);
    token = r.token; store.set(token);
    $('#aPass').value = '';
    startApp(r.user);
  } catch (err) { $('#authMsg').textContent = err.message; }
};

$('#signOut').onclick = async () => {
  try { await api('/api/auth/logout', 'POST'); } catch (e) { /* ignore */ }
  showLogin('');
};

function startApp(u) {
  user = u;
  document.body.classList.remove('booting');
  document.body.classList.add('authed');
  if (!navBuilt) buildNav();
  $('#uName').textContent = u.fullName;
  $('#uRole').textContent = u.role === 'ADMIN' ? 'Administrator' : 'Student';
  $('#uAvatar').textContent = initials(u.fullName);
  show('dashboard');
}

/* ---------- navigation ---------- */
function buildNav() {
  navBuilt = true;
  $('#nav').innerHTML = VIEWS.map((v) => `<button data-view="${v}">${icon(v)}<span>${labelOf(v)}</span></button>`).join('');
  $('#nav').onclick = (e) => { const b = e.target.closest('button'); if (b) show(b.dataset.view); };
  $('#today').textContent = new Date().toLocaleDateString(undefined, { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
}

function show(view) {
  document.querySelectorAll('#nav button').forEach((b) => b.classList.toggle('active', b.dataset.view === view));
  $('#pageTitle').textContent = labelOf(view);
  $('#pageSub').textContent = view === 'dashboard' ? `Welcome back, ${user.fullName.split(' ')[0]}` : (SUBS[view] || ENTITIES[view].sub);
  const panel = ['dashboard', 'booking', 'loans'].includes(view) ? view : 'crud';
  ['dashboard', 'crud', 'booking', 'loans'].forEach((id) => ($('#' + id).hidden = id !== panel));
  window.scrollTo({ top: 0 });
  if (view === 'dashboard') loadDashboard();
  else if (view === 'booking') loadBooking();
  else if (view === 'loans') loadLoans();
  else openCrud(view);
}

/* ---------- dashboard ---------- */
function barList(obj, tone) {
  const entries = Object.entries(obj);
  if (!entries.length) return '<p class="note">No data yet.</p>';
  const max = Math.max(...entries.map(([, v]) => v));
  return `<div class="bars">${entries.map(([k, v]) => `<div class="bar-row" style="--tone:${tone}">
    <span class="name" title="${esc(k)}">${esc(k)}</span>
    <span class="track"><span class="fill" style="width:${Math.max(6, (v / max) * 100)}%"></span></span>
    <span class="num">${v}</span></div>`).join('')}</div>`;
}

async function loadDashboard() {
  try {
    const s = await api('/api/stats');
    const keys = ['students', 'faculty', 'courses', 'books', 'resources', 'events', 'bookings', 'loans'];
    const go = { bookings: 'booking' };
    $('#dashboard').innerHTML =
      `<div class="stats">${keys.map((k) => `<button class="stat" style="--tone:${TONES[k]}" data-go="${go[k] || k}">
         <span class="ico">${icon(k)}</span><b>${s[k]}</b><span>${STAT_LABELS[k] || k.charAt(0).toUpperCase() + k.slice(1)}</span></button>`).join('')}</div>
       <div class="panels">
         <div class="card"><h2>Students by department</h2>${barList(s.studentsByDepartment, 'var(--c1)')}</div>
         <div class="card"><h2>Resources by type</h2>${barList(s.resourcesByType, 'var(--c4)')}</div>
       </div>`;
    $('#dashboard').onclick = (e) => { const b = e.target.closest('[data-go]'); if (b) show(b.dataset.go); };
  } catch (err) { toast(err.message, true); }
}

/* ---------- generic list + form (students, faculty, courses, library, resources, events) ---------- */
function openCrud(key) {
  current = key; editingId = null;
  $('#title').textContent = ENTITIES[key].label;
  $('#search').value = '';
  $('#formCard').hidden = !isAdmin();      // only administrators can add or edit
  renderForm();
  loadTable();
}

function renderForm() {
  const e = ENTITIES[current];
  $('#formTitle').textContent = editingId ? `Edit ${e.one}` : `Add a new ${e.one}`;
  $('#form').innerHTML = e.fields.map(([name, label, type, options]) => `<label>${label}${
    type === 'select'
      ? `<select name="${name}">${options.map((o) => `<option>${o}</option>`).join('')}</select>`
      : `<input name="${name}" type="${type}" required>`}</label>`).join('') +
    `<div class="actions"><button type="submit" class="primary">${editingId ? 'Save changes' : 'Add record'}</button>${
      editingId ? '<button type="button" id="cancelEdit">Cancel</button>' : ''}</div>`;
  if (editingId) $('#cancelEdit').onclick = () => { editingId = null; renderForm(); };
}

$('#form').onsubmit = async (e) => {
  e.preventDefault();
  const body = {};
  ENTITIES[current].fields.forEach(([name, , type]) => {
    const v = e.target.elements[name].value;
    body[name] = type === 'number' ? (v === '' ? 0 : Number(v)) : v;
  });
  try {
    if (editingId) await api(`/api/${current}/${editingId}`, 'PUT', body);
    else await api(`/api/${current}`, 'POST', body);
    toast(editingId ? 'Changes saved' : 'Record added');
    editingId = null; renderForm(); loadTable();
  } catch (err) { toast(err.message, true); }
};

function cell(key, field, row) {
  const v = row[field];
  if (field === 'name' && (key === 'students' || key === 'faculty'))
    return `<div class="person"><span class="avatar" style="background:hsl(${hue(v)} 55% 42%)">${esc(initials(v))}</span>${esc(v)}</div>`;
  if (field === 'type') return `<span class="badge ${esc(v)}">${esc(v)}</span>`;
  if (field === 'semester') return `<span class="badge">Sem ${esc(v)}</span>`;
  if (field === 'available') return `<span class="badge ${v > 0 ? 'ok' : 'warn'}">${v} of ${row.copies}</span>`;
  return esc(v);
}

async function loadTable() {
  const key = current, q = $('#search').value.trim();
  try {
    rowsCache = await api(q ? `/api/${key}/search?q=${encodeURIComponent(q)}` : `/api/${key}`);
    const e = ENTITIES[key];
    const cols = [...e.fields.map((f) => [f[0], f[1]]), ...(e.extra || [])];
    $('#count').textContent = `${rowsCache.length} ${rowsCache.length === 1 ? 'record' : 'records'}`;
    const rows = rowsCache.map((r) => {
      const borrow = key === 'books' ? `<button class="soft" data-borrow="${r.id}" ${r.available < 1 ? 'disabled' : ''}>Borrow</button>` : '';
      const manage = isAdmin() ? `<button data-edit="${r.id}">Edit</button><button class="danger" data-del="${r.id}">Delete</button>` : '';
      return `<tr><td class="id">${r.id}</td>${cols.map((c) => `<td>${cell(key, c[0], r)}</td>`).join('')}
        <td><div class="row-actions">${borrow}${manage}</div></td></tr>`;
    }).join('');
    $('#table').innerHTML = `<thead><tr><th class="id">ID</th>${cols.map((c) => `<th>${c[1]}</th>`).join('')}<th></th></tr></thead>
      <tbody>${rows || `<tr><td class="empty" colspan="${cols.length + 2}">${q ? 'Nothing matches your search.' : 'No records yet.'}</td></tr>`}</tbody>`;
  } catch (err) { toast(err.message, true); }
}

$('#table').onclick = async (e) => {
  const b = e.target.closest('button'); if (!b) return;
  if (b.dataset.borrow) return borrowBook(b.dataset.borrow);
  if (b.dataset.edit) {
    const row = rowsCache.find((r) => r.id == b.dataset.edit);
    editingId = row.id; renderForm();
    ENTITIES[current].fields.forEach(([name]) => ($('#form').elements[name].value = row[name]));
    $('#form').scrollIntoView({ behavior: 'smooth', block: 'center' });
  } else if (b.dataset.del && confirm('Delete this record?')) {
    try { await api(`/api/${current}/${b.dataset.del}`, 'DELETE'); toast('Record deleted'); loadTable(); }
    catch (err) { toast(err.message, true); }
  }
};

let searchTimer;
$('#search').oninput = () => { clearTimeout(searchTimer); searchTimer = setTimeout(loadTable, 250); };

/* ---------- library: borrowing and returning ---------- */
async function borrowBook(bookId) {
  let member = null;
  if (isAdmin()) {                         // an administrator issues the book to someone
    member = prompt('Issue this book to (member name):');
    if (!member) return;
  }
  try {
    await api('/api/library/loans', 'POST', { bookId: Number(bookId), member, days: 14 });
    toast('Book issued. It is due back in 14 days.');
    loadTable();
  } catch (err) { toast(err.message, true); }
}

async function loadLoans() {
  try {
    const loans = await api('/api/library/loans');
    $('#loanTitle').textContent = isAdmin() ? 'All loans' : 'My borrowed books';
    $('#loanCount').textContent = `${loans.length} ${loans.length === 1 ? 'loan' : 'loans'}`;
    const status = (l) => l.returned ? '<span class="badge ok">Returned</span>'
      : l.fine > 0 ? `<span class="badge warn">Overdue, fine ₹${l.fine}</span>` : '<span class="badge">On loan</span>';
    $('#loanTable').innerHTML = `<thead><tr><th>Book</th><th>Member</th><th>Issued</th><th>Due</th><th>Status</th><th></th></tr></thead><tbody>${
      loans.map((l) => `<tr><td><b>${esc(l.bookTitle)}</b></td><td>${esc(l.member)}</td><td>${esc(l.issueDate)}</td><td>${esc(l.dueDate)}</td>
        <td>${status(l)}</td><td><div class="row-actions">${l.returned ? '' : `<button class="soft" data-return="${l.id}">Mark returned</button>`}</div></td></tr>`).join('')
      || '<tr><td class="empty" colspan="6">No loans yet. Borrow a book from the Library page.</td></tr>'}</tbody>`;
  } catch (err) { toast(err.message, true); }
}

$('#loanTable').onclick = async (e) => {
  const b = e.target.closest('[data-return]'); if (!b) return;
  try { await api(`/api/library/loans/${b.dataset.return}/return`, 'POST'); toast('Book returned'); loadLoans(); }
  catch (err) { toast(err.message, true); }
};

/* ---------- resource booking ---------- */
async function loadBooking() {
  try {
    const [resources, bookings] = await Promise.all([api('/api/resources'), api('/api/bookings')]);
    $('#bkResource').innerHTML = resources.map((r) => `<option value="${r.id}">${esc(r.name)} (${esc(r.type)})</option>`).join('')
      || '<option value="">No resources yet</option>';
    $('#bkUser').value = user.fullName;
    $('#bkUser').readOnly = !isAdmin();
    const byId = Object.fromEntries(resources.map((r) => [r.id, r]));
    $('#bkTable').innerHTML = `<thead><tr><th>Resource</th><th>Type</th><th>Booked by</th><th>Time</th><th></th></tr></thead><tbody>${
      bookings.map((b) => { const r = byId[b.resourceId] || {};
        return `<tr><td><b>${esc(r.name || b.resourceId)}</b></td><td><span class="badge ${esc(r.type)}">${esc(r.type || '')}</span></td>
        <td>${esc(b.bookedBy)}</td><td>${esc(b.slot.replace('T', ' '))}</td>
        <td><div class="row-actions"><button class="danger" data-cancel="${b.id}">Cancel</button></div></td></tr>`; }).join('')
      || '<tr><td class="empty" colspan="5">No bookings yet. Book a resource above.</td></tr>'}</tbody>`;
  } catch (err) { toast(err.message, true); }
}

$('#bkForm').onsubmit = async (e) => {
  e.preventDefault();
  try {
    await api('/api/bookings', 'POST', { resourceId: Number($('#bkResource').value), bookedBy: $('#bkUser').value, slot: $('#bkSlot').value });
    toast('Booked'); loadBooking();
  } catch (err) { toast(err.message, true); }   // e.g. 409: slot already taken
};

$('#bkTable').onclick = async (e) => {
  const b = e.target.closest('button');
  if (!b || !b.dataset.cancel) return;
  try { await api(`/api/bookings/${b.dataset.cancel}`, 'DELETE'); toast('Booking cancelled'); loadBooking(); }
  catch (err) { toast(err.message, true); }
};

/* ---------- start ---------- */
(async function boot() {
  if (!token) return showLogin();
  try { startApp(await api('/api/auth/me')); } catch (e) { showLogin(); }
})();
