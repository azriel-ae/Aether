# Vercel deployment — single environment variable

The repository is a single Next.js project at the repository root. The root `package.json`, `next.config.ts`, `tsconfig.json`, and `app/` directory are the only Vercel deployment entry points.

## Project settings

- Root Directory: repository root (`./`) or leave the default root setting.
- Framework Preset: Next.js.
- Install Command: default.
- Build Command: default (`next build`).
- Output Directory: default.

Do not select a subfolder. Do not use `backend` as Root Directory; that directory has intentionally been removed to avoid two competing deployment roots.

## Environment variables

Only add:

- `GEMINI_API_KEY`: Google AI Studio API key.

The model is hardcoded as `gemini-3.8-flash` in both API routes. `GEMINI_MODEL` and `APP_API_TOKEN` are not required. Redeploy after adding or changing the key.

## Health check

`GET /api/health` returns `status: configured` if the key exists and `needs_configuration` if it does not. It never reveals the key.

## Android client

The Android app needs the deployed backend URL as a local Gradle property named `API_BASE_URL` when building the APK. This is not a Vercel environment variable and is not a Gemini secret. Keep `GEMINI_API_KEY` exclusively on Vercel.

## Security

With only one environment variable and no user auth, `/api/chat` and `/api/analyze` are public endpoints. Add authentication, rate limiting, per-user quotas, and monitoring before public distribution.
