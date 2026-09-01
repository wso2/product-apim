@cleanup
Feature: Publisher Network Access Control - endpoint URL validation when the feature is disabled

  When no network access-control policy is configured (the default), the outbound host-validation gate is
  inactive and no URL is blocked - a loopback or link-local endpoint URL validates without a "could not be
  resolved" error. This is the negative control proving the gate is opt-in. Ports HostValidationDisabledTestCase.
  Runs in the network-access-control-disabled container (no policy overlay).

  @cap:publisher @feat:network-access-control @rule:disabled @type:regression @legacy:HostValidationDisabledTestCase
  Scenario: Endpoint validation of a loopback URL is not blocked when the feature is disabled
    Given The system is ready and I have valid publisher access tokens as "publisherUser"
    When I validate the endpoint url "http://127.0.0.1:9090/backend"
    Then The response status code should be 200
    And The response should not contain "could not be resolved"

  @cap:publisher @feat:network-access-control @rule:disabled @type:regression @legacy:HostValidationDisabledTestCase
  Scenario: Endpoint validation of a link-local URL is not blocked when the feature is disabled
    Given The system is ready and I have valid publisher access tokens as "publisherUser"
    When I validate the endpoint url "http://169.254.169.254/backend"
    Then The response status code should be 200
    And The response should not contain "could not be resolved"
