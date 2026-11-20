# API

Base URL: deployed Vercel project URL. All requests use HTTPS. No app bearer token is required in this configuration.

## POST /api/analyze
JSON body: `{ "prompt": "...", "mimeType": "image/jpeg", "imageBase64": "..." }`. Supported image MIME types: JPEG, PNG, WebP. Image payload and prompt lengths are bounded. Returns `{ "answer": "...", "model": "gemini-3.8-flash" }`.

## POST /api/chat
JSON body: `{ "messages": [{ "role": "user" | "model", "text": "..." }] }`. Returns `{ "answer": "...", "model": "gemini-3.8-flash" }`.

## GET /api/health
Reports whether `GEMINI_API_KEY` exists without exposing its value.

The model is fixed in backend source. Only `GEMINI_API_KEY` is required in Vercel environment variables. These API routes are publicly callable in this simple setup; add authentication, durable rate limiting, quotas, and abuse monitoring before public release.
