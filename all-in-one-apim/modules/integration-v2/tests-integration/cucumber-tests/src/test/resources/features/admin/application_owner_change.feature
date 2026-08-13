@cleanup
Feature: Admin Application Owner Change

  Admin-plane application ownership transfer (POST /applications/{id}/change-owner?owner=…): an admin transfers
  an application to another valid subscriber in the same domain (the application then appears under the new owner
  in the admin application listing), and transferring to a non-existent user is rejected. Ports the portable
  cases of OAuthApplicationOwnerUpdateTestCase. Runs in the admin block; ×2 tenant for the valid transfer.
  Teardown via @cleanup removes the application (registered as the creating actor).

  # Valid owner change within the same domain: the admin creates an application, transfers it to the subscriber
  # (a valid same-domain subscriber), and the application then lists under the subscriber in the admin API.
  @cap:admin @feat:application-management @rule:change-owner @type:regression @dep:devportal @legacy:OAuthApplicationOwnerUpdateTestCase
  Scenario Outline: An application owner can be changed to another user in the same domain as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"

    # Materialize the target as a subscriber first: change-owner requires the new owner to exist in the user
    # store AND resolve as a subscriber (server-side updateApplicationOwner does isExistingUser + getSubscriber;
    # a target that has never touched the devportal has no AM_SUBSCRIBER row). Acting as the target and creating
    # a throwaway application initialises that row, so the transfer below resolves the owner cleanly instead of
    # failing with a generic 500. The throwaway app is registered for teardown (deleted as its creating actor).
    When I have a valid DCR application as "<newOwner>"
    And I have a valid Devportal access token as "<newOwner>"
    And I act as "<newOwner>"
    And I create an application "${UNIQUE:OwnerSeed}" with visibility "PRIVATE" as "seedAppId"
    Then The response status code should be 201

    # Back to the admin, who owns the application to be transferred.
    When I act as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "ownerChangeApp"
    And I create an application with payload "ownerChangeApp"
    Then The response status code should be 201
    And I extract response field "applicationId" and store it as "ownerChangeAppId"

    # Transfer ownership to the subscriber (same domain as the acting admin, resolved by the actor's @domain).
    When I change the owner of application "ownerChangeAppId" to "<newOwner>"
    Then The response status code should be 200

    # The application now lists under the new owner in the admin applications API.
    When I retrieve the admin applications owned by "<newOwner>"
    Then The response status code should be 200
    And The response should contain "{{ownerChangeAppId}}"

    Examples:
      | actor             | newOwner                   |
      | admin             | subscriberUser             |
      | admin@tenant1.com | subscriberUser@tenant1.com |

  # Negative: transferring an application to a NON-EXISTENT user is rejected. Pinned live: the change-owner REST
  # call returns 500 "Error while updating application owner" (the owner-resolution failure surfaces as a server
  # error, not a 404). Runs x2-tenant (super + tenant1): the owner-resolution failure is tenant-agnostic.
  @cap:admin @feat:application-management @rule:change-owner @type:negative @legacy:OAuthApplicationOwnerUpdateTestCase
  Scenario Outline: Changing an application owner to a non-existent user is rejected as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "noOwnerApp"
    And I create an application with payload "noOwnerApp"
    Then The response status code should be 201
    And I extract response field "applicationId" and store it as "noOwnerAppId"
    When I change the owner of application "noOwnerAppId" to the raw user "nonExistentOwner${UNIQUE:X}"
    Then The response status code should be 500

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Negative: transferring to a user in a DIFFERENT tenant is rejected. The legacy pinned 500; pinned again live on
  # 4.7 (see report). NOTE: a 500 for a cross-tenant transfer is a server-error smell rather than a designed 4xx
  # rejection — flagged as an upstream-bug candidate; this scenario pins the ACTUAL current behaviour.
  @cap:admin @feat:application-management @rule:change-owner @type:negative @legacy:OAuthApplicationOwnerUpdateTestCase
  Scenario: Changing an application owner to a user in a different tenant is rejected
    Given The system is ready
    And I have valid access tokens as "admin"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "xtApp"
    And I create an application with payload "xtApp"
    Then The response status code should be 201
    And I extract response field "applicationId" and store it as "xtAppId"
    # subscriberUser@tenant1.com is a valid user, but in the tenant1.com domain — cross-tenant transfer is refused.
    When I change the owner of application "xtAppId" to "subscriberUser@tenant1.com"
    Then The response status code should be 500

  # Negative: transferring to a user who ALREADY owns an application with the same name is rejected. The legacy
  # pinned 500; pinned again live on 4.7 (see report). NOTE: like the cross-tenant case, a 500 here is an upstream-
  # bug candidate (a name clash is arguably a 409); this scenario pins the ACTUAL current behaviour.
  @cap:admin @feat:application-management @rule:change-owner @type:negative @legacy:OAuthApplicationOwnerUpdateTestCase
  Scenario Outline: Changing an application owner to a user who already owns a same-named application is rejected as <owner>
    Given The system is ready
    And I have valid access tokens as "<owner>"
    # The subscriber owns an application with a shared name (its own DCR + devportal token materialise its
    # subscriber presence, so the later transfer fails for the NAME CLASH — not for a missing subscriber).
    Given I have a valid DCR application as "<subscriber>"
    And I have a valid Devportal access token as "<subscriber>"
    And I act as "<subscriber>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "clashSubApp"
    And I set the field "name" to "ClashName${UNIQUE:C}" in the payload "clashSubApp"
    And I create an application with payload "clashSubApp"
    Then The response status code should be 201
    And I extract response field "name" and store it as "clashName"
    # The owner (admin) holds an application with the SAME name.
    Given I act as "<owner>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "clashAdminApp"
    And I set the field "name" to "{{clashName}}" in the payload "clashAdminApp"
    And I create an application with payload "clashAdminApp"
    Then The response status code should be 201
    And I extract response field "applicationId" and store it as "clashAdminAppId"
    # Transferring the owner's app to the subscriber (who already has that name) is refused.
    When I change the owner of application "clashAdminAppId" to "<subscriber>"
    Then The response status code should be 500

    Examples:
      | owner             | subscriber                 |
      | admin             | subscriberUser             |
      | admin@tenant1.com | subscriberUser@tenant1.com |
