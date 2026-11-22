'use strict';
const { AiError } = require('./nvidia');

/**
 * Keeps one person from using up the free NVIDIA allowance: a daily request cap and at most `maxParallel`
 * requests at the same time. Counts live in memory (one server process); they reset when the server restarts,
 * which is fine for a cap whose job is to stop runaway use, not to bill anyone.
 */
function createUsageLimiter({ dailyLimit = 150, maxParallel = 2, now = () => new Date() } = {}) {
  const days = new Map();   // userId -> { day, count }
  const running = new Map(); // userId -> number
  const dayKey = () => now().toISOString().slice(0, 10);

  function prune(today) {
    if (days.size < 5000) return;
    for (const [k, v] of days) if (v.day !== today) days.delete(k);
  }

  /** Counts one request and returns a function that must be called when it ends. Throws QUOTA or BUSY. */
  function acquire(userId) {
    const id = String(userId);
    const today = dayKey();
    prune(today);
    const cur = days.get(id);
    const used = cur && cur.day === today ? cur.count : 0;
    if (used >= dailyLimit) throw new AiError('QUOTA');
    if ((running.get(id) || 0) >= maxParallel) throw new AiError('BUSY');
    days.set(id, { day: today, count: used + 1 });
    running.set(id, (running.get(id) || 0) + 1);
    let released = false;
    return () => {
      if (released) return;
      released = true;
      const n = (running.get(id) || 1) - 1;
      if (n <= 0) running.delete(id); else running.set(id, n);
    };
  }

  const remaining = (userId) => {
    const cur = days.get(String(userId));
    return Math.max(0, dailyLimit - (cur && cur.day === dayKey() ? cur.count : 0));
  };

  return { acquire, remaining, dailyLimit };
}

module.exports = { createUsageLimiter };
