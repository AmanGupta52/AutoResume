// Resume layouts for the web app. Same data shape as the Android app (see backend/src/utils/resumeSchema.js).
// Everything that comes from the person is escaped, so a resume can never run script.

export const TEMPLATES = ['Modern Professional', 'Minimal ATS', 'Premium Creative', 'Developer Resume', 'Executive Premium', 'Compact Two-Column', 'Academic CV', 'Classic Serif', 'Bold Header', 'Timeline'];
export const SERIF_TEMPLATES = ['Executive Premium', 'Academic CV', 'Classic Serif'];
export const BAND_TEMPLATES = { 'Premium Creative': 0.5, 'Bold Header': 0.55 };
export const COLORS = { Blue: '#0A84FF', Green: '#30D158', Cyan: '#5AC8FA', Slate: '#8E8E93', Dark: '#1F2937' };
export const SIZES = ['Compact', 'Standard', 'Large'];
export const MARGINS = { Narrow: 10, Standard: 15, Wide: 20 };
export const SECTION_TITLES = {
  summary: 'Summary', experience: 'Experience', education: 'Education', skills: 'Skills',
  projects: 'Projects', certifications: 'Certifications', languages: 'Languages', achievements: 'Awards and interests',
};
export const DEFAULT_ORDER = Object.keys(SECTION_TITLES);

/** The mobile app has more templates; on the web they show with the closest layout. */
export function webTemplate(id) {
  const t = String(id || '');
  if (TEMPLATES.includes(t)) return t;
  if (/developer|terminal/i.test(t)) return 'Developer Resume';
  if (/creative/i.test(t)) return 'Premium Creative';
  if (/minimal|sheets|standard|ats/i.test(t)) return 'Minimal ATS';
  if (/executive|corporate/i.test(t)) return 'Executive Premium';
  if (/compact|two/i.test(t)) return 'Compact Two-Column';
  return 'Modern Professional';
}

/** "Standard;font=Lora;tight=0;order=summary,experience,..." (stored in the same field by the mobile app) */
export function parseDesign(raw) {
  const parts = String(raw || '').split(';');
  const size = SIZES.includes(parts[0]) ? parts[0] : 'Standard';
  let font = 'Default';
  let tight = false;
  let order = DEFAULT_ORDER.slice();
  for (const p of parts.slice(1)) {
    const i = p.indexOf('=');
    if (i < 0) continue;
    const k = p.slice(0, i);
    const v = p.slice(i + 1);
    if (k === 'font') font = v || 'Default';
    else if (k === 'tight') tight = v === '1';
    else if (k === 'order') {
      const wanted = v.split(',').filter((x) => DEFAULT_ORDER.includes(x));
      order = [...new Set(wanted), ...DEFAULT_ORDER.filter((x) => !wanted.includes(x))];
    }
  }
  return { size, font, tight, order };
}

export function encodeDesign(d) {
  return `${d.size};font=${d.font};tight=${d.tight ? 1 : 0};order=${d.order.join(',')}`;
}

export const esc = (s) =>
  String(s == null ? '' : s).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

/** Only http(s) links are ever made clickable. "linkedin.com/in/me" becomes https://linkedin.com/in/me. */
export function safeUrl(u) {
  const s = String(u || '').trim();
  if (!s) return '';
  if (/^[a-z][a-z0-9+.-]*:/i.test(s)) return /^https?:\/\//i.test(s) ? s : '';
  return /^[\w-]+(\.[\w-]+)+(\/\S*)?$/.test(s) ? 'https://' + s : '';
}

const lines = (t) => String(t || '').split('\n').map((s) => s.trim().replace(/^[•\-*–]\s*/, '')).filter(Boolean);
const bullets = (t) => { const l = lines(t); return l.length ? `<ul>${l.map((x) => `<li>${esc(x)}</li>`).join('')}</ul>` : ''; };
const range = (a, b, cur) => [a, cur ? 'Present' : b].filter(Boolean).join(' – ');
const link = (u, label) => { const h = safeUrl(u); return h ? `<a href="${esc(h)}">${esc(label || u)}</a>` : esc(label || u); };
const has = (s) => String(s || '').trim() !== '';

