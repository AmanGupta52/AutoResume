'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const { loadConfig, configWarnings } = require('../src/config');
const { createNvidiaService, AiError, rankModels, cleanText } = require('../src/services/nvidia');
const { createUsageLimiter } = require('../src/services/usage');

const cfg = (env = {}) => loadConfig({ NVIDIA_API_KEY: 'nvapi-test', ...env });
const json = (obj, status = 200) => new Response(JSON.stringify(obj), { status, headers: { 'Content-Type': 'application/json' } });
const MODELS = { data: [{ id: 'nvidia/nv-embed-v1' }, { id: 'old/retired-instruct' }, { id: 'meta/llama-3.1-8b-instruct' }, { id: 'nvidia/nemotron-3-ultra-550b-a55b' }] };
const chatOk = (text) => json({ choices: [{ message: { content: text } }] });
const sse = (...deltas) => new Response(deltas.map((d) => `data: ${JSON.stringify({ choices: [{ delta: { content: d } }] })}\n\n`).join('') + 'data: [DONE]\n\n', { status: 200 });

test('config: key is cleaned, "auto" means no pinned model, defaults are sane', () => {
  const c = cfg({ NVIDIA_API_KEY: ' "nvapi-abc" ', NVIDIA_MODEL: 'AUTO' });
  assert.equal(c.ai.apiKey, 'nvapi-abc');
  assert.equal(c.ai.model, '');
  assert.equal(c.ai.baseUrl, 'https://integrate.api.nvidia.com/v1');
  assert.equal(c.ai.dailyLimit, 150);
  assert.equal(cfg({ NVIDIA_MODEL: 'meta/x' }).ai.model, 'meta/x');
  assert.deepEqual(configWarnings(loadConfig({ NVIDIA_API_KEY: 'wrong' })).length, 1);
  assert.deepEqual(configWarnings(cfg()), []);
});

test('ranking keeps chat models, small and fast first; <think> is stripped', () => {
  const ranked = rankModels(MODELS.data.map((m) => m.id));
  assert.ok(!ranked.includes('nvidia/nv-embed-v1'));
  assert.equal(ranked[ranked.length - 1], 'nvidia/nemotron-3-ultra-550b-a55b');
  assert.equal(cleanText('<think>secret</think>Hello ```x```'), 'Hello x');
  assert.equal(cleanText('Hi <think>unfinished'), 'Hi');
});

test('without a key the service says so and never calls NVIDIA', async () => {
  let calls = 0;
  const ai = createNvidiaService(loadConfig({}), async () => { calls++; return chatOk('x'); });
  assert.equal(ai.enabled, false);
  await assert.rejects(() => ai.complete({ messages: [{ role: 'user', content: 'hi' }] }), (e) => e instanceof AiError && e.code === 'NO_KEY' && e.status === 503);
  assert.equal(calls, 0);
});

test('a retired model (410) is skipped, the next one is used and remembered', async () => {
  const tried = [];
  const f = async (url, opts) => {
    if (url.endsWith('/models')) return json(MODELS);
    const body = JSON.parse(opts.body);
    tried.push(body.model);
    assert.equal(opts.headers.Authorization, 'Bearer nvapi-test');
    assert.equal(body.stream, false);
    assert.ok(body.max_tokens >= 1200);
    return body.model === 'old/retired-instruct' ? new Response('gone', { status: 410 }) : chatOk('<think>hm</think>Fine answer');
  };
  const ai = createNvidiaService(cfg(), f);
  const out = await ai.complete({ messages: [{ role: 'user', content: 'hi' }] });
  assert.equal(out.text, 'Fine answer');
  assert.equal(out.model, 'meta/llama-3.1-8b-instruct');
  assert.deepEqual(tried, ['old/retired-instruct', 'meta/llama-3.1-8b-instruct']);
  tried.length = 0;
  await ai.complete({ messages: [{ role: 'user', content: 'again' }] });
  assert.equal(tried.length, 1);           // goes straight to the model that worked
  assert.equal(tried[0], out.model);
  assert.equal(ai.info().model, out.model);
});

test('a pinned NVIDIA_MODEL is tried first', async () => {
  const tried = [];
  const f = async (url, opts) => { if (url.endsWith('/models')) return json(MODELS); tried.push(JSON.parse(opts.body).model); return chatOk('ok'); };
  await createNvidiaService(cfg({ NVIDIA_MODEL: 'my/pinned-model' }), f).complete({ messages: [{ role: 'user', content: 'hi' }] });
  assert.equal(tried[0], 'my/pinned-model');
});

test('if the model list cannot be read the built-in fallback models are used', async () => {
  const tried = [];
  const f = async (url, opts) => { if (url.endsWith('/models')) throw new TypeError('fetch failed'); tried.push(JSON.parse(opts.body).model); return chatOk('ok'); };
  await createNvidiaService(cfg(), f).complete({ messages: [{ role: 'user', content: 'hi' }] });
  assert.equal(tried[0], 'nvidia/nemotron-3.5-lightning-30b-a3b');
});

