// The helper dialogs of the editor: target job, AI preview-before-apply, job match (ATS), cover letter, Ask AI.
// Nothing the AI writes touches the resume until the person taps "Use this".
import { h, toast, toastAction, modal, withBusy, fill } from './ui.js';
import { api, ApiError } from './api.js';
import { newRef } from './model.js';
import { printHtml } from './print.js';
import { letterHtml } from './letter.js';
import { parseTarget, encodeTarget, targetOf } from './shared/resume.js';
import { analyzeResume, ATS_PRESETS } from './shared/ats.js';
import { currentText, tabForKind, TWEAKS, newNumbers, hasPlaceholders, parseSkills, offlineCoverLetter, COVER_TONES } from './shared/prompts.js';

const field = (label, input) => h('label', { class: 'field' }, h('span', {}, label), input);
const errText = (e) => (e instanceof ApiError ? e.message : 'Something went wrong. Please try again.');
const clone = (o) => JSON.parse(JSON.stringify(o));

async function copyText(text) {
  try { await navigator.clipboard.writeText(text); toast('Copied.'); } catch { toast('Could not copy. Select the text and copy it by hand.', 'error'); }
}

// ---------------------------------------------------------------- target job
export function openTargetJob(store, onSaved) {
  const t = targetOf(store.data);
  const title = h('input', { type: 'text', maxlength: 160, value: t.title, placeholder: 'e.g. Marketing Manager' });
  const desc = h('textarea', { rows: 9, maxlength: 2500, placeholder: 'Paste the job post to get matching keywords and skills' });
  desc.value = t.description;
  const save = h('button', { class: 'btn primary', type: 'button', onclick: () => {
    store.data.atsFeedback = encodeTarget({ title: title.value, description: desc.value });
    store.changed(); m.close(); if (onSaved) onSaved();
  } }, 'Save');
  const m = modal('Which job are you applying for?', h('div', {},
    h('p', { class: 'muted' }, 'Every AI feature and the job match use this. Your resume text is never changed by it.'),
    field('Job title', title), field('Job description (optional)', desc), h('div', { class: 'actions' }, save)));
  title.focus();
}

// ---------------------------------------------------------------- AI: preview before applying
/**
 * req = { kind: 'SUMMARY'|'BULLETS'|'SKILLS'|'ADVICE', index?, tweak? }.  hooks.repaint() redraws the form after a change.
 */
