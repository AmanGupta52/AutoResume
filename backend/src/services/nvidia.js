'use strict';

/**
 * AI through NVIDIA's hosted, OpenAI-compatible API (https://build.nvidia.com). The key lives only on this server
 * (NVIDIA_API_KEY); browsers and phones talk to this backend, never to NVIDIA.
 *
 * NVIDIA retires free models every few weeks and answers HTTP 410 (or 404) for them, so a fixed model name always
 * breaks sooner or later. This service therefore:
 *  1. tries NVIDIA_MODEL first when one is set (anything but "auto"),
 *  2. asks NVIDIA which chat models are live right now (GET /v1/models) and ranks them (small and fast first),
 *  3. skips any model that answers 410/404 and moves on to the next one,
 *  4. remembers the model that worked.
 * Every failure is an AiError with a code; nothing is ever invented.
 */

const MESSAGES = {
  NO_KEY: [503, 'AI is not set up on this server. The server owner must add NVIDIA_API_KEY.'],
  NO_INTERNET: [502, 'The server could not reach the AI service. Please try again in a moment.'],
  TIMEOUT: [504, 'The AI took too long to answer. Tap Retry.'],
  BAD_KEY: [502, "The AI service did not accept the server's API key. The server owner must check NVIDIA_API_KEY."],
  RATE_LIMIT: [429, 'Too many requests right now. Wait a few seconds and tap Retry.'],
  SERVER: [502, 'The AI service is busy. Tap Retry in a moment.'],
  MODEL_GONE: [502, 'NVIDIA has retired every AI model this server tried. Please try again later.'],
  EMPTY: [502, 'The AI sent an empty answer. Tap Retry.'],
  OTHER: [502, 'Something went wrong with the AI. Tap Retry.'],
  QUOTA: [429, 'You have used all your AI requests for today. They reset tomorrow.'],
  BUSY: [429, 'Another AI request is still running. Wait for it to finish.'],
};

class AiError extends Error {
  constructor(code, detail) {
    super((MESSAGES[code] || MESSAGES.OTHER)[1]);
    this.name = 'AiError';
    this.code = MESSAGES[code] ? code : 'OTHER';
    this.status = (MESSAGES[this.code])[0];
    this.detail = detail ? String(detail).slice(0, 200) : undefined;
  }
}

const NOT_CHAT = ['embed', 'rerank', 'guard', 'safety', 'clip', 'parse', 'detector', 'translate', 'asr', 'tts', 'ocr', 'reward', 'diffusion', 'image', 'video',
  'retriev', 'jailbreak', 'topic-control', 'vlm', '-vl', 'vision', 'audio', 'speech', 'fuyu', 'kosmos', 'paligemma', 'neva', 'deplot', 'bge', 'synthetic'];
const PREFER = ['lightning', 'nano', 'instruct', 'flash', 'mini', 'small', 'gemma', 'llama', 'qwen', 'mistral', 'nemotron'];
const HUGE = /(\d{3,})b/;
/** Last resort if /v1/models cannot be read. Models seen live on integrate.api.nvidia.com in Nov 2026. */
const FALLBACK = ['nvidia/nemotron-3.5-lightning-30b-a3b', 'nvidia/nemotron-3-ultra-550b-a55b'];
const MODEL_LIST_TTL_MS = 30 * 60 * 1000;

/** Keeps chat models only and puts fast, small ones first. */
function rankModels(ids) {
  const score = (id) => {
    const l = id.toLowerCase();
    let p = PREFER.findIndex((w) => l.includes(w));
    if (p < 0) p = PREFER.length;
    const m = HUGE.exec(l);
    const size = m ? Number(m[1]) : 0;
    return p + (size >= 200 || l.includes('ultra') ? 100 : 0);
  };
  return ids.filter((id) => NOT_CHAT.every((w) => !id.toLowerCase().includes(w))).map((id, i) => ({ id, i, s: score(id) }))
    .sort((a, b) => a.s - b.s || a.i - b.i).map((x) => x.id);
}

