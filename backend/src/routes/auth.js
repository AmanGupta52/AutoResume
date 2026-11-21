'use strict';
const express = require('express');
const bcrypt = require('bcryptjs');
const User = require('../models/User');
const Resume = require('../models/Resume');
const { HttpError, wrap } = require('../middleware/error');
const { signToken, requireAuth } = require('../middleware/auth');
const { generateCode, hashCode, safeEqualHex } = require('../utils/otp');
const { normalizeEmail, isEmail, passwordProblem, cleanName, isCode } = require('../utils/validate');

// compared against when the email is unknown, so "no such user" and "wrong password" take the same time
const DUMMY_HASH = bcrypt.hashSync('not-a-real-password-1', 10);

function publicUser(u) {
  return { id: String(u._id), name: u.name, email: u.email };
}

function createAuthRouter(config, email, storage) {
  const router = express.Router();
  const { limits } = config;

  /** Creates a fresh code for the user, stores only its hash, and emails it. */
  async function issueCode(user, purpose) {
    const sentAt = user.otp && user.otp.sentAt ? user.otp.sentAt.getTime() : 0;
    const wait = Math.ceil((sentAt + limits.resendCooldownSeconds * 1000 - Date.now()) / 1000);
    if (wait > 0) throw new HttpError(429, 'TOO_SOON', `Please wait ${wait} seconds before asking for another code.`, { retryAfter: wait });
    const code = generateCode();
    user.otp = {
      hash: hashCode(config.jwtSecret, user.email, purpose, code),
      purpose,
      expiresAt: new Date(Date.now() + limits.codeMinutes * 60 * 1000),
      attempts: 0,
      sentAt: new Date(),
    };
    await user.save();
    try {
      await email.sendCode({ to: user.email, name: user.name, code, purpose });
    } catch (e) {
      console.error('[email] could not send:', e.message);
      throw new HttpError(502, 'EMAIL_FAILED', 'We could not send the email. Please try again in a moment.');
    }
  }

  /** Checks a code. Wrong guesses are counted; after too many the code is burned. */
  async function consumeCode(user, purpose, code) {
    const otp = user.otp;
    if (!otp || !otp.hash || otp.purpose !== purpose || !otp.expiresAt || otp.expiresAt.getTime() < Date.now()) {
      throw new HttpError(400, 'CODE_EXPIRED', 'That code has expired. Ask for a new one.');
    }
    if (otp.attempts >= limits.codeMaxAttempts) {
      throw new HttpError(429, 'CODE_LOCKED', 'Too many wrong codes. Ask for a new one.');
    }
    if (!safeEqualHex(otp.hash, hashCode(config.jwtSecret, user.email, purpose, code))) {
      user.otp.attempts = otp.attempts + 1;
      await user.save();
      throw new HttpError(400, 'CODE_WRONG', 'That code is not right.');
    }
    user.otp = undefined;
  }

  router.post('/register', wrap(async (req, res) => {
    const name = cleanName(req.body.name);
    const mail = normalizeEmail(req.body.email);
    const pw = req.body.password;
    if (!name) throw new HttpError(400, 'INVALID', 'Please enter your name.');
    if (!isEmail(mail)) throw new HttpError(400, 'INVALID', 'Please enter a valid email address.');
    const problem = passwordProblem(pw);
    if (problem) throw new HttpError(400, 'INVALID', problem);

    let user = await User.findOne({ email: mail }).select('+passwordHash +otp');
    if (user && user.verified) throw new HttpError(409, 'EMAIL_TAKEN', 'An account with this email already exists. Please log in.');
    const passwordHash = await bcrypt.hash(pw, 12);
    if (user) {
      user.name = name;
      user.passwordHash = passwordHash;
    } else {
      user = new User({ name, email: mail, passwordHash });
    }
    await issueCode(user, 'verify');
    res.status(201).json({ needsVerification: true, email: mail, message: 'We sent a 6-digit code to your email.' });
  }));

  router.post('/verify', wrap(async (req, res) => {
    const mail = normalizeEmail(req.body.email);
    if (!isEmail(mail) || !isCode(req.body.code)) throw new HttpError(400, 'INVALID', 'Enter the 6-digit code from your email.');
    const user = await User.findOne({ email: mail }).select('+otp');
    if (!user) throw new HttpError(400, 'CODE_EXPIRED', 'That code has expired. Ask for a new one.');
    await consumeCode(user, 'verify', String(req.body.code));
    user.verified = true;
    await user.save();
    res.json({ token: signToken(config, user), user: publicUser(user) });
  }));

  router.post('/resend', wrap(async (req, res) => {
    const mail = normalizeEmail(req.body.email);
    if (!isEmail(mail)) throw new HttpError(400, 'INVALID', 'Please enter a valid email address.');
    const user = await User.findOne({ email: mail }).select('+otp');
    // same answer whether or not the account exists
    if (user && !user.verified) await issueCode(user, 'verify');
    res.json({ message: 'If this email needs verification, a new code is on its way.' });
  }));

  router.post('/login', wrap(async (req, res) => {
    const mail = normalizeEmail(req.body.email);
    const pw = typeof req.body.password === 'string' ? req.body.password : '';
    const user = isEmail(mail) ? await User.findOne({ email: mail }).select('+passwordHash +otp') : null;
    const ok = await bcrypt.compare(pw, user ? user.passwordHash : DUMMY_HASH);
    if (!user || !ok) throw new HttpError(401, 'BAD_LOGIN', 'Wrong email or password.');
    if (!user.verified) {
      try { await issueCode(user, 'verify'); } catch (e) { if (!(e instanceof HttpError && e.status === 429)) throw e; }
      throw new HttpError(403, 'EMAIL_NOT_VERIFIED', 'Please verify your email. We sent you a code.', { email: mail });
    }
    res.json({ token: signToken(config, user), user: publicUser(user) });
  }));

  router.post('/forgot', wrap(async (req, res) => {
    const mail = normalizeEmail(req.body.email);
    if (!isEmail(mail)) throw new HttpError(400, 'INVALID', 'Please enter a valid email address.');
    const user = await User.findOne({ email: mail }).select('+otp');
    if (user && user.verified) {
      try { await issueCode(user, 'reset'); } catch (e) { if (!(e instanceof HttpError && e.status === 429)) throw e; }
    }
    res.json({ message: 'If an account exists for this email, a code is on its way.' });
  }));

  router.post('/reset', wrap(async (req, res) => {
    const mail = normalizeEmail(req.body.email);
    if (!isEmail(mail) || !isCode(req.body.code)) throw new HttpError(400, 'INVALID', 'Enter the 6-digit code from your email.');
    const problem = passwordProblem(req.body.password);
    if (problem) throw new HttpError(400, 'INVALID', problem);
    const user = await User.findOne({ email: mail }).select('+otp +passwordHash');
    if (!user || !user.verified) throw new HttpError(400, 'CODE_EXPIRED', 'That code has expired. Ask for a new one.');
    await consumeCode(user, 'reset', String(req.body.code));
    user.passwordHash = await bcrypt.hash(req.body.password, 12);
    user.tokenVersion += 1; // signs out every device
    await user.save();
    res.json({ message: 'Your password was changed. Please log in.' });
  }));

  router.get('/me', requireAuth(config), wrap(async (req, res) => {
    res.json({ user: publicUser(req.user) });
  }));

  // delete the account and every resume in it (asks for the password again)
  router.delete('/me', requireAuth(config), wrap(async (req, res) => {
    const user = await User.findById(req.user._id).select('+passwordHash');
    const pw = typeof req.body.password === 'string' ? req.body.password : '';
    if (!(await bcrypt.compare(pw, user.passwordHash))) throw new HttpError(401, 'BAD_LOGIN', 'Wrong password.');
    const withFiles = await Resume.find({ userId: user._id, 'pdf.publicId': { $exists: true, $ne: null } }).select('pdf');
    await Promise.allSettled(withFiles.map((r) => storage.destroy(r.pdf.publicId)));
    await Resume.deleteMany({ userId: user._id });
    await User.deleteOne({ _id: user._id });
    res.json({ message: 'Your account and all your resumes were deleted.' });
  }));

  return router;
}

module.exports = { createAuthRouter };
