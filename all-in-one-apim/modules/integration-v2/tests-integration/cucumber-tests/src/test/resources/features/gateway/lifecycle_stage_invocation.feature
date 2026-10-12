@cleanup
Feature: Gateway Lifecycle-Stage Invocation

  Ports the lifecycle-stage invocation semantics from the legacy APIRevisionTestCase: the gateway's runtime
  response to an invocation depends on the
  API's lifecycle state — a deployed-but-unpublished (CREATED) API is invocable via the publisher internal key,
  a PUBLISHED API via a subscription token (200), a BLOCKED API is refused (503), a DEPRECATED API is still
  invocable (200), and a RETIRED API is gone (404). Run in BOTH the super tenant and tenant1.com to prove the
  lifecycle enforcement is tenant-agnostic (the tenant API is addressed by its full /t/<tenant> context). Runs
  in the concurrent IntegrationV2-Gateway block (backend started). Teardown via the per-scenario cleanup hook.

  # Also provides the gateway-plane parity for AccessibilityOfBlockAPITestCase (published->blocked; blocked
  # invocation refused — legacy asserts 503). This outline retires the API after published->blocked->deprecated;
  # AccessibilityOfRetireAPITestCase's never-blocked published->deprecated->retired arc is the next scenario. The
  # store-visibility side of retire (the devportal 403 after retire) is ported in publisher/api_lifecycle.feature.
  @cap:gateway @feat:rest-invocation @type:regression @dep:publisher @legacy:APIRevisionTestCase @legacy:AccessibilityOfBlockAPITestCase @legacy:AccessibilityOfRetireAPITestCase @legacy:APISecurityTestCase
  Scenario Outline: The gateway response to an invocation tracks the API lifecycle state as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "revApiId" and deployed it
    When I retrieve the "apis" resource with id "revApiId"
    And I extract response field "context" and store it as "revContext"

    # CREATED (deployed, not yet published): invocable via the publisher internal API key.
    When I generate an internal API key for API "revApiId" and store it as "internalKey"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{revContext}}/1.0.0/customers/123/" with method "GET" using internal key "internalKey" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    # PUBLISHED: invocable via an application subscription token.
    When I publish the "apis" resource with id "revApiId"
    Then The lifecycle status of API "revApiId" should be "Published"

    # The internal key minted while the API was CREATED still authenticates AFTER the publish — publishing does
    # not invalidate an already-issued internal key (APISecurityTestCase asserts this explicitly).
    When I invoke the API at gateway context "{{revContext}}/1.0.0/customers/123/" with method "GET" using internal key "internalKey" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    When I have set up application with keys, subscribed to API "revApiId", and obtained access token for "revSub"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{revContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    When I invoke the API at gateway context "{{revContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" with request header "Accept" set to "text/xml" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "<id>123</id><name>John</name></Customer>"

    # BLOCKED: the gateway refuses the invocation with 503 AND returns the API-blocked fault payload. The FAULT CODE
    # is asserted, not just the status: 503 is also what an unreachable/suspended backend returns (303001), so the
    # code is what distinguishes "the gateway deliberately blocked this API" from "the upstream is down" — and it is
    # the assertion AccessibilityOfBlockAPITestCase makes (HTTP_RESPONSE_DATA_API_BLOCK, code 700700).
    When I change the lifecycle of API "revApiId" with action "Block"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{revContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 503 within 60 seconds
    Then The response status code should be 503
    # Exact fields, not substrings. The gateway fault envelope is code/type/description on a NON-2xx status, so
    # neither the management-envelope step (which reads `message`) nor the response-field step (which requires
    # 2xx) fits — hence the dedicated fault step.
    And The fault response should have code "700700" type "API blocked" and description "This API has been blocked temporarily. Please try again later or contact the system administrators."
    # The fault is a JSON document whose code is a JSON string.
    And The response should contain "{\"code\":\"700700\",\"type\":\"API blocked\",\"description\":\"This API has been blocked temporarily. Please try again later or contact the system administrators.\"}"

    # DEPRECATED: still invocable (200).
    When I change the lifecycle of API "revApiId" with action "Deprecate"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{revContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    # RETIRED: undeployed from the gateway → 404.
    When I change the lifecycle of API "revApiId" with action "Retire"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{revContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 404 within 60 seconds
    Then The response status code should be 404
    And The fault response should have code "404" type "Status report" and description "The requested resource is not available."
    And The response should contain "\"message\":\"Not Found\""

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports AccessibilityOfRetireAPITestCase — a PUBLISHED API taken straight to DEPRECATED and then RETIRED (never
  # blocked) is undeployed from the gateway. Each change-lifecycle response carries the new state. After retire the
  # token minted while PUBLISHED gets the unknown-context 404 document ("Not Found" / "The requested resource is not
  # available."), which is distinct from the unmatched-resource 404 ("Runtime Error") a still-deployed API gives.
  @cap:gateway @feat:rest-invocation @type:regression @dep:publisher @legacy:AccessibilityOfRetireAPITestCase
  Scenario Outline: A published API deprecated and then retired is undeployed from the gateway as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "retApiId" and deployed it
    When I retrieve the "apis" resource with id "retApiId"
    And I extract response field "context" and store it as "retContext"
    When I publish the "apis" resource with id "retApiId"
    Then The lifecycle status of API "retApiId" should be "Published"
    When I have set up application with keys, subscribed to API "retApiId", and obtained access token for "retSub"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{retContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    When I change the lifecycle of API "retApiId" with action "Deprecate"
    Then The response status code should be 200
    And The value of response field "lifecycleState.state" should be "Deprecated"
    And The lifecycle status of API "retApiId" should be "Deprecated"

    When I change the lifecycle of API "retApiId" with action "Retire"
    Then The response status code should be 200
    And The value of response field "lifecycleState.state" should be "Retired"
    And The lifecycle status of API "retApiId" should be "Retired"
    When I invoke the API at gateway context "{{retContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 404 within 60 seconds
    Then The response status code should be 404
    And The fault response should have code "404" type "Status report" and description "The requested resource is not available."
    And The response should contain "\"message\":\"Not Found\""

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports APIAccessibilityOfPublishedOldAPIAndPublishedCopyAPITestCase and the direct-v2-subscription ordering
  # from AccessibilityOfDeprecatedOldAPIAndPublishedCopyAPITestCase: publish v1 and its v2 copy, subscribe one
  # application directly to both while v1 is still Published, then deprecate v1. Both versions remain invocable
  # and the v2 subscription remains usable. The old + new context share the same base context; only the version
  # path segment differs. Runs in both tenants (the tenant context carries /t/<tenant>).
  @cap:gateway @feat:rest-invocation @rule:versioning @type:regression @dep:publisher @legacy:APIAccessibilityOfPublishedOldAPIAndPublishedCopyAPITestCase
  Scenario Outline: A published API and its published copy are both invocable at the gateway as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "oldApiId" and deployed it
    When I retrieve the "apis" resource with id "oldApiId"
    And I extract response field "context" and store it as "oldContext"
    When I publish the "apis" resource with id "oldApiId"
    Then The lifecycle status of API "oldApiId" should be "Published"

    # Copy v1 -> v2 (not default), deploy and publish the copy without deprecating v1.
    When I create a new version "2.0.0" of "apis" resource "oldApiId" with default version "false" as "newApiId"
    Then The response status code should be 201
    When I deploy the API with id "newApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "newApiId"
    Then The lifecycle status of API "newApiId" should be "Published"
    And The lifecycle status of API "oldApiId" should be "Published"

    # Both versions are readable by id in the devportal. v2 is read first: it is 403 until published, so its 200
    # proves the post-publish state that the v1 read then observes.
    When I retrieve the devportal API "newApiId" until the response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "{{newApiId}}"
    When I retrieve the devportal API "oldApiId" until the response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "{{oldApiId}}"

    # Subscribe one app to BOTH versions and obtain a token that carries both subscriptions.
    When I have set up application with keys, subscribed to API "oldApiId", and obtained access token for "oldSub"
    Then The response status code should be 200
    When I put the following JSON payload in context as "newVerSub"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Bronze"}
    """
    And I subscribe to API "newApiId" using application "createdAppId" with payload "newVerSub" as "newSub"
    Then The response status code should be 201
    When I request an access token for application id "createdAppId" using payload "createApplicationAccessTokenPayload"
    Then The response status code should be 200

    # Both versions invocable at their version-specific gateway paths.
    When I invoke the API at gateway context "{{oldContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    When I invoke the API at gateway context "{{oldContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" with request header "Accept" set to "text/xml" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "<id>123</id><name>John</name></Customer>"
    When I invoke the API at gateway context "{{oldContext}}/2.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    When I invoke the API at gateway context "{{oldContext}}/2.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" with request header "Accept" set to "text/xml" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "<id>123</id><name>John</name></Customer>"

    # The app has an explicit subscription to the copied v2 (not only the original v1 subscription). Deprecating
    # v1 afterwards must not invalidate that v2 subscription or its token.
    When I change the lifecycle of API "oldApiId" with action "Deprecate"
    Then The response status code should be 200
    And The lifecycle status of API "oldApiId" should be "Deprecated"
    And The lifecycle status of API "newApiId" should be "Published"
    When I invoke the API at gateway context "{{oldContext}}/2.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports AccessibilityOfOldAPIAndCopyAPIWithOutReSubscriptionTestCase — publish v1, subscribe an app to v1, copy
  # to v2 and publish WITHOUT requiring re-subscription: the v1 subscription/token is honoured on v2, so v2 is
  # invocable (200) even though the app never subscribed to v2 directly. This is the default publish behaviour
  # (no re-subscription checklist).
  @cap:gateway @feat:rest-invocation @rule:versioning @type:regression @dep:publisher @legacy:AccessibilityOfOldAPIAndCopyAPIWithOutReSubscriptionTestCase
  Scenario Outline: A copied API published without re-subscription honours the old subscription token as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "noResubApiId" and deployed it
    When I retrieve the "apis" resource with id "noResubApiId"
    And I extract response field "context" and store it as "noResubContext"
    When I publish the "apis" resource with id "noResubApiId"
    Then The lifecycle status of API "noResubApiId" should be "Published"

    # Subscribe an app to v1 and obtain its token.
    When I have set up application with keys, subscribed to API "noResubApiId", and obtained access token for "noResubSub"
    Then The response status code should be 200

    # Copy to v2, deploy and publish WITHOUT the re-subscription option (default).
    When I create a new version "2.0.0" of "apis" resource "noResubApiId" with default version "false" as "noResubNewApiId"
    Then The response status code should be 201
    When I deploy the API with id "noResubNewApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "noResubNewApiId"
    Then The lifecycle status of API "noResubNewApiId" should be "Published"

    # The v1 token invokes v2 successfully — no re-subscription required.
    When I invoke the API at gateway context "{{noResubContext}}/2.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    # Preserve the legacy content-negotiation assertion in this exact no-re-subscription state: the v1 token must
    # invoke the copied v2 as XML without having a direct v2 subscription.
    When I invoke the API at gateway context "{{noResubContext}}/2.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" with request header "Accept" set to "text/xml" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "<id>123</id><name>John</name></Customer>"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports AccessibilityOfDeprecatedOldAPIAndPublishedCopyAPITestCase — publishing v2 WITH the "Deprecate old
  # versions after publishing the API" checklist auto-deprecates the previously-published v1, while v2 becomes
  # PUBLISHED. The DEPRECATED v1 keeps serving existing subscriptions (still invocable, 200 — a deprecated API is
  # not blocked, only hidden from new subscribers), and the new v2 is invocable too. Uses the checklist-carrying
  # lifecycle step.
  @cap:gateway @feat:rest-invocation @rule:versioning @type:regression @dep:publisher @legacy:AccessibilityOfDeprecatedOldAPIAndPublishedCopyAPITestCase
  Scenario Outline: Publishing a copy with deprecate-old-versions auto-deprecates the old API but keeps it invocable as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "depOldApiId" and deployed it
    When I retrieve the "apis" resource with id "depOldApiId"
    And I extract response field "context" and store it as "depContext"
    When I publish the "apis" resource with id "depOldApiId"
    Then The lifecycle status of API "depOldApiId" should be "Published"

    # Subscribe an app to v1 before deprecation and obtain its token.
    When I have set up application with keys, subscribed to API "depOldApiId", and obtained access token for "depSub"
    Then The response status code should be 200

    # Copy to v2, deploy, and publish WITH the deprecate-old-versions checklist.
    When I create a new version "2.0.0" of "apis" resource "depOldApiId" with default version "false" as "depNewApiId"
    Then The response status code should be 201
    When I deploy the API with id "depNewApiId"
    Then The response status code should be 201
    When I change the lifecycle of API "depNewApiId" with action "Publish" and checklist "Deprecate old versions after publishing the API:true"
    Then The response status code should be 200

    # v2 is Published; v1 was auto-deprecated.
    And The lifecycle status of API "depNewApiId" should be "Published"
    And The lifecycle status of API "depOldApiId" should be "Deprecated"

    # The newer version stays readable by id in the devportal once its sibling is deprecated.
    When I retrieve the devportal API "depNewApiId" until the response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "{{depNewApiId}}"
    And The value of response field "lifeCycleStatus" should be "PUBLISHED"

    # The deprecated v1 still serves its existing subscription (200). Because the v1 subscription pre-dated the
    # v2 copy, it was inherited onto v2 at create-version time (and the deprecate-old-versions publish does NOT
    # require re-subscription), so the SAME token invokes v2 directly (200) — no re-subscription needed.
    When I invoke the API at gateway context "{{depContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    When I invoke the API at gateway context "{{depContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" with request header "Accept" set to "text/xml" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "<id>123</id><name>John</name></Customer>"
    When I invoke the API at gateway context "{{depContext}}/2.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    When I invoke the API at gateway context "{{depContext}}/2.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" with request header "Accept" set to "text/xml" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "<id>123</id><name>John</name></Customer>"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports AccessibilityOfOldAPIAndCopyAPIWithReSubscriptionTestCase — publishing v2 WITH the "Requires
  # re-subscription when publishing the API" checklist means the v1 subscription token does NOT carry over to v2:
  # the v1 token still invokes v1 (200) but is refused on v2 (an unsubscribed-but-valid token at the gateway is
  # 403 — verified live) until the app re-subscribes to v2 and mints a fresh token, after which v2 is invocable
  # (200). Uses the checklist-carrying lifecycle step.
  @cap:gateway @feat:rest-invocation @rule:versioning @type:regression @dep:publisher @legacy:AccessibilityOfOldAPIAndCopyAPIWithReSubscriptionTestCase
  Scenario Outline: A copy published requiring re-subscription refuses the old token on the new version until re-subscribed as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "resubApiId" and deployed it
    When I retrieve the "apis" resource with id "resubApiId"
    And I extract response field "context" and store it as "resubContext"
    When I publish the "apis" resource with id "resubApiId"
    Then The lifecycle status of API "resubApiId" should be "Published"

    # Register both grants used by legacy: keep the existing client-credentials path and allow the password-grant
    # exchange after re-subscription. Use the same bounded stale-mapping recovery as the application setup composite.
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "resubAppPayload"
    And I create an application with payload "resubAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "resubKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for fresh application id "createdAppId" with payload "resubKeysPayload", retrying on 409 stale mapping conflicts
    Then The response status code should be 200
    When I put the following JSON payload in context as "resubV1SubPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Bronze"}
    """
    And I subscribe to API "resubApiId" using application "createdAppId" with payload "resubV1SubPayload" as "resubSubId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "resubClientCredentialsTokenPayload"
    """
    {"consumerSecret": "{{appConsumerSecret}}", "validityPeriod": 3600}
    """
    And I request an access token for application id "createdAppId" using payload "resubClientCredentialsTokenPayload"
    Then The response status code should be 200

    # Copy to v2, deploy, and publish WITH the re-subscription-required checklist.
    When I create a new version "2.0.0" of "apis" resource "resubApiId" with default version "false" as "resubNewApiId"
    Then The response status code should be 201
    When I deploy the API with id "resubNewApiId"
    Then The response status code should be 201
    When I change the lifecycle of API "resubNewApiId" with action "Publish" and checklist "Requires re-subscription when publishing the API:true"
    Then The response status code should be 200
    And The lifecycle status of API "resubNewApiId" should be "Published"

    # The v1 token still invokes v1 (200) but is refused on v2 (403) — re-subscription is required.
    When I invoke the API at gateway context "{{resubContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    When I invoke the API at gateway context "{{resubContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" with request header "Accept" set to "text/xml" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "<id>123</id><name>John</name></Customer>"
    When I invoke the API at gateway context "{{resubContext}}/2.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403
    # The refusal is the subscription-validation fault, not another 403 cause.
    And The error response should have code "900908" message "Resource forbidden " and description containing "User is NOT authorized to access the Resource. API Subscription validation failed."

    # Re-subscribe the app to v2 and mint a fresh token; v2 is now invocable.
    When I put the following JSON payload in context as "resubNewSub"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Bronze"}
    """
    And I subscribe to API "resubNewApiId" using application "createdAppId" with payload "resubNewSub" as "resubNewSubId"
    Then The response status code should be 201
    When I request an access token for application id "createdAppId" using payload "resubClientCredentialsTokenPayload"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{resubContext}}/2.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    When I invoke the API at gateway context "{{resubContext}}/2.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" with request header "Accept" set to "text/xml" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "<id>123</id><name>John</name></Customer>"

    # Legacy re-issued the user's token through password grant after subscribing to v2. Keep that distinct grant
    # path and prove the newly issued token can invoke the re-subscribed version.
    When I request an OAuth access token for the current user using password grant with scope "PRODUCTION"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{resubContext}}/2.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" with request header "Accept" set to "text/xml" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "<id>123</id><name>John</name></Customer>"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports EditAPIContextAndCheckAccessibilityTestCase — an API's context is immutable after creation: submitting an
  # API update whose context field is changed does NOT re-route the API. The original context keeps serving (200)
  # and the changed context is never routable (404). The JSON calls below retain V2's field-level checks; additional
  # legacy-compatible calls use the exact /customers/123 path (no trailing slash) and Accept: text/xml, ensuring
  # that path form and negotiated response also remain valid before and after the context update. The update itself
  # is accepted by the publisher (the context field is
  # ignored, not rejected) — verified live: the PUT returns 200 but the routing context is unchanged. Runs
  # x2-tenant (super + tenant1): the old context is invoked verbatim (it already carries the /t/<tenant> prefix),
  # and the candidate new context is invoked via the tenant-prefixing "resource at path" variant so the 404
  # negative signal is clean in BOTH tenants (a bare path would 404 trivially for the tenant row).
  @cap:gateway @feat:rest-invocation @rule:context-immutable @type:regression @dep:publisher @legacy:EditAPIContextAndCheckAccessibilityTestCase
  Scenario Outline: Editing an API context does not change gateway routing as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "ctxApiId" and deployed it
    When I retrieve the "apis" resource with id "ctxApiId"
    And I extract response field "context" and store it as "ctxOldContext"
    And I put the response payload in context as "ctxPayload"
    When I publish the "apis" resource with id "ctxApiId"
    Then The lifecycle status of API "ctxApiId" should be "Published"
    When I have set up application with keys, subscribed to API "ctxApiId", and obtained access token for "ctxSub"
    Then The response status code should be 200

    # Original context is invocable.
    When I invoke the API at gateway context "{{ctxOldContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    # Legacy invokes this exact resource path without a trailing slash and negotiates XML.
    When I invoke the API at gateway context "{{ctxOldContext}}/1.0.0/customers/123" with method "GET" using access token "generatedAccessToken" and payload "" with request header "Accept" set to "text/xml" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "<id>123</id><name>John</name></Customer>"

    # Submit an update that changes the context to a fresh unique value; redeploy so any routing change would
    # take effect. The candidate new context is captured up-front so it can be invoked verbatim afterwards.
    When I generate a unique value and store it as "ctxNewContext"
    And I set the field "context" to "{{ctxNewContext}}" in the payload "ctxPayload"
    And I update "apis" resource of id "ctxApiId" with payload "ctxPayload"
    Then The response status code should be 200
    And The value of response field "context" should be "{{ctxOldContext}}"
    When I deploy the API with id "ctxApiId"
    Then The response status code should be 201
    # The invocations below run against the redeployed revision, not the one deployed before the update.
    And the "apis" resource "ctxApiId" should be live on the gateway, redeploying if propagation is lost

    # The original context still serves (context is immutable); the attempted new context is never routable (404).
    When I invoke the API at gateway context "{{ctxOldContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    # The legacy XML/no-trailing-slash contract must continue to route through the immutable original context too.
    When I invoke the API at gateway context "{{ctxOldContext}}/1.0.0/customers/123" with method "GET" using access token "generatedAccessToken" and payload "" with request header "Accept" set to "text/xml" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "<id>123</id><name>John</name></Customer>"
    When I invoke the API resource at path "/{{ctxNewContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 404 within 60 seconds
    Then The response status code should be 404
    And The fault response should have code "404" type "Status report" and description "The requested resource is not available."
    And The response should contain "\"message\":\"Not Found\""

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # The publisher INTERNAL KEY is scoped to the API it was minted for, and survives publication. Closes the
  # untested half of APISecurityTestCase#testCreateAndDeployRevisionWithInternalKeyTesting: the lifecycle scenario
  # above already proves an internal key invokes its own CREATED (deployed-but-unpublished) API with 200, but never
  # that the key is API-SCOPED nor that it keeps working after the API is published. Both are asserted here on two
  # APIs deployed side by side: A's key invokes A (200) but is REFUSED on B (403 — an authenticated-but-unauthorised
  # credential, not a 401), B's own key invokes B (200), and after A is PUBLISHED its key still invokes it (200).
  # Runs in both tenants (each API is addressed by its own full /t/<tenant> context).
  @cap:gateway @feat:rest-invocation @rule:internal-key @type:regression @dep:publisher @legacy:APISecurityTestCase
  Scenario Outline: An internal API key is scoped to its own API and keeps working after publish as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "ikApiA" and deployed it
    When I retrieve the "apis" resource with id "ikApiA"
    And I extract response field "context" and store it as "ikContextA"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "ikApiB" and deployed it
    When I retrieve the "apis" resource with id "ikApiB"
    And I extract response field "context" and store it as "ikContextB"

    When I generate an internal API key for API "ikApiA" and store it as "ikKeyA"
    Then The response status code should be 200
    When I generate an internal API key for API "ikApiB" and store it as "ikKeyB"
    Then The response status code should be 200

    # A's key on A (CREATED, unpublished) → 200; B's key on B → 200. Both are valid keys, which is what makes the
    # cross-API refusal below a statement about SCOPE rather than about a bad key.
    When I invoke the API at gateway context "{{ikContextA}}/1.0.0/customers/123/" with method "GET" using internal key "ikKeyA" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{ikContextB}}/1.0.0/customers/123/" with method "GET" using internal key "ikKeyB" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    # A's key presented on B → refused with 403 (the key authenticates but is not authorised for this API).
    When I invoke the API at gateway context "{{ikContextB}}/1.0.0/customers/123/" with method "GET" using internal key "ikKeyA" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403

    # Publishing A does not invalidate its internal key — it still invokes A (200).
    When I publish the "apis" resource with id "ikApiA"
    Then The lifecycle status of API "ikApiA" should be "Published"
    When I invoke the API at gateway context "{{ikContextA}}/1.0.0/customers/123/" with method "GET" using internal key "ikKeyA" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