/** Removes <think>...</think> (also an unfinished one while streaming) and stray markdown fences. */
function cleanText(s) {
  return String(s || '').replace(/<think>[\s\S]*?<\/think>/g, '').replace(/<think>[\s\S]*$/, '').replace(/```/g, '').trim();
}

function httpError(model, status, text) {
  if (status === 410 || status === 404) return new AiError('MODEL_GONE', `${model}: HTTP ${status}`);
  if (status === 401 || status === 403) return new AiError('BAD_KEY', `HTTP ${status}`);
  if (status === 429) return new AiError('RATE_LIMIT', 'HTTP 429');
  if (status >= 500) return new AiError('SERVER', `${model}: HTTP ${status}`);
  return new AiError('OTHER', `${model}: HTTP ${status} ${String(text || '').slice(0, 160)}`);
}

function networkError(e) {
  if (e instanceof AiError) return e;
  if (e && e.name === 'TimeoutError') return new AiError('TIMEOUT');
  if (e && e.name === 'AbortError') return e; // the person left: nothing to report
  return new AiError('NO_INTERNET', e && e.cause && e.cause.code ? e.cause.code : e && e.message);
}

function createNvidiaService(config, fetchImpl = globalThis.fetch) {
  const c = config.ai;
  const key = String(c.apiKey || '').trim();
  const enabled = key.length > 0 && key !== 'MY_NVIDIA_API_KEY';
  const dead = new Set();
  let working = null;
  let discovered = null;
  let discoveredAt = 0;

  const signalFor = (outer) => {
    const t = AbortSignal.timeout(c.timeoutMs);
    return outer ? AbortSignal.any([t, outer]) : t;
  };
  const headers = (accept) => ({ Authorization: `Bearer ${key}`, 'Content-Type': 'application/json', Accept: accept });

  async function discoverModels(outer) {
    if (discovered && Date.now() - discoveredAt < MODEL_LIST_TTL_MS) return discovered;
    try {
      const res = await fetchImpl(`${c.baseUrl}/models`, { headers: headers('application/json'), signal: signalFor(outer) });
      if (res.ok) {
        const json = await res.json();
        const ids = (Array.isArray(json.data) ? json.data : []).map((m) => (m && typeof m.id === 'string' ? m.id : '')).filter(Boolean);
        const ranked = rankModels(ids);
        if (ranked.length) { discovered = ranked; discoveredAt = Date.now(); }
        return ranked;
      }
    } catch (e) {
      if (outer && outer.aborted) throw e;
      /* no list: the fallback models are used instead */
    }
    return [];
  }

  async function candidates(outer) {
    const list = new Set();
    if (working) list.add(working);
    if (c.model) list.add(c.model);
    (await discoverModels(outer)).forEach((m) => list.add(m));
    FALLBACK.forEach((m) => list.add(m));
    return [...list].filter((m) => !dead.has(m)).slice(0, 6);
  }

  /** Tries the candidate models in order; a retired one (410/404) is skipped and remembered as dead. */
  async function withModels(outer, attempt) {
    if (!enabled) throw new AiError('NO_KEY');
    let last = new AiError('MODEL_GONE');
    for (let round = 0; round < 2; round++) {
      for (const model of await candidates(outer)) {
        try {
          const r = await attempt(model);
          working = model;
          return r;
        } catch (e) {
          if (e instanceof AiError && e.code === 'MODEL_GONE') { dead.add(model); last = e; continue; }
          throw e;
        }
      }
      discovered = null; // everything we knew is dead: read the live list again once
    }
    throw last;
  }

  const bodyFor = (model, messages, maxTokens, temperature, stream) => JSON.stringify({
    model,
    // extra room because some models spend tokens on hidden reasoning before the visible answer
    max_tokens: Math.min(Math.max(Number(maxTokens) || 700, 16), 2000) + 1200,
    temperature: Math.min(Math.max(Number(temperature) || 0, 0), 1),
    stream,
    messages: messages.map((m) => ({ role: m.role, content: m.content })),
  });

  /** The whole answer at once. @returns {Promise<{text:string, model:string}>} */
  async function complete({ messages, maxTokens = 700, temperature = 0.4, signal }) {
    return withModels(signal, async (model) => {
      let res;
      try {
        res = await fetchImpl(`${c.baseUrl}/chat/completions`, { method: 'POST', headers: headers('application/json'), body: bodyFor(model, messages, maxTokens, temperature, false), signal: signalFor(signal) });
        if (!res.ok) throw httpError(model, res.status, await res.text().catch(() => ''));
        const json = await res.json();
        const msg = json && json.choices && json.choices[0] && json.choices[0].message;
        const text = cleanText(msg && typeof msg.content === 'string' ? msg.content : '');
        if (!text) throw new AiError('EMPTY', model);
        return { text, model };
      } catch (e) {
        throw networkError(e);
      }
    });
  }

  /**
   * Streams the answer. `onText` receives the full cleaned text so far, every time it grows.
   * @returns {Promise<{text:string, model:string}>}
   */
  async function stream({ messages, maxTokens = 700, temperature = 0.4, onText, signal }) {
    return withModels(signal, async (model) => {
      try {
        const res = await fetchImpl(`${c.baseUrl}/chat/completions`, { method: 'POST', headers: headers('text/event-stream'), body: bodyFor(model, messages, maxTokens, temperature, true), signal: signalFor(signal) });
        if (!res.ok) throw httpError(model, res.status, await res.text().catch(() => ''));
        const decoder = new TextDecoder();
        let raw = '';
        let buffer = '';
        let finished = false;
        const handleLine = (line) => {
          if (!line.startsWith('data:')) return;
          const data = line.slice(5).trim();
          if (data === '[DONE]') { finished = true; return; }
          let delta = '';
          try {
            const j = JSON.parse(data);
            const d = j && j.choices && j.choices[0] && j.choices[0].delta;
            delta = d && typeof d.content === 'string' ? d.content : '';
          } catch { /* a partial or odd line: skip it */ }
          if (delta) { raw += delta; if (onText) onText(cleanText(raw)); }
        };
        for await (const chunk of res.body) {
          buffer += decoder.decode(chunk, { stream: true });
          let nl;
          while ((nl = buffer.indexOf('\n')) >= 0) { handleLine(buffer.slice(0, nl).replace(/\r$/, '')); buffer = buffer.slice(nl + 1); }
          if (finished) break;
        }
        if (buffer) handleLine(buffer.replace(/\r$/, ''));
        const text = cleanText(raw);
        if (!text) throw new AiError('EMPTY', model);
        return { text, model };
      } catch (e) {
        throw networkError(e);
      }
    });
  }

  return { enabled, complete, stream, info: () => ({ enabled, model: working, pinned: c.model || null }) };
}

module.exports = { createNvidiaService, AiError, rankModels, cleanText, FALLBACK };
