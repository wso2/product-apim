@cleanup
Feature: DevPortal API Ratings

  DevPortal per-user rating of a published API: a user sets their rating (1–5), the stored value is returned,
  and the rating can be removed. Also verifies that the tags supplied at API creation are carried on the
  published API (the publisher-plane metadata half of the legacy TagsRatingTestCase). Ratings are a
  sub-resource of the API and cascade-delete with it — teardown via the @cleanup hook. Ports TagsRatingTestCase.

  @cap:devportal @feat:ratings @type:regression @dep:publisher @legacy:TagsRatingTestCase
  Scenario Outline: An API's tags are retained and an administrator rating can be set and removed as <actor>
    Given The system is ready
    And I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "rtApiId" and deployed it

    # The tags supplied at creation are carried on the API (the tags half of TagsRatingTestCase).
    When I retrieve the "apis" resource with id "rtApiId"
    Then The response status code should be 200
    And The response field "tags" should be exactly the list "tag18-1,tag18-2,tag18-3"
    And The response array field "tags" should have exactly 3 entries

    And the "apis" resource "rtApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "rtApiId"
    Then The lifecycle status of API "rtApiId" should be "Published"

    # Set the user rating to 4 → the stored value is returned.
    When I set my rating of API "rtApiId" to 4
    Then The response status code should be 200
    And The response field "ratingId" should be non-blank
    And The value of response field "rating" should be "4"

    # Remove the rating.
    When I delete my rating of API "rtApiId"
    Then The response status code should be 200

    Examples: Super-tenant and tenant administrators in both username modes
      | actor                                    |
      | superTenantAdmin                         |
      | admin@tenant1.com                        |

  @cap:devportal @feat:ratings @type:regression @dep:publisher @legacy:TagsRatingTestCase
  Scenario Outline: A secondary-store publisher and subscriber retains tags and can set and remove a rating in <tenant>
    Given The system is ready and I have valid access tokens as "admin<suffix>"
    And I generate a unique value and store it as "rt"
    When I provision secondary user-store user "SECONDARY.COM/{{rt}}" with password "Secondary@123" and roles "Internal/creator,Internal/publisher,Internal/subscriber" as an actor in tenant "<tenant>"
    And The system is ready and I have valid publisher access tokens as "SECONDARY.COM/{{rt}}<suffix>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "rtApiId" and deployed it

    When I retrieve the "apis" resource with id "rtApiId"
    Then The response status code should be 200
    And The response field "tags" should be exactly the list "tag18-1,tag18-2,tag18-3"
    And The response array field "tags" should have exactly 3 entries

    And the "apis" resource "rtApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "rtApiId"
    Then The lifecycle status of API "rtApiId" should be "Published"

    When I set my rating of API "rtApiId" to 4
    Then The response status code should be 200
    And The response field "ratingId" should be non-blank
    And The value of response field "rating" should be "4"
    When I delete my rating of API "rtApiId"
    Then The response status code should be 200

    Examples: Secondary-store actor in both tenant contexts
      | tenant       | suffix       |
      | carbon.super |              |
      | tenant1.com  | @tenant1.com |
