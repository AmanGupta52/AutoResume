# AI setup (NVIDIA free API) - web app and phone app

The AI runs **on the backend**. The web app and the Android app both call your backend; only the backend knows the
NVIDIA key. Nothing secret is built into the APK or sent to a browser.

## 1. Turn it on (2 minutes)

1. Get a free key at <https://build.nvidia.com> (starts with `nvapi-`).
2. Put it in `backend/.env` (or your host's environment variables, e.g. Render / Railway / Fly):

   ```
   NVIDIA_API_KEY=nvapi-xxxxxxxxxxxxxxxx
   NVIDIA_MODEL=auto          # the server picks a live model; NVIDIA retires free models every few weeks
   AI_DAILY_LIMIT=150         # AI requests per person per day (protects your free allowance)
   AI_MAX_PARALLEL=2          # requests one person can run at the same time
   ```
3. Restart the backend. The log says `AI: ON (NVIDIA ...)`. `GET /api/health` shows `"ai": true`.
4. Web: open **Settings > Test AI connection**. Phone: **Settings > Test AI connection** (log in first).

No key = AI buttons are hidden and everything else keeps working (cover letters fall back to a fill-in draft).

> **Rotate any key that was ever in a zip, a commit or a chat.** The `backend/.env` from the zip you uploaded was
> NOT copied into this package. Create new secrets (NVIDIA key, `JWT_SECRET`, SMTP / Cloudinary / Mongo passwords)
> if the old ones were real.

## 2. Phone app

Build with only `BACKEND_URL` (see `.env.example`). The app's AI needs a logged-in account because it goes
through `POST /api/ai/chat`. The old `NVIDIA_API_KEY` / `NVIDIA_MODEL` build settings are gone.

## 3. What runs where

| Feature | Web | Phone | Where it runs |
|---|---|---|---|
| ATS / job match, writing check, strength, suggestion cards, template pick | yes | yes | on the device (same rules: `web/js/shared/*`, Kotlin port in the app) |
| AI summary, bullets, skills, fixes (preview before apply, number guard, Undo) | yes | yes | backend -> NVIDIA |
| Ask AI chat (streaming) | yes | yes | backend -> NVIDIA |
| AI cover letter (+ offline draft) | yes | yes | backend -> NVIDIA |
| 10 templates, fonts, colours, order, fit to 1 page, page counter | yes | yes | device |
| PDF, Word, print | yes | yes | device |
| LinkedIn zip import, guided setup, Hindi | yes | yes | device |

## 4. API (all need `Authorization: Bearer <login token>`)

| Method and path | Body | Answer |
|---|---|---|
| `GET /api/ai/status` | - | `{enabled, remainingToday, dailyLimit}` |
| `POST /api/ai/test` | - | `{ok, ms, model}` |
| `POST /api/ai/chat` | `{messages:[{role,content}], maxTokens?, temperature?, stream?}` | `{text, model}` (generic, used by the phone app) |
| `POST /api/ai/assist` | `{kind: SUMMARY\|BULLETS\|SKILLS\|ADVICE, index?, tweak?, base?, data, stream?}` | `{text, model, newNumbers, hasPlaceholders, skills?}` |
| `POST /api/ai/ask` | `{tab, data, messages, stream?}` | `{text, model}` |
| `POST /api/ai/ats-advice` | `{data, jobDescription, stream?}` | `{text, model}` |
| `POST /api/ai/cover-letter` | `{data, role?, company?, manager?, tone?, jobDescription?}` | `{text, ai, notice?}` |

`data` is the resume object (same shape the app syncs). With `"stream": true` the answer is server-sent events:
`text` (answer so far), then `done` (final) or `error` (`{error, message, detail}`).

Errors: `NO_KEY` 503, `BAD_KEY` 502, `RATE_LIMIT` 429, `QUOTA` 429, `BUSY` 429, `TIMEOUT` 504, `SERVER`/`NO_INTERNET`/`MODEL_GONE`/`EMPTY`/`OTHER` 502, `INVALID` 400, `UNAUTHORIZED` 401.

## 5. Safety rules built in

- The AI is told to use only facts from the resume. Numbers it adds that are not in the resume must be confirmed by the
  person; `[bracket]` placeholders must be replaced before "Use this" works.
- Nothing is written to the resume until the person taps **Use this** (and **Undo** is offered).
- One person cannot exhaust your free allowance: daily cap, 2 parallel requests, 60 AI requests / 10 min per IP.
- `/api/ai/chat` caps size (20 messages, 30,000 characters, 1,200 output tokens).

## 6. Troubleshooting

| Message | Fix |
|---|---|
| "AI is not set up on this server" | `NVIDIA_API_KEY` is empty on the server; restart after setting it. |
| "did not accept the server's API key" | Wrong or revoked key. Create a new one at build.nvidia.com. |
| "retired every AI model" | Temporary NVIDIA outage; the server re-reads the live model list on the next request. |
| Phone: "Log in to your account..." | AI needs login + `BACKEND_URL` in the build. |
| 404 on `/api/ai/...` | The deployed backend is the old version. Redeploy. |

Needs Node 20.3 or newer.
