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

  # NOTE: AllowedScopesTestWithCorsDisabled shares this exact allowed_scopes + operation-scope assertion
  # surface — it differs only in [apim.cors] enable=false, which does not change any status asserted here — so
  # it is @legacy-tagged on this same scenario rather than given a separate config block/container.
  @cap:gateway @feat:security-enforcement @type:regression @dep:publisher @legacy:AllowedScopesTestCase @legacy:AllowedScopesTestWithCorsDisabled
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

    # scope1 — whitelisted AND satisfies the operation binding → 200.
    When I request an OAuth access token for the current user using password grant with scope "scope1"
    Then The response status code should be 200
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    # scope2 — whitelisted AND satisfies the operation binding → 200.
    When I request an OAuth access token for the current user using password grant with scope "scope2"
    Then The response status code should be 200
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    # scope3 — does NOT satisfy the operation binding (scope1/scope2) → refused at the gateway (403).
    When I request an OAuth access token for the current user using password grant with scope "scope3"
    Then The response status code should be 200
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403

    # No explicit scope (default scope) — likewise lacks the operation scope → 403.
    When I request an OAuth access token for the current user using password grant with scope ""
    Then The response status code should be 200
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # PER-RESOURCE scope binding with WILDCARD PRECEDENCE — the discriminating half of AllowedScopesTestCase's
  # ExampleAPI. Three resources on ONE API bind different scopes, and the gateway routes each incoming path to the
  # MOST SPECIFIC matching operation: /echo/products/catalog/{id} (exact → ScopeA) beats /echo/products/* (segment
  # wildcard → ScopeC) beats /echo/* (global wildcard → ScopeE). All paths route to the node backend /echo/* which
  # 200s for any subpath, so ONLY the gateway's per-resource scope check distinguishes 200 from 403: a token whose
  # single scope matches the operation the path resolves to → 200; the same token on a path that resolves to a
  # DIFFERENT operation → 403.
  # TRIM (lead-approved): ScopeB (/products/popular) and ScopeD (/orders) are omitted — they are additional EXACT
  # bindings, the same tier already proven by ScopeA; the three ported (A exact, C segment-wildcard, E
  # global-wildcard) cover all three distinct binding tiers with six invocations instead of twenty-five.
  @cap:gateway @feat:security-enforcement @type:regression @dep:publisher @legacy:AllowedScopesTestCase
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

    # ScopeA (exact /echo/products/catalog/{id}): its own path → 200; a foreign path (resolves to ScopeC) → 403.
    When I request an OAuth access token for the current user using password grant with scope "ScopeA"
    Then The response status code should be 200
    And I invoke the API at gateway context "{{scopeMatrixContext}}/1.0.0/echo/products/catalog/1" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{scopeMatrixContext}}/1.0.0/echo/products/other" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403

    # ScopeC (segment wildcard /echo/products/*): a /echo/products/... path → 200; the exact-binding path (ScopeA) → 403.
    When I request an OAuth access token for the current user using password grant with scope "ScopeC"
    Then The response status code should be 200
    And I invoke the API at gateway context "{{scopeMatrixContext}}/1.0.0/echo/products/popular" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{scopeMatrixContext}}/1.0.0/echo/products/catalog/1" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403

    # ScopeE (global wildcard /echo/*): a non-products path → 200; a /echo/products/... path (resolves to ScopeC) → 403.
    When I request an OAuth access token for the current user using password grant with scope "ScopeE"
    Then The response status code should be 200
    And I invoke the API at gateway context "{{scopeMatrixContext}}/1.0.0/echo/orders" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{scopeMatrixContext}}/1.0.0/echo/products/popular" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
