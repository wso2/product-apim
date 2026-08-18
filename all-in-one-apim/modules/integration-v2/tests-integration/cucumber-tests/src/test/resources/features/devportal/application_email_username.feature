@cleanup
Feature: DevPortal Application Ownership Under an Email-Form Username

  An application created by a principal whose USERNAME IS AN EMAIL ADDRESS is attributed to that principal in
  full, doubly-qualified form. Runs in the IntegrationV2-EmailUserName block, which sets emailUserMode=true and
  enable_email_domain=true.

  WHY THIS IS NOT COVERED BY A 201 WALK. Creating the application would return 201 even if the owner were
  silently truncated at the first "@" — the application would still exist, owned by SOMEONE. What is pinned is
  the stored owner string itself, which is where an "@"-splitting bug would actually surface: the devportal
  records the owner from the authenticated principal, and a name that itself contains "@" is exactly the input
  that makes "split off the tenant" ambiguous.

  Complements publisher/api_product_email_username.feature, which pins the same idea on the PROVIDER of an API
  product; this is the devportal-plane consumer half.

  @cap:devportal @feat:applications @rule:email-username @type:regression @legacy:APIProductLifecycleTest
  Scenario Outline: An application created by an email-form principal records the full principal as owner as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"

    # Pin the acting principal's PHYSICAL username first — if the block ever provisioned plain usernames this
    # feature would pass while testing nothing.
    When I store the acting actor credentials as "appOwnerUser" and "appOwnerPass"
    Then the actual value of "appOwnerUser" should match the expected value:
      """
      <expectedUsername>
      """

    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "emailAppPayload"
    And I create an application with payload "emailAppPayload"
    Then The response status code should be 201

    # The assertion the dimension exists for: the stored owner is the email-form principal, in full.
    When I retrieve the application with id "createdAppId"
    Then The response status code should be 200
    And The value of response field "owner" should be "<expectedOwner>"

    Examples:
      | actor                  | expectedUsername                  | expectedOwner                     |
      | emailAdmin             | emailAdmin@email.com@carbon.super | emailAdmin@email.com              |
      | emailAdmin@tenant1.com | emailAdmin@email.com@tenant1.com  | emailAdmin@email.com@tenant1.com  |
