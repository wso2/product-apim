@cleanup
Feature: DevPortal Application Group Sharing

  DevPortal-plane organization/group sharing: an application carrying a `groups` field is shared with that
  group, the field is persisted on the application, and members of the group — who are NOT the owner — get a
  defined set of rights over it. The default container silently drops `groups` (so devportal/applications
  deliberately does NOT assert it); this feature runs in the IntegrationV2-CustomAuthHeaderAndAppSharing
  block, whose overlay enables `[apim.devportal] enable_application_sharing` with
  `application_sharing_type = "default"`.

  How membership is established, and the trap in it. The server resolves a caller's group through
  DefaultGroupIDExtractorImpl, reading the local claim `http://wso2.org/claims/organization`; an application is
  shared with that group when its `groups` field holds the SAME string. But the extractor has TWO methods that
  format the group DIFFERENTLY, and which one runs is decided by config:

    getGroupingIdentifiers      (single-group)  -> "<tenantDomain>/<claimValue>"
    getGroupingIdentifierList   (multi-group)   -> "<claimValue>", with NO tenant prefix

  Enabling `enable_application_sharing` writes a `<GroupingExtractor>` into api-manager.xml, and
  APIUtil.isMultiGroupAppSharingEnabled() returns true for ANY extractor implementing NewPostLoginExecutor —
  which DefaultGroupIDExtractorImpl does. So this block always takes the MULTI-group path and the group is the
  BARE claim value. Sharing with the tenant-prefixed form instead stores a groupId no member can ever match:
  verified — every group-member assertion below then returns 403 while the negatives still pass, so the
  scenario would look merely strict rather than broken. Runs as DevPortal consumers in both the super tenant
  and tenant1.com. Teardown via the per-scenario cleanup hook (the create steps register the applications).

  @cap:devportal @feat:applications @type:regression @rule:org-sharing @legacy:ApplicationCreationTestCase
  Scenario Outline: An application can be shared with an organization group as <actor>
    Given The system is ready and I have valid devportal access token as "<actor>"

    # Create the application
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    And I extract response field "name" and store it as "shareAppName"

    # Share it with organization "org1" AND genuinely change the application: with sharing enabled the groups
    # field is persisted (the default container would silently drop it). The description and tier asserted here
    # differ from the ones the create payload used — the earlier version of this check re-sent the create
    # description ("Test application for scenarios") and the create tier (Unlimited), so it passed whether or
    # not the update was applied at all. Ports the real edit of
    # ApplicationSharingTestCase#testEditApplicationByApplicationOwner, which changes both.
    When I put JSON payload from file "artifacts/payloads/update_apim_test_app.json" in context as "appUpdatePayload"
    And I set the field "name" to "{{shareAppName}}" in the payload "appUpdatePayload"
    And I set the field "description" to "This app has been edited" in the payload "appUpdatePayload"
    And I set the field "throttlingPolicy" to "10PerMin" in the payload "appUpdatePayload"
    And I update the application "createdAppId" with payload "appUpdatePayload"
    Then The response status code should be 200
    And The response should contain "org1"

    # Re-read the application: the changed description and tier are persisted, not merely echoed by the PUT.
    When I retrieve the application with id "createdAppId"
    Then The response status code should be 200
    And The response should contain "This app has been edited"
    And The response should contain "10PerMin"

    Examples:
      | actor                       |
      | subscriberUser              |
      | subscriberUser@tenant1.com  |

  # A groupId supplied at CREATE time (not only via a later update) must be honoured and echoed back on the
  # created application. Ports ApplicationTestCase#testAddApplicationWithGroupId — which never ran upstream:
  # its @Test annotation is commented out (with a note to re-enable it after a carbon-apimgt upgrade), so this
  # is the first time the create-time groupId is actually verified.
  @cap:devportal @feat:applications @type:regression @rule:org-sharing @legacy:ApplicationTestCase
  Scenario Outline: A groupId supplied at application creation is echoed back as <actor>
    Given The system is ready and I have valid devportal access token as "<actor>"
    And I generate a unique value and store it as "createGroup"
    When I create an application "${UNIQUE:GroupIdApp}" shared with group "{{createGroup}}" as "groupIdAppId"
    Then The response status code should be 201
    And The response should contain "{{createGroup}}"
    # Persisted, not just echoed by the create response.
    When I retrieve the application "groupIdAppId"
    Then The response status code should be 200
    And The response should contain "{{createGroup}}"

    Examples:
      | actor                       |
      | subscriberUser              |
      | subscriberUser@tenant1.com  |

  # The rights a GROUP MEMBER who is not the owner holds over a shared application: they may VIEW it, they may
  # NOT update it (403). Whether they may REVOKE its API keys is not asserted here — see the TODO below. Ports the cross-user half of ApplicationSharingTestCase
  # — testEditApplicationByUserInApplicationGroup, testAPIKeyRevocationBySharedUser and
  # testOpaqueAPIKeyRevocationBySharedUser — none of which v2 covered: every application and api-key operation
  # ran as the OWNER, which cannot distinguish "shared users may do this" from "anyone authenticated may".
  #
  # Two legacy caveats this replaces rather than mirrors:
  #   * testAPIKeyRevocationBySharedUser hand-mints its API key with a private JWTGenerator instead of the
  #     product's generate endpoint, and then calls revoke with NO assertion at all on the result — the only
  #     asserted thing in the method is the application's name. Here the key is generated through the product
  #     and the revoke's status is asserted.
  #   * testOpaqueAPIKeyRevocationBySharedUser is commented out in its entirety (the whole method sits inside a
  #     /** ... */ block), so the opaque generate/list/revoke-by-keyUUID path was never exercised upstream.
  @cap:devportal @feat:applications @type:regression @rule:org-sharing @legacy:ApplicationSharingTestCase
  Scenario Outline: A group member can view a shared application and list its keys but cannot update it in <tenant>
    Given The system is ready
    And I have valid access tokens as "admin<suffix>"
    And I generate a unique value and store it as "shareGroup"
    And I provision user "shareOwner" with roles "Internal/subscriber" in tenant "<tenant>"
    And I provision user "shareMember" with roles "Internal/subscriber" in tenant "<tenant>"
    And I provision user "shareOutsider" with roles "Internal/subscriber" in tenant "<tenant>"
    # Owner and member share one organization claim, so both resolve to the group "{{shareGroup}}".
    # The outsider is given a DIFFERENT organization, which is what makes the positives statements about group
    # membership rather than about being authenticated at all.
    And I set the user claim "http://wso2.org/claims/organization" to "{{shareGroup}}" for user "shareOwner" in tenant "<tenant>"
    And I set the user claim "http://wso2.org/claims/organization" to "{{shareGroup}}" for user "shareMember" in tenant "<tenant>"
    And I set the user claim "http://wso2.org/claims/organization" to "outsider{{shareGroup}}" for user "shareOutsider" in tenant "<tenant>"
    And The system is ready and I have valid devportal access token as "shareOwner<suffix>"
    And The system is ready and I have valid devportal access token as "shareMember<suffix>"
    And The system is ready and I have valid devportal access token as "shareOutsider<suffix>"

    # The owner creates an application shared with the group.
    When I act as "shareOwner<suffix>"
    And I create an application "${UNIQUE:SharedApp}" shared with group "{{shareGroup}}" as "sharedAppId"
    Then The response status code should be 201
    And I extract response field "name" and store it as "sharedAppName"

    # A GROUP MEMBER may VIEW it, and sees the owner's application (same name), not a copy of their own.
    When I act as "shareMember<suffix>"
    And I retrieve the application "sharedAppId"
    Then The response status code should be 200
    And The response should contain "{{sharedAppName}}"

    # ...but may NOT update it: update is owner-only, so the member is refused 403.
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "memberEditPayload"
    And I set the field "name" to "{{sharedAppName}}" in the payload "memberEditPayload"
    And I set the field "description" to "This app has been edited by the group member" in the payload "memberEditPayload"
    And I update the application "sharedAppId" with payload "memberEditPayload"
    Then The response status code should be 403

    # The owner's application is untouched by the refused update.
    When I act as "shareOwner<suffix>"
    And I retrieve the application "sharedAppId"
    Then The response status code should be 200
    And The response should not contain "This app has been edited by the group member"

    # A NON-member is refused the view outright — the control that makes the member's 200 mean "shared".
    When I act as "shareOutsider<suffix>"
    And I retrieve the application "sharedAppId"
    Then The response status code should be 403

    # API-KEY VISIBILITY FOR THE SHARED USER. The owner generates an application API key; the member then
    # lists. Listing and revocation are authorised DIFFERENTLY, which is what the steps below pin — the
    # revocation half itself is NOT asserted (see the TODO after this block).
    When I act as "shareOwner<suffix>"
    And I put the following JSON payload in context as "sharedKeyGenPayload"
    """
    {"keyName": "SharedRevokeKey", "validityPeriod": 3600, "additionalProperties": {"permittedIP": "", "permittedReferer": ""}}
    """
    And I request an api key for application id "sharedAppId" using payload "sharedKeyGenPayload"
    Then The response status code should be 200

    # The OWNER lists the key to obtain its keyUUID.
    And I retrieve the api key UUID for application id "sharedAppId" as "sharedKeyUuid"
    Then The response status code should be 200

    # The member is AUTHORISED for the application (the listing returns 200, not 403) and yet sees NO keys:
    # getAppBoundAPIKeys authorises via the shared-application check but then queries the keys scoped to the
    # REQUESTING username, so the owner's key is invisible to them. Pinned because it is the asymmetry that
    # decides how the revocation below has to be driven — and it is why legacy's
    # testOpaqueAPIKeyRevocationBySharedUser, which has the member discover the UUID from their own listing,
    # could not have worked as written.
    When I act as "shareMember<suffix>"
    And I retrieve the api keys of application "sharedAppId"
    Then The response status code should be 200
    And The api key list should contain exactly 0 entries

    # TODO(coverage): a group MEMBER revoking the OWNER's api key by UUID is not asserted here.
    # WHAT THE MISSING SCENARIO MUST ASSERT, once the intended contract is confirmed: the member calls
    # POST /applications/{id}/api-keys/PRODUCTION/revoke with the OWNER's keyUUID, and the outcome is pinned
    # EXACTLY — either it succeeds (200, and the owner's key then lists as revoked from both the member's and
    # the owner's view) or it is refused with a definite status and code. Both halves matter: the member's own
    # key list is already asserted above, so only the cross-user leg is open.
    # OBSERVED TODAY, both tenants: that call answers 500, so neither contract can be pinned yet. Decide the
    # intended behaviour first — do not assert 500, which would freeze today's response as the contract.

    Examples:
      | tenant       | suffix       |
      | carbon.super |              |
      | tenant1.com  | @tenant1.com |
