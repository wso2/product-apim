@cleanup
Feature: DevPortal API Ratings

  DevPortal per-user rating of a published API: a user sets their rating (1–5), the stored value is returned,
  and the rating can be removed. Also verifies that the tags supplied at API creation are carried on the
  published API (the publisher-plane metadata half of the legacy TagsRatingTestCase). Ratings are a
  sub-resource of the API and cascade-delete with it — teardown via the @cleanup hook. Ports TagsRatingTestCase.

  @cap:devportal @feat:ratings @type:regression @dep:publisher @legacy:TagsRatingTestCase
  Scenario Outline: An API's tags are retained and a user rating can be set and removed as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "rtApiId" and deployed it

    # The tags supplied at creation are carried on the API (the tags half of TagsRatingTestCase).
    When I retrieve the "apis" resource with id "rtApiId"
    Then The response status code should be 200
    And The response should contain "tag18-1"

    When I publish the "apis" resource with id "rtApiId"
    Then The lifecycle status of API "rtApiId" should be "Published"

    # Set the user rating to 4 → the stored value is returned.
    When I set my rating of API "rtApiId" to 4
    Then The response status code should be 200
    And The response should contain "\"rating\":4"

    # Remove the rating.
    When I delete my rating of API "rtApiId"
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
