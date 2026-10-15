@cleanup
Feature: Publisher endpoint validation

  Endpoint validation checks that APIM can contact the configured backend. The local route matches the bundled
  calculator webapp behavior used by APIMANAGER2611EndpointValidationTestCase: the bare context redirects to its
  trailing-slash URL, which serves the landing page for GET and HEAD. This runs in an isolated Publisher block with
  the backend fixture enabled, for super-tenant and tenant admins.

  @cap:publisher @feat:api-config @rule:endpoint-validation-calculator-redirect @type:regression @legacy:APIMANAGER2611EndpointValidationTestCase
  Scenario Outline: Endpoint validation succeeds for the legacy calculator URL as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "headValApiPayload"
    And I set the JSON object field "endpointConfig" from file "artifacts/payloads/endpoint_validation_endpoint_config.json" in the payload "headValApiPayload"
    And I create an "apis" resource with payload "headValApiPayload" as "headValApiId"
    Then The response status code should be 201

    When I validate the endpoint "http://nodebackend:3001/am/sample/calculator/v1" for API "headValApiId"
    Then The response status code should be 200
    And The value of response field "statusCode" should be "200"
    And The response should contain "OK"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
