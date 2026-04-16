Feature: API-bound API Key Management

  Background:
    Given The system is ready and I have valid access tokens for current user

  # Step 1: Create API, Application, enable API key security, deploy and subscribe
  Scenario: Setup API and Application
    Given I have created an api from "artifacts/payloads/create_apim_test_api.json" as "testApiId" and deployed it
    And I retrieve the "apis" resource with id "testApiId"
    And I put the response payload in context as "testApiPayload"
    And I wait for deployment of the resource in "testApiPayload"
    And I publish the "apis" resource with id "testApiId"
    Then The response status code should be 200

    # Update security scheme to include api_key
    When I update the "apis" resource "testApiId" and "testApiPayload" with configuration type "securityScheme" and value:
      """
      ["api_key", "oauth_basic_auth_api_key_mandatory", "oauth2"]
      """
    Then The response status code should be 200

    When I retrieve the "apis" resource with id "testApiId"
    Then The response status code should be 200
    And I put the response payload in context as "testApiPayload"
    When I deploy the API with id "testApiId"
    Then The response status code should be 201
    And I wait until "apis" "testApiId" revision is deployed in the gateway

    # New application
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "<createAppPayload>"
    When I create an application with payload "<createAppPayload>"
    And I retrieve the application with id "createdAppId"
    Then The response status code should be 200

    # Subscribe application to API
    When I put the following JSON payload in context as "<subscriptionPayload>"
    """
    {
      "applicationId": "{{createdAppId}}",
      "apiId": "{{testApiId}}",
      "throttlingPolicy": "Unlimited"
    }
    """
    And I create a subscription using payload "<subscriptionPayload>"
    Then The response status code should be 201
    And I extract response field "subscriptionId" and store it as "<testSubscriptionId>"


  # Step 2: Generate key, associate, invoke, dissociate, invoke(fail), re-associate, revoke, invoke(fail)
  Scenario: Generate API key, associate, dissociate, and revoke from API side
    # Generate API-bound API key
    When I put the following JSON payload in context as "<apiKeyGenPayload>"
    """
    {
      "keyName": "TestKey1",
      "keyType": "PRODUCTION",
      "validityPeriod": 3600,
      "additionalProperties": {
        "permittedIP": "",
        "permittedReferer": ""
      }
    }
    """
    And I generate an api-bound api key for api "testApiId" with payload "<apiKeyGenPayload>" as "apiBoundApiKey1"
    Then The response status code should be 200

    # Find the keyUUID of the generated key
    When I find the keyUUID of api key "apiBoundApiKey1Name" for api "testApiId" as "apiKey1UUID"

    # Associate key to the subscribed application from API side
    When I associate api key "apiKey1UUID" to application "createdAppId" from api "testApiId"
    Then The response status code should be 200

    # Invoke API with associated key - should succeed
    When I invoke the API resource at path "/apiTestContext/1.0.0/customers/123/" with method "GET" using api key "apiBoundApiKey1"
    Then The response status code should be 200

    # Dissociate key from application
    When I dissociate api key "apiKey1UUID" from api "testApiId"
    Then The response status code should be 200

    # Invoke API with dissociated key - should fail
    When I invoke the API resource at path "/apiTestContext/1.0.0/customers/123/" with method "GET" using api key "apiBoundApiKey1"
    Then The response status code should be 403

    # Re-associate to test revoke behavior
    When I associate api key "apiKey1UUID" to application "createdAppId" from api "testApiId"
    Then The response status code should be 200

    # Revoke the API key
    When I revoke api key "apiKey1UUID" for api "testApiId"
    Then The response status code should be 200

    # Invoke API with revoked key - should fail
    When I invoke the API resource at path "/apiTestContext/1.0.0/customers/123/" with method "GET" using api key "apiBoundApiKey1"
    Then The response status code should be 401

  # Step 3: Generate new key, associate from app side, invoke, regenerate, invoke
  Scenario: Associate from application side and regenerate API key
    # Generate a second API key
    When I put the following JSON payload in context as "<apiKeyGenPayload2>"
    """
    {
      "keyName": "TestKey2",
      "keyType": "PRODUCTION",
      "validityPeriod": 3600,
      "additionalProperties": {
        "permittedIP": "",
        "permittedReferer": ""
      }
    }
    """
    And I generate an api-bound api key for api "testApiId" with payload "<apiKeyGenPayload2>" as "apiBoundApiKey2"
    Then The response status code should be 200

    # Find the keyUUID
    When I find the keyUUID of api key "apiBoundApiKey2Name" for api "testApiId" as "apiKey2UUID"

    # Associate from application side
    When I associate api key "apiKey2UUID" for api "testApiId" to application "createdAppId" with key type "PRODUCTION"
    Then The response status code should be 200

    # Invoke API with the new associated key - should succeed
    When I invoke the API resource at path "/apiTestContext/1.0.0/customers/123/" with method "GET" using api key "apiBoundApiKey2"
    Then The response status code should be 200

    # Regenerate the API key
    When I regenerate api key "apiKey2UUID" for api "testApiId" as "apiBoundApiKey2Regenerated"
    Then The response status code should be 200

    # Invoke API with regenerated key - should succeed (association maintained)
    When I invoke the API resource at path "/apiTestContext/1.0.0/customers/123/" with method "GET" using api key "apiBoundApiKey2Regenerated"
    Then The response status code should be 200

    # Old key should no longer work after regeneration
    When I invoke the API resource at path "/apiTestContext/1.0.0/customers/123/" with method "GET" using api key "apiBoundApiKey2"
    Then The response status code should be 401

  # Step 4: Cleanup
  Scenario: Cleanup created resources
    When I delete the application with id "createdAppId"
    Then The response status code should be 200

    When I delete the "apis" resource with id "testApiId"
    Then The response status code should be 200
