'use strict';

/**
 * The resume document is the same shape the Android app keeps in Room (see data/ResumeEntity.kt).
 * Only known fields are kept, strings are capped and lists are limited, so a client can never store
 * arbitrary data or huge payloads. The profile photo is NOT synced (it stays on the phone).
 */
const S = (max) => ({ type: 'string', max });

const ENTRY = {
  experience: { company: S(160), role: S(160), startDate: S(40), endDate: S(40), isCurrent: { type: 'boolean' }, description: S(4000) },
  education: { institution: S(200), degree: S(200), startYear: S(20), endYear: S(20), grade: S(60), achievements: S(1500) },
  skills: { name: S(80), level: S(30), category: S(40) },
  projects: { title: S(160), techStack: S(300), description: S(3000), githubLink: S(300), liveLink: S(300) },
  certifications: { name: S(200), issuer: S(160), date: S(40), credentialId: S(100), url: S(300) },
  languages: { name: S(60), proficiency: S(30) },
};

const TOP = {
  fullName: S(120), jobTitle: S(160), phone: S(40), email: S(254), location: S(160),
  linkedinUrl: S(300), githubUrl: S(300), portfolioUrl: S(300),
  summary: S(3000), achievements: S(4000),
  templateId: S(60), colorSchemeId: S(30), fontSize: S(200), margins: S(30),
  atsFeedback: S(4000),
};

const MAX_ENTRIES = 40;

function cleanString(v, max) {
  if (typeof v !== 'string') return '';
  // keep line breaks and tabs, drop other control characters
  return v.replace(/[\u0000-\u0008\u000B\u000C\u000E-\u001F\u007F]/g, '').slice(0, max);
}

function cleanEntry(raw, shape) {
  const src = raw && typeof raw === 'object' && !Array.isArray(raw) ? raw : {};
  const out = {};
  for (const [key, rule] of Object.entries(shape)) {
    out[key] = rule.type === 'boolean' ? src[key] === true : cleanString(src[key], rule.max);
  }
  return out;
}

function sanitizeResumeData(input) {
  const src = input && typeof input === 'object' && !Array.isArray(input) ? input : {};
  const out = {};
  for (const [key, rule] of Object.entries(TOP)) out[key] = cleanString(src[key], rule.max);
  for (const [key, shape] of Object.entries(ENTRY)) {
    const list = Array.isArray(src[key]) ? src[key].slice(0, MAX_ENTRIES) : [];
    out[key] = list.map((e) => cleanEntry(e, shape));
  }
  const score = Number(src.atsScore);
  out.atsScore = Number.isFinite(score) ? Math.max(0, Math.min(100, Math.round(score))) : 0;
  return out;
}

function cleanTitle(v) {
  return cleanString(typeof v === 'string' ? v : '', 120).replace(/\s+/g, ' ').trim();
}

module.exports = { sanitizeResumeData, cleanTitle, MAX_ENTRIES };
