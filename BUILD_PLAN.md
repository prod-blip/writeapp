# ClearWrite — Product and Engineering Build Plan

## 1. Product summary

ClearWrite is an Android writing-review app for people who already have a draft. The user pastes text or imports a document, runs a clarity check, reviews specific problems, and edits the draft inside the app.

The product should be positioned as a **clear writing coach**, not as an AI content generator. Its core promise is:

> Paste a draft, see what is unclear, and improve it one issue at a time.

The first release should optimize for speed and focus. When the app opens, the user sees the editor immediately. There is no dashboard, onboarding wall, or large marketing message between the user and the primary task.

## 2. Product principles

1. **The draft comes first.** The editor occupies most of the screen.
2. **Analysis is free; AI rewriting is paid.** Free users can understand and manually fix every detected issue.
3. **AI assists instead of replacing the writer.** Suggestions are shown for approval and are never silently applied.
4. **Every result must be actionable.** Tapping an issue takes the user to the exact sentence involved.
5. **User text belongs to the user.** Copying or exporting a draft must never be locked behind payment.
6. **The app should work without an account for the basic journey.** Sign-in should be requested only when it provides clear value, such as cross-device access in a later release.
7. **Privacy must be understandable.** Clearly explain when text is processed locally and when selected text is sent to an AI service.

## 3. Target users and primary use cases

### Initial target users

- Students reviewing assignments and applications
- Professionals improving emails, reports, and proposals
- Job seekers reviewing cover letters and profile text
- People writing in English as an additional language
- Anyone who finds conventional grammar tools too crowded or technical

### Primary use cases

- Paste an email and identify unclear or overly long sentences
- Import a draft and review clarity issues one by one
- Manually edit a draft using explanations from the app
- Ask AI to rewrite a selected problem sentence as a paid action
- Return to one saved work-in-progress document

### Not in the initial product

- A blank-page AI writing generator
- Collaborative editing
- Full Microsoft Word-style formatting
- Plagiarism detection
- Citation management
- Desktop or web applications
- Cloud synchronization
- PDF editing

These can be reconsidered after the core review loop is validated.

## 4. Agreed user journey

The agreed product journey contains 12 wireframes. The first seven have been drawn and saved in the repository; wireframes 8–12 were agreed conceptually and remain to be drawn. They are still part of the approved product scope.

| Step | State | User action | App response | Wireframe status |
|---|---|---|---|---|
| 1 | Empty editor | Opens the app | Shows a large editor, Paste action, compact Import action, and menu | [Drawn: 01](wireframes/01-empty-editor-wireframe.png) |
| 2 | Draft ready | Pastes, types, or imports text | Updates word count and schedules local analysis automatically | [Drawn: 02](wireframes/02-text-added-wireframe.png) |
| 3 | Import | Taps Import | Opens a compact document-source sheet; successful import returns to the editor | [Drawn: 03](wireframes/03-import-document-sheet-wireframe.png) |
| 4 | Checking | Pauses after editing | Shows progress in the collapsed panel and analyzes the current draft locally after a debounce | [Drawn: 04](wireframes/04-checking-loading-wireframe.png) |
| 5 | Analysis overview | Pulls up the collapsed panel | Expands the persistent sheet to show the clarity score and issue categories | [Drawn: 05](wireframes/05-analysis-overview-wireframe.png) |
| 6 | Issue review | Taps an issue such as “6 long sentences” | Collapses the overview, scrolls to the first matching sentence, highlights it, and explains the problem | [Drawn: 06](wireframes/06-selected-issue-wireframe.png) |
| 7 | AI suggestion | Taps Improve with AI | Pro users receive a suggested revision with Replace, Edit, and Dismiss actions; free users branch to the contextual paywall first | [Drawn: 07](wireframes/07-ai-suggestion-wireframe.png) |
| 8 | Suggestion outcome | Replaces, edits, dismisses, or manually fixes the sentence | Updates the draft and issue status, preserves Undo where text changed, then offers the next issue | Agreed; not yet drawn |
| 9 | Review complete | Finishes editing the draft | Automatically refreshes the analysis without presenting the old score as current | Agreed; not yet drawn |
| 10 | Saved documents | Opens Saved document(s) from the menu | Free users see their single saved document; Pro users can browse their expanded document history | Agreed; not yet drawn |
| 11 | Settings and check preferences | Opens Settings or Check preferences | Lets the user control supported analysis preferences, appearance, accessibility options, and subscription access | Agreed; not yet drawn |
| 12 | Privacy, help, and data controls | Opens the relevant menu item | Shows privacy explanations, AI-processing disclosure, data controls, help, and feedback options | Agreed; not yet drawn |

