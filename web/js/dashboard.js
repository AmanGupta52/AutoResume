import { h, toast, timeAgo, modal, withBusy, fill } from './ui.js';
import { api, session } from './api.js';
import { blankData, newRef, normalize, sampleData } from './model.js';
import { downloadAndStorePdf, downloadDocx, isStoredPdfUrl } from './pdfActions.js';
import { renderResumeHtml, webTemplate } from './templates.js';
import { importLinkedInZip } from './linkedin.js';


/** A small picture of the first page. */
function thumbnail(r) {
  const f = h('iframe', { title: '', tabindex: '-1', loading: 'lazy', scrolling: 'no', class: 'thumb-frame' });
  f.srcdoc = renderResumeHtml(normalize(r), 'screen');
  return h('div', { class: 'thumb', 'aria-hidden': 'true' }, f);
}

export function renderDashboard(root, nav) {
  const list = h('div', { class: 'grid' }, h('p', { class: 'muted' }, 'Loading your resumes...'));
  const user = session.user;
  const fileInput = h('input', { type: 'file', accept: '.zip,application/zip', hidden: true });
  fileInput.addEventListener('change', async () => {
    const file = fileInput.files && fileInput.files[0];
    fileInput.value = '';
    if (!file) return;
    const imported = await importLinkedInZip(file);
    if (!imported) { toast('That file is not a LinkedIn data export (.zip with Profile.csv, Positions.csv...).', 'error'); return; }
    try {
      const { resume } = await api.createResume({ name: imported.name, data: imported.data, clientRef: newRef(), clientUpdatedAt: Date.now() });
      toast('Imported. Please check each section.');
      nav('#/edit/' + resume.id);
    } catch (e) { toast(e.message, 'error'); }
  });

  async function load() {
    try {
      const { resumes } = await api.listResumes();
      if (!resumes.length) {
        fill(list, h('div', { class: 'empty card' }, h('h2', {}, 'No resumes yet'), h('p', { class: 'muted' }, 'Create your first resume. It saves automatically.'),
          h('div', { class: 'actions center' }, h('button', { class: 'btn primary', onclick: newResume }, 'Create a resume'), h('button', { class: 'btn', onclick: () => fileInput.click() }, 'Import from LinkedIn'))));
        return;
      }
      fill(list, ...resumes.map(cardFor));
    } catch (e) {
      fill(list, h('div', { class: 'error' }, e.message, ' ', h('button', { class: 'link', onclick: load }, 'Try again')));
    }
  }

  async function createAndOpen(name, data, route) {
    try {
      const { resume } = await api.createResume({ name, data, clientRef: newRef(), clientUpdatedAt: Date.now() });
      nav(`#/${route}/${resume.id}`);
    } catch (e) { toast(e.message, 'error'); }
  }

  function newResume() {
    const nameIn = h('input', { type: 'text', maxlength: 120, value: 'My resume' });
    let mode = 'guided';
    const opt = (value, title, hint, checked) => h('label', { class: 'radio' }, h('input', { type: 'radio', name: 'mode', value, checked, onchange: () => { mode = value; } }), h('span', {}, h('b', {}, title), h('br'), h('span', { class: 'muted small' }, hint)));
    const go = h('button', { class: 'btn primary', type: 'button', onclick: () => withBusy(go, 'Creating...', async () => {
      const name = nameIn.value.trim();
      if (!name) { toast('Give the resume a name.', 'error'); return; }
      m.close();
      if (mode === 'example') await createAndOpen(name, sampleData(), 'edit');
      else await createAndOpen(name, blankData(user), mode === 'guided' ? 'guided' : 'edit');
    }) }, 'Create');
    const m = modal('New resume', h('div', {}, h('label', { class: 'field' }, h('span', {}, 'Name this resume'), nameIn),
      opt('guided', 'Step by step', 'One question at a time. Best for a first resume.', true), opt('blank', 'Blank', 'Open the editor with empty sections.', false), opt('example', 'Start from an example', 'A filled-in sample you can overwrite.', false),
      h('div', { class: 'actions' }, go)));
    nameIn.select();
  }

  async function rename(r) {
    const name = (window.prompt('Rename this resume', r.name) || '').trim();
    if (!name || name === r.name) return;
    try { await api.updateResume(r.id, { name, clientUpdatedAt: Math.max(Date.now(), (r.clientUpdatedAt || 0) + 1) }); toast('Renamed.'); load(); } catch (e) { toast(e.message, 'error'); }
  }
  async function duplicate(r) {
    try { await api.createResume({ name: `${r.name} (copy)`.slice(0, 120), data: r.data, clientRef: newRef(), clientUpdatedAt: Date.now() }); toast('Copy created.'); load(); } catch (e) { toast(e.message, 'error'); }
  }
  async function removePdf(r) {
    if (!window.confirm('Remove the saved PDF from your account? Your resume stays.')) return;
    try { await api.deletePdf(r.id); toast('Saved PDF removed.'); load(); } catch (e) { toast(e.message, 'error'); }
  }
  async function remove(r) {
    if (!window.confirm(`Delete "${r.name}"? This cannot be undone.`)) return;
    try { await api.deleteResume(r.id); toast('Deleted.'); load(); } catch (e) { toast(e.message, 'error'); }
  }

  function cardFor(r) {
    const d = r.data || {};
    return h('article', { class: 'card resume-card' },
      thumbnail(r),
      h('h2', {}, r.name),
      h('p', { class: 'muted' }, [d.fullName, d.jobTitle].filter(Boolean).join(' · ') || 'Empty resume'),
      h('p', { class: 'muted small' }, `${webTemplate(d.templateId)} · updated ${timeAgo(r.updatedAt)}`),
      r.pdf && isStoredPdfUrl(r.pdf.url)
        ? h('p', { class: 'muted small' }, `PDF saved ${timeAgo(r.pdf.uploadedAt)} · `, h('a', { class: 'link', href: r.pdf.url, target: '_blank', rel: 'noopener noreferrer' }, 'Open'), ' · ', h('button', { class: 'link', onclick: () => removePdf(r) }, 'Remove'))
        : null,
      h('div', { class: 'actions' },
        h('button', { class: 'btn primary', onclick: () => nav('#/edit/' + r.id) }, 'Open'),
        h('button', { class: 'btn', onclick: async (e) => { const b = e.currentTarget; b.disabled = true; try { await downloadAndStorePdf(normalize(r)); load(); } finally { b.disabled = false; } } }, 'PDF'),
        h('button', { class: 'btn', onclick: () => downloadDocx(normalize(r)) }, 'Word'),
        h('button', { class: 'btn', onclick: () => rename(r) }, 'Rename'),
        h('button', { class: 'btn', onclick: () => duplicate(r) }, 'Duplicate'),
        h('button', { class: 'btn danger', onclick: () => remove(r) }, 'Delete')));
  }

  fill(root, h('main', { class: 'page' },
    h('div', { class: 'page-head' }, h('div', {}, h('h1', {}, 'My resumes'), h('p', { class: 'muted' }, 'Saved to your account. Download a PDF or Word file any time.')),
      h('div', { class: 'actions' }, h('button', { class: 'btn', onclick: () => fileInput.click() }, 'Import LinkedIn'), h('button', { class: 'btn primary', onclick: newResume }, '+ New resume'))),
    fileInput, list));
  load();
}

export function deleteAccountFlow(nav) {
  const pw = window.prompt('To delete your account and ALL your resumes forever, type your password:');
  if (!pw) return;
  api.deleteAccount(pw).then(() => { session.clear(); toast('Your account was deleted.'); nav('#/register'); }).catch((e) => toast(e.message, 'error'));
}
