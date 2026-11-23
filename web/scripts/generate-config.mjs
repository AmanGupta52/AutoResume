// Git deploys only: writes config.js from Netlify environment variables API_BASE and APK_URL.
import { writeFileSync, readFileSync } from 'node:fs';
const { API_BASE, APK_URL } = process.env;
if (!API_BASE) { console.log('API_BASE not set: keeping config.js as it is.'); process.exit(0); }
const cfg = { API_BASE: API_BASE.replace(/\/+$/, ''), APK_URL: APK_URL || '' };
writeFileSync('config.js', `window.AUTORESUME_CONFIG = ${JSON.stringify(cfg, null, 2)};\n`);
console.log('config.js written for', cfg.API_BASE);
