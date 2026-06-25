# integration-v2 Product Coverage Map (`master-new-test-framework-public`)

> **Purpose.** Map what the **product** Cucumber tests on `master-new-test-framework-public` actually
> cover, using the *same capability vocabulary* as [`legacy-feature-coverage-map.md`](legacy-feature-coverage-map.md)
> so the two can be cross-checked by eye. This branch's v2 tests were **not authored by us** and the suite
> is partial — this map is the honest inventory we compare against the legacy baseline.
>
> **Scope — what IS mapped.** Only genuine product feature tests: `features/publisher/` (9 files),
> `features/header/` (1), `features/legacyApplicationSecrets/` (1) — **11 feature files, 73 scenarios**.
>
> **Scope — what is NOT mapped (excluded as migration / framework / infra).**
> - `features/migration/` (22 files: `migrated_*`, `legacy_api_key`, `api_policies`, `api_provider_change`,
>   `new_api_product_from_migrated_apis`, …) — **migration tests**, wired via the
>   `runners.migration.*` classes in `testng.xml` (the suite AnuGayan added in commit *"Improve v2 test execution"*).
> - `features/framework-verification/` (9 files: `block_probe*`, `dynamic_lifecycle`) — framework self-tests.
> - `features/common/` (6 files: system init/shutdown, tenant init) — infrastructure, not coverage.
> - The `testng-parity-baseline.xml` and `testng-auth-context-validation.xml` suites AnuGayan added are
>   migration/parity-validation harnesses; the publisher features they reference (`jwt_token_format`,
>   `subscription_throttling_policy`, `devportal_search_visibility`, `graphql_api_baseline`,
>   `websocket_api_baseline`, `governance_policy_baseline`, `openid_token`, `refresh_token`, `revoke_token`,
>   `sandbox_token`, `api_key_invocation`, …) do **not exist on this branch** — they live only on
>   `master-new-test-framework`. So they are out of scope here by construction.
>
> **Method.** Hand-curated from reading the `.feature` files on `master-new-test-framework-public`.
> Unit of coverage is the **Cucumber scenario** (vs. the legacy `@Test` method). Line numbers are anchors.
> Scenarios that are pure plumbing are annotated `(setup)` / `(teardown)` so real behavior coverage stands out.
>
> **Date.** 2026-06-26. Path prefix for all entries:
> `all-in-one-apim/modules/integration-v2/tests-integration/cucumber-tests/src/test/resources/features/`

## Health notes (observed while mapping)

- **No `@tags` anywhere.** None of the 11 product features carry Gherkin tags (no `@cap`/`@feat`/`@rule`,
  not even `@cleanup`). The future tag-convention DX work starts from zero here.
- **GraphQL is setup-only.** "Create GraphQL API" appears as a fixture in 4 features but **no GraphQL
  behavior** (query/subscription/complexity/depth) is asserted. Counted under GraphQL APIs as setup, not coverage.
- **Heavy plumbing ratio.** Of 73 scenarios, a large share are create/delete app+API lifecycle plumbing;
  net distinct product behaviors are fewer (see annotations).

## Capability index

Capabilities use the legacy map's names so rows line up for cross-checking.

| Capability | Covered here? | Where |
| ---------- | ------------- | ----- |
| API Publishing & Lifecycle | ✅ | create_an_api_through…, create_deploy_publish_an_api, create_new_application |
| API Configuration & Policies | ✅ | api_runtime_configurations, api_other_common_configurations |
| API Security | ✅ (partial) | api_other_common_configurations (scopes), header/custom_authorization_header |
| API Visibility & Access Control | ❌ | — |
| API Versioning | ✅ (thin) | api_versioning (1 scenario) |
| API Definitions & Import/Export | ✅ (import only) | import_OAS_definition |
| API Documentation | ✅ | api_documents |
| API Products | ❌ | — |
| API Revisions | ❌ | — |
| Applications & Subscriptions | ✅ | create_new_application, legacyApplicationSecrets/create_new_application, others |
| Authentication, Tokens & Key Management | ✅ | api_bound_api_key, create_new_application (client secrets), legacyApplicationSecrets |
| DevPortal / Store & Search | ❌ | — (search/store features are migration-only) |
| Multi-tenancy & Organizations | ❌ | — |
| Throttling & Rate Limiting | ❌ | — |
| Admin, Governance & Workflows | ❌ | — |
| Gateway, Mediation & Invocation | ✅ (partial) | create_deploy_publish (REST+SOAP invoke), invoke scenarios |
| GraphQL APIs | ⚠️ setup-only | api_documents, api_other_common_configurations, api_runtime_configurations, create_deploy_publish |
| Streaming APIs (WebSocket/WebSub/Async) | ❌ | — |
| AI APIs & MCP | ❌ | — |
| REST API Surface & Service Catalog | ❌ | — |
| Analytics, Logging & Observability | ❌ | — |
| Platform & Server Management | ❌ | — |

