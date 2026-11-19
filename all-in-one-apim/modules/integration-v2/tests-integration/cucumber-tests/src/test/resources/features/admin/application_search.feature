@cleanup
Feature: Admin Application Search

  Ports ApplicationsSearchByNameOrOwnerTestCase: an admin can search Developer Portal applications via the
  admin API both by name (GET /admin/v4/applications?name=<name>) and by owner (?user=<owner>). Two dimensions
  are covered: the admin finding its OWN application, and — the authorization property the legacy class was
  really about — the admin finding ANOTHER user's application, which no scenario asserted before.
  Both search modes assert the matched entry's owner, not merely that the name appears in the body: the two
  dimensions differ ONLY in whose application comes back, so a name-substring check cannot tell them apart.

  @cap:admin @feat:application-management @type:regression @legacy:ApplicationsSearchByNameOrOwnerTestCase
  Scenario Outline: An admin can search its own applications by exact or partial name and by owner as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I generate a unique alphanumeric value and store it as "searchAppStem"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "searchAppPayloadOne"
    And I set the field "name" to "AppSearch{{searchAppStem}}One" in the payload "searchAppPayloadOne"
    And I create an application with payload "searchAppPayloadOne"
    Then The response status code should be 201
    And I extract response field "name" and store it as "searchAppNameOne"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "searchAppPayloadTwo"
    And I set the field "name" to "AppSearch{{searchAppStem}}Two" in the payload "searchAppPayloadTwo"
    And I create an application with payload "searchAppPayloadTwo"
    Then The response status code should be 201
    And I extract response field "name" and store it as "searchAppNameTwo"
    # Exact-name and shared-prefix searches use the same two uniquely named fixtures. The table asserts the full
    # result set, so exact search must return one app and partial search must return both, with the right owner.
    When I search admin applications by name "<query>"
    Then The response status code should be 200
    And The searched applications should contain exactly "<expectedNames>" owned by "<actor>"
    # Search by owner (the acting admin).
    When I search admin applications owned by actor "<actor>"
    Then The response status code should be 200
    And The searched applications should include "{{searchAppNameOne}}" owned by "<actor>"
    And The searched applications should include "{{searchAppNameTwo}}" owned by "<actor>"

    Examples:
      | actor             | query                         | expectedNames                               |
      | admin             | {{searchAppNameOne}}          | {{searchAppNameOne}}                        |
      | admin             | AppSearch{{searchAppStem}}    | {{searchAppNameOne}},{{searchAppNameTwo}}   |
      | admin@tenant1.com  | {{searchAppNameOne}}          | {{searchAppNameOne}}                        |
      | admin@tenant1.com  | AppSearch{{searchAppStem}}    | {{searchAppNameOne}},{{searchAppNameTwo}}   |

  # The cross-owner dimension: a different admin must find both apps created by the subscriber, using either an
  # exact name or their shared partial prefix, and owner search must continue to find them under that subscriber.
  @cap:admin @feat:application-management @rule:cross-owner @type:regression @dep:devportal @legacy:ApplicationsSearchByNameOrOwnerTestCase
  Scenario Outline: An admin can search another user's applications by exact or partial name and by owner as <actor>
    Given The system is ready and I have valid devportal access token as "<appOwner>"
    When I generate a unique alphanumeric value and store it as "otherAppStem"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "otherAppPayloadOne"
    And I set the field "name" to "AppSearch{{otherAppStem}}One" in the payload "otherAppPayloadOne"
    And I create an application with payload "otherAppPayloadOne"
    Then The response status code should be 201
    And I extract response field "name" and store it as "otherAppNameOne"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "otherAppPayloadTwo"
    And I set the field "name" to "AppSearch{{otherAppStem}}Two" in the payload "otherAppPayloadTwo"
    And I create an application with payload "otherAppPayloadTwo"
    Then The response status code should be 201
    And I extract response field "name" and store it as "otherAppNameTwo"

    # Switch to the admin — a DIFFERENT principal from the application's owner — and verify exact and partial search.
    Given I have valid access tokens as "<actor>"
    When I search admin applications by name "<query>"
    Then The response status code should be 200
    And The searched applications should contain exactly "<expectedNames>" owned by "<appOwner>"
    When I search admin applications owned by actor "<appOwner>"
    Then The response status code should be 200
    And The searched applications should include "{{otherAppNameOne}}" owned by "<appOwner>"
    And The searched applications should include "{{otherAppNameTwo}}" owned by "<appOwner>"
    And The searched applications should include "DefaultApplication" owned by "<appOwner>"

    Examples:
      | actor             | appOwner                   | query                        | expectedNames                             |
      | admin             | subscriberUser             | {{otherAppNameOne}}          | {{otherAppNameOne}}                       |
      | admin             | subscriberUser             | AppSearch{{otherAppStem}}    | {{otherAppNameOne}},{{otherAppNameTwo}}   |
      | admin@tenant1.com  | subscriberUser@tenant1.com | {{otherAppNameOne}}          | {{otherAppNameOne}}                       |
      | admin@tenant1.com  | subscriberUser@tenant1.com | AppSearch{{otherAppStem}}    | {{otherAppNameOne}},{{otherAppNameTwo}}   |
