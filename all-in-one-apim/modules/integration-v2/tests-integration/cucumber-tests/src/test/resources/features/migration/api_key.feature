Feature: New product creation

  Background:
    Given The system is ready and I have valid access tokens for current user

  Scenario: Find, Create APIs
    # Step 1: Create new api
    Given I have created an api from "artifacts/payloads/create_apim_test_api.json" as "RestAPIId" and deployed it
    And I retrieve the "apis" resource with id "RestAPIId"
    And I put the response payload in context as "RestAPIPayload"

    And I wait for deployment of the resource in "RestAPIPayload"
    And I publish the "apis" resource with id "RestAPIId"
    Then The response status code should be 200
    Then The lifecycle status of API "RestAPIId" should be "Published"

    # Step 2: Find migrated api
    When I find the apiUUID of the API created with the name "APIM18PublisherTest" and version "1.0.0" as "migratedAPIId"
    And I retrieve the "apis" resource with id "migratedAPIId"
    Then The response status code should be 200
    And I put the response payload in context as "migratedAPIPayload"

    # Step 3: Create an application
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "<createAppPayload>"
    And I create an application with payload "<createAppPayload>"
    Then The response status code should be 201

  # Step 4: Update api to use api key
  Scenario Outline: Update api to use API key
    When I update the "apis" resource "<apiID>" and "<apiUpdatePayload>" with configuration type "securityScheme" and value:
      """
      ["api_key", "oauth_basic_auth_api_key_mandatory", "oauth2"]
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "<apiID>"
    And The "apis" resource should reflect the updated "securityScheme" as:
      """
      ["api_key", "oauth_basic_auth_api_key_mandatory", "oauth2"]
      """

    When I retrieve the "apis" resource with id "<apiID>"
    Then The response status code should be 200
    And I put the response payload in context as "<apiUpdatePayload>"
    When I deploy the API with id "<apiID>"
    Then The response status code should be 201
    And I wait until "apis" "<apiID>" revision is deployed in the gateway

    When I put the following JSON payload in context as "<apiSubscriptionPayload>"
    """
    {
      "applicationId": "{{applicationId}}",
      "apiId": "{{apiId}}",
      "throttlingPolicy": "Unlimited"
    }
    """
    And I subscribe to API "<apiID>" using application "createdAppId" with payload "<apiSubscriptionPayload>" as "<subscriptionId>"

    When I put the following JSON payload in context as "<apiKeyGenerationPayload>"
    """
    {
      "validityPeriod": 3600,
      "additionalProperties": {
        "permittedIP": "",
        "permittedReferer": ""
      }
    }
    """
    And I request an api key for application id "createdAppId" using payload "<apiKeyGenerationPayload>"
    Then The response status code should be 200

    # Step 5: Invoking the API
    When I invoke the API resource at path "<resourcePath>" with method "GET" using api key "apiKey"
    Then The response status code should be 200

    Examples:
      | apiID            |  apiUpdatePayload          | subscriptionId   | resourcePath                         |
      | RestAPIId        | RestAPIPayload             | restApiSubId     | /apiTestContext/1.0.0/customers/123/ |
      | migratedAPIId    | migratedAPIPayload         | migratedApiSubId | /apiContext/1.0.0/customers/123      |


  # Step 6: Delete newy created resources
  Scenario: Delete the API
    When I delete the application with id "createdAppId"
    Then The response status code should be 200

    When I delete the "apis" resource with id "RestAPIId"
    Then The response status code should be 200