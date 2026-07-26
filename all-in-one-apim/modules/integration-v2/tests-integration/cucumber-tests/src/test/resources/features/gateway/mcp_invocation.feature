@cleanup
Feature: MCP tool invocation through the gateway

  Gateway-plane invocation of MCP tools across all three creation types (proxy / from-OpenAPI / from-API): the
  full stateful MCP JSON-RPC handshake, scope enforcement (200 with scope / 403 without), and subscription
  throttling (429). Publisher-plane CRUD is in publisher/mcp_servers.feature. Needs the node MCP backend. ×2 tenant.

  # Invocation: publish + subscribe + the full stateful MCP handshake through the gateway.
  @cap:gateway @feat:mcp-invocation @rule:proxy @type:regression @dep:publisher @legacy:MCPServerTestCase
  Scenario Outline: Invoke a tool on a proxied MCP server through the gateway as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create an MCP server proxy to "http://nodebackend:3020/mcp" exposing tools "echo,add,get_pets" as "mcpId"
    Then The response status code should be 201
    When I deploy the "mcp-servers" resource with id "mcpId"
    When I publish the "mcp-servers" resource with id "mcpId"
    Then The response status code should be 200
    When I retrieve the "mcp-servers" resource with id "mcpId"
    And I extract response field "context" and store it as "mcpContext"
    When I have set up application with keys, subscribed to API "mcpId" with plan "Unlimited", and obtained access token for "mcpSubId"
    Then The response status code should be 200
    # Full MCP handshake through the gateway: initialize (session) → notifications/initialized → tools/list
    # (must advertise echo) → tools/call echo — the stateful round-trip to the real SDK-backed MCP server.
    When I invoke the MCP tool "echo" with arguments "{\"message\":\"hello mcp\"}" at gateway context "{{mcpContext}}" version "1.0.0" using access token "generatedAccessToken" expecting result containing "hello mcp" within 90 seconds
    # Value-add 1 — REAL tool execution (legacy asserted only canned echoes): args are forwarded and the real
    # result is computed/returned by the SDK server (add 2+3=5; get_pets returns actual pet data).
    When I invoke the MCP tool "add" with arguments "{\"a\":2,\"b\":3}" at gateway context "{{mcpContext}}" version "1.0.0" using access token "generatedAccessToken" expecting result containing "5" within 90 seconds
    When I invoke the MCP tool "get_pets" with arguments "{}" at gateway context "{{mcpContext}}" version "1.0.0" using access token "generatedAccessToken" expecting result containing "max" within 90 seconds
    # Value-add 2 — multi-call SESSION CONTINUITY: one initialize, then several tools/call on the SAME
    # Mcp-Session-Id (proves the gateway persists MCP session state across calls).
    When I invoke MCP tools in one session at gateway context "{{mcpContext}}" version "1.0.0" using access token "generatedAccessToken" with calls "echo|{\"message\":\"multi\"}|multi ; add|{\"a\":10,\"b\":20}|30" within 90 seconds
    # Value-add 3 — JSON-RPC error passthrough: a non-exposed tool yields an MCP error through the gateway.
    When I invoke the MCP tool "nosuchtool" with arguments "{}" at gateway context "{{mcpContext}}" version "1.0.0" using access token "generatedAccessToken" expecting an error within 90 seconds
    # Value-add 4 — negative auth at the gateway: an invalid token is rejected (401).
    When I invoke the MCP server at gateway context "{{mcpContext}}" version "1.0.0" with an invalid token expecting status 401 within 60 seconds
    When I delete the MCP server "mcpId"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Enforcement: a scope-gated MCP tool is refused (403) without the scope and allowed (200) with it.
  @cap:gateway @feat:mcp-invocation @rule:proxy @type:regression @dep:publisher @legacy:MCPServerTestCase
  Scenario Outline: A scope-gated MCP tool is enforced (200 with the scope, 403 without) as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create an MCP server proxy to "http://nodebackend:3020/mcp" exposing tools "echo" as "mcpId"
    Then The response status code should be 201
    # Gate the echo tool with a scope bound to the tenant admin role.
    When I gate the MCP server "mcpId" tool "echo" with scope "mcpScopeEnf" bound to role "admin"
    Then The response status code should be 200
    When I deploy the "mcp-servers" resource with id "mcpId"
    When I publish the "mcp-servers" resource with id "mcpId"
    Then The response status code should be 200
    When I retrieve the "mcp-servers" resource with id "mcpId"
    And I extract response field "context" and store it as "mcpContext"
    # Subscribe an app with client_credentials + password grants (password needed to mint a scoped user token).
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "mcpScopeAppPayload"
    And I create an application with payload "mcpScopeAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "mcpScopeKeysPayload"
      """
      {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
      """
    And I generate client credentials for application id "createdAppId" with payload "mcpScopeKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "mcpScopeSubPayload"
      """
      {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
      """
    And I subscribe to API "mcpId" using application "createdAppId" with payload "mcpScopeSubPayload" as "mcpScopeSubId"
    Then The response status code should be 201
    # A token WITH the scope calls the gated tool (200).
    When I request an OAuth access token for the current user using password grant with scope "mcpScopeEnf"
    Then The response status code should be 200
    When I invoke the MCP tool "echo" with arguments "{\"message\":\"scoped\"}" at gateway context "{{mcpContext}}" version "1.0.0" using access token "generatedAccessToken" expecting status 200 within 90 seconds
    # A token WITHOUT the scope is refused at the tool call (403).
    When I request an OAuth access token for the current user using password grant with scope "openid"
    Then The response status code should be 200
    When I invoke the MCP tool "echo" with arguments "{\"message\":\"scoped\"}" at gateway context "{{mcpContext}}" version "1.0.0" using access token "generatedAccessToken" expecting status 403 within 90 seconds
    When I delete the MCP server "mcpId"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Enforcement: a subscription bound to a low request-count policy is throttled (429) once it exceeds the limit.
  # Doc-advocated (auth+throttling+analytics on MCP servers) though the legacy left it disabled. Uses the robust
  # cumulative until-429 pattern within the minute window (each /mcp request counts toward the subscription quota).
  @cap:gateway @feat:mcp-invocation @rule:proxy @type:regression @dep:admin @dep:publisher @legacy:MCPServerTestCase
  Scenario Outline: A proxied MCP server subscription is throttled with 429 once it exceeds its limit as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    # A bespoke subscription policy allowing only 10 requests/min (reachable in a test; high enough that a few
    # full MCP handshakes succeed before the quota trips).
    When I create a subscription throttling policy "${UNIQUE:mcpSub10perMin}" allowing 10 requests per minute
    Then The response status code should be 201
    When I create an MCP server proxy to "http://nodebackend:3020/mcp" exposing tools "echo" as "mcpId"
    Then The response status code should be 201
    # The MCP server must OFFER the low tier for a subscription to use it.
    When I update the MCP server "mcpId" to offer policies "Unlimited,{{subThrottlePolicyName}}"
    Then The response status code should be 200
    When I deploy the "mcp-servers" resource with id "mcpId"
    When I publish the "mcp-servers" resource with id "mcpId"
    Then The response status code should be 200
    When I retrieve the "mcp-servers" resource with id "mcpId"
    And I extract response field "context" and store it as "mcpContext"
    # An application subscribed on the LOW subscription tier, keyed (password grant for a user token).
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "mcpThrottleAppPayload"
    And I create an application with payload "mcpThrottleAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "mcpThrottleKeysPayload"
      """
      {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
      """
    And I generate client credentials for application id "createdAppId" with payload "mcpThrottleKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "mcpThrottleSubPayload"
      """
      {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "{{subThrottlePolicyName}}"}
      """
    And I subscribe to API "mcpId" using application "createdAppId" with payload "mcpThrottleSubPayload" as "mcpThrottleSubId"
    Then The response status code should be 201
    When I request an OAuth access token for the current user using password grant with scope "PRODUCTION"
    Then The response status code should be 200
    # Drive past the 10/min subscription limit — the gateway must eventually refuse with 429 (cumulative retry).
    When I invoke the MCP tool "echo" with arguments "{\"message\":\"t\"}" at gateway context "{{mcpContext}}" version "1.0.0" using access token "generatedAccessToken" expecting status 429 within 90 seconds
    When I delete the MCP server "mcpId"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Invocation + value-adds: the gateway translates tools/call → HTTP to the REST backend and returns real data.
  @cap:gateway @feat:mcp-invocation @rule:openapi @type:regression @dep:publisher @legacy:MCPServerTestCase
  Scenario Outline: Invoke OpenAPI-generated MCP tools through the gateway (MCP to HTTP) as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create an MCP server from openapi "artifacts/payloads/OAS/mcp_petstore_oas3.json" with backend "http://nodebackend:3001/jaxrs_basic/services/customers/customerservice" as "mcpId"
    Then The response status code should be 201
    When I deploy the "mcp-servers" resource with id "mcpId"
    When I publish the "mcp-servers" resource with id "mcpId"
    Then The response status code should be 200
    When I retrieve the "mcp-servers" resource with id "mcpId"
    And I extract response field "context" and store it as "mcpContext"
    When I have set up application with keys, subscribed to API "mcpId" with plan "Unlimited", and obtained access token for "mcpSubId"
    Then The response status code should be 200
    # Value-add — real MCP↔HTTP: tools/call get_pets → gateway calls the REST backend → returns real pet data.
    When I invoke the MCP tool "get_pets" with arguments "{}" at gateway context "{{mcpContext}}" version "1.0.0" using access token "generatedAccessToken" expecting result containing "max" within 90 seconds
    # Value-add — path-param tool: get_pets_by_petId {petId:123} → gateway maps to GET /pets/123 on the backend.
    When I invoke the MCP tool "get_pets_by_petId" with arguments "{\"petId\":\"123\"}" at gateway context "{{mcpContext}}" version "1.0.0" using access token "generatedAccessToken" expecting result containing "max" within 90 seconds
    # Value-add — error passthrough + negative auth.
    When I invoke the MCP tool "nosuchtool" with arguments "{}" at gateway context "{{mcpContext}}" version "1.0.0" using access token "generatedAccessToken" expecting an error within 90 seconds
    # The DirectBackend (OpenAPI) subtype rejects an invalid token at tools/call with 403 (the proxy subtype
    # returns 401 — a verified per-subtype difference); asserted strictly so a future code change is caught.
    When I invoke the MCP server at gateway context "{{mcpContext}}" version "1.0.0" with an invalid token expecting status 403 within 60 seconds
    When I delete the MCP server "mcpId"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Enforcement: scope-gated tool invocation on the OpenAPI subtype (legacy tested this only for proxy/existing-api).
  @cap:gateway @feat:mcp-invocation @rule:openapi @type:regression @dep:publisher @legacy:MCPServerTestCase
  Scenario Outline: A scope-gated OpenAPI-generated MCP tool is enforced (200 with scope, 403 without) as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create an MCP server from openapi "artifacts/payloads/OAS/mcp_petstore_oas3.json" with backend "http://nodebackend:3001/jaxrs_basic/services/customers/customerservice" as "mcpId"
    Then The response status code should be 201
    When I gate the MCP server "mcpId" tool "get_pets" with scope "mcpOasScopeEnf" bound to role "admin"
    Then The response status code should be 200
    When I deploy the "mcp-servers" resource with id "mcpId"
    When I publish the "mcp-servers" resource with id "mcpId"
    Then The response status code should be 200
    When I retrieve the "mcp-servers" resource with id "mcpId"
    And I extract response field "context" and store it as "mcpContext"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "mcpOasAppPayload"
    And I create an application with payload "mcpOasAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "mcpOasKeysPayload"
      """
      {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
      """
    And I generate client credentials for application id "createdAppId" with payload "mcpOasKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "mcpOasSubPayload"
      """
      {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
      """
    And I subscribe to API "mcpId" using application "createdAppId" with payload "mcpOasSubPayload" as "mcpOasSubId"
    Then The response status code should be 201
    When I request an OAuth access token for the current user using password grant with scope "mcpOasScopeEnf"
    Then The response status code should be 200
    When I invoke the MCP tool "get_pets" with arguments "{}" at gateway context "{{mcpContext}}" version "1.0.0" using access token "generatedAccessToken" expecting status 200 within 90 seconds
    When I request an OAuth access token for the current user using password grant with scope "openid"
    Then The response status code should be 200
    When I invoke the MCP tool "get_pets" with arguments "{}" at gateway context "{{mcpContext}}" version "1.0.0" using access token "generatedAccessToken" expecting status 403 within 90 seconds
    When I delete the MCP server "mcpId"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Enforcement: subscription throttling on the OpenAPI subtype.
  @cap:gateway @feat:mcp-invocation @rule:openapi @type:regression @dep:admin @dep:publisher @legacy:MCPServerTestCase
  Scenario Outline: An OpenAPI-generated MCP subscription is throttled with 429 once it exceeds its limit as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create a subscription throttling policy "${UNIQUE:mcpOasSub10perMin}" allowing 10 requests per minute
    Then The response status code should be 201
    When I create an MCP server from openapi "artifacts/payloads/OAS/mcp_petstore_oas3.json" with backend "http://nodebackend:3001/jaxrs_basic/services/customers/customerservice" as "mcpId"
    Then The response status code should be 201
    When I update the MCP server "mcpId" to offer policies "Unlimited,{{subThrottlePolicyName}}"
    Then The response status code should be 200
    When I deploy the "mcp-servers" resource with id "mcpId"
    When I publish the "mcp-servers" resource with id "mcpId"
    Then The response status code should be 200
    When I retrieve the "mcp-servers" resource with id "mcpId"
    And I extract response field "context" and store it as "mcpContext"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "mcpOasThrAppPayload"
    And I create an application with payload "mcpOasThrAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "mcpOasThrKeysPayload"
      """
      {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
      """
    And I generate client credentials for application id "createdAppId" with payload "mcpOasThrKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "mcpOasThrSubPayload"
      """
      {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "{{subThrottlePolicyName}}"}
      """
    And I subscribe to API "mcpId" using application "createdAppId" with payload "mcpOasThrSubPayload" as "mcpOasThrSubId"
    Then The response status code should be 201
    When I request an OAuth access token for the current user using password grant with scope "PRODUCTION"
    Then The response status code should be 200
    When I invoke the MCP tool "get_pets" with arguments "{}" at gateway context "{{mcpContext}}" version "1.0.0" using access token "generatedAccessToken" expecting status 429 within 90 seconds
    When I delete the MCP server "mcpId"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Invocation + value-adds: the gateway routes tools/call through the underlying API to its backend.
  @cap:gateway @feat:mcp-invocation @rule:api @type:regression @dep:publisher @legacy:MCPServerTestCase
  Scenario Outline: Invoke API-generated MCP tools through the gateway as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I import openapi definition from "artifacts/payloads/OAS/mcp_petstore_oas3.json" with additional properties "artifacts/payloads/mcp_petstore_api_props.json" as "backingApiId"
    Then The response status code should be 201
    When I deploy the "apis" resource with id "backingApiId"
    When I create an MCP server from api "backingApiId" exposing paths "/pets,/pets/{petId}" as "mcpId"
    Then The response status code should be 201
    When I deploy the "mcp-servers" resource with id "mcpId"
    When I publish the "mcp-servers" resource with id "mcpId"
    Then The response status code should be 200
    When I retrieve the "mcp-servers" resource with id "mcpId"
    And I extract response field "context" and store it as "mcpContext"
    When I have set up application with keys, subscribed to API "mcpId" with plan "Unlimited", and obtained access token for "mcpSubId"
    Then The response status code should be 200
    # Value-add — real routing through the underlying API to its backend → real pet data.
    When I invoke the MCP tool "get_pets" with arguments "{}" at gateway context "{{mcpContext}}" version "1.0.0" using access token "generatedAccessToken" expecting result containing "max" within 90 seconds
    # Value-add — path-param tool routed to GET /pets/123 through the API.
    When I invoke the MCP tool "get_pets_by_petId" with arguments "{\"petId\":\"123\"}" at gateway context "{{mcpContext}}" version "1.0.0" using access token "generatedAccessToken" expecting result containing "max" within 90 seconds
    # Value-add — error passthrough + negative auth.
    When I invoke the MCP tool "nosuchtool" with arguments "{}" at gateway context "{{mcpContext}}" version "1.0.0" using access token "generatedAccessToken" expecting an error within 90 seconds
    When I invoke the MCP server at gateway context "{{mcpContext}}" version "1.0.0" with an invalid token expecting status 403 within 60 seconds
    When I delete the MCP server "mcpId"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Enforcement: scope-gated tool invocation on the API subtype.
  @cap:gateway @feat:mcp-invocation @rule:api @type:regression @dep:publisher @legacy:MCPServerTestCase
  Scenario Outline: A scope-gated API-generated MCP tool is enforced (200 with scope, 403 without) as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I import openapi definition from "artifacts/payloads/OAS/mcp_petstore_oas3.json" with additional properties "artifacts/payloads/mcp_petstore_api_props.json" as "backingApiId"
    Then The response status code should be 201
    When I deploy the "apis" resource with id "backingApiId"
    When I create an MCP server from api "backingApiId" exposing paths "/pets" as "mcpId"
    Then The response status code should be 201
    When I gate the MCP server "mcpId" tool "get_pets" with scope "mcpApiScopeEnf" bound to role "admin"
    Then The response status code should be 200
    When I deploy the "mcp-servers" resource with id "mcpId"
    When I publish the "mcp-servers" resource with id "mcpId"
    Then The response status code should be 200
    When I retrieve the "mcp-servers" resource with id "mcpId"
    And I extract response field "context" and store it as "mcpContext"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "mcpApiAppPayload"
    And I create an application with payload "mcpApiAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "mcpApiKeysPayload"
      """
      {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
      """
    And I generate client credentials for application id "createdAppId" with payload "mcpApiKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "mcpApiSubPayload"
      """
      {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
      """
    And I subscribe to API "mcpId" using application "createdAppId" with payload "mcpApiSubPayload" as "mcpApiSubId"
    Then The response status code should be 201
    When I request an OAuth access token for the current user using password grant with scope "mcpApiScopeEnf"
    Then The response status code should be 200
    When I invoke the MCP tool "get_pets" with arguments "{}" at gateway context "{{mcpContext}}" version "1.0.0" using access token "generatedAccessToken" expecting status 200 within 90 seconds
    When I request an OAuth access token for the current user using password grant with scope "openid"
    Then The response status code should be 200
    When I invoke the MCP tool "get_pets" with arguments "{}" at gateway context "{{mcpContext}}" version "1.0.0" using access token "generatedAccessToken" expecting status 403 within 90 seconds
    When I delete the MCP server "mcpId"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Enforcement: subscription throttling on the API subtype.
  @cap:gateway @feat:mcp-invocation @rule:api @type:regression @dep:admin @dep:publisher @legacy:MCPServerTestCase
  Scenario Outline: An API-generated MCP subscription is throttled with 429 once it exceeds its limit as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create a subscription throttling policy "${UNIQUE:mcpApiSub10perMin}" allowing 10 requests per minute
    Then The response status code should be 201
    When I import openapi definition from "artifacts/payloads/OAS/mcp_petstore_oas3.json" with additional properties "artifacts/payloads/mcp_petstore_api_props.json" as "backingApiId"
    Then The response status code should be 201
    When I deploy the "apis" resource with id "backingApiId"
    When I create an MCP server from api "backingApiId" exposing paths "/pets" as "mcpId"
    Then The response status code should be 201
    When I update the MCP server "mcpId" to offer policies "Unlimited,{{subThrottlePolicyName}}"
    Then The response status code should be 200
    When I deploy the "mcp-servers" resource with id "mcpId"
    When I publish the "mcp-servers" resource with id "mcpId"
    Then The response status code should be 200
    When I retrieve the "mcp-servers" resource with id "mcpId"
    And I extract response field "context" and store it as "mcpContext"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "mcpApiThrAppPayload"
    And I create an application with payload "mcpApiThrAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "mcpApiThrKeysPayload"
      """
      {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
      """
    And I generate client credentials for application id "createdAppId" with payload "mcpApiThrKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "mcpApiThrSubPayload"
      """
      {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "{{subThrottlePolicyName}}"}
      """
    And I subscribe to API "mcpId" using application "createdAppId" with payload "mcpApiThrSubPayload" as "mcpApiThrSubId"
    Then The response status code should be 201
    When I request an OAuth access token for the current user using password grant with scope "PRODUCTION"
    Then The response status code should be 200
    When I invoke the MCP tool "get_pets" with arguments "{}" at gateway context "{{mcpContext}}" version "1.0.0" using access token "generatedAccessToken" expecting status 429 within 90 seconds
    When I delete the MCP server "mcpId"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
