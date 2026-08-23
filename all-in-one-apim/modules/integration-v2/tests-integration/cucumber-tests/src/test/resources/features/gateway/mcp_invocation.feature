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
    # The revisions LIST endpoint (never retrieved before): the revision the deploy step created is listed, and its
    # apiInfo.id ties it to THIS MCP server — an MCP server's revisions must not be reported against another id.
    When I retrieve the revisions of "mcp-servers" resource "mcpId"
    Then The response status code should be 200
    And The value of response field "count" should be "1"
    And The value of response field "list[0].apiInfo.id" should be "{{mcpId}}"
    And The value of response field "list[0].description" should be "new Revision"
    When I publish the "mcp-servers" resource with id "mcpId"
    Then The response status code should be 200
    When I retrieve the "mcp-servers" resource with id "mcpId"
    And I extract response field "context" and store it as "mcpContext"
    When I have set up application with keys, subscribed to API "mcpId" with plan "Unlimited", and obtained access token for "mcpSubId"
    Then The response status code should be 200
    # The PROXY subtype's initialize handshake is a PASSTHROUGH of the upstream MCP server's own serverInfo (the
    # node mock identifies itself as wso2-mock-mcp) with capabilities.tools empty — materially different from the
    # gateway-synthesised handshake the DirectBackend/ExistingApi subtypes answer with, so it is pinned separately.
    Then the MCP initialize handshake at gateway context "{{mcpContext}}" version "1.0.0" using access token "generatedAccessToken" should be exactly this result within 90 seconds:
      """
      {"protocolVersion":"2025-06-18","capabilities":{"tools":{}},"serverInfo":{"name":"wso2-mock-mcp","version":"1.0.0"}}
      """
    # Full MCP handshake through the gateway: initialize (session) → notifications/initialized → tools/list
    # (must advertise echo) → tools/call echo — the stateful round-trip to the real SDK-backed MCP server.
    When I invoke the MCP tool "echo" with arguments "{\"message\":\"hello mcp\"}" at gateway context "{{mcpContext}}" version "1.0.0" using access token "generatedAccessToken" expecting exact result text "hello mcp" within 90 seconds
    # …and the WHOLE JSON-RPC result, exactly: the proxy subtype relays the upstream MCP server's result verbatim,
    # so there is exactly ONE text content block and NO isError field (the DirectBackend subtype adds one).
    Then the MCP tool "echo" with arguments "{\"message\":\"hello mcp\"}" at gateway context "{{mcpContext}}" version "1.0.0" using access token "generatedAccessToken" should return exactly this result within 90 seconds:
      """
      {"content":[{"type":"text","text":"hello mcp"}]}
      """
    # Value-add 1 — REAL tool execution (legacy asserted only canned echoes): args are forwarded and the real
    # result is computed/returned by the SDK server (add 2+3=5; get_pets returns actual pet data).
    When I invoke the MCP tool "add" with arguments "{\"a\":2,\"b\":3}" at gateway context "{{mcpContext}}" version "1.0.0" using access token "generatedAccessToken" expecting exact result text "5" within 90 seconds
    When I invoke the MCP tool "get_pets" with arguments "{}" at gateway context "{{mcpContext}}" version "1.0.0" using access token "generatedAccessToken" expecting exact result text "[{\"id\":1,\"name\":\"max\"}]" within 90 seconds
    # Value-add 2 — multi-call SESSION CONTINUITY: one initialize, then several tools/call on the SAME
    # Mcp-Session-Id (proves the gateway persists MCP session state across calls).
    When I invoke MCP tools in one session at gateway context "{{mcpContext}}" version "1.0.0" using access token "generatedAccessToken" with calls "echo|{\"message\":\"multi\"}|multi ; add|{\"a\":10,\"b\":20}|30" within 90 seconds
    # Value-add 3 — JSON-RPC error passthrough: a non-exposed tool yields an MCP error through the gateway.
    When I invoke the MCP tool "nosuchtool" with arguments "{}" at gateway context "{{mcpContext}}" version "1.0.0" using access token "generatedAccessToken" expecting an error within 90 seconds
    # Value-add 4 — negative auth at the gateway: an invalid token is rejected (401).
    When I invoke the MCP server at gateway context "{{mcpContext}}" version "1.0.0" with an invalid token expecting status 401 within 60 seconds
    # Tool DELETE → redeploy → GATEWAY propagation for the PROXY subtype (the DirectBackend equivalent is below):
    # narrowing the exposed set drops the removed tool from what clients discover, the rest stay, and the removed
    # tool no longer resolves. Asserted as a SET, not an order: unlike the DirectBackend/ExistingApi subtypes (whose
    # tools/list follows the publisher's URL-mapping order), the proxy subtype's advertised ORDER matches neither the
    # submitted nor the persisted order (submitted echo,add,get_pets → publisher add,echo,get_pets → gateway
    # echo,get_pets,add), so there is no order to pin and asserting one would be flaky, not stricter.
    When I update the MCP server "mcpId" to expose tools "echo,add"
    Then The response status code should be 200
    When I deploy the "mcp-servers" resource with id "mcpId"
    When I list MCP tools at gateway context "{{mcpContext}}" version "1.0.0" using access token "generatedAccessToken" expecting tools "echo,add" and not "get_pets" within 90 seconds
    When I invoke the MCP tool "get_pets" with arguments "{}" at gateway context "{{mcpContext}}" version "1.0.0" using access token "generatedAccessToken" expecting an error within 90 seconds
    When I delete the MCP server "mcpId"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Proxying must keep the full tool definition (title/annotations/_meta/outputSchema), both in storage and
  # in the gateway tools/list.
  @cap:gateway @feat:mcp-invocation @rule:proxy @type:regression @dep:publisher @legacy:MCPServerTestCase
  Scenario Outline: A proxied MCP server preserves full tool metadata in tools/list as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create an MCP server proxy to "http://nodebackend:3020/mcp" exposing tools "get_weather" as "mcpId"
    Then The response status code should be 201
    When I deploy the "mcp-servers" resource with id "mcpId"
    When I publish the "mcp-servers" resource with id "mcpId"
    Then The response status code should be 200
    When I retrieve the "mcp-servers" resource with id "mcpId"
    And I extract response field "context" and store it as "mcpContext"
    # Persisted-side: the stored operation keeps the full metadata.
    Then the stored MCP server "mcpId" tool "get_weather" retains full metadata
    When I have set up application with keys, subscribed to API "mcpId" with plan "Unlimited", and obtained access token for "mcpSubId"
    Then The response status code should be 200
    # Gateway-side: tools/list advertises get_weather with the metadata intact.
    Then the MCP tools list at gateway context "{{mcpContext}}" version "1.0.0" using access token "generatedAccessToken" advertises tool "get_weather" with preserved metadata within 90 seconds
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
    When I invoke the MCP tool "echo" with arguments "{\"message\":\"scoped\"}" at gateway context "{{mcpContext}}" version "1.0.0" using access token "generatedAccessToken" expecting exact result text "scoped" within 90 seconds
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

  # Enforcement: the OTHER two throttling levels an MCP server has, plus the half that proves causation.
  # The scenarios above throttle via the SUBSCRIPTION tier; this one covers the API (server) level and the
  # OPERATION (tool) level advanced policies, and in between reverts the server to Unlimited and requires a burst
  # well past the old limit to pass cleanly — without that half, a 429 could just as well have come from an ambient
  # rate limit and the test would still be green. The subscription is deliberately Unlimited throughout, so the
  # only thing that can throttle is the policy under test. Ports testThrottlingForProxySubtype, whose @Test was
  # commented out in the legacy suite and therefore never actually ran. Both organizations exercise the
  # independently scoped API and operation policies despite the minute-granular throttle windows.
  @cap:gateway @feat:mcp-invocation @rule:proxy @type:regression @dep:admin @dep:publisher @legacy:MCPServerTestCase
  Scenario Outline: A proxied MCP server is throttled by an API-level policy, unthrottled once reverted, then throttled by an operation-level policy as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create an advanced throttling policy "${UNIQUE:mcpAdv5PerMin}" allowing 5 requests per minute
    Then The response status code should be 201
    When I create an MCP server proxy to "http://nodebackend:3020/mcp" exposing tools "echo" as "mcpId"
    Then The response status code should be 201
    # API (server) LEVEL — the policy caps the whole MCP server irrespective of the subscription tier.
    When I update the MCP server "mcpId" to use API-level throttling policy "{{advThrottlePolicyName}}"
    Then The response status code should be 200
    And The value of response field "throttlingPolicy" should be "{{advThrottlePolicyName}}"
    When I deploy the "mcp-servers" resource with id "mcpId"
    When I publish the "mcp-servers" resource with id "mcpId"
    Then The response status code should be 200
    When I retrieve the "mcp-servers" resource with id "mcpId"
    And I extract response field "context" and store it as "mcpContext"
    When I have set up application with keys, subscribed to API "mcpId" with plan "Unlimited", and obtained access token for "mcpSubId"
    Then The response status code should be 200
    When I invoke the MCP tool "echo" with arguments "{\"message\":\"t\"}" at gateway context "{{mcpContext}}" version "1.0.0" using access token "generatedAccessToken" expecting status 429 within 120 seconds
    # REVERT — back to Unlimited at the server level; a burst well past the old 5/min limit must now pass cleanly.
    When I update the MCP server "mcpId" to use API-level throttling policy "Unlimited"
    Then The response status code should be 200
    And The value of response field "throttlingPolicy" should be "Unlimited"
    When I deploy the "mcp-servers" resource with id "mcpId"
    Then the MCP tool "echo" with arguments "{\"message\":\"t\"}" at gateway context "{{mcpContext}}" version "1.0.0" using access token "generatedAccessToken" should return status 200 for 8 consecutive calls within 180 seconds
    # OPERATION (tool) LEVEL — the same policy bound to the echo tool alone, with the server level CLEARED, so a
    # 429 here can only be the operation's own policy. The server level must be null rather than "Unlimited":
    # a non-null server-level policy overwrites every operation's policy with it (verified live), which is why
    # the legacy set it to null before binding the operation policy.
    When I update the MCP server "mcpId" setting tool "echo" throttling policy "{{advThrottlePolicyName}}"
    Then The response status code should be 200
    And The response should contain "\"throttlingPolicy\":null"
    And The value of response field "operations[0].throttlingPolicy" should be "{{advThrottlePolicyName}}"
    When I deploy the "mcp-servers" resource with id "mcpId"
    When I invoke the MCP tool "echo" with arguments "{\"message\":\"t\"}" at gateway context "{{mcpContext}}" version "1.0.0" using access token "generatedAccessToken" expecting status 429 within 120 seconds
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
    # The revisions LIST endpoint: the created revision is listed against THIS MCP server, with the description sent.
    When I retrieve the revisions of "mcp-servers" resource "mcpId"
    Then The response status code should be 200
    And The value of response field "count" should be "1"
    And The value of response field "list[0].apiInfo.id" should be "{{mcpId}}"
    And The value of response field "list[0].description" should be "new Revision"
    When I publish the "mcp-servers" resource with id "mcpId"
    Then The response status code should be 200
    And The value of response field "lifecycleState.state" should be "Published"
    When I retrieve the "mcp-servers" resource with id "mcpId"
    And I extract response field "context" and store it as "mcpContext"
    And I extract response field "name" and store it as "mcpName"
    # A published MCP server must be DISCOVERABLE by a consumer — it has its own devportal collection
    # (/mcp-servers; it is absent from /apis), and its devportal representation must agree with the publisher DTO.
    Then The devportal should report MCP server "mcpId" exactly once with the same fields within 120 seconds
    When I have set up application with keys, subscribed to API "mcpId" with plan "Unlimited", and obtained access token for "mcpSubId"
    Then The response status code should be 200
    # The DirectBackend subtype's handshake is SYNTHESISED by the gateway from the MCP server's own metadata (its
    # name/version + a fixed description), unlike the proxy subtype's upstream passthrough. Pinned exactly — a
    # contains-check is precisely what would sail past a wrong protocolVersion.
    Then the MCP initialize handshake at gateway context "{{mcpContext}}" version "1.0.0" using access token "generatedAccessToken" should be exactly this result within 90 seconds:
      """
      {"protocolVersion":"2025-06-18","capabilities":{"tools":{"listChanged":false}},
       "serverInfo":{"name":"{{mcpName}}","version":"1.0.0","description":"This is an MCP Server"}}
      """
    # Value-add — real MCP↔HTTP: tools/call get_pets → gateway calls the REST backend → returns real pet data.
    When I invoke the MCP tool "get_pets" with arguments "{}" at gateway context "{{mcpContext}}" version "1.0.0" using access token "generatedAccessToken" expecting exact result text "[{\"id\":1,\"name\":\"max\"}]" within 90 seconds
    # …and the WHOLE result, exactly: one text content block carrying the backend's body verbatim, isError false.
    Then the MCP tool "get_pets" with arguments "{}" at gateway context "{{mcpContext}}" version "1.0.0" using access token "generatedAccessToken" should return exactly this result within 90 seconds:
      """
      {"content":[{"type":"text","text":"[{\"id\":1,\"name\":\"max\"}]"}],"isError":false}
      """
    # Value-add — path-param tool: get_pets_by_petId {petId:123} → gateway maps to GET /pets/123 on the backend.
    When I invoke the MCP tool "get_pets_by_petId" with arguments "{\"petId\":\"123\"}" at gateway context "{{mcpContext}}" version "1.0.0" using access token "generatedAccessToken" expecting exact result text "{\"name\":\"max\"}" within 90 seconds
    # Exactly: the single pet, so the path argument really reached the backend as /pets/123 (a whole-list result
    # would satisfy a contains-"max" check while proving the path param was dropped).
    Then the MCP tool "get_pets_by_petId" with arguments "{\"petId\":\"123\"}" at gateway context "{{mcpContext}}" version "1.0.0" using access token "generatedAccessToken" should return exactly this result within 90 seconds:
      """
      {"content":[{"type":"text","text":"{\"name\":\"max\"}"}],"isError":false}
      """
    # Value-add — error passthrough + negative auth.
    When I invoke the MCP tool "nosuchtool" with arguments "{}" at gateway context "{{mcpContext}}" version "1.0.0" using access token "generatedAccessToken" expecting an error within 90 seconds
    # The DirectBackend (OpenAPI) subtype rejects an invalid token at tools/call with 403 (the proxy subtype
    # returns 401 — a verified per-subtype difference); asserted strictly so a future code change is caught.
    When I invoke the MCP server at gateway context "{{mcpContext}}" version "1.0.0" with an invalid token expecting status 403 within 60 seconds
    # Tool-update → redeploy → GATEWAY propagation (ports the legacy tool-operations arc; assertions hardened
    # per upstream PR #14237: tools/list names parsed and compared as a SET, order-independent): removing a
    # tool and deploying a new revision drops it from the gateway's advertised list (the rest stay), and
    # calling the removed tool now errors.
    When I update the MCP server "mcpId" removing tool "get_pets_by_petId"
    Then The response status code should be 200
    When I deploy the "mcp-servers" resource with id "mcpId"
    When I list MCP tools at gateway context "{{mcpContext}}" version "1.0.0" using access token "generatedAccessToken" expecting tools "get_pets" and not "get_pets_by_petId" within 90 seconds
    When I invoke the MCP tool "get_pets_by_petId" with arguments "{\"petId\":\"123\"}" at gateway context "{{mcpContext}}" version "1.0.0" using access token "generatedAccessToken" expecting an error within 90 seconds
    # Tool ORDER as clients discover it, for the DirectBackend subtype too (it was pinned only for ExistingApi):
    # a tool-replacing update submits [new delete_oldpets, kept get_pets], and the gateway serves the deployed
    # revision's URL mappings ordered by mapping id — i.e. the submitted order. This is the same expectation the
    # legacy held for BOTH backend-mapped subtypes (its EXPECTED_UPDATED_TOOL_LIST_RESPONSE was shared), and it is
    # a real guarantee here, unlike the proxy subtype whose advertised order matches no submitted order.
    When I update the MCP server "mcpId" replacing its tools with "DELETE /oldpets" then "get_pets" re-described as "Return a list of pets"
    Then The response status code should be 200
    And the MCP server operations should be exactly "delete_oldpets,get_pets" in that order
    When I deploy the "mcp-servers" resource with id "mcpId"
    Then the MCP server should advertise tools in order "delete_oldpets,get_pets" at gateway context "{{mcpContext}}" version "1.0.0" using access token "generatedAccessToken" within 90 seconds
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
    When I invoke the MCP tool "get_pets" with arguments "{}" at gateway context "{{mcpContext}}" version "1.0.0" using access token "generatedAccessToken" expecting exact result text "[{\"id\":1,\"name\":\"max\"}]" within 90 seconds
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
    # The revisions LIST endpoint: the created revision is listed against THIS MCP server (not the backing API).
    When I retrieve the revisions of "mcp-servers" resource "mcpId"
    Then The response status code should be 200
    And The value of response field "count" should be "1"
    And The value of response field "list[0].apiInfo.id" should be "{{mcpId}}"
    And The value of response field "list[0].description" should be "new Revision"
    When I publish the "mcp-servers" resource with id "mcpId"
    Then The response status code should be 200
    When I retrieve the "mcp-servers" resource with id "mcpId"
    And I extract response field "context" and store it as "mcpContext"
    When I have set up application with keys, subscribed to API "mcpId" with plan "Unlimited", and obtained access token for "mcpSubId"
    Then The response status code should be 200
    # Value-add — real routing through the underlying API to its backend → real pet data.
    When I invoke the MCP tool "get_pets" with arguments "{}" at gateway context "{{mcpContext}}" version "1.0.0" using access token "generatedAccessToken" expecting exact result text "[{\"id\":1,\"name\":\"max\"}]" within 90 seconds
    # Value-add — path-param tool routed to GET /pets/123 through the API.
    When I invoke the MCP tool "get_pets_by_petId" with arguments "{\"petId\":\"123\"}" at gateway context "{{mcpContext}}" version "1.0.0" using access token "generatedAccessToken" expecting exact result text "{\"name\":\"max\"}" within 90 seconds
    # Value-add — error passthrough + negative auth.
    When I invoke the MCP tool "nosuchtool" with arguments "{}" at gateway context "{{mcpContext}}" version "1.0.0" using access token "generatedAccessToken" expecting an error within 90 seconds
    When I invoke the MCP server at gateway context "{{mcpContext}}" version "1.0.0" with an invalid token expecting status 403 within 60 seconds
    # Tool ORDER as clients discover it, for the ExistingApi subtype (see the OpenAPI scenario above): the new tool
    # references the backing API's DELETE /oldpets resource, and tools/list must advertise the submitted order.
    When I update the MCP server "mcpId" replacing its tools with "DELETE /oldpets" then "get_pets" re-described as "Return a list of pets"
    Then The response status code should be 200
    And the MCP server operations should be exactly "delete_oldpets,get_pets" in that order
    When I deploy the "mcp-servers" resource with id "mcpId"
    Then the MCP server should advertise tools in order "delete_oldpets,get_pets" at gateway context "{{mcpContext}}" version "1.0.0" using access token "generatedAccessToken" within 90 seconds
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
    When I invoke the MCP tool "get_pets" with arguments "{}" at gateway context "{{mcpContext}}" version "1.0.0" using access token "generatedAccessToken" expecting exact result text "[{\"id\":1,\"name\":\"max\"}]" within 90 seconds
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
