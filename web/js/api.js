// Talks to the backend. The login token lives in localStorage; every call sends it as a Bearer header.

// Backend address comes from config.js (window.AUTORESUME_CONFIG.API_BASE). Empty = same origin (backend serves the web app).
const CFG = (typeof window !== 'undefined' && window.AUTORESUME_CONFIG) || {};
export const API_BASE = String(CFG.API_BASE || '').trim().replace(/\/+$/, '');
const API = API_BASE + '/api';

const TOKEN = 'autoresume_token';
const USER = 'autoresume_user';

export const session = {
  get token() { try { return localStorage.getItem(TOKEN) || ''; } catch { return ''; } },
  get user() { try { return JSON.parse(localStorage.getItem(USER) || 'null'); } catch { return null; } },
  set(token, user) { localStorage.setItem(TOKEN, token); localStorage.setItem(USER, JSON.stringify(user)); },
  clear() { localStorage.removeItem(TOKEN); localStorage.removeItem(USER); },
};

export class ApiError extends Error {
  constructor(status, code, message, data) {
    super(message);
    this.status = status;
    this.code = code;
    this.data = data || {};
  }
}

async function request(method, path, body, auth = true) {
  let res;
  try {
    const binary = body instanceof Uint8Array; // a PDF file is sent as it is, everything else as JSON
    res = await fetch(API + path, {
      method,
      headers: { 'Content-Type': binary ? 'application/pdf' : 'application/json', ...(auth && session.token ? { Authorization: 'Bearer ' + session.token } : {}) },
      body: body === undefined ? undefined : binary ? body : JSON.stringify(body),
    });
  } catch {
    throw new ApiError(0, 'NETWORK', 'Cannot reach the server. Check your internet connection.');
  }
  let data = {};
  try { data = await res.json(); } catch { /* empty body */ }
  if (!res.ok) {
    if (res.status === 401 && auth && session.token && data.error === 'UNAUTHORIZED') {
      session.clear();
      window.dispatchEvent(new Event('auth:expired'));
    }
    throw new ApiError(res.status, data.error || 'ERROR', data.message || 'Something went wrong.', data);
  }
  return data;
}

/**
 * Streams an AI answer (server-sent events). `onText` gets the full answer so far each time it grows.
 * Resolves with the final event ({text, model, newNumbers, ...}) or throws ApiError. `signal` cancels it.
 */
async function stream(path, body, onText, signal) {
  let res;
  try {
    res = await fetch(API + path, { method: 'POST', signal, headers: { 'Content-Type': 'application/json', Accept: 'text/event-stream', Authorization: 'Bearer ' + session.token }, body: JSON.stringify({ ...body, stream: true }) });
  } catch (e) {
    if (e && e.name === 'AbortError') throw e;
    throw new ApiError(0, 'NETWORK', 'Cannot reach the server. Check your internet connection.');
  }
  if (!res.ok) {
    let data = {};
    try { data = await res.json(); } catch { /* empty */ }
    if (res.status === 401 && data.error === 'UNAUTHORIZED') { session.clear(); window.dispatchEvent(new Event('auth:expired')); }
    throw new ApiError(res.status, data.error || 'ERROR', data.message || 'Something went wrong.', data);
  }
  const reader = res.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';
  let done = null;
  const handle = (block) => {
    let event = 'message';
    let data = '';
    for (const line of block.split('\n')) {
      if (line.startsWith('event:')) event = line.slice(6).trim();
      else if (line.startsWith('data:')) data += line.slice(5).trim();
    }
    if (!data) return;
    let j;
    try { j = JSON.parse(data); } catch { return; }
    if (event === 'text') onText && onText(j.text || '');
    else if (event === 'done') done = j;
    else if (event === 'error') throw new ApiError(502, j.error || 'ERROR', j.message || 'The AI did not answer.', j);
  };
  for (;;) {
    const { value, done: end } = await reader.read();
    if (end) break;
    buffer += decoder.decode(value, { stream: true });
    let i;
    while ((i = buffer.indexOf('\n\n')) >= 0) { handle(buffer.slice(0, i)); buffer = buffer.slice(i + 2); }
  }
  if (buffer.trim()) handle(buffer);
  if (!done) throw new ApiError(502, 'EMPTY', 'The AI sent an empty answer. Tap Retry.');
  return done;
}

export const api = {
  register: (name, email, password) => request('POST', '/auth/register', { name, email, password }, false),
  verify: (email, code) => request('POST', '/auth/verify', { email, code }, false),
  resend: (email) => request('POST', '/auth/resend', { email }, false),
  login: (email, password) => request('POST', '/auth/login', { email, password }, false),
  forgot: (email) => request('POST', '/auth/forgot', { email }, false),
  reset: (email, code, password) => request('POST', '/auth/reset', { email, code, password }, false),
  deleteAccount: (password) => request('DELETE', '/auth/me', { password }),
  listResumes: () => request('GET', '/resumes'),
  getResume: (id) => request('GET', '/resumes/' + encodeURIComponent(id)),
  createResume: (body) => request('POST', '/resumes', body),
  updateResume: (id, body) => request('PUT', '/resumes/' + encodeURIComponent(id), body),
  deleteResume: (id) => request('DELETE', '/resumes/' + encodeURIComponent(id)),
  uploadPdf: (id, bytes) => request('PUT', '/resumes/' + encodeURIComponent(id) + '/pdf', bytes),
  // AI (the NVIDIA key stays on the server). Each takes the resume's data; pass an AbortSignal to cancel.
  aiStatus: () => request('GET', '/ai/status'),
  aiTest: () => request('POST', '/ai/test', {}),
  aiAssist: (body, onText, signal) => stream('/ai/assist', body, onText, signal),
  aiAsk: (body, onText, signal) => stream('/ai/ask', body, onText, signal),
  aiAtsAdvice: (body, onText, signal) => stream('/ai/ats-advice', body, onText, signal),
  aiCoverLetter: (body) => request('POST', '/ai/cover-letter', body),
  deletePdf: (id) => request('DELETE', '/resumes/' + encodeURIComponent(id) + '/pdf'),
};