After a sentence is replaced, manually edited, or ignored, the app moves to the next issue in that category. When all issues have been handled, it offers to check the updated draft again. The app must not pretend that an old score reflects newly edited text.

## 5. Main screen specification

### Top app bar

- Small ClearWrite wordmark or title on the left
- Compact Import icon on the right
- Overflow menu on the far right
- No large headline or introductory message

The overflow menu initially contains:

- Saved document
- Start new document
- Check preferences
- Subscription / Upgrade
- Settings
- Privacy and data
- Help and feedback

### Editor area

- Occupies all available space between the app bar and bottom action area
- Placeholder: “Paste or type your text here…”
- Small Paste control visible in the empty state
- Plain-text editing in the MVP
- Current word count shown unobtrusively
- Imported filename shown only when relevant
- Autosave status shown only when it changes or fails
- Analysis highlights are applied as text annotations, not by changing the underlying content

### Bottom analysis area

There is no permanent **Check writing** or **View analysis** button. After text is pasted, imported, or edited, the local analysis runs automatically: approximately 300 ms after a paste/import and 1 second after typing pauses.

A persistent analysis sheet appears only when the editor contains text. It has two heights:

1. **Collapsed:** a curved 72 dp window with a centered drag handle, showing checking progress or the current score, word count, issue count, and writing goal
2. **Overview:** a draggable/tappable expansion showing document statistics and issue categories with counts

The panel never expands itself while the user is typing. It remains visible above the software keyboard, uses the drag handle instead of “pull up/down” instructions, and selecting an issue collapses it before returning to the highlighted text and focused issue explanation. Word count belongs in this compact summary rather than in a separate editor row.

Rewrite, Tone, Shorten, and similar AI tools do not appear as permanent bottom navigation items.

The focused issue actions are:

- Edit myself — available to everyone
- Ignore — available to everyone
- Improve with AI — visible to everyone but marked Pro for free users

Showing the Pro action lets users understand the value before seeing the paywall. It should not imply that AI usage is included in the free plan.

## 6. Writing analysis design

### Free analysis

The free clarity check should use deterministic or locally computed rules wherever practical. This makes results fast, explainable, inexpensive, and available without exposing the AI feature for free.

Initial issue categories:

- Long sentences
- Long paragraphs
- Difficult or uncommon words
- Passive voice
- Repeated words or phrases
- Excessive filler or hedge words
- Readability level
- Sentence-length variation

Grammar and spelling should only be included if accuracy is adequate. They should not delay the clarity-focused MVP.

### Analysis pipeline

1. Normalize line endings and non-destructive whitespace for analysis.
2. Split the text into paragraphs and sentences.
3. Assign stable IDs to each paragraph and sentence.
4. Record the character start and end offsets against the original text.
5. Run each rule and produce issues linked to sentence IDs.
6. Calculate category counts and the overall clarity score.
7. Return a structured analysis result.
8. Render issue highlights using the stored offsets.

The original text must not be silently normalized or changed.

### Suggested issue model

```json
{
  "id": "issue_123",
  "documentRevision": 4,
  "sentenceId": "sentence_12",
  "type": "LONG_SENTENCE",
  "severity": "medium",
  "startOffset": 412,
  "endOffset": 583,
  "title": "Long sentence",
  "explanation": "This sentence contains 34 words and may be difficult to follow.",
  "metadata": {
    "wordCount": 34,
    "recommendedMaximum": 25
  }
}
```

### Clarity score

ClearWrite Score v2 is a deterministic 0–100 score calculated from four internal dimensions:

- Sentence clarity: 30%
- Concision and precision: 25%
- Audience readability: 25%
- Flow and structure: 20%

Sentence-level difficulty signals use the highest primary severity for each sentence, so a sentence is not penalized twice for being both long and hard to read. Possible passive voice adds only a small capped amount because passive construction can be appropriate. Phrase issues are normalized per 100 words, paragraph penalties grow with the amount over the limit, and repeated starts are normalized by sentence count.

