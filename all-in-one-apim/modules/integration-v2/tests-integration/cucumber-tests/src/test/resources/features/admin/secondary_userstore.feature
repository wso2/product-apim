@cleanup
Feature: Admin Secondary User Store (case-insensitive)

  Admin-plane secondary user store: a JDBC-backed secondary user store (domain SECONDARY.COM, case-insensitive
  usernames) is stood up entirely at RUNTIME by the framework when the block sets initSecondaryUserStore=true — the
  usermgt schema is created in a fresh embedded H2 DB from the product's own dbscripts (in-container), the store is
  registered via UserStoreConfigAdminService.addUserStore (SOAP, hot-deploys asynchronously), and the block waits
  until the domain is active. No seeded .mv.db fixture and no boot-time serverFilesToCopy (see
  SecondaryUserStoreProvisioner + docs/devs/secondary-userstore-framework-architecture.md). A user added in that
  store with one username case is then resolvable via a DIFFERENT case of the username (its role list is returned).
  Ports SecondaryUserStoreCaseInsensitiveTestCase and adds the ×4 store-user-as-actor coverage (2 store users × 2
  tenants). Runs in its own thread-count=1 block so no sibling class shares the container-global store. Each
  scenario is self-contained (creates its own prerequisites inline); store users/roles are removed inline by the
  final step and APIs/applications by the per-scenario @cleanup hook. The store itself lives for the container's
  lifetime.

  # Runs in BOTH tenants (×2): the store is registered per-tenant on the shared DB, so case-insensitive resolution
  # is proven for each tenant's SECONDARY.COM store. A single <tenant> column drives both the acting admin
  # (admin@<tenant>) and the store SOAP calls. Each row self-provisions and removes its own user/role (isolated by
  # UM_TENANT_ID, so the identical name in the two rows is two distinct users).
  @cap:admin @feat:tenants-orgs @rule:secondary-userstore @type:regression @legacy:SecondaryUserStoreCaseInsensitiveTestCase
  Scenario Outline: A user in a case-insensitive secondary user store resolves by any username case (<tenant>)
    Given The system is ready
    And I have valid access tokens as "admin@<tenant>"
    # The SECONDARY.COM store is stood up at block boot by the framework (runtime addUserStore + schema from the
    # product dbscripts). Add a role, then a user carrying that role.
    When I provision role "SECONDARY.COM/userrole1" in tenant "<tenant>"
    And I provision store user "SECONDARY.COM/testUser1" with password "password123" and roles "Internal/subscriber,SECONDARY.COM/userrole1" in tenant "<tenant>"
    # Existence is asserted via isExistingUser — NOT via a non-empty role list. PIN: getRoleListOfUser returns
    # "Internal/everyone" for ANY username string (existing or not), so a non-empty role list is a FALSE POSITIVE
    # for existence.
    Then the user "SECONDARY.COM/testUser1" in tenant "<tenant>" should exist
    # The user carries the assigned store role (proves add + role binding).
    Then the roles of store user "SECONDARY.COM/testUser1" in tenant "<tenant>" should contain "SECONDARY.COM/userrole1"
    # The store is case-insensitive: the UPPERCASE username resolves the same user and returns the same role.
    And the roles of store user "SECONDARY.COM/TESTUSER1" in tenant "<tenant>" should contain "SECONDARY.COM/userrole1"
    # Case-insensitivity holds for BOTH role-resolution paths, not just the SECONDARY.COM-domain one: the
    # case-variant lookup also returns the HYBRID (Internal/) role. Internal roles attach to the username string in
    # the hybrid-role table (a different path than store-domain roles), and on 4.7.0 that path honors the store's
    # CaseInsensitiveUsername too — so an UPPERCASE username still resolves its Internal/subscriber membership.
    And the roles of store user "SECONDARY.COM/TESTUSER1" in tenant "<tenant>" should contain "Internal/subscriber"
    # Teardown: remove the user and role from the store — and ASSERT the removal took effect. A delete whose
    # response is never checked is how a leaking teardown stays green; isExistingUser is the same check the
    # existence assertion above uses, so the two bracket the user's whole lifetime.
    When I remove the secondary user store user "SECONDARY.COM/testUser1" and role "SECONDARY.COM/userrole1" in tenant "<tenant>"
    Then the user "SECONDARY.COM/testUser1" in tenant "<tenant>" should not exist
    # Case-insensitivity holds for the deletion too: the UPPERCASE username also resolves to "absent".
    And the user "SECONDARY.COM/TESTUSER1" in tenant "<tenant>" should not exist

    Examples:
      | tenant       |
      | carbon.super |
      | tenant1.com  |

  # The case dimensions the scenario above does not cover, all in one arc: an UPPER-case-named store ROLE, the
  # REVERSE lookup direction (a user created in UPPER case resolved by a LOWER-case username — above only proves
  # mixed-case-created resolved by UPPER case), and the POST-DELETE state of a store role. Legacy
  # SecondaryUserStoreCaseInsensitiveTestCase covers all three; its post-delete block asserted only its first user
  # and only the casings it happened to enumerate, and it never checked the users survived the role deletion, so
  # its assertions could have passed for the wrong reason.
  #
  # Uses names disjoint from the scenario above so a mid-scenario failure there cannot collide here (both run in
  # this same runner, sequentially, against the same per-tenant store).
  @cap:admin @feat:tenants-orgs @rule:secondary-userstore @type:regression @legacy:SecondaryUserStoreCaseInsensitiveTestCase
  Scenario Outline: Store roles resolve in either case direction and a deleted role leaves every case variant (<tenant>)
    Given The system is ready
    And I have valid access tokens as "admin@<tenant>"
    # Two store roles — one named in LOWER case, one in UPPER case.
    When I provision role "SECONDARY.COM/ciroleone" in tenant "<tenant>"
    And I provision role "SECONDARY.COM/CIROLETWO" in tenant "<tenant>"
    # Two store users — one added in MIXED case carrying the lowercase role, one added in UPPER case carrying the
    # uppercase role.
    And I provision store user "SECONDARY.COM/ciUserA" with password "password123" and roles "Internal/subscriber,SECONDARY.COM/ciroleone" in tenant "<tenant>"
    And I provision store user "SECONDARY.COM/CIUSERB" with password "password123" and roles "Internal/subscriber,SECONDARY.COM/CIROLETWO" in tenant "<tenant>"
    Then the user "SECONDARY.COM/ciUserA" in tenant "<tenant>" should exist
    And the user "SECONDARY.COM/CIUSERB" in tenant "<tenant>" should exist

    # Baseline — each user resolved with the casing it was created with carries its own role, in the role's
    # ORIGINAL casing (so the uppercase-named role is stored and returned as UPPER case, not normalised).
    Then the roles of store user "SECONDARY.COM/ciUserA" in tenant "<tenant>" should contain "SECONDARY.COM/ciroleone"
    And the roles of store user "SECONDARY.COM/CIUSERB" in tenant "<tenant>" should contain "SECONDARY.COM/CIROLETWO"
    # Forward direction (the control for the pair): mixed-case-created user resolved by an UPPER-case username.
    And the roles of store user "SECONDARY.COM/CIUSERA" in tenant "<tenant>" should contain "SECONDARY.COM/ciroleone"
    # REVERSE direction: UPPER-case-created user resolved by a LOWER-case username.
    And the roles of store user "SECONDARY.COM/ciuserb" in tenant "<tenant>" should contain "SECONDARY.COM/CIROLETWO"

    # Delete both ROLES only, leaving the users in place so their role lists remain queryable.
    When I remove the secondary user store role "SECONDARY.COM/ciroleone" in tenant "<tenant>"
    And I remove the secondary user store role "SECONDARY.COM/CIROLETWO" in tenant "<tenant>"
    # POST-DELETE: neither role remains on EITHER user. The absence check is case-INSENSITIVE (see the step's
    # javadoc), so one assertion per role covers every casing variant rather than only the ones enumerated.
    Then the roles of store user "SECONDARY.COM/ciUserA" in tenant "<tenant>" should not contain "SECONDARY.COM/ciroleone"
    And the roles of store user "SECONDARY.COM/ciUserA" in tenant "<tenant>" should not contain "SECONDARY.COM/CIROLETWO"
    And the roles of store user "SECONDARY.COM/CIUSERB" in tenant "<tenant>" should not contain "SECONDARY.COM/ciroleone"
    And the roles of store user "SECONDARY.COM/CIUSERB" in tenant "<tenant>" should not contain "SECONDARY.COM/CIROLETWO"
    # …and the case-variant username lookups agree, so the deletion is not merely invisible to one casing.
    And the roles of store user "SECONDARY.COM/CIUSERA" in tenant "<tenant>" should not contain "SECONDARY.COM/ciroleone"
    And the roles of store user "SECONDARY.COM/ciuserb" in tenant "<tenant>" should not contain "SECONDARY.COM/CIROLETWO"
    # Non-vacuity: the users still EXIST, so the absences above are the role's removal and not "the user is gone".
    And the user "SECONDARY.COM/ciUserA" in tenant "<tenant>" should exist
    And the user "SECONDARY.COM/CIUSERB" in tenant "<tenant>" should exist
    # The retained hybrid role is untouched by the store-role deletion.
    And the roles of store user "SECONDARY.COM/ciUserA" in tenant "<tenant>" should contain "Internal/subscriber"

    # Teardown: remove both users, asserted.
    When I remove the secondary user store user "SECONDARY.COM/ciUserA" in tenant "<tenant>"
    And I remove the secondary user store user "SECONDARY.COM/CIUSERB" in tenant "<tenant>"
    Then the user "SECONDARY.COM/ciUserA" in tenant "<tenant>" should not exist
    And the user "SECONDARY.COM/CIUSERB" in tenant "<tenant>" should not exist

    Examples:
      | tenant       |
      | carbon.super |
      | tenant1.com  |

  # ChangeApiProviderSecondaryUserStore: an API's provider (ownership) can be transferred to a user that lives in
  # the SECONDARY.COM user store, and the transfer is honoured (the API re-owns to SECONDARY.COM/... and its
  # documentation survives). Reuses the secondary store this block stands up at boot. Ports the REST variant of
  # ChangeApiProviderSecondaryUserStoreTestCase; the SOAP, SOAP-to-REST and GraphQL variants follow below. Runs in
  # BOTH tenants (×2); a single <tenant> column drives the acting admin (admin@<tenant>) and the store SOAP calls.
  @cap:publisher @feat:api-lifecycle @rule:secondary-userstore @dep:admin @type:regression @legacy:ChangeApiProviderSecondaryUserStoreTestCase
  Scenario Outline: An API's provider can be changed to a secondary-user-store user (<tenant>)
    Given The system is ready
    And I have valid access tokens as "admin@<tenant>"
    # A user in the SECONDARY.COM store to receive ownership (carries only global Internal/ roles).
    When I provision store user "SECONDARY.COM/testUser1" with password "password123" and roles "Internal/subscriber,Internal/publisher,Internal/creator" in tenant "<tenant>"

    # Create, deploy and publish an API, and add a document to it (a retained artifact to verify after the change).
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "cpSecApiId" and deployed it
    When I publish the "apis" resource with id "cpSecApiId"
    Then The lifecycle status of API "cpSecApiId" should be "Published"
    When I put the following JSON payload in context as "newDocumentPayload"
    """
    {"name":"SecProviderChangeDoc","type":"HOWTO","summary":"doc that must survive the provider change","sourceType":"INLINE","visibility":"API_LEVEL"}
    """
    And I add the document to API "cpSecApiId"
    Then The response status code should be 201

    # Change the provider to the secondary-store user; the API re-owns and the document is retained. The
    # change-provider endpoint resolves a tenant provider only when TENANT-QUALIFIED, so <provider> is the bare
    # store name in the super tenant but @tenant1.com-qualified in the tenant (a real per-tenant difference, not a
    # redundant column). The "contains" check uses the bare name — a substring of both forms.
    When I change the provider of API "cpSecApiId" to "<provider>"
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "cpSecApiId"
    Then The response should contain "SECONDARY.COM/testUser1"
    When I retrieve all available documents for "cpSecApiId"
    Then The response should contain "SecProviderChangeDoc"

    # Teardown: remove the secondary-store user (only Internal/ hybrid roles were assigned — those are global and
    # must not be deleted, so this is a user-only removal).
    When I remove the secondary user store user "SECONDARY.COM/testUser1" in tenant "<tenant>"
    Then the user "SECONDARY.COM/testUser1" in tenant "<tenant>" should not exist

    Examples:
      | tenant       | provider                            |
      | carbon.super | SECONDARY.COM/testUser1             |
      | tenant1.com  | SECONDARY.COM/testUser1@tenant1.com |

  # API-TYPE variants of the transfer-to-a-store-user (closing the reduction the scenario above used to declare).
  # Each type carries state a provider change could silently lose, and the store user is the receiving owner:
  # SOAP -> the WSDL binding, SOAP-to-REST -> the generated conversion sequences, GraphQL -> the schema definition.
  # Runs x2 tenants, extending ChangeApiProviderSecondaryUserStoreTestCase (whose userModeDataProvider yielded only
  # SUPER_TENANT_ADMIN). Each scenario provisions and removes its OWN store user so a failure cannot strand a shared name. This
  # block has no initBackend, so — as with the REST scenario above — these assert the publisher plane only.
  @cap:publisher @feat:api-lifecycle @rule:secondary-userstore @dep:admin @type:regression @legacy:ChangeApiProviderSecondaryUserStoreTestCase
  Scenario Outline: A SOAP API's provider can be changed to a secondary-user-store user as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I provision store user "SECONDARY.COM/soapProvUser" with password "password123" and roles "Internal/subscriber,Internal/publisher,Internal/creator" in tenant "<tenant>"
    And I generate a unique value and store it as "secSoapName"
    And I generate a unique value and store it as "secSoapCtx"
    When I put the following JSON payload in context as "secSoapProps"
    """
    {"name":"{{secSoapName}}","context":"/{{secSoapCtx}}","version":"1.0.0"}
    """
    And I import a WSDL API from file "artifacts/wsdl/hello.wsdl" with additional properties "secSoapProps" and implementation type "SOAP" as "secSoapApiId"
    Then The response status code should be 201
    # Baseline: the WSDL served BEFORE the transfer, captured verbatim (same shape as the SOAP-to-REST scenario's
    # policy snapshot below) so the post-transfer check can be byte-exact rather than merely "still retrievable".
    When I retrieve the WSDL definition of API "secSoapApiId"
    Then The response status code should be 200
    And I put the response payload in context as "secSoapWsdlBefore"

    When I change the provider of API "secSoapApiId" to "SECONDARY.COM/soapProvUser<suffix>"
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "secSoapApiId"
    Then The response status code should be 200
    And The value of response field "provider" should be "SECONDARY.COM/soapProvUser<suffix>"
    And The value of response field "type" should be "SOAP"
    # The WSDL binding survived the transfer to a store-resident owner — byte-identical, not merely retrievable.
    When I retrieve the WSDL definition of API "secSoapApiId"
    Then The response status code should be 200
    And I put the response payload in context as "secSoapWsdlAfter"
    And The stored value "secSoapWsdlAfter" should equal "secSoapWsdlBefore"

    # Teardown, verified: the removal step is best-effort and asserts nothing, so a delete that faulted
    # would leave this fixed username in the store and only surface as a confusing collision later.
    When I remove the secondary user store user "SECONDARY.COM/soapProvUser" in tenant "<tenant>"
    Then the user "SECONDARY.COM/soapProvUser" in tenant "<tenant>" should not exist

    Examples:
      | actor | tenant | suffix |
      | admin | carbon.super |  |
      | admin@tenant1.com | tenant1.com | @tenant1.com |

  @cap:publisher @feat:api-lifecycle @rule:secondary-userstore @dep:admin @type:regression @legacy:ChangeApiProviderSecondaryUserStoreTestCase
  Scenario Outline: A SOAP-to-REST API's provider can be changed to a secondary-user-store user as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I provision store user "SECONDARY.COM/s2rProvUser" with password "password123" and roles "Internal/subscriber,Internal/publisher,Internal/creator" in tenant "<tenant>"
    And I generate a unique value and store it as "secS2rName"
    And I generate a unique value and store it as "secS2rCtx"
    When I put the following JSON payload in context as "secS2rProps"
    """
    {"name":"{{secS2rName}}","context":"/{{secS2rCtx}}","version":"1.0.0","policies":["Unlimited"],"endpointConfig":{"endpoint_type":"http","production_endpoints":{"url":"http://nodebackend:3021/service"},"sandbox_endpoints":{"url":"http://nodebackend:3021/service"}}}
    """
    And I import a WSDL API from file "artifacts/wsdl/hello.wsdl" with additional properties "secS2rProps" and implementation type "SOAPTOREST" as "secS2rApiId"
    Then The response status code should be 201
    # Baseline: the generated conversion sequences, captured verbatim before the transfer.
    And I snapshot the "in" resource policies of API "secS2rApiId" for resource "sayHello" verb "post" as "secS2rInBefore"
    And I snapshot the "out" resource policies of API "secS2rApiId" for resource "sayHello" verb "post" as "secS2rOutBefore"

    When I change the provider of API "secS2rApiId" to "SECONDARY.COM/s2rProvUser<suffix>"
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "secS2rApiId"
    Then The response status code should be 200
    And The value of response field "provider" should be "SECONDARY.COM/s2rProvUser<suffix>"
    And The value of response field "type" should be "SOAPTOREST"
    # The conversion sequences are byte-identical after the transfer to a store-resident owner.
    Then The "in" resource policies of API "secS2rApiId" for resource "sayHello" verb "post" should be byte-identical to snapshot "secS2rInBefore"
    And The "out" resource policies of API "secS2rApiId" for resource "sayHello" verb "post" should be byte-identical to snapshot "secS2rOutBefore"

    # Teardown, verified: the removal step is best-effort and asserts nothing, so a delete that faulted
    # would leave this fixed username in the store and only surface as a confusing collision later.
    When I remove the secondary user store user "SECONDARY.COM/s2rProvUser" in tenant "<tenant>"
    Then the user "SECONDARY.COM/s2rProvUser" in tenant "<tenant>" should not exist

    Examples:
      | actor | tenant | suffix |
      | admin | carbon.super |  |
      | admin@tenant1.com | tenant1.com | @tenant1.com |

  @cap:publisher @feat:api-lifecycle @rule:secondary-userstore @dep:admin @type:regression @legacy:ChangeApiProviderSecondaryUserStoreTestCase
  Scenario Outline: A GraphQL API's provider can be changed to a secondary-user-store user as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I provision store user "SECONDARY.COM/gqlProvUser" with password "password123" and roles "Internal/subscriber,Internal/publisher,Internal/creator" in tenant "<tenant>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_graphql_api.json" in context as "secGqlPayload"
    And I create a GraphQL API with schema file "artifacts/payloads/graphql_schema.graphql" and additional properties "secGqlPayload" as "secGqlApiId"
    Then The response status code should be 201
    # Baseline: the SDL served BEFORE the transfer. Only schemaDefinition is captured — the envelope's `name`
    # embeds the provider (admin--<api>.graphql), so it changes BY DESIGN on transfer and would mask the SDL.
    When I retrieve the GraphQL schema of API "secGqlApiId"
    Then The response status code should be 200
    And I extract response field "schemaDefinition" and store it as "secGqlSchemaBefore"

    When I change the provider of API "secGqlApiId" to "SECONDARY.COM/gqlProvUser<suffix>"
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "secGqlApiId"
    Then The response status code should be 200
    And The value of response field "provider" should be "SECONDARY.COM/gqlProvUser<suffix>"
    And The value of response field "type" should be "GRAPHQL"
    # The schema definition survived the transfer to a store-resident owner — byte-identical, not merely present.
    When I retrieve the GraphQL schema of API "secGqlApiId"
    Then The response status code should be 200
    And I extract response field "schemaDefinition" and store it as "secGqlSchemaAfter"
    And The stored value "secGqlSchemaAfter" should equal "secGqlSchemaBefore"
    And The response should contain "type Query"

    # Teardown, verified: the removal step is best-effort and asserts nothing, so a delete that faulted
    # would leave this fixed username in the store and only surface as a confusing collision later.
    When I remove the secondary user store user "SECONDARY.COM/gqlProvUser" in tenant "<tenant>"
    Then the user "SECONDARY.COM/gqlProvUser" in tenant "<tenant>" should not exist

    Examples:
      | actor | tenant | suffix |
      | admin | carbon.super |  |
      | admin@tenant1.com | tenant1.com | @tenant1.com |

  # PROBE (shared-DB isolation): the SECONDARY.COM store is registered for BOTH tenants against ONE shared H2 DB.
  # A user seeded into one tenant's store must be invisible to the other tenant's store — the usermgt UM_* tables
  # carry UM_TENANT_ID, so identical (domain, username) rows in different tenants are distinct users. This is the
  # empirical proof of the architecture's shared-DB claim (docs/devs/secondary-userstore-framework-architecture.md).
  @cap:admin @feat:tenants-orgs @rule:secondary-userstore @type:regression
  Scenario: The shared secondary-store DB isolates users by tenant (UM_TENANT_ID)
    Given The system is ready
    And I have valid access tokens as "admin"
    When I provision role "SECONDARY.COM/isoRole1" in tenant "carbon.super"
    And I provision store user "SECONDARY.COM/isoUser1" with password "password123" and roles "SECONDARY.COM/isoRole1" in tenant "carbon.super"
    Then the user "SECONDARY.COM/isoUser1" in tenant "carbon.super" should exist
    # Same store domain, same username, other tenant → absent (distinct UM_TENANT_ID on the shared DB).
    And the user "SECONDARY.COM/isoUser1" in tenant "tenant1.com" should not exist
    # The runtime facility builds a FRESH empty schema (dbscripts DDL only) and addUserStore just registers the
    # store config — so NO admin is auto-created on registration (unlike copying a pre-seeded WSO2SHARED_DB, which
    # carries a SECONDARY.COM/admin row). Every store user is one the framework explicitly seeds.
    And the user "SECONDARY.COM/admin" in tenant "carbon.super" should not exist
    When I remove the secondary user store user "SECONDARY.COM/isoUser1" and role "SECONDARY.COM/isoRole1" in tenant "carbon.super"
    Then the user "SECONDARY.COM/isoUser1" in tenant "carbon.super" should not exist

  # PROBE (store user as actor): a least-privilege publisher living in the SECONDARY.COM store — seeded as an ACTOR
  # by the framework (publisherUser1) — can DCR + obtain tokens (password grant) and drive the publisher plane.
  # Runs in BOTH tenants (×2). Its created API is torn down as that store user by the @cleanup hook.
  @cap:publisher @feat:api-lifecycle @rule:secondary-userstore @dep:admin @type:regression
  Scenario Outline: A secondary-store publisher user can authenticate and create+publish an API as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I have created an api from "artifacts/payloads/create_apim_test_api.json" as "storeActorApiId" and deployed it
    And I publish the "apis" resource with id "storeActorApiId"
    Then The lifecycle status of API "storeActorApiId" should be "Published"

    Examples:
      | actor                                    |
      | SECONDARY.COM/publisherUser1             |
      | SECONDARY.COM/publisherUser1@tenant1.com |

  # ×4 completion: a least-privilege consumer living in the SECONDARY.COM store subscribes to a published API in
  # BOTH tenants. Self-contained: the tenant's primary-store publisher publishes the prerequisite API, then the
  # secondary-store subscriber creates an application and subscribes — proving a store user drives the devportal
  # consumer plane. With the publisher scenario above, this gives the full ×4: {publisher, subscriber} ×
  # {carbon.super, tenant1.com}. A single <tenantSuffix> column drives BOTH principals (primary publisher and the
  # store subscriber are the same tenant), so no redundant per-actor column is needed.
  @cap:devportal @feat:subscribe @rule:secondary-userstore @dep:publisher @dep:admin @type:regression
  Scenario Outline: A secondary-store subscriber can subscribe an application to a published API as SECONDARY.COM/subscriberUser1<tenantSuffix>
    Given The system is ready and I have valid publisher access tokens as "publisherUser<tenantSuffix>"
    When I have created an api from "artifacts/payloads/create_apim_test_api.json" as "storeSubApiId" and deployed it
    And I publish the "apis" resource with id "storeSubApiId"
    Then The lifecycle status of API "storeSubApiId" should be "Published"

    # Act as the secondary-store subscriber (same tenant): create an application and subscribe it to the API.
    Given The system is ready and I have valid devportal access token as "SECONDARY.COM/subscriberUser1<tenantSuffix>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "storeSubPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "storeSubApiId" using application "createdAppId" with payload "storeSubPayload" as "storeSubId"
    When I retrieve the subscription for Api "storeSubApiId" by Application "createdAppId"
    Then The response status code should be 200

    Examples:
      | tenantSuffix |
      |              |
      | @tenant1.com |
