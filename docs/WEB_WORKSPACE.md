# Aether Web Workspace

The root page is a working browser interface, not just an API landing page.

## Chat
- Sends `{ messages: [{ role: "user" | "model", text }] }` to `POST /api/chat`.
- Keeps the conversation in browser memory for the current page session.
- Starts a new conversation with the "Percakapan baru" action.

## Image analysis
- User selects a JPG, PNG, or WEBP image in the browser.
- Images larger than 2.8 MB are rejected before upload.
- The image is converted to base64 and sent to `POST /api/analyze` only after the user presses "Analisis gambar".
- No background screen capture is performed.

## Configuration
Set `GEMINI_API_KEY` in Vercel and redeploy. The model is pinned in server routes as `gemini-3.8-flash`. The health endpoint checks whether the variable exists; it does not validate whether the key is accepted by Google. The first chat or image request verifies the actual Gemini call.

## Troubleshooting
- `Server AI configuration is missing`: add `GEMINI_API_KEY` to the correct Vercel project and Production environment, then redeploy.
- `Gemini request failed`: check the key, Google AI Studio project/API access, model availability, and quota. The response may include a Gemini error detail.
- `Unable to reach Gemini`: retry and inspect Vercel Runtime Logs.
- `413` or request too large: choose a smaller image. The browser interface enforces 2.8 MB.
