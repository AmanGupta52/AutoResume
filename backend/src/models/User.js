'use strict';
const mongoose = require('mongoose');

const userSchema = new mongoose.Schema(
  {
    name: { type: String, required: true, trim: true, maxlength: 80 },
    email: { type: String, required: true, unique: true, lowercase: true, trim: true, maxlength: 254 },
    passwordHash: { type: String, required: true, select: false },
    verified: { type: Boolean, default: false },
    // one active code at a time (email verification or password reset)
    otp: {
      type: new mongoose.Schema(
        {
          hash: String,
          purpose: { type: String, enum: ['verify', 'reset'] },
          expiresAt: Date,
          attempts: { type: Number, default: 0 },
          sentAt: Date,
        },
        { _id: false },
      ),
      select: false,
    },
    // bumping this signs the person out on every device (used after a password reset)
    tokenVersion: { type: Number, default: 0 },
  },
  { timestamps: true },
);

// accounts that never verify their email are removed after 7 days
userSchema.index({ createdAt: 1 }, { expireAfterSeconds: 7 * 24 * 3600, partialFilterExpression: { verified: false } });

module.exports = mongoose.model('User', userSchema);
