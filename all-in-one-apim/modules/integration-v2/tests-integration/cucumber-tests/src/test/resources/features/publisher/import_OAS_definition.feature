Feature: Create APIs by importing Open API Specification (2, 3, 3.1)

  Background:
    Given The system is ready and I have valid access tokens for current user

  Scenario: Create an application
    When I have set up a application with keys
    Then The response status code should be 200

  Scenario Outline: Import API Definition
    # Step 1: Import API definition and create api
    When I import open api definition from "<apiDefinition>" , additional properties from "<additionalProperty>" and create api as "<apiID>"
    Then The response status code should be 201

    When  I retrieve the "apis" resource with id "<apiID>"
    Then The response status code should be 200
    And I put the response payload in context as "<apiPayload>"

    # Step 2: Create a new revision of an API and save in context as "revisionId"
    When I put the following JSON payload in context as "<createRevisionPayload>"
    """
    {
      "description":"Another new Revision"
    }
    """
    And I make a request to create a revision for "apis" resource "<apiID>" with payload "<createRevisionPayload>"
    Then The response status code should be 201

    # Step 3: Deploy revision to gateway environment
    When I deploy revision "revisionId" of "apis" resource "<apiID>"
    Then The response status code should be 201
    And I wait for deployment of the resource in "<apiPayload>"

    # Step 4: Publish the API
    And I publish the "apis" resource with id "<apiID>"
    Then The response status code should be 200
    Then The lifecycle status of API "<apiID>" should be "Published"

   # Step 5: Subscribe and invoke
    When I subscribe to resource "<apiID>", with "createdAppId" and obtained access token for "<subscriptionID>" with scope ""
    And I invoke the API resource at path "<apiResource>" with method "GET" using access token "<generatedAccessToken>" and payload ""

    Then The response status code should be 200
    And I delete the subscription with id "<subscriptionID>"

  # Step 5: Remove the created API
    When I delete the "apis" resource with id "<apiID>"
    Then The response status code should be 200

    Examples:
      | apiDefinition                                       | additionalProperty                                         | apiID         | apiResource                    | apiPayload     | subscriptionID |
      | artifacts/payloads/OAS/OAS2ApiDefinition.json       | artifacts/payloads/OAS/OAS2AdditionalProperties.json       | oas2ApiId     | /oas2/1.0.0/hello              | oas2ApiPayload | oas2ApiSubId   |
      | artifacts/payloads/OAS/OAS3ApiDefinition.json       | artifacts/payloads/OAS/OAS3AdditionalProperties.json       | oas3ApiId     | /oas3/1.0.0/hello              | oas3ApiPayload | oas3ApiSubId   |
      | artifacts/payloads/OAS/OAS3.1ApiDefinition.json     | artifacts/payloads/OAS/OAS3.1AdditionalProperties.json     | oas31ApiId     | /oas31/1.0.0/hello            | oas31ApiPayload| oas31ApiSubId  |


  Scenario: Removing created resource
    When I delete the application with id "createdAppId"
    Then The response status code should be 200
