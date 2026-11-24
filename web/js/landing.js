import { h } from './ui.js';

const CFG = window.AUTORESUME_CONFIG || {};
const TH = window.AutoResumeTheme;
const REDUCE = !!(window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches);

const feature = (icon, title, text, i) => h('div', { class: 'lp-feature lp-reveal', style: `--i:${i}` }, h('div', { class: 'lp-ico', 'aria-hidden': 'true' }, icon), h('h3', {}, title), h('p', { class: 'muted' }, text));
const step = (n, title, text, i) => h('li', { class: 'lp-step lp-glass lp-reveal', style: `--i:${i}` }, h('span', { class: 'lp-num' }, String(n)), h('div', {}, h('strong', {}, title), h('p', { class: 'muted' }, text)));

/** Small button in the nav that cycles System / Light / Dark / OLED. */
function themeButton() {
  if (!TH) return null;
  const b = h('button', { class: 'btn small', type: 'button' });
  const sync = () => { b.textContent = TH.glyph(); b.title = TH.label(); b.setAttribute('aria-label', TH.label()); };
  b.addEventListener('click', () => { TH.cycle(); sync(); });
  sync();
  return b;
}

/** "Pick your look" tiles: every tile switches the whole site straight away. */
function themePicker() {
  if (!TH) return null;
  const tiles = TH.options.map(([id, name]) => {
    const b = h('button', { type: 'button', class: 'lp-theme', 'aria-pressed': String(TH.get() === id) },
      h('span', { class: `lp-tp ${id}`, 'aria-hidden': 'true' }), h('span', { class: 'lp-tl' }, name));
    b.addEventListener('click', () => { TH.set(id); tiles.forEach((t, i) => t.setAttribute('aria-pressed', String(TH.options[i][0] === id))); });
    return b;
  });
  return h('div', { class: 'lp-themes' }, tiles);
}

/** Layered glass cards that float, follow the pointer and drift with the scroll position. */
function heroVisual() {
  const sheet = h('div', { class: 'lp-sheet', 'aria-hidden': 'true' },
    h('i', { class: 'n' }), h('i', { class: 'r' }), h('i', { class: 'gap' }),
    h('i', { class: 't' }), h('i', {}), h('i', { class: 's' }), h('i', { class: 'gap' }),
    h('i', { class: 'r' }), h('i', {}), h('i', { class: 't' }), h('i', { class: 's' }), h('i', {}), h('i', { class: 'gap' }),
    h('i', { class: 'r' }), h('i', { class: 't' }), h('i', {}));
  return h('div', { class: 'lp-visual lp-reveal', style: '--i:2' },
    h('div', { class: 'lp-layer lp-glass lp-hero-main lp-float' }, sheet),
    h('div', { class: 'lp-layer lp-glass lp-score lp-float', style: 'animation-delay:-3s;animation-duration:8s' },
      h('div', { class: 'lp-ring' }, h('b', {}, '92')), h('div', { class: 'small muted' }, 'ATS score')),
    h('div', { class: 'lp-layer lp-glass lp-kw lp-float', style: 'animation-delay:-5s;animation-duration:9s' },
      h('span', { class: 'lp-cap' }, 'Keywords'),
      h('span', { class: 'lp-chip' }, '✓ Python'), h('span', { class: 'lp-chip' }, '✓ SQL'), h('span', { class: 'lp-chip' }, '✓ Leadership'),
      h('span', { class: 'lp-chip miss' }, '+ Docker')));
}

/** Scroll + pointer parallax. Cleans itself up when the landing page is no longer in the DOM. */
function wireParallax(root, hero) {
  if (REDUCE) return;
  let tick = false;
  const onScroll = () => {
    if (!root.isConnected) { window.removeEventListener('scroll', onScroll); return; }
    if (tick) return;
    tick = true;
    requestAnimationFrame(() => { root.style.setProperty('--sy', String(Math.min(window.scrollY, 2200))); tick = false; });
  };
  window.addEventListener('scroll', onScroll, { passive: true });
  hero.addEventListener('pointermove', (e) => {
    const r = hero.getBoundingClientRect();
    root.style.setProperty('--mx', (((e.clientX - r.left) / r.width - 0.5) * 2).toFixed(3));
    root.style.setProperty('--my', (((e.clientY - r.top) / r.height - 0.5) * 2).toFixed(3));
  });
  hero.addEventListener('pointerleave', () => { root.style.setProperty('--mx', '0'); root.style.setProperty('--my', '0'); });
}

