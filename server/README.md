# Alokchitri server

One Vercel serverless function for the app's Photo feedback screen (screen 9).

`POST /api/photo-feedback` with JSON:

```json
{ "image": "<base64 JPEG, ≤1280 px>", "language": "bn", "phone": "Xiaomi Redmi Note 13",
  "exif": { "iso": 3200, "shutter": "1/30", "aperture": 1.8, "focalLength": 5.4, "flash": false } }
```

It returns `{ "whatHappened": "…", "nextTime": "…" }` in the requested language. Errors come back as JSON:
`400` for a bad request or image, `422` when the model gives no feedback, `503` when the API is busy, and `502` for other upstream errors.
The app spends a free feedback only on a `200`.

## Deploy

1. In Vercel, create a new project from this GitHub repo with **Root Directory** = `server`. Use the default framework preset ("Other").
2. Add the environment variable `ANTHROPIC_API_KEY` (Production and Preview). You can also set `FEEDBACK_MODEL`; the default is `claude-opus-5-5`.
3. Deploy. Then set `AppConfig.FEEDBACK_URL` in the app to `https://<your-domain>/api/photo-feedback`. The placeholder there assumes the project is named `alokchitri-server`.

`vercel.json` lets the function run for up to 60 s, which matches how long the app waits for an answer.

The function asks Claude for structured JSON output at low effort and sets `fallbacks: "default"`, so a safety
decline is retried on Anthropic's recommended fallback model instead of failing.

## Not in v1

- The daily free quota is counted on the device. A determined user can reset it by clearing app data.
  To enforce it on the server, add a KV store (for example Upstash Redis from the Vercel Marketplace) keyed by a per-install ID.
- There is no app authentication, so anyone who finds the URL can call it. Watch usage in the Anthropic Console, and add a
  spend limit there before launch.

```bash
npm install
npm run typecheck
```
