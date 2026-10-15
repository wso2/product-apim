@cleanup
Feature: Gateway Basic-Auth Application Security

  The basic_auth application-security cases of APISecurityTestCase that gateway/security_enforcement.feature's
  single basic-auth scenario does not reach: WHICH principals a basic_auth API authenticates (several distinct
  carbon users, including a username whose local part contains an "@"), that a non-existent user is a DISTINCT
  case from a wrong password for an existing one, that non-Basic credentials are refused on a basic_auth-only
  API, and the one deliberate POSITIVE — an internal API key invokes a basic_auth-only API, bypassing the API's
  declared scheme.

  Each case asserts its own EXACT status. Every scenario is self-contained (it creates and publishes its own
  basic_auth API inline) and teardown is the per-scenario @cleanup hook.

  # Ports testInvokeBasicAuth. The legacy shape matters and is preserved: a wrong password is rejected FIRST,
  # then EACH of several distinct users authenticates, then the SAME wrong password is rejected AGAIN — the
  # closing negative is the point of the test, proving a preceding success does not leave the gate open (e.g. via
  # a cached authentication decision keyed only on the username).
  #
  # The final principal is provisioned at runtime with an EMAIL-STYLE username, so its tenant-qualified form
  # carries TWO "@" characters ("<name>@wso2.com@<tenant>") — the username-parsing risk the legacy exercised with
  # apisecUser2@wso2.com / apisecUser2@abc.com. MultitenantUtils splits the tenant off the LAST "@", so the local
  # part's "@" must survive. This is provisioned per row with a runner-unique name (never a hardcoded one) and
  # removed at the end of the row.
  #
  # Runs in BOTH tenants (x2): the Basic username IS tenant-qualified, so principal resolution is exactly the
  # dimension tenancy can break. A single <tenant> column drives the acting admin, the actor references and the
  # user-store SOAP calls.
  @cap:gateway @feat:security-enforcement @rule:basic-auth-principals @type:regression @dep:publisher @legacy:APISecurityTestCase
  Scenario Outline: A basic_auth API authenticates each carbon principal and keeps rejecting a wrong password (<tenant>)
    Given The system is ready
    And I have valid access tokens as "admin@<tenant>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "baApiId" and deployed it
    When I retrieve the "apis" resource with id "baApiId"
    Then The response status code should be 200
    And I put the response payload in context as "baApiPayload"
    When I update the "apis" resource "baApiId" and "baApiPayload" with configuration type "securityScheme" and value:
      """
      ["basic_auth", "oauth_basic_auth_api_key_mandatory"]
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "baApiId"
    And I extract response field "context" and store it as "baContext"
    And I extract response field "name" and store it as "baApiName"
    # Hot-swap gate. The first invocation below is a WRONG password expecting 401 — and the OLD (default-security)
    # artifact answers a Basic credential with 401 too, so that assertion could be satisfied before the security
    # scheme ever reached synapse. A success probe would fix the gate but destroy the point of the scenario ("before
    # any success"), so gate on synapse's own re-add line instead: it reads the log and issues no request, leaving
    # the wrong-password call the scenario's genuine first invocation.
    And I mark the current end of the server log file "wso2carbon.log"
    When I deploy the API with id "baApiId"
    Then The response status code should be 201
    And I wait until "apis" "baApiId" revision is deployed in the gateway
    And The server log file "wso2carbon.log" should gain a line containing all of the following within 60 seconds
      | {{baApiName}}                                       |
      | was added to the Synapse configuration successfully |
    When I publish the "apis" resource with id "baApiId"
    Then The lifecycle status of API "baApiId" should be "Published"

    # An EMAIL-STYLE principal created for this row: "<unique>@wso2.com" in the primary user store, so its
    # tenant-qualified Basic username is "<unique>@wso2.com@<tenant>" (two "@").
    When I generate a unique alphanumeric value and store it as "baUsr"
    And I provision store user "{{baUsr}}@wso2.com" with password "Basic@Pass123" and roles "Internal/subscriber" in tenant "<tenant>"

    # A valid user with the WRONG password -> 401 (before any success).
    When I invoke the API at gateway context "{{baContext}}/1.0.0/customers/123/" with method "GET" using basic auth for actor "admin@<tenant>" with password "totallyWrongPassword" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401

    # EACH distinct carbon principal authenticates -> 200. Four seeded actors with different role sets
    # (tenant admin, all-roles, creator+publisher, subscriber-only) plus the email-style runtime principal.
    When I invoke the API at gateway context "{{baContext}}/1.0.0/customers/123/" with method "GET" using basic auth for actor "admin@<tenant>" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    When I invoke the API at gateway context "{{baContext}}/1.0.0/customers/123/" with method "GET" using basic auth for actor "userKey1@<tenant>" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    When I invoke the API at gateway context "{{baContext}}/1.0.0/customers/123/" with method "GET" using basic auth for actor "publisherUser@<tenant>" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    When I invoke the API at gateway context "{{baContext}}/1.0.0/customers/123/" with method "GET" using basic auth for actor "subscriberUser@<tenant>" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    # The email-style username: the "@" in the LOCAL part must not be mistaken for the tenant separator.
    When I invoke the API at gateway context "{{baContext}}/1.0.0/customers/123/" with method "GET" using basic auth username "{{baUsr}}@wso2.com@<tenant>" password "Basic@Pass123" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    # The SAME wrong password is STILL rejected after all those successes -> 401.
    When I invoke the API at gateway context "{{baContext}}/1.0.0/customers/123/" with method "GET" using basic auth for actor "admin@<tenant>" with password "totallyWrongPassword" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401

    # Remove the runtime principal. The step's phrasing says "secondary user store" for historical reasons, but
    # its SOAP deleteUser is store-agnostic — an unqualified username resolves in the PRIMARY store, which is
    # where the provision step above put it.
    When I remove the secondary user store user "{{baUsr}}@wso2.com" in tenant "<tenant>"

    Examples:
      | tenant       |
      | carbon.super |
      | tenant1.com  |

  # Ports testInvokeBasicAuthInvalidCredentials2 — a DISTINCT case from the wrong-password negative above. A
  # wrong password for an EXISTING user and credentials for a user that does not exist at all take different
  # paths through the authenticator (credential mismatch vs. principal lookup miss), so they are separate
  # scenarios asserting their own status rather than one widened check. The username is tenant-qualified on
  # purpose: an unqualified made-up name resolves against the super tenant and would answer 403 on a tenant API,
  # which would be a different failure mode entirely. It is built in the block's physical username form, because in
  # an email-username block a non-email name before @<tenant> is itself parsed as a super-tenant email username.
  @cap:gateway @feat:security-enforcement @rule:basic-auth-principals @type:negative @dep:publisher @legacy:APISecurityTestCase
  Scenario Outline: A basic_auth API rejects Basic credentials for a non-existent user (<tenant>)
    Given The system is ready
    And I have valid access tokens as "admin@<tenant>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "bnApiId" and deployed it
    When I retrieve the "apis" resource with id "bnApiId"
    Then The response status code should be 200
    And I put the response payload in context as "bnApiPayload"
    When I update the "apis" resource "bnApiId" and "bnApiPayload" with configuration type "securityScheme" and value:
      """
      ["basic_auth", "oauth_basic_auth_api_key_mandatory"]
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "bnApiId"
    And I extract response field "context" and store it as "bnContext"
    When I deploy the API with id "bnApiId"
    Then The response status code should be 201
    And I wait until "apis" "bnApiId" revision is deployed in the gateway
    When I publish the "apis" resource with id "bnApiId"
    Then The lifecycle status of API "bnApiId" should be "Published"

    # Positive control: the API really does authenticate Basic credentials, so the following 401 is the
    # principal-lookup miss and not a dead route or a misconfigured scheme.
    When I invoke the API at gateway context "{{bnContext}}/1.0.0/customers/123/" with method "GET" using basic auth for actor "admin@<tenant>" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    # A username that was never provisioned, qualified to this tenant -> 401.
    When I generate a unique alphanumeric value and store it as "bnUsr"
    And I store the physical username of unprovisioned user "{{bnUsr}}" as "bnPhysicalUsr"
    And I invoke the API at gateway context "{{bnContext}}/1.0.0/customers/123/" with method "GET" using basic auth username "{{bnPhysicalUsr}}@<tenant>" password "anyPassword123" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401

    Examples:
      | tenant       |
      | carbon.super |
      | tenant1.com  |

  # Ports testInvokeBearerTokenForBasicNegative and testInvokeAPIKeyForBasicOauthAPINegative: on an API whose
  # ONLY declared application-security scheme is basic_auth, neither a valid OAuth2 bearer token nor a valid
  # DevPortal API key is accepted. Both credentials belong to an application that IS subscribed to this API, so
  # the 401 is the scheme gate — not a missing subscription (which would be 403) and not a bad credential.
  #
  # Runs x2 tenants so the scheme gate is verified in each tenant.
  @cap:gateway @feat:security-enforcement @rule:scheme-mismatch @type:negative @dep:publisher @dep:key-manager @legacy:APISecurityTestCase
  Scenario Outline: A basic_auth-only API refuses a valid OAuth2 bearer token and a valid API key as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "bsApiId" and deployed it
    When I retrieve the "apis" resource with id "bsApiId"
    Then The response status code should be 200
    And I put the response payload in context as "bsApiPayload"
    When I update the "apis" resource "bsApiId" and "bsApiPayload" with configuration type "securityScheme" and value:
      """
      ["basic_auth", "oauth_basic_auth_api_key_mandatory"]
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "bsApiId"
    And I extract response field "context" and store it as "bsContext"
    When I deploy the API with id "bsApiId"
    Then The response status code should be 201
    And I wait until "apis" "bsApiId" revision is deployed in the gateway
    When I publish the "apis" resource with id "bsApiId"
    Then The lifecycle status of API "bsApiId" should be "Published"

    # An application subscribed to this API, with an OAuth token and an API key — both valid, both irrelevant here.
    When I have set up application with keys, subscribed to API "bsApiId", and obtained access token for "bsSubId"
    Then The response status code should be 200
    When I put the following JSON payload in context as "bsApiKeyGenPayload"
    """
    {"keyName": "BasicAuthOnlyKey", "validityPeriod": 3600, "additionalProperties": {"permittedIP": "", "permittedReferer": ""}}
    """
    And I request an api key for application id "createdAppId" using payload "bsApiKeyGenPayload"
    Then The response status code should be 200

    # Positive control: Basic credentials ARE accepted, so the API is live and its scheme took effect.
    When I invoke the API at gateway context "{{bsContext}}/1.0.0/customers/123/" with method "GET" using basic auth for actor "<actor>" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    # A valid OAuth2 bearer token of the subscribed application -> 401 (testInvokeBearerTokenForBasicNegative).
    When I invoke the API at gateway context "{{bsContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    # A valid API key of the same application -> 401 (testInvokeAPIKeyForBasicOauthAPINegative).
    When I invoke the API at gateway context "{{bsContext}}/1.0.0/customers/123/" with method "GET" using api key "apiKey" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401

    Examples:
      | actor |
      | admin |
      | admin@tenant1.com |

  # Email-shaped credentials are accepted when email-domain parsing is enabled and rejected with the exact
  # tenant-mismatch contract when it is disabled. This feature verifies both configurations in their own blocks.
  @cap:gateway @feat:security-enforcement @rule:basic-auth-email-username @type:regression @dep:publisher @legacy:APISecurityTestCase
  Scenario: An email-style username authenticates against a basic-auth API in the super tenant
    Given The system is ready
    And I have valid access tokens as "superTenantAdmin"
    And I have created an api from "artifacts/payloads/create_apim_oauth_basicauth_api.json" as "beApiId" and deployed it
    When I publish the "apis" resource with id "beApiId"
    Then The lifecycle status of API "beApiId" should be "Published"
    When I retrieve the "apis" resource with id "beApiId"
    And I extract response field "context" and store it as "beContext"
    And the "apis" resource "beApiId" should be live on the gateway, redeploying if propagation is lost
    And I wait until "apis" "beApiId" revision is deployed in the gateway

    When I provision a user with name prefix "beWso2" and email domain "wso2.com" password "Password@123" and roles "Internal/subscriber" storing the username as "beWso2"
    And I provision a user with name prefix "beAbc" and email domain "abc.com" password "Password@123" and roles "Internal/subscriber" storing the username as "beAbc"

    # apisecUser2@wso2.com equivalent, tenant-qualified. The BODY assertion is what makes this a credential
    # ACCEPTANCE check rather than bare reachability — it proves the credential carried through to the backend.
    When I invoke the API at gateway context "{{beContext}}/1.0.0/customers/123/" with method "GET" using basic auth username "{{beWso2LoginName}}" password "Password@123" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""

    # apisecUser2@abc.com equivalent — a different email domain must behave identically.
    When I invoke the API at gateway context "{{beContext}}/1.0.0/customers/123/" with method "GET" using basic auth username "{{beAbcLoginName}}" password "Password@123" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""

    # The RAW store name, with no tenant suffix, also authenticates in the super tenant: one "@" is read as a
    # super-tenant login, so the derived tenant is carbon.super and matches the publisher's. Pinned because it is
    # the behaviour the last-"@" rule produces, and a change to it would be a silent auth-surface change.
    When I invoke the API at gateway context "{{beContext}}/1.0.0/customers/123/" with method "GET" using basic auth username "{{beWso2}}" password "Password@123" until response status code becomes 403 when email username mode is disabled or 200 when enabled within 60 seconds
    Then the email-shaped Basic-auth response should be accepted with body containing "\"name\":\"John\"" when email username mode is enabled, otherwise rejected with code "900908"

  # The tenant half of the tenant-x2 rule (CLAUDE.md §12). Legacy asserted 200 for every users[] entry, and the
  # doubly-qualified form delivers it here too — but ONLY that form, which is the whole point of the dimension.
  #
  # THE RULE, as observed on a live container. With enable_email_domain ON a username is split on its LAST "@"
  # only when it carries TWO OR MORE; one "@" is read as a super-tenant login whose name happens to contain one.
  # BasicAuthAuthenticator then refuses (900908) unless the derived tenant equals the publisher's:
  #   name@wso2.com@tenant1.com  -> 2 "@" -> tenant1.com -> matches the tenant API      -> 200
  #   name@tenant1.com           -> 1 "@" -> carbon.super -> mismatches the tenant API  -> 403/900908
  # The second line is also why a PLAIN tenant user cannot Basic-auth under email mode; the gateway names it:
  #   "Basic Authentication failure: tenant domain mismatch for user :<user>@tenant1.com@carbon.super"
  # That is SPECIFIED behaviour, not a limitation to work around: the IS docs this configuration follows state
  # that tenants accept ONLY email-style usernames (the super tenant accepts both). So the 403 below is the rule
  # being enforced correctly — do not "fix" it.
  # Both rows are pinned because the CONTRAST is the coverage — asserting only the 200 would leave the
  # qualifier rule untested, and asserting only the 403 would miss the parity legacy actually required.
  @cap:gateway @feat:security-enforcement @rule:basic-auth-email-username @type:regression @dep:publisher @legacy:APISecurityTestCase
  Scenario: An email-style username authenticates against a tenant-deployed basic-auth API only when tenant-qualified
    Given The system is ready
    And I have valid access tokens as "admin@tenant1.com"
    And I have created an api from "artifacts/payloads/create_apim_oauth_basicauth_api.json" as "btApiId" and deployed it
    When I publish the "apis" resource with id "btApiId"
    Then The lifecycle status of API "btApiId" should be "Published"
    When I retrieve the "apis" resource with id "btApiId"
    And I extract response field "context" and store it as "btContext"
    And the "apis" resource "btApiId" should be live on the gateway, redeploying if propagation is lost
    And I wait until "apis" "btApiId" revision is deployed in the gateway

    When I provision a user with name prefix "btWso2" and email domain "wso2.com" password "Password@123" and roles "Internal/subscriber" storing the username as "btWso2"
    And I provision a user with name prefix "btAbc" and email domain "abc.com" password "Password@123" and roles "Internal/subscriber" storing the username as "btAbc"

    # Tenant-qualified (two "@") -> resolves to tenant1.com -> authenticates. This is legacy's parity row.
    When I invoke the API at gateway context "{{btContext}}/1.0.0/customers/123/" with method "GET" using basic auth username "{{btWso2LoginName}}" password "Password@123" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""

    # The second legacy domain must behave identically in the tenant too.
    When I invoke the API at gateway context "{{btContext}}/1.0.0/customers/123/" with method "GET" using basic auth username "{{btAbcLoginName}}" password "Password@123" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""

    # The SAME user's raw store name (one "@") -> resolves to carbon.super -> refused against a tenant API.
    # Exactly 403/900908, never widened: a different 4xx here would be a real change in the auth surface.
    When I invoke the API at gateway context "{{btContext}}/1.0.0/customers/123/" with method "GET" using basic auth username "{{btWso2}}" password "Password@123" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403
    And The error response should have code "900908" message "Resource forbidden " and description containing "Resource forbidden "

  # A mode-aware principal as the OAuth RESOURCE OWNER, invoked TWICE. Email mode uses an email-form username;
  # standard mode uses a plain username because email-domain parsing is disabled. The second call is the point: the
  # analogous DOTTED-username defect (key-manager/backend_jwt.feature) reproduced only on the second invocation,
  # because the first populates a cache that the second reads back. An "@" in the name exercises the same
  # generation/decode path, so a single call would not catch that class of regression.
  # The username itself is not asserted — 4.7.0 masks the JWT subject/enduser to a pseudonymous UUID (verified by
  # probe: no X-JWT-Assertion reached the backend here), so a clean 200 on BOTH calls is the observable, matching
  # what backend_jwt.feature asserts for the dotted case.
  # Both tenants (CLAUDE.md §12). The tenant-qualified credential carries two "@", so it resolves to the named
  # tenant in both cases — the same form works for the token endpoint either side.
  @cap:gateway @feat:security-enforcement @rule:basic-auth-email-username @type:regression @dep:key-manager @legacy:APISecurityTestCase
  Scenario Outline: An email-style principal works as an OAuth resource owner across repeated invocations as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "boApiId" and deployed it
    When I publish the "apis" resource with id "boApiId"
    Then The lifecycle status of API "boApiId" should be "Published"
    When I retrieve the "apis" resource with id "boApiId"
    And I extract response field "context" and store it as "boContext"
    And the "apis" resource "boApiId" should be live on the gateway, redeploying if propagation is lost
    And I wait until "apis" "boApiId" revision is deployed in the gateway

    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "boAppPayload"
    And I create an application with payload "boAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "boKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "boKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "boSubPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "boApiId" using application "createdAppId" with payload "boSubPayload" as "boSubId"
    Then The response status code should be 201

    When I provision a user with name prefix "boOwner" and email domain "wso2.com" only in email-user mode password "Password@123" and roles "Internal/subscriber" storing the username as "boOwner"
    # Standard mode provisions a plain username; email-user mode provisions the email form and its tenant-qualified login.
    When I request an OAuth access token using password grant as user "{{boOwnerLoginName}}" with password "Password@123"
    Then The response status code should be 200

    # First invocation.
    When I invoke the API at gateway context "{{boContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""
    # Second invocation — the regression-catching one. The BODY assertion below is the real detector here: the
    # exact-count step would assert the repeat's status without retrying, but it does not store a response, so the
    # body check would silently re-read the FIRST call's. Keeping until-200 preserves the stronger body assertion,
    # and the regression it guards is deterministic (a poisoned cache fails every retry, not just the first).
    When I invoke the API at gateway context "{{boContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""

    Examples:
      | actor             |
      | superTenantAdmin  |
      | admin@tenant1.com |

  # An admin password reset invalidates an email-style user's EXISTING Basic credential, in BOTH tenants
  # (CLAUDE.md §12). The tenant row is meaningful because the tenant-QUALIFIED credential authenticates before
  # the reset (200, pinned above) — so the refusal afterwards is attributable to the reset, not to the
  # credential having been refused all along.
  @cap:gateway @feat:security-enforcement @rule:basic-auth-email-username @type:regression @dep:publisher @legacy:APISecurityTestCase
  Scenario Outline: Resetting an email-style user's password invalidates the old basic credential as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_oauth_basicauth_api.json" as "bpApiId" and deployed it
    When I publish the "apis" resource with id "bpApiId"
    Then The lifecycle status of API "bpApiId" should be "Published"
    When I retrieve the "apis" resource with id "bpApiId"
    And I extract response field "context" and store it as "bpContext"
    And the "apis" resource "bpApiId" should be live on the gateway, redeploying if propagation is lost
    And I wait until "apis" "bpApiId" revision is deployed in the gateway

    When I provision a user with name prefix "bpUser" and email domain "wso2.com" only in email-user mode password "Password@123" and roles "Internal/subscriber" storing the username as "bpUser"
    # Works BEFORE the reset — otherwise the refusal below would prove nothing.
    When I invoke the API at gateway context "{{bpContext}}/1.0.0/customers/123/" with method "GET" using basic auth username "{{bpUserLoginName}}" password "Password@123" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""

    When I change the password of user "bpUser" to "Changed@456" as the tenant admin

    # The OLD credential is now refused as invalid — 900901, distinct from the 900908 tenant-mismatch refusal.
    When I invoke the API at gateway context "{{bpContext}}/1.0.0/customers/123/" with method "GET" using basic auth username "{{bpUserLoginName}}" password "Password@123" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    And The error response should have code "900901" message "Invalid Credentials" and description containing "Make sure you have provided the correct security credentials"
    # The NEW credential works, proving the account was re-credentialed rather than disabled.
    When I invoke the API at gateway context "{{bpContext}}/1.0.0/customers/123/" with method "GET" using basic auth username "{{bpUserLoginName}}" password "Changed@456" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    Examples:
      | actor             |
      | superTenantAdmin  |
      | admin@tenant1.com |

  # Ports testInvokeInternalKeyForBasicAuthOnlyAPI — a deliberate POSITIVE, not a negative. The publisher's
  # internal API key BYPASSES the API's declared application-security scheme: it invokes a basic_auth-ONLY API
  # successfully (200), even though the OAuth token and API key above are both refused on the same API. That is
  # the internal key's purpose (the publisher try-out path, which must work whatever the API declares), and
  # asserting it keeps a future change that "fixed" the internal key into a 401 from passing silently.
  #
  # Runs x2 tenants so the internal-key scheme bypass is verified in each tenant.
  @cap:gateway @feat:security-enforcement @rule:internal-key-bypass @type:regression @dep:publisher @legacy:APISecurityTestCase
  Scenario Outline: An internal API key invokes a basic_auth-only API, bypassing the declared scheme as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "bkApiId" and deployed it
    When I retrieve the "apis" resource with id "bkApiId"
    Then The response status code should be 200
    And I put the response payload in context as "bkApiPayload"
    When I update the "apis" resource "bkApiId" and "bkApiPayload" with configuration type "securityScheme" and value:
      """
      ["basic_auth", "oauth_basic_auth_api_key_mandatory"]
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "bkApiId"
    And I extract response field "context" and store it as "bkContext"
    And I extract response field "name" and store it as "bkApiName"
    # Hot-swap gate. An internal API key is validated independently of the declared scheme, so it returns 200
    # against the OLD (default-security) artifact as well — an until-200 poll would be satisfied before basic_auth
    # ever reached synapse, and the scenario would "prove" the bypass without the API being basic_auth-only yet.
    # Synapse's own re-add line is the only signal here that separates the two artifacts.
    And I mark the current end of the server log file "wso2carbon.log"
    When I deploy the API with id "bkApiId"
    Then The response status code should be 201
    And I wait until "apis" "bkApiId" revision is deployed in the gateway
    And The server log file "wso2carbon.log" should gain a line containing all of the following within 60 seconds
      | {{bkApiName}}                                       |
      | was added to the Synapse configuration successfully |
    When I publish the "apis" resource with id "bkApiId"
    Then The lifecycle status of API "bkApiId" should be "Published"

    # The legacy asserts the generation is a 200 before invoking, so both halves are pinned.
    When I generate an internal API key for API "bkApiId" and store it as "bkInternalKey"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{bkContext}}/1.0.0/customers/123/" with method "GET" using internal key "bkInternalKey" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    Examples:
      | actor |
      | admin |
      | admin@tenant1.com |
