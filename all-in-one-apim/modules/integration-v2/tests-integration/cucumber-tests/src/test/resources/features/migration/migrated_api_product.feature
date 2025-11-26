# check for the migrated product

# Update existing product

# Modify properties in resources

Feature: Migrated API Products

  Background:
    Given The system is ready and I have valid access tokens for current user

  Scenario: Migrated API Products Revisions
    # Step 1: Find the api product
    When I find the api product created with the name "ADPAPIProduct" as "apiProductId"
    Then The response status code should be 200

    # Step 2: Get exiting revisions
    When I get the existing revision as "existingRevisionID" for "api-products" resource with "apiProductId"
    Then The response status code should be 200

    # Step 3: Create a new revision of an existing API and save in context as "revisionId"
    When I put the following JSON payload in context as "<createRevisionPayload>"
    """
    {
      "description":"Another new Revision"
    }
    """
    And I make a request to create a revision for "api-products" resource "apiProductId" with payload "<createRevisionPayload>"
    Then The response status code should be 201

    # At this point, we have newly created revision as "revisionId" and already existing first revision as "existingRevisionID"

    # Step 4: Deploy a specific revision to a gateway environment
    When I deploy revision "revisionId" of "api-products" resource "apiProductId"
    Then The response status code should be 201

    # Step 5: Undeploy an existing API revision
    When I undeploy revision "revisionId" of "api-products" resource "apiProductId"
    Then The response status code should be 201

    # Step 6: Restore a previous revision to make it the current working copy
    When I restore a previous revision "revisionId" of "api-products" resource "apiProductId"
    Then The response status code should be 201

    # Step 7: Delete an unused or old revision, created revision
    When I Delete the "api-products" resource revision with "revisionId" for "apiProductId"
    Then The response status code should be 200

  Scenario: Migrated API Products LifeCycle

    # Step 1: Find the api product
    When I retrieve the "api-products" resource with id "apiProductId"
    And I put the response payload in context as "<apiProductPayload>"

    # Step 2: Update subscription tiers to include "Unlimited" : for generalize the below steps
    When I update the "api-products" resource "apiProductId" and "<apiProductPayload>" with configuration type "policies" and value:
      """
      ["Bronze","Gold","Unlimited"]
      """
    Then The response status code should be 200

    # Step 3: Check lifecycle status
    When I retrieve the "api-products" resource with id "apiProductId"
    Then The lifecycle status of API "apiProductId" should be "Published"

    # Step 4: Subscribe to api,"createdAppId" and "subscriptionId" saved in cotext
    When I have set up application with keys, subscribed to API "apiProductId", and obtained access token for "subscriptionId"

    # Step 5: Block the subscription
    When I block the subscription with "subscriptionId" for the resource
    Then The response status code should be 200

    # Step 6: Check the subscription "BLOCKED"
    When I retrieve the subscriptions for resource "apiProductId"
    Then The response should contain "BLOCKED"

    # Step 7: Unblock the subscription
    When I unblock the subscription with "subscriptionId" for the resource
    Then The response status code should be 200

    # Step 8: Check the subscription "UNBLOCKED"
    When I retrieve the subscriptions for resource "apiProductId"
    Then The response should contain "UNBLOCKED"

    # Step 9: Remove the subscription and application
    When I delete the subscription with id "<subscriptionId>"
    When I delete the application with id "<createdAppId>"
    Then The response status code should be 200









