import { h, toast, withBusy } from './ui.js';
import { api, session, ApiError } from './api.js';

const pending = { email: '', notice: '' }; // carried between the steps (verify, reset)

function passwordInput(id, autocomplete) {
  const input = h('input', { id, type: 'password', required: true, autocomplete, minlength: 8, maxlength: 72 });
  const toggle = h('button', { type: 'button', class: 'link small', onclick: () => {
    const show = input.type === 'password';
    input.type = show ? 'text' : 'password';
    toggle.textContent = show ? 'Hide' : 'Show';
  } }, 'Show');
  return { input, row: h('div', { class: 'pw-row' }, input, toggle) };
}

function card(title, subtitle, form, footer) {
  return h('main', { class: 'auth' },
    h('div', { class: 'brand big' }, 'AutoResume'),
    h('div', { class: 'card auth-card' }, h('h1', {}, title), subtitle ? h('p', { class: 'muted' }, subtitle) : null, form, footer ? h('div', { class: 'auth-foot' }, footer) : null));
}

const link = (text, href) => h('a', { href, class: 'link' }, text);
const errorBox = () => h('div', { class: 'error', role: 'alert', hidden: true });
function showError(box, msg) { box.textContent = msg; box.hidden = !msg; }
const errText = (e) => (e instanceof ApiError ? e.message : 'Something went wrong. Please try again.');

function field(label, input) { return h('label', { class: 'field' }, h('span', {}, label), input); }

export function loginView(nav) {
  const err = errorBox();
  const email = h('input', { type: 'email', required: true, autocomplete: 'email', maxlength: 254, value: pending.email });
  const pw = passwordInput('lp', 'current-password');
  const btn = h('button', { type: 'submit', class: 'btn primary block' }, 'Log in');
  const form = h('form', { novalidate: false, onsubmit: async (e) => {
    e.preventDefault(); showError(err, '');
    await withBusy(btn, 'Logging in...', async () => {
      try {
        const r = await api.login(email.value, pw.input.value);
        session.set(r.token, r.user); pending.email = '';
        nav('#/resumes');
      } catch (x) {
        if (x.code === 'EMAIL_NOT_VERIFIED') { pending.email = email.value.trim().toLowerCase(); pending.notice = 'We sent you a new code.'; nav('#/verify'); return; }
        showError(err, errText(x));
      }
    });
  } },
  err, field('Email', email), field('Password', pw.row), btn);
  const notice = pending.notice; pending.notice = '';
  return card('Welcome back', notice || 'Log in to see your resumes.', form,
    [link('Forgot password?', '#/forgot'), h('span', {}, ' · '), link('Create an account', '#/register')]);
}

export function registerView(nav) {
  const err = errorBox();
  const name = h('input', { type: 'text', required: true, autocomplete: 'name', maxlength: 80 });
  const email = h('input', { type: 'email', required: true, autocomplete: 'email', maxlength: 254 });
  const pw = passwordInput('rp', 'new-password');
  const btn = h('button', { type: 'submit', class: 'btn primary block' }, 'Create account');
  const form = h('form', { onsubmit: async (e) => {
    e.preventDefault(); showError(err, '');
    await withBusy(btn, 'Creating...', async () => {
      try {
        const r = await api.register(name.value, email.value, pw.input.value);
        pending.email = r.email; pending.notice = r.message; nav('#/verify');
      } catch (x) { showError(err, errText(x)); }
    });
  } },
  err, field('Your name', name), field('Email', email), field('Password', pw.row),
  h('p', { class: 'muted small' }, 'At least 8 characters, with a letter and a number.'), btn);
  return card('Create your account', 'Your resumes are saved to your account, so you can edit them on the web and on your phone.', form,
    [h('span', {}, 'Already have an account? '), link('Log in', '#/login')]);
}

export function verifyView(nav) {
  if (!pending.email) { nav('#/login'); return h('div'); }
  const err = errorBox();
  const code = h('input', { type: 'text', inputmode: 'numeric', autocomplete: 'one-time-code', pattern: '[0-9]{6}', maxlength: 6, required: true, class: 'code' });
  const btn = h('button', { type: 'submit', class: 'btn primary block' }, 'Verify email');
  const resend = h('button', { type: 'button', class: 'link' }, 'Send a new code');
  let timer;
  const cooldown = (s) => {
    clearInterval(timer); resend.disabled = true;
    let left = s; resend.textContent = `Send a new code (${left}s)`;
    timer = setInterval(() => { left -= 1; if (left <= 0) { clearInterval(timer); resend.disabled = false; resend.textContent = 'Send a new code'; } else resend.textContent = `Send a new code (${left}s)`; }, 1000);
  };
  cooldown(60);
  resend.addEventListener('click', async () => {
    try { await api.resend(pending.email); toast('A new code is on its way.'); cooldown(60); } catch (x) { showError(err, errText(x)); if (x.data && x.data.retryAfter) cooldown(x.data.retryAfter); }
  });
  const form = h('form', { onsubmit: async (e) => {
    e.preventDefault(); showError(err, '');
    await withBusy(btn, 'Checking...', async () => {
      try {
        const r = await api.verify(pending.email, code.value.trim());
        clearInterval(timer); session.set(r.token, r.user); pending.email = ''; toast('Email verified. Welcome!');
        nav('#/resumes');
      } catch (x) { showError(err, errText(x)); }
    });
  } }, err, field('6-digit code', code), btn);
  const notice = pending.notice || 'We sent a 6-digit code to your email.'; pending.notice = '';
  return card('Check your email', `${notice} (${pending.email})`, form, [resend, h('span', {}, ' · '), link('Back to log in', '#/login')]);
}

export function forgotView(nav) {
  const err = errorBox();
  const email = h('input', { type: 'email', required: true, autocomplete: 'email', maxlength: 254, value: pending.email });
  const btn = h('button', { type: 'submit', class: 'btn primary block' }, 'Send code');
  const form = h('form', { onsubmit: async (e) => {
    e.preventDefault(); showError(err, '');
    await withBusy(btn, 'Sending...', async () => {
      try { const r = await api.forgot(email.value); pending.email = email.value.trim().toLowerCase(); pending.notice = r.message; nav('#/reset'); }
      catch (x) { showError(err, errText(x)); }
    });
  } }, err, field('Email', email), btn);
  return card('Forgot your password?', 'Enter your email and we will send you a code.', form, link('Back to log in', '#/login'));
}

export function resetView(nav) {
  if (!pending.email) { nav('#/forgot'); return h('div'); }
  const err = errorBox();
  const code = h('input', { type: 'text', inputmode: 'numeric', autocomplete: 'one-time-code', pattern: '[0-9]{6}', maxlength: 6, required: true, class: 'code' });
  const pw = passwordInput('np', 'new-password');
  const btn = h('button', { type: 'submit', class: 'btn primary block' }, 'Change password');
  const form = h('form', { onsubmit: async (e) => {
    e.preventDefault(); showError(err, '');
    await withBusy(btn, 'Saving...', async () => {
      try { const r = await api.reset(pending.email, code.value.trim(), pw.input.value); pending.notice = r.message; nav('#/login'); }
      catch (x) { showError(err, errText(x)); }
    });
  } }, err, field('6-digit code', code), field('New password', pw.row), btn);
  const notice = pending.notice || 'Enter the code we emailed you.'; pending.notice = '';
  return card('Choose a new password', `${notice} (${pending.email})`, form, link('Back to log in', '#/login'));
}