Audience readability uses the selected writing goal: Accessible (grade 7), General (grade 9), Professional (grade 11), or Academic / technical (grade 14). Drafts below 100 words show a provisional score; longer drafts show a stable score. The reading grade remains a separate statistic rather than being presented as the clarity score itself.

The analysis sheet keeps this calculation out of the main interface. It shows the overall score, score status and goal, basic document counts, and the actionable issue categories.

Before launch, test the scoring against a curated set of clear and unclear samples. The score is a guide, not an objective measure of writing quality; the interface should say this in plain language.

### Keeping results valid after editing

Every text change increments a local document revision. Existing issues are tied to the revision that produced them.

- Small edits outside a highlighted sentence may leave the analysis visible but mark it as potentially outdated.
- Editing a highlighted sentence resolves that displayed issue and removes its highlight.
- Rechecking creates a completely new analysis result.
- AI results are discarded if the underlying sentence changes before the response arrives.

For the first release, prefer correctness over clever incremental re-analysis: clearly show “Draft changed — check again” after meaningful edits.

## 7. AI improvement experience

AI is used only when a Pro user explicitly requests help for a selected issue or a passage they selected manually. It is not required for the free check.

For a manual selection, long-pressing text keeps Android's normal selection handles and copy/paste controls available. A compact `Rewrite with AI · Pro` action appears in the editor. It opens choices for Improve clarity, Shorten, Simplify, Change tone, and Custom instruction. Selections are limited to 3,000 characters per request.

### Request scope

Send the backend:

- The selected sentence or passage
- One sentence before and after it when context is needed
- The issue type and explanation
- The requested action, such as improve clarity, shorten, or change tone
- The user-selected tone, if applicable
- A short custom rewrite instruction, if the user chose that option
- A request ID and document revision

Do not send the entire document when a small excerpt is sufficient.

### Response contract

The AI service should return structured data:

```json
{
  "requestId": "req_456",
  "replacement": "A clearer version of the selected sentence.",
  "explanation": "This version separates the main idea from the supporting detail.",
  "warnings": []
}
```

### User controls

- **Replace:** substitutes only the selected range
- **Edit:** inserts the suggestion into an editable preview before substitution
- **Dismiss:** closes the suggestion without altering the document
- **Try again:** may be available to Pro users, but counts toward fair-use limits

The app must show the proposed replacement before modifying the draft. Undo should remain available after replacement.

### Prompt and output safeguards

- Treat document text as user content, never as instructions to the system.
- Require structured output and validate it on the server.
- Limit input and output length.
- Reject empty, malformed, or unexpectedly large responses.
- Preserve the writer’s meaning and facts; do not invent information.
- Do not claim that a rewrite is objectively better. Present it as a suggestion.

## 8. Free and Pro plans

### Free

- Paste, type, and edit text
- Import TXT and DOCX documents
- Run clarity checks
- View the score, all detected issue categories, and explanations
- Jump to every affected sentence
- Fix issues manually or ignore them
- Check documents up to 1,500 words
- Save one document
- Copy and export the user’s text
- No AI improvements

### Pro

- Contextual AI improvements
- Rewrite, shorten, and tone adjustments within the issue flow
- Higher document limit, initially proposed as 10,000 words
- More saved documents and history
- “Improve all” only after individual suggestions are reliable and controllable
- Future premium import formats, such as PDF, if extraction quality is acceptable

Avoid marketing Pro as unlimited. Use a clearly stated fair-use or monthly AI allowance so cost and abuse remain controllable.

### Paywall touchpoints

The user should not see a paywall on app launch or when running the basic clarity check. Show it when the user:

- Taps Improve with AI
- Taps a contextual Rewrite, Shorten, or Tone action
- Tries to save a second document
- Tries to check more than 1,500 words
- Selects a clearly labelled premium capability

The paywall should explain the feature that triggered it and offer monthly and annual plans. It must be dismissible. If dismissed, the user returns to the exact sentence or document state they were reviewing.

Potential initial pricing to validate, not a final commitment:

- ₹299 per month
- ₹2,499 per year

Test pricing through Play Console experiments after real usage and AI cost data are available.

### Entitlement behavior

- A free user can replace the one saved document after confirming the replacement.
- Unsaved content remains editable during the current session and should be recoverable after an accidental app close.
- Losing Pro must never delete documents. Extra documents become read-only or export-only until the user chooses which one remains actively saved.
- A billing or network error must not erase an AI result already shown or any user text.

