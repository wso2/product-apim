Feature: Migrated OAS API Management

  Background:
    Given The system is ready and I have valid access tokens for current user

  Scenario: Create an application
    When I have set up a application with keys
    Then The response status code should be 200

  Scenario Outline: Migrated API Revision Management
    # Step 1: Find apis and update as needed
    When I find the apiUUID of the API created with the name "<apiName>" and version "<apiVersion>" as "<apiID>"
    And I retrieve the "apis" resource with id "<apiID>"
    And I put the response payload in context as "<apiPayload>"

    # Update policies for subscription
    When I update the "apis" resource "<apiID>" and "<apiPayload>" with configuration type "policies" and value:
      """
      ["Bronze","Gold","Unlimited"]
      """
    And I retrieve the "apis" resource with id "<apiID>"
    And I put the response payload in context as "<apiPayload>"

    # Update endpoints for invocations
    When I prepare an endpoint update with "http", "http://nodebackend:3001/jaxrs_basic/services/customers/customerservice/" and "http://nodebackend:3001/jaxrs_basic/services/customers/customerservice/" as "<endpointUpdateConfig>"
    And I update the "apis" resource "<apiID>" and "<apiPayload>" with configuration type "endpointConfig" and value:
      """
      <endpointUpdateConfig>
      """
    And I retrieve the "apis" resource with id "<apiID>"
    And I put the response payload in context as "<apiPayload>"


    # Step 2: Verify the availability of  existing revisions
    When I get the existing revision as "existingRevisionID" for "apis" resource with "<apiID>"
    Then The response status code should be 200

    # Step 3: Create a new revision of an existing API and save in context as "revisionId"
    When I put the following JSON payload in context as "<createRevisionPayload>"
    """
    {
      "description":"Another new Revision"
    }
    """
    And I make a request to create a revision for "apis" resource "<apiID>" with payload "<createRevisionPayload>"
    Then The response status code should be 201

    # Step 4: Deploy revision to gateway environment
    When I deploy revision "revisionId" of "apis" resource "<apiID>"
    Then The response status code should be 201
    And I wait until "apis" "<apiID>" revision is deployed in the gateway

   # Step 5: Subscribe and invoke
    When I subscribe to resource "<apiID>", with "createdAppId" and obtained access token for "<subscriptionID>" with scope ""
    And I invoke the API resource at path "<apiResource>" with method "GET" using access token "<generatedAccessToken>" and payload ""

    Then The response status code should be 200
    And I delete the subscription with id "<subscriptionID>"

    Examples:
      | apiName                 | apiVersion   | apiID         | apiResource                    | apiPayload     | subscriptionID |
      | ADPOAS2RestAPI          | 1.0.0        | oas2ApiId     | /adp-oas2-rest/1.0.0/hello     | oas2ApiPaylaod | oas2ApiSubId   |
      | ADPOAS3RestAPI          | 1.0.0        | oas3ApiId     | /adp-oas3-rest/1.0.0/hello     | oas3ApiPaylaod | oas3ApiSubId   |

    Scenario: Removing created resource
      When I delete the application with id "createdAppId"
      Then The response status code should be 200