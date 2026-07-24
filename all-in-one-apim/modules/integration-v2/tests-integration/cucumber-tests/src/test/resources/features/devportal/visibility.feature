@cleanup
Feature: DevPortal Store-side Role-based Visibility

  Ports DevPortalVisibilityTestCase (commented-out in the legacy testng.xml but fully implemented) and
  APIVisibilityWithDirectURLTestCase. A published API with DevPortal visibility RESTRICTED to a role is hidden
  from the store for anyone who does not carry that role: a direct DevPortal GET of the API, its documents, its
  document content and its swagger all return 404 to an anonymous caller and to an authenticated user with a
  DIFFERENT role, and 200 to a user carrying the role — while a user with a publisher role sees it regardless
  (the publisher bypass). A default (public) API is reachable anonymously.

  Custom roles and the role-bearing devportal users are provisioned inline per scenario (SOAP addRole / addUser)
  so nothing is shared across runners; unique lowercase role names (WSO2 role validation matches the stored case)
  and unique user names keep parallel scenarios isolated. The restricted status the store returns to an
  unauthorised caller is 404 (the store hides the API, it does not 403), asserted exactly. Anonymous reads carry
  the tenant context header so a tenant API resolves for the unauthenticated caller (without it a tenant API is
  404 regardless of visibility). Run in both tenants.

  # Restricted-visibility API + its document + its swagger: 404 to anonymous / wrong-role, 200 to right-role and
  # to the publisher-bypass user. This single scenario covers testRestrictedDevPortalAPIAccess,
  # testRestrictedDevPortalDocumentAccess and testRestrictedDevPortalOpenAPISpecAccess plus the direct-URL API
  # GET of APIVisibilityWithDirectURLTestCase (the same /apis/{id} store read).
  @cap:devportal @feat:visibility @type:regression @dep:publisher @legacy:DevPortalVisibilityTestCase @legacy:APIVisibilityWithDirectURLTestCase
  Scenario Outline: A role-restricted API and its documents and swagger are hidden from unauthorised consumers in <tenant>
    Given The system is ready
    And I have valid access tokens as "admin<suffix>"
    And I generate a unique value and store it as "visrole"
    And I generate a unique value and store it as "visotherrole"
    And I provision store-visibility role "{{visrole}}" in tenant "<tenant>"
    And I provision store-visibility role "{{visotherrole}}" in tenant "<tenant>"
    And I provision user "visRoleUser" with roles "Internal/subscriber,{{visrole}}" in tenant "<tenant>"
    And I provision user "visOtherUser" with roles "Internal/subscriber,{{visotherrole}}" in tenant "<tenant>"
    And I provision user "visPubUser" with roles "Internal/publisher,Internal/creator,Internal/subscriber" in tenant "<tenant>"
    And The system is ready and I have valid devportal access token as "visRoleUser<suffix>"
    And The system is ready and I have valid devportal access token as "visOtherUser<suffix>"

    # The admin owns the restricted API (admin can assign any role); a publisher-role user (no restricted role)
    # will later exercise the publisher bypass.
    Given The system is ready and I have valid publisher access tokens as "visPubUser<suffix>"
    Given I act as "admin<suffix>"
    When I have created an api from "artifacts/payloads/create_apim_test_api.json" with restricted visibility for roles "{{visrole}}" as "visApiId" and deployed it
    And I publish the "apis" resource with id "visApiId"
    Then The lifecycle status of API "visApiId" should be "Published"
    # Add a document (captured as documentID) then post its inline content separately (the create-payload
    # inlineContent is metadata only; the retrievable /content resource must be posted), so document AND document
    # content store visibility can both be checked.
    When I prepare a document named "${UNIQUE:VisDoc}" of type "HOWTO" with sourceType "INLINE" and content "Sample restricted content"
    And I add the document to API "visApiId"
    And I add inline content "Sample restricted content body" to document "documentID" of API "visApiId"

    # Anonymous: API, document, content and swagger are all 404.
    Then I retrieve the devportal "api" of API "visApiId" anonymously in tenant "<tenant>" until the response status code becomes 404 within 60 seconds
    And I retrieve the devportal "document" of API "visApiId" anonymously in tenant "<tenant>" until the response status code becomes 404 within 60 seconds
    And I retrieve the devportal "document content" of API "visApiId" anonymously in tenant "<tenant>" until the response status code becomes 404 within 60 seconds
    And I retrieve the devportal "swagger" of API "visApiId" anonymously in tenant "<tenant>" until the response status code becomes 404 within 60 seconds

    # A user with a DIFFERENT role: all 404.
    When I act as "visOtherUser<suffix>"
    Then I retrieve the devportal "api" of API "visApiId" until the response status code becomes 404 within 60 seconds
    And I retrieve the devportal "document" of API "visApiId" until the response status code becomes 404 within 60 seconds
    And I retrieve the devportal "document content" of API "visApiId" until the response status code becomes 404 within 60 seconds
    And I retrieve the devportal "swagger" of API "visApiId" until the response status code becomes 404 within 60 seconds

    # The role-bearing user: all 200.
    When I act as "visRoleUser<suffix>"
    Then I retrieve the devportal "api" of API "visApiId" until the response status code becomes 200 within 60 seconds
    And I retrieve the devportal "document" of API "visApiId" until the response status code becomes 200 within 60 seconds
    And I retrieve the devportal "document content" of API "visApiId" until the response status code becomes 200 within 60 seconds
    And I retrieve the devportal "swagger" of API "visApiId" until the response status code becomes 200 within 60 seconds

    # The publisher-role user (no restricted role): sees it via the publisher bypass — 200.
    When I act as "visPubUser<suffix>"
    Then I retrieve the devportal "api" of API "visApiId" until the response status code becomes 200 within 60 seconds
    And I retrieve the devportal "swagger" of API "visApiId" until the response status code becomes 200 within 60 seconds

    Examples:
      | tenant       | suffix       |
      | carbon.super |              |
      | tenant1.com  | @tenant1.com |

  # A default (public) API is reachable by an anonymous store caller. Ports testAnonymousUserAccessDevPortalAPI.
  @cap:devportal @feat:visibility @type:smoke @dep:publisher @legacy:DevPortalVisibilityTestCase
  Scenario Outline: A public API is reachable anonymously from the store in <tenant>
    Given The system is ready and I have valid publisher access tokens as "publisherUser<suffix>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "pubApiId" and deployed it
    When I publish the "apis" resource with id "pubApiId"
    Then The lifecycle status of API "pubApiId" should be "Published"
    Then I retrieve the devportal "api" of API "pubApiId" anonymously in tenant "<tenant>" until the response status code becomes 200 within 60 seconds

    Examples:
      | tenant       | suffix       |
      | carbon.super |              |
      | tenant1.com  | @tenant1.com |
