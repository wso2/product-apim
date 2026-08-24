@cleanup
Feature: Publisher New API Version Subscriber Notification

  Publishing a NEW VERSION of an API that already has subscribers mails those subscribers. Ports
  NotificationTestCase, whose own success branch was the vacuous assertTrue(true, "Email received BY Greenmail
  server") — the message was never inspected, so the test passed on any mail at all (and on no mail at all, had
  the branch been reached differently). Here the captured message is asserted: exactly one, to exactly the
  subscriber's address, with exactly the subject the tenant-config Title renders, and a body carrying the new
  version and API name.

  MECHANICS, all verified in source rather than assumed:
  - The trigger is the PUBLISH of the new version, not its creation — LifeCycleUtils.changeLifecycle calls
    sendEmailNotification only when targetStatus is PUBLISHED, once per OLD PUBLISHED version that has
    subscribers.
  - THE EXPECTED MESSAGE COUNT IS DERIVED, NOT GUESSED, AND IS TRUE BY CONSTRUCTION. Because the notifier emits
    one NotificationDTO per old published+subscribed version, the count is exactly the number of such
    PREDECESSORS. This scenario builds a fresh, uniquely-named API family and gives it exactly ONE predecessor —
    v1.0.0, published and subscribed below — and publishes exactly one successor, so the count is 1. Adding a
    second published+subscribed version before the trigger would make it 2: change the pinned number with the
    fixture, never widen the assertion to absorb it.
  - Enablement is per-TENANT, in tenant-config: NotificationsEnabled plus a Notifications entry of type
    new_api_version. The shipped default is "false", so each Examples row flips its OWN tenant's config and
    restores it (registered for cleanup as well, so a mid-scenario failure cannot leak an enabled notifier into
    sibling scenarios on this container).
  - The recipient is the SUBSCRIBER's emailaddress claim, resolved through DefaultClaimsRetriever. The block's
    provisioned users carry no email, so the scenario provisions its own subscriber and sets the claim.
  - The mail leaves the APIM container through the carbon "email" output-event adapter (NOT the axis2 mailto
    transport), pointed at the node backend's SMTP capture sink by this block's [output_adapter.email] overlay.
  - The Title template is "Version $2 of $1 Released" with $1 = the NEW api name and $2 = the NEW version, so
    the subject is fully determined and is pinned exactly rather than by substring.

  Runs in BOTH tenants, and the tenant is what the recipient address is built from ("...@carbon.super" vs
  "...@tenant1.com"), so each row's exact recipient assertion is a statement about ITS tenant's notification —
  not a check that could pass against the other tenant's mail.

  @cap:publisher @feat:versioning @rule:new-version-notification @type:regression @dep:admin @dep:devportal @legacy:NotificationTestCase
  Scenario Outline: Publishing a new version mails the existing subscribers in <tenant>
    Given The system is ready
    And I have valid access tokens as "admin<suffix>"

    # A scenario-owned subscriber with a UNIQUE email address. Unique by construction is also what makes the
    # sink safe under parallelism and makes a stale message impossible: nothing can have been delivered to an
    # address that did not exist before this scenario (which is why no sink reset is issued).
    And I generate a unique value and store it as "notifymailbox"
    And I provision user "notifySubscriber" with roles "Internal/subscriber" in tenant "<tenant>"
    And I set the user claim "http://wso2.org/claims/emailaddress" to "{{notifymailbox}}@<tenant>" for user "notifySubscriber" in tenant "<tenant>"

    # Turn on the new_api_version email notifier for THIS tenant.
    When I capture the tenant configuration as "notifyOriginalConf"
    And I register tenant configuration "notifyOriginalConf" for cleanup
    And I capture the tenant configuration as "notifyEnabledConf"
    And I set the field "NotificationsEnabled" to "true" in the payload "notifyEnabledConf"
    And I set the JSON field "Notifications" from file "artifacts/payloads/notification_new_api_version.json" in the payload "notifyEnabledConf"
    And I update the tenant configuration from "notifyEnabledConf"
    Then The response status code should be 200

    # v1: a PUBLISHED API — only a published old version is walked by the notifier. This is THE ONE predecessor
    # of the family, and the API is created fresh with a unique name, so no other version of it can exist. That
    # is what makes the "exactly 1 message" pin below true by construction rather than by luck.
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "notifyApiId" and deployed it
    When I retrieve the "apis" resource with id "notifyApiId"
    Then The response status code should be 200
    And I extract response field "name" and store it as "notifyApiName"
    When I publish the "apis" resource with id "notifyApiId"
    Then The lifecycle status of API "notifyApiId" should be "Published"

    # ...with a subscriber. The subscription is what makes the notifier fire at all.
    Given The system is ready and I have valid devportal access token as "notifySubscriber<suffix>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "notifyAppPayload"
    And I create an application with payload "notifyAppPayload"
    Then The response status code should be 201
    When I subscribe application "createdAppId" to API "notifyApiId" retrying transient errors as "notifySubId"
    Then The response status code should be 201

    # THE TRIGGER: create v2 and publish it.
    Given I act as "admin<suffix>"
    When I create a new version "2.0.0" of "apis" resource "notifyApiId" with default version "false" as "notifyNewApiId"
    Then The response status code should be 201
    When I deploy the API with id "notifyNewApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "notifyNewApiId"
    Then The lifecycle status of API "notifyNewApiId" should be "Published"

    # The mail. Asserted on the captured message, never on "some mail arrived": exactly one — 1 = the family's
    # single published+subscribed predecessor (v1.0.0 above), so a second message here would be a duplicate — to
    # exactly this tenant's subscriber, with the rendered Title as the whole subject, and the rendered Template
    # naming the new version and the API. The step waits for arrival and then for the count to STOP changing, so
    # a duplicate landing moments later is caught rather than missed.
    Then The captured mailbox of "{{notifymailbox}}@<tenant>" should settle at exactly 1 message within 120 seconds
    And The value of response field "count" should be "1"
    And The response array field "messages" should have exactly 1 entries
    And The value of response field "$.messages[0].to" should be "{{notifymailbox}}@<tenant>"
    And The value of response field "$.messages[0].from" should be "apim-notifications@wso2.test"
    And The value of response field "$.messages[0].subject" should be "Version 2.0.0 of {{notifyApiName}} Released"
    And The response should contain "Version 2.0.0 of the {{notifyApiName}} API is now available in the API Store."

    # Restore the tenant configuration explicitly; the registration above is the failure-safe net.
    When I update the tenant configuration from "notifyOriginalConf"
    Then The response status code should be 200

    Examples:
      | tenant       | suffix       |
      | carbon.super |              |
      | tenant1.com  | @tenant1.com |
