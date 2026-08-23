@cleanup
Feature: Gateway Basic Authentication With Email-Form Usernames

  Closes the email-form fan-out of legacy APISecurityTestCase#testInvokeBasicAuth, whose
  users[] = {apisecUser, apisecUser2@wso2.com, apisecUser2@abc.com} asserted 200 for EVERY entry. v2 already
  covers the plain entry in gateway/security_enforcement.feature; the two email-form entries are closed here.

  WHY THIS LIVES IN ITS OWN BLOCK. A username that is itself an email address only resolves correctly when the
  server sets EnableEmailUserName (rendered from [tenant_mgt] enable_email_domain). Only the
  IntegrationV2-EmailUserName block sets it, and that block also needs initBackend=true for these scenarios,
  since Basic auth must reach the real backend for a 200 to mean anything.

  WHAT MAKES THE @-SPLIT A REAL PRODUCT SURFACE, not a cosmetic variation. With the setting ON, a username is
  split on its LAST "@" only when it carries TWO OR MORE "@" — one "@" is read as a super-tenant login whose
  name happens to contain one. BasicAuthAuthenticator then derives the end user's tenant from that split and
  REFUSES the call (900908) unless it equals the API publisher's tenant. So the split rule is what decides
  whether an email-form credential authenticates at all.

  Behaviour was established by probe against a live 4.7.0 container before these assertions were written; the
  super-tenant and tenant results genuinely DIFFER, so each is pinned exactly in its own scenario rather than
  widened into one permissive assertion (CLAUDE.md §12).

  # Legacy parity: BOTH email domains authenticate. Two different domains are used deliberately — legacy used
  # wso2.com and abc.com — which is what proves the domain part of the name is inert to resolution rather than
  # some special-cased value.
  @cap:gateway @feat:security-enforcement @rule:basic-auth-email-username @type:regression @dep:publisher @legacy:APISecurityTestCase
  Scenario: An email-form username authenticates against a basic-auth API in the super tenant
    Given The system is ready
    And I have valid access tokens as "admin"
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
    When I invoke the API at gateway context "{{beContext}}/1.0.0/customers/123/" with method "GET" using basic auth username "{{beWso2}}" password "Password@123" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""

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
  # that tenants accept ONLY email-form usernames (the super tenant accepts both). So the 403 below is the rule
  # being enforced correctly — do not "fix" it.
  # Both rows are pinned because the CONTRAST is the coverage — asserting only the 200 would leave the
  # qualifier rule untested, and asserting only the 403 would miss the parity legacy actually required.
  @cap:gateway @feat:security-enforcement @rule:basic-auth-email-username @type:regression @dep:publisher @legacy:APISecurityTestCase
  Scenario: An email-form username authenticates against a tenant-deployed basic-auth API only when tenant-qualified
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

  # An email-form principal as the OAuth RESOURCE OWNER, invoked TWICE. The second call is the point: the
  # analogous DOTTED-username defect (key-manager/backend_jwt.feature) reproduced only on the second invocation,
  # because the first populates a cache that the second reads back. An "@" in the name exercises the same
  # generation/decode path, so a single call would not catch that class of regression.
  # The username itself is not asserted — 4.7.0 masks the JWT subject/enduser to a pseudonymous UUID (verified by
  # probe: no X-JWT-Assertion reached the backend here), so a clean 200 on BOTH calls is the observable, matching
  # what backend_jwt.feature asserts for the dotted case.
  # Both tenants (CLAUDE.md §12). The tenant-qualified credential carries two "@", so it resolves to the named
  # tenant in both cases — the same form works for the token endpoint either side.
  @cap:gateway @feat:security-enforcement @rule:basic-auth-email-username @type:regression @dep:key-manager @legacy:APISecurityTestCase
  Scenario Outline: An email-form principal works as an OAuth resource owner across repeated invocations as <actor>
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

    When I provision a user with name prefix "boOwner" and email domain "wso2.com" password "Password@123" and roles "Internal/subscriber" storing the username as "boOwner"
    # The tenant-qualified form (two "@") is what the token endpoint accepts in BOTH tenants.
    When I request an OAuth access token using password grant as user "{{boOwnerLoginName}}" with password "Password@123"
    Then The response status code should be 200

    # First invocation.
    When I invoke the API at gateway context "{{boContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""
    # Second invocation — the regression-catching one.
    When I invoke the API at gateway context "{{boContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # An admin password reset invalidates an email-form user's EXISTING Basic credential, in BOTH tenants
  # (CLAUDE.md §12). The tenant row is meaningful because the tenant-QUALIFIED credential authenticates before
  # the reset (200, pinned above) — so the refusal afterwards is attributable to the reset, not to the
  # credential having been refused all along.
  @cap:gateway @feat:security-enforcement @rule:basic-auth-email-username @type:regression @dep:publisher @legacy:APISecurityTestCase
  Scenario Outline: Resetting an email-form user's password invalidates the old basic credential as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_oauth_basicauth_api.json" as "bpApiId" and deployed it
    When I publish the "apis" resource with id "bpApiId"
    Then The lifecycle status of API "bpApiId" should be "Published"
    When I retrieve the "apis" resource with id "bpApiId"
    And I extract response field "context" and store it as "bpContext"
    And the "apis" resource "bpApiId" should be live on the gateway, redeploying if propagation is lost
    And I wait until "apis" "bpApiId" revision is deployed in the gateway

    When I provision a user with name prefix "bpUser" and email domain "wso2.com" password "Password@123" and roles "Internal/subscriber" storing the username as "bpUser"
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
      | admin             |
      | admin@tenant1.com |
