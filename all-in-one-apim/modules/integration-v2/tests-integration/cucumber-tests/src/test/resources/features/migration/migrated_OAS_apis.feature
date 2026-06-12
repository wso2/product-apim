Feature: Migrated OAS API Management

  Background:
    Given The system is ready and I have valid access tokens for current user

  Scenario: Create an application
    When I create a test application and store the id as "<createdAppId>"
    Then I generate keys for application "<createdAppId>" and store consumer secret as "<consumerSecret>" and key mapping id as "<keyMappingId>"

  Scenario Outline: Migrated OAS API Management
    # Step 1: Find apis and update as needed
    When I find the API created with the name "<apiName>" and version "<apiVersion>"
    And I wait until the response status code is 200
    And I extract response field "count" and store it as "<apiCount>"
    And the actual value of "<apiCount>" should match the expected value:
      """
      1
      """
    And I extract response field "list[0].id" and store it as "<apiID>"

    And I retrieve the "apis" resource with id "<apiID>"
    And I wait until the response status code is 200
    And I put the response payload in context as "<apiPayload>"

    # Update policies for subscription
    When I update the "apis" resource "<apiID>" and "<apiPayload>" with configuration type "policies" and value:
      """
      ["Bronze","Gold","Unlimited"]
      """
    And I wait until the response status code is 200
    And I retrieve the "apis" resource with id "<apiID>"
    And I wait until the response status code is 200
    And I put the response payload in context as "<apiPayload>"
    And The "apis" resource should reflect the updated "policies" as:
      """
     ["Bronze","Gold","Unlimited"]
      """

    # Update endpoints for invocations
    When I prepare an endpoint update with "http", "http://nodebackend:3001/jaxrs_basic/services/customers/customerservice/" and "http://nodebackend:3001/jaxrs_basic/services/customers/customerservice/" as "<endpointUpdateConfig>"
    And I update the "apis" resource "<apiID>" and "<apiPayload>" with configuration type "endpointConfig" and value:
      """
      <endpointUpdateConfig>
      """
    And I retrieve the "apis" resource with id "<apiID>"
    And I wait until the response status code is 200
    And I put the response payload in context as "<apiPayload>"

    # Step 2: Verify the availability of  existing revisions
    When I get the existing revision for "apis" resource with "<apiID>"
    And I wait until the response status code is 200
    And I extract response field "list[0].id" and store it as "<existingRevisionID>"

    # Step 3: Create a new revision of an existing API and save in context as "revisionId"
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

    # Step 4: Deploy revision to gateway environment
    When I deploy revision "revisionId" of "apis" resource "<apiID>"
    And I wait until the response status code is 201
    And I wait until "apis" "<apiID>" revision is deployed in the gateway

   # Step 5: Subscribe and invoke
    When I subscribe to resource "<apiID>" using application "<createdAppId>" and store subscription as "<subscriptionID>"
    And I obtain an access token with scope "" for application "<createdAppId>" using key mapping "<keyMappingId>" and consumer secret "<consumerSecret>" and store as "<generatedAccessToken>"

    And I invoke the API resource at path "<apiResource>" with method "GET" using access token "<generatedAccessToken>" and payload ""
    Then The response status code should be 200

    When I delete the subscription with id "<subscriptionID>"
    And I wait until the response status code is 200

    Examples:
      | apiName                 | apiVersion   | apiID         | apiResource                    | apiPayload     | subscriptionID |
      | ADPOAS2RestAPI          | 1.0.0        | oas2ApiId     | /adp-oas2-rest/1.0.0/hello     | oas2ApiPaylaod | oas2ApiSubId   |
      | ADPOAS3RestAPI          | 1.0.0        | oas3ApiId     | /adp-oas3-rest/1.0.0/hello     | oas3ApiPaylaod | oas3ApiSubId   |

    Scenario: Removing created resource
      When I delete the application with id "createdAppId"
      And I wait until the response status code is 200
