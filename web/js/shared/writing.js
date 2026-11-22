// Offline wording and spelling check for resume text. Port of the Android WritingCheck.
import { bulletLines } from './resume.js';

const WEAK = [
  ['responsible for', 'Start with an action verb: Led, Managed, Built, Delivered.'],
  ['duties included', 'Describe results, not duties.'],
  ['worked on', 'Say what you did and what changed: Built, Improved, Launched.'],
  ['helped with', 'Say your actual contribution: Supported, Drafted, Ran.'],
  ['helped to', 'Say your actual contribution.'],
  ['assisted with', 'Be specific about what you owned.'],
  ['participated in', 'Say what you contributed.'],
  ['tasked with', 'Replace with the action you took.'],
  ['in charge of', 'Use Led, Owned or Managed.'],
  ['hard worker', 'Show it with a result instead of claiming it.'],
  ['team player', 'Show it with an example of teamwork.'],
  ['go-getter', 'Cliche. Replace with a measurable result.'],
  ['think outside the box', 'Cliche. Give an example of a creative solution.'],
  ['references available', 'Not needed. Remove it to save space.'],
  ['etc', 'Be specific or remove it.'],
];

const MISSPELL = {
  recieve: 'receive', seperate: 'separate', definately: 'definitely', managment: 'management', acheive: 'achieve', experiance: 'experience',
  responsability: 'responsibility', occured: 'occurred', accomodate: 'accommodate', teh: 'the', adress: 'address', enviroment: 'environment',
  sucessful: 'successful', succesful: 'successful', langauge: 'language', knowlege: 'knowledge', proffesional: 'professional',
  profesional: 'professional', tommorow: 'tomorrow', untill: 'until', developement: 'development', analyse: 'analyze', calender: 'calendar',
  comunication: 'communication', excellant: 'excellent', independant: 'independent', maintainance: 'maintenance', refered: 'referred',
};

const EMAIL_RE = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
const LEVEL = { HIGH: 0, MEDIUM: 1, LOW: 2 };
const escapeRe = (s) => s.replace(/[.*+?^${}()|[\]\\\/-]/g, '\\$&');

function checkText(section, where, text, out) {
  const lower = text.toLowerCase();
  for (const [phrase, fix] of WEAK) {
    if (new RegExp(`(?<![a-z])${escapeRe(phrase)}(?![a-z])`).test(lower)) out.push({ section, where, problem: `Weak phrase: "${phrase}".`, fix, level: 'MEDIUM' });
  }
  for (const [wrong, right] of Object.entries(MISSPELL)) {
    if (new RegExp(`(?<![a-z])${wrong}(?![a-z])`).test(lower)) out.push({ section, where, problem: `Possible misspelling: "${wrong}".`, fix: `Did you mean "${right}"?`, level: 'HIGH' });
  }
  if (/^(i|my|me)\b/i.test(text.trim())) out.push({ section, where, problem: 'Starts with a first-person pronoun.', fix: 'Resumes drop "I" and "my". Start with a verb.', level: 'MEDIUM' });
  if (text.includes('  ')) out.push({ section, where, problem: 'Double spaces.', fix: 'Use single spaces.', level: 'LOW' });
}

/** @returns {{section:string, where:string, problem:string, fix:string, level:'HIGH'|'MEDIUM'|'LOW'}[]} most serious first */
export function analyzeWriting(d) {
  const out = [];
  const email = String(d.email || '').trim();
  if (email && !EMAIL_RE.test(email)) out.push({ section: 'Basics', where: 'Email', problem: 'The email address looks invalid.', fix: 'Check for typos, for example name@example.com.', level: 'HIGH' });
  if (!email) out.push({ section: 'Basics', where: 'Email', problem: 'No email address.', fix: 'Recruiters need a way to reach you.', level: 'HIGH' });
  if (!String(d.phone || '').trim()) out.push({ section: 'Basics', where: 'Phone', problem: 'No phone number.', fix: 'Add a number where you can be reached.', level: 'MEDIUM' });

  const s = String(d.summary || '').trim();
  if (s) {
    if (s.length < 60) out.push({ section: 'Summary', where: 'Summary', problem: 'The summary is very short.', fix: 'Aim for 2-4 sentences (200-400 characters).', level: 'MEDIUM' });
    if (s.length > 600) out.push({ section: 'Summary', where: 'Summary', problem: 'The summary is long.', fix: 'Trim it to 4 sentences or fewer.', level: 'LOW' });
    checkText('Summary', 'Summary', s, out);
  } else out.push({ section: 'Summary', where: 'Summary', problem: 'No summary.', fix: 'A 2-3 sentence summary helps both recruiters and ATS.', level: 'MEDIUM' });

  (d.experience || []).forEach((e, i) => {
    const where = `Job ${i + 1}` + (String(e.role || '').trim() ? ` (${e.role})` : '');
    const bl = bulletLines(e.description);
    if (!bl.length) { out.push({ section: 'Experience', where, problem: 'No achievements listed.', fix: 'Add 3-5 bullets that show results.', level: 'HIGH' }); return; }
    const withNumbers = bl.filter((b) => /\d/.test(b)).length;
    if (withNumbers * 2 < bl.length) out.push({ section: 'Experience', where, problem: `Only ${withNumbers} of ${bl.length} bullets contain a number.`, fix: 'Add figures: %, money, team size, time saved, users.', level: 'MEDIUM' });
    const counts = new Map();
    bl.forEach((b) => { const w = b.split(' ')[0].toLowerCase(); counts.set(w, (counts.get(w) || 0) + 1); });
    for (const [w, n] of counts) if (n >= 3 && w.length > 2) out.push({ section: 'Experience', where, problem: `${n} bullets start with "${w}".`, fix: 'Vary your opening verbs.', level: 'LOW' });
    bl.forEach((b, bi) => {
      const w = `${where}, bullet ${bi + 1}`;
      if (b.length > 220) out.push({ section: 'Experience', where: w, problem: 'This bullet is very long.', fix: 'Split it or cut it to one line or two.', level: 'LOW' });
      if (b.length < 25) out.push({ section: 'Experience', where: w, problem: 'This bullet is very short.', fix: 'Say what you did and the result.', level: 'LOW' });
      if (b[0] !== b[0].toUpperCase()) out.push({ section: 'Experience', where: w, problem: 'Starts with a lowercase letter.', fix: 'Capitalise the first word.', level: 'LOW' });
      checkText('Experience', w, b, out);
    });
  });
  if (!(d.experience || []).length) out.push({ section: 'Experience', where: 'Experience', problem: 'No work experience listed.', fix: 'Add jobs, internships, volunteering or projects.', level: 'MEDIUM' });

  if ((d.skills || []).length < 5) out.push({ section: 'Skills', where: 'Skills', problem: 'Fewer than 5 skills.', fix: 'List 6-12 skills that match the jobs you want.', level: 'MEDIUM' });
  (d.projects || []).forEach((p, i) => { if (String(p.description || '').trim()) checkText('Projects', `Project ${i + 1}`, p.description, out); });

  return out.sort((a, b) => LEVEL[a.level] - LEVEL[b.level]);
}