## 9. Android implementation

### Recommended stack

- Kotlin
- Jetpack Compose and Material 3
- Single-activity architecture
- Navigation Compose
- ViewModel and StateFlow
- Coroutines
- Room for documents, revisions, and cached analyses
- DataStore for preferences and lightweight state
- Retrofit/OkHttp or Ktor Client for backend communication
- WorkManager only for appropriate deferrable work, such as opt-in backups or queued telemetry
- Google Play Billing for subscriptions

Pin dependency versions through a Gradle version catalog. Select supported stable versions when implementation begins rather than hard-coding versions in this plan.

### Android CLI development workflow

Android CLI is the standard agent-development and UI-verification interface for this project. Gradle remains responsible for compilation and deterministic tests, while Android CLI manages project inspection, emulators, APK deployment, live UI hierarchy inspection, screenshots, and Android-specific agent guidance.

The normal implementation loop is:

1. Implement a small Compose screen or behavior.
2. Run local unit tests and build the debug APK with Gradle.
3. Use `android describe` to locate and verify build artifacts.
4. Start a representative emulator with `android emulator start`.
5. Deploy with `android run`, using incremental installation on later iterations.
6. Inspect interactive and accessible elements with `android layout`.
7. Capture and visually examine the real screen with `android screen capture`.
8. Run Compose UI tests on the emulator.
9. Compare the result with the approved wireframe and simplicity rules before continuing.

Use Android Studio alongside the CLI for deep debugging, profiling, and Compose preview rendering. Test file import, the software keyboard, accessibility services, Google Play Billing, and performance on physical Android devices before release.

### Suggested module structure

```text
app/
core/
  common/
  database/
  designsystem/
  network/
  textanalysis/
feature/
  editor/
  analysis/
  importdocument/
  paywall/
  settings/
  documents/
```

For a small initial team, these can start as packages in one app module. Extract Gradle modules only when build time or ownership makes the added complexity worthwhile.

### Key application state

```text
EditorUiState
├── documentId
├── title / imported filename
├── text
├── wordCount
├── revision
├── saveState
├── checkState: idle | checking | success | failed | stale
├── analysisSummary
├── selectedIssue
├── analysisSheetState: collapsed | overview | focused
└── entitlement: free | pro
```

Keep the document as the single source of truth. UI highlights and analysis overlays should be derived state, not alternate copies of the text.

### Editor implementation considerations

- Preserve cursor and selection when analysis panels open or close.
- Map sentence offsets carefully after edits.
- Support undo and redo for manual edits and AI replacement.
- Handle large pastes without blocking the main thread.
- Keep the keyboard open when users choose Edit myself.
- Ensure highlighted text remains legible in light and dark themes.
- Never communicate severity by color alone.

### Document import

Use Android’s Storage Access Framework with `ACTION_OPEN_DOCUMENT`. Do not request broad storage access.

MVP formats:

- `.txt`: decode safely and retain line breaks
- `.docx`: extract paragraphs and basic list text; warn that complex formatting is not preserved

Import steps:

1. User chooses a file through the system picker.
2. App validates MIME type and file size.
3. Text is extracted off the main thread.
4. App previews or inserts the extracted text into the editor.
5. Original file is never modified.

Add an Android share target after the editor flow is stable so users can send selected text or compatible documents to ClearWrite from other apps.

### Offline behavior

- Editing, the one local saved document, and deterministic clarity checks should work offline.
- AI improvement requires a network connection.
- If offline, the Pro AI action explains the connection requirement without opening a misleading loading state.
- Never queue AI requests containing user text for silent submission later unless the user explicitly requests retry.

## 10. Backend and service design

### Responsibilities

The backend should:

- Authenticate app installations or signed-in users
- Verify Pro entitlements server-side
- Accept AI improvement requests
- Apply rate limits and document-size limits
- Construct and version prompts
- Call the AI provider without exposing credentials to the app
- Validate structured responses
- Track aggregate cost and reliability metrics
- Enforce data-retention rules

The free deterministic analyzer can initially run on-device. Moving it to the server later may improve consistency across platforms, but it should not create unnecessary cost or make basic checking internet-dependent.

### Suggested endpoints

