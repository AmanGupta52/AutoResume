'use strict';
const path = require('path');
const { pathToFileURL } = require('url');

/**
 * The resume logic (ATS check, writing check, coaching, AI prompts) is written once, as plain ES modules in
 * web/js/shared. The browser runs them directly and this server imports them, so the web app, the phone and the
 * backend can never disagree about, for example, what an ATS score is or what the AI is told.
 */
const dir = path.join(__dirname, '..', '..', '..', 'web', 'js', 'shared');
let loaded = null;

function loadShared() {
  if (!loaded) {
    const get = (f) => import(pathToFileURL(path.join(dir, f)).href);
    loaded = Promise.all([get('resume.js'), get('prompts.js'), get('ats.js'), get('writing.js'), get('coach.js')])
      .then(([resume, prompts, ats, writing, coach]) => ({ resume, prompts, ats, writing, coach }))
      .catch((e) => { loaded = null; throw e; });
  }
  return loaded;
}

module.exports = { loadShared };
