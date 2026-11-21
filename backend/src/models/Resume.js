'use strict';
const mongoose = require('mongoose');

const resumeSchema = new mongoose.Schema(
  {
    userId: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true, index: true },
    name: { type: String, required: true, maxlength: 120 },
    // all resume content (see utils/resumeSchema.js). No PDF or file is ever stored.
    data: { type: mongoose.Schema.Types.Mixed, default: {} },
    // lets a device retry "create" safely without making duplicates
    clientRef: { type: String, maxlength: 64 },
    // time of the last edit on the device, used to decide which copy is newer
    clientUpdatedAt: { type: Number, default: 0 },
  },
  { timestamps: true, minimize: false },
);

resumeSchema.index({ userId: 1, clientRef: 1 }, { unique: true, partialFilterExpression: { clientRef: { $type: 'string' } } });

module.exports = mongoose.model('Resume', resumeSchema);
