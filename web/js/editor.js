import { h, toast, debounce, uid } from './ui.js';
import { api } from './api.js';
import { factories, normalize } from './model.js';
import { printResume } from './print.js';
import { downloadAndStorePdf, isStoredPdfUrl } from './pdfActions.js';
import { renderResumeHtml, TEMPLATES, COLORS, SIZES, MARGINS, SECTION_TITLES, parseDesign, encodeDesign, webTemplate } from './templates.js';

const A4_PX = 793.7; // 210 mm at 96 dpi

/** Opens the editor for one resume. Returns a cleanup function for the router. */
export function renderEditor(root, id, nav) {
  root.replaceChildren(h('main', { class: 'page' }, h('p', { class: 'muted' }, 'Loading your resume...')));
  let cleanup = () => {};
  let closed = false;
  api.getResume(id)
    .then(({ resume }) => { if (!closed) cleanup = build(root, normalize(resume), nav); })
    .catch((e) => {
      if (closed) return;
      root.replaceChildren(h('main', { class: 'page' }, h('div', { class: 'error' }, e.message), h('p', {}, h('a', { href: '#/resumes', class: 'link' }, 'Back to my resumes'))));
    });
  return () => { closed = true; cleanup(); };
}

function build(root, initial, nav) {
  let resume = initial;
  let version = 0;      // bumps on every edit
  let saving = false;
  let queued = false;
  let retryTimer = null;
  let conflict = null;
  let destroyed = false;

  const status = h('span', { class: 'status muted small', role: 'status' }, 'Saved');
  const banner = h('div', { class: 'banner', hidden: true });
  const form = h('div', { class: 'form' });
  const frame = h('iframe', { class: 'pv-frame', title: 'Resume preview', scrolling: 'no' });
  const wrap = h('div', { class: 'pv-wrap' }, frame);
  const pane = h('div', { class: 'preview' }, wrap);
  const panes = h('div', { class: 'panes', 'data-view': 'edit' }, form, pane);

  // ---------- saving ----------
  function setStatus(text, bad) { status.textContent = text; status.classList.toggle('bad', !!bad); }

  async function save() {
    if (destroyed && !resume) return;
    if (saving) { queued = true; return; }
    saving = true;
    const mine = version;
    setStatus('Saving...');
    try {
      const stamp = Math.max(Date.now(), (resume.clientUpdatedAt || 0) + 1);
      const { resume: saved } = await api.updateResume(resume.id, { name: resume.name, data: resume.data, clientUpdatedAt: stamp });
      resume.clientUpdatedAt = saved.clientUpdatedAt;
      if (mine === version) setStatus('All changes saved'); else queued = true;
    } catch (e) {
      if (e.status === 409 && e.data && e.data.resume) { showConflict(e.data.resume); }
      else if (e.status === 0 || e.status >= 500) { setStatus('Offline. Retrying...', true); clearTimeout(retryTimer); if (!destroyed) retryTimer = setTimeout(save, 5000); }
      else { setStatus('Not saved: ' + e.message, true); }
    } finally {
      saving = false;
      if (queued && !conflict) { queued = false; if (!destroyed) saveSoon(); }
    }
  }
  const saveSoon = debounce(save, 800);

  function showConflict(server) {
    conflict = server;
    setStatus('Not saved: changed elsewhere', true);
    banner.hidden = false;
    banner.replaceChildren(
      h('span', {}, 'This resume was changed on another device or tab. '),
      h('button', { class: 'btn small', onclick: () => { resume = normalize(server); conflict = null; banner.hidden = true; paintForm(); paintPreview(); paintPdfLink(); setStatus('Loaded the latest version'); } }, 'Load the latest'),
      h('button', { class: 'btn small', onclick: () => { resume.clientUpdatedAt = server.clientUpdatedAt; conflict = null; banner.hidden = true; changed(); } }, 'Keep mine'));
  }

  function changed() {
    version += 1;
    setStatus('Unsaved changes...');
    paintPreviewSoon();
    saveSoon();
  }

  // ---------- preview ----------
  function fit() {
    const w = pane.clientWidth - 24;
    if (w <= 0) return;
    const scale = Math.min(1, w / A4_PX);
    let height = 1123;
    try { height = frame.contentDocument.documentElement.scrollHeight || height; } catch { /* not ready */ }
    frame.style.width = A4_PX + 'px';
    frame.style.height = height + 'px';
    frame.style.transform = `scale(${scale})`;
    wrap.style.width = A4_PX * scale + 'px';
    wrap.style.height = height * scale + 'px';
  }
  frame.addEventListener('load', fit);
  function paintPreview() { frame.srcdoc = renderResumeHtml(resume, 'screen'); }
  const paintPreviewSoon = debounce(paintPreview, 250);
  const ro = new ResizeObserver(fit);
  ro.observe(pane);

  // ---------- form building blocks ----------
  function text(label, get, set, o = {}) {
    const input = o.multiline
      ? h('textarea', { rows: o.rows || 4, placeholder: o.placeholder || '', maxlength: o.max })
      : h('input', { type: o.type || 'text', placeholder: o.placeholder || '', maxlength: o.max || 300, autocomplete: o.autocomplete || 'off' });
    input.value = get() || '';
    input.addEventListener('input', () => { set(input.value); changed(); });
    return h('label', { class: 'field' + (o.half ? ' half' : '') }, h('span', {}, label), input);
  }
  function select(label, options, get, set, o = {}) {
    const sel = h('select', {}, options.map(([v, t]) => h('option', { value: v, selected: v === get() }, t)));
    sel.addEventListener('change', () => { set(sel.value); changed(); if (o.repaint) paintForm(); });
    return h('label', { class: 'field half' }, h('span', {}, label), sel);
  }
  function check(label, get, set) {
    const c = h('input', { type: 'checkbox', checked: !!get() });
    c.addEventListener('change', () => { set(c.checked); changed(); });
    return h('label', { class: 'check' }, c, h('span', {}, label));
  }
  const section = (title, hint, ...children) => h('section', { class: 'card sec-card' }, h('h2', {}, title), hint ? h('p', { class: 'muted small' }, hint) : null, ...children);
  const row = (...c) => h('div', { class: 'two' }, ...c);

  function listSection(title, hint, key, label, fieldsFor, addText) {
    const list = h('div', { class: 'entries' });
    const move = (i, d) => { const a = resume.data[key]; const j = i + d; if (j < 0 || j >= a.length) return; [a[i], a[j]] = [a[j], a[i]]; paint(); changed(); };
    function paint() {
      list.replaceChildren(...resume.data[key].map((entry, i) => h('div', { class: 'entry' },
        h('div', { class: 'entry-head' }, h('strong', {}, `${label} ${i + 1}`),
          h('span', { class: 'entry-actions' },
            h('button', { type: 'button', class: 'icon', title: 'Move up', 'aria-label': 'Move up', onclick: () => move(i, -1) }, '↑'),
            h('button', { type: 'button', class: 'icon', title: 'Move down', 'aria-label': 'Move down', onclick: () => move(i, 1) }, '↓'),
            h('button', { type: 'button', class: 'icon danger', title: 'Remove', 'aria-label': 'Remove', onclick: () => { resume.data[key].splice(i, 1); paint(); changed(); } }, '✕'))),
        ...fieldsFor(entry))));
    }
    paint();
    return section(title, hint, list, h('button', { type: 'button', class: 'btn', onclick: () => { resume.data[key].push(factories[key]()); paint(); changed(); } }, addText));
  }

  function skillsSection() {
    const chips = h('div', { class: 'chips' });
    const input = h('input', { type: 'text', placeholder: 'Type a skill and press Enter (commas add several)', maxlength: 200 });
    function paint() {
      chips.replaceChildren(...resume.data.skills.map((s, i) => h('span', { class: 'chip' }, s.name,
        h('button', { type: 'button', 'aria-label': `Remove ${s.name}`, onclick: () => { resume.data.skills.splice(i, 1); paint(); changed(); } }, '✕'))));
    }
    function add() {
      const have = new Set(resume.data.skills.map((s) => s.name.toLowerCase()));
      for (const n of input.value.split(',').map((x) => x.trim().slice(0, 80)).filter(Boolean)) {
        if (!have.has(n.toLowerCase())) { resume.data.skills.push({ ...factories.skills(), name: n }); have.add(n.toLowerCase()); }
      }
      input.value = ''; paint(); changed();
    }
    input.addEventListener('keydown', (e) => { if (e.key === 'Enter') { e.preventDefault(); add(); } });
    paint();
    return section('Skills', 'List 6 to 12 skills that match the job you want.', chips,
      h('div', { class: 'add-row' }, input, h('button', { type: 'button', class: 'btn', onclick: add }, 'Add')));
  }

  function designSection() {
    const d = resume.data;
    const design = () => parseDesign(d.fontSize);
    const setDesign = (patch) => { d.fontSize = encodeDesign({ ...design(), ...patch }); };
    const order = h('ol', { class: 'order' });
    function paintOrder() {
      const cur = design().order;
      order.replaceChildren(...cur.map((k, i) => h('li', {}, SECTION_TITLES[k],
        h('span', { class: 'entry-actions' },
          h('button', { type: 'button', class: 'icon', 'aria-label': 'Move up', onclick: () => swap(i, -1) }, '↑'),
          h('button', { type: 'button', class: 'icon', 'aria-label': 'Move down', onclick: () => swap(i, 1) }, '↓')))));
    }
    function swap(i, dir) {
      const cur = design().order.slice(); const j = i + dir;
      if (j < 0 || j >= cur.length) return;
      [cur[i], cur[j]] = [cur[j], cur[i]];
      setDesign({ order: cur }); paintOrder(); changed();
    }
    paintOrder();
    const tplOptions = TEMPLATES.map((t) => [t, t]);
    const shownTpl = webTemplate(d.templateId);
    const note = d.templateId && d.templateId !== shownTpl ? h('p', { class: 'muted small' }, `"${d.templateId}" (from the mobile app) is shown with the ${shownTpl} layout on the web. Pick a template here to change it.`) : null;
    return section('Design', 'Template, colour and page settings.',
      note,
      row(select('Template', tplOptions, () => shownTpl, (v) => { d.templateId = v; }),
        select('Colour', Object.keys(COLORS).map((c) => [c, c]), () => d.colorSchemeId, (v) => { d.colorSchemeId = v; })),
      row(select('Text size', SIZES.map((s) => [s, s]), () => design().size, (v) => setDesign({ size: v })),
        select('Page margins', Object.keys(MARGINS).map((m) => [m, m]), () => d.margins, (v) => { d.margins = v; })),
      check('Tighter spacing (fit more on a page)', () => design().tight, (v) => setDesign({ tight: v })),
      h('p', { class: 'muted small' }, 'Order of sections'), order);
  }

  // ---------- the form ----------
  function paintForm() {
    const d = resume.data;
    form.replaceChildren(
      section('Basics', null,
        row(text('Full name', () => d.fullName, (v) => { d.fullName = v; }, { max: 120, autocomplete: 'name' }), text('Job title', () => d.jobTitle, (v) => { d.jobTitle = v; }, { max: 160 })),
        row(text('Email', () => d.email, (v) => { d.email = v; }, { type: 'email', max: 254 }), text('Phone', () => d.phone, (v) => { d.phone = v; }, { type: 'tel', max: 40 })),
        text('Location', () => d.location, (v) => { d.location = v; }, { placeholder: 'City, Country', max: 160 }),
        row(text('LinkedIn', () => d.linkedinUrl, (v) => { d.linkedinUrl = v; }, { max: 300 }), text('GitHub', () => d.githubUrl, (v) => { d.githubUrl = v; }, { max: 300 })),
        text('Portfolio / website', () => d.portfolioUrl, (v) => { d.portfolioUrl = v; }, { max: 300 })),
      section('Summary', 'Two to four sentences: who you are and what you are best at.',
        text('Professional summary', () => d.summary, (v) => { d.summary = v; }, { multiline: true, rows: 5, max: 3000 })),
      listSection('Experience', 'Start with your latest job. One achievement per line.', 'experience', 'Job', (e) => [
        row(text('Job title', () => e.role, (v) => { e.role = v; }, { max: 160 }), text('Company', () => e.company, (v) => { e.company = v; }, { max: 160 })),
        row(text('Start', () => e.startDate, (v) => { e.startDate = v; }, { placeholder: 'Jun 2022', max: 40 }), text('End', () => e.endDate, (v) => { e.endDate = v; }, { placeholder: 'Aug 2024', max: 40 })),
        check('I currently work here', () => e.isCurrent, (v) => { e.isCurrent = v; }),
        text('Achievements (one per line)', () => e.description, (v) => { e.description = v; }, { multiline: true, rows: 5, max: 4000, placeholder: 'Start with a verb and add a number: Cut report time by 40%' }),
      ], '+ Add a job'),
      listSection('Education', null, 'education', 'School', (e) => [
        row(text('School', () => e.institution, (v) => { e.institution = v; }, { max: 200 }), text('Degree / course', () => e.degree, (v) => { e.degree = v; }, { max: 200 })),
        row(text('From', () => e.startYear, (v) => { e.startYear = v; }, { max: 20 }), text('To', () => e.endYear, (v) => { e.endYear = v; }, { max: 20 })),
        text('Grade (optional)', () => e.grade, (v) => { e.grade = v; }, { max: 60 }),
      ], '+ Add education'),
      skillsSection(),
      listSection('Projects', null, 'projects', 'Project', (e) => [
        row(text('Title', () => e.title, (v) => { e.title = v; }, { max: 160 }), text('Tools used', () => e.techStack, (v) => { e.techStack = v; }, { max: 300 })),
        text('Description', () => e.description, (v) => { e.description = v; }, { multiline: true, rows: 3, max: 3000 }),
        row(text('Code link', () => e.githubLink, (v) => { e.githubLink = v; }, { max: 300 }), text('Live link', () => e.liveLink, (v) => { e.liveLink = v; }, { max: 300 })),
      ], '+ Add a project'),
      listSection('Certifications', null, 'certifications', 'Certificate', (e) => [
        row(text('Name', () => e.name, (v) => { e.name = v; }, { max: 200 }), text('Issuer', () => e.issuer, (v) => { e.issuer = v; }, { max: 160 })),
        text('Date', () => e.date, (v) => { e.date = v; }, { max: 40 }),
      ], '+ Add a certification'),
      listSection('Languages', null, 'languages', 'Language', (e) => [
        row(text('Language', () => e.name, (v) => { e.name = v; }, { max: 60 }),
          select('Level', ['Native', 'Fluent', 'Intermediate', 'Basic'].map((x) => [x, x]), () => e.proficiency, (v) => { e.proficiency = v; })),
      ], '+ Add a language'),
      section('Awards and interests', 'One per line.', text('Awards, publications, volunteering, hobbies', () => d.achievements, (v) => { d.achievements = v; }, { multiline: true, rows: 4, max: 4000 })),
      designSection());
  }

  // ---------- page ----------
  const nameInput = h('input', { class: 'title-input', value: resume.name, maxlength: 120, 'aria-label': 'Resume name' });
  nameInput.addEventListener('input', () => { resume.name = nameInput.value.trim() || 'Untitled resume'; changed(); });
  const viewBtn = h('button', { class: 'btn small only-mobile', onclick: () => {
    const next = panes.dataset.view === 'edit' ? 'preview' : 'edit';
    panes.dataset.view = next; viewBtn.textContent = next === 'edit' ? 'Preview' : 'Edit'; if (next === 'preview') setTimeout(fit, 0);
  } }, 'Preview');

  // the PDF: made in the browser, copied to this computer, stored in Cloudinary (MongoDB keeps the link)
  const pdfLink = h('a', { class: 'link small', target: '_blank', rel: 'noopener noreferrer', hidden: true }, 'Saved PDF');
  function paintPdfLink() {
    const ok = resume.pdf && isStoredPdfUrl(resume.pdf.url);
    pdfLink.hidden = !ok;
    if (ok) { pdfLink.href = resume.pdf.url; pdfLink.title = 'The PDF stored in your account'; }
  }
  const pdfBtn = h('button', { class: 'btn primary small', onclick: async () => {
    if (!resume.data.fullName.trim()) toast('Tip: add your name first.');
    pdfBtn.disabled = true; pdfBtn.textContent = 'Making PDF...';
    try {
      const stored = await downloadAndStorePdf(resume);
      if (stored) { resume.pdf = stored; paintPdfLink(); }
    } finally { pdfBtn.disabled = false; pdfBtn.textContent = 'Download PDF'; }
  } }, 'Download PDF');
  paintPdfLink();

  const onBeforeUnload = (e) => { if (version > 0 && (saving || status.classList.contains('bad') || status.textContent.startsWith('Unsaved'))) { e.preventDefault(); e.returnValue = ''; } };
  window.addEventListener('beforeunload', onBeforeUnload);

  root.replaceChildren(h('div', { class: 'editor' },
    h('div', { class: 'bar' },
      h('a', { href: '#/resumes', class: 'btn small' }, '← My resumes'), nameInput, status,
      viewBtn,
      pdfLink, pdfBtn,
      h('button', { class: 'btn small', title: 'Print, or Save as PDF from the print window', onclick: () => printResume(resume) }, 'Print')),
    banner, panes));
  paintForm();
  paintPreview();

  return () => {
    destroyed = true;
    window.removeEventListener('beforeunload', onBeforeUnload);
    ro.disconnect();
    clearTimeout(retryTimer);
    paintPreviewSoon.cancel();
    saveSoon.cancel();
    if (version > 0 && !conflict && !status.textContent.startsWith('All changes saved')) save(); // last save on the way out
  };
}
