Feature: Migrated Applications Update

  Background:
    Given The system is ready and I have valid access tokens for current user

  # Step 1: Find migrated application and update it
  Scenario: Retrieve and update migrated Application
    When I fetch the application with name "CustomerApp"
    Then I wait until the response status code is 200
    And I extract response field "list[0].applicationId" and store it as "<migratedAppId>"

    # Update the description and throttling policy of the migrated application
    When I put JSON payload from file "artifacts/payloads/update_migrated_customerApp.json" in context as "migratedAppUpdatePayload"
    And I update the application "migratedAppId" with payload "migratedAppUpdatePayload"
    And I wait until the response status code is 200
    And The response should contain "Updated description for migrated app"
    And The response should contain "50PerMin"
