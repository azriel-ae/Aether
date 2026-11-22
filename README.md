# Aether 

Android-native Kotlin application plus a usable Next.js web workspace for Gemini chat and user-selected image analysis.

## Vercel deployment (repository root)

This repository is configured as **one Next.js project at the repository root**. The deployment entry points are the root `package.json` and root `app/` directory. There is no nested backend directory to select.

1. Push the contents of this folder to the root of your GitHub repository (not inside an additional wrapper folder).
2. In Vercel, import or open the project connected to that repository.
3. Set **Root Directory** to the repository root (`./`) or leave it at the default root setting. Do not select a subfolder.
4. Framework Preset: Next.js (auto-detect).
5. Leave Install Command, Build Command, and Output Directory at their defaults.
6. Add exactly one Vercel Environment Variable:

   - `GEMINI_API_KEY` — your Google AI Studio API key.

7. Enable it for Production, Preview, and Development as appropriate, save, then redeploy.

The Gemini model is fixed in source as `gemini-3.8-flash`. No `GEMINI_MODEL`, `APP_API_TOKEN`, or other Vercel environment variable is required.

## Use the web workspace\n\nOpen the project root URL after deployment. The web app includes Chat Gemini and Scan gambar tabs. Chat sends messages to `/api/chat`; Scan gambar accepts user-selected JPG, PNG, or WEBP images up to 2.8 MB and sends them to `/api/analyze` only after the user clicks Analyze. See `docs/WEB_WORKSPACE.md`.\n\n## Verify deployment

Open `https://YOUR-PROJECT.vercel.app/api/health`.

- `status: configured` means Vercel can read `GEMINI_API_KEY`.
- `status: needs_configuration` means the variable is missing from the deployment environment; add it and redeploy.

The health endpoint never returns the key itself. Test `/api/chat` and `/api/analyze` after health reports configured.

## Android setup

The Android client must know the public URL of your deployed backend. Set the local Gradle property `API_BASE_URL=https://YOUR-PROJECT.vercel.app/` when building the APK, for example: `./gradlew :app:assembleDebug -PAPI_BASE_URL=https://YOUR-PROJECT.vercel.app/`. This is a backend URL, not a Vercel environment variable. Never put `GEMINI_API_KEY` inside Android source, Gradle properties, or the APK.

## Local backend development

Create `.env.local` in the repository root with `GEMINI_API_KEY=...`, then run:

```bash
npm install
npm run typecheck
npm run build
npm run dev
```

## Security and limitations

The simplified endpoints are publicly callable because the requested Vercel configuration uses only one environment variable and no user authentication. Input validation and payload limits do not replace authentication, durable rate limiting, quotas, or abuse monitoring. Keep the prototype private during testing and add those protections before public distribution.

The Android Scan Screen action (launched from the floating bubble or app home screen) can capture a user-approved frame from the previously viewed website or app using Android MediaProjection. After the system consent prompt is approved, Aether backgrounds its activity, captures one frame, and opens a rectangle selector from the capture notification. The user must press Analyze before the selected crop is uploaded. A visible Android notification is used. Protected screens (`FLAG_SECURE`) remain blocked; the app does not bypass screen protections or conceal capture activity.

This is not a verified production release until the Next.js build, Vercel deployment, live Gemini requests, Android build, and real-device tests pass.

## Security update (20 November 2026)

- Next.js pinned to `15.5.29` (patched 15.5 release).
- React and React DOM pinned to `19.1.7`.
- Vercel project Root Directory: `./`.
- Required Vercel Environment Variable: `GEMINI_API_KEY` only.
- After replacing repository files, commit and push the changes, then trigger a fresh deployment.
- If a lockfile exists in your Git repository, regenerate it using `npm install` from this project root and commit the updated lockfile. This archive does not include a generated lockfile.



