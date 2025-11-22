Feature: Publisher API Management
  This feature tests API creation, validation, deletion, and error handling via the Publisher REST API.

  Background:
    Given The system is ready
    And I have a valid DCR application for the current user
    And I have a valid Publisher access token for the current user
    And I have a valid Devportal access token for the current user

  Scenario: Create an API Through the Publisher Rest API
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "<createApiPayload>"
    And I create an API with payload "<createApiPayload>"

  Scenario: Get the API details by ID
    When  I retrieve the "apis" resource with id "<createdApiId>"
    Then The response status code should be 200
    And The response should contain "APIMTest"
    And The response should contain "apiTestContext"
    And The response should contain "1.0.0"
    And The response should contain "lastUpdatedTimestamp"
    And I put the response payload in context as "<retrievedApiPayload>"

  Scenario: Update API with the description and tiersCollection
    When I put JSON payload from file "artifacts/payloads/update_apim_test_api.json" in context as "<apiUpdatePayload>"
    And I update "apis" resource of id "<createdApiId>" with payload "<apiUpdatePayload>"
    Then The response status code should be 200
    And I retrieve the "apis" resource with id "<createdApiId>"
    Then The response status code should be 200
    And The response should contain "Updated description for the created API"
    And The response should contain "Gold"
    And The response should contain "Bronze"
    And The response should contain "Silver"

  Scenario: Ensure API update does not change the API name
    When I put JSON payload from file "artifacts/payloads/rename_apim_test_api.json" in context as "<apiUpdatePayload>"
    And I update "apis" resource of id "<createdApiId>" with payload "<apiUpdatePayload>"
    Then The response status code should be 200
    And I retrieve the "apis" resource with id "<createdApiId>"
    Then The response should contain "APIMTest"
    But The response should not contain "APIMTestRenamed"

  Scenario: Publish the API
    When I put the following JSON payload in context as "<createRevisionPayload>"
    """
    {
      "description":"Initial Revision"
    }
    """
    And  I make a request to create a revision for "apis" resource "<createdApiId>" with payload "<createRevisionPayload>"
    And I put the following JSON payload in context as "<deployRevisionPayload>"
    """
    [
      {
        "name": "{{gatewayEnvironment}}",
        "vhost": "localhost",
        "displayOnDevportal": true
      }
    ]
    """
    And I make a request to deploy revision "<revisionId>" of "apis" resource "<createdApiId>" with payload "<deployRevisionPayload>"
    Then The response status code should be 201
    Then The lifecycle status of API "<createdApiId>" should be "Created"
    And I wait for deployment of the resource in "<retrievedApiPayload>"
    And I publish the "apis" resource with id "<createdApiId>"
    Then The lifecycle status of API "<createdApiId>" should be "Published"

  Scenario: Subscribe to the API using an application
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "<createAppPayload>"
    When I create an application with payload "<createAppPayload>"
    And I retrieve the application with id "<createdAppId>"
    Then The response status code should be 200

    And I put the following JSON payload in context as "<generateApplicationKeysPayload>"
    """
    {
      "keyType": "PRODUCTION",
      "grantTypesToBeSupported": [
        "client_credentials"
      ]
    }
    """
    And I generate client credentials for application id "<createdAppId>" with payload "<generateApplicationKeysPayload>"
    Then The response status code should be 200
    And The response should contain "consumerKey"
    And The response should contain "consumerSecret"

    And I put the following JSON payload in context as "<apiSubscriptionPayload>"
    """
    {
      "applicationId": "{{applicationId}}",
      "apiId": "{{apiId}}",
      "throttlingPolicy": "Bronze"
    }
    """
    And I subscribe to API "<createdApiId>" using application "<createdAppId>" with payload "<apiSubscriptionPayload>"
    And I retrieve the subscription for Api "<createdApiId>" by Application "<createdAppId>"
    Then The response status code should be 200
    And The subscription with id "<subscriptionId>" should be in the list of all subscriptions

  Scenario: Retrieve all APIs created through the Publisher REST API
    When I retrieve all APIs created through the Publisher REST API
    Then The API with id "<createdApiId>" should be in the list of all APIS

  Scenario: Invoke API
    When I put the following JSON payload in context as "<createApplicationAccessTokenPayload>"
    """
    {
      "consumerSecret": "{{appConsumerSecret}}",
      "validityPeriod": 3600
    }
    """
    And I request an access token for application id "<createdAppId>" using payload "<createApplicationAccessTokenPayload>"
    Then The response status code should be 200
    And I invoke the API resource at path "apiTestContext/1.0.0/customers/123/" with method "GET" using access token "<generatedAccessToken>" and payload ""
    Then The response status code should be 200

  Scenario: Delete the created Application
    When I delete the subscription with id "<subscriptionId>"
    Then The response status code should be 200
    When I delete the application with id "<createdAppId>"
    Then The response status code should be 200

  Scenario: Delete the created API
    When I delete the "apis" resource with id "<createdApiId>"
    Then The response status code should be 200
