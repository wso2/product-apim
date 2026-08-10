@cleanup
Feature: Email-Form Username Login

  Login with an EMAIL ADDRESS as the username. The block runs with emailUserMode=true, so every user the
  framework provisions carries an email-form physical username while the actor REFERENCES stay logical
  (publisherUser, admin@tenant1.com) — an email address never appears in a reference, because a reference is
  split on its FIRST at-sign into userKey and tenant. Each scenario first pins the exact physical username it is
  acting as, so a silently-plain provisioning can never make these scenarios pass, then proves that credential
  actually reaches the product planes with a real call that returns real data.

  Framed as key-manager token issuance because the credential is the subject: the auth composites mint the
  actor's tokens through DCR + password grant against the email-form username, and the REST authenticators
  (OAuthJwtAuthenticatorImpl / OAuthOpaqueAuthenticatorImpl) then have to resolve that email subject back to a
  user — the step that fails with 401 when enable_email_domain is off. Ports EmailUserNameLoginTestCase.
  Teardown via the per-scenario cleanup hook.

  # Legacy arc 1: a tenant whose ADMIN is an email address logs in to all three planes. The tenant admin is the
  # one actor with admin rights, so it is the only one that can prove the Admin portal. The DevPortal and Admin
  # planes are proven by shipped data (DefaultApplication / Unlimited), not merely a 200; the Publisher list is
  # legitimately empty for a fresh tenant, so it asserts the list envelope and the API round trip is left to the
  # second scenario.
  @cap:key-manager @feat:token-issuance @type:smoke @rule:email-username @legacy:EmailUserNameLoginTestCase
  Scenario: A tenant admin whose username is an email address reaches the Publisher, DevPortal and Admin planes
    Given The system is ready
    And I have valid access tokens as "admin@tenant1.com"
    When I store the acting actor credentials as "emailAdminName" and "emailAdminPassword"
    Then the actual value of "emailAdminName" should match the expected value:
      """
      admin@email.com@tenant1.com
      """
    When I retrieve all APIs created through the Publisher REST API
    Then The response status code should be 200
    And The response should contain "list"
    When I list the DevPortal applications
    Then The response status code should be 200
    And The response should contain "DefaultApplication"
    When I retrieve all "application" throttling policies
    Then The response status code should be 200
    And The response should contain "Unlimited"

  # Legacy arc 2: a NON-ADMIN tenant user whose username is an email address logs in to the Publisher. The
  # tenant1.com row is the legacy case — and it was provisioned BY the email-form tenant admin above, so it also
  # proves that admin's own carbon-level credential works. The carbon.super row is the super-tenant variant the
  # legacy coverage map claims but the legacy class never actually exercised. Both rows need the flag: with it
  # off the REST authenticators only strip a "@carbon.super" suffix carrying at most one at-sign, so neither
  # email-form subject resolves. The API create + provider read-back proves the email username round-trips as an
  # API PROVIDER, not just as a token subject: the provider step re-fetches the API directly by id and asserts the
  # stored provider EQUALS the actor's physical username (with the "@carbon.super" suffix stripped for a super
  # tenant user), so it pins the exact value rather than merely a 2xx. The password stays the plain base name in
  # this mode, so passing it explicitly also pins that emailUserMode changes the username and nothing else.
  #
  # The LISTING assertion that follows is kept as well, and it is the one that needed work rather than removal.
  # The Publisher's `GET /apis` listing is search-index backed and is not read-your-writes: measured on this build,
  # an assertion made milliseconds after the 201 returned `{"count":0,"list":[]}` for the super-tenant row, and the
  # tenant1.com row flipped between listed and not listed across otherwise identical runs. A flip-flop rules out a
  # deterministic "email-form providers are never listed" rule for that row, which pointed at indexing lag rather
  # than a product defect — so the fix belongs in the step, and the shared glue now polls the listing until the id
  # appears (bounded by the propagation window) instead of reading it once. The assertion is unchanged in strength:
  # still presence-by-id. If it ever fails at the deadline the failure text carries the last listing body, so the
  # cause is readable without a second run.
  @cap:key-manager @feat:token-issuance @type:regression @rule:email-username @legacy:EmailUserNameLoginTestCase
  Scenario Outline: A non-admin user whose username is an email address works on the Publisher as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I store the acting actor credentials as "emailUserName" and "emailUserPassword"
    Then the actual value of "emailUserName" should match the expected value:
      """
      <expectedUsername>
      """
    And a password-grant token request for the acting user with password "<password>" returns status 200
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "emailApiCreate"
    And I create an "apis" resource with payload "emailApiCreate" as "emailApiId"
    Then The response status code should be 201
    And The provider of API "emailApiId" should match actor "<actor>"
    When I retrieve all APIs created through the Publisher REST API
    Then The API with id "emailApiId" should be in the list of all APIS

    Examples:
      | actor                     | expectedUsername                      | password        |
      | publisherUser             | publisherUser1@email.com@carbon.super | publisherUser1  |
      | publisherUser@tenant1.com | publisherUser11@email.com@tenant1.com | publisherUser11 |

  # Legacy arc 2, the two planes the outline above cannot reach: LoginWithTenantUserEmailUserNameTestCase proved the
  # NON-ADMIN email user on the DevPortal (getAllApps) and the Admin portal (throttlingPoliciesApplicationGet) as
  # well as the Publisher, and only the Publisher leg was ported.
  # A SIBLING scenario rather than extra steps on the outline above, for a token reason and not a stylistic one:
  # that outline deliberately opens with the LEAST-PRIVILEGE publisher composite, which mints no admin token at all
  # (§12), so the Admin-portal leg is unreachable from inside it. Here the full composite is used instead, and the
  # scenario therefore also pins something the least-privilege one cannot: that an email-form credential survives
  # the password grant for the ADMIN token's scope set, not only the publisher/devportal ones.
  # The DevPortal leg is a WRITE followed by a READ (create an application, then find it by name in the listing)
  # rather than the legacy's bare assertNotNull on an empty list, which would have passed for a user whose
  # DevPortal access was completely broken as long as the endpoint answered. A UNIQUE application name is the
  # marker rather than "DefaultApplication", so the read cannot be satisfied by something this scenario did not
  # create.
  # THE ACTOR IS `userKey1`, NOT `publisherUser`, and that is a correctness requirement rather than a preference.
  # Legacy's LoginWithTenantUserEmailUserNameTestCase provisioned its tenant user with
  # {Internal/subscriber, Internal/publisher, Internal/creator} (EmailUserNameLoginTestCase:151-152); v2's
  # `userKey1` (testUser1 / testUser11) is exactly that role set, while `publisherUser` carries creator+publisher
  # with NO subscriber role. MEASURED with publisherUser first, under the build lock: the DevPortal application
  # create answered `{"code":401,...,"description":"Unauthenticated request"} expected [201] but found [401]` in
  # BOTH tenants (/tmp/w10w8-run4-locked-deliveryprobe.log) - i.e. porting the row onto the publisher actor cannot
  # express it at all, because the role set legacy relied on is the whole point of the row.
  @cap:key-manager @feat:token-issuance @type:regression @rule:email-username @legacy:EmailUserNameLoginTestCase
  Scenario Outline: A non-admin user whose username is an email address reaches the DevPortal and Admin planes as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I store the acting actor credentials as "emailPlaneUserName" and "emailPlaneUserPassword"
    Then the actual value of "emailPlaneUserName" should match the expected value:
      """
      <expectedUsername>
      """
    # DevPortal plane: an application created by this credential is listed back to it
    When I generate a unique alphanumeric value and store it as "emailPlaneAppName"
    And I put the following JSON payload in context as "emailPlaneAppCreate"
    """
    {"name": "{{emailPlaneAppName}}", "throttlingPolicy": "Unlimited", "description": "Email-username DevPortal plane"}
    """
    And I create an application with payload "emailPlaneAppCreate"
    Then The response status code should be 201
    When I list the DevPortal applications
    Then The response status code should be 200
    And The response should contain "{{emailPlaneAppName}}"
    # Admin plane: the shipped application policies are readable, asserted on shipped data and not on a bare 200
    When I retrieve all "application" throttling policies
    Then The response status code should be 200
    And The response should contain "Unlimited"

    Examples:
      | actor                | expectedUsername                 |
      | userKey1             | testUser1@email.com@carbon.super |
      | userKey1@tenant1.com | testUser11@email.com@tenant1.com |
