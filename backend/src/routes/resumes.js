'use strict';
const express = require('express');
const mongoose = require('mongoose');
const rateLimit = require('express-rate-limit');
const Resume = require('../models/Resume');
const { HttpError, wrap } = require('../middleware/error');
const { sanitizeResumeData, cleanTitle } = require('../utils/resumeSchema');

function view(r) {
  return {
    id: String(r._id),
    name: r.name,
    data: r.data,
    clientRef: r.clientRef || null,
    pdf: r.pdf && r.pdf.url ? { url: r.pdf.url, bytes: r.pdf.bytes, uploadedAt: r.pdf.uploadedAt } : null,
    clientUpdatedAt: r.clientUpdatedAt,
    createdAt: r.createdAt,
    updatedAt: r.updatedAt,
  };
}

function parseStamp(v) {
  const n = Number(v);
  return Number.isFinite(n) && n > 0 && n < 4102444800000 ? Math.floor(n) : null; // before year 2100
}

function cleanRef(v) {
  return typeof v === 'string' && /^[A-Za-z0-9_-]{8,64}$/.test(v) ? v : undefined;
}

function createResumesRouter(config, requireAuth, storage) {
  const router = express.Router();
  router.use(requireAuth);

  // a failed cleanup in Cloudinary must never break the request: the link is already gone from MongoDB
  const dropFile = (publicId) => {
    if (publicId) storage.destroy(publicId).catch((e) => console.error('[cloudinary] cleanup failed:', e.message));
  };

  const findOwn = async (req) => {
    if (!mongoose.isValidObjectId(req.params.id)) throw new HttpError(404, 'NOT_FOUND', 'Resume not found.');
    const r = await Resume.findOne({ _id: req.params.id, userId: req.user._id });
    if (!r) throw new HttpError(404, 'NOT_FOUND', 'Resume not found.');
    return r;
  };

  // every resume of the signed-in person (content included; there are at most 50)
  router.get('/', wrap(async (req, res) => {
    const list = await Resume.find({ userId: req.user._id }).sort({ clientUpdatedAt: -1 }).limit(config.limits.resumesPerUser);
    res.json({ resumes: list.map(view) });
  }));

  router.post('/', wrap(async (req, res) => {
    const clientRef = cleanRef(req.body.clientRef);
    if (clientRef) {
      // a retry of a create that already worked: return the existing one instead of making a duplicate
      const existing = await Resume.findOne({ userId: req.user._id, clientRef });
      if (existing) return res.status(200).json({ resume: view(existing) });
    }
    const count = await Resume.countDocuments({ userId: req.user._id });
    if (count >= config.limits.resumesPerUser) {
      throw new HttpError(403, 'LIMIT', `You can keep up to ${config.limits.resumesPerUser} resumes. Delete one first.`);
    }
    const doc = await Resume.create({
      userId: req.user._id,
      name: cleanTitle(req.body.name) || 'Untitled resume',
      data: sanitizeResumeData(req.body.data),
      clientRef,
      clientUpdatedAt: parseStamp(req.body.clientUpdatedAt) || Date.now(),
    });
    res.status(201).json({ resume: view(doc) });
  }));

  router.get('/:id', wrap(async (req, res) => {
    res.json({ resume: view(await findOwn(req)) });
  }));

  router.put('/:id', wrap(async (req, res) => {
    const doc = await findOwn(req);
    const stamp = parseStamp(req.body.clientUpdatedAt) || Date.now();
    if (stamp < doc.clientUpdatedAt) {
      // another device saved a newer version: do not overwrite it, send it back
      throw new HttpError(409, 'CONFLICT', 'This resume was changed on another device.', { resume: view(doc) });
    }
    if (req.body.name !== undefined) doc.name = cleanTitle(req.body.name) || doc.name;
    if (req.body.data !== undefined) {
      doc.data = sanitizeResumeData(req.body.data);
      doc.markModified('data');
    }
    doc.clientUpdatedAt = stamp;
    await doc.save();
    res.json({ resume: view(doc) });
  }));

  router.delete('/:id', wrap(async (req, res) => {
    const doc = await findOwn(req);
    const publicId = doc.pdf && doc.pdf.publicId;
    await doc.deleteOne();
    dropFile(publicId);
    res.json({ deleted: true });
  }));

  // ---- the PDF of a resume: the file goes to Cloudinary, MongoDB keeps the link ----
  const pdfLimiter = rateLimit({ windowMs: 60 * 60 * 1000, limit: 60, standardHeaders: 'draft-7', legacyHeaders: false,
    message: { error: 'RATE_LIMIT', message: 'Too many PDF uploads. Please try again later.' } });

  // the body is the PDF itself (Content-Type: application/pdf), not JSON
  router.put('/:id/pdf', pdfLimiter, express.raw({ type: 'application/pdf', limit: config.limits.maxPdfBytes }), wrap(async (req, res) => {
    if (!storage.enabled) throw new HttpError(503, 'PDF_STORAGE_OFF', 'PDF storage is not set up on this server.');
    const doc = await findOwn(req);
    const body = req.body;
    if (!Buffer.isBuffer(body) || body.length < 100) {
      throw new HttpError(400, 'INVALID', 'Send the PDF file as the request body with Content-Type: application/pdf.');
    }
    if (body.subarray(0, 5).toString('latin1') !== '%PDF-') throw new HttpError(400, 'INVALID', 'That file is not a PDF.');
    const up = await storage.uploadPdf({ buffer: body, publicId: storage.newPublicId(String(req.user._id), String(doc._id)) });
    const previous = doc.pdf && doc.pdf.publicId;
    doc.pdf = { url: up.url, publicId: up.publicId, bytes: up.bytes, uploadedAt: new Date() };
    await doc.save();
    dropFile(previous); // only the latest PDF of a resume is kept
    res.json({ resume: view(doc) });
  }));

  router.delete('/:id/pdf', wrap(async (req, res) => {
    const doc = await findOwn(req);
    const previous = doc.pdf && doc.pdf.publicId;
    doc.pdf = null;
    await doc.save();
    dropFile(previous);
    res.json({ resume: view(doc) });
  }));

  return router;
}

module.exports = { createResumesRouter };