export function openProposal(store, req, hooks = {}) {
  const d0 = store.data;
  const before = currentText(req.kind, req.index, d0);
  const title = req.kind === 'SUMMARY' ? 'Summary'
    : req.kind === 'BULLETS' ? (() => { const e = d0.experience[req.index]; return e ? `Job ${req.index + 1}${e.role ? ' - ' + e.role : ''}` : 'Achievements'; })()
      : req.kind === 'SKILLS' ? 'Skills for your target job' : 'Fixes for the top problems';
  let request = { ...req };
  let text = '';
  let base = request.tweak ? before : '';
  let loading = false;
  let error = null;
  let ctrl = null;
  let chosen = new Set();
  let confirmed = false;

  const area = h('textarea', { rows: 9, 'aria-label': 'AI answer' });
  const chips = h('div', { class: 'chips' });
  const status = h('p', { class: 'muted small', role: 'status' });
  const guard = h('div', { class: 'guard', hidden: true });
  const errBox = h('div', { class: 'error', role: 'alert', hidden: true });
  const useBtn = h('button', { class: 'btn primary', type: 'button', onclick: apply }, 'Use this');
  const tryBtn = h('button', { class: 'btn', type: 'button', onclick: () => start(base) }, 'Try again');
  const cancelBtn = h('button', { class: 'btn', type: 'button', onclick: () => m.close() }, 'Cancel');
  const tweaks = h('div', { class: 'chips' }, (req.kind === 'SUMMARY' || req.kind === 'BULLETS') ? ['SHORTER', 'STRONGER', 'FORMAL'].map((k) =>
    h('button', { class: 'btn small', type: 'button', onclick: () => { if (loading) return; request = { ...request, tweak: k }; start(text.trim() || before); } }, TWEAKS[k].label)) : []);

  const body = h('div', {},
    before ? h('div', { class: 'before' }, h('strong', { class: 'small' }, 'Now'), h('p', { class: 'small preline' }, before)) : null,
    status, req.kind === 'SKILLS' ? chips : area, guard, errBox, tweaks,
    h('div', { class: 'actions' }, req.kind === 'ADVICE'
      ? [h('button', { class: 'btn', type: 'button', onclick: () => copyText(text) }, 'Copy'), tryBtn, cancelBtn]
      : [useBtn, tryBtn, cancelBtn]));
  const m = modal(title, body, { wide: true, onClose: () => { if (ctrl) ctrl.abort(); } });

  function paintChips() {
    fill(chips, ...parseSkills(text).map((s) => {
      const on = chosen.has(s);
      return h('button', { type: 'button', class: 'chip pick' + (on ? ' on' : ''), 'aria-pressed': String(on), onclick: () => { if (on) chosen.delete(s); else chosen.add(s); paintChips(); refresh(); } }, (on ? '✓ ' : '+ ') + s);
    }));
    if (!loading && !chips.childNodes.length) chips.append(h('span', { class: 'muted' }, 'No new skills to add.'));
  }

  /** Numbers the AI added must be confirmed; [bracket] placeholders must be filled in first. */
  function refresh() {
    const t = text.trim();
    const nums = req.kind === 'SKILLS' || req.kind === 'ADVICE' ? [] : newNumbers(t, store.data);
    const ph = req.kind === 'SKILLS' || req.kind === 'ADVICE' ? false : hasPlaceholders(t);
    fill(guard);
    if (ph) guard.append(h('p', {}, 'Replace the [bracket] placeholders with your real numbers before using this text.'));
    if (nums.length) {
      const c = h('input', { type: 'checkbox', checked: confirmed });
      c.addEventListener('change', () => { confirmed = c.checked; refresh(); });
      guard.append(h('label', { class: 'check' }, c, h('span', {}, `These numbers are not in your resume: ${nums.join(', ')}. I confirm they are true.`)));
    }
    guard.hidden = !guard.childNodes.length;
    const empty = req.kind === 'SKILLS' ? chosen.size === 0 : !t;
    useBtn.disabled = loading || empty || ph || (nums.length > 0 && !confirmed);
    tryBtn.disabled = loading;
    tweaks.querySelectorAll('button').forEach((b) => { b.disabled = loading; });
  }

  async function start(newBase) {
    if (ctrl) ctrl.abort();
    ctrl = new AbortController();
    const mine = ctrl;
    base = newBase;
    const keep = text;
    loading = true; error = null; text = ''; confirmed = false; errBox.hidden = true;
    status.textContent = 'Writing...';
    area.value = ''; chosen = new Set(); paintChips(); refresh();
    try {
      const done = await api.aiAssist({ kind: req.kind, index: req.index, tweak: request.tweak, tab: tabForKind(req.kind), base, data: store.data }, (t) => {
        if (mine.signal.aborted) return;
        text = t; area.value = t; if (req.kind === 'SKILLS') { chosen = new Set(parseSkills(t).filter((s) => !store.data.skills.some((x) => x.name.toLowerCase() === s.toLowerCase()))); paintChips(); }
      }, mine.signal);
      text = done.text; area.value = text;
      if (req.kind === 'SKILLS') { chosen = new Set(parseSkills(text).filter((s) => !store.data.skills.some((x) => x.name.toLowerCase() === s.toLowerCase()))); paintChips(); }
      status.textContent = 'Check the text, edit it if you like, then tap Use this.';
    } catch (e) {
      if (mine.signal.aborted) return;
      text = keep; area.value = keep; error = e;
      errBox.hidden = false; errBox.textContent = errText(e) + (e.data && e.data.detail ? ` (${e.data.detail})` : '');
      status.textContent = '';
    } finally {
      if (ctrl === mine) { loading = false; refresh(); }
    }
  }

  function apply() {
    if (loading) return;
    const d = store.data;
    const snapshot = clone(d);
    const t = text.trim();
    if (req.kind === 'SUMMARY') d.summary = t;
    else if (req.kind === 'BULLETS' && d.experience[req.index]) d.experience[req.index].description = t;
    else if (req.kind === 'SKILLS') {
      const have = new Set(d.skills.map((s) => s.name.toLowerCase()));
      for (const n of chosen) if (!have.has(n.toLowerCase())) { d.skills.push({ name: n, level: 'Intermediate', category: 'Technical' }); have.add(n.toLowerCase()); }
    } else return;
    store.changed();
    m.close();
    if (hooks.repaint) hooks.repaint();
    toastAction('Added to your resume', 'Undo', () => { store.setData(snapshot); if (hooks.repaint) hooks.repaint(); });
  }
  area.addEventListener('input', () => { text = area.value; refresh(); });
  refresh();
  start(base);
}

