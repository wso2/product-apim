@cleanup
Feature: DevPortal Store-side API Visibility Across Domains

  Ports the STORE-plane half of APIVisibilityByDomainTestCase (wholly uncovered before this file) and the
  cross-domain legs of APIVisibilityByPublicTestCase / APIVisibilityByRoleTestCase. Separate from
  devportal/visibility.feature: that file is the ROLE-based store visibility port and asserts GET-by-id status
  codes on an API and its sub-resources; this one asserts LIST MEMBERSHIP in the store — the observable the legacy
  classes actually use (getAllPublishedAPIs / getAPIListFromStoreAsAnonymousUser + isAPIAvailableInStore) — across
  DOMAINS, which a by-id 404 does not prove.

  The three DevPortal visibility modes are pinned separately because their store semantics genuinely differ:
    * PRIVATE — the DTO value behind the UI's "Visible to my domain": listed for every principal of the API's own
      tenant (creator, that tenant's admin, and a plain subscriber of that tenant) and for nobody outside it,
      INCLUDING the anonymous listing of its own tenant (which is not a domain principal at all).
    * PUBLIC — additionally listed for the anonymous caller of its own tenant, but still never for another
      tenant's principals or another tenant's anonymous listing.
    * RESTRICTED — listed only for a caller carrying a visible role, so its OWNER (who holds the role) sees it
      while a same-domain subscriber does not; it is likewise invisible across domains, including to the other
      tenant's admin, who carries an identically NAMED admin role in its own tenant.

  This is the DevPortal visibility field only. It is not accessControl (publisher-plane —
  publisher/access_control.feature) and it is not application visibility (an application's sharing scope, a
  different concept that happens to share the PRIVATE spelling).

  Membership is probed as a name-scoped store search (GET /apis?query=name:<uniqueName>) asserting an EXACT count:
  1 = present, 0 = absent. The name is scenario-unique so the count is unambiguous, and scoping by it keeps the
  assertion exact under parallel load rather than reading the unfiltered first page. A non-200 leaves the count
  unresolved and fails the step, so an absence is always a real empty 200 — an anonymous request rejected outright
  can never be mistaken for "the API is hidden".

  Index timing: store listing is index-backed and eventually consistent, so polling "until count is 0" can pass on
  an index that has not yet seen the API. Every scenario therefore establishes PRESENCE for an entitled principal
  FIRST — the creator, the owner, or (for PUBLIC) the anonymous caller of its own tenant — and only then asserts
  the absences against that same warm index. The PUBLIC scenario's anonymous PRESENCE assertion doubles as the
  control for the anonymous probe itself: it proves the unauthenticated listing does surface APIs, so the
  anonymous count 0 in the other scenarios is a real hiding decision and not a broken code path.

  APIs are created in carbon.super; carbon.super principals supply the same-domain assertions and *@tenant1.com
  principals the other-domain ones (the block provisions both). The acting actor is set explicitly at the start of
  every scenario — it leaks from whatever ran last.

  # PRIVATE = "Visible to my domain": the whole domain sees it in the store, nobody outside does, and neither does
  # the anonymous listing of its own domain. Closes the store half of APIVisibilityByDomainTestCase
  # (testVisibilityForCreatorInStore, ...AdminInSameDomainInStore, ...AnotherUserInSameDomainInStore,
  # ...AnotherUserInOtherDomainInStore, ...AdminInOtherDomainInStore, ...AnonymousUserInSameDomainInStore,
  # ...AnonymousUserInOtherDomainInStore).
  @cap:devportal @feat:visibility @rule:cross-domain @type:regression @dep:publisher @legacy:APIVisibilityByDomainTestCase
  Scenario: A domain-private API is listed in the store for its own domain only
    Given The system is ready
    And I have valid access tokens as "admin"
    And The system is ready and I have valid devportal access token as "subscriberUser"
    And The system is ready and I have valid devportal access token as "subscriberUser@tenant1.com"
    And I have valid access tokens as "admin@tenant1.com"

    Given The system is ready and I have valid publisher access tokens as "publisherUser"
    And I generate a unique value and store it as "privVisName"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "privVisPayload"
    And I set the field "name" to "{{privVisName}}" in the payload "privVisPayload"
    And I have created an api from context payload "privVisPayload" with devportal visibility "PRIVATE" for roles "" as "privVisApiId" and deployed it
    When I publish the "apis" resource with id "privVisApiId"
    Then The lifecycle status of API "privVisApiId" should be "Published"

    # PRESENT for its creator in the store. Also the warm-index gate for every absence below.
    When I search DevPortal APIs with content query "name:{{privVisName}}" until the result count is 1 within 60 seconds
    Then The response status code should be 200

    # PRESENT for the same-domain admin.
    When I act as "admin"
    And I search DevPortal APIs with content query "name:{{privVisName}}" until the result count is 1 within 60 seconds
    Then The response status code should be 200

    # PRESENT for a plain subscriber of the same domain — "my domain" means the domain, not just its privileged users.
    When I act as "subscriberUser"
    And I search DevPortal APIs with content query "name:{{privVisName}}" until the result count is 1 within 60 seconds
    Then The response status code should be 200

    # ABSENT for another domain's subscriber and for another domain's admin.
    When I act as "subscriberUser@tenant1.com"
    And I search DevPortal APIs with content query "name:{{privVisName}}" until the result count is 0 within 60 seconds
    Then The response status code should be 200
    When I act as "admin@tenant1.com"
    And I search DevPortal APIs with content query "name:{{privVisName}}" until the result count is 0 within 60 seconds
    Then The response status code should be 200

    # ABSENT from the ANONYMOUS listing of its OWN domain (an anonymous caller is not a domain principal) and from
    # the anonymous listing of the other domain.
    When I search DevPortal APIs anonymously in tenant "carbon.super" with query "name:{{privVisName}}" until the result count is 0 within 60 seconds
    Then The response status code should be 200
    When I search DevPortal APIs anonymously in tenant "tenant1.com" with query "name:{{privVisName}}" until the result count is 0 within 60 seconds
    Then The response status code should be 200

  # PUBLIC is visible to everyone INCLUDING the anonymous caller of its own domain — but "everyone" stops at the
  # tenant boundary. Closes the cross-domain legs of APIVisibilityByPublicTestCase
  # (...AnotherUserInOtherDomainInStore, ...AdminInOtherDomainInStore, ...AnonymousUserInOtherDomainInStore) plus
  # its same-domain admin and anonymous-same-domain legs, as list membership rather than a by-id read.
  @cap:devportal @feat:visibility @rule:cross-domain @type:regression @dep:publisher @legacy:APIVisibilityByPublicTestCase
  Scenario: A public API is listed anonymously in its own domain but never in another domain
    Given The system is ready
    And I have valid access tokens as "admin"
    And The system is ready and I have valid devportal access token as "subscriberUser@tenant1.com"
    And I have valid access tokens as "admin@tenant1.com"

    Given The system is ready and I have valid publisher access tokens as "publisherUser"
    And I generate a unique value and store it as "pubVisName"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "pubVisPayload"
    And I set the field "name" to "{{pubVisName}}" in the payload "pubVisPayload"
    And I have created an api from context payload "pubVisPayload" with devportal visibility "PUBLIC" for roles "" as "pubVisApiId" and deployed it
    When I publish the "apis" resource with id "pubVisApiId"
    Then The lifecycle status of API "pubVisApiId" should be "Published"

    # PRESENT for its creator, then for the same-domain admin.
    When I search DevPortal APIs with content query "name:{{pubVisName}}" until the result count is 1 within 60 seconds
    Then The response status code should be 200
    When I act as "admin"
    And I search DevPortal APIs with content query "name:{{pubVisName}}" until the result count is 1 within 60 seconds
    Then The response status code should be 200

    # PRESENT in the ANONYMOUS listing of its own domain. This is the control for the anonymous probe: it proves an
    # unauthenticated listing really does return APIs, so the anonymous count 0 assertions elsewhere are hiding
    # decisions and not a rejected request.
    When I search DevPortal APIs anonymously in tenant "carbon.super" with query "name:{{pubVisName}}" until the result count is 1 within 60 seconds
    Then The response status code should be 200

    # ABSENT for another domain's subscriber, another domain's admin, and another domain's anonymous listing —
    # PUBLIC does not mean cross-tenant.
    When I act as "subscriberUser@tenant1.com"
    And I search DevPortal APIs with content query "name:{{pubVisName}}" until the result count is 0 within 60 seconds
    Then The response status code should be 200
    When I act as "admin@tenant1.com"
    And I search DevPortal APIs with content query "name:{{pubVisName}}" until the result count is 0 within 60 seconds
    Then The response status code should be 200
    When I search DevPortal APIs anonymously in tenant "tenant1.com" with query "name:{{pubVisName}}" until the result count is 0 within 60 seconds
    Then The response status code should be 200

  # RESTRICTED: only a role-bearing caller of the API's own domain sees it. The OWNER's own store view was
  # unasserted anywhere in the corpus before this scenario. The other domain's admin is the sharp case — it holds
  # an identically NAMED admin role in its own tenant, so a role check that ignored the tenant would leak the API.
  # Closes the cross-domain and anonymous legs of APIVisibilityByRoleTestCase.
  @cap:devportal @feat:visibility @rule:cross-domain @type:regression @dep:publisher @legacy:APIVisibilityByRoleTestCase
  Scenario: A role-restricted API is listed in the store only for a role-bearing caller of its own domain
    Given The system is ready
    And I have valid access tokens as "admin"
    And The system is ready and I have valid devportal access token as "subscriberUser"
    And The system is ready and I have valid devportal access token as "subscriberUser@tenant1.com"
    And I have valid access tokens as "admin@tenant1.com"

    # Owned by the carbon.super admin and restricted to the admin role, so the owner carries the visible role.
    Given I act as "admin"
    And I generate a unique value and store it as "rolVisName"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "rolVisPayload"
    And I set the field "name" to "{{rolVisName}}" in the payload "rolVisPayload"
    And I have created an api from context payload "rolVisPayload" with devportal visibility "RESTRICTED" for roles "admin" as "rolVisApiId" and deployed it
    When I publish the "apis" resource with id "rolVisApiId"
    Then The lifecycle status of API "rolVisApiId" should be "Published"

    # PRESENT for the OWNER's own store view (it holds the visible role). Warm-index gate for the absences below.
    When I search DevPortal APIs with content query "name:{{rolVisName}}" until the result count is 1 within 60 seconds
    Then The response status code should be 200

    # ABSENT for a same-domain subscriber who lacks the visible role — RESTRICTED really restricts within the domain.
    When I act as "subscriberUser"
    And I search DevPortal APIs with content query "name:{{rolVisName}}" until the result count is 0 within 60 seconds
    Then The response status code should be 200

    # ABSENT for the other domain's admin (same role name, different tenant) and for the other domain's subscriber.
    When I act as "admin@tenant1.com"
    And I search DevPortal APIs with content query "name:{{rolVisName}}" until the result count is 0 within 60 seconds
    Then The response status code should be 200
    When I act as "subscriberUser@tenant1.com"
    And I search DevPortal APIs with content query "name:{{rolVisName}}" until the result count is 0 within 60 seconds
    Then The response status code should be 200

    # ABSENT from the anonymous listing of both domains.
    When I search DevPortal APIs anonymously in tenant "carbon.super" with query "name:{{rolVisName}}" until the result count is 0 within 60 seconds
    Then The response status code should be 200
    When I search DevPortal APIs anonymously in tenant "tenant1.com" with query "name:{{rolVisName}}" until the result count is 0 within 60 seconds
    Then The response status code should be 200
