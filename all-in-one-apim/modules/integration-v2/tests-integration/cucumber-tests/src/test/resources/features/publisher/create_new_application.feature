Feature: Publisher API Management
  This feature tests application creation, key generation, multiple client secret handling,
  token generation, API invocation, secret deletion rules, application sharing, and cleanup
  for a newly created application.

  Background:
    Given The system is ready and I have valid access tokens for current user

  # Create a new application
  Scenario: Create new application
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "<createAppPayload>"
    And I create an application with payload "<createAppPayload>"
    And I retrieve the application with id "<createdAppId>"
    Then The response status code should be 200

  Scenario: Create a new API, deploy and publish it
    When I have created an api from "artifacts/payloads/create_apim_test_api.json" as "<createdApiId>" and deployed it
    And I retrieve the "apis" resource with id "<createdApiId>"
    And I put the response payload in context as "<createdAPIPayload>"
    And I wait for deployment of the resource in "<createdAPIPayload>"
    And I publish the "apis" resource with id "<createdApiId>"
    Then The response status code should be 200
    Then The lifecycle status of API "<createdApiId>" should be "Published"

  Scenario: Subscribe the new API using the created application
    And I put the following JSON payload in context as "<apiSubscriptionPayload>"
    """
    {
      "applicationId": "{{createdAppId}}",
      "apiId": "{{createdApiId}}",
      "throttlingPolicy": "Bronze"
    }
    """
    And I create a subscription using payload "<apiSubscriptionPayload>"
    Then The response status code should be 201
    And I extract response field "subscriptionId" and store it as "<subscriptionId>"

  # Generate the initial production key mapping for the application
  Scenario: Generate initial application keys
    When I put the following JSON payload in context as "<generateApplicationKeysPayload>"
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
    And I extract response field "consumerSecret" and store it as "<appConsumerSecret>"
    And I extract response field "keyMappingId" and store it as "<keyMappingId>"

  # Create two additional client secrets for the same key mapping
  Scenario: Generate multiple client secrets for the new application
    # First additional client secret with custom values
    When I put the following JSON payload in context as "<generateApplicationSecretPayload1>"
    """
    {
      "additionalProperties": {
        "description": "First additional client secret",
        "expiresIn": 15552000
      }
    }
    """
    And I generate a client secret for application id "createdAppId" using payload "<generateApplicationSecretPayload1>" and key mapping id "<keyMappingId>"
    Then The response status code should be 201
    And I extract response field "secretValue" and store it as "<generatedConsumerSecret1>"
    And I extract response field "secretId" and store it as "<generatedSecretId1>"

    # Second additional client secret using default values
    When I put the following JSON payload in context as "<generateApplicationSecretPayload2>"
    """
    {}
    """
    And I generate a client secret for application id "createdAppId" using payload "<generateApplicationSecretPayload2>" and key mapping id "<keyMappingId>"
    Then The response status code should be 201
    And I extract response field "secretValue" and store it as "<generatedConsumerSecret2>"
    And I extract response field "secretId" and store it as "<generatedSecretId2>"

  # Generate tokens
  Scenario: Generate access tokens using two client secrets
    # Generate token using the initial secret created with the application keys
    When I put the following JSON payload in context as "<createApplicationAccessTokenPayload>"
    """
    {
      "consumerSecret": "{{appConsumerSecret}}",
      "validityPeriod": 3600
    }
    """
    And I request an access token for application id "createdAppId" using payload "<createApplicationAccessTokenPayload>" and key mapping id "<keyMappingId>"
    Then The response status code should be 200
    And I extract response field "accessToken" and store it as "<generatedAccessTokenForInitialSecret>"

    # Generate token using the first additional secret
    When I put the following JSON payload in context as "<createAccessTokenPayloadForGeneratedSecret1>"
    """
    {
      "consumerSecret": "{{generatedConsumerSecret1}}",
      "validityPeriod": 3600
    }
    """
    And I request an access token for application id "<createdAppId>" using payload "<createAccessTokenPayloadForGeneratedSecret1>" and key mapping id "<keyMappingId>"
    Then The response status code should be 200
    And I extract response field "accessToken" and store it as "<generatedAccessTokenForGeneratedSecret1>"

  # Verify that the issued tokens can invoke the API
  Scenario: Invoke API using tokens generated from two client secrets
    # Token from the initial secret
    And I invoke the API resource at path "/apiTestContext/1.0.0/customers/123/" with method "GET" using access token "<generatedAccessTokenForInitialSecret>" and payload ""
    Then The response status code should be 200

    # Token from the first additional secret
    And I invoke the API resource at path "/apiTestContext/1.0.0/customers/123/" with method "GET" using access token "<generatedAccessTokenForGeneratedSecret1>" and payload ""
    Then The response status code should be 200

  # Confirm that the application now has three secrets in total
  Scenario: Verify the application has three client secrets
    When I retrieve existing application secrets for "<createdAppId>" using key mapping id "<keyMappingId>"
    Then The response status code should be 200
    And Secrets count should be 3
    # Store the original secret id so it can be deleted later.
    And I extract the first secret details from the response and store it as "<initialSecretId>"

  # Delete the original secret
  Scenario: Delete the initial client secret
    When I put the following JSON payload in context as "<deleteInitialSecretPayload>"
    """
    {
      "secretId": "{{initialSecretId}}"
    }
    """
    And I revoke the client secret for application id "<createdAppId>" using payload "<deleteInitialSecretPayload>" and key mapping id "<keyMappingId>"
    Then The response status code should be 204

  # Delete the first additional secret
  Scenario: Delete the first additional client secret
    When I put the following JSON payload in context as "<deleteGeneratedSecretPayload1>"
    """
    {
      "secretId": "{{generatedSecretId1}}"
    }
    """
    And I revoke the client secret for application id "<createdAppId>" using payload "<deleteGeneratedSecretPayload1>" and key mapping id "<keyMappingId>"
    Then The response status code should be 204

  # Attempt to delete the latest remaining secret
  # This is expected to fail
  Scenario: Attempt to delete the latest client secret
    When I put the following JSON payload in context as "<deleteGeneratedSecretPayload2>"
    """
    {
      "secretId": "{{generatedSecretId2}}"
    }
    """
    And I revoke the client secret for application id "<createdAppId>" using payload "<deleteGeneratedSecretPayload2>" and key mapping id "<keyMappingId>"
    # Then The response status code should be 400

  # After deleting the first two secrets, only the latest secret should remain
  Scenario: Verify only the latest client secret remains
    When I retrieve existing application secrets for "<createdAppId>" using key mapping id "<keyMappingId>"
    Then The response status code should be 200
    And Secrets count should be 1

  # The oldest issued token should remain valid even after its secret is deleted
  Scenario: Verify oldest issued token remains valid after secret deletion
    And I invoke the API resource at path "/apiTestContext/1.0.0/customers/123/" with method "GET" using access token "<generatedAccessTokenForInitialSecret>" and payload ""
    Then The response status code should be 200

  # Share the application with an organization
  Scenario: Share application with organization
    When I put JSON payload from file "artifacts/payloads/update_apim_test_app.json" in context as "<appUpdatePayload>"
    And I update the application "<createdAppId>" with payload "<appUpdatePayload>"
    Then The response status code should be 200
    And The response should contain "org1"

  # Clean up the resources
  Scenario: Delete the subscription
    When I delete the subscription with id "<subscriptionId>"
    Then The response status code should be 200

  Scenario: Delete the created application
    When I delete the application with id "<createdAppId>"
    Then The response status code should be 200

  Scenario: Delete the created API
    When I delete the "apis" resource with id "<createdApiId>"
    Then The response status code should be 200
