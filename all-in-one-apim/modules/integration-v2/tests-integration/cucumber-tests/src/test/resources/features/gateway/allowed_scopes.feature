@cleanup
Feature: Gateway Allowed-Scopes Enforcement

  Ports AllowedScopesTestCase. The block runs with the `[oauth] allowed_scopes = ["^device_.*", "openid",
  "scope1", "scope2"]` deployment.toml overlay (the allowed-scopes whitelist), and an API whose GET
  /customers/{id} operation requires scope1 or scope2 (both bound to the admin role). A token minted with a
  whitelisted, operation-matching scope (scope1 or scope2) invokes the resource successfully (200); a token
  carrying a scope that does NOT satisfy the operation binding — scope3, or no explicit scope (the default
  scope) — is refused at the gateway (403). Runs x2-tenant (super + tenant1) as each tenant's admin, whose
  role owns that tenant's scope bindings; scopes, APIs and the catalog are tenant-isolated, and the config
  overlay is container-global (applies to both tenants), so the block still runs thread-count=1. Needs the
  block backend (`initBackend`) for runtime invocation. Teardown via the per-scenario cleanup hook.

  @cap:gateway @feat:security-enforcement @type:regression @dep:publisher @legacy:AllowedScopesTestCase
  Scenario Outline: A whitelisted operation scope is enforced at the gateway as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"

    # Create the API (scope1/scope2 declared inline and bound to the GET operation), deploy and publish.
    When I put JSON payload from file "artifacts/payloads/create_apim_allowed_scopes_api.json" in context as "allowedScopesCreatePayload"
    And I create an "apis" resource with payload "allowedScopesCreatePayload" as "allowedScopesApiId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "allowedScopesRevPayload"
    """
    {"description":"allowed-scopes revision"}
    """
    And I make a request to create a revision for "apis" resource "allowedScopesApiId" with payload "allowedScopesRevPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "allowedScopesDeployPayload"
    """
    [{"name":"{{gatewayEnvironment}}","vhost":"localhost","displayOnDevportal":true}]
    """
    And I make a request to deploy revision "revisionId" of "apis" resource "allowedScopesApiId" with payload "allowedScopesDeployPayload"
    Then The response status code should be 201
    When I publish the "apis" resource with id "allowedScopesApiId"
    Then The lifecycle status of API "allowedScopesApiId" should be "Published"
    When I retrieve the "apis" resource with id "allowedScopesApiId"
    And I extract response field "context" and store it as "apiContext"

    # Subscribe an application, keyed for the password grant so we can mint scope-specific tokens.
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "generateApplicationKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateApplicationKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "apiSubscriptionPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "allowedScopesApiId" using application "createdAppId" with payload "apiSubscriptionPayload" as "subscriptionId"
    Then The response status code should be 201

    # scope1 — whitelisted AND satisfies the operation binding → 200. The ISSUED TOKEN is asserted too: legacy
    # pins both the granted scope and the token lifetime (the values the whitelist path must not alter), so a token
    # that invokes successfully while carrying the wrong scope or lifetime still fails here.
    # expires_in is 86400, NOT the legacy's 3600: this harness's base configuration sets it explicitly
    # (artifacts/configFiles/basic/deployment.toml, [oauth.token_validation] user_access_token_validity = 86400),
    # whereas legacy ran on the distribution default of 3600. Pinned to the CONFIGURED value — the assertion's
    # point is that the whitelist path issues a bounded token of exactly the configured lifetime.
    When I request an OAuth access token for the current user using password grant with scope "scope1"
    Then The response status code should be 200
    And The issued token scope list should include "scope1" and exclude "scope2,scope3"
    And The value of response field "expires_in" should be "86400"
    And I copy context value "generatedAccessToken" to "scope1Token"
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "scope1Token" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "{\"id\":123,\"name\":\"John\"}"

    # scope2 — whitelisted AND satisfies the operation binding → 200.
    When I request an OAuth access token for the current user using password grant with scope "scope2"
    Then The response status code should be 200
    And The issued token scope list should include "scope2" and exclude "scope1,scope3"
    And The value of response field "expires_in" should be "86400"
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "{\"id\":123,\"name\":\"John\"}"

    # The scope1 token STILL works after the scope2 token was issued — issuing a second, differently-scoped token
    # on the same client credentials must not invalidate the first (legacy's "check if scope1 token is valid" leg).
    # Without this, a key manager that revoked-and-replaced the previous token per client would pass every other
    # assertion here.
    When I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "scope1Token" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "{\"id\":123,\"name\":\"John\"}"

    # scope3 — granted in the token (it is not role-restricted) but does NOT satisfy the operation binding
    # (scope1/scope2) → refused at the gateway (403).
    When I request an OAuth access token for the current user using password grant with scope "scope3"
    Then The response status code should be 200
    And The issued token scope list should include "scope3" and exclude "scope1,scope2"
    And The value of response field "expires_in" should be "86400"
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403

    # No explicit scope — the token carries EXACTLY "default", which likewise lacks the operation scope → 403.
    When I request an OAuth access token for the current user using password grant with scope ""
    Then The response status code should be 200
    And The issued token scope list should include "default" and exclude "scope1,scope2,scope3"
    And The value of response field "expires_in" should be "86400"
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # PER-RESOURCE scope binding with WILDCARD PRECEDENCE — the FULL 5x5 matrix of AllowedScopesTestCase's
  # ExampleAPI (previously trimmed to 6 of the 25 cells; the trim is now reversed). Five resources on ONE API bind
  # five different scopes, and the gateway routes each incoming path to the MOST SPECIFIC matching operation:
  #   /echo/products/catalog/{categoryId}  exact            -> ScopeA   (probed with /echo/products/catalog/1)
  #   /echo/products/popular               exact            -> ScopeB
  #   /echo/products/*                     segment wildcard -> ScopeC   (probed with /echo/products/noexactmatch)
  #   /echo/orders                         exact            -> ScopeD
  #   /echo/*                              global wildcard  -> ScopeE   (probed with /echo/noexactmatch)
  # Every path routes to the node backend /echo/* which 200s for any subpath, so ONLY the gateway's per-resource
  # scope check distinguishes 200 from 403. Each of the five single-scope tokens is invoked against all five paths:
  # 200 on the ONE path that resolves to its own operation, 403 on the other four. Why all 25 cells rather than the
  # three distinct binding tiers: the discriminating property is the resolution ORDER between overlapping
  # operations, and an off-diagonal cell only exercises the pair it names — e.g. ScopeB-on-/echo/products/noexactmatch
  # and ScopeC-on-/echo/products/popular are the two directions of the exact-vs-segment-wildcard tie-break, and a
  # gateway that resolved that tie the wrong way would still pass the diagonal plus any single off-diagonal cell.
  #
  # NOT PORTED — the AllowedScopesTestWithCorsDisabled container. That legacy class asserts this SAME 5x5 status
  # matrix; diffing the two overlays, it differs ONLY by `[apim.cors] enable = false` (plus dropping an unrelated
  # `[apim] gateway_type` line). Every request here is a plain GET with NO Origin header, which the CORS handler
  # passes through untouched, so the whole matrix is byte-identical under either setting — it cannot distinguish the
  # configurations, and a dedicated container is real capacity for zero discrimination. The CORS handler's own
  # behaviour is covered by gateway/cors.feature. Residual risk, recorded rather than silently dropped: this does
  # not prove the 403 is produced with the CORS handler absent from the API's in-sequence.
  @cap:gateway @feat:security-enforcement @type:regression @dep:publisher @legacy:AllowedScopesTestCase @legacy:AllowedScopesTestWithCorsDisabled
  Scenario Outline: Per-resource scopes with wildcard precedence are enforced independently as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_scope_matrix_api.json" in context as "scopeMatrixCreatePayload"
    And I create an "apis" resource with payload "scopeMatrixCreatePayload" as "scopeMatrixApiId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "scopeMatrixRevPayload"
    """
    {"description":"scope-matrix revision"}
    """
    And I make a request to create a revision for "apis" resource "scopeMatrixApiId" with payload "scopeMatrixRevPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "scopeMatrixDeployPayload"
    """
    [{"name":"{{gatewayEnvironment}}","vhost":"localhost","displayOnDevportal":true}]
    """
    And I make a request to deploy revision "revisionId" of "apis" resource "scopeMatrixApiId" with payload "scopeMatrixDeployPayload"
    Then The response status code should be 201
    When I publish the "apis" resource with id "scopeMatrixApiId"
    Then The lifecycle status of API "scopeMatrixApiId" should be "Published"
    When I retrieve the "apis" resource with id "scopeMatrixApiId"
    And I extract response field "context" and store it as "scopeMatrixContext"

    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "scopeMatrixAppPayload"
    And I create an application with payload "scopeMatrixAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "generateApplicationKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateApplicationKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "scopeMatrixSubPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "scopeMatrixApiId" using application "createdAppId" with payload "scopeMatrixSubPayload" as "scopeMatrixSubId"
    Then The response status code should be 201

    # --- Row A: ScopeA (exact /echo/products/catalog/{categoryId}) — 200 on its own path, 403 on the other four.
    When I request an OAuth access token for the current user using password grant with scope "ScopeA"
    Then The response status code should be 200
    And The issued token scope list should include "ScopeA" and exclude "ScopeB,ScopeC,ScopeD,ScopeE"
    And I invoke the API at gateway context "{{scopeMatrixContext}}/1.0.0/echo/products/catalog/1" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "echo/products/catalog/1"
    When I invoke the API at gateway context "{{scopeMatrixContext}}/1.0.0/echo/products/popular" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403
    When I invoke the API at gateway context "{{scopeMatrixContext}}/1.0.0/echo/products/noexactmatch" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403
    When I invoke the API at gateway context "{{scopeMatrixContext}}/1.0.0/echo/orders" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403
    When I invoke the API at gateway context "{{scopeMatrixContext}}/1.0.0/echo/noexactmatch" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403

    # --- Row B: ScopeB (exact /echo/products/popular) — the exact binding wins over the /echo/products/* wildcard.
    When I request an OAuth access token for the current user using password grant with scope "ScopeB"
    Then The response status code should be 200
    And The issued token scope list should include "ScopeB" and exclude "ScopeA,ScopeC,ScopeD,ScopeE"
    And I invoke the API at gateway context "{{scopeMatrixContext}}/1.0.0/echo/products/popular" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "echo/products/popular"
    When I invoke the API at gateway context "{{scopeMatrixContext}}/1.0.0/echo/products/catalog/1" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403
    When I invoke the API at gateway context "{{scopeMatrixContext}}/1.0.0/echo/products/noexactmatch" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403
    When I invoke the API at gateway context "{{scopeMatrixContext}}/1.0.0/echo/orders" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403
    When I invoke the API at gateway context "{{scopeMatrixContext}}/1.0.0/echo/noexactmatch" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403

    # --- Row C: ScopeC (segment wildcard /echo/products/*) — 200 only on the products path with no exact binding.
    When I request an OAuth access token for the current user using password grant with scope "ScopeC"
    Then The response status code should be 200
    And The issued token scope list should include "ScopeC" and exclude "ScopeA,ScopeB,ScopeD,ScopeE"
    And I invoke the API at gateway context "{{scopeMatrixContext}}/1.0.0/echo/products/noexactmatch" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "echo/products/noexactmatch"
    When I invoke the API at gateway context "{{scopeMatrixContext}}/1.0.0/echo/products/catalog/1" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403
    When I invoke the API at gateway context "{{scopeMatrixContext}}/1.0.0/echo/products/popular" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403
    When I invoke the API at gateway context "{{scopeMatrixContext}}/1.0.0/echo/orders" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403
    When I invoke the API at gateway context "{{scopeMatrixContext}}/1.0.0/echo/noexactmatch" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403

    # --- Row D: ScopeD (exact /echo/orders) — the exact binding wins over the /echo/* global wildcard.
    When I request an OAuth access token for the current user using password grant with scope "ScopeD"
    Then The response status code should be 200
    And The issued token scope list should include "ScopeD" and exclude "ScopeA,ScopeB,ScopeC,ScopeE"
    And I invoke the API at gateway context "{{scopeMatrixContext}}/1.0.0/echo/orders" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "echo/orders"
    When I invoke the API at gateway context "{{scopeMatrixContext}}/1.0.0/echo/products/catalog/1" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403
    When I invoke the API at gateway context "{{scopeMatrixContext}}/1.0.0/echo/products/popular" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403
    When I invoke the API at gateway context "{{scopeMatrixContext}}/1.0.0/echo/products/noexactmatch" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403
    When I invoke the API at gateway context "{{scopeMatrixContext}}/1.0.0/echo/noexactmatch" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403

    # --- Row E: ScopeE (global wildcard /echo/*) — 200 only on the path no more specific operation claims.
    When I request an OAuth access token for the current user using password grant with scope "ScopeE"
    Then The response status code should be 200
    And The issued token scope list should include "ScopeE" and exclude "ScopeA,ScopeB,ScopeC,ScopeD"
    And I invoke the API at gateway context "{{scopeMatrixContext}}/1.0.0/echo/noexactmatch" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "echo/noexactmatch"
    When I invoke the API at gateway context "{{scopeMatrixContext}}/1.0.0/echo/products/catalog/1" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403
    When I invoke the API at gateway context "{{scopeMatrixContext}}/1.0.0/echo/products/popular" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403
    When I invoke the API at gateway context "{{scopeMatrixContext}}/1.0.0/echo/products/noexactmatch" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403
    When I invoke the API at gateway context "{{scopeMatrixContext}}/1.0.0/echo/orders" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
