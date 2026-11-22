// Makes a real, text-based PDF of a resume (selectable text, readable by applicant tracking systems).
// No libraries, no DOM: it runs in the browser and in Node (that is how it is tested).
// Fonts are the 14 standard PDF fonts, so every viewer has them and the file stays small (a few KB).
// Limit: only Western European characters (Windows-1252) can be written. Anything else becomes "?",
// and the result reports it (`lossy`) so the app can tell the person.

import { METRICS } from './pdf-metrics.js';
import { parseDesign, webTemplate, COLORS, MARGINS, SECTION_TITLES, safeUrl, SERIF_TEMPLATES, darken } from './templates.js';

const MM = 72 / 25.4;
const PAGE_W = 595.28;
const PAGE_H = 841.89;
const FONTS = ['Helvetica', 'Helvetica-Bold', 'Times-Roman', 'Times-Bold', 'Times-Italic'];
const FONT_ID = Object.fromEntries(FONTS.map((f, i) => [f, 'F' + (i + 1)]));

// Windows-1252 bytes 0x80-0x9F (undefined ones are left out)
const HIGH = '€\u0081‚ƒ„…†‡ˆ‰Š‹Œ\u008DŽ\u008F\u0090‘’“”•–—˜™š›œ\u009DžŸ';
const CP1252 = new Map();
[...HIGH].forEach((ch, i) => { if (!/[\u0080-\u009F]/.test(ch)) CP1252.set(ch, 0x80 + i); });
const SWAP = { '→': '->', '←': '<-', '‐': '-', '‑': '-', '−': '-', '\u2009': ' ', '\u202F': ' ', '\u200B': '', '\u2192': '->', '✓': 'v', '★': '*' };

