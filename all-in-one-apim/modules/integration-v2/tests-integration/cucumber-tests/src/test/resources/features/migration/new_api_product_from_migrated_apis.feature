Feature: New product creation

  Background:
    Given The system is ready and I have valid access tokens for current user

  Scenario: Find/Create apis for creating new API Product
    # Step 1: Find the id of a migrated rest api
    When I find the API created with the name "ADPRestAPI" and version "1.0.0"
    And I wait until the response status code is 200 and the value of response field "count" is "1"
    And I extract response field "list[0].id" and store it as "<migratedAPIId>"

    # Find the id of a migrated api product
    When I find the api product created with the name "ADPAPIProduct"
    And I wait until the response status code is 200 and the value of response field "count" is "1"
    And I extract response field "list[0].id" and store it as "<apiProductId>"

    # Step 2: create new api
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "<apiPayload>"
    And I create an "apis" resource with payload "<apiPayload>"
    And I wait until the response status code is 201
    And I extract response field "id" and store it as "<newAPIId>"

    # Step 3: create new api product, used api Ids must be there in payload
    When I create a new API product from apis "newAPIId" and "migratedAPIId"
    And I wait until the response status code is 201
    And I extract response field "id" and store it as "<newAPIProductId>"

  Scenario: Deploy and publish new API Product
    When  I retrieve the "api-products" resource with id "newAPIProductId"
    And I wait until the response status code is 200
    And I put the response payload in context as "<retrievedApiProductPayload>"

    # Step 4: Create a new revision of an existing API and save in context as "revisionId"
    When I put the following JSON payload in context as "<createRevisionPayload>"
    """
    {
      "description":"Initial Revision"
    }
    """
    And I make a request to create a revision for "api-products" resource "newAPIProductId" with payload "<createRevisionPayload>"
    And I wait until the response status code is 201
    And I extract response field "id" and store it as "<revisionId>"
    And I wait for 3 seconds

    # Step 5: Deploy revision to a gateway environment
    When I deploy revision "revisionId" of "api-products" resource "newAPIProductId"
    And I wait until the response status code is 201
    And I wait for deployment of the resource in "<retrievedApiProductPayload>"

    # Step 6: Publish the API product
    When I execute lifecycle action "Publish" on "api-products" resource "<newAPIProductId>" and wait for state "Published"

    # Step 7: Subscribe and invoke
    When I create a test application and store the id as "<createdAppId>"
    Then I generate keys for application "<createdAppId>" and store consumer secret as "<consumerSecret>" and key mapping id as "<keyMappingId>"

    When I subscribe to resource "newAPIProductId" using application "createdAppId" and store subscription as "<subscriptionId>"
    And I obtain an access token with scope "adp-shared-scope-with-roles, new-shared-scope" for application "<createdAppId>" using key mapping "<keyMappingId>" and consumer secret "<consumerSecret>" and store as "<generatedAccessToken>"

    When I invoke the API resource at path "apiTestProductContext/1.0.0/customers/123/" with method "GET" using access token "<generatedAccessToken>" and payload ""
    Then The response status code should be 200

    And I invoke the API resource at path "apiTestProductContext/1.0.0/users/123/" with method "GET" using access token "<generatedAccessToken>" and payload ""
    Then The response status code should be 403

  Scenario Outline: Create new versions of migrated API products

    # Step 1: Retrieve payload
    When I retrieve the "api-products" resource with id "<apiProductID>"
    And I wait until the response status code is 200
    And I put the response payload in context as "<apiProductPayload>"

    # Step 2: Create a new version
    When I create a new version "<newVersion>" of "api-products" resource "<apiProductID>" with default version "<defaultProperty>"
    And I wait until the response status code is <expectedStatus>
    And The response should contain "<newVersion>"
    And The response should contain "<defaultProperty>"
    And I extract response field "id" and store it as "<newVersionId>"
    # Check the lifecycle status of the new version
    Then I get the lifecycle status of API "<newVersionId>"
    Then I wait until the response status code is 200 and the value of response field "state" is "<expectedLifecycle>"


    # Step 3: Enable/disable default versioning
    When I update the "api-products" resource "<apiProductID>" and "<apiProductPayload>" with configuration type "<configType>" and value:
      """
      <configValue>
      """
    And I wait until the response status code is 200
    When I retrieve the "api-products" resource with id "<apiProductID>"
    And I wait until the response status code is 200
    And The "api-products" resource should reflect the updated "<configType>" as:
      """
      <configValue>
      """

    # Step 4: Delete the version
    When I delete the "api-products" resource with id "<newVersionId>"
    And I wait until the response status code is 200

    Examples:
      | apiProductID      | newVersion | defaultProperty | expectedStatus | newVersionId    | expectedLifecycle     | configType             | configValue    |
      | apiProductId      | 5.0.0      | false           | 201            | newVersionApiId | Created               |  isDefaultVersion      |     true       |
      | newAPIProductId   | 5.0.0      | false           | 201            | newVersionApiId | Created               |  isDefaultVersion      |     true       |


  # delete created resources
  Scenario: Delete the subscription
    When I delete the subscription with id "subscriptionId"
    And I wait until the response status code is 200

  Scenario: Delete the created application
    When I delete the application with id "createdAppId"
    And I wait until the response status code is 200

  Scenario: Delete the created API Product
    When I delete the "api-products" resource with id "newAPIProductId"
    And I wait until the response status code is 200

  Scenario: Delete the created API
    When I delete the "apis" resource with id "newAPIId"
    And I wait until the response status code is 200
