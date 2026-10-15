@cleanup
Feature: API Governance Artifact Compliance

  Ports the asynchronous half of the legacy APIComplianceTestCase: a freshly-created API or MCP server is
  evaluated asynchronously against the org's default policy and becomes NON_COMPLIANT (its governedPolicies are
  VIOLATED). The evaluation is a background job that can take a couple of minutes, so the step polls (never
  sleeps) until the status settles. Verified live: the wire value is NON_COMPLIANT (underscore) — the generated
  DTO enum's "NON-COMPLIANT" is not what the REST API returns. Runs ×2 tenant for parity with the legacy Factory.
  The proxy and API-generated MCP server evaluations are in governance/compliance_mcp_variants, and the
  BLOCK-on-deploy policy in governance/compliance_blocking. Teardown via @cleanup removes the created artifacts.

  @cap:governance @feat:compliance @type:regression @dep:publisher @legacy:APIComplianceTestCase
  Scenario Outline: A freshly-created API is evaluated as non-compliant against the default policy as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have a valid Governance access token as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "compApiPayload"
    And I create an "apis" resource with payload "compApiPayload" as "compApiId"
    Then The response status code should be 201
    When I retrieve the compliance of API "compApiId" until the status is "NON_COMPLIANT" within 240 seconds
    Then The response status code should be 200
    And The response should contain "NON_COMPLIANT"
    And The response should contain "VIOLATED"
    And I verify the API compliance response includes governed policies

    Examples:
      | actor            |
      | admin            |
      | admin@tenant1.com |

  # An MCP server is a governed artifact too (type API, extendedType MCP). A freshly-created one is evaluated
  # against the org's default policy just like an API and settles NON_COMPLIANT. Ports MCPComplianceTestCase —
  # MCP governance lives under the governance capability (not the mcp capability).
  @cap:governance @feat:compliance @type:regression @dep:publisher @legacy:MCPComplianceTestCase
  Scenario Outline: A freshly-created MCP server is evaluated for compliance against the default policy as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have a valid Governance access token as "<actor>"
    When I create an MCP server from openapi "artifacts/payloads/OAS/mcp_petstore_legacy_compliance_oas3.json" with backend "http://nodebackend:3001/jaxrs_basic/services/customers/customerservice" as "govMcpId"
    Then The response status code should be 201
    And I extract response field "name" and store it as "govMcpName"
    When I retrieve the compliance of API "govMcpId" until the status is "NON_COMPLIANT" within 240 seconds
    Then The response status code should be 200
    And The response should contain "NON_COMPLIANT"
    # The governed artifact is the MCP server (extendedType MCP), not a plain API.
    And The response should contain "MCP"
    # ARTIFACT IDENTITY — the compliance result must be tied to the artifact it claims to describe. Without this the
    # assertions above only prove that SOME artifact is non-compliant and MCP-typed; a result computed for a
    # different (or stale) artifact would satisfy them just as well. An MCP server is governed as type API with
    # extendedType MCP, so both are pinned, alongside the id/name/version echo.
    And The value of response field "id" should be "{{govMcpId}}"
    And The value of response field "info.type" should be "API"
    And The value of response field "info.extendedType" should be "MCP"
    And The value of response field "info.name" should be "{{govMcpName}}"
    And The value of response field "info.version" should be "1.0.0"
    # The evaluation actually ran against a policy — a PENDING/unevaluated artifact reports no governed policies.
    # Scoped to the shipped default policy by name. governedPolicies lists every policy the org applies, so a
    # sibling scenario's uniquely-named policy would otherwise add an entry here; filtering makes this race-free
    # by construction rather than by the block's thread-count. The count pins that the default was evaluated
    # exactly once — the status set alone cannot tell "evaluated and violated" from "not evaluated at all".
    And The response array field "governedPolicies[?(@.name=='WSO2 API Management Best Practices')].status" should have exactly 1 entries
    And The response field "governedPolicies[?(@.name=='WSO2 API Management Best Practices')].status" should be exactly the list "VIOLATED"

    Examples:
      | actor            |
      | admin            |
      | admin@tenant1.com |
