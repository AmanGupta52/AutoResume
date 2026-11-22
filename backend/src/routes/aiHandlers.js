'use strict';
const { HttpError } = require('../middleware/error');
const { AiError } = require('../services/nvidia');
const { sanitizeResumeData } = require('../utils/resumeSchema');
const { loadShared } = require('../utils/shared');

const clampInt = (v, min, max, fallback) => {
  const n = Math.floor(Number(v));
  return Number.isFinite(n) ? Math.min(Math.max(n, min), max) : fallback;
};
const str = (v, max) => (typeof v === 'string' ? v.replace(/[\u0000-\u0008\u000B\u000C\u000E-\u001F\u007F]/g, '').slice(0, max) : '');
const invalid = (msg) => new HttpError(400, 'INVALID', msg);
const toHttp = (e) => (e instanceof AiError ? new HttpError(e.status, e.code, e.message, e.detail ? { detail: e.detail } : {}) : e);
const isAbort = (e) => e && e.name === 'AbortError';

/**
 * The AI endpoints. All of them need a login, count against the person's daily allowance and answer either as one
 * JSON object or, with `"stream": true`, as server-sent events:
 *   event: text   data: {"text": "<answer so far>"}
 *   event: done   data: {"text": "...", "model": "...", ...extra}
 *   event: error  data: {"error": "RATE_LIMIT", "message": "...", "detail": "..."}
 * (Problems found before the answer starts, such as a bad request or the daily limit, are normal JSON errors.)
 *
 * The handlers use only `req.body`, `req.user` and a few `res` methods, so they are tested without a web server.
 */
