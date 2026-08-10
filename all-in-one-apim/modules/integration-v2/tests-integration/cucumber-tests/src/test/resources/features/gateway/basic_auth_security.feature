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
    When I deploy the API with id "baApiId"
    Then The response status code should be 201
    And I wait until "apis" "baApiId" revision is deployed in the gateway
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
    And The response should contain "{\"id\":123,\"name\":\"John\"}"
    When I invoke the API at gateway context "{{baContext}}/1.0.0/customers/123/" with method "GET" using basic auth for actor "userKey1@<tenant>" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "{\"id\":123,\"name\":\"John\"}"
    When I invoke the API at gateway context "{{baContext}}/1.0.0/customers/123/" with method "GET" using basic auth for actor "publisherUser@<tenant>" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "{\"id\":123,\"name\":\"John\"}"
    When I invoke the API at gateway context "{{baContext}}/1.0.0/customers/123/" with method "GET" using basic auth for actor "subscriberUser@<tenant>" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "{\"id\":123,\"name\":\"John\"}"
    # The email-style username: the "@" in the LOCAL part must not be mistaken for the tenant separator.
    When I invoke the API at gateway context "{{baContext}}/1.0.0/customers/123/" with method "GET" using basic auth username "{{baUsr}}@wso2.com@<tenant>" password "Basic@Pass123" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "{\"id\":123,\"name\":\"John\"}"

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
  # which would be a different failure mode entirely.
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
    And The response should contain "{\"id\":123,\"name\":\"John\"}"

    # A username that was never provisioned, qualified to this tenant -> 401.
    When I generate a unique alphanumeric value and store it as "bnUsr"
    And I invoke the API at gateway context "{{bnContext}}/1.0.0/customers/123/" with method "GET" using basic auth username "{{bnUsr}}@<tenant>" password "anyPassword123" until response status code becomes 401 within 60 seconds
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
  Scenario Outline: A basic_auth-only API refuses a valid OAuth2 bearer token and a valid API key
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
    And The response should contain "{\"id\":123,\"name\":\"John\"}"

    # A valid OAuth2 bearer token of the subscribed application -> 401 (testInvokeBearerTokenForBasicNegative).
    When I invoke the API at gateway context "{{bsContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    # A valid API key of the same application -> 401 (testInvokeAPIKeyForBasicOauthAPINegative).
    When I invoke the API at gateway context "{{bsContext}}/1.0.0/customers/123/" with method "GET" using api key "apiKey" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401

  # Ports testInvokeInternalKeyForBasicAuthOnlyAPI — a deliberate POSITIVE, not a negative. The publisher's
  # internal API key BYPASSES the API's declared application-security scheme: it invokes a basic_auth-ONLY API
  # successfully (200), even though the OAuth token and API key above are both refused on the same API. That is
  # the internal key's purpose (the publisher try-out path, which must work whatever the API declares), and
  # asserting it keeps a future change that "fixed" the internal key into a 401 from passing silently.
  #
  # Runs x2 tenants so the internal-key scheme bypass is verified in each tenant.

    Examples:
      | actor |
      | admin |
      | admin@tenant1.com |

  @cap:gateway @feat:security-enforcement @rule:internal-key-bypass @type:regression @dep:publisher @legacy:APISecurityTestCase
  Scenario Outline: An internal API key invokes a basic_auth-only API, bypassing the declared scheme
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
    When I deploy the API with id "bkApiId"
    Then The response status code should be 201
    And I wait until "apis" "bkApiId" revision is deployed in the gateway
    When I publish the "apis" resource with id "bkApiId"
    Then The lifecycle status of API "bkApiId" should be "Published"

    # The legacy asserts the generation is a 200 before invoking, so both halves are pinned.
    When I generate an internal API key for API "bkApiId" and store it as "bkInternalKey"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{bkContext}}/1.0.0/customers/123/" with method "GET" using internal key "bkInternalKey" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "{\"id\":123,\"name\":\"John\"}"

    Examples:
      | actor |
      | admin |
      | admin@tenant1.com |
