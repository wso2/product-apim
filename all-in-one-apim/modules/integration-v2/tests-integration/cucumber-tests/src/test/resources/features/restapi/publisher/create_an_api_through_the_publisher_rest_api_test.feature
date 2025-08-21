Feature: Publisher API Management
  This feature tests API creation, validation, deletion, and error handling via the Publisher REST API.

  Background:
    Given The repository directory path is set to the test context
    And The zip file at relative location "/all-in-one-apim/modules/distribution/product/target/wso2am-4.5.0.zip" is extracted to "/src/main/resources/apim"
    And I have initialized the NodeApp server container
    And I have initialized the Default API Manager container
    And I initialize the Publisher REST API client with username "admin", password "admin" and tenant "carbon.super"
    And I initialize the Store REST API client with username "admin", password "admin" and tenant "carbon.super"

  Scenario: Create an API Through the Publisher Rest API
    When I create an API with the JSON payload from file "features/restapi/publisher/data/createAPI.json"
    Then The response status code should be 201

  Scenario: Get the API details by ID
    When  I retrieve the API with id "<createdApiId>"
    Then The response status code should be 200
    And The response should contain "APIMTest"
    And The response should contain "apiTestContext"
    And The response should contain "1.0.0"
    And The response should contain "lastUpdatedTimestamp"

  Scenario: Update API with the description and tiersCollection
    When I update API of id "<createdApiId>" with the JSON payload from file "features/restapi/publisher/data/updateAPI.json"
    Then The response status code should be 200
    And I retrieve the API with id "<createdApiId>"
    Then The response status code should be 200
    And The response should contain "Updated description for the created API"
    And The response should contain "Gold"
    And The response should contain "Bronze"
    And The response should contain "Silver"

  Scenario: Ensure API update does not change the API name
    When I update API of id "<createdApiId>" with the JSON payload from file "features/restapi/publisher/data/renameAPI.json"
    Then The response status code should be 200
    When I retrieve the API with id "<createdApiId>"
    Then The response should contain "APIMTest"
    But The response should not contain "APIMTestRenamed"

  Scenario: Publish the API
    When I deploy a revision of the API with id "<createdApiId>"
    And I publish the API with id "<createdApiId>"
    Then The lifecycle status of API "<createdApiId>" should be "Published"

  Scenario: Subscribe to the API using an application
    When I create an application with the following details
      | name             | APIMTestApp4            |
      | throttlingPolicy | Unlimited              |
      | description      | Test app for scenarios |

    Then I should be able to retrieve the application with id "<createdAppId>"
    And I subscribe to API "<createdApiId>" using application "<createdAppId>" with throttling policy "Bronze"
    Then I should be able to retrieve the subscription for Api "<createdApiId>" by Application "<createdAppId>"

  Scenario: Retrieve all APIs created through the Publisher REST API
    When I retrieve all APIs created through the Publisher REST API
    Then The API with id "<createdApiId>" should be in the list of all APIS

  Scenario: Invoke API
    When I generate client credentials for application id "<createdAppId>" with key type "PRODUCTION"
    And I request an access token using grant type "client_credentials" without any scope
    And I invoke API of ID "<createdApiId>" with path "/customers/123/" and method GET using access token "<generatedAccessToken>"
    Then The response status code should be 200

  Scenario: Delete the created Application
    When I delete the subscription with id "<createdSubscriptionId>"
    Then The response status code should be 200
    When I delete the application with id "<createdAppId>"
    Then The response status code should be 200

  Scenario: Delete the created API
    When I delete the API with id "<createdApiId>"
    Then The response status code should be 200