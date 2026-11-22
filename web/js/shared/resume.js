// Resume data helpers shared by the web app (browser) and the backend (Node).
// The shape matches the Android app's Resume (see backend/src/utils/resumeSchema.js).
// No DOM, no Node APIs: this file must run in both places.

export const factories = {
  experience: () => ({ company: '', role: '', startDate: '', endDate: '', isCurrent: false, description: '' }),
  education: () => ({ institution: '', degree: '', startYear: '', endYear: '', grade: '', achievements: '' }),
  skills: () => ({ name: '', level: 'Intermediate', category: 'Technical' }),
  projects: () => ({ title: '', techStack: '', description: '', githubLink: '', liveLink: '' }),
  certifications: () => ({ name: '', issuer: '', date: '', credentialId: '', url: '' }),
  languages: () => ({ name: '', proficiency: 'Fluent' }),
};

export function blankData(user) {
  return {
    fullName: (user && user.name) || '', jobTitle: '', phone: '', email: (user && user.email) || '', location: '',
    linkedinUrl: '', githubUrl: '', portfolioUrl: '', summary: '',
    education: [], experience: [], skills: [], projects: [], certifications: [], languages: [],
    achievements: '', templateId: 'Modern Professional', colorSchemeId: 'Blue', fontSize: 'Standard', margins: 'Standard',
    atsScore: 0, atsFeedback: '',
  };
}

/** Makes sure every field exists, so nothing ever meets undefined. */
export function normalize(resume) {
  const base = blankData(null);
  const data = { ...base, ...((resume && resume.data) || {}) };
  for (const k of Object.keys(factories)) {
    data[k] = Array.isArray(data[k]) ? data[k].map((e) => ({ ...factories[k](), ...(e && typeof e === 'object' ? e : {}) })) : [];
  }
  for (const [k, v] of Object.entries(base)) if (typeof v === 'string' && typeof data[k] !== 'string') data[k] = '';
  return { ...resume, data };
}

/** A realistic example resume, for "Start from an example". */
export function sampleData() {
  const skill = (name) => ({ name, level: 'Intermediate', category: 'Technical' });
  return {
    ...blankData(null),
    fullName: 'Alex Morgan', jobTitle: 'Marketing Manager', phone: '+1 555 0100', email: 'alex.morgan@example.com', location: 'Austin, TX',
    summary: 'Marketing manager with 6 years of experience growing B2B brands through content, lifecycle email and paid campaigns. Led a team of 5 and increased qualified leads by 42% in 12 months.',
    experience: [
      { company: 'Northwind', role: 'Marketing Manager', startDate: 'Mar 2022', endDate: '', isCurrent: true,
        description: '• Grew qualified leads 42% year over year with a new content and webinar programme.\n• Cut cost per lead by 18% by reallocating paid budget using cohort reports.\n• Managed a team of 5 and a 1.2M annual budget.' },
      { company: 'Brightside', role: 'Marketing Specialist', startDate: 'Jun 2019', endDate: 'Feb 2022', isCurrent: false,
        description: '• Launched lifecycle email flows that lifted trial-to-paid conversion by 11%.\n• Ran 30+ A/B tests on landing pages.' },
    ],
    education: [{ institution: 'University of Texas', degree: 'B.A. Communications', startYear: '2015', endYear: '2019', grade: '', achievements: '' }],
    skills: ['SEO', 'Content strategy', 'Google Analytics', 'HubSpot', 'Email marketing', 'A/B testing', 'Team leadership'].map(skill),
    languages: [{ name: 'English', proficiency: 'Native' }],
  };
}

const has = (s) => String(s == null ? '' : s).trim() !== '';

/** Lines of a description, without bullet characters. */
export function bulletLines(desc) {
  return String(desc || '').split('\n').map((l) => l.trim().replace(/^[•\-*–\s]+/, '').trim()).filter(Boolean);
}

// ---------------------------------------------------------------- target job
// The job the person is applying for is kept inside `atsFeedback`, exactly like the Android app does:
// "T1" + US + title + US + description   (US = unit separator, never typed by a person)
const US = '\u001F';

export function parseTarget(raw) {
  const s = String(raw || '');
  if (!s.startsWith('T1' + US)) return { title: '', description: '' };
  const p = s.split(US);
  return { title: p[1] || '', description: p[2] || '' };
}
export const encodeTarget = (t) => (has(t && t.title) || has(t && t.description) ? `T1${US}${String(t.title || '').trim()}${US}${String(t.description || '').trim()}` : '');
export const targetOf = (d) => parseTarget(d && d.atsFeedback);
export const targetIsSet = (t) => has(t.title) || has(t.description);

// ---------------------------------------------------------------- text of a resume
const dates = (start, end, cur) => [start, cur ? 'Present' : end].filter(has).join(' - ');

