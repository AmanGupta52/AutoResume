// Resume data helpers. The logic lives in shared/ (also used by the backend); this file keeps the old import path.
export { factories, blankData, normalize, sampleData } from './shared/resume.js';

export function newRef() {
  return 'w' + (crypto.randomUUID ? crypto.randomUUID().replace(/-/g, '') : String(Date.now()) + Math.random().toString(36).slice(2, 12));
}
