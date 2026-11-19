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
