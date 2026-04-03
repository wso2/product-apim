Feature: Migrated Legacy API Key Invocation

  Background:
    Given The system is ready and I have valid devportal access token for current user

  # Step 1: Find the migrated API and application
  Scenario: Setup - Find migrated API and application
    # Find migrated API from devportal
    When I find the apiUUID of the API with name "APIM18PublisherTest" and version "1.0.0" from devportal as "migratedAPIId"
    Then The response status code should be 200
    When I retrieve the API with id "migratedAPIId" from devportal
    Then The response status code should be 200
    And I put the response payload in context as "migratedAPIPayload"

    # Find migrated application (already subscribed to the migrated API)
    When I fetch the application with "CustomerApp" as "migratedAppId"
    Then The response status code should be 200

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
    And I generate a legacy api key for application "migratedAppId" with payload "<legacyApiKeyGenPayload>" as "legacyApiKey1"
    Then The response status code should be 200

    # List legacy API keys and find the keyUUID by name
    When I find the keyUUID of legacy api key "legacyApiKey1Name" for application "migratedAppId" with key type "PRODUCTION" as "legacyKey1UUID"

    # Invoke migrated API with generated legacy key - should succeed
    When I invoke the API resource at path "/apiContext/1.0.0/customers/123/" with method "GET" using api key "legacyApiKey1"
    Then The response status code should be 200

    # Regenerate the legacy API key
    When I regenerate legacy api key "legacyKey1UUID" for application "migratedAppId" with key type "PRODUCTION" as "legacyApiKey1Regenerated"
    Then The response status code should be 200

    # Invoke API with regenerated key - should succeed
    When I invoke the API resource at path "/apiContext/1.0.0/customers/123/" with method "GET" using api key "legacyApiKey1Regenerated"
    Then The response status code should be 200

    # Invoke API with old key after regeneration - should fail
    When I invoke the API resource at path "/apiContext/1.0.0/customers/123/" with method "GET" using api key "legacyApiKey1"
    Then The response status code should be 401
