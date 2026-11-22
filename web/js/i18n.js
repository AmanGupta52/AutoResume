// Interface language (English / Hindi). Strings are looked up by their English text; anything missing stays English.
import { HI } from './i18n-hi.js';

const KEY = 'autoresume_lang';
export const LANGS = [['en', 'English'], ['hi', 'हिन्दी (Hindi)']];
let code = 'en';
try { code = localStorage.getItem(KEY) || 'en'; } catch { /* private mode */ }
if (!LANGS.some(([c]) => c === code)) code = 'en';

export const getLang = () => code;
export function setLang(c) {
  code = LANGS.some(([x]) => x === c) ? c : 'en';
  try { localStorage.setItem(KEY, code); } catch { /* ignore */ }
  document.documentElement.lang = code;
}
export const t = (s) => (code === 'hi' && typeof s === 'string' ? HI[s] || s : s);
document.documentElement.lang = code;
