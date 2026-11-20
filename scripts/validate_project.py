"""Offline structural checks; this is not a substitute for Gradle/Next.js builds."""
from pathlib import Path
import json
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
required = [
    "android/settings.gradle.kts",
    "android/app/build.gradle.kts",
    "android/app/src/main/AndroidManifest.xml",
    "android/app/src/main/java/com/gemini/floating/MainActivity.kt",
    "android/app/src/main/java/com/gemini/floating/FloatingService.kt",
    "android/app/src/main/java/com/gemini/floating/ScreenCaptureService.kt",
    "android/app/src/main/java/com/gemini/floating/SelectionActivity.kt",
    "android/app/src/main/java/com/gemini/floating/GeminiApi.kt",
    "android/app/src/main/java/com/gemini/floating/ChatActivity.kt",
    "android/app/src/main/java/com/gemini/floating/ChatApi.kt",
    "android/app/src/main/java/com/gemini/floating/ChatHistoryDb.kt",
    "package.json",
    "app/api/analyze/route.ts",
    "app/api/chat/route.ts",
    "app/api/health/route.ts",
    "next.config.ts",
    "vercel.json",
    "docs/PROGRESS.md",
    "docs/HANDOVER.md",
]
missing = [item for item in required if not (ROOT / item).is_file()]
if missing:
    raise SystemExit("Missing required project files: " + ", ".join(missing))
for item in ["package.json", "tsconfig.json", "vercel.json"]:
    json.loads((ROOT / item).read_text(encoding="utf-8"))
ET.parse(ROOT / "android/app/src/main/AndroidManifest.xml")
if (any((ROOT / name).exists() for name in [".env.local", ".env.production", ".env"])):
    raise SystemExit("Do not package local environment files")
package = json.loads((ROOT / "package.json").read_text(encoding="utf-8"))
if "next" not in package.get("dependencies", {}):
    raise SystemExit("Root package.json must declare Next.js")
if (ROOT / "backend").exists():
    raise SystemExit("Remove nested backend directory; deployment must use repository root")
print(f"Structural validation passed: {len(required)} required files, root Next.js dependency, 3 JSON files, Android manifest XML.")
print("Reminder: run Gradle build, npm typecheck/build, Vercel integration, and physical-device tests before release.")