const BLOCKS = {
  summary: (d) => (has(d.summary) ? `<p>${esc(d.summary).replace(/\n/g, '<br>')}</p>` : ''),
  experience: (d) =>
    (d.experience || []).filter((e) => has(e.role) || has(e.company) || has(e.description)).map((e) =>
      `<div class="item"><div class="row"><span><b>${esc(e.role)}</b>${has(e.role) && has(e.company) ? ' · ' : ''}${esc(e.company)}</span>` +
      `<span class="meta">${esc(range(e.startDate, e.endDate, e.isCurrent))}</span></div>${bullets(e.description)}</div>`).join(''),
  education: (d) =>
    (d.education || []).filter((e) => has(e.institution) || has(e.degree)).map((e) =>
      `<div class="item"><div class="row"><span><b>${esc(e.degree)}</b>${has(e.degree) && has(e.institution) ? ' · ' : ''}${esc(e.institution)}</span>` +
      `<span class="meta">${esc(range(e.startYear, e.endYear, false))}</span></div>` +
      (has(e.grade) ? `<div class="sub">${esc(e.grade)}</div>` : '') + bullets(e.achievements) + '</div>').join(''),
  skills: (d) => {
    const groups = new Map();
    for (const s of d.skills || []) if (has(s.name)) { const c = s.category || 'Skills'; groups.set(c, [...(groups.get(c) || []), s.name]); }
    const many = groups.size > 1;
    return [...groups].map(([c, names]) => `<p class="skills">${many ? `<b>${esc(c)}:</b> ` : ''}${names.map(esc).join(' · ')}</p>`).join('');
  },
  projects: (d) =>
    (d.projects || []).filter((p) => has(p.title) || has(p.description)).map((p) =>
      `<div class="item"><div class="row"><span><b>${esc(p.title)}</b>${has(p.techStack) ? ' · ' + esc(p.techStack) : ''}</span>` +
      `<span class="meta">${[p.githubLink && link(p.githubLink, 'Code'), p.liveLink && link(p.liveLink, 'Live')].filter(Boolean).join(' · ')}</span></div>` +
      (has(p.description) ? `<div class="sub">${esc(p.description)}</div>` : '') + '</div>').join(''),
  certifications: (d) =>
    (d.certifications || []).filter((c) => has(c.name)).map((c) =>
      `<div class="item"><div class="row"><span><b>${esc(c.name)}</b>${has(c.issuer) ? ' · ' + esc(c.issuer) : ''}</span><span class="meta">${esc(c.date)}</span></div></div>`).join(''),
  languages: (d) => {
    const l = (d.languages || []).filter((x) => has(x.name));
    return l.length ? `<p class="skills">${l.map((x) => esc(x.name) + (has(x.proficiency) ? ` (${esc(x.proficiency)})` : '')).join(' · ')}</p>` : '';
  },
  achievements: (d) => (has(d.achievements) ? bullets(d.achievements) || `<p>${esc(d.achievements)}</p>` : ''),
};

function section(key, d) {
  const inner = BLOCKS[key](d);
  return inner ? `<section class="sec"><h2>${esc(SECTION_TITLES[key])}</h2>${inner}</section>` : '';
}

function contactLine(d) {
  const items = [
    has(d.email) && esc(d.email), has(d.phone) && esc(d.phone), has(d.location) && esc(d.location),
    has(d.linkedinUrl) && link(d.linkedinUrl, 'LinkedIn'), has(d.githubUrl) && link(d.githubUrl, 'GitHub'), has(d.portfolioUrl) && link(d.portfolioUrl, 'Portfolio'),
  ].filter(Boolean);
  return items.join(' <i>|</i> ');
}

export const FONT_CHOICES = ['Default', 'Inter', 'Source Sans', 'Lora'];
const FONT = { 'Source Sans': 'system-ui,"Segoe UI",Roboto,"Helvetica Neue",Arial,sans-serif', Default: 'system-ui,-apple-system,"Segoe UI",Roboto,Helvetica,Arial,sans-serif', Inter: 'system-ui,"Segoe UI",Roboto,Arial,sans-serif',
  Lora: 'Georgia,"Times New Roman",serif', Georgia: 'Georgia,"Times New Roman",serif' };

