@cleanup
Feature: DevPortal Application Group Sharing

  DevPortal-plane organization/group sharing: an application updated with a `groups` field is shared with that
  organization and the field is persisted on the application. The default container silently drops `groups`
  (so devportal/applications deliberately does NOT assert it) — this feature runs in the
  IntegrationV2-CustomAuthHeaderAndAppSharing block, whose overlay enables
  `[apim.devportal] enable_application_sharing`. Runs as a DevPortal consumer in both the super tenant and
  tenant1.com. Teardown via the per-scenario cleanup hook (the create step registers the application).

  @cap:devportal @feat:applications @type:regression @rule:org-sharing @legacy:ApplicationCreationTestCase
  Scenario Outline: An application can be shared with an organization group as <actor>
    Given The system is ready and I have valid devportal access token as "<actor>"

    # Create the application
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201

    # Share it with organization "org1": with application sharing enabled the groups field is persisted
    # (the default container would silently drop it).
    When I put JSON payload from file "artifacts/payloads/update_apim_test_app.json" in context as "appUpdatePayload"
    And I update the application "createdAppId" with payload "appUpdatePayload"
    Then The response status code should be 200
    And The response should contain "org1"

    Examples:
      | actor                       |
      | subscriberUser              |
      | subscriberUser@tenant1.com  |
