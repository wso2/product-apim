@cleanup
Feature: DevPortal Application Management

  DevPortal-plane application lifecycle: create an application, retrieve it, update it (name/description/
  throttling), and delete it. Runs as a DevPortal consumer (subscriber-role) in both the super tenant and
  tenant1.com. Key generation and token issuance are covered by key-manager/oauth-keys. Teardown via the
  per-scenario cleanup hook (the create step registers the application).

  Note: cross-tenant organization (group) sharing is NOT asserted here — it requires group-sharing enabled in
  the server config (claim/`enable_cross_tenant_group_sharing`), which the default container does not carry;
  the default server silently drops the `groups` field. That assertion belongs in a feature with its own
  group-sharing TOML overlay, like gateway/custom-auth-header has its own block.

  # Also tags ApplicationCallbackURLTestCase (commented out of the legacy testng.xml). Its observable — "create an
  # application with callbackUrl X, then the app list entry's callbackUrl equals X" — is NOT PORTABLE: the DevPortal
  # store v1 ApplicationDTO has NO callbackUrl field on this build (verified against the shipped
  # api#am#devportal.war); callbackUrl exists only on the KEY (ApplicationKeyDTO /
  # ApplicationKeyGenerateRequestDTO). The product removed the application-level field, which is consistent with the
  # class being commented out AND with two of its four methods being @Test(enabled=false) noting that callback-URL
  # validation was removed. So this lifecycle scenario carries the tag for the class's create/update arc, but the
  # callback-URL assertion itself has no product surface left to assert. The KEY-level callbackUrl (including the
  # cross-owner overwrite regression) is covered in key-manager/oauth_keys.feature — deliberately NOT substituted
  # here, since a key field is not the application field the legacy asserted.
  @cap:devportal @feat:applications @type:smoke @legacy:ApplicationCreationTestCase @legacy:ApplicationCallbackURLTestCase
  Scenario Outline: Create, update and delete an application as <actor>
    Given The system is ready and I have valid devportal access token as "<actor>"

    # Create the application
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I retrieve the application with id "createdAppId"
    Then The response status code should be 200

    # Update the application. The description and tier asserted here must DIFFER from the create payload's:
    # this check previously re-asserted "Test application for scenarios" — the description the create payload
    # already set — and the tier stayed Unlimited, so it passed whether or not the update did anything.
    When I extract response field "name" and store it as "lifecycleAppName"
    And I put JSON payload from file "artifacts/payloads/update_apim_test_app.json" in context as "appUpdatePayload"
    And I set the field "name" to "{{lifecycleAppName}}" in the payload "appUpdatePayload"
    And I set the field "description" to "This app has been edited" in the payload "appUpdatePayload"
    And I set the field "throttlingPolicy" to "10PerMin" in the payload "appUpdatePayload"
    And I update the application "createdAppId" with payload "appUpdatePayload"
    Then The response status code should be 200
    And The response should contain "This app has been edited"
    And The response should contain "10PerMin"
    # Re-read: the change is persisted, not merely echoed back by the PUT.
    When I retrieve the application with id "createdAppId"
    Then The response status code should be 200
    And The response should contain "This app has been edited"
    And The response should contain "10PerMin"

    # Delete the application
    When I delete the application with id "createdAppId"
    Then The response status code should be 200

    Examples:
      | actor                       |
      | subscriberUser              |
      | subscriberUser@tenant1.com  |

  @cap:devportal @feat:applications @type:negative @legacy:ApplicationCreationTestCase
  Scenario Outline: A publisher-role user without app-management scope cannot create an application as <actor>
    Given The system is ready and I have valid devportal access token as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I attempt to create an application with payload "createAppPayload"
    Then The response status code should be 401

    Examples:
      | actor                      |
      | publisherUser              |
      | publisherUser@tenant1.com  |

  # Ports the duplicate-name gap of APIM678ApplicationCreationTestCase — creating a SECOND application with a name
  # that the SAME owner already used is rejected. The first application's resolved name is captured and reused for
  # the second create, whose clash is isolated to the name. Pinned live: 409 "An application already exists with
  # name …" (the response's numeric `code` field is the HTTP 409, not a distinct APIM error code). ×2 tenant. (The
  # create/update/delete + custom-attribute CRUD of APIM678 is already covered by this feature's lifecycle
  # scenario and devportal/application_attributes.feature.)
  @cap:devportal @feat:applications @type:negative @rule:duplicate-name @legacy:APIM678ApplicationCreationTestCase
  Scenario Outline: Creating an application with an already-used name is rejected as <actor>
    Given The system is ready and I have valid devportal access token as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "dupAppA"
    And I create an application with payload "dupAppA"
    Then The response status code should be 201
    And I extract response field "name" and store it as "dupAppName"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "dupAppB"
    And I set the field "name" to "{{dupAppName}}" in the payload "dupAppB"
    And I attempt to create an application with payload "dupAppB"
    Then The response status code should be 409
    # Include the name: the product builds "An application already exists with name " + name, so pinning the
    # name proves the conflict names the duplicated application rather than any pre-existing one.
    And The response should contain "An application already exists with name {{dupAppName}}"

    Examples:
      | actor                       |
      | subscriberUser              |
      | subscriberUser@tenant1.com  |

  # Ports CAPIMGT12CallBackURLOverwriteTestCase — application names are unique PER OWNER, not globally: two
  # DIFFERENT users may each own an application with the SAME name, and they remain independent applications
  # (distinct ids). This is the isolation the CAPIMGT-12 regression guards (updating one owner's app must not
  # touch another owner's similarly-named app). Runs x2-tenant (super + tenant1), each with two distinct owners
  # (admin + subscriber in that tenant). The callback-URL-in-key-config detail of the legacy is a key-manager
  # concern; the per-owner name isolation is the portable regression core.
  @cap:devportal @feat:applications @rule:owner-isolation @type:regression @legacy:CAPIMGT12CallBackURLOverwriteTestCase
  Scenario Outline: Two different owners can hold same-named applications independently as <owner>
    Given The system is ready
    And I have valid access tokens as "<owner>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "isoAppAdmin"
    And I set the field "name" to "SharedNameApp${UNIQUE:Iso}" in the payload "isoAppAdmin"
    And I create an application with payload "isoAppAdmin"
    Then The response status code should be 201
    And I extract response field "applicationId" and store it as "isoAdminAppId"
    And I extract response field "name" and store it as "isoSharedName"

    # A different owner (subscriber) creates an application with the SAME name — accepted (per-owner uniqueness).
    Given I act as "<otherOwner>"
    And I have a valid Devportal access token for the current user
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "isoAppSub"
    And I set the field "name" to "{{isoSharedName}}" in the payload "isoAppSub"
    And I create an application with payload "isoAppSub"
    Then The response status code should be 201
    And I extract response field "applicationId" and store it as "isoSubAppId"

    # They are independent applications (distinct ids).
    Then The stored value "isoAdminAppId" should not equal "isoSubAppId"

    # Distinct ids at CREATE time is the weaker half of the isolation claim: it says the two records were
    # created separately, not that they stay separate. The regression this guards is a DELETE of one owner's
    # application taking the other's with it, so delete one and prove the other survives — the lifetime half.
    # Ports ApplicationSharingTestCase#testUserTwoApplicationRemoval.
    When I delete the application with id "isoSubAppId"
    Then The response status code should be 200
    # The deleted one is really gone...
    When I retrieve the application "isoSubAppId"
    Then The response status code should be 404
    # ...and the OTHER owner's same-named application is untouched.
    Given I act as "<owner>"
    When I retrieve the application "isoAdminAppId"
    Then The response status code should be 200
    And The response should contain "{{isoSharedName}}"

    Examples:
      | owner             | otherOwner                 |
      | admin             | subscriberUser             |
      | admin@tenant1.com | subscriberUser@tenant1.com |

  # Ports the EMPTY-description row of APIM678ApplicationCreationTestCase's createApplicationWithValidData provider
  # ({"NewApplication2", appTier, ""}). Every other application create in this suite supplies a non-empty
  # description, so a server that rejected or silently defaulted a blank one would go unnoticed. The description is
  # asserted as exactly the empty string on the create response AND on the subsequent GET (proving it persisted
  # rather than merely echoing).
  @cap:devportal @feat:applications @rule:empty-description @type:regression @legacy:APIM678ApplicationCreationTestCase
  Scenario Outline: An application can be created with an empty description as <actor>
    Given The system is ready and I have valid devportal access token as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "emptyDescApp"
    And I set the field "description" to "" in the payload "emptyDescApp"
    And I create an application with payload "emptyDescApp"
    Then The response status code should be 201
    And The value of response field "description" should be ""
    When I retrieve the application with id "createdAppId"
    Then The response status code should be 200
    And The value of response field "description" should be ""

    Examples:
      | actor                      |
      | subscriberUser             |
      | subscriberUser@tenant1.com |

  # Ports APIM678ApplicationCreationTestCase#testUpdateApplication's tier and name updates. The lifecycle scenario
  # above performs ONE combined update and asserts only the description, so a PUT that silently ignored
  # throttlingPolicy or name would still pass it. Here each field is changed in its OWN request and the response is
  # pinned to the exact new value, with the pre-update value captured as the baseline so "changed" is provable.
  @cap:devportal @feat:applications @rule:field-updates @type:regression @legacy:APIM678ApplicationCreationTestCase
  Scenario Outline: An application's throttling tier and name can each be updated as <actor>
    Given The system is ready and I have valid devportal access token as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "fieldUpdApp"
    And I create an application with payload "fieldUpdApp"
    Then The response status code should be 201
    And I extract response field "name" and store it as "fieldUpdOrigName"
    # BASELINE — the tier and name as created (Unlimited / the generated name).
    And The value of response field "throttlingPolicy" should be "Unlimited"

    # Update ONLY the throttling tier (name/description unchanged) — the new tier must be reflected.
    When I put the following JSON payload in context as "fieldUpdTierPayload"
    """
    {"name": "{{fieldUpdOrigName}}", "throttlingPolicy": "10PerMin", "description": "Test application for scenarios"}
    """
    And I update the application "createdAppId" with payload "fieldUpdTierPayload"
    Then The response status code should be 200
    And The value of response field "throttlingPolicy" should be "10PerMin"
    And The value of response field "name" should be "{{fieldUpdOrigName}}"

    # Update ONLY the name (tier stays on the just-updated value) — the new name must be reflected.
    When I generate a unique value and store it as "fieldUpdNewName"
    And I put the following JSON payload in context as "fieldUpdNamePayload"
    """
    {"name": "{{fieldUpdNewName}}", "throttlingPolicy": "10PerMin", "description": "Test application for scenarios"}
    """
    And I update the application "createdAppId" with payload "fieldUpdNamePayload"
    Then The response status code should be 200
    And The value of response field "name" should be "{{fieldUpdNewName}}"
    And The value of response field "throttlingPolicy" should be "10PerMin"
    # ...and the rename really replaced the original name rather than adding an alias.
    When I retrieve the application with id "createdAppId"
    Then The response status code should be 200
    And The value of response field "name" should be "{{fieldUpdNewName}}"
    And The response should not contain "{{fieldUpdOrigName}}"

    Examples:
      | actor                      |
      | subscriberUser             |
      | subscriberUser@tenant1.com |

  # Ports APIM678ApplicationCreationTestCase#testRemoveApplication. The legacy put its post-delete assertion INSIDE a
  # catch block with no fail() on the non-throwing path, so a GET that still returned the application would have
  # passed silently — deletion was never actually proven. Here the post-delete GET is asserted 404 directly. (The
  # per-scenario cleanup hook re-deletes the id; ResourceCleanup treats a 404 as already-gone, so no leak warning.)
  @cap:devportal @feat:applications @rule:delete @type:regression @legacy:APIM678ApplicationCreationTestCase
  Scenario Outline: A deleted application can no longer be retrieved as <actor>
    Given The system is ready and I have valid devportal access token as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "delApp"
    And I create an application with payload "delApp"
    Then The response status code should be 201
    # BASELINE — the application is retrievable before the delete.
    When I retrieve the application with id "createdAppId"
    Then The response status code should be 200
    When I delete the application with id "createdAppId"
    Then The response status code should be 200
    When I retrieve the application with id "createdAppId"
    Then The response status code should be 404

    Examples:
      | actor                      |
      | subscriberUser             |
      | subscriberUser@tenant1.com |

  # Ports the createApplicationWithInValidData provider of APIM678ApplicationCreationTestCase, which was ENTIRELY
  # COMMENTED OUT in the legacy source pending product issue 6012, and whose assertion was assertNull(response) — the
  # SDK-null idiom, not an HTTP status. So nothing could be carried over: the expectations below were determined
  # EMPIRICALLY against the 4.7 container. BOTH an empty application name and an empty throttlingPolicy ARE rejected
  # (product issue 6012 is fixed as far as these two rows go) with exactly:
  #   400 {"code":400,"message":"Bad Request","description":"Validation Error",
  #        "error":[{"code":"400_name","message":"name: size must be between 1 and 100"}]}
  #   400 {... "error":[{"code":"400_throttlingPolicy",
  #        "message":"throttlingPolicy: size must be between 1 and 2147483647"}]}
  # — i.e. bean-validation size constraints, not an APIM error code. The per-field message is asserted (not just the
  # 400) so a validator that started rejecting the WRONG field would fail here. Uses the non-asserting
  # attempt-to-create step so a wrongly-SUCCESSFUL create fails this scenario instead of passing silently.
  @cap:devportal @feat:applications @rule:invalid-data @type:negative @legacy:APIM678ApplicationCreationTestCase
  Scenario Outline: Creating an application with an empty <field> is rejected as <actor>
    Given The system is ready and I have valid devportal access token as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "invalidApp"
    And I set the field "<field>" to "" in the payload "invalidApp"
    And I attempt to create an application with payload "invalidApp"
    Then The response status code should be 400
    And The response should contain "400_<field>"
    And The response should contain "<field>: size must be between 1 and <max>"

    Examples:
      | actor                      | field            | max        |
      | subscriberUser             | name             | 100        |
      | subscriberUser             | throttlingPolicy | 2147483647 |
      | subscriberUser@tenant1.com | name             | 100        |
      | subscriberUser@tenant1.com | throttlingPolicy | 2147483647 |
