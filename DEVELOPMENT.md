# ClearWrite development

The Android application is in `android-app/`. Android CLI is the primary interface for emulator deployment and UI inspection; Gradle builds and tests the code.

## Prerequisites

- Android Studio and Android SDK
- Java 17 or newer
- Android CLI

Verify the local setup:

```bash
android -V
android --sdk=/Users/atulpandey/Library/Android/sdk info
```

## Build and test

```bash
cd android-app
./gradlew testDebugUnitTest assembleDebug
```

Run Compose UI tests when an emulator is connected:

```bash
./gradlew connectedDebugAndroidTest
```

## Run through Android CLI

From the repository root:

```bash
android --sdk=/Users/atulpandey/Library/Android/sdk describe \
  --project_dir=/Users/atulpandey/Desktop/atul/github/Project/writeapp/android-app

android --sdk=/Users/atulpandey/Library/Android/sdk emulator list
android --sdk=/Users/atulpandey/Library/Android/sdk emulator start Medium_Phone

android --sdk=/Users/atulpandey/Library/Android/sdk run \
  --device=emulator-5554 \
  --apks=/Users/atulpandey/Desktop/atul/github/Project/writeapp/android-app/app/build/outputs/apk/debug/app-debug.apk
```

Use `--cold` with `android emulator start` when an emulator snapshot is unhealthy.

## Inspect the live UI

```bash
android --sdk=/Users/atulpandey/Library/Android/sdk layout \
  --device=emulator-5554 \
  --pretty \
  --no-idle

android --sdk=/Users/atulpandey/Library/Android/sdk screen capture \
  --device=emulator-5554 \
  --output=/tmp/clearwrite.png
```

Every captured image must be visually inspected. The UI hierarchy is used to check interaction targets and accessibility semantics; it does not replace visual review.

## Current implementation

- Material 3 Compose project
- Paste-first editor
- Automatic, debounced local analysis after paste, import, or typing
- Persistent curved 72 dp analysis panel with a centered drag handle; its compact summary includes word count and can be tapped or dragged between collapsed and overview states
- Live word count and 1,500-word free limit
- Server-verified ClearWrite Pro entitlement through Google Play Billing 9.1
- 10,000-word Pro analysis limit; AI rewriting is gated to verified Pro purchases in release builds
- TXT and DOCX import through the system file picker
- Ten local checks covering sentence and paragraph length, readability difficulty, possible passive voice, weakeners, wordy phrases, filler-heavy sentences, repeated starts, and repeated words
- ClearWrite Score v2 with four weighted subscores, audience goals, short-draft confidence, readability grade, reading time, and document statistics
- Analysis sheet that prioritizes categories with actionable findings
- Category-to-issue navigation with exact text selection and highlighting
- Focused issue explanation with previous/next navigation
- Pro-labelled AI improvements from either a focused issue or any text selection
- Selection rewrite choices for clarity, length, simplicity, tone, and a custom instruction
- AI suggestion sheet with original/rewrite preview, explicit replace, retry, dismiss, review warning, temporary green confirmation, and one-step undo
- A dependency-free Node 22 backend proxy with validation, authentication, rate limiting, mock mode, and OpenAI Responses API support
- Empty, populated, issue-navigation, and AI suggestion Compose tests
- Analyzer, source-offset, and free-plan boundary unit tests

## Run the AI flow locally

Start the mock backend before launching the Android app:

```bash
cd backend
npm test
npm start
```

The debug app connects to `http://10.0.2.2:8787`, the emulator alias for this computer. Mock mode uses no API credits. Debug builds have development access to Pro-labelled AI actions so the complete flow can be tested. To test the OpenAI provider, see [backend/README.md](backend/README.md); the API key belongs only in the backend environment.

Production builds send the current Google Play purchase token to the backend. The backend independently verifies `clearwrite_pro` before processing AI text, so changing local app state cannot unlock paid AI usage. Debug builds retain development access for emulator testing.

## Release configuration

The production AI URL is injected at build time and is not stored in source control:

```bash
./gradlew bundleRelease -PCLEARWRITE_AI_BACKEND_URL=https://your-api.example.com
```

Release signing reads `android-app/keystore.properties`. Copy `keystore.properties.example`, point it to the private upload keystore, and keep both files backed up outside the repository. Free users have one auto-saved local document; starting a new one replaces it after confirmation.

For the first release, run `./create-upload-key.sh` from `android-app/`. The script asks for the password privately, creates the upload key and local signing configuration, and does not print the password. Back up the `.jks` file and password before uploading the first bundle.
