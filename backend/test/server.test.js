import assert from "node:assert/strict";
import { after, before, test } from "node:test";
import { mockImprovement } from "../src/ai.js";
import { createClearWriteServer, validateBillingRequest, validateRequest } from "../src/server.js";

const validRequest = {
  requestId: "request-1",
  documentRevision: 3,
  selectedText: "We utilize this tool in order to write clearly.",
  contextBefore: "",
  contextAfter: "",
  issueType: "WORDY_PHRASE",
  action: "make_concise",
  tone: "preserve",
  customInstruction: "",
  locale: "en",
};

test("validates a complete request", () => {
  assert.equal(validateRequest(validRequest), null);
});

test("rejects oversized selected text", () => {
  assert.match(validateRequest({ ...validRequest, selectedText: "x".repeat(3_001) }), /selectedText/);
});

test("rejects oversized custom rewrite instructions", () => {
  assert.match(
    validateRequest({ ...validRequest, customInstruction: "x".repeat(241) }),
    /Custom instruction/,
  );
});

test("mock improvement is deterministic and concise", () => {
  const result = mockImprovement(validRequest);
  assert.equal(result.suggestions[0].text, "We use this tool to write clearly.");
});

test("validates the configured Play product", () => {
  assert.equal(
    validateBillingRequest({
      packageName: "com.atulpandey.clearwrite",
      productId: "clearwrite_pro",
      purchaseToken: "play-token",
    }),
    null,
  );
});

let server;
let baseUrl;

before(async () => {
  server = createClearWriteServer(
    { AI_PROVIDER: "mock", AI_DEV_TOKEN: "test-token" },
    { verifyPurchase: async () => ({ active: true, accessUntilMillis: Date.now() + 60_000 }) },
  );
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
  baseUrl = `http://127.0.0.1:${server.address().port}`;
});

after(async () => {
  await new Promise((resolve, reject) => server.close((error) => (error ? reject(error) : resolve())));
});

test("health endpoint is public", async () => {
  const response = await fetch(`${baseUrl}/health`);
  assert.equal(response.status, 200);
  assert.deepEqual(await response.json(), { status: "ok", provider: "mock" });
});

test("privacy policy is public and describes AI data handling", async () => {
  const response = await fetch(`${baseUrl}/privacy`);
  assert.equal(response.status, 200);
  assert.match(response.headers.get("content-type"), /^text\/html/);
  const policy = await response.text();
  assert.match(policy, /ClearWrite Privacy Policy/);
  assert.match(policy, /store: false/);
  assert.match(policy, /up to 30 days/);
  assert.match(policy, /atulmailing@gmail\.com/);
});

test("improvement endpoint requires authorization", async () => {
  const response = await fetch(`${baseUrl}/v1/ai/improvements`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(validRequest),
  });
  assert.equal(response.status, 401);
});

test("improvement endpoint returns suggestions", async () => {
  const response = await fetch(`${baseUrl}/v1/ai/improvements`, {
    method: "POST",
    headers: {
      Authorization: "Bearer test-token",
      "Content-Type": "application/json",
    },
    body: JSON.stringify(validRequest),
  });
  assert.equal(response.status, 200);
  const payload = await response.json();
  assert.equal(payload.requestId, validRequest.requestId);
  assert.equal(payload.suggestions[0].text, "We use this tool to write clearly.");
});

test("billing endpoint verifies a Play purchase token", async () => {
  const response = await fetch(`${baseUrl}/v1/billing/verify`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      packageName: "com.atulpandey.clearwrite",
      productId: "clearwrite_pro",
      purchaseToken: "play-token",
    }),
  });
  assert.equal(response.status, 200);
  assert.equal((await response.json()).active, true);
});

test("production AI requires a verified Pro purchase", async () => {
  const productionServer = createClearWriteServer(
    { AI_PROVIDER: "mock", NODE_ENV: "production" },
    { verifyPurchase: async (token) => ({ active: token === "valid-token", accessUntilMillis: Date.now() + 60_000 }) },
  );
  await new Promise((resolve) => productionServer.listen(0, "127.0.0.1", resolve));
  const productionUrl = `http://127.0.0.1:${productionServer.address().port}`;
  try {
    const denied = await fetch(`${productionUrl}/v1/ai/improvements`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(validRequest),
    });
    assert.equal(denied.status, 403);

    const allowed = await fetch(`${productionUrl}/v1/ai/improvements`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-Play-Purchase-Token": "valid-token",
      },
      body: JSON.stringify(validRequest),
    });
    assert.equal(allowed.status, 200);
  } finally {
    await new Promise((resolve, reject) =>
      productionServer.close((error) => (error ? reject(error) : resolve())),
    );
  }
});
