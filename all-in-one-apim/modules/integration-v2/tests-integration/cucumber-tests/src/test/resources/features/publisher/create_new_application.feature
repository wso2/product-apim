Feature: Publisher API Management
  This feature tests API creation, validation, deletion, and error handling via the Publisher REST API.

  Background:
    Given The system is ready and I have valid access tokens for current user

 # Step 1: Create new application
  Scenario: Create new application
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "<createAppPayload>"
    When I create an application with payload "<createAppPayload>"
    And I retrieve the application with id "createdAppId"
    Then The response status code should be 200

  # Step 2: Generate keys for application
  Scenario: Generate keys
    When I put the following JSON payload in context as "<generateApplicationKeysPayload>"
    """
    {
      "keyType": "PRODUCTION",
      "grantTypesToBeSupported": [
        "client_credentials"
      ]
    }
    """
    And I generate client credentials for application id "createdAppId" with payload "<generateApplicationKeysPayload>"
    Then The response status code should be 200
    And The response should contain "consumerKey"
    And The response should contain "consumerSecret"

 # Step 3: Invoke API
  Scenario: Invoke API
    When I put the following JSON payload in context as "<createApplicationAccessTokenPayload>"
    """
    {
      "consumerSecret": "{{appConsumerSecret}}",
      "validityPeriod": 3600
    }
    """
    And I request an access token for application id "createdAppId" using payload "<createApplicationAccessTokenPayload>"
    Then The response status code should be 200

  # Step 2: Share the application
  Scenario: Share Application with organization
    When I put JSON payload from file "artifacts/payloads/update_apim_test_app.json" in context as "appUpdatePayload"
    And I update the application "createdAppId" with payload "appUpdatePayload"
    Then The response status code should be 200
    And The response should contain "org1"

  Scenario: Delete the created Application
    When I delete the application with id "createdAppId"
    Then The response status code should be 200



