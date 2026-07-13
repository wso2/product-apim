@cleanup
Feature: Gateway Multi-Protocol Invocation Across Restart

  A REST, a GraphQL and a WebSocket API are each deployed, published, subscribed and invoked; then the APIM server
  is gracefully restarted ONCE; then all three are invoked again and must still work — proving deployments and
  routing of every protocol survive a restart (schema/definition + gateway wiring re-loaded). GraphQL restart is
  legacy parity (GraphqlServerRestartTestCase); WebSocket restart is beyond legacy (no legacy WS-restart test).
  Runs in the IntegrationV2-ServerRestart block (overlay enables `[server] enable_restart_from_api`, initBackend
  starts the node backends); thread-count=1 because the restart bounces the shared server. Single-tenant (super):
  restart survival is server-wide and each restart is expensive. One shared restart covers all three protocols.

  @cap:gateway @feat:rest-invocation @rule:multi-protocol-restart @type:regression @dep:publisher @legacy:GraphqlServerRestartTestCase
  Scenario: REST, GraphQL and WebSocket APIs all keep working across a graceful server restart
    Given The system is ready
    And I have valid access tokens as "admin"

    # --- REST API: deploy, subscribe, invoke, preserve its token/context ---
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "restApiId" and deployed it
    When I publish the "apis" resource with id "restApiId"
    Then The lifecycle status of API "restApiId" should be "Published"
    When I retrieve the "apis" resource with id "restApiId"
    And I extract response field "context" and store it as "restContext"
    When I have set up application with keys, subscribed to API "restApiId", and obtained access token for "restSubId"
    Then The response status code should be 200
    And I copy context value "generatedAccessToken" to "restToken"
    And I invoke the API at gateway context "{{restContext}}/1.0.0/customers/123/" with method "GET" using access token "restToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    # --- GraphQL API: deploy, subscribe, invoke a query, preserve its token/context ---
    When I put JSON payload from file "artifacts/payloads/create_apim_test_graphql_api.json" in context as "gqlRsPayload"
    And I create a GraphQL API with schema file "artifacts/payloads/graphql_schema.graphql" and additional properties "gqlRsPayload" as "gqlRsApiId"
    Then The response status code should be 201
    And I deploy the API with id "gqlRsApiId"
    When I publish the "apis" resource with id "gqlRsApiId"
    Then The lifecycle status of API "gqlRsApiId" should be "Published"
    When I retrieve the "apis" resource with id "gqlRsApiId"
    And I extract response field "context" and store it as "gqlRsContext"
    When I have set up application with keys, subscribed to API "gqlRsApiId", and obtained access token for "gqlRsSubId"
    Then The response status code should be 200
    And I copy context value "generatedAccessToken" to "gqlToken"
    When I put the following JSON payload in context as "gqlRsQuery"
    """
    {"query": "{languages{code name}}"}
    """
    And I invoke the API at gateway context "{{gqlRsContext}}/1.0.0" with method "POST" using access token "gqlToken" and payload "gqlRsQuery" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    # --- WebSocket API: deploy, subscribe (async), invoke echo, preserve its token/context ---
    And I have created an api from "artifacts/payloads/create_apim_ws_echo_api.json" as "wsRsApiId" and deployed it
    When I publish the "apis" resource with id "wsRsApiId"
    Then The lifecycle status of API "wsRsApiId" should be "Published"
    When I retrieve the "apis" resource with id "wsRsApiId"
    And I extract response field "context" and store it as "wsRsContext"
    When I have set up application with keys, subscribed to API "wsRsApiId" with plan "AsyncUnlimited", and obtained access token for "wsRsSubId"
    Then The response status code should be 200
    And I copy context value "generatedAccessToken" to "wsToken"
    And I invoke the WebSocket API at gateway ws context "{{wsRsContext}}/1.0.0" with message "hello ws" using access token "wsToken" expecting echo "HELLO WS" within 60 seconds

    # --- One graceful restart — then every protocol must still work ---
    When I gracefully restart the API Manager server
    And I invoke the API at gateway context "{{restContext}}/1.0.0/customers/123/" with method "GET" using access token "restToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{gqlRsContext}}/1.0.0" with method "POST" using access token "gqlToken" and payload "gqlRsQuery" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the WebSocket API at gateway ws context "{{wsRsContext}}/1.0.0" with message "hello ws" using access token "wsToken" expecting echo "HELLO WS" within 60 seconds
