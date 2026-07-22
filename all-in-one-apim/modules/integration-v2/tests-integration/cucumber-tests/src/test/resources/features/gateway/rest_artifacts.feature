@cleanup
Feature: Gateway REST Artifact Retrieval

  Gateway-plane retrieval of a deployed API's synapse artifacts via the gateway internal REST API
  (api/am/gateway/v2): the API artifact, its endpoints, its local entry and its mediation sequences. Ports
  GatewayRestAPITestCase. An addHeader operation policy is attached to the request, response and fault flows so the
  deployed API carries three mediation sequences; the gateway REST API then surfaces the artifact (carrying the API
  name and id), the production+sandbox endpoints, the local entry (carrying the API id) and the three sequences
  (each carrying the injected header name). Pinned live: this gateway REST API authenticates with BASIC admin
  credentials (a Bearer token is rejected 401). Runs in the gateway block (backend up); admin actor in both
  tenants. Torn down by the cleanup hook.

  @cap:gateway @feat:rest-invocation @rule:gateway-artifacts @type:regression @dep:publisher @legacy:GatewayRestAPITestCase
  Scenario Outline: A deployed API's synapse artifacts are retrievable via the gateway REST API in <tenant>
    Given The system is ready
    And I have valid access tokens as "admin<suffix>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "gwArtApiPayload"
    And I create an "apis" resource with payload "gwArtApiPayload" as "gwArtApiId"
    Then The response status code should be 201
    And I extract response field "name" and store it as "gwArtApiName"

    # Attach an addHeader operation policy to the request, response and fault flows so all three mediation
    # sequences are generated for the deployed API.
    When I attach the common operation policy "addHeader" to operation 0 of API "gwArtApiId" in flows "request,response,fault" with parameters "{\"headerName\":\"GatewayArtifactTestHeader\",\"headerValue\":\"GatewayArtifactTestValue\"}"
    Then The response status code should be 200
    When I publish the "apis" resource with id "gwArtApiId"
    Then The lifecycle status of API "gwArtApiId" should be "Published"
    And I deploy the API with id "gwArtApiId"
    Then The response status code should be 201

    # The API artifact carries the API name and id (poll until the gateway has materialised the artifact).
    When I retrieve the gateway "api-artifact" for API "{{gwArtApiName}}" version "1.0.0" in tenant "<tenant>" until it is available within 60 seconds
    Then The response status code should be 200
    And The response should contain "{{gwArtApiName}}"
    And The response should contain "{{gwArtApiId}}"
    # The endpoints artifact carries the production and sandbox endpoints.
    When I retrieve the gateway "end-points" for API "{{gwArtApiName}}" version "1.0.0" in tenant "<tenant>"
    Then The response status code should be 200
    And The response should contain "production"
    And The response should contain "sandbox"
    # The local entry carries the API id.
    When I retrieve the gateway "local-entry" for API "{{gwArtApiName}}" version "1.0.0" in tenant "<tenant>"
    Then The response status code should be 200
    And The response should contain "{{gwArtApiId}}"
    # The sequences carry the injected header name (request/response/fault mediation).
    When I retrieve the gateway "sequence" for API "{{gwArtApiName}}" version "1.0.0" in tenant "<tenant>"
    Then The response status code should be 200
    And The response should contain "GatewayArtifactTestHeader"

    Examples:
      | tenant       | suffix       |
      | carbon.super |              |
      | tenant1.com  | @tenant1.com |
