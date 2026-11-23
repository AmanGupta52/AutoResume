import { h } from './ui.js';

const CFG = window.AUTORESUME_CONFIG || {};
const feature = (icon, title, text) => h('div', { class: 'lp-feature' }, h('div', { class: 'lp-ico', 'aria-hidden': 'true' }, icon), h('h3', {}, title), h('p', { class: 'muted' }, text));
const step = (n, title, text) => h('li', { class: 'lp-step' }, h('span', { class: 'lp-num' }, String(n)), h('div', {}, h('strong', {}, title), h('p', { class: 'muted' }, text)));

/** Public home page shown to visitors who are not logged in. */
export function landingView() {
  const apk = String(CFG.APK_URL || '').trim();
  return h('div', { class: 'lp' },
    h('header', { class: 'lp-nav' },
      h('span', { class: 'brand' }, 'AutoResume'),
      h('span', { class: 'spacer' }),
      h('a', { class: 'btn small', href: '#/login' }, 'Log in'),
      h('a', { class: 'btn small primary', href: '#/register' }, 'Sign up free')),
    h('section', { class: 'lp-hero' },
      h('h1', {}, 'A resume that gets past the ATS.'),
      h('p', { class: 'lp-sub' }, 'Build a clean, one-page resume, check it against any job post, write a matching cover letter and download a real A4 PDF. Works in your browser and syncs with the Android app.'),
      h('div', { class: 'actions center' },
        h('a', { class: 'btn primary lp-cta', href: '#/register' }, 'Create my resume'),
        apk ? h('a', { class: 'btn lp-cta', href: apk, rel: 'noopener' }, 'Get the Android app') : null),
      h('p', { class: 'muted small' }, 'Free. No credit card. English and Hindi.')),
    h('section', { class: 'lp-section' },
      h('h2', { class: 'lp-h2' }, 'Everything you need to apply'),
      h('div', { class: 'lp-grid' },
        feature('✓', 'ATS check', 'Paste a job description and see your score, missing keywords and exactly what to fix.'),
        feature('▦', '10 templates', 'Switch template, font, colour and section order. Fit to one page with a single tap.'),
        feature('✎', 'Writing check', 'Finds weak phrases, missing numbers, typos and bullets that are too long or too short.'),
        feature('✦', 'AI help', 'Summary, bullets, skills and cover letters. You preview every change before it is applied.'),
        feature('⇩', 'PDF and Word', 'Real text PDFs that recruiters can search, plus a Word (.docx) copy and print.'),
        feature('⇄', 'Web + phone sync', 'Start on your laptop, finish on your phone. Your resumes follow your account.'))),
    h('section', { class: 'lp-section lp-alt' },
      h('h2', { class: 'lp-h2' }, 'How it works'),
      h('ol', { class: 'lp-steps' },
        step(1, 'Sign up', 'Verify your email with a 6-digit code.'),
        step(2, 'Fill it in', 'Use the guided setup, import a LinkedIn export, or edit freely with a live preview.'),
        step(3, 'Match the job', 'Run the ATS check, tailor your keywords, then download the PDF.'))),
    h('section', { class: 'lp-section lp-final' },
      h('h2', { class: 'lp-h2' }, 'Ready when you are'),
      h('div', { class: 'actions center' }, h('a', { class: 'btn primary lp-cta', href: '#/register' }, 'Get started'), h('a', { class: 'btn lp-cta', href: '#/login' }, 'I already have an account'))),
    h('footer', { class: 'lp-foot muted small' }, 'AutoResume · Your resumes are stored in your account and never shared.'));
}
