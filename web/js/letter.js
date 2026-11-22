import { COLORS, MARGINS, SERIF_TEMPLATES, webTemplate, esc } from './templates.js';

/** A4 cover letter in the resume's accent colour and font style (print / Save as PDF). */
export function letterHtml(resume, body, company) {
  const d = resume.data;
  const accent = COLORS[d.colorSchemeId] || COLORS.Blue;
  const serif = SERIF_TEMPLATES.includes(webTemplate(d.templateId));
  const family = serif ? 'Georgia,"Times New Roman",serif' : 'system-ui,-apple-system,"Segoe UI",Roboto,Helvetica,Arial,sans-serif';
  const mm = (MARGINS[d.margins] || MARGINS.Standard) + 6;
  const contact = [d.email, d.phone, d.location].filter((x) => String(x || '').trim()).map(esc).join('  |  ');
  const paragraphs = String(body || '').split(/\n\s*\n/).filter((p) => p.trim()).map((p) => `<p>${esc(p.trim()).replace(/\n/g, '<br>')}</p>`).join('');
  const date = new Date().toLocaleDateString(undefined, { year: 'numeric', month: 'long', day: 'numeric' });
  return `<!doctype html><html><head><meta charset="utf-8"><title>${esc((d.fullName || 'Your Name') + ' - Cover letter')}</title><style>
@page{size:A4;margin:${mm}mm}*{box-sizing:border-box}body{margin:0;font-family:${family};font-size:11pt;line-height:1.55;color:#1f2937;-webkit-print-color-adjust:exact;print-color-adjust:exact}
.head{border-bottom:3px solid ${accent};padding-bottom:10px;margin-bottom:22px}h1{margin:0;font-size:26px;color:${accent}}.role{color:#6b7280;margin-top:2px}.contact{margin-top:6px;color:#6b7280;font-size:11px}
p{margin:0 0 12px}</style></head><body>
<div class="head"><h1>${esc(d.fullName || 'Your Name')}</h1><div class="role">${esc(d.jobTitle)}</div><div class="contact">${contact}</div></div>
<div style="margin-bottom:4px">${esc(date)}</div>${String(company || '').trim() ? `<div style="margin-bottom:16px"><b>${esc(company)}</b></div>` : ''}${paragraphs}</body></html>`;
}
