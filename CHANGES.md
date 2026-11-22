# Changes: AI backend, web parity, bug fixes

## AI on the backend (see AI-SETUP.md)
- New `backend/src/services/nvidia.js` (live model discovery, retired-model skipping, streaming), `services/usage.js` (daily and parallel caps), `routes/ai.js` + `routes/aiHandlers.js` (7 endpoints).
- The Android app no longer contains an NVIDIA key: `AiClient` calls `POST /api/ai/chat`; `NVIDIA_*` build settings removed.
- Shared resume logic (ATS, writing check, coach, AI prompts, number guard) lives once in `web/js/shared/` and is used by the browser and the server.

## Web app now has every phone feature
Tabs (Basics...Check), strength meter, next step, suggestion cards, target job, AI preview-before-apply with Shorter/Stronger/More formal, Ask AI chat (streaming), job match (ATS) with tailored copy, writing check, cover letter (AI + offline, print/PDF), 10 templates with live thumbnails, fonts, colours, section order, fit to 1 page, page counter, Word export, LinkedIn zip import, step-by-step setup, welcome screen, rename, thumbnails, undo for deletes, Hindi, light/dark theme, settings with AI test.

## Bugs fixed
- **Target job lost after sync**: the server stripped the unit-separator control character the app uses to store the target job, so job title/description were destroyed on the first sync. Now preserved (regression test).
- Pure-DOM `replaceChildren` showed the text "null" in the new screens; replaced with a safe `fill()` helper.
- Tab bar collapsed on phones (flex shrink).
- `PDF` generator now supports all 10 templates (band headers, serif, centred).
- Node engines raised to >=20.3 (`AbortSignal.any`).
- `backend/.env` removed from the package (it was inside the zip you shared).

## Tests
`cd backend && npm test`: 49 tests (NVIDIA service, handlers, shared logic, schema, existing tests). Browser tests were run against the real web app with a fake NVIDIA.

# AutoResume - rebuilt UI

The old UI layer (about 9,000 lines, mixed hard-coded colours, fake login/OTP, three renderers) was replaced.

## Kept
Room database (`data/`), resume HTML/PDF renderer, ATS engine, Gemini client. Existing saved resumes still open.

## New
- Material 3 design system with checked contrast pairs for light and dark: no more white-on-white text
- Screens: Resumes, Editor (8 tabs, autosave, strength meter), Preview/Export, ATS check, Settings
- One document for preview, print and PDF (`ResumeHtmlHelper`)
- Template picker with thumbnails; font size and margins apply to the output
- Profile photo is copied into app storage (no broken URIs) and can be removed
- No fake data: new resumes are blank (or an optional neutral example)
- AI buttons say so when no key is configured instead of inserting Android-developer placeholder text

## Removed
Fake login/register/OTP/forgot-password screens, fake notifications, Neumorphism hack layer, localisation shim.
Add real accounts only when there is a backend.

## Round 3 - features
Tailor to a job, cover letters (AI or offline draft, PDF), Word export, LinkedIn zip import, writing check, 5 new templates,
font choice, section reordering, tighter spacing, fit-to-1-page with page counter, real thumbnails, onboarding,
swipeable editor tabs with completion ticks, undo after delete, illustrated empty states, haptics, Hindi interface,
HTML escaping of all user text, unit tests in CI, new app icon, signed-release instructions.

Not included: importing text from a PDF resume. Android cannot read PDF text without an extra library and OCR; the LinkedIn zip
import covers the "do not retype" case without new dependencies. Design extras (font, order, spacing) are stored inside the
existing `fontSize` column, so there is no database migration and existing resumes keep working.

## Round 4 - professional templates and NVIDIA AI
- All 10 templates redesigned in one renderer (`ExtraTemplates.kt`); the old 1,000-line legacy renderer is gone. Saved resumes keep their template ids.
- Type is set in points (10.4 pt body by default, A4) instead of tiny pixel sizes, with proper heading hierarchy and spacing.
- Bundled fonts (Inter, Source Sans 3, Lora; SIL Open Font License) in `assets/fonts`, embedded into the HTML so preview, print and PDF look the same on every phone. Font choice in the editor: Default (the template's own), Inter, Source Sans, Lora.
- AI now uses NVIDIA's hosted models (`api/AiClient.kt`, OpenAI-compatible API). Set `NVIDIA_API_KEY` (and optionally `NVIDIA_MODEL`) in `.env` or as GitHub secrets. On failure the app says so instead of inserting placeholder text.

## AI and user experience update
- `api/AiClient.kt`: typed errors (`AiError`), streaming, optional key typed in Settings
- `ui/AiAssist.kt`: context package, prompts, number guard, offline suggestions, template picker
- `ui/AiSheets.kt`: target job dialog, suggestion strip, preview-before-apply sheet, Ask AI chat
- `ui/GuidedScreen.kt`: step-by-step first resume
- `ui/TargetJob.kt`: target job stored in the unused `atsFeedback` column (no database migration, nothing is wiped)
- Editor: target job chip, tappable "Next step", live preview toggle, Ask AI button, suggestions on every tab

## AI fix: retired NVIDIA model (HTTP 410)
- `meta/llama-3.3-70b-instruct` was retired by NVIDIA, so every AI call failed. `AiClient` now reads the live model list
  (GET /v1/models), skips models answering 410/404 and remembers the one that works. `NVIDIA_MODEL=auto` by default.
- Removed the in-app key box. The key comes only from the build (`NVIDIA_API_KEY` secret or variable).
- Fixed JSON `null` being read as the text "null" in streamed answers.
- Settings has a "Test AI connection" button; error cards show the technical detail.

## Full stack: backend, web app, accounts and sync
- `backend/`: Express + MongoDB API, EmailJS email codes, JWT login, resumes per user (no PDFs stored)
- `web/`: plain-JS web app (login, register, dashboard, editor, live preview, Download PDF in the browser)
- Android: login/registration screens, two-way sync (`ResumeSync`), PDFs saved on the phone (Downloads/AutoResume)
- See `FULLSTACK.md` for setup, EmailJS template, deployment and the API

## PDF storage in Cloudinary
- Backend: `services/cloudinary.js` (signed REST upload/destroy, no SDK), `PUT/DELETE /api/resumes/:id/pdf`, `pdf` link on the resume in MongoDB; files are removed when a resume, its PDF or the account is deleted
- Web: `js/pdf.js` makes a real text PDF in the browser (4 layouts, links, pages); Download PDF saves a copy and uploads it; the dashboard shows the saved link
- Android: the exported PDF is saved on the phone and also sent to the account
- Tests: signing, upload/destroy requests, link checks, and PDF structure (xref table, escaping, pagination)
