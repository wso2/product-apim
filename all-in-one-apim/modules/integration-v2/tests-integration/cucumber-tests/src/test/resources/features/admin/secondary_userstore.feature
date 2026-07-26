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
    When I provision store role "SECONDARY.COM/userrole1" in tenant "<tenant>"
    And I provision store user "SECONDARY.COM/testUser1" with password "password123" and roles "Internal/subscriber,SECONDARY.COM/userrole1" in tenant "<tenant>"
    # Existence is asserted via isExistingUser — NOT via a non-empty role list. PIN: getRoleListOfUser returns
    # "Internal/everyone" for ANY username string (existing or not), so a non-empty role list is a FALSE POSITIVE
    # for existence.
    Then the store user "SECONDARY.COM/testUser1" in tenant "<tenant>" should exist
    # The user carries the assigned store role (proves add + role binding).
    Then the roles of store user "SECONDARY.COM/testUser1" in tenant "<tenant>" should contain "SECONDARY.COM/userrole1"
    # The store is case-insensitive: the UPPERCASE username resolves the same user and returns the same role.
    And the roles of store user "SECONDARY.COM/TESTUSER1" in tenant "<tenant>" should contain "SECONDARY.COM/userrole1"
    # Case-insensitivity holds for BOTH role-resolution paths, not just the SECONDARY.COM-domain one: the
    # case-variant lookup also returns the HYBRID (Internal/) role. Internal roles attach to the username string in
    # the hybrid-role table (a different path than store-domain roles), and on 4.7.0 that path honors the store's
    # CaseInsensitiveUsername too — so an UPPERCASE username still resolves its Internal/subscriber membership.
    And the roles of store user "SECONDARY.COM/TESTUSER1" in tenant "<tenant>" should contain "Internal/subscriber"
    # Teardown: remove the user and role from the store.
    When I remove the secondary user store user "SECONDARY.COM/testUser1" and role "SECONDARY.COM/userrole1" in tenant "<tenant>"

    Examples:
      | tenant       |
      | carbon.super |
      | tenant1.com  |

  # ChangeApiProviderSecondaryUserStore: an API's provider (ownership) can be transferred to a user that lives in
  # the SECONDARY.COM user store, and the transfer is honoured (the API re-owns to SECONDARY.COM/... and its
  # documentation survives). Reuses the secondary store this block stands up at boot. Ports the core of
  # ChangeApiProviderSecondaryUserStoreTestCase (the SOAP/GraphQL API-type variants are documented reductions —
  # the provider-change-to-secondary-user mechanism is type-independent). Runs in BOTH tenants (×2); a single
  # <tenant> column drives the acting admin (admin@<tenant>) and the store SOAP calls.
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

    Examples:
      | tenant       | provider                            |
      | carbon.super | SECONDARY.COM/testUser1             |
      | tenant1.com  | SECONDARY.COM/testUser1@tenant1.com |

  # PROBE (shared-DB isolation): the SECONDARY.COM store is registered for BOTH tenants against ONE shared H2 DB.
  # A user seeded into one tenant's store must be invisible to the other tenant's store — the usermgt UM_* tables
  # carry UM_TENANT_ID, so identical (domain, username) rows in different tenants are distinct users. This is the
  # empirical proof of the architecture's shared-DB claim (docs/devs/secondary-userstore-framework-architecture.md).
  @cap:admin @feat:tenants-orgs @rule:secondary-userstore @type:regression
  Scenario: The shared secondary-store DB isolates users by tenant (UM_TENANT_ID)
    Given The system is ready
    And I have valid access tokens as "admin"
    When I provision store role "SECONDARY.COM/isoRole1" in tenant "carbon.super"
    And I provision store user "SECONDARY.COM/isoUser1" with password "password123" and roles "SECONDARY.COM/isoRole1" in tenant "carbon.super"
    Then the store user "SECONDARY.COM/isoUser1" in tenant "carbon.super" should exist
    # Same store domain, same username, other tenant → absent (distinct UM_TENANT_ID on the shared DB).
    And the store user "SECONDARY.COM/isoUser1" in tenant "tenant1.com" should not exist
    # The runtime facility builds a FRESH empty schema (dbscripts DDL only) and addUserStore just registers the
    # store config — so NO admin is auto-created on registration (unlike copying a pre-seeded WSO2SHARED_DB, which
    # carries a SECONDARY.COM/admin row). Every store user is one the framework explicitly seeds.
    And the store user "SECONDARY.COM/admin" in tenant "carbon.super" should not exist
    When I remove the secondary user store user "SECONDARY.COM/isoUser1" and role "SECONDARY.COM/isoRole1" in tenant "carbon.super"

  # PROBE (store user as actor): a least-privilege publisher living in the SECONDARY.COM store — seeded as an ACTOR
  # by the framework (publisherUser1) — can DCR + obtain tokens (password grant) and drive the publisher plane.
  # Runs in BOTH tenants (×2). Its created API is torn down as that store user by the @cleanup hook.
  @cap:publisher @feat:api-lifecycle @rule:secondary-userstore @dep:admin @type:regression
  Scenario Outline: A secondary-store publisher user can authenticate and create+publish an API
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
  Scenario Outline: A secondary-store subscriber can subscribe an application to a published API
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
