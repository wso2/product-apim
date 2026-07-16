@cleanup
Feature: Publisher Network Access Control - Remote OpenAPI ref resolution

  Publisher-plane enforcement of the outbound host-validation policy on remote
  OpenAPI/Swagger references embedded in a submitted definition. Under the private-block policy
  (deny + block_private_network_access), a reference that resolves to a loopback/private host is
  rejected before any outbound fetch - validate-openapi and archive import fail with HTTP 400 and a
  "not trusted" error. Ports SafeRefResolutionTestCase (private-block group). Runs in the
  network-access-control-private-block container.

  # Inline definition with a schema reference to a loopback host is blocked (OAS 3.0 / 3.1 / Swagger 2.0).
  @cap:publisher @feat:network-access-control @rule:ref-resolution @type:negative @legacy:SafeRefResolutionTestCase
  Scenario Outline: A loopback reference in a <variant> definition is rejected as not trusted as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I validate the openapi definition from file "<definition>"
    Then The response status code should be 400
    And The response should contain "not trusted"

    Examples:
      | actor                     | variant     | definition                                          |
      | publisherUser             | OAS 3.0     | artifacts/payloads/networkAccessControl/oas30_loopback_ref.json     |
      | publisherUser             | OAS 3.1     | artifacts/payloads/networkAccessControl/oas31_loopback_ref.json     |
      | publisherUser             | Swagger 2.0 | artifacts/payloads/networkAccessControl/swagger20_loopback_ref.json |
      | publisherUser@tenant1.com | OAS 3.0     | artifacts/payloads/networkAccessControl/oas30_loopback_ref.json     |
      | publisherUser@tenant1.com | OAS 3.1     | artifacts/payloads/networkAccessControl/oas31_loopback_ref.json     |
      | publisherUser@tenant1.com | Swagger 2.0 | artifacts/payloads/networkAccessControl/swagger20_loopback_ref.json |

  # Multi-file OpenAPI archive whose master document has a direct reference to a loopback host is blocked
  # (the archive remote-reference gate rejects it before resolution) - OAS 3.0 and Swagger 2.0.
  @cap:publisher @feat:network-access-control @rule:archive-ref @type:negative @legacy:SafeRefResolutionTestCase
  Scenario Outline: A loopback reference in a <variant> OpenAPI archive is rejected as not trusted as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I import api from archive "<archive>" with additional properties "artifacts/payloads/archive_additional_properties.json" as "networkAccessControlArchiveApiId"
    Then The response status code should be 400
    And The response should contain "not trusted"

    Examples:
      | actor                     | variant     | archive                                                |
      | publisherUser             | OAS 3.0     | artifacts/payloads/networkAccessControl/oas30_archive_loopback.zip     |
      | publisherUser             | Swagger 2.0 | artifacts/payloads/networkAccessControl/swagger20_archive_loopback.zip |
      | publisherUser@tenant1.com | OAS 3.0     | artifacts/payloads/networkAccessControl/oas30_archive_loopback.zip     |
      | publisherUser@tenant1.com | Swagger 2.0 | artifacts/payloads/networkAccessControl/swagger20_archive_loopback.zip |
