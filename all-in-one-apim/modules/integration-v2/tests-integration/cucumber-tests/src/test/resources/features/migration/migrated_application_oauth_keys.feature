Feature: Multiple Client Secrets for Migrated Applications

  Background:
    Given The system is ready and I have valid access tokens for current user

  Scenario: Get the existing consumer secret and key mapping id for the migrated application
    When I fetch the application with "CustomerApp" as "<migratedAppId>"
    Then The response status code should be 200
    And I get the consumer secret and key mapping id from file "artifacts/oauthKeys/application_oauth_keys.json" and store them as "<preGeneratedConsumerSecret>" and "<preGeneratedKeyMappingId>"

  # Create multiple secrets for the migrated application
  Scenario: Generate Multiple Consumer Secrets for the Migrated Application
    # First Client secret with custom values
    When I put the following JSON payload in context as "<generateApplicationSecretPayload1>"
    """
    {
      "additionalProperties": {
        "description": "Test multiple secrets for migrated app",
        "expiresIn": 15552000
      }
    }
    """
    And I generate a client secret for application id "<migratedAppId>" using payload "<generateApplicationSecretPayload1>" and key mapping id "<preGeneratedKeyMappingId>"
    Then The response status code should be 201
    And I extract response field "secretValue" and store it as "<newlyGeneratedConsumerSecret1>"
    And I extract response field "secretId" and store it as "<newlyGeneratedSecretId1>"

    # Second Client secret with default values
    When I put the following JSON payload in context as "<generateApplicationSecretPayload2>"
    """
    {}
    """
    And I generate a client secret for application id "<migratedAppId>" using payload "<generateApplicationSecretPayload2>" and key mapping id "<preGeneratedKeyMappingId>"
    Then The response status code should be 201
    And I extract response field "secretValue" and store it as "<newlyGeneratedConsumerSecret2>"
    And I extract response field "secretId" and store it as "<newlyGeneratedSecretId2>"

  Scenario: Generate access tokens
    # Generate access token using the migrated secret
    When I put the following JSON payload in context as "<createApplicationAccessTokenPayload>"
    """
    {
      "consumerSecret": "{{preGeneratedConsumerSecret}}",
      "validityPeriod": 3600
    }
    """
    And I request an access token for application id "<migratedAppId>" using payload "<createApplicationAccessTokenPayload>" and key mapping id "<preGeneratedKeyMappingId>"
    Then The response status code should be 200
    And I extract response field "accessToken" and store it as "<generatedAccessTokenForMigratedSecret>"


    # Generate access token using the first newly generated secret
    When I put the following JSON payload in context as "<createAccessTokenPayloadForNewSecret1>"
    """
    {
      "consumerSecret": "{{newlyGeneratedConsumerSecret1}}",
      "validityPeriod": 3600
    }
    """
    And I request an access token for application id "<migratedAppId>" using payload "<createAccessTokenPayloadForNewSecret1>" and key mapping id "<preGeneratedKeyMappingId>"
    Then The response status code should be 200
    And I extract response field "accessToken" and store it as "<generatedAccessTokenForNewSecret1>"


    # Generate access token using the second newly generated secret
    When I put the following JSON payload in context as "<createAccessTokenPayloadForNewSecret2>"
    """
    {
      "consumerSecret": "{{newlyGeneratedConsumerSecret2}}",
      "validityPeriod": 3600
    }
    """
    And I request an access token for application id "<migratedAppId>" using payload "<createAccessTokenPayloadForNewSecret2>" and key mapping id "<preGeneratedKeyMappingId>"
    Then The response status code should be 200
    And I extract response field "accessToken" and store it as "<generatedAccessTokenForNewSecret2>"

  Scenario: Invoke API
    # Using access token generated from the migrated secret
    And I invoke the API resource at path "/apiContext/1.0.0/customers/123/" with method "GET" using access token "<generatedAccessTokenForMigratedSecret>" and payload ""
    Then The response status code should be 200

    # Using access token generated from the first newly generated consumer secret
    And I invoke the API resource at path "/apiContext/1.0.0/customers/123/" with method "GET" using access token "<generatedAccessTokenForNewSecret1>" and payload ""
    Then The response status code should be 200


  Scenario: Get the secret id for the migrated consumer secret
    When I retrieve existing application secrets for "<migratedAppId>" using key mapping id "<preGeneratedKeyMappingId>"
    Then The response status code should be 200
    And Secrets count should be 3
    Then I extract the first secret details from the response and store it as "<preGeneratedSecretId>"

  Scenario: Delete the migrated consumer secret
    When I put the following JSON payload in context as "<deleteMigratedSecretPayload>"
    """
    {
      "secretId": "{{preGeneratedSecretId}}"
    }
    """
    And I revoke the client secret for application id "<migratedAppId>" using payload "<deleteMigratedSecretPayload>" and key mapping id "<preGeneratedKeyMappingId>"
    Then The response status code should be 204

  Scenario: Delete the first newly generated consumer secret
    When I put the following JSON payload in context as "<deleteNewSecretPayload1>"
    """
    {
      "secretId": "{{newlyGeneratedSecretId1}}"
    }
    """
    And I revoke the client secret for application id "<migratedAppId>" using payload "<deleteNewSecretPayload1>" and key mapping id "<preGeneratedKeyMappingId>"
    Then The response status code should be 204

  Scenario: Attempt to delete the latest consumer secret
    # This should fail because the latest secret must be retained.
    When I put the following JSON payload in context as "<deleteNewSecretPayload2>"
    """
    {
      "secretId": "{{newlyGeneratedSecretId2}}"
    }
    """
    And I revoke the client secret for application id "<migratedAppId>" using payload "<deleteNewSecretPayload2>" and key mapping id "<preGeneratedKeyMappingId>"
    # Then The response status code should be 400
    When I retrieve existing application secrets for "<migratedAppId>" using key mapping id "<preGeneratedKeyMappingId>"
    Then The response status code should be 200
    And Secrets count should be 1


  # Verify the API invocation after deleting the secrets
  Scenario: Invoke API
    # Using access token generated from the migrated secret
    And I invoke the API resource at path "/apiContext/1.0.0/customers/123/" with method "GET" using access token "<generatedAccessTokenForMigratedSecret>" and payload ""
    Then The response status code should be 200

    # Using access token generated from the first newly generated consumer secret
    And I invoke the API resource at path "/apiContext/1.0.0/customers/123/" with method "GET" using access token "<generatedAccessTokenForNewSecret1>" and payload ""
    Then The response status code should be 200
