Feature: Migrated Applications Update

  Background:
    Given The system is ready and I have valid access tokens for current user

  # Step 1: Find migrated application and update it
  Scenario: Retrieve and update migrated Application
    When I fetch the application with "CustomerApp" as "migratedAppId"
    Then The response status code should be 200

    # Update the description and throttling policy of the migrated application
    When I put JSON payload from file "artifacts/payloads/update_migrated_customerApp.json" in context as "migratedAppUpdatePayload"
    And I update the application "migratedAppId" with payload "migratedAppUpdatePayload"
    Then The response status code should be 200
    And The response should contain "Updated description for migrated app"
    And The response should contain "50PerMin"
