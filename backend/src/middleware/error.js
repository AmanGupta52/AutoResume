'use strict';

class HttpError extends Error {
  constructor(status, code, message, extra) {
    super(message);
    this.status = status;
    this.code = code;
    this.extra = extra || {};
  }
}

/** Wraps an async route so a thrown error reaches the error handler. */
const wrap = (fn) => (req, res, next) => Promise.resolve(fn(req, res, next)).catch(next);

function notFound(_req, res) {
  res.status(404).json({ error: 'NOT_FOUND', message: 'Not found.' });
}

// eslint-disable-next-line no-unused-vars
function errorHandler(err, _req, res, _next) {
  if (err instanceof HttpError) {
    return res.status(err.status).json({ error: err.code, message: err.message, ...err.extra });
  }
  if (err && err.type === 'entity.too.large') {
    return res.status(413).json({ error: 'TOO_LARGE', message: 'That is too much data.' });
  }
  if (err && err.type === 'entity.parse.failed') {
    return res.status(400).json({ error: 'BAD_JSON', message: 'The request body is not valid JSON.' });
  }
  console.error('[error]', err && err.stack ? err.stack : err);
  return res.status(500).json({ error: 'SERVER_ERROR', message: 'Something went wrong on the server.' });
}

module.exports = { HttpError, wrap, notFound, errorHandler };
