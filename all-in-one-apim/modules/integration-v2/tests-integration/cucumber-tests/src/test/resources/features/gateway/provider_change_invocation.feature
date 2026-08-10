@cleanup
Feature: Gateway Invocation After An API Provider Change

  Gateway-plane runtime half of the API provider (ownership) transfer: after an API is re-owned by a different
  provider, the deployed revision is undeployed, a NEW revision is created and deployed by the new owner, and the
  API is still invocable through the gateway with the SAME application token (200). Covers the REST and GraphQL
  types. This block opts into the node backend (initBackend), which a gateway invocation requires (§11); the
  publisher-plane retention assertions of the same transfer live in publisher/api_provider_change. Ports the
  invocation legs of ChangeApiProviderTestCase (ChangeApiProvider and ChangeGraphQLApiProvider). Teardown via the
  per-scenario cleanup hook.

  # Runs x2 tenants to prove the provider-change invocation arc remains tenant-scoped.
  @cap:gateway @feat:rest-invocation @rule:provider-change @type:regression @dep:publisher @dep:admin @legacy:ChangeApiProviderTestCase
  Scenario Outline: A REST API stays invocable after its provider changes and a new revision is deployed
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I provision user "pciNewProvider" with roles "Internal/creator,Internal/publisher" in tenant "<tenant>"
    And I act as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "pciApiId" and deployed it
    # Keep the deployed revision's id: it is undeployed and replaced AFTER the provider change.
    And I put value "revisionId" in context as "pciRevision1"
    When I publish the "apis" resource with id "pciApiId"
    Then The lifecycle status of API "pciApiId" should be "Published"
    When I retrieve the "apis" resource with id "pciApiId"
    And I extract response field "context" and store it as "pciApiContext"

    # BASELINE: the API is invocable through the gateway BEFORE the ownership transfer.
    When I have set up application with keys, subscribed to API "pciApiId", and obtained access token for "pciSubId"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{pciApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "{\"id\":123,\"name\":\"John\"}"

    # Transfer ownership.
    When I change the provider of API "pciApiId" to "pciNewProvider<suffix>"
    Then The response status code should be 200
    And The provider of API "pciApiId" should match actor "pciNewProvider<suffix>"

    # Undeploy the pre-change revision and deploy a NEW revision created after the change.
    When I undeploy revision "pciRevision1" of "apis" resource "pciApiId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "pciRevisionPayload"
    """
    {"description":"Revision created after the provider change"}
    """
    And I make a request to create a revision for "apis" resource "pciApiId" with payload "pciRevisionPayload"
    Then The response status code should be 201
    And I extract response field "id" and store it as "pciRevision2"
    When I deploy revision "pciRevision2" of "apis" resource "pciApiId"
    Then The response status code should be 201
    And the "apis" resource "pciApiId" should be live on the gateway, redeploying if propagation is lost

    # The re-owned API is invocable again through the gateway with the SAME application token.
    When I invoke the API at gateway context "{{pciApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "{\"id\":123,\"name\":\"John\"}"

  # GraphQL counterpart. Uses the SELF-HEALING deploy gate ("should be live on the gateway, redeploying if
  # propagation is lost", §15) rather than a plain retry, as the other GraphQL gateway features do — a dropped
  # at-most-once deploy event can only be fixed by re-emitting it. Runs x2 tenants like the REST counterpart.

    Examples:
      | actor | tenant | suffix |
      | admin | carbon.super |  |
      | admin@tenant1.com | tenant1.com | @tenant1.com |

  @cap:gateway @feat:graphql-invocation @rule:provider-change @type:regression @dep:publisher @dep:admin @legacy:ChangeApiProviderTestCase
  Scenario Outline: A GraphQL API stays invocable after its provider changes and a new revision is deployed
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I provision user "gciNewProvider" with roles "Internal/creator,Internal/publisher" in tenant "<tenant>"
    And I act as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_graphql_api.json" in context as "gciApiPayload"
    And I create a GraphQL API with schema file "artifacts/payloads/graphql_schema.graphql" and additional properties "gciApiPayload" as "gciApiId"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "gciApiId"
    And I put the response payload in context as "gciRetrievedPayload"
    And I extract response field "context" and store it as "gciApiContext"

    When I put the following JSON payload in context as "createRevisionPayload"
    """
    {"description":"Initial Revision"}
    """
    And I make a request to create a revision for "apis" resource "gciApiId" with payload "createRevisionPayload"
    Then The response status code should be 201
    And I extract response field "id" and store it as "gciRevision1"
    When I deploy revision "gciRevision1" of "apis" resource "gciApiId"
    Then The response status code should be 201
    And I wait for deployment of the resource in "gciRetrievedPayload"
    And the "apis" resource "gciApiId" should be live on the gateway, redeploying if propagation is lost
    And I publish the "apis" resource with id "gciApiId"
    Then The lifecycle status of API "gciApiId" should be "Published"

    # BASELINE: the GraphQL query resolves through the gateway BEFORE the ownership transfer.
    When I have set up application with keys, subscribed to API "gciApiId", and obtained access token for "gciSubId"
    Then The response status code should be 200
    When I put the following JSON payload in context as "gciQuery"
    """
    {"query": "{languages{code name}}"}
    """
    And I invoke the API at gateway context "{{gciApiContext}}/1.0.0" with method "POST" using access token "generatedAccessToken" and payload "gciQuery" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "Afrikaans"

    # Transfer ownership.
    When I change the provider of API "gciApiId" to "gciNewProvider<suffix>"
    Then The response status code should be 200
    And The provider of API "gciApiId" should match actor "gciNewProvider<suffix>"

    # Undeploy the pre-change revision and deploy a NEW revision created after the change.
    When I undeploy revision "gciRevision1" of "apis" resource "gciApiId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "gciRevisionPayload"
    """
    {"description":"Revision created after the provider change"}
    """
    And I make a request to create a revision for "apis" resource "gciApiId" with payload "gciRevisionPayload"
    Then The response status code should be 201
    And I extract response field "id" and store it as "gciRevision2"
    When I deploy revision "gciRevision2" of "apis" resource "gciApiId"
    Then The response status code should be 201
    And the "apis" resource "gciApiId" should be live on the gateway, redeploying if propagation is lost

    # The re-owned GraphQL API resolves the same query again with the SAME application token.
    When I invoke the API at gateway context "{{gciApiContext}}/1.0.0" with method "POST" using access token "generatedAccessToken" and payload "gciQuery" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "Afrikaans"

    Examples:
      | actor | tenant | suffix |
      | admin | carbon.super |  |
      | admin@tenant1.com | tenant1.com | @tenant1.com |
