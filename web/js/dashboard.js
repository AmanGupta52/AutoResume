import { h, toast, timeAgo } from './ui.js';
import { api, session } from './api.js';
import { blankData, newRef, normalize } from './model.js';
import { printResume } from './print.js';

export function renderDashboard(root, nav) {
  const list = h('div', { class: 'grid' }, h('p', { class: 'muted' }, 'Loading your resumes...'));
  const user = session.user;

  async function load() {
    try {
      const { resumes } = await api.listResumes();
      if (!resumes.length) {
        list.replaceChildren(h('div', { class: 'empty card' }, h('h2', {}, 'No resumes yet'), h('p', { class: 'muted' }, 'Create your first resume. It saves automatically.'),
          h('button', { class: 'btn primary', onclick: create }, 'Create a resume')));
        return;
      }
      list.replaceChildren(...resumes.map(cardFor));
    } catch (e) {
      list.replaceChildren(h('div', { class: 'error' }, e.message, ' ', h('button', { class: 'link', onclick: load }, 'Try again')));
    }
  }

  async function create() {
    const name = (window.prompt('Name this resume', 'My resume') || '').trim();
    if (!name) return;
    try {
      const { resume } = await api.createResume({ name, data: blankData(user), clientRef: newRef(), clientUpdatedAt: Date.now() });
      nav('#/edit/' + resume.id);
    } catch (e) { toast(e.message, 'error'); }
  }

  async function duplicate(r) {
    try {
      await api.createResume({ name: `${r.name} (copy)`.slice(0, 120), data: r.data, clientRef: newRef(), clientUpdatedAt: Date.now() });
      toast('Copy created.'); load();
    } catch (e) { toast(e.message, 'error'); }
  }

  async function remove(r) {
    if (!window.confirm(`Delete "${r.name}"? This cannot be undone.`)) return;
    try { await api.deleteResume(r.id); toast('Deleted.'); load(); } catch (e) { toast(e.message, 'error'); }
  }

  function cardFor(r) {
    const d = r.data || {};
    return h('article', { class: 'card resume-card' },
      h('h2', {}, r.name),
      h('p', { class: 'muted' }, [d.fullName, d.jobTitle].filter(Boolean).join(' · ') || 'Empty resume'),
      h('p', { class: 'muted small' }, `${d.templateId || 'Modern Professional'} · updated ${timeAgo(r.updatedAt)}`),
      h('div', { class: 'actions' },
        h('button', { class: 'btn primary', onclick: () => nav('#/edit/' + r.id) }, 'Open'),
        h('button', { class: 'btn', onclick: () => printResume(normalize(r)) }, 'Download PDF'),
        h('button', { class: 'btn', onclick: () => duplicate(r) }, 'Duplicate'),
        h('button', { class: 'btn danger', onclick: () => remove(r) }, 'Delete')));
  }

  root.replaceChildren(h('main', { class: 'page' },
    h('div', { class: 'page-head' }, h('div', {}, h('h1', {}, 'My resumes'), h('p', { class: 'muted' }, 'Saved to your account. Download a PDF any time.')),
      h('button', { class: 'btn primary', onclick: create }, '+ New resume')),
    list));
  load();
}

export function deleteAccountFlow(nav) {
  const pw = window.prompt('To delete your account and ALL your resumes forever, type your password:');
  if (!pw) return;
  api.deleteAccount(pw).then(() => { session.clear(); toast('Your account was deleted.'); nav('#/register'); }).catch((e) => toast(e.message, 'error'));
}
