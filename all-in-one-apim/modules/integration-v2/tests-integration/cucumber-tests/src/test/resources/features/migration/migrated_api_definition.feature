Feature: Migrated API Definition

  Background:
    Given The system is ready and I have valid access tokens for current user

# Step 1: Find the api
  Scenario: Migrated API Retrieval
    When I find the apiUUID of the API created with the name "ADPRestAPI" and version "1.0.0" as "ADPRestAPIId"
    And I retrieve "apis" resource definition for "ADPRestAPIId"
    Then The response status code should be 200
    And The response should contain "/hello"

# Step 2: Update the swagger definition
  Scenario: Update swagger definition
    When I update the "apis" resource definition with "artifacts/payloads/ADPRestAPISwaggerDefinition.json" for "ADPRestAPIId"
    Then The response status code should be 200
    And The response should not contain "/hello"

# Step 3: Verify the update
  Scenario: Update Verification
    When I retrieve "apis" resource definition for "ADPRestAPIId"
    Then The response status code should be 200
    And The response should not contain "/hello"
