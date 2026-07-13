@cleanup
Feature: Publisher Shared Scope Enforcement Across Restart

  Ports the legacy SharedScopeTestWithRestart, strengthened to assert the scope's CORE FUNCTIONAL property
  across an in-product graceful restart — not just that its definition persists. A shared scope is attached
  to an API operation, then invoked with a token that HAS the scope (200) and one that lacks it (403); after
  a graceful restart, both must still hold — proving the gateway re-loads and still enforces the scope. This
  is the functional analogue of token_persistence_restart (which invokes across a restart), rather than a
  bare "definition survives" check. The plain shared-scope CRUD + update + ×2-tenant coverage lives in
  publisher/scopes.feature; only the restart-enforcement dimension lives here. Runs in the
  IntegrationV2-ServerRestart block (overlay enables `[server] enable_restart_from_api`, `initBackend` starts
  the gateway backend); the block runs sequentially (thread-count=1) because the restart bounces the shared
  server. SINGLE-TENANT (super) by design, matching token_persistence_restart: enforcement is gateway-wide
  and each restart is expensive. Teardown via the per-scenario cleanup hook.

  @cap:publisher @feat:scopes @type:regression @dep:gateway @legacy:SharedScopeTestWithRestart
  Scenario: Shared scope enforcement persists across a graceful server restart
    Given The system is ready and I have valid publisher access tokens as "admin"
    When I create a new shared scope as "sharedScopeRestartEnf"
    Then The response status code should be 201

    # Attach the shared scope to the API and its GET operation, then deploy once (so the deployed revision
    # enforces the scope).
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "scopeApiCreatePayload"
    And I create an "apis" resource with payload "scopeApiCreatePayload" as "scopeApiId"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "scopeApiId"
    And I put the response payload in context as "scopeApiPayload"
    When I update the "apis" resource "scopeApiId" and "scopeApiPayload" with configuration type "scopes" and value:
      """
      [{"shared":true,"scope":{"name":"sharedScopeRestartEnf","displayName":"sharedScopeRestartEnf","description":"restart enforcement scope","bindings":["admin"]}}]
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "scopeApiId"
    And I put the response payload in context as "scopeApiPayload"
    When I update the "apis" resource "scopeApiId" and "scopeApiPayload" with configuration type "operations" and value:
      """
      [{"target":"/customers/{id}","verb":"GET","authType":"Application & Application User","throttlingPolicy":"Unlimited","scopes":["sharedScopeRestartEnf"],"operationPolicies":{"request":[],"response":[],"fault":[]}}]
      """
    Then The response status code should be 200
    When I put the following JSON payload in context as "scopeRevPayload"
    """
    {"description":"scope revision"}
    """
    And I make a request to create a revision for "apis" resource "scopeApiId" with payload "scopeRevPayload"
    When I put the following JSON payload in context as "scopeDeployPayload"
    """
    [{"name":"{{gatewayEnvironment}}","vhost":"localhost","displayOnDevportal":true}]
    """
    And I make a request to deploy revision "revisionId" of "apis" resource "scopeApiId" with payload "scopeDeployPayload"
    When I publish the "apis" resource with id "scopeApiId"
    Then The lifecycle status of API "scopeApiId" should be "Published"
    When I retrieve the "apis" resource with id "scopeApiId"
    And I extract response field "context" and store it as "apiContext"

    # Subscribe an application and key it.
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
    And I subscribe to API "scopeApiId" using application "createdAppId" with payload "apiSubscriptionPayload" as "subscriptionId"
    Then The response status code should be 201

    # Baseline enforcement: a token WITH the scope succeeds (200), one WITHOUT it is refused (403).
    When I request an OAuth access token for the current user using password grant with scope "sharedScopeRestartEnf"
    Then The response status code should be 200
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I request an OAuth access token for the current user using password grant with scope "openid"
    Then The response status code should be 200
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403

    # Graceful restart — enforcement must STILL hold (with-scope 200, without-scope 403).
    When I gracefully restart the API Manager server
    And I request an OAuth access token for the current user using password grant with scope "sharedScopeRestartEnf"
    Then The response status code should be 200
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I request an OAuth access token for the current user using password grant with scope "openid"
    Then The response status code should be 200
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403
