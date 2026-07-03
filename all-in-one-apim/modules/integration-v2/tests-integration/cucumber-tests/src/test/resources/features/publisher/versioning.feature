@cleanup
Feature: Publisher API Versioning

  Publisher-plane versioning: creating a new version of an API, the default-version flag, and taking
  the new version through its lifecycle to Published. Asserts only publisher outcomes — that the new
  version is invocable at the gateway is covered separately by gateway/rest-invocation. Positive flow
  runs as a least-privilege publisher in both the super tenant and tenant1.com.

  @cap:publisher @feat:versioning @type:smoke @legacy:APIVersioningTestCase
  Scenario Outline: Create, version and publish an API as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it

    When I create a new version "2.0.0" of "apis" resource "createdApiId" with default version "true" as "newVersionId"
    Then The response status code should be 201
    And The response should contain "2.0.0"
    And The lifecycle status of API "newVersionId" should be "Created"

    When I retrieve the "apis" resource with id "newVersionId"
    Then The response status code should be 200
    And The response should contain "2.0.0"
    And I put the response payload in context as "retrievedApiPayload"
    # The new version was created as the default. APIM keeps a single default version, so assert the flag is
    # set on the new version AND that the original version was flipped out of default (the reflect step
    # re-fetches the id from the preceding retrieve and retries, tolerating the propagation delay).
    And The "apis" resource should reflect the updated "isDefaultVersion" as:
      """
      true
      """
    When I retrieve the "apis" resource with id "createdApiId"
    Then The response status code should be 200
    And The "apis" resource should reflect the updated "isDefaultVersion" as:
      """
      false
      """

    When I deploy the API with id "newVersionId"
    Then The response status code should be 201
    And I wait for deployment of the resource in "retrievedApiPayload"

    When I publish the "apis" resource with id "newVersionId"
    Then The lifecycle status of API "newVersionId" should be "Published"

    Examples:
      | actor                      |
      | publisherUser              |
      | publisherUser@tenant1.com  |

  @cap:publisher @feat:versioning @type:negative @legacy:APIVersioningTestCase
  Scenario Outline: A subscriber-role user cannot create a new API version as <actor>
    # Create the base API as a publisher, then re-authenticate as a subscriber whose token lacks the
    # api_create scope and confirm the version-create is rejected (401), in both tenants.
    Given The system is ready and I have valid publisher access tokens as "<publisher>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it
    Given The system is ready and I have valid publisher access tokens as "<subscriber>"
    When I attempt to create a new version "2.0.0" of "apis" resource "createdApiId" with default version "true"
    Then The response status code should be 401
    # Switch back so @cleanup deletes the publisher-owned base API with the publisher's token.
    And I act as "<publisher>"

    Examples:
      | publisher                  | subscriber                  |
      | publisherUser              | subscriberUser              |
      | publisherUser@tenant1.com  | subscriberUser@tenant1.com  |
