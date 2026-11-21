import { h, toast } from './ui.js';
import { session } from './api.js';
import { loginView, registerView, verifyView, forgotView, resetView } from './auth.js';
import { renderDashboard, deleteAccountFlow } from './dashboard.js';
import { renderEditor } from './editor.js';

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
      h('button', { class: 'btn small', onclick: logout }, 'Log out'),
      h('button', { class: 'btn small danger-text', onclick: () => deleteAccountFlow(nav) }, 'Delete account')),
    content);
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
    root.replaceChildren(authView(nav));
    return;
  }
  if (!loggedIn) return nav('#/login');

  const edit = hash.match(/^#\/edit\/([A-Za-z0-9]+)$/);
  const body = h('div', { class: 'content' });
  root.replaceChildren(shell(body));
  if (edit) {
    const stop = renderEditor(body, edit[1], nav);
    leave = stop;
  } else {
    renderDashboard(body, nav);
  }
}

window.addEventListener('hashchange', render);
window.addEventListener('auth:expired', () => { toast('Your session expired. Please log in again.', 'error'); nav('#/login'); });
render();
