# Feature Specification

- Movable floating bubble, expandable control panel, minimize-to-bubble, and stop action.
- Android foreground notification while overlay service runs.
- User-triggered MediaProjection consent; one-frame capture; no stealth capture.
- Rectangle selection with four resize handles and drag-to-move; crop is previewed before upload.
- Gemini image analysis via Vercel API.
- Multi-turn chat with local SQLite history and context seeded from the last image analysis.
- Appearance settings: Dark/Light preference, accent color, and panel opacity.
- Protected content is not bypassed. If Android or the target app blocks capture, user must use an authorized export/share route instead.


## Cross-app screen scanner (Android)
- Start Scan Screen from the floating bubble or the Android home screen.
- Android displays its system MediaProjection consent dialog. The user must approve it.
- After approval, Aether backgrounds its own activity and returns to the previously viewed app.
- The capture service waits briefly, captures one frame, and stops projection.
- The user opens the capture notification, adjusts a rectangle, and explicitly presses Analyze.
- Only the selected crop is uploaded to the configured Aether backend.
- Android secure-window content (`FLAG_SECURE`) and other OS-protected content remain unavailable.
- A visible Android foreground notification is shown during capture. No stealth capture or protection bypass is implemented.
