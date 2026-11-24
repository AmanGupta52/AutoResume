// Theme switcher. Classic script (not a module) so the theme is set before the first paint: no flash.
// Modes: system, light, dark, oled. The resolved mode is written to <html data-theme>, the choice to data-pref.
(function () {
  var KEY = 'autoresume_theme';
  var OPTIONS = [['system', 'System'], ['light', 'Light'], ['dark', 'Dark'], ['oled', 'OLED black']];
  var GLYPH = { system: '\u25D0', light: '\u2600', dark: '\u263E', oled: '\u25CF' };
  var BAR = { light: '#eef0fb', dark: '#0a0c1c', oled: '#000000' };
  var root = document.documentElement;
  var mq = window.matchMedia ? window.matchMedia('(prefers-color-scheme: dark)') : null;

  function get() {
    try {
      var v = localStorage.getItem(KEY);
      for (var i = 0; i < OPTIONS.length; i++) if (OPTIONS[i][0] === v) return v;
    } catch (e) { /* private mode */ }
    return 'system';
  }
  function resolve(p) { return p === 'system' ? (mq && mq.matches ? 'dark' : 'light') : p; }
  function apply() {
    var p = get(), r = resolve(p);
    root.setAttribute('data-theme', r);
    root.setAttribute('data-pref', p);
    root.style.colorScheme = r === 'light' ? 'light' : 'dark';
    var m = document.querySelector('meta[name=theme-color]');
    if (m) m.setAttribute('content', BAR[r]);
  }
  function set(v) {
    try { localStorage.setItem(KEY, v); } catch (e) { /* ignore */ }
    apply();
  }
  function cycle() {
    var p = get(), i = 0;
    for (var k = 0; k < OPTIONS.length; k++) if (OPTIONS[k][0] === p) i = k;
    var next = OPTIONS[(i + 1) % OPTIONS.length][0];
    set(next);
    return next;
  }
  function label() {
    var p = get();
    for (var i = 0; i < OPTIONS.length; i++) if (OPTIONS[i][0] === p) return 'Theme: ' + OPTIONS[i][1];
    return 'Theme';
  }

  if (mq) {
    var onChange = function () { if (get() === 'system') apply(); };
    if (mq.addEventListener) mq.addEventListener('change', onChange); else if (mq.addListener) mq.addListener(onChange);
  }
  window.AutoResumeTheme = { options: OPTIONS, get: get, set: set, cycle: cycle, glyph: function () { return GLYPH[get()]; }, label: label };
  apply();
})();
