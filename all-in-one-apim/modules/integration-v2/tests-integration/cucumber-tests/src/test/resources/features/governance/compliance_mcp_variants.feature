@cleanup
Feature: API Governance MCP Server Compliance

  Proxy and API-generated MCP servers (the MCP variants of the legacy APIComplianceTestCase evaluation) are
  evaluated asynchronously against the org's default policy and become NON_COMPLIANT. The evaluation is a
  background job that can take a couple of minutes, so the step polls (never sleeps) until the status settles.
  Runs ×2 tenant. Teardown via @cleanup removes the created artifacts.

  # Same governance evaluation for a PROXY-created MCP server (compliance is create-method-agnostic; covered per
  # type for symmetry with the invocation suites).
  @cap:governance @feat:compliance @type:regression @dep:publisher @legacy:MCPComplianceTestCase
  Scenario Outline: A freshly-created proxy MCP server is evaluated for compliance against the default policy as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have a valid Governance access token as "<actor>"
    When I create an MCP server proxy to "http://nodebackend:3020/mcp" exposing tools "echo" as "govProxyMcpId"
    Then The response status code should be 201
    When I retrieve the compliance of API "govProxyMcpId" until the status is "NON_COMPLIANT" within 240 seconds
    Then The response status code should be 200
    And The response should contain "NON_COMPLIANT"
    And The response should contain "MCP"

    Examples:
      | actor            |
      | admin            |
      | admin@tenant1.com |

  # Same governance evaluation for an API-generated MCP server.
  @cap:governance @feat:compliance @type:regression @dep:publisher @legacy:MCPComplianceTestCase
  Scenario Outline: A freshly-created API-generated MCP server is evaluated for compliance against the default policy as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have a valid Governance access token as "<actor>"
    When I import openapi definition from "artifacts/payloads/OAS/mcp_petstore_oas3.json" with additional properties "artifacts/payloads/mcp_petstore_api_props.json" as "govBackingApiId"
    Then The response status code should be 201
    When I deploy the "apis" resource with id "govBackingApiId"
    When I create an MCP server from api "govBackingApiId" exposing paths "/pets" as "govApiMcpId"
    Then The response status code should be 201
    When I retrieve the compliance of API "govApiMcpId" until the status is "NON_COMPLIANT" within 240 seconds
    Then The response status code should be 200
    And The response should contain "NON_COMPLIANT"
    And The response should contain "MCP"

    Examples:
      | actor            |
      | admin            |
      | admin@tenant1.com |
