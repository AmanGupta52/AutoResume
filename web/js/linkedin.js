// Builds a resume from LinkedIn's "Get a copy of your data" zip (Profile, Positions, Education, Skills...).
// Port of the Android LinkedInImport. Reads the zip in the browser (DecompressionStream), nothing is uploaded.
import { blankData, factories } from './shared/resume.js';

/** Small CSV parser that handles quotes, escaped quotes and line breaks inside quoted cells. */
export function parseCsv(text) {
  const rows = [];
  let row = [];
  let cell = '';
  let q = false;
  const s = String(text).replace(/^\uFEFF/, '');
  for (let i = 0; i < s.length; i++) {
    const c = s[i];
    if (q) {
      if (c === '"') { if (s[i + 1] === '"') { cell += '"'; i++; } else q = false; } else cell += c;
    } else if (c === '"') q = true;
    else if (c === ',') { row.push(cell); cell = ''; }
    else if (c === '\r') { /* skip */ }
    else if (c === '\n') { row.push(cell); cell = ''; rows.push(row); row = []; }
    else cell += c;
  }
  if (cell || row.length) { row.push(cell); rows.push(row); }
  return rows.filter((r) => r.some((c) => c.trim() !== ''));
}

function table(text) {
  if (!text) return [];
  const rows = parseCsv(text);
  if (rows.length < 2) return [];
  const header = rows[0].map((h) => h.trim());
  return rows.slice(1).map((r) => Object.fromEntries(header.map((h, i) => [h, (r[i] || '').trim()])));
}

const bulletize = (desc) => String(desc || '').split('\n').map((l) => l.trim()).filter(Boolean).map((l) => '• ' + l.replace(/^[•\-\s]+/, '')).join('\n');
const WANTED = new Set(['profile.csv', 'positions.csv', 'education.csv', 'skills.csv', 'certifications.csv', 'languages.csv']);

/** Reads the wanted CSV files out of a zip (stored or deflated). Throws if it is not a zip. */
export async function readZipCsv(buffer) {
  const v = new DataView(buffer);
  const bytes = new Uint8Array(buffer);
  let eocd = -1;
  for (let i = buffer.byteLength - 22; i >= Math.max(0, buffer.byteLength - 65557); i--) if (v.getUint32(i, true) === 0x06054b50) { eocd = i; break; }
  if (eocd < 0) throw new Error('not a zip');
  const count = v.getUint16(eocd + 10, true);
  let p = v.getUint32(eocd + 16, true);
  const files = {};
  const dec = new TextDecoder('utf-8');
  for (let n = 0; n < count; n++) {
    if (v.getUint32(p, true) !== 0x02014b50) throw new Error('bad zip');
    const method = v.getUint16(p + 10, true);
    const csize = v.getUint32(p + 20, true);
    const nameLen = v.getUint16(p + 28, true);
    const extraLen = v.getUint16(p + 30, true);
    const commentLen = v.getUint16(p + 32, true);
    const local = v.getUint32(p + 42, true);
    const name = dec.decode(bytes.subarray(p + 46, p + 46 + nameLen));
    p += 46 + nameLen + extraLen + commentLen;
    const base = name.split('/').pop().toLowerCase();
    if (name.endsWith('/') || !WANTED.has(base)) continue;
    const start = local + 30 + v.getUint16(local + 26, true) + v.getUint16(local + 28, true);
    const raw = bytes.subarray(start, start + csize);
    let data;
    if (method === 0) data = raw;
    else if (method === 8) data = new Uint8Array(await new Response(new Blob([raw]).stream().pipeThrough(new DecompressionStream('deflate-raw'))).arrayBuffer());
    else continue;
    if (data.length > 5 * 1024 * 1024) continue;
    files[base] = dec.decode(data);
  }
  return files;
}

/** @returns {{name:string, data:object}|null} null when the zip has none of LinkedIn's files */
export function resumeFromLinkedIn(files) {
  if (!files || !Object.keys(files).length) return null;
  const profile = table(files['profile.csv'])[0] || {};
  const data = {
    ...blankData(null),
    fullName: [profile['First Name'], profile['Last Name']].filter(Boolean).join(' '),
    jobTitle: profile['Headline'] || '', summary: profile['Summary'] || '', location: profile['Geo Location'] || '',
    experience: table(files['positions.csv']).map((r) => ({ ...factories.experience(), company: r['Company Name'] || '', role: r['Title'] || '', startDate: r['Started On'] || '', endDate: r['Finished On'] || '', isCurrent: !r['Finished On'], description: bulletize(r['Description']) })),
    education: table(files['education.csv']).map((r) => ({ ...factories.education(), institution: r['School Name'] || '', degree: r['Degree Name'] || '', startYear: (r['Start Date'] || '').slice(-4), endYear: (r['End Date'] || '').slice(-4), achievements: r['Notes'] || '' })),
    skills: table(files['skills.csv']).filter((r) => r['Name']).map((r) => ({ ...factories.skills(), name: r['Name'] })),
    certifications: table(files['certifications.csv']).map((r) => ({ ...factories.certifications(), name: r['Name'] || '', issuer: r['Authority'] || '', date: r['Started On'] || '', url: r['Url'] || '' })),
    languages: table(files['languages.csv']).filter((r) => r['Name']).map((r) => ({ ...factories.languages(), name: r['Name'], proficiency: r['Proficiency'] || 'Fluent' })),
  };
  return { name: 'Imported from LinkedIn', data };
}

export async function importLinkedInZip(file) {
  try { return resumeFromLinkedIn(await readZipCsv(await file.arrayBuffer())); } catch { return null; }
}