export function darken(hex, f = 0.62) {
  const v = parseInt(String(hex).replace('#', ''), 16) || 0;
  const c = (n) => Math.floor(n * f).toString(16).padStart(2, '0');
  return '#' + c((v >> 16) & 255) + c((v >> 8) & 255) + c(v & 255);
}

const BASE_CSS = `
*{box-sizing:border-box}html,body{margin:0}
body{color:#111;background:#fff;line-height:1.35;-webkit-print-color-adjust:exact;print-color-adjust:exact}
a{color:inherit;text-decoration:none}
h1{margin:0}h2{margin:0 0 4px}p{margin:0 0 4px}ul{margin:3px 0 0;padding-left:1.15em}li{margin:1px 0}
.item{margin:0 0 8px;break-inside:avoid}.row{display:flex;justify-content:space-between;gap:12px}.meta{white-space:nowrap;color:#555}.sub{color:#333}
.sec{margin:0 0 12px}.head i{font-style:normal;opacity:.5;margin:0 2px}.skills{margin:0 0 3px}
`;

const LAYOUT_CSS = {
  'Modern Professional': `
    .head{border-bottom:2px solid var(--accent);padding-bottom:8px;margin-bottom:12px}
    h1{font-size:2.1em;color:var(--accent);letter-spacing:.2px}.title{font-size:1.15em;margin:2px 0 4px}.contact{font-size:.92em;color:#333}
    h2{font-size:.95em;text-transform:uppercase;letter-spacing:1px;color:var(--accent);border-bottom:1px solid #ddd;padding-bottom:2px}`,
  'Minimal ATS': `
    body{font-family:Arial,Helvetica,sans-serif!important}
    .head{text-align:center;margin-bottom:12px}h1{font-size:1.9em}.title{margin:2px 0}.contact{font-size:.92em}
    h2{font-size:1em;text-transform:uppercase;border-bottom:1px solid #000;padding-bottom:1px}.meta{color:#000}`,
  'Executive Premium': `
    body{font-family:Georgia,"Times New Roman",serif!important}
    .head{text-align:center;border-bottom:2px solid #B8860B;padding-bottom:10px;margin-bottom:14px}
    h1{font-size:2.1em;letter-spacing:1px}.title{font-style:italic;margin:2px 0 4px;color:#444}.contact{font-size:.92em;color:#333}
    h2{font-size:1em;text-transform:uppercase;letter-spacing:1.5px;color:#7a5a08;border-bottom:1px solid #e3d3a4;padding-bottom:2px}`,
  'Compact Two-Column': `
    .head{background:var(--accent);color:#fff;padding:12px 16px;margin-bottom:12px}h1{font-size:1.9em}.title{margin:2px 0}.contact{font-size:.9em}.head a{color:#fff}
    .cols{display:grid;grid-template-columns:1.9fr 1fr;gap:18px}
    h2{font-size:.9em;text-transform:uppercase;letter-spacing:1px;color:var(--accent);border-bottom:1px solid #ddd;padding-bottom:2px}
    .side .row{display:block}.side .meta{display:block}`,
  'Premium Creative': `
    .head{background:var(--dark);color:#fff;padding:18px 20px;margin-bottom:12px;border-radius:0 0 12px 12px}h1{font-size:2.2em;color:#fff}.title{margin:2px 0 4px;opacity:.92}.contact{font-size:.9em;opacity:.95}.head a{color:#fff}
    h2{font-size:.95em;text-transform:uppercase;letter-spacing:1px;border-left:5px solid var(--accent);padding-left:8px}`,
  'Developer Resume': `
    .head{background:#0f172a;color:#f8fafc;padding:16px 20px;border-radius:8px;margin-bottom:12px}h1{font-size:2em;color:#fff}.title{color:var(--accent);margin:2px 0 4px;filter:brightness(1.35)}.contact{font-size:.9em;color:#cbd5e1}.head a{color:#cbd5e1}
    h2{font-size:.9em;text-transform:uppercase;letter-spacing:1px;color:var(--accent);border-bottom:1px solid #ddd;padding-bottom:2px}.meta,.sub{font-family:ui-monospace,Menlo,Consolas,monospace;font-size:.88em}`,
  'Academic CV': `
    body{font-family:Georgia,"Times New Roman",serif!important}
    .head{text-align:center;margin-bottom:10px}h1{font-size:1.9em}.title{font-style:italic;color:#555}.contact{font-size:.92em;color:#444}
    h2{font-variant:small-caps;font-size:1.1em;letter-spacing:.5px;border-bottom:1px solid #000;padding-bottom:2px}`,
  'Classic Serif': `
    body{font-family:Georgia,"Times New Roman",serif!important}
    .head{text-align:center;border-bottom:2px solid #111;padding-bottom:8px;margin-bottom:12px}h1{font-size:1.8em;text-transform:uppercase;letter-spacing:2px}.title{letter-spacing:1px;color:#555;margin:2px 0}.contact{font-size:.92em;color:#444}
    h2{font-size:.95em;text-transform:uppercase;letter-spacing:1.5px;border-bottom:1px solid #ccc;padding-bottom:2px}`,
  'Bold Header': `
    .head{background:var(--dark);color:#fff;padding:18px 20px;border-radius:10px;margin-bottom:12px}h1{color:#fff;font-size:2.1em}.title{opacity:.9;margin:2px 0 4px}.contact{font-size:.92em}.head a{color:#fff}
    h2{font-size:.95em;text-transform:uppercase;letter-spacing:1px;color:var(--accent)}`,
  Timeline: `
    h1{font-size:2.1em;border-left:5px solid var(--accent);padding-left:10px}.title{padding-left:15px;color:#555}.contact{padding-left:15px;margin:4px 0 10px;font-size:.92em;color:#444}
    h2{font-size:1em;color:var(--accent);border-bottom:2px solid var(--accent);display:inline-block;padding-bottom:2px}
    .item{border-left:2px solid #ddd;padding-left:10px}`,
};

