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
    And I have valid access tokens as "<adminActor>"
    And I generate a unique value and store it as "acrole"
    And I provision role "<rolePrefix>{{acrole}}" in tenant "<tenant>"
    And I provision access-control user "<actorPrefix>acOwner" with roles "Internal/creator,Internal/publisher,<rolePrefix>{{acrole}}" in tenant "<tenant>"
    And I provision access-control user "<actorPrefix>acCreatorIn" with roles "Internal/creator,<rolePrefix>{{acrole}}" in tenant "<tenant>"
    And I provision access-control user "<actorPrefix>acCreatorOut" with roles "Internal/creator,Internal/publisher" in tenant "<tenant>"
    And The system is ready and I have valid publisher access tokens as "<actorPrefix>acOwner<suffix>"
    And The system is ready and I have valid publisher access tokens as "<actorPrefix>acCreatorIn<suffix>"
    And The system is ready and I have valid publisher access tokens as "<actorPrefix>acCreatorOut<suffix>"
    And I generate a unique value and store it as "acExpectedNameSuffix"

    # The primary examples retain the admin owner. The secondary example uses the allowed secondary creator as owner,
    # while keeping the no-role creator as an independent denied checker.
    Given I act as "<ownerActor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "acApiPayload"
    And I set the field "name" to "AccessControl{{acExpectedNameSuffix}}" in the payload "acApiPayload"
    When I have created an api from context payload "acApiPayload" with restricted access control for roles "<rolePrefix>{{acrole}}" as "acApiId" and deployed it
    And I publish the "apis" resource with id "acApiId"
    Then The lifecycle status of API "acApiId" should be "Published"
    And I retrieve the "apis" resource with id "acApiId"
    Then The response status code should be 200
    And The response field "accessControlRoles" should be exactly the list "<expectedRolePrefix>{{acrole}}"
    And The value of response field "name" should be "AccessControl{{acExpectedNameSuffix}}"
    And I extract response field "name" and store it as "acApiName"

    # A creator carrying the access role can view it.
    When I act as "<actorPrefix>acCreatorIn<suffix>"
    And I retrieve the "apis" resource with id "acApiId"
    Then The response status code should be 200
    And The response field "accessControlRoles" should be exactly the list "<expectedRolePrefix>{{acrole}}"
    And The value of response field "name" should be "AccessControl{{acExpectedNameSuffix}}"
    And The value of response field "name" should be "{{acApiName}}"

    # A creator WITHOUT the access role is forbidden.
    When I act as "<actorPrefix>acCreatorOut<suffix>"
    And I retrieve the "apis" resource with id "acApiId"
    Then The response status code should be 403
    And The response should contain "User is not authorized to access the API"

    Examples:
      | tenant       | suffix       | actorPrefix    | rolePrefix    | expectedRolePrefix | adminActor        | ownerActor                   |
      | carbon.super |              |                |               |                    | superTenantAdmin  | superTenantAdmin             |
      | tenant1.com  | @tenant1.com |                |               |                    | admin@tenant1.com | admin@tenant1.com            |
      | carbon.super |              | SECONDARY.COM/ | SECONDARY.COM/ | secondary.com/     | superTenantAdmin  | SECONDARY.COM/acOwner       |

  # Publisher CONTENT SEARCH is filtered by accessControl roles: two APIs sharing one scenario-unique description
  # word, both access-restricted to the same role, are counted (exactly 2) for a creator carrying that role and not
  # counted at all (exactly 0) for one who does not. Ports ContentSearchTestCase testContentSearchWithAccessControl.
  # This is a DIFFERENT observable from the by-id 403 above — a 403 on one id does not prove the API is absent from
  # the searcher's result SET. The fixture deliberately carries TWO matching APIs: it reproduces legacy's exact
  # count of 2 with a count this fixture actually justifies (legacy's 2 came from one API plus its definition
  # document, an index shape this build no longer produces — the description search here counts one hit per API), and
  # it stops the filter passing by merely capping at a single hit. The count-2 leg runs FIRST so the count-0 leg
  # cannot pass against an index that never saw the APIs.
  @cap:publisher @feat:visibility @rule:content-search @type:regression @legacy:ContentSearchTestCase
  Scenario Outline: Publisher content search counts an access-restricted API only for creators carrying the role in <tenant>
    Given The system is ready
    And I have valid access tokens as "<adminActor>"
    And I generate a unique value and store it as "acsrole"
    And I generate a unique value and store it as "acsDesc"
    And I provision role "<rolePrefix>{{acsrole}}" in tenant "<tenant>"
    And I provision user "<actorPrefix>acsOwner" with roles "Internal/creator,Internal/publisher,<rolePrefix>{{acsrole}}" in tenant "<tenant>"
    And I provision user "<actorPrefix>acsIn" with roles "Internal/creator,Internal/publisher,<rolePrefix>{{acsrole}}" in tenant "<tenant>"
    And I provision user "<actorPrefix>acsOut" with roles "Internal/creator,Internal/publisher" in tenant "<tenant>"
    And The system is ready and I have valid publisher access tokens as "<actorPrefix>acsIn<suffix>"
    And The system is ready and I have valid publisher access tokens as "<actorPrefix>acsOut<suffix>"
    And The system is ready and I have valid publisher access tokens as "<actorPrefix>acsOwner<suffix>"

    # Two APIs carrying the same unique description word, both restricted to the same access role. In the
    # secondary-store example, the role-bearing secondary actor owns both APIs; the no-role actor remains a reader.
    Given I act as "<ownerActor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "acsPayload1"
    And I set the description of context payload "acsPayload1" to "Access filtered search {{acsDesc}}"
    When I have created an api from context payload "acsPayload1" with restricted access control for roles "<rolePrefix>{{acsrole}}" as "acsApi1" and deployed it
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "acsPayload2"
    And I set the description of context payload "acsPayload2" to "Access filtered search {{acsDesc}}"
    When I have created an api from context payload "acsPayload2" with restricted access control for roles "<rolePrefix>{{acsrole}}" as "acsApi2" and deployed it

    # The creator carrying the access role is served both.
    When I act as "<actorPrefix>acsIn<suffix>"
    And I search Publisher APIs with content query "description:{{acsDesc}}" until the result count is 2 within 60 seconds
    # The creator without it is served neither.
    When I act as "<actorPrefix>acsOut<suffix>"
    And I search Publisher APIs with content query "description:{{acsDesc}}" until the result count is 0 within 60 seconds

    Examples:
      | tenant       | suffix       | adminActor        | actorPrefix    | rolePrefix    | ownerActor                   |
      | carbon.super |              | superTenantAdmin  |                |               | superTenantAdmin             |
      | tenant1.com  | @tenant1.com | admin@tenant1.com |                |               | admin@tenant1.com            |
      | carbon.super |              | superTenantAdmin  | SECONDARY.COM/ | SECONDARY.COM/ | SECONDARY.COM/acsOwner       |

  # Unlike the direct-create restriction above, this regression path changes access control on an existing API.
  # Search is through unified /search (legacy's endpoint); the authorized positive result is established before
  # the denied search so a zero cannot pass against a cold index.
  @cap:publisher @feat:visibility @rule:content-search @type:regression @legacy:ContentSearchTestCase
  Scenario Outline: Publisher unified search applies access control updated before publish in <tenant>
    Given The system is ready
    And I have valid access tokens as "<adminActor>"
    And I generate a unique value and store it as "acsUpdateRole"
    And I generate a unique value and store it as "acsUpdateDesc"
    And I provision role "<rolePrefix>{{acsUpdateRole}}" in tenant "<tenant>"
    And I provision user "<actorPrefix>acsUpdateOwner" with roles "Internal/creator,Internal/publisher,<rolePrefix>{{acsUpdateRole}}" in tenant "<tenant>"
    And I provision user "<actorPrefix>acsUpdateIn" with roles "Internal/creator,Internal/publisher,<rolePrefix>{{acsUpdateRole}}" in tenant "<tenant>"
    And I provision user "<actorPrefix>acsUpdateOut" with roles "Internal/creator,Internal/publisher" in tenant "<tenant>"
    And The system is ready and I have valid publisher access tokens as "<actorPrefix>acsUpdateIn<suffix>"
    And The system is ready and I have valid publisher access tokens as "<actorPrefix>acsUpdateOut<suffix>"
    And The system is ready and I have valid publisher access tokens as "<actorPrefix>acsUpdateOwner<suffix>"

    Given I act as "<ownerActor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "acsUpdatePayload"
    And I set the description of context payload "acsUpdatePayload" to "Access update search {{acsUpdateDesc}}"
    And I have created an api from context payload "acsUpdatePayload" as "acsUpdateApiId" and deployed it
    When I update the Publisher access-control roles of API "acsUpdateApiId" to "<rolePrefix>{{acsUpdateRole}}"
    Then The response status code should be 200
    When I publish the "apis" resource with id "acsUpdateApiId"
    Then The lifecycle status of API "acsUpdateApiId" should be "Published"

    When I act as "<actorPrefix>acsUpdateIn<suffix>"
    And I unified-search the "publisher" plane for "{{acsUpdateDesc}}" until the result count is 2 within 60 seconds
    When I act as "<actorPrefix>acsUpdateOut<suffix>"
    And I unified-search the "publisher" plane for "{{acsUpdateDesc}}" until the result count is 0 within 60 seconds

  Examples:
      | tenant       | suffix       | adminActor        | actorPrefix    | rolePrefix    | ownerActor                   |
      | carbon.super |              | superTenantAdmin  |                |               | superTenantAdmin             |
      | tenant1.com  | @tenant1.com | admin@tenant1.com |                |               | admin@tenant1.com            |
      | carbon.super |              | superTenantAdmin  | SECONDARY.COM/ | SECONDARY.COM/ | SECONDARY.COM/acsUpdateOwner |

  # Separate from the pre-publish restriction above: this proves unified search updates its authorization filter
  # when accessControl changes on an API that was already published and indexed. The positive result is established
  # before mutation, then the API is republished and both allowed/denied searches are rechecked.
  @cap:publisher @feat:visibility @rule:content-search @post-publish-access-control-search @type:regression @legacy:ContentSearchTestCase
  Scenario Outline: Publisher unified search applies access-control mutation after publish in <tenant>
    Given The system is ready
    And I have valid access tokens as "<adminActor>"
    And I generate a unique value and store it as "acsPostRole"
    And I generate a unique value and store it as "acsPostDesc"
    And I provision role "<rolePrefix>{{acsPostRole}}" in tenant "<tenant>"
    And I provision user "<actorPrefix>acsPostOwner" with roles "Internal/creator,Internal/publisher,<rolePrefix>{{acsPostRole}}" in tenant "<tenant>"
    And I provision user "<actorPrefix>acsPostIn" with roles "Internal/creator,Internal/publisher,<rolePrefix>{{acsPostRole}}" in tenant "<tenant>"
    And I provision user "<actorPrefix>acsPostOut" with roles "Internal/creator,Internal/publisher" in tenant "<tenant>"
    And The system is ready and I have valid publisher access tokens as "<actorPrefix>acsPostIn<suffix>"
    And The system is ready and I have valid publisher access tokens as "<actorPrefix>acsPostOut<suffix>"
    And The system is ready and I have valid publisher access tokens as "<actorPrefix>acsPostOwner<suffix>"

    Given I act as "<ownerActor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "acsPostPayload"
    And I set the description of context payload "acsPostPayload" to "Post-publish access search {{acsPostDesc}}"
    And I have created an api from context payload "acsPostPayload" as "acsPostApiId" and deployed it
    And the "apis" resource "acsPostApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "acsPostApiId"
    Then The lifecycle status of API "acsPostApiId" should be "Published"

    # Warm and prove the unrestricted Publisher index includes the API for both principals before the access change.
    When I act as "<actorPrefix>acsPostIn<suffix>"
    And I unified-search the "publisher" plane for "{{acsPostDesc}}" until the result count is 2 within 60 seconds
    When I act as "<actorPrefix>acsPostOut<suffix>"
    And I unified-search the "publisher" plane for "{{acsPostDesc}}" until the result count is 2 within 60 seconds

    Given I act as "<ownerActor>"
    When I update the Publisher access-control roles of API "acsPostApiId" to "<rolePrefix>{{acsPostRole}}"
    Then The response status code should be 200
    When I publish the "apis" resource with id "acsPostApiId"
    Then The lifecycle status of API "acsPostApiId" should be "Published"

    When I act as "<actorPrefix>acsPostIn<suffix>"
    And I unified-search the "publisher" plane for "{{acsPostDesc}}" until the result count is 2 within 60 seconds
    When I act as "<actorPrefix>acsPostOut<suffix>"
    And I unified-search the "publisher" plane for "{{acsPostDesc}}" until the result count is 0 within 60 seconds

    Examples:
      | tenant       | suffix       | adminActor        | actorPrefix    | rolePrefix    | ownerActor                   |
      | carbon.super |              | superTenantAdmin  |                |               | superTenantAdmin             |
      | tenant1.com  | @tenant1.com | admin@tenant1.com |                |               | admin@tenant1.com            |
      | carbon.super |              | superTenantAdmin  | SECONDARY.COM/ | SECONDARY.COM/ | SECONDARY.COM/acsPostOwner   |

  # No access control: every creator/publisher can view it (200). Ports
  # testAPIAdditionWithoutAccessControlRestriction.
  @cap:publisher @feat:visibility @type:regression @legacy:PublisherAccessControlTestCase
  Scenario Outline: An API without access control is visible to every creator in <tenant>
    Given The system is ready
    And I have valid access tokens as "<adminActor>"
    And I provision access-control user "<actorPrefix>nacCreatorA" with roles "Internal/creator" in tenant "<tenant>"
    And I provision access-control user "<actorPrefix>nacCreatorB" with roles "Internal/publisher" in tenant "<tenant>"
    And I provision access-control user "<actorPrefix>nacOwner" with roles "Internal/creator,Internal/publisher" in tenant "<tenant>"
    And The system is ready and I have valid publisher access tokens as "<actorPrefix>nacCreatorA<suffix>"
    And The system is ready and I have valid publisher access tokens as "<actorPrefix>nacCreatorB<suffix>"
    And The system is ready and I have valid publisher access tokens as "<actorPrefix>nacOwner<suffix>"
    And I generate a unique value and store it as "nacExpectedNameSuffix"

    Given I act as "<ownerActor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "nacApiPayload"
    And I set the field "name" to "NoAcl{{nacExpectedNameSuffix}}" in the payload "nacApiPayload"
    When I have created an api from context payload "nacApiPayload" as "nacApiId" and deployed it
    And I publish the "apis" resource with id "nacApiId"
    Then The lifecycle status of API "nacApiId" should be "Published"
    And I retrieve the "apis" resource with id "nacApiId"
    Then The response status code should be 200
    And The value of response field "name" should be "NoAcl{{nacExpectedNameSuffix}}"
    And I extract response field "name" and store it as "nacApiName"
    When I act as "<actorPrefix>nacCreatorA<suffix>"
    And I retrieve the "apis" resource with id "nacApiId"
    Then The response status code should be 200
    And The value of response field "name" should be "NoAcl{{nacExpectedNameSuffix}}"
    And The value of response field "name" should be "{{nacApiName}}"
    When I act as "<actorPrefix>nacCreatorB<suffix>"
    And I retrieve the "apis" resource with id "nacApiId"
    Then The response status code should be 200
    And The value of response field "name" should be "NoAcl{{nacExpectedNameSuffix}}"
    And The value of response field "name" should be "{{nacApiName}}"

    Examples:
      | tenant       | suffix       | actorPrefix    | adminActor        | ownerActor                    |
      | carbon.super |              |                | superTenantAdmin  | superTenantAdmin              |
      | tenant1.com  | @tenant1.com |                | admin@tenant1.com | admin@tenant1.com             |
      | carbon.super |              | SECONDARY.COM/ | superTenantAdmin  | SECONDARY.COM/nacOwner        |

  # Restricted access (role1) + PUBLIC store visibility: access control does not touch the store, so a plain
  # subscriber (no role1) still discovers it in the store. Ports
  # testGetPublicAPIFromStoreWithRestrictedPublisherAccess.
  @cap:publisher @feat:visibility @type:regression @dep:devportal @legacy:PublisherAccessControlTestCase
  Scenario Outline: A restricted-access publicly-visible API is still discoverable in the store in <tenant>
    Given The system is ready
    And I have valid access tokens as "<adminActor>"
    And I generate a unique value and store it as "acpvrole"
    And I provision role "<rolePrefix>{{acpvrole}}" in tenant "<tenant>"
    And I provision access-control user "<actorPrefix>acpvOwner" with roles "Internal/creator,Internal/publisher,<rolePrefix>{{acpvrole}}" in tenant "<tenant>"
    And I provision access-control user "<actorPrefix>acpvSub" with roles "Internal/subscriber" in tenant "<tenant>"
    And The system is ready and I have valid devportal access token as "<actorPrefix>acpvSub<suffix>"
    And The system is ready and I have valid publisher access tokens as "<ownerActor>"

    Given I act as "<ownerActor>"
    When I have created an api from "artifacts/payloads/create_apim_test_api.json" with restricted access control for roles "<rolePrefix>{{acpvrole}}" and public visibility as "acpvApiId" and deployed it
    And I publish the "apis" resource with id "acpvApiId"
    Then The lifecycle status of API "acpvApiId" should be "Published"
    When I act as "<actorPrefix>acpvSub<suffix>"
    Then I retrieve the devportal "api" of API "acpvApiId" until the response status code becomes 200 within 60 seconds
    And The value of response field "id" should be "{{acpvApiId}}"

    Examples:
      | tenant       | suffix       | actorPrefix    | rolePrefix    | adminActor        | ownerActor                    |
      | carbon.super |              |                |               | superTenantAdmin  | superTenantAdmin              |
      | tenant1.com  | @tenant1.com |                |               | admin@tenant1.com | admin@tenant1.com             |
      | carbon.super |              | SECONDARY.COM/ | SECONDARY.COM/ | superTenantAdmin  | SECONDARY.COM/acpvOwner      |

  # Public access + RESTRICTED store visibility (role1): a publisher-role user WITHOUT role1 still sees it in the
  # store (the publisher bypass), while a plain subscriber without role1 does not (404). Ports
  # testCheckPublisherRoleCanViewRestrictedVisibilityAPIs.
  @cap:devportal @feat:visibility @type:regression @dep:publisher @legacy:PublisherAccessControlTestCase
  Scenario Outline: A publisher-role user sees a restricted-visibility API in the store while a plain subscriber does not in <tenant>
    Given The system is ready
    And I have valid access tokens as "<adminActor>"
    And I generate a unique value and store it as "pvrole"
    And I provision access-control visibility role "{{pvrole}}" in tenant "<tenant>" for store "<roleStore>"
    And I provision access-control user "<actorPrefix>pvPubSub" with roles "Internal/publisher,Internal/subscriber" in tenant "<tenant>"
    And I provision access-control user "<actorPrefix>pvSub" with roles "Internal/subscriber" in tenant "<tenant>"
    And I provision access-control user "<actorPrefix>pvOwner" with roles "Internal/creator,Internal/publisher" in tenant "<tenant>"
    And The system is ready and I have valid devportal access token as "<actorPrefix>pvPubSub<suffix>"
    And The system is ready and I have valid devportal access token as "<actorPrefix>pvSub<suffix>"
    And The system is ready and I have valid publisher access tokens as "<ownerActor>"

    Given I act as "<ownerActor>"
    When I have created an api from "artifacts/payloads/create_apim_test_api.json" with restricted visibility for roles "<rolePrefix>{{pvrole}}" as "pvApiId" and deployed it
    And I publish the "apis" resource with id "pvApiId"
    Then The lifecycle status of API "pvApiId" should be "Published"
    # Publisher-role user (no visibility role) — visible via the bypass.
    When I act as "<actorPrefix>pvPubSub<suffix>"
    Then I retrieve the devportal "api" of API "pvApiId" until the response status code becomes 200 within 60 seconds
    And The value of response field "id" should be "{{pvApiId}}"
    # Plain subscriber (no visibility role) — hidden.
    When I act as "<actorPrefix>pvSub<suffix>"
    Then I retrieve the devportal "api" of API "pvApiId" until the response status code becomes 404 within 60 seconds

    Examples:
      | tenant       | suffix       | actorPrefix    | rolePrefix    | roleStore      | adminActor        | ownerActor                    |
      | carbon.super |              |                |               |                | superTenantAdmin  | superTenantAdmin              |
      | tenant1.com  | @tenant1.com |                |               |                | admin@tenant1.com | admin@tenant1.com             |
      | carbon.super |              | SECONDARY.COM/ | SECONDARY.COM/ | SECONDARY.COM   | superTenantAdmin  | SECONDARY.COM/pvOwner         |

  # Store visibility restricted to the built-in admin role: a Store actor with Internal/publisher must see the
  # API through the Publisher privilege bypass, while a plain subscriber without admin must receive 404. This is
  # distinct from the custom-role bypass above and ports APIVisibilityByRoleTestCase's direct admin-role Store check.
  @cap:devportal @feat:visibility @type:regression @dep:publisher @legacy:APIVisibilityByRoleTestCase
  Scenario Outline: Publisher privilege bypasses admin-restricted Store visibility for <tenant> actors
    Given The system is ready
    And I have valid access tokens as "<adminActor>"
    And I provision access-control user "<actorPrefix>roleVisPublisher" with roles "Internal/publisher,Internal/subscriber" in tenant "<tenant>"
    And I provision access-control user "<actorPrefix>roleVisSubscriber" with roles "Internal/subscriber" in tenant "<tenant>"
    And The system is ready and I have valid devportal access token as "<actorPrefix>roleVisPublisher<suffix>"
    And The system is ready and I have valid devportal access token as "<actorPrefix>roleVisSubscriber<suffix>"
    And The system is ready and I have valid publisher access tokens as "<ownerActor>"

    Given I act as "<ownerActor>"
    When I have created an api from "artifacts/payloads/create_apim_test_api.json" with restricted visibility for roles "admin" as "roleVisAdminApiId" and deployed it
    And I publish the "apis" resource with id "roleVisAdminApiId"
    Then The lifecycle status of API "roleVisAdminApiId" should be "Published"

    When I act as "<actorPrefix>roleVisPublisher<suffix>"
    Then I retrieve the devportal "api" of API "roleVisAdminApiId" until the response status code becomes 200 within 60 seconds
    And The value of response field "id" should be "{{roleVisAdminApiId}}"
    When I act as "<actorPrefix>roleVisSubscriber<suffix>"
    Then I retrieve the devportal "api" of API "roleVisAdminApiId" until the response status code becomes 404 within 60 seconds

    Examples:
      | tenant       | suffix       | actorPrefix    | adminActor        | ownerActor        |
      | carbon.super |              |                | superTenantAdmin  | superTenantAdmin  |
      | tenant1.com  | @tenant1.com |                | admin@tenant1.com | admin@tenant1.com |
      | carbon.super |              | SECONDARY.COM/ | superTenantAdmin  | superTenantAdmin  |
      | tenant1.com  | @tenant1.com | SECONDARY.COM/ | admin@tenant1.com | admin@tenant1.com |

  # The legacy combined case keeps Publisher access control and Store visibility on ONE API. First the API is
  # public in the Store and restricted in Publisher; after publish, Store visibility is changed to a role, then
  # Publisher and Store reads are checked against the updated state. This verifies the controls compose through
  # mutation and republish, beyond the independent cases above.
  @cap:publisher @feat:visibility @type:regression @dep:devportal @legacy:PublisherAccessControlTestCase
  Scenario Outline: Publisher ACL and Store visibility updates compose on the same API in <tenant>
    Given The system is ready
    And I have valid access tokens as "<adminActor>"
    And I generate a unique value and store it as "comboaclrole"
    And I generate a unique value and store it as "comboStoreRole"
    And I provision role "<rolePrefix>{{comboaclrole}}" in tenant "<tenant>"
    And I provision access-control visibility role "{{comboStoreRole}}" in tenant "<tenant>" for store "<roleStore>"
    And I provision access-control user "<actorPrefix>comboOwner" with roles "Internal/creator,Internal/publisher,<rolePrefix>{{comboaclrole}}" in tenant "<tenant>"
    And I provision access-control user "<actorPrefix>comboAllowed" with roles "Internal/creator,<rolePrefix>{{comboaclrole}},<rolePrefix>{{comboStoreRole}}" in tenant "<tenant>"
    And I provision access-control user "<actorPrefix>comboDenied" with roles "Internal/publisher,Internal/subscriber" in tenant "<tenant>"
    And I provision access-control user "<actorPrefix>comboStoreSub" with roles "Internal/subscriber,<rolePrefix>{{comboStoreRole}}" in tenant "<tenant>"
    And The system is ready and I have valid publisher access tokens as "<actorPrefix>comboAllowed<suffix>"
    And The system is ready and I have valid publisher access tokens as "<actorPrefix>comboDenied<suffix>"
    And The system is ready and I have valid devportal access token as "<actorPrefix>comboStoreSub<suffix>"
    And The system is ready and I have valid publisher access tokens as "<actorPrefix>comboOwner<suffix>"
    And The system is ready and I have valid publisher access tokens as "<ownerActor>"
    And I generate a unique value and store it as "comboExpectedNameSuffix"

    Given I act as "<ownerActor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "comboApiPayload"
    And I set the field "name" to "AclStore{{comboExpectedNameSuffix}}" in the payload "comboApiPayload"
    When I have created an api from context payload "comboApiPayload" with restricted access control for roles "<rolePrefix>{{comboaclrole}}" as "comboApiId" and deployed it
    And I publish the "apis" resource with id "comboApiId"
    Then The lifecycle status of API "comboApiId" should be "Published"

    # Before the Store update, matching Publisher creator is allowed and a Publisher without the ACL role is denied.
    When I act as "<actorPrefix>comboAllowed<suffix>"
    And I retrieve the "apis" resource with id "comboApiId"
    Then The response status code should be 200
    And The response field "accessControlRoles" should be exactly the list "<expectedRolePrefix>{{comboaclrole}}"
    And The value of response field "name" should be "AclStore{{comboExpectedNameSuffix}}"
    And I extract response field "name" and store it as "comboApiName"
    When I act as "<actorPrefix>comboDenied<suffix>"
    And I retrieve the "apis" resource with id "comboApiId"
    Then The response status code should be 403

    # Update Store visibility on that same API, preserving its Publisher ACL, then republish the new state.
    Given I act as "<adminActor>"
    When I set the visibility roles of API "comboApiId" to "<rolePrefix>{{comboStoreRole}}"
    Then The response status code should be 200
    And I retrieve the "apis" resource with id "comboApiId"
    Then The response status code should be 200
    And The response field "visibleRoles" should be exactly the list "<rolePrefix>{{comboStoreRole}}"
    And The response field "accessControlRoles" should be exactly the list "<expectedRolePrefix>{{comboaclrole}}"
    And The value of response field "visibility" should be "RESTRICTED"
    And The value of response field "accessControl" should be "RESTRICTED"
    When I publish the "apis" resource with id "comboApiId"
    Then The lifecycle status of API "comboApiId" should be "Published"

    When I act as "<actorPrefix>comboAllowed<suffix>"
    And I retrieve the "apis" resource with id "comboApiId"
    Then The response status code should be 200
    And The response field "accessControlRoles" should be exactly the list "<expectedRolePrefix>{{comboaclrole}}"
    And The value of response field "name" should be "AclStore{{comboExpectedNameSuffix}}"
    And The value of response field "name" should be "{{comboApiName}}"
    When I act as "<actorPrefix>comboDenied<suffix>"
    And I retrieve the "apis" resource with id "comboApiId"
    Then The response status code should be 403
    When I act as "<actorPrefix>comboStoreSub<suffix>"
    Then I retrieve the devportal "api" of API "comboApiId" until the response status code becomes 200 within 60 seconds
    And The value of response field "id" should be "{{comboApiId}}"

    Examples:
      | tenant       | suffix       | actorPrefix    | rolePrefix    | expectedRolePrefix | roleStore      | adminActor        | ownerActor                    |
      | carbon.super |              |                |               |                    |                | superTenantAdmin  | superTenantAdmin              |
      | tenant1.com  | @tenant1.com |                |               |                    |                | admin@tenant1.com | admin@tenant1.com             |
      | carbon.super |              | SECONDARY.COM/ | SECONDARY.COM/ | secondary.com/     | SECONDARY.COM   | superTenantAdmin  | SECONDARY.COM/comboOwner      |
