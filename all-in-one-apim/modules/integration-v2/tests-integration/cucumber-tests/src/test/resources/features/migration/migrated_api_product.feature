
Feature: Migrated API Products

  Background:
    Given The system is ready and I have valid access tokens for current user

  Scenario: Update the migrated product

    # Step 1: Find api Ids to update the api product
    When I find the API created with the name "APIM18PublisherTest" and version "1.0.0"
    And I wait until the response status code is 200
    And I extract response field "count" and store it as "<apiCount>"
    And the actual value of "<apiCount>" should match the expected value:
      """
      1
      """
    And I extract response field "list[0].id" and store it as "firstApiUuid"

    When I find the API created with the name "ADPRestAPI" and version "1.0.0"
    And I wait until the response status code is 200
    And I extract response field "count" and store it as "<apiCount>"
    And the actual value of "<apiCount>" should match the expected value:
      """
      1
      """
    And I extract response field "list[0].id" and store it as "secondApiUuid"

    # Step 2: Find api product
    When I find the api product created with the name "ADPAPIProduct"
    And I wait until the response status code is 200 and the value of response field "count" is "1"
    And I extract response field "list[0].id" and store it as "<apiProductId>"

    And  I retrieve the "api-products" resource with id "apiProductId"
    And I wait until the response status code is 200
    And I put the response payload in context as "<apiProductPayload>"
          
    # Step 3: Update subscription tiers to include "Unlimited" : for generalize the below steps
    When I update the "api-products" resource "apiProductId" and "<apiProductPayload>" with configuration type "policies" and value:
    """
    ["Bronze","Gold","Unlimited"]
    """
    And I wait until the response status code is 200
    And I retrieve the "api-products" resource with id "apiProductId"
    And I wait until the response status code is 200
    And The "api-products" resource should reflect the updated "policies" as:
      """
    ["Bronze","Gold","Unlimited"]
      """

    # Step 4: Update the api product to modify properties in resources
    When I put JSON payload from file "artifacts/payloads/update_apim_adpapiproduct.json" in context as "<apiProductUpdatePayload>"
    And I update api product resource of id "apiProductId" with payload "<apiProductUpdatePayload>"
    And I wait until the response status code is 200
    And The response should contain "/customers/{id}"

  Scenario: Subscribe to application
    # Step 1: Check lifecycle status and subscribe to product
    When I retrieve the "api-products" resource with id "apiProductId"
    And I wait until the response status code is 200
    Then I get the lifecycle status of API "<apiProductId>"
    Then I wait until the response status code is 200 and the value of response field "state" is "Published"

    And I have set up application with keys, subscribed to API "<apiProductId>", store subscription id as "<subscriptionId>" and access token as "<generatedAccessToken>"

  Scenario: Migrated API Products Revisions
    # Step 1: Find the api product
    When I find the api product created with the name "ADPAPIProduct"
    And I wait until the response status code is 200 and the value of response field "count" is "1"
    And I extract response field "list[0].id" and store it as "<apiProductId>"

    # Step 2: Get exiting revisions
    When I get the existing revision for "api-products" resource with "apiProductId"
    And I wait until the response status code is 200
    And I extract response field "list[0].id" and store it as "<existingRevisionID>"

    # Step 3: Create a new revision of an existing API and save in context as "revisionId"
    When I put the following JSON payload in context as "<createRevisionPayload>"
    """
    {
      "description":"Another new Revision"
    }
    """
    And I make a request to create a revision for "api-products" resource "apiProductId" with payload "<createRevisionPayload>"
    And I wait until the response status code is 201
    And I extract response field "id" and store it as "<revisionId>"
    And I wait for 3 seconds

    # At this point, we have newly created revision as "revisionId" and already existing first revision as "existingRevisionID"

    # Step 4: Deploy a specific revision to a gateway environment
    When I deploy revision "revisionId" of "api-products" resource "apiProductId"
    And I wait until the response status code is 201
    And I wait until "api-products" "apiProductId" revision is deployed in the gateway

    # Step 5: Invoke api product
    When I invoke the API resource at path "/adp-api-product/1.0.0/customers/123" with method "GET" using access token "<generatedAccessToken>" and payload ""
    Then The response status code should be 200

    # Step 6: Undeploy an existing API revision
    When I undeploy revision "revisionId" of "api-products" resource "apiProductId"
    And I wait until the response status code is 201

    # Step 7: Restore a previous revision to make it the current working copy
    When I restore a previous revision "revisionId" of "api-products" resource "apiProductId"
    And I wait until the response status code is 201

    # Step 8: Delete an unused or old revision, created revision
    When I Delete the "api-products" resource revision with "revisionId" for "apiProductId"
    And I wait until the response status code is 200

  Scenario: Migrated API Products LifeCycle

    # Step 1: Find the api product
    When I retrieve the "api-products" resource with id "apiProductId"
    And I wait until the response status code is 200
    And I put the response payload in context as "<apiProductPayload>"

    # Step 2: Block the subscription
    When I block the subscription with "subscriptionId" for the resource
    And I wait until the response status code is 200

    # Step 3: Check the subscription "BLOCKED"
    When I retrieve the subscriptions for resource "apiProductId"
    Then The response should contain "BLOCKED"

    # Step 4: Unblock the subscription
    When I unblock the subscription with "subscriptionId" for the resource
    And I wait until the response status code is 200

    # Step 5: Check the subscription "UNBLOCKED"
    When I retrieve the subscriptions for resource "apiProductId"
    Then The response should contain "UNBLOCKED"

  Scenario: Delete the subscription
    When I delete the subscription with id "<subscriptionId>"
    And I wait until the response status code is 200

  Scenario: Cleanup
    When I delete the application with id "<createdAppId>"
    And I wait until the response status code is 200
