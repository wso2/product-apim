@cleanup
Feature: Admin Application Search

  Ports ApplicationsSearchByNameOrOwnerTestCase: an admin can search Developer Portal applications via the
  admin API both by name (GET /admin/v4/applications?name=<name>) and by owner (?user=<owner>). Two dimensions
  are covered: the admin finding its OWN application, and — the authorization property the legacy class was
  really about — the admin finding ANOTHER user's application, which no scenario asserted before.
  Both search modes assert the matched entry's owner, not merely that the name appears in the body: the two
  dimensions differ ONLY in whose application comes back, so a name-substring check cannot tell them apart.

  @cap:admin @feat:application-management @type:regression @legacy:ApplicationsSearchByNameOrOwnerTestCase
  Scenario Outline: An admin can search its own applications by name and by owner as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "searchAppPayload"
    And I create an application with payload "searchAppPayload"
    Then The response status code should be 201
    And I extract response field "name" and store it as "searchAppName"
    # Search by name.
    When I search admin applications by name "{{searchAppName}}"
    Then The response status code should be 200
    And The searched applications should include "{{searchAppName}}" owned by "<actor>"
    # Search by owner (the acting admin).
    When I search admin applications owned by actor "<actor>"
    Then The response status code should be 200
    And The searched applications should include "{{searchAppName}}" owned by "<actor>"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # The cross-owner dimension (legacy testApplicationSearchUserByName / testApplicationSearchUserByOwner, where
  # the admin searched for the SUBSCRIBER-owned TestApp1/TestApp2): the application is created by the subscriber
  # and the admin — a different principal — must still find it, by name and by ?user=<the subscriber>. The
  # subscriber's own application is registered for teardown as the subscriber, so cleanup deletes it with that
  # actor's devportal token (CLAUDE.md §5).
  #
  # The legacy class also expected the subscriber's auto-created DefaultApplication in the owner search, and that
  # expectation holds here — verified live on 4.7.0: the owner search returns exactly two applications, the one
  # created above and DefaultApplication, both under the subscriber. (The subscriber never logs into the portal;
  # creating the application through the devportal is what materialises the subscriber and its DefaultApplication.)
  # It is asserted ONLY in the owner search, which is where legacy expected it: the name search is filtered by our
  # unique application name, so DefaultApplication legitimately does not come back there.
  @cap:admin @feat:application-management @rule:cross-owner @type:regression @dep:devportal @legacy:ApplicationsSearchByNameOrOwnerTestCase
  Scenario Outline: An admin can search another user's applications by name and by owner as <actor>
    Given The system is ready and I have valid devportal access token as "<appOwner>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "otherAppPayload"
    And I create an application with payload "otherAppPayload"
    Then The response status code should be 201
    And I extract response field "name" and store it as "otherAppName"

    # Switch to the admin — a DIFFERENT principal from the application's owner — and search.
    Given I have valid access tokens as "<actor>"
    When I search admin applications by name "{{otherAppName}}"
    Then The response status code should be 200
    And The searched applications should include "{{otherAppName}}" owned by "<appOwner>"
    When I search admin applications owned by actor "<appOwner>"
    Then The response status code should be 200
    And The searched applications should include "{{otherAppName}}" owned by "<appOwner>"
    And The searched applications should include "DefaultApplication" owned by "<appOwner>"

    Examples:
      | actor             | appOwner                   |
      | admin             | subscriberUser             |
      | admin@tenant1.com | subscriberUser@tenant1.com |
