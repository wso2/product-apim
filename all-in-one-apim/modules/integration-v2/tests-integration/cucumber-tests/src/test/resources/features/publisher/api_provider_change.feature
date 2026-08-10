@cleanup
Feature: Publisher API Provider Change

  Admin-plane transfer of an API's provider (ownership) via POST /api/am/admin/v4/apis/{apiId}/change-provider:
  the API is re-owned by the target provider while its metadata (name, description, endpoints, resources) and its
  default-version flag are retained, a revision taken before the change keeps the ORIGINAL provider (a revision is
  an immutable snapshot), the API stays fully manageable by its new owner, and a change to a provider in a
  DIFFERENT tenant is rejected. Covers the REST, SOAP, SOAP-to-REST and GraphQL API types. Ports the
  publisher-plane assertions of ChangeApiProviderTestCase; the post-change GATEWAY invocation legs live in
  gateway/provider_change_invocation (§11 — the publisher features assert only the publisher plane). Runs as the
  tenant admin in both tenants; the target provider is a second creator/publisher user provisioned inline. Torn
  down by the cleanup hook.

  @cap:publisher @feat:api-lifecycle @type:regression @dep:admin @legacy:ChangeApiProviderTestCase
  Scenario Outline: Changing an API's provider re-owns it and retains its metadata in <tenant>
    Given The system is ready
    And I have valid access tokens as "admin<suffix>"
    And I provision user "apiNewProvider" with roles "Internal/creator,Internal/publisher" in tenant "<tenant>"
    # The admin authors the API (with a distinctive description) and deploys it.
    And I act as "admin<suffix>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "cpApiPayload"
    And I set the field "description" to "Provider change retention marker" in the payload "cpApiPayload"
    And I create an "apis" resource with payload "cpApiPayload" as "cpApiId"
    Then The response status code should be 201
    And I extract response field "name" and store it as "cpApiName"

    # A revision taken BEFORE the change: its provider must stay the ORIGINAL one. Captured under its own key
    # because the shared "revisionId" key is overwritten by any later revision create.
    When I put the following JSON payload in context as "cpRevisionPayload"
    """
    {"description":"Revision taken before the provider change"}
    """
    And I make a request to create a revision for "apis" resource "cpApiId" with payload "cpRevisionPayload"
    Then The response status code should be 201
    And I extract response field "id" and store it as "cpRevision1"

    # BASELINE for the issue-#5038 guard: the API IS the default version BEFORE the change (the fixture payload
    # sets isDefaultVersion). Without this baseline the post-change assertion would prove nothing.
    When I retrieve the "apis" resource with id "cpApiId"
    Then The response status code should be 200
    And The value of response field "isDefaultVersion" should be "true"

    # Change the provider to the second user.
    When I change the provider of API "cpApiId" to "apiNewProvider<suffix>"
    Then The response status code should be 200

    # The API is re-owned by the new provider and its metadata is retained.
    When I retrieve the "apis" resource with id "cpApiId"
    Then The response status code should be 200
    And The provider of API "cpApiId" should match actor "apiNewProvider<suffix>"
    And The response should contain "{{cpApiName}}"
    And The response should contain "Provider change retention marker"
    And The response should contain "nodebackend:3001/jaxrs_basic/services/customers/customerservice"
    And The response should contain "/customers/{id}"
    # NAMED regression guard (fix for issue #5038): the default-version flag survives the ownership transfer.
    And The value of response field "isDefaultVersion" should be "true"

    # The revision is an immutable snapshot: reading the API by its REVISION uuid still reports the ORIGINAL
    # provider, and the revision's swagger is still retrievable.
    And The provider of API "cpRevision1" should match actor "admin<suffix>"
    When I retrieve the swagger of "apis" resource "cpRevision1"
    Then The response status code should be 200
    And The response should contain "/customers/{id}"

    Examples:
      | tenant       | suffix       |
      | carbon.super |              |
      | tenant1.com  | @tenant1.com |

  # The API stays fully MANAGEABLE by its new owner: after the transfer the description, the business information
  # (all four owner fields), the subscription tiers, an existing document and a new comment can all still be
  # written, each verified by a following read; and the deployed revision can be undeployed and REPLACED by a new
  # revision created after the change. Ports the numbered post-change update sequence of
  # ChangeApiProviderTestCase#ChangeApiProvider (steps 1-10) plus its undeploy/redeploy leg. The remaining steps of
  # that sequence (11-18: adding a swagger resource, the OpenAPI description, the endpoint URL and the local scope)
  # are the same generic publisher update path already covered by publisher/api_config and publisher/definitions,
  # so they are not repeated here. The gateway invocation that legacy performs after the redeploy is in
  # gateway/provider_change_invocation.
  @cap:publisher @feat:api-lifecycle @rule:post-change-updates @type:regression @dep:admin @legacy:ChangeApiProviderTestCase
  Scenario Outline: An API remains fully manageable by its new provider in <tenant>
    Given The system is ready
    And I have valid access tokens as "admin<suffix>"
    And I provision user "arcNewProvider" with roles "Internal/creator,Internal/publisher" in tenant "<tenant>"
    And I act as "admin<suffix>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "arcApiId" and deployed it
    # Keep the deployed revision's id: it is undeployed and replaced AFTER the provider change.
    And I put value "revisionId" in context as "arcRevision1"

    # A document authored BEFORE the change — it is the one updated afterwards.
    When I put the following JSON payload in context as "newDocumentPayload"
    """
    {"name":"ProviderChangeHowTo","type":"HOWTO","summary":"doc summary before the provider change","sourceType":"INLINE","visibility":"API_LEVEL"}
    """
    And I add the document to API "arcApiId"
    Then The response status code should be 201
    And I put value "documentID" in context as "arcDocumentId"

    # The fixture payload ships Gold in policies, so strip it first — otherwise "the new owner can add the Gold
    # tier" would be vacuous.
    When I retrieve the "apis" resource with id "arcApiId"
    And I put the response payload in context as "arcApiDto"
    When I update the "apis" resource "arcApiId" and "arcApiDto" with configuration type "policies" and value:
    """
    ["Unlimited"]
    """
    Then The response status code should be 200

    # Transfer ownership.
    When I change the provider of API "arcApiId" to "arcNewProvider<suffix>"
    Then The response status code should be 200
    And The provider of API "arcApiId" should match actor "arcNewProvider<suffix>"

    # 1. The description can still be updated -> re-read shows the new value.
    When I retrieve the "apis" resource with id "arcApiId"
    And I put the response payload in context as "arcApiDto"
    When I update the "apis" resource "arcApiId" and "arcApiDto" with configuration type "description" and value:
    """
    This is an updated description after provider change
    """
    Then The response status code should be 200
    And The "apis" resource should reflect the updated "description" as:
    """
    This is an updated description after provider change
    """

    # 2. The business information can still be updated -> ALL FOUR owner fields are re-read exactly.
    When I retrieve the "apis" resource with id "arcApiId"
    And I put the response payload in context as "arcApiDto"
    When I update the "apis" resource "arcApiId" and "arcApiDto" with configuration type "businessInformation" and value:
    """
    {"businessOwner":"Updated Jane Roe","businessOwnerEmail":"marketing@pizzashack.com","technicalOwner":"John Doe","technicalOwnerEmail":"architecture@pizzashack.com"}
    """
    Then The response status code should be 200
    And The "apis" resource should reflect the updated "businessInformation" as:
    """
    {"businessOwner":"Updated Jane Roe","businessOwnerEmail":"marketing@pizzashack.com","technicalOwner":"John Doe","technicalOwnerEmail":"architecture@pizzashack.com"}
    """

    # 3. The Gold subscription tier can still be added -> policies is exactly Unlimited + Gold. Asserted as an
    # UNORDERED SET: the server returns policies alphabetically, but nothing in the contract promises an order for a
    # tier list, so pinning the returned sequence would over-fit an implementation detail. The expectation is
    # therefore written in the natural "existing + added" order and compared set-wise.
    When I retrieve the "apis" resource with id "arcApiId"
    And I put the response payload in context as "arcApiDto"
    When I update the "apis" resource "arcApiId" and "arcApiDto" with configuration type "policies" and value:
    """
    ["Unlimited","Gold"]
    """
    Then The response status code should be 200
    And The "apis" resource should reflect the updated "policies" as an unordered set:
    """
    ["Unlimited","Gold"]
    """

    # 4. The existing document can still be updated -> the new summary is visible in the document list.
    When I put the following JSON payload in context as "newDocumentPayload"
    """
    {"name":"ProviderChangeHowTo","type":"HOWTO","summary":"doc summary updated after the provider change","sourceType":"INLINE","visibility":"API_LEVEL"}
    """
    And I update the document with "arcDocumentId" for API "arcApiId"
    Then The response status code should be 200
    When I retrieve all available documents for "arcApiId"
    Then The response status code should be 200
    And The response should contain "doc summary updated after the provider change"

    # 5. A comment can still be added -> it reads back with the exact content, category and entry point.
    When I add a "publisher" comment "Comment after provider change" with category "general" to API "arcApiId" as "arcCommentId"
    Then The response status code should be 201
    When I retrieve the "publisher" comment "arcCommentId" of API "arcApiId" with reply limit 3 offset 0
    Then The response status code should be 200
    And The value of response field "content" should be "Comment after provider change"
    And The value of response field "category" should be "general"
    And The value of response field "entryPoint" should be "PUBLISHER"

    # 6. The pre-change revision can be undeployed and a NEW revision created and deployed after the change.
    When I undeploy revision "arcRevision1" of "apis" resource "arcApiId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "arcRevisionPayload"
    """
    {"description":"Revision created after the provider change"}
    """
    And I make a request to create a revision for "apis" resource "arcApiId" with payload "arcRevisionPayload"
    Then The response status code should be 201
    And I extract response field "id" and store it as "arcRevision2"
    When I deploy revision "arcRevision2" of "apis" resource "arcApiId"
    Then The response status code should be 201

    Examples:
      | tenant       | suffix       |
      | carbon.super |              |
      | tenant1.com  | @tenant1.com |

  # SOAP-API provider change: a WSDL-imported SOAP API keeps its TYPE-SPECIFIC state (SOAP type + retrievable WSDL
  # definition) as well as its provider after ownership transfer — the type variant exists precisely to catch
  # WSDL-binding loss on change, which the REST variant cannot represent. A revision taken before the change keeps
  # the ORIGINAL provider. Ports the SOAP variant of ChangeApiProviderTestCase (ChangeSoapApiProvider).
  @cap:publisher @feat:api-lifecycle @rule:soap @type:regression @dep:admin @legacy:ChangeApiProviderTestCase
  Scenario Outline: Changing a SOAP API's provider retains its WSDL binding in <tenant>
    Given The system is ready
    And I have valid access tokens as "admin<suffix>"
    And I provision user "soapNewProvider" with roles "Internal/creator,Internal/publisher" in tenant "<tenant>"
    And I act as "admin<suffix>"
    And I generate a unique value and store it as "cpSoapName"
    And I generate a unique value and store it as "cpSoapCtx"
    When I put the following JSON payload in context as "cpSoapProps"
    """
    {"name":"{{cpSoapName}}","context":"/{{cpSoapCtx}}","version":"1.0.0"}
    """
    And I import a WSDL API from file "artifacts/wsdl/hello.wsdl" with additional properties "cpSoapProps" and implementation type "SOAP" as "cpSoapApiId"
    Then The response status code should be 201
    And The response should contain "SOAP"

    # A revision taken BEFORE the change, to prove the snapshot keeps the original provider.
    When I put the following JSON payload in context as "cpSoapRevisionPayload"
    """
    {"description":"Revision taken before the SOAP provider change"}
    """
    And I make a request to create a revision for "apis" resource "cpSoapApiId" with payload "cpSoapRevisionPayload"
    Then The response status code should be 201
    And I extract response field "id" and store it as "cpSoapRevision1"

    # Change the provider to the second user.
    When I change the provider of API "cpSoapApiId" to "soapNewProvider<suffix>"
    Then The response status code should be 200

    # Re-owned, still SOAP-typed, and the WSDL definition is still retrievable (the WSDL binding survived).
    When I retrieve the "apis" resource with id "cpSoapApiId"
    Then The response status code should be 200
    And The provider of API "cpSoapApiId" should match actor "soapNewProvider<suffix>"
    And The value of response field "type" should be "SOAP"
    When I retrieve the WSDL definition of API "cpSoapApiId"
    Then The response status code should be 200
    When I retrieve the swagger of "apis" resource "cpSoapApiId"
    Then The response status code should be 200

    # The revision snapshot keeps the ORIGINAL provider, and its WSDL and swagger stay readable by revision uuid.
    And The provider of API "cpSoapRevision1" should match actor "admin<suffix>"
    When I retrieve the WSDL definition of API "cpSoapRevision1"
    Then The response status code should be 200
    When I retrieve the swagger of "apis" resource "cpSoapRevision1"
    Then The response status code should be 200

    Examples:
      | tenant       | suffix       |
      | carbon.super |              |
      | tenant1.com  | @tenant1.com |

  # SOAP-to-REST provider change: the generated conversion ("resource policy") SEQUENCES are the type-specific
  # state a provider change could silently lose, so they are compared BYTE-IDENTICALLY before and after — a
  # non-empty check would pass on a regenerated-but-different sequence. They must also remain UPDATABLE by the new
  # owner and readable through the pre-change revision uuid. Ports
  # ChangeApiProviderTestCase#ChangeSoapToRestApiProvider.
  @cap:publisher @feat:api-lifecycle @rule:soap-to-rest @type:regression @dep:admin @legacy:ChangeApiProviderTestCase
  Scenario Outline: Changing a SOAP-to-REST API's provider keeps its sequences byte-identical in <tenant>
    Given The system is ready
    And I have valid access tokens as "admin<suffix>"
    And I provision user "s2rNewProvider" with roles "Internal/creator,Internal/publisher" in tenant "<tenant>"
    And I act as "admin<suffix>"
    And I generate a unique value and store it as "cpS2rName"
    And I generate a unique value and store it as "cpS2rCtx"
    When I put the following JSON payload in context as "cpS2rProps"
    """
    {"name":"{{cpS2rName}}","context":"/{{cpS2rCtx}}","version":"1.0.0","policies":["Unlimited"],"endpointConfig":{"endpoint_type":"http","production_endpoints":{"url":"http://nodebackend:3021/service"},"sandbox_endpoints":{"url":"http://nodebackend:3021/service"}}}
    """
    And I import a WSDL API from file "artifacts/wsdl/hello.wsdl" with additional properties "cpS2rProps" and implementation type "SOAPTOREST" as "cpS2rApiId"
    Then The response status code should be 201
    And The response should contain "sayHello"

    # Baseline: the in and out conversion sequences of the generated sayHello resource, captured verbatim.
    When I snapshot the "in" resource policies of API "cpS2rApiId" for resource "sayHello" verb "post" as "cpS2rInBefore"
    And I snapshot the "out" resource policies of API "cpS2rApiId" for resource "sayHello" verb "post" as "cpS2rOutBefore"

    When I put the following JSON payload in context as "cpS2rRevisionPayload"
    """
    {"description":"Revision taken before the SOAP-to-REST provider change"}
    """
    And I make a request to create a revision for "apis" resource "cpS2rApiId" with payload "cpS2rRevisionPayload"
    Then The response status code should be 201
    And I extract response field "id" and store it as "cpS2rRevision1"

    # Change the provider to the second user.
    When I change the provider of API "cpS2rApiId" to "s2rNewProvider<suffix>"
    Then The response status code should be 200
    And The provider of API "cpS2rApiId" should match actor "s2rNewProvider<suffix>"

    # CORE: both sequence directions are byte-identical to the pre-change baseline.
    Then The "in" resource policies of API "cpS2rApiId" for resource "sayHello" verb "post" should be byte-identical to snapshot "cpS2rInBefore"
    And The "out" resource policies of API "cpS2rApiId" for resource "sayHello" verb "post" should be byte-identical to snapshot "cpS2rOutBefore"
    # The API definition is still retrievable after the change.
    When I retrieve the swagger of "apis" resource "cpS2rApiId"
    Then The response status code should be 200

    # The sequences are readable through the pre-change revision uuid, and that snapshot keeps the ORIGINAL
    # provider. Read before the update below, so the comparison is against an untouched working copy.
    And The provider of API "cpS2rRevision1" should match actor "admin<suffix>"
    Then The "in" resource policies of API "cpS2rRevision1" for resource "sayHello" verb "post" should be byte-identical to snapshot "cpS2rInBefore"
    And The "out" resource policies of API "cpS2rRevision1" for resource "sayHello" verb "post" should be byte-identical to snapshot "cpS2rOutBefore"

    # The sequences remain UPDATABLE by the new owner, and the update is visible on re-read.
    When I append "<!-- Updated after provider change -->" to each "in" resource policy of API "cpS2rApiId" for resource "sayHello" verb "post"
    And I retrieve the "in" resource policies of API "cpS2rApiId" for resource "sayHello" verb "post"
    Then The response status code should be 200
    And The response should contain "Updated after provider change"
    When I append "<!-- Updated after provider change -->" to each "out" resource policy of API "cpS2rApiId" for resource "sayHello" verb "post"
    And I retrieve the "out" resource policies of API "cpS2rApiId" for resource "sayHello" verb "post"
    Then The response status code should be 200
    And The response should contain "Updated after provider change"

    Examples:
      | tenant       | suffix       |
      | carbon.super |              |
      | tenant1.com  | @tenant1.com |

  # GraphQL provider change: the schema definition is the type-specific state, so it (and the generated swagger)
  # must still be retrievable after the transfer, and the pre-change revision must still report the ORIGINAL
  # provider and serve its schema. Ports the publisher-plane half of
  # ChangeApiProviderTestCase#ChangeGraphQLApiProvider (its invocation leg is in
  # gateway/provider_change_invocation).
  @cap:publisher @feat:api-lifecycle @rule:graphql @type:regression @dep:admin @legacy:ChangeApiProviderTestCase
  Scenario Outline: Changing a GraphQL API's provider retains its schema in <tenant>
    Given The system is ready
    And I have valid access tokens as "admin<suffix>"
    And I provision user "gqlNewProvider" with roles "Internal/creator,Internal/publisher" in tenant "<tenant>"
    And I act as "admin<suffix>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_graphql_api.json" in context as "cpGqlPayload"
    And I create a GraphQL API with schema file "artifacts/payloads/graphql_schema.graphql" and additional properties "cpGqlPayload" as "cpGqlApiId"
    Then The response status code should be 201

    When I put the following JSON payload in context as "cpGqlRevisionPayload"
    """
    {"description":"Revision taken before the GraphQL provider change"}
    """
    And I make a request to create a revision for "apis" resource "cpGqlApiId" with payload "cpGqlRevisionPayload"
    Then The response status code should be 201
    And I extract response field "id" and store it as "cpGqlRevision1"

    # Change the provider to the second user.
    When I change the provider of API "cpGqlApiId" to "gqlNewProvider<suffix>"
    Then The response status code should be 200

    # Re-owned, still GRAPHQL-typed, and both the schema definition and the swagger survive the transfer.
    When I retrieve the "apis" resource with id "cpGqlApiId"
    Then The response status code should be 200
    And The provider of API "cpGqlApiId" should match actor "gqlNewProvider<suffix>"
    And The value of response field "type" should be "GRAPHQL"
    When I retrieve the GraphQL schema of API "cpGqlApiId"
    Then The response status code should be 200
    And The response should contain "type Query"
    When I retrieve the swagger of "apis" resource "cpGqlApiId"
    Then The response status code should be 200

    # The revision snapshot keeps the ORIGINAL provider and still serves its schema and swagger.
    And The provider of API "cpGqlRevision1" should match actor "admin<suffix>"
    When I retrieve the GraphQL schema of API "cpGqlRevision1"
    Then The response status code should be 200
    And The response should contain "type Query"
    When I retrieve the swagger of "apis" resource "cpGqlRevision1"
    Then The response status code should be 200

    Examples:
      | tenant       | suffix       |
      | carbon.super |              |
      | tenant1.com  | @tenant1.com |

  # A cross-tenant provider change is rejected: changing a super-tenant API's provider to a tenant1.com user (and
  # vice versa) fails with 400 and error 901409 with the literal "Tenant mismatch" description. Ports the
  # cross-tenant rejection assertion.
  @cap:publisher @feat:api-lifecycle @type:negative @dep:admin @legacy:ChangeApiProviderTestCase
  Scenario Outline: A cross-tenant provider change is rejected in <tenant>
    Given The system is ready
    And I have valid access tokens as "admin<suffix>"
    And I act as "admin<suffix>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "ctpApiId" and deployed it
    # Attempt to change the provider to an admin in the OTHER tenant -> 400 + 901409 + "Tenant mismatch".
    When I change the provider of API "ctpApiId" to "admin<otherSuffix>"
    Then The response status code should be 400
    And The response should contain "901409"
    And The response should contain "Tenant mismatch"

    Examples:
      | tenant       | suffix       | otherSuffix  |
      | carbon.super |              | @tenant1.com |
      | tenant1.com  | @tenant1.com |              |

  # verify-first NOTE: the tenant of `admin` (no suffix) is carbon.super and `admin@tenant1.com` is tenant1.com;
  # the cross-tenant row targets the opposite tenant's admin as the (invalid) new provider.
