'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const { createAiHandlers } = require('../src/routes/aiHandlers');
const { createUsageLimiter } = require('../src/services/usage');
const { AiError } = require('../src/services/nvidia');
const { HttpError } = require('../src/middleware/error');
const { sanitizeResumeData } = require('../src/utils/resumeSchema');

function fakeRes() {
  const l = {};
  const res = {
    statusCode: 200, body: null, headers: null, chunks: [], writableEnded: false, writableFinished: false,
    on(ev, fn) { (l[ev] = l[ev] || []).push(fn); return res; },
    off(ev, fn) { l[ev] = (l[ev] || []).filter((f) => f !== fn); return res; },
    emit(ev) { (l[ev] || []).forEach((f) => f()); },
    json(o) { res.body = o; res.writableEnded = res.writableFinished = true; },
    writeHead(c, h) { res.statusCode = c; res.headers = h; },
    write(s) { res.chunks.push(s); return true; },
    end() { res.writableEnded = res.writableFinished = true; },
    events() { return res.chunks.join('').split('\n\n').filter(Boolean).map((b) => { const [e, d] = b.split('\n'); return [e.slice(7), JSON.parse(d.slice(6))]; }); },
  };
  return res;
}
const user = { _id: 'user-1' };
const sample = () => ({
  fullName: 'Asha Rao', jobTitle: 'Data Analyst', email: 'asha@example.com', phone: '+91 98765 43210', location: 'Pune',
  summary: 'Analyst with 3 years of experience.', experience: [{ company: 'Acme', role: 'Analyst', startDate: 'Jan 2022', endDate: '', isCurrent: true, description: '• Built dashboards used by 40 managers.' }],
  skills: [{ name: 'SQL' }, { name: 'Excel' }],
});
const mk = (over = {}) => {
  const calls = [];
  const ai = { enabled: true, async complete(o) { calls.push(o); return { text: 'Result text with 99% and [X]', model: 'm/test' }; },
    async stream(o) { calls.push(o); o.onText('Part'); o.onText('Part two'); return { text: 'Part two', model: 'm/test' }; }, ...over };
  const usage = createUsageLimiter({ dailyLimit: 3, maxParallel: 2 });
  return { calls, h: createAiHandlers({ ai, usage }), usage };
};
const rejectsWith = (p, status, code) => assert.rejects(p, (e) => e instanceof HttpError && e.status === status && e.code === code);

test('assist: builds the prompt from the resume, answers as JSON with the safety checks', async () => {
  const { h, calls } = mk();
  const res = fakeRes();
  await h.assist({ user, body: { kind: 'SUMMARY', tab: 'Summary', data: sample() } }, res);
  assert.equal(res.body.text, 'Result text with 99% and [X]');
  assert.equal(res.body.model, 'm/test');
  assert.deepEqual(res.body.newNumbers, ['99%']);   // 99% is not in the resume: the person must confirm it
  assert.equal(res.body.hasPlaceholders, true);
  const sys = calls[0].messages[0].content;
  const usr = calls[0].messages[1].content;
  assert.match(sys, /professional summary/);
  assert.match(usr, /CURRENT SECTION: Summary/);
  assert.match(usr, /NAME: Asha Rao/);
  assert.match(usr, /JOB 1: Analyst at Acme/);
  assert.equal(calls[0].maxTokens, 450);
});

test('assist: bullets need a real job with a title; skills come back as a list', async () => {
  const { h } = mk();
  await rejectsWith(h.assist({ user, body: { kind: 'BULLETS', index: 5, data: sample() } }, fakeRes()), 400, 'INVALID');
  const d = sample(); d.experience[0].role = '  ';
  await rejectsWith(h.assist({ user, body: { kind: 'BULLETS', index: 0, data: d } }, fakeRes()), 400, 'INVALID');
  await rejectsWith(h.assist({ user, body: { kind: 'NOPE', data: sample() } }, fakeRes()), 400, 'INVALID');
  await rejectsWith(h.assist({ user, body: { kind: 'SUMMARY' } }, fakeRes()), 400, 'INVALID');
  const { h: h2 } = mk({ async complete() { return { text: 'SQL, Power BI, Tableau.', model: 'm' }; } });
  const res = fakeRes();
  await h2.assist({ user, body: { kind: 'SKILLS', data: sample() } }, res);
  assert.deepEqual(res.body.skills, ['SQL', 'Power BI', 'Tableau']);
});

