import { api } from './api.js';

// Whether the server has AI turned on (NVIDIA_API_KEY set). Asked once per page load; AI buttons are hidden when off.
let state = { checked: false, enabled: false, remaining: null };
let pending = null;

export function loadAiState(force = false) {
  if (!force && pending) return pending;
  pending = api.aiStatus()
    .then((r) => { state = { checked: true, enabled: !!r.enabled, remaining: r.remainingToday }; return state; })
    .catch(() => { state = { checked: true, enabled: false, remaining: null }; pending = null; return state; });
  return pending;
}
export const aiOn = () => state.enabled;
