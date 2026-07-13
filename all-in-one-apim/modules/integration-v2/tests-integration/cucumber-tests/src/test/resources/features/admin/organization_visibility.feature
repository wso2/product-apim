@cleanup
Feature: Admin Organization Visibility (B2B)

  Ports ConsumerOrganizationVisibilityTestCase: B2B organization CRUD and per-organization visibility of APIs,
  key managers, applications and subscription policies. Setup registers the organizationId local claim (SOAP),
  tags the acting tenant admin as a parent-org member, creates two sub-organizations, and provisions org-scoped
  devportal users. Run ×2 tenant (carbon.super + tenant1.com) — the harness is tenant-parameterized (claim /
  user provisioning / org creation use the row's tenant admin). DevPortal visibility is eventually consistent
  after a visibleOrganizations change, so checks poll. Super-tenant only in legacy; v2 exercises both tenants.

  @cap:admin @feat:tenants-orgs @type:regression @legacy:ConsumerOrganizationVisibilityTestCase
  Scenario Outline: Organizations can be created and an API's visibility is enforced per organization in <tenant>
    Given The system is ready
    And I register the organization local claim in tenant "<tenant>"
    And I set the organization claim of user "admin" in tenant "<tenant>" to "123-456-789"
    And I have valid access tokens as "admin<suffix>"
    When I create an organization "${UNIQUE:suborg1}" with display name "Sub Org One" as "subOrg1"
    Then The response status code should be 201
    When I create an organization "${UNIQUE:suborg2}" with display name "Sub Org Two" as "subOrg2"
    Then The response status code should be 201
    When I retrieve all organizations
    Then The response status code should be 200
    And The response should contain "Sub Org One"
    And The response should contain "Sub Org Two"
    And I provision organization user "orgpub1" with roles "Internal/creator,Internal/publisher" in organization "123-456-789" in tenant "<tenant>"
    And I provision organization user "orgdev1" with roles "Internal/subscriber" in organization "123-456-789" in tenant "<tenant>"
    And I provision organization user "suborg1dev" with roles "Internal/subscriber" in organization "{{subOrg1External}}" in tenant "<tenant>"
    And I provision organization user "suborg2dev" with roles "Internal/subscriber" in organization "{{subOrg2External}}" in tenant "<tenant>"
    And The system is ready and I have valid devportal access token as "orgdev1<suffix>"
    And The system is ready and I have valid devportal access token as "suborg1dev<suffix>"
    And The system is ready and I have valid devportal access token as "suborg2dev<suffix>"
    And The system is ready and I have valid publisher access tokens as "orgpub1<suffix>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "orgApiId" and deployed it
    When I publish the "apis" resource with id "orgApiId"
    Then The lifecycle status of API "orgApiId" should be "Published"
    When I act as "orgpub1<suffix>"
    And I set the visible organizations of API "orgApiId" to "none"
    Then The response status code should be 200
    When I act as "orgdev1<suffix>"
    Then I retrieve the devportal API "orgApiId" until the response status code becomes 200 within 60 seconds
    When I act as "suborg1dev<suffix>"
    Then I retrieve the devportal API "orgApiId" until the response status code becomes 403 within 60 seconds
    Then I retrieve the devportal API "orgApiId" anonymously in tenant "<tenant>" until the response status code becomes 403 within 60 seconds
    When I act as "orgpub1<suffix>"
    And I set the visible organizations of API "orgApiId" to "{{subOrg1}}"
    Then The response status code should be 200
    When I act as "suborg1dev<suffix>"
    Then I retrieve the devportal API "orgApiId" until the response status code becomes 200 within 60 seconds
    When I act as "orgdev1<suffix>"
    Then I retrieve the devportal API "orgApiId" until the response status code becomes 200 within 60 seconds
    When I act as "suborg2dev<suffix>"
    Then I retrieve the devportal API "orgApiId" until the response status code becomes 403 within 60 seconds
    When I act as "orgpub1<suffix>"
    And I set the visible organizations of API "orgApiId" to "all"
    Then The response status code should be 200
    When I act as "suborg2dev<suffix>"
    Then I retrieve the devportal API "orgApiId" until the response status code becomes 200 within 60 seconds
    Then I retrieve the devportal API "orgApiId" anonymously in tenant "<tenant>" until the response status code becomes 200 within 60 seconds
    When I act as "admin<suffix>"
    And I delete the organization "subOrg1"
    Then The response status code should be 200
    When I delete the organization "subOrg2"
    Then The response status code should be 200

    Examples:
      | tenant       | suffix       |
      | carbon.super |              |
      | tenant1.com  | @tenant1.com |

  @cap:admin @feat:tenants-orgs @type:regression @legacy:ConsumerOrganizationVisibilityTestCase
  Scenario Outline: A key manager is visible only to its allowed organizations in <tenant>
    Given The system is ready
    And I register the organization local claim in tenant "<tenant>"
    And I set the organization claim of user "admin" in tenant "<tenant>" to "123-456-789"
    And I have valid access tokens as "admin<suffix>"
    When I create an organization "${UNIQUE:kmsuborg1}" with display name "KM Sub Org One" as "kmSubOrg1"
    Then The response status code should be 201
    When I create an organization "${UNIQUE:kmsuborg2}" with display name "KM Sub Org Two" as "kmSubOrg2"
    Then The response status code should be 201
    And I provision organization user "kmsub1dev" with roles "Internal/subscriber" in organization "{{kmSubOrg1External}}" in tenant "<tenant>"
    And I provision organization user "kmsub2dev" with roles "Internal/subscriber" in organization "{{kmSubOrg2External}}" in tenant "<tenant>"
    And The system is ready and I have valid devportal access token as "kmsub1dev<suffix>"
    And The system is ready and I have valid devportal access token as "kmsub2dev<suffix>"
    And I act as "admin<suffix>"
    When I create a key manager from payload "artifacts/payloads/keymanagers/wso2is.json" with allowed organizations "{{kmSubOrg1}}" as "orgKmId"
    Then The response status code should be 201
    When I act as "kmsub1dev<suffix>"
    Then I retrieve the devportal key managers until it contains "orgKmId" within 60 seconds
    When I act as "kmsub2dev<suffix>"
    Then I retrieve the devportal key managers until it does not contain "orgKmId" within 60 seconds
    When I act as "admin<suffix>"
    And I set the allowed organizations of key manager "orgKmId" to "none"
    Then The response status code should be 200
    When I act as "kmsub1dev<suffix>"
    Then I retrieve the devportal key managers until it does not contain "orgKmId" within 60 seconds

    Examples:
      | tenant       | suffix       |
      | carbon.super |              |
      | tenant1.com  | @tenant1.com |

  @cap:devportal @feat:applications @type:regression @dep:admin @legacy:ConsumerOrganizationVisibilityTestCase
  Scenario Outline: An application shared with an organization is visible only within that organization in <tenant>
    Given The system is ready
    And I register the organization local claim in tenant "<tenant>"
    And I set the organization claim of user "admin" in tenant "<tenant>" to "123-456-789"
    And I have valid access tokens as "admin<suffix>"
    When I create an organization "${UNIQUE:shareorg1}" with display name "Share Org One" as "shareOrg1"
    Then The response status code should be 201
    When I create an organization "${UNIQUE:shareorg2}" with display name "Share Org Two" as "shareOrg2"
    Then The response status code should be 201
    And I provision organization user "share1a" with roles "Internal/subscriber" in organization "{{shareOrg1External}}" in tenant "<tenant>"
    And I provision organization user "share1b" with roles "Internal/subscriber" in organization "{{shareOrg1External}}" in tenant "<tenant>"
    And I provision organization user "share2a" with roles "Internal/subscriber" in organization "{{shareOrg2External}}" in tenant "<tenant>"
    And The system is ready and I have valid devportal access token as "share1a<suffix>"
    And The system is ready and I have valid devportal access token as "share1b<suffix>"
    And The system is ready and I have valid devportal access token as "share2a<suffix>"
    When I act as "share1a<suffix>"
    And I create an application "${UNIQUE:ShareApp}" with visibility "SHARED_WITH_ORG" as "shareAppId"
    Then The response status code should be 201
    When I act as "share1b<suffix>"
    And I retrieve the application "shareAppId"
    Then The response status code should be 200
    When I act as "share2a<suffix>"
    And I retrieve the application "shareAppId"
    Then The response status code should be 403
    When I act as "share1a<suffix>"
    And I set the visibility of application "shareAppId" to "PRIVATE"
    Then The response status code should be 200
    When I act as "share1b<suffix>"
    And I retrieve the application "shareAppId"
    Then The response status code should be 403

    Examples:
      | tenant       | suffix       |
      | carbon.super |              |
      | tenant1.com  | @tenant1.com |

  # Method 7 — org policy tier applied + subscribable, plus the disallowed-tier negative. A tier NOT in the
  # org's policy is cleanly rejected with 403 ("Tier Unlimited is not allowed ... Only [Bronze] Tiers are
  # allowed") — verified live. (An earlier probe wrongly recorded 500 900967; that was an unresolved-placeholder
  # artifact in the test payload, not product behaviour — see the report.)
  @cap:admin @feat:tenants-orgs @type:regression @dep:devportal @legacy:ConsumerOrganizationVisibilityTestCase
  Scenario Outline: An organization's subscription-policy tier is applied and subscribable in <tenant>
    Given The system is ready
    And I register the organization local claim in tenant "<tenant>"
    And I set the organization claim of user "admin" in tenant "<tenant>" to "123-456-789"
    And I have valid access tokens as "admin<suffix>"
    When I create an organization "${UNIQUE:polsuborg1}" with display name "Pol Sub Org One" as "polSubOrg1"
    Then The response status code should be 201
    And I provision organization user "polpub1" with roles "Internal/creator,Internal/publisher" in organization "123-456-789" in tenant "<tenant>"
    And I provision organization user "pol1dev" with roles "Internal/subscriber" in organization "{{polSubOrg1External}}" in tenant "<tenant>"
    And The system is ready and I have valid devportal access token as "pol1dev<suffix>"
    And The system is ready and I have valid publisher access tokens as "polpub1<suffix>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "polApiId" and deployed it
    When I publish the "apis" resource with id "polApiId"
    Then The lifecycle status of API "polApiId" should be "Published"
    When I act as "polpub1<suffix>"
    And I set organization policies of API "polApiId" for organization "{{polSubOrg1}}" to tier "Bronze"
    Then The response status code should be 200
    When I act as "pol1dev<suffix>"
    Then I retrieve the devportal API "polApiId" until it contains "Bronze" within 60 seconds
    And I create an application "${UNIQUE:PolApp}" with visibility "PRIVATE" as "polAppId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "polSubBronze"
    """
    {"applicationId": "{{polAppId}}", "apiId": "{{polApiId}}", "throttlingPolicy": "Bronze"}
    """
    And I subscribe to API "polApiId" using application "polAppId" with payload "polSubBronze" as "polSubId"
    Then The response status code should be 201
    When I delete the subscription with id "polSubId"
    Then The response status code should be 200
    When I put the following JSON payload in context as "polSubUnlimited"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I attempt to subscribe to API "polApiId" using application "polAppId" with payload "polSubUnlimited"
    Then The response status code should be 403

    Examples:
      | tenant       | suffix       |
      | carbon.super |              |
      | tenant1.com  | @tenant1.com |