/** The resume as plain text for the AI (same layout the Android app sends). */
export function resumeText(d) {
  const out = [];
  out.push(`NAME: ${d.fullName} | TITLE: ${d.jobTitle}`);
  const contact = [d.email, d.phone, d.location].filter(has);
  if (contact.length) out.push(`CONTACT: ${contact.join(' | ')}`);
  out.push(`SUMMARY: ${has(d.summary) ? d.summary : '(empty)'}`);
  (d.experience || []).forEach((e, i) => out.push(`JOB ${i + 1}: ${e.role} at ${e.company} (${dates(e.startDate, e.endDate, e.isCurrent)})\n${has(e.description) ? e.description : '(no achievements yet)'}`));
  (d.education || []).forEach((e, i) => out.push(`EDUCATION ${i + 1}: ${e.degree}, ${e.institution} (${e.startYear}-${e.endYear}) ${e.grade}`));
  if ((d.skills || []).length) out.push(`SKILLS: ${d.skills.map((s) => s.name).join(', ')}`);
  (d.projects || []).forEach((p, i) => out.push(`PROJECT ${i + 1}: ${p.title} (${p.techStack}): ${p.description}`));
  if ((d.certifications || []).length) out.push(`CERTIFICATIONS: ${d.certifications.map((c) => c.name).join(', ')}`);
  if ((d.languages || []).length) out.push(`LANGUAGES: ${d.languages.map((l) => l.name).join(', ')}`);
  if (has(d.achievements)) out.push(`OTHER: ${d.achievements}`);
  return out.join('\n').trim().slice(0, 6000);
}

/** The resume as plain text for the ATS check and the cover letter. */
export function resumeToPlainText(d) {
  const parts = [];
  if (has(d.fullName) || has(d.jobTitle)) parts.push(`${d.fullName} — ${d.jobTitle}`.replace(/^[\s—]+|[\s—]+$/g, ''));
  const contacts = [d.email, d.phone, d.location].filter(has);
  if (contacts.length) parts.push(contacts.join(' | '));
  if (has(d.summary)) parts.push(`Summary:\n${d.summary}`);
  if ((d.experience || []).length) {
    parts.push('Work Experience:\n' + d.experience.map((e) => `${e.role} at ${e.company} (${dates(e.startDate, e.endDate, e.isCurrent)})\n${e.description}`.trim()).join('\n\n'));
  }
  if ((d.education || []).length) {
    parts.push('Education:\n' + d.education.map((e) => `${e.degree}, ${e.institution} (${e.startYear}-${e.endYear}) ${e.grade}\n${e.achievements}`.trim()).join('\n'));
  }
  if ((d.skills || []).length) parts.push('Skills:\n' + d.skills.map((s) => s.name).join(', '));
  if ((d.projects || []).length) parts.push('Projects:\n' + d.projects.map((p) => `${p.title} (${p.techStack}): ${p.description}`).join('\n'));
  return parts.join('\n\n').trim();
}

// ---------------------------------------------------------------- coaching basics
export function yearsOfExperience(d, nowYear = new Date().getFullYear()) {
  if (!(d.experience || []).length) return null;
  const years = d.experience.map((e) => { const m = /(19|20)\d{2}/.exec(e.startDate || ''); return m ? Number(m[0]) : null; }).filter((y) => y != null);
  if (!years.length) return null;
  return Math.max(0, nowYear - Math.min(...years));
}

/** What a finished resume needs. `tab` is the editor tab (see EDITOR_TABS) where it is fixed. */
export function checks(d) {
  const exp = d.experience || [];
  return [
    { done: has(d.fullName) && has(d.jobTitle), label: 'Add your name and job title', tab: 0 },
    { done: has(d.email) && has(d.phone), label: 'Add email and phone', tab: 0 },
    { done: String(d.summary || '').length >= 60, label: 'Write a summary of at least two sentences', tab: 1 },
    { done: exp.length > 0, label: 'Add your work experience', tab: 2 },
    { done: exp.every((e) => String(e.description || '').length >= 40), label: 'Describe achievements for every job', tab: 2 },
    { done: (d.education || []).length > 0, label: 'Add your education', tab: 3 },
    { done: (d.skills || []).length >= 5, label: 'List at least 5 skills', tab: 4 },
    { done: has(d.location), label: 'Add your location', tab: 0 },
  ];
}

export function strength(d) {
  const c = checks(d);
  return Math.floor((c.filter((x) => x.done).length * 100) / c.length);
}
export const nextStep = (d) => checks(d).find((c) => !c.done) || null;

/** Names of the editor tabs, in order. The AI is told which one the person is on. */
export const EDITOR_TABS = ['Basics', 'Summary', 'Experience', 'Education', 'Skills', 'Projects', 'More', 'Design', 'Check'];
