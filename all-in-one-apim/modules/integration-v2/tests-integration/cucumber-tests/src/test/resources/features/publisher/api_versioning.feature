Feature: API Versioning
  This feature tests API versioning functionality including creating new versions,
  lifecycle management, and version-specific operations via the Publisher REST API.

  Background:
    Given The system is ready and I have valid access tokens for current user

  Scenario Outline: Full lifecycle of an API version
    # Step 1: Create base API - "createdApiId" stored in context
    Given I have created an api and deployed it

    # Step 2: Create a new version
    When I create a new version "<newVersion>" of API "<createdApiId>" with default version "<defaultProperty>" as "<newVersionId>"
    Then The response status code should be <expectedStatus>
    And The response should contain "<newVersion>"
    And The response should contain "<defaultProperty>"
    And The lifecycle status of API "<newVersionId>" should be "<expectedLifecycle>"

    # Step 3: Verify the version exists
    When I retrieve the API with id "<newVersionId>"
    Then The response status code should be 200
    And The response should contain "<newVersion>"
    And I put the response payload in context as "<retrievedApiPayload>"

    # Step 4: Deploy the version
    When I deploy the API with id "<newVersionId>"
    Then The response status code should be 201
    And I wait for deployment of the API in "<retrievedApiPayload>"

    # Step 5: Publish the new version
    When I publish the API with id "<newVersionId>"
    Then The lifecycle status of API "<newVersionId>" should be "Published"

    # Step 6: Invoke the new version
    When I have set up application with keys, subscribed to API "<newVersionId>", and obtained access token
    And I invoke the API resource at path "apiTestContext/<newVersion>/customers/123/" with method "GET" using access token "<generatedAccessToken>" and payload ""
    Then The response status code should be 200

    # Step 7: Remove the subscription and application
    When I delete the subscription with id "<subscriptionId>"
    Then The response status code should be 200
    When I delete the application with id "<createdAppId>"
    Then The response status code should be 200

    # Step 7: Delete the version
    When I delete the API with id "<newVersionId>"
    Then The response status code should be 200

    # Step 8: Delete the base API
    When I delete the API with id "<createdApiId>"
    Then The response status code should be 200

    Examples:
      | newVersion | defaultProperty | expectedStatus | newVersionId    | expectedLifecycle |
      | 2.0.0      | false           | 201            | newVersionApiId | Created           |
      | 3.0.0      | true            | 201            | newVersionApiId2| Created           |






