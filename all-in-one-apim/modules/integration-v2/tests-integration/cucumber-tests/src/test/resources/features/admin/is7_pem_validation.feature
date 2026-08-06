@cap:admin @feat:external-key-manager @rule:pem @type:regression @dep:gateway @cleanup
Feature: External Key Manager PEM Certificate Validation

  The self-validate-JWT mode has two signature-verification configurations: a JWKS endpoint (covered by the
  other IS7 blocks, whose wso2is7*.json all use certificates.type=JWKS) and a pinned PEM certificate. This block
  registers the key manager with certificates.type=PEM whose value is IS's TOKEN-SIGNING certificate (the
  primary-keystore wso2carbon cert, committed at artifacts/certs/is7signing/is7-signing.pem - NOT the wso2is TLS
  cert, a different key pair), and proves the gateway validates IS-issued JWTs against that pinned cert. The
  key-rotation walk then proves the PEM really is what validates (and pins the operational difference from JWKS,
  mirroring the token-exchange rotation canary): swap the key manager's certificate to a pinned cert of a
  DIFFERENT key pair (the committed is7trustedidp cert, modelling a post-rotation stale pin) - a FRESHLY minted
  IS token is rejected 401 (fresh so the gateway token cache cannot serve a pre-swap validation); re-upload the
  live signing cert - a fresh token passes 200 again. PEM pins must be re-uploaded on rotation; JWKS re-fetches. per-scenario cleanup via the cleanup hook.

  Scenario: The gateway validates and rejects IS7 tokens according to the pinned PEM certificate
    Given The system is ready
    And I have valid access tokens as "admin"
    When I create a key manager from payload "artifacts/payloads/keymanagers/wso2is7-pem.json" as "pemKmId"
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
    {"keyType": "PRODUCTION", "keyManager": "{{pemKmIdName}}", "grantTypesToBeSupported": ["client_credentials"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateApplicationKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "apiSubscriptionPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "createdApiId" using application "createdAppId" with payload "apiSubscriptionPayload" as "subscriptionId"
    Then The response status code should be 201
    # Positive: a token signed by IS's live key passes gateway self-validation against the pinned PEM.
    When I request an OAuth access token from the external key manager using client credentials grant
    Then The response status code should be 200
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    # Rotation canary: pin a cert of a DIFFERENT key pair -> a FRESH IS token no longer validates (401).
    When I update the key manager "pemKmId" setting its PEM certificate from file "artifacts/certs/is7trustedidp/idp-cert.pem"
    Then The response status code should be 200
    When I request an OAuth access token from the external key manager using client credentials grant
    Then The response status code should be 200
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    # Recovery: re-upload the live signing cert -> a fresh token validates again (200).
    When I update the key manager "pemKmId" setting its PEM certificate from file "artifacts/certs/is7signing/is7-signing.pem"
    Then The response status code should be 200
    When I request an OAuth access token from the external key manager using client credentials grant
    Then The response status code should be 200
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
