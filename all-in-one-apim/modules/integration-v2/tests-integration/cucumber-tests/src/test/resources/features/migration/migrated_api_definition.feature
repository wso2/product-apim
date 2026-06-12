Feature: Migrated API Definition

  Background:
    Given The system is ready and I have valid access tokens for current user

# Step 1: Find the api
  Scenario: Migrated API Retrieval
    When I find the API created with the name "ADPRestAPI" and version "1.0.0"
    And I wait until the response status code is 200
    And I extract response field "count" and store it as "<apiCount>"
    And the actual value of "<apiCount>" should match the expected value:
      """
      1
      """
    And I extract response field "list[0].id" and store it as "ADPRestAPIId"

    And I retrieve "apis" resource definition for "ADPRestAPIId"
    And I wait until the response status code is 200

# Step 2: Update the swagger definition
  Scenario: Update swagger definition
    When I update the "apis" resource definition with "artifacts/payloads/ADPRestAPISwaggerDefinition.json" for "ADPRestAPIId"
    And I wait until the response status code is 200
    And The response should not contain "/hello"

# Step 3: Verify the update
  Scenario: Update Verification
    When I retrieve "apis" resource definition for "ADPRestAPIId"
    And I wait until the response status code is 200
    And The response should not contain "/hello"
