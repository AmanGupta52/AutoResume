'use strict';

/** CLOUDINARY_URL=cloudinary://API_KEY:API_SECRET@CLOUD_NAME, or the three separate variables. */
function parseCloudinary(env) {
  let cloudName = env.CLOUDINARY_CLOUD_NAME || '';
  let apiKey = env.CLOUDINARY_API_KEY || '';
  let apiSecret = env.CLOUDINARY_API_SECRET || '';
  const m = /^cloudinary:\/\/([^:\s]+):([^@\s]+)@([^/?\s]+)/.exec(env.CLOUDINARY_URL || '');
  if (m) { apiKey = apiKey || m[1]; apiSecret = apiSecret || m[2]; cloudName = cloudName || m[3]; }
  const folder = String(env.CLOUDINARY_FOLDER || 'autoresume').replace(/[^\w/-]/g, '').replace(/^\/+|\/+$/g, '') || 'autoresume';
  return { cloudName, apiKey, apiSecret, folder };
}

const unquote = (v) => String(v || '').trim().replace(/^["']+|["']+$/g, '').trim();

/** NVIDIA_API_KEY is the free key from https://build.nvidia.com (starts with "nvapi-"). NVIDIA_MODEL "auto" lets the server pick a live model. */
function parseAi(env) {
  const model = unquote(env.NVIDIA_MODEL);
  const n = (v, d) => (Number(v) > 0 ? Number(v) : d);
  return {
    apiKey: unquote(env.NVIDIA_API_KEY),
    model: model.toLowerCase() === 'auto' ? '' : model,
    baseUrl: (unquote(env.NVIDIA_BASE_URL) || 'https://integrate.api.nvidia.com/v1').replace(/\/+$/, ''),
    timeoutMs: n(env.AI_TIMEOUT_MS, 90000),
    dailyLimit: n(env.AI_DAILY_LIMIT, 150),
    maxParallel: n(env.AI_MAX_PARALLEL, 2),
  };
}

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
    cloudinary: parseCloudinary(env),
    ai: parseAi(env),
    limits: {
      maxPdfBytes: 8 * 1024 * 1024,
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
  const cl = c.cloudinary;
  const set = [cl.cloudName, cl.apiKey, cl.apiSecret].filter(Boolean).length;
  if (set > 0 && set < 3) p.push('Cloudinary is half configured: set CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY and CLOUDINARY_API_SECRET (or CLOUDINARY_URL).');
  return p;
}

/** Things worth telling the owner about that do not stop the server. */
function configWarnings(c) {
  const w = [];
  if (c.ai.apiKey && !c.ai.apiKey.startsWith('nvapi-')) w.push('NVIDIA_API_KEY does not start with "nvapi-". Copy the key again from https://build.nvidia.com.');
  if (c.env === 'production' && c.clientOrigins.length === 0) w.push('CLIENT_ORIGIN is empty: only the web app served by this server can call the API from a browser.');
  return w;
}

module.exports = { loadConfig, configProblems, configWarnings };
