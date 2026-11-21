// Talks to the backend. The login token lives in localStorage; every call sends it as a Bearer header.

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
    res = await fetch('/api' + path, {
      method,
      headers: { 'Content-Type': 'application/json', ...(auth && session.token ? { Authorization: 'Bearer ' + session.token } : {}) },
      body: body === undefined ? undefined : JSON.stringify(body),
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
};
