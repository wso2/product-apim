Feature: Migrated API Lifecycle Management

  Background:
    Given The system is ready and I have valid access tokens for current user

  Scenario Outline: Migrated API Lifecycle Management
    # Find the api
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
    And I put the response payload in context as "<apiPayload>"

    # Deploy the API
    When I deploy the API with id "<apiID>"
    And I wait until the response status code is 201
    And I wait for deployment of the resource in "<apiPayload>"

    # Publish the API
    When I execute lifecycle action "Publish" on "apis" resource "<apiID>" and wait for state "Published"

    # Demote the API to created state
    When I execute lifecycle action "Demote to Created" on "apis" resource "<apiID>" and wait for state "Created"

    Examples:
      | apiName                  | apiVersion   | apiID         |
      | ADPRestAPI               | 2.0.0        | RestApiId     |
      | ADPStarWarsAPI           | 2.0.0        | GraphQLApiId  |
      | ADPPhoneVerificationAPI  | 2.0.0        | SoapApiId     |
      | ADPIfElseAPI             | 2.0.0        | AsyncApiId    |