## Coverage detail by capability

### API Publishing & Lifecycle
- **create-via-publisher-rest** — Create an API through the Publisher REST API
  - `publisher/create_an_api_through_the_publisher_rest_api_test.feature:10`
- **get-api-by-id** — Retrieve API details by ID
  - `publisher/create_an_api_through_the_publisher_rest_api_test.feature:13`
- **update-api** — Update description/tiersCollection; verify name is not changed by update
  - `publisher/create_an_api_through_the_publisher_rest_api_test.feature:22`, `:33`
- **publish-api** — Publish the API (lifecycle change)
  - `publisher/create_an_api_through_the_publisher_rest_api_test.feature:42`
- **list-all-apis** — Retrieve all APIs created through the Publisher REST API
  - `publisher/create_an_api_through_the_publisher_rest_api_test.feature:115`
- **create-deploy-publish** — Create + deploy + publish API(s) end to end
  - `publisher/create_deploy_publish_an_api.feature:8` (Create APIs, outline), `:27` (Deploy, outline)
  - `publisher/create_new_application.feature:18` (Create a new API, deploy and publish it)
- _Teardown:_ delete API — `create_an_api_through…:150`, `create_deploy_publish_an_api.feature:131`, `create_new_application.feature:181`

### API Configuration & Policies
- **update-runtime-configuration** — Update API runtime configuration
  - `publisher/api_runtime_configurations.feature:31` (outline)
- **configure-custom-properties** — Set API custom properties
  - `publisher/api_other_common_configurations.feature:32` (outline)
- **configure-resources** — Configure API resources
  - `publisher/api_other_common_configurations.feature:53` (outline)
- _Setup/teardown:_ create API — `api_runtime_configurations.feature:9,:21`; remove APIs — `:68`

### API Security
- **shared-scope-crud** — Create/add/update/delete shared scope and API scopes
  - `publisher/api_other_common_configurations.feature:95` (create shared scope), `:102` (add scope to API, outline), `:124` (update scopes, outline), `:150` (delete shared scope)
- **custom-authorization-header** — Create API with a custom authorization header and invoke with it
  - `header/custom_authorization_header.feature:6` (create API w/ custom auth header), `:64` (invoke with `Test-Custom-Header`)

### API Versioning
- **api-version-lifecycle** — Full lifecycle of an API version (single scenario outline)
  - `publisher/api_versioning.feature:8` (outline) — *thin vs legacy version/copy/default-version family*

### API Definitions & Import/Export
- **import-oas** — Import API definition from OpenAPI 2 / 3 / 3.1
  - `publisher/import_OAS_definition.feature:10` (outline)
  - _Setup/teardown:_ create application `:6`, remove resource `:64`
  - *Import only; no export, no SOAP/WSDL coverage (legacy has both).*

### API Documentation
- **add-api-document** — Add a new document to an API
  - `publisher/api_documents.feature:31` (outline)
  - _Setup:_ create REST API `:9` (outline), create GraphQL API `:21`

### Applications & Subscriptions
- **create-application** — Create a new application
  - `publisher/create_new_application.feature:10`
  - `legacyApplicationSecrets/create_new_application.feature:7` (multiple-consumer-secrets-disabled variant)
- **subscribe-to-api** — Subscribe to an API using an application
  - `publisher/create_an_api_through_the_publisher_rest_api_test.feature:77`
  - `publisher/create_new_application.feature:29`
  - `header/custom_authorization_header.feature:51`
- **update-subscription** — Update subscription of an API
  - `publisher/api_other_common_configurations.feature:71` (outline)
- _Teardown:_ delete subscription/application — `create_an_api_through…:142,:146`; `create_new_application.feature:173,:177`; `custom_authorization_header.feature:92,:96`; `legacyApplicationSecrets/create_new_application.feature:51`

### Authentication, Tokens & Key Management
- **api-bound-api-key** — Generate, associate, dissociate and revoke an API-bound API key (from API side and application side), regenerate
  - `publisher/api_bound_api_key.feature:52` (API side), `:113` (application side + regenerate)
  - _Setup/teardown:_ `:7`, `:165`, `:169`
