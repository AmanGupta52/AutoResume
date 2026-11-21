'use strict';
const jwt = require('jsonwebtoken');
const User = require('../models/User');
const { HttpError } = require('./error');

function signToken(config, user) {
  return jwt.sign({ sub: String(user._id), v: user.tokenVersion }, config.jwtSecret, { expiresIn: config.jwtExpiresIn });
}

function requireAuth(config) {
  return async (req, _res, next) => {
    try {
      const header = req.headers.authorization || '';
      const token = header.startsWith('Bearer ') ? header.slice(7) : '';
      if (!token) throw new HttpError(401, 'UNAUTHORIZED', 'Please log in.');
      let payload;
      try {
        payload = jwt.verify(token, config.jwtSecret);
      } catch (e) {
        throw new HttpError(401, 'UNAUTHORIZED', 'Your session has expired. Please log in again.');
      }
      const user = await User.findById(payload.sub);
      if (!user || !user.verified || user.tokenVersion !== payload.v) {
        throw new HttpError(401, 'UNAUTHORIZED', 'Your session has expired. Please log in again.');
      }
      req.user = user;
      next();
    } catch (e) {
      next(e);
    }
  };
}

module.exports = { signToken, requireAuth };