/**
 * Full HTML document of a resume. mode "screen" adds page padding for the preview;
 * mode "print" uses @page margins so the browser's Save as PDF gets a clean A4 page.
 */
export function renderResumeHtml(resume, mode = 'screen') {
  const d = (resume && resume.data) || {};
  const tpl = webTemplate(d.templateId);
  const design = parseDesign(d.fontSize);
  const accent = COLORS[d.colorSchemeId] || COLORS.Blue;
  const margin = MARGINS[d.margins] || MARGINS.Standard;
  const pt = { Compact: 9.5, Standard: 10.5, Large: 11.5 }[design.size];
  const family = SERIF_TEMPLATES.includes(tpl) ? FONT.Lora : FONT[design.font] || FONT.Default;
  const gap = design.tight ? 0.8 : 1;
  const dark = darken(accent, BAND_TEMPLATES[tpl] || 0.55);

  const head = `<header class="head"><h1>${esc(d.fullName || 'Your name')}</h1>` +
    (has(d.jobTitle) ? `<div class="title">${esc(d.jobTitle)}</div>` : '') + `<div class="contact">${contactLine(d)}</div></header>`;

  let body;
  if (tpl === 'Compact Two-Column') {
    const sideKeys = ['education', 'skills', 'languages', 'certifications'];
    const main = design.order.filter((k) => !sideKeys.includes(k)).map((k) => section(k, d)).join('');
    const side = design.order.filter((k) => sideKeys.includes(k)).map((k) => section(k, d)).join('');
    body = `${head}<div class="cols"><main>${main}</main><aside class="side">${side}</aside></div>`;
  } else {
    const order = tpl === 'Academic CV' && !/order=/.test(String(d.fontSize || '')) ? ['education', 'achievements', 'experience', 'projects', 'certifications', 'skills', 'languages', 'summary'] : design.order;
    body = head + order.map((k) => section(k, d)).join('');
  }

  const page = mode === 'print'
    ? `@page{size:A4;margin:${margin}mm}`
    : `body{width:210mm;min-height:297mm;padding:${margin}mm}`;
  const title = esc((d.fullName || 'Resume') + ' - Resume');
  return `<!doctype html><html lang="en"><head><meta charset="utf-8"><title>${title}</title><style>
:root{--accent:${accent};--dark:${dark}}${BASE_CSS}${page}
body{font-family:${family};font-size:${pt}pt}
.sec{margin-bottom:${12 * gap}px}.item{margin-bottom:${8 * gap}px}
${LAYOUT_CSS[tpl]}
</style></head><body>${body}</body></html>`;
}
