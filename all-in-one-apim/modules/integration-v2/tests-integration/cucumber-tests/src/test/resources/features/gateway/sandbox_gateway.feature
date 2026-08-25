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

  # Unsecured resource on a BOTH-endpoints API deployed to the SANDBOX gateway. Secured baselines mirror the
  # scenario above: the SANDBOX token routes to the sandbox endpoint (echo/sandbox, 200) and the PRODUCTION token
  # is rejected 401 "Production Key Provided for Sandbox Gateway". Once the resource's authType is None the gateway
  # has no key type to route on and serves the endpoint of its OWN environment (sandbox), so the PRODUCTION token
  # that was 401 now succeeds AND is served by the SANDBOX endpoint — the discriminating arm that pins endpoint
  # selection following the gateway ENVIRONMENT TYPE, not falling through to production. Ports the both-endpoints
  # arc of InvokeAPIWithVariousEndpointsAndTokensInSandboxEnvTestCase#testInvokeAPIWithBothEndpointsAndTokens.
  @cap:gateway @feat:rest-invocation @rule:sandbox-gateway @type:regression @dep:publisher @legacy:InvokeAPIWithVariousEndpointsAndTokensInSandboxEnvTestCase
  Scenario Outline: An unsecured resource on a both-endpoints API serves the sandbox endpoint for either token on the sandbox gateway as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_prodsandbox_api.json" as "sbuApiId" and deployed it
    When I publish the "apis" resource with id "sbuApiId"
    Then The lifecycle status of API "sbuApiId" should be "Published"
    When I retrieve the "apis" resource with id "sbuApiId"
    And I extract response field "context" and store it as "sbuContext"

    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "sbuApp"
    And I create an application with payload "sbuApp"
    Then The response status code should be 201
    When I put the following JSON payload in context as "sbuSub"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "sbuApiId" using application "createdAppId" with payload "sbuSub" as "sbuSubId"
    Then The response status code should be 201

    # Mint a SANDBOX token and keep it under its own key (generatedAccessToken is overwritten by the next mint).
    When I put the following JSON payload in context as "sbuSandboxKeys"
    """
    {"keyType": "SANDBOX", "grantTypesToBeSupported": ["client_credentials"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "sbuSandboxKeys"
    Then The response status code should be 200
    When I put the following JSON payload in context as "sbuSandboxTokenReq"
    """
    {"consumerSecret": "{{appConsumerSecret}}", "validityPeriod": 3600}
    """
    And I request an access token for application id "createdAppId" using payload "sbuSandboxTokenReq"
    Then The response status code should be 200
    And I extract response field "accessToken" and store it as "sbuSandboxToken"

    # Mint a PRODUCTION token and keep it under its own key.
    When I put the following JSON payload in context as "sbuProdKeys"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "sbuProdKeys"
    Then The response status code should be 200
    When I put the following JSON payload in context as "sbuProdTokenReq"
    """
    {"consumerSecret": "{{appConsumerSecret}}", "validityPeriod": 3600}
    """
    And I request an access token for application id "createdAppId" using payload "sbuProdTokenReq"
    Then The response status code should be 200
    And I extract response field "accessToken" and store it as "sbuProdToken"

    # Secured baseline A: the SANDBOX token routes to the sandbox endpoint (echo/sandbox, 200).
    When I invoke the API at gateway context "{{sbuContext}}/1.0.0/x" with method "GET" using access token "sbuSandboxToken" and payload "" until response body contains "echo/sandbox" within 60 seconds
    Then The response status code should be 200
    # Secured baseline B (discriminating): the PRODUCTION token is rejected 401 by the sandbox gateway. This 401 is
    # what makes the post-flip 200 attributable — it proves the resource was genuinely secured.
    When I invoke the API at gateway context "{{sbuContext}}/1.0.0/x" with method "GET" using access token "sbuProdToken" and payload "" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    And The response should contain "Production Key Provided for Sandbox Gateway"

    # Flip the resource to authType None, redeploy, and gate on propagation before reading — a read before the new
    # config reaches the gateway sees the OLD secured behaviour and produces a false failure.
    When I retrieve the "apis" resource with id "sbuApiId"
    And I put the response payload in context as "sbuApiPayload"
    When I update the "apis" resource "sbuApiId" and "sbuApiPayload" with configuration type "operations" and value:
      """
      [{"target":"/x","verb":"GET","authType":"None","throttlingPolicy":"Unlimited"}]
      """
    Then The response status code should be 200
    When I deploy the API with id "sbuApiId"
    And the "apis" resource "sbuApiId" should be live on the gateway, redeploying if propagation is lost
    And I wait until "apis" "sbuApiId" revision is deployed in the gateway

    # Unsecured reading, SANDBOX token: still the sandbox endpoint. This body-gated invoke is also the propagation
    # gate for the production-token read that follows.
    When I invoke the API at gateway context "{{sbuContext}}/1.0.0/x" with method "GET" using access token "sbuSandboxToken" and payload "" until response body contains "echo/sandbox" within 60 seconds
    Then The response status code should be 200
    And The response should not contain "echo/prod"
    # Unsecured reading, PRODUCTION token (discriminating): the 401 is gone and — because this is a SANDBOX gateway —
    # the SANDBOX endpoint answers, NOT production. The not-contains pins that endpoint selection followed the
    # gateway environment type rather than falling through to the production endpoint.
    When I invoke the API at gateway context "{{sbuContext}}/1.0.0/x" with method "GET" using access token "sbuProdToken" and payload "" until response body contains "echo/sandbox" within 60 seconds
    Then The response status code should be 200
    And The response should not contain "echo/prod"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Unsecured resource on a SANDBOX-ENDPOINT-ONLY API deployed to the sandbox gateway. Secured baselines: the
  # SANDBOX token routes to the sole endpoint (echo/sandbox, 200) and the PRODUCTION token is rejected 401
  # "Production Key Provided for Sandbox Gateway" — the sandbox-gateway rule wins over the standalone 900901
  # no-production-endpoint case (legacy asserts 401 here, not 900901). Once the resource is unsecured, both tokens
  # serve the only endpoint that exists (sandbox). Ports the sandbox-only arc of
  # InvokeAPIWithVariousEndpointsAndTokensInSandboxEnvTestCase#testInvokeAPIWithSandboxEndpointAndBothTokens.
  @cap:gateway @feat:rest-invocation @rule:sandbox-gateway @type:regression @dep:publisher @legacy:InvokeAPIWithVariousEndpointsAndTokensInSandboxEnvTestCase
  Scenario Outline: An unsecured resource on a sandbox-only API serves the sandbox endpoint for either token on the sandbox gateway as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_sandboxonly_api.json" as "ssuApiId" and deployed it
    When I publish the "apis" resource with id "ssuApiId"
    Then The lifecycle status of API "ssuApiId" should be "Published"
    When I retrieve the "apis" resource with id "ssuApiId"
    And I extract response field "context" and store it as "ssuContext"

    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "ssuApp"
    And I create an application with payload "ssuApp"
    Then The response status code should be 201
    When I put the following JSON payload in context as "ssuSub"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "ssuApiId" using application "createdAppId" with payload "ssuSub" as "ssuSubId"
    Then The response status code should be 201

    # Mint a SANDBOX token and keep it under its own key.
    When I put the following JSON payload in context as "ssuSandboxKeys"
    """
    {"keyType": "SANDBOX", "grantTypesToBeSupported": ["client_credentials"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "ssuSandboxKeys"
    Then The response status code should be 200
    When I put the following JSON payload in context as "ssuSandboxTokenReq"
    """
    {"consumerSecret": "{{appConsumerSecret}}", "validityPeriod": 3600}
    """
    And I request an access token for application id "createdAppId" using payload "ssuSandboxTokenReq"
    Then The response status code should be 200
    And I extract response field "accessToken" and store it as "ssuSandboxToken"

    # Mint a PRODUCTION token and keep it under its own key.
    When I put the following JSON payload in context as "ssuProdKeys"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "ssuProdKeys"
    Then The response status code should be 200
    When I put the following JSON payload in context as "ssuProdTokenReq"
    """
    {"consumerSecret": "{{appConsumerSecret}}", "validityPeriod": 3600}
    """
    And I request an access token for application id "createdAppId" using payload "ssuProdTokenReq"
    Then The response status code should be 200
    And I extract response field "accessToken" and store it as "ssuProdToken"

    # Secured baseline A: the SANDBOX token routes to the sole (sandbox) endpoint (echo/sandbox, 200).
    When I invoke the API at gateway context "{{ssuContext}}/1.0.0/x" with method "GET" using access token "ssuSandboxToken" and payload "" until response body contains "echo/sandbox" within 60 seconds
    Then The response status code should be 200
    # Secured baseline B (discriminating): the PRODUCTION token is rejected 401 by the sandbox gateway.
    When I invoke the API at gateway context "{{ssuContext}}/1.0.0/x" with method "GET" using access token "ssuProdToken" and payload "" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    And The response should contain "Production Key Provided for Sandbox Gateway"

    # Flip the resource to authType None, redeploy, and gate on propagation before reading.
    When I retrieve the "apis" resource with id "ssuApiId"
    And I put the response payload in context as "ssuApiPayload"
    When I update the "apis" resource "ssuApiId" and "ssuApiPayload" with configuration type "operations" and value:
      """
      [{"target":"/x","verb":"GET","authType":"None","throttlingPolicy":"Unlimited"}]
      """
    Then The response status code should be 200
    When I deploy the API with id "ssuApiId"
    And the "apis" resource "ssuApiId" should be live on the gateway, redeploying if propagation is lost
    And I wait until "apis" "ssuApiId" revision is deployed in the gateway

    # Unsecured reading, SANDBOX token: still the sandbox endpoint. Also the propagation gate for the read below.
    When I invoke the API at gateway context "{{ssuContext}}/1.0.0/x" with method "GET" using access token "ssuSandboxToken" and payload "" until response body contains "echo/sandbox" within 60 seconds
    Then The response status code should be 200
    And The response should not contain "echo/prod"
    # Unsecured reading, PRODUCTION token (discriminating): the 401 is gone; the sole (sandbox) endpoint answers.
    When I invoke the API at gateway context "{{ssuContext}}/1.0.0/x" with method "GET" using access token "ssuProdToken" and payload "" until response body contains "echo/sandbox" within 60 seconds
    Then The response status code should be 200
    And The response should not contain "echo/prod"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
