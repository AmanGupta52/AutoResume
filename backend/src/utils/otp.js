'use strict';
const crypto = require('crypto');

/** A 6-digit code from a secure random source. */
function generateCode() {
  return String(crypto.randomInt(0, 1000000)).padStart(6, '0');
}

/** Codes are stored hashed (HMAC), tied to the email and purpose, so a leaked database does not leak codes. */
function hashCode(secret, email, purpose, code) {
  return crypto.createHmac('sha256', secret).update(`${email}|${purpose}|${code}`).digest('hex');
}

function safeEqualHex(a, b) {
  const x = Buffer.from(String(a || ''), 'hex');
  const y = Buffer.from(String(b || ''), 'hex');
  return x.length === y.length && x.length > 0 && crypto.timingSafeEqual(x, y);
}

module.exports = { generateCode, hashCode, safeEqualHex };
