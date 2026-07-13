# Legacy Integration Test Coverage Map

> **Purpose.** A curated, feature-shaped map of what the **legacy TestNG backend suite**
> (`all-in-one-apim/modules/integration/tests-integration/tests-backend`) actually exercises.
> It is the *trusted baseline* we cross-check the new integration-v2 (Cucumber) coverage against,
> and the primary tool for spotting **features covered by multiple test classes** (duplicate candidates)
> before we re-author them.
>
> **Scope.** Legacy `tests-backend` only. ~255 `*TestCase` classes across ~40 packages were partitioned
> into six capability buckets (Parts A–F) and mapped by exploration agents to
> *capability → feature → behavior → `class:line` (method)*.
>
> **Method.** Hand-curated from reading the test sources — feature-organized, **not** code-coverage-derived.
> Line numbers are approximate anchors, not exact. `(dup)` markers and each part's
> "Duplicate coverage" footer flag overlaps; the consolidated
> [Cross-section duplicate candidates](#cross-section-duplicate-candidates) section below synthesizes them.
>
> **Date.** 2026-06-26. Path prefix for all entries:
> `all-in-one-apim/modules/integration/tests-integration/tests-backend/src/test/java/org/wso2/am/integration/tests/`

## How this document is organized

The suite was split into six partitions so parallel agents could map it in depth. Each partition keeps
its own feature tree and a per-section duplicate footer. **Capabilities span partitions** — use the index
below to find every section that touches a capability, then read the consolidated duplicate section to see
where the same feature is tested more than once.

| Part | Packages mapped | ~Classes |
| ---- | --------------- | -------- |
| A | `api/`, `version/`, `publisher/`, `apiproduct/` | 81 |
| B | applications, subscriptions, auth/tokens/key-mgmt, tenancy | 28 |
| C | admin, governance, workflows, throttling, tenancy, gateway-policy | 24 |
| D | `graphql/`, streaming (WebSocket/WebSub/Async), config & policies | 25 |
| E | `aiapi/`+MCP, REST API surface & service catalog, analytics, platform | 28 |
| F | `other/` catch-all (bug-ID regressions) | 69 |

## Capability index

Each capability links to the parts that cover it. A capability appearing in **multiple parts** is itself a
signal to check the duplicate section.

| Capability | Covered in parts |
| ---------- | ---------------- |
| API Publishing & Lifecycle | A, F |
| API Configuration & Policies | A, D, F |
| API Security | A |
| API Visibility & Access Control | A |
| API Versioning | A, F |
| API Definitions & Import/Export | A, D, F |
| API Documentation | A |
| API Products | A |
| API Revisions | A |
| Applications & Subscriptions | B, F |
| Authentication, Tokens & Key Management | B, F |
| DevPortal / Store & Search | A, B, E, F |
| Multi-tenancy & Organizations | B, C, F |
| Throttling & Rate Limiting | C, D, F |
| Admin, Governance & Workflows | C, F |
| Gateway, Mediation & Invocation | C, E, F |
| GraphQL APIs | D |
| Streaming APIs (WebSocket / WebSub / Async) | D |
| AI APIs & MCP | E |
| REST API Surface & Service Catalog | E |
| Analytics, Logging & Observability | E, F |
| Platform & Server Management | E, F |

## Cross-section duplicate candidates

This consolidates the per-part "Duplicate coverage" footers and adds **cross-partition** overlaps — the
same feature tested in classes that landed in different partitions. These are the highest-value targets to
collapse when re-authoring in integration-v2. Within-partition detail lives in each part's own footer below.

### Cross-partition (same feature, classes in different parts)

- **Scopes & shared-scope enforcement** — *Part B* (Authentication, Tokens & Key Management) + *Part F*
  (`APIScopeTestCase`, `APIScopeTestForTenantsTestCase`, `APICreationForTenantsTestCase`, `AllowedScopesTestCase`,
  `AllowedScopesTestWithCorsDisabled`, `SharedScopeTestCase`, `SharedScopeTestWithRestart`,
  `TokenEncryptionScopeTestCase`). The `other/` scope classes largely re-test mainstream OAuth-scope /
  shared-scope behavior. Also touches *Part D* (`WebSocketAPIScopeTestCase`).
- **Subscriptions (create / list / delete)** — *Part B* (Applications & Subscriptions) + *Part E*
  (`restapi/testcases/SubscriptionTestCase`, `MultipleSubscriptionsTestCase`, `OnHoldSubscriptionWorkflowIdTestCase`)
  + *Part F* (`APIM710AllSubscriptionsByApplicationTestCase`, `APIMANAGER4480AllSubscriptionsByApplicationTestCase`,
  `DeleteSubscribedApiFromPublisherTestCase`). Single vs. multiple vs. workflow-ref vs. listing/delete slices of
  one subscription surface.
- **Throttling / rate limiting** — *Part C* (`throttling/*`, `restapi/admin/throttlingpolicy/*` CRUD) + *Part D*
  (`websub/ThrottlingTestCase`) + *Part F* (throttling regressions in `other/`). The admin policy-CRUD matrix
  (Application / Subscription / Advanced / Custom) repeats the same add+get+update+delete+dup-name+missing-id shape.
- **API version-copy** — *Part A* (version family: APIM366/370/371/372/373/374 pairs re-run across HTTP verbs,
  `Accessibility*Copy*` lifecycle classes) + *Part F* (`CopyNewVersionTestCase`, `NewCopyWithDefaultVersion`,
  `SameVersionAPITestCase`).
- **Import / Export & SOAP/WSDL handling** — *Part A* (API Definitions & Import/Export) + *Part D* (Async
  import/publish) + *Part F* (`APIImportExportTestCase`, `SOAPAPIImportExportTestCase`, `WSDLImportTestCase`,
  `SoapToRestTestCase`, `APIMANAGER5843WSDLHostnameTestCase`).
- **API search across portals** — *Part A* (DevPortal/Store search) + *Part B* (DevPortal search) + *Part E*
  (`restapi/ContentSearchTestCase`, `search/DevPortalSearchTest`, `rest/SearchPaginatedAPIsWithMultipleStatusTestCase`)
  + *Part F* (DevPortal search). Same search surface exercised from publisher / devportal / paginated paths.
- **Gateway error & fault handling** — *Part C* / *Part E* (gateway invocation & mediation) + *Part F* regression
  edge cases: `ErrorMessageTypeTestCase`, `ErrorResponseCheckTestCase`, `APIMANAGER5326CustomStatusMsgTestCase`,
  `APIM5474SingleCharacterQueryParameter`, `APIMANAGER3965TestCase`; status-code passthrough pair
  (`APIMANAGER4464...204` / `APIMANAGER4533...200`); Location-header trio (`LocationHeaderTestCase`,
  `RelativeUrlLocationHeaderTestCase`, `GIT2231HeadRequestNPEErrorTestCase`).
- **Change-API-provider** — *Part C* (`admin/ChangeApiProviderTestCase` vs `ChangeApiProviderSecondaryUserStoreTestCase`,
  primary vs secondary user store). Edge of mainstream lifecycle in *Part A*.
- **Application CRUD** — *Part B* (Applications & Subscriptions) + *Part F* (`APIM678ApplicationCreationTestCase`).

### Within-partition (see each part's footer for line-level detail)

- **Part A** — version/copy/re-subscription family (APIM366/372, 370/373, 371/374 matched pairs across verbs);
  endpoint-security split across Add/Change classes; mutual-SSL overlap (`APISecurityTestCase` vs
  `APISecurityMutualSSLCertificateChainValidationTestCase`); create-API-via-publisher-REST repeated;
  default-version repeated for products.
- **Part B** — refresh-token (`RefreshTokenTestCase` vs `TokenAPITestCase`); cross-tenant subscription
  (`CrossTenantSubscriptionUpdateTestCase` ≡ `CrossTenantSubscriptionTestCase`); backend-JWT-for-JWT-app verified 3×
  (`JWTTestCase` / `FederatedUserJWTTestCase` / `URLSafeJWTTestCase`, same `testEnableJWTAndClaimsForJWTApp`);
  comment CRUD near-mirror (`DevPortalCommentTest` vs `PublisherCommentTest`); 4 `TokenPersistence*TestSuite`
  re-run base token flows.
- **Part C** — `JWTRequestCountThrottling` vs `JWTBandwidthThrottling` near-mirrors; subscription throttling
  re-tested 3×; the 4 `restapi/admin/throttlingpolicy` CRUD classes share one matrix; change-provider pair;
  compliance pair (`APIComplianceTestCase` REST vs `MCPComplianceTestCase`).
- **Part D** — shared publish+subscribe boilerplate across WebSocket (×4) and WebSub (×6) families;
  GraphQL invocation in `GraphqlTestCase` + `GraphQLQueryAnalysisTest`; GraphQL complexity config vs
  enforcement split; Async create/import/subscribe (`AsyncAPITestCase` vs `AsyncAPITestWithValidationCase`).
- **Part E** — subscription-via-REST split across 3 `restapi/testcases` classes; prototype-API store visibility 2×;
  AI API create/publish/invoke duplicated (`AIAPITestCase` vs Gemini unlimited-tier variant); search re-exercised
  from publisher/devportal/pagination.
- **Part F** — `other/` is the largest cross-suite duplicate source: ~8 scope classes; gateway
  invocation/mediation/fault bug-ID regressions; 3 overlapping subscription-listing + app CRUD; version-copy trio;
  import/export (standard/SOAP/WSDL). Bug-ID regression classes broadly re-verify mainstream features in edge cases.

## Coverage detail by partition

Each partition's full feature tree follows. Headings are demoted one level to nest under this document; every part's own "Duplicate coverage (within this section)" footer is preserved.

## Legacy TestNG Coverage Map — Part A (api/, version/, publisher/, apiproduct/)

Scope root: `all-in-one-apim/modules/integration/tests-integration/tests-backend/src/test/java/org/wso2/am/integration/tests/`
Paths below are relative to that root.

### API Publishing & Lifecycle

#### API Creation & Deployment
- **create-deploy-with-mutual-ssl** — Create and deploy an API with Mutual SSL enabled
  - `api/APICreationTestCase.java:72` (testCreateAndDeployApiWithMutualSSLEnabled)
- **create-deploy-with-gateway-type** — Create and deploy an API specifying gateway type
  - `api/APICreationTestCase.java:94` (testCreateAndDeployApiWithGatewayType)
- **create-via-publisher-rest** — Create an API through the Publisher REST API
  - `publisher/APIM18CreateAnAPIThroughThePublisherRestAPITestCase.java:104` (testCreateAnAPIThroughThePublisherRest)
  - (dup) also covered by `publisher/APIM519CreateAnAPIThroughTheRestAPIWithoutLoggingInTestCase.java:82`, `publisher/APIM574...TestCase.java:87`
- **create-with-malformed-context** — Reject API creation with a malformed context
  - `publisher/APIM18CreateAnAPIThroughThePublisherRestAPITestCase.java:154` (testCreateAnAPIWithMalformedContextThroughThePublisherRest)
  - (dup) also covered by `publisher/APIMANAGER5834APICreationWithInvalidInputsTestCase.java:69` (invalid context) and `apiproduct/APIProductCreationTestCase.java:302` (product malformed context)
- **remove-via-publisher-rest** — Remove/delete an API through the Publisher REST API
  - `publisher/APIM18CreateAnAPIThroughThePublisherRestAPITestCase.java:178` (testRemoveAnAPIThroughThePublisherRest)
- **create-only-sandbox-endpoints** — Create an API that has only Sandbox endpoints
  - `publisher/APIM18CreateAnAPIThroughThePublisherRestAPITestCase.java:255` (testCreateApiWithOnlySandboxEndpoints)
- **create-without-login-rejected** — Creating an API without logging in is rejected
  - `publisher/APIM519CreateAnAPIThroughTheRestAPIWithoutLoggingInTestCase.java:82` (testCreateAnAPIThroughThePublisherRest)
- **create-missing-mandatory-fields-rejected** — Reject creation when mandatory fields (name/context/version/tier/action) are missing
  - `publisher/APIM514CreateAnAPIWithoutProvidingMandatoryFieldsTestCase.java:86` (no name), `:111` (no context), `:135` (no version), `:161` (no tier), `:230` (no action)
- **create-with-invalid-inputs-rejected** — Reject API creation with invalid context inputs
  - `publisher/APIMANAGER5834APICreationWithInvalidInputsTestCase.java:69` (testAPICreationWithInvalidContext)
- **context-must-match-previous-versions** — Context must match previous versions of same API
  - `publisher/APIMANAGER5834APICreationWithInvalidInputsTestCase.java:77` (testContextMatchesPreviousAPIVersions)
- **update-via-publisher-rest** — Update an existing API (incl. after rename) through Publisher REST
  - `publisher/APIM520UpdateAnAPIThroughThePublisherRestAPITestCase.java:86` (testUpdateAnAPIThroughThePublisherRest)
  - `publisher/APIM520UpdateAnAPIThroughThePublisherRestAPITestCase.java:144` (testUpdateAnAPIThroughThePublisherRestAfterRename)
- **list-all-apis / api-exists-check** — Get all APIs created and check if an API exists via Publisher REST
  - `publisher/APIM534GetAllTheAPIsCreatedThroughThePublisherRestAPITestCase.java:122` (testGetAllTheAPICreatedThroughThePublisherRestAPI)
  - `publisher/APIM534GetAllTheAPIsCreatedThroughThePublisherRestAPITestCase.java:162` (testCheckIfAnAPIExistsThroughThePublisherRestAPI)
- **empty-cors-configuration-handling** — Creating an API with empty CORS configuration behaves correctly
  - `publisher/CheckEmptyCORSConfigurationsTestCase.java:87` (testCheckEmptyCORSConfigurations)
- **edit-api-info-and-verify** — Edit API metadata and verify update reflected in Publisher
  - `api/lifecycle/EditAPIAndCheckUpdatedInformationTestCase.java:92` (testEditAPIInformation)
  - `api/lifecycle/EditAPIAndCheckUpdatedInformationTestCase.java:127` (testUpdatedAPIInformationFromAPIPublisher)
- **update-null-fields-bad-request** — Null endpointConfig / security scheme yields Bad Request
  - `api/lifecycle/UpdateAPINullPointerTestCase.java:78` (testBadRequestWithSecuritySchemeAsNull)
  - `api/lifecycle/UpdateAPINullPointerTestCase.java:89` (testBadRequestWithEndpointConfigAsNull)
- **availability-in-publisher-after-create** — Created API is available in Publisher
  - `api/lifecycle/APIPublishingAndVisibilityInStoreTestCase.java:63` (testAvailabilityOfAPIInPublisher)

#### API Lifecycle State Transitions
- **publish-and-store-visibility-transition** — Pre-publish hidden, post-publish visible in Store
  - `api/lifecycle/APIPublishingAndVisibilityInStoreTestCase.java:91` (testVisibilityOfAPIInStoreBeforePublishing)
  - `api/lifecycle/APIPublishingAndVisibilityInStoreTestCase.java:108` (testAPIPublishing)
- **change-status-via-rest-prototyped** — Move API to PROTOTYPED state via Publisher REST
  - `publisher/APIM574ChangeTheStatusOfAnAPIToPrototypedThroughThePublisherRestAPITestCase.java:118` (testChangeTheStatusOfTheAPIToPrototyped)
- **full-status-cycle-via-rest** — Drive API through CREATED/PUBLISHED/DEPRECATED/RETIRED via REST
  - `publisher/APIM574ChangeTheStatusOfAnAPIToPrototypedThroughThePublisherRestAPITestCase.java:134/149/165/181` (toCreated, toPublished, toDeprecated, toRetired)
- **block-api-blocks-invocation** — Blocked API rejects invocation; works before block
  - `api/lifecycle/AccessibilityOfBlockAPITestCase.java:87` (before block), `:130` (change to block), `:141` (after block)
- **retire-api-removes-from-store** — Retired API unavailable in Store and not invocable
  - `api/lifecycle/AccessibilityOfRetireAPITestCase.java:103/117/130/143/155` (before retire, deprecate, retire, store availability, after retire)
- **deprecate-old-on-new-publish** — Deprecate old version when publishing new copy; both invocable
  - `api/lifecycle/AccessibilityOfDeprecatedOldAPIAndPublishedCopyAPITestCase.java:96/149/160/176/200/212/224/254`
  - (dup) overlaps `version/APIM366...`, `version/APIM372...` (deprecate-old-version flow)
- **subscription-retained-on-demote-to-created** — Subscriptions retained when API demoted to CREATED
  - `api/lifecycle/APIMANAGER5337SubscriptionRetainTestCase.java:55` (testAPIErrorResponse)
- **lifecycle-tab-and-checklist-visibility** — LC tab, checklist items and LC-state-change visible in Publisher
  - `api/lifecycle/RegistryLifeCycleInclusionTest.java:64` (testAPIInfoLifecycleTabForPublishedAPI)
  - `api/lifecycle/RegistryLifeCycleInclusionTest.java:100` (testChecklistItemsVisibility)
  - `api/lifecycle/RegistryLifeCycleInclusionTest.java:124` (testLCStateChangeVisibility)
- **custom-lifecycle-states** — Custom lifecycle definition state transitions work
  - `api/lifecycle/CustomLifeCycleTestCase.java:104` (testCustomLifeCycle)
- **publish-old-and-copy-both-accessible** — Both published old and copied published API accessible
  - `api/lifecycle/APIAccessibilityOfPublishedOldAPIAndPublishedCopyAPITestCase.java:98/120/136/173/188/203` (copy, publish copy, availability, subscribe old/new, access both)

#### Copy / New Version Creation
- **copy-api-to-newer-version** — Copy an API to a new version via Publisher REST
  - `publisher/APIM548CopyAnAPIToANewerVersionThroughThePublisherRestAPITestCase.java:97` (testCopyAnAPIToANewerVersionThroughThePublisherRest)
- **older-version-exists-check** — Detect whether an older version of an API exists via REST
  - `publisher/APIM570CheckIfAnOlderVersionOfTheAPIExistsThroughThePublisherRestAPITestCase.java:109` (create+get)
  - `publisher/APIM570CheckIfAnOlderVersionOfTheAPIExistsThroughThePublisherRestAPITestCase.java:149` (testCheckIfAnOlderVersionOfTheAPIExists...)
- **new-version-create-and-update** — Create a new version, publish, then update it
  - `api/lifecycle/NewVersionUpdateTestCase.java:72` (testAPINewVersionCreation)
  - `api/lifecycle/NewVersionUpdateTestCase.java:104` (testNewVersionAPIUpdate)
- **multiple-versioned-apis-count-when-disabled** — API count correct when "display multiple versioned APIs" disabled
  - `api/lifecycle/NewVersionUpdateTestCase.java:125` (testCheckMultipleVersionedAPIsCount)
- **pluggable-versioning-strategy** — Pluggable versioning strategy resolves versions correctly
  - `api/lifecycle/PluggableVersioningStrategyTestCase.java:49` (testPluggableVersioningStratergy)

#### Config Change Test Harness (lifecycle setup)
- **apim-config-change-environment-setup** — Apply config / deploy webapps required by lifecycle suite (harness, not a feature assertion)
  - `api/lifecycle/APIManagerConfigurationChangeTest.java:47` (configureEnvironment)
  - `api/lifecycle/APIManagerConfigurationChangeTestSuite.java:37` (configureEnvironment, before/after-test setup; no @Test assertions)

### API Configuration & Policies

#### Resources & Operations
- **add-edit-remove-rest-resource** — Add/edit/remove REST resources and verify invocation
  - `api/lifecycle/AddEditRemoveRESTResourceTestCase.java:91/108/129/182` (GET, POST before add, POST+GET after add, after URL pattern)
- **invoke-similar-resources-different-verbs** — Resources with same path but different verbs resolve correctly
  - `api/lifecycle/APIInvocationWithSimilarResourcesAndDifferentVerbsTestCase.java:74` (testInvokeAllResources)
- **resource-special-characters-invocation** — GET resource with special characters in path invocable
  - `api/lifecycle/APIResourceWithSpecialCharactersInvocation.java:76` (testInvokeGETResourceWithSpecialCharacters)
- **uri-template-resources** — URI-template resources for default API and normal API; encode behavior
  - `publisher/APIResourceWithTemplateTestCase.java:92/120/148` (default API template, API template, uri encoding)
- **change-auth-type-of-resource** — Per-resource auth types (Application, Application User, both, None)
  - `api/lifecycle/ChangeAuthTypeOfResourceTestCase.java:120/150/190/240`

#### Endpoints & Endpoint Security
- **change-endpoint-url** — Change API backend endpoint URL and invoke new endpoint
  - `api/lifecycle/ChangeAPIEndPointURLTestCase.java:73/96/139` (before change, edit, after change)
- **endpoint-security-basic-per-type** — Add Basic endpoint security for Production / Sandbox / both
  - `api/lifecycle/AddEndPointSecurityPerTypeTestCase.java:166/229/292` (production, sandbox, both)
  - (dup) overlaps `api/lifecycle/ChangeEndPointSecurityPerTypeTestCase.java:160/211/262` (update variant)
- **endpoint-security-oauth-grant-types** — OAuth endpoint security for client-credentials & password grants
  - `api/lifecycle/AddEndPointSecurityPerTypeTestCase.java:367` (client creds), `:490` (password)
  - (dup) overlaps `api/lifecycle/ChangeEndPointSecurityPerTypeTestCase.java:330` (client creds), `:414` (password)
- **endpoint-security-import-with-definition** — API definition import carrying endpoint security
  - `api/lifecycle/AddEndPointSecurityPerTypeTestCase.java:445` (testAPIDefinitionImportWithEndpointSecurity)
- **update-endpoint-security-per-type** — Update existing endpoint security for Production / Sandbox / both
  - `api/lifecycle/ChangeEndPointSecurityPerTypeTestCase.java:160/211/262`
- **endpoint-security-password-complexity** — Endpoint security works with simple and complex passwords
  - `api/lifecycle/ChangeEndPointSecurityOfAPITestCase.java:103` (simple), `:153` (complex)
- **endpoint-certificate-management** — Upload/search/delete endpoint certs and invoke through them
  - `api/lifecycle/APIEndpointCertificateTestCase.java:165/178/202/219/235/292/329/394` (invoke w/o cert, upload, dup alias, expired, search, invoke, after remove, delete missing)
- **endpoint-certificate-usage-reporting** — Query certificate usage by alias with pagination
  - `api/lifecycle/APIEndpointCertificateUsageTestCase.java:131/142/160` (incorrect alias, by alias, pagination)

#### CORS / Tags / Tiers / Handlers
- **change-api-tags-and-filter** — Change API tags; filter-by-tag reflects change
  - `api/lifecycle/ChangeAPITagsTestCase.java:82` (before), `:107` (after change)
- **custom-handler-invocation** — Newly added custom handler is hit on invocation
  - `api/lifecycle/AddNewHandlerAndInvokeAPITestCase.java:111` (testAPIInvocationHitsTheNewHandler)
- **tiers-xml-edit-reflected-in-publisher** — Editing tiers.xml reflected in Permission/Manage pages
  - `api/lifecycle/EditTiersXMLAndVerifyInPublisherTestCase.java:105/124/139/161`
- **list-throttling-tiers** — Get all throttling/API tiers via Publisher REST
  - `publisher/APIM634GetAllTheThrottlingTiersFromThePublisherRestAPITestCase.java:67` (testGetAllTheThrottlingTiers)
- **list-subscription-throttling-policies-by-quota** — Get subscription throttling policies for event-count quota type
  - `publisher/APIMGetAllSubscriptionThrottlingPolicies.java:60` (testGetAllSubscriptionThrottlingPoliciesByQuotaType)
- **delete-tier-attached-to-api** — Update API after deleting an attached subscription tier
  - `publisher/DeleteTierAlreadyAttachedToAPITestCase.java:75` (testUpdateAPIAfterDeletingAttachedSubscriptionTier)
- **change-subscription-business-plan-forcefully** — Force-update a subscription's business plan; validations
  - `publisher/ChangeSubscriptionBusinessPlanForcefullyTestCase.java:154/174/201/221/232` (invalid subId, invalid plan, restricted subscriber, valid tiers, pending status)
- **linter-custom-rules** — Get linter custom rules via Publisher REST
  - `publisher/GetLinterCustomRulesThroughThePublisherRestAPITestCase.java:93` (testGetLinterCustomRulesThroughThePublisherRestAPI)

#### Throttling Enforcement (tier-driven)
- **api-tier-throttling-and-edit** — Throttle under Gold tier, recover after window, edit tier to Silver
  - `api/lifecycle/ChangeAPITierAndTestInvokingTestCase.java:90/154/174/198`
- **application-tier-throttling** — Throttle driven by Application tier combined with API tier
  - `api/lifecycle/ChangeApplicationTierAndTestInvokingTestCase.java:94/140/177`
- **resource-tier-throttling** — Throttle driven by per-resource tier (Unlimited/Silver/Gold)
  - `api/lifecycle/ChangeResourceTierAndTestInvokingTestCase.java:95/162/232`

#### API Context
- **dynamic-templated-context** — Templated API context invocation and search
  - `api/lifecycle/DynamicAPIContextTestCase.java:104` (templated context), `:140` (search by templated context)
- **edit-context-and-accessibility** — Change context; old context fails, new context works
  - `api/lifecycle/EditAPIContextAndCheckAccessibilityTestCase.java:98/157/175/192`

### API Security

#### Application-Level Security (OAuth2 / API Key / Basic / Internal Key)
- **oauth2-only-api-security** — OAuth2-protected API; basic-auth rejected
  - `api/lifecycle/APISecurityTestCase.java:482` (testCreateAndPublishAPIWithOAuth2)
  - `api/lifecycle/APISecurityTestCase.java:505` (testInvocationWithBasicAuthForOauthOnlyAPINegative)
- **api-key-security-and-restrictions** — API-Key invocation incl. IP and Referer restrictions, only-API-key, revoked keys
  - `api/lifecycle/APISecurityTestCase.java:516/558/825/913/994/1094` (api keys, basic-auth negative, IP cond, referer cond, key-only, revoked)
- **internal-key-on-created-api** — Internal Key token behavior on CREATED-state API
  - `api/lifecycle/APISecurityTestCase.java:372` (testCreateAndDeployRevisionWithInternalKeyTesting)
- **token-type-cross-use-negative** — Cross-use of JWT/API-key/internal-key tokens rejected
  - `api/lifecycle/APISecurityTestCase.java:1184/1197/1217/1233/1249/1257/1287` (apiKey-as-JWT, JWT-as-apiKey, internalKey-as-apiKey, etc.)
- **basic-auth-security** — Basic-auth API invocation incl. invalid creds and wrong token types negative
  - `api/lifecycle/APISecurityTestCase.java:1298/1327/1339/1350`
- **disable-security-tryout-resource** — Disabling resource security allows unauthenticated try-out (ELK analytics enabled)
  - `api/lifecycle/DisableSecurityAndTryOutRESTResourceWithElkAnalyticsEnabledTestCase.java:81` (testTurnOffSecurityAndInvokeGETResource)

#### Mutual SSL
- **mutual-ssl-only-and-mandatory** — Mutual-SSL-only and mandatory invocation (cert match/no-match)
  - `api/lifecycle/APISecurityTestCase.java:570/591/715` (mtls-only negative, mtls-only positive, mtls+oauth mandatory)
  - (dup) overlaps `api/lifecycle/APISecurityMutualSSLCertificateChainValidationTestCase.java:156/175`
- **mutual-ssl-with-oauth-optional-mandatory-mix** — App-security mandatory + mutual-ssl optional combinations
  - `api/lifecycle/APISecurityTestCase.java:623/648/665/691/759/783/804`
- **mutual-ssl-certificate-chain-validation** — Cert chain validation; unsupported cert rejected, mandatory mtls accepted
  - `api/lifecycle/APISecurityMutualSSLCertificateChainValidationTestCase.java:156` (negative), `:175` (mandatory positive)

#### JWT Audience Validation
- **jwt-audience-validation** — Invoke with/without audience validation; invalid vs valid audience
  - `api/lifecycle/AudienceValidationTestCase.java:232/269/313` (no validation, fail, pass)

### API Visibility & Access Control (Publisher/Store)

#### Visibility by Public
- **visibility-public-across-users-domains** — Public-visibility API visible to creator/admin/other users/anonymous, same & cross domain
  - `api/lifecycle/APIVisibilityByPublicTestCase.java:176/212/222/233/246/257/268/279/289/300/310/320`

#### Visibility by Role
- **visibility-by-role** — Role-based visibility for creator/admin/subscriber across domains & anonymous
  - `api/lifecycle/APIVisibilityByRoleTestCase.java:231/286/302/319/337/355/373/391/412/430/447/466/480`
- **tag-visibility-by-role** — Tag visibility differs for anonymous vs authorized user (public+role APIs)
  - `api/lifecycle/APITagVisibilityByRoleTestCase.java:112/148/162`

#### Visibility by Domain
- **visibility-by-domain** — Domain-scoped visibility for creator/admin/other users across same & other domains
  - `api/lifecycle/APIVisibilityByDomainTestCase.java:176/205/215/227/238/250/260/272/283/295/306/318`

#### Direct-URL / Anonymous Access
- **visibility-via-direct-url** — Restricted API reachable via direct URL only with login / proper role
  - `api/lifecycle/APIVisibilityWithDirectURLTestCase.java:107/150/159` (anonymous, login, without restricted role)

#### Subscriber/User Overview
- **users-and-docs-in-overview** — API overview shows correct user count and docs-tab info
  - `api/lifecycle/UsersAndDocsInAPIOverviewTestCase.java:91/129`

### API Versioning

#### Re-Subscription & Deprecation on New Copy (old TestNG flows)
- **new-copy-without-resubscription** — Old subscription works on new copy when re-subscription not required
  - `api/lifecycle/AccessibilityOfOldAPIAndCopyAPIWithOutReSubscriptionTestCase.java:101/120/143`
- **new-copy-requires-resubscription** — Old subscription invalid on new copy until re-subscribed
  - `api/lifecycle/AccessibilityOfOldAPIAndCopyAPIWithReSubscriptionTestCase.java:99/119/146/172/186/207`
- **publish-new-copy-deprecate-old** — Publish new copy with "deprecate old versions" option
  - `version/APIM366PublishNewCopyGivenDeprecateOldVersionTestCase.java:114` (testPublishNewCopyGivenDeprecateOldVersion)
  - (dup) `version/APIM372PublishNewCopyGivenDeprecateOldVersionTestCase.java:128` (same flow, exercises GET/POST/PUT/DELETE)
- **publish-new-copy-require-resubscription** — Publish new copy with "require re-subscription" option
  - `version/APIM370PublishNewCopyGivenRequireReSubscriptionTestCase.java:113`
  - (dup) `version/APIM373PublishNewCopyGivenRequireReSubscriptionTestCase.java:136` (same flow, multi-verb variant)
- **publish-new-copy-deprecate-and-require-resubscription** — Both deprecate-old AND require-re-subscription
  - `version/APIM371PublishNewCopyGivenDeprecateOldVersionAndRequireReSubscriptionTestCase.java:117`
  - (dup) `version/APIM374PublishNewCopyGivenDeprecateOldVersionAndRequireReSubscriptionTestCase.java:131` (multi-verb variant)

#### Default Version
- **default-version-api** — Create API without/with default-version; switch default back; context==version case
  - `version/DefaultVersionAPITestCase.java:101/171/244/293`
- **default-version-with-scopes** — Default-version API behaves correctly when scopes are attached
  - `version/DefaultVersionWithScopesTestCase.java:97` (testDefaultVersionAPIWithScopes)

### API Definitions & Import/Export

#### Swagger / Definition Import
- **import-swagger-same-context-conflict** — Importing two swaggers with same context handled
  - `publisher/APIM18CreateAnAPIThroughThePublisherRestAPITestCase.java:194` (testImportSwaggerAndCreateAPIWithSameContext)
- **import-archive-with-remote-references** — Create APIs from archives whose master swagger has remote $refs
  - `publisher/APIM18CreateAnAPIThroughThePublisherRestAPITestCase.java:220` (valid)
  - `publisher/APIM18CreateAnAPIThroughThePublisherRestAPITestCase.java:315` (incorrect/random master swagger filename)

#### SDK Generation
- **sdk-generation** — Generate client SDK for an API
  - `api/sdk/SDKGenerationTestCase.java:85` (testSDKGeneration)
- **sdk-generation-private-apis** — Generate SDK for private APIs
  - `api/sdk/SDKGenerationTestCase.java:180` (testSDKGenerationForPrivateAPIs)

### API Documentation

#### Documentation by Type & Source
- **doc-howto-inline-and-url** — Add HowTo docs (inline + URL)
  - `publisher/APIM611AddDocumentationToAnAPIWithDocTypeHowToThroughPublisherRestAPITestCase.java:91/135`
- **doc-sample-sdk-other-file** — Add HowTo/Sample-SDK/Other docs sourced from file
  - `publisher/APIM614AddDocumentationToAnAPIWithDocTypeSampleAndSDKThroughPublisherRestAPITestCase.java:88/126/152/178`
- **doc-sample-sdk-inline-url** — Add Sample/SDK docs (inline + URL)
  - `publisher/APIM620AddDocumentationToAnAPIWithDocTypeSampleAndSDKThroughPublisherRestAPITestCase.java:89/134`
- **doc-public-forum** — Add Public-Forum doc (inline)
  - `publisher/APIM623AddDocumentationToAnAPIWithDocTypePublicForumThroughPublisherRestAPITestCase.java:96`
- **doc-support-forum** — Add Support-Forum doc (inline)
  - `publisher/APIM625AddDocumentationToAnAPIWithDocTypeSupportForumThroughPublisherRestAPITestCase.java:94`
- **doc-other-inline-url-and-remove** — Add Other docs (inline + URL) and remove doc
  - `publisher/APIM627AddDocumentationToAnAPIWithDocTypeOtherThroughPublisherRestAPITestCase.java:94/137/158`

### DevPortal / Store & Search

#### Role / User Validation
- **validate-user-role-existing-nonexisting** — Validate role of existing vs non-existing user via Publisher REST
  - `publisher/APIM638ValidateTheRoleOfAnExistingUserThroughThePublisherRestAPITestCase.java:89/103`

### API Products

#### API Product Creation & Invocation
- **create-and-invoke-api-product** — Create an API Product and invoke it
  - `apiproduct/APIProductCreationTestCase.java:138` (testCreateAndInvokeApiProduct)
- **product-new-version-creation** — Create a new version of a product (plain & default-version)
  - `apiproduct/APIProductCreationTestCase.java:220` (testAPIProductNewVersionCreation)
  - `apiproduct/APIProductCreationTestCase.java:261` (testAPIProductNewVersionCreationWithDefaultVersion)
- **product-malformed-context-rejected** — Reject product creation with malformed context
  - `apiproduct/APIProductCreationTestCase.java:302` (testCreateApiProductWithMalformedContext)
- **product-with-visibility-restricted-api** — Product depending on a visibility-restricted API
  - `apiproduct/APIProductCreationTestCase.java:332` (testCreateAndInvokeApiProductWithVisibilityRestrictedApi)
- **product-with-api-category** — Product creation/deployment with an API category attached
  - `apiproduct/APIProductCreationTestCase.java:396` (testCreateAndInvokeApiProductWithAPICategoryAdded)
- **product-with-scopes** — Product invocation when underlying API has scopes
  - `apiproduct/APIProductCreationTestCase.java:455` (testCreateAndInvokeApiProductWithScopes)
- **product-with-operation-policies** — Product with request/response operation policies on its APIs
  - `apiproduct/APIProductCreationTestCase.java:523` (request), `:616` (response)
- **product-with-advertise-only-api** — Create product referencing an advertise-only API
  - `apiproduct/APIProductCreationTestCase.java:710` (testCreateApiProductWithAdvertiseOnlyApi)
- **product-with-mutual-ssl** — Deploy product with Mutual SSL enabled
  - `apiproduct/APIProductCreationTestCase.java:771` (testCreateAndDeployApiProductWithMutualSSLEnabled)
- **product-swagger-definition-reference** — Product swagger definition references verified
  - `apiproduct/APIProductCreationTestCase.java:802` (testAPIProductSwaggerDefinition)
- **product-reflects-underlying-api-update** — Product reflects changes when underlying API is updated
  - `apiproduct/APIProductCreationTestCase.java:855` (testUpdateUnderlyingAPIofAPIProduct)

#### API Product Lifecycle
- **product-create-publish-block** — Create, publish, then block an API Product (invocation blocked)
  - `apiproduct/lifecycle/APIProductLifecycleTest.java:117/151/183` (create, publish, block)
- **product-delete-with-subscription-and-retired** — Delete deprecated product w/ subscription and retired product
  - `apiproduct/lifecycle/APIProductLifecycleTest.java:218` (deprecated w/ subscription)
  - `apiproduct/lifecycle/APIProductLifecycleTest.java:244` (retired)

#### API Product Revisions
- **product-revision-create-deploy-undeploy** — Create, list, deploy and undeploy a product revision
  - `api/revision/APIProductRevisionTestCase.java:70/107/126/141` (add, get, deploy, undeploy)
- **product-revision-restore-and-delete** — Restore product from a revision (incl. deleted-resources case) and delete revision
  - `api/revision/APIProductRevisionTestCase.java:157` (restore w/ deleted resources), `:233` (restore), `:245` (delete)

### API Revisions

#### Revision CRUD & Validation
- **revision-create** — Create an API revision (with/without description; invalid API negative)
  - `api/revision/APIRevisionTestCase.java:89` (create), `:137` (invalid API), `:151` (without description)
- **revision-list-before-after-deploy** — List revisions before and after deployment
  - `api/revision/APIRevisionTestCase.java:164` (get), `:182` (get deployed)
- **revision-delete-and-validation** — Delete revision (with deployments; invalid API/revision UUID negatives)
  - `api/revision/APIRevisionTestCase.java:541` (having deployments), `:565` (delete), `:586/:596` (invalid UUIDs)

#### Revision Deployment to Gateway
- **deploy-revision-to-gateway** — Deploy a revision to gateway environments; verify ack counts
  - `api/revision/APIRevisionTestCase.java:200` (deploy), `:215` (deployment ack counts)
- **deploy-revision-negative-validation** — Reject deploy with invalid API/revision/deployment-info/vhost
  - `api/revision/APIRevisionTestCase.java:375/391/407/422`
- **undeploy-revision-and-validation** — Undeploy a revision; invalid API/revision/deployment-info negatives
  - `api/revision/APIRevisionTestCase.java:439/454/470/487`
- **restore-revision-and-validation** — Restore API from a revision; invalid UUID negatives
  - `api/revision/APIRevisionTestCase.java:503` (restore), `:522/:531` (invalid UUIDs)

#### Revision Invocation across Lifecycle States
- **invoke-revision-per-lifecycle-state** — Invoke a deployed revision in CREATED/PUBLISHED/BLOCKED/DEPRECATED/RETIRED states
  - `api/revision/APIRevisionTestCase.java:606/644/679/699/719`
- **deleted-api-no-trace-in-admin** — Deleted API leaves no trace in admin console
  - `api/revision/APIRevisionTestCase.java:739` (testIfTracesOfDeletedApisVisible)

---

### Duplicate coverage (within this section)

- **create-api-via-publisher-rest** — covered by `publisher/APIM18CreateAnAPIThroughThePublisherRestAPITestCase.java`, `publisher/APIM519CreateAnAPIThroughTheRestAPIWithoutLoggingInTestCase.java`, and `publisher/APIM574ChangeTheStatusOfAnAPIToPrototypedThroughThePublisherRestAPITestCase.java` (all create an API via Publisher REST as a precondition; APIM519 adds the no-login negative).
- **malformed/invalid context rejection** — covered by `publisher/APIM18CreateAnAPIThroughThePublisherRestAPITestCase.java` (malformed context), `publisher/APIMANAGER5834APICreationWithInvalidInputsTestCase.java` (invalid context + context-matches-previous), and `apiproduct/APIProductCreationTestCase.java` (product malformed context) — same context-validation behavior across API and Product creation.
- **publish-new-copy / deprecate-old / require-resubscription flows** — the `version/APIM366` & `APIM372` pair (deprecate-old), `APIM370` & `APIM373` pair (require-resubscription), and `APIM371` & `APIM374` pair (both) each test the same product behavior; within each pair the 37x variant just repeats the flow across GET/POST/PUT/DELETE verbs. Additionally `api/lifecycle/AccessibilityOfDeprecatedOldAPIAndPublishedCopyAPITestCase.java`, `AccessibilityOfOldAPIAndCopyAPIWithReSubscriptionTestCase.java`, and `AccessibilityOfOldAPIAndCopyAPIWithOutReSubscriptionTestCase.java` cover the same deprecate-old / re-subscription semantics from the lifecycle package.
- **endpoint-security-per-type (Basic + OAuth grants)** — covered by `api/lifecycle/AddEndPointSecurityPerTypeTestCase.java` (add) and `api/lifecycle/ChangeEndPointSecurityPerTypeTestCase.java` (update); both exercise Production/Sandbox/both and client-credentials/password grant types.
- **mutual-SSL invocation** — covered by `api/lifecycle/APISecurityTestCase.java` and `api/lifecycle/APISecurityMutualSSLCertificateChainValidationTestCase.java` (overlapping mtls-only negative + mandatory-mtls invocation assertions).
- **publish + store-visibility transition** — `api/lifecycle/APIPublishingAndVisibilityInStoreTestCase.java` overlaps with the many `APIVisibilityBy*` classes on the "API hidden before publish / visible after publish" behavior, though the visibility classes focus on role/domain/public scoping.
- **default-version** — `version/DefaultVersionAPITestCase.java` and `version/DefaultVersionWithScopesTestCase.java` both assert default-version resolution; the latter adds the scopes dimension. `apiproduct/APIProductCreationTestCase.java:261` repeats default-version behavior for products.

## Legacy TestNG Coverage Map — Part B

Scope: `application/`, `token/`, `jwt/`, `login/`, `crossSubscription/`, `comments/`
Root: `all-in-one-apim/modules/integration/tests-integration/tests-backend/src/test/java/org/wso2/am/integration/tests/`
(All class paths below are relative to that root.)

---

### Applications & Subscriptions

#### Application lifecycle (CRUD / key generation)
- **application-crud-by-id** — Get/update/remove application by ID, generate keys by ID, add & list subscriptions by app ID, cleanup registration
  - `application/ApplicationTestCase.java:166` (testGetApplicationById)
  - `application/ApplicationTestCase.java:173` (testApplicationKeyGenerationById)
  - `application/ApplicationTestCase.java:182` (testUpdateApplicationById)
  - `application/ApplicationTestCase.java:212` (testAddSubscriptionApplicationById)
  - `application/ApplicationTestCase.java:222` (testGetSubscriptionForApplicationById)
  - `application/ApplicationTestCase.java:238` (testCleanupApplicationRegistrationById)
  - `application/ApplicationTestCase.java:247` (testRemoveApplicationById)
- **application-key-mapping** — Map existing OAuth keys to an application (positive + negative), fetch key details by key-mapping ID
  - `application/ApplicationTestCase.java:253` (testMapApplicationKeysNegative)
  - `application/ApplicationTestCase.java:277` (testMapApplicationKeys)
  - `application/ApplicationTestCase.java:306` (testFetchKeyDetailsByKeyMappingID)
- **application-attributes** — Add/verify custom application attributes for JWT and OAuth applications
  - `application/ApplicationAttributesTestCase.java:94` (testVerifyApplicationAttributesInJWTApplication)
  - `application/ApplicationAttributesTestCase.java:134` (testVerifyApplicationAttributesInOauthApplication)

#### Application credentials / secrets
- **consumer-secret-regenerate** — Regenerate consumer secret after application key generation
  - `application/ApplicationConsumerSecretRegenerateTestCase.java:69` (testApplicationCreation)
  - `application/ApplicationConsumerSecretRegenerateTestCase.java:88` (testRegenerateConsumerSecret)
- **multiple-consumer-secrets (key-mapping scoped)** — Generate/list/revoke multiple additional consumer secrets per key mapping; count/list consistency; additionalProperties (description) returned; minimal payload; SANDBOX key-mapping secret lifecycle
  - `application/ApplicationTestCase.java:327` (testGenerateConsumerSecretForKeyMappingId)
  - `application/ApplicationTestCase.java:356` (testGetConsumerSecretsForKeyMappingId)
  - `application/ApplicationTestCase.java:375` (testRevokeConsumerSecretForKeyMappingId)
  - `application/ApplicationTestCase.java:398` (testGenerateMultipleSecretsForSameKeyMapping)
  - `application/ApplicationTestCase.java:424` (testSecretListCountMatchesGenerated)
  - `application/ApplicationTestCase.java:445` (testGenerateSecretAdditionalPropertiesReturnedInList)
  - `application/ApplicationTestCase.java:486` (testGenerateSecretWithMinimalPayload)
  - `application/ApplicationTestCase.java:509` (testGenerateSecretForSandboxKeyMapping)
- **multiple-secrets-token-behavior** — Token can be obtained with a generated additional secret; token fails after secret revoked; two secrets work concurrently; tokens from different secrets carry same sub/application
  - `application/MultipleClientSecretsTokenTestCase.java:102` (testTokenGenerationWithGeneratedSecret)
  - `application/MultipleClientSecretsTokenTestCase.java:140` (testTokenGenerationFailsAfterRevoke)
  - `application/MultipleClientSecretsTokenTestCase.java:161` (testBothSecretsWorkConcurrently)
  - `application/MultipleClientSecretsTokenTestCase.java:217` (testTokensFromDifferentSecretsHaveSameApplication)

#### Application callback URL
- **callback-url-create-update** — Create application then update with callback URL; accept valid IP-form callback URLs
  - `application/ApplicationCallbackURLTestCase.java:62` (testApplicationCreation)
  - `application/ApplicationCallbackURLTestCase.java:89` (testApplicationUpdate)
  - `application/ApplicationCallbackURLTestCase.java:114` (testApplicationUpdateIpAsCallBackURL)
  - `application/ApplicationCallbackURLTestCase.java:140` (testApplicationUpdateValidIpAsCallBackURL)
- **callback-url-overwrite** — Callback URL is overwritten on key update (with SAML2/NTLM grant types)
  - `application/CAPIMGT12CallBackURLOverwriteTestCase.java:81` (callBackUrlUpdateTestCase)

#### Application subscription blocking
- **block-unblock-subscription** — Block/unblock works for application & owner names containing hyphens
  - `application/ApplicationBlockSubscriptionTestCase.java:85` (testBlockUnblockSubscription)

#### Subscription validation
- **disable-subscription-validation** — API can be invoked without a subscription when subscription validation is disabled; invocation with external token; re-enabling restores enforcement
  - `application/SubscriptionValidationDisableTestCase.java:72` (testSubscriptionValidationDisablingForAPI)
  - `application/SubscriptionValidationDisableTestCase.java:88` (testAPIInvocationWithoutSubscription)
  - `application/SubscriptionValidationDisableTestCase.java:115` (testAPIInvocationWithExternalToken)
  - `application/SubscriptionValidationDisableTestCase.java:147` (testAPIInvocationAfterEnablingSubscriptionValidation)

#### Application group sharing
- **group-shared-applications** — Shared (group) application lifecycle: removal cascade across users, edit by owner and by group member; API-key revocation by a shared user
  - `application/groupSharing/ApplicationSharingTestCase.java:92` (testUserTwoApplicationRemoval)
  - `application/groupSharing/ApplicationSharingTestCase.java:105` (testEditApplicationByApplicationOwner)
  - `application/groupSharing/ApplicationSharingTestCase.java:118` (testEditApplicationByUserInApplicationGroup)
  - `application/groupSharing/ApplicationSharingTestCase.java:133` (testAPIKeyRevocationBySharedUser)
  - (helper/config: `application/groupSharing/ApplicationSharingConfig.java` — no @Test)

---

### Authentication, Tokens & Key Management

#### OAuth Grant Types
- **authorization-code-grant** — Auth-code token generation; auth request without callback URL; app display name shown on consent page
  - `application/GrantTypeTokenGenerateTestCase.java:214` (testAuthCode)
  - `application/GrantTypeTokenGenerateTestCase.java:378` (testAuthRequestWithoutCallbackURL)
  - `application/GrantTypeTokenGenerateTestCase.java:393` (testAuthCodeAppDisplayName)
- **implicit-grant** — Implicit token generation
  - `application/GrantTypeTokenGenerateTestCase.java:287` (testImplicit)
- **grant-type-app-setup / negative** — App creation for grant tests; app creation without callback URL; token generation fails with corrupted client credentials
  - `application/GrantTypeTokenGenerateTestCase.java:173` (testApplicationCreation)
  - `application/GrantTypeTokenGenerateTestCase.java:190` (testTokenGenerationWithCorruptedClientCredentials)
  - `application/GrantTypeTokenGenerateTestCase.java:346` (testApplicationCreationWithoutCallBackURL)
- **password-grant** — Used as the base token-acquisition path across token/JWT suites (e.g. revocation setups in `jwt/MicroGWJWTRevocationTestCase.java:167`)
- **client-credentials-grant** — Backend JWT issued under client-credentials grant
  - `jwt/JWTTestCase.java:424` (testBackendJWTWithClientCredentialsGrant)
- **jwt-bearer-grant (urn:ietf...jwt-bearer)** — Exchange a 3rd-party/IDP-issued JWT for an APIM token: valid registered IDP, non-registered IDP (negative), expired JWT, tampered JWT, JWT signed with different cert, scope mapping before/after adding IDP roles
  - `jwt/JWTGrantTestCase.java:131` (testGenerateTokenWithValidRegisteredIDP)
  - `jwt/JWTGrantTestCase.java:141` (testGenerateTokenForNonRegisteredIDP)
  - `jwt/JWTGrantTestCase.java:151` (testGenerateTokenWithExpiredJWT)
  - `jwt/JWTGrantTestCase.java:160` (testGenerateTokenWithTamperedJWT)
  - `jwt/JWTGrantTestCase.java:170` (testGenerateTokenWithJWTSignedWithDifferentCert)
  - `jwt/JWTGrantTestCase.java:182` (testGenerateTokenWithScopesUsingJWTBeforeAddingIdpRoles)
  - `jwt/JWTGrantTestCase.java:205` (testGenerateTokenWithScopesUsingJWTWithIdpRoles)

#### Token API (issue / refresh / revoke)
- **token-api-issue** — Token API issues access token and invoke API; OAuth token variant; infinite (non-expiring) token
  - `token/TokenAPITestCase.java:113` (testTokenAPITestCase)
  - `token/TokenAPITestCase.java:251` (testOauthTokenAPITestCase)
  - `token/TokenAPITestCase.java:306` (testInfiniteTokenAPITestCase)
- **refresh-token** — Refresh token grant produces a new working access token
  - `token/RefreshTokenTestCase.java:85` (testRefreshTokenAPITestCase)
  - (dup) also covered by `token/TokenAPITestCase.java:203` (testRefreshTokenAPITestCase)
- **invalid-token-rejection** — API access with an invalid token is rejected
  - `token/InvalidTokenTestCase.java:86` (testAPIAccessWithInvalidToken)

#### Sandbox vs Production tokens & endpoints
- **sandbox-token-endpoint-matrix (APIM34)** — Sandbox-token invocation behavior when API has both / sandbox-only / production-only endpoints; production-token vs sandbox-only endpoint; both tokens with both endpoints
  - `token/APIM34InvokeAPIWithSandboxTokenTestCase.java:124` (testInvokeAPIFromSandboxTokenWhenProvideBothEndPoints)
  - `token/APIM34InvokeAPIWithSandboxTokenTestCase.java:195` (testInvokeAPIFromSandboxTokenWhenProvideOnlySandboxEndPoint)
  - `token/APIM34InvokeAPIWithSandboxTokenTestCase.java:266` (testInvokeAPIFromSandboxTokenWhenProvideOnlyProductionEndPoint)
  - `token/APIM34InvokeAPIWithSandboxTokenTestCase.java:338` (testInvokeAPIFromProductionTokenWhenProvideOnlySandboxEndPoint)
  - `token/APIM34InvokeAPIWithSandboxTokenTestCase.java:410` (testInvokeAPIFromSandboxAndProductionTokenWhenProvideBothEndPoints)
- **various-endpoints-and-tokens (default env)** — Invoke API with both endpoints+tokens, production-endpoint + both tokens, sandbox-endpoint + both tokens
  - `token/InvokeAPIWithVariousEndpointsAndTokensTestCase.java:118` (testInvokeAPIWithBothEndpointsAndTokens)
  - `token/InvokeAPIWithVariousEndpointsAndTokensTestCase.java:204` (testInvokeAPIWithProductionEndpointAndBothTokens)
  - `token/InvokeAPIWithVariousEndpointsAndTokensTestCase.java:291` (testInvokeAPIWithSandboxEndpointAndBothTokens)
- **various-endpoints-and-tokens (sandbox env)** — Same matrix variants in sandbox-only environment
  - `token/InvokeAPIWithVariousEndpointsAndTokensInSandboxEnvTestCase.java:120` (testInvokeAPIWithBothEndpointsAndTokens)
  - `token/InvokeAPIWithVariousEndpointsAndTokensInSandboxEnvTestCase.java:206` (testInvokeAPIWithSandboxEndpointAndBothTokens)

#### OpenID Connect
- **openid-scope-token & userinfo** — Issue access token with openid scope; call /userinfo with opaque OIDC token and with JWT OIDC token
  - `token/OpenIDTokenAPITestCase.java:82` (testGenerateAccessTokenWithOpenIdScope)
  - `token/OpenIDTokenAPITestCase.java:97` (testCallUserInfoApiWithOpenIdAccessToken)
  - `token/OpenIDTokenAPITestCase.java:107` (testCallUserInfoApiWithOpenIdJWTAccessToken)

#### Token persistence (config-driven suites)
- **token-persistence-mode** — Server-config suites enabling token-persistence deployment.toml and re-running token generation flows; opaque, JWT, external-IDP-JWT, and URL-safe-JWT variants (config harness only; assertions live in the base lifecycle flow they wrap)
  - `token/TokenPersistenceTestSuite.java:31` (config: opaque token persistence)
  - `token/TokenPersistenceJWTTestSuite.java:31` (config: JWT token persistence)
  - `token/TokenPersistenceExternalIDPJWTTestSuite.java:33` (config: external-IDP JWT persistence)
  - `token/TokenPersistenceURLSafeJWTTestSuite.java:31` (config: URL-safe JWT persistence)

#### PKCE
- **pkce-key-generation** — Key generation/token with PKCE (S256), plain-text challenge, and plain-text bypass-secret variants
  - `application/PkceEnabledApplicationTestCase.java:125` (testApplicationCreationKeyGenerationWithPkce)
  - `application/PkceEnabledApplicationTestCase.java:146` (testApplicationCreationKeyGenerationWithPkcePlainText)
  - `application/PkceEnabledApplicationTestCase.java:169` (testApplicationCreationKeyGenerationWithPkcePlainTextByPassSecret)

#### Backend JWT generation (claims forwarded to backend)
- **backend-jwt-for-app-types** — Backend JWT + claims for JWT-token-type app, for API-key app (opaque & non-opaque), under client-credentials grant, under auth-code grant
  - `jwt/JWTTestCase.java:217` (testEnableJWTAndClaimsForJWTApp)
  - `jwt/JWTTestCase.java:339` (testEnableJWTAndClaimsForAPIKeyApp)
  - `jwt/JWTTestCase.java:398` (testOpaqueAPIKeyForAPIKeyApp)
  - `jwt/JWTTestCase.java:424` (testBackendJWTWithClientCredentialsGrant)
  - `jwt/JWTTestCase.java:468` (testBackendJWTWithAuthCodeGrant)
- **backend-jwt-federated-user** — Backend JWT generation & claim verification for a federated user on a JWT-type app
  - `jwt/FederatedUserJWTTestCase.java:233` (testEnableJWTAndClaimsForJWTApp)
  - `jwt/FederatedUserJWTTestCase.java:298` (testVerifyJWTClaimsInFederatedUserJWTAPP)
- **backend-jwt-url-safe** — Backend JWT generation for JWT app with URL-safe encoding
  - `jwt/urlsafe/URLSafeJWTTestCase.java:112` (testEnableJWTAndClaimsForJWTApp)
  - (suite harness: `jwt/urlsafe/UrlSafeJWTTestSuite.java` — config only)
- **backend-jwt-decoding** — Decode/verify backend JWT for a custom application; opaque-API-key variant
  - `jwt/jwtdecoding/JWTDecodingTestCase.java:130` (testJWTDecodingforCustomApplication)
  - `jwt/jwtdecoding/JWTDecodingTestCase.java:181` (testJWTDecodingforCustomApplicationWithOpaqueKey)
- (helpers, no @Test: `jwt/BackendJWTUtil.java`, `jwt/JWTGenerator.java`, `jwt/idp/JWTGeneratorUtil.java`)

#### API Key authentication
- **api-key-only-secured-api** — Invoke API secured only with API key (opaque & non-opaque) when backend JWT enabled
  - `jwt/JWTTestCase.java:277` (testAPIKeyOnlySecuredAPIInvocation)
  - `jwt/JWTTestCase.java:313` (testOpaqueAPIKeyOnlySecuredAPIInvocation)
- **api-key-revocation (shared app)** — Shared user can revoke an API key generated by another user
  - `application/groupSharing/ApplicationSharingTestCase.java:133` (testAPIKeyRevocationBySharedUser)

#### JWT / Token revocation
- **jwt-token-revocation** — Direct JWT (jti) revocation via revoke endpoint
  - `jwt/JWTRevocationTestCase.java:99` (testJWTTokenRevocation)
- **consumer-app-based-jwt-revocation** — Revoking the consumer application revokes its issued JWTs
  - `jwt/ConsumerAppBasedJWTRevocation.java:72` (testConsumerAppBasedJWTRevocation)
- **micro-gw-jwt-revocation (ETCD + JMS)** — Revoke JWT then verify revoked jti propagates to ETCD key store and to JMS revocation topic
  - `jwt/MicroGWJWTRevocationTestCase.java:167` (revokeRequestTestCase)
  - `jwt/MicroGWJWTRevocationTestCase.java:201` (checkETCDForRevokedJTITestCase)
  - `jwt/MicroGWJWTRevocationTestCase.java:238` (checkJMSTopicForRevokedJTITestCase)
  - (suite harness: `jwt/JWTTestSuite.java` — config only)

#### External IDP / token exchange
- **external-idp-jwt-invocation** — Invoke API using a JWT issued by an external IDP; negatives (invalid consumer key, unknown cert); second valid-invocation variant
  - `jwt/idp/ExternalIDPJWTTestCase.java:180` (testInvokeExternalIDPGeneratedJWT)
  - `jwt/idp/ExternalIDPJWTTestCase.java:239` (testInvokeExternalIDPGeneratedJWTNegative1)
  - `jwt/idp/ExternalIDPJWTTestCase.java:261` (testInvokeExternalIDPGeneratedJWTNegative2)
  - `jwt/idp/ExternalIDPJWTTestCase.java:283` (testInvokeExternalIDPGeneratedJWT1)
- **token-exchange-grant (KM display token endpoints)** — Token-exchange grant config on key manager: display token-endpoint behavior, create KM for exchange type, exchange + direct grant, invoke from exchanged token, update KM to exchange-only / direct, remove exchange grant and verify invocation
  - `jwt/idp/ExternalIDPJWTTestCase.java:341` (testIDPDisplaytokenEndpoints)
  - `jwt/idp/ExternalIDPJWTTestCase.java:395` (testCreateKeyManagerForExchangeType)
  - `jwt/idp/ExternalIDPJWTTestCase.java:451` (testExchangeAndDirectGrantType)
  - `jwt/idp/ExternalIDPJWTTestCase.java:491` (testInvokeFromExchangeToken)
  - `jwt/idp/ExternalIDPJWTTestCase.java:529` (testUpdateKMToExchangeOnly)
  - `jwt/idp/ExternalIDPJWTTestCase.java:579` (testRemoveExchangeGrantAndCheckInvocation)
  - `jwt/idp/ExternalIDPJWTTestCase.java:620` (testUpdateKMToDirect)
- **key-generation-disabled-negative** — Generating consumer keys fails (901405/400) when OAuth-app creation disabled for that key manager
  - `jwt/idp/ExternalIDPJWTTestCase.java:773` (generateKeysNegative)
  - (suite harness: `jwt/idp/ExternalIDPJWTTestSuite.java` — config only)

---

### DevPortal / Store & Search

#### Login & session
- **login-validation** — Invalid user login to Publisher rejected; subscriber-role user blocked from Publisher; valid login to Store
  - `login/LoginValidationTestCase.java:89` (testInvalidLoginAsPublisherTestCase)
  - `login/LoginValidationTestCase.java:106` (testInvalidLoginAsSubscriberTestCase)
  - `login/LoginValidationTestCase.java:138` (testLoginToStoreTestCase)
- **email-username-login** — Login with email-format username (super tenant & tenant user)
  - `login/EmailUserNameLoginTestCase.java:104` (email username login — super tenant)
  - `login/EmailUserNameLoginTestCase.java:143` (login with email username for tenant user)
- **password-change** — DevPortal/subscriber user can change own password
  - `login/PasswordChangeTestCase.java:73` (testChangeSubscriberUserPassword)

#### API comments (DevPortal side)
- **devportal-comment-crud-and-replies** — Deploy API; add root comments & nested replies; pagination of replies and root comments; total counts; get-all comments; edit; delete (existing + non-existing)
  - `comments/DevPortalCommentTest.java:111` (testDevPortalDeployAPITest)
  - `comments/DevPortalCommentTest.java:142` (testDevPortalAddRootCommentsToAPIByAdminTest)
  - `comments/DevPortalCommentTest.java:162` (testAddRepliesToRootCommentByAdminTest)
  - `comments/DevPortalCommentTest.java:212` (testDevPortalPaginatedCommentListTest)
  - `comments/DevPortalCommentTest.java:230` (testDevPortalGetAllCommentsTest)
  - `comments/DevPortalCommentTest.java:248` (testDevPortalPaginatedRootCommentsTest)
  - `comments/DevPortalCommentTest.java:261` (testDevPortalTotalCommentsOfPaginatedRootCommentsTest)
  - `comments/DevPortalCommentTest.java:278` (testDevPortalGetRepliesOfCommentTest)
  - `comments/DevPortalCommentTest.java:297` (testDevPortalPaginationOfRepliesOfCommentTest)
  - `comments/DevPortalCommentTest.java:314` (testDevPortalTotalRepliesOfPaginationOfRepliesOfCommentTest)
  - `comments/DevPortalCommentTest.java:420` (testDevPortalEditCommentTest)
  - `comments/DevPortalCommentTest.java:459` (testDevPortalDeleteCommentTest)
  - `comments/DevPortalCommentTest.java:473` (testDevPortalDeleteNotExistingCommentTest)
  - `comments/DevPortalCommentTest.java:480` (testDevPortalAddNewRootCommentWithReplyTest)
- **devportal-comment-cross-portal-visibility** — Comments/replies made in DevPortal are visible & paginated correctly from Publisher
  - `comments/DevPortalCommentTest.java:332` (testVerifyPublisherGetAllCommentsTest)
  - `comments/DevPortalCommentTest.java:351` (testVerifyPublisherPaginatedCommentListTest)
  - `comments/DevPortalCommentTest.java:370` (testVerifyPublisherGetRepliesOfCommentTest)
  - `comments/DevPortalCommentTest.java:390` (testVerifyPublisherPaginationOfRepliesOfCommentTest)
  - `comments/DevPortalCommentTest.java:588` (testPublisherNonAdminUserAddReplyToCommentFromDevPortalTest)
  - `comments/DevPortalCommentTest.java:606` (testPublisherAdminUserAddReplyToCommentFromDevPortalTest)
  - `comments/DevPortalCommentTest.java:648` (testVerifyPublisherAdminDeleteCommentTest)
- **devportal-comment-authorization** — Edit/delete permission rules by owner / non-owner non-admin / non-owner admin; add comment & replies by non-admin user
  - `comments/DevPortalCommentTest.java:508` (testDevPortalEditCommentByNonOwnerNonAdminUserTest)
  - `comments/DevPortalCommentTest.java:520` (testDevPortalEditCommentByNonOwnerAdminUserTest)
  - `comments/DevPortalCommentTest.java:537` (testDevPortalAddCommentByNonAdminUserTest)
  - `comments/DevPortalCommentTest.java:554` (testDevPortalAddReplyToNonAdminUserCommentByAdminUserTest)
  - `comments/DevPortalCommentTest.java:571` (testDevPortalAddReplyToAdminUserCommentByNonAdminUserTest)
  - `comments/DevPortalCommentTest.java:624` (testDevPortalDeleteCommentByNonOwnerNonAdminUserTest)
  - `comments/DevPortalCommentTest.java:633` (testDevPortalDeleteCommentByNonOwnerAdminUserTest)

#### API comments (Publisher side)
- **publisher-comment-crud-and-replies** — Same CRUD/replies/pagination/totals/edit/delete matrix executed from Publisher
  - `comments/PublisherCommentTest.java:113` (testPublisherDeployAPITest)
  - `comments/PublisherCommentTest.java:144` (testPublisherAddRootCommentsToAPIByAdminTest)
  - `comments/PublisherCommentTest.java:164` (testAddRepliesToRootCommentByAdminTest)
  - `comments/PublisherCommentTest.java:211` (testPublisherPaginatedCommentListTest)
  - `comments/PublisherCommentTest.java:228` (testPublisherGetAllCommentsTest)
  - `comments/PublisherCommentTest.java:245` (testPublisherPaginatedRootCommentsTest)
  - `comments/PublisherCommentTest.java:257` (testPublisherTotalCommentsOfPaginatedRootCommentsTest)
  - `comments/PublisherCommentTest.java:273` (testPublisherGetRepliesOfCommentTest)
  - `comments/PublisherCommentTest.java:291` (testPublisherPaginationOfRepliesOfCommentTest)
  - `comments/PublisherCommentTest.java:307` (testPublisherTotalRepliesOfPaginationOfRepliesOfCommentTest)
  - `comments/PublisherCommentTest.java:400` (testPublisherEditCommentTest)
  - `comments/PublisherCommentTest.java:438` (testPublisherDeleteCommentTest)
  - `comments/PublisherCommentTest.java:452` (testPublisherDeleteNotExistingCommentTest)
  - `comments/PublisherCommentTest.java:459` (testPublisherAddNewRootCommentWithReplyTest)
- **publisher-comment-cross-portal-visibility** — Publisher comments visible/paginated from DevPortal; cross-portal reply chains
  - `comments/PublisherCommentTest.java:324` (testVerifyDevPortalGetAllCommentsTest)
  - `comments/PublisherCommentTest.java:341` (testVerifyDevPortalPaginatedRootCommentsTest)
  - `comments/PublisherCommentTest.java:353` (testVerifyDevPortalGetRepliesOfCommentTest)
  - `comments/PublisherCommentTest.java:371` (testVerifyDevPortalPaginationOfRepliesOfCommentTest)
  - `comments/PublisherCommentTest.java:561` (testDevPortalNonAdminUserAddReplyToCommentFromPublisherTest)
  - `comments/PublisherCommentTest.java:578` (testDevPortalAdminUserAddReplyToCommentFromPublisherTest)
  - `comments/PublisherCommentTest.java:619` (testVerifyDevPortalAdminDeleteCommentTest)
- **publisher-comment-authorization** — Edit/delete by owner / non-owner non-admin / non-owner admin; comment & reply by non-admin user
  - `comments/PublisherCommentTest.java:485` (testPublisherEditCommentByNonOwnerNonAdminUserTest)
  - `comments/PublisherCommentTest.java:497` (testPublisherEditCommentByNonOwnerAdminUserTest)
  - `comments/PublisherCommentTest.java:513` (testPublisherAddCommentByNonAdminUserTest)
  - `comments/PublisherCommentTest.java:529` (testPublisherAddReplyToNonAdminUserCommentByAdminUserTest)
  - `comments/PublisherCommentTest.java:545` (testPublisherAddReplyToAdminUserCommentByNonAdminUserTest)
  - `comments/PublisherCommentTest.java:595` (testPublisherDeleteCommentByNonOwnerNonAdminUserTest)
  - `comments/PublisherCommentTest.java:604` (testPublisherDeleteCommentByNonOwnerAdminUserTest)

---

### Multi-tenancy & Organizations

#### Cross-tenant API/policy visibility
- **cross-tenant-api-visibility** — API created in one tenant visible (or directly available) from another tenant's store
  - `crossSubscription/CrossTenantSubscriptionTestCase.java:342` (testVisibilityOfAPIFromOtherDomain)
  - `crossSubscription/CrossTenantSubscriptionTestCase.java:360` (testVisibilityOfAPIFromOtherDomain2)
  - `crossSubscription/CrossTenantSubscriptionTestCase.java:386` (testDirectAPIAvailability)
  - `crossSubscription/CrossTenantSubscriptionTestCase.java:394` (testDirectAPIAvailability2)
- **cross-tenant-policy-visibility** — Application & subscription throttling policies visibility across tenants
  - `crossSubscription/CrossTenantSubscriptionTestCase.java:402` (testApplicationPolicyAvailabilityInTenant2)
  - `crossSubscription/CrossTenantSubscriptionTestCase.java:423` (testApplicationPolicyAvailabilityInTenant1)
  - `crossSubscription/CrossTenantSubscriptionTestCase.java:443` (testSubscriptionPolicyAvailabilityInTenant1)
  - `crossSubscription/CrossTenantSubscriptionTestCase.java:464` (testSubscriptionPolicyAvailabilityInTenant2)
- **cross-tenant-key-manager-visibility** — Retrieve key managers of one tenant as a user of another tenant
  - `crossSubscription/CrossTenantSubscriptionTestCase.java:612` (getKeyManagersFromTenant1FromTenant2User)
  - `crossSubscription/CrossTenantSubscriptionTestCase.java:632` (getKeyManagersFromTenant2FromTenant1User)

#### Cross-tenant application & subscription
- **cross-tenant-application-creation** — Create application in one tenant as a user of another tenant (positive + negative with same-tenant policy)
  - `crossSubscription/CrossTenantSubscriptionTestCase.java:485` (testCreateApplicationInTenant1FromTenant2User)
  - `crossSubscription/CrossTenantSubscriptionTestCase.java:495` (testCreateApplicationInTenant2FromTenant1User)
  - `crossSubscription/CrossTenantSubscriptionTestCase.java:506` (testCreateApplicationInTenant1FromTenant2UserNegative)
  - `crossSubscription/CrossTenantSubscriptionTestCase.java:518` (testCreateApplicationInTenant2FromTenant1UserNegative)
- **cross-tenant-subscription** — Subscribe a tenant-A app to a tenant-B API (both directions), including restricted-policy variants
  - `crossSubscription/CrossTenantSubscriptionTestCase.java:530` (testCreateSubscriptionFromTenant2AppToTenant1API)
  - `crossSubscription/CrossTenantSubscriptionTestCase.java:541` (testCreateSubscriptionFromTenant1AppToTenant2API)
  - `crossSubscription/CrossTenantSubscriptionTestCase.java:552` (testCreateSubscriptionFromTenant2AppToTenant1APIRestrictedPolicy)
  - `crossSubscription/CrossTenantSubscriptionTestCase.java:566` (testCreateSubscriptionFromTenant1AppToTenant2APIRestrictedPolicy)
- **cross-tenant-key-generation & invocation** — Generate keys for a tenant-1 app in tenant-2/tenant-1 store; invoke with token in same vs other tenant; new-app + token on already-subscribed app
  - `crossSubscription/CrossTenantSubscriptionTestCase.java:582` (testCreateNewApplicationAndGenerateTokenSubscribedApplication)
  - `crossSubscription/CrossTenantSubscriptionTestCase.java:656` (testGenerateKeysFromTenant1AppInTenant2Store)
  - `crossSubscription/CrossTenantSubscriptionTestCase.java:673` (testGenerateKeysFromTenant1AppInTenant1Store)
  - `crossSubscription/CrossTenantSubscriptionTestCase.java:740` (invokeFromTokenInSameTenant)
  - `crossSubscription/CrossTenantSubscriptionTestCase.java:761` (invokeFromTokenInOtherTenant)
- **cross-tenant-key/subscription retrieval** — Retrieve OAuth keys and subscriptions of an app from tenant-1/tenant-2 stores
  - `crossSubscription/CrossTenantSubscriptionTestCase.java:689` (testRetrieveOauthKeysFromTenant1Store)
  - `crossSubscription/CrossTenantSubscriptionTestCase.java:716` (testRetrieveOauthKeysFromTenant2)
  - `crossSubscription/CrossTenantSubscriptionTestCase.java:785` (testRetrieveSubscriptionsFromApplicationId)
  - `crossSubscription/CrossTenantSubscriptionTestCase.java:818` (testRetrieveSubscriptionsFromApplicationId2)
- **cross-tenant-subscription-update** — New application can generate access token using an already-subscribed application (subscription reuse)
  - `crossSubscription/CrossTenantSubscriptionUpdateTestCase.java:101` (testCreateNewApplicationAndGenerateTokenSubscribedApplication)
  - (suite harness: `crossSubscription/CrossTenantSubscriptionTestSuite.java` — config only)

---

### Duplicate coverage (within this section)

- **refresh-token grant** — covered by `token/RefreshTokenTestCase.java:85` and `token/TokenAPITestCase.java:203` (both run a refresh-token flow and assert a new working access token; RefreshTokenTestCase is the standalone version, TokenAPITestCase bundles it into a larger token-API flow).
- **new-app-generates-token-on-already-subscribed-application** — covered by `crossSubscription/CrossTenantSubscriptionUpdateTestCase.java:101` and `crossSubscription/CrossTenantSubscriptionTestCase.java:582` (identical method name `testCreateNewApplicationAndGenerateTokenSubscribedApplication`; the *UpdateTestCase is effectively an extracted/standalone copy of the same scenario).
- **backend-JWT-generation-for-JWT-app** — covered by `jwt/JWTTestCase.java:217`, `jwt/FederatedUserJWTTestCase.java:233`, and `jwt/urlsafe/URLSafeJWTTestCase.java:112` (all three share the method `testEnableJWTAndClaimsForJWTApp`; differ only by user type (federated) and encoding (URL-safe), so the core backend-JWT-claims behavior is verified three times).
- **opaque-vs-non-opaque API-key backend-JWT/decoding** — overlap between `jwt/JWTTestCase.java` (testOpaqueAPIKeyOnlySecuredAPIInvocation / testOpaqueAPIKeyForAPIKeyApp) and `jwt/jwtdecoding/JWTDecodingTestCase.java` (testJWTDecodingforCustomApplicationWithOpaqueKey) — both exercise backend JWT under opaque API keys, JWTDecodingTestCase focusing on decode/verify of the same emitted JWT.
- **comment CRUD + cross-portal visibility + authorization** — `comments/DevPortalCommentTest.java` and `comments/PublisherCommentTest.java` are near-mirror images: each runs the full add/reply/paginate/edit/delete + permission matrix and additionally verifies the *other* portal sees the same comments. Substantial intentional duplication between the two portals.
- **token-persistence suites vs base token flows** — the four `token/TokenPersistence*TestSuite.java` classes are config harnesses that re-run already-covered opaque/JWT/external-IDP/URL-safe token flows under a persistence deployment.toml; coverage of the underlying token behavior duplicates the corresponding non-persistence token/JWT test cases.

## Part C — Admin, Governance, Workflows, Throttling, Multi-tenancy, Gateway Policy

Scope: `admin/`, `restapi/admin/`, `throttling/`, `apimGovernance/`, `workflow/`, `organization/`, `tenantsync/`, `gatewayPolicy/`
(All paths relative to `.../tests-backend/src/test/java/org/wso2/am/integration/tests/`)

### Throttling & Rate Limiting

#### Runtime Throttling Enforcement (invocation hits 429)
- **api-level-throttling-enforced** — Requests beyond API/resource throttle tier get throttled
  - `throttling/APIThrottlingTestCase.java:83` (testAPIThrottling_1)
  - (dup) JWT path: `throttling/JWTRequestCountThrottlingTestCase.java:326` (testAPILevelThrottling)
- **application-level-throttling-enforced** — Requests beyond application tier throttled
  - `throttling/JWTRequestCountThrottlingTestCase.java:228` (testApplicationLevelThrottling)
  - (dup) bandwidth variant: `throttling/JWTBandwidthThrottlingTestCase.java:164` (testApplicationLevelThrottling)
- **subscription-level-throttling-enforced** — Requests beyond subscription tier throttled
  - `throttling/JWTRequestCountThrottlingTestCase.java:255` (testSubscriptionLevelThrottling)
  - (dup) bandwidth variant: `throttling/JWTBandwidthThrottlingTestCase.java:205` (testSubscriptionLevelThrottling)
  - (dup) policy-mgmt + enforce: `restapi/admin/throttlingpolicy/SubscriptionThrottlingPolicyTestCase.java:205` (testSubscriptionLevelThrottling)
- **conditional-throttling-ip-header-query-jwtclaim** — API-level throttling honoring IP / header / query / JWT-claim conditions
  - `throttling/JWTRequestCountThrottlingTestCase.java:350` (IP), `:390` (header), `:409` (query), `:430` (JWT claim)
  - non-authenticated resource + JWT claim condition: `throttling/JWTRequestCountThrottlingTestCase.java:283`
- **bandwidth-throttling-enforced** — Bandwidth (data volume) based throttling enforcement at app/sub/API level
  - `throttling/JWTBandwidthThrottlingTestCase.java:164,205,246`
- **hard-throttling-backend-tps-limit** — Hard throttle limit (production TPS) on backend enforced
  - `throttling/HardThrottlingTestCase.java:85` (testProductionLimit)
- **burst-control-rate-limiting** — Burst/rate-limit portion of subscription tier enforced on changed tier
  - `throttling/BurstControlTestCase.java:169` (testBurstLimitChange)

#### Application Throttle Policy Lifecycle (Admin REST)
- **application-policy-crud-requestcount** — Add application policy with request-count limit
  - `restapi/admin/throttlingpolicy/ApplicationThrottlingPolicyTestCase.java:68`
- **application-policy-crud-bandwidth** — Add application policy with bandwidth limit
  - `restapi/admin/throttlingpolicy/ApplicationThrottlingPolicyTestCase.java:98`
- **application-policy-get-update** — Get and update application policy
  - `restapi/admin/throttlingpolicy/ApplicationThrottlingPolicyTestCase.java:128`
- **application-policy-delete / negatives** — Delete, duplicate-name reject, delete non-existent id
  - `restapi/admin/throttlingpolicy/ApplicationThrottlingPolicyTestCase.java:154,163,176`
- **application-policy-reset-runtime** — Reset application throttle counters (request-count & bandwidth) at runtime
  - `throttling/ApplicationThrottlingResetTestCase.java:214,232`

#### Subscription Throttle Policy Lifecycle (Admin REST)
- **subscription-policy-crud-requestcount / bandwidth** — Add with request-count / bandwidth limit
  - `restapi/admin/throttlingpolicy/SubscriptionThrottlingPolicyTestCase.java:132,169`
- **subscription-policy-permission-visibility** — Restricted policies visible only to permitted roles
  - `restapi/admin/throttlingpolicy/SubscriptionThrottlingPolicyTestCase.java:322` (testCheckPolicyPermission)
- **subscription-policy-get-update / delete / negatives**
  - `restapi/admin/throttlingpolicy/SubscriptionThrottlingPolicyTestCase.java:351,377,386,399`

#### Advanced (API/Resource) Throttle Policy Lifecycle (Admin REST)
- **advanced-policy-crud-requestcount / bandwidth / conditional-groups**
  - `restapi/admin/throttlingpolicy/AdvancedThrottlingPolicyTestCase.java:120,152,184`
- **advanced-policy-get-update / delete / negatives**
  - `restapi/admin/throttlingpolicy/AdvancedThrottlingPolicyTestCase.java:218,244,272,281,294`
- **advanced-policy-level-switch** — Switch policy between operation-level and API-level
  - `restapi/admin/throttlingpolicy/AdvancedThrottlingPolicyTestCase.java:309,367`
- **advanced-policy-multi-admin-permission** — Cross-admin-user delete restrictions for assigned API/resource policies
  - `restapi/admin/throttlingpolicy/AdvancedThrottlingPolicyTestCase.java:427,458,508`

#### Custom Throttle Policy Lifecycle (Admin REST)
- **custom-policy-crud** — Add / get / update / delete custom global throttle policy + negatives
  - `restapi/admin/throttlingpolicy/CustomThrottlingPolicyTestCase.java:60,97,119,142,159,173`

#### Deny / Block Conditions (Admin REST)
- **block-conditions-add-and-search** — Add blocking conditions and search by type/value (incl. exact value)
  - `restapi/admin/throttlingpolicy/DenyPolicySearchTestCase.java:76,121,159`

#### Throttle Policy Listing
- **throttle-policies-list-all** — Get all throttle policies (type:all)
  - `restapi/admin/throttlingpolicy/GetThrottlingPoliciesTestCase.java:56` (testThrottlePoliciesGet)

#### Throttle Policy Export / Import (Admin REST)
- **throttle-policy-export** — Export custom / subscription / application / advanced policies
  - `restapi/admin/throttlingpolicy/ThrottlePolicyExportImportTestCase.java:253,284,312,340`
- **throttle-policy-import-conflict / update / new** — Import conflict (no update), import with update, import brand-new
  - `restapi/admin/throttlingpolicy/ThrottlePolicyExportImportTestCase.java:370,382,394,406 / 418,426,434,442 / 450,460,470,480`

#### Tier Management (legacy admin dashboard)
- **api-tier-add-delete** — Add and delete an API throttling tier via admin-dashboard client
  - `throttling/APITierManagementTestCase.java:66,80`

#### Unlimited Tier Disabled / Configurable Default Policy
- **unlimited-tier-disabled-behavior** — When Unlimited tier disabled: GraphQL/REST default to configured policy; reject Unlimited tier on API/app; Settings API omits Unlimited
  - `throttling/unlimitedDisable/UnlimitedTierDisabledTestCase.java:110,158,210,277,312,325`
- **configurable-default-policy-behavior** — Configured default policy applied when no tier set; Settings API & default app policy reflect config
  - `throttling/unlimitedDisable/ConfigurableDefaultPolicyTestCase.java:183,229,279,287`
- (helpers, no @Test) `throttling/unlimitedDisable/UnlimitedTierDisabledConfig.java`, `throttling/ThrottlingUtils.java`

### Admin, Governance & Workflows

#### Key Manager Management (Admin REST)
- **keymanager-crud-per-connector** — Add (mandatory / missing-mandatory negative / optional params), get, update, delete key managers across connector types: Auth0, WSO2IS, Keycloak, Okta, PingFederate, ForgeRock
  - `restapi/admin/KeyManagersTestCase.java:110,154,190,237,258,274` (Auth0)
  - `:291,330,362,408,428,443` (WSO2IS); `:459,498,529,572,592,607` (Keycloak); `:624,663,693,741,762,777` (Okta)
  - `:794,835,866,915,937,952` (PingFederate); `:969,1008,1039,1088,1108,1123` (ForgeRock)
- **keymanager-duplicate-name-reject** — `restapi/admin/KeyManagersTestCase.java:1140`
- **keymanager-role-permissions** — Key manager visibility/permissions by role (WSO2IS) — `restapi/admin/KeyManagersTestCase.java:1153`

#### Gateway Environment Management (Admin REST)
- **environment-crud-and-vhosts** — Add env (no VHost / single / multiple VHosts), special-char name negative, duplicate-env negative, get all, update, update by swapping VHost
  - `restapi/admin/EnvironmentTestCase.java:126,147,261,301,289,336,431,458` (many adjacent tests at :168,191,217,240 are commented out)
- **environment-revision-deploy-with-vhost** — Deploy API revision to a VHost; validate Devportal API/Swagger response
  - `restapi/admin/EnvironmentTestCase.java:364,411`
- **environment-delete-with-revisions** — Delete env blocked while revisions deployed; succeeds after undeploy
  - `restapi/admin/EnvironmentTestCase.java:475,503`
- **gateway-permissions / default-env instances** — Gateway permission checks; get gateway instances in default environment
  - `restapi/admin/EnvironmentTestCase.java:536,654`

#### API System Scopes / Role-Scope Mapping (Admin REST)
- **scope-role-mapping-crud** — Add, get, delete role-alias scope mapping
  - `restapi/admin/APISystemScopesTestCase.java:66,91,99`

#### Application Search (Admin REST)
- **application-search-by-name-or-owner** — Admin and non-admin search of applications by name and by owner
  - `restapi/admin/ApplicationsSearchByNameOrOwnerTestCase.java:143,161,179,197`

#### API Provider Change (Admin)
- **change-api-provider** — Change provider/owner of REST, SOAP, SOAP-to-REST, GraphQL APIs
  - `admin/ChangeApiProviderTestCase.java:169,576,720,969`
- **change-api-provider-secondary-userstore** — Same change-provider flows but new provider is a secondary user store user
  - `admin/ChangeApiProviderSecondaryUserStoreTestCase.java:193,582,724,971`

#### Application Owner Update (Admin)
- **oauth-app-owner-update** — Update OAuth application ownership: validate new owner is a subscriber, cross-tenant rejection, name-collision rejection, same-domain transfer, update by new owner, transfer with custom roles
  - `admin/OAuthApplicationOwnerUpdateTestCase.java:200,211,222,234,246,294`

#### API Governance — Compliance
- **api-compliance-default-policy** — Compliance details of a REST API after creation with default policy
  - `apimGovernance/APIComplianceTestCase.java:96`
- **api-deployment-blocking-by-policy** — API deployment blocked when governance policy violated
  - `apimGovernance/APIComplianceTestCase.java:132`
- **mcp-server-compliance-default-policy** — Compliance details of an MCP Server after creation with default policy
  - `apimGovernance/MCPComplianceTestCase.java:100`

#### API Governance — Policy Management
- **governance-policy-crud** — Default policy retrieval; create / update / delete valid global governance policy
  - `apimGovernance/PolicyMgtTestCase.java:74,90,120,146`

#### API Governance — Ruleset Management
- **governance-ruleset-crud** — Default ruleset retrieval; create (valid / invalid / JSON content); update (valid / invalid); delete (valid / invalid-when-attached-to-policy)
  - `apimGovernance/RulesetMgtTestCase.java:98,119,147,172,202,234,247,275`

#### Workflows (approval executor)
- **workflow-approval-all-types** — Approval-based workflow process for API, API Product, Application (create/update/delete), Subscription (create/update/delete), Application Registration, User Sign-Up, API Revision Deployment; plus cleanup
  - `workflow/WorkflowApprovalExecutorTest.java:160,256,332,424,685,859,950,1164,1319,1404,1485,1719`

### Multi-tenancy & Organizations

#### Organizations (consumer-facing visibility & sharing)
- **organization-add** — Add a consumer organization
  - `organization/ConsumerOrganizationVisibilityTestCase.java:158`
- **org-visibility-on-api** — Set API visibility to none / specific-org / all-orgs
  - `organization/ConsumerOrganizationVisibilityTestCase.java:179,217,254`
- **org-keymanager-visibility** — Key manager restricted to allowed organizations
  - `organization/ConsumerOrganizationVisibilityTestCase.java:291`
- **org-application-sharing** — Application sharing across organizations
  - `organization/ConsumerOrganizationVisibilityTestCase.java:389`
- **org-specific-subscription-policies** — Organization-scoped subscription throttle policies
  - `organization/ConsumerOrganizationVisibilityTestCase.java:414`

#### Tenant Synchronization (APIM-IS tenant sync)
- **tenant-lifecycle-event-sync** — APIM reacts to IS tenant management events: create, update, activate, deactivate
  - `tenantsync/APIMTenantCreationNotificationTestCase.java:171,194,230,252`
- (suite wrapper, config load/restore only) `tenantsync/APIMISTenantSyncTestSuite.java:37,48`
- (model helper, no @Test) `tenantsync/model/TenantManagementEvent.java`

### Gateway, Mediation & Invocation

#### Gateway Policy (gateway-level mediation policies)
- **gateway-policy-lifecycle-and-invocation** — Invoke API before policy; add policy; deploy; verify effect on invocation; update deployed policy; deploy a second policy to same gateway; delete active-deployment policy; undeploy; delete policy with no active deployment
  - `gatewayPolicy/GatewayPolicyTestCase.java:96,107,132,150,176,218,252,262,276`

### Duplicate coverage (within this section)
- **Subscription-level throttling enforcement** — covered by `throttling/JWTRequestCountThrottlingTestCase.java`, `throttling/JWTBandwidthThrottlingTestCase.java`, and `restapi/admin/throttlingpolicy/SubscriptionThrottlingPolicyTestCase.java` (the JWT pair test request-count vs bandwidth variants; the admin-REST class re-verifies enforcement after policy CRUD).
- **Application- and API-level throttling enforcement** — covered by both `throttling/JWTRequestCountThrottlingTestCase.java` and `throttling/JWTBandwidthThrottlingTestCase.java` (parallel request-count vs bandwidth structures with near-identical method names) and partly by `throttling/APIThrottlingTestCase.java`.
- **Throttle policy CRUD pattern** — the Application / Subscription / Advanced / Custom policy test classes (`restapi/admin/throttlingpolicy/*`) each repeat the same add(requestcount/bandwidth)+get+update+delete+duplicate-name+non-existent-id matrix; `ThrottlePolicyExportImportTestCase` and `GetThrottlingPoliciesTestCase` overlap on policy creation/listing.
- **Change-API-provider flows** — `admin/ChangeApiProviderTestCase.java` and `admin/ChangeApiProviderSecondaryUserStoreTestCase.java` are near-identical (REST/SOAP/SOAP-to-REST/GraphQL), differing only in primary vs secondary user store for the new provider.
- **Compliance with default policy** — `apimGovernance/APIComplianceTestCase.java` and `apimGovernance/MCPComplianceTestCase.java` duplicate the "create-with-default-policy then check compliance" behavior for REST APIs vs MCP Servers.

## Legacy TestNG Coverage Map — Part D

Scope: `graphql/`, `graphqlqueryanalysis/`, `websocket/`, `streamingapis/`, `solace/`, `operationPolicy/`, `sequence/`, `header/`, `schemaValidation/`, `oas/`

Base dir: `all-in-one-apim/modules/integration/tests-integration/tests-backend/src/test/java/org/wso2/am/integration/tests/`

### GraphQL APIs
#### Schema-based creation & definition management
- **create-graphql-api-from-schema** — Create and publish a GraphQL API by uploading a GraphQL schema (with interfaces)
  - `graphql/GraphqlTestCase.java:170`
- **create-graphql-api-malformed-context** — Reject GraphQL API creation when schema given with a malformed context
  - `graphql/GraphqlTestCase.java:214`
- **create-graphql-api-from-sdl-url** — Create GraphQL API using an SDL URL
  - `graphql/GraphqlTestCase.java:252`
- **create-graphql-api-from-endpoint** — Create GraphQL API by pointing at a GraphQL endpoint
  - `graphql/GraphqlTestCase.java:291`
- **retrieve-schema-definition** — Retrieve GraphQL schema definition at the publisher
  - `graphql/GraphqlTestCase.java:330`
- **update-schema-definition** — Update GraphQL schema definition of an existing API
  - `graphql/GraphqlTestCase.java:337`

#### Query/operation invocation & security
- **invoke-graphql-with-jwt-app** — Invoke GraphQL API using a JWT-type application
  - `graphql/GraphqlTestCase.java:351`
  - (dup) also covered by `graphqlqueryanalysis/GraphQLQueryAnalysisTest.java:166`
- **invoke-graphql-with-oauth-app** — Invoke GraphQL API using an OAuth-type application
  - `graphql/GraphqlTestCase.java:376`
  - (dup) also covered by `graphqlqueryanalysis/GraphQLQueryAnalysisTest.java:218`
- **operation-level-oauth-scopes** — Enforce operation/field-level OAuth scopes for GraphQL operations
  - `graphql/GraphqlTestCase.java:402`
- **operation-level-security** — Enforce operation-level security settings for GraphQL operations
  - `graphql/GraphqlTestCase.java:495`

#### Query analysis (depth & complexity)
- **add-graphql-complexity-values** — Add per-field GraphQL complexity values in the publisher
  - `graphqlqueryanalysis/GraphQLQueryAnalysisTest.java:124`
- **retrieve-graphql-complexity-values** — View GraphQL complexity values in the developer portal
  - `graphqlqueryanalysis/GraphQLQueryAnalysisTest.java:155`
- **query-complexity-enforcement** — Reject GraphQL queries exceeding configured complexity (via subscriptions invocation)
  - `graphql/GraphqlSubscriptionTestCase.java:320`
- **query-depth-enforcement** — Reject GraphQL queries exceeding configured depth (via subscriptions invocation)
  - `graphql/GraphqlSubscriptionTestCase.java:384`

#### GraphQL subscriptions (WebSocket transport)
- **publish-graphql-api-with-subscriptions** — Publish a GraphQL API exposing subscription operations
  - `graphql/GraphqlSubscriptionTestCase.java:178`
- **graphql-subscription-jwt-app-subscription** — Create a JWT application and subscribe to the GraphQL subscription API
  - `graphql/GraphqlSubscriptionTestCase.java:268`
- **graphql-subscription-invoke-with-jwt-token** — Invoke GraphQL subscription over WebSocket with a valid JWT token
  - `graphql/GraphqlSubscriptionTestCase.java:282`
- **graphql-subscription-invalid-payload** — Reject GraphQL subscription invocation with an invalid payload
  - `graphql/GraphqlSubscriptionTestCase.java:305`
- **graphql-subscription-scopes** — Enforce scopes on GraphQL subscription invocation
  - `graphql/GraphqlSubscriptionTestCase.java:416`
- **graphql-subscription-throttling** — Apply throttling to GraphQL subscription invocations
  - `graphql/GraphqlSubscriptionTestCase.java:517`

_Helper (non-test) classes: `graphql/websocket/client/SubscriptionWSClientImpl.java`, `graphql/websocket/server/SubscriptionServerCreator.java`, `graphql/websocket/server/SubscriptionWSServerImpl.java`._

### Streaming APIs
#### WebSocket APIs — lifecycle & invocation
- **publish-websocket-api** — Publish a WebSocket API
  - `websocket/WebSocketAPITestCase.java:167`
- **websocket-app-subscription** — Create an application and subscribe to a WebSocket API
  - `websocket/WebSocketAPITestCase.java:213`
- **websocket-invocation-with-token** — Invoke a subscribed WebSocket API with a valid OAuth token
  - `websocket/WebSocketAPITestCase.java:224`
- **websocket-jwt-app-subscription** — Create a JWT application and subscribe to a WebSocket API
  - `websocket/WebSocketAPITestCase.java:247`
  - (dup) also `websocket/WebSocketAPICorsValidationTestCase.java:206`, `websocket/WebSocketAPIScopeTestCase.java:290`
- **websocket-invocation-with-jwt-token** — Invoke WebSocket API with a JWT token
  - `websocket/WebSocketAPITestCase.java:258`
- **websocket-sandbox-only-endpoint** — Invoke WebSocket API with only sandbox endpoint configured (remove prod endpoint)
  - `websocket/WebSocketAPITestCase.java:280`
- **websocket-invalid-token-invocation** — Reject WebSocket API invocation with an invalid token
  - `websocket/WebSocketAPITestCase.java:445`
- **websocket-malformed-context** — Reject WebSocket API creation with a malformed context
  - `websocket/WebSocketAPITestCase.java:823`

#### WebSocket APIs — throttling
- **websocket-throttling** — Apply throttling to WebSocket API invocations
  - `websocket/WebSocketAPITestCase.java:364`

#### WebSocket APIs — API key & OAuth auth modes
- **websocket-apikey-when-disabled** — Reject API-key invocation when API Key auth is not enabled
  - `websocket/WebSocketAPITestCase.java:471`
- **websocket-invocation-with-apikey** — Invoke WebSocket API using an API key
  - `websocket/WebSocketAPITestCase.java:509`
- **websocket-invocation-with-opaque-apikey** — Invoke WebSocket API using an opaque API key
  - `websocket/WebSocketAPITestCase.java:549`
- **websocket-oauth-when-disabled** — Reject OAuth access-token invocation when OAuth auth is not enabled
  - `websocket/WebSocketAPITestCase.java:573`
- **websocket-expired-apikey** — Reject WebSocket invocation using an expired API key
  - `websocket/WebSocketAPITestCase.java:596`
- **websocket-apikey-ip-restriction** — Invoke WebSocket API with an API key generated under IP restrictions
  - `websocket/WebSocketAPITestCase.java:635`
- **websocket-opaque-apikey-ip-restriction** — Invoke WebSocket API with an opaque API key + IP restrictions
  - `websocket/WebSocketAPITestCase.java:666`
- **websocket-apikey-restricted-other-ip** — Reject API key restricted for another IP
  - `websocket/WebSocketAPITestCase.java:688`
- **websocket-apikey-referer-restriction** — Invoke WebSocket API with API key generated under Referer restrictions
  - `websocket/WebSocketAPITestCase.java:726`
- **websocket-opaque-apikey-referer-restriction** — Invoke WebSocket API with opaque API key + Referer restrictions
  - `websocket/WebSocketAPITestCase.java:759`
- **websocket-apikey-restricted-other-referer** — Reject API key restricted for another Referer
  - `websocket/WebSocketAPITestCase.java:783`

#### WebSocket APIs — scopes (per-resource/topic)
- **publish-websocket-api-with-scopes** — Publish a WebSocket API with resource/topic-level scopes
  - `websocket/WebSocketAPIScopeTestCase.java:152`
- **websocket-scope-generate-keys** — Generate keys for the scoped WebSocket API
  - `websocket/WebSocketAPIScopeTestCase.java:302`
- **websocket-scope-invoke-products-catalog** — Invoke PRODUCTS_CATALOG_1 endpoint with a scoped token
  - `websocket/WebSocketAPIScopeTestCase.java:317`
- **websocket-scope-invoke-products-popular** — Invoke PRODUCTS_POPULAR endpoint with a scoped token
  - `websocket/WebSocketAPIScopeTestCase.java:354`
- **websocket-scope-invoke-orders** — Invoke ORDERS endpoint with a scoped token
  - `websocket/WebSocketAPIScopeTestCase.java:390`
- **websocket-scope-invoke-wildcard** — Invoke WILDCARD endpoint with a scoped token
  - `websocket/WebSocketAPIScopeTestCase.java:426`

#### WebSocket APIs — CORS
- **websocket-cors-origin-validation** — Validate CORS origin header on WebSocket API invocations
  - `websocket/WebSocketAPICorsValidationTestCase.java:229`

#### WebSocket APIs — tracing/observability
- **websocket-invocation-with-tracing** — Invoke a WebSocket API with distributed tracing enabled
  - `websocket/WebSocketAPIInvocationWithTracingTestCase.java:207` (publish:150, subscribe:196)

#### WebSocket APIs — gateway URLs
- **websocket-gateway-urls** — Verify WS API gateway URLs are exposed correctly
  - `websocket/APIMANAGER5869WSGayewatURLTestCase.java:163` (create:91, publish:118)
- **websocket-gateway-urls-after-config-change** — Verify WS API gateway URLs after a gateway config change
  - `websocket/APIMANAGER5869WSGayewatURLTestCase.java:204`

_Helper (non-test) classes: `websocket/client/WebSocketClientImpl.java`, `websocket/server/WebSocketServerImpl.java`._

#### WebSub APIs
- **publish-websub-api** — Publish a WebSub API
  - `streamingapis/websub/WebSubAPITestCase.java:168`
  - (dup) also in FailedWebSub:150, LeaseTime:149, MultipleWebSub:165, SecretValidation:148, Throttling:206
- **websub-app-subscription** — Create an application and subscribe to a WebSub API
  - `streamingapis/websub/WebSubAPITestCase.java:213`
- **invoke-websub-api** — Invoke (publish event to) a WebSub API
  - `streamingapis/websub/WebSubAPITestCase.java:224`
- **websub-invalid-topic** — Reject WebSub invocation when payload-URL topic is invalid
  - `streamingapis/websub/WebSubAPITestCase.java:238`
- **websub-query-parameters** — Invoke WebSub API passing parameters as query parameters
  - `streamingapis/websub/WebSubAPITestCase.java:253`
- **websub-form-url-encoded** — Invoke WebSub API passing parameters as form-url-encoded data
  - `streamingapis/websub/WebSubAPITestCase.java:277`
- **websub-multiple-subscriptions-count** — Verify subscription count with multiple subscriptions
  - `streamingapis/websub/WebSubAPITestCase.java:307`
- **websub-mandatory-parameters-present** — Verify availability of mandatory subscription parameters
  - `streamingapis/websub/WebSubAPITestCase.java:383`
- **websub-missing-mandatory-parameters** — Reject subscription when mandatory parameters are missing
  - `streamingapis/websub/WebSubAPITestCase.java:410`
- **websub-subscriber-verification** — Perform subscriber (hub) verification handshake
  - `streamingapis/websub/WebSubAPITestCase.java:425`

##### WebSub — failure/negative invocation
- **websub-invalid-token** — Reject WebSub invocation with an invalid token
  - `streamingapis/websub/FailedWebSubSubscriptionTestCase.java:213`
- **websub-no-callback-url** — Reject WebSub invocation without a callback URL
  - `streamingapis/websub/FailedWebSubSubscriptionTestCase.java:226`
- **websub-no-topic** — Reject WebSub invocation without a topic name
  - `streamingapis/websub/FailedWebSubSubscriptionTestCase.java:237`

##### WebSub — lease time
- **websub-lease-time-subscription** — Honor subscription lease time for WebSub
  - `streamingapis/websub/LeaseTimeSubscriptionTestCase.java:205` (publish:149, subscribe:194)

##### WebSub — multiple subscriptions
- **websub-multiple-subscription-fanout** — Deliver events to multiple WebSub subscriptions
  - `streamingapis/websub/MultipleWebSubSubcriptionTestCase.java:234` (publish:165, subscribe:210)

##### WebSub — secret/HMAC validation
- **websub-hub-secret-validation** — Validate hub secret (signature) on delivered WebSub events
  - `streamingapis/websub/SecretValidationTestCase.java:205` (publish:148, subscribe:194)

##### WebSub — throttling
- **websub-events-throttling** — Throttle WebSub event publishing rate
  - `streamingapis/websub/ThrottlingTestCase.java:269`
- **websub-subscription-count-throttling** — Throttle by max WebSub subscription count
  - `streamingapis/websub/ThrottlingTestCase.java:294`

_Helper (non-test) classes: `streamingapis/websub/client/WebhookSender.java`, `streamingapis/websub/server/CallbackServerServlet.java`, `CallbackServerServlet2.java`, `CallbackServerServletWithSubVerification.java`._

#### Server-Sent Events (SSE) APIs
- **publish-sse-api** — Publish an SSE API
  - `streamingapis/serversentevents/ServerSentEventsAPITestCase.java:154`
- **sse-app-subscription** — Create an application and subscribe to an SSE API
  - `streamingapis/serversentevents/ServerSentEventsAPITestCase.java:195`
- **sse-topic-retrieval** — Retrieve topics of an SSE API
  - `streamingapis/serversentevents/ServerSentEventsAPITestCase.java:206`
- **invoke-sse-api** — Invoke (stream events from) an SSE API
  - `streamingapis/serversentevents/ServerSentEventsAPITestCase.java:215`
- **sse-throttling** — Apply throttling to SSE API invocations
  - `streamingapis/serversentevents/ServerSentEventsAPITestCase.java:236`

_Helper (non-test) classes: `streamingapis/serversentevents/client/SimpleSseReceiver.java`, `streamingapis/serversentevents/server/SseServlet.java`._

#### Async APIs (AsyncAPI import/publish)
- **create-async-api-without-advertise-only** — Create an Async API as a normal (non-advertise-only) API
  - `streamingapis/async/AsyncAPITestCase.java:91`
  - (dup) also `streamingapis/async/AsyncAPITestWithValidationCase.java:97`
- **import-publish-async-api** — Import and publish an Async API
  - `streamingapis/async/AsyncAPITestCase.java:119`
  - (dup) also `streamingapis/async/AsyncAPITestWithValidationCase.java:153`
- **async-app-subscription** — Create an application and subscribe to an Async API
  - `streamingapis/async/AsyncAPITestCase.java:177`
  - (dup) also `streamingapis/async/AsyncAPITestWithValidationCase.java:312`

##### Async APIs — definition validation (V2/V3)
- **create-async-api-v3-without-advertise-only** — Create an AsyncAPI V3 as a normal API
  - `streamingapis/async/AsyncAPITestWithValidationCase.java:125`
- **import-publish-async-api-v3** — Import and publish an AsyncAPI V3
  - `streamingapis/async/AsyncAPITestWithValidationCase.java:211`
- **import-invalid-async-api-v2** — Reject import of an invalid AsyncAPI V2 definition
  - `streamingapis/async/AsyncAPITestWithValidationCase.java:269`
- **import-invalid-async-api-v3** — Reject import of an invalid AsyncAPI V3 definition
  - `streamingapis/async/AsyncAPITestWithValidationCase.java:334`
- **async-v3-app-subscription** — Create an application and subscribe to an AsyncAPI V3 API
  - `streamingapis/async/AsyncAPITestWithValidationCase.java:323`

_TestNG suite/aggregator (non-test) classes: `streamingapis/async/AsyncAPITestSuite.java`, `streamingapis/async/AsyncAPIValidationTestSuite.java`. Shared util: `streamingapis/StreamingApiTestUtils.java`._

#### Solace (event broker) APIs
- **solace-show-in-devportal** — Show a published Solace API in the Developer Portal
  - `solace/SolaceTestCase.java:896`
- **solace-generate-keys** — Generate keys for a Solace subscription application
  - `solace/SolaceTestCase.java:958`
- **solace-new-subscription** — Create a new subscription for a Solace API
  - `solace/SolaceTestCase.java:971`
- **solace-definition-import** — Import a Solace AsyncAPI definition and create an API
  - `solace/SolaceTestCase.java:995`
- **solace-deploy-to-broker** — Deploy a created Solace API into the Solace broker
  - `solace/SolaceTestCase.java:1041`
- **solace-undeploy-from-broker** — Undeploy a Solace API from the Solace broker
  - `solace/SolaceTestCase.java:1055`
- **solace-retire-lifecycle-undeploys** — Retiring a deployed Solace API undeploys it from the broker
  - `solace/SolaceTestCase.java:1088`
- **solace-delete-undeploys** — Deleting a deployed Solace API undeploys it from the broker
  - `solace/SolaceTestCase.java:1174`

### API Configuration & Policies
#### Operation policies — common (reusable) policy management
- **add-common-operation-policy** — Add a common (reusable) operation policy
  - `operationPolicy/OperationPolicyTestCase.java:152`
- **export-common-operation-policy** — Export a common operation policy
  - `operationPolicy/OperationPolicyTestCase.java:171`
- **export-non-existing-common-policy** — Reject export of a non-existing common policy
  - `operationPolicy/OperationPolicyTestCase.java:256`
- **delete-common-operation-policy** — Delete a common operation policy
  - `operationPolicy/OperationPolicyTestCase.java:263`
- **import-common-operation-policy** — Import a common operation policy
  - `operationPolicy/OperationPolicyTestCase.java:273`
- **import-existing-common-policy** — Reject import of an already-existing common policy
  - `operationPolicy/OperationPolicyTestCase.java:288`
- **import-invalid-common-policy** — Reject import of an invalid common policy
  - `operationPolicy/OperationPolicyTestCase.java:297`
- **export-common-policy-json-definition** — Export a common policy with a JSON policy definition
  - `operationPolicy/OperationPolicyTestCase.java:356`
- **add-common-policy-yaml** — Add a common operation policy via a YAML specification file
  - `operationPolicy/OperationPolicyTestCase.java:735`
- **delete-common-policy-yaml** — Delete a YAML-defined common operation policy
  - `operationPolicy/OperationPolicyTestCase.java:756`

#### Operation policies — API-specific policy management
- **add-api-specific-policy** — Add an API-specific operation policy
  - `operationPolicy/OperationPolicyTestCase.java:412`
- **add-api-specific-policy-duplicate-name** — Reject adding an API-specific policy with a duplicate name
  - `operationPolicy/OperationPolicyTestCase.java:431`
- **delete-api-specific-policy** — Delete an API-specific operation policy
  - `operationPolicy/OperationPolicyTestCase.java:444`
- **add-api-specific-policy-yaml** — Add an API-specific policy via a YAML definition
  - `operationPolicy/OperationPolicyTestCase.java:703`
- **delete-api-specific-policy-yaml** — Delete a YAML-defined API-specific policy
  - `operationPolicy/OperationPolicyTestCase.java:724`
- **clone-common-policy-to-api-level** — Validate cloning of a common policy to API level on update
  - `operationPolicy/OperationPolicyTestCase.java:572`

#### Operation policies — attachment & invocation behavior
- **invoke-api-before-adding-policy** — Baseline invocation before attaching a (log mediation) policy
  - `operationPolicy/OperationPolicyTestCase.java:454`
- **invoke-api-after-adding-add-header-policy** — Verify add-header policy effect on invocation
  - `operationPolicy/OperationPolicyTestCase.java:466`
- **fresh-api-root-path-operation-policy** — Attach operation policy to a fresh API root-path operation
  - `operationPolicy/OperationPolicyTestCase.java:509`
- **policy-addition-missing-attributes** — Reject policy attachment with missing required attributes
  - `operationPolicy/OperationPolicyTestCase.java:586`
- **policy-not-supported-flow** — Reject attaching a policy to an unsupported flow
  - `operationPolicy/OperationPolicyTestCase.java:606`
- **new-version-after-adding-policy** — Create a new API version after adding an operation policy
  - `operationPolicy/OperationPolicyTestCase.java:626`
- **invoke-after-multiple-policies** — Verify behavior with multiple operation policies attached
  - `operationPolicy/OperationPolicyTestCase.java:667`

#### Operation policies — secret attributes
- **add-policy-with-secrets** — Add an API-specific policy with secret attributes
  - `operationPolicy/OperationPolicyTestCase.java:767`
- **invoke-before-secret-policy** — Baseline invocation before attaching a secret-attribute policy
  - `operationPolicy/OperationPolicyTestCase.java:785`
- **invoke-after-secret-policy** — Verify behavior after attaching a secret-attribute policy
  - `operationPolicy/OperationPolicyTestCase.java:795`
- **retrieve-secret-policy-attributes** — Retrieve secret policy attributes (masked)
  - `operationPolicy/OperationPolicyTestCase.java:821`
- **update-secret-policy-preserve** — Update secret policy attributes with preserve option
  - `operationPolicy/OperationPolicyTestCase.java:835`
- **version-with-secret-policy** — Create a new version carrying secret-attribute policy
  - `operationPolicy/OperationPolicyTestCase.java:860`
- **delete-policy-with-secrets** — Delete an API-specific policy with secret attributes
  - `operationPolicy/OperationPolicyTestCase.java:893`

#### Operation policies — JWT claim-based access validator
- **jwt-claim-policy-before-adding** — Baseline invocation before adding the JWT-claim access validator policy
  - `operationPolicy/JWTClaimBasedAccessValidatorPolicyTestCase.java:90`
- **jwt-claim-policy-valid** — Allow invocation when JWT claim matches the policy
  - `operationPolicy/JWTClaimBasedAccessValidatorPolicyTestCase.java:102`
- **jwt-claim-policy-invalid-claim-name** — Reject when configured claim name is invalid
  - `operationPolicy/JWTClaimBasedAccessValidatorPolicyTestCase.java:135`
- **jwt-claim-policy-invalid-claim-value** — Reject when claim value does not match
  - `operationPolicy/JWTClaimBasedAccessValidatorPolicyTestCase.java:168`
- **jwt-claim-policy-valid-regex** — Allow invocation when JWT claim matches a valid regex
  - `operationPolicy/JWTClaimBasedAccessValidatorPolicyTestCase.java:201`
- **jwt-claim-policy-invalid-regex** — Reject when claim does not match the regex
  - `operationPolicy/JWTClaimBasedAccessValidatorPolicyTestCase.java:236`
- **jwt-claim-policy-inverted-validation** — Reject/allow with inverted (negated) validation
  - `operationPolicy/JWTClaimBasedAccessValidatorPolicyTestCase.java:271`

#### Mediation sequences / dynamic endpoint
- **dynamic-default-endpoint-invocation** — Invoke API routed through a default/dynamic endpoint (sequence-based)
  - `sequence/DefaultEndpointTestCase.java:63`

#### Schema validation (request/response against OpenAPI schema)
- **schema-validation-invalid-request-body** — Reject request with an invalid request body
  - `schemaValidation/SchemaValidationTestCase.java:111`
- **schema-validation-missing-required-headers** — Reject request missing required headers
  - `schemaValidation/SchemaValidationTestCase.java:123`
- **schema-validation-with-required-headers** — Allow request that includes required headers
  - `schemaValidation/SchemaValidationTestCase.java:131`
- **schema-validation-invalid-response-body** — Reject/flag invalid response body
  - `schemaValidation/SchemaValidationTestCase.java:142`
- **schema-validation-valid-request-body** — Allow valid request body
  - `schemaValidation/SchemaValidationTestCase.java:152`
- **schema-validation-valid-response-body** — Allow valid response body
  - `schemaValidation/SchemaValidationTestCase.java:164`
- **schema-validation-unsecured-resource** — Validate valid requests for unsecured API resources
  - `schemaValidation/SchemaValidationTestCase.java:171`

#### Custom headers & header handling (gateway/transport)
- **transport-headers-property** — Honor TRANSPORT_HEADERS mediation property on responses
  - `header/APIMANAGER3357ContentTypeTestCase.java:65`
- **duplicate-transfer-encoding-header** — Read duplicate Transfer-Encoding header in the response
  - `header/APIMANAGER3614DuplicateTransferEncodingHeaderTestCase.java:73`
- **backend-tls-version** — Use correct TLS version on gateway-to-backend calls
  - `header/APIMANAGER4568TLSTestCase.java:75`
- **force-http-content-length** — Honor FORCE_HTTP_CONTENT_LENGTH property
  - `header/ContentLengthHeaderTestCase.java:81`
- **preserve-charset-in-content-type** — Preserve charset in the Content-Type header
  - `header/ESBJAVA3447PreserveCharsetInContentTypeTestCase.java:70`
- **auth-header-order** — Preserve Authorization header ordering
  - `header/ESBJAVA5121CheckAuthHeaderOrderTestCase.java:65`
- **gateway-header-splitting** — Handle gateway header splitting correctly
  - `header/HeaderSplitingTestCase.java:60`
- **publish-new-copy-require-resubscription** — Publish new API copy with require-re-subscription (header context dup-handling)
  - `header/DuplicateHeaderTestCase.java:117`

##### Custom auth/API-key header configuration
- **system-wide-custom-auth-header** — Set a system-wide custom Authorization header for all APIs
  - `header/CustomHeaderTestCase.java:117`
- **invoke-with-default-apikey-header** — Invoke an API using the default API key header
  - `header/CustomHeaderTestCase.java:148`
- **invoke-with-default-apikey-header-opaque** — Invoke with default API key header using an opaque key
  - `header/CustomHeaderTestCase.java:188`
- **invoke-with-custom-apikey-header** — Invoke an API using a custom API key header
  - `header/CustomHeaderTestCase.java:219`
- **invoke-with-custom-apikey-header-opaque** — Invoke with custom API key header using an opaque key
  - `header/CustomHeaderTestCase.java:266`

##### CORS header handling
- **cors-allow-credentials-specific-origin** — Set Access-Control-Allow-Credentials for a specific origin
  - `header/CORSAccessControlAllowCredentialsHeaderTestCase.java:92`
- **cors-sdk-generation** — Generate all supported SDKs (alongside CORS config)
  - `header/CORSAccessControlAllowCredentialsHeaderTestCase.java:178`
- **cors-options-from-backend** — Return CORS headers in OPTIONS response routed from backend
  - `header/CORSBackendTrafficRouteTestCase.java:105`
- **cors-preflight-response-headers** — Include CORS headers in pre-flight (OPTIONS) response
  - `header/CORSHeadersTestCase.java:149`
- **cors-response-headers** — Include CORS headers in normal responses
  - `header/CORSHeadersTestCase.java:187`
- **cors-enable-and-verify** — Enable CORS on an API and verify headers
  - `header/CORSHeadersTestCase.java:214`
- **cors-via-swagger-and-verify** — Enable CORS through swagger config and verify
  - `header/CORSHeadersTestCase.java:266`

_Setup/helper (non-test) classes: `header/CORSAccessControlAllowCredentialsHeaderSetup.java`, `header/util/SimpleSocketServer.java`._

### API Definitions & Import/Export
#### OpenAPI (OAS) definition lifecycle
- **oas-create-api** — Create an API (OAS-backed)
  - `oas/OASTestCase.java:71`
- **oas-update-api** — Update API metadata
  - `oas/OASTestCase.java:86`
- **oas-update-api-definition** — Update the API's OpenAPI definition
  - `oas/OASTestCase.java:101`
- **oas-add-advance-configs** — Add advanced configs to the API definition
  - `oas/OASTestCase.java:143`
- **oas-import-api-definition** — Import an OpenAPI definition
  - `oas/OASTestCase.java:175`
- **oas-import-unsupported-server-blocks** — Validate/reject import with unsupported server blocks
  - `oas/OASTestCase.java:219`
- **oas-validate-empty-resource-path** — Validate API definition with empty resource paths
  - `oas/OASTestCase.java:246`
- **oas-import-empty-resource-path** — Import API definition with empty resource paths
  - `oas/OASTestCase.java:258`
- **oas-update-empty-resource-path** — Update API definition with empty resource paths
  - `oas/OASTestCase.java:280`

_Helper (non-test) classes: `oas/OAS2Utils.java`, `oas/OAS3Utils.java`, `oas/OASBaseUtils.java`._

### Duplicate coverage (within this section)
- **GraphQL JWT/OAuth app invocation** — covered by `graphql/GraphqlTestCase.java` (testInvokeGraphqlAPIUsingJWTApplication / OAuthApplication) and `graphqlqueryanalysis/GraphQLQueryAnalysisTest.java` (same method names). The query-analysis class re-runs the basic JWT/OAuth invocation flow as setup for complexity assertions, so the plain-invocation behavior is duplicated.
- **GraphQL complexity/depth enforcement** — partially overlapping between `graphqlqueryanalysis/GraphQLQueryAnalysisTest.java` (add/view complexity values) and `graphql/GraphqlSubscriptionTestCase.java` (testGraphQLAPIInvocationForComplexity / ForDepth, which actually enforce the limits at invocation). Configuration vs enforcement split across two classes.
- **WebSocket publish + JWT-app-subscription boilerplate** — `publishWebSocketAPI` and `testWebSocketAPIJWTApplicationSubscription` are repeated near-verbatim across `websocket/WebSocketAPITestCase.java`, `WebSocketAPIScopeTestCase.java`, `WebSocketAPICorsValidationTestCase.java`, and `WebSocketAPIInvocationWithTracingTestCase.java` as setup steps; only the final assertion (scopes/CORS/tracing) differs.
- **WebSub publish + app-subscription boilerplate** — `testPublishWebSubApi` / `testWebSubApiApplicationSubscription` recur as setup in all six WebSub classes (`WebSubAPITestCase`, `FailedWebSubSubscriptionTestCase`, `LeaseTimeSubscriptionTestCase`, `MultipleWebSubSubcriptionTestCase`, `SecretValidationTestCase`, `ThrottlingTestCase`); each class adds one distinct feature (failure handling, lease time, multi-sub, secret/HMAC, throttling).
- **Async API create/import/subscribe** — `AsyncAPITestCase.java` and `AsyncAPITestWithValidationCase.java` share the create-without-advertise-only, import-publish, and app-subscription behaviors; the WithValidation class is a superset adding V3 and invalid-definition validation.

## Part E — Legacy TestNG Coverage Map

Scope: `restapi/` (excluding `restapi/admin/`), `rest/`, `analytics/`, `logging/`, `stats/`, `server/`, `listener/`, `search/`, `json/`, `prototype/`, `samples/`, `ui/`, `resources/`, `mcp/`, `aiapi/`.

### AI APIs & MCP

#### AI Service Providers (LLM provider config)
- **list-predefined-providers** — Retrieve built-in AI service providers and validate expected providers/properties
  - `aiapi/AIAPITestCase.java:195`
- **add-custom-provider-no-auth** — Add a custom AI service provider with no auth
  - `aiapi/AIAPITestCase.java:240`
- **get-custom-provider** — Retrieve a provider and validate name/api-version
  - `aiapi/AIAPITestCase.java:260`
- **update-custom-provider** — Update provider to apikey auth and verify config
  - `aiapi/AIAPITestCase.java:333`
- **list-provider-models** — Retrieve models for an AI service provider
  - `aiapi/AIAPITestCase.java:692`

#### AI API Lifecycle & Invocation
- **unsecured-ai-api-create-publish** — Create/deploy/publish AI API with unsecured provider
  - `aiapi/AIAPITestCase.java:276`
- **unsecured-ai-api-invoke** — Invoke unsecured AI API (token, and opaque API key)
  - `aiapi/AIAPITestCase.java:285`, `aiapi/AIAPITestCase.java:304`
- **secured-ai-api-create-publish** — Create/deploy/publish secured (Mistral) AI API
  - `aiapi/AIAPITestCase.java:358`
- **secured-ai-api-invoke** — Invoke secured AI API (token, and opaque API key)
  - `aiapi/AIAPITestCase.java:367`, `aiapi/AIAPITestCase.java:394`
- **gemini-unlimited-tier-disabled** — Gemini 1.1.0 create/publish + invoke + throttle-tier validation when `enable_unlimited_tier=false`
  - `aiapi/GeminiAPIUnlimitedTierDisabledTestCase.java:150`, `aiapi/GeminiAPIUnlimitedTierDisabledTestCase.java:160`, `aiapi/GeminiAPIUnlimitedTierDisabledTestCase.java:188`

#### AI API Endpoints (multi-endpoint CRUD)
- **endpoint-crud** — Add/get-all/get-one/update/delete AI API endpoints (prod + sandbox)
  - `aiapi/AIAPITestCase.java:422`, `aiapi/AIAPITestCase.java:475`, `aiapi/AIAPITestCase.java:519`, `aiapi/AIAPITestCase.java:554`, `aiapi/AIAPITestCase.java:605`, `aiapi/AIAPITestCase.java:650`

#### AI API Routing Policies
- **round-robin-model-policy** — Invoke AI API after adding round-robin model policy
  - `aiapi/AIAPITestCase.java:725`
- **failover-policy-new-version** — Create new API version with failover policy
  - `aiapi/AIAPITestCase.java:810`

#### MCP Servers
- **mcp-from-openapi** — Create MCP server from an OpenAPI definition
  - `mcp/MCPServerTestCase.java:495`
- **mcp-direct-backend-revision-deploy** — Create/deploy/validate revision (direct-backend subtype)
  - `mcp/MCPServerTestCase.java:524`
- **mcp-direct-backend-subscribe-invoke** — Publish/subscribe/invoke + visibility (direct-backend)
  - `mcp/MCPServerTestCase.java:543`
- **mcp-from-existing-api** — Create MCP server using an existing API
  - `mcp/MCPServerTestCase.java:597`
- **mcp-existing-api-revision-deploy** — Revision deploy (existing-API subtype)
  - `mcp/MCPServerTestCase.java:647`
- **mcp-existing-api-subscribe-invoke** — Subscribe/invoke + visibility (existing-API)
  - `mcp/MCPServerTestCase.java:664`
- **mcp-from-third-party-proxy** — Create MCP server proxying a third-party MCP server
  - `mcp/MCPServerTestCase.java:696`
- **mcp-proxy-revision-deploy** — Revision deploy (proxy subtype)
  - `mcp/MCPServerTestCase.java:738`
- **mcp-proxy-subscribe-invoke** — Subscribe/invoke (proxy subtype)
  - `mcp/MCPServerTestCase.java:759`
- **mcp-tool-operations** — Update tool operations for direct-backend / existing-API / proxy subtypes
  - `mcp/MCPServerTestCase.java:789`, `mcp/MCPServerTestCase.java:852`, `mcp/MCPServerTestCase.java:899`
- **mcp-scopes** — Update MCP server scopes and validate invocation (existing-API, proxy)
  - `mcp/MCPServerTestCase.java:943`, `mcp/MCPServerTestCase.java:1010`
- **mcp-throttling-proxy** — Throttling for proxy subtype (currently commented-out `@Test`)
  - `mcp/MCPServerTestCase.java:1076`

### REST API Surface & Service Catalog

#### Publisher/Store API Object CRUD (REST-driven, data-driven)
- **api-handling** — Generic API CRUD handling via REST API
  - `restapi/testcases/APITestCase.java:59`
- **api-lifecycle-change** — API lifecycle state changes via REST API
  - `restapi/testcases/APILifecycleTestCase.java:60`
- **api-with-scopes-update-template** — Create API with scopes then update resource template
  - `restapi/testcases/APIMANAGER4877CreateAPIWithScopesAndUpdateTemplateTestCase.java:59`
- **api-update-without-thumbnail** — Update API lacking thumbnail attribute and API summary
  - `restapi/testcases/APIMANAGER5872UpdateAPIWithoutThumbnailValueAndAPISummaryTestCase.java:60`
- **application-handling** — Application CRUD; (add-with-groupId variant commented out)
  - `restapi/testcases/ApplicationTestCase.java:59`, `restapi/testcases/ApplicationTestCase.java:77`
- **application-custom-attributes** — Application handling with custom attributes
  - `restapi/testcases/ApplicationWithCustomAttributesTestCase.java:59`
- **application-regenerate-consumer-secret** — Regenerate OAuth consumer secret for an application
  - `restapi/testcases/ApplicationRegenerateConsumerSecretTestCase.java:66`
- **application-scope-retrieval** — Validate REST API for retrieving application scopes
  - `restapi/testcases/ApplicationScopeValidationTestCase.java:63`
- **environment-listing** — API environment object handling
  - `restapi/testcases/EnvironmentTestCase.java:59`
- **subscription-handling** — API subscription CRUD
  - `restapi/testcases/SubscriptionTestCase.java:60`
- **multiple-subscriptions** — Multiple subscriptions to APIs
  - `restapi/testcases/MultipleSubscriptionsTestCase.java:55`
- **subscription-workflow-ref-id** — Returns workflow external reference id from subscriptions REST API (on-hold workflow)
  - `restapi/testcases/OnHoldSubscriptionWorkflowIdTestCase.java:87`
- **tag-handling** — API tags CRUD
  - `restapi/testcases/TagTestCase.java:60`
- **tier-handling** — Throttling tier CRUD
  - `restapi/testcases/TierTestCase.java:60`

#### OAuth App / Key Management via REST
- **keygen-contains-granttypes-callback** — Key generation response includes grant types + callback URL
  - `restapi/GIT_1628_OAuthAppUpdateViaRestApiTestCase.java:104`
- **token-gen-disabled-granttypes** — Password / client-credentials token generation when grant type not enabled
  - `restapi/GIT_1628_OAuthAppUpdateViaRestApiTestCase.java:153`, `restapi/GIT_1628_OAuthAppUpdateViaRestApiTestCase.java:171`
- **update-granttypes-callback** — Update and retrieve grant types + callback URL; verify keys
  - `restapi/GIT_1628_OAuthAppUpdateViaRestApiTestCase.java:186`, `restapi/GIT_1628_OAuthAppUpdateViaRestApiTestCase.java:251`, `restapi/GIT_1628_OAuthAppUpdateViaRestApiTestCase.java:285`
- **keygen-default-granttypes** — Key generation returns all grant types when request omits them
  - `restapi/GIT_1628_OAuthAppUpdateViaRestApiTestCase.java:326`

#### URL/Name Encoding & API Detail Retrieval
- **url-encoded-api-name** — Publisher API detail + lifecycle-to-publish for hyphenated API name
  - `restapi/GIT_1638_UrlEncodedApiNameTestCase.java:125`, `restapi/GIT_1638_UrlEncodedApiNameTestCase.java:151`

#### Gateway REST API
- **gateway-rest-api** — Gateway REST API (comment/rating per description) end-to-end
  - `restapi/GatewayRestAPITestCase.java:60`

#### Service Catalog REST API
- **service-crud** — Create/get-by-UUID/update/delete service via Service Catalog REST API
  - `restapi/ServiceCatalogRestAPITestCase.java:84`, `restapi/ServiceCatalogRestAPITestCase.java:155`, `restapi/ServiceCatalogRestAPITestCase.java:270`, `restapi/ServiceCatalogRestAPITestCase.java:439`
- **service-search** — Search services in catalog
  - `restapi/ServiceCatalogRestAPITestCase.java:183`
- **service-definition-retrieval** — Get service definition by UUID
  - `restapi/ServiceCatalogRestAPITestCase.java:252`
- **service-import-export** — Import and export service
  - `restapi/ServiceCatalogRestAPITestCase.java:320`, `restapi/ServiceCatalogRestAPITestCase.java:362`
- **service-to-api** — Create an API from a catalog service via Publisher REST API
  - `restapi/ServiceCatalogRestAPITestCase.java:378`
- **service-usage** — Get service usage by UUID
  - `restapi/ServiceCatalogRestAPITestCase.java:418`

#### Content Search
- **basic-content-search** — Search API content
  - `restapi/ContentSearchTestCase.java:122`
- **document-content-search** — Search inside API documentation content
  - `restapi/ContentSearchTestCase.java:185`
- **content-search-access-control** — Content search honors access control
  - `restapi/ContentSearchTestCase.java:242`
- **content-search-store-visibility** — Content search honors store visibility
  - `restapi/ContentSearchTestCase.java:308`

#### REST Request Robustness & Security
- **doc-api-parameter-tampering** — Tampered parameters must not leak stack trace in DocAPI response
  - `rest/DocAPIParameterTamperingTest.java:66`
- **malformed-request** — Malformed POST with message building must not break the system
  - `rest/MalformedRequestTest.java:55`
- **paginated-search-multi-status** — Paginated API count correct across multiple lifecycle statuses
  - `rest/SearchPaginatedAPIsWithMultipleStatusTestCase.java:67`

#### URI Template Encoding (REST resource matching)
- **uri-template-percent-encoding** — Query/path params with percent-encoding and escaped variants
  - `rest/UriTemplateReservedCharacterEncodingTest.java:56`, `rest/UriTemplateReservedCharacterEncodingTest.java:76`, `rest/UriTemplateReservedCharacterEncodingTest.java:97`, `rest/UriTemplateReservedCharacterEncodingTest.java:118`
- **uri-template-param-decoding** — Param decoding for space/trailing-%/plus/escaped-at-expansion/full-URL cases
  - `rest/UriTemplateReservedCharacterEncodingTest.java:139`, `rest/UriTemplateReservedCharacterEncodingTest.java:167`, `rest/UriTemplateReservedCharacterEncodingTest.java:195`, `rest/UriTemplateReservedCharacterEncodingTest.java:223`, `rest/UriTemplateReservedCharacterEncodingTest.java:251`
- **rest-uri-template-mapping** — REST URI template URL mapping (comma-separated variant commented out)
  - `rest/URLMappingRESTTestCase.java:80`

#### API Resource Scopes
- **add-scope-to-resource** — Add a scope to an API resource
  - `resources/APIResourceModificationTestCase.java:69`

### DevPortal / Store & Search

#### DevPortal Search
- **devportal-api-search** — DevPortal API search using filters
  - `search/DevPortalSearchTest.java:158`
- **devportal-subscription-mgmt-search** — DevPortal subscription-management API search using filters
  - `search/DevPortalSearchTest.java:253`

#### Prototyped API Store Visibility
- **prototyped-api-store-visibility** — Prototype-deployed API visible in Store
  - `prototype/APIM23VisibilityOfPrototypedAPIInStoreTestCase.java:84`
- **prototyped-api-different-view-visibility** — Open saved design-stage API, deploy as prototype, verify Store/general visibility + tag list
  - `prototype/APIM24VisibilityOfPrototypedAPIOfDifferentViewInStoreTestCase.java:72`, `prototype/APIM24VisibilityOfPrototypedAPIOfDifferentViewInStoreTestCase.java:133`, `prototype/APIM24VisibilityOfPrototypedAPIOfDifferentViewInStoreTestCase.java:142`

### Gateway, Mediation & Invocation

#### Prototyped API Invocation & Mocking
- **prototype-endpoint-invoke** — Create prototype-endpoint API and invoke; demote to created and invoke
  - `prototype/PrototypedAPITestcase.java:106`, `prototype/PrototypedAPITestcase.java:191`
- **inline-prototype-mock-oas** — Inline prototype with generated mock for OAS3 and OAS2
  - `prototype/PrototypedAPITestcase.java:268`, `prototype/PrototypedAPITestcase.java:336`

#### JSON/XML Mediation
- **json-to-xml** — JSON to XML conversion mediation
  - `json/ESBJAVA3380TestCase.java:103`

### Analytics, Logging & Observability

#### Analytics (ELK / Log Analytics)
- **log-analytics-invocation** — Enable APIM Log Analytics and invoke an API
  - `analytics/APIMAnalyticsTest.java:110`
- **elk-analytics-respond-mediator** — ELK analytics with a Respond Mediator operation policy
  - `analytics/ELKAnalyticsWithRespondMediatorTestCase.java:107`

#### Per-API / Per-Resource Logging
- **per-api-logging** — HTTP request to per-API logging-enabled API
  - `logging/APILoggingTest.java:106`
- **per-resource-logging** — Per-resource logging enabled API
  - `logging/APILoggingTest.java:194`
- **similar-template-logging** — Logging with similar URI templates
  - `logging/APILoggingTest.java:330`

#### Correlation Logging
- **default-correlation-config** — Retrieve default correlation configs via DevOps API
  - `logging/CorrelationLoggingTest.java:146`
- **enable-all-correlation** — Enable all correlation configs via DevOps API
  - `logging/CorrelationLoggingTest.java:171`
- **enable-specific-correlation** — Enable specific correlation configs via DevOps API
  - `logging/CorrelationLoggingTest.java:221`
- **persisted-correlation-config** — Correlation component configs persist
  - `logging/CorrelationLoggingTest.java:336`

#### Server Health Logs
- **oom-heapdump** — Verify a heapdump is generated on OOM
  - `logging/OOMLogsCheckTest.java:31`

#### Invocation Statistics Publishing
- **stat-api-creation** — Create sample API for stat publishing
  - `stats/APIInvocationStatPublisherTestCase.java:117`
- **stat-request-event** — API invocation produces request event (incl. anonymous)
  - `stats/APIInvocationStatPublisherTestCase.java:187`, `stats/APIInvocationStatPublisherTestCase.java:226`
- **stat-execution-time-event** — Execution-time event stream
  - `stats/APIInvocationStatPublisherTestCase.java:241`
- **stat-fault-event** — Fault event stream
  - `stats/APIInvocationStatPublisherTestCase.java:278`
- **stat-throttle-event** — Throttle event stream
  - `stats/APIInvocationStatPublisherTestCase.java:354`

### Platform & Server Management

#### Server Startup & OSGi
- **server-startup-logs** — Verify no server startup errors in logs
  - `server/mgt/APIMgtServerStartupTestCase.java:55`
- **osgi-unsatisfied-components** — Identify/store unsatisfied OSGi components
  - `server/mgt/OSGIServerBundleStatusTestCase.java:69`

#### TestNG Listeners (infra, no `@Test`)
- **suite-alter-listener** — `IAlterSuiteListener` that mutates the suite at runtime
  - `listener/APIMAlterSuiteListener.java:30`
- **test-execution-listener** — `ITestListener` capturing test lifecycle events
  - `listener/APIMTestExecutionListener.java:28`

#### Sample APIs (smoke)
- **pizzashack-sample** — PizzaShack sample API end-to-end
  - `samples/PizzaShackAPITestCase.java:66`
- **youtube-sample** — YouTube sample API end-to-end
  - `samples/YouTubeAPITestCase.java:74`

#### UI Smoke
- **ui-integration-runner** — Drives the UI integration test executor
  - `ui/APIMANAGERUIIntegrationTestRunner.java:89`

### Non-test helpers (in scope, no `@Test`)
- `restapi/RESTAPITestConstants.java` — constants only
- `restapi/utils/RESTAPITestUtil.java`, `restapi/utils/DataDrivenTestUtils.java` — data-driven REST test harness used by `restapi/testcases/*`
- `server/mgt/CarbonTestServerManager.java` — server bootstrap helper

### Duplicate coverage (within this section)
- **Prototype API Store visibility** — covered by `prototype/APIM23VisibilityOfPrototypedAPIInStoreTestCase.java` and `prototype/APIM24VisibilityOfPrototypedAPIOfDifferentViewInStoreTestCase.java` (both assert a prototype-deployed API appears in the Store; APIM24 adds the design-stage-then-deploy path and tag-list visibility).
- **Subscription via REST API** — covered by `restapi/testcases/SubscriptionTestCase.java`, `restapi/testcases/MultipleSubscriptionsTestCase.java`, and `restapi/testcases/OnHoldSubscriptionWorkflowIdTestCase.java` (single vs. multiple subscriptions vs. workflow-ref-id slice of the same subscription REST surface).
- **Content / search surface overlap** — `restapi/ContentSearchTestCase.java` (publisher content search) and `search/DevPortalSearchTest.java` (DevPortal search) and `rest/SearchPaginatedAPIsWithMultipleStatusTestCase.java` (paginated search) all exercise the API search surface from different portals; partial, not exact, duplication.
- **AI API invocation** — `aiapi/AIAPITestCase.java` and `aiapi/GeminiAPIUnlimitedTierDisabledTestCase.java` both cover AI API create/publish/invoke; Gemini case is a config-variant (unlimited tier disabled) of the same invocation flow.

## Legacy TestNG Coverage Map — Part F: `other/` package

Scope: `all-in-one-apim/modules/integration/tests-integration/tests-backend/src/test/java/org/wso2/am/integration/tests/other/`
This is a catch-all package dominated by regression tests (many named after bug/issue IDs). Each class is filed under the product capability it actually exercises.

### API Publishing & Lifecycle
#### Lifecycle state changes
- **DAO lifecycle** — Full API lifecycle state transition test exercising DAO/state persistence
  - `other/DAOTestCase.java:78`
- **API state-change workflow** — API lifecycle state change with approval/reject workflow (HTTP redirect workflow)
  - `other/APIStateChangeWorkflowTestCase.java:126`
#### Mandatory properties on update
- **Update guards** — API update is rejected without mandatory properties, succeeds with them (restart variant)
  - `other/MandatoryPropertiesTestWithRestart.java:82`
#### Endpoint type update
- **Endpoint type change** — Create/subscribe API then switch to sequence-backend endpoint and re-invoke
  - `other/APIEndpointTypeUpdateTestCase.java:83`
#### Endpoint listing / retrieval
- **Get all endpoints** — Retrieve all configured endpoints for an API
  - `other/APIM720GetAllEndPointsTestCase.java:139`
#### Name-case validation (regression)
- **Duplicate name different case** — Adding API with same name but different case is rejected
  - `other/APIMANAGER3226APINameWithDifferentCaseTestCase.java:77`

### API Configuration & Policies
#### API Categories
- **Category CRUD + attach** — Add/update/get/delete API category, validation (no name, special chars, duplicate), attach to API
  - `other/APICategoriesTestCase.java:70`
#### Advanced configurations
- **Tenant config CRUD** — Get/add/update tenant configuration; invalid-JWT update returns 401; get schema
  - `other/AdvancedConfigurationsTestCase.java:58`
#### Deny policies (blocking conditions)
- **Deny policy CRUD + scopes** — Add/get/update/delete API deny policies by context, application, user, IP, IP-range; invalid-input validation
  - `other/APIDenyPolicyTestCase.java:76`
#### Documentation
- **Document CRUD/listing** — Update document content/metadata and get all documents
  - `other/APIM714GetAllDocumentationTestCase.java:108`
#### Resource ordering (regression)
- **Swagger resource order** — Resource order is preserved in the generated swagger
  - `other/APIM4765ResourceOrderInSwagger.java:61`

### API Definitions & Import/Export
#### Standard API import/export
- **Export/import round-trip** — Export API (with endpoint security, thumbnail, restricted role) and re-import; state and invocation preserved
  - `other/APIImportExportTestCase.java:292`
#### SOAP / SOAP-to-REST import/export
- **SOAP API export/import** — Export and import Soap-to-REST APIs
  - `other/SOAPAPIImportExportTestCase.java:164`
#### WSDL-based creation
- **WSDL import + invoke** — Create API from WSDL (url, archive, multiple files), download WSDL, invoke SOAP backend
  - `other/WSDLImportTestCase.java:144`
- **WSDL hostname rewrite (regression)** — WSDL served reflects correct gateway hostname
  - `other/APIMANAGER5843WSDLHostnameTestCase.java:72`
#### SOAP-to-REST conversion
- **SOAP-to-REST resources/invoke** — Resource generation, in/out sequence validation, default & revisioned invocation, JWT/OAuth apps, scopes, content-type
  - `other/SoapToRestTestCase.java:178`
#### Security audit
- **Security audit report** — Retrieve API security audit report for an API
  - `other/APISecurityAuditTestCase.java:60`

### API Versioning
- **Copy new version** — Create a new version of an existing API
  - `other/CopyNewVersionTestCase.java:79`
- **New copy with default version** — Copy a version and mark it the default
  - `other/NewCopyWithDefaultVersion.java:76`
- **Same-version copy guard** — Copying to an already-existing version is handled correctly
  - `other/SameVersionAPITestCase.java:77`

### Applications & Subscriptions
#### Application CRUD
- **Application lifecycle** — Create (valid data + custom attributes), duplicate guard, list, update, remove application
  - `other/APIM678ApplicationCreationTestCase.java:122`
#### Subscriptions listing
- **Subscriptions by application/API** — List all subscriptions by application id and by API id
  - `other/APIM710AllSubscriptionsByApplicationTestCase.java:149`
- **Subscriptions via getAllSubscriptions (regression)** — List subscriptions via getAllSubscriptions / getAllSubscriptionsOfApplication (by id and name)
  - `other/APIMANAGER4480AllSubscriptionsByApplicationTestCase.java:130`
#### Delete subscribed API
- **Delete subscribed API from publisher** — Deleting a subscribed API from publisher/store; subscription stops working
  - `other/DeleteSubscribedApiFromPublisherTestCase.java:159`
#### Broken-API regression
- **Role change effect on subscribed API (regression)** — Changing role of a subscribed API does not break it in the store
  - `other/APIMANAGER4373BrokenAPIInStoreTestCase.java:102`

### Authentication, Tokens & Key Management
#### Scopes
- **API scope (admin/subscriber roles)** — Scope assignment/validation across roles, copy/update API with scopes, REST API scopes
  - `other/APIScopeTestCase.java:104`
- **API scope for tenants** — Same scope key reused across tenants
  - `other/APIScopeTestForTenantsTestCase.java:111`
- **Custom scope assignment for tenants** — Custom scope assignment for role in tenant context
  - `other/APICreationForTenantsTestCase.java:67`
- **Allowed/whitelisted scopes** — Token for whitelisted scopes invokes APIs
  - `other/AllowedScopesTestCase.java:160`
- **Allowed scopes (CORS disabled)** — Whitelisted-scope token invocation with CORS disabled
  - `other/AllowedScopesTestWithCorsDisabled.java:84`
- **Shared scopes CRUD** — Add/get/update/delete shared scope
  - `other/SharedScopeTestCase.java:59`
- **Shared scopes CRUD (restart)** — Shared scope CRUD with server restart
  - `other/SharedScopeTestWithRestart.java:70`
- **Token encryption + scopes** — Scopes work correctly when token encryption is enabled
  - `other/TokenEncryptionScopeTestCase.java:80`
#### Key generation
- **Key generation (PostgreSQL) (regression)** — Application creation and key generation against PGSQL
  - `other/APIMANAGER5327KeyGenerationWithPGSQLTestCase.java:86`
#### Token revocation
- **One-time-token revoke flow** — Revoke-one-time-token policy attached; JWT with matching scope revoked, others not
  - `other/RevokeOneTimeTokenFlowTestCase.java:161`
#### Invalid token handling
- **Invalid token invocation** — Calling API with invalid token returns proper failure message
  - `other/APIInvocationFailureTestCase.java:67`
- **Invalid token + large payload** — Unauthorized response when large payload sent with invalid token
  - `other/InvalidAuthTokenLargePayloadTestCase.java:138`

### Admin, Governance & Workflows
#### Workflows
- **Subscription workflow HTTP redirect** — Custom subscription-creation workflow returns redirect URI
  - `other/SubscriptionWFHTTPRedirectTest.java:80`
#### Notifications
- **New-API notification email** — Existing subscribers notified when a new API version is created
  - `other/NotificationTestCase.java:112`
- **Tenant claims / notification** — Tenant claim handling tied to notification feature
  - `other/TenantClaimsTestCase.java:118`
#### Access control
- **Publisher access control** — Publisher-side role-restricted API visibility/retrieval
  - `other/PublisherAccessControlTestCase.java:188`
- **DevPortal visibility** — DevPortal visibility restrictions on API, docs and openapi spec
  - `other/DevPortalVisibilityTestCase.java:181`
#### User store
- **Secondary user store case-insensitivity** — Add/delete roles of any case in secondary userstore
  - `other/SecondaryUserStoreCaseInsensitiveTestCase.java:87`

### Multi-tenancy & Organizations
- **Tenant domain validation** — Adding tenant with invalid domain is rejected
  - `other/TenantDomainValidationTestCase.java:68`

### Throttling & Rate Limiting
- **Prototyped API + monetization (regression)** — Disable advanced throttling, enable monetization, create prototyped API and verify store loads
  - `other/APIMANAGER5417PrototypedAPIsInMonetizedTestCase.java:94`

### Gateway, Mediation & Invocation
#### Mediation
- **Script mediator null handling (regression)** — Script mediator works when JSON response contains a null value
  - `other/ScriptMediatorTestCase.java:50`
- **messageType property in-sequence (regression)** — GET behaves correctly with in-sequence setting messageType property
  - `other/APIInvocationWithMessageTypeProperty.java:62`
#### HTTP method support
- **HTTP PATCH support** — Sending HTTP PATCH returns 200 OK from backend through gateway
  - `other/HttpPATCHSupportTestCase.java:78`
- **HTTP HEAD endpoint validation (regression)** — Validate endpoint that does not support HTTP HEAD
  - `other/APIMANAGER2611EndpointValidationTestCase.java:58`
- **HEAD request NPE (regression)** — HEAD request no longer triggers NPE; Location header correct
  - `other/GIT2231HeadRequestNPEErrorTestCase.java:59`
#### Load balancing
- **Load-balanced endpoints** — Round-robin across production and sandbox LB endpoints
  - `other/LoadBalancedEndPointTestCase.java:107`
#### Location header
- **Location header correctness** — Gateway rewrites Location header correctly
  - `other/LocationHeaderTestCase.java:69`
- **Relative-URL Location header** — Relative-URL Location header handled correctly
  - `other/RelativeUrlLocationHeaderTestCase.java:65`
#### Backend status-code passthrough (regression)
- **204 passthrough** — Backend returning 204 is passed through correctly
  - `other/APIMANAGER4464BackendReturningStatusCode204TestCase.java:86`
- **200 passthrough (regression)** — Backend returning 200 passthrough (ESBJAVA-4386/CARBON-15759)
  - `other/APIMANAGER4533BackendReturningStatusCode200TestCase.java:95`
#### Backend auth schemes
- **Digest authentication** — API with digest-authenticated backend works
  - `other/DigestAuthenticationTestCase.java:60`
- **NTLM authentication** — NTLM handshake exchange (Windows-only)
  - `other/NTLMTestCase.java:62`
#### Request-timeout regression
- **NPE after request timeout (regression)** — No NPE occurs after a backend request timeout
  - `other/APIM4312NPEAfterRequestTimeoutTestCase.java:86`

### Analytics, Logging & Observability — Error Handling
- **Auth-failure JSON format** — Auth failures returned in JSON format
  - `other/ErrorMessageTypeTestCase.java:59`
- **Error response security** — Error responses do not leak security-sensitive info
  - `other/ErrorResponseCheckTestCase.java:82`
- **Custom status message (regression)** — Custom status message preserved in 400 response
  - `other/APIMANAGER5326CustomStatusMsgTestCase.java:70`
- **Single-char query parameter error (regression)** — Error responses correct for single-character query parameter
  - `other/APIM5474SingleCharacterQueryParameter.java:58`
- **Error responses (regression APIMANAGER3965)** — Error/response behavior around API creation flow
  - `other/APIMANAGER3965TestCase.java:71`

### DevPortal / Store & Search
#### Search
- **Search by tag** — Store search by API tag and by group tag
  - `other/APISearchAPIByTagTestCase.java:147`
#### Pagination
- **Paginated search counts (regression)** — Pagination offset/limit counts correct in publisher and devportal
  - `other/APIMANAGER4081PaginationCountTestCase.java:115`
#### Tags & rating
- **Tags, comments, rating** — Comment/rating and tag verification on store
  - `other/TagsRatingTestCase.java:70`

### Platform & Server Management
- **Carbon application upload** — Upload a Carbon Application (.car file)
  - `other/CarbonAppUploadTestCase.java:51`
- **Multi-config server start (helper base)** — Base class: starts server with multiple configs, restores them (no @Test of its own)
  - `other/AdvancedConfigDeploymentConfig.java:42`
- **Web-app deployment (helper base)** — Base class: deploys/cleans backend web apps for advanced tests (no @Test of its own)
  - `other/AdvancedWebAppDeploymentConfig.java:42`

### Duplicate coverage (within this section)
- **Subscriptions listing** filed under 3 classes — `APIM710AllSubscriptionsByApplicationTestCase`, `APIMANAGER4480AllSubscriptionsByApplicationTestCase`, and (delete side) `DeleteSubscribedApiFromPublisherTestCase`; all overlap with mainstream Applications & Subscriptions tests in the main suite.
- **Scopes** are heavily re-tested — `APIScopeTestCase`, `APIScopeTestForTenantsTestCase`, `APICreationForTenantsTestCase`, `AllowedScopesTestCase`, `AllowedScopesTestWithCorsDisabled`, `SharedScopeTestCase`, `SharedScopeTestWithRestart`, `TokenEncryptionScopeTestCase` all exercise scope assignment/enforcement; likely duplicate the OAuth-scope / shared-scope tests in the main Authentication & Key Management suite.
- **API versioning copy** — `CopyNewVersionTestCase`, `NewCopyWithDefaultVersion`, `SameVersionAPITestCase` all re-test version-copy; likely duplicate the mainstream API Versioning tests.
- **Error/fault responses** — `ErrorMessageTypeTestCase`, `ErrorResponseCheckTestCase`, `APIMANAGER5326CustomStatusMsgTestCase`, `APIM5474SingleCharacterQueryParameter`, `APIMANAGER3965TestCase` are regression edge cases of mainstream gateway error-handling.
- **Backend status-code passthrough** — `APIMANAGER4464...204` and `APIMANAGER4533...200` are near-identical regression pairs of the same gateway passthrough behavior.
- **Location header** — `LocationHeaderTestCase`, `RelativeUrlLocationHeaderTestCase`, and `GIT2231HeadRequestNPEErrorTestCase` all verify Location-header rewriting in the gateway.
- **Import/Export** — `APIImportExportTestCase`, `SOAPAPIImportExportTestCase` overlap with the dedicated import-export tests in the main suite; `WSDLImportTestCase` + `SoapToRestTestCase` + `APIMANAGER5843WSDLHostnameTestCase` overlap on SOAP/WSDL handling.
- **Application CRUD** — `APIM678ApplicationCreationTestCase` likely duplicates mainstream Applications tests.
- Bug-ID regression classes broadly re-verify mainstream gateway invocation/mediation, scopes, subscriptions and error-handling features in specific edge cases — prime duplicate candidates.


## Coverage tree (capability → test class)

A condensed, scan-friendly view of the same data above: every capability and the distinct legacy
test classes that exercise it (merged across partitions, line numbers dropped). A class marked
`*` appears under **more than one capability** — a cross-cutting / likely-duplicate signal.
Use this to find where a new test belongs and which existing classes already touch that area.

```
Legacy integration tests
├── API Publishing & Lifecycle  (33)
│   ├── APIAccessibilityOfPublishedOldAPIAndPublishedCopyAPITestCase
│   ├── APICreationTestCase
│   ├── APIEndpointTypeUpdateTestCase
│   ├── APIM18CreateAnAPIThroughThePublisherRestAPITestCase *
│   ├── APIM514CreateAnAPIWithoutProvidingMandatoryFieldsTestCase
│   ├── APIM519CreateAnAPIThroughTheRestAPIWithoutLoggingInTestCase
│   ├── APIM520UpdateAnAPIThroughThePublisherRestAPITestCase
│   ├── APIM534GetAllTheAPIsCreatedThroughThePublisherRestAPITestCase
│   ├── APIM548CopyAnAPIToANewerVersionThroughThePublisherRestAPITestCase
│   ├── APIM570CheckIfAnOlderVersionOfTheAPIExistsThroughThePublisherRestAPITestCase
│   ├── APIM574ChangeTheStatusOfAnAPIToPrototypedThroughThePublisherRestAPITestCase
│   ├── APIM720GetAllEndPointsTestCase
│   ├── APIMANAGER3226APINameWithDifferentCaseTestCase
│   ├── APIMANAGER5337SubscriptionRetainTestCase
│   ├── APIMANAGER5834APICreationWithInvalidInputsTestCase
│   ├── APIManagerConfigurationChangeTest
│   ├── APIManagerConfigurationChangeTestSuite
│   ├── APIProductCreationTestCase *
│   ├── APIPublishingAndVisibilityInStoreTestCase
│   ├── APIStateChangeWorkflowTestCase
│   ├── AccessibilityOfBlockAPITestCase
│   ├── AccessibilityOfDeprecatedOldAPIAndPublishedCopyAPITestCase
│   ├── AccessibilityOfRetireAPITestCase
│   ├── CheckEmptyCORSConfigurationsTestCase
│   ├── CustomLifeCycleTestCase
│   ├── DAOTestCase
│   ├── EditAPIAndCheckUpdatedInformationTestCase
│   ├── MandatoryPropertiesTestWithRestart
│   ├── NewVersionUpdateTestCase
│   ├── PluggableVersioningStrategyTestCase
│   ├── RegistryLifeCycleInclusionTest
│   ├── TestCase
│   └── UpdateAPINullPointerTestCase
├── API Configuration & Policies  (47)
│   ├── APICategoriesTestCase
│   ├── APIDenyPolicyTestCase
│   ├── APIEndpointCertificateTestCase
│   ├── APIEndpointCertificateUsageTestCase
│   ├── APIInvocationWithSimilarResourcesAndDifferentVerbsTestCase
│   ├── APIM4765ResourceOrderInSwagger
│   ├── APIM634GetAllTheThrottlingTiersFromThePublisherRestAPITestCase
│   ├── APIM714GetAllDocumentationTestCase
│   ├── APIMANAGER3357ContentTypeTestCase
│   ├── APIMANAGER3614DuplicateTransferEncodingHeaderTestCase
│   ├── APIMANAGER4568TLSTestCase
│   ├── APIMGetAllSubscriptionThrottlingPolicies
│   ├── APIResourceWithSpecialCharactersInvocation
│   ├── APIResourceWithTemplateTestCase
│   ├── AddEditRemoveRESTResourceTestCase
│   ├── AddEndPointSecurityPerTypeTestCase
│   ├── AddNewHandlerAndInvokeAPITestCase
│   ├── AdvancedConfigurationsTestCase
│   ├── CORSAccessControlAllowCredentialsHeaderSetup
│   ├── CORSAccessControlAllowCredentialsHeaderTestCase
│   ├── CORSBackendTrafficRouteTestCase
│   ├── CORSHeadersTestCase
│   ├── ChangeAPIEndPointURLTestCase
│   ├── ChangeAPITagsTestCase
│   ├── ChangeAPITierAndTestInvokingTestCase
│   ├── ChangeApplicationTierAndTestInvokingTestCase
│   ├── ChangeAuthTypeOfResourceTestCase
│   ├── ChangeEndPointSecurityOfAPITestCase
│   ├── ChangeEndPointSecurityPerTypeTestCase
│   ├── ChangeResourceTierAndTestInvokingTestCase
│   ├── ChangeSubscriptionBusinessPlanForcefullyTestCase
│   ├── ContentLengthHeaderTestCase
│   ├── CustomHeaderTestCase
│   ├── DefaultEndpointTestCase
│   ├── DeleteTierAlreadyAttachedToAPITestCase
│   ├── DuplicateHeaderTestCase
│   ├── DynamicAPIContextTestCase
│   ├── ESBJAVA3447PreserveCharsetInContentTypeTestCase
│   ├── ESBJAVA5121CheckAuthHeaderOrderTestCase
│   ├── EditAPIContextAndCheckAccessibilityTestCase
│   ├── EditTiersXMLAndVerifyInPublisherTestCase
│   ├── GetLinterCustomRulesThroughThePublisherRestAPITestCase
│   ├── HeaderSplitingTestCase
│   ├── JWTClaimBasedAccessValidatorPolicyTestCase
│   ├── OperationPolicyTestCase
│   ├── SchemaValidationTestCase
│   └── SimpleSocketServer
├── API Security  (4)
│   ├── APISecurityMutualSSLCertificateChainValidationTestCase
│   ├── APISecurityTestCase
│   ├── AudienceValidationTestCase
│   └── DisableSecurityAndTryOutRESTResourceWithElkAnalyticsEnabledTestCase
├── API Visibility & Access Control  (6)
│   ├── APITagVisibilityByRoleTestCase
│   ├── APIVisibilityByDomainTestCase
│   ├── APIVisibilityByPublicTestCase
│   ├── APIVisibilityByRoleTestCase
│   ├── APIVisibilityWithDirectURLTestCase
│   └── UsersAndDocsInAPIOverviewTestCase
├── API Versioning  (13)
│   ├── APIM366PublishNewCopyGivenDeprecateOldVersionTestCase
│   ├── APIM370PublishNewCopyGivenRequireReSubscriptionTestCase
│   ├── APIM371PublishNewCopyGivenDeprecateOldVersionAndRequireReSubscriptionTestCase
│   ├── APIM372PublishNewCopyGivenDeprecateOldVersionTestCase
│   ├── APIM373PublishNewCopyGivenRequireReSubscriptionTestCase
│   ├── APIM374PublishNewCopyGivenDeprecateOldVersionAndRequireReSubscriptionTestCase
│   ├── AccessibilityOfOldAPIAndCopyAPIWithOutReSubscriptionTestCase
│   ├── AccessibilityOfOldAPIAndCopyAPIWithReSubscriptionTestCase
│   ├── CopyNewVersionTestCase
│   ├── DefaultVersionAPITestCase
│   ├── DefaultVersionWithScopesTestCase
│   ├── NewCopyWithDefaultVersion
│   └── SameVersionAPITestCase
├── API Definitions & Import/Export  (12)
│   ├── APIImportExportTestCase
│   ├── APIM18CreateAnAPIThroughThePublisherRestAPITestCase *
│   ├── APIMANAGER5843WSDLHostnameTestCase
│   ├── APISecurityAuditTestCase
│   ├── OAS2Utils
│   ├── OAS3Utils
│   ├── OASBaseUtils
│   ├── OASTestCase
│   ├── SDKGenerationTestCase
│   ├── SOAPAPIImportExportTestCase
│   ├── SoapToRestTestCase
│   └── WSDLImportTestCase
├── API Documentation  (6)
│   ├── APIM611AddDocumentationToAnAPIWithDocTypeHowToThroughPublisherRestAPITestCase
│   ├── APIM614AddDocumentationToAnAPIWithDocTypeSampleAndSDKThroughPublisherRestAPITestCase
│   ├── APIM620AddDocumentationToAnAPIWithDocTypeSampleAndSDKThroughPublisherRestAPITestCase
│   ├── APIM623AddDocumentationToAnAPIWithDocTypePublicForumThroughPublisherRestAPITestCase
│   ├── APIM625AddDocumentationToAnAPIWithDocTypeSupportForumThroughPublisherRestAPITestCase
│   └── APIM627AddDocumentationToAnAPIWithDocTypeOtherThroughPublisherRestAPITestCase
├── API Products  (3)
│   ├── APIProductCreationTestCase *
│   ├── APIProductLifecycleTest
│   └── APIProductRevisionTestCase
├── API Revisions  (1)
│   └── APIRevisionTestCase
├── Applications & Subscriptions  (15)
│   ├── APIM678ApplicationCreationTestCase
│   ├── APIM710AllSubscriptionsByApplicationTestCase
│   ├── APIMANAGER4373BrokenAPIInStoreTestCase
│   ├── APIMANAGER4480AllSubscriptionsByApplicationTestCase
│   ├── ApplicationAttributesTestCase
│   ├── ApplicationBlockSubscriptionTestCase
│   ├── ApplicationCallbackURLTestCase
│   ├── ApplicationConsumerSecretRegenerateTestCase
│   ├── ApplicationSharingConfig
│   ├── ApplicationSharingTestCase *
│   ├── ApplicationTestCase *
│   ├── CAPIMGT12CallBackURLOverwriteTestCase
│   ├── DeleteSubscribedApiFromPublisherTestCase
│   ├── MultipleClientSecretsTokenTestCase
│   └── SubscriptionValidationDisableTestCase
├── Authentication, Tokens & Key Management  (41)
│   ├── APICreationForTenantsTestCase
│   ├── APIInvocationFailureTestCase
│   ├── APIM34InvokeAPIWithSandboxTokenTestCase
│   ├── APIMANAGER5327KeyGenerationWithPGSQLTestCase
│   ├── APIScopeTestCase
│   ├── APIScopeTestForTenantsTestCase
│   ├── AllowedScopesTestCase
│   ├── AllowedScopesTestWithCorsDisabled
│   ├── ApplicationSharingTestCase *
│   ├── BackendJWTUtil
│   ├── ConsumerAppBasedJWTRevocation
│   ├── ExternalIDPJWTTestCase
│   ├── ExternalIDPJWTTestSuite
│   ├── FederatedUserJWTTestCase
│   ├── GrantTypeTokenGenerateTestCase
│   ├── InvalidAuthTokenLargePayloadTestCase
│   ├── InvalidTokenTestCase
│   ├── InvokeAPIWithVariousEndpointsAndTokensInSandboxEnvTestCase
│   ├── InvokeAPIWithVariousEndpointsAndTokensTestCase
│   ├── JWTDecodingTestCase
│   ├── JWTGenerator
│   ├── JWTGeneratorUtil
│   ├── JWTGrantTestCase
│   ├── JWTRevocationTestCase
│   ├── JWTTestCase
│   ├── JWTTestSuite
│   ├── MicroGWJWTRevocationTestCase
│   ├── OpenIDTokenAPITestCase
│   ├── PkceEnabledApplicationTestCase
│   ├── RefreshTokenTestCase
│   ├── RevokeOneTimeTokenFlowTestCase
│   ├── SharedScopeTestCase
│   ├── SharedScopeTestWithRestart
│   ├── TokenAPITestCase
│   ├── TokenEncryptionScopeTestCase
│   ├── TokenPersistenceExternalIDPJWTTestSuite
│   ├── TokenPersistenceJWTTestSuite
│   ├── TokenPersistenceTestSuite
│   ├── TokenPersistenceURLSafeJWTTestSuite
│   ├── URLSafeJWTTestCase
│   └── UrlSafeJWTTestSuite
├── DevPortal / Store & Search  (12)
│   ├── APIM23VisibilityOfPrototypedAPIInStoreTestCase
│   ├── APIM24VisibilityOfPrototypedAPIOfDifferentViewInStoreTestCase
│   ├── APIM638ValidateTheRoleOfAnExistingUserThroughThePublisherRestAPITestCase
│   ├── APIMANAGER4081PaginationCountTestCase
│   ├── APISearchAPIByTagTestCase
│   ├── DevPortalCommentTest
│   ├── DevPortalSearchTest
│   ├── EmailUserNameLoginTestCase
│   ├── LoginValidationTestCase
│   ├── PasswordChangeTestCase
│   ├── PublisherCommentTest
│   └── TagsRatingTestCase
├── Multi-tenancy & Organizations  (8)
│   ├── APIMISTenantSyncTestSuite
│   ├── APIMTenantCreationNotificationTestCase
│   ├── ConsumerOrganizationVisibilityTestCase
│   ├── CrossTenantSubscriptionTestCase
│   ├── CrossTenantSubscriptionTestSuite
│   ├── CrossTenantSubscriptionUpdateTestCase
│   ├── TenantDomainValidationTestCase
│   └── TenantManagementEvent
├── Throttling & Rate Limiting  (19)
│   ├── APIMANAGER5417PrototypedAPIsInMonetizedTestCase
│   ├── APIThrottlingTestCase
│   ├── APITierManagementTestCase
│   ├── AdvancedThrottlingPolicyTestCase
│   ├── ApplicationThrottlingPolicyTestCase
│   ├── ApplicationThrottlingResetTestCase
│   ├── BurstControlTestCase
│   ├── ConfigurableDefaultPolicyTestCase
│   ├── CustomThrottlingPolicyTestCase
│   ├── DenyPolicySearchTestCase
│   ├── GetThrottlingPoliciesTestCase
│   ├── HardThrottlingTestCase
│   ├── JWTBandwidthThrottlingTestCase
│   ├── JWTRequestCountThrottlingTestCase
│   ├── SubscriptionThrottlingPolicyTestCase
│   ├── ThrottlePolicyExportImportTestCase
│   ├── ThrottlingUtils
│   ├── UnlimitedTierDisabledConfig
│   └── UnlimitedTierDisabledTestCase
├── Admin, Governance & Workflows  (18)
│   ├── APIComplianceTestCase
│   ├── APISystemScopesTestCase
│   ├── ApplicationsSearchByNameOrOwnerTestCase
│   ├── ChangeApiProviderSecondaryUserStoreTestCase
│   ├── ChangeApiProviderTestCase
│   ├── DevPortalVisibilityTestCase
│   ├── EnvironmentTestCase *
│   ├── KeyManagersTestCase
│   ├── MCPComplianceTestCase
│   ├── NotificationTestCase
│   ├── OAuthApplicationOwnerUpdateTestCase
│   ├── PolicyMgtTestCase
│   ├── PublisherAccessControlTestCase
│   ├── RulesetMgtTestCase
│   ├── SecondaryUserStoreCaseInsensitiveTestCase
│   ├── SubscriptionWFHTTPRedirectTest
│   ├── TenantClaimsTestCase
│   └── WorkflowApprovalExecutorTest
├── Gateway, Mediation & Invocation  (16)
│   ├── APIInvocationWithMessageTypeProperty
│   ├── APIM4312NPEAfterRequestTimeoutTestCase
│   ├── APIMANAGER2611EndpointValidationTestCase
│   ├── APIMANAGER4464BackendReturningStatusCode204TestCase
│   ├── APIMANAGER4533BackendReturningStatusCode200TestCase
│   ├── DigestAuthenticationTestCase
│   ├── ESBJAVA3380TestCase
│   ├── GIT2231HeadRequestNPEErrorTestCase
│   ├── GatewayPolicyTestCase
│   ├── HttpPATCHSupportTestCase
│   ├── LoadBalancedEndPointTestCase
│   ├── LocationHeaderTestCase
│   ├── NTLMTestCase
│   ├── PrototypedAPITestcase
│   ├── RelativeUrlLocationHeaderTestCase
│   └── ScriptMediatorTestCase
├── GraphQL APIs  (6)
│   ├── GraphQLQueryAnalysisTest
│   ├── GraphqlSubscriptionTestCase
│   ├── GraphqlTestCase
│   ├── SubscriptionServerCreator
│   ├── SubscriptionWSClientImpl
│   └── SubscriptionWSServerImpl
├── Streaming APIs  (26)
│   ├── APIMANAGER5869WSGayewatURLTestCase
│   ├── AsyncAPITestCase
│   ├── AsyncAPITestSuite
│   ├── AsyncAPITestWithValidationCase
│   ├── AsyncAPIValidationTestSuite
│   ├── CallbackServerServlet
│   ├── CallbackServerServlet2
│   ├── CallbackServerServletWithSubVerification
│   ├── FailedWebSubSubscriptionTestCase
│   ├── LeaseTimeSubscriptionTestCase
│   ├── MultipleWebSubSubcriptionTestCase
│   ├── SecretValidationTestCase
│   ├── ServerSentEventsAPITestCase
│   ├── SimpleSseReceiver
│   ├── SolaceTestCase
│   ├── SseServlet
│   ├── StreamingApiTestUtils
│   ├── ThrottlingTestCase
│   ├── WebSocketAPICorsValidationTestCase
│   ├── WebSocketAPIInvocationWithTracingTestCase
│   ├── WebSocketAPIScopeTestCase
│   ├── WebSocketAPITestCase
│   ├── WebSocketClientImpl
│   ├── WebSocketServerImpl
│   ├── WebSubAPITestCase
│   └── WebhookSender
├── AI APIs & MCP  (3)
│   ├── AIAPITestCase
│   ├── GeminiAPIUnlimitedTierDisabledTestCase
│   └── MCPServerTestCase
├── REST API Surface & Service Catalog  (25)
│   ├── APILifecycleTestCase
│   ├── APIMANAGER4877CreateAPIWithScopesAndUpdateTemplateTestCase
│   ├── APIMANAGER5872UpdateAPIWithoutThumbnailValueAndAPISummaryTestCase
│   ├── APIResourceModificationTestCase
│   ├── APITestCase
│   ├── ApplicationRegenerateConsumerSecretTestCase
│   ├── ApplicationScopeValidationTestCase
│   ├── ApplicationTestCase *
│   ├── ApplicationWithCustomAttributesTestCase
│   ├── ContentSearchTestCase
│   ├── DocAPIParameterTamperingTest
│   ├── EnvironmentTestCase *
│   ├── GIT_1628_OAuthAppUpdateViaRestApiTestCase
│   ├── GIT_1638_UrlEncodedApiNameTestCase
│   ├── GatewayRestAPITestCase
│   ├── MalformedRequestTest
│   ├── MultipleSubscriptionsTestCase
│   ├── OnHoldSubscriptionWorkflowIdTestCase
│   ├── SearchPaginatedAPIsWithMultipleStatusTestCase
│   ├── ServiceCatalogRestAPITestCase
│   ├── SubscriptionTestCase
│   ├── TagTestCase
│   ├── TierTestCase
│   ├── URLMappingRESTTestCase
│   └── UriTemplateReservedCharacterEncodingTest
├── Analytics, Logging & Observability  (11)
│   ├── APIInvocationStatPublisherTestCase
│   ├── APILoggingTest
│   ├── APIM5474SingleCharacterQueryParameter
│   ├── APIMANAGER3965TestCase
│   ├── APIMANAGER5326CustomStatusMsgTestCase
│   ├── APIMAnalyticsTest
│   ├── CorrelationLoggingTest
│   ├── ELKAnalyticsWithRespondMediatorTestCase
│   ├── ErrorMessageTypeTestCase
│   ├── ErrorResponseCheckTestCase
│   └── OOMLogsCheckTest
├── Platform & Server Management  (10)
│   ├── APIMANAGERUIIntegrationTestRunner
│   ├── APIMAlterSuiteListener
│   ├── APIMTestExecutionListener
│   ├── APIMgtServerStartupTestCase
│   ├── AdvancedConfigDeploymentConfig
│   ├── AdvancedWebAppDeploymentConfig
│   ├── CarbonAppUploadTestCase
│   ├── OSGIServerBundleStatusTestCase
│   ├── PizzaShackAPITestCase
│   └── YouTubeAPITestCase
└── Non-test helpers (in scope, no `@Test`)  (4)
    ├── CarbonTestServerManager
    ├── DataDrivenTestUtils
    ├── RESTAPITestConstants
    └── RESTAPITestUtil
```

**Cross-cutting classes (5)** — appear under multiple capabilities (review for overlap):

- `APIM18CreateAnAPIThroughThePublisherRestAPITestCase` — API Definitions & Import/Export; API Publishing & Lifecycle
- `APIProductCreationTestCase` — API Products; API Publishing & Lifecycle
- `ApplicationSharingTestCase` — Applications & Subscriptions; Authentication, Tokens & Key Management
- `ApplicationTestCase` — Applications & Subscriptions; REST API Surface & Service Catalog
- `EnvironmentTestCase` — Admin, Governance & Workflows; REST API Surface & Service Catalog

