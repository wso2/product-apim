# Integration-v2 Test Port — Coverage Comparison Report

**Source:** `master-new-test-framework` — 22 product feature files (21 `publisher/` + 1 `header/`), each with one
legacy runner, every scenario fanned out ×4 by the `@Factory` user-mode provider.

> **Correction (baseline scope).** The "22" counted only the `publisher/` + `header/` groups. The legacy cucumber
> suite also has a `restart/` group — `restart/token_persistence_restart.feature` — which was **omitted** from
> this count. It is a real legacy *product* feature (token persistence across an APIM restart), not migration.
> It is now **ported and covered** (`key-manager/token-persistence`, §4). The `migration/` group remains out of
> scope (see API Products). So the true legacy product-feature set is **23**, and **all 23 are covered**.
**Target:** `master-public` (this branch) — 25 capability-decomposed feature files across
`publisher / devportal / gateway / key-manager`, run on the parallel-on-shared-container (v2) lane with
explicit per-actor / per-tenant `Scenario Outline`s.

> **Why the file counts differ (22 → 25).** The legacy organised features by *page/screen* (e.g. one
> `create_new_application` file mixing app CRUD + key-gen + invoke). We reorganised by *capability* — so a
> single legacy feature often splits across several capability features (publisher / devportal / gateway /
> key-manager). The comparison below is therefore **scenario-level**, not file-level.

---

## 1. Coverage matrix — legacy feature → ported coverage

| # | Legacy feature | Key legacy scenarios | Ported to | Status |
|--:|----------------|----------------------|-----------|--------|
| 1 | `create_an_api_through_the_publisher_rest_api_test` | create, get-by-id, update desc/tiers, update-doesn't-rename, publish, list | `publisher/api-lifecycle` | ✅ covered (subscribe/invoke → §3) |
| 2 | `create_deploy_publish_an_api` | create REST/SOAP/GraphQL/WS, deploy, publish, invoke REST/GraphQL/SOAP | `publisher/{api-lifecycle, soap-design, graphql-design, streaming-design}` + `gateway/{rest,soap,graphql}-invocation` | ✅ covered (split by capability) |
| 3 | `api_runtime_configurations` | responseCaching, cacheTimeout, schemaValidation, transport, CORS, throttlingPolicy — across REST/SOAP/GraphQL/WS | `publisher/api-config` | ✅ covered (full type matrix, each ×2-tenant) |
| 4 | `api_other_common_configurations` | custom properties, resources, subscription policies (REST/SOAP/GraphQL/WS), **shared scopes** | `publisher/api-config` (props/resources/policies across all 4 types, ×2-tenant) + `publisher/scopes` (shared scopes) | ✅ covered (split) |
| 5 | `api_versioning` | new version, default-version flag, lifecycle, deploy, publish, invoke v2 | `publisher/versioning` (publisher plane) + `gateway/rest-invocation` (invoke) | ✅ covered (split) |
| 6 | `import_OAS_definition` | import OAS 2/3/3.1, revision, deploy, publish, invoke | `publisher/definitions` + `gateway/rest-invocation` | ✅ covered (split) |
| 7 | `api_documents` | add / list / get / update / delete document | `publisher/docs` | ✅ covered |
| 8 | `graphql_api_baseline` | create-from-schema, deploy, publish | `publisher/graphql-design` | ✅ covered |
| 9 | `websocket_api_baseline` | create, deploy, publish | `publisher/streaming-design` | ✅ covered |
| 10 | `create_new_application` | app CRUD, key-gen, token, org-share | `devportal/applications` (CRUD) + `key-manager/oauth-keys` (key-gen/token) | ✅ covered (split) — org-share dropped (§2) |
| 11 | `devportal_search_visibility` | search published API by name/context | `devportal/search` | ✅ covered |
| 12 | `subscription_blocking` | block / unblock, invoke→401 between | `devportal/subscription-management` (block/unblock) + `gateway/security-enforcement` (blocked→reject) | ✅ covered (split) |
| 13 | `subscription_throttling_policy` | update plan Unlimited→Gold | `devportal/subscription-management` | ✅ covered |
| 14 | `jwt_token_format` | password-grant token in JWT format | `key-manager/token-issuance` (`@rule:jwt-format`) | ✅ covered |
| 15 | `openid_token` | openid-scoped token + userinfo | `key-manager/token-issuance` (`@rule:openid`) | ✅ covered |
| 16 | `refresh_token` | password grant, refresh re-issue, invoke | `key-manager/token-issuance` (`@rule:refresh`) | ✅ covered |
| 17 | `sandbox_token` | sandbox-key token, invoke | `key-manager/token-issuance` (`@rule:sandbox`) | ✅ covered |
| 18 | `revoke_token` | revoke token, invoke→401 | `key-manager/token-revocation` | ✅ covered |
| 19 | `api_key_invocation` | enable api_key scheme, gen key, invoke | `key-manager/api-key` | ✅ covered |
| 20 | `invalid_token_invocation` | invoke with garbage token → 401 | `gateway/security-enforcement` | ✅ covered |
| 21 | `custom_authorization_header` | invoke with custom auth header | `gateway/custom-auth-header` | ✅ covered (own overlay block; +negative) |
| 22 | `governance_policy_baseline` | common + API-specific operation policy CRUD | `publisher/operation-policies` | ✅ covered (improved — see §3) |

