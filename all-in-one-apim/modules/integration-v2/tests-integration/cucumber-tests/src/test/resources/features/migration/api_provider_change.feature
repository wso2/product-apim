Feature: Migrated API Provider Change

  Background:
    Given The system is ready and I have valid access tokens for current user

# Step 1: Find the api
  Scenario Outline: Migrated API Retrieval
    When I find the API created with the name "<apiName>" and version "<apiVersion>"
    And I wait until the response status code is 200
    And I extract response field "count" and store it as "<apiCount>"
    And the actual value of "<apiCount>" should match the expected value:
      """
      1
      """
    And I extract response field "list[0].id" and store it as "<apiID>"

    And I retrieve the "apis" resource with id "<apiID>"
    And I wait until the response status code is 200

    Examples:
      | apiName                  | apiVersion   | apiID         |
      | ADPRestAPI               | 1.0.0        | RestApiId     |
      | ADPStarWarsAPI           | 1.0.0        | GraphQLApiId  |

# Step 2: Change api provider
  Scenario Outline: Change API provider
    When I update the api provider with "<providerName>" for "<apiID>"
    And I wait until the response status code is 200
    When I retrieve the "apis" resource with id "<apiID>"
    And I wait until the response status code is 200
    And The "apis" resource should reflect the updated "provider" as:
      """
      <providerName>
      """

    Examples:
      |apiID          |providerName              |
      | RestApiId     |   admin                  |
      | GraphQLApiId  |   admin                  |
