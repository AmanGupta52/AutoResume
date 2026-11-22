import { generateResumePdf, pdfFileName } from './pdf.js';
import { api } from './api.js';
import { toast } from './ui.js';
import { generateResumeDocx, docxFileName } from './docx.js';

/** Only links that really point at Cloudinary over https are ever shown as clickable. */
export const isStoredPdfUrl = (u) => /^https:\/\/res\.cloudinary\.com\//.test(String(u || ''));

function saveToDevice(bytes, name) {
  const url = URL.createObjectURL(new Blob([bytes], { type: 'application/pdf' }));
  const a = document.createElement('a');
  a.href = url;
  a.download = name;
  document.body.append(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 10000);
}

/**
 * Makes the PDF in the browser, saves a copy to the person's computer, and sends it to the backend,
 * which stores it in Cloudinary and keeps only the link in MongoDB. Returns the stored pdf info or null.
 */
export async function downloadAndStorePdf(resume) {
  const { bytes, lossy } = generateResumePdf(resume);
  saveToDevice(bytes, pdfFileName(resume));
  if (lossy) toast('Some characters (for example Hindi) cannot be written to this PDF and show as "?". Use Print > Save as PDF to keep them.', 'error');
  try {
    const { resume: saved } = await api.uploadPdf(resume.id, bytes);
    toast('PDF downloaded and saved to your account.');
    return saved.pdf;
  } catch (e) {
    if (e.code === 'PDF_STORAGE_OFF') toast('PDF downloaded. Cloud storage is not set up on this server.');
    else toast(`PDF downloaded, but saving it to your account failed: ${e.message}`, 'error');
    return null;
  }
}

/** Word (.docx): plain and ATS-friendly, saved to this computer only. */
export function downloadDocx(resume) {
  const bytes = generateResumeDocx(resume);
  const url = URL.createObjectURL(new Blob([bytes], { type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' }));
  const a = document.createElement('a');
  a.href = url;
  a.download = docxFileName(resume);
  document.body.append(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 10000);
  toast('Word file downloaded.');
}
