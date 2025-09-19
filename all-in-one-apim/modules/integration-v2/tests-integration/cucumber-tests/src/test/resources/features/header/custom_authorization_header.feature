Feature: Custom Header Authorization

  Background:
    Given The system is ready
    And I have a valid DCR application for username "admin", password "admin" and tenant "carbon.super"
    And I have a valid Publisher access token with username "admin" and password "admin"
    And I have a valid Devportal access token with username "admin" and password "admin"

  Scenario: Create an API Through the Publisher Rest API and subscribe using an Application
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "<createApiPayload>"
    And I create an API with payload "<createApiPayload>"
    Then The response status code should be 201
    And I put JSON payload from file "artifacts/payloads/customHeaderTest/update_apim_test_api.json" in context as "<apiUpdatePayload>"
    And I update API of id "<createdApiId>" with payload "<apiUpdatePayload>"
    Then The response status code should be 200
    And I put the following JSON payload in context as "<createRevisionPayload>"
    """
    {
      "description":"Initial Revision"
    }
    """
    And I make a request to create a revision for API "<createdApiId>" with payload "<createRevisionPayload>"
    Then The response status code should be 201
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
    And I make a request to deploy revision "<revisionId>" of API "<createdApiId>" with payload "<deployRevisionPayload>"
    Then The response status code should be 201
    Then I wait for 10 seconds
    And I publish the API with id "<createdApiId>"
    Then The lifecycle status of API "<createdApiId>" should be "Published"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "<createAppPayload>"
    When I create an application with payload "<createAppPayload>"
    Then The response status code should be 201
    And I put the following JSON payload in context as "<apiSubscriptionPayload>"
    """
    {
      "applicationId": "{{applicationId}}",
      "apiId": "{{apiId}}",
      "throttlingPolicy": "Bronze"
    }
    """
    And I subscribe to API "<createdApiId>" using application "<createdAppId>" with payload "<apiSubscriptionPayload>"
    Then The response status code should be 201

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
    Then The response status code should be 200
    When I put the following JSON payload in context as "<createApplicationAccessTokenPayload>"
    """
    {
      "consumerSecret": "{{appConsumerSecret}}",
      "validityPeriod": 3600
    }
    """
    And I request an access token for application id "<createdAppId>" using payload "<createApplicationAccessTokenPayload>"
    Then The response status code should be 200
    And I invoke the API resource at path "/apiTestContext/1.0.0/customers/123/" with method "GET" using access token "<generatedAccessToken>" and payload ""
    Then The response status code should be 401
#    And I invoke the API resource at path "/apiTestContext/1.0.0/customers/123/" with method "GET" using access token "<generatedAccessToken>" and payload ""
#    Then The response status code should be 200
