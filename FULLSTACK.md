# AutoResume: backend + web + mobile

```
backend/   Node.js API (Express + MongoDB). Also serves the web app.
web/       Web app (plain HTML/CSS/JS, no build step). Make resumes, download PDF.
app/       Android app. Works offline, syncs with the backend after login.
```

What goes where
- **MongoDB**: accounts, all resume content, and for each resume the **link** to its latest PDF.
- **Cloudinary**: the PDF files themselves (one latest PDF per resume; a new one replaces the old).
- **Web**: *Download PDF* makes a real text PDF in the browser, saves a copy on your computer, and sends it to the backend, which puts it in Cloudinary. *Print* is also there for text the PDF file cannot hold (see limits).
- **Mobile**: the PDF is saved on the phone (Downloads/AutoResume) **and** sent to your account the same way.
- **EmailJS**: sends the 6-digit codes for email verification and password reset.

The Cloudinary secret never leaves the server: browsers and phones send the PDF to the backend, and the backend uploads it.

## 1. Run it on your computer

```bash
cd backend
cp .env.example .env        # then edit .env (see below)
npm install
npm start                   # http://localhost:4000  (API + web app)
npm test                    # unit tests
```

Minimum `.env` for local testing: `MONGODB_URI` and a long random `JWT_SECRET`.
Generate the secret with: `node -e "console.log(require('crypto').randomBytes(48).toString('hex'))"`.
With `EMAIL_MODE=console` (the default) the 6-digit codes are printed in the server log, so you can test registration without EmailJS.

## 2. MongoDB

Create a free cluster on MongoDB Atlas, add a database user, allow your server's IP (or 0.0.0.0/0 while testing), and copy the connection string into `MONGODB_URI`, for example
`mongodb+srv://USER:PASSWORD@cluster0.xxxxx.mongodb.net/autoresume`.

## 3. EmailJS (real emails)

1. emailjs.com > Email Services > add a service (Gmail etc.). Copy the **Service ID**.
2. Email Templates > Create. Use **one** template for both purposes:
   - Subject: `{{subject_line}}`
   - To Email: `{{to_email}}`
   - Content:
     ```
     Hi {{to_name}},

     {{message}}

     Your code: {{code}}

     It expires in {{minutes}} minutes.

     {{app_name}}
     ```
   Copy the **Template ID**.
3. Account > General: copy the **Public Key**. Account > API keys: copy the **Private Key**.
4. **Account > Security: turn on "Allow EmailJS API for non-browser applications".** Without this EmailJS answers 403 to the server.
5. In `.env` set `EMAIL_MODE=emailjs` and the four `EMAILJS_*` values.

The free EmailJS plan has a monthly email limit; the server also limits how often one address can ask for codes.

## 4. Cloudinary (PDF storage)

1. Create a free account at cloudinary.com.
2. Dashboard > Settings > API Keys: copy the **Cloud name**, **API key** and **API secret**.
3. In `backend/.env` set either `CLOUDINARY_URL=cloudinary://API_KEY:API_SECRET@CLOUD_NAME` or the three `CLOUDINARY_*` values. `CLOUDINARY_FOLDER` (default `autoresume`) is where the files go.
4. Start the server. It prints `PDF storage: Cloudinary (...)`, or `PDF storage: OFF` if the variables are empty (everything else still works).

Good to know:
- Files are stored as Cloudinary "raw" files named `autoresume/<userId>/<resumeId>-<random>.pdf`. The random part makes the link impossible to guess, but **anyone who has the link can open the PDF**, so treat it like a private link. If you later need stricter privacy, switch to Cloudinary's *authenticated* delivery with signed links.
- Only the newest PDF of a resume is kept. Deleting a resume, removing its PDF, or deleting the account also deletes the files in Cloudinary.
- Each PDF may be up to 8 MB (a resume is normally under 100 KB).
- If opening a saved link says the file type is restricted, allow PDF delivery in Cloudinary: Settings > Security > Restricted media types.

