Feature: Migrated Legacy API Key Invocation

  Background:
    Given The system is ready and I have valid devportal access token for current user

  # Step 1: Find the migrated API and application
  Scenario: Setup - Find migrated API and application
    # Find migrated API from devportal
    When I find the apiUUID of the API with name "APIM18PublisherTest" and version "1.0.0" from devportal
    And I wait until the response status code is 200 and the value of response field "count" is "1"
    And I extract response field "list[0].id" and store it as "<migratedAPIId>"
    When I retrieve the API with id "<migratedAPIId>" from devportal
    And I wait until the response status code is 200
    And I put the response payload in context as "<migratedAPIPayload>"

    # Find migrated application (already subscribed to the migrated API)
    When I fetch the application with name "CustomerApp"
    Then I wait until the response status code is 200
    And I extract response field "list[0].applicationId" and store it as "<migratedAppId>"

  # Step 2: Generate legacy API key, invoke, list, regenerate, invoke, revoke, invoke
  Scenario: Generate, regenerate, revoke legacy API key and invoke API
    # Generate a PRODUCTION legacy API key
    When I put the following JSON payload in context as "<legacyApiKeyGenPayload>"
    """
    {
      "keyName": "LegacyTestKey1",
      "validityPeriod": 7200,
      "additionalProperties": {
        "permittedIP": "",
        "permittedReferer": ""
      }
    }
    """
    And I generate a legacy api key for application "<migratedAppId>" with payload "<legacyApiKeyGenPayload>"
    And I wait until the response status code is 200
    And I extract response field "apikey" and store it as "<legacyApiKey1>"
    And I extract response field "keyName" and store it as "<legacyApiKey1Name>"
    # Allow time for the Gateway cache to sync the new key
    And I wait for 2 seconds

    # List legacy API keys and find the keyUUID by name
    When I get the list of legacy api keys for application "<migratedAppId>" with key type "PRODUCTION"
    And I wait until the response status code is 200
    And I extract response field "$" and store it as "legacyKeysList"
    And I find the resource with following properties in "legacyKeysList" as "matchedKeyObject"
      | keyName |  <legacyApiKey1Name> |
    And I extract field "keyUUID" from "matchedKeyObject" and store it as "legacyKey1UUID"

    # Invoke migrated API with generated legacy key - should succeed
    When I invoke the API resource at path "/apiContext/1.0.0/customers/123/" with method "GET" using api key "legacyApiKey1"
    Then The response status code should be 200

    # Regenerate the legacy API key
    When I regenerate legacy api key "legacyKey1UUID" for application "migratedAppId" with key type "PRODUCTION"
    And I wait until the response status code is 200
    And I extract response field "apikey" and store it as "legacyApiKey1Regenerated"
    # Allow time for the Gateway cache to sync the new key
    And I wait for 2 seconds

    # Invoke API with regenerated key - should succeed
    When I invoke the API resource at path "/apiContext/1.0.0/customers/123/" with method "GET" using api key "legacyApiKey1Regenerated"
    Then The response status code should be 200

    # Invoke API with old key after regeneration - should fail
    When I invoke the API resource at path "/apiContext/1.0.0/customers/123/" with method "GET" using api key "legacyApiKey1"
    Then The response status code should be 401
