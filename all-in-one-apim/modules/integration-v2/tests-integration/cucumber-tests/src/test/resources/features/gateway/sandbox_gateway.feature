@cleanup
Feature: Gateway Sandbox-Only Environment

  Runtime enforcement on a SANDBOX-only gateway environment (the IntegrationV2-SandboxGateway block overlay sets the
  Default gateway environment type = "sandbox"). An API with both production and sandbox endpoints, deployed to this
  sandbox gateway, accepts a SANDBOX token (routing to the sandbox endpoint) but rejects a PRODUCTION token with a
  401 "Production Key Provided for Sandbox Gateway" — distinct from the standalone 900901 "no sandbox/production
  endpoint" cases in gateway/rest-invocation. Ports InvokeAPIWithVariousEndpointsAndTokensInSandboxEnvTestCase.
  Teardown via the per-scenario cleanup hook.

  @cap:gateway @feat:rest-invocation @rule:sandbox-gateway @type:regression @dep:publisher @legacy:InvokeAPIWithVariousEndpointsAndTokensInSandboxEnvTestCase
  Scenario Outline: A sandbox-only gateway accepts a sandbox token and rejects a production token as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_prodsandbox_api.json" as "sgApiId" and deployed it
    When I publish the "apis" resource with id "sgApiId"
    Then The lifecycle status of API "sgApiId" should be "Published"
    When I retrieve the "apis" resource with id "sgApiId"
    And I extract response field "context" and store it as "sgContext"

    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "sgApp"
    And I create an application with payload "sgApp"
    Then The response status code should be 201
    When I put the following JSON payload in context as "sgSub"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "sgApiId" using application "createdAppId" with payload "sgSub" as "sgSubId"
    Then The response status code should be 201

    # A SANDBOX token is accepted on the sandbox gateway -> routes to the sandbox endpoint (200).
    When I put the following JSON payload in context as "sgSandboxKeys"
    """
    {"keyType": "SANDBOX", "grantTypesToBeSupported": ["client_credentials"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "sgSandboxKeys"
    Then The response status code should be 200
    When I put the following JSON payload in context as "sgSandboxToken"
    """
    {"consumerSecret": "{{appConsumerSecret}}", "validityPeriod": 3600}
    """
    And I request an access token for application id "createdAppId" using payload "sgSandboxToken"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{sgContext}}/1.0.0/x" with method "GET" using access token "generatedAccessToken" and payload "" until response body contains "echo/sandbox" within 60 seconds
    Then The response status code should be 200

    # A PRODUCTION token is REJECTED by the sandbox gateway -> 401 "Production Key Provided for Sandbox Gateway".
    When I put the following JSON payload in context as "sgProdKeys"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "sgProdKeys"
    Then The response status code should be 200
    When I put the following JSON payload in context as "sgProdToken"
    """
    {"consumerSecret": "{{appConsumerSecret}}", "validityPeriod": 3600}
    """
    And I request an access token for application id "createdAppId" using payload "sgProdToken"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{sgContext}}/1.0.0/x" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    And The response should contain "Production Key Provided for Sandbox Gateway"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
