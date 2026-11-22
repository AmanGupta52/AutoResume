import { h, toast, modal, withBusy, fill } from './ui.js';
import { t, LANGS, getLang, setLang } from './i18n.js';
import { aiOn, loadAiState } from './aistate.js';
import { session, api } from './api.js';
import { loginView, registerView, verifyView, forgotView, resetView } from './auth.js';
import { renderDashboard, deleteAccountFlow } from './dashboard.js';
import { renderEditor } from './editor.js';
import { renderGuided } from './guided.js';

const THEME = 'autoresume_theme';
const applyTheme = (v) => { if (v === 'light' || v === 'dark') document.documentElement.dataset.theme = v; else delete document.documentElement.dataset.theme; };
try { applyTheme(localStorage.getItem(THEME) || 'system'); } catch { /* ignore */ }

const root = document.getElementById('app');
let leave = () => {};

const nav = (hash) => {
  if (location.hash === hash) render(); else location.hash = hash;
};

const AUTH_ROUTES = { '#/login': loginView, '#/register': registerView, '#/verify': verifyView, '#/forgot': forgotView, '#/reset': resetView };

function shell(content) {
  const user = session.user;
  return h('div', { class: 'shell' },
    h('header', { class: 'topbar' },
      h('a', { href: '#/resumes', class: 'brand' }, 'AutoResume'),
      h('span', { class: 'spacer' }),
      h('span', { class: 'muted small user' }, user ? user.name : ''),
      h('button', { class: 'btn small', onclick: openSettings }, 'Settings'),
      h('button', { class: 'btn small', onclick: logout }, 'Log out')),
    content);
}

function openSettings() {
  const user = session.user;
  const lang = h('div', { class: 'seg' }, LANGS.map(([c, name]) => h('button', { type: 'button', class: 'btn small' + (getLang() === c ? ' primary' : ''), 'aria-pressed': String(getLang() === c), onclick: () => { setLang(c); m.close(); render(); openSettings(); } }, name)));
  const theme = h('select', {}, [['system', 'System'], ['light', 'Light'], ['dark', 'Dark']].map(([v, name]) => h('option', { value: v, selected: (localStorage.getItem(THEME) || 'system') === v }, name)));
  theme.addEventListener('change', () => { try { localStorage.setItem(THEME, theme.value); } catch { /* ignore */ } applyTheme(theme.value); });
  const aiText = h('p', { class: 'muted', role: 'status' }, 'Checking...');
  const test = h('button', { class: 'btn', type: 'button', hidden: true, onclick: () => withBusy(test, 'Testing...', async () => {
    try { const r = await api.aiTest(); toast(`AI works (${r.ms} ms).`); } catch (e) { toast(e.message + (e.data && e.data.detail ? ` (${e.data.detail})` : ''), 'error'); }
  }) }, 'Test AI connection');
  loadAiState(true).then((st) => {
    aiText.textContent = st.enabled ? `On. Suggestions, summary, bullets, skills, cover letters and the Ask AI helper work in the editor.${st.remaining != null ? ` ${st.remaining} AI requests left today.` : ''}` : 'Off. The server has no NVIDIA key. Add NVIDIA_API_KEY to the backend settings.';
    test.hidden = !st.enabled;
  });
  const field = (l, c) => h('label', { class: 'field' }, h('span', {}, l), c);
  const m = modal('Settings', h('div', {},
    h('h3', {}, 'Language'), lang, h('p', { class: 'muted small' }, 'Changes the menus and buttons. Your resume text is never translated.'),
    field('Theme', theme),
    h('h3', {}, 'AI writing help'), aiText, test,
    h('h3', {}, 'Account'), h('p', { class: 'muted' }, user ? `${user.name} · ${user.email}` : ''),
    h('p', { class: 'muted small' }, 'Your resumes are saved to your account and sync with the phone app.'),
    h('div', { class: 'actions' }, h('button', { class: 'btn', type: 'button', onclick: () => { m.close(); logout(); } }, 'Log out'), h('button', { class: 'btn danger', type: 'button', onclick: () => { m.close(); deleteAccountFlow(nav); } }, 'Delete account'))));
}

function logout() {
  session.clear();
  toast('You are logged out.');
  nav('#/login');
}

function render() {
  leave(); leave = () => {};
  const hash = location.hash || '#/';
  const loggedIn = !!session.token;
  const authView = AUTH_ROUTES[hash];

  if (authView) {
    if (loggedIn && hash !== '#/verify' && hash !== '#/reset') return nav('#/resumes');
    fill(root, authView(nav));
    return;
  }
  if (!loggedIn) return nav('#/login');

  const edit = hash.match(/^#\/edit\/([A-Za-z0-9]+)$/);
  const guided = hash.match(/^#\/guided\/([A-Za-z0-9]+)$/);
  const body = h('div', { class: 'content' });
  fill(root, shell(body));
  if (edit) {
    leave = renderEditor(body, edit[1], nav);
  } else if (guided) {
    leave = renderGuided(body, guided[1], nav);
  } else {
    renderDashboard(body, nav);
  }
}

window.addEventListener('hashchange', render);
if (session.token) loadAiState();
window.addEventListener('auth:expired', () => { toast('Your session expired. Please log in again.', 'error'); nav('#/login'); });
render();
