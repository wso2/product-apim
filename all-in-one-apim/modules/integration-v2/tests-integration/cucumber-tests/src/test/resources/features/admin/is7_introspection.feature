@cap:admin @feat:external-key-manager @rule:introspection @type:regression @dep:gateway @cleanup
Feature: External Key Manager Introspection Mode

  Verifies the external-key-manager block wired with the IS7 KM in INTROSPECTION mode
  (wso2is7-introspect.json, enableSelfValidationJWT=false): the gateway validates tokens by calling IS
  /oauth2/introspect rather than self-validating the JWT locally. Boots IS 7.x, registers the KM, creates and
  deploys an API, subscribes an app, generates keys (DCR into IS), obtains an IS token and invokes through the
  gateway (200 - proves the KM works in introspection mode), then revokes the token at IS and asserts IS reports
  it inactive at its introspection endpoint (proves revocation propagates to the IdP in introspection mode), then
  invokes again through the gateway and asserts it is finally rejected with 401 (proves the gateway enforces the
  revocation in introspection mode).

  Note on revoke->401 latency: in introspection mode the keymgt layer caches each introspection result in a
  dedicated IntrospectionCache (DefaultKeyValidationHandler.getAccessTokenInfo) that is consulted only for token
  expiry, is never invalidated by the IS->APIM /notify pipeline, and defaults to a 900s (15 min) TTL. So a
  revoked-but-unexpired token keeps passing the gateway until that entry evicts. The introspection block sets a
  short apim.cache.token_expiry_time (see testng-is7km-introspect.xml overlay) to bound that window; the revoke->401
  poll below therefore succeeds once the cache entry evicts and the gateway re-introspects. (Self-validate mode, in
  is7_key_manager_boot.feature, reflects revocation immediately via the /notify pipeline instead.)

  Scenario: Obtain a token from IS7 (introspection mode) and invoke a deployed API through the gateway
    Given The system is ready
    And I have valid access tokens as "admin"
    When I create a key manager from payload "artifacts/payloads/keymanagers/wso2is7-introspect.json" as "superKmId"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"
    When I retrieve the "apis" resource with id "createdApiId"
    And I extract response field "context" and store it as "apiContext"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app_oauth.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "generateApplicationKeysPayload"
    """
    {"keyType": "PRODUCTION", "keyManager": "{{superKmIdName}}", "grantTypesToBeSupported": ["client_credentials", "password", "refresh_token"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateApplicationKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "apiSubscriptionPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "createdApiId" using application "createdAppId" with payload "apiSubscriptionPayload" as "subscriptionId"
    Then The response status code should be 201
    When I request an OAuth access token from the external key manager using client credentials grant
    Then The response status code should be 200
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    # Introspection-mode revocation at the IdP: revoke the IS-issued token, then IS must report it inactive
    # at /oauth2/introspect (which is what the gateway consults per request in this mode).
    When I revoke the access token at the external key manager
    Then The response status code should be 200
    And the access token should be inactive at the external key manager introspection endpoint
    # Gateway enforcement in introspection mode: once the keymgt IntrospectionCache entry evicts (short TTL set by
    # this block's overlay), the gateway re-introspects, IS reports the token inactive, and the request is rejected.
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401

  Scenario: A tenant-org IS7 token is introspection-validated and rejected after revocation at IS
    # Key-manager configs are org-scoped, so the tenant admin registers its OWN introspection-mode WSO2-IS-7
    # key manager before the arc (the super scenario's key manager exists only in the super tenant). The same
    # IntrospectionCache TTL note above applies - the revoke->401 poll succeeds once the entry evicts.
    Given The system is ready
    And I have valid access tokens as "admin@tenant1.com"
    When I create a key manager from payload "artifacts/payloads/keymanagers/wso2is7-introspect.json" as "tenantKmId"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"
    When I retrieve the "apis" resource with id "createdApiId"
    And I extract response field "context" and store it as "apiContext"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app_oauth.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "generateApplicationKeysPayload"
    """
    {"keyType": "PRODUCTION", "keyManager": "{{tenantKmIdName}}", "grantTypesToBeSupported": ["client_credentials", "password", "refresh_token"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateApplicationKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "apiSubscriptionPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "createdApiId" using application "createdAppId" with payload "apiSubscriptionPayload" as "subscriptionId"
    Then The response status code should be 201
    When I request an OAuth access token from the external key manager using client credentials grant
    Then The response status code should be 200
    # The tenant API's context already carries the /t/tenant1.com prefix - invoke it verbatim.
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I revoke the access token at the external key manager
    Then The response status code should be 200
    And the access token should be inactive at the external key manager introspection endpoint
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
