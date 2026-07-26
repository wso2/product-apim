@cleanup
Feature: Publisher API Products

  Ports the core of the legacy APIProductCreationTestCase + APIProductRevisionTestCase: an API Product aggregates
  selected resources of existing APIs into a new entity with its own context/version, taken through revision and
  lifecycle. This feature covers the publisher plane — create + verify the aggregation, read the product's
  swagger definition, reject a malformed context, create a new version (incl. as default), confirm the product
  tracks its underlying API, and the product revision lifecycle (reusing the generic revision steps with
  resourceType "api-products"). Gateway invocation of a product is covered by gateway/api_product_invocation.
  ×2 tenant where the concern is tenant-agnostic. Teardown via @cleanup — the product is deleted before its
  underlying API (a product references the API).

  @cap:publisher @feat:products @type:smoke @dep:publisher @legacy:APIProductCreationTestCase
  Scenario Outline: Create an API product aggregating an API and read its definition as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "prodApiId" and deployed it
    When I create an API product "${UNIQUE:AggProduct}" with context "${UNIQUE:aggProductCtx}" from API "prodApiId" as "productId"
    Then The response status code should be 201
    # The product retrieve echoes the aggregated API id, and its swagger carries the aggregated resource path.
    When I retrieve the "api-products" resource with id "productId"
    Then The response status code should be 200
    And The response should contain "{{prodApiId}}"
    When I retrieve the API product swagger of "productId"
    Then The response status code should be 200
    And The response should contain "/customers/{id}"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  @cap:publisher @feat:products @type:negative @dep:publisher @legacy:APIProductCreationTestCase
  Scenario: A malformed API product context is rejected
    Given The system is ready and I have valid publisher access tokens as "publisherUser"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "prodApiIdNeg" and deployed it
    When I attempt to create an API product "${UNIQUE:BadCtxProduct}" with context "invalid context with spaces" from API "prodApiIdNeg"
    Then The response status code should be 400

  @cap:publisher @feat:products @type:regression @dep:publisher @legacy:APIProductCreationTestCase
  Scenario Outline: Create a new version of an API product as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "verApiId" and deployed it
    When I create an API product "${UNIQUE:VerProduct}" with context "${UNIQUE:verProductCtx}" from API "verApiId" as "verProductId"
    Then The response status code should be 201
    When I create a new version "2.0.0" of API product "verProductId" with default version "false" as "verProductV2Id"
    Then The response status code should be 201
    When I retrieve the "api-products" resource with id "verProductV2Id"
    Then The response status code should be 200
    And The response should contain "2.0.0"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  @cap:publisher @feat:products @type:regression @dep:publisher @legacy:APIProductCreationTestCase
  Scenario: A new API product version can be created as the default version
    Given The system is ready and I have valid publisher access tokens as "publisherUser"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "defVerApiId" and deployed it
    When I create an API product "${UNIQUE:DefVerProduct}" with context "${UNIQUE:defVerProductCtx}" from API "defVerApiId" as "defProductId"
    Then The response status code should be 201
    When I create a new version "2.0.0" of API product "defProductId" with default version "true" as "defProductV2Id"
    Then The response status code should be 201
    # Re-fetch the new version and confirm it is flagged as the default.
    And The "api-products" resource should reflect the updated "isDefaultVersion" as:
      """
      true
      """

  @cap:publisher @feat:products @type:regression @dep:publisher @legacy:APIProductCreationTestCase
  Scenario Outline: An API product tracks its underlying API after the API is updated as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "underApiId" and deployed it
    When I create an API product "${UNIQUE:TrackProduct}" with context "${UNIQUE:trackProductCtx}" from API "underApiId" as "trackProductId"
    Then The response status code should be 201
    # Update the underlying API (description), then confirm the product still references it + its operations.
    When I retrieve the "apis" resource with id "underApiId"
    And I put the response payload in context as "underApiPayload"
    When I update the "apis" resource "underApiId" and "underApiPayload" with configuration type "description" and value:
      """
      Updated backing API for product tracking
      """
    Then The response status code should be 200
    When I retrieve the "api-products" resource with id "trackProductId"
    Then The response status code should be 200
    And The response should contain "{{underApiId}}"
    And The response should contain "/customers/{id}"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  @cap:publisher @feat:products @type:regression @dep:publisher @legacy:APIProductRevisionTestCase
  Scenario Outline: API product revision lifecycle — create, list, deploy, undeploy, restore, delete as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "revApiId" and deployed it
    When I create an API product "${UNIQUE:RevProduct}" with context "${UNIQUE:revProductCtx}" from API "revApiId" as "revProductId"
    Then The response status code should be 201

    When I put the following JSON payload in context as "prodRevPayload"
    """
    {"description":"product revision 1"}
    """
    And I make a request to create a revision for "api-products" resource "revProductId" with payload "prodRevPayload"
    Then The response status code should be 201
    And I extract response field "id" and store it as "prodRevId"
    When I retrieve the revisions of "api-products" resource "revProductId"
    Then The response status code should be 200

    When I deploy revision "prodRevId" of "api-products" resource "revProductId"
    Then The response status code should be 201
    And I wait until "api-products" "revProductId" revision is deployed in the gateway
    When I undeploy revision "prodRevId" of "api-products" resource "revProductId"
    Then The response status code should be 201
    When I restore revision "prodRevId" of "api-products" resource "revProductId"
    Then The response status code should be 201
    When I delete revision "prodRevId" of "api-products" resource "revProductId"
    Then The response status code should be 200

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  @cap:publisher @feat:products @type:negative @dep:publisher @legacy:APIProductLifecycleTest
  Scenario Outline: A published API product with an active subscription cannot be deleted, but can be deprecated as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "d5ApiId" and deployed it
    When I create an API product "${UNIQUE:DelSubProduct}" with context "${UNIQUE:delSubProductCtx}" from API "d5ApiId" as "d5ProductId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "d5Rev"
    """
    {"description":"product revision for delete-with-subscription"}
    """
    And I make a request to create a revision for "api-products" resource "d5ProductId" with payload "d5Rev"
    Then The response status code should be 201
    When I deploy revision "revisionId" of "api-products" resource "d5ProductId"
    Then The response status code should be 201
    When I publish the "api-products" resource with id "d5ProductId"
    Then The response status code should be 200
    When I have set up application with keys, subscribed to API "d5ProductId", and obtained access token for "d5SubId"
    Then The response status code should be 200
    # Delete is rejected while an active subscription exists.
    When I delete the "api-products" resource with id "d5ProductId"
    Then The response status code should be 409
    And The response should contain "active subscriptions exist"
    # But the product can still be deprecated.
    When I change the lifecycle of "api-products" resource "d5ProductId" with action "Deprecate"
    Then The response status code should be 200
    When I retrieve the "api-products" resource with id "d5ProductId"
    Then The response should contain "DEPRECATED"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:publisher @feat:revisions @type:regression @dep:publisher @legacy:APIProductRevisionTestCase
  Scenario Outline: Restoring the underlying API to a revision missing resources a product depends on is rejected as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "d6ApiId" and deployed it
    # Revision 1 captures the API's original resources (/customers/{id}).
    When I put the following JSON payload in context as "d6Rev1Payload"
    """
    {"description":"revision 1 - original resources"}
    """
    And I make a request to create a revision for "apis" resource "d6ApiId" with payload "d6Rev1Payload"
    Then The response status code should be 201
    And I extract response field "id" and store it as "d6Rev1Id"
    # Add new resources to the API, then capture revision 2 with the enlarged resource set.
    When I retrieve the "apis" resource with id "d6ApiId"
    And I put the response payload in context as "d6ApiPayload"
    When I update the "apis" resource "d6ApiId" and "d6ApiPayload" with configuration type "operations" and value:
      """
      [{"target":"/customers/{id}","verb":"GET"},{"target":"/customers/{id}","verb":"DELETE"},{"target":"/missing-resource","verb":"GET"},{"target":"/missing-resource","verb":"POST"},{"target":"/missing-resource/{id}","verb":"GET"}]
      """
    Then The response status code should be 200
    When I put the following JSON payload in context as "d6Rev2Payload"
    """
    {"description":"revision 2 - with added resources"}
    """
    And I make a request to create a revision for "apis" resource "d6ApiId" with payload "d6Rev2Payload"
    Then The response status code should be 201
    And I extract response field "id" and store it as "d6Rev2Id"
    # A product aggregates the API's current (enlarged) resource set, then is deployed.
    When I create an API product "${UNIQUE:RestoreProduct}" with context "${UNIQUE:restoreProductCtx}" from API "d6ApiId" as "d6ProductId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "d6ProdRev"
    """
    {"description":"product revision"}
    """
    And I make a request to create a revision for "api-products" resource "d6ProductId" with payload "d6ProdRev"
    Then The response status code should be 201
    When I deploy revision "revisionId" of "api-products" resource "d6ProductId"
    Then The response status code should be 201
    # Restoring the API to revision 2 (which has the product's resources) succeeds.
    When I restore revision "d6Rev2Id" of "apis" resource "d6ApiId"
    Then The response status code should be 201
    # Restoring to revision 1 (which lacks the added resources the product depends on) is rejected.
    When I restore revision "d6Rev1Id" of "apis" resource "d6ApiId"
    Then The response status code should be 400

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  @cap:publisher @feat:products @type:regression @dep:publisher @legacy:APIProductLifecycleTest
  Scenario: An API product moves through its lifecycle and can be deleted when retired
    Given The system is ready and I have valid publisher access tokens as "publisherUser"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "lcApiId" and deployed it
    When I create an API product "${UNIQUE:LcProduct}" with context "${UNIQUE:lcProductCtx}" from API "lcApiId" as "lcProductId"
    Then The response status code should be 201
    # Publish → Deprecate → Retire, confirming each transition on the publisher plane.
    When I publish the "api-products" resource with id "lcProductId"
    Then The response status code should be 200
    When I retrieve the "api-products" resource with id "lcProductId"
    Then The response should contain "PUBLISHED"
    When I change the lifecycle of "api-products" resource "lcProductId" with action "Deprecate"
    Then The response status code should be 200
    When I retrieve the "api-products" resource with id "lcProductId"
    Then The response should contain "DEPRECATED"
    When I change the lifecycle of "api-products" resource "lcProductId" with action "Retire"
    Then The response status code should be 200
    When I retrieve the "api-products" resource with id "lcProductId"
    Then The response should contain "RETIRED"
    # A retired product can be deleted (legacy testDeleteRetiredAPIProducts).
    When I delete the "api-products" resource with id "lcProductId"
    Then The response status code should be 200
