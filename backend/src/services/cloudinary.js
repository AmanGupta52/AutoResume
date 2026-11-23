'use strict';
const crypto = require('crypto');
const { HttpError } = require('../middleware/error');

/**
 * Stores PDF files in Cloudinary through its signed REST API (no SDK needed).
 * The API secret never leaves the server: phones and browsers send the PDF to this backend,
 * and the backend uploads it. MongoDB keeps only the resulting link.
 *
 * PDFs are uploaded as "raw" files with a long random name, so the link cannot be guessed.
 * Anyone who has the link can open the file, so treat it like a private link.
 */
function createCloudinaryService(config, fetchImpl = globalThis.fetch) {
  const c = config.cloudinary;
  const enabled = Boolean(c.cloudName && c.apiKey && c.apiSecret);

  /** SHA-1 of the sorted "key=value" pairs plus the API secret (Cloudinary's signing rule). */
  function sign(params) {
    const text = Object.keys(params)
      .filter((k) => params[k] !== undefined && params[k] !== null && params[k] !== '')
      .sort()
      .map((k) => `${k}=${params[k]}`)
      .join('&');
    return crypto.createHash('sha1').update(text + c.apiSecret).digest('hex');
  }

  // One flat folder for every PDF (no per-user sub-folders), so they all land in the same place in the Media Library.
  function newPublicId(userId, resumeId) {
    return `${c.folder}/${userId}-${resumeId}-${crypto.randomBytes(12).toString('hex')}.pdf`;
  }

  async function post(path, form) {
    let res;
    try {
      res = await fetchImpl(`https://api.cloudinary.com/v1_1/${c.cloudName}/raw/${path}`, { method: 'POST', body: form });
    } catch (e) {
      throw new HttpError(502, 'STORAGE_FAILED', 'Could not reach the PDF storage. Please try again.');
    }
    let json = {};
    try { json = await res.json(); } catch { /* empty */ }
    if (!res.ok) {
      const reason = json && json.error && json.error.message ? String(json.error.message).slice(0, 200) : `HTTP ${res.status}`;
      console.error('[cloudinary]', path, reason);
      const err = new HttpError(502, 'STORAGE_FAILED', 'The PDF could not be saved. Please try again.');
      err.reason = reason;
      throw err;
    }
    return json;
  }

  /**
   * Newer Cloudinary accounts use "dynamic folder mode": the folder you see in the Media Library comes from the
   * asset_folder field, NOT from the slashes in public_id. Without it files pile up in the root ("Home") or in
   * odd places. So we send asset_folder too (old "fixed folder" accounts ignore it; if one rejects it we retry without).
   */
  async function uploadPdf({ buffer, publicId }) {
    if (!enabled) throw new HttpError(503, 'PDF_STORAGE_OFF', 'PDF storage is not set up on this server.');
    const send = (withFolder) => {
      const timestamp = Math.floor(Date.now() / 1000);
      const signed = { public_id: publicId, timestamp };
      if (withFolder) signed.asset_folder = c.folder;
      const form = new FormData();
      form.append('file', new Blob([buffer], { type: 'application/pdf' }), 'resume.pdf');
      form.append('api_key', c.apiKey);
      for (const [k, v] of Object.entries(signed)) form.append(k, String(v));
      form.append('signature', sign(signed));
      return post('upload', form);
    };
    let json;
    try {
      json = await send(true);
    } catch (e) {
      if (e && e.reason && /asset_folder/i.test(e.reason)) json = await send(false);
      else throw e;
    }
    const url = String(json.secure_url || '');
    // only ever store a link that really points at Cloudinary over https
    if (!/^https:\/\/res\.cloudinary\.com\//.test(url) || !json.public_id) {
      throw new HttpError(502, 'STORAGE_FAILED', 'The PDF storage gave an unexpected answer.');
    }
    return { url, publicId: String(json.public_id), bytes: Number(json.bytes) || buffer.length };
  }

  /** Deletes a stored PDF. Callers treat failure as non-fatal (the link is dropped from MongoDB anyway). */
  async function destroy(publicId) {
    if (!enabled || !publicId) return;
    const timestamp = Math.floor(Date.now() / 1000);
    const form = new FormData();
    form.append('api_key', c.apiKey);
    form.append('timestamp', String(timestamp));
    form.append('public_id', publicId);
    form.append('invalidate', 'true');
    form.append('signature', sign({ public_id: publicId, timestamp, invalidate: 'true' }));
    await post('destroy', form);
  }

  return { enabled, uploadPdf, destroy, newPublicId, sign };
}

module.exports = { createCloudinaryService };
