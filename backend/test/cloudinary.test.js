'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const crypto = require('node:crypto');
const { loadConfig, configProblems } = require('../src/config');
const { createCloudinaryService } = require('../src/services/cloudinary');

const cfg = (extra = {}) => loadConfig({ CLOUDINARY_CLOUD_NAME: 'democloud', CLOUDINARY_API_KEY: 'key123', CLOUDINARY_API_SECRET: 'sec', ...extra });
const fakePdf = Buffer.from('%PDF-1.4\n' + 'x'.repeat(200));

test('Cloudinary settings: three variables, the one-line URL, and half-configured', () => {
  assert.equal(cfg().cloudinary.cloudName, 'democloud');
  const fromUrl = loadConfig({ CLOUDINARY_URL: 'cloudinary://111:abc_DEF@mycloud' }).cloudinary;
  assert.deepEqual([fromUrl.cloudName, fromUrl.apiKey, fromUrl.apiSecret], ['mycloud', '111', 'abc_DEF']);
  assert.equal(loadConfig({ CLOUDINARY_FOLDER: '../evil folder/' }).cloudinary.folder, 'evilfolder');
  assert.deepEqual(configProblems(loadConfig({ MONGODB_URI: 'm', JWT_SECRET: 'a'.repeat(40) })), []); // fully off is fine
  assert.ok(configProblems(loadConfig({ MONGODB_URI: 'm', JWT_SECRET: 'a'.repeat(40), CLOUDINARY_CLOUD_NAME: 'x' })).some((p) => p.includes('half configured')));
});

test('signature = sha1 of the sorted parameters plus the secret', () => {
  const svc = createCloudinaryService(cfg());
  const expected = crypto.createHash('sha1').update('public_id=a/b.pdf&timestamp=1700000000sec').digest('hex');
  assert.equal(svc.sign({ timestamp: 1700000000, public_id: 'a/b.pdf', file: undefined, empty: '' }), expected);
});

test('public ids are inside the folder, per user and resume, and never repeat', () => {
  const svc = createCloudinaryService(cfg({ CLOUDINARY_FOLDER: 'autoresume' }));
  const a = svc.newPublicId('u1', 'r1');
  assert.match(a, /^autoresume\/u1-r1-[0-9a-f]{24}\.pdf$/); // one flat folder, no per-user sub-folders
  assert.notEqual(a, svc.newPublicId('u1', 'r1'));
});

test('upload sends a signed form to the raw endpoint and returns the https link', async () => {
  let call;
  const svc = createCloudinaryService(cfg(), async (url, init) => {
    call = { url, form: init.body };
    return { ok: true, json: async () => ({ secure_url: 'https://res.cloudinary.com/democloud/raw/upload/v1/autoresume/u/r.pdf', public_id: 'autoresume/u/r.pdf', bytes: 321 }) };
  });
  const out = await svc.uploadPdf({ buffer: fakePdf, publicId: 'autoresume/u/r.pdf' });
  assert.equal(call.url, 'https://api.cloudinary.com/v1_1/democloud/raw/upload');
  assert.equal(call.form.get('api_key'), 'key123');
  assert.equal(call.form.get('public_id'), 'autoresume/u/r.pdf');
  const ts = call.form.get('timestamp');
  assert.equal(call.form.get('asset_folder'), 'autoresume'); // dynamic-folder accounts file it here
  assert.equal(call.form.get('signature'), svc.sign({ public_id: 'autoresume/u/r.pdf', timestamp: ts, asset_folder: 'autoresume' }));
  assert.ok(!JSON.stringify([...call.form.keys()]).includes('secret'));
  assert.equal(call.form.get('file').size, fakePdf.length);
  assert.deepEqual(out, { url: 'https://res.cloudinary.com/democloud/raw/upload/v1/autoresume/u/r.pdf', publicId: 'autoresume/u/r.pdf', bytes: 321 });
});

test('upload refuses links that are not Cloudinary and reports storage errors', async () => {
  const evil = createCloudinaryService(cfg(), async () => ({ ok: true, json: async () => ({ secure_url: 'https://evil.example/x.pdf', public_id: 'p' }) }));
  await assert.rejects(evil.uploadPdf({ buffer: fakePdf, publicId: 'p' }), (e) => e.status === 502);
  const down = createCloudinaryService(cfg(), async () => ({ ok: false, status: 401, json: async () => ({ error: { message: 'Invalid api_key' } }) }));
  const orig = console.error; console.error = () => {};
  await assert.rejects(down.uploadPdf({ buffer: fakePdf, publicId: 'p' }), (e) => e.status === 502 && e.code === 'STORAGE_FAILED');
  const offline = createCloudinaryService(cfg(), async () => { throw new Error('ENOTFOUND'); });
  await assert.rejects(offline.uploadPdf({ buffer: fakePdf, publicId: 'p' }), (e) => e.status === 502);
  console.error = orig;
});

test('without settings the service is off and says so', async () => {
  const svc = createCloudinaryService(loadConfig({}), async () => { throw new Error('must not be called'); });
  assert.equal(svc.enabled, false);
  await assert.rejects(svc.uploadPdf({ buffer: fakePdf, publicId: 'p' }), (e) => e.status === 503 && e.code === 'PDF_STORAGE_OFF');
  await svc.destroy('anything'); // quietly does nothing
});

test('destroy is signed and asks Cloudinary to invalidate the cached link', async () => {
  let call;
  const svc = createCloudinaryService(cfg(), async (url, init) => { call = { url, form: init.body }; return { ok: true, json: async () => ({ result: 'ok' }) }; });
  await svc.destroy('autoresume/u/r.pdf');
  assert.equal(call.url, 'https://api.cloudinary.com/v1_1/democloud/raw/destroy');
  assert.equal(call.form.get('signature'), svc.sign({ public_id: 'autoresume/u/r.pdf', timestamp: call.form.get('timestamp'), invalidate: 'true' }));
});

test('an account that rejects asset_folder gets one retry without it', async () => {
  const forms = [];
  const svc = createCloudinaryService(cfg(), async (url, init) => {
    forms.push(init.body);
    if (forms.length === 1) return { ok: false, status: 400, json: async () => ({ error: { message: 'Invalid parameter asset_folder' } }) };
    return { ok: true, json: async () => ({ secure_url: 'https://res.cloudinary.com/democloud/raw/upload/v1/autoresume/u/r.pdf', public_id: 'autoresume/u/r.pdf', bytes: 5 }) };
  });
  const orig = console.error; console.error = () => {};
  const out = await svc.uploadPdf({ buffer: fakePdf, publicId: 'autoresume/u/r.pdf' });
  console.error = orig;
  assert.equal(forms.length, 2);
  assert.equal(forms[1].get('asset_folder'), null);
  assert.equal(out.publicId, 'autoresume/u/r.pdf');
});