```text
POST /v1/installations
GET  /v1/entitlements
POST /v1/ai/improvements
POST /v1/billing/google/notifications
POST /v1/billing/google/restore
DELETE /v1/account
```

Possible request:

```json
{
  "requestId": "req_456",
  "documentRevision": 4,
  "sentenceId": "sentence_12",
  "selectedText": "The sentence to improve...",
  "contextBefore": "Optional previous sentence.",
  "contextAfter": "Optional next sentence.",
  "issueType": "LONG_SENTENCE",
  "action": "IMPROVE_CLARITY",
  "tone": null,
  "locale": "en-IN"
}
```

### Backend technology

Either Kotlin/Ktor or TypeScript with a mature HTTP framework is suitable. Choose based on team experience. The service should be stateless where possible and use:

- PostgreSQL for accounts, purchases, entitlements, quotas, and consent records
- Redis or equivalent only if rate limiting/caching needs justify it
- A managed secrets system for provider credentials
- Centralized structured logs with text content excluded

### Identity approach

For MVP, issue an anonymous installation ID and signed token. Do not use a hardware identifier. Add optional account creation later for cross-device history or account recovery.

Subscription ownership is linked through verified Google Play purchase information. If accounts are added, define a careful purchase-linking and restoration flow so one purchase cannot accidentally attach to multiple unrelated accounts.

### Billing verification

- Start purchases through Google Play Billing in the Android app.
- Send the purchase token to the backend.
- Verify purchases with Google’s service APIs.
- Acknowledge purchases correctly.
- Process real-time developer notifications for renewals, cancellations, grace periods, and revocations.
- Cache entitlement locally for responsive UI but treat the server as authoritative.
- Provide Restore purchases.

## 11. Data model

### Local entities

**Document**

- ID
- Title or imported filename
- Current text
- Word count
- Revision number
- Created and updated timestamps
- Saved slot status
- Import source type

**Analysis**

- ID
- Document ID
- Document revision
- Score and scoring-version number
- Category summaries
- Created timestamp
- Stale flag

**Issue**

- ID
- Analysis ID
- Sentence ID
- Type and severity
- Original offsets
- Explanation
- Status: open, manually edited, replaced, ignored

**Pending recovery draft**

- Latest in-progress text
- Timestamp
- Whether it has been promoted to the single saved-document slot

### Server entities

- Installation or user
- Subscription and entitlement
- Purchase event
- AI quota usage
- Consent/privacy version
- AI request metadata without draft text

Do not retain raw drafts or AI excerpts by default. If temporary processing logs are essential, redact them and set a short, explicit retention period.

## 12. Privacy, security, and trust

### Privacy requirements

- Explain before the first AI request that selected writing will be sent to a third-party AI processor.
- Show a link to the privacy policy from the paywall, settings, and first AI consent message.
- Do not use document text for advertising.
- Do not retain document text on the backend by default.
- Provide deletion controls if an account is introduced.
- Complete the Play Data Safety disclosure accurately.
- Make analytics opt-out available if non-essential analytics are used.

### Security requirements

- Keep all AI and server credentials outside the Android package.
- Require TLS for all network traffic.
- Store authentication material using Android-supported secure storage.
- Verify billing entitlements on the server.
- Rate-limit by installation/user, IP risk signal, and entitlement tier.
- Enforce request size and time limits.
- Exclude user text, authorization headers, and purchase tokens from logs and crash reports.
- Add abuse detection without fingerprinting users unnecessarily.
- Review third-party SDKs and minimize them.

### User safety and expectation setting

The app should not claim that its score or AI suggestions guarantee correctness, suitability, admission, employment, or professional outcomes. AI suggestions can change nuance, names, numbers, or factual meaning; remind users to review before replacing text.

## 13. Accessibility and localization

- Minimum 48dp touch targets
- Screen-reader labels for icons, issue counts, score, highlights, and sheet actions
- Dynamic text sizing without clipped controls
- Logical focus order when a sheet expands or a sentence is selected
- Non-color indicators for issue type and severity
- Sufficient contrast in light and dark themes
- Keyboard and switch-access support where possible
- Haptic feedback used sparingly and never as the only status signal
- Plain language in issue explanations

Build text segmentation with locale awareness. Launch can focus on English, but do not hard-code assumptions that make future languages impossible. Clearly state which language is supported rather than producing unreliable analysis for unsupported text.

## 14. Analytics and product measurement

