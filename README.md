# AutoResume

Build a resume or CV on your phone, check how well it passes ATS screening, and export a clean A4 PDF.
Native Android app (Kotlin, Jetpack Compose, Room) with a soft Neumorphism interface.

## Features
- Resume editor: personal info, summary, experience, education, skills, projects, certifications, languages
- 5 templates: Modern Professional, Minimal ATS, Premium Creative, Developer Resume, Executive Premium
- Colour themes, font size and margin settings
- Live preview that is the same document as the PDF (what you see is what you export)
- A4 PDF export: share, save or print
- ATS score analyzer with job-description matching
- AI help for summaries, bullet points and skills (Gemini)
- Light and dark mode, multiple resumes stored on-device

## Download the APK
1. Open the **Actions** tab of this repository and choose the latest **Build APK** run.
2. Download the **AutoResume-apk** artifact and unzip it.
3. Install `app-debug.apk` on your phone (allow "install unknown apps" when asked).

For a permanent download link, push a tag: `git tag v1.0 && git push origin v1.0`.
The APK is then attached to the repository's **Releases** page.

## Set up GitHub builds
1. Push this project to a GitHub repository (branch `main`).
2. Optional, Settings > Secrets and variables > Actions:
   - `GEMINI_API_KEY` - enables the AI features in the built APK
   - `KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_PASSWORD` - also builds a signed release APK
     (create the base64 with `base64 -w0 my-upload-key.jks`; key alias must be `upload`)
3. Every push builds a debug APK automatically. You can also run it by hand from the Actions tab.

Tip: run `gradle wrapper` once locally and commit `gradlew` and `gradle/wrapper/`. The workflow then uses it.
If the build asks for a newer Gradle, change `9.3.1` in `.github/workflows/android-build.yml`.

## Build locally
1. Install Android Studio and open this folder.
2. Copy `.env.example` to `.env` and set `GEMINI_API_KEY`.
3. If you have no `debug.keystore`, create one:
   `keytool -genkeypair -keystore debug.keystore -storepass android -keypass android -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000`
4. Run the app on a device or emulator (Android 7.0+, API 24).

## Project layout
- `ui/ResumeHtmlHelper.kt` - the single resume document used by preview, print and PDF
- `ui/ResumePdfHelper.kt`, `android/print/PdfPrint.kt` - HTML to A4 PDF
- `ui/Neu.kt`, `ui/theme/` - Neumorphism design system
- `data/ResumeEntity.kt` - Room database models
- `api/GeminiClient.kt` - AI requests

Never commit `.env`, keystores or API keys.
