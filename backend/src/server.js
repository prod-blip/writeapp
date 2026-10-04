import http from "node:http";
import { pathToFileURL } from "node:url";
import { improveWriting, ProviderError } from "./ai.js";
import { GooglePlayError, verifyGooglePlaySubscription } from "./googlePlay.js";
import { privacyPolicyHtml } from "./privacy.js";
import {
  grantReviewerAccess,
  ReviewerAccessError,
  verifyReviewerAccessToken,
} from "./reviewerAccess.js";

const MAX_BODY_BYTES = 16_384;
const WINDOW_MS = 60_000;
const MAX_REQUESTS_PER_WINDOW = 20;
const rateLimits = new Map();

export function createClearWriteServer(
  env = process.env,
  { verifyPurchase = verifyGooglePlaySubscription } = {},
) {
  return http.createServer(async (request, response) => {
    setSecurityHeaders(response);
    if (request.method === "GET" && request.url === "/health") {
      return sendJson(response, 200, { status: "ok", provider: env.AI_PROVIDER || "mock" });
    }
    if (request.method === "GET" && request.url === "/privacy") {
      response.setHeader(
        "Content-Security-Policy",
        "default-src 'none'; style-src 'unsafe-inline'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'",
      );
      response.setHeader("X-Frame-Options", "DENY");
      response.writeHead(200, { "Content-Type": "text/html; charset=utf-8" });
      return response.end(privacyPolicyHtml);
    }

    const clientId = request.socket.remoteAddress || "local";
    if (!allowRequest(clientId)) {
      return sendJson(response, 429, { error: "Too many requests. Please wait and try again." });
    }

    if (request.method === "POST" && request.url === "/v1/billing/verify") {
      try {
        const body = await readJsonBody(request);
        const validationError = validateBillingRequest(body, env);
        if (validationError) return sendJson(response, 400, { error: validationError });
        const entitlement = await verifyPurchase(body.purchaseToken, env);
        return sendJson(response, 200, entitlement);
      } catch (error) {
        if (error instanceof HttpError || error instanceof GooglePlayError) {
          return sendJson(response, error.statusCode, { error: error.message });
        }
        return sendJson(response, 500, { error: "Unexpected server error." });
      }
    }

    if (request.method === "POST" && request.url === "/v1/reviewer/access") {
      try {
        const body = await readJsonBody(request);
        if (!body || typeof body !== "object" || !validString(body.code, 1, 200)) {
          return sendJson(response, 400, { error: "A reviewer access code is required." });
        }
        return sendJson(response, 200, grantReviewerAccess(body.code, env));
      } catch (error) {
        if (error instanceof HttpError || error instanceof ReviewerAccessError) {
          return sendJson(response, error.statusCode, { error: error.message });
        }
        return sendJson(response, 500, { error: "Unexpected server error." });
      }
    }

    if (request.method !== "POST" || request.url !== "/v1/ai/improvements") {
      return sendJson(response, 404, { error: "Not found." });
    }

    if (env.NODE_ENV === "production") {
      const reviewerToken = request.headers["x-reviewer-access-token"];
      const reviewerAuthorized =
        typeof reviewerToken === "string" && verifyReviewerAccessToken(reviewerToken, env);
      const purchaseToken = request.headers["x-play-purchase-token"];
      if (!reviewerAuthorized && (typeof purchaseToken !== "string" || !purchaseToken)) {
        return sendJson(response, 403, { error: "An active ClearWrite Pro subscription is required." });
      }
      if (!reviewerAuthorized) {
        try {
          const entitlement = await verifyPurchase(purchaseToken, env);
          if (!entitlement.active) {
            return sendJson(response, 403, { error: "An active ClearWrite Pro subscription is required." });
          }
        } catch (error) {
          if (error instanceof GooglePlayError) {
            return sendJson(response, error.statusCode, { error: error.message });
          }
          return sendJson(response, 503, { error: "Subscription verification is temporarily unavailable." });
        }
      }
    } else {
      const expectedToken = env.AI_DEV_TOKEN || "clearwrite-local-development";
      if (request.headers.authorization !== `Bearer ${expectedToken}`) {
        return sendJson(response, 401, { error: "Unauthorized." });
      }
    }

    try {
      const body = await readJsonBody(request);
      const validationError = validateRequest(body);
      if (validationError) return sendJson(response, 400, { error: validationError });

      const result = await improveWriting(body, env);
      return sendJson(response, 200, {
        requestId: body.requestId,
        documentRevision: body.documentRevision,
        ...result,
      });
    } catch (error) {
      if (error instanceof HttpError || error instanceof ProviderError || error instanceof GooglePlayError) {
        return sendJson(response, error.statusCode, { error: error.message });
      }
      return sendJson(response, 500, { error: "Unexpected server error." });
    }
  });
}