Analytics must use event metadata, not draft content.

### Core funnel

```text
App opened
→ Text pasted/imported
→ Check started
→ Analysis completed
→ Issue selected
→ Issue manually fixed or ignored
→ AI action tapped
→ Paywall viewed
→ Purchase completed
→ AI suggestion shown
→ Suggestion replaced/edited/dismissed
```

### Initial metrics

- Percentage of new users who add text
- Percentage who complete their first check
- Time from launch to first result
- Issue-category selection rate
- Percentage who edit at least one issue
- Check-again rate after editing
- Seven-day return rate
- AI action-to-paywall conversion
- Paywall-to-purchase conversion
- AI suggestion acceptance, edit, and dismissal rates
- AI cost per active Pro user
- Crash-free sessions and analysis failure rate

Track the one-document limit and word-limit paywalls separately; they represent different user needs.

## 15. Testing strategy

### Unit tests

- Sentence and paragraph segmentation
- Word count for punctuation, apostrophes, numbers, and Unicode text
- Every analysis rule and boundary value
- Clarity-score calculation and versioning
- Offset-to-highlight mapping
- Revision invalidation
- Free saved-document limit
- Word-limit enforcement
- Entitlement and quota decisions
- AI response validation

### Curated analysis corpus

Create a version-controlled, privacy-safe set of original sample texts containing:

- Short and long sentences
- Bullets and numbered lists
- Headings and fragments
- Abbreviations and decimals
- Dialogue and quotations
- Indian and international English names
- Emoji and Unicode punctuation
- Very long paragraphs
- Deliberate passive voice, repetition, and filler words

Each sample should have expected sentence boundaries and issues. This corpus prevents rule changes from silently degrading results.

### UI tests

- Empty-to-pasted-text flow
- Import success, unsupported type, and extraction failure
- Check loading, success, empty result, and error states
- Analysis sheet at all three heights
- Tap issue, scroll to sentence, and move to next issue
- Manual edit and stale-analysis message
- Free user tapping Improve with AI
- Dismiss paywall and return to selected issue
- Pro suggestion Replace, Edit, Dismiss, and Undo
- Save first document and attempt second
- Process death and draft recovery
- Rotation and configuration change
- TalkBack focus flow and large font sizes

### Integration and backend tests

- Billing verification and entitlement changes
- Purchase restore
- AI provider success, timeout, rate limit, malformed output, and safety rejection
- Request idempotency
- Text exclusion from logs
- Account deletion if accounts are implemented

### Release tests

- Low-memory and slow-device behavior
- No-network and intermittent-network behavior
- Large document at plan limits
- Supported Android versions and representative screen sizes
- Internal Play testing purchase flows
- Privacy disclosures and store listing alignment

## 16. Delivery phases

### Phase 0 — Product definition and technical spike (1–2 weeks)

Deliverables:

- Confirm app name and visual direction
- Turn wireframes into a small interactive prototype
- Define the exact English analysis rules and score formula
- Create the first curated analysis corpus
- Prototype sentence segmentation, offset mapping, and Compose highlighting
- Test TXT and DOCX extraction
- Select backend language and AI provider
- Draft privacy policy and data-flow diagram
- Validate proposed subscription positioning with potential users

Exit criteria:

- The complete core journey can be demonstrated in a prototype
- Highlight offsets remain correct for the test corpus
- The team agrees on what the clarity score means

### Phase 1 — Local Android core (2–3 weeks)

Deliverables:

- Android project and design system
- Empty and populated editor states
- Paste and typing behavior
- Word count
- Local document persistence and crash recovery
- One saved-document free rule
- TXT and DOCX import
- Deterministic analyzer and clarity score
- Loading and error states

Exit criteria:

- A user can paste/import a supported document and receive repeatable results offline
- Closing and reopening the app does not lose the recoverable draft

### Phase 2 — Analysis interaction (2 weeks)

Deliverables:

- Expandable analysis bottom sheet
- Category summary and issue counts
- Sentence highlighting and scrolling
- Focused issue explanation
- Edit myself and Ignore flows
- Next/previous issue navigation
- Revision invalidation and Check again behavior
- Accessibility pass for the core flow

Exit criteria:

- Every surfaced issue reliably maps to the correct text
- A user can manually review all issues without AI

### Phase 3 — Backend, AI, and subscription (2–3 weeks)

Deliverables:

