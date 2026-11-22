// Word (.docx) export: a plain, ATS-friendly document written without libraries (a .docx is a zip of XML).
// Port of the Android DocxExporter. Runs in the browser and in Node (that is how it is tested).
import { parseDesign } from './templates.js';
import { bulletLines } from './shared/resume.js';

const ACCENTS = { Green: '1B8A3C', Cyan: '0E7C9B', Slate: '4B5563', Dark: '1F2937', Blue: '1F5FBF' };
const GREY = '4B5563';
const has = (s) => String(s == null ? '' : s).trim() !== '';
const x = (s) => String(s == null ? '' : s).replace(/[\u0000-\u0008\u000B\u000C\u000E-\u001F]/g, '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

const run = (text, { bold, size = 21, color, italic } = {}) =>
  `<w:r><w:rPr><w:rFonts w:ascii="Calibri" w:hAnsi="Calibri" w:cs="Calibri"/>${bold ? '<w:b/>' : ''}${italic ? '<w:i/>' : ''}${color ? `<w:color w:val="${color}"/>` : ''}<w:sz w:val="${size}"/></w:rPr><w:t xml:space="preserve">${x(text)}</w:t></w:r>`;

function para(runs, { after = 60, before = 0, center, rightTab, border, indent } = {}) {
  let ppr = '<w:pPr>';
  if (border) ppr += `<w:pBdr><w:bottom w:val="single" w:sz="6" w:space="1" w:color="${border}"/></w:pBdr>`;
  if (rightTab) ppr += '<w:tabs><w:tab w:val="right" w:pos="9638"/></w:tabs>';
  ppr += `<w:spacing w:before="${before}" w:after="${after}"/>`;
  if (indent) ppr += '<w:ind w:left="360" w:hanging="240"/>';
  if (center) ppr += '<w:jc w:val="center"/>';
  return `<w:p>${ppr}</w:pPr>${runs}</w:p>`;
}

const heading = (t, color) => para(run(t.toUpperCase(), { bold: true, size: 23, color }), { after: 80, before: 200, border: color });
const bullets = (desc) => bulletLines(desc).map((l) => para(run('•  ' + l), { after: 30, indent: true })).join('');
const range = (a, b, cur) => [a, cur ? 'Present' : b].filter(has).join(' – ');
const tabRow = (left, right, before = 60) => para(run(left, { bold: true }) + '<w:r><w:tab/></w:r>' + run(right, { size: 19, color: GREY }), { after: 10, before, rightTab: true });

export function docxDocumentXml(d) {
  const accent = ACCENTS[d.colorSchemeId] || ACCENTS.Blue;
  const order = parseDesign(d.fontSize).order;
  let b = '';
  b += para(run(d.fullName || 'Your Name', { bold: true, size: 44, color: accent }), { after: 20, center: true });
  if (has(d.jobTitle)) b += para(run(d.jobTitle, { size: 26, color: GREY }), { after: 40, center: true });
  const contact = [d.email, d.phone, d.location, d.linkedinUrl, d.githubUrl, d.portfolioUrl].filter(has);
  if (contact.length) b += para(run(contact.join('  |  '), { size: 19, color: GREY }), { after: 80, center: true });

  for (const key of order) {
    if (key === 'summary' && has(d.summary)) b += heading('Summary', accent) + para(run(d.summary));
    else if (key === 'experience') {
      const list = (d.experience || []).filter((e) => has(e.role) || has(e.company));
      if (list.length) {
        b += heading('Experience', accent);
        for (const e of list) {
          b += tabRow(e.role, range(e.startDate, e.endDate, e.isCurrent));
          if (has(e.company)) b += para(run(e.company, { italic: true, color: GREY }), { after: 30 });
          b += bullets(e.description);
        }
      }
    } else if (key === 'education') {
      const list = (d.education || []).filter((e) => has(e.institution) || has(e.degree));
      if (list.length) {
        b += heading('Education', accent);
        for (const e of list) {
          b += tabRow(e.degree, range(e.startYear, e.endYear, false));
          const sub = [e.institution, e.grade].filter(has).join(' · ');
          if (sub) b += para(run(sub, { italic: true, color: GREY }), { after: 30 });
          b += bullets(e.achievements);
        }
      }
    } else if (key === 'skills') {
      const names = (d.skills || []).map((s) => s.name).filter(has);
      if (names.length) b += heading('Skills', accent) + para(run(names.join(', ')));
    } else if (key === 'projects') {
      const list = (d.projects || []).filter((p) => has(p.title));
      if (list.length) {
        b += heading('Projects', accent);
        for (const p of list) {
          b += para(run(p.title, { bold: true }) + (has(p.techStack) ? run('  –  ' + p.techStack, { color: GREY }) : ''), { after: 20, before: 60 });
          if (has(p.description)) b += para(run(p.description), { after: 30 });
          const links = [p.githubLink, p.liveLink].filter(has);
          if (links.length) b += para(run(links.join('  |  '), { size: 19, color: GREY }), { after: 30 });
        }
      }
    } else if (key === 'certifications') {
      const list = (d.certifications || []).filter((c) => has(c.name));
      if (list.length) {
        b += heading('Certifications', accent);
        for (const c of list) { const more = [c.issuer, c.date].filter(has).join(', '); b += para(run(c.name, { bold: true }) + run(more ? '  –  ' + more : ''), { after: 30 }); }
      }
    } else if (key === 'languages') {
      const list = (d.languages || []).filter((l) => has(l.name));
      if (list.length) b += heading('Languages', accent) + para(run(list.map((l) => `${l.name} (${l.proficiency})`).join('  ·  ')));
    } else if (key === 'achievements' && has(d.achievements)) b += heading('Awards and interests', accent) + bullets(d.achievements);
  }
  return '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>' + b +
    '<w:sectPr><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="1134" w:right="1134" w:bottom="1134" w:left="1134" w:header="708" w:footer="708" w:gutter="0"/></w:sectPr></w:body></w:document>';
}

// ---------------------------------------------------------------- a tiny zip writer (stored, no compression)
const CRC = (() => { const t = new Uint32Array(256); for (let n = 0; n < 256; n++) { let c = n; for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1; t[n] = c >>> 0; } return t; })();
export function crc32(bytes) { let c = 0xffffffff; for (let i = 0; i < bytes.length; i++) c = CRC[(c ^ bytes[i]) & 255] ^ (c >>> 8); return (c ^ 0xffffffff) >>> 0; }

export function zipStore(files) {
  const enc = new TextEncoder();
  const parts = [];
  const central = [];
  let offset = 0;
  const u16 = (n) => [n & 255, (n >> 8) & 255];
  const u32 = (n) => [n & 255, (n >>> 8) & 255, (n >>> 16) & 255, (n >>> 24) & 255];
  for (const [name, content] of files) {
    const nameB = enc.encode(name);
    const data = typeof content === 'string' ? enc.encode(content) : content;
    const crc = crc32(data);
    const local = new Uint8Array([...u32(0x04034b50), ...u16(20), ...u16(0x0800), ...u16(0), ...u16(0), ...u16(0x21), ...u32(crc), ...u32(data.length), ...u32(data.length), ...u16(nameB.length), ...u16(0), ...nameB]);
    parts.push(local, data);
    central.push(new Uint8Array([...u32(0x02014b50), ...u16(20), ...u16(20), ...u16(0x0800), ...u16(0), ...u16(0), ...u16(0x21), ...u32(crc), ...u32(data.length), ...u32(data.length), ...u16(nameB.length), ...u16(0), ...u16(0), ...u16(0), ...u16(0), ...u32(0), ...u32(offset), ...nameB]));
    offset += local.length + data.length;
  }
  const cdSize = central.reduce((s, c) => s + c.length, 0);
  const end = new Uint8Array([...u32(0x06054b50), ...u16(0), ...u16(0), ...u16(files.length), ...u16(files.length), ...u32(cdSize), ...u32(offset), ...u16(0)]);
  const all = [...parts, ...central, end];
  const out = new Uint8Array(all.reduce((s, p) => s + p.length, 0));
  let o = 0;
  for (const p of all) { out.set(p, o); o += p.length; }
  return out;
}

const HEAD = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>';
export function generateResumeDocx(resume) {
  const d = (resume && resume.data) || {};
  return zipStore([
    ['[Content_Types].xml', `${HEAD}<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/></Types>`],
    ['_rels/.rels', `${HEAD}<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/></Relationships>`],
    ['word/document.xml', docxDocumentXml(d)],
  ]);
}

export function docxFileName(resume) {
  const d = (resume && resume.data) || {};
  const base = (d.fullName || (resume && resume.name) || 'Resume').replace(/[^\w .-]+/g, '').replace(/^[.\s-]+/, '').trim().slice(0, 60) || 'Resume';
  return `${base} - Resume.docx`;
}
