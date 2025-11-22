Feature: Migrated API Versioning

  Background:
    Given The system is ready and I have valid access tokens for current user

# Step 1: Find the api
  Scenario Outline: Migrated API Retrieval
    When I find the apiUUID of the API created with the name "<apiName>" and version "<apiVersion>" as "<apiID>"
    And I retrieve the "apis" resource with id "<apiID>"
    And I put the response payload in context as "<apiUpdatePayload>"

    Examples:
      | apiName                  | apiVersion   | apiID         | apiUpdatePayload              |
      | ADPRestAPI               | 1.0.0        | RestApiId     | ADPRestAPIPayload             |
      | ADPStarWarsAPI           | 1.0.0        | GraphQLApiId  | ADPGraphQLAPIPayload          |
      | ADPPhoneVerificationAPI  | 1.0.0        | SoapApiId     | ADPPhoneVerificationAPIPayload|
      | ADPIfElseAPI             | 1.0.0        | AsyncApiId    | ADPIfElseAPIPayload           |

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

  Scenario Outline: Create new versions of migrated APIs
    # Step 3: Create a new version
    When I create a new version "<newVersion>" of "apis" resource "<apiID>" with default version "<defaultProperty>" as "<newVersionId>"
    Then The response status code should be <expectedStatus>
    And The response should contain "<newVersion>"
    And The response should contain "<defaultProperty>"
    And The lifecycle status of API "<newVersionId>" should be "<expectedLifecycle>"

    # Step 4: Enable/disable default versioning
    When I update the "apis" resource "<apiID>" and "<apiUpdatePayload>" with configuration type "<configType>" and value:
      """
      <configValue>
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "<apiID>"
    And The "apis" resource should reflect the updated "<configType>" as:
      """
      <configValue>
      """

    # Step 5: Delete the version
    When I delete the "apis" resource with id "<newVersionId>"
    Then The response status code should be 200

    Examples:
    | apiID       | newVersion | defaultProperty | expectedStatus | newVersionId    | expectedLifecycle | apiUpdatePayload              | configType             | configValue    |
    | RestApiId   | 5.0.0      | false           | 201            | newVersionApiId | Created           |ADPRestAPIPayload              |  isDefaultVersion      |     true       |
    | RestApiId   | 6.0.0      | true            | 201            | newVersionApiId | Created           |ADPRestAPIPayload              |  isDefaultVersion      |     false      |
    | GraphQLApiId| 5.0.0      | false           | 201            | newVersionApiId | Created           |ADPGraphQLAPIPayload           |  isDefaultVersion      |     true       |
    | SoapApiId   | 5.0.0      | false           | 201            | newVersionApiId | Created           |ADPPhoneVerificationAPIPayload |  isDefaultVersion      |     true       |
    | AsyncApiId  | 5.0.0      | false           | 201            | newVersionApiId | Created           |ADPIfElseAPIPayload            |  isDefaultVersion      |     true       |

