'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const path = require('node:path');
const { pathToFileURL } = require('node:url');

const load = () => import(pathToFileURL(path.join(__dirname, '..', '..', 'web', 'js', 'pdf.js')).href);

const resume = (extra = {}, tpl = 'Modern Professional') => ({
  name: 'x',
  data: {
    fullName: 'Asha Rao (Mumbai)', jobTitle: 'Marketing Manager', email: 'asha@example.com', phone: '+91 98765 43210', location: 'Mumbai',
    linkedinUrl: 'linkedin.com/in/asha', summary: 'Café owner and “marketer” – 6 years.',
    experience: [{ company: 'Acme', role: 'Manager', startDate: '2020', endDate: '', isCurrent: true, description: '• Grew leads by 42%\n- Led a team of 5' }],
    skills: [{ name: 'SEO', category: 'Technical' }], templateId: tpl, colorSchemeId: 'Blue', fontSize: 'Standard', margins: 'Standard', ...extra,
  },
});
const text = (u8) => Buffer.from(u8).toString('latin1');

test('the PDF is a well-formed file: header, trailer and a correct xref table', async () => {
  const { generateResumePdf } = await load();
  for (const tpl of ['Modern Professional', 'Minimal ATS', 'Executive Premium', 'Compact Two-Column']) {
    const { bytes, pages, lossy } = generateResumePdf(resume({}, tpl));
    const s = text(bytes);
    assert.ok(s.startsWith('%PDF-1.4') && s.trimEnd().endsWith('%%EOF'), tpl);
    assert.ok(pages >= 1 && !lossy, tpl);
    const start = Number(/startxref\n(\d+)\n/.exec(s)[1]);
    assert.equal(s.slice(start, start + 4), 'xref', tpl);
    // every xref entry points at "<n> 0 obj"
    const entries = [...s.slice(start).matchAll(/(\d{10}) 00000 n /g)].map((m) => Number(m[1]));
    entries.forEach((off, i) => assert.equal(s.slice(off, off + String(i + 1).length + 6), `${i + 1} 0 obj`, `${tpl} object ${i + 1}`));
    assert.ok(bytes.length > 1000 && bytes.length < 200000, `${tpl} size ${bytes.length}`);
  }
});

test('text is real text, brackets are escaped, accents and smart quotes are kept', async () => {
  const { generateResumePdf } = await load();
  const s = text(generateResumePdf(resume()).bytes);
  assert.ok(s.includes('\\(Mumbai\\)'));
  assert.ok(s.includes('Caf\\351'));     // é in WinAnsi
  assert.ok(s.includes('\\223marketer\\224')); // “ ”
  assert.ok(s.includes('Grew leads by 42%') || s.includes('Grew') );
  assert.ok(s.includes('https://linkedin.com/in/asha'));
});

test('characters outside Western European text are reported, not hidden', async () => {
  const { generateResumePdf } = await load();
  assert.equal(generateResumePdf(resume({ fullName: 'अशा Rao' })).lossy, true);
  assert.equal(generateResumePdf(resume({ summary: 'résumé – naïve “ok” €5' })).lossy, false);
});

test('a long resume flows onto more pages and an empty one still makes a valid page', async () => {
  const { generateResumePdf } = await load();
  const job = { company: 'C', role: 'R', startDate: '2020', endDate: '2021', isCurrent: false, description: Array.from({ length: 8 }, (_, i) => `Did thing number ${i} with a long sentence that needs to wrap across the line width at least once or twice`).join('\n') };
  const many = generateResumePdf(resume({ experience: Array.from({ length: 16 }, () => job) }));
  assert.ok(many.pages >= 3, `pages ${many.pages}`);
  const empty = generateResumePdf({ data: {} });
  assert.equal(empty.pages, 1);
  assert.ok(text(empty.bytes).startsWith('%PDF-'));
});

test('hostile text cannot break the file and unsafe links are not made clickable', async () => {
  const { generateResumePdf } = await load();
  const r = generateResumePdf(resume({ fullName: ') Tj ET /Evil (', linkedinUrl: 'javascript:alert(1)', githubUrl: 'x'.repeat(5000), summary: 'a'.repeat(3000) }));
  const s = text(r.bytes);
  assert.ok(!s.includes('javascript:'));
  assert.ok(s.includes('\\) Tj ET /Evil \\('));
  assert.ok(r.pages >= 1);
});

test('download file names are safe', async () => {
  const { pdfFileName } = await load();
  assert.equal(pdfFileName({ data: { fullName: 'Asha Rao' } }), 'Asha Rao - Resume.pdf');
  assert.equal(pdfFileName({ name: 'My CV', data: {} }), 'My CV - Resume.pdf');
  assert.equal(pdfFileName({ data: { fullName: '../../etc/passwd<>' } }), 'etcpasswd - Resume.pdf');
});
