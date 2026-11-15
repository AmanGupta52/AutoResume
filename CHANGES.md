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
