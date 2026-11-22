import { h, toast, toastAction, debounce, fill } from './ui.js';
import { api } from './api.js';
import { factories, normalize } from './model.js';
import { createStore } from './store.js';
import { printResume } from './print.js';
import { downloadAndStorePdf, downloadDocx, isStoredPdfUrl } from './pdfActions.js';
import { renderResumeHtml, TEMPLATES, COLORS, SIZES, MARGINS, SECTION_TITLES, FONT_CHOICES, parseDesign, encodeDesign, webTemplate } from './templates.js';
import { EDITOR_TABS, strength, nextStep, targetOf, targetIsSet } from './shared/resume.js';
import { suggestions } from './shared/coach.js';
import { analyzeWriting } from './shared/writing.js';
import { aiOn, loadAiState } from './aistate.js';
import { openTargetJob, openProposal, openAts, openCoverLetter, createChat } from './tools.js';

const A4_PX = 793.7; // 210 mm at 96 dpi
const PAGE_PX = 1122.5;
const clone = (o) => JSON.parse(JSON.stringify(o));

/** Opens the editor for one resume. Returns a cleanup function for the router. */
export function renderEditor(root, id, nav) {
  fill(root, h('main', { class: 'page' }, h('p', { class: 'muted' }, 'Loading your resume...')));
  let cleanup = () => {};
  let closed = false;
  api.getResume(id)
    .then(({ resume }) => { if (!closed) cleanup = build(root, normalize(resume), nav); })
    .catch((e) => {
      if (closed) return;
      fill(root, h('main', { class: 'page' }, h('div', { class: 'error' }, e.message), h('p', {}, h('a', { href: '#/resumes', class: 'link' }, 'Back to my resumes'))));
    });
  return () => { closed = true; cleanup(); };
}

/** How many A4 pages the resume fills, measured in a hidden frame. */
export function measurePages(resume) {
  return new Promise((resolve) => {
    const f = document.createElement('iframe');
    f.setAttribute('aria-hidden', 'true');
    f.style.cssText = `position:fixed;left:-9999px;top:0;width:${A4_PX}px;height:1200px;border:0;visibility:hidden`;
    const finish = (n) => { f.remove(); resolve(n); };
    f.addEventListener('load', () => { try { finish(Math.max(1, Math.ceil((f.contentDocument.documentElement.scrollHeight - 3) / PAGE_PX))); } catch { finish(1); } });
    f.srcdoc = renderResumeHtml(resume, 'screen');
    document.body.append(f);
    setTimeout(() => finish(1), 4000);
  });
}

