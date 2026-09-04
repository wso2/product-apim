@cleanup
Feature: Gateway Throttle Isolation Across Email-Form And Plain Usernames

  Application-level throttling must give each PRINCIPAL its own bucket, including when one principal's username
  is itself an email address. Runs in the IntegrationV2-EmailUserName block (enable_email_domain=true,
  initBackend=true).

  WHAT THIS GUARDS, STATED HONESTLY. ThrottleHandler builds
  applicationLevelThrottleKey = applicationId + ":" + authorizedUser, and appends the tenant only when
  StringUtils.contains(authorizedUser, tenantDomain) is FALSE — a SUBSTRING test where a suffix/equality test
  is meant. Two accounts collide when one is named exactly "<other>@<tenantDomain>", because the plain form
  gets the append and the other skips it, landing on one key. A collision would let one account consume
  another's per-user quota.

  IT IS NOT REACHABLE IN A SUPPORTED CONFIGURATION, and this test does not claim otherwise. Tenants accept
  only email-form usernames, so the pair cannot exist there (both possible pairings were built and each is
  blocked by an account that cannot authenticate — see the comment on the scenario). In the super tenant both
  name forms ARE allowed, but the tenant domain is the literal string "carbon.super", so the colliding account
  would have to be "<local>@carbon.super" — not a real email domain. This test therefore uses a SYNTHETIC
  account that could not arise naturally.

  WHY KEEP IT ANYWAY. The fragile operator is still in the product, and what currently prevents the collision
  is incidental rather than deliberate (4.7.0 masks the token subject to a pseudonymous UUID, so authorizedUser
  is never the raw username the substring test reasons about). A refactor of either the append logic or the
  masking makes this live. This is a structural regression guard on that code path, not evidence of a defect.

  THE TWO CONTROLS ARE WHAT MAKE THE RESULT MEAN ANYTHING. Without them a bare "the email user got 200" proves
  nothing:
    * OTHER (a DISTINCT base) must get 200 on the exhausted application — otherwise the bucket is per
      APPLICATION rather than per user, and no collision could ever be visible this way.
    * PLAIN must STILL be throttled at the end — otherwise the throttle window simply rolled over during the
      polling and every 200 above is meaningless.

  # SUPER TENANT ONLY. The tenant row is IMPOSSIBLE, not skipped — both pairings that could collide were built
  # and run, and each is blocked by an account that cannot authenticate under email mode.
  #
  # For two accounts to compute one key, account Q's store name must be exactly "<P's name>@<tenantDomain>":
  #   pairing 1: P = plain "alice", Q = "alice@tenant1.com"
  #              -> P cannot authenticate; tenants allow only email-form usernames (403 on Basic auth,
  #                 400 invalid_grant on password grant, both observed).
  #   pairing 2: P = "alice@wso2.com", Q = "alice@wso2.com@tenant1.com"
  #              -> Q cannot authenticate; its credential carries four "@" and the token endpoint answers
  #                 400 invalid_grant, even though the store accepts the name (observed).
  # There is no third pairing: in a tenant P must itself be email-form, which forces Q to carry two "@".
  #
  # Add the tenant row only if either account form becomes able to authenticate.

  @cap:gateway @feat:throttling-enforcement @rule:email-username @type:regression @dep:admin @dep:devportal
  Scenario: An email-form principal does not share an application throttle bucket with the plain user of the same name
    Given The system is ready
    And I have valid access tokens as "admin"

    # Two principals sharing ONE base: plain "<base>" and email-form "<base>@carbon.super".
    When I generate a unique alphanumeric value and store it as "bBase"
    And I provision a user with exact name "bc{{bBase}}" password "Password@123" and roles "Internal/subscriber" storing the username as "uPlain"
    And I provision a user with exact name "bc{{bBase}}@carbon.super" password "Password@123" and roles "Internal/subscriber" storing the username as "uEmail"
    When I generate a unique alphanumeric value and store it as "bOther"
    And I provision a user with exact name "bo{{bOther}}" password "Password@123" and roles "Internal/subscriber" storing the username as "uOther"

    # A LOW application policy so the bucket is reachable.
    When I create an application throttling policy "${UNIQUE:bLow3}" allowing 3 requests per minute
    Then The response status code should be 201
    And I copy context value "appThrottlePolicyName" to "bPolicy"

    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "bApiId" and deployed it
    When I publish the "apis" resource with id "bApiId"
    Then The lifecycle status of API "bApiId" should be "Published"
    When I retrieve the "apis" resource with id "bApiId"
    And I extract response field "context" and store it as "bCtx"
    And the "apis" resource "bApiId" should be live on the gateway, redeploying if propagation is lost
    And I wait until "apis" "bApiId" revision is deployed in the gateway

    When I put the following JSON payload in context as "bAppPayload"
    """
    {"name":"${UNIQUE:BProbeApp}","throttlingPolicy":"{{bPolicy}}","description":"B probe"}
    """
    And I create an application with payload "bAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "bKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "bKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "bSubPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "bApiId" using application "createdAppId" with payload "bSubPayload" as "bSubId"
    Then The response status code should be 201

    # UNKNOWN #1 — can the email-form user get a token with the RAW store name? (The previous attempt passed the
    # tenant-suffixed form and got invalid_grant.) If this fails, the probe stops here and that IS the finding.
    When I request an OAuth access token using password grant as user "{{uEmail}}" with password "Password@123"
    Then The response status code should be 200
    And I copy context value "generatedAccessToken" to "tokenEmail"

    When I request an OAuth access token using password grant as user "{{uPlain}}" with password "Password@123"
    Then The response status code should be 200
    And I copy context value "generatedAccessToken" to "tokenPlain"
    When I request an OAuth access token using password grant as user "{{uOther}}" with password "Password@123"
    Then The response status code should be 200
    And I copy context value "generatedAccessToken" to "tokenOther"

    # Exhaust the bucket as PLAIN.
    When I invoke the API at gateway context "{{bCtx}}/1.0.0/customers/123/" with method "GET" using access token "tokenPlain" and payload "" until response status code becomes 429 within 60 seconds
    Then The response status code should be 429

    # UNKNOWN #2 — the CONTROL. A distinct-base user on the same exhausted application.
    # Asserting 200 as a guess; if it is really 429 the failure message says so.
    When I invoke the API at gateway context "{{bCtx}}/1.0.0/customers/123/" with method "GET" using access token "tokenOther" and payload "" until response status code becomes 200 within 20 seconds
    Then The response status code should be 200

    # UNKNOWN #3 — the collision candidate. Asserting 200 (no collision) as the guess; a 429 here WITH the
    # control at 200 is the collision reproducing.
    When I invoke the API at gateway context "{{bCtx}}/1.0.0/customers/123/" with method "GET" using access token "tokenEmail" and payload "" until response status code becomes 200 within 20 seconds
    Then The response status code should be 200

    # WINDOW-VALIDITY CONTROL. The two invokes above poll for up to 20s each, so up to ~40s can elapse against a
    # 60s window. If the window simply ROLLED OVER, everyone gets 200 and the "no collision" reading is bogus.
    # PLAIN must therefore STILL be throttled at this instant. If this comes back 200, the window reset and the
    # whole measurement above is VOID — not evidence of anything.
    When I invoke the API at gateway context "{{bCtx}}/1.0.0/customers/123/" with method "GET" using access token "tokenPlain" and payload "" until response status code becomes 429 within 5 seconds
    Then The response status code should be 429

  # OWNER-RESETS-OWN reset with an EMAIL-FORM owner: an application owned by an email-form principal (its username
  # IS an email address, so its application throttle key is built from the email-form authorizedUser) is cleared by
  # THAT OWNER via the DevPortal reset endpoint. The reset authorizes on the CALLER's ownership
  # (APIConsumerImpl#resetApplicationThrottlePolicy validates the caller owns the app — there is no admin
  # override), so an admin CANNOT clear another principal's application; the owner performs its own reset. Ports
  # the TENANT_EMAIL_USER owner of ApplicationThrottlingResetTestCase, whose reset was driven by each fanned owner
  # over their OWN application. The owner's PHYSICAL username is PINNED first (the two-at-sign email form) so the
  # scenario cannot silently pass while exercising a plain-username throttle key — which is the whole point of
  # running this arc for an email-form principal.
  @cap:gateway @feat:throttling-enforcement @rule:email-username @type:regression @dep:admin @dep:publisher @dep:devportal @legacy:ApplicationThrottlingResetTestCase
  Scenario Outline: An email-form principal resets their own application's throttle counter clearing the 429 as <ownerActor>
    Given The system is ready
    And I have valid access tokens as "<adminActor>"

    # A bespoke application policy allowing only 3 requests/min (admin-only op).
    When I create an application throttling policy "${UNIQUE:emResetThrottle3}" allowing 3 requests per minute
    Then The response status code should be 201
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "emResetApiId" and deployed it
    And the "apis" resource "emResetApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "emResetApiId"
    Then The lifecycle status of API "emResetApiId" should be "Published"
    When I retrieve the "apis" resource with id "emResetApiId"
    And I extract response field "context" and store it as "emResetContext"

    # SWITCH to the EMAIL-FORM principal, who OWNS the application. Pin its PHYSICAL username first (the two-at-sign
    # email form) — if the block ever provisioned plain usernames this pin fails loudly instead of the scenario
    # passing while testing a plain-username throttle key.
    Given The system is ready and I have valid devportal access token as "<ownerActor>"
    When I store the acting actor credentials as "emOwnerName" and "emOwnerPassword"
    Then the actual value of "emOwnerName" should match the expected value:
      """
      <ownerPhysical>
      """
    When I create an application "${UNIQUE:EmResetApp}" with throttling policy from "appThrottlePolicyName"
    Then The response status code should be 201
    When I put the following JSON payload in context as "generateApplicationKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateApplicationKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "apiSubscriptionPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "emResetApiId" using application "createdAppId" with payload "apiSubscriptionPayload" as "emResetSubId"
    Then The response status code should be 201
    When I request an OAuth access token for the current user using password grant with scope "PRODUCTION"
    Then The response status code should be 200

    # Drive the email-form owner's token past the 3/min limit -> 429 code 900803 (APPLICATION-level), the limit the reset clears.
    When I invoke the API at gateway context "{{emResetContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 429 within 60 seconds
    Then The response status code should be 429
    And The value of error response field "code" should be "900803"

    # Reset the EMAIL-FORM owner's own application counter AS that owner (the reset authorizes on the caller's
    # ownership, so this must be the owner and not an admin).
    When I reset the application throttle policy for "createdAppId" owned by "<ownerActor>"
    Then The response status code should be 200
    # Post-reset invocation uses the same email-form owner's token -> succeeds again, proving the owner cleared
    # their own bucket even though the throttle key is built from a two-at-sign email username.
    When I invoke the API at gateway context "{{emResetContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    # Re-drive -> 429 with the same application code: proves the reset CLEARED the counter (not disabled throttling).
    When I invoke the API at gateway context "{{emResetContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 429 within 60 seconds
    Then The response status code should be 429
    And The value of error response field "code" should be "900803"

    Examples:
      | adminActor        | ownerActor             | ownerPhysical                     |
      | admin             | emailAdmin             | emailAdmin@email.com@carbon.super |
      | admin@tenant1.com | emailAdmin@tenant1.com | emailAdmin@email.com@tenant1.com  |
