# Alokchitri server

One Vercel serverless function for the app's Photo feedback screen (screen 9).

It asks a self-hosted Gemma first (Ollama on the Mac mini, behind the auth proxy and Cloudflare Tunnel that Rewardly's
Sage already uses), and falls back to Claude when Ollama is unset, offline, rate limited or slower than 40 s. Either
backend is optional: with neither reachable the function answers `503`, and the app keeps the free feedback.

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
2. Add the environment variables (Production and Preview):
   - `OLLAMA_API_URL`: the public tunnel URL of the Ollama auth proxy, without `/api/chat`.
   - `OLLAMA_API_KEY`: the proxy's secret key.
   - `OLLAMA_MODEL` (optional): default `gemma4:26b`. It must accept images.
   - `ANTHROPIC_API_KEY` (optional): turns on the Claude fallback. `FEEDBACK_MODEL` picks its model; the default is `claude-opus-5-5`.
3. Deploy. Then set `AppConfig.FEEDBACK_URL` in the app to `https://<your-domain>/api/photo-feedback`. The placeholder there assumes the project is named `alokchitri-server`.

`vercel.json` lets the function run for up to 60 s, which matches how long the app waits for an answer. Ollama gets
the first 40 s, which leaves Claude time to answer if Ollama doesn't.

Ollama handles one photo at a time (about 15 s each on `gemma4:26b` on the Mac mini) and shares the machine with
Rewardly's Sage, so a burst of photos queues up there. Without the Claude fallback, requests past 40 s get `503`.

The function asks Claude for structured JSON output at low effort and sets `fallbacks: "default"`, so a safety
decline is retried on Anthropic's recommended fallback model instead of failing.

## Not in v1

- The daily free quota is counted on the device. A determined user can reset it by clearing app data.
  To enforce it on the server, add a KV store (for example Upstash Redis from the Vercel Marketplace) keyed by a per-install ID.
- The Mac mini has to stay on and keep `cloudflared` and the proxy running. They don't restart by themselves after a reboot.
- There is no app authentication, so anyone who finds the URL can call it. Watch usage in the Anthropic Console, and add a
  spend limit there before launch.

```bash
npm install
npm run typecheck
```
