'use strict';

/** Reads settings from an environment object. Pure, so it can be tested. */
function loadConfig(env) {
  const list = (v) => String(v || '').split(',').map((s) => s.trim()).filter(Boolean);
  return {
    env: env.NODE_ENV || 'development',
    port: Number(env.PORT) || 4000,
    trustProxy: env.TRUST_PROXY === '1' || env.TRUST_PROXY === 'true',
    clientOrigins: list(env.CLIENT_ORIGIN),
    mongoUri: env.MONGODB_URI || '',
    jwtSecret: env.JWT_SECRET || '',
    jwtExpiresIn: env.JWT_EXPIRES_IN || '30d',
    appName: env.APP_NAME || 'AutoResume',
    email: {
      mode: (env.EMAIL_MODE || 'console').toLowerCase(),
      serviceId: env.EMAILJS_SERVICE_ID || '',
      templateId: env.EMAILJS_TEMPLATE_ID || '',
      publicKey: env.EMAILJS_PUBLIC_KEY || '',
      privateKey: env.EMAILJS_PRIVATE_KEY || '',
    },
    limits: {
      resumesPerUser: 50,
      codeMinutes: 15,
      codeMaxAttempts: 5,
      resendCooldownSeconds: 60,
    },
  };
}

/** Returns a list of problems that make the server unsafe or unable to run. */
function configProblems(c) {
  const p = [];
  if (!c.mongoUri) p.push('MONGODB_URI is missing.');
  if (c.jwtSecret.length < 32) p.push('JWT_SECRET must be at least 32 characters.');
  if (c.jwtSecret.startsWith('change-me')) p.push('JWT_SECRET still has the example value. Generate a random one.');
  if (c.email.mode === 'emailjs') {
    const names = { serviceId: 'EMAILJS_SERVICE_ID', templateId: 'EMAILJS_TEMPLATE_ID', publicKey: 'EMAILJS_PUBLIC_KEY', privateKey: 'EMAILJS_PRIVATE_KEY' };
    for (const [k, envName] of Object.entries(names)) {
      if (!c.email[k]) p.push(`${envName} is missing (EMAIL_MODE=emailjs).`);
    }
  } else if (c.env === 'production') {
    p.push('EMAIL_MODE must be "emailjs" in production, otherwise nobody receives their codes.');
  }
  return p;
}

module.exports = { loadConfig, configProblems };
