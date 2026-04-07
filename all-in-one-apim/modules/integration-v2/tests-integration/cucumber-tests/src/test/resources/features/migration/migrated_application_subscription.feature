Feature: Migrated/New API subscription using Migrated/New Applications

  Background:
    Given The system is ready and I have valid access tokens for current user

  # Step 1: Find migrated application
  Scenario: Retrieve the migrated Application
    When I fetch the application with "CustomerApp" as "migratedAppId"
    Then The response status code should be 200

  # Step 2: Find an existing API and Create a new API and Application for subscriptions
  Scenario: Prepare APIs and New Application for Subscriptions
    # Existing subscription for CustomerApp
    When I find the apiUUID of the API created with the name "APIM18PublisherTest" and version "1.0.0" as "migratedAPIId"
    And I retrieve the "apis" resource with id "migratedAPIId"
    Then The response status code should be 200
    And I put the response payload in context as "migratedAPIPayload"

    # Exiting API to be subscribed
    When I find the apiUUID of the API created with the name "ADPRestAPI" and version "1.0.0" as "ADPRestAPIId"
    And I retrieve the "apis" resource with id "ADPRestAPIId"
    Then The response status code should be 200
    And I put the response payload in context as "ADPRestAPIPayload"

    # New API to be subscribed
    Given I have created an api from "artifacts/payloads/create_apim_test_api.json" as "RestAPIId" and deployed it
    And I retrieve the "apis" resource with id "RestAPIId"
    And I put the response payload in context as "RestAPIPayload"
    And I wait for deployment of the resource in "RestAPIPayload"
    And I publish the "apis" resource with id "RestAPIId"
    Then The response status code should be 200
    Then The lifecycle status of API "RestAPIId" should be "Published"

    # New application
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "<createAppPayload>"
    When I create an application with payload "<createAppPayload>"
    And I retrieve the application with id "createdAppId"
    Then The response status code should be 200

  # Step 3: Update the api subscription policies before subscribing
  Scenario Outline: Update policies
    When I update the "apis" resource "<apiContextKey>" and "<apiPayloadContextKey>" with configuration type "policies" and value:
      """
     ["Bronze","Gold","Unlimited"]
      """
    Then The response status code should be 200
    And I retrieve the "apis" resource with id "<apiContextKey>"
    And I put the response payload in context as "<apiPayloadContextKey>"
    And The "apis" resource should reflect the updated "policies" as:
      """
     ["Bronze","Gold","Unlimited"]
      """

    Examples:
      |apiContextKey  |apiPayloadContextKey |
      |migratedAPIId  |migratedAPIPayload   |
      |RestAPIId      |RestAPIPayload       |
      |ADPRestAPIId   |ADPRestAPIPayload    |

  # Step 7: Change subscription plan for migrated subscription
  Scenario: Change subscription plan for migrated application
    When I retrieve the subscription for Api "migratedAPIId" by Application "migratedAppId"
    Then The response status code should be 200

    When I get the subscription with id "subscriptionId"
    Then The response status code should be 200
    And I put the response payload in context as "subscriptionPayload"

    When I update the subscription "subscriptionId" with subscription plan "Bronze"
    Then The response status code should be 200

  Scenario Outline: Subscribe, block, unblock, and delete subscriptions for existing and new APIs
    # Step 8: Subscribe APIs
    When I put value "<apiContextKey>" in context as "selectedApiId"
    And I put value "<applicationContextKey>" in context as "selectedApplicationId"
    And I put the following JSON payload in context as "apiSubscriptionPayload"
    """
    {
      "applicationId": "{{selectedApplicationId}}",
      "apiId": "{{selectedApiId}}",
      "throttlingPolicy": "Bronze"
    }
    """
    And I create a subscription using payload "apiSubscriptionPayload"
    Then The response status code should be 201
    And I extract response field "subscriptionId" and store it as "<subscriptionContextKey>"

    # Block the subscription
    When I block the subscription with "<subscriptionContextKey>" for the resource
    Then The response status code should be 200

    When I retrieve the subscriptions for resource "<apiContextKey>"
    Then The response should contain "BLOCKED"

    # Unblock the subscription
    When I unblock the subscription with "<subscriptionContextKey>" for the resource
    Then The response status code should be 200

    When I retrieve the subscriptions for resource "<apiContextKey>"
    Then The response should contain "UNBLOCKED"

    # Delete the subscription
    When I delete the subscription with id "<subscriptionContextKey>"
    Then The response status code should be 200

    Examples:
      | apiContextKey | subscriptionContextKey   | applicationContextKey |
      | ADPRestAPIId  | migratedApiSubscriptionId | migratedAppId         |
      | RestAPIId     | newApiSubscriptionId      | migratedAppId         |
      | ADPRestAPIId  | migratedApiSubscriptionId | createdAppId          |
      | RestAPIId     | newApiSubscriptionId      | createdAppId          |

  # Step 10: Remove created resources
  Scenario: Delete the created resources
    When I delete the application with id "createdAppId"
    Then The response status code should be 200

    When I delete the "apis" resource with id "RestAPIId"
    Then The response status code should be 200
