'use strict';
const path = require('path');
const express = require('express');
const helmet = require('helmet');
const cors = require('cors');
const rateLimit = require('express-rate-limit');
const { createEmailService } = require('./services/email');
const { createCloudinaryService } = require('./services/cloudinary');
const { createAuthRouter } = require('./routes/auth');
const { createResumesRouter } = require('./routes/resumes');
const { requireAuth } = require('./middleware/auth');
const { notFound, errorHandler } = require('./middleware/error');

function createApp(config, { email = createEmailService(config), storage = createCloudinaryService(config) } = {}) {
  const app = express();
  if (config.trustProxy) app.set('trust proxy', 1);
  app.disable('x-powered-by');

  app.use(helmet({
    contentSecurityPolicy: {
      useDefaults: false,
      directives: {
        'default-src': ["'self'"],
        'script-src': ["'self'"],
        // the printable resume is rendered with its own <style>, so inline styles must be allowed
        'style-src': ["'self'", "'unsafe-inline'"],
        'img-src': ["'self'", 'data:'],
        'connect-src': ["'self'"],
        'frame-src': ["'self'", 'about:'],
        'object-src': ["'none'"],
        'base-uri': ["'self'"],
        'form-action': ["'self'"],
        'frame-ancestors': ["'none'"],
      },
    },
  }));
  // phones send no Origin header and are always allowed; browsers must be on the list (or same-origin)
  app.use(cors({
    origin(origin, cb) {
      if (!origin || config.clientOrigins.includes(origin)) return cb(null, true);
      return cb(null, false);
    },
  }));
  app.use(express.json({ limit: '512kb' }));

  const authLimiter = rateLimit({ windowMs: 15 * 60 * 1000, limit: 40, standardHeaders: 'draft-7', legacyHeaders: false,
    message: { error: 'RATE_LIMIT', message: 'Too many attempts. Please wait a few minutes.' } });
  const mailLimiter = rateLimit({ windowMs: 60 * 60 * 1000, limit: 12, standardHeaders: 'draft-7', legacyHeaders: false,
    message: { error: 'RATE_LIMIT', message: 'Too many emails requested. Please try again later.' } });
  const apiLimiter = rateLimit({ windowMs: 15 * 60 * 1000, limit: 600, standardHeaders: 'draft-7', legacyHeaders: false,
    message: { error: 'RATE_LIMIT', message: 'Too many requests. Please slow down.' } });

  app.get('/api/health', (_req, res) => res.json({ ok: true }));
  app.use('/api/auth/register', mailLimiter);
  app.use('/api/auth/resend', mailLimiter);
  app.use('/api/auth/forgot', mailLimiter);
  app.use('/api/auth', authLimiter, createAuthRouter(config, email, storage));
  app.use('/api/resumes', apiLimiter, createResumesRouter(config, requireAuth(config), storage));
  app.use('/api', notFound);

  // the web app (plain files, no build step)
  const webDir = path.join(__dirname, '..', '..', 'web');
  app.use(express.static(webDir, { index: 'index.html', maxAge: '5m' }));
  app.get(/^\/(?!api\/).*/, (_req, res) => res.sendFile(path.join(webDir, 'index.html')));

  app.use(errorHandler);
  return app;
}

module.exports = { createApp };
