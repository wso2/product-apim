Feature: Create APIs by importing Open API Specification (2, 3, 3.1)

  Background:
    Given The system is ready and I have valid access tokens for current user

  Scenario: Create an application
    When I create a test application and store the id as "<createdAppId>"
    Then I generate keys for application "<createdAppId>" and store consumer secret as "<consumerSecret>" and key mapping id as "<keyMappingId>"

  Scenario Outline: Import API Definition
    # Step 1: Import API definition and create api
    When I import open api definition from "<apiDefinition>", additional properties from "<additionalProperty>" and create api
    And I wait until the response status code is 201
    And I extract response field "id" and store it as "<apiID>"

    When  I retrieve the "apis" resource with id "<apiID>"
    And I wait until the response status code is 200
    And I wait until the response status code is 200
    And I put the response payload in context as "<apiPayload>"

    # Step 2: Create a new revision of an API and save in context as "revisionId"
    When I put the following JSON payload in context as "<createRevisionPayload>"
    """
    {
      "description":"Another new Revision"
    }
    """
    And I make a request to create a revision for "apis" resource "<apiID>" with payload "<createRevisionPayload>"
    And I wait until the response status code is 201
    And I extract response field "id" and store it as "<revisionId>"
    And I wait for 3 seconds

    # Step 3: Deploy revision to gateway environment
    When I deploy revision "revisionId" of "apis" resource "<apiID>"
    And I wait until the response status code is 201
    And I wait for deployment of the resource in "<apiPayload>"

    # Step 4: Publish the API
    When I publish the "apis" resource with id "<apiID>"
    And I wait until the response status code is 200
    Then I get the lifecycle status of API "<apiID>"
    Then I wait until the response status code is 200 and the value of response field "state" is "Published"

   # Step 5: Subscribe and invoke
    When I subscribe to resource "<apiID>" using application "<createdAppId>" and store subscription as "<subscriptionID>"
    And I obtain an access token with scope "" for application "<createdAppId>" using key mapping "<keyMappingId>" and consumer secret "<consumerSecret>" and store as "<generatedAccessToken>"
    And I invoke the API resource at path "<apiResource>" with method "GET" using access token "<generatedAccessToken>" and payload ""
    Then The response status code should be 200

    # Step 6: Delete the subscription
    When I delete the subscription with id "<subscriptionID>"
    And I wait until the response status code is 200

    # Step 5: Remove the created API
    When I delete the "apis" resource with id "<apiID>"
    And I wait until the response status code is 200

    Examples:
      | apiDefinition                                       | additionalProperty                                         | apiID         | apiResource                    | apiPayload     | subscriptionID |
      | artifacts/payloads/OAS/OAS2ApiDefinition.json       | artifacts/payloads/OAS/OAS2AdditionalProperties.json       | oas2ApiId     | /oas2/1.0.0/hello              | oas2ApiPayload | oas2ApiSubId   |
      | artifacts/payloads/OAS/OAS3ApiDefinition.json       | artifacts/payloads/OAS/OAS3AdditionalProperties.json       | oas3ApiId     | /oas3/1.0.0/hello              | oas3ApiPayload | oas3ApiSubId   |
      | artifacts/payloads/OAS/OAS3.1ApiDefinition.json     | artifacts/payloads/OAS/OAS3.1AdditionalProperties.json     | oas31ApiId     | /oas31/1.0.0/hello            | oas31ApiPayload| oas31ApiSubId  |

  Scenario: Removing created resource
    When I delete the application with id "createdAppId"
    And I wait until the response status code is 200