- **application-key-generation** — Generate initial application keys / access token
  - `publisher/create_new_application.feature:43`
  - `legacyApplicationSecrets/create_new_application.feature:16` (generate keys), `:32` (generate access token)
- **multiple-client-secrets** — Generate multiple client secrets, mint tokens from two secrets, verify count, delete secrets (incl. attempt to delete latest = rejected), verify only latest remains, verify old token still valid after secret deletion
  - `publisher/create_new_application.feature:59`, `:86`, `:122`, `:129`, `:140`, `:152`, `:163`, `:168`
  - _Note: the only genuinely novel-vs-legacy behavior cluster in this branch._

### Gateway, Mediation & Invocation
- **invoke-rest-api** — Invoke a published REST API through the gateway
  - `publisher/create_an_api_through_the_publisher_rest_api_test.feature:127`
  - `publisher/create_deploy_publish_an_api.feature:82` (outline)
- **invoke-soap-api** — Invoke a SOAP API through the gateway
  - `publisher/create_deploy_publish_an_api.feature:102` (outline)
- **invoke-with-generated-tokens** — Invoke API using tokens generated from two client secrets
  - `publisher/create_new_application.feature:112`
  - _Setup/teardown:_ application creation `create_deploy_publish_an_api.feature:78`, delete app `:127`

### GraphQL APIs ⚠️ setup-only
- **create-graphql-api (fixture)** — GraphQL APIs are created as setup but no GraphQL-specific behavior is asserted
  - `publisher/api_documents.feature:21`, `api_other_common_configurations.feature:21`, `api_runtime_configurations.feature:21`, `create_deploy_publish_an_api.feature:21`
  - *No query/mutation/subscription/complexity/depth coverage (legacy has a full GraphQL section).*

## Cross-check against the legacy baseline (observational)

Read alongside `legacy-feature-coverage-map.md`. These are plain observations from the two maps — **not** an
automated gap tool; they're here so the gaps are visible at a glance.

**Legacy capabilities with NO product coverage on this branch:**
API Visibility & Access Control · API Products · API Revisions · DevPortal / Store & Search ·
Multi-tenancy & Organizations · Throttling & Rate Limiting · Admin, Governance & Workflows ·
Streaming APIs (WebSocket/WebSub/Async) · AI APIs & MCP · REST API Surface & Service Catalog ·
Analytics, Logging & Observability · Platform & Server Management.

**Covered but materially thinner than legacy:**
- *API Versioning* — 1 scenario vs the legacy version/copy/default-version family.
- *API Definitions & Import/Export* — OAS import only; no export, no SOAP/WSDL.
- *GraphQL APIs* — fixture creation only; legacy asserts invocation/complexity/depth.
- *Gateway, Mediation & Invocation* — basic REST/SOAP invoke; no mediation policies, fault handling, headers.

**At/near parity (worth a closer look for overlap):**
- *Authentication, Tokens & Key Management* — API-bound API key + multiple-client-secret lifecycle is solid;
  the multi-client-secret cluster is genuinely new vs legacy.
- *Applications & Subscriptions* — create/subscribe/update covered (CRUD breadth still below legacy).

## Coverage tree (capability → feature → scenario)

A condensed, scan-friendly view of the product coverage only. `(s)` = setup, `(t)` = teardown.