test('assist: a tweak and the draft reach the prompt', async () => {
  const { h, calls } = mk();
  await h.assist({ user, body: { kind: 'BULLETS', index: 0, tweak: 'NUMBERS', base: 'old draft', data: sample() } }, fakeRes());
  const usr = calls[0].messages[1].content;
  assert.match(usr, /write the bullets for JOB 1 \(Analyst at Acme\)/);
  assert.match(usr, /CURRENT DRAFT:\nold draft/);
  assert.match(usr, /square brackets/);
});

test('assist: streaming sends text events, then done with the checks', async () => {
  const { h } = mk();
  const res = fakeRes();
  await h.assist({ user, body: { kind: 'SUMMARY', stream: true, data: sample() } }, res);
  assert.equal(res.statusCode, 200);
  assert.match(res.headers['Content-Type'], /text\/event-stream/);
  const ev = res.events();
  assert.deepEqual(ev.map((e) => e[0]), ['start', 'text', 'text', 'done']);
  assert.equal(ev[2][1].text, 'Part two');
  assert.equal(ev[3][1].text, 'Part two');
  assert.deepEqual(ev[3][1].newNumbers, []);
  assert.equal(res.writableEnded, true);
});

test('streaming: an AI failure becomes an error event, not a crash', async () => {
  const { h } = mk({ async stream() { throw new AiError('RATE_LIMIT', 'HTTP 429'); } });
  const res = fakeRes();
  await h.ask({ user, body: { tab: 'Skills', stream: true, data: sample(), messages: [{ role: 'user', content: 'help' }] } }, res);
  const last = res.events().pop();
  assert.equal(last[0], 'error');
  assert.equal(last[1].error, 'RATE_LIMIT');
  assert.equal(last[1].detail, 'HTTP 429');
  assert.equal(res.writableEnded, true);
});

test('errors before the answer starts are normal JSON errors with the right status', async () => {
  const off = mk({ enabled: false });
  await rejectsWith(off.h.assist({ user, body: { kind: 'SUMMARY', data: sample() } }, fakeRes()), 503, 'NO_KEY');
  const bad = mk({ async complete() { throw new AiError('BAD_KEY', 'HTTP 401'); } });
  await rejectsWith(bad.h.assist({ user, body: { kind: 'SUMMARY', data: sample() } }, fakeRes()), 502, 'BAD_KEY');
});

test('daily limit: the request is refused with QUOTA, and a failed request still frees its slot', async () => {
  const { h, usage } = mk({ async complete() { throw new AiError('SERVER'); } });
  for (let i = 0; i < 3; i++) await assert.rejects(h.assist({ user, body: { kind: 'SUMMARY', data: sample() } }, fakeRes()));
  await rejectsWith(h.assist({ user, body: { kind: 'SUMMARY', data: sample() } }, fakeRes()), 429, 'QUOTA');
  assert.equal(usage.remaining(user._id), 0);
  const st = fakeRes();
  await h.status({ user }, st);
  assert.deepEqual(st.body, { enabled: true, remainingToday: 0, dailyLimit: 3 });
});

test('a person who leaves cancels the request to NVIDIA', async () => {
  let signal;
  const { h } = mk({ complete(o) { signal = o.signal; return new Promise((_, rej) => o.signal.addEventListener('abort', () => rej(Object.assign(new Error('aborted'), { name: 'AbortError' })))); } });
  const res = fakeRes();
  const p = h.assist({ user, body: { kind: 'SUMMARY', data: sample() } }, res);
  await new Promise((r) => setTimeout(r, 10));
  res.emit('close');
  await p;
  assert.equal(signal.aborted, true);
  assert.equal(res.body, null);
});

