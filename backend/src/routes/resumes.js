'use strict';
const express = require('express');
const mongoose = require('mongoose');
const Resume = require('../models/Resume');
const { HttpError, wrap } = require('../middleware/error');
const { sanitizeResumeData, cleanTitle } = require('../utils/resumeSchema');

function view(r) {
  return {
    id: String(r._id),
    name: r.name,
    data: r.data,
    clientRef: r.clientRef || null,
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

function createResumesRouter(config, requireAuth) {
  const router = express.Router();
  router.use(requireAuth);

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
    await doc.deleteOne();
    res.json({ deleted: true });
  }));

  return router;
}

module.exports = { createResumesRouter };
