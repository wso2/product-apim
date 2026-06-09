Feature: New application with multiple consumer secrets disabled

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
    And I extract response field "consumerSecret" and store it as "<appConsumerSecret>"
    And I extract response field "keyMappingId" and store it as "<keyMappingId>"

  # Step 3: Generate Access Token for the newly created application
  Scenario:  Generate Access Token
    When I put the following JSON payload in context as "<createApplicationAccessTokenPayload>"
    """
    {
      "consumerSecret": "{{appConsumerSecret}}",
      "validityPeriod": 3600
    }
    """
    And I request an access token for application id "createdAppId" using payload "<createApplicationAccessTokenPayload>" and key mapping id "<keyMappingId>"
    Then The response status code should be 200
    And I extract response field "accessToken" and store it as "<generatedAccessToken>"

  # Step 6: Delete the consumer key and secret generated for the new application
  Scenario: Delete generated keys
    When I delete the generated keys for "createdAppId"
    Then The response status code should be 200
    Then I retrieve existing application keys for "createdAppId"
    Then Oauth-keys count should be 0

  Scenario: Delete the created Application
    When I delete the application with id "createdAppId"
    Then The response status code should be 200

