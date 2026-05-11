Feature: Custom Header Authorization

  Background:
    Given The system is ready
    And I have a valid DCR application for the current user
    And I have a valid Publisher access token for the current user
    And I have a valid Devportal access token for the current user

  Scenario: Create an API with custom authorization header
    # Create the API
    When I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it
    And I put JSON payload from file "artifacts/payloads/customHeaderTest/update_apim_test_api.json" in context as "<apiUpdatePayload>"
    # Update the API
    And I update "apis" resource of id "<createdApiId>" with payload "<apiUpdatePayload>"
    And I wait until the response status code is 200

    # Create a revision and deploy the API
    And I put the following JSON payload in context as "<createRevisionPayload>"
    """
    {
      "description":"Initial Revision"
    }
    """
    And I make a request to create a revision for "apis" resource "<createdApiId>" with payload "<createRevisionPayload>"
    And I wait until the response status code is 201
    And I extract response field "id" and store it as "<revisionId>"
    And I wait for 3 seconds
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
    And I wait until the response status code is 201
    And  I retrieve the "apis" resource with id "<createdApiId>"
    And I wait until the response status code is 200
    And I put the response payload in context as "<retrievedApiPayload>"
    And I wait for deployment of the resource in "<retrievedApiPayload>"

    # Publish the API
    And I publish the "apis" resource with id "<createdApiId>"
    And I wait until the response status code is 200
    Then I get the lifecycle status of API "<createdApiId>"
    Then I wait until the response status code is 200 and the value of response field "state" is "Published"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "<createAppPayload>"

  Scenario: Create an application
    When I create an application with payload "<createAppPayload>"
    And I wait until the response status code is 201
    And I extract response field "applicationId" and store it as "<createdAppId>"

  Scenario: Subscribe to the API using the created application
    And I put the following JSON payload in context as "<apiSubscriptionPayload>"
    """
    {
      "applicationId": "{{createdAppId}}",
      "apiId": "{{createdApiId}}",
      "throttlingPolicy": "Bronze"
    }
    """
    And I create a subscription using payload "<apiSubscriptionPayload>"
    And I wait until the response status code is 201
    And I extract response field "subscriptionId" and store it as "<subscriptionId>"

  Scenario: Invoke API using custom authentication header named Test-Custom-Header
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
    And I wait until the response status code is 200
    And I extract response field "consumerSecret" and store it as "<appConsumerSecret>"
    And I extract response field "keyMappingId" and store it as "<keyMappingId>"
    When I put the following JSON payload in context as "<createApplicationAccessTokenPayload>"
    """
    {
      "consumerSecret": "{{appConsumerSecret}}",
      "validityPeriod": 3600
    }
    """
    And I request an access token for application id "<createdAppId>" using payload "<createApplicationAccessTokenPayload>" and key mapping id "<keyMappingId>"
    And I wait until the response status code is 200
    And I extract response field "accessToken" and store it as "<generatedAccessToken>"
    And I invoke the API resource at path "/apiTestContext/1.0.0/customers/123/" with method "GET" using access token "<generatedAccessToken>" and payload ""
    Then The response status code should be 401

  # Clean up
  Scenario: Delete the subscription
    When I delete the subscription with id "<subscriptionId>"
    And I wait until the response status code is 200

  Scenario: Delete the application
    And I delete the application with id "<createdAppId>"
    And I wait until the response status code is 200

  Scenario: Delete the API
    And I delete the "apis" resource with id "<createdApiId>"
    And I wait until the response status code is 200
