@cleanup
Feature: MCP Server authoring (publisher plane)

  Publisher-plane CRUD of MCP servers across all three creation types — proxy (to a third-party MCP server),
  from-OpenAPI (DirectBackend), and from-API (ExistingApi) — plus backend-endpoint management. Gateway
  invocation is in gateway/mcp_invocation.feature. Needs the node MCP backend (proxy create-validation). ×2 tenant.

  # CRUD: create exposing a subset of the backend's tools, read them back, update the exposed set, delete.
  @cap:publisher @feat:mcp-servers @rule:proxy @type:regression @dep:publisher @legacy:MCPServerTestCase
  Scenario Outline: Full CRUD lifecycle of a proxied MCP server as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    # CREATE — expose only echo + add (of the backend's echo/add/get_pets); assert the discovered tools persist
    When I create an MCP server proxy to "http://nodebackend:3020/mcp" exposing tools "echo,add" as "mcpId"
    Then The response status code should be 201
    And The response should contain "echo"
    And The response should contain "add"
    # Least-privilege: the backend also offers get_pets, but it was NOT selected — so it must not be exposed.
    And The response should not contain "get_pets"
    # READ — retrieve returns the server with its operations (still the selected subset only)
    When I retrieve the "mcp-servers" resource with id "mcpId"
    Then The response status code should be 200
    And The response should contain "echo"
    And The response should contain "add"
    And The response should not contain "get_pets"
    # UPDATE (ADD) — expand the exposed set to add get_pets; the persisted operations reflect it
    When I update the MCP server "mcpId" to expose tools "echo,add,get_pets"
    Then The response status code should be 200
    And The response should contain "get_pets"
    # READ-BACK — the add persisted
    When I retrieve the "mcp-servers" resource with id "mcpId"
    Then The response should contain "get_pets"
    # UPDATE (REMOVE) — narrow back to echo,add; get_pets is dropped
    When I update the MCP server "mcpId" to expose tools "echo,add"
    Then The response status code should be 200
    When I retrieve the "mcp-servers" resource with id "mcpId"
    Then The response should contain "echo"
    And The response should not contain "get_pets"
    # DELETE — removed; a subsequent retrieve 404s
    When I delete the MCP server "mcpId"
    Then The response status code should be 200
    When I retrieve the "mcp-servers" resource with id "mcpId"
    Then The response status code should be 404

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Backend-endpoint management for a PROXY MCP server. A correct update PUTs the backend back in full, INCLUDING
  # its `definition` (MCP-tools JSON, not an OpenAPI spec). carbon-apimgt < 9.33.147 wrongly re-validated that
  # definition as OpenAPI on update, failing a correct PUT with 900754 "Error while parsing OpenAPI definition —
  # attribute tools is unexpected" (HTTP 400) — a product regression. It is fixed in carbon-apimgt 9.33.147, the
  # version this branch now builds, so the scenario is enabled. Do NOT work around any recurrence by stripping the
  # definition (that would hide a regression).
  @cap:publisher @feat:mcp-servers @rule:proxy @type:regression @dep:publisher @legacy:MCPServerTestCase
  Scenario Outline: Manage the backend endpoint of a proxied MCP server as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create an MCP server proxy to "http://nodebackend:3020/mcp" exposing tools "echo,add" as "mcpId"
    Then The response status code should be 201
    # LIST the server's backend endpoints and capture the (single) backend's id
    When I retrieve the backends of MCP server "mcpId" and store the first backend id as "mcpBackendId"
    Then The response status code should be 200
    # GET the backend by id and capture it for a round-trip update
    When I retrieve backend "mcpBackendId" of MCP server "mcpId"
    Then The response status code should be 200
    And I put the response payload in context as "mcpBackendPayload"
    # endpointConfig is a stringified JSON blob (escaped \/), so edit the endpoint URL at text level (its port
    # has no slashes). The definition is sent back unchanged (a correct update includes it).
    When I replace "nodebackend:3020" with "nodebackend:3021" in the payload "mcpBackendPayload"
    And I update backend "mcpBackendId" of MCP server "mcpId" with payload "mcpBackendPayload"
    Then The response status code should be 200
    When I retrieve backend "mcpBackendId" of MCP server "mcpId"
    Then The response status code should be 200
    And The response should contain "nodebackend:3021"
    When I delete the MCP server "mcpId"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # CRUD: create from OAS (both tools generated), read, narrow to a subset (remove a tool), delete.
  @cap:publisher @feat:mcp-servers @rule:openapi @type:regression @dep:publisher @legacy:MCPServerTestCase
  Scenario Outline: Full CRUD lifecycle of an OpenAPI-generated MCP server as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create an MCP server from openapi "artifacts/payloads/OAS/mcp_petstore_oas3.json" with backend "http://nodebackend:3001/jaxrs_basic/services/customers/customerservice" as "mcpId"
    Then The response status code should be 201
    And The response should contain "get_pets"
    And The response should contain "get_pets_by_petId"
    When I retrieve the "mcp-servers" resource with id "mcpId"
    Then The response status code should be 200
    And The response should contain "get_pets"
    And The response should contain "get_pets_by_petId"
    # UPDATE (REMOVE) — narrow the exposed tools to just get_pets (docs "select tools to import" / least-privilege).
    When I update the MCP server "mcpId" removing tool "get_pets_by_petId"
    Then The response status code should be 200
    When I retrieve the "mcp-servers" resource with id "mcpId"
    Then The response should contain "get_pets"
    And The response should not contain "get_pets_by_petId"
    # UPDATE (ADD) — re-add the removed tool (inverse of remove); it comes back
    When I re-add the removed tool to the MCP server "mcpId"
    Then The response status code should be 200
    When I retrieve the "mcp-servers" resource with id "mcpId"
    Then The response should contain "get_pets_by_petId"
    When I delete the MCP server "mcpId"
    Then The response status code should be 200
    When I retrieve the "mcp-servers" resource with id "mcpId"
    Then The response status code should be 404

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Backend-endpoint management: an OpenAPI-generated MCP server has its OWN backend (the REST endpoint the
  # generated tools call). List it, get it by id, update its URL, and read the update back. (list/get/update
  # only — the backend is created implicitly with the server and has no separate add/delete.)
  @cap:publisher @feat:mcp-servers @rule:openapi @type:regression @dep:publisher @legacy:MCPServerTestCase
  Scenario Outline: Manage the backend endpoint of an OpenAPI-generated MCP server as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create an MCP server from openapi "artifacts/payloads/OAS/mcp_petstore_oas3.json" with backend "http://nodebackend:3001/jaxrs_basic/services/customers/customerservice" as "mcpId"
    Then The response status code should be 201
    When I retrieve the backends of MCP server "mcpId" and store the first backend id as "mcpBackendId"
    Then The response status code should be 200
    When I retrieve backend "mcpBackendId" of MCP server "mcpId"
    Then The response status code should be 200
    And I put the response payload in context as "mcpBackendPayload"
    # A correct update sends the backend back in full (INCLUDING its definition — an OpenAPI spec here, which the
    # server validates cleanly). endpointConfig is a stringified JSON blob (escaped \/), so edit the endpoint URL
    # at text level using a slash-free segment ("customerservice") to avoid the blob's escaped slashes.
    When I replace "customerservice" with "customerservice_updated" in the payload "mcpBackendPayload"
    And I update backend "mcpBackendId" of MCP server "mcpId" with payload "mcpBackendPayload"
    Then The response status code should be 200
    When I retrieve backend "mcpBackendId" of MCP server "mcpId"
    Then The response status code should be 200
    And The response should contain "customerservice_updated"
    When I delete the MCP server "mcpId"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # CRUD: import+deploy an API, generate an MCP server from it, read, narrow the tools, delete.
  @cap:publisher @feat:mcp-servers @rule:api @type:regression @dep:publisher @legacy:MCPServerTestCase
  Scenario Outline: Full CRUD lifecycle of an API-generated MCP server as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I import openapi definition from "artifacts/payloads/OAS/mcp_petstore_oas3.json" with additional properties "artifacts/payloads/mcp_petstore_api_props.json" as "backingApiId"
    Then The response status code should be 201
    When I deploy the "apis" resource with id "backingApiId"
    When I create an MCP server from api "backingApiId" exposing paths "/pets,/pets/{petId}" as "mcpId"
    Then The response status code should be 201
    And The response should contain "get_pets"
    And The response should contain "get_pets_by_petId"
    When I retrieve the "mcp-servers" resource with id "mcpId"
    Then The response status code should be 200
    And The response should contain "get_pets"
    And The response should contain "get_pets_by_petId"
    # UPDATE (REMOVE) — narrow the exposed tools (least-privilege).
    When I update the MCP server "mcpId" removing tool "get_pets_by_petId"
    Then The response status code should be 200
    When I retrieve the "mcp-servers" resource with id "mcpId"
    Then The response should contain "get_pets"
    And The response should not contain "get_pets_by_petId"
    # UPDATE (ADD) — re-add the removed tool (inverse of remove); it comes back
    When I re-add the removed tool to the MCP server "mcpId"
    Then The response status code should be 200
    When I retrieve the "mcp-servers" resource with id "mcpId"
    Then The response should contain "get_pets_by_petId"
    When I delete the MCP server "mcpId"
    Then The response status code should be 200
    When I retrieve the "mcp-servers" resource with id "mcpId"
    Then The response status code should be 404

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