## 5. Deploy (one server for API + web)

Any Node host works (Render, Railway, Fly, a VPS). Deploy the **whole repository**, because the server serves `../web`.
- Build command: `cd backend && npm install`
- Start command: `cd backend && npm start`
- Environment variables: everything in `backend/.env.example` (set `NODE_ENV=production`, `TRUST_PROXY=1`, `EMAIL_MODE=emailjs`).
- Use HTTPS (hosts above give it for free). Android blocks plain http.

The server refuses to start in production if `JWT_SECRET` is weak or EmailJS is not configured.

## 6. Mobile app

Add `BACKEND_URL` (for example `https://autoresume.onrender.com`, no trailing slash) as a GitHub **secret or variable**, next to `NVIDIA_API_KEY`, then build as usual. Without `BACKEND_URL` the app builds without login, offline only, exactly as before.

After login the app syncs in both directions:
- every resume is saved to your account (Room stays as the offline copy)
- the newer edit wins; offline edits and deletions are sent when the connection returns
- Settings > Account has *Sync now* and *Sign out* (sign out syncs first, then removes the resumes from the phone; they stay in your account)
- *Download PDF* in the app saves the PDF on the phone and also sends it to your account (needs the server's Cloudinary setup and a synced resume)
- your AI features are unchanged and still need `NVIDIA_API_KEY`

## API (all under /api, JSON)

| Method | Path | Purpose |
|---|---|---|
| POST | /auth/register `{name,email,password}` | create account, email a code |
| POST | /auth/verify `{email,code}` | verify email, returns `{token,user}` |
| POST | /auth/resend `{email}` | new code (60 s cooldown) |
| POST | /auth/login `{email,password}` | returns `{token,user}` (403 `EMAIL_NOT_VERIFIED` if not verified) |
| POST | /auth/forgot `{email}` / /auth/reset `{email,code,password}` | password reset |
| DELETE | /auth/me `{password}` | delete account, all resumes and their stored PDFs |
| GET | /resumes | all of your resumes |
| POST | /resumes | create (`clientRef` makes retries safe) |
| GET/PUT/DELETE | /resumes/:id | read, update (409 if a newer copy exists), delete |
| PUT | /resumes/:id/pdf | body = the PDF file (`Content-Type: application/pdf`, max 8 MB); stored in Cloudinary, link saved on the resume |
| DELETE | /resumes/:id/pdf | remove the stored PDF |

A resume from the API has `pdf: { url, bytes, uploadedAt }`, or `pdf: null`.

Send `Authorization: Bearer <token>` on everything except the auth calls.

## Security notes
- Passwords: bcrypt (cost 12), 8-72 characters, one letter and one number.
- Codes: 6 digits, stored only as an HMAC hash, expire in 15 minutes, 5 wrong tries burn the code.
- Login answers are the same for "no such user" and "wrong password"; rate limits protect login, registration and email sending.
- The PDF upload only accepts real PDFs (`%PDF-` header, size limit), is rate limited, and the stored link is only kept if it points at `https://res.cloudinary.com/`.
- Resume content is cut down to known fields and sizes before it is stored; the web app never inserts text as HTML, and links are only clickable if they are http(s).
- The web app keeps the login token in `localStorage`; the phone keeps it in app storage. Anyone who can read those can use the session, so sign out on shared devices. A password reset signs out every device.

## Known limits
- The profile photo is not synced (it stays on the phone).
- The web app has 4 layouts (Modern Professional, Minimal ATS, Executive Premium, Compact Two-Column). The mobile app's other templates show with the closest layout on the web; the template name is kept.
- The web PDF uses standard PDF fonts, so it can only hold Western European characters (letters with accents, “smart quotes”, € and so on). Other scripts, for example Hindi, show as `?`; the app warns you, and *Print > Save as PDF* keeps them (that copy is not uploaded). PDFs made on the phone do not have this limit.
- AI help, ATS check and cover letters are mobile-only.
- Account deletion is on the web (top bar > Delete account).
