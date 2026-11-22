import { api } from './api.js';
import { normalize } from './model.js';
import { debounce } from './ui.js';

/**
 * Autosave for one open resume (used by the editor and the guided setup).
 * Saves 0.8 s after the last edit, retries while offline, never overwrites a newer copy from another device
 * (the server answers 409 and the caller decides: load the latest, or keep mine).
 */
export function createStore(initial, { onStatus, onConflict }) {
  let resume = initial;
  let version = 0;
  let savedVersion = 0;
  let saving = false;
  let queued = false;
  let retryTimer = null;
  let conflict = null;
  let destroyed = false;

  async function save() {
    if (saving) { queued = true; return; }
    if (conflict) return;
    saving = true;
    const mine = version;
    onStatus('Saving...');
    try {
      const stamp = Math.max(Date.now(), (resume.clientUpdatedAt || 0) + 1);
      const { resume: saved } = await api.updateResume(resume.id, { name: resume.name, data: resume.data, clientUpdatedAt: stamp });
      resume.clientUpdatedAt = saved.clientUpdatedAt;
      savedVersion = mine;
      if (mine === version) onStatus('All changes saved'); else queued = true;
    } catch (e) {
      if (e.status === 409 && e.data && e.data.resume) { conflict = e.data.resume; onStatus('Not saved: changed elsewhere', true); onConflict(conflict); }
      else if (e.status === 0 || e.status >= 500) { onStatus('Offline. Retrying...', true); clearTimeout(retryTimer); if (!destroyed) retryTimer = setTimeout(save, 5000); }
      else onStatus('Not saved: ' + e.message, true);
    } finally {
      saving = false;
      if (queued && !conflict) { queued = false; if (!destroyed) saveSoon(); }
    }
  }
  const saveSoon = debounce(save, 800);

  return {
    get resume() { return resume; },
    get data() { return resume.data; },
    changed() { version += 1; onStatus('Unsaved changes...'); saveSoon(); },
    get unsaved() { return version > savedVersion; },
    /** Another device saved a newer copy: take it. */
    loadLatest() { resume = normalize(conflict); conflict = null; savedVersion = version; onStatus('Loaded the latest version'); return resume; },
    /** ...or keep this copy and overwrite the other one. */
    keepMine() { resume.clientUpdatedAt = conflict.clientUpdatedAt; conflict = null; this.changed(); },
    /** Replace the whole data (used by Undo). */
    setData(data) { resume.data = data; this.changed(); },
    flush() { saveSoon.cancel(); return save(); },
    destroy() {
      destroyed = true;
      clearTimeout(retryTimer);
      saveSoon.cancel();
      if (version > savedVersion && !conflict) save();
    },
  };
}
