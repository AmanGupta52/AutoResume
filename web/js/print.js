import { renderResumeHtml } from './templates.js';

/**
 * Makes the PDF in the browser: the resume is printed from a hidden page, and "Save as PDF" in the print
 * window writes the file to the person's own computer. Nothing is uploaded and no PDF is stored on the server.
 * Text stays real text, so applicant tracking systems can read it.
 */
export function printResume(resume) {
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
  frame.srcdoc = renderResumeHtml(resume, 'print');
  document.body.append(frame);
}
