import { h, toast, modal, withBusy, fill } from './ui.js';
import { t, LANGS, getLang, setLang } from './i18n.js';
import { aiOn, loadAiState } from './aistate.js';
import { session, api, API_BASE } from './api.js';
import { landingView } from './landing.js';
import { loginView, registerView, verifyView, forgotView, resetView } from './auth.js';
import { renderDashboard, deleteAccountFlow } from './dashboard.js';
import { renderEditor } from './editor.js';
import { renderGuided } from './guided.js';

// Theme (system / light / dark / OLED black) is handled by /theme.js, which runs before first paint.
const TH = window.AutoResumeTheme;

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
      themeToggle(),
      h('button', { class: 'btn small', onclick: openSettings }, 'Settings'),
      h('button', { class: 'btn small', onclick: logout }, 'Log out')),
    content);
}

/** Quick theme button in the top bar: cycles System, Light, Dark, OLED black. */
function themeToggle() {
  if (!TH) return null;
  const b = h('button', { class: 'btn small', type: 'button' });
  const sync = () => { b.textContent = TH.glyph(); b.title = TH.label(); b.setAttribute('aria-label', TH.label()); };
  b.addEventListener('click', () => { TH.cycle(); sync(); });
  sync();
  return b;
}

function openSettings() {
  const user = session.user;
  const lang = h('div', { class: 'seg' }, LANGS.map(([c, name]) => h('button', { type: 'button', class: 'btn small' + (getLang() === c ? ' primary' : ''), 'aria-pressed': String(getLang() === c), onclick: () => { setLang(c); m.close(); render(); openSettings(); } }, name)));
  const themeOptions = TH ? TH.options : [['system', 'System'], ['light', 'Light'], ['dark', 'Dark']];
  const theme = h('select', {}, themeOptions.map(([v, name]) => h('option', { value: v, selected: (TH ? TH.get() : 'system') === v }, name)));
  theme.addEventListener('change', () => { if (TH) TH.set(theme.value); });
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
  if (!loggedIn) {
    if (hash === '#/' || hash === '#') { fill(root, landingView()); return; }
    return nav('#/login');
  }

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

// If the backend address is wrong or the server is asleep, say so instead of failing silently on login.
async function checkServer() {
  document.getElementById('server-banner')?.remove();
  let ok = false;
  try {
    const res = await fetch(API_BASE + '/api/health', { cache: 'no-store' });
    const j = await res.json();
    ok = res.ok && j && j.ok === true;
  } catch { ok = false; }
  if (ok) return;
  const where = API_BASE ? 'the server at ' + API_BASE : 'a server. Set API_BASE in config.js';
  const banner = h('div', { id: 'server-banner', class: 'server-banner', role: 'alert' },
    h('span', {}, 'Cannot reach ' + where + '. Free servers can take about a minute to wake up.'),
    h('button', { type: 'button', onclick: checkServer }, 'Retry'));
  document.body.prepend(banner);
}
checkServer();
