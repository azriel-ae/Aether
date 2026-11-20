# Architecture

## Components
- `android/`: native Kotlin application using Jetpack Compose for home/chat/settings and Android services for overlay/capture.
- `backend/`: Next.js App Router API deployed as Vercel Functions.
- Gemini credentials are read only from backend environment variables.

## Android flow
1. User grants overlay permission and explicitly starts the floating foreground service.
2. User taps the bubble to expand the control panel.
3. User starts screen capture from the app; Android displays the MediaProjection consent UI.
4. `ScreenCaptureService` captures one frame, saves a temporary cache image, and posts a notification to review it.
5. `SelectionActivity` lets the user move/resize a rectangle and crops the selected pixels.
6. The cropped image is compressed and sent to `POST /api/analyze` over HTTPS.
7. The answer is stored in local chat history; follow-up messages use `POST /api/chat`.

Protected screens may return blank/blocked frames. The app intentionally does not bypass FLAG_SECURE or website/app restrictions and does not conceal capture activity.

## Backend
- `POST /api/analyze`: validates prompt, image size, and MIME type, then calls Gemini `Interactions API`.
- `POST /api/chat`: validates a bounded conversation and calls Gemini `Interactions API`.
- Both routes use server-side `GEMINI_API_KEY`; this minimal deployment mode does not authenticate app users.
- `GEMINI_API_KEY` is the only required backend environment variable. The model is fixed in backend source as `gemini-3.8-flash`.

## Local configuration
Set only the Gradle property `API_BASE_URL` in the user's local Gradle configuration. The Gemini key is never embedded in the APK. Because this minimal configuration does not authenticate callers, add per-user authentication (for example Firebase Auth/App Check), quotas, and a durable rate limiter before public distribution.

## Data handling
Captured frames are stored temporarily in the app cache for selection and are not uploaded until the user presses Analyze. The backend does not persist image payloads. The image is transmitted to the configured Vercel backend and then to Google Gemini for inference; users should not submit data they are not authorized to share. Chat history is stored locally in SQLite.
