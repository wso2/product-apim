Feature: Migrated API Versioning

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
    And I put the response payload in context as "<apiUpdatePayload>"

    Examples:
      | apiName                  | apiVersion   | apiID         | apiUpdatePayload              |
      | ADPRestAPI               | 1.0.0        | RestApiId     | ADPRestAPIPayload             |
      | ADPStarWarsAPI           | 1.0.0        | GraphQLApiId  | ADPGraphQLAPIPayload          |
      | ADPPhoneVerificationAPI  | 1.0.0        | SoapApiId     | ADPPhoneVerificationAPIPayload|
      | ADPIfElseAPI             | 1.0.0        | AsyncApiId    | ADPIfElseAPIPayload           |


  Scenario Outline: Create new versions of migrated APIs
    # Step 2: Create a new version
    When I create a new version "<newVersion>" of "apis" resource "<apiID>" with default version "<defaultProperty>"
    And I wait until the response status code is <expectedStatus>
    And The response should contain "<newVersion>"
    And The response should contain "<defaultProperty>"
    And I extract response field "id" and store it as "<newVersionId>"
    Then I get the lifecycle status of API "<newVersionId>"
    Then I wait until the response status code is 200 and the value of response field "state" is "<expectedLifecycle>"

    # Step 3: Enable/disable default versioning
    When I update the "apis" resource "<apiID>" and "<apiUpdatePayload>" with configuration type "<configType>" and value:
      """
      <configValue>
      """
    And I wait until the response status code is 200
    When I retrieve the "apis" resource with id "<apiID>"
    And I wait until the response status code is 200
    And The "apis" resource should reflect the updated "<configType>" as:
      """
      <configValue>
      """

    # Step 4: Delete the version
    When I delete the "apis" resource with id "<newVersionId>"
    And I wait until the response status code is 200

    Examples:
    | apiID       | newVersion | defaultProperty | expectedStatus | newVersionId    | expectedLifecycle | apiUpdatePayload              | configType             | configValue    |
    | RestApiId   | 5.0.0      | false           | 201            | newVersionApiId | Created           |ADPRestAPIPayload              |  isDefaultVersion      |     true       |
    | RestApiId   | 6.0.0      | true            | 201            | newVersionApiId | Created           |ADPRestAPIPayload              |  isDefaultVersion      |     false      |
    | GraphQLApiId| 5.0.0      | false           | 201            | newVersionApiId | Created           |ADPGraphQLAPIPayload           |  isDefaultVersion      |     true       |
    | SoapApiId   | 5.0.0      | false           | 201            | newVersionApiId | Created           |ADPPhoneVerificationAPIPayload |  isDefaultVersion      |     true       |
    | AsyncApiId  | 5.0.0      | false           | 201            | newVersionApiId | Created           |ADPIfElseAPIPayload            |  isDefaultVersion      |     true       |