```
integration-v2 product tests (master-new-test-framework-public)
├── API Publishing & Lifecycle
│   ├── create_an_api_through_the_publisher_rest_api_test.feature
│   │   ├── Create an API Through the Publisher Rest API        :10
│   │   ├── Get the API details by ID                           :13
│   │   ├── Update API (description/tiers; name unchanged)      :22 :33
│   │   ├── Publish the API                                     :42
│   │   ├── Retrieve all APIs                                   :115
│   │   └── Delete the created API (t)                          :150
│   ├── create_deploy_publish_an_api.feature
│   │   ├── Create APIs (outline)                               :8
│   │   ├── Deploy an API (outline)                             :27
│   │   └── Delete the created resources (t, outline)           :131
│   └── create_new_application.feature
│       ├── Create a new API, deploy and publish it             :18
│       └── Delete the created API (t)                          :181
├── API Configuration & Policies
│   ├── api_runtime_configurations.feature
│   │   ├── Creating an API (s, outline)                        :9
│   │   ├── Update runtime configuration of API (outline)       :31
│   │   └── Remove the APIs (t, outline)                        :68
│   └── api_other_common_configurations.feature
│       ├── Configuring Custom properties (outline)             :32
│       └── Configuring Resources (outline)                     :53
├── API Security
│   ├── api_other_common_configurations.feature
│   │   ├── Create a new shared scope                           :95
│   │   ├── Add scope to API (outline)                          :102
│   │   ├── Update Scopes (outline)                             :124
│   │   └── Delete the created shared scope (t)                 :150
│   └── header/custom_authorization_header.feature
│       ├── Create an API with custom authorization header      :6
│       └── Invoke API using Test-Custom-Header                 :64
├── API Versioning
│   └── api_versioning.feature
│       └── Full lifecycle of an API version (outline)          :8
├── API Definitions & Import/Export
│   └── import_OAS_definition.feature
│       ├── Create an application (s)                           :6
│       ├── Import API Definition — OAS 2/3/3.1 (outline)       :10
│       └── Removing created resource (t)                       :64
├── API Documentation
│   └── api_documents.feature
│       ├── Create an API (s, outline)                          :9
│       ├── Create GraphQL API (s)                              :21
│       └── Add a new Document for API (outline)                :31
├── Applications & Subscriptions
│   ├── create_new_application.feature
│   │   ├── Create new application                              :10
│   │   ├── Subscribe the new API using the application         :29
│   │   ├── Delete the subscription (t)                         :173
│   │   └── Delete the created application (t)                  :177
│   ├── legacyApplicationSecrets/create_new_application.feature
│   │   ├── Create new application (secrets disabled)           :7
│   │   └── Delete the created Application (t)                  :51
│   ├── create_an_api_through_…rest_api_test.feature
│   │   ├── Subscribe to the API using an application           :77
│   │   ├── Delete the created subscription (t)                 :142
│   │   └── Delete the created Application (t)                  :146
│   ├── api_other_common_configurations.feature
│   │   └── Update Subscription of API (outline)               :71
│   └── header/custom_authorization_header.feature
│       ├── Create an application (s)                           :45
│       ├── Subscribe to the API                               :51
│       ├── Delete the subscription (t)                        :92
│       └── Delete the application (t)                         :96
├── Authentication, Tokens & Key Management
│   ├── api_bound_api_key.feature
│   │   ├── Setup API and Application (s)                       :7
│   │   ├── Generate/associate/dissociate/revoke (API side)    :52
│   │   ├── Associate (app side) + regenerate                  :113
│   │   ├── Delete the application (t)                          :165
│   │   └── Delete the API (t)                                 :169
│   ├── create_new_application.feature
│   │   ├── Generate initial application keys                  :43
│   │   ├── Generate multiple client secrets                   :59
│   │   ├── Generate access tokens using two client secrets    :86
│   │   ├── Invoke API using those tokens                      :112
│   │   ├── Verify application has three client secrets        :122
│   │   ├── Delete the initial client secret                   :129
│   │   ├── Delete the first additional client secret          :140
│   │   ├── Attempt to delete the latest client secret         :152
│   │   ├── Verify only the latest client secret remains       :163
│   │   └── Verify oldest token valid after secret deletion    :168
│   └── legacyApplicationSecrets/create_new_application.feature
│       ├── Generate keys                                       :16
│       ├── Generate Access Token                               :32
│       └── Delete generated keys (t)                           :45
├── Gateway, Mediation & Invocation
│   ├── create_an_api_through_…rest_api_test.feature
│   │   └── Invoke API                                          :127
│   ├── create_deploy_publish_an_api.feature
│   │   ├── Application creation (s)                            :78
│   │   ├── Invoking apis — REST (outline)                     :82
│   │   ├── Invoking SOAP api (outline)                        :102
│   │   └── Delete the app (t)                                 :127
│   └── create_new_application.feature
│       └── Invoke API using tokens from two client secrets    :112
└── GraphQL APIs  (setup-only — no behavior asserted)
    └── Create GraphQL API (fixture in 4 features)
        api_documents:21 · api_other_common_configurations:21 · api_runtime_configurations:21 · create_deploy_publish_an_api:21
```

## Excluded from this map (for the record)

| Bucket | Dir | Files | Why excluded |
| ------ | --- | ----- | ------------ |
| Migration tests | `features/migration/` | 22 | Migration coverage (`runners.migration.*` in `testng.xml`, added by AnuGayan in *"Improve v2 test execution"*) |
| Framework verification | `features/framework-verification/` | 9 | `block_probe*`, `dynamic_lifecycle` — framework self-tests |
| Infrastructure | `features/common/` | 6 | System init/shutdown, tenant init — not product coverage |