const hex = (h) => { const m = /^#?([0-9a-f]{6})$/i.exec(h); const n = m ? parseInt(m[1], 16) : 0; return [(n >> 16) / 255, ((n >> 8) & 255) / 255, (n & 255) / 255]; };
const num = (v) => String(Math.round(v * 100) / 100);

/** Splits an Unicode string into the Windows-1252 "binary string" PDF needs. */
function makeEncoder() {
  const state = { lossy: false };
  const enc = (str, quiet) => {
    let out = '';
    for (let ch of String(str == null ? '' : str)) {
      if (ch === '\t' || ch === '\u00A0') ch = ' ';
      if (SWAP[ch] !== undefined) ch = SWAP[ch];
      for (const c of ch) {
        const code = c.codePointAt(0);
        if (code >= 32 && code < 127) out += c;
        else if (code >= 0xA0 && code <= 0xFF) out += c;
        else if (CP1252.has(c)) out += String.fromCharCode(CP1252.get(c));
        else if (code < 32 || code === 127) out += ' ';
        else { out += '?'; if (!quiet) state.lossy = true; }
      }
    }
    return out;
  };
  return { enc, state };
}

const width = (bin, font, size, cs = 0) => {
  const m = METRICS[font];
  let w = 0;
  for (let i = 0; i < bin.length; i++) { const c = bin.charCodeAt(i); w += c >= 32 ? m[c - 32] || 0 : 0; }
  return (w * size) / 1000 + cs * bin.length;
};

const pdfString = (bin) => '(' + bin.replace(/[\\()]/g, '\\$&').replace(/[\u0080-\u00FF]/g, (c) => '\\' + c.charCodeAt(0).toString(8).padStart(3, '0')) + ')';
const asciiUrl = (u) => encodeURI(u).replace(/[\\()]/g, (c) => '%' + c.charCodeAt(0).toString(16).toUpperCase());

/**
 * @param resume {{name?:string, data:object}}
 * @returns {{bytes:Uint8Array, pages:number, lossy:boolean}}
 */
export function generateResumePdf(resume) {
  const d = (resume && resume.data) || {};
  const { enc, state } = makeEncoder();
  const tpl = webTemplate(d.templateId);
  const design = parseDesign(d.fontSize);
  const accent = hex(COLORS[d.colorSchemeId] || COLORS.Blue);
  const M = (MARGINS[d.margins] || MARGINS.Standard) * MM;
  const base = { Compact: 9.5, Standard: 10.5, Large: 11.5 }[design.size];
  const gap = design.tight ? 0.8 : 1;
  const INK = [0.07, 0.07, 0.08];
  const GREY = [0.33, 0.33, 0.35];

  const serif = SERIF_TEMPLATES.includes(tpl);
  const F = { body: serif ? 'Times-Roman' : 'Helvetica', bold: serif ? 'Times-Bold' : 'Helvetica-Bold', italic: serif ? 'Times-Italic' : 'Helvetica' };
  const st = {
    'Modern Professional': { name: [accent, 24, 'left'], head: accent, rule: [0.87, 0.87, 0.88], headRuleW: 0.6, bottomRule: [accent, 1.5], cs: 1 },
    'Minimal ATS': { name: [[0, 0, 0], 20, 'center'], head: [0, 0, 0], rule: [0, 0, 0], headRuleW: 0.8, bottomRule: null, cs: 0.4 },
    'Executive Premium': { name: [INK, 24, 'center'], head: hex('#7A5A08'), rule: hex('#E3D3A4'), headRuleW: 0.7, bottomRule: [hex('#B8860B'), 1.5], cs: 1.2 },
    'Compact Two-Column': { name: [[1, 1, 1], 21, 'left'], head: accent, rule: [0.87, 0.87, 0.88], headRuleW: 0.6, bottomRule: null, cs: 1, band: accent },
    'Premium Creative': { name: [[1, 1, 1], 24, 'left'], head: accent, rule: [0.87, 0.87, 0.88], headRuleW: 0.6, bottomRule: null, cs: 1, band: hex(darken(COLORS[d.colorSchemeId] || COLORS.Blue, 0.5)) },
    'Bold Header': { name: [[1, 1, 1], 23, 'left'], head: accent, rule: [0.87, 0.87, 0.88], headRuleW: 0.6, bottomRule: null, cs: 1, band: hex(darken(COLORS[d.colorSchemeId] || COLORS.Blue, 0.55)) },
    'Developer Resume': { name: [[1, 1, 1], 22, 'left'], head: accent, rule: [0.87, 0.87, 0.88], headRuleW: 0.6, bottomRule: null, cs: 1, band: hex('#0f172a') },
    'Academic CV': { name: [INK, 21, 'center'], head: [0, 0, 0], rule: [0, 0, 0], headRuleW: 0.8, bottomRule: null, cs: 0.6 },
    'Classic Serif': { name: [INK, 22, 'center'], head: INK, rule: [0.8, 0.8, 0.82], headRuleW: 0.6, bottomRule: [INK, 1.5], cs: 1.2 },
    Timeline: { name: [accent, 23, 'left'], head: accent, rule: accent, headRuleW: 0.9, bottomRule: null, cs: 1 },
  }[tpl];

  // ---------------------------------------------------------------- text layout
  /** runs: [{text, font?, color?, url?, size?}] -> lines of positioned words */
  function layoutRuns(runs, size, maxW, cs = 0) {
    const atoms = [];
    for (const run of runs) {
      const font = run.font || F.body;
      const parts = enc(run.text).split(/( +)/);
      for (const p of parts) {
        if (p === '') continue;
        if (/^ +$/.test(p)) { if (atoms.length) atoms[atoms.length - 1].gap = width(' ', atoms[atoms.length - 1].font, size, cs) * p.length; }
        else atoms.push({ t: p, font, color: run.color || INK, url: run.url, gap: 0 });
      }
    }
    const lines = [];
    let line = { words: [], w: 0 };
    const push = () => { lines.push(line); line = { words: [], w: 0 }; };
    for (const a of atoms) {
      let pieces = [a.t];
      const full = width(a.t, a.font, size, cs);
      if (full > maxW) { // a very long word: cut it so it never leaves the page
        pieces = []; let cur = '';
        for (const ch of a.t) { if (width(cur + ch, a.font, size, cs) > maxW && cur) { pieces.push(cur); cur = ch; } else cur += ch; }
        if (cur) pieces.push(cur);
      }
      pieces.forEach((t, i) => {
        const w = width(t, a.font, size, cs);
        const lastPiece = i === pieces.length - 1;
        if (line.words.length && line.w + w > maxW + 0.01) { line.w -= line.words[line.words.length - 1].gap || 0; push(); }
        line.words.push({ t, font: a.font, color: a.color, url: a.url, x: line.w, w, gap: lastPiece ? a.gap : 0 });
        line.w += w + (lastPiece ? a.gap : 0);
      });
    }
    if (line.words.length || !lines.length) { if (line.words.length) line.w -= line.words[line.words.length - 1].gap || 0; push(); }
    return lines;
  }

  const lhOf = (size) => size * 1.32;

  // ---------------------------------------------------------------- drawing surface
  const pages = [];
  const newPage = () => { const p = { ops: [], links: [] }; pages.push(p); return p; };
  const Y = (top) => PAGE_H - top;

  function drawText(page, x, baseTop, bin, font, size, color, cs = 0) {
    if (!bin) return;
    // Tc (letter spacing) stays set in the PDF graphics state, so every text run sets it explicitly
    page.ops.push(`BT /${FONT_ID[font]} ${num(size)} Tf ${num(cs)} Tc ${color.map((c) => num(c)).join(' ')} rg ${num(x)} ${num(Y(baseTop))} Td ${pdfString(bin)} Tj ET`);
  }
  function drawLine(page, x1, yTop, x2, color, w) {
    page.ops.push(`${color.map((c) => num(c)).join(' ')} RG ${num(w)} w ${num(x1)} ${num(Y(yTop))} m ${num(x2)} ${num(Y(yTop))} l S`);
  }
  function drawRect(page, x, yTop, w, h, color) {
    page.ops.push(`${color.map((c) => num(c)).join(' ')} rg ${num(x)} ${num(Y(yTop + h))} ${num(w)} ${num(h)} re f`);
  }
  function drawLines(page, lines, x, yTop, w, size, align) {
    const lh = lhOf(size);
    lines.forEach((ln, i) => {
      const off = align === 'center' ? (w - ln.w) / 2 : align === 'right' ? w - ln.w : 0;
      const base = yTop + i * lh + (lh - size) / 2 + size * 0.82;
      for (const wd of ln.words) {
        drawText(page, x + off + wd.x, base, wd.t, wd.font, size, wd.color);
        if (wd.url) page.links.push({ x1: x + off + wd.x, x2: x + off + wd.x + wd.w, y1: Y(base + size * 0.25), y2: Y(base - size * 0.8), url: wd.url });
      }
    });
  }

  // ---------------------------------------------------------------- blocks
  // a block is one unbreakable piece: { h, before, keepNext, draw(page, x, yTop) }
  const para = (runs, o) => {
    const size = o.size || base;
    const ind = o.indent || 0;
    const lines = layoutRuns(runs, size, o.w - ind);
    return {
      h: lines.length * lhOf(size), before: (o.before || 0) * gap, keepNext: !!o.keepNext,
      draw(page, x, y) {
        if (o.bullet) drawText(page, x + 1.5, y + (lhOf(size) - size) / 2 + size * 0.82, enc('•'), F.body, size, INK);
        drawLines(page, lines, x + ind, y, o.w - ind, size, o.align || 'left');
      },
    };
  };

  const row = (leftRuns, rightRuns, o) => {
    const size = o.size || base;
    const rightLines = rightRuns && rightRuns.length ? layoutRuns(rightRuns, size - 0.5, o.w) : [];
    const rightW = rightLines.length ? rightLines[0].w : 0;
    const stack = o.stack || rightW > o.w * 0.55;
    const leftW = stack || !rightW ? o.w : o.w - rightW - 10;
    const left = layoutRuns(leftRuns, size, leftW);
    const lh = lhOf(size);
    const leftH = left.length * lh;
    const rightH = stack && rightLines.length ? rightLines.length * lhOf(size - 0.5) : 0;
    return {
      h: leftH + rightH, before: (o.before || 0) * gap, keepNext: o.keepNext !== false,
      draw(page, x, y) {
        drawLines(page, left, x, y, leftW, size, 'left');
        if (rightLines.length) {
          if (stack) drawLines(page, rightLines, x, y + leftH, o.w, size - 0.5, 'left');
          else drawLines(page, [rightLines[0]], x, y, o.w, size - 0.5, 'right');
        }
      },
    };
  };

  const heading = (title, w) => {
    const size = 9.5;
    const bin = enc(title.toUpperCase());
    return {
      h: lhOf(size) + 5, before: 11 * gap, keepNext: true,
      draw(page, x, y) {
        drawText(page, x, y + (lhOf(size) - size) / 2 + size * 0.82, bin, F.bold, size, st.head, st.cs);
        drawLine(page, x, y + lhOf(size) + 1, x + w, st.rule, st.headRuleW);
      },
    };
  };

  const has = (s) => String(s || '').trim() !== '';
  const lines = (t) => String(t || '').split('\n').map((s) => s.trim().replace(/^[•\-*–]\s*/, '')).filter(Boolean);
  const range = (a, b, cur) => [a, cur ? 'Present' : b].filter(Boolean).join(' – ');
  const bulletsOf = (text, w) => lines(text).map((l, i) => para([{ text: l }], { w, indent: 11, bullet: true, before: i === 0 ? 2 : 1 }));
  const titleRuns = (a, b) => [{ text: a || '', font: F.bold }, ...(has(a) && has(b) ? [{ text: ' · ' }] : []), { text: b || '' }];

  function sectionBlocks(key, w, stack) {
    const blocks = [];
    const R = (l, r, o = {}) => row(l, r, { w, stack, ...o });
    if (key === 'summary') {
      String(d.summary || '').split('\n').filter(has).forEach((p, i) => blocks.push(para([{ text: p.trim() }], { w, before: i ? 3 : 3 })));
    } else if (key === 'experience') {
      for (const e of d.experience || []) {
        if (!(has(e.role) || has(e.company) || has(e.description))) continue;
        blocks.push(R(titleRuns(e.role, e.company), [{ text: range(e.startDate, e.endDate, e.isCurrent), color: GREY }], { before: 6, keepNext: has(e.description) }));
        blocks.push(...bulletsOf(e.description, w));
      }
    } else if (key === 'education') {
      for (const e of d.education || []) {
        if (!(has(e.institution) || has(e.degree))) continue;
        blocks.push(R(titleRuns(e.degree, e.institution), [{ text: range(e.startYear, e.endYear, false), color: GREY }], { before: 6, keepNext: has(e.grade) || has(e.achievements) }));
        if (has(e.grade)) blocks.push(para([{ text: e.grade, color: GREY }], { w, before: 1 }));
        blocks.push(...bulletsOf(e.achievements, w));
      }
    } else if (key === 'skills') {
      const groups = new Map();
      for (const s of d.skills || []) if (has(s.name)) { const c = s.category || 'Skills'; groups.set(c, [...(groups.get(c) || []), s.name]); }
      const many = groups.size > 1;
      let i = 0;
      for (const [c, names] of groups) {
        blocks.push(para([...(many ? [{ text: c + ': ', font: F.bold }] : []), { text: names.join(' · ') }], { w, before: i++ ? 2 : 3 }));
      }
    } else if (key === 'projects') {
      for (const p of d.projects || []) {
        if (!(has(p.title) || has(p.description))) continue;
        const links = [];
        const code = safeUrl(p.githubLink); const live = safeUrl(p.liveLink);
        if (code) links.push({ text: 'Code', url: code, color: accent });
        if (code && live) links.push({ text: ' · ' });
        if (live) links.push({ text: 'Live', url: live, color: accent });
        blocks.push(R(titleRuns(p.title, p.techStack), links, { before: 6, keepNext: has(p.description) }));
        if (has(p.description)) blocks.push(para([{ text: p.description.replace(/\s*\n\s*/g, ' ') }], { w, before: 1 }));
      }
    } else if (key === 'certifications') {
      for (const c of d.certifications || []) {
        if (!has(c.name)) continue;
        blocks.push(R(titleRuns(c.name, c.issuer), [{ text: c.date || '', color: GREY }], { before: 4, keepNext: false }));
      }
    } else if (key === 'languages') {
      const l = (d.languages || []).filter((x) => has(x.name)).map((x) => x.name + (has(x.proficiency) ? ` (${x.proficiency})` : ''));
      if (l.length) blocks.push(para([{ text: l.join(' · ') }], { w, before: 3 }));
    } else if (key === 'achievements') {
      const l = lines(d.achievements);
      if (l.length) blocks.push(...bulletsOf(d.achievements, w));
    }
    return blocks.length ? [heading(SECTION_TITLES[key], w), ...blocks] : [];
  }

  // ---------------------------------------------------------------- header
  const innerW = PAGE_W - 2 * M;
  const contactSegs = () => {
    const segs = [];
    const add = (text, url) => { if (segs.length) segs.push({ text: '  |  ', color: [0.6, 0.6, 0.62] }); segs.push({ text, url, color: url ? accent : (st.band ? [1, 1, 1] : GREY) }); };
    if (has(d.email)) add(d.email);
    if (has(d.phone)) add(d.phone);
    if (has(d.location)) add(d.location);
    if (has(d.linkedinUrl)) add('LinkedIn', safeUrl(d.linkedinUrl) || undefined);
    if (has(d.githubUrl)) add('GitHub', safeUrl(d.githubUrl) || undefined);
    if (has(d.portfolioUrl)) add('Portfolio', safeUrl(d.portfolioUrl) || undefined);
    if (st.band) segs.forEach((s) => { if (s.url) s.color = [1, 1, 1]; });
    return segs;
  };

  const band = !!st.band;
  const hx = M;
  const hw = innerW;
  const [nameColor, nameSize, nameAlign] = st.name;
  const headParts = [];
  headParts.push({ lines: layoutRuns([{ text: d.fullName || 'Your name', font: F.bold, color: nameColor }], nameSize, hw), size: nameSize, align: nameAlign, gapAfter: 3 });
  if (has(d.jobTitle)) headParts.push({ lines: layoutRuns([{ text: d.jobTitle, font: serif ? F.italic : F.body, color: band ? [1, 1, 1] : [0.2, 0.2, 0.22] }], base + 1.5, hw), size: base + 1.5, align: nameAlign, gapAfter: 3 });
  const cs = contactSegs();
  if (cs.length) headParts.push({ lines: layoutRuns(cs, base - 1, hw), size: base - 1, align: nameAlign, gapAfter: 0 });
  const headTextH = headParts.reduce((s, p) => s + p.lines.length * lhOf(p.size) + p.gapAfter, 0);
  const padBand = band ? 14 : 0;
  const headerH = padBand * 2 + headTextH + (st.bottomRule ? 10 : 0) + (band ? 14 : 8);

  function drawHeader(page) {
    let y = band ? 0 : M;
    if (band) { drawRect(page, 0, 0, PAGE_W, padBand * 2 + headTextH, st.band); y = padBand; }
    for (const p of headParts) { drawLines(page, p.lines, hx, y, hw, p.size, p.align); y += p.lines.length * lhOf(p.size) + p.gapAfter; }
    if (st.bottomRule) drawLine(page, hx, y + 5, hx + hw, st.bottomRule[0], st.bottomRule[1]);
  }

  // ---------------------------------------------------------------- columns and pages
  const colGap = 18;
  let flows;
  if (tpl === 'Compact Two-Column') {
    const mainW = (innerW - colGap) * 0.655;
    const sideW = innerW - colGap - mainW;
    const sideKeys = ['education', 'skills', 'languages', 'certifications'];
    flows = [
      { x: M, w: mainW, blocks: design.order.filter((k) => !sideKeys.includes(k)).flatMap((k) => sectionBlocks(k, mainW, false)) },
      { x: M + mainW + colGap, w: sideW, blocks: design.order.filter((k) => sideKeys.includes(k)).flatMap((k) => sectionBlocks(k, sideW, true)) },
    ];
  } else {
    flows = [{ x: M, w: innerW, blocks: design.order.flatMap((k) => sectionBlocks(k, innerW, false)) }];
  }

  const bottom = PAGE_H - M;
  const idx = flows.map(() => 0);
  let first = true;
  do {
    const page = newPage();
    let top = M;
    if (first) { drawHeader(page); top = M + headerH; if (band) top = padBand * 2 + headTextH + 16; first = false; }
    flows.forEach((flow, k) => {
      let y = top;
      while (idx[k] < flow.blocks.length) {
        const b = flow.blocks[idx[k]];
        const atTop = y === top;
        // keep a heading with the lines that follow it
        let need = (atTop ? 0 : b.before) + b.h;
        for (let j = idx[k]; flow.blocks[j].keepNext && j + 1 < flow.blocks.length; j++) need += flow.blocks[j + 1].before + flow.blocks[j + 1].h;
        if (!atTop && y + need > bottom + 0.01) break;
        const gapTop = atTop ? 0 : b.before;
        b.draw(page, flow.x, y + gapTop);
        y += gapTop + b.h;
        idx[k] += 1;
      }
    });
  } while (flows.some((f, k) => idx[k] < f.blocks.length));

  // ---------------------------------------------------------------- file
  const out = [];
  let size = 0;
  const offsets = [];
  const put = (s) => { out.push(s); size += s.length; };
  const objects = []; // index = id - 1
  const addObj = (body) => { objects.push(body); return objects.length; };

  const catalogId = addObj('');
  const pagesId = addObj('');
  const info = `<< /Title ${pdfString(enc((d.fullName || 'Resume') + ' - Resume', true))} /Producer (AutoResume) /Creator (AutoResume Web) >>`;
  const infoId = addObj(info);
  const fontIds = FONTS.map((f) => addObj(`<< /Type /Font /Subtype /Type1 /BaseFont /${f} /Encoding /WinAnsiEncoding >>`));
  const fontDict = FONTS.map((f, i) => `/${FONT_ID[f]} ${fontIds[i]} 0 R`).join(' ');
  const kids = [];
  for (const p of pages) {
    const content = p.ops.join('\n');
    const cId = addObj(`<< /Length ${content.length} >>\nstream\n${content}\nendstream`);
    const annots = p.links.map((l) => addObj(`<< /Type /Annot /Subtype /Link /Rect [${num(l.x1)} ${num(Math.min(l.y1, l.y2))} ${num(l.x2)} ${num(Math.max(l.y1, l.y2))}] /Border [0 0 0] /A << /S /URI /URI (${asciiUrl(l.url)}) >> >>`));
    const pageId = addObj(`<< /Type /Page /Parent ${pagesId} 0 R /MediaBox [0 0 ${PAGE_W} ${PAGE_H}] /Resources << /Font << ${fontDict} >> >> /Contents ${cId} 0 R${annots.length ? ` /Annots [${annots.map((a) => a + ' 0 R').join(' ')}]` : ''} >>`);
    kids.push(pageId);
  }
  objects[catalogId - 1] = `<< /Type /Catalog /Pages ${pagesId} 0 R >>`;
  objects[pagesId - 1] = `<< /Type /Pages /Count ${kids.length} /Kids [${kids.map((k) => k + ' 0 R').join(' ')}] >>`;

  put('%PDF-1.4\n%\u00E2\u00E3\u00CF\u00D3\n');
  objects.forEach((body, i) => { offsets.push(size); put(`${i + 1} 0 obj\n${body}\nendobj\n`); });
  const xref = size;
  put(`xref\n0 ${objects.length + 1}\n0000000000 65535 f \n`);
  offsets.forEach((o) => put(String(o).padStart(10, '0') + ' 00000 n \n'));
  put(`trailer\n<< /Size ${objects.length + 1} /Root ${catalogId} 0 R /Info ${infoId} 0 R >>\nstartxref\n${xref}\n%%EOF\n`);

  const bin = out.join('');
  const bytes = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i) & 255;
  return { bytes, pages: pages.length, lossy: state.lossy };
}

/** Safe file name for the download: "Asha Rao - Resume.pdf" */
export function pdfFileName(resume) {
  const d = (resume && resume.data) || {};
  const base = (d.fullName || (resume && resume.name) || 'Resume').replace(/[^\w .-]+/g, '').replace(/^[.\s-]+/, '').trim().slice(0, 60) || 'Resume';
  return `${base} - Resume.pdf`;
}
