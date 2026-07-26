@cleanup
Feature: Publisher Access Control & Store Visibility

  Ports PublisherAccessControlTestCase: an API's publisher-plane ACCESS CONTROL (who among creators/publishers may
  view and edit it) is independent of its DevPortal store VISIBILITY (who among consumers may discover it), and the
  two interact. A restricted-access API is 403 in the Publisher to a creator lacking the access role, while store
  visibility is unaffected by access control; a publisher-role user can always view a restricted-visibility API in
  the store (the publisher bypass).

  Custom roles and users are provisioned inline per scenario (SOAP addRole / addUser); unique lowercase role names
  (WSO2 role validation matches the stored case) and unique user names keep parallel scenarios isolated. Restricted
  access in the Publisher is enforced as 403 ("User is not authorized to access the API"), asserted exactly; store
  hiding is 404. Run in both tenants.

  # Restricted access control (role1): the admin owner and a creator WITH role1 view it (200); a creator WITHOUT
  # role1 is 403. Ports testAPIAdditionWithAccessControlRestriction.
  @cap:publisher @feat:visibility @type:regression @legacy:PublisherAccessControlTestCase
  Scenario Outline: A restricted-access API is visible in the Publisher only to creators carrying the access role in <tenant>
    Given The system is ready
    And I have valid access tokens as "admin<suffix>"
    And I generate a unique value and store it as "acrole"
    And I provision role "{{acrole}}" in tenant "<tenant>"
    And I provision user "acCreatorIn" with roles "Internal/creator,{{acrole}}" in tenant "<tenant>"
    And I provision user "acCreatorOut" with roles "Internal/creator,Internal/publisher" in tenant "<tenant>"
    And The system is ready and I have valid publisher access tokens as "acCreatorIn<suffix>"
    And The system is ready and I have valid publisher access tokens as "acCreatorOut<suffix>"

    # The admin owns the restricted-access API.
    Given I act as "admin<suffix>"
    When I have created an api from "artifacts/payloads/create_apim_test_api.json" with restricted access control for roles "{{acrole}}" as "acApiId" and deployed it
    And I retrieve the "apis" resource with id "acApiId"
    Then The response status code should be 200
    And The response should contain "{{acrole}}"

    # A creator carrying the access role can view it.
    When I act as "acCreatorIn<suffix>"
    And I retrieve the "apis" resource with id "acApiId"
    Then The response status code should be 200

    # A creator WITHOUT the access role is forbidden.
    When I act as "acCreatorOut<suffix>"
    And I retrieve the "apis" resource with id "acApiId"
    Then The response status code should be 403
    And The response should contain "User is not authorized to access the API"

    Examples:
      | tenant       | suffix       |
      | carbon.super |              |
      | tenant1.com  | @tenant1.com |

  # No access control: every creator/publisher can view it (200). Ports
  # testAPIAdditionWithoutAccessControlRestriction.
  @cap:publisher @feat:visibility @type:regression @legacy:PublisherAccessControlTestCase
  Scenario Outline: An API without access control is visible to every creator in <tenant>
    Given The system is ready
    And I have valid access tokens as "admin<suffix>"
    And I provision user "nacCreatorA" with roles "Internal/creator" in tenant "<tenant>"
    And I provision user "nacCreatorB" with roles "Internal/publisher" in tenant "<tenant>"
    And The system is ready and I have valid publisher access tokens as "nacCreatorA<suffix>"
    And The system is ready and I have valid publisher access tokens as "nacCreatorB<suffix>"

    Given I act as "admin<suffix>"
    When I have created an api from "artifacts/payloads/create_apim_test_api.json" as "nacApiId" and deployed it
    When I act as "nacCreatorA<suffix>"
    And I retrieve the "apis" resource with id "nacApiId"
    Then The response status code should be 200
    When I act as "nacCreatorB<suffix>"
    And I retrieve the "apis" resource with id "nacApiId"
    Then The response status code should be 200

    Examples:
      | tenant       | suffix       |
      | carbon.super |              |
      | tenant1.com  | @tenant1.com |

  # Restricted access (role1) + PUBLIC store visibility: access control does not touch the store, so a plain
  # subscriber (no role1) still discovers it in the store. Ports
  # testGetPublicAPIFromStoreWithRestrictedPublisherAccess.
  @cap:publisher @feat:visibility @type:regression @dep:devportal @legacy:PublisherAccessControlTestCase
  Scenario Outline: A restricted-access publicly-visible API is still discoverable in the store in <tenant>
    Given The system is ready
    And I have valid access tokens as "admin<suffix>"
    And I generate a unique value and store it as "acpvrole"
    And I provision role "{{acpvrole}}" in tenant "<tenant>"
    And I provision user "acpvSub" with roles "Internal/subscriber" in tenant "<tenant>"
    And The system is ready and I have valid devportal access token as "acpvSub<suffix>"

    Given I act as "admin<suffix>"
    When I have created an api from "artifacts/payloads/create_apim_test_api.json" with restricted access control for roles "{{acpvrole}}" and public visibility as "acpvApiId" and deployed it
    And I publish the "apis" resource with id "acpvApiId"
    Then The lifecycle status of API "acpvApiId" should be "Published"
    When I act as "acpvSub<suffix>"
    Then I retrieve the devportal "api" of API "acpvApiId" until the response status code becomes 200 within 60 seconds

    Examples:
      | tenant       | suffix       |
      | carbon.super |              |
      | tenant1.com  | @tenant1.com |

  # Public access + RESTRICTED store visibility (role1): a publisher-role user WITHOUT role1 still sees it in the
  # store (the publisher bypass), while a plain subscriber without role1 does not (404). Ports
  # testCheckPublisherRoleCanViewRestrictedVisibilityAPIs.
  @cap:devportal @feat:visibility @type:regression @dep:publisher @legacy:PublisherAccessControlTestCase
  Scenario Outline: A publisher-role user sees a restricted-visibility API in the store while a plain subscriber does not in <tenant>
    Given The system is ready
    And I have valid access tokens as "admin<suffix>"
    And I generate a unique value and store it as "pvrole"
    And I provision store-visibility role "{{pvrole}}" in tenant "<tenant>"
    And I provision user "pvPubSub" with roles "Internal/publisher,Internal/subscriber" in tenant "<tenant>"
    And I provision user "pvSub" with roles "Internal/subscriber" in tenant "<tenant>"
    And The system is ready and I have valid devportal access token as "pvPubSub<suffix>"
    And The system is ready and I have valid devportal access token as "pvSub<suffix>"

    Given I act as "admin<suffix>"
    When I have created an api from "artifacts/payloads/create_apim_test_api.json" with restricted visibility for roles "{{pvrole}}" as "pvApiId" and deployed it
    And I publish the "apis" resource with id "pvApiId"
    Then The lifecycle status of API "pvApiId" should be "Published"
    # Publisher-role user (no visibility role) — visible via the bypass.
    When I act as "pvPubSub<suffix>"
    Then I retrieve the devportal "api" of API "pvApiId" until the response status code becomes 200 within 60 seconds
    # Plain subscriber (no visibility role) — hidden.
    When I act as "pvSub<suffix>"
    Then I retrieve the devportal "api" of API "pvApiId" until the response status code becomes 404 within 60 seconds

    Examples:
      | tenant       | suffix       |
      | carbon.super |              |
      | tenant1.com  | @tenant1.com |
