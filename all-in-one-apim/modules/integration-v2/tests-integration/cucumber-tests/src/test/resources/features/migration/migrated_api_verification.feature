Feature: Migrated API Verification

  Background:
    Given The system is ready
    And I have a valid DCR application for the current user
    And I have a valid Publisher access token for the current user
    And I have a valid Devportal access token for the current user

  Scenario: Migrated API Retrieval, Update and Invocation
    When I find the API created with the name "APIM18PublisherTest" and version "1.0.0"
    And I wait until the response status code is 200
    And I extract response field "count" and store it as "<apiCount>"
    And the actual value of "<apiCount>" should match the expected value:
      """
      1
      """
    And I extract response field "list[0].id" and store it as "<selectedApiId>"

    And I retrieve the "apis" resource with id "<selectedApiId>"
    And I wait until the response status code is 200
    And I wait until the response status code is 200
    And I put the response payload in context as "<retrievedApiPayload>"

    And I get the generated access token from file "artifacts/accessTokens/api_invocation_access_tokens.json"
    And I invoke the API resource at path "/apiContext/1.0.0/customers/123/" with method "GET" using access token "<generatedAccessToken>" and payload ""
    Then The response status code should be 200
    And The response should contain the header "x-response-header" with value "x-res-value"

    When I update "apis" resource of id "<selectedApiId>" with payload "<retrievedApiPayload>"
    And I wait until the response status code is 200
    And The response should contain the following api policies
      | request  | custom_add_request_header   |
      | response | custom_add_response_header  |
      | fault    | json_fault                  |

    When I put the following JSON payload in context as "<createRevisionPayload>"
    """
    {
      "description":"Initial Revision"
    }
    """
    And  I make a request to create a revision for "apis" resource "<selectedApiId>" with payload "<createRevisionPayload>"
    And I wait until the response status code is 201
    And I extract response field "id" and store it as "<revisionId>"
    And I wait for 3 seconds

    When I put the following JSON payload in context as "<deployRevisionPayload>"
    """
    [
      {
        "name": "Production and Sandbox",
        "vhost": "localhost",
        "displayOnDevportal": true
      }
    ]
    """
    And I make a request to deploy revision "<revisionId>" of "apis" resource "<selectedApiId>" with payload "<deployRevisionPayload>"
    And I wait until the response status code is 201
    Then I wait for undeployment of the previous API revision in "<retrievedApiPayload>"
    Then I wait for deployment of the resource in "<retrievedApiPayload>"

    And I get the generated access token from file "artifacts/accessTokens/api_invocation_access_tokens.json"
    And I invoke the API resource at path "/apiContext/1.0.0/customers/123/" with method "GET" using access token "<generatedAccessToken>" and payload ""
    Then The response status code should be 200

    And I invoke the API resource at path "/apiContext/1.0.0/check-header" with method "GET" using access token "<generatedAccessToken>" and payload ""
    Then The response status code should be 200
    And The response should contain the header "x-response-header" with value "x-res-value"
