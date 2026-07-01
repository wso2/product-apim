@cleanup
Feature: Publisher API Lifecycle

  Publisher-plane lifecycle of a REST API: create + deploy, retrieve, update (description & tiers),
  the update-does-not-rename invariant, publish, and presence in the API list. Asserts only publisher
  outcomes — subscription and gateway invocation are covered by devportal/subscribe and
  gateway/rest-invocation. Teardown via the @cleanup hook.

  @cap:publisher @feat:api-lifecycle @type:smoke @legacy:APIMANAGERPublisherTestCase
  Scenario Outline: Create, update, publish and list a REST API as <actor>
    # Runs as a least-privilege publisher user (creator+publisher, not admin), in both the super tenant
    # and tenant1.com, proving the publisher-plane lifecycle is tenant-agnostic.
    Given The system is ready and I have valid publisher access tokens as "<actor>"

    # Create + deploy a revision
    Given I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it

    # A freshly created (not-yet-published) API is in the CREATED lifecycle state
    And The lifecycle status of API "createdApiId" should be "Created"

    # Retrieve and confirm the created metadata
    When I retrieve the "apis" resource with id "createdApiId"
    Then The response status code should be 200
    And The response should contain "1.0.0"
    And The response should contain "lastUpdatedTimestamp"
    And I put the response payload in context as "retrievedApiPayload"

    # Update description and tier collection
    When I put JSON payload from file "artifacts/payloads/update_apim_test_api.json" in context as "apiUpdatePayload"
    And I update "apis" resource of id "createdApiId" with payload "apiUpdatePayload"
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "createdApiId"
    Then The response status code should be 200
    And The response should contain "Updated description for the created API"
    And The response should contain "Gold"
    And The response should contain "Bronze"
    And The response should contain "Silver"

    # Updating must not rename the API
    When I put JSON payload from file "artifacts/payloads/rename_apim_test_api.json" in context as "apiRenamePayload"
    And I update "apis" resource of id "createdApiId" with payload "apiRenamePayload"
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "createdApiId"
    Then The response should not contain "APIMTestRenamed"

    # Publish and confirm it is listed
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"
    When I retrieve all APIs created through the Publisher REST API
    Then The API with id "createdApiId" should be in the list of all APIS

    Examples:
      | actor                      |
      | publisherUser              |
      | publisherUser@tenant1.com  |

  @cap:publisher @feat:api-lifecycle @type:negative @legacy:APIMANAGERPublisherTestCase
  Scenario Outline: A subscriber-role user cannot create an API as <actor>
    # A subscriber-only (self-signup-equivalent) user obtains a token, but it lacks the api_create scope, so
    # the Publisher API rejects the create as unauthenticated-for-this-resource (401). Proves publisher-plane
    # role enforcement in both tenants.
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "subscriberApiPayload"
    And I attempt to create an "apis" resource with payload "subscriberApiPayload"
    Then The response status code should be 401

    Examples:
      | actor                       |
      | subscriberUser              |
      | subscriberUser@tenant1.com  |
