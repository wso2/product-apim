Feature: Migrated API Verification

  Background:
    Given The system is ready
    And I have a valid DCR application for the current user
    And I have a valid Publisher access token for the current user
    And I have a valid Devportal access token for the current user

  Scenario: Migrated API Retrieval, Update and Invocation
    When I find the apiUUID of the API created with the name "APIM18PublisherTest" and version "1.0.0" as "<selectedApiId>"
    And I retrieve the API with id "<selectedApiId>"
    Then The response status code should be 200
    And I put the response payload in context as "<retrievedApiPayload>"

    And I get the generated access token from file "artifacts/accessTokens/api_invocation_access_tokens.json"
    And I invoke the API resource at path "/apiContext/1.0.0/customers/123/" with method "GET" using access token "<generatedAccessToken>" and payload ""
    Then The response status code should be 200

    When I update API of id "<selectedApiId>" with payload "<retrievedApiPayload>"
    Then The response status code should be 200
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
    And I make a request to create a revision for API "<selectedApiId>" with payload "<createRevisionPayload>"
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
    And I make a request to deploy revision "<revisionId>" of API "<selectedApiId>" with payload "<deployRevisionPayload>"
    Then The response status code should be 201
    Then I wait for undeployment of the previous API revision in "<retrievedApiPayload>"
    Then I wait for deployment of the API in "<retrievedApiPayload>"


    And I get the generated access token from file "artifacts/accessTokens/api_invocation_access_tokens.json"
    And I invoke the API resource at path "/apiContext/1.0.0/customers/123/" with method "GET" using access token "<generatedAccessToken>" and payload ""

    And I invoke the API resource at path "/apiContext/1.0.0/check-header" with method "GET" using access token "<generatedAccessToken>" and payload ""
    Then The response status code should be 200
    And The response should contain the header "x-response-header" with value "x-res-value"
