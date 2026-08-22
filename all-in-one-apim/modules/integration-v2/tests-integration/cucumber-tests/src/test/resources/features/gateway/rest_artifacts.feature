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
    # sequences are generated for the deployed API — with a DISTINCT header per flow. One shared header name would
    # make the per-flow assertions below indistinguishable: any single generated sequence would satisfy all three,
    # so a missing Out or Fault sequence would still pass. The attach step merges into the API's existing
    # operationPolicies, so three successive single-flow calls leave all three flows populated.
    When I attach the common operation policy "addHeader" to operation 0 of API "gwArtApiId" in flows "request" with parameters "{\"headerName\":\"GwArtRequestHeader\",\"headerValue\":\"GwArtRequestValue\"}"
    Then The response status code should be 200
    When I attach the common operation policy "addHeader" to operation 0 of API "gwArtApiId" in flows "response" with parameters "{\"headerName\":\"GwArtResponseHeader\",\"headerValue\":\"GwArtResponseValue\"}"
    Then The response status code should be 200
    When I attach the common operation policy "addHeader" to operation 0 of API "gwArtApiId" in flows "fault" with parameters "{\"headerName\":\"GwArtFaultHeader\",\"headerValue\":\"GwArtFaultValue\"}"
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
    # The endpoints artifact carries EXACTLY the two endpoints (production and sandbox), each pointing at the
    # API's configured backend URL. The count is what matters: a substring check for "production"/"sandbox" passes
    # just as well if the gateway materialised one endpoint, three, or a stale extra from a previous revision.
    When I retrieve the gateway "end-points" for API "{{gwArtApiName}}" version "1.0.0" in tenant "<tenant>"
    Then The response status code should be 200
    And The response array field "endpoints" should have exactly 2 entries
    And The response should contain "production"
    And The response should contain "sandbox"
    # The backend URL the publisher configured is the address inside the deployed endpoint artifacts.
    And The response should contain "http://nodebackend:3001/jaxrs_basic/services/customers/customerservice"
    # The local entry: EXACTLY one, carrying the API id.
    When I retrieve the gateway "local-entry" for API "{{gwArtApiName}}" version "1.0.0" in tenant "<tenant>"
    Then The response status code should be 200
    And The response array field "localEntries" should have exactly 1 entries
    And The response should contain "{{gwArtApiId}}"
    # EXACTLY three mediation sequences (In, Out, Fault) — one per attached flow — and each one carries ITS OWN
    # header, matched by the synapse sequence name's --In/--Out/--Fault suffix. This is the assertion that proves
    # all three flows were generated: the per-flow step requires exactly one sequence per flow, so a missing Out or
    # Fault sequence (or two sequences colliding on one flow) fails.
    When I retrieve the gateway "sequence" for API "{{gwArtApiName}}" version "1.0.0" in tenant "<tenant>"
    Then The response status code should be 200
    And The response array field "sequences" should have exactly 3 entries
    And The gateway sequence for flow "In" should contain "GwArtRequestHeader"
    And The gateway sequence for flow "Out" should contain "GwArtResponseHeader"
    And The gateway sequence for flow "Fault" should contain "GwArtFaultHeader"

    Examples:
      | tenant       | suffix       |
      | carbon.super |              |
      | tenant1.com  | @tenant1.com |
