@cleanup
Feature: Publisher-side API Visibility Across Domains

  Ports the PUBLISHER-plane half of APIVisibilityByDomainTestCase, APIVisibilityByPublicTestCase and
  APIVisibilityByRoleTestCase. Those classes assert LIST MEMBERSHIP — getAllAPIs() + isAPIAvailable(...) — rather
  than a GET-by-id, and they assert it for every DevPortal visibility mode with the SAME outcome. That is the
  behaviour pinned here: the DevPortal visibility field (PUBLIC / PRIVATE / RESTRICTED) is a devportal-plane
  concern and does NOT filter the Publisher list, which is scoped by TENANT — so an API is listed for every
  publisher-plane principal of its own domain (its creator, its domain's admin, and another creator in the same
  domain) and for none in another domain (neither that domain's creator nor that domain's admin).

  Note the distinction from publisher/access_control.feature: that covers accessControl / accessControlRoles, the
  publisher-plane restriction. This file never sets those fields — every API here is unrestricted on the publisher
  plane, so the only thing that could hide it from a same-domain principal is the visibility field, and the point
  is that it does not.

  List membership is probed as a name-scoped publisher search (GET /apis?query=name:<uniqueName>) asserting an
  EXACT count: 1 = present, 0 = absent. The name is scenario-unique, so the count is unambiguous, and scoping the
  list by it is what keeps the assertion exact under parallel load — reading the unfiltered first page of /apis
  would hide the API behind the default page size and read as a false "absent". A non-200 leaves the count
  unresolved and fails the step, so an absence is always a real empty 200, never an error mistaken for absence.

  Index timing: the publisher search index is eventually consistent, so an absence poll can pass instantly on an
  index that has not yet seen the API. Every scenario therefore asserts the CREATOR's presence (count 1) FIRST —
  which proves the API is indexed — and only then asserts the cross-domain absences against that same warm index.

  Actor coverage is one logical matrix: primary-store and SECONDARY.COM publishers create in both tenants, with
  all visibility/role combinations represented for each tenant/store combination. The same generic runner executes
  this matrix in standard and emailUserMode blocks; the block-aware actor resolver supplies the corresponding
  physical username. The same-domain peer is provisioned inline in both tenants, so every row checks the
  same-domain non-admin path as well as the matching tenant admin and cross-domain principals.

  @cap:publisher @feat:visibility @rule:cross-domain @type:regression @legacy:APIVisibilityByDomainTestCase @legacy:APIVisibilityByPublicTestCase @legacy:APIVisibilityByRoleTestCase
  Scenario Outline: A published API with <visibility> devportal visibility created by <creatorActor> is listed only in <creatorTenant>
    Given The system is ready
    And I have valid access tokens as "admin"
    And I provision user "visDomPeer" with roles "Internal/creator,Internal/publisher,Internal/subscriber" in tenant "carbon.super"
    And I provision user "visDomPeer" with roles "Internal/creator,Internal/publisher,Internal/subscriber" in tenant "tenant1.com"
    And The system is ready and I have valid publisher access tokens as "visDomPeer"
    And The system is ready and I have valid publisher access tokens as "visDomPeer@tenant1.com"
    And The system is ready and I have valid access tokens as "<sameDomainAdmin>"
    And The system is ready and I have valid publisher access tokens as "<otherDomainPeer>"
    And I have valid access tokens as "<otherDomainAdmin>"

    # Author + publish as the table-selected least-privileged publisher, under a scenario-unique name so the
    # name-scoped list query below resolves to exactly this API.
    Given The system is ready and I have valid publisher access tokens as "<creatorActor>"
    And I generate a unique value and store it as "visDomName"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "visDomPayload"
    And I set the field "name" to "{{visDomName}}" in the payload "visDomPayload"
    And I have created an api from context payload "visDomPayload" with devportal visibility "<visibility>" for roles "<roles>" as "visDomApiId" and deployed it
    When I publish the "apis" resource with id "visDomApiId"
    Then The lifecycle status of API "visDomApiId" should be "Published"

    # PRESENT for its CREATOR. This is also the warm-index gate for every absence assertion that follows.
    When I search Publisher APIs with content query "name:{{visDomName}}" until the result count is 1 within 60 seconds
    Then The response status code should be 200

    # PRESENT for the creator's SAME-domain admin.
    When I act as "<sameDomainAdmin>"
    And I search Publisher APIs with content query "name:{{visDomName}}" until the result count is 1 within 60 seconds
    Then The response status code should be 200

    # PRESENT for ANOTHER creator in the same domain (neither the creator nor an admin).
    When I act as "visDomPeer@<creatorTenant>"
    And I search Publisher APIs with content query "name:{{visDomName}}" until the result count is 1 within 60 seconds
    Then The response status code should be 200

    # ABSENT for a creator in ANOTHER domain.
    When I act as "<otherDomainPeer>"
    And I search Publisher APIs with content query "name:{{visDomName}}" until the result count is 0 within 60 seconds
    Then The response status code should be 200

    # ABSENT for the ADMIN of another domain — a tenant admin is not a superuser over other tenants.
    When I act as "<otherDomainAdmin>"
    And I search Publisher APIs with content query "name:{{visDomName}}" until the result count is 0 within 60 seconds
    Then The response status code should be 200

    Examples:
      | visibility | roles               | creatorActor                            | creatorTenant | sameDomainAdmin  | otherDomainPeer                              | otherDomainAdmin |
      | PUBLIC     |                     | publisherUser                           | carbon.super  | superTenantAdmin | publisherUser@tenant1.com                    | admin@tenant1.com |
      | PRIVATE    |                     | publisherUser                           | carbon.super  | superTenantAdmin | publisherUser@tenant1.com                    | admin@tenant1.com |
      | RESTRICTED | Internal/subscriber | publisherUser                           | carbon.super  | superTenantAdmin | publisherUser@tenant1.com                    | admin@tenant1.com |
      | RESTRICTED | admin               | publisherUser                           | carbon.super  | superTenantAdmin | publisherUser@tenant1.com                    | admin@tenant1.com |
      | PUBLIC     |                     | publisherUser@tenant1.com               | tenant1.com   | admin@tenant1.com | publisherUser                                | superTenantAdmin |
      | PRIVATE    |                     | publisherUser@tenant1.com               | tenant1.com   | admin@tenant1.com | publisherUser                                | superTenantAdmin |
      | RESTRICTED | Internal/subscriber | publisherUser@tenant1.com               | tenant1.com   | admin@tenant1.com | publisherUser                                | superTenantAdmin |
      | RESTRICTED | admin               | publisherUser@tenant1.com               | tenant1.com   | admin@tenant1.com | publisherUser                                | superTenantAdmin |
      | PUBLIC     |                     | SECONDARY.COM/publisherUser1             | carbon.super  | superTenantAdmin | SECONDARY.COM/publisherUser1@tenant1.com     | admin@tenant1.com |
      | PRIVATE    |                     | SECONDARY.COM/publisherUser1             | carbon.super  | superTenantAdmin | SECONDARY.COM/publisherUser1@tenant1.com     | admin@tenant1.com |
      | RESTRICTED | Internal/subscriber | SECONDARY.COM/publisherUser1             | carbon.super  | superTenantAdmin | SECONDARY.COM/publisherUser1@tenant1.com     | admin@tenant1.com |
      | RESTRICTED | admin               | SECONDARY.COM/publisherUser1             | carbon.super  | superTenantAdmin | SECONDARY.COM/publisherUser1@tenant1.com     | admin@tenant1.com |
      | PUBLIC     |                     | SECONDARY.COM/publisherUser1@tenant1.com | tenant1.com   | admin@tenant1.com | SECONDARY.COM/publisherUser1                 | superTenantAdmin |
      | PRIVATE    |                     | SECONDARY.COM/publisherUser1@tenant1.com | tenant1.com   | admin@tenant1.com | SECONDARY.COM/publisherUser1                 | superTenantAdmin |
      | RESTRICTED | Internal/subscriber | SECONDARY.COM/publisherUser1@tenant1.com | tenant1.com   | admin@tenant1.com | SECONDARY.COM/publisherUser1                 | superTenantAdmin |
      | RESTRICTED | admin               | SECONDARY.COM/publisherUser1@tenant1.com | tenant1.com   | admin@tenant1.com | SECONDARY.COM/publisherUser1                 | superTenantAdmin |
