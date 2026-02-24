Feature: Migrated API Provider Change

  Background:
    Given The system is ready and I have valid access tokens for current user

# Step 1: Find the api
  Scenario Outline: Migrated API Retrieval
    When I find the apiUUID of the API created with the name "<apiName>" and version "<apiVersion>" as "<apiID>"
    And I retrieve the "apis" resource with id "<apiID>"

    Examples:
      | apiName                  | apiVersion   | apiID         |
      | ADPRestAPI               | 1.0.0        | RestApiId     |
      | ADPStarWarsAPI           | 1.0.0        | GraphQLApiId  |

# Step 2: Change api provider
  Scenario Outline: Change API provider
    When I update the api provider with "<providerName>" for "<apiID>"
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "<apiID>"
    And The "apis" resource should reflect the updated "provider" as:
      """
      <providerName>
      """

    Examples:
      |apiID          |providerName              |
      | RestApiId     |   admin                  |
      | GraphQLApiId  |   admin                  |
