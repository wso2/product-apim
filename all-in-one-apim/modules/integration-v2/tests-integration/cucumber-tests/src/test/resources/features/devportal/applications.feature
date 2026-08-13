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

  # Also tags ApplicationCallbackURLTestCase (commented-out in the legacy suite): its two ENABLED methods verified
  # basic callback-URL persistence through application create + update, which this create/update/delete lifecycle
  # subsumes. The class's two DISABLED methods asserted callback-URL FORMAT validation (malformed URL / invalid
  # IP), which the product no longer enforces (the legacy comments state the validation was removed) — so there is
  # nothing left to port for those.
  @cap:devportal @feat:applications @type:smoke @legacy:ApplicationCreationTestCase @legacy:ApplicationCallbackURLTestCase
  Scenario Outline: Create, update and delete an application as <actor>
    Given The system is ready and I have valid devportal access token as "<actor>"

    # Create the application
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I retrieve the application with id "createdAppId"
    Then The response status code should be 200

    # Update the application
    When I put JSON payload from file "artifacts/payloads/update_apim_test_app.json" in context as "appUpdatePayload"
    And I update the application "createdAppId" with payload "appUpdatePayload"
    Then The response status code should be 200
    And The response should contain "Test application for scenarios"

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
    And The response should contain "An application already exists"

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

    Examples:
      | owner             | otherOwner                 |
      | admin             | subscriberUser             |
      | admin@tenant1.com | subscriberUser@tenant1.com |