export function validateBillingRequest(body, env = process.env) {
  if (!body || typeof body !== "object" || Array.isArray(body)) return "A JSON object is required.";
  const expectedPackage = env.GOOGLE_PLAY_PACKAGE_NAME || "com.atulpandey.clearwrite";
  const expectedProduct = env.GOOGLE_PLAY_PRODUCT_ID || "clearwrite_pro";
  if (body.packageName !== expectedPackage) return "packageName is invalid.";
  if (body.productId !== expectedProduct) return "productId is invalid.";
  if (!validString(body.purchaseToken, 1, 4_096)) return "purchaseToken is invalid.";
  return null;
}

export function validateRequest(body) {
  if (!body || typeof body !== "object" || Array.isArray(body)) return "A JSON object is required.";
  if (!validString(body.requestId, 1, 100)) return "requestId is required.";
  if (!Number.isSafeInteger(body.documentRevision) || body.documentRevision < 0) {
    return "documentRevision must be a non-negative integer.";
  }
  if (!validString(body.selectedText, 1, 3_000)) return "selectedText must contain 1–3000 characters.";
  if (!optionalString(body.contextBefore, 2_000) || !optionalString(body.contextAfter, 2_000)) {
    return "Context is too long.";
  }
  if (!validString(body.issueType, 1, 80)) return "issueType is required.";
  if (!validString(body.action, 1, 80)) return "action is required.";
  if (!optionalString(body.tone, 40) || !optionalString(body.locale, 20)) return "Invalid options.";
  if (!optionalString(body.customInstruction, 240)) return "Custom instruction is too long.";
  return null;
}

async function readJsonBody(request) {
  let body = "";
  for await (const chunk of request) {
    body += chunk;
    if (Buffer.byteLength(body) > MAX_BODY_BYTES) throw new HttpError("Request body is too large.", 413);
  }
  try {
    return JSON.parse(body);
  } catch {
    throw new HttpError("Malformed JSON.", 400);
  }
}

function allowRequest(clientId) {
  const now = Date.now();
  const current = rateLimits.get(clientId);
  if (!current || now - current.startedAt >= WINDOW_MS) {
    rateLimits.set(clientId, { startedAt: now, count: 1 });
    return true;
  }
  current.count += 1;
  return current.count <= MAX_REQUESTS_PER_WINDOW;
}

function validString(value, min, max) {
  return typeof value === "string" && value.trim().length >= min && value.length <= max;
}

function optionalString(value, max) {
  return value === undefined || (typeof value === "string" && value.length <= max);
}

function setSecurityHeaders(response) {
  response.setHeader("Cache-Control", "no-store");
  response.setHeader("X-Content-Type-Options", "nosniff");
  response.setHeader("Referrer-Policy", "no-referrer");
}

function sendJson(response, statusCode, body) {
  response.writeHead(statusCode, { "Content-Type": "application/json; charset=utf-8" });
  response.end(JSON.stringify(body));
}

class HttpError extends Error {
  constructor(message, statusCode) {
    super(message);
    this.statusCode = statusCode;
  }
}

const isMain = process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href;
if (isMain) {
  const port = Number(process.env.PORT || 8787);
  createClearWriteServer().listen(port, "0.0.0.0", () => {
    console.log(`ClearWrite AI backend listening on http://localhost:${port} (${process.env.AI_PROVIDER || "mock"})`);
  });
}
