// Everything the AI is told. The backend builds the prompts from the resume the browser or phone sends, so the
// NVIDIA key and the prompts never leave the server. The browser also imports this file for the number guard,
// the preview titles and the offline cover letter. Facts-only rules are in every system prompt.
import { resumeText, resumeToPlainText, targetOf, yearsOfExperience, checks, EDITOR_TABS } from './resume.js';
import { analyzeWriting } from './writing.js';

export const KINDS = ['SUMMARY', 'BULLETS', 'SKILLS', 'ADVICE'];

/** Quick rewrite choices shown in the preview sheet. */
export const TWEAKS = {
  SHORTER: { label: 'Shorter', instruction: 'Make it clearly shorter and keep only the strongest facts.' },
  STRONGER: { label: 'Stronger', instruction: 'Make it stronger and more confident: sharper action verbs, clearer results. Add no new facts.' },
  FORMAL: { label: 'More formal', instruction: 'Use a more formal, professional tone.' },
  NUMBERS: { label: 'Add numbers', instruction: 'Where a number would make a point stronger but the resume does not give one, write a placeholder in square brackets, for example [X%] or [N customers]. Never write a real number that is not in the resume.' },
  YEARS: { label: 'Add years', instruction: 'Mention the total years of experience given in the context, and nothing else new.' },
};

const FOCUS = {
  Basics: 'contact details, links and the job title',
  Summary: 'the professional summary (2-4 sentences)',
  Experience: 'work achievements: action verbs, results and numbers',
  Education: 'schools, degrees and dates',
  Skills: 'the skills list only',
  Projects: 'project descriptions and tools used',
  More: 'certifications, languages, awards and interests',
  Design: 'template, fonts and layout',
  Check: 'fixing the problems found in the writing check',
  ATS: 'matching the resume to a job description',
  Guided: 'building the first version of the resume step by step',
};

export const isTab = (t) => typeof t === 'string' && t in FOCUS;

/** The context package every request carries: current tab, target job, what is missing, whole resume. */
export function contextBlock(tab, d) {
  const target = targetOf(d);
  const years = yearsOfExperience(d);
  const missing = checks(d).filter((c) => !c.done).map((c) => c.label);
  let s = `CURRENT SECTION: ${tab} (focus: ${FOCUS[tab] || 'the resume'})\n`;
  s += `TARGET JOB: ${target.title.trim() || String(d.jobTitle || '').trim() || 'not set'}\n`;
  if (target.description.trim()) s += `JOB DESCRIPTION:\n${target.description.slice(0, 2500)}\n`;
  if (years != null) s += `YEARS OF EXPERIENCE (counted from the dates): about ${years}\n`;
  s += `STILL MISSING: ${missing.length ? missing.join('; ') : 'nothing important'}\n`;
  s += `\nRESUME:\n${resumeText(d)}`;
  return s;
}

const RULES = "Use ONLY facts that appear in the resume. Never invent employers, numbers, dates, years or skills. Plain text, no markdown. Return only the requested text, with no introduction.";

export function systemPrompt(kind) {
  switch (kind) {
    case 'SUMMARY': return `You are an expert resume writer. Write a professional summary of about 3 sentences, without the pronoun 'I'. Tailor the wording to the target job when one is given. ${RULES}`;
    case 'BULLETS': return `You are a resume career coach. Write 3 to 5 achievement bullets for ONE job. Each bullet starts with '• ' and an active verb, max 22 words. Match the target job when given. If the person gave no numbers, do not invent any. ${RULES}`;
    case 'SKILLS': return 'Return ONLY a comma-separated list of 8 relevant skills for the target job. Prefer skills the resume already hints at and keywords from the job description. Do not repeat skills already listed. No numbering, no extra text.';
    case 'ADVICE': return `You are a resume coach. For each problem listed, write the exact improved text the person can paste in. Number the answers 1, 2, 3. Where a number is missing write a placeholder like [X%] for the person to fill in. ${RULES}`;
    default: throw new Error('Unknown kind');
  }
}

/** The editor tab a request belongs to. */
export const tabForKind = (kind) => ({ SUMMARY: 'Summary', BULLETS: 'Experience', SKILLS: 'Skills', ADVICE: 'Check' }[kind]);

/** The text the request starts from (what the person has now). */
export function currentText(kind, index, d) {
  if (kind === 'SUMMARY') return d.summary || '';
  if (kind === 'BULLETS') return ((d.experience || [])[index] || {}).description || '';
  return '';
}

export function userPrompt({ kind, index = -1, tweak, tab, base = '' }, d) {
  const ctx = contextBlock(tab || tabForKind(kind), d);
  let task;
  if (kind === 'SUMMARY') task = 'Task: write the professional summary.';
  else if (kind === 'BULLETS') {
    const e = (d.experience || [])[index];
    task = `Task: write the bullets for JOB ${index + 1}` + (e ? ` (${e.role} at ${e.company})` : '') + '.';
  } else if (kind === 'SKILLS') task = 'Task: suggest skills that are not in the SKILLS line yet.';
  else task = `Task: fix these problems:\n${adviceProblems(d)}`;
  const draft = String(base || '').trim() ? `\n\nCURRENT DRAFT:\n${base}` : '';
  const tw = tweak && TWEAKS[tweak] ? `\n\nRewrite instruction: ${TWEAKS[tweak].instruction}` : '';
  return `${ctx}\n\n${task}${draft}${tw}`;
}

