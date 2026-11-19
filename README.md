# AutoResume

Build a resume or CV on your phone, check how well it passes ATS screening, and export a clean A4 PDF.
Native Android app (Kotlin, Jetpack Compose, Room) with a soft Neumorphism interface.

## Features
**Build**
- Resume list with real first-page thumbnails: create (blank or example), rename, duplicate, delete
- Import from a LinkedIn data export (.zip) so you do not retype your history
- Editor with autosave, swipeable tabs, green ticks per finished section, undo after deleting, and a live "resume strength" meter
- 10 templates with thumbnails, font choice (Sans / Serif / Mono), text size, margins, tighter spacing, section reordering, accent colours
- "Fit to 1 page" and a live page counter in the preview

**Output**
- Preview, print and PDF are the same document, so they always match
- A4 PDF (save or share), Word (.docx) and print

**Get hired**
- ATS check against a pasted job description: score, keyword gaps, fixes
- Tailor to a job: tap the missing keywords that are true for you and create a tailored copy; the original stays untouched
- Writing check: weak phrases ("responsible for"), missing numbers, misspellings, long or short bullets, invalid email
- Cover letter: written by AI when an NVIDIA key is set, otherwise a fill-in draft from your resume; exports as a matching PDF
- AI help (optional): summary, bullet points, skill ideas

**App**
- First-run onboarding, English and Hindi interface, system / light / dark theme
- Everything stays on the phone

## Download the APK
1. Open the **Actions** tab of this repository and choose the latest **Build APK** run.
2. Download the **AutoResume-apk** artifact and unzip it.
3. Install `app-debug.apk` on your phone (allow "install unknown apps" when asked).

For a permanent download link, push a tag: `git tag v1.0 && git push origin v1.0`.
The APK is then attached to the repository's **Releases** page.

## Set up GitHub builds
1. Push this project to a GitHub repository (branch `main`).
2. Optional, Settings > Secrets and variables > Actions:
   - `NVIDIA_API_KEY` - enables the AI features in the built APK (optional `NVIDIA_MODEL` to pick another free model)
   - `KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_PASSWORD` - also builds a signed release APK
     (create the base64 with `base64 -w0 my-upload-key.jks`; key alias must be `upload`)
3. Every push builds a debug APK automatically. You can also run it by hand from the Actions tab.

Tip: run `gradle wrapper` once locally and commit `gradlew` and `gradle/wrapper/`. The workflow then uses it.
If the build asks for a newer Gradle, change `9.3.1` in `.github/workflows/android-build.yml`.

## Signed release APK (for sharing widely or the Play Store)
1. Create a key once and keep it safe (losing it means you cannot update the app):
   `keytool -genkeypair -v -keystore my-upload-key.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000`
2. Convert it: `base64 -w0 my-upload-key.jks` and add these repository secrets:
   `KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_PASSWORD`.
3. Push. The workflow then also builds a signed `app-release.apk`.
Debug APKs are fine for testing but are not accepted by the Play Store.

## Tests
Unit tests run on every push (Actions > "unit-test-report" artifact). Locally: `gradle testDebugUnitTest`.

## Build locally
1. Install Android Studio and open this folder.
2. Copy `.env.example` to `.env` and set `NVIDIA_API_KEY` (get a free key at https://build.nvidia.com).
3. If you have no `debug.keystore`, create one:
   `keytool -genkeypair -keystore debug.keystore -storepass android -keypass android -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000`
4. Run the app on a device or emulator (Android 7.0+, API 24).

## Project layout
- `MainActivity.kt` - entry point, navigation shell
- `ui/ResumeViewModel.kt` - state, autosave, undo, AI, ATS, import, cover letter
- `ui/ExtraTemplates.kt`, `ui/DesignOptions.kt` - five extra templates, font / order / spacing options
- `ui/WritingCheck.kt`, `ui/CoverLetter.kt`, `ui/DocxExporter.kt`, `ui/LinkedInImport.kt` - writing check, letters, Word export, LinkedIn import
- `ui/Translations.kt`, `ui/HindiStrings.kt` - interface languages
- `ui/ResumesScreen.kt`, `EditorScreen.kt`, `PreviewScreen.kt`, `AtsScreen.kt`, `SettingsScreen.kt` - screens
- `ui/Ui.kt`, `ui/theme/` - design system (Material 3 colour roles, soft cards, template thumbnails)
- `ui/ResumeHtmlHelper.kt` - the single resume document used by preview, print and PDF
- `ui/ResumePdfHelper.kt`, `android/print/PdfPrint.kt` - HTML to A4 PDF
- `ui/AtsAnalyzerEngine.kt` - keyword and structure scoring
- `data/ResumeEntity.kt` - Room database models
- `api/AiClient.kt` - AI requests (NVIDIA hosted models, OpenAI-compatible API)

Never commit `.env`, keystores or API keys.
