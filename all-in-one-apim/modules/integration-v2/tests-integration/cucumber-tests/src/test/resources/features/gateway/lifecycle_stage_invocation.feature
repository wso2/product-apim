@cleanup
Feature: Gateway Lifecycle-Stage Invocation

  Ports the lifecycle-stage invocation semantics from the legacy APIRevisionServerRestartTestCase (functional
  concern; the legacy "restart" was incidental): the gateway's runtime response to an invocation depends on the
  API's lifecycle state — a deployed-but-unpublished (CREATED) API is invocable via the publisher internal key,
  a PUBLISHED API via a subscription token (200), a BLOCKED API is refused (503), a DEPRECATED API is still
  invocable (200), and a RETIRED API is gone (404). Run in BOTH the super tenant and tenant1.com to prove the
  lifecycle enforcement is tenant-agnostic (the tenant API is addressed by its full /t/<tenant> context). Runs
  in the concurrent IntegrationV2-Gateway block (backend started). Teardown via the per-scenario cleanup hook.

  # Also provides the gateway-plane parity for AccessibilityOfBlockAPITestCase (published->blocked; blocked
  # invocation refused — legacy asserts 503) and the gateway half of AccessibilityOfRetireAPITestCase
  # (published->deprecated->retired; retired invocation -> 404). The store-visibility side of retire (the
  # devportal 403 after retire) is a publisher/devportal concern and is ported in publisher/api_lifecycle.feature.
  @cap:gateway @feat:rest-invocation @type:regression @dep:publisher @legacy:APIRevisionServerRestartTestCase @legacy:AccessibilityOfBlockAPITestCase @legacy:AccessibilityOfRetireAPITestCase @legacy:APISecurityTestCase
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

    # PUBLISHED: invocable via an application subscription token.
    When I publish the "apis" resource with id "revApiId"
    Then The lifecycle status of API "revApiId" should be "Published"
    When I have set up application with keys, subscribed to API "revApiId", and obtained access token for "revSub"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{revContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    # BLOCKED: the gateway refuses the invocation with 503.
    When I change the lifecycle of API "revApiId" with action "Block"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{revContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 503 within 60 seconds
    Then The response status code should be 503

    # DEPRECATED: still invocable (200).
    When I change the lifecycle of API "revApiId" with action "Deprecate"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{revContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    # RETIRED: undeployed from the gateway → 404.
    When I change the lifecycle of API "revApiId" with action "Retire"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{revContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 404 within 60 seconds
    Then The response status code should be 404

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports APIAccessibilityOfPublishedOldAPIAndPublishedCopyAPITestCase — publish v1, copy it to v2, publish v2
  # WITHOUT deprecating v1: BOTH versions are simultaneously invocable at the gateway (v1 at /ctx/1.0.0, v2 at
  # /ctx/2.0.0) with a token from an app subscribed to both. The old + new context share the same base context;
  # only the version path segment differs. Runs in both tenants (the tenant context carries /t/<tenant>).
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
    When I invoke the API at gateway context "{{oldContext}}/2.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

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

    # The deprecated v1 still serves its existing subscription (200). Because the v1 subscription pre-dated the
    # v2 copy, it was inherited onto v2 at create-version time (and the deprecate-old-versions publish does NOT
    # require re-subscription), so the SAME token invokes v2 directly (200) — no re-subscription needed.
    When I invoke the API at gateway context "{{depContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{depContext}}/2.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

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

    # Subscribe an app to v1 and obtain its token.
    When I have set up application with keys, subscribed to API "resubApiId", and obtained access token for "resubSub"
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
    When I invoke the API at gateway context "{{resubContext}}/2.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403

    # Re-subscribe the app to v2 and mint a fresh token; v2 is now invocable.
    When I put the following JSON payload in context as "resubNewSub"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Bronze"}
    """
    And I subscribe to API "resubNewApiId" using application "createdAppId" with payload "resubNewSub" as "resubNewSubId"
    Then The response status code should be 201
    When I request an access token for application id "createdAppId" using payload "createApplicationAccessTokenPayload"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{resubContext}}/2.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports EditAPIContextAndCheckAccessibilityTestCase — an API's context is immutable after creation: submitting an
  # API update whose context field is changed does NOT re-route the API. The original context keeps serving (200)
  # and the changed context is never routable (404). This mirrors the legacy assertions exactly (old context -> 200,
  # new context -> 404 after the "change"). The update itself is accepted by the publisher (the context field is
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

    # Submit an update that changes the context to a fresh unique value; redeploy so any routing change would
    # take effect. The candidate new context is captured up-front so it can be invoked verbatim afterwards.
    When I generate a unique value and store it as "ctxNewContext"
    And I set the field "context" to "{{ctxNewContext}}" in the payload "ctxPayload"
    And I update "apis" resource of id "ctxApiId" with payload "ctxPayload"
    Then The response status code should be 200
    And I deploy the API with id "ctxApiId"

    # The original context still serves (context is immutable); the attempted new context is never routable (404).
    When I invoke the API at gateway context "{{ctxOldContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the API resource at path "/{{ctxNewContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 404 within 60 seconds
    Then The response status code should be 404

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
