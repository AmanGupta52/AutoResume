'use strict';
const express = require('express');
const { wrap } = require('../middleware/error');
const { createAiHandlers } = require('./aiHandlers');

function createAiRouter(requireAuth, ai, usage) {
  const router = express.Router();
  const h = createAiHandlers({ ai, usage });
  router.use(requireAuth);
  router.get('/status', wrap(h.status));
  router.post('/test', wrap(h.test));
  router.post('/chat', wrap(h.chat));
  router.post('/assist', wrap(h.assist));
  router.post('/ask', wrap(h.ask));
  router.post('/ats-advice', wrap(h.atsAdvice));
  router.post('/cover-letter', wrap(h.coverLetter));
  return router;
}

module.exports = { createAiRouter };
