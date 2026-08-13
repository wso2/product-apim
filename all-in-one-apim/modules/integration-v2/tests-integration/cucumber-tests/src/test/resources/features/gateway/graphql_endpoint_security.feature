@cleanup
Feature: Gateway GraphQL Endpoint Security

  The gateway injects the backend credential (endpoint_security) when invoking a GraphQL API whose backend
  requires authentication. The node /graphql-secured endpoint returns 401 without `Authorization: Bearer
  graphql-secret`; the API's endpoint_security.production carries that credential and the gateway injects it, so
  the query returns 200. Documented endpoint security applied to GraphQL (the analogue of the AI secured-provider
  flow). Ports the backend-auth dimension of GraphqlTestCase. Single-tenant (super).

  @cap:gateway @feat:graphql-invocation @rule:endpoint-security @type:regression @dep:publisher @legacy:GraphqlTestCase
  Scenario: A GraphQL API's required backend credential is injected by the gateway
    Given The system is ready
    And I have valid access tokens as "admin"
    And I put JSON payload from file "artifacts/payloads/create_apim_graphql_secured_api.json" in context as "gqlSecPayload"
    And I create a GraphQL API with schema file "artifacts/payloads/graphql_schema.graphql" and additional properties "gqlSecPayload" as "gqlSecApiId"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "gqlSecApiId"
    And I put the response payload in context as "gqlSecFullPayload"
    And I extract response field "context" and store it as "gqlSecContext"
    # import-graphql-schema silently drops endpointConfig when endpoint_security is present, so the backend
    # credential cannot be set at create time — it must be applied via the endpoint-config UPDATE path.
    And I put the following JSON payload in context as "gqlSecEndpointConfig"
    """
    {"endpoint_type":"http","sandbox_endpoints":{"url":"http://nodebackend:3003/graphql-secured"},"production_endpoints":{"url":"http://nodebackend:3003/graphql-secured"},"endpoint_security":{"production":{"enabled":true,"type":"apikey","apiKeyIdentifier":"Authorization","apiKeyValue":"Bearer graphql-secret","apiKeyIdentifierType":"HEADER"},"sandbox":{"enabled":true,"type":"apikey","apiKeyIdentifier":"Authorization","apiKeyValue":"Bearer graphql-secret","apiKeyIdentifierType":"HEADER"}}}
    """
    And I update the "apis" resource "gqlSecApiId" and "gqlSecFullPayload" with configuration type "endpointConfig" and value:
    """
    gqlSecEndpointConfig
    """
    Then The response status code should be 200
    And I deploy the API with id "gqlSecApiId"
    When I publish the "apis" resource with id "gqlSecApiId"
    Then The lifecycle status of API "gqlSecApiId" should be "Published"
    # Deploy-readiness gate (self-healing): the JMS deploy event is at-most-once — if the gateway dropped
    # it, waiting alone can never succeed, so this re-deploys the revision after an exhausted window.
    And the "apis" resource "gqlSecApiId" should be live on the gateway, redeploying if propagation is lost
    When I have set up application with keys, subscribed to API "gqlSecApiId", and obtained access token for "gqlSecSubId"
    Then The response status code should be 200
    When I put the following JSON payload in context as "gqlSecQuery"
    """
    {"query": "{languages{code name}}"}
    """
    # Reaching 200 PROVES injection: /graphql-secured returns 401 without the Bearer credential the gateway injects
    And I invoke the API at gateway context "{{gqlSecContext}}/1.0.0" with method "POST" using access token "generatedAccessToken" and payload "gqlSecQuery" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