// ---------------------------------------------------------------- job match (ATS)
function meter(label, pct) {
  return h('div', { class: 'meter-row' }, h('span', { class: 'small' }, label), h('div', { class: 'meter', role: 'progressbar', 'aria-valuenow': String(pct), 'aria-valuemin': '0', 'aria-valuemax': '100' }, h('i', { style: `width:${pct}%` })), h('b', { class: 'small' }, pct + '%'));
}

export function openAts(store, { aiOn, onChanged }) {
  const d = store.data;
  const target = targetOf(d);
  const jd = h('textarea', { rows: 7, maxlength: 6000, placeholder: 'Paste the full posting, or tap an example above' });
  jd.value = target.description;
  let preset = [];
  const presets = h('div', { class: 'chips' }, ATS_PRESETS.map((p) => h('button', { class: 'btn small', type: 'button', onclick: () => { jd.value = p.description; preset = p.keywords; } }, p.label)));
  const out = h('div', { class: 'ats-out' });
  const go = h('button', { class: 'btn primary', type: 'button', onclick: analyze }, 'Analyze');

  function analyze() {
    const text = jd.value.trim();
    if (text.length < 10) { toast('Paste the job description first.', 'error'); return; }
    const r = analyzeResume(store.data, text, preset);
    store.data.atsScore = r.overallScore; store.changed(); if (onChanged) onChanged();
    const picked = new Set();
    const addBtn = h('button', { class: 'btn primary', type: 'button', disabled: true, onclick: () => tailor(picked, text) }, 'Create a tailored copy');
    const missing = h('div', { class: 'chips' }, r.missingKeywords.map((k) => h('button', { type: 'button', class: 'chip pick', 'aria-pressed': 'false', onclick: (e) => {
      const b = e.currentTarget;
      if (picked.has(k)) { picked.delete(k); b.classList.remove('on'); b.textContent = '+ ' + k; b.setAttribute('aria-pressed', 'false'); } else { picked.add(k); b.classList.add('on'); b.textContent = '✓ ' + k; b.setAttribute('aria-pressed', 'true'); }
      addBtn.disabled = picked.size === 0;
    } }, '+ ' + k)));
    const aiBox = h('div', {});
    fill(out, 
      h('div', { class: 'score' }, h('div', { class: 'big' }, String(r.overallScore)), h('div', {}, h('b', {}, 'Overall match'), h('p', { class: 'muted small' }, 'Out of 100. Keywords count most.'))),
      meter('Keyword match', r.keywordsMatchPercent), meter('Completeness', r.completenessScore), meter('Readability', r.readabilityScore), meter('Contact info', r.contactInfoScore),
      h('h3', {}, 'Keywords'),
      h('p', { class: 'small' }, 'Found in your resume'), h('div', { class: 'chips' }, r.foundKeywords.length ? r.foundKeywords.map((k) => h('span', { class: 'chip ok' }, '✓ ' + k)) : h('span', { class: 'muted' }, 'None yet.')),
      r.missingKeywords.length ? [h('p', { class: 'small' }, 'Missing - tap the ones that are TRUE for you'), missing, h('p', { class: 'muted small' }, 'Your original resume is not changed. Only add skills you genuinely have.'), addBtn] : null,
      h('h3', {}, 'What to improve'),
      r.suggestions.map((s) => h('div', { class: 'tip sev-' + s.severity.toLowerCase() }, h('b', {}, `${s.severity}: ${s.title}`), h('p', { class: 'small' }, s.instruction))),
      aiOn ? h('button', { class: 'btn', type: 'button', onclick: (e) => aiAdvice(e.currentTarget, aiBox, text) }, 'Get AI suggestions for this job') : null, aiBox);
  }

  async function tailor(picked, text) {
    const base = store.data;
    const have = new Set(base.skills.map((s) => s.name.toLowerCase()));
    const added = [...picked].filter((k) => !have.has(k.toLowerCase())).map((name) => ({ name, level: 'Intermediate', category: 'Technical' }));
    const copy = clone(base);
    copy.skills = [...base.skills, ...added]; copy.atsScore = 0;
    copy.atsFeedback = encodeTarget({ ...parseTarget(base.atsFeedback), description: parseTarget(base.atsFeedback).description || text });
    try {
      const { resume } = await api.createResume({ name: `${store.resume.name} - tailored`.slice(0, 120), data: copy, clientRef: newRef(), clientUpdatedAt: Date.now() });
      toastAction(`Created a tailored copy with ${added.length} new skill${added.length === 1 ? '' : 's'}`, 'Open', () => { m.close(); location.hash = '#/edit/' + resume.id; });
    } catch (e) { toast(e.message, 'error'); }
  }

  async function aiAdvice(btn, box, text) {
    const pre = h('div', { class: 'ai-text preline' }, 'Writing...');
    fill(box, pre);
    await withBusy(btn, 'Writing...', async () => {
      try {
        const done = await api.aiAtsAdvice({ data: store.data, jobDescription: text }, (t) => { pre.textContent = t; });
        pre.textContent = done.text;
        box.append(h('div', { class: 'actions' }, h('button', { class: 'btn small', type: 'button', onclick: () => copyText(done.text) }, 'Copy')),
          h('p', { class: 'muted small' }, 'Written from your resume and the job posting. Check every number before you use it.'));
      } catch (e) { pre.replaceWith(h('div', { class: 'error' }, errText(e))); }
    });
  }

  const m = modal('Job match (ATS check)', h('div', {},
    h('p', { class: 'muted' }, 'See how well this resume matches a job posting before you apply.'), presets, field('Job description', jd), h('div', { class: 'actions' }, go), out), { wide: true });
  if (target.description.trim().length >= 10) analyze();
}

