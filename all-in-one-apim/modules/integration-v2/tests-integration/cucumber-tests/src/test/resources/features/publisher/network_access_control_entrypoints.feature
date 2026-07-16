@cleanup
Feature: Publisher Network Access Control - additional publisher entry points

  Publisher-plane enforcement of the outbound host-validation policy on two further entry points that resolve
  a user-supplied OpenAPI definition: updating an existing API's definition (PUT swagger) and importing an
  OpenAPI as an MCP server. Under the private-block policy (deny + block_private_network_access) a reference
  that resolves to a loopback host is rejected with HTTP 400 and a "not trusted" error before any outbound
  fetch. Runs in the network-access-control-private-block container.

  # Updating an existing API's definition (PUT /apis/{id}/swagger) with a loopback reference is blocked. The
  # API is first created from a clean definition (no remote reference) whose backend endpoint is a public IP
  # (8.8.8.8, never contacted) so the create passes the policy; the update then replaces the definition with a
  # loopback reference, which is the only blockable URL. asserting the definition-gate message pins the block to that gate.
  @cap:publisher @feat:network-access-control @rule:update-swagger @type:negative
  Scenario Outline: Updating an API definition with a loopback <variant> reference is rejected as not trusted as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I import openapi definition from "artifacts/payloads/networkAccessControl/<seed>" with additional properties "artifacts/payloads/networkAccessControl/nac_seed_public_props.json" as "nacUpdateApiId"
    When I update the swagger of "apis" resource "nacUpdateApiId" from file "artifacts/payloads/networkAccessControl/<blocked>"
    Then The response status code should be 400
    And The response should contain "not trusted"
    And The response should contain "definition contains a URL that is not trusted"

    Examples:
      | actor                     | variant     | seed                      | blocked                     |
      | publisherUser             | OAS 3.0     | oas30_seed_clean.json     | oas30_loopback_ref.json     |
      | publisherUser             | Swagger 2.0 | swagger20_seed_clean.json | swagger20_loopback_ref.json |
      | publisherUser@tenant1.com | OAS 3.0     | oas30_seed_clean.json     | oas30_loopback_ref.json     |
      | publisherUser@tenant1.com | Swagger 2.0 | swagger20_seed_clean.json | swagger20_loopback_ref.json |

  # Importing an OpenAPI whose schema reference targets a loopback host as an MCP server is blocked when the
  # embedded reference is resolved (createMCPServerFromOpenAPI validates the definition through the same gate).
  # The MCP backend is a public IP (8.8.8.8, never contacted) so the endpoint validation the MCP create runs
  # first passes - the loopback $ref is then the only blockable URL, and asserting the definition-gate message
  # pins the block to that gate rather than the endpoint gate (900405).
  @cap:publisher @feat:network-access-control @rule:mcp-import @type:negative
  Scenario Outline: Importing an OpenAPI with a loopback reference as an MCP server is rejected as not trusted as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I create an MCP server from openapi "artifacts/payloads/networkAccessControl/mcp_oas30_loopback_ref.json" with backend "http://8.8.8.8" as "nacMcpBlockedId"
    Then The response status code should be 400
    And The response should contain "not trusted"
    And The response should contain "definition contains a URL that is not trusted"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |
