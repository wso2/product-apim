@cap:gateway @feat:platform-gateway
Feature: API Platform Gateway lifecycle

  A platform gateway is a standalone gateway runtime driven by the APIM control plane. It is registered from the
  admin plane (which mints a one-time registration token), then the running gateway connects to the control plane
  with that token and shows as active.

  @type:smoke @dep:publisher
  Scenario: Register a platform gateway, deploy an API-key-secured REST API, and enforce auth at the gateway
    Given The system is ready
    And I have valid access tokens as "admin"
    When I register a platform gateway "smoke-gateway"
    Then The response status code should be 201
    When I connect the platform gateway with the issued registration token
    Then the platform gateway "smoke-gateway" becomes active within 120 seconds
    When I create and deploy a REST API from "artifacts/payloads/create_platform_gateway_api.json" to the platform gateway as "pgApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "pgApiId"
    Then The lifecycle status of API "pgApiId" should be "Published"
    When I generate an internal API key for API "pgApiId" and store it as "pgApiKey"
    # The API carries an api-key-auth policy, so the gateway ENFORCES the key: valid → 200, missing/wrong → 401.
    When I invoke the deployed API on the platform gateway with header "ApiKey" set to "pgApiKey" until response status code becomes 200 within 90 seconds
    Then The response status code should be 200
    # The API also carries a set-headers request policy adding X-Injected-By-Policy; the echo backend reflects
    # request headers in its body, so the injected value appearing there proves the policy was applied at the gateway.
    And The response should contain "pgw-set-headers-applied"
    When I invoke the deployed API on the platform gateway without authentication until response status code becomes 401 within 30 seconds
    Then The response status code should be 401
    When I invoke the deployed API on the platform gateway with header "ApiKey" set to "totally-wrong-api-key" until response status code becomes 401 within 30 seconds
    Then The response status code should be 401

  @type:negative
  Scenario: A registered platform gateway that never connects stays inactive
    Given The system is ready
    And I have valid access tokens as "admin"
    When I register a platform gateway "unconnected-gw"
    Then The response status code should be 201
    And the platform gateway "unconnected-gw" is inactive
