# Recurring defect patterns (A–F)

Each pattern below was found in real block audits of this suite. For each: the log signature that reveals it, how
to confirm it, and the fix that removes the cause without relaxing anything. Use them as detectors, not as
conclusions: confirm every instance with its own evidence.

## A. Assertions that pass for the wrong reason

**A1 — Authorization asserted, but a different rejection produced the status.** A scenario expects 401/403 for
"token/key/scope not allowed", but the gateway logs a different reason: `No matching resource found in the API`,
`User is not subscribed to access the API`, `Resource forbidden`. Example: an "API-scoped internal key is refused on
API B" scenario passed because the key's principal was never subscribed to B (`GatewayUtils User is not subscribed`),
so the key-scoping logic was never reached.
*Confirm*: read the gateway WARN/ERROR at the request timestamp and compare its reason with the scenario's claim.
*Fix*: set up the preconditions so only the claimed cause can reject (subscribe, deploy the resource), and assert
the error code/description that identifies the cause, not just the status.

**A2 — A helper accepts any failure as the expected error.** E.g. an "expect MCP JSON-RPC error" helper returning
success when the body contains "error" OR the HTTP status is ≥ 400; a gateway routing/authentication failure then
satisfies it. *Confirm*: read the helper's predicate; find the log line that shows the actual failure. *Fix*: assert
the exact protocol-level error (JSON-RPC `error.code`, the product error code) and the transport status separately.

**A3 — A proxy signal stands in for the real one.** E.g. "deployed to the gateway" asserted by polling the Publisher
revision-deployments list, while the gateway logs `No API Artifacts` / `Storage returned null`; or a parser-level
rejection (`HostDeniedException: IP is restricted`) satisfying a scenario about the APIM network-access policy.
*Confirm*: find the server line for the claimed mechanism; check whether it is present at all. *Fix*: gate/assert on
the real signal (gateway route live, policy-specific error code), or restructure the input so the earlier layer
cannot short-circuit.

**A4 — Counting exceptions as hits.** E.g. a WebSocket flood helper incrementing "reached the gateway" in a
`catch (Exception)` block, so local connection failures count as gateway handshakes. *Fix*: count only observed
server responses; assert the server-side evidence where possible.

**A5 — A fixture proves itself.** The test seeds a mock/stub and then reads it back; the read proves the fixture,
not product persistence or behaviour. *Fix*: assert on the product's own read path.

**A6 — The asserted actor is not the one that acted.** A role-gated scenario passes because the fixture also grants
an admin role (e.g. a federated user mapped to `creator,publisher,subscriber,admin` "proving" the creator mapping).
*Fix*: provision a least-privilege actor for the claim, and add the negative actor that must be refused.

## B. Coverage gaps

- **Status-only negatives**: a 400 is asserted, but nothing pins *why* (the server log shows the reason, the test does
  not). Assert the error response's `code` and `description` fields where the product provides them
  (`The value of error response field "code" should be "..."`), not a body substring.
- **Substring assertions**: `should contain` is a substring match; a code like `901403` asserted as text anywhere in
  the body can match unrelated content. Prefer field-exact assertions.
- **Documented-but-untested behaviour**: a feature comment records a known product response (500/900967) and
  therefore omits the negative. Record it as a gap with the product issue it is waiting on; do not enshrine a 500.
- **Title/description promises more than the steps assert** (e.g. "rotation revokes the old credential" with no
  assertion on the old credential).
- **Fan-out holes**: the email-username/tenant/secondary-store row exists but the step ignores the actor dimension.

## C. Resource leaks

**C1 — The sweep has no list for the resource type.** E.g. webhook/WebSub subscription rows that API deletion does not
remove; anything created by a non-actor (federated SSO user, product-created sign-up user) registered under the
acting actor so cleanup has no owner token. *Signature*: test-side `Cleanup: no token for the owner` / `may leak`;
server rows that outlive the block. *Fix*: register under the correct owner type (e.g. foreign-owned applications are
transferred to the tenant admin, then deleted) or add a dedicated cleanup list.

**C2 — Cleanup cannot complete a pending state.** E.g. an approval workflow parks a deleted application in
`DELETE_PENDING` (delete answered 201; a second delete answers 400); if the step deregisters on 201 or the sweep
handles only 201, the application and its pending task leak. *Fix*: keep the id registered on an accepted-but-pending
delete and let the sweep complete the pending state (approve the task) and verify the resource is gone.

**C3 — A failing product delete clears the list anyway.** The sweep's `finally` drops ids whose delete returned 500,
so nothing retries them. Treat the 500 as a finding (often a product bug or a configuration gap); do not hide it.

