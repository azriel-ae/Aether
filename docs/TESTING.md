# Manual Test Checklist

## Backend
- [ ] `npm install` completes.
- [ ] `npm run typecheck` passes.
- [ ] `npm run build` passes.
- [ ] Missing `GEMINI_API_KEY` returns HTTP 503.
- [ ] Invalid JSON, oversized prompt, unsupported MIME, and oversized image are rejected.
- [ ] Valid image request returns a Gemini answer.
- [ ] `/api/health` reports `configured` when the key exists and does not expose the key.
- [ ] Chat request preserves user/model roles and returns an answer.
- [ ] Deploy `backend/` to Vercel and repeat API checks against production URL.

## Android
- [ ] Gradle sync and debug build pass with JDK 17+ and Android SDK 35.
- [ ] App requests notification permission where required.
- [ ] Overlay permission flow opens Android Settings.
- [ ] User starts and stops the foreground floating service.
- [ ] Bubble can be dragged and tapped to expand/minimize.
- [ ] Capture flow shows the Android MediaProjection consent dialog.
- [ ] Capture notification appears while capture service runs.
- [ ] Protected/FLAG_SECURE content remains blocked or blank; no bypass is attempted.
- [ ] Selection image aspect ratio is correct in portrait and landscape.
- [ ] Rectangle moves and all four corners resize; displayed pixel dimensions are plausible.
- [ ] Crop output matches the selected rectangle.
- [ ] Analyze sends only after user taps Analyze.
- [ ] Gemini answer appears and is saved to local chat history.
- [ ] Follow-up chat retains the analysis context.
- [ ] Theme, accent, and opacity preferences persist after app restart.
- [ ] Test Android 10–16 and at least two device vendors if possible.

## Release
- [ ] Production authentication and quota controls are in place.
- [ ] No secrets or local environment files are present in the release archive.
- [ ] Signed APK installs on a clean device.
- [ ] Final ZIP contains source, docs, and only a verified APK.
