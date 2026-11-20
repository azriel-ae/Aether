# Development Progress

## Phase 1 — Foundation
- [x] Android Kotlin/Compose project scaffold.
- [x] Next.js App Router backend scaffold for Vercel.
- [x] Server-side Gemini key handling.
- [x] Environment template and project documentation.

## Phase 2 — UI & Settings (implementation draft)
- [x] Home screen with floating, capture, chat, and settings entry points.
- [x] Theme, accent color, and opacity preferences stored locally.
- [ ] Visual QA on physical Android devices.

## Phase 3 — Floating Engine (implementation draft)
- [x] User-started foreground overlay service.
- [x] Draggable bubble, expand/minimize panel, stop control.
- [x] Foreground notification and overlay permission check.
- [ ] Test behavior across Android OEMs and API levels.
- [ ] Add fully adjustable floating panel dimensions.

## Phase 4 — Screen Capture & Rectangle Selection (implementation draft)
- [x] Explicit MediaProjection consent flow.
- [x] Foreground mediaProjection service and one-frame capture.
- [x] Rectangle crop UI with four corner handles and move gesture.
- [x] Bitmap crop and image downscaling/compression.
- [ ] Test coordinate mapping and rotation on multiple devices.
- [ ] Confirm expected behavior on protected/FLAG_SECURE content (must remain blocked).

## Phase 5 — Gemini Integration (implementation draft)
- [x] Next.js `/api/analyze` route with request validation.
- [x] Android HTTP client and image upload flow.
- [x] Gemini API key remains server-side.
- [ ] Verify with a real Gemini API key and deployed Vercel endpoint.

## Phase 6 — Chat & History (implementation draft)
- [x] Next.js `/api/chat` multi-turn endpoint.
- [x] Android chat screen and follow-up messages.
- [x] Local SQLite conversation history.
- [x] Seed chat context with the latest image-analysis response.
- [ ] Test long conversations and failure/retry behavior.

## Phase 7 — Testing & Security
- [x] Backend requires a configured bearer token (fails closed if missing).
- [x] Validate prompt length, image MIME type, and image size (3 MB compressed image cap).
- [x] Capture uses Android's visible consent and foreground notification.
- [ ] Add production-grade user authentication, quotas, and distributed rate limiting before public release.
- [ ] Run Android Gradle build and device/emulator tests.
- [ ] Run Next.js typecheck and production build.

## Phase 8 — Release
- [ ] Configure real Vercel project and Gemini credentials.
- [ ] Configure local Android build properties.
- [ ] Build and sign release APK.
- [ ] Install and verify APK on a real Android device.
- [ ] Package source, documentation, and verified APK into final ZIP.

## Verification environment limitation
The current build workspace has Java 21 and Node.js 22, but no Android SDK, no Gradle installation/wrapper, and npm dependency installation could not complete. Therefore Android compilation, Next.js build, API integration, and device behavior are **not yet verified**. Do not label this project production-ready until those checks pass.

## UI/UX refinement — 2026-11-19
- Reworked the floating bubble into a compact 50dp monogram control with a restrained accent color, rounded geometry, and drag threshold to avoid accidental opens.
- Reworked the overlay panel into a compact hierarchy: one primary screen-analysis action, chat, appearance settings, and a simple dismiss action. Removed emoji and generic Android button clutter from the overlay.
- Kept the overlay user-started and visible. It is not designed to evade detection by host apps, Android security indicators, secure-window protections, or the lock screen.
- Remaining UI work: unify launcher, chat, settings, and selection screens under one Material 3 design system; verify on multiple Android sizes and contrast settings.
- Build/device verification remains pending because this environment does not provide an Android SDK/Gradle installation or a physical Android device.

## UI/UX refinement — 2026-11-19
- Main screen refreshed with a restrained light surface, clear heading, one primary floating-assistant action, and three compact feature rows.
- Chat screen refreshed with distinct but subtle message alignment, a quiet empty state, compact composer, and clearer send/loading/error states.
- Settings screen grouped into focused sections with Indonesian labels and less visual noise.
- Selection screen controls and instruction area restyled for a cleaner hierarchy; selection mechanics and explicit Analyze action remain unchanged.
- No emoji, decorative gradients, unnecessary dashboard cards, or fake activity indicators added.
- Still requires Android SDK/Gradle build and physical-device visual/accessibility testing. Theme/accent preferences are persisted; verifying their application across every screen remains a follow-up integration task.


## Cross-app scan flow refinement — 2026-11-20
- Floating bubble Scan Screen now launches the MediaProjection consent flow.
- After explicit approval, the Android activity backgrounds so the previously viewed app can be shown before the one-frame capture.
- Capture is delayed briefly to avoid capturing Aether's own consent-return screen.
- The selection screen retains movable/resizable rectangle selection and explicit Analyze action.
- Android launcher, chat, selection, settings, and floating overlay accents were aligned to the warm ivory / charcoal / sage palette.
- This source-level flow still requires compilation and physical-device testing across Android versions/OEMs.