**C4 — A configuration overlay removes a distribution default.** TOML arrays (`[[event_listener]]`, etc.) in an
overlay *replace* the distribution's whole array. Example: an SSO overlay's `[[event_listener]]` dropped the
default `token_revocation` listener, so OAuth-app deletion NPE'd (`getEventSender()` null) and DCR clients leaked.
*Confirm*: compare the overlay's arrays with the distribution's `deployment.toml` defaults. *Fix*: restate the default
entries in the overlay.

## D. Test hygiene

**D1 — Cleanup re-deletes what a step already deleted.** *Signature*: a server not-found ERROR (`Requested
subscription/service/workflow with Id ... not found`) or a cleanup WARN on a 404/500 after the scenario's own delete.
*Fix*: the deleting step calls `ResourceCleanup.deregisterIfDeleted(list, id, response)` (drop on a confirmed delete;
keep on failure and on an accepted-but-pending delete). If the resource is removed as a side effect of another
operation (e.g. product retirement removes its subscriptions), deregister it there.

**D2 — The record type is not matched by `deregister`.** A dedicated record (e.g. application key mappings) needs
explicit matching; otherwise `deregisterIfDeleted` silently does nothing and the sweep re-deletes (product answers 500
on an already-removed key mapping → false `may leak`).

**D3 — Per-scenario cleanup sweeps shared fixtures.** A `@cleanup` scenario deleting `_setup_*` fixtures that later
scenarios need. Per-scenario cleanup must be scoped to resources registered during that scenario.

**D4 — Literal placeholders sent to the server.** `${UNIQUE:...}` or `{{...}}` appearing in server logs or resource
names (e.g. 900610 at API create from an unresolved role). *Fix*: resolve in the provisioning step.

**D5 — Drift between prose and configuration.** A feature/runner comment says `thread-count=1` while the suite runs
2; a block named for a runner it no longer contains. Correct the prose (comments state what the code IS).

## E. Test-side robustness

**E1 — Invocation before the route/key is live.** *Signature*: `InboundWebSocketProcessor No matching API was found`,
`APIAuthenticationHandler ... Missing Credentials`/401 on the first attempt(s), then success on retry. Each
transient is absorbed by a retrying invocation helper. *Fix*: gate on the real readiness signal (a probe of the live
route; key/subscription propagation) before the asserted call. Never a static sleep.

**E2 — Retrying a deterministic or non-idempotent failure.** *Signature*: `Transient 900967 ... retrying` followed by
a different server error (e.g. `Key Mappings already exists`) because the first attempt partially succeeded. *Fix*:
retry only genuinely transient, idempotent requests; for a deterministic 900967 assert the documented outcome.

**E3 — Fixtures reaching real external hosts.** *Signature*: `UnknownHostException` / outbound timeouts to
example-looking or vendor hosts (`api.allowed.example.com`, a real Auth0 tenant). The assertion then depends on DNS
and the internet. *Fix*: point fixtures at the suite's node backend (it has stubs, e.g. `oauth-token-stub`) and
assert the policy decision itself.

**E4 — Asynchronous configuration not awaited.** Certificate/listener reloads (mTLS trust takes effect only after the
listener reload), endpoint-certificate gateway events, key-manager registration events, tenant activation. Moving on
after the REST 2xx races the async consumer. *Fix*: poll the effect (a handshake that succeeds, the gateway event
processed) with the suite's readiness helpers.

**E5 — Teardown order produces noise or masks issues.** E.g. stopping APIM before a dependent standalone gateway, so
the gateway logs connection loss. Order teardown by dependency.

**E6 — Shared mutable state across parallel classes.** Overlapping Publisher writes on one server (the client lock
logs `publisher-lock: ... waited Nms` at INFO), server-global configuration flipped by one class while another runs.
Check the block's `thread-count` and the runner's documented isolation needs.

**E7 — Host-environment interference.** Docker network create/remove by other lanes aborting in-flight browser
requests on Linux (`net::ERR_NETWORK_CHANGED`, `ChunkLoadError`; the suite isolates the browser in a network
namespace — check `Browser network: private namespace` and `network-change request failures: 0`); a stale prebuilt
image (`node-app-server`) missing routes the code under test needs (Express HTML 404).

## F. Product bugs

Prove from the log sequence plus bytecode on the stack trace. Typical shapes: NPE in a listener or handler; a cache
cleared before the transaction commits (stale re-cache); a swallowed RuntimeException in a JMS/event listener; a
fixed temp path shared by concurrent operations; a 500 where the contract is 4xx. State the trigger, the bytecode
branch, the observable impact, and whether a sound test-side mitigation exists. See `known-product-issues.md`.
