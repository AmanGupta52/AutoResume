import { h, toast, fill } from './ui.js';
import { api } from './api.js';
import { factories, normalize } from './model.js';
import { createStore } from './store.js';
import { targetOf, encodeTarget, strength, nextStep } from './shared/resume.js';
import { templateFor } from './shared/coach.js';
import { webTemplate } from './templates.js';
import { aiOn, loadAiState } from './aistate.js';
import { openProposal, createChat } from './tools.js';
import { downloadAndStorePdf } from './pdfActions.js';

const LAST = 6;
const STEPS = [
  ['About you', 'Recruiters need to reach you. Add the basics.'],
  ['The job you want', 'Everything the AI writes will be matched to this job.'],
  ['Your summary', 'Two to four sentences: who you are and what you are best at.'],
  ['Your latest job', 'Start with the most recent. You can add older jobs later.'],
  ['Your education', 'School, college or course. Ongoing studies count too.'],
  ['Your skills', 'List 6 to 12 skills that match the job you want.'],
  ['All set', 'Your first version is ready.'],
];
const blankJob = (e) => !e.role.trim() && !e.company.trim() && !e.description.trim();
const blankEdu = (e) => !e.institution.trim() && !e.degree.trim();

/** One question per screen. The normal editor stays available for later changes. */
export function renderGuided(root, id, nav) {
  fill(root, h('main', { class: 'page' }, h('p', { class: 'muted' }, 'Loading your resume...')));
  let cleanup = () => {};
  let closed = false;
  api.getResume(id).then(({ resume }) => { if (!closed) cleanup = build(root, normalize(resume), nav); })
    .catch((e) => { if (!closed) fill(root, h('main', { class: 'page' }, h('div', { class: 'error' }, e.message), h('a', { href: '#/resumes', class: 'link' }, 'Back to my resumes'))); });
  return () => { closed = true; cleanup(); };
}

