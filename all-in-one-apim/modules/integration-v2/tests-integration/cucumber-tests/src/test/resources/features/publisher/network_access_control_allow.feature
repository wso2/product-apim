@cleanup
Feature: Publisher Network Access Control - allow-mode remote reference resolution

  Positive controls for the outbound host-validation policy. Under an allow-mode policy that allow-lists the
  in-network fixtures host, a remote reference to that host is fetched and the definition validates - it is
  NOT blocked as "not trusted". And even under allow-mode, a nested reference from an allow-listed document
  to a non-allow-listed host is still rejected, because the resolver re-validates every crawled reference.
  Ports SafeRefResolutionTestCase (allow-mode group). Runs in the network-access-control-allow container (needs the node
  fixtures backend on nodebackend:3021).

  # An allow-listed remote reference resolves (is fetched) and the definition validates - not blocked.
  @cap:publisher @feat:network-access-control @rule:allow-mode @type:regression @legacy:SafeRefResolutionTestCase
  Scenario Outline: An allow-listed remote reference in a <variant> definition is fetched, not blocked, as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I validate the openapi definition from file "<definition>"
    Then The response status code should be 200
    And The response should not contain "not trusted"

    Examples:
      | actor                     | variant     | definition                                             |
      | publisherUser             | OAS 3.0     | artifacts/payloads/networkAccessControl/oas30_nodebackend_ref.json     |
      | publisherUser             | OAS 3.1     | artifacts/payloads/networkAccessControl/oas31_nodebackend_ref.json     |
      | publisherUser             | Swagger 2.0 | artifacts/payloads/networkAccessControl/swagger20_nodebackend_ref.json |
      | publisherUser@tenant1.com | OAS 3.0     | artifacts/payloads/networkAccessControl/oas30_nodebackend_ref.json     |
      | publisherUser@tenant1.com | OAS 3.1     | artifacts/payloads/networkAccessControl/oas31_nodebackend_ref.json     |
      | publisherUser@tenant1.com | Swagger 2.0 | artifacts/payloads/networkAccessControl/swagger20_nodebackend_ref.json |

  # A nested reference from an allow-listed document to a non-allow-listed loopback host is still blocked
  # (the crawl re-validates nested references, so the second-order reference cannot escape the policy).
  @cap:publisher @feat:network-access-control @rule:nested-ref @type:negative @legacy:SafeRefResolutionTestCase
  Scenario Outline: A nested reference to a non-allow-listed host is rejected as not trusted as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I validate the openapi definition from file "artifacts/payloads/networkAccessControl/swagger20_nested_ref.json"
    Then The response status code should be 400
    And The response should contain "not trusted"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |
