@cleanup
Feature: Publisher Network Access Control - endpoint URL validation under an allow-mode policy

  Publisher-plane enforcement of the outbound host-validation policy on user-supplied endpoint URLs under an
  allow-mode platform policy. In allow mode only allow-listed hosts pass, so any other host - including a
  private-network address - is blocked, and a platform allow-mode policy overrides a tenant-level allow policy
  (the platform decision is authoritative). Ports the HostValidationAllowModeTestCase cases. Runs in the
  network-access-control-allow container (allow-mode, only the in-network fixtures host allow-listed).

  @cap:publisher @feat:network-access-control @rule:allow-mode @type:negative @legacy:HostValidationAllowModeTestCase
  Scenario: Endpoint validation of a non-allow-listed URL is blocked under allow mode
    Given The system is ready and I have valid publisher access tokens as "publisherUser"
    When I validate the endpoint url "http://evil.attacker.com/endpoint"
    Then The response status code should be 200
    And The response should contain "could not be resolved"

  @cap:publisher @feat:network-access-control @rule:allow-mode @type:negative @legacy:HostValidationAllowModeTestCase
  Scenario: Endpoint validation of a private-IP URL is blocked under allow mode
    Given The system is ready and I have valid publisher access tokens as "publisherUser"
    When I validate the endpoint url "http://10.10.10.10/endpoint"
    Then The response status code should be 200
    And The response should contain "could not be resolved"

  # A tenant allow policy cannot widen what the platform allow policy permits: the non-listed host stays blocked.
  @cap:publisher @feat:network-access-control @rule:precedence @type:negative @legacy:HostValidationAllowModeTestCase @dep:admin
  Scenario: A platform allow-mode policy overrides a tenant allow policy
    Given The system is ready and I have valid publisher access tokens as "publisherUser"
    And I have valid access tokens as "admin"
    And I apply a tenant allow policy for hosts "*.corp" as "admin"
    And I act as "publisherUser"
    When I validate the endpoint url "http://api.corp/endpoint"
    Then The response status code should be 200
    And The response should contain "could not be resolved"
    And I remove the applied tenant policy
