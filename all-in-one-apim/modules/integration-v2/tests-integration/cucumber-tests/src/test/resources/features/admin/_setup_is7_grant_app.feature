@setup
Feature: Setup - WSO2 IS 7.x runtime app for grant-type tests

  Creates and deploys an API to the node backend, publishes it, creates an application, generates keys against
  the runner-registered WSO2-IS-7 external key manager (so APIM does DCR into IS and returns IS client credentials), and
  subscribes. Asserts only create success; the created ids are registered for the runner's AfterClass cleanup.
  Later grant-type scenarios in the same runner reuse this app's IS credentials and the API context, so this
  runs once (listed first by the _setup_ prefix) and is not torn down per-scenario.

  Scenario: Provision the IS7-bound API, application and keys
    Given The system is ready
    And I have valid access tokens as "admin"
    # Register the external key manager first - the runner-shared KM fixture every later keygen/token step uses.
    When I create a key manager from payload "artifacts/payloads/keymanagers/wso2is7.json" as "grantKm"
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
    {"keyType": "PRODUCTION", "keyManager": "{{grantKmName}}", "grantTypesToBeSupported": ["client_credentials", "password", "refresh_token", "authorization_code", "urn:ietf:params:oauth:grant-type:device_code", "urn:ietf:params:oauth:grant-type:jwt-bearer", "urn:ietf:params:oauth:grant-type:saml2-bearer"], "callbackUrl": "https://localhost/callback"}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateApplicationKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "apiSubscriptionPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "createdApiId" using application "createdAppId" with payload "apiSubscriptionPayload" as "subscriptionId"
    Then The response status code should be 201
