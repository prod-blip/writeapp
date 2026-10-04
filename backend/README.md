# ClearWrite AI backend

This small Node 22 service keeps the OpenAI API key off the Android device. Local development uses a deterministic mock provider, so no API credits are needed to test the complete app flow.

## Run locally

```bash
cd backend
npm test
npm start
```

The Android debug build connects to `http://10.0.2.2:8787`, which maps the emulator to this computer. The default development bearer token is intentionally accepted only outside production.

## Use OpenAI

Export the values in your shell, then restart the server:

```bash
export AI_PROVIDER=openai
export AI_DEV_TOKEN=clearwrite-local-development
export OPENAI_API_KEY=your_key_here
export OPENAI_MODEL=your_chosen_model
npm start
```

Do not put `OPENAI_API_KEY` in `.env` unless that file remains untracked, and never put it in Android `BuildConfig` or the APK. Requests use the Responses API with Structured Outputs and `store: false`.

## Before production

Production AI requests require a Google Play purchase token. The server verifies the `clearwrite_pro` subscription with the Android Publisher API and acknowledges newly verified purchases before granting access.

Configure these secrets in the hosting provider rather than committing them:

```bash
export NODE_ENV=production
export AI_PROVIDER=openai
export OPENAI_API_KEY=your_key_here
export OPENAI_MODEL=your_chosen_model
export GOOGLE_PLAY_PACKAGE_NAME=com.atulpandey.clearwrite
export GOOGLE_PLAY_PRODUCT_ID=clearwrite_pro
export GOOGLE_PLAY_SERVICE_ACCOUNT_JSON='the-complete-service-account-json'
export CLEARWRITE_REVIEW_ACCESS_CODE='a-private-random-code-of-at-least-16-characters'
```

The Google service account must be linked to the Play Console developer account and granted permission to view subscriptions and manage orders. Production deployment must use HTTPS. Before a broad launch, add durable distributed rate limits, Real-time Developer Notifications for immediate revocations, privacy-safe abuse monitoring, secret management, and spend alerts.

`CLEARWRITE_REVIEW_ACCESS_CODE` enables the Play review team to unlock Pro without making a purchase. Keep it only in the hosting provider and Play Console's Sign in details. The backend exchanges it for a signed token that expires after 30 days; reviewers can enter the same code again during a later review. Never embed this code in the Android app or commit its real value.