**Tally:** 22 of 22 fully covered · **0 parked**.

> **All parked items now closed.** `governance_policy` → `publisher/operation-policies`; `custom-auth-header`
> and org/group sharing → the shared `IntegrationV2-CustomAuthHeaderAndAppSharing` block, which carries both
> features' extra config (`[apim.oauth_config] auth_header` + `[apim.devportal] enable_application_sharing`) in
> one feature-specific overlay merged on top of `basic` via the new `tomlExtraOverlayPath` block parameter — so
> one extra container serves both instead of two.

---

## 2. Intentionally dropped test cases (and why)

| Dropped / changed | From | Reason |
|-------------------|------|--------|
| **Inline `Delete the …` teardown scenarios** | every legacy feature | Replaced by the `@After`/`@AfterClass` cleanup hooks. Inline deletes get skipped when an earlier step fails (residue); hooks are idempotent and run on failure. |
| **The ×4 `@Factory` user-mode fan-out** | all features | Retired. Running *every* test in 4 modes (super-admin, super-user, tenant-admin, tenant-user) is mostly redundant. Replaced by **explicit ×2-tenant `Scenario Outline`s** (super + tenant) on the meaningful axis, plus targeted role negatives — coverage of the distinct paths without the blanket cost. |
| **Literal name assertions** (`contains "APIMTest"`, `"apiTestContext"`) | api-lifecycle, search, etc. | Names are now `${UNIQUE:…}`-randomised for parallel safety, so literals are invalid. Replaced by dynamic-value assertions (capture the generated name/context, assert on that) or structural checks. |
| **`isDefaultVersion` substring check** | api_versioning | The legacy `contains "<defaultProperty>"` was a whole-body substring match (passed on *any* boolean field) — not a real check. We dropped it and **replaced it with a real value+flip assertion** (see §4, closed) rather than port the false-confidence version. |
| **Org/group sharing assertion** (`groups:["org1"]`) | create_new_application | Requires `enable_cross_tenant_group_sharing` server config the default container lacks; the server silently drops the field. Deferred to a config-specific feature rather than asserting a no-op. |
| **Migration-only access-token-from-file step** | (invocation steps) | The `get generated access token from file` step is a migration-dataset concern; not needed for fresh-provisioned v2 tokens. |
| **External SOAP backend** (`ws.cdyne.com`) | create_deploy_publish (SOAP) | Replaced by an in-network `soap-stub` (`nodebackend:3019`) so SOAP invocation is deterministic and not dependent on a flaky external service. |

---

## 3. Additionally covered (beyond the legacy)

| Added area | Where | Note |
|-----------|-------|------|
| **Tenant ×2 on every feature** | all publisher/devportal/gateway/key-manager | Each feature runs in **super tenant AND tenant1.com** explicitly (legacy did this only via the blanket ×4 factory, often skipped in practice). |
| **Least-privilege actor model** | publisher features run as `publisherUser` (creator+publisher, non-admin) | Legacy ran everything as admin in most modes; we prove the actual minimum privilege works. |
| **Role-enforcement negatives** (NEW class of test) | api-lifecycle, definitions, docs, graphql/soap/streaming-design, scopes, api-config, devportal applications/subscribe/subscription-management | "A subscriber/publisher-role user is **rejected** (401)" — the legacy had almost no negative role coverage. |
| **No-subscription gateway negative** | `gateway/security-enforcement` | Valid token from an **unsubscribed** app → 403. Distinct runtime-enforcement path the legacy didn't isolate. |
| **Invalid-API-key negative** | `key-manager/api-key` | Garbage API key → 401. |
| **Capability-isolated invocation** | `gateway/{rest,soap,graphql}-invocation` as standalone `@cap:gateway` features | Invocation is now first-class and reusable, not buried inside publisher create flows. |
| **Operation-policy CRUD hardened** | `publisher/operation-policies` | Legacy asserted only 201/200. Port adds ×2-tenant, **content assertions** (persisted spec name/category, listed among common policies), and two negatives: subscriber→401 (role) and malformed-spec rejection (validation; APIM surfaces it as 500 — asserted with the validation message, quirk noted). |
| **Custom-auth-header negative** | `gateway/custom-auth-header` | Beyond the legacy positive, asserts the standard `Authorization` header is **rejected** (401) when a custom auth header is configured. |
| **Reusable extra-overlay block mechanism** | `tomlExtraOverlayPath` + `Utils.mergeTomls` | New framework hook: a block layers a small feature-specific TOML overlay on top of `basic` (base + basic + extra) without restating the whole distribution config — lets unrelated config-needing features (custom-auth + app-sharing) share one container. |
| **In-product graceful server restart** | `ServerLifecycleSteps` + `ServerReadiness.awaitRestart` | New framework capability: a step bounces the carbon JVM in place via `ServerAdmin.restartGracefully` (container/ports/DB untouched) and blocks on a down→up readiness gate. Enables restart-class tests (token persistence) the v2 shared-container model otherwise couldn't express. |

