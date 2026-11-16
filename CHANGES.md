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