- Backend deployment pipeline and environments
- Anonymous installation authentication
- AI improvement endpoint and structured response validation
- Contextual AI suggestion UI
- Replace, Edit, Dismiss, Try again, and Undo
- Google Play Billing integration
- Server-side entitlement verification and notifications
- Paywall at the agreed touchpoints
- Restore purchases
- Rate limits, quotas, cost monitoring, and spend alerts

Exit criteria:

- Free accounts cannot call paid AI endpoints
- A verified Pro purchase unlocks AI reliably across reinstall/restore scenarios
- User text is absent from normal operational logs

### Phase 4 — Beta hardening (2 weeks)

Deliverables:

- Analytics with content-safe event properties
- Privacy, consent, terms, and Data Safety materials
- End-to-end test coverage for critical flows
- Performance profiling
- Crash reporting configured to redact text
- Internal and closed Play testing
- In-app help and feedback
- Store listing and screenshots

Exit criteria:

- No known data-loss defect
- Target crash-free rate is met during closed testing
- AI cost and latency are within defined limits
- Billing, downgrade, cancellation, and refund states have been tested

### Phase 5 — Launch and iteration

First post-launch priorities:

1. Fix analysis accuracy and document-loss issues before adding features.
2. Study where users stop in the check-to-edit funnel.
3. Improve explanations for categories users rarely act on.
4. Tune paywall copy and pricing without reducing the free analysis value.
5. Add the Android share target.
6. Evaluate PDF import, multiple languages, cloud sync, and Improve all only from evidence.

The estimated MVP schedule is roughly 9–12 weeks for a small experienced team. Treat this as a sequencing guide, not a fixed deadline; text analysis quality, billing review, and beta findings can change it.

## 17. Suggested backlog

### Must have for MVP

- Paste-first editor
- TXT and DOCX import
- Word count and 1,500-word free limit
- One saved document for free
- Local draft recovery
- Free deterministic clarity analysis
- Score and categorized issue overview
- Issue-to-sentence navigation and highlighting
- Manual edit and ignore
- Stale-result handling and recheck
- Pro paywall at contextual touchpoints
- AI sentence improvement for verified Pro users
- AI rewriting for any user-selected passage, with clarity, shorten, simplify, tone, and custom controls
- Replace, edit, dismiss, and undo suggestion
- Play subscription and restore
- Settings, privacy, help, and feedback
- Accessibility, privacy, error, and offline states

### Should have soon after MVP

- Share to ClearWrite
- Analysis preferences
- Multiple Pro documents and document history
- Improved tone and shorten controls in the selected-sentence flow
- Onboarding hints shown contextually on first use
- Better DOCX list and heading preservation

### Later candidates

- PDF text extraction
- Cross-device sync and accounts
- Additional languages
- Browser share/extension or desktop companion
- Batch or Improve all flow
- Team or education plans

## 18. Decisions to validate before coding deeply

1. Which English variants will the first release support?
2. What exact thresholds define long sentences, long paragraphs, and difficult words?
3. Is the clarity score useful in user tests, or do category labels work better?
4. Should the single free saved document be fully replaced, archived, or export-only when a new one is saved?
5. What are the Pro monthly AI allowance and behavior when it is reached?
6. Is sign-in excluded from MVP, or required for subscription recovery beyond Google Play restore?
7. How long, if at all, may the AI provider retain submitted excerpts?
8. Which minimum Android version matches the target audience and testing capacity?
9. What latency target is acceptable for AI suggestions?
10. Which features and prices perform best in user interviews and Play experiments?

These are product decisions, not reasons to delay the editor, free analyzer, or issue-review prototypes.

## 19. Definition of MVP success

The MVP is ready for public release when:

- A new user can reach a useful analysis without instructions or an account.
- Free users can inspect and manually resolve every surfaced issue.
- Tapping an issue consistently selects the correct sentence.
- Editing never silently corrupts, replaces, uploads, or loses a draft.
- AI is available only to verified Pro users and always requires approval before replacement.
- Paywalls appear only at the agreed feature boundaries.
- The one-document free limit and downgrade behavior are predictable and non-destructive.
- Privacy disclosures accurately describe real data flows.
- Billing, offline, accessibility, and failure states are tested—not just the happy path.
- Production monitoring can identify crashes, backend failures, latency, and AI cost without collecting users’ writing.