/** Fade sections in as they scroll into view. Without IntersectionObserver everything stays visible. */
function wireReveal(root) {
  if (REDUCE || !('IntersectionObserver' in window)) return;
  root.classList.add('js');
  const io = new IntersectionObserver((entries) => {
    for (const e of entries) if (e.isIntersecting) { e.target.classList.add('in'); io.unobserve(e.target); }
  }, { threshold: 0.12, rootMargin: '0px 0px -5% 0px' });
  root.querySelectorAll('.lp-reveal').forEach((el) => io.observe(el));
}

/** Public home page shown to visitors who are not logged in. */
export function landingView() {
  const apk = String(CFG.APK_URL || '').trim();
  const hero = h('section', { class: 'lp-hero' },
    h('div', { class: 'lp-hero-copy lp-reveal' },
      h('span', { class: 'lp-badge' }, 'ATS-friendly · Free · English and Hindi'),
      h('h1', {}, 'A resume that gets past the ATS.'),
      h('p', { class: 'lp-sub' }, 'Build a clean, one-page resume, check it against any job post, write a matching cover letter and download a real A4 PDF. Works in your browser and syncs with the Android app.'),
      h('div', { class: 'actions' },
        h('a', { class: 'btn primary lp-cta', href: '#/register' }, 'Create my resume'),
        apk ? h('a', { class: 'btn lp-cta', href: apk, rel: 'noopener' }, 'Get the Android app') : null),
      h('p', { class: 'muted small' }, 'Free. No credit card. English and Hindi.')),
    heroVisual());

  const root = h('div', { class: 'lp' },
    h('span', { class: 'lp-blob b1', 'aria-hidden': 'true' }),
    h('span', { class: 'lp-blob b2', 'aria-hidden': 'true' }),
    h('span', { class: 'lp-blob b3', 'aria-hidden': 'true' }),
    h('header', { class: 'lp-nav' },
      h('span', { class: 'brand' }, 'AutoResume'),
      h('span', { class: 'spacer' }),
      themeButton(),
      h('a', { class: 'btn small', href: '#/login' }, 'Log in'),
      h('a', { class: 'btn small primary', href: '#/register' }, 'Sign up free')),
    hero,
    h('section', { class: 'lp-section' },
      h('h2', { class: 'lp-h2 lp-reveal' }, 'Everything you need to apply'),
      h('div', { class: 'lp-grid' },
        feature('✓', 'ATS check', 'Paste a job description and see your score, missing keywords and exactly what to fix.', 0),
        feature('▦', '10 templates', 'Switch template, font, colour and section order. Fit to one page with a single tap.', 1),
        feature('✎', 'Writing check', 'Finds weak phrases, missing numbers, typos and bullets that are too long or too short.', 2),
        feature('✦', 'AI help', 'Summary, bullets, skills and cover letters. You preview every change before it is applied.', 3),
        feature('⇩', 'PDF and Word', 'Real text PDFs that recruiters can search, plus a Word (.docx) copy and print.', 4),
        feature('⇄', 'Web + phone sync', 'Start on your laptop, finish on your phone. Your resumes follow your account.', 5))),
    h('section', { class: 'lp-section lp-alt' },
      h('h2', { class: 'lp-h2 lp-reveal' }, 'How it works'),
      h('ol', { class: 'lp-steps' },
        step(1, 'Sign up', 'Verify your email with a 6-digit code.', 0),
        step(2, 'Fill it in', 'Use the guided setup, import a LinkedIn export, or edit freely with a live preview.', 1),
        step(3, 'Match the job', 'Run the ATS check, tailor your keywords, then download the PDF.', 2))),
    TH ? h('section', { class: 'lp-section' },
      h('h2', { class: 'lp-h2 lp-reveal' }, 'Pick your look'),
      h('p', { class: 'lp-lead lp-reveal', style: '--i:1' }, 'Light, dark or pure OLED black. Your choice is remembered on this device.'),
      h('div', { class: 'lp-reveal', style: '--i:2' }, themePicker())) : null,
    h('section', { class: 'lp-section lp-final' },
      h('div', { class: 'lp-banner lp-glass lp-reveal' },
        h('h2', { class: 'lp-h2' }, 'Ready when you are'),
        h('div', { class: 'actions center' }, h('a', { class: 'btn primary lp-cta', href: '#/register' }, 'Get started'), h('a', { class: 'btn lp-cta', href: '#/login' }, 'I already have an account')))),
    h('footer', { class: 'lp-foot muted small' }, 'AutoResume · Your resumes are stored in your account and never shared.'));

  wireParallax(root, hero);
  wireReveal(root);
  return root;
}
