// Tiny DOM helpers. Text is always added as text nodes, never as HTML, so nothing a person types can run as code.

import { t } from './i18n.js';

const TRANSLATED_ATTRS = ['placeholder', 'title', 'aria-label'];

export function h(tag, attrs, ...children) {
  const el = document.createElement(tag);
  for (const [k, v] of Object.entries(attrs || {})) {
    if (v == null || v === false) continue;
    if (k === 'class') el.className = v;
    else if (k.startsWith('on') && typeof v === 'function') el.addEventListener(k.slice(2).toLowerCase(), v);
    else if (k === 'value' || k === 'checked' || k === 'selected' || k === 'disabled') el[k] = v;
    else if (v === true) el.setAttribute(k, '');
    else el.setAttribute(k, TRANSLATED_ATTRS.includes(k) ? t(String(v)) : String(v));
  }
  for (const c of children.flat(Infinity)) {
    if (c == null || c === false) continue;
    el.append(c.nodeType ? c : document.createTextNode(t(String(c))));
  }
  return el;
}

export function toast(message, kind = 'info') {
  let box = document.getElementById('toasts');
  if (!box) { box = h('div', { id: 'toasts', 'aria-live': 'polite' }); document.body.append(box); }
  const el = h('div', { class: `toast ${kind}` }, message);
  box.append(el);
  setTimeout(() => el.remove(), kind === 'error' ? 6000 : 3500);
  return el;
}

export function debounce(fn, ms) {
  let t;
  const d = (...a) => { clearTimeout(t); t = setTimeout(() => fn(...a), ms); };
  d.cancel = () => clearTimeout(t);
  d.flush = (...a) => { clearTimeout(t); fn(...a); };
  return d;
}

export function timeAgo(iso) {
  const s = Math.max(0, (Date.now() - new Date(iso).getTime()) / 1000);
  if (s < 60) return 'just now';
  if (s < 3600) return `${Math.floor(s / 60)} min ago`;
  if (s < 86400) return `${Math.floor(s / 3600)} h ago`;
  if (s < 86400 * 30) return `${Math.floor(s / 86400)} days ago`;
  return new Date(iso).toLocaleDateString();
}

/** Disables a button and shows a label while a promise runs. */
export async function withBusy(button, label, fn) {
  const old = button.textContent;
  button.disabled = true;
  button.textContent = t(label);
  try { return await fn(); } finally { button.disabled = false; button.textContent = old; }
}

let uidN = 0;
export const uid = () => `f${++uidN}`;

/** A toast with one action button (used for Undo). */
export function toastAction(message, label, onClick) {
  const el = toast(message);
  const b = h('button', { class: 'link toast-btn', onclick: () => { el.remove(); onClick(); } }, label);
  el.append(' ', b);
  return el;
}

/**
 * A dialog. `content` is a node; returns { close, box }. Esc and the backdrop close it; focus goes in and comes back.
 * `onClose` runs once however it was closed.
 */
export function modal(title, content, { onClose, wide } = {}) {
  const previous = document.activeElement;
  const box = h('div', { class: 'modal' + (wide ? ' wide' : ''), role: 'dialog', 'aria-modal': 'true', 'aria-label': title, tabindex: '-1' },
    h('div', { class: 'modal-head' }, h('h2', {}, title), h('button', { class: 'icon', type: 'button', 'aria-label': 'Close', onclick: () => close() }, '✕')),
    h('div', { class: 'modal-body' }, content));
  const back = h('div', { class: 'backdrop', onmousedown: (e) => { if (e.target === back) close(); } }, box);
  let closed = false;
  function close() {
    if (closed) return;
    closed = true;
    document.removeEventListener('keydown', onKey);
    back.remove();
    if (previous && previous.focus) previous.focus();
    if (onClose) onClose();
  }
  function onKey(e) { if (e.key === 'Escape') { e.stopPropagation(); close(); } }
  document.addEventListener('keydown', onKey);
  document.body.append(back);
  box.focus();
  return { close, box };
}

/** Replaces the children of `el`. Unlike replaceChildren, it flattens arrays and skips null/false (so "null" never shows up as text). */
export const fill = (el, ...kids) => el.replaceChildren(...kids.flat(Infinity).filter((x) => x != null && x !== false));