test('chat (generic, used by the Android app): validates and caps what it forwards', async () => {
  const { h, calls } = mk();
  const ok = fakeRes();
  await h.chat({ user, body: { messages: [{ role: 'system', content: 'Be brief' }, { role: 'user', content: 'hi' }], maxTokens: 99999, temperature: 7 } }, ok);
  assert.equal(calls[0].maxTokens, 1200);
  assert.equal(calls[0].temperature, 1);
  assert.equal(ok.body.text.startsWith('Result'), true);
  const bad = (messages) => rejectsWith(h.chat({ user, body: { messages } }, fakeRes()), 400, 'INVALID');
  await bad([]);
  await bad([{ role: 'user', content: 'a' }, { role: 'system', content: 'late system' }, { role: 'user', content: 'b' }]);
  await bad([{ role: 'user', content: 'hi' }, { role: 'assistant', content: 'last is assistant' }]);
  await bad([{ role: 'tool', content: 'x' }]);
  await bad([{ role: 'user', content: 'x'.repeat(12001) }]);
  await bad(Array.from({ length: 21 }, () => ({ role: 'user', content: 'x' })));
});

test('ask: the system prompt knows the tab and the resume; history is limited to 8 messages', async () => {
  const { h, calls } = mk();
  const history = Array.from({ length: 12 }, (_, i) => ({ role: i % 2 ? 'assistant' : 'user', content: `m${i}` }));
  history.push({ role: 'user', content: 'last question' });
  await h.ask({ user, body: { tab: 'Experience', data: sample(), messages: history } }, fakeRes());
  const msgs = calls[0].messages;
  assert.equal(msgs[0].role, 'system');
  assert.match(msgs[0].content, /"Experience" section/);
  assert.match(msgs[0].content, /JOB 1: Analyst at Acme/);
  assert.equal(msgs.length, 9);
  assert.equal(msgs[8].content, 'last question');
  await rejectsWith(h.ask({ user, body: { tab: 'x', data: sample(), messages: [{ role: 'assistant', content: 'hi' }] } }, fakeRes()), 400, 'INVALID');
});

test('ats-advice needs a job description', async () => {
  const { h, calls } = mk();
  await rejectsWith(h.atsAdvice({ user, body: { data: sample(), jobDescription: 'short' } }, fakeRes()), 400, 'INVALID');
  await h.atsAdvice({ user, body: { data: sample(), jobDescription: 'We need a data analyst with SQL, Tableau and Power BI experience.' } }, fakeRes());
  assert.match(calls[0].messages[1].content, /JOB DESCRIPTION TO MATCH:\nWe need a data analyst/);
  assert.match(calls[0].messages[1].content, /CURRENT SECTION: ATS/);
});

test('cover letter: AI text when it works, an offline draft (never an error) when it does not', async () => {
  const good = mk();
  const r1 = fakeRes();
  await good.h.coverLetter({ user, body: { data: sample(), role: 'BI Analyst', company: 'Globex', tone: 'Friendly' } }, r1);
  assert.equal(r1.body.ai, true);
  assert.match(good.calls[0].messages[1].content, /role 'BI Analyst' at 'Globex'/);
  assert.match(good.calls[0].messages[1].content, /Tone: Friendly/);

  const failing = mk({ async complete() { throw new AiError('TIMEOUT'); } });
  const r2 = fakeRes();
  await failing.h.coverLetter({ user, body: { data: sample(), company: 'Globex' } }, r2);
  assert.equal(r2.statusCode, 200);
  assert.equal(r2.body.ai, false);
  assert.match(r2.body.notice, /took too long/);
  assert.match(r2.body.text, /Dear Hiring Manager,\n\nI am writing to apply for the Data Analyst position at Globex\./);
  assert.match(r2.body.text, /Sincerely,\nAsha Rao$/);

  const off = mk({ enabled: false });
  const r3 = fakeRes();
  await off.h.coverLetter({ user, body: { data: sample() } }, r3);
  assert.equal(r3.body.ai, false);
  assert.match(r3.body.notice, /AI is off/);
  assert.equal(off.calls.length, 0);
});

test('test endpoint: one tiny request, reports the time', async () => {
  const { h, calls } = mk();
  const res = fakeRes();
  await h.test({ user }, res);
  assert.equal(res.body.ok, true);
  assert.equal(typeof res.body.ms, 'number');
  assert.equal(calls[0].maxTokens, 20);
});

test('regression: the target job survives the server (the unit separator used to be stripped on every sync)', () => {
  const US = '\u001F';
  const stored = `T1${US}Data Analyst${US}Needs SQL and Tableau`;
  const clean = sanitizeResumeData({ atsFeedback: stored, summary: `bad${US}char\u0007` });
  assert.equal(clean.atsFeedback, stored);
  assert.equal(clean.summary, 'badchar');   // every other field still loses control characters
});
