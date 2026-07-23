@cap:admin @feat:external-key-manager @rule:self-validate @type:smoke @dep:gateway @cleanup
Feature: External Key Manager End-to-End Token Flow

  The full external-key-manager arc, end to end, per connector client-auth mode. The block boots WSO2 Identity
  Server 7.x on the shared docker network with cert trust established (bootExternalIdentityServer); the
  scenario itself REGISTERS IS as a third-party key manager via the admin REST API - registration is admin
  product behaviour under test, not block infrastructure. It then creates and deploys a simple API to the node
  backend, publishes it, creates an application, subscribes, generates keys against the registered key manager
  (so APIM does DCR into IS and returns IS client credentials), obtains a token FROM IS with those credentials,
  and invokes the deployed API through the gateway until it returns HTTP 200. The outline runs the identical
  arc for both client-auth modes - BasicAuth (wso2is7.json) and Mutual-TLS (wso2is7-mtls.json,
  tls_client_auth; the DCR/keygen leg is what exercises APIM's client certificate against IS). Per-scenario
  cleanup via the cleanup hook (applications sweep before key managers, so the KM delete is FK-safe).

  Scenario Outline: Obtain a token from IS7 (<kmAuthMode> client auth) and invoke a deployed API through the gateway
    Given The system is ready
    And I have valid access tokens as "admin"
    When I create a key manager from payload "<kmPayload>" as "bootKm"
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
    {"keyType": "PRODUCTION", "keyManager": "{{bootKmName}}", "grantTypesToBeSupported": ["client_credentials", "password", "refresh_token"]}
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
    # --- Self-validate-JWT token revocation (both outline rows are self-validate mode) ---
    # Revoke the IS-issued token at IS; the gateway self-validates the JWT locally and does NOT re-contact IS per
    # request, so it learns of the revocation via the IS->APIM notification pipeline (IS event handler jar ->
    # https://wso2am:9443/internal/data/v1/notify -> tokenRevocation JMS topic -> gateway revoked-JWT cache; a
    # sub-second cache window applies), then rejects the token with 401. This needs the IS deployment.toml to send
    # X-WSO2-KEY-MANAGER=WSO2-IS (the APIM event-handler TYPE the IS7 connector registers), not the KM instance
    # name - see the overlay. It also needs this block to hold the wso2am notification alias
    # (receiveExternalIsNotifications). (Introspection-mode revocation is covered in is7_introspection.feature.)
    When I revoke the access token at the external key manager
    Then The response status code should be 200
    # Prove the revoke reached IS itself (isolates an IS-side revoke issue from gateway-side enforcement).
    And the access token should be inactive at the external key manager introspection endpoint
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 401 within 30 seconds
    Then The response status code should be 401

    Examples:
      | kmAuthMode | kmPayload                                       |
      | Basic      | artifacts/payloads/keymanagers/wso2is7.json      |
      | Mutual-TLS | artifacts/payloads/keymanagers/wso2is7-mtls.json |
