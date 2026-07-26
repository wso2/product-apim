@cleanup
Feature: API Governance Artifact Compliance

  Ports the legacy APIComplianceTestCase: governance enforcement over artifacts. Two behaviours:
  (1) a policy with a BLOCK action on API_DEPLOY (attaching the default rulesets a bare API violates) rejects
      deployment — creating a revision fails with 903300. This is synchronous and deterministic, so it runs
      ×2 tenant.
  (2) a freshly-created API is evaluated asynchronously against the org's default policy and becomes
      NON_COMPLIANT (its governedPolicies are VIOLATED). The evaluation is a background job that can take a
      couple of minutes, so the step polls (never sleeps) until the status settles. Verified live: the wire
      value is NON_COMPLIANT (underscore) — the generated DTO enum's "NON-COMPLIANT" is not what the REST API
      returns. Runs ×2 tenant for parity with the legacy Factory; the two async evaluations overlap with the
      other governance runners at CP=2, so the block wall-clock stays bounded by the ~2-minute poll ceiling.
  Teardown via @cleanup removes the created API and the governance policy (policy before its rulesets).

  @cap:governance @feat:compliance @type:regression @dep:publisher @legacy:APIComplianceTestCase
  Scenario Outline: A blocking governance policy rejects API deployment as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have a valid Governance access token as "<actor>"
    When I create a governance policy "${UNIQUE:BlockPolicy}" attaching the default rulesets blocking API deployment as "blockPolicyId"
    Then The response status code should be 201
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "govApiPayload"
    And I create an "apis" resource with payload "govApiPayload" as "govApiId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "govRevPayload"
    """
    {"description":"governance revision"}
    """
    And I attempt to create a revision for "apis" resource "govApiId" with payload "govRevPayload"
    Then The response status code should be 400
    And The response should contain "903300"

    Examples:
      | actor            |
      | admin            |
      | admin@tenant1.com |

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
    When I create an MCP server from openapi "artifacts/payloads/OAS/mcp_petstore_oas3.json" with backend "http://nodebackend:3001/jaxrs_basic/services/customers/customerservice" as "govMcpId"
    Then The response status code should be 201
    When I retrieve the compliance of API "govMcpId" until the status is "NON_COMPLIANT" within 240 seconds
    Then The response status code should be 200
    And The response should contain "NON_COMPLIANT"
    # The governed artifact is the MCP server (extendedType MCP), not a plain API.
    And The response should contain "MCP"

    Examples:
      | actor            |
      | admin            |
      | admin@tenant1.com |

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
