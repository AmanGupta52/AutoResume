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

  function newPublicId(userId, resumeId) {
    return `${c.folder}/${userId}/${resumeId}-${crypto.randomBytes(12).toString('hex')}.pdf`;
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
      throw new HttpError(502, 'STORAGE_FAILED', 'The PDF could not be saved. Please try again.');
    }
    return json;
  }

  async function uploadPdf({ buffer, publicId }) {
    if (!enabled) throw new HttpError(503, 'PDF_STORAGE_OFF', 'PDF storage is not set up on this server.');
    const timestamp = Math.floor(Date.now() / 1000);
    const form = new FormData();
    form.append('file', new Blob([buffer], { type: 'application/pdf' }), 'resume.pdf');
    form.append('api_key', c.apiKey);
    form.append('timestamp', String(timestamp));
    form.append('public_id', publicId);
    form.append('signature', sign({ public_id: publicId, timestamp }));
    const json = await post('upload', form);
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
