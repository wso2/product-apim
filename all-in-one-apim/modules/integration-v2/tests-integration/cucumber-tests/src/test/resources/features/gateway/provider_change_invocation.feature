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
  Scenario Outline: A REST API stays invocable after its provider changes and a new revision is deployed as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I provision user "pciNewProvider" with roles "Internal/creator,Internal/publisher" in tenant "<tenant>"
    # Mint the new provider's publisher tokens so the post-transfer revision legs can run AS them.
    And The system is ready and I have valid publisher access tokens as "pciNewProvider<suffix>"
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
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    # Transfer ownership.
    When I change the provider of API "pciApiId" to "pciNewProvider<suffix>"
    Then The response status code should be 200
    And The provider of API "pciApiId" should match actor "pciNewProvider<suffix>"
    # The undeploy/revision/deploy legs below run AS THE NEW PROVIDER — the feature header's claim.
    When I act as "pciNewProvider<suffix>"

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
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    Examples:
      | actor | tenant | suffix |
      | admin | carbon.super |  |
      | admin@tenant1.com | tenant1.com | @tenant1.com |

  # GraphQL counterpart. Uses the SELF-HEALING deploy gate ("should be live on the gateway, redeploying if
  # propagation is lost", §15) rather than a plain retry, as the other GraphQL gateway features do — a dropped
  # at-most-once deploy event can only be fixed by re-emitting it. Runs x2 tenants like the REST counterpart.
  @cap:gateway @feat:graphql-invocation @rule:provider-change @type:regression @dep:publisher @dep:admin @legacy:ChangeApiProviderTestCase
  Scenario Outline: A GraphQL API stays invocable after its provider changes and a new revision is deployed as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I provision user "gciNewProvider" with roles "Internal/creator,Internal/publisher" in tenant "<tenant>"
    # Mint the new provider's publisher tokens so the post-transfer revision legs can run AS them.
    And The system is ready and I have valid publisher access tokens as "gciNewProvider<suffix>"
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
    # The undeploy/revision/deploy legs below run AS THE NEW PROVIDER — the feature header's claim.
    When I act as "gciNewProvider<suffix>"

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

  # DEFAULT-VERSION ROUTING across an ownership transfer. The DAO's two statements are keyed differently: the
  # default-version row is updated by API NAME + old provider (so it re-owns the default-version record of EVERY
  # version of that name), while the API row is updated by a single UUID. A name with two versions is therefore
  # the shape where the two can drift apart, and the user-visible consequence of a drift is that the UNVERSIONED
  # URL stops resolving to the default version.
  #
  # HOW "served by 2.0.0" IS PROVEN. The application is subscribed to 2.0.0 ONLY. A gateway request carrying that
  # application's token against the unversioned context therefore answers 200 only while the route resolves to
  # 2.0.0; had it drifted onto 1.0.0 the same token would be unsubscribed there and answered 403. A bare 200 on
  # the unversioned URL would not have distinguished the two.
  # scenario: KB-APIM-0001-S09
  @cap:gateway @feat:rest-invocation @rule:provider-change @type:regression @dep:publisher @dep:admin @legacy:ChangeApiProviderTestCase
  Scenario Outline: Unversioned routing still resolves to the re-owned default version as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I provision user "dvNewProvider" with roles "Internal/creator,Internal/publisher" in tenant "<tenant>"
    And I act as "<actor>"
    # Version 1.0.0 — created, deployed and published.
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "dvApiV1" and deployed it
    When I publish the "apis" resource with id "dvApiV1"
    Then The lifecycle status of API "dvApiV1" should be "Published"

    # Version 2.0.0 of the SAME name, taking over as the default version, then deployed and published too.
    When I create a new version "2.0.0" of "apis" resource "dvApiV1" with default version "true" as "dvApiV2"
    When I put the following JSON payload in context as "dvRevisionPayload"
    """
    {"description":"Initial revision of the default version"}
    """
    And I make a request to create a revision for "apis" resource "dvApiV2" with payload "dvRevisionPayload"
    Then The response status code should be 201
    And I extract response field "id" and store it as "dvRevisionV2"
    When I deploy revision "dvRevisionV2" of "apis" resource "dvApiV2"
    Then The response status code should be 201
    And the "apis" resource "dvApiV2" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "dvApiV2"
    Then The lifecycle status of API "dvApiV2" should be "Published"

    # BASELINE: 2.0.0 is the default version, 1.0.0 is not, and the unversioned URL resolves to 2.0.0.
    When I retrieve the "apis" resource with id "dvApiV2"
    Then The response status code should be 200
    And The value of response field "isDefaultVersion" should be "true"
    And I extract response field "context" and store it as "dvApiContext"
    When I retrieve the "apis" resource with id "dvApiV1"
    Then The response status code should be 200
    And The value of response field "isDefaultVersion" should be "false"
    When I have set up application with keys, subscribed to API "dvApiV2", and obtained access token for "dvSubId"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{dvApiContext}}/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"

    # Transfer ownership of 2.0.0 ONLY.
    When I change the provider of API "dvApiV2" to "dvNewProvider<suffix>"
    Then The response status code should be 200
    And The provider of API "dvApiV2" should match actor "dvNewProvider<suffix>"
    # 1.0.0 was not named in the call and keeps its original owner.
    And The provider of API "dvApiV1" should match actor "<actor>"
    When I retrieve the "apis" resource with id "dvApiV2"
    Then The response status code should be 200
    And The value of response field "isDefaultVersion" should be "true"

    # The unversioned route still resolves to the re-owned 2.0.0.
    When I invoke the API at gateway context "{{dvApiContext}}/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    Examples:
      | actor | tenant | suffix |
      | admin | carbon.super |  |
      | admin@tenant1.com | tenant1.com | @tenant1.com |
