# ClearWrite — Product & Technical Overview

**ClearWrite is an Android writing editor that helps people understand and improve a draft without handing the entire document to an AI system.** Users can type, paste, or import writing, receive a clarity score and focused issue list, edit manually, and optionally ask AI to rewrite only a selected passage. It is designed for students, professionals, and creators who want practical feedback without a full document suite.

## What it solves

Many writing tools either flag problems without enough context or rewrite too much at once. ClearWrite identifies specific clarity problems, takes the user directly to the affected text, explains the concern, and leaves every change under the user's control. AI is an optional assistant—not the default analysis engine or an automatic replacement for the user's voice.

## How the product works

1. The user types, pastes, or imports a TXT/DOCX draft.
2. A debounced on-device analyzer calculates ClearWrite Score v2 and finds long or difficult sentences, long paragraphs, possible passive voice, weakening or wordy language, filler-heavy sentences, and repetition.
3. Tapping a category highlights the exact passage and enables issue-by-issue navigation.
4. The user edits manually or, with ClearWrite Pro, requests an AI rewrite for an issue or any selection.
5. Suggestions can be reviewed, edited, applied, dismissed, or undone.

> Important scope: the current local analyzer is a deterministic clarity checker, not a comprehensive grammar or spell checker. English is the initial focus, and a sentence can receive a perfect score if it does not match one of the supported checks.

## Architecture at a glance

```text
Android app (Kotlin + Jetpack Compose + Material 3)
  ├─ Editor, issue navigation, settings and subscription UI
  ├─ Local analyzer → issues, statistics and clarity score (offline)
  ├─ DataStore → drafts, document library and preferences (on device)
  ├─ Google Play Billing → purchase and restore flow
  └─ HTTPS → ClearWrite backend (only for AI, entitlement and reviewer access)
                    │
                    ▼
          Node.js 22 service on Render
          ├─ Validates requests and applies rate/size limits
          ├─ Verifies Pro via Google Play Android Publisher API
          ├─ Proxies selected text to the OpenAI Responses API
          └─ Serves health and privacy-policy endpoints
```

The state-driven Android client uses `MainScreenViewModel`, Kotlin `StateFlow`, and coroutines for analysis, persistence, billing, and networking. Navigation 3 handles app navigation; the system file picker handles imports. DataStore keeps drafts, titles, the document library, and preferences in private on-device storage. Free users receive one auto-saved document and analysis up to 1,500 words. Pro raises the limit to 10,000 words, enables multiple documents, and unlocks AI rewriting.

## Backend, hosting and AI integration

The backend is a dependency-free Node.js 22 HTTP service hosted on **Render**. Its URL is injected into release builds; the OpenAI key, Google service-account credentials, and reviewer code remain in the hosting environment and never ship in the APK.

For an AI request, the app sends only the selection, limited nearby context, requested action or tone, locale, and technical identifiers. After validating the request and Pro entitlement, the backend calls the **OpenAI Responses API** with Structured Outputs. The model is instructed to preserve meaning, facts, names, numbers, claims, and language while returning one or two alternatives. Requests use `store: false`; ClearWrite maintains no document-content database and does not intentionally log drafts or suggestions.

Development uses a deterministic mock provider, so the full rewrite flow can be tested without API cost. Production includes timeouts, a 16 KB body limit, security headers, and an in-memory limit of 20 requests per client per minute.

## Payments and access control

ClearWrite Pro uses **Google Play Billing 9.1**. Plans and localized prices come from Play Console rather than being embedded in the app. After purchase or restore, the backend independently verifies the Play purchase token through the Android Publisher API and acknowledges new purchases. Verified access may be cached on-device for up to 24 hours for temporary offline continuity.

AI endpoints check entitlement server-side, so changing local app state cannot unlock paid AI use. An expiring reviewer token lets Google Play reviewers evaluate Pro without a real purchase; the private code remains server-side.

## Privacy and data model

- Drafts, imported content, titles, settings, and the document library stay on-device; there is no ClearWrite account or cloud sync.
- Standard analysis is fully local and works offline.
- Text leaves the device only after the user explicitly requests an AI rewrite.
- Google Play processes payment details; ClearWrite handles purchase tokens and entitlement status, not card or bank information.
- There is no third-party advertising, sale of drafts, or targeted advertising; production traffic uses HTTPS.

## Build, quality and delivery

The app is built with Gradle, Kotlin, Jetpack Compose, Material 3, coroutines, DataStore, and Navigation 3. Unit and Compose tests cover analysis, scoring, offsets, limits, ViewModel behavior, editor flows, imports, issue navigation, and AI suggestions. Node tests cover validation, AI responses, billing gates, reviewer access, health, and privacy. Signed Android App Bundles are distributed through Google Play.

**Product principle:** check locally, explain clearly, rewrite selectively, and keep the user in control.