/** The top writing problems, as the ADVICE request lists them. */
export const adviceProblems = (d) => analyzeWriting(d).slice(0, 3).map((i) => `- ${i.where}: ${i.problem} ${i.fix}`).join('\n');

export function chatSystem(tab, d) {
  return `You are the AI helper inside a resume-builder app. The person is working on the "${tab}" section, so answer about that section unless they ask about something else. ` +
    'Jobs are numbered (JOB 1, JOB 2) in the resume; use those numbers when they say "my second job". Use ONLY facts from the resume; never invent numbers, employers or skills; ' +
    'if a number is missing, ask for it or use a [bracket] placeholder. Keep answers under 150 words, plain text, no markdown. When you rewrite something, give the rewritten text first.\n\n' +
    contextBlock(tab, d);
}

export const ATS_ADVICE_SYSTEM = 'You are an ATS optimisation expert. Compare the resume with the job description and give: 1) a rewritten professional summary, 2) up to 5 concrete bullet rewrites that naturally include missing keywords. Use ONLY facts present in the resume; never invent numbers. Plain text, no markdown.';
export const atsAdviceUser = (d, jobDescription) => `${contextBlock('ATS', d)}\n\nJOB DESCRIPTION TO MATCH:\n${jobDescription}`;

export const COVER_TONES = ['Professional', 'Friendly', 'Confident'];
export const COVER_SYSTEM = 'You write honest, specific cover letters. Use ONLY facts that appear in the resume; never invent employers, numbers or skills. Plain text, no markdown, no placeholders in brackets. Return only the letter.';
export function coverUser(d, { role, company, manager, tone, jobDescription }) {
  const target = targetOf(d);
  const useRole = String(role || '').trim() || target.title.trim() || String(d.jobTitle || '').trim();
  const useJd = String(jobDescription || '').trim() || target.description;
  return `Write a cover letter for the role '${useRole}' at '${String(company || '').trim() || 'the company'}', addressed to '${String(manager || '').trim() || 'Hiring Manager'}'. ` +
    `Tone: ${COVER_TONES.includes(tone) ? tone : 'Professional'}. Length: 220-300 words, 3 short paragraphs plus sign-off.\n\nRESUME:\n${resumeToPlainText(d)}\n\nJOB DESCRIPTION:\n${useJd}`;
}

/** Fill-in-the-blanks draft used when AI is not available. Uses only facts from the resume. */
export function offlineCoverLetter(d, role, company, manager) {
  const greeting = String(manager || '').trim() ? `Dear ${manager.trim()},` : 'Dear Hiring Manager,';
  const targetRole = String(role || '').trim() || String(d.jobTitle || '').trim() || 'open';
  const at = String(company || '').trim() ? ` at ${company.trim()}` : '';
  const latest = (d.experience || []).find((e) => String(e.role || '').trim());
  const firstBullet = latest ? String(latest.description || '').split('\n').map((s) => s.trim().replace(/^[•\-*\s]+/, '')).find(Boolean) : null;
  const skills = (d.skills || []).map((s) => s.name).filter((n) => String(n || '').trim()).slice(0, 5);
  const intro = String(d.summary || '').split('.').map((s) => s.trim()).find(Boolean);

  const p1 = `I am writing to apply for the ${targetRole} position${at}. ${intro ? `${intro}.` : ''}`.trim();
  let p2 = '';
  if (latest) {
    p2 += `In my role as ${latest.role}`;
    if (String(latest.company || '').trim()) p2 += ` at ${latest.company}`;
    p2 += ', ';
    p2 += firstBullet ? `${firstBullet.charAt(0).toLowerCase()}${firstBullet.slice(1)}`.replace(/\.+$/, '') + '. ' : 'I built the experience this position requires. ';
  }
  if (skills.length) p2 += `My strengths include ${skills.join(', ')}.`;
  const p3 = 'I would welcome the chance to discuss how my background can help your team. Thank you for your time and consideration.';
  return [greeting, p1, p2.trim(), p3, `Sincerely,\n${String(d.fullName || '').trim()}`].filter((x) => x.trim()).join('\n\n');
}

// ---------------------------------------------------------------- guard
// Safety check before AI text goes into the resume: numbers the person never wrote, and leftover [placeholders].
const NUM_RE = /\d[\d,.]*%?/g;
const PLACEHOLDER_RE = /\[[^\]]{1,40}\]/;
export const hasPlaceholders = (text) => PLACEHOLDER_RE.test(String(text || ''));
const cleanNum = (s) => s.replace(/[.,]+$/, '');

/** Numbers in `proposed` that do not appear anywhere in the resume as it is now. */
export function newNumbers(proposed, d) {
  const known = new Set((resumeText(d).match(NUM_RE) || []).map(cleanNum));
  const found = (String(proposed || '').match(NUM_RE) || []).map(cleanNum).filter(Boolean);
  return [...new Set(found.filter((n) => !known.has(n)))];
}

/** Skills from a comma or line separated answer. */
export function parseSkills(text) {
  return String(text || '').split(/[,\n]/).map((s) => s.trim().replace(/^[•\-*\s]+/, '').replace(/\.+$/, '').trim()).filter((s) => s && s.length <= 40);
}

export { EDITOR_TABS };
