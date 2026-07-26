@cleanup
Feature: Key Manager Token Persistence Across Restart

  Key-manager-plane token persistence: a valid token keeps working and a revoked token stays revoked across a
  graceful APIM server restart — proving token state is persisted to the DB, not held only in memory. Runs in
  the IntegrationV2-ServerRestart block, whose overlay enables `[oauth.token_persistence]` and exposes the
  ServerAdmin `restartGracefully` operation (`[server] enable_restart_from_api`); the block runs sequentially
  (thread-count=1) because the restart bounces the shared server. SINGLE-TENANT (super) by design: token
  persistence is server/DB-wide behaviour, identical for every tenant, and each restart is expensive — running
  it ×2 tenant would add ~no coverage for ~double the restarts. Teardown via the per-scenario cleanup hook.

  @cap:key-manager @feat:token-persistence @type:regression @dep:gateway @legacy:TokenPersistenceRestartTestCase
  Scenario: A valid token stays valid and a revoked token stays revoked across a server restart
    Given The system is ready
    And I have valid access tokens as "admin"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"
    When I retrieve the "apis" resource with id "createdApiId"
    And I extract response field "context" and store it as "apiContext"

    # Subscribe an application and obtain a password-grant access token
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "generateApplicationKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password", "refresh_token"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateApplicationKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "apiSubscriptionPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "createdApiId" using application "createdAppId" with payload "apiSubscriptionPayload" as "subscriptionId"
    Then The response status code should be 201
    When I request an OAuth access token for the current user using password grant with scope "PRODUCTION"
    Then The response status code should be 200
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    # Restart the server — a valid token must survive (state persisted to the DB)
    When I gracefully restart the API Manager server
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    # Revoke the token — invocation is now rejected
    When I revoke the OAuth access token "generatedAccessToken"
    Then The response status code should be 200
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401

    # Restart again — the revocation must also survive (revoked token stays revoked)
    When I gracefully restart the API Manager server
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
