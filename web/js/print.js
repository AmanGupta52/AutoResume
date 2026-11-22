import { renderResumeHtml } from './templates.js';

/** Prints any complete HTML document from a hidden frame; "Save as PDF" in the print window writes it to the person's computer. */
export function printHtml(html) {
  const frame = document.createElement('iframe');
  frame.setAttribute('aria-hidden', 'true');
  frame.style.cssText = 'position:fixed;right:0;bottom:0;width:0;height:0;border:0;visibility:hidden';
  const cleanup = () => setTimeout(() => frame.remove(), 1000);
  frame.addEventListener('load', () => {
    const win = frame.contentWindow;
    if (!win) return cleanup();
    win.addEventListener('afterprint', cleanup);
    win.focus();
    win.print();
    setTimeout(() => frame.remove(), 120000); // in case afterprint never fires
  });
  frame.srcdoc = html;
  document.body.append(frame);
}

/**
 * Prints the resume. Text stays real text, so applicant tracking systems can read it. Nothing is uploaded.
 */
export const printResume = (resume) => printHtml(renderResumeHtml(resume, 'print'));
