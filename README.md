# Aether 

Android-native Kotlin application plus a usable Next.js web workspace 

## Vercel deployment

1. Push the contents of this folder to the root of your GitHub repository (not inside an additional wrapper folder).
2. In Vercel, import or open the project connected to that repository.
3. Set **Root Directory** to the repository root (`./`) or leave it at the default root setting. Do not select a subfolder.
4. Framework Preset: Next.js (auto-detect).
5. Leave Install Command, Build Command, and Output Directory at their defaults.
6. Add exactly one Vercel Environment Variable
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

