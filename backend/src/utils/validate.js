'use strict';

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

function normalizeEmail(v) {
  return String(v || '').trim().toLowerCase();
}

function isEmail(v) {
  const e = normalizeEmail(v);
  return e.length <= 254 && EMAIL_RE.test(e);
}

/** bcrypt only reads the first 72 bytes, so longer passwords are refused instead of silently cut. */
function passwordProblem(pw) {
  if (typeof pw !== 'string') return 'Password is required.';
  if (pw.length < 8) return 'Password must be at least 8 characters.';
  if (Buffer.byteLength(pw, 'utf8') > 72) return 'Password is too long (72 bytes at most).';
  if (!/[A-Za-z]/.test(pw) || !/\d/.test(pw)) return 'Password needs at least one letter and one number.';
  return null;
}

function cleanName(v) {
  return String(v || '').replace(/[\u0000-\u001F<>]/g, '').trim().slice(0, 80);
}

function isCode(v) {
  return /^\d{6}$/.test(String(v || ''));
}

module.exports = { normalizeEmail, isEmail, passwordProblem, cleanName, isCode };
