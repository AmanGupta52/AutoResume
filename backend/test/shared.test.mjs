import test from 'node:test';
import assert from 'node:assert/strict';
import { sampleData, blankData, strength, nextStep, parseTarget, encodeTarget, normalize } from '../../web/js/shared/resume.js';
import { analyzeResume, containsKeyword, extractKeywords } from '../../web/js/shared/ats.js';
import { analyzeWriting } from '../../web/js/shared/writing.js';
import { templateFor, suggestions } from '../../web/js/shared/coach.js';
import { newNumbers, hasPlaceholders, parseSkills } from '../../web/js/shared/prompts.js';

test('keywords match whole words only', () => {
  assert.equal(containsKeyword('digital marketing', 'Git'), false);
  assert.equal(containsKeyword('worked at google', 'Go'), false);
  assert.equal(containsKeyword('uses ci/cd and node.js daily', 'CI/CD'), true);
  assert.equal(containsKeyword('c++ developer', 'C++'), true);
});

test('ATS: a matching resume scores higher than an empty one; missing keywords are listed', () => {
  const jd = 'Seeking a Data Analyst with SQL, Python, Tableau and Excel. Experience with A/B testing.';
  const good = sampleData(); good.skills.push({ name: 'SQL' }, { name: 'Tableau' });
  const a = analyzeResume(good, jd);
  const b = analyzeResume(blankData(null), jd);
  assert.ok(a.overallScore > b.overallScore);
  assert.ok(a.foundKeywords.includes('SQL'));
  assert.ok(a.missingKeywords.includes('Python'));
  assert.ok(a.suggestions.length >= 1);
  assert.ok(extractKeywords('').length === 0);
});

test('writing check finds weak phrases, misspellings, bad email; worst first', () => {
  const d = sampleData();
  d.email = 'not-an-email';
  d.experience[0].description = '• responsible for teh dashboards\n• worked on reports';
  const issues = analyzeWriting(d);
  assert.ok(issues.some((i) => /Weak phrase: "responsible for"/.test(i.problem)));
  assert.ok(issues.some((i) => /misspelling: "teh"/.test(i.problem)));
  assert.ok(issues.some((i) => i.where === 'Email' && i.level === 'HIGH'));
  const order = issues.map((i) => ({ HIGH: 0, MEDIUM: 1, LOW: 2 }[i.level]));
  assert.deepEqual(order, [...order].sort((x, y) => x - y));
});

test('strength, next step, target job round trip', () => {
  assert.equal(strength(blankData(null)), 12);
  assert.equal(strength(sampleData()), 100);
  assert.equal(nextStep(sampleData()), null);
  assert.match(nextStep(blankData(null)).label, /name and job title/);
  const t = { title: 'Data Analyst', description: 'SQL\nand more' };
  assert.deepEqual(parseTarget(encodeTarget(t)), t);
  assert.equal(encodeTarget({ title: '', description: ' ' }), '');
  const n = normalize({ id: 'x', data: { experience: [{ role: 'A' }], skills: 'bad' } });
  assert.equal(n.data.experience[0].company, '');
  assert.deepEqual(n.data.skills, []);
});

test('template choice and suggestion cards', () => {
  assert.equal(templateFor('Backend Developer', '')[0], 'Developer Resume');
  assert.equal(templateFor('International sales rep', '')[0], 'Minimal ATS');   // "intern" must not match "international"
  assert.equal(templateFor('Marketing Manager', '')[0], 'Premium Creative');
  const d = blankData(null);
  const s = suggestions(1, d, true);
  assert.equal(s[0].action.req.kind, 'SUMMARY');
  assert.equal(suggestions(1, d, false).some((x) => x.action.type === 'ai'), false);   // no AI cards when AI is off
  assert.ok(suggestions(0, d, true).length <= 3);
});

test('AI guard: invented numbers and placeholders are caught', () => {
  const d = sampleData();
  assert.deepEqual(newNumbers('Grew leads 42% and cut cost 18%', d), []);
  assert.deepEqual(newNumbers('Grew leads 77%', d), ['77%']);
  assert.equal(hasPlaceholders('Saved [N hours]'), true);
  assert.equal(hasPlaceholders('Saved 3 hours'), false);
  assert.deepEqual(parseSkills('• SQL, Power BI.\n- Tableau'), ['SQL', 'Power BI', 'Tableau']);
});
