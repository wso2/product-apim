@cleanup
Feature: MCP Server authoring (publisher plane)

  Publisher-plane CRUD of MCP servers across all three creation types — proxy (to a third-party MCP server),
  from-OpenAPI (DirectBackend), and from-API (ExistingApi) — plus backend-endpoint management. Gateway
  invocation is in gateway/mcp_invocation.feature. Needs the node MCP backend (proxy create-validation). ×2 tenant.

  # CRUD: create exposing a subset of the backend's tools, read them back, update the exposed set, delete.
  @cap:publisher @feat:mcp-servers @rule:proxy @type:regression @legacy:MCPServerTestCase
  Scenario Outline: Full CRUD lifecycle of a proxied MCP server as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
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
    # The exposed set is now the backend's FULL advertised tool set — exactly echo, add and get_pets and nothing
    # else. So exposing only two of them (above, and in the invocation feature's throttle/scope scenarios) is this
    # suite's deliberate least-privilege choice, NOT a product limit on how many discovered tools can be imported.
    # Note the ORDER: the proxy subtype returns its operations sorted by tool name, not in the order submitted
    # ("echo,add,get_pets" in → add,echo,get_pets out) — unlike the backend-mapped subtypes, which preserve
    # submission order (see the two ordering scenarios below).
    And the MCP server operations should be exactly "add,echo,get_pets" in that order
    # READ-BACK — the add persisted
    When I retrieve the "mcp-servers" resource with id "mcpId"
    Then The response should contain "get_pets"
    # Tool FIDELITY for a PROXIED tool: the discovered schema and description must be stored VERBATIM from the
    # upstream MCP server's tools/list (nothing re-derived, nothing dropped) — a proxy that mangled a tool's input
    # schema would advertise a contract its own backend rejects, and the presence checks above would not notice.
    Then the MCP server "mcpId" tool "echo" should have schema definition:
      """
      {"type":"object","properties":{"message":{"type":"string"}},"required":["message"]}
      """
    And the MCP server "mcpId" tool "echo" should have description "Echoes the provided message"
    And the MCP server "mcpId" tool "add" should have schema definition:
      """
      {"type":"object","properties":{"a":{"type":"number"},"b":{"type":"number"}},"required":["a","b"]}
      """
    And the MCP server "mcpId" tool "add" should have description "Adds two numbers"
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
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # Backend-endpoint management for a PROXY MCP server. A correct update PUTs the backend back in full, INCLUDING
  # its `definition` (MCP-tools JSON, not an OpenAPI spec). carbon-apimgt < 9.33.147 wrongly re-validated that
  # definition as OpenAPI on update, failing a correct PUT with 900754 "Error while parsing OpenAPI definition —
  # attribute tools is unexpected" (HTTP 400) — a product regression. It is fixed in carbon-apimgt 9.33.147, the
  # version this branch now builds, so the scenario is enabled. Do NOT work around any recurrence by stripping the
  # definition (that would hide a regression).
  @cap:publisher @feat:mcp-servers @rule:proxy @type:regression @legacy:MCPServerTestCase
  Scenario Outline: Manage the backend endpoint of a proxied MCP server as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
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
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # CRUD: create from OAS (both tools generated), read, narrow to a subset (remove a tool), delete.
  @cap:publisher @feat:mcp-servers @rule:openapi @type:regression @legacy:MCPServerTestCase
  Scenario Outline: Full CRUD lifecycle of an OpenAPI-generated MCP server as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I create an MCP server from openapi "artifacts/payloads/OAS/mcp_petstore_oas3.json" with backend "http://nodebackend:3001/jaxrs_basic/services/customers/customerservice" as "mcpId"
    Then The response status code should be 201
    And The response should contain "get_pets"
    And The response should contain "get_pets_by_petId"
    # The generated tools come back in the order their operations were submitted (/pets then /pets/{petId}),
    # and ONLY those — the OAS also defines DELETE /oldpets, which was not selected.
    And the MCP server operations should be exactly "get_pets,get_pets_by_petId" in that order
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
    # STRUCTURAL schema check (order-independent, ports the legacy schema assertion as hardened by upstream
    # PR #14237 — exact-string JSON compare flaked on unguaranteed key order): the path-param tool's
    # PRODUCT-GENERATED input schema pins the OAS→MCP schema derivation. Pinned actual behaviour: the
    # generator prefixes each parameter with its OAS location ("path_petId" for the in:path param) and emits
    # a bare draft-agnostic object schema (no $schema / additionalProperties envelope).
    Then the MCP server "mcpId" tool "get_pets_by_petId" should have schema definition:
      """
      {"type":"object","properties":{"path_petId":{"type":"string","description":"The id of the pet to retrieve"}},"required":["path_petId"]}
      """
    When I delete the MCP server "mcpId"
    Then The response status code should be 200
    When I retrieve the "mcp-servers" resource with id "mcpId"
    Then The response status code should be 404

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # Backend-endpoint management: an OpenAPI-generated MCP server has its OWN backend (the REST endpoint the
  # generated tools call). List it, get it by id, update its URL, and read the update back. (list/get/update
  # only — the backend is created implicitly with the server and has no separate add/delete.)
  @cap:publisher @feat:mcp-servers @rule:openapi @type:regression @legacy:MCPServerTestCase
  Scenario Outline: Manage the backend endpoint of an OpenAPI-generated MCP server as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
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
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # CRUD: import+deploy an API, generate an MCP server from it, read, narrow the tools, delete.
  @cap:publisher @feat:mcp-servers @rule:api @type:regression @legacy:MCPServerTestCase
  Scenario Outline: Full CRUD lifecycle of an API-generated MCP server as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I import openapi definition from "artifacts/payloads/OAS/mcp_petstore_oas3.json" with additional properties "artifacts/payloads/mcp_petstore_api_props.json" as "backingApiId"
    Then The response status code should be 201
    When I deploy the "apis" resource with id "backingApiId"
    When I create an MCP server from api "backingApiId" exposing paths "/pets,/pets/{petId}" as "mcpId"
    Then The response status code should be 201
    And The response should contain "get_pets"
    And The response should contain "get_pets_by_petId"
    # Same submission-order guarantee as the OpenAPI flow, over the API's resources this time.
    And the MCP server operations should be exactly "get_pets,get_pets_by_petId" in that order
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
    # Tool FIDELITY for the API-generated subtype (it was pinned only for the OpenAPI one): generating tools from an
    # existing API's resources must derive the SAME schema and description as generating them from the OAS directly —
    # same parameter-location prefix ("path_petId"), same bare object schema, same description off the OAS summary.
    # Without this, a subtype that silently dropped the parameter or mislabelled the tool would still pass.
    Then the MCP server "mcpId" tool "get_pets_by_petId" should have schema definition:
      """
      {"type":"object","properties":{"path_petId":{"type":"string","description":"The id of the pet to retrieve"}},"required":["path_petId"]}
      """
    And the MCP server "mcpId" tool "get_pets_by_petId" should have description "Get a pet by ID"
    And the MCP server "mcpId" tool "get_pets" should have description "Get a list of pets"
    When I delete the MCP server "mcpId"
    Then The response status code should be 200
    When I retrieve the "mcp-servers" resource with id "mcpId"
    Then The response status code should be 404

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # The order operations are SUBMITTED in is the order the publisher stores and returns them — the URL mappings are
  # (re)inserted in submission order and read back by mapping id. So an update that submits [new delete_oldpets,
  # kept get_pets] must come back in exactly that order, NOT in the order the tools were originally created in.
  @cap:publisher @feat:mcp-servers @rule:openapi @type:regression @legacy:MCPServerTestCase
  Scenario Outline: Submitted tool order is preserved for an OpenAPI-generated MCP server as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I create an MCP server from openapi "artifacts/payloads/OAS/mcp_petstore_oas3.json" with backend "http://nodebackend:3001/jaxrs_basic/services/customers/customerservice" as "mcpId"
    Then The response status code should be 201
    And the MCP server operations should be exactly "get_pets,get_pets_by_petId" in that order
    # Replace the tool set with [new DELETE /oldpets, kept get_pets] — get_pets_by_petId is dropped by the update.
    When I update the MCP server "mcpId" replacing its tools with "DELETE /oldpets" then "get_pets" re-described as "Return a list of pets"
    Then The response status code should be 200
    And the MCP server operations should be exactly "delete_oldpets,get_pets" in that order
    # The new tool's description is derived from the backend definition; the kept tool carries the one just sent.
    And The response should contain "Delete all old pets"
    And The response should contain "Return a list of pets"
    # The order is persisted, not merely echoed by the update response.
    When I retrieve the "mcp-servers" resource with id "mcpId"
    Then The response status code should be 200
    And the MCP server operations should be exactly "delete_oldpets,get_pets" in that order
    # A tool ADDED by an update is generated with the SAME fidelity as one generated at create: the new DELETE-verb
    # tool gets the backend definition's own description and a parameterless object schema. The bare
    # contains-"Delete all old pets" checks above cannot tell that description apart from one that landed on the
    # WRONG operation, and say nothing at all about the generated schema.
    Then the MCP server "mcpId" tool "delete_oldpets" should have schema definition:
      """
      {"type":"object","properties":{}}
      """
    And the MCP server "mcpId" tool "delete_oldpets" should have description "Delete all old pets"
    # The kept tool carries the description the update SENT, not the one the OAS derived at create.
    And the MCP server "mcpId" tool "get_pets" should have description "Return a list of pets"
    When I delete the MCP server "mcpId"
    Then The response status code should be 200

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # The same ordering guarantee for the ExistingApi subtype, where each tool maps to a resource of the backing API
  # (so the new tool references the API's DELETE /oldpets resource rather than a backend path).
  @cap:publisher @feat:mcp-servers @rule:api @type:regression @legacy:MCPServerTestCase
  Scenario Outline: Submitted tool order is preserved for an API-generated MCP server as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I import openapi definition from "artifacts/payloads/OAS/mcp_petstore_oas3.json" with additional properties "artifacts/payloads/mcp_petstore_api_props.json" as "backingApiId"
    Then The response status code should be 201
    When I deploy the "apis" resource with id "backingApiId"
    When I create an MCP server from api "backingApiId" exposing paths "/pets,/pets/{petId}" as "mcpId"
    Then The response status code should be 201
    And the MCP server operations should be exactly "get_pets,get_pets_by_petId" in that order
    When I update the MCP server "mcpId" replacing its tools with "DELETE /oldpets" then "get_pets" re-described as "Return a list of pets"
    Then The response status code should be 200
    And the MCP server operations should be exactly "delete_oldpets,get_pets" in that order
    And The response should contain "Delete all old pets"
    And The response should contain "Return a list of pets"
    When I retrieve the "mcp-servers" resource with id "mcpId"
    Then The response status code should be 200
    And the MCP server operations should be exactly "delete_oldpets,get_pets" in that order
    When I delete the MCP server "mcpId"
    Then The response status code should be 200

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |
