import { createHash, createHmac, timingSafeEqual } from "node:crypto";

const REVIEW_ACCESS_DURATION_MS = 30 * 24 * 60 * 60 * 1_000;
const TOKEN_AUDIENCE = "clearwrite-play-review";

export class ReviewerAccessError extends Error {
  constructor(message, statusCode) {
    super(message);
    this.statusCode = statusCode;
  }
}

export function grantReviewerAccess(code, env = process.env, now = Date.now()) {
  const configuredCode = configuredReviewCode(env);
  if (!validCode(code) || !constantTimeEqual(code.trim(), configuredCode)) {
    throw new ReviewerAccessError("The reviewer access code is invalid.", 403);
  }

  const accessUntilMillis = now + REVIEW_ACCESS_DURATION_MS;
  const payload = Buffer.from(
    JSON.stringify({ audience: TOKEN_AUDIENCE, expiresAt: accessUntilMillis }),
    "utf8",
  ).toString("base64url");
  const signature = sign(payload, configuredCode);
  return {
    active: true,
    accessUntilMillis,
    reviewToken: `${payload}.${signature}`,
  };
}

export function verifyReviewerAccessToken(token, env = process.env, now = Date.now()) {
  if (typeof token !== "string" || token.length > 2_048) return false;
  const [payload, signature, extra] = token.split(".");
  if (!payload || !signature || extra !== undefined) return false;

  let configuredCode;
  try {
    configuredCode = configuredReviewCode(env);
  } catch {
    return false;
  }
  if (!constantTimeEqual(signature, sign(payload, configuredCode))) return false;

  try {
    const claims = JSON.parse(Buffer.from(payload, "base64url").toString("utf8"));
    return (
      claims?.audience === TOKEN_AUDIENCE &&
      Number.isSafeInteger(claims.expiresAt) &&
      claims.expiresAt > now
    );
  } catch {
    return false;
  }
}

function configuredReviewCode(env) {
  const code = env.CLEARWRITE_REVIEW_ACCESS_CODE;
  if (!validCode(code)) {
    throw new ReviewerAccessError("Reviewer access is not configured.", 503);
  }
  return code.trim();
}

function validCode(value) {
  return typeof value === "string" && value.trim().length >= 16 && value.length <= 200;
}

function sign(payload, secret) {
  return createHmac("sha256", secret).update(payload).digest("base64url");
}

function constantTimeEqual(left, right) {
  const leftDigest = createHash("sha256").update(left).digest();
  const rightDigest = createHash("sha256").update(right).digest();
  return timingSafeEqual(leftDigest, rightDigest);
}
