@cleanup
Feature: Publisher Network Access Control - Nested WSDL import

  Publisher-plane enforcement of the outbound host-validation policy on nested schema imports embedded in
  a WSDL 1.1 document imported as a SOAP API. Under the private-block policy, a nested xsd:import whose
  schemaLocation targets a loopback host, or uses a filesystem path traversal, is rejected with HTTP 400
  and a "not trusted" error before any fetch. Ports HostValidationWsdlNestedImportTestCase. Runs in the
  network-access-control-private-block container.

  @cap:publisher @feat:network-access-control @rule:wsdl-nested-import @type:negative @legacy:HostValidationWsdlNestedImportTestCase
  Scenario Outline: A WSDL nested xsd:import via <target> is rejected as not trusted as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I attempt to import wsdl from archive "<archive>" with additional properties "artifacts/payloads/networkAccessControl/wsdl_additional_properties.json"
    Then The response status code should be 400
    And The response should contain "not trusted"

    Examples:
      | actor                     | target           | archive                                          |
      | publisherUser             | a loopback host  | artifacts/payloads/networkAccessControl/wsdl_nested_loopback.zip |
      | publisherUser             | a path traversal | artifacts/payloads/networkAccessControl/wsdl_nested_traversal.zip |
      | publisherUser@tenant1.com | a loopback host  | artifacts/payloads/networkAccessControl/wsdl_nested_loopback.zip |
      | publisherUser@tenant1.com | a path traversal | artifacts/payloads/networkAccessControl/wsdl_nested_traversal.zip |