// ---------------------------------------------------------------- cover letter
export function openCoverLetter(store, { aiOn }) {
  const d = store.data;
  const target = targetOf(d);
  const role = h('input', { type: 'text', maxlength: 160, value: target.title || d.jobTitle || '' });
  const company = h('input', { type: 'text', maxlength: 160 });
  const manager = h('input', { type: 'text', maxlength: 160, placeholder: 'Hiring Manager' });
  const tone = h('select', {}, COVER_TONES.map((t) => h('option', { value: t }, t)));
  const jd = h('textarea', { rows: 4, maxlength: 6000, placeholder: 'Paste the job post (optional)' });
  jd.value = target.description;
  const letter = h('textarea', { rows: 14, maxlength: 8000, 'aria-label': 'Your cover letter' });
  const note = h('p', { class: 'muted small', role: 'status' });
  const gen = h('button', { class: 'btn primary', type: 'button', onclick: () => withBusy(gen, 'Writing...', async () => {
    note.textContent = '';
    try {
      const r = await api.aiCoverLetter({ data: store.data, role: role.value, company: company.value, manager: manager.value, tone: tone.value, jobDescription: jd.value });
      letter.value = r.text; note.textContent = r.ai ? 'Written from your resume and the job posting. Always read and edit it before sending.' : (r.notice || '');
    } catch (e) {
      // the server is out of reach: the fill-in draft needs no server
      letter.value = offlineCoverLetter(store.data, role.value, company.value, manager.value);
      note.textContent = 'Could not reach the server, so here is a basic draft you can edit.';
    }
  }) }, aiOn ? 'Write the letter' : 'Make a draft');
  const m = modal('Cover letter', h('div', {},
    h('div', { class: 'two' }, field('Role', role), field('Company', company)), h('div', { class: 'two' }, field('Hiring manager', manager), field('Tone', tone)),
    field('Job description', jd), h('div', { class: 'actions' }, gen), note, letter,
    h('div', { class: 'actions' },
      h('button', { class: 'btn', type: 'button', onclick: () => letter.value.trim() ? copyText(letter.value) : toast('Write the letter first.') }, 'Copy'),
      h('button', { class: 'btn', type: 'button', onclick: () => letter.value.trim() ? printHtml(letterHtml(store.resume, letter.value, company.value)) : toast('Write the letter first.') }, 'Print / Save as PDF'))), { wide: true });
}

