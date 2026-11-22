// Keyword and structure scoring for the "Job match" (ATS) check. Port of the Android AtsAnalyzerEngine.
import { resumeToPlainText } from './resume.js';

export const ATS_PRESETS = [
  { id: 'android', label: 'Android Engineer', description: 'Seeking an Android Software Engineer experienced with Kotlin, Jetpack Compose, MVVM architecture, Coroutines, Room Database, REST APIs, Git, Unit Testing, CI/CD pipelines, and Agile delivery to build high-performance mobile apps.',
    keywords: ['Kotlin', 'Jetpack Compose', 'MVVM', 'Coroutines', 'Room', 'REST APIs', 'Git', 'Unit Testing', 'CI/CD', 'Agile', 'Android SDK', 'Performance'] },
  { id: 'fullstack', label: 'Full-Stack Dev', description: 'Looking for a Full-Stack Developer proficient in TypeScript, React, Node.js, SQL, REST APIs, Docker, AWS, Git, Automated Testing, CI/CD, and System Design to deliver scalable web applications.',
    keywords: ['TypeScript', 'React', 'Node.js', 'SQL', 'REST APIs', 'Docker', 'AWS', 'Git', 'Testing', 'CI/CD', 'System Design', 'Agile'] },
  { id: 'pm', label: 'Product Manager', description: 'Hiring a Product Manager skilled in Product Roadmap, Stakeholder Management, Agile Scrum, KPIs, User Research, A/B Testing, Prioritization, Cross-functional Leadership, Analytics, SQL, and Go-to-Market strategy.',
    keywords: ['Roadmap', 'Stakeholder Management', 'Agile', 'KPIs', 'User Research', 'A/B Testing', 'Prioritization', 'Cross-functional', 'Analytics', 'SQL', 'Go-to-Market', 'Strategy'] },
  { id: 'data', label: 'Data Analyst', description: 'Seeking a Data Analyst with expertise in SQL, Python, Tableau, Power BI, Data Visualization, ETL pipelines, Statistics, Excel, A/B Testing, Forecasting, and Stakeholder Reporting.',
    keywords: ['SQL', 'Python', 'Tableau', 'Power BI', 'Data Visualization', 'ETL', 'Statistics', 'Excel', 'A/B Testing', 'Forecasting', 'Reporting', 'Analytics'] },
  { id: 'ux', label: 'UX Designer', description: 'Looking for a UI/UX Designer experienced in Figma, Design Systems, Prototyping, User Research, Wireframing, Accessibility, Usability Testing, Interaction Design, Typography, and Developer Handoff.',
    keywords: ['Figma', 'Design Systems', 'Prototyping', 'User Research', 'Wireframing', 'Accessibility', 'Usability Testing', 'Interaction Design', 'Typography', 'Responsive Design', 'Collaboration', 'Handoff'] },
];

const DICTIONARY = ['Kotlin', 'Java', 'Python', 'TypeScript', 'JavaScript', 'Swift', 'C++', 'Go', 'Jetpack Compose', 'Android SDK', 'MVVM', 'Clean Architecture', 'Coroutines', 'Flow',
  'Room', 'SQLite', 'SQL', 'PostgreSQL', 'MySQL', 'MongoDB', 'Firebase', 'Firestore', 'React', 'Node.js', 'Next.js', 'GraphQL', 'REST APIs', 'Retrofit', 'Ktor',
  'Docker', 'Kubernetes', 'AWS', 'GCP', 'Azure', 'CI/CD', 'Git', 'GitHub', 'Unit Testing', 'JUnit', 'Robolectric', 'Automated Testing', 'TDD',
  'Agile', 'Scrum', 'Jira', 'Code Review', 'System Design', 'Microservices', 'Performance', 'Optimization', 'Security', 'Authentication', 'OAuth',
  'Roadmap', 'Stakeholder Management', 'KPIs', 'OKRs', 'User Research', 'A/B Testing', 'Prioritization', 'Cross-functional', 'Analytics', 'Go-to-Market',
  'Tableau', 'Power BI', 'Data Visualization', 'ETL', 'Statistics', 'Excel', 'Forecasting', 'Figma', 'Design Systems', 'Prototyping', 'Wireframing', 'Accessibility',
  'Usability Testing', 'Interaction Design', 'Typography', 'Responsive Design', 'Leadership', 'Communication', 'Mentoring', 'Collaboration', 'Problem Solving'];

