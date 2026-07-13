@cleanup
Feature: DevPortal Application Management

  DevPortal-plane application lifecycle: create an application, retrieve it, update it (name/description/
  throttling), and delete it. Runs as a DevPortal consumer (subscriber-role) in both the super tenant and
  tenant1.com. Key generation and token issuance are covered by key-manager/oauth-keys. Teardown via the
  per-scenario cleanup hook (the create step registers the application).

  Note: cross-tenant organization (group) sharing is NOT asserted here — it requires group-sharing enabled in
  the server config (claim/`enable_cross_tenant_group_sharing`), which the default container does not carry;
  the default server silently drops the `groups` field. That assertion belongs in a feature with its own
  group-sharing TOML overlay, like gateway/custom-auth-header has its own block.

  @cap:devportal @feat:applications @type:smoke @legacy:ApplicationCreationTestCase
  Scenario Outline: Create, update and delete an application as <actor>
    Given The system is ready and I have valid devportal access token as "<actor>"

    # Create the application
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I retrieve the application with id "createdAppId"
    Then The response status code should be 200

    # Update the application
    When I put JSON payload from file "artifacts/payloads/update_apim_test_app.json" in context as "appUpdatePayload"
    And I update the application "createdAppId" with payload "appUpdatePayload"
    Then The response status code should be 200
    And The response should contain "Test application for scenarios"

    # Delete the application
    When I delete the application with id "createdAppId"
    Then The response status code should be 200

    Examples:
      | actor                       |
      | subscriberUser              |
      | subscriberUser@tenant1.com  |

  @cap:devportal @feat:applications @type:negative @legacy:ApplicationCreationTestCase
  Scenario Outline: A publisher-role user without app-management scope cannot create an application as <actor>
    Given The system is ready and I have valid devportal access token as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I attempt to create an application with payload "createAppPayload"
    Then The response status code should be 401

    Examples:
      | actor                      |
      | publisherUser              |
      | publisherUser@tenant1.com  |
