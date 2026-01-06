Feature: New product creation

  Background:
    Given The system is ready and I have valid access tokens for current user

  Scenario: Find/Create apis for creating new API Product
    # Step 1: find migrated rest apis, products
    When I find the apiUUID of the API created with the name "ADPRestAPI" and version "1.0.0" as "migratedAPIId"
    Then The response status code should be 200

    When I find the api product created with the name "ADPAPIProduct" as "apiProductId"
    Then The response status code should be 200

    # Step 2: create new api
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "<apiPayload>"
    And I create an "apis" resource with payload "<apiPayload>" as "newAPIId"

    # Step 3: create new api product, used api Ids must be there in payload
    When I create a new API product as "newAPIProductId" from apis "newAPIId" and "migratedAPIId"
    Then The response status code should be 201

  Scenario: Deploy and publish new API Product
    When  I retrieve the "api-products" resource with id "newAPIProductId"
    And I put the response payload in context as "<retrievedApiProductPayload>"

    # Step 4: Create a new revision of an existing API and save in context as "revisionId"
    When I put the following JSON payload in context as "<createRevisionPayload>"
    """
    {
      "description":"Initial Revision"
    }
    """
    And I make a request to create a revision for "api-products" resource "newAPIProductId" with payload "<createRevisionPayload>"
    Then The response status code should be 201

    # Step 5: Deploy revision to a gateway environment
    When I deploy revision "revisionId" of "api-products" resource "newAPIProductId"
    Then The response status code should be 201
    And I wait for deployment of the resource in "<retrievedApiProductPayload>"

    # Step 6: Publish the API product
    And I publish the "api-products" resource with id "newAPIProductId"
    Then The lifecycle status of API "newAPIProductId" should be "Published"

    # Step 7: Subscribe and invoke
    When I have set up a application with keys
    And I subscribe to resource "newAPIProductId", with "createdAppId" and obtained access token for "subscriptionId" with scope "adp-shared-scope-with-roles, new-shared-scope"

    When I invoke the API resource at path "apiTestProductContext/1.0.0/customers/123/" with method "GET" using access token "<generatedAccessToken>" and payload ""
    Then The response status code should be 200

    And I invoke the API resource at path "apiTestProductContext/1.0.0/users/123/" with method "GET" using access token "<generatedAccessToken>" and payload ""
    Then The response status code should be 403

  Scenario Outline: Create new versions of migrated API products

#    # Step 1: Retrieve payload
    When I retrieve the "api-products" resource with id "<apiProductID>"
    And I put the response payload in context as "<apiProductPayload>"

    # Step 2: Create a new version
    When I create a new version "<newVersion>" of "api-products" resource "<apiProductID>" with default version "<defaultProperty>" as "<newVersionId>"
    Then The response status code should be <expectedStatus>
    And The response should contain "<newVersion>"
    And The response should contain "<defaultProperty>"
    And The lifecycle status of API "<newVersionId>" should be "<expectedLifecycle>"

    # Step 3: Enable/disable default versioning
    When I update the "api-products" resource "<apiProductID>" and "<apiProductPayload>" with configuration type "<configType>" and value:
      """
      <configValue>
      """
    Then The response status code should be 200
    When I retrieve the "api-products" resource with id "<apiProductID>"
    And The "api-products" resource should reflect the updated "<configType>" as:
      """
      <configValue>
      """

    # Step 4: Delete the version
    When I delete the "api-products" resource with id "<newVersionId>"
    Then The response status code should be 200

    Examples:
      | apiProductID      | newVersion | defaultProperty | expectedStatus | newVersionId    | expectedLifecycle     | configType             | configValue    |
      | apiProductId      | 5.0.0      | false           | 201            | newVersionApiId | Created               |  isDefaultVersion      |     true       |
      | newAPIProductId   | 5.0.0      | false           | 201            | newVersionApiId | Created               |  isDefaultVersion      |     true       |


  # delete created resources
  Scenario: Remove the APIs
    When I delete the subscription with id "subscriptionId"
    Then The response status code should be 200

    When I delete the application with id "createdAppId"
    Then The response status code should be 200

    When I delete the "api-products" resource with id "newAPIProductId"
    Then The response status code should be 200

    When I delete the "apis" resource with id "newAPIId"
    Then The response status code should be 200
