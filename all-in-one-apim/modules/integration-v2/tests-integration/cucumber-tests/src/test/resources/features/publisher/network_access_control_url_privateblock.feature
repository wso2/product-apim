@cleanup
Feature: Publisher Network Access Control - endpoint URL validation under the private-block policy

  Publisher-plane enforcement of the outbound host-validation policy on user-supplied endpoint URLs (not
  definition references). Under the private-block policy (deny + block_private_network_access), a URL that
  resolves to a loopback or link-local host is rejected before any outbound fetch: endpoint validation reports
  the block, and Key Manager / WSDL entry points fail with HTTP 400 and a "could not be resolved" error. Ports
  the HostValidationPrivateBlockTestCase URL-gate cases. Runs in the network-access-control-private-block container.

  # Endpoint validation returns 200 with an error field (not a 4xx), so the block is asserted on the body.
  @cap:publisher @feat:network-access-control @rule:endpoint-validation @type:negative @legacy:HostValidationPrivateBlockTestCase
  Scenario: Endpoint validation of a loopback URL is blocked under the private-block policy
    Given The system is ready and I have valid publisher access tokens as "publisherUser"
    When I validate the endpoint url "http://127.0.0.1:9090/backend"
    Then The response status code should be 200
    And The response should contain "could not be resolved"

  @cap:publisher @feat:network-access-control @rule:key-manager @type:negative @legacy:HostValidationPrivateBlockTestCase
  Scenario: Key Manager creation with a link-local URL is rejected under the private-block policy
    Given The system is ready and I have valid publisher access tokens as "publisherUser"
    And I have valid access tokens as "admin"
    When I create a key manager with endpoints on host "169.254.169.254"
    Then The response status code should be 400
    And The response should contain "could not be resolved"

  @cap:publisher @feat:network-access-control @rule:wsdl-url @type:negative @legacy:HostValidationPrivateBlockTestCase
  Scenario: WSDL import from a loopback URL is rejected under the private-block policy
    Given The system is ready and I have valid publisher access tokens as "publisherUser"
    When I import a wsdl api from url "http://127.0.0.1:9090/service?wsdl" with properties file "artifacts/payloads/networkAccessControl/wsdl_additional_properties.json"
    Then The response status code should be 400
    And The response should contain "could not be resolved"
