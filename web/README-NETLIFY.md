# AutoResume web (Netlify)

Static site, no build step. It talks to your existing backend (`backend/` folder) and shares accounts and resumes with the Android app.

## Deploy in 4 steps
1. **Deploy your backend first** (Render / Railway / Fly...) and note its https address, e.g. `https://autoresume-api.onrender.com`. Check `https://YOUR-BACKEND/api/health` shows `{"ok":true,...}`.
2. **Edit `config.js`** in this folder: set `API_BASE` to that address (https, no trailing slash). Optional: `APK_URL` adds a "Get the Android app" button.
3. **Upload**: Netlify > Add new site > Deploy manually > drag this whole folder in.
4. **Allow the site on the backend**: in the backend environment set
   `CLIENT_ORIGIN=https://YOUR-SITE.netlify.app`
   (comma separate if you also use a custom domain) and restart the backend.

If you skip step 4, login fails because the browser blocks the call (CORS). If you skip step 2, a yellow banner says the server cannot be reached.

## Git deploys (optional)
Set Netlify environment variables `API_BASE` and `APK_URL`; `netlify.toml` writes `config.js` during the build.

## Notes
- Routes use `#/...`, so no redirect rules are needed and page refresh works.
- `_headers` sets a strict Content-Security-Policy (scripts only from this site). `connect-src` allows any https backend so you need no extra edit; replace `https:` with your backend address to tighten it.
- Do not proxy `/api` through Netlify: AI answers stream and Netlify cuts proxied requests at about 26 seconds.
- The backend can still serve the web app itself (`backend/` serves `web/`), so nothing you had breaks.
