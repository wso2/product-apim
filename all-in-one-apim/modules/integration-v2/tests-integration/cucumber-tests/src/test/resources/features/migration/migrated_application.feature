Feature: Migrated Applications

  Background:
    Given The system is ready and I have valid access tokens for current user

  # Step 1: Find migrated application and update it
  Scenario: Retrieve and update migrated Application
    When I fetch the application with "CustomerApp" as "migratedAppId"
    Then The response status code should be 200

    When I put JSON payload from file "artifacts/payloads/update_migrated_customerApp.json" in context as "migratedAppUpdatePayload"
    And I update the application "migratedAppId" with payload "migratedAppUpdatePayload"
    Then The response status code should be 200
    And The response should contain "CustomerAppUpdated"
    And The response should contain "50PerMin"

  # Step 2: Create and find resources for next steps
  Scenario: Find/Create applications and APIs for new subscriptions
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
    When I update the "apis" resource "<apiID>" and "<apiPayload>" with configuration type "policies" and value:
      """
     ["Bronze","Gold","Unlimited"]
      """
    Then The response status code should be 200
    And I retrieve the "apis" resource with id "<apiID>"
    And I put the response payload in context as "<apiPayload>"
    And The "apis" resource should reflect the updated "policies" as:
      """
     ["Bronze","Gold","Unlimited"]
      """

    Examples:
      |apiID          |apiPayload        |
      |migratedAPIId  |migratedAPIPayload|
      |RestAPIId      |RestAPIPayload    |
      |ADPRestAPIId   |ADPRestAPIPayload |


  # Step 4: Generate a token from migrated application
  Scenario: Generate Access Token for Migrated application
    # Put current consumerSecret, keyMappingId in context for generating access token
    When I retrieve existing application keys for "migratedAppId"
    Then The response status code should be 200

    When I put the following JSON payload in context as "<AccessTokenPayload>"
    """
    {
      "consumerSecret": "{{appConsumerSecret}}",
      "validityPeriod": 3600
    }
    """
    And I request an access token for application id "migratedAppId" using payload "AccessTokenPayload"
    Then The response status code should be 200

  # Step 5: Update keys in the migrated application(not same as generate keys)
  Scenario: Update keys
    When I put JSON payload from file "artifacts/payloads/update_keys_of_migrated_customerApp.json" in context as "updateKeysPayload"
    And I update the keys for application with "migratedAppId"
    Then The response status code should be 200
    And The response should contain "http://sample.com/callback/url"

  # Step 6: Delete generated keys
  Scenario: Delete generated keys
    When I delete the generated keys for "migratedAppId"
    Then The response status code should be 200

  # Step 7: Change subscription plan for migrated subscription
  Scenario: Change subscription plan for migrated application
    When I retrieve the subscription for Api "migratedAPIId" by Application "migratedAppId"
    Then The response status code should be 200

    When I get the subscription with id "subscriptionId"
    Then The response status code should be 200
    And I put the response payload in context as "subscriptionPayload"

    When I update the subscription "subscriptionId" with subscription plan "Bronze"
    Then The response status code should be 200

  Scenario Outline: Unsubscribe/Subscribe existing and new APIs
    # Step 8: Subscribe APIs
    When I put the following JSON payload in context as "<apiSubscriptionPayload>"
    """
    {
      "applicationId": "{{applicationId}}",
      "apiId": "{{apiId}}",
      "throttlingPolicy": "Bronze"
    }
    """
    And I subscribe to API "<apiID>" using application "<applicationId>" with payload "<apiSubscriptionPayload>" as "<subscriptionId>"
    Then The response status code should be 200

    # Step 9: Block the subscription
    When I block the subscription with "<subscriptionId>" for the resource
    Then The response status code should be 200

    When I retrieve the subscriptions for resource "<apiID>"
    Then The response should contain "BLOCKED"

    # Step 10: Unblock the subscription
    When I unblock the subscription with "<subscriptionId>" for the resource
    Then The response status code should be 200

    When I retrieve the subscriptions for resource "<apiID>"
    Then The response should contain "UNBLOCKED"

    When I delete the subscription with id "<subscriptionId>"
    Then The response status code should be 200
      Examples:
      | apiID       | subscriptionId           | applicationId   |
      |ADPRestAPIId |migratedAPISubscriptionId | migratedAppId   |
      | RestAPIId   |newAPISubscriptionId      | migratedAppId   |
      |ADPRestAPIId |migratedAPISubscriptionId | createdAppId    |
      | RestAPIId   |newAPISubscriptionId      | createdAppId    |

  # Step 10: Remove created resources
  Scenario: Delete the created resources
    When I delete the application with id "createdAppId"
    Then The response status code should be 200

    When I delete the "apis" resource with id "RestAPIId"
    Then The response status code should be 200

 # Step 11: Remove migrated application
  Scenario: Delete migrated application
    When I delete the application with id "migratedAppId"
    Then The response status code should be 200
