export const privacyPolicyHtml = `<!doctype html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="color-scheme" content="light dark">
    <title>ClearWrite Privacy Policy</title>
    <style>
      :root {
        color-scheme: light dark;
        font-family: Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
        background: #f8f6f1;
        color: #202124;
      }
      body { margin: 0; }
      main { max-width: 760px; margin: 0 auto; padding: 56px 24px 80px; }
      .mark { color: #4d61b5; font-size: 1rem; font-weight: 700; letter-spacing: .02em; }
      h1 { margin: 12px 0 8px; font-size: clamp(2rem, 6vw, 3.2rem); line-height: 1.08; }
      h2 { margin: 36px 0 10px; font-size: 1.25rem; }
      p, li { color: #55585e; font-size: 1rem; line-height: 1.7; }
      ul { padding-left: 22px; }
      a { color: #4055aa; }
      .updated { margin-top: 0; color: #72757d; }
      .summary { margin: 28px 0; padding: 20px; border: 1px solid #dcddd9; border-radius: 16px; background: #fff; }
      .summary p { margin: 0; color: #33363b; }
      @media (prefers-color-scheme: dark) {
        :root { background: #191a18; color: #f1f1ed; }
        p, li, .updated { color: #c4c6c2; }
        .summary { background: #222320; border-color: #393a36; }
        .summary p { color: #e5e6e1; }
        a { color: #aebcff; }
      }
    </style>
  </head>
  <body>
    <main>
      <div class="mark">ClearWrite</div>
      <h1>Privacy Policy</h1>
      <p class="updated">Effective: 4 October 2026</p>

      <div class="summary">
        <p>Your drafts stay on your device. ClearWrite sends text off the device only when you ask for an AI rewrite.</p>
      </div>

      <p>This Privacy Policy explains how ClearWrite, operated by Atul Pandey ("ClearWrite", "we", "us", or "our"), handles information when you use the ClearWrite Android application and its supporting services.</p>

      <h2>Information stored on your device</h2>
      <p>ClearWrite stores your drafts, document titles, imported document content, writing preferences, AI-consent choice, and subscription-verification state in the app's private storage on your device. ClearWrite does not provide a cloud document account or synchronize your document library to our servers.</p>
      <p>You can remove individual documents inside the app. Uninstalling ClearWrite removes its locally stored app data, subject to your device and Android settings.</p>

      <h2>Local writing analysis</h2>
      <p>Clarity scoring and rule-based writing checks are performed on your device. Your full document is not sent to our server merely to generate the standard writing analysis.</p>

      <h2>AI rewrite requests</h2>
      <p>AI rewriting is optional and available only when you request it. To generate a suggestion, ClearWrite sends the selected passage, limited nearby context, your requested action or tone, language information, and technical request identifiers to the ClearWrite backend. The backend sends the writing content to OpenAI to generate the suggestion.</p>
      <p>The ClearWrite backend does not write submitted passages or AI suggestions to a document database and does not intentionally include their content in application logs. Requests to OpenAI use the Responses API with <code>store: false</code>. OpenAI states that API data is not used to train its models by default unless the API customer opts in, but content may be retained in abuse-monitoring logs for up to 30 days under OpenAI's standard data controls. See <a href="https://developers.openai.com/api/docs/guides/your-data">OpenAI's API data controls</a>.</p>

      <h2>Subscription information</h2>
      <p>ClearWrite uses Google Play Billing for ClearWrite Pro. The app and backend process a Google Play purchase token, product identifier, package identifier, subscription status, and expiry information to verify and restore access. We do not receive or store your full payment-card or bank-account details. Payments are processed by Google Play under Google's own terms and privacy practices.</p>

      <h2>Technical information</h2>
      <p>When the app contacts our backend, the hosting provider and server may process standard network information such as IP address, request time, device network information, and HTTP headers to deliver the service, prevent abuse, diagnose failures, and apply rate limits. We do not use this information for advertising or cross-app tracking.</p>

      <h2>Service providers</h2>
      <p>We use service providers only to operate ClearWrite:</p>
      <ul>
        <li>OpenAI, to generate user-requested AI writing suggestions.</li>
        <li>Render, to host the ClearWrite backend.</li>
        <li>Google Play, to distribute the app and process and verify subscriptions.</li>
      </ul>
      <p>These providers may process information in countries other than your own and handle it under their respective privacy terms.</p>

      <h2>Advertising and sale of data</h2>
      <p>ClearWrite does not contain third-party advertising, does not sell personal information, and does not use your drafts for targeted advertising.</p>

      <h2>Retention and deletion</h2>
      <ul>
        <li>Documents and settings remain on your device until you delete them or uninstall the app.</li>
        <li>The ClearWrite backend processes AI content transiently and does not maintain a document-content database.</li>
        <li>OpenAI and infrastructure providers may retain information for the periods described in their own policies, including OpenAI's standard abuse-monitoring retention described above.</li>
        <li>Subscription verification data is retained locally and may be cached temporarily by the backend to confirm access.</li>
      </ul>

      <h2>Security</h2>
      <p>ClearWrite uses HTTPS for production network requests and keeps the OpenAI API key on the backend rather than in the Android app. No method of transmission or storage is completely secure, so we cannot guarantee absolute security.</p>

      <h2>Children</h2>
      <p>ClearWrite is not directed to children under 13. If you believe a child has provided personal information through an AI request, contact us so we can review the request.</p>

      <h2>Your choices and rights</h2>
      <p>You may use ClearWrite's local analysis without requesting an AI rewrite. You can delete locally saved documents in the app, uninstall the app to remove its local data, and manage or cancel your subscription through Google Play. Depending on where you live, you may also have legal rights concerning personal information processed by us.</p>

      <h2>Changes to this policy</h2>
      <p>We may update this policy as ClearWrite changes. We will revise the effective date on this page when we make changes.</p>

      <h2>Contact</h2>
      <p>For privacy questions or requests, email <a href="mailto:atulmailing@gmail.com">atulmailing@gmail.com</a>.</p>
    </main>
  </body>
</html>`;
