# Architecture Decisions

1. Android is native Kotlin/Compose, not a WebView wrapper.
2. Gemini API keys stay on the server in Vercel environment variables.
3. Android screen capture uses the documented MediaProjection consent and foreground-service flow.
4. The application does not bypass FLAG_SECURE, anti-capture controls, or conceal screen-capture activity.
5. Cropped image is uploaded only after explicit user action.
6. Chat history is local SQLite for the prototype; no server-side transcript persistence.
7. A shared bearer token is a private-test gate only; public release requires per-user auth and quotas.

## UI/UX direction (2026-11-19)
- Visual direction: modern, clean, compact, purposeful; no emoji, excessive gradients, decorative cards, or redundant controls.
- Prioritize one clear action per screen, strong typography hierarchy, consistent spacing, accessible contrast, and restrained accent color.
- Floating bubble stays compact and draggable; its panel contains only screen analysis, chat, appearance settings, and dismiss.
- Security boundary: no stealth overlay, app-detection evasion, lock-screen bypass, or circumvention of protected content. Use Android's visible, user-consented overlay and MediaProjection flows only.

## UI direction (2026-11-19)
- Use a restrained, product-like interface: neutral surfaces, one blue accent, strong typography hierarchy, consistent rounded corners, and compact spacing.
- Avoid emoji, excessive cards, decorative gradients, gratuitous labels, and placeholder dashboard metrics.
- Prioritize task clarity: start/stop the bubble, choose a permitted screen region, chat, and adjust appearance.
- Keep capture consent explicit and preserve Android protected-content restrictions; visual refinement must not disguise overlays or evade host-app security.
