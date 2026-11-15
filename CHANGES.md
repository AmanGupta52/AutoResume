# AutoResume - what changed

## Root cause of "preview != template != PDF"
The app had THREE separate renderers: a Compose preview, an HTML renderer (Print button) and a
hand-drawn Canvas PDF (Share button). They could never match.

Now there is ONE: `ResumeHtmlHelper.generateHtml()`.
- Preview  = that HTML in a WebView (`ResumeWebPreview`, forPreview = true)
- Print    = that HTML via the system print dialog
- Share/Save PDF = that HTML written to a real A4 PDF (`ResumePdfHelper.exportPdf` + `android/print/PdfPrint.kt`)

## Fixes in the document itself
- Font size (Compact/Standard/Large) and margins (Narrow/Standard/Wide) now reach the document (they were ignored)
- Legacy template ids are mapped (`normalizeTemplate`), so the right template always renders
- Emoji removed from contact lines/projects/certs (rendered as boxes in PDF, bad for ATS)
- `href="#"` links now point to the real LinkedIn / GitHub / website URLs
- Fonts that do not exist on Android replaced with Roboto / Noto Serif / Roboto Mono
- Page-break rules so headings and entries are not cut between pages
- Preview screen no longer overwrites a real profile photo with "initials"

## Neumorphism (Soft UI)
- `ui/theme/AppTheme.kt`, `Theme.kt`: single-tone soft palette (light + dark), rounder shapes
- `ui/Neu.kt`: raised / inset modifiers, `Card`, `Button`, `NeuChoice`, `NeuSwatch`
  `Card` and `Button` live in `com.example.ui`, so every existing screen picks up the soft look automatically
- Hard-coded greys/blues across screens mapped onto the palette

## Security
- Gemini HTTP logging no longer prints the API key in release builds

## Not touched / needs your decision
- Login/Register are fake (any email + password works) - needs a real backend
- Model name `gemini-3.5-flash` in GeminiClient.kt - verify it exists for your key
- Dead code: old Compose resume renderer in AutoResumeSecondaryFlows.kt, app/script.js, app/script2.js
- Raised shadows use Paint.setShadowLayer: full effect on Android 9+ (API 28+), flatter on API 24-27
