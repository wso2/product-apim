Feature: Migrated API Lifecycle Management

  Background:
    Given The system is ready and I have valid access tokens for current user

  Scenario Outline: Migrated API Lifecycle Management
    # Step 1: Find the api
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


    # Step 3: Deploy the API
    When I deploy the API with id "<apiID>"
    And I wait until the response status code is 201
    And I wait for deployment of the resource in "<apiPayload>"

    # Step 4: Publish the API
    When I publish the "apis" resource with id "<apiID>"
    And I wait until the response status code is 200
    Then I get the lifecycle status of API "<apiID>"
    Then I wait until the response status code is 200 and the value of response field "state" is "Published"

    # Step 5: Demote the API to created state
    When I demote the "apis" resource with id "<apiID>" to created state
    And I wait until the response status code is 200
    Then I get the lifecycle status of API "<apiID>"
    Then I wait until the response status code is 200 and the value of response field "state" is "Created"

    Examples:
      | apiName                  | apiVersion   | apiID         |
      | ADPRestAPI               | 2.0.0        | RestApiId     |
      | ADPStarWarsAPI           | 2.0.0        | GraphQLApiId  |
      | ADPPhoneVerificationAPI  | 2.0.0        | SoapApiId     |
      | ADPIfElseAPI             | 2.0.0        | AsyncApiId    |