const ACTION_VERBS = ['led', 'built', 'engineered', 'developed', 'designed', 'architected', 'implemented', 'improved', 'reduced', 'increased', 'optimized', 'launched',
  'delivered', 'managed', 'created', 'automated', 'spearheaded', 'collaborated', 'scaled', 'streamlined', 'tested'];

const STOP = new Set(['with', 'from', 'that', 'this', 'have', 'will', 'your', 'team', 'work', 'experience', 'looking', 'seeking', 'hiring', 'role', 'build', 'using',
  'strong', 'skills', 'ability', 'required', 'preferred', 'years', 'including', 'across', 'must', 'good', 'great', 'candidate', 'position', 'company', 'about']);

const escapeRe = (s) => s.replace(/[.*+?^${}()|[\]\\\/-]/g, '\\$&');

/** "Git" must not match "Digital" and "Go" must not match "Google": alphanumeric boundaries. */
export function containsKeyword(haystackLower, keyword) {
  const kw = String(keyword || '').trim().toLowerCase();
  if (!kw) return false;
  return new RegExp(`(?<![a-z0-9])${escapeRe(kw)}(?![a-z0-9])`).test(haystackLower);
}

const trimEnd = (s) => s.replace(/[.,;:]+$/, '');

export function extractKeywords(jobDescription, presetKeywords = [], extra = []) {
  const jd = String(jobDescription || '');
  const lower = jd.toLowerCase();
  const collected = new Map();
  const add = (raw) => {
    const cleaned = trimEnd(String(raw).trim());
    if (cleaned.length >= 2 && !collected.has(cleaned.toLowerCase())) collected.set(cleaned.toLowerCase(), cleaned);
  };
  presetKeywords.forEach((k) => { if (containsKeyword(lower, k)) add(k); });
  DICTIONARY.forEach((k) => { if (containsKeyword(lower, k)) add(k); });
  extra.forEach((k) => { if (containsKeyword(lower, k)) add(k); });
  if (collected.size < 6) {
    const seen = new Set();
    let n = 0;
    for (const m of jd.matchAll(/[A-Za-z][A-Za-z0-9+#./-]{2,}/g)) {
      const t = trimEnd(m[0]);
      const l = t.toLowerCase();
      if (STOP.has(l) || !(/[A-Z]/.test(t) || l.length >= 6) || seen.has(l)) continue;
      seen.add(l);
      add(t);
      if (++n >= 12) break;
    }
  }
  if (!collected.size && presetKeywords.length) presetKeywords.forEach(add);
  return [...collected.values()].slice(0, 14);
}

/** @returns {{overallScore:number, keywordsMatchPercent:number, formattingScore:number, completenessScore:number, readabilityScore:number, contactInfoScore:number, foundKeywords:string[], missingKeywords:string[], suggestions:{title:string,severity:string,instruction:string}[]}} */
export function analyzeText(resumeText, jobDescription, presetKeywords = [], extra = []) {
  const keywords = extractKeywords(jobDescription, presetKeywords, extra);
  const lower = String(resumeText || '').toLowerCase();
  const found = [];
  const missing = [];
  keywords.forEach((k) => (containsKeyword(lower, k) ? found : missing).push(k));
  const keywordMatchPercent = Math.max(0, Math.min(100, Math.floor((found.length / Math.max(keywords.length, 1)) * 100)));

  const any = (list) => list.some((w) => lower.includes(w));
  const hasExperience = any(['experience', 'engineer', 'developer', 'manager', 'analyst', 'designer', 'worked', 'company']);
  const hasEducation = any(['education', 'university', 'college', 'b.s.', 'b.a.', 'm.s.', 'degree', 'gpa', 'cgpa', 'bachelor', 'master']);
  const hasSkills = any(['skills', 'technologies', 'tools', 'proficient', 'stack']) || found.length >= 3;
  const hasContact = lower.includes('@') || /\d{3}[-.\s]?\d{3}/.test(lower) || lower.includes('linkedin') || lower.includes('github');

  const text = String(resumeText || '');
  const completeness = Math.max(0, Math.min(100, (hasExperience ? 35 : 10) + (hasEducation ? 25 : 5) + (hasSkills ? 25 : 5) + (text.trim().length >= 120 ? 15 : 5)));

  const wordCount = text.trim().split(/\s+/).filter(Boolean).length;
  const bulletLinesN = text.split('\n').filter((l) => /^\s*[•\-*]/.test(l)).length;
  const metricMatches = (text.match(/(\d+%|\$\d+|\d+x|\d+\+\s|\b\d{2,}\b)/g) || []).length;
  const actionVerbCount = ACTION_VERBS.filter((v) => new RegExp(`\\b${v}\\b`).test(lower)).length;

  const lengthPts = wordCount >= 120 ? 35 : wordCount >= 60 ? 25 : wordCount >= 25 ? 15 : 5;
  const bulletPts = bulletLinesN >= 3 ? 25 : bulletLinesN >= 1 ? 15 : 5;
  const metricPts = metricMatches >= 3 ? 25 : metricMatches >= 1 ? 15 : 0;
  const verbPts = actionVerbCount >= 3 ? 15 : actionVerbCount >= 1 ? 10 : 0;
  const readability = Math.max(0, Math.min(100, lengthPts + bulletPts + metricPts + verbPts));
  const contactInfoScore = hasContact && actionVerbCount >= 2 ? 95 : hasContact || actionVerbCount >= 2 ? 75 : 45;
  const overall = Math.max(0, Math.min(100, Math.floor(keywordMatchPercent * 0.55 + completeness * 0.2 + readability * 0.25)));

  const suggestions = [];
  if (missing.length) {
    suggestions.push({ title: `Add ${missing.length} missing job-description keywords`, severity: missing.length >= 4 ? 'High' : 'Medium',
      instruction: `Naturally weave in high-signal terms from the job description: ${missing.slice(0, 5).join(', ')}. Tap any missing keyword below to add it to your skills.` });
  }
  if (metricMatches < 2) suggestions.push({ title: 'Quantify achievements with numbers or percentages', severity: 'High',
    instruction: "ATS reviewers prioritize measurable impact. Replace generic duties with outcomes like 'Reduced API latency by 28%' or 'Supported 50k+ monthly active users'." });
  if (bulletLinesN < 2) suggestions.push({ title: 'Structure experience using concise bullet points', severity: 'Medium',
    instruction: "Start achievement lines with '•' or '-' so automated parsers and recruiters can scan your contributions quickly." });
  if (actionVerbCount < 2) suggestions.push({ title: 'Lead sentences with strong action verbs', severity: 'Medium',
    instruction: 'Begin bullets with verbs like Engineered, Architected, Led, Optimized, or Delivered instead of passive phrasing.' });
  if (wordCount < 60) suggestions.push({ title: 'Expand resume content for fuller keyword coverage', severity: 'High',
    instruction: `Your resume currently has ${wordCount} words. Add your core skills, 1-2 work experience entries, and education details (aim for 120+ words).` });
  if (!hasEducation) suggestions.push({ title: 'Include education credentials', severity: 'Low', instruction: 'Add your degree, institution, and graduation year so education filters match your profile.' });
  if (!hasContact) suggestions.push({ title: 'Add email or portfolio/GitHub handle', severity: 'Low', instruction: 'Include at least an email address or LinkedIn/GitHub link when running a full-resume ATS check.' });
  if (!suggestions.length) suggestions.push({ title: 'Strong keyword & structure alignment', severity: 'Low',
    instruction: 'Your text already covers the core job description keywords, quantified metrics, and structured formatting. Tailor your top summary line to mirror the exact role title.' });

  return { overallScore: overall, keywordsMatchPercent: keywordMatchPercent, formattingScore: readability, completenessScore: completeness, readabilityScore: readability,
    contactInfoScore, foundKeywords: found, missingKeywords: missing, suggestions };
}

/** Convenience: analyse a resume object against a job description. */
export const analyzeResume = (data, jobDescription, presetKeywords = []) => analyzeText(resumeToPlainText(data), jobDescription, presetKeywords);
