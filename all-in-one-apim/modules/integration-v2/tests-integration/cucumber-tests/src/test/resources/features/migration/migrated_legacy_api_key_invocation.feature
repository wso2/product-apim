Feature: Migrated Legacy API Key Invocation

  Background:
    Given The system is ready and I have valid devportal access token for current user

  # Step 1: Find the migrated API
  Scenario: Find migrated API
    # Find migrated API from devportal
    When I find the apiUUID of the API with name "APIM18PublisherTest" and version "1.0.0" from devportal
    And I wait until the response status code is 200 and the value of response field "count" is "1"
    And I extract response field "list[0].id" and store it as "<migratedAPIId>"
    # Retrieve API details and store payload
    When I retrieve the API with id "migratedAPIId" from devportal
    And I wait until the response status code is 200
    And I put the response payload in context as "migratedAPIPayload"

  # Step 2: Invoke the migrated API using old pre-migration API key
  Scenario: Invoke API with old API key generated in 3.2.0
    And I get the generated api key from file "artifacts/accessTokens/api_invocation_api_keys.json"

    When I invoke the API resource at path "/apiContext/1.0.0/customers/123/" with method "GET" using api key "generatedApiKey"
    Then The response status code should be 200
