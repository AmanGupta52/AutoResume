# AutoResume: backend + web + mobile

```
backend/   Node.js API (Express + MongoDB). Also serves the web app.
web/       Web app (plain HTML/CSS/JS, no build step). Make resumes, download PDF.
app/       Android app. Works offline, syncs with the backend after login.
```

What goes where
- **MongoDB**: accounts and all resume content (text fields only).
- **PDF files**: never stored on the server. On the **web** the browser creates the PDF (Download PDF > Save as PDF). On **mobile** the PDF is saved on the phone (Downloads/AutoResume).
- **EmailJS**: sends the 6-digit codes for email verification and password reset.

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

## 4. Deploy (one server for API + web)

Any Node host works (Render, Railway, Fly, a VPS). Deploy the **whole repository**, because the server serves `../web`.
- Build command: `cd backend && npm install`
- Start command: `cd backend && npm start`
- Environment variables: everything in `backend/.env.example` (set `NODE_ENV=production`, `TRUST_PROXY=1`, `EMAIL_MODE=emailjs`).
- Use HTTPS (hosts above give it for free). Android blocks plain http.

The server refuses to start in production if `JWT_SECRET` is weak or EmailJS is not configured.

## 5. Mobile app

Add `BACKEND_URL` (for example `https://autoresume.onrender.com`, no trailing slash) as a GitHub **secret or variable**, next to `NVIDIA_API_KEY`, then build as usual. Without `BACKEND_URL` the app builds without login, offline only, exactly as before.

After login the app syncs in both directions:
- every resume is saved to your account (Room stays as the offline copy)
- the newer edit wins; offline edits and deletions are sent when the connection returns
- Settings > Account has *Sync now* and *Sign out* (sign out syncs first, then removes the resumes from the phone; they stay in your account)
- your AI features are unchanged and still need `NVIDIA_API_KEY`

## API (all under /api, JSON)

| Method | Path | Purpose |
|---|---|---|
| POST | /auth/register `{name,email,password}` | create account, email a code |
| POST | /auth/verify `{email,code}` | verify email, returns `{token,user}` |
| POST | /auth/resend `{email}` | new code (60 s cooldown) |
| POST | /auth/login `{email,password}` | returns `{token,user}` (403 `EMAIL_NOT_VERIFIED` if not verified) |
| POST | /auth/forgot `{email}` / /auth/reset `{email,code,password}` | password reset |
| DELETE | /auth/me `{password}` | delete account and all resumes |
| GET | /resumes | all of your resumes |
| POST | /resumes | create (`clientRef` makes retries safe) |
| GET/PUT/DELETE | /resumes/:id | read, update (409 if a newer copy exists), delete |

Send `Authorization: Bearer <token>` on everything except the auth calls.

## Security notes
- Passwords: bcrypt (cost 12), 8-72 characters, one letter and one number.
- Codes: 6 digits, stored only as an HMAC hash, expire in 15 minutes, 5 wrong tries burn the code.
- Login answers are the same for "no such user" and "wrong password"; rate limits protect login, registration and email sending.
- Resume content is cut down to known fields and sizes before it is stored; the web app never inserts text as HTML, and links are only clickable if they are http(s).
- The web app keeps the login token in `localStorage`; the phone keeps it in app storage. Anyone who can read those can use the session, so sign out on shared devices. A password reset signs out every device.

## Known limits
- The profile photo is not synced (it stays on the phone).
- The web app has 4 layouts (Modern Professional, Minimal ATS, Executive Premium, Compact Two-Column). The mobile app's other templates show with the closest layout on the web; the template name is kept.
- AI help, ATS check and cover letters are mobile-only.
- Account deletion is on the web (top bar > Delete account).
