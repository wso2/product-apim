Feature: API-bound API Key with Migrated Artifacts

  Background:
    Given The system is ready and I have valid devportal access token for current user

  # Step 1: Find migrated API and application, ensure api_key security is enabled
  Scenario: Setup - Find migrated resources and enable API key security
    # Find migrated API from devportal
    When I find the apiUUID of the API with name "APIM18PublisherTest" and version "1.0.0" from devportal as "migratedAPIId"
    Then The response status code should be 200
    # Retrieve API details and store payload
    When I retrieve the API with id "migratedAPIId" from devportal
    Then The response status code should be 200
    And I put the response payload in context as "migratedAPIPayload"

    # Find migrated application (already subscribed to the migrated API)
    When I fetch the application with "CustomerApp" as "migratedAppId"
    Then The response status code should be 200

  # Step 2: Generate key, associate from API side, invoke, dissociate, invoke(fail), re-associate, revoke, invoke(fail)
  Scenario: Generate API key, associate, dissociate, and revoke from API side
    # Generate API-bound API key
    When I put the following JSON payload in context as "<apiKeyGenPayload>"
    """
    {
      "keyName": "MigratedTestKey1",
      "keyType": "PRODUCTION",
      "validityPeriod": 3600,
      "additionalProperties": {
        "permittedIP": "",
        "permittedReferer": ""
      }
    }
    """
    And I generate an api-bound api key for api "migratedAPIId" with payload "<apiKeyGenPayload>" as "apiBoundApiKey1"
    Then The response status code should be 200

    # Find the keyUUID of the generated key
    When I find the keyUUID of api key "apiBoundApiKey1Name" for api "migratedAPIId" as "apiKey1UUID"

    # Associate key to the migrated application from API side
    When I associate api key "apiKey1UUID" to application "migratedAppId" from api "migratedAPIId"
    Then The response status code should be 200

    # Invoke migrated API with associated key - should succeed
    When I invoke the API resource at path "/apiContext/1.0.0/customers/123/" with method "GET" using api key "apiBoundApiKey1"
    Then The response status code should be 200

    # Dissociate key from application
    When I dissociate api key "apiKey1UUID" from api "migratedAPIId"
    Then The response status code should be 200

    # Invoke migrated API with dissociated key - should fail
    When I invoke the API resource at path "/apiContext/1.0.0/customers/123/" with method "GET" using api key "apiBoundApiKey1"
    Then The response status code should be 403

    # Re-associate to test revoke behavior
    When I associate api key "apiKey1UUID" to application "migratedAppId" from api "migratedAPIId"
    Then The response status code should be 200

    # Revoke the API key
    When I revoke api key "apiKey1UUID" for api "migratedAPIId"
    Then The response status code should be 200

    # Invoke migrated API with revoked key - should fail
    When I invoke the API resource at path "/apiContext/1.0.0/customers/123/" with method "GET" using api key "apiBoundApiKey1"
    Then The response status code should be 401

  # Step 3: Generate new key, associate from app side, invoke, regenerate, invoke
  Scenario: Associate from application side and regenerate API key
    # Generate a second API key
    When I put the following JSON payload in context as "<apiKeyGenPayload2>"
    """
    {
      "keyName": "MigratedTestKey2",
      "keyType": "PRODUCTION",
      "validityPeriod": 3600,
      "additionalProperties": {
        "permittedIP": "",
        "permittedReferer": ""
      }
    }
    """
    And I generate an api-bound api key for api "migratedAPIId" with payload "<apiKeyGenPayload2>" as "apiBoundApiKey2"
    Then The response status code should be 200

    # Find the keyUUID
    When I find the keyUUID of api key "apiBoundApiKey2Name" for api "migratedAPIId" as "apiKey2UUID"

    # Associate from application side
    When I associate api key "apiKey2UUID" for api "migratedAPIId" to application "migratedAppId" with key type "PRODUCTION"
    Then The response status code should be 200

    # Invoke migrated API with the new associated key - should succeed
    When I invoke the API resource at path "/apiContext/1.0.0/customers/123/" with method "GET" using api key "apiBoundApiKey2"
    Then The response status code should be 200

    # Regenerate the API key
    When I regenerate api key "apiKey2UUID" for api "migratedAPIId" as "apiBoundApiKey2Regenerated"
    Then The response status code should be 200

    # Invoke with regenerated key - should succeed (association maintained)
    When I invoke the API resource at path "/apiContext/1.0.0/customers/123/" with method "GET" using api key "apiBoundApiKey2Regenerated"
    Then The response status code should be 200

    # Old key should no longer work after regeneration
    When I invoke the API resource at path "/apiContext/1.0.0/customers/123/" with method "GET" using api key "apiBoundApiKey2"
    Then The response status code should be 401
