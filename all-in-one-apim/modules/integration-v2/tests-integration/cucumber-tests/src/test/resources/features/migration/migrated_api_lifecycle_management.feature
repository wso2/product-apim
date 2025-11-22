Feature: Migrated API Lifecycle Management

  Background:
    Given The system is ready and I have valid access tokens for current user

  Scenario Outline: Migrated API Lifecycle Management
    # Step 1: Find the api
    When I find the apiUUID of the API created with the name "<apiName>" and version "<apiVersion>" as "<apiID>"
    And I retrieve the "apis" resource with id "<apiID>"
    And I put the response payload in context as "<apiPayload>"

    # Step 2: Update subscription tiers to include "Unlimited" : for generalize the below steps
    When I update the "apis" resource "<apiID>" and "<apiPayload>" with configuration type "policies" and value:
      """
      ["Bronze","Gold","Unlimited"]
      """
    Then The response status code should be 200

    # Step 3: Deploy the API
    When I deploy the API with id "<apiID>"
    Then The response status code should be 201
    And I wait for deployment of the resource in "<apiPayload>"

    # Step 4: Publish the API
    When I publish the "apis" resource with id "<apiID>"
    Then The lifecycle status of API "<apiID>" should be "Published"

    # Step 5: Subscribe to api,"createdAppId" and "subscriptionId" saved in cotext
    When I have set up application with keys, subscribed to API "<apiID>", and obtained access token

    # Step 6: Block the subscription
    When I block the subscription with "subscriptionId" for the resource
    Then The response status code should be 200

    # Step 7: Check the subscription "BLOCKED"
    When I retrieve the subscriptions for resource "<apiID>"
    Then The response should contain "BLOCKED"

    # Step 8: Unblock the subscription
    When I unblock the subscription with "subscriptionId" for the resource
    Then The response status code should be 200

    # Step 9: Check the subscription "UNBLOCKED"
    When I retrieve the subscriptions for resource "<apiID>"
    Then The response should contain "UNBLOCKED"

    # Step 7: Remove the subscription and application
    When I delete the subscription with id "<subscriptionId>"
    When I delete the application with id "<createdAppId>"
    Then The response status code should be 200

    Examples:
      | apiName                  | apiVersion   | apiID         |
      | ADPRestAPI               | 2.0.0        | RestApiId     |
      | ADPStarWarsAPI           | 2.0.0        | GraphQLApiId  |
      | ADPPhoneVerificationAPI  | 2.0.0        | SoapApiId     |
      | ADPIfElseAPI             | 2.0.0        | AsyncApiId    |