// ---------------------------------------------------------------- Ask AI (chat helper)
/** One chat per open resume; closing the dialog keeps the conversation. */
export function createChat(store, getTab) {
  let messages = [];
  let streaming = null; // partial answer, or null
  let ctrl = null;
  let error = null;

  function open() {
    const list = h('div', { class: 'chat-list', role: 'log', 'aria-live': 'polite' });
    const input = h('textarea', { rows: 2, maxlength: 2000, placeholder: 'Ask about this section, e.g. "make my second job stronger"', 'aria-label': 'Message' });
    const send = h('button', { class: 'btn primary', type: 'button', onclick: () => submit(input.value) }, 'Send');
    const stop = h('button', { class: 'btn', type: 'button', hidden: true, onclick: halt }, 'Stop');
    const clear = h('button', { class: 'btn small', type: 'button', onclick: () => { halt(); messages = []; error = null; paint(); } }, 'Clear chat');
    input.addEventListener('keydown', (e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); submit(input.value); } });

    function paint() {
      fill(list, 
        ...(messages.length || streaming !== null ? [] : [h('p', { class: 'muted' }, 'Ask me anything about your resume. I know the section you are on and the job you are applying for.')]),
        ...messages.map((x) => h('div', { class: 'msg ' + x.role }, x.content)),
        streaming !== null ? h('div', { class: 'msg assistant' }, streaming || '...') : null,
        error ? h('div', { class: 'error' }, errText(error), ' ', h('button', { class: 'link', type: 'button', onclick: retry }, 'Retry')) : null);
      list.scrollTop = list.scrollHeight;
      send.disabled = streaming !== null; stop.hidden = streaming === null;
    }
    function halt() {
      if (ctrl) ctrl.abort();
      if (streaming) messages.push({ role: 'assistant', content: streaming });
      streaming = null; paint();
    }
    function retry() {
      const last = messages[messages.length - 1];
      if (!last || last.role !== 'user') return;
      messages.pop(); submit(last.content);
    }
    async function submit(text) {
      const t = text.trim();
      if (!t || streaming !== null) return;
      input.value = ''; error = null;
      messages.push({ role: 'user', content: t });
      streaming = ''; paint();
      ctrl = new AbortController();
      const mine = ctrl;
      try {
        const done = await api.aiAsk({ tab: getTab(), data: store.data, messages: messages.slice(-8) }, (x) => { if (!mine.signal.aborted) { streaming = x; paint(); } }, mine.signal);
        if (!mine.signal.aborted) { messages.push({ role: 'assistant', content: done.text }); streaming = null; }
      } catch (e) {
        if (!mine.signal.aborted) { error = e; streaming = null; }
      }
      paint();
    }

    const m = modal('Ask AI', h('div', { class: 'chat' }, list, h('div', { class: 'chat-in' }, input, h('div', { class: 'actions' }, send, stop, clear))), { wide: true, onClose: () => { if (ctrl) ctrl.abort(); if (streaming) { if (streaming) messages.push({ role: 'assistant', content: streaming }); streaming = null; } } });
    paint();
    input.focus();
  }
  return { open };
}