---

## 4. Gaps worth considering (legacy had it; port does not / only partially)

| Gap | Legacy source | Severity | Note |
|-----|---------------|----------|------|
| ~~**Custom authorization header**~~ | `custom_authorization_header` | — | ✅ **Closed.** Ported to `gateway/custom-auth-header` in the `IntegrationV2-CustomAuthHeaderAndAppSharing` block (overlay `[apim.oauth_config] auth_header = "Test-Custom-Header"`). ×2-tenant positive (token in the custom header → 200) plus a negative (same token in standard `Authorization` → 401). |
| ~~**Governance / operation policies**~~ | `governance_policy_baseline` | — | ✅ **Closed.** Ported to `publisher/operation-policies` (CRUD of common + API-specific operation policies), improved with ×2-tenant, content assertions, and role/validation negatives. The cleanup leak is fixed — common (tenant-global) policies are registered to `CREATED_OPERATION_POLICY_IDS` and swept by `ResourceCleanup`. |
| **API Products** | `migration/migrated_api_product`, `migration/new_api_product_from_migrated_apis` (migration only) | Low | **Not a port gap** — the legacy tested API Products only in the **migration** suite (out of scope), never in the 22 product features. So neither the source *product* set nor our port covers fresh API-Product creation. A `publisher/products` feature would be net-new coverage, not parity. |
| ~~**Multi-API-type config matrix**~~ | `api_runtime_configurations` / `api_other_common_configurations` ran config updates across REST+SOAP+GraphQL+WS | — | ✅ **Closed (full cross-product).** `api-config` runs the runtime-config fields (caching, cacheTimeout, schemaValidation, transport, CORS) on REST/SOAP/GraphQL, the throttling policy on WS, and the common-config fields (`additionalProperties`, `operations` with type-specific verbs, `policies`) on **all four** types — each **×2 tenant** (super + tenant1.com). `_setup_config_api` creates all four base API types in both tenants. Exceeds the legacy, which ran the type matrix in the super tenant only. |
| ~~**Token persistence across restart**~~ | `restart/token_persistence_restart` | — | ✅ **Closed.** Ported to `key-manager/token-persistence` in the new **`IntegrationV2-ServerRestart`** block. Asserts a valid token still works and a revoked token stays revoked across **two** graceful server restarts. Implemented via an **in-product** graceful restart (Carbon `ServerAdmin.restartGracefully` SOAP op, gated by `[server] enable_restart_from_api`) — the container/ports/DB are untouched, only the JVM bounces — plus `ServerReadiness.awaitRestart()` (down→up) and the `token-persistence` overlay. Block runs `thread-count=1` (restart bounces the shared server). |
| **SOAP/GraphQL invocation ×2 robustness** | — | Low | Covered, but SOAP/GraphQL invocation depend on stub/sample backends; richer payload/query assertions (vs "200 + routed") could be deepened. |
| ~~**`isDefaultVersion` correctly asserted**~~ | api_versioning | — | ✅ **Closed.** `versioning.feature` now asserts the real single-default-version behaviour: after creating v2.0.0 as default, the new version's `isDefaultVersion` is `true` **and** the original version flips to `false` (re-fetch + reflect, with retry for propagation). This is stronger than the legacy substring check, which only matched the literal "true"/"false" anywhere in the body. |
| ~~**Org/group sharing**~~ | create_new_application | — | ✅ **Closed.** Ported to `devportal/application-sharing` in the same `IntegrationV2-CustomAuthHeaderAndAppSharing` block (overlay `[apim.devportal] enable_application_sharing = true`). ×2-tenant: an application updated with `groups:["org1"]` persists the field (asserted), which the default container silently drops. |

---

## 5. Summary

- **23 / 23** legacy product features fully covered; **0 parked, 0 outstanding**.
- Coverage is **reorganised by capability**, so several legacy features map to multiple ported features (and vice-versa) — tracked scenario-level above.
- The port **adds** a whole negative/role-enforcement axis and explicit per-tenant coverage the legacy lacked.
- **No un-ported legacy product features remain.** Every closed gap this round — multi-API-type config matrix, `isDefaultVersion` assertion, operation-policy CRUD (governance), custom-auth-header, org/group sharing, and token-persistence-across-restart — is verified green. **API Products** stays migration-only (net-new, not a port gap).
