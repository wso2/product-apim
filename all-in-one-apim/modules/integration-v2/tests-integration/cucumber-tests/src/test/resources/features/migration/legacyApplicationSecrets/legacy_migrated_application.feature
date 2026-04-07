Feature: Migrated Applications with multiple consumer secrets disabled
  This feature uses the already created application client secret to generate new access token

  Background:
    Given The system is ready and I have valid access tokens for current user

  # Find migrated application
  Scenario: Retrieve and update migrated Application
    When I fetch the application with "CustomerApp" as "<migratedAppId>"
    Then The response status code should be 200

  Scenario: Get consumer secret of the Migrated application
    When I retrieve existing application keys for "<migratedAppId>"
    Then The response status code should be 200
    # Get the existing consumerSecret and keyMappingId of the migrated application
    And I extract the first oauth2 key details from the application keys response and store them as "<appConsumerSecret>" and "<keyMappingId>"

  # Generate an access token using the existing keys of the application
  Scenario: Generate Access Token for Migrated application
    When I put the following JSON payload in context as "<AccessTokenPayload>"
    """
    {
      "consumerSecret": "{{appConsumerSecret}}",
      "validityPeriod": 3600
    }
    """
    And I request an access token for application id "<migratedAppId>" using payload "<AccessTokenPayload>" and key mapping id "<keyMappingId>"
    Then The response status code should be 200

