'use strict';

const EMAILJS_URL = 'https://api.emailjs.com/api/v1.0/email/send';

const COPY = {
  verify: {
    subject: 'Verify your email',
    message: 'Use this code to finish creating your account.',
  },
  reset: {
    subject: 'Reset your password',
    message: 'Use this code to choose a new password. If you did not ask for it, you can ignore this email.',
  },
};

/**
 * Sends a 6-digit code with EmailJS (REST API). One EmailJS template serves both purposes; it uses these variables:
 *   {{to_email}} {{to_name}} {{app_name}} {{subject_line}} {{message}} {{code}} {{minutes}}
 * In EMAIL_MODE=console the code is only printed (for local testing), never sent.
 *
 * EmailJS must allow server calls: Account > Security > "Allow EmailJS API for non-browser applications".
 */
function createEmailService(config, fetchImpl = globalThis.fetch) {
  async function sendCode({ to, name, code, purpose }) {
    const copy = COPY[purpose] || COPY.verify;
    if (config.email.mode !== 'emailjs') {
      console.log(`[email:console] ${purpose} code for ${to}: ${code}`);
      return;
    }
    const res = await fetchImpl(EMAILJS_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        service_id: config.email.serviceId,
        template_id: config.email.templateId,
        user_id: config.email.publicKey,
        accessToken: config.email.privateKey,
        template_params: {
          to_email: to,
          to_name: name,
          app_name: config.appName,
          subject_line: `${copy.subject} - ${config.appName}`,
          message: copy.message,
          code,
          minutes: String(config.limits.codeMinutes),
        },
      }),
    });
    if (!res.ok) {
      const text = await res.text().catch(() => '');
      // never log keys; EmailJS answers with a short reason such as "The Public Key is invalid"
      const err = new Error(`EmailJS ${res.status}: ${text.slice(0, 200)}`);
      err.status = res.status;
      throw err;
    }
  }
  return { sendCode };
}

module.exports = { createEmailService };
