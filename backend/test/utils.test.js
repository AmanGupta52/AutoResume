'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const { generateCode, hashCode, safeEqualHex } = require('../src/utils/otp');
const { isEmail, normalizeEmail, passwordProblem, cleanName, isCode } = require('../src/utils/validate');
const { sanitizeResumeData, cleanTitle } = require('../src/utils/resumeSchema');
const { loadConfig, configProblems } = require('../src/config');
const { createEmailService } = require('../src/services/email');

test('codes are 6 digits and verify only with the same email, purpose and code', () => {
  for (let i = 0; i < 50; i++) assert.match(generateCode(), /^\d{6}$/);
  const h = hashCode('secret', 'a@b.co', 'verify', '123456');
  assert.ok(safeEqualHex(h, hashCode('secret', 'a@b.co', 'verify', '123456')));
  assert.ok(!safeEqualHex(h, hashCode('secret', 'a@b.co', 'verify', '123457')));
  assert.ok(!safeEqualHex(h, hashCode('secret', 'a@b.co', 'reset', '123456')));
  assert.ok(!safeEqualHex(h, hashCode('secret', 'x@b.co', 'verify', '123456')));
  assert.ok(!safeEqualHex('', ''));
});

test('email and password rules', () => {
  assert.equal(normalizeEmail('  Me@Mail.COM '), 'me@mail.com');
  assert.ok(isEmail('me@mail.com'));
  assert.ok(!isEmail('me@mail'));
  assert.ok(!isEmail('no spaces@mail.com'));
  assert.ok(passwordProblem('short1'));
  assert.ok(passwordProblem('onlyletters'));
  assert.ok(passwordProblem('12345678'));
  assert.ok(passwordProblem('a1' + 'x'.repeat(80)));
  assert.equal(passwordProblem('goodpass1'), null);
  assert.equal(cleanName('  <b>Asha</b>\n'), 'bAsha/b');
  assert.ok(isCode('012345'));
  assert.ok(!isCode('12345'));
  assert.ok(!isCode('12345a'));
});

test('resume data keeps only known fields and caps sizes', () => {
  const dirty = {
    fullName: 'Asha Rao', summary: 'x'.repeat(5000), avatarUri: 'file:///secret', __proto__: { evil: 1 }, evil: 'x',
    experience: [{ company: 'Acme', role: 'Dev', isCurrent: 'yes', description: 'a\u0000b', extra: 1 }, 'not an object'],
    skills: new Array(100).fill({ name: 'JS', level: 'Expert' }),
    atsScore: 999,
  };
  const c = sanitizeResumeData(dirty);
  assert.equal(c.fullName, 'Asha Rao');
  assert.equal(c.summary.length, 3000);
  assert.ok(!('avatarUri' in c) && !('evil' in c));
  assert.equal(c.experience.length, 2);
  assert.equal(c.experience[0].isCurrent, false);
  assert.equal(c.experience[0].description, 'ab');
  assert.ok(!('extra' in c.experience[0]));
  assert.equal(c.experience[1].company, '');
  assert.equal(c.skills.length, 40);
  assert.equal(c.atsScore, 100);
  assert.deepEqual(sanitizeResumeData(null).education, []);
  assert.equal(sanitizeResumeData('x').fullName, '');
  assert.equal(cleanTitle('  My   resume \n'), 'My resume');
});

test('config problems are reported', () => {
  const ok = loadConfig({ MONGODB_URI: 'mongodb://x', JWT_SECRET: 'a'.repeat(40) });
  assert.deepEqual(configProblems(ok), []);
  assert.ok(configProblems(loadConfig({})).length >= 2);
  assert.ok(configProblems(loadConfig({ MONGODB_URI: 'm', JWT_SECRET: 'change-me' + 'x'.repeat(30) })).some((p) => p.includes('example')));
  const prod = loadConfig({ NODE_ENV: 'production', MONGODB_URI: 'm', JWT_SECRET: 'a'.repeat(40) });
  assert.ok(configProblems(prod).some((p) => p.includes('EMAIL_MODE')));
  const mail = loadConfig({ EMAIL_MODE: 'emailjs', MONGODB_URI: 'm', JWT_SECRET: 'a'.repeat(40) });
  assert.equal(configProblems(mail).filter((p) => p.includes('EMAILJS_')).length, 4);
});

test('EmailJS request has the keys and template variables, and errors are surfaced', async () => {
  const config = loadConfig({ EMAIL_MODE: 'emailjs', EMAILJS_SERVICE_ID: 's', EMAILJS_TEMPLATE_ID: 't', EMAILJS_PUBLIC_KEY: 'pub', EMAILJS_PRIVATE_KEY: 'priv' });
  let sent;
  const ok = createEmailService(config, async (url, init) => { sent = { url, body: JSON.parse(init.body) }; return { ok: true }; });
  await ok.sendCode({ to: 'a@b.co', name: 'Asha', code: '123456', purpose: 'verify' });
  assert.equal(sent.url, 'https://api.emailjs.com/api/v1.0/email/send');
  assert.equal(sent.body.service_id, 's');
  assert.equal(sent.body.user_id, 'pub');
  assert.equal(sent.body.accessToken, 'priv');
  assert.equal(sent.body.template_params.code, '123456');
  assert.equal(sent.body.template_params.to_email, 'a@b.co');
  const bad = createEmailService(config, async () => ({ ok: false, status: 403, text: async () => 'API calls are disabled for non-browser applications' }));
  await assert.rejects(bad.sendCode({ to: 'a@b.co', name: 'A', code: '1', purpose: 'reset' }), /EmailJS 403/);
  // console mode never calls the network
  const consoleSvc = createEmailService(loadConfig({}), async () => { throw new Error('must not be called'); });
  const orig = console.log; console.log = () => {};
  await consoleSvc.sendCode({ to: 'a@b.co', name: 'A', code: '000000', purpose: 'verify' });
  console.log = orig;
});
