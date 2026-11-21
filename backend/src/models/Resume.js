'use strict';
const mongoose = require('mongoose');

const resumeSchema = new mongoose.Schema(
  {
    userId: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true, index: true },
    name: { type: String, required: true, maxlength: 120 },
    // all resume content (see utils/resumeSchema.js). PDF files are not kept here, only the link (see `pdf`).
    data: { type: mongoose.Schema.Types.Mixed, default: {} },
    // the latest PDF of this resume. The file lives in Cloudinary; only its link is kept here.
    pdf: {
      type: new mongoose.Schema(
        { url: String, publicId: String, bytes: Number, uploadedAt: Date },
        { _id: false },
      ),
      default: null,
    },
    // lets a device retry "create" safely without making duplicates
    clientRef: { type: String, maxlength: 64 },
    // time of the last edit on the device, used to decide which copy is newer
    clientUpdatedAt: { type: Number, default: 0 },
  },
  { timestamps: true, minimize: false },
);

resumeSchema.index({ userId: 1, clientRef: 1 }, { unique: true, partialFilterExpression: { clientRef: { $type: 'string' } } });

module.exports = mongoose.model('Resume', resumeSchema);