function createAiHandlers({ ai, usage }) {
  /** Runs one model request: counts it, answers as JSON or as a stream, never leaves a request counted. */
  async function run(req, res, { messages, maxTokens, temperature = 0.4, stream, extra }) {
    let release;
    try {
      if (!ai.enabled) throw new AiError('NO_KEY');
      release = usage.acquire(req.user._id);
    } catch (e) { throw toHttp(e); }

    const ac = new AbortController();
    const onClose = () => { if (!res.writableFinished) ac.abort(); };
    res.on('close', onClose);
    try {
      if (!stream) {
        const out = await ai.complete({ messages, maxTokens, temperature, signal: ac.signal });
        res.json({ text: out.text, model: out.model, ...(extra ? await extra(out.text) : {}) });
        return;
      }
      res.writeHead(200, { 'Content-Type': 'text/event-stream; charset=utf-8', 'Cache-Control': 'no-cache, no-transform', Connection: 'keep-alive', 'X-Accel-Buffering': 'no' });
      const send = (event, data) => { if (!res.writableEnded) res.write(`event: ${event}\ndata: ${JSON.stringify(data)}\n\n`); };
      send('start', {});
      try {
        const out = await ai.stream({ messages, maxTokens, temperature, signal: ac.signal, onText: (t) => send('text', { text: t }) });
        send('done', { text: out.text, model: out.model, ...(extra ? await extra(out.text) : {}) });
      } catch (e) {
        if (!isAbort(e)) {
          const ae = e instanceof AiError ? e : new AiError('OTHER');
          send('error', { error: ae.code, message: ae.message, ...(ae.detail ? { detail: ae.detail } : {}) });
        }
      } finally {
        if (!res.writableEnded) res.end();
      }
    } catch (e) {
      if (isAbort(e)) return;
      throw toHttp(e);
    } finally {
      res.off('close', onClose);
      release();
    }
  }

  const resumeData = (req) => {
    if (!req.body || typeof req.body.data !== 'object' || req.body.data === null || Array.isArray(req.body.data)) throw invalid('Send the resume as "data".');
    return sanitizeResumeData(req.body.data);
  };

  return {
    async status(req, res) {
      res.json({ enabled: ai.enabled, remainingToday: usage.remaining(req.user._id), dailyLimit: usage.dailyLimit });
    },

    /** Settings > Test AI connection: one tiny request that shows exactly why AI fails, if it does. */
    async test(req, res) {
      const started = Date.now();
      await run(req, res, { messages: [{ role: 'system', content: 'Reply with the single word OK.' }, { role: 'user', content: 'ping' }], maxTokens: 20, temperature: 0, stream: false,
        extra: async () => ({ ok: true, ms: Date.now() - started }) });
    },

    /** Generic chat for clients that build their own prompts (the Android app). Limits keep it from becoming a free chatbot. */
    async chat(req, res) {
      const b = req.body || {};
      const raw = Array.isArray(b.messages) ? b.messages : [];
      if (raw.length < 1 || raw.length > 20) throw invalid('Send between 1 and 20 messages.');
      let total = 0;
      const messages = raw.map((m, i) => {
        const role = m && m.role;
        const content = m && typeof m.content === 'string' ? m.content : '';
        if (!['system', 'user', 'assistant'].includes(role) || !content.trim()) throw invalid('Every message needs a role and some text.');
        if (role === 'system' && i !== 0) throw invalid('Only the first message can be a system message.');
        if (content.length > 12000) throw invalid('One of the messages is too long.');
        total += content.length;
        return { role, content: str(content, 12000) };
      });
      if (total > 30000) throw invalid('That is too much text for one request.');
      if (messages[messages.length - 1].role !== 'user') throw invalid('The last message must come from the user.');
      await run(req, res, { messages, maxTokens: clampInt(b.maxTokens, 16, 1200, 700), temperature: Math.min(Math.max(Number(b.temperature) || 0.4, 0), 1), stream: b.stream === true });
    },

    /** Summary, achievement bullets, skills, or fixes for the writing check, for one resume. */
    async assist(req, res) {
      const { prompts } = await loadShared();
      const b = req.body || {};
      const kind = b.kind;
      if (!prompts.KINDS.includes(kind)) throw invalid('Unknown request kind.');
      const data = resumeData(req);
      const index = clampInt(b.index, 0, 39, -1);
      if (kind === 'BULLETS') {
        const job = data.experience[index];
        if (!job) throw invalid('Choose one of the jobs.');
        if (!String(job.role).trim()) throw invalid('Add the job title for this job first.');
      }
      const tweak = typeof b.tweak === 'string' && prompts.TWEAKS[b.tweak] ? b.tweak : undefined;
      const tab = prompts.isTab(b.tab) ? b.tab : prompts.tabForKind(kind);
      const messages = [
        { role: 'system', content: prompts.systemPrompt(kind) },
        { role: 'user', content: prompts.userPrompt({ kind, index, tweak, tab, base: str(b.base, 6000) }, data) },
      ];
      await run(req, res, {
        messages, maxTokens: kind === 'ADVICE' ? 900 : 450, stream: b.stream === true,
        extra: async (text) => ({ newNumbers: prompts.newNumbers(text, data), hasPlaceholders: prompts.hasPlaceholders(text), ...(kind === 'SKILLS' ? { skills: prompts.parseSkills(text) } : {}) }),
      });
    },

    /** The "Ask AI" helper: it always knows the open tab, the target job and the whole resume. */
    async ask(req, res) {
      const { prompts } = await loadShared();
      const b = req.body || {};
      const data = resumeData(req);
      const tab = prompts.isTab(b.tab) ? b.tab : 'Basics';
      const history = (Array.isArray(b.messages) ? b.messages : []).slice(-8).map((m) => {
        const role = m && m.role;
        const content = str(m && m.content, 4000);
        if (!['user', 'assistant'].includes(role) || !content.trim()) throw invalid('Every message needs a role and some text.');
        return { role, content };
      });
      if (!history.length || history[history.length - 1].role !== 'user') throw invalid('The last message must come from the user.');
      await run(req, res, { messages: [{ role: 'system', content: prompts.chatSystem(tab, data) }, ...history], maxTokens: 600, temperature: 0.5, stream: b.stream === true });
    },

    /** Rewritten summary and bullets that match a pasted job description. */
    async atsAdvice(req, res) {
      const { prompts } = await loadShared();
      const data = resumeData(req);
      const jd = str(req.body.jobDescription, 6000).trim();
      if (jd.length < 20) throw invalid('Paste the job description first.');
      await run(req, res, { messages: [{ role: 'system', content: prompts.ATS_ADVICE_SYSTEM }, { role: 'user', content: prompts.atsAdviceUser(data, jd) }], maxTokens: 900, stream: req.body.stream === true });
    },

    /** AI cover letter; when AI is off or fails, a fill-in draft from the resume (never a failure). */
    async coverLetter(req, res) {
      const { prompts, resume } = await loadShared();
      const b = req.body || {};
      const data = resumeData(req);
      const opts = { role: str(b.role, 160), company: str(b.company, 160), manager: str(b.manager, 160), tone: str(b.tone, 20), jobDescription: str(b.jobDescription, 6000) };
      const offline = (notice) => {
        const role = opts.role.trim() || resume.targetOf(data).title.trim() || String(data.jobTitle || '').trim();
        res.json({ text: prompts.offlineCoverLetter(data, role, opts.company, opts.manager), ai: false, notice });
      };
      let release;
      try {
        if (!ai.enabled) throw new AiError('NO_KEY');
        release = usage.acquire(req.user._id);
      } catch (e) {
        return offline(e.code === 'NO_KEY' ? 'AI is off, so here is a fill-in draft from your resume. Edit it before sending.' : `${e.message} Here is a basic draft you can edit.`);
      }
      const ac = new AbortController();
      const onClose = () => { if (!res.writableFinished) ac.abort(); };
      res.on('close', onClose);
      try {
        const out = await ai.complete({ messages: [{ role: 'system', content: prompts.COVER_SYSTEM }, { role: 'user', content: prompts.coverUser(data, opts) }], maxTokens: 700, temperature: 0.5, signal: ac.signal });
        res.json({ text: out.text, ai: true, model: out.model });
      } catch (e) {
        if (isAbort(e)) return;
        offline(`${e instanceof AiError ? e.message : 'AI did not answer.'} Here is a basic draft you can edit.`);
      } finally {
        res.off('close', onClose);
        release();
      }
    },
  };
}

module.exports = { createAiHandlers };
