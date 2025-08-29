Feature: Migrated API Verification

  Background:
    Given The repository directory path is set to the test context
    And The zip file at relative location "/all-in-one-apim/modules/distribution/product/target/wso2am-4.5.0.zip" is extracted to "/src/main/resources/apim"
    Given I have initialized the NodeApp server container
    Given I have initialized the Default API Manager container
    And I initialize the Publisher REST API client with username "admin", password "admin" and tenant "carbon.super"
    And I initialize the Store REST API client with username "admin", password "admin" and tenant "carbon.super"

  Scenario: Migrated API Retrieval, Update and Invocation
    When I find the apiUUID of the API created with the name "APIM18PublisherTest" and version "1.0.0"
    And I retrieve the API with id "<selectedApiId>"
    Then The response status code should be 200

    And I get the generated access token from file "features/migration/accessTokens/api_invocation_access_tokens.json"
    And I invoke API of ID "<selectedApiId>" with path "/customers/123/" and method GET using access token "<generatedAccessToken>"
    Then The response status code should be 200

    And I update API of id "<selectedApiId>" with the following details
      | description         | Simple Customer Service API description   2                   |
    Then The response status code should be 200

    Then I verify the updated API with id "<selectedApiId>" has the following api policies
      | request  | custom_add_request_header   |
      | response | custom_add_response_header  |
      | fault    | json_fault                  |
    And I deploy a revision of the API with id "<selectedApiId>"

    And I get the generated access token from file "features/migration/accessTokens/api_invocation_access_tokens.json"
    And I invoke API of ID "<selectedApiId>" with path "/customers/123/" and method GET using access token "<generatedAccessToken>"

    And I invoke API of ID "<selectedApiId>" with path "/check-header" and method GET using access token "<generatedAccessToken>"
    Then The response status code should be 200
    And The response should contain the header "x-response-header" with value "x-res-value"