function build(root, initial, nav) {
  let tab = 0;
  let pages = 1;
  const status = h('span', { class: 'status muted small', role: 'status' }, 'Saved');
  const banner = h('div', { class: 'banner', hidden: true });
  const form = h('div', { class: 'form' });
  const frame = h('iframe', { class: 'pv-frame', title: 'Resume preview', scrolling: 'no' });
  const wrap = h('div', { class: 'pv-wrap' }, frame);
  const pageInfo = h('span', { class: 'small muted' }, '1 page');
  const fitBtn = h('button', { class: 'btn small', type: 'button', hidden: true, onclick: fitOnePage }, 'Fit to 1 page');
  const pane = h('div', { class: 'preview' }, h('div', { class: 'pv-bar' }, pageInfo, fitBtn), wrap);
  const panes = h('div', { class: 'panes', 'data-view': 'edit' }, form, pane);

  const store = createStore(initial, {
    onStatus: (text, bad) => { status.textContent = text; status.classList.toggle('bad', !!bad); },
    onConflict: (server) => {
      banner.hidden = false;
      fill(banner, h('span', {}, 'This resume was changed on another device or tab. '),
        h('button', { class: 'btn small', onclick: () => { store.loadLatest(); banner.hidden = true; nameInput.value = store.resume.name; paintAll(); } }, 'Load the latest'),
        h('button', { class: 'btn small', onclick: () => { store.keepMine(); banner.hidden = true; changed(); } }, 'Keep mine'));
    },
  });
  const d = () => store.data;
  const chat = createChat(store, () => EDITOR_TABS[tab] || 'Basics');

  function changed() { store.changed(); paintPreviewSoon(); paintAssistSoon(); }
  // the strength meter and suggestion cards follow what is typed (only that card is redrawn, so typing keeps its focus)
  const paintAssistSoon = debounce(() => { const old = form.querySelector('.assist'); if (old) old.replaceWith(assistant()); }, 500);
  const repaint = () => { paintAll(); };
  function paintAll() { paintForm(); paintPreview(); paintPdfLink(); }

  // ---------- preview ----------
  function fit() {
    const w = pane.clientWidth - 24;
    if (w <= 0) return;
    const scale = Math.min(1, w / A4_PX);
    let height = 1123;
    try { height = frame.contentDocument.documentElement.scrollHeight || height; } catch { /* not ready */ }
    pages = Math.max(1, Math.ceil((height - 3) / PAGE_PX));
    pageInfo.textContent = pages === 1 ? '1 page' : `${pages} pages`;
    fitBtn.hidden = pages <= 1;
    frame.style.width = A4_PX + 'px';
    frame.style.height = height + 'px';
    frame.style.transform = `scale(${scale})`;
    wrap.style.width = A4_PX * scale + 'px';
    wrap.style.height = height * scale + 'px';
  }
  frame.addEventListener('load', fit);
  function paintPreview() { frame.srcdoc = renderResumeHtml(store.resume, 'screen'); }
  const paintPreviewSoon = debounce(paintPreview, 250);
  const ro = new ResizeObserver(fit);
  ro.observe(pane);

  /** Tighter spacing, then compact text, then narrow margins: stop as soon as it fits on one page. */
  async function fitOnePage() {
    const snapshot = clone(d());
    const design = () => parseDesign(d().fontSize);
    const steps = [() => { d().fontSize = encodeDesign({ ...design(), tight: true }); }, () => { d().fontSize = encodeDesign({ ...design(), size: 'Compact' }); }, () => { d().margins = 'Narrow'; }];
    fitBtn.disabled = true;
    let n = await measurePages(store.resume);
    for (const step of steps) { if (n <= 1) break; step(); n = await measurePages(store.resume); }
    fitBtn.disabled = false;
    changed(); paintForm();
    if (n <= 1) toastAction('Fitted on one page.', 'Undo', () => { store.setData(snapshot); paintAll(); });
    else toastAction('Still more than one page. Shorten the text or remove old jobs.', 'Undo', () => { store.setData(snapshot); paintAll(); });
  }

  // ---------- form building blocks ----------
  function text(label, get, set, o = {}) {
    const input = o.multiline
      ? h('textarea', { rows: o.rows || 4, placeholder: o.placeholder || '', maxlength: o.max })
      : h('input', { type: o.type || 'text', placeholder: o.placeholder || '', maxlength: o.max || 300, autocomplete: o.autocomplete || 'off' });
    input.value = get() || '';
    input.addEventListener('input', () => { set(input.value); changed(); });
    return h('label', { class: 'field' }, h('span', {}, label), input);
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
  const aiBtn = (label, onclick, disabled) => (aiOn() ? h('button', { type: 'button', class: 'btn ai', onclick, disabled: !!disabled }, '✨ ' + label) : null);

  /** Removes something and offers Undo (the whole resume is restored). */
  function removeWithUndo(label, change) {
    const snapshot = clone(d());
    change();
    changed(); paintForm();
    toastAction(`${label} removed`, 'Undo', () => { store.setData(snapshot); paintAll(); });
  }

  function listSection(title, hint, key, label, fieldsFor, addText, extra) {
    const list = h('div', { class: 'entries' });
    const move = (i, dir) => { const a = d()[key]; const j = i + dir; if (j < 0 || j >= a.length) return; [a[i], a[j]] = [a[j], a[i]]; paint(); changed(); };
    function paint() {
      fill(list, ...d()[key].map((entry, i) => h('div', { class: 'entry' },
        h('div', { class: 'entry-head' }, h('strong', {}, `${label} ${i + 1}`),
          h('span', { class: 'entry-actions' },
            h('button', { type: 'button', class: 'icon', title: 'Move up', 'aria-label': 'Move up', onclick: () => move(i, -1) }, '↑'),
            h('button', { type: 'button', class: 'icon', title: 'Move down', 'aria-label': 'Move down', onclick: () => move(i, 1) }, '↓'),
            h('button', { type: 'button', class: 'icon danger', title: 'Remove', 'aria-label': 'Remove', onclick: () => removeWithUndo(label, () => d()[key].splice(i, 1)) }, '✕'))),
        ...fieldsFor(entry, i))));
    }
    paint();
    return section(title, hint, list, h('button', { type: 'button', class: 'btn', onclick: () => { d()[key].push(factories[key]()); paint(); changed(); } }, addText), extra ? extra() : null);
  }

  function skillsSection() {
    const chips = h('div', { class: 'chips' });
    const input = h('input', { type: 'text', placeholder: 'Type a skill and press Enter (commas add several)', maxlength: 200 });
    function paint() {
      fill(chips, ...d().skills.map((s, i) => h('span', { class: 'chip' }, s.name,
        h('button', { type: 'button', 'aria-label': `Remove ${s.name}`, onclick: () => removeWithUndo('Skill', () => d().skills.splice(i, 1)) }, '✕'))));
    }
    function add() {
      const have = new Set(d().skills.map((s) => s.name.toLowerCase()));
      for (const n of input.value.split(',').map((x) => x.trim().slice(0, 80)).filter(Boolean)) {
        if (!have.has(n.toLowerCase())) { d().skills.push({ ...factories.skills(), name: n }); have.add(n.toLowerCase()); }
      }
      input.value = ''; paint(); changed();
    }
    input.addEventListener('keydown', (e) => { if (e.key === 'Enter') { e.preventDefault(); add(); } });
    paint();
    const needJob = !d().jobTitle.trim() && !targetIsSet(targetOf(d()));
    return section('Skills', 'List 6 to 12 skills that match the job you want.', chips,
      h('div', { class: 'add-row' }, input, h('button', { type: 'button', class: 'btn', onclick: add }, 'Add')),
      aiBtn('Suggest skills for my target job', () => needJob ? toast('Add your job title or target job first.') : openProposal(store, { kind: 'SKILLS' }, { repaint })));
  }

  function designSection() {
    const data = d();
    const design = () => parseDesign(data.fontSize);
    const setDesign = (patch) => { data.fontSize = encodeDesign({ ...design(), ...patch }); };
    const order = h('ol', { class: 'order' });
    function paintOrder() {
      fill(order, ...design().order.map((k, i) => h('li', {}, SECTION_TITLES[k],
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
    const shown = webTemplate(data.templateId);
    const thumbs = h('div', { class: 'tpl-grid' }, TEMPLATES.map((name) => {
      const sample = { ...store.resume, data: { ...data, templateId: name } };
      const tf = h('iframe', { title: name, tabindex: '-1', loading: 'lazy', class: 'tpl-frame', scrolling: 'no' });
      tf.srcdoc = renderResumeHtml(sample, 'screen');
      return h('button', { type: 'button', class: 'tpl' + (name === shown ? ' on' : ''), 'aria-pressed': String(name === shown), onclick: () => { data.templateId = name; changed(); paintForm(); } },
        h('span', { class: 'tpl-thumb' }, tf), h('span', { class: 'small' }, name));
    }));
    return [
      section('Template', 'Pick a look. Your content stays the same.', thumbs),
      section('Design', 'Colour and page settings.',
        row(select('Colour', Object.keys(COLORS).map((c) => [c, c]), () => data.colorSchemeId, (v) => { data.colorSchemeId = v; }),
          select('Font', FONT_CHOICES.map((f) => [f, f]), () => design().font, (v) => setDesign({ font: v }))),
        row(select('Text size', SIZES.map((s) => [s, s]), () => design().size, (v) => setDesign({ size: v })),
          select('Page margins', Object.keys(MARGINS).map((m) => [m, m]), () => data.margins, (v) => { data.margins = v; })),
        check('Tighter spacing (fit more on a page)', () => design().tight, (v) => setDesign({ tight: v })),
        h('p', { class: 'muted small' }, 'Order of sections'), order,
        h('button', { type: 'button', class: 'btn', onclick: fitOnePage }, 'Fit to 1 page')),
    ];
  }

  function checkSection() {
    const issues = analyzeWriting(d());
    const [score, next] = [strength(d()), nextStep(d())];
    const LEVEL = { HIGH: 'High', MEDIUM: 'Medium', LOW: 'Low' };
    return [
      section('Resume strength', null,
        h('div', { class: 'meter-row' }, h('div', { class: 'meter' }, h('i', { style: `width:${score}%` })), h('b', {}, score + '%')),
        h('p', { class: 'muted' }, next ? next.label : 'Looking complete. Check the preview and the job match.'),
        h('div', { class: 'actions' },
          h('button', { class: 'btn', type: 'button', onclick: () => openAts(store, { aiOn: aiOn(), onChanged: () => {} }) }, 'Job match (ATS)'),
          aiBtn('Fix the top problems', () => openProposal(store, { kind: 'ADVICE' }, { repaint }), !issues.length))),
      section('Writing check', 'Weak phrases, missing numbers, misspellings and more.',
        issues.length ? h('div', {}, issues.map((i) => h('div', { class: 'tip sev-' + i.level.toLowerCase() }, h('b', {}, `${LEVEL[i.level]}: ${i.where}`), h('p', { class: 'small' }, i.problem + ' ' + i.fix))))
          : h('p', {}, '✓ No problems found.')),
    ];
  }

  function assistant() {
    const data = d();
    const score = strength(data);
    const next = nextStep(data);
    const target = targetOf(data);
    const cards = suggestions(tab, data, aiOn(), webTemplate(data.templateId)).filter((c) => c.action.type !== 'editTarget'); // the button above does that
    const act = (a) => {
      if (a.type === 'ai') openProposal(store, a.req, { repaint });
      else if (a.type === 'tip') toast(a.text);
      else if (a.type === 'goTab') setTab(a.tab);
      else if (a.type === 'setTitle') { data.jobTitle = a.title; changed(); paintForm(); }
      else if (a.type === 'useTemplate') { data.templateId = a.name; changed(); paintForm(); toast(a.why); }
      else if (a.type === 'editTarget') openTargetJob(store, () => paintForm());
    };
    return h('section', { class: 'card assist' },
      h('div', { class: 'meter-row' }, h('span', { class: 'small' }, 'Resume strength'), h('div', { class: 'meter' }, h('i', { style: `width:${score}%` })), h('b', { class: 'small' }, score + '%')),
      next ? h('button', { type: 'button', class: 'link small', onclick: () => setTab(next.tab) }, 'Next step: ' + next.label) : h('span', { class: 'small muted' }, '✓ Looking complete.'),
      h('div', { class: 'actions' },
        h('button', { type: 'button', class: 'btn small', onclick: () => openTargetJob(store, () => paintForm()) }, targetIsSet(target) ? `Applying for: ${target.title || 'job set'}` : 'Set the job you are applying for')),
      cards.length ? h('div', { class: 'cards' }, cards.map((c) => h('button', { type: 'button', class: 'sugg', onclick: () => act(c.action) }, (c.action.type === 'ai' ? '✨ ' : '') + c.label))) : null);
  }

  // ---------- the form (one tab at a time, like the phone app) ----------
  function tabBody() {
    const data = d();
    switch (tab) {
      case 0: return [section('Basics', null,
        row(text('Full name', () => data.fullName, (v) => { data.fullName = v; }, { max: 120, autocomplete: 'name' }), text('Job title', () => data.jobTitle, (v) => { data.jobTitle = v; }, { max: 160 })),
        row(text('Email', () => data.email, (v) => { data.email = v; }, { type: 'email', max: 254 }), text('Phone', () => data.phone, (v) => { data.phone = v; }, { type: 'tel', max: 40 })),
        text('Location', () => data.location, (v) => { data.location = v; }, { placeholder: 'City, Country', max: 160 }),
        row(text('LinkedIn', () => data.linkedinUrl, (v) => { data.linkedinUrl = v; }, { max: 300 }), text('GitHub', () => data.githubUrl, (v) => { data.githubUrl = v; }, { max: 300 })),
        text('Portfolio / website', () => data.portfolioUrl, (v) => { data.portfolioUrl = v; }, { max: 300 }))];
      case 1: return [section('Summary', 'Two to four sentences: who you are and what you are best at.',
        text('Professional summary', () => data.summary, (v) => { data.summary = v; }, { multiline: true, rows: 6, max: 3000 }),
        aiBtn(data.summary.trim() ? 'Improve my summary' : 'Write it for me', () => openProposal(store, { kind: 'SUMMARY', tweak: data.summary.trim() ? 'STRONGER' : undefined }, { repaint })))];
      case 2: return [listSection('Experience', 'Start with your latest job. One achievement per line.', 'experience', 'Job', (e, i) => [
        row(text('Job title', () => e.role, (v) => { e.role = v; }, { max: 160 }), text('Company', () => e.company, (v) => { e.company = v; }, { max: 160 })),
        row(text('Start', () => e.startDate, (v) => { e.startDate = v; }, { placeholder: 'Jun 2022', max: 40 }), text('End', () => e.endDate, (v) => { e.endDate = v; }, { placeholder: 'Aug 2024', max: 40 })),
        check('I currently work here', () => e.isCurrent, (v) => { e.isCurrent = v; }),
        text('Achievements (one per line)', () => e.description, (v) => { e.description = v; }, { multiline: true, rows: 5, max: 4000, placeholder: 'Start with a verb and add a number: Cut report time by 40%' }),
        aiBtn('Suggest bullets', () => (e.role.trim() ? openProposal(store, { kind: 'BULLETS', index: i }, { repaint }) : toast('Add the job title for this job first.'))),
      ], '+ Add a job')];
      case 3: return [listSection('Education', null, 'education', 'School', (e) => [
        row(text('School', () => e.institution, (v) => { e.institution = v; }, { max: 200 }), text('Degree / course', () => e.degree, (v) => { e.degree = v; }, { max: 200 })),
        row(text('From', () => e.startYear, (v) => { e.startYear = v; }, { max: 20 }), text('To', () => e.endYear, (v) => { e.endYear = v; }, { max: 20 })),
        text('Grade (optional)', () => e.grade, (v) => { e.grade = v; }, { max: 60 }),
      ], '+ Add education')];
      case 4: return [skillsSection()];
      case 5: return [listSection('Projects', null, 'projects', 'Project', (e) => [
        row(text('Title', () => e.title, (v) => { e.title = v; }, { max: 160 }), text('Tools used', () => e.techStack, (v) => { e.techStack = v; }, { max: 300 })),
        text('Description', () => e.description, (v) => { e.description = v; }, { multiline: true, rows: 3, max: 3000 }),
        row(text('Code link', () => e.githubLink, (v) => { e.githubLink = v; }, { max: 300 }), text('Live link', () => e.liveLink, (v) => { e.liveLink = v; }, { max: 300 })),
      ], '+ Add a project')];
      case 6: return [
        listSection('Certifications', null, 'certifications', 'Certificate', (e) => [
          row(text('Name', () => e.name, (v) => { e.name = v; }, { max: 200 }), text('Issuer', () => e.issuer, (v) => { e.issuer = v; }, { max: 160 })),
          text('Date', () => e.date, (v) => { e.date = v; }, { max: 40 })], '+ Add a certification'),
        listSection('Languages', null, 'languages', 'Language', (e) => [
          row(text('Language', () => e.name, (v) => { e.name = v; }, { max: 60 }),
            select('Level', ['Native', 'Fluent', 'Intermediate', 'Basic'].map((x) => [x, x]), () => e.proficiency, (v) => { e.proficiency = v; }))], '+ Add a language'),
        section('Awards and interests', 'One per line.', text('Awards, publications, volunteering, hobbies', () => data.achievements, (v) => { data.achievements = v; }, { multiline: true, rows: 4, max: 4000 })),
      ];
      case 7: return designSection();
      default: return checkSection();
    }
  }

  function setTab(n) { tab = n; paintForm(); form.scrollTop = 0; }
  const tabBar = h('div', { class: 'tabs', role: 'tablist' });
  function paintTabs() {
    fill(tabBar, ...EDITOR_TABS.map((name, i) => h('button', { type: 'button', role: 'tab', class: 'tab' + (i === tab ? ' on' : ''), 'aria-selected': String(i === tab), onclick: () => setTab(i) }, name)));
  }
  function paintForm() {
    const keep = form.scrollTop;
    paintTabs();
    fill(form, tabBar, assistant(), ...tabBody());
    form.scrollTop = keep;
  }

  // ---------- page ----------
  const nameInput = h('input', { class: 'title-input', value: store.resume.name, maxlength: 120, 'aria-label': 'Resume name' });
  nameInput.addEventListener('input', () => { store.resume.name = nameInput.value.trim() || 'Untitled resume'; changed(); });
  const viewBtn = h('button', { class: 'btn small only-mobile', onclick: () => {
    const next = panes.dataset.view === 'edit' ? 'preview' : 'edit';
    panes.dataset.view = next; viewBtn.textContent = next === 'edit' ? 'Preview' : 'Edit'; if (next === 'preview') setTimeout(fit, 0);
  } }, 'Preview');

  const pdfLink = h('a', { class: 'link small', target: '_blank', rel: 'noopener noreferrer', hidden: true }, 'Saved PDF');
  function paintPdfLink() {
    const r = store.resume;
    const ok = r.pdf && isStoredPdfUrl(r.pdf.url);
    pdfLink.hidden = !ok;
    if (ok) { pdfLink.href = r.pdf.url; pdfLink.title = 'The PDF stored in your account'; }
  }
  const pdfBtn = h('button', { class: 'btn primary small', onclick: async () => {
    if (!d().fullName.trim()) toast('Tip: add your name first.');
    pdfBtn.disabled = true; pdfBtn.textContent = 'Making PDF...';
    try {
      const stored = await downloadAndStorePdf(store.resume);
      if (stored) { store.resume.pdf = stored; paintPdfLink(); }
    } finally { pdfBtn.disabled = false; pdfBtn.textContent = 'Download PDF'; }
  } }, 'Download PDF');

  const onBeforeUnload = (e) => { if (store.unsaved || status.classList.contains('bad')) { e.preventDefault(); e.returnValue = ''; } };
  window.addEventListener('beforeunload', onBeforeUnload);

  const aiButtons = () => (aiOn() ? [h('button', { class: 'btn small ai', onclick: () => chat.open() }, '✨ Ask AI')] : []);
  const bar = h('div', { class: 'bar' });
  function paintBar() {
    fill(bar, 
      h('a', { href: '#/resumes', class: 'btn small' }, '← My resumes'), nameInput, status, viewBtn,
      ...aiButtons(),
      h('button', { class: 'btn small', onclick: () => openAts(store, { aiOn: aiOn(), onChanged: () => paintForm() }) }, 'Job match'),
      h('button', { class: 'btn small', onclick: () => openCoverLetter(store, { aiOn: aiOn() }) }, 'Cover letter'),
      pdfLink, pdfBtn,
      h('button', { class: 'btn small', onclick: () => downloadDocx(store.resume) }, 'Word'),
      h('button', { class: 'btn small', title: 'Print, or Save as PDF from the print window', onclick: () => printResume(store.resume) }, 'Print'));
  }
  fill(root, h('div', { class: 'editor' }, bar, banner, panes));
  paintBar();
  paintAll();
  loadAiState().then(() => { paintBar(); paintForm(); });

  return () => {
    window.removeEventListener('beforeunload', onBeforeUnload);
    ro.disconnect();
    paintPreviewSoon.cancel();
    paintAssistSoon.cancel();
    store.destroy();
  };
}
