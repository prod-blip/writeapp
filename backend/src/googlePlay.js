import crypto from "node:crypto";

const TOKEN_AUDIENCE = "https://oauth2.googleapis.com/token";
const ANDROID_PUBLISHER_SCOPE = "https://www.googleapis.com/auth/androidpublisher";
const ACCESS_STATES = new Set([
  "SUBSCRIPTION_STATE_ACTIVE",
  "SUBSCRIPTION_STATE_IN_GRACE_PERIOD",
  "SUBSCRIPTION_STATE_CANCELED",
]);
const oauthCache = new Map();
const entitlementCache = new Map();

export async function verifyGooglePlaySubscription(purchaseToken, env = process.env) {
  if (!purchaseToken || purchaseToken.length > 4_096) {
    return { active: false, accessUntilMillis: 0 };
  }

  const packageName = env.GOOGLE_PLAY_PACKAGE_NAME || "com.atulpandey.clearwrite";
  const productId = env.GOOGLE_PLAY_PRODUCT_ID || "clearwrite_pro";
  const cached = entitlementCache.get(purchaseToken);
  if (cached && cached.cacheUntilMillis > Date.now()) return cached.entitlement;

  const accessToken = await getGoogleAccessToken(env);
  const url =
    `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${encodeURIComponent(packageName)}` +
    `/purchases/subscriptionsv2/tokens/${encodeURIComponent(purchaseToken)}`;
  const response = await fetch(url, {
    headers: { Authorization: `Bearer ${accessToken}` },
    signal: AbortSignal.timeout(15_000),
  });

  if (response.status === 400 || response.status === 404) {
    return { active: false, accessUntilMillis: 0 };
  }
  if (!response.ok) {
    throw new GooglePlayError("Google Play could not verify the subscription.", 503);
  }

  const purchase = await response.json();
  const matchingItems = (purchase.lineItems || []).filter((item) => item.productId === productId);
  const accessUntilMillis = matchingItems.reduce(
    (latest, item) => Math.max(latest, Date.parse(item.expiryTime || "") || 0),
    0,
  );
  const active = ACCESS_STATES.has(purchase.subscriptionState) && accessUntilMillis > Date.now();
  const entitlement = { active, accessUntilMillis };

  entitlementCache.set(purchaseToken, {
    entitlement,
    cacheUntilMillis: Date.now() + (active ? 5 * 60_000 : 30_000),
  });

  if (active && purchase.acknowledgementState === "ACKNOWLEDGEMENT_STATE_PENDING") {
    await acknowledgeSubscription({ packageName, productId, purchaseToken, accessToken });
  }
  return entitlement;
}

async function getGoogleAccessToken(env) {
  const credentials = readCredentials(env);
  const cached = oauthCache.get(credentials.clientEmail);
  if (cached && cached.expiresAtMillis > Date.now() + 60_000) return cached.token;

  const nowSeconds = Math.floor(Date.now() / 1_000);
  const header = base64Url(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const claims = base64Url(
    JSON.stringify({
      iss: credentials.clientEmail,
      scope: ANDROID_PUBLISHER_SCOPE,
      aud: TOKEN_AUDIENCE,
      iat: nowSeconds,
      exp: nowSeconds + 3_600,
    }),
  );
  const unsignedToken = `${header}.${claims}`;
  const signature = crypto.sign("RSA-SHA256", Buffer.from(unsignedToken), credentials.privateKey);
  const assertion = `${unsignedToken}.${signature.toString("base64url")}`;

  const response = await fetch(TOKEN_AUDIENCE, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion,
    }),
    signal: AbortSignal.timeout(15_000),
  });
  if (!response.ok) throw new GooglePlayError("Google Play credentials were rejected.", 503);
  const body = await response.json();
  if (!body.access_token) throw new GooglePlayError("Google Play returned no access token.", 503);
  oauthCache.set(credentials.clientEmail, {
    token: body.access_token,
    expiresAtMillis: Date.now() + Number(body.expires_in || 3_600) * 1_000,
  });
  return body.access_token;
}

function readCredentials(env) {
  let clientEmail = env.GOOGLE_PLAY_SERVICE_ACCOUNT_EMAIL;
  let privateKey = env.GOOGLE_PLAY_PRIVATE_KEY?.replace(/\\n/g, "\n");
  if (env.GOOGLE_PLAY_SERVICE_ACCOUNT_JSON) {
    let parsed;
    try {
      parsed = JSON.parse(env.GOOGLE_PLAY_SERVICE_ACCOUNT_JSON);
    } catch {
      throw new GooglePlayError("GOOGLE_PLAY_SERVICE_ACCOUNT_JSON is invalid.", 503);
    }
    clientEmail = parsed.client_email;
    privateKey = parsed.private_key;
  }
  if (!clientEmail || !privateKey) {
    throw new GooglePlayError("Google Play subscription verification is not configured.", 503);
  }
  return { clientEmail, privateKey };
}

async function acknowledgeSubscription({ packageName, productId, purchaseToken, accessToken }) {
  const url =
    `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${encodeURIComponent(packageName)}` +
    `/purchases/subscriptions/${encodeURIComponent(productId)}/tokens/${encodeURIComponent(purchaseToken)}:acknowledge`;
  const response = await fetch(url, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${accessToken}`,
      "Content-Type": "application/json",
    },
    body: "{}",
    signal: AbortSignal.timeout(15_000),
  });
  if (!response.ok) throw new GooglePlayError("The subscription could not be acknowledged.", 503);
}

function base64Url(value) {
  return Buffer.from(value).toString("base64url");
}

export class GooglePlayError extends Error {
  constructor(message, statusCode = 503) {
    super(message);
    this.name = "GooglePlayError";
    this.statusCode = statusCode;
  }
}