function build(root, initial, nav) {
  let step = 0;
  const status = h('span', { class: 'status muted small', role: 'status' }, 'Saved');
  const body = h('div', { class: 'guided-body' });
  const bar = h('i', {});
  const footer = h('div', { class: 'guided-foot' });
  const store = createStore(initial, {
    onStatus: (t, bad) => { status.textContent = t; status.classList.toggle('bad', !!bad); },
    onConflict: () => toast('This resume was changed on another device. Open it in the editor to choose which copy to keep.', 'error'),
  });
  const d = () => store.data;
  const chat = createChat(store, () => 'Guided');
  const edit = () => nav('#/edit/' + store.resume.id);

  const input = (label, get, set, o = {}) => {
    const el = o.multiline ? h('textarea', { rows: o.rows || 5, maxlength: o.max, placeholder: o.placeholder || '' }) : h('input', { type: o.type || 'text', maxlength: o.max || 300, placeholder: o.placeholder || '' });
    el.value = get() || '';
    el.addEventListener('input', () => { set(el.value); store.changed(); });
    return h('label', { class: 'field' }, h('span', {}, label), el);
  };
  const aiButton = (label, onclick, disabled) => (aiOn() ? h('button', { type: 'button', class: 'btn ai block', onclick, disabled: !!disabled }, '✨ ' + label) : null);

  function goTo(n) {
    const t = targetOf(d());
    if (step === 1 && !d().jobTitle.trim() && t.title.trim()) d().jobTitle = t.title;
    step = Math.min(Math.max(n, 0), LAST);
    const data = d();
    if (step === 3 && !data.experience.length) data.experience.push(factories.experience());
    if (step === 4 && !data.education.length) data.education.push(factories.education());
    if (step === LAST) {
      data.experience = data.experience.filter((e) => !blankJob(e));
      data.education = data.education.filter((e) => !blankEdu(e));
      const t2 = targetOf(data);
      const [name] = templateFor(t2.title || data.jobTitle, t2.description);
      if (data.templateId === 'Modern Professional' && name !== data.templateId) data.templateId = name;
    }
    store.changed();
    paint();
  }

  function stepBody() {
    const data = d();
    switch (step) {
      case 0: return [input('Full name', () => data.fullName, (v) => { data.fullName = v; }, { max: 120 }), input('Email', () => data.email, (v) => { data.email = v; }, { type: 'email', max: 254 }),
        input('Phone', () => data.phone, (v) => { data.phone = v; }, { type: 'tel', max: 40 }), input('Location', () => data.location, (v) => { data.location = v; }, { placeholder: 'City, Country', max: 160 })];
      case 1: {
        const t = () => targetOf(data);
        const set = (patch) => { data.atsFeedback = encodeTarget({ ...t(), ...patch }); };
        return [input('Job title', () => t().title, (v) => set({ title: v }), { placeholder: 'e.g. Marketing Manager', max: 160 }),
          input('Job description (optional)', () => t().description, (v) => set({ description: v }), { multiline: true, rows: 7, max: 2500, placeholder: 'Paste the job post to get matching keywords and skills' })];
      }
      case 2: return [input('Professional summary', () => data.summary, (v) => { data.summary = v; }, { multiline: true, rows: 6, max: 3000 }),
        aiButton('Write it for me', () => openProposal(store, { kind: 'SUMMARY' }, { repaint: paint }))];
      case 3: {
        const e = data.experience[0];
        return [input('Job title', () => e.role, (v) => { e.role = v; }, { max: 160 }), input('Company', () => e.company, (v) => { e.company = v; }, { max: 160 }),
          h('div', { class: 'two' }, input('Start', () => e.startDate, (v) => { e.startDate = v; }, { placeholder: 'Jun 2022', max: 40 }), e.isCurrent ? null : input('End', () => e.endDate, (v) => { e.endDate = v; }, { placeholder: 'Aug 2024', max: 40 })),
          h('label', { class: 'check' }, h('input', { type: 'checkbox', checked: e.isCurrent, onchange: (ev) => { e.isCurrent = ev.target.checked; store.changed(); paint(); } }), h('span', {}, 'I currently work here')),
          input('What did you achieve? (one per line)', () => e.description, (v) => { e.description = v; }, { multiline: true, rows: 5, max: 4000, placeholder: 'Start with a verb and add a number: Cut report time by 40%' }),
          aiButton('Suggest bullets', () => (e.role.trim() ? openProposal(store, { kind: 'BULLETS', index: 0 }, { repaint: paint }) : toast('Add the job title for this job first.')))];
      }
      case 4: {
        const e = data.education[0];
        return [input('School', () => e.institution, (v) => { e.institution = v; }, { max: 200 }), input('Degree / course', () => e.degree, (v) => { e.degree = v; }, { max: 200 }),
          h('div', { class: 'two' }, input('From', () => e.startYear, (v) => { e.startYear = v; }, { max: 20 }), input('To', () => e.endYear, (v) => { e.endYear = v; }, { max: 20 }))];
      }
      case 5: {
        const chips = h('div', { class: 'chips' });
        const field = h('input', { type: 'text', maxlength: 200, placeholder: 'Type a skill and press Enter (commas add several)' });
        const paintChips = () => fill(chips, ...data.skills.map((s, i) => h('span', { class: 'chip' }, s.name, h('button', { type: 'button', 'aria-label': `Remove ${s.name}`, onclick: () => { data.skills.splice(i, 1); store.changed(); paintChips(); } }, '✕'))));
        const add = () => {
          const have = new Set(data.skills.map((s) => s.name.toLowerCase()));
          for (const n of field.value.split(',').map((x) => x.trim().slice(0, 80)).filter(Boolean)) if (!have.has(n.toLowerCase())) { data.skills.push({ ...factories.skills(), name: n }); have.add(n.toLowerCase()); }
          field.value = ''; store.changed(); paintChips();
        };
        field.addEventListener('keydown', (ev) => { if (ev.key === 'Enter') { ev.preventDefault(); add(); } });
        paintChips();
        return [chips, h('div', { class: 'add-row' }, field, h('button', { type: 'button', class: 'btn', onclick: add }, 'Add')),
          aiButton('Suggest skills for my target job', () => (!data.jobTitle.trim() && !targetOf(data).title.trim() ? toast('Add your job title or target job first.') : openProposal(store, { kind: 'SKILLS' }, { repaint: paint })))];
      }
      default: {
        const t = targetOf(data);
        const [name, why] = templateFor(t.title || data.jobTitle, t.description);
        const s = strength(data); const n = nextStep(data);
        return [h('div', { class: 'card' }, h('h2', {}, `Resume strength ${s}%`), h('div', { class: 'meter' }, h('i', { style: `width:${s}%` })), h('p', { class: 'muted' }, n ? n.label : 'Looking complete. Check the preview and the writing check.')),
          h('div', { class: 'card' }, h('h2', {}, `Template: ${webTemplate(data.templateId)}`), h('p', { class: 'muted' }, webTemplate(data.templateId) === name ? `Chosen for your job. ${why}` : 'You can change it in the Design tab.')),
          h('div', { class: 'actions' }, h('button', { class: 'btn primary', type: 'button', onclick: edit }, 'Open the editor'),
            h('button', { class: 'btn', type: 'button', onclick: async (ev) => { const b = ev.currentTarget; b.disabled = true; try { await downloadAndStorePdf(store.resume); } finally { b.disabled = false; } } }, 'Download PDF'))];
      }
    }
  }

  function paint() {
    bar.style.width = ((step + 1) / (LAST + 1)) * 100 + '%';
    fill(body, h('h1', {}, STEPS[step][0]), h('p', { class: 'muted' }, STEPS[step][1]), ...stepBody());
    fill(footer, 
      step > 0 ? h('button', { class: 'btn', type: 'button', onclick: () => goTo(step - 1) }, 'Back') : null,
      step < LAST ? h('button', { class: 'btn primary grow', type: 'button', onclick: () => goTo(step + 1) }, step === LAST - 1 ? 'Finish' : 'Next') : null);
    window.scrollTo(0, 0);
  }

  fill(root, h('main', { class: 'page guided' },
    h('div', { class: 'guided-top' }, h('button', { class: 'btn small', type: 'button', onclick: () => (step === 0 ? edit() : goTo(step - 1)) }, '←'), h('span', { class: 'muted' }, 'Step'), h('b', { class: 'step-n' }), status, h('span', { class: 'spacer' }),
      h('span', { class: 'ask-slot' }), h('button', { class: 'btn small', type: 'button', onclick: edit }, 'Skip setup')),
    h('div', { class: 'meter' }, bar), body, footer));
  const stepN = root.querySelector('.step-n');
  const paintBase = paint;
  paint = () => { stepN.textContent = `${step + 1} of ${LAST + 1}`; paintBase(); };
  paint();
  loadAiState().then(() => {
    if (aiOn()) root.querySelector('.ask-slot').replaceChildren(h('button', { class: 'btn small ai', type: 'button', onclick: () => chat.open() }, '✨ Ask AI'));
    paint();
  });
  return () => store.destroy();
}