test('every failure becomes a typed error', async () => {
  const run = (status, body = 'x') => createNvidiaService(cfg(), async (url) => (url.endsWith('/models') ? json(MODELS) : new Response(body, { status })))
    .complete({ messages: [{ role: 'user', content: 'hi' }] });
  await assert.rejects(() => run(401), (e) => e.code === 'BAD_KEY' && e.status === 502 && !/nvapi/.test(e.message));
  await assert.rejects(() => run(403), (e) => e.code === 'BAD_KEY');
  await assert.rejects(() => run(429), (e) => e.code === 'RATE_LIMIT' && e.status === 429);
  await assert.rejects(() => run(503), (e) => e.code === 'SERVER');
  await assert.rejects(() => run(400, 'bad request body'), (e) => e.code === 'OTHER' && /400/.test(e.detail));
  await assert.rejects(() => run(410), (e) => e.code === 'MODEL_GONE');          // every model retired
  const empty = createNvidiaService(cfg(), async (url) => (url.endsWith('/models') ? json(MODELS) : chatOk('<think>only thoughts</think>')));
  await assert.rejects(() => empty.complete({ messages: [{ role: 'user', content: 'hi' }] }), (e) => e.code === 'EMPTY');
  const offline = createNvidiaService(cfg(), async () => { throw new TypeError('fetch failed', { cause: { code: 'ENOTFOUND' } }); });
  await assert.rejects(() => offline.complete({ messages: [{ role: 'user', content: 'hi' }] }), (e) => e.code === 'NO_INTERNET' && e.detail === 'ENOTFOUND');
  const keepAlive = setInterval(() => {}, 50); // AbortSignal.timeout does not keep a bare test process alive (a real server is)
  const slow = createNvidiaService(cfg({ AI_TIMEOUT_MS: '30' }), (url, opts) => new Promise((_, reject) => opts.signal.addEventListener('abort', () => reject(opts.signal.reason))));
  await assert.rejects(() => slow.complete({ messages: [{ role: 'user', content: 'hi' }] }), (e) => e.code === 'TIMEOUT' && e.status === 504);
  clearInterval(keepAlive);
});

test('streaming sends the cleaned answer so far and returns the final text', async () => {
  const f = async (url, opts) => {
    if (url.endsWith('/models')) return json(MODELS);
    assert.equal(JSON.parse(opts.body).stream, true);
    assert.equal(opts.headers.Accept, 'text/event-stream');
    return sse('<think>plan', ' it</think>Hel', 'lo ', 'world');
  };
  const seen = [];
  const out = await createNvidiaService(cfg(), f).stream({ messages: [{ role: 'user', content: 'hi' }], onText: (t) => seen.push(t) });
  assert.equal(out.text, 'Hello world');
  assert.deepEqual(seen, ['', 'Hel', 'Hello', 'Hello world']);
});

test('streaming handles lines split across chunks and ignores noise', async () => {
  const enc = new TextEncoder();
  const parts = ['data: {"choices":[{"delta":{"content":"A"}}]}\n', '\n: keep-alive\n\ndata: {"choices":[{"del', 'ta":{"content":"B"}}]}\n\ndata: not json\n\ndata: {"choices":[{"delta":{"content":null}}]}\n\ndata: [DONE]\n\n'];
  const body = new ReadableStream({ start(c) { parts.forEach((p) => c.enqueue(enc.encode(p))); c.close(); } });
  const f = async (url) => (url.endsWith('/models') ? json(MODELS) : new Response(body, { status: 200 }));
  const out = await createNvidiaService(cfg(), f).stream({ messages: [{ role: 'user', content: 'hi' }] });
  assert.equal(out.text, 'AB');
});

test('a stream that starts with an HTTP error fails with the typed error', async () => {
  const f = async (url) => (url.endsWith('/models') ? json(MODELS) : new Response('nope', { status: 429 }));
  await assert.rejects(() => createNvidiaService(cfg(), f).stream({ messages: [{ role: 'user', content: 'hi' }] }), (e) => e.code === 'RATE_LIMIT');
});

test('usage limiter: daily cap, parallel cap, release, new day', () => {
  let day = '2026-11-21T10:00:00Z';
  const u = createUsageLimiter({ dailyLimit: 5, maxParallel: 2, now: () => new Date(day) });
  const a = u.acquire('u1');
  const b = u.acquire('u1');
  assert.throws(() => u.acquire('u1'), (e) => e.code === 'BUSY' && e.status === 429);
  a(); a();                                  // releasing twice must not free a second slot
  const c = u.acquire('u1');
  assert.throws(() => u.acquire('u1'), (e) => e.code === 'BUSY');
  b(); c();
  u.acquire('u1')(); u.acquire('u1')();      // 5 requests used today
  assert.equal(u.remaining('u1'), 0);
  assert.throws(() => u.acquire('u1'), (e) => e.code === 'QUOTA' && e.status === 429);
  assert.ok(u.acquire('u2'));                // other people are not affected
  day = '2026-11-22T00:05:00Z';
  assert.equal(u.remaining('u1'), 5);
  assert.ok(u.acquire('u1'));
});
