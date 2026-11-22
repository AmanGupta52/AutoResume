// Offline coaching: free, instant, and the base of every suggestion card. AI runs only when a card is tapped.
// Port of the Android ResumeCoach.suggestions / templateFor.
import { bulletLines, targetOf, targetIsSet, yearsOfExperience, nextStep } from './resume.js';
import { analyzeText } from './ats.js';
import { resumeToPlainText } from './resume.js';
import { analyzeWriting } from './writing.js';

const GENERIC_TITLES = new Set(['developer', 'engineer', 'manager', 'student', 'fresher', 'worker', 'employee', 'professional', 'designer', 'analyst', 'intern', 'executive', 'consultant', 'assistant', 'officer']);

/** Template name and a one-line reason, chosen from the job. Corporate roles get the plain ATS-safe one. */
export function templateFor(title, description) {
  const t = `${title || ''} ${String(description || '').slice(0, 300)}`.toLowerCase();
  // each word must start at a word boundary, so "intern" does not match "international"
  const has = (...w) => w.some((x) => new RegExp(`\\b${x}`).test(t));
  if (has('professor', 'researcher', 'phd', 'lecturer', 'academic', 'research scientist', 'postdoc')) return ['Academic CV', 'Education and research come first, as academic readers expect.'];
  if (has('director', 'vice president', 'vp\\b', 'chief', 'head of', 'executive', 'ceo\\b', 'cfo\\b', 'cto\\b')) return ['Executive Premium', 'A calm serif look that suits senior roles.'];
  if (has('intern\\b', 'interns\\b', 'internship', 'fresher', 'graduate', 'trainee', 'student', 'entry.level')) return ['Compact Two-Column', 'Education first and skills on the side, good when experience is short.'];
  if (has('developer', 'software', 'programmer', 'devops', 'data scientist', 'data engineer', 'machine learning', 'engineer')) return ['Developer Resume', 'Shows tech skills clearly for engineering roles.'];
  if (has('designer', 'creative', 'marketing', 'brand', 'content', 'social media', 'video', 'photograph', 'artist')) return ['Premium Creative', 'A colour header that suits creative and marketing roles.'];
  if (has('accountant', 'finance', 'bank', 'audit', 'legal', 'lawyer', 'insurance', 'government', 'hr\\b', 'human resources', 'operations', 'admin', 'analyst', 'manager', 'sales', 'consultant', 'coordinator')) return ['Minimal ATS', 'Plain single column that applicant systems read most reliably.'];
  return ['Modern Professional', 'A clean all-round layout.'];
}

/**
 * Up to 3 suggestion cards for an editor tab (0 Basics ... 8 Check).
 * action.type: 'ai' {req:{kind,index,tweak}} | 'tip' {text} | 'goTab' {tab} | 'setTitle' {title} | 'useTemplate' {name, why} | 'editTarget'
 * `currentTemplate` is the template shown now (already mapped to one of the 10 names).
 */
export function suggestions(tab, d, aiOn, currentTemplate = '') {
  const out = [];
  const target = targetOf(d);
  const lc = (s) => String(s || '').toLowerCase();
  const ai = (label, req) => { if (aiOn) out.push({ label, action: { type: 'ai', req } }); };
  const tip = (label, text) => out.push({ label, action: { type: 'tip', text } });
  const exp = d.experience || [];
  const years = yearsOfExperience(d);

  if (tab === 0) {
    const title = String(d.jobTitle || '').trim();
    if (!title) tip('Add your job title', 'Type the job you want in the Job title field.');
    else if (!title.includes(' ') && GENERIC_TITLES.has(lc(title))) tip('Your job title is too general. Add a speciality', 'For example: "Backend Developer" instead of "Developer".');
    if (target.title && title && lc(target.title) !== lc(title) && !lc(target.title).split(' ').some((w) => w.length > 3 && lc(title).includes(w))) {
      out.push({ label: `Use "${target.title}" as your title`, action: { type: 'setTitle', title: target.title } });
    }
    if (!String(d.linkedinUrl || '').trim()) tip('Add your LinkedIn link', 'Paste your LinkedIn profile link in the LinkedIn field.');
    if (!targetIsSet(target)) out.push({ label: 'Set the job you are applying for', action: { type: 'editTarget' } });
  } else if (tab === 1) {
    const s = String(d.summary || '');
    if (!s.trim()) ai('Write my summary', { kind: 'SUMMARY' });
    else {
      if (s.length > 450) ai('Make the summary shorter', { kind: 'SUMMARY', tweak: 'SHORTER' });
      if (years != null && !/\d+\+?\s*(years|yrs)/i.test(s)) ai('Add your years of experience', { kind: 'SUMMARY', tweak: 'YEARS' });
      ai('Make it stronger', { kind: 'SUMMARY', tweak: 'STRONGER' });
    }
    if (!targetIsSet(target)) out.push({ label: 'Set a target job so the summary fits it', action: { type: 'editTarget' } });
  } else if (tab === 2) {
    if (!exp.length) tip('Add your latest job first', 'Tap "Add a job" below.');
    const noNumbers = exp.map((e, i) => [i, bulletLines(e.description).filter((b) => !/\d/.test(b)).length]);
    const total = noNumbers.reduce((s, x) => s + x[1], 0);
    const firstWeak = noNumbers.find((x) => x[1] > 0);
    if (firstWeak && total > 0) ai(`${total} bullet${total === 1 ? ' has' : 's have'} no numbers. Add results`, { kind: 'BULLETS', index: firstWeak[0], tweak: 'NUMBERS' });
    const empty = exp.findIndex((e) => String(e.description || '').length < 40 && String(e.role || '').trim());
    if (empty >= 0) ai(`Write bullets for job ${empty + 1}`, { kind: 'BULLETS', index: empty });
    if (exp.length && !out.length) ai('Make job 1 sound stronger', { kind: 'BULLETS', index: 0, tweak: 'STRONGER' });
  } else if (tab === 3) {
    if (!(d.education || []).length) tip('Add your education, even if you are still studying', 'Tap "Add education" below.');
  } else if (tab === 4) {
    if ((d.skills || []).length < 6) ai('Add 6 skills that match your target job', { kind: 'SKILLS' });
    else if (String(target.description).trim()) {
      const missing = analyzeText(resumeToPlainText(d), target.description, []).missingKeywords;
      if (missing.length) ai(`Job needs: ${missing.slice(0, 3).join(', ')}. Find matching skills`, { kind: 'SKILLS' });
    }
    if (!targetIsSet(target)) out.push({ label: 'Set a target job to get matching skills', action: { type: 'editTarget' } });
  } else if (tab === 5) {
    if (!(d.projects || []).length && exp.length < 2) tip('Add one project. It helps when you have little experience', 'Tap "Add a project" below.');
  } else if (tab === 6) {
    if (!(d.languages || []).length) tip('Add the languages you speak', 'Tap "Add language" below.');
  } else if (tab === 7) {
    const [name, why] = templateFor(target.title || d.jobTitle, target.description);
    if (currentTemplate !== name) out.push({ label: `Use "${name}" for this job`, action: { type: 'useTemplate', name, why } });
  } else {
    const top = analyzeWriting(d).slice(0, 3);
    if (top.length) ai(`Fix the top ${top.length} problems`, { kind: 'ADVICE' });
  }
  if (!out.length) {
    const n = nextStep(d);
    if (n && n.tab !== tab) out.push({ label: `Next: ${n.label}`, action: { type: 'goTab', tab: n.tab } });
  }
  return out.slice(0, 3);
}
