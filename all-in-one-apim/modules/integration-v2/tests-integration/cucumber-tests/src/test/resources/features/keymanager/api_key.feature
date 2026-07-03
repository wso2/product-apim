@cleanup
Feature: Key Manager API Key

  Key-manager-plane API-key issuance and use: enable the api_key security scheme on an API, generate an API
  key for a subscribed application, and invoke the API through the gateway using that key. Runs as admin in
  both the super tenant and tenant1.com. Teardown via the per-scenario cleanup hook.

  @cap:key-manager @feat:api-key @type:smoke @dep:gateway @legacy:APIKeyInvocationTestCase
  Scenario Outline: Generate an API key and invoke a published API with it as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it
    When I retrieve the "apis" resource with id "createdApiId"
    Then The response status code should be 200
    And I put the response payload in context as "createdApiPayload"

    # Enable the api_key security scheme
    When I update the "apis" resource "createdApiId" and "createdApiPayload" with configuration type "securityScheme" and value:
      """
      ["api_key", "oauth_basic_auth_api_key_mandatory", "oauth2"]
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "createdApiId"
    Then The response should contain "api_key"
    And I extract response field "context" and store it as "apiContext"

    When I deploy the API with id "createdApiId"
    Then The response status code should be 201
    And I wait until "apis" "createdApiId" revision is deployed in the gateway
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"

    # Subscribe an application and generate an API key
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "apiSubscriptionPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "createdApiId" using application "createdAppId" with payload "apiSubscriptionPayload" as "subscriptionId"
    Then The response status code should be 201

    When I put the following JSON payload in context as "apiKeyGenerationPayload"
    """
    {"keyName": "TestAPIKey", "validityPeriod": 3600, "additionalProperties": {"permittedIP": "", "permittedReferer": ""}}
    """
    And I request an api key for application id "createdAppId" using payload "apiKeyGenerationPayload"
    Then The response status code should be 200

    # Invoke through the gateway using the API key (full context path, no tenant re-prefix)
    When I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using api key "apiKey" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:key-manager @feat:api-key @type:negative @dep:gateway @legacy:APIKeyInvocationTestCase
  Scenario Outline: Invoke an api_key-secured API with an invalid key is rejected as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it
    When I retrieve the "apis" resource with id "createdApiId"
    Then The response status code should be 200
    And I put the response payload in context as "createdApiPayload"
    When I update the "apis" resource "createdApiId" and "createdApiPayload" with configuration type "securityScheme" and value:
      """
      ["api_key", "oauth_basic_auth_api_key_mandatory", "oauth2"]
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "createdApiId"
    And I extract response field "context" and store it as "apiContext"
    When I deploy the API with id "createdApiId"
    Then The response status code should be 201
    And I wait until "apis" "createdApiId" revision is deployed in the gateway
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"

    # Invoke with a garbage API key — the gateway must reject it
    When I put the following JSON payload in context as "invalidApiKey"
    """
    invalid-api-key-value
    """
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using api key "invalidApiKey" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
