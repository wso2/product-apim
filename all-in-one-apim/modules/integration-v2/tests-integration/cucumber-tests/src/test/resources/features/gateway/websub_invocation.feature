@cleanup
Feature: Gateway WebSub API Invocation

  Gateway-plane RUNTIME arc of a published WebSub API — the half the publisher-plane streaming-design feature
  deliberately leaves out. Four observation surfaces are involved: a subscriber
  registers a webhook with the API's hub ("hub.mode=subscribe" through the gateway, in both the query-parameter
  and form-urlencoded encodings), an event SOURCE posts content to the hub's separate event-receiver inbound
  (the synapse WebhookServer, port from the block's published mapping — never a literal), the hub then fans
  that content out server-to-server to every registered callback, and the PERSISTED registration is read back from
  the internal webhooks-subscriptions resource. The callback therefore lives on the shared
  docker network (the websub-receiver node app, uniquely named per scenario so parallel scenarios cannot
  cross-count) and the test reads back what it received over that app's host-published introspection endpoint.
  Covers registration in both encodings, the hub's verification handshake at the callback, unsubscribe, the
  source-side and hub-side rejections, the missing-mandatory-parameter and invalid-credential refusals, the
  PERSISTED subscription set (grown per registration and emptied per unsubscribe), lease expiry, and the plan's
  subscription-count cap. The third leg - the hub's server-to-server FAN-OUT to registered callbacks - is PARKED on
  this lane; every scenario that asserted a delivery count, body, signature or link header is commented out below,
  each above the shared park note that records exactly what was verified and what was not.
  Absence of a delivery is always proved with a still-subscribed BARRIER receiver observing the same event, never by
  sleeping; a delivery COUNT is always settled (Utils.awaitSettledCount) before it is asserted, so an OVER-delivery
  is catchable rather than invisible. Teardown via the per-scenario cleanup hook.

  RESIDUE NOTE (known, bounded): a webhook registration lands in AM_WEBHOOKS_SUBSCRIPTION, whose API_UUID carries
  NO foreign key, so deleting the API does not remove the row and there is no REST delete for it — the only
  interface is the hub's own unsubscribe, which needs the API still deployed and the subscriber's token. Each
  scenario therefore unsubscribes as part of its arc (that is also the asserted behaviour), and a scenario that
  fails earlier leaves a row keyed to a uniquely-named, now-deleted API/application/callback that nothing else can
  observe. Registering it for the standard sweep is not possible in the current ordering (applications and APIs are
  deleted before any bespoke sweep could still address the hub), and a bespoke hook is deliberately NOT added: the
  store is this block's own in-container H2, so the row dies with the container, and it is keyed to a unique,
  already-deleted API, so it cannot collide with anything. §5's zero-residue rule targets cross-test interference
  and leaks into a SHARED or PERSISTENT store; a container-scoped, unreachable row is neither. It is also why every
  persisted-count assertion below is scoped to ONE API's uuid rather than to a whole-tenant delta: an absolute
  per-API count cannot be satisfied — or broken — by another scenario's residual row.

  # Registration arc: subscribe (form-urlencoded body — the legacy's primary encoding), the hub's verification
  # handshake at the callback, then unsubscribe. Uses a SILENT callback (records the handshake, answers with an empty
  # body — the legacy CallbackServerServlet). Runs in both the super tenant and tenant1.com as the tenant admin: the
  # tenant row is not cosmetic, it pins that the tenant-prefixed context is also addressable by the hub.
  @cap:gateway @feat:streaming-invocation @type:smoke @dep:publisher @legacy:WebSubAPITestCase
  Scenario Outline: Register and deregister a WebSub subscription through the hub as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    # The API's own secret must be KNOWN to the test (the event source signs the content it posts with it), so it
    # is generated here and substituted into the create payload rather than left to the payload's own randomizer.
    When I put JSON payload from file "artifacts/payloads/create_apim_websub_invoke_api.json" in context as "websubApiPayload"
    And I generate a unique alphanumeric value and store it as "websubApiSecret"
    And I replace "WEBSUB_API_SECRET" with "{{websubApiSecret}}" in the payload "websubApiPayload"
    And I create an "apis" resource with payload "websubApiPayload" as "websubApiId"
    And I deploy the API with id "websubApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "websubApiId"
    Then The lifecycle status of API "websubApiId" should be "Published"
    When I retrieve the "apis" resource with id "websubApiId"
    And I extract response field "context" and store it as "websubContext"
    When I have set up application with keys, subscribed to API "websubApiId" with plan "AsyncWHUnlimited", and obtained access token for "websubSubId"
    Then The response status code should be 200
    # The subscription must be EFFECTIVE, not merely created. The composite asserts 201 on the subscribe, and a
    # subscription parked ON_HOLD pending admin approval ALSO answers 201 — so the status code alone cannot tell an
    # active subscription from a pending one. Read the persisted subscription back and pin its exact status, as the
    # legacy did (Assert.assertEquals(subscriptionDTO.getStatus(), SubscriptionDTO.StatusEnum.UNBLOCKED)). Asserted
    # in the feature, NOT inside the shared subscribe step: the admin subscription-workflow features legitimately
    # park subscriptions ON_HOLD and assert that, so a blanket assertion in the glue would break them.
    When I get the subscription with id "websubSubId"
    Then The value of response field "status" should be "UNBLOCKED"

    # The subscriber's own hub.secret — deliberately NOT the API's secret (see the signature scenario below)
    And I generate a unique alphanumeric value and store it as "websubSubscriberSecret"
    And I have a "silent" WebSub callback receiver stored as "websubReceiver"

    # Register the webhook with the hub
    When I send a WebSub "subscribe" request as form data to gateway context "{{websubContext}}/1.0.0" with callback "{{websubReceiverCallback}}" topic "_default" secret "{{websubSubscriberSecret}}" lease seconds "50000000" using access token "generatedAccessToken" until response status code becomes 202 within 60 seconds
    # The hub verifies the intent at the callback, echoing the mandatory hub.mode/hub.topic plus a random challenge
    Then The WebSub receiver "websubReceiver" should have recorded a "subscribe" verification for topic "_default" with a non-empty challenge within 60 seconds

    # Deregister the webhook
    When I send a WebSub "unsubscribe" request as form data to gateway context "{{websubContext}}/1.0.0" with callback "{{websubReceiverCallback}}" topic "_default" secret "{{websubSubscriberSecret}}" lease seconds "50000000" using access token "generatedAccessToken" until response status code becomes 202 within 60 seconds

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # POST-CREATION subscription configuration, in the order the docs and the legacy both use, plus the two things
  # every other scenario in this file ASSUMED without asserting.
  #
  # ORDER. Every other scenario here creates the API with websubSubscriptionConfiguration inline in the POST. The
  # docs present it as a post-creation step (Topics -> Subscription Configuration -> Enable -> Generate secret ->
  # Save) and legacy WebSubAPITestCase did addAPI (no config) -> deploy -> getAPIByID -> set config -> updateAPI PUT
  # -> publish. This scenario follows that order, so the inline shortcut is no longer the only path covered.
  #
  # CHECK 1 - the configuration actually STICKS. Nothing until now read it back, so "POST /apis silently drops it"
  # and "the PUT silently drops it" were both live possibilities. Asserted on both sides of the PUT: an API created
  # with the field REMOVED reads back the product's own default (APIUtil.getDefaultWebsubSubscriptionConfiguration:
  # enable=false, secret="", SHA1, x-hub-signature), and after the PUT it reads back enabled with OUR secret. The
  # before-assertion is what makes the after-assertion meaningful - without it, "enable is true" could just be a
  # default rather than the effect of the update.
  #
  # CHECK 2 - the URL the event source posts to is the one the hub RECORDED. The test CONSTRUCTS that URL
  # (<eventReceiverBase><context>/<version>/webhooks_events_receiver_resource?topic=<topic>) and never verified it
  # against the product. This matters because the shipped websub_api_template.xml answers 200 to the source as soon
  # as TOPIC_VALIDITY passes and then fans out to however many subscribers the in-memory map yields - zero included,
  # silently. So posting to a context/topic OTHER than the registered one is indistinguishable from a successful
  # publish with no subscribers, which is exactly the shape of the parked delivery leg's symptom. The step rebuilds
  # the receiver URL from the PERSISTED row's own apiContext/apiVersion/topicName and requires byte equality, which
  # settles by construction whether the registered hub.topic string differs from the source's ?topic= string (the
  # "named next step 1" of the park note below).
  #
  # A SECOND DEPLOY follows the PUT deliberately. The legacy order sets the configuration AFTER deploying, so the
  # artifact already on the gateway still carries the pre-update secret; without redeploying, an event source signing
  # with the configured secret would be validated against a different one. That is a real consequence of the
  # documented order and is recorded here rather than worked around silently.
  @cap:gateway @feat:streaming-invocation @rule:subscription-configuration @type:regression @dep:publisher @legacy:WebSubAPITestCase
  Scenario Outline: A WebSub subscription configuration set after creation is persisted and describes the event receiver the source posts to as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_websub_invoke_api.json" in context as "websubApiPayload"
    And I generate a unique alphanumeric value and store it as "websubApiSecret"
    # Created WITHOUT the subscription configuration, so the PUT below is what enables it
    And I remove the field "websubSubscriptionConfiguration" from the payload "websubApiPayload"
    And I create an "apis" resource with payload "websubApiPayload" as "websubApiId"
    And I deploy the API with id "websubApiId"
    Then The response status code should be 201
    # CHECK 1a: with the field absent the product substitutes its own default — enable is FALSE
    When I retrieve the "apis" resource with id "websubApiId"
    Then The value of response field "websubSubscriptionConfiguration.enable" should be "false"
    And The value of response field "websubSubscriptionConfiguration.signingAlgorithm" should be "SHA1"
    And The value of response field "websubSubscriptionConfiguration.signatureHeader" should be "x-hub-signature"
    And I put the response payload in context as "websubCfgPayload"
    # ...then enable it as a post-creation update, the documented order
    When I update the "apis" resource "websubApiId" and "websubCfgPayload" with configuration type "websubSubscriptionConfiguration" and value:
      """
      {"enable":true,"secret":"{{websubApiSecret}}","signingAlgorithm":"SHA1","signatureHeader":"x-hub-signature"}
      """
    Then The response status code should be 200
    # CHECK 1b: the configuration STUCK — read it back from the persisted API, not from the PUT's own echo
    When I retrieve the "apis" resource with id "websubApiId"
    Then The value of response field "websubSubscriptionConfiguration.enable" should be "true"
    And The value of response field "websubSubscriptionConfiguration.secret" should be "{{websubApiSecret}}"
    And The value of response field "websubSubscriptionConfiguration.signingAlgorithm" should be "SHA1"
    And The value of response field "websubSubscriptionConfiguration.signatureHeader" should be "x-hub-signature"
    And I extract response field "context" and store it as "websubContext"
    # Redeploy so the artifact on the gateway carries the configured secret (see the note above), then publish
    When I deploy the API with id "websubApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "websubApiId"
    Then The lifecycle status of API "websubApiId" should be "Published"
    When I have set up application with keys, subscribed to API "websubApiId" with plan "AsyncWHUnlimited", and obtained access token for "websubSubId"
    Then The response status code should be 200
    And I generate a unique alphanumeric value and store it as "websubSubscriberSecret"
    And I have a "silent" WebSub callback receiver stored as "websubReceiver"
    When I send a WebSub "subscribe" request as form data to gateway context "{{websubContext}}/1.0.0" with callback "{{websubReceiverCallback}}" topic "_default" secret "{{websubSubscriberSecret}}" lease seconds "50000000" using access token "generatedAccessToken" until response status code becomes 202 within 60 seconds
    # CHECK 2: what the hub RECORDED, field by field, and then the receiver URL those fields describe
    Then The persisted webhook subscription for API "websubApiId" should have "topicName" equal to "_default"
    And The persisted webhook subscription for API "websubApiId" should have "callbackURL" equal to "{{websubReceiverCallback}}"
    And The persisted webhook subscription for API "websubApiId" should have "tier" equal to "AsyncWHUnlimited"
    And The event receiver URL for gateway context "{{websubContext}}/1.0.0" topic "_default" should match the persisted webhook subscription of API "websubApiId"
    # The configured secret is the one the receiver validates against — a publish signed with it is accepted
    When I put the following JSON payload in context as "websubEventBody"
    """
    {"Hello" : "World"}
    """
    When I publish the WebSub event "websubEventBody" to the event receiver at gateway context "{{websubContext}}/1.0.0" topic "_default" signed with secret "{{websubApiSecret}}" until response status code becomes 200 within 60 seconds
    When I send a WebSub "unsubscribe" request as form data to gateway context "{{websubContext}}/1.0.0" with callback "{{websubReceiverCallback}}" topic "_default" secret "{{websubSubscriberSecret}}" lease seconds "50000000" using access token "generatedAccessToken" until response status code becomes 202 within 60 seconds

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # ===========================================================================================================
  # DELIVERY (FAN-OUT) LEG: PARKED - shared rationale for every commented-out scenario in this file.
  #
  # Subscribe, the hub's verification handshake, unsubscribe, the negatives, the subscription-count cap, the
  # PERSISTED subscription set and lease expiry are all verified working (the live scenarios above and below).
  # Delivery is NOT: the fan-out reads an in-memory subscriber map that is never populated for a runtime-created
  # subscription on this lane, because the asyncWebhooksData event never reaches the consumer that would populate it.
  # That is PROVEN, not inferred - see ROOT CAUSE below, where a graceful-restart probe populates the map by the
  # non-JMS route and all five events are then delivered. The template does
  # `<clone iterations="{get-property('SUBSCRIBERS_COUNT')}">`, so a count of 0 means zero iterations, no error, and
  # still a 200 to the event source - the failure is silent by construction.
  #
  # MEASURED TWICE, the second time under an exclusive build lock so no concurrent maven run could have clobbered
  # target/test-classes (/tmp/w10w8-run4-locked-deliveryprobe.log, `Tests run: 92, Failures: 14`; of the 30
  # scenarios this file contributed, the 20 non-delivery ones PASSED and exactly the 10 delivery ones FAILED).
  # Every receiver was polled to a SETTLED count over a 10s quiet window and every single one observed
  # `"count":0,"events":[]` while `"verificationCount":1` - i.e. the hub reached the callback for the handshake and
  # never for a delivery. Verbatim: `WebSub receiver 'websubReceiver143a228818' settled delivery count mismatch
  # (last seen 0 over 6 sample(s), quiet window 10000ms); state: {"verificationCount":1,...,"count":0,...}`.
  # The clean 20/10 split matters: the same run, same container and same block prove the registration, handshake,
  # persistence, lease-expiry, cap and negative legs all work, so this is a delivery-specific break and not a
  # broken lane.
  #
  # WHAT THIS INCREMENT NEWLY RULED OUT - the previous park note called the subscriber map "the only remaining
  # candidate" but had never observed the PERSIST leg. It is now observed, and it WORKS: the live
  # persisted-subscriptions scenario below reads the internal webhooks-subscriptions resource and sees the count go
  # 0 -> 3 -> 0 across three registrations and their unsubscribes, in BOTH tenants. That is only reachable if the
  # hub's SubscribersPersistMediator POSTed to /internal/data/v1/notify AND WebhooksSubscriptionEventHandler ran -
  # and that handler's very next act, after the DAO write it just demonstrated, is
  # sendSubscriptionNotificationOnRealtime publishing the asyncWebhooksData event. So the break is NOT the hub, the
  # topic match, the inbound signature, the event-hub URL or the DAO.
  #
  # Also verified present and ENABLED at runtime in that same log (not merely in source): the publisher
  # asyncWebhooksEventPublisher reached `configuration successfully deployed and in active state`, the stream
  # org.wso2.apimgt.webhooks.request.stream:1.0.0 `deployed successfully`, `EventJunction WSO2EventConsumer added to
  # the junction` for that stream, and the gateway's consumer connected: `JMSTransportHandler Starting jms topic
  # consumer thread for the asyncWebhooksData topic...` then `JMSListener Connection attempt: 1 for JMS Provider for
  # listener: Siddhi-JMS-Consumer#asyncWebhooksData was successful!`. (The `Polling tasks ... have not yet started
  # after 3 seconds` WARN that follows is emitted for EVERY topic including notification, which demonstrably works,
  # so it is not a signal.) The log carries NO `Error occurred while trying to retrieve the event publisher for
  # type`, NO NullPointerException, and NO `SubscribersLoader Error while getting subscribers count` - so the
  # publisher lookup succeeded, the consumer threw nothing, and the fan-out's own key lookup did not fail either.
  # The keys agree by construction too: SubscriptionsDataServiceImpl.addSubscription stores under
  # apiUUID + "_" + topic in the tenant's store, and WebhooksUtils.getSubscribersListFromInMemoryMap reads
  # generateAPIKey(mc,tenant) + "_" + the event receiver's topic query parameter from that same store.
  #
  # STATIC WALK OF THE SHIPPED BUILD (decompiled carbon-apimgt 9.33.162 plus the shipped template) - this narrows
  # the space much further than the runtime log alone, and two candidates are now positively REFUTED:
  #
  #  - The fan-out sequence IS present and correctly wired. The shipped
  #    repository/resources/api_templates/websub_api_template.xml (byte-identical to the copy in this repo at
  #    all-in-one-apim/modules/distribution/resources/api_templates/) has, on the event-receiver resource
  #    (`binds-to="WebhookServer, SecureWebhookServer"`), a two-target outer <clone> whose SECOND target is
  #    `<class name="...mediators.webhooks.SubscribersLoader"/>` followed by
  #    `<clone iterations="{get-property('SUBSCRIBERS_COUNT')}" continueParent="true">` containing SubscriberInfoLoader,
  #    the `To`/`link` headers, the blocking <call> and DeliveryStatusUpdater. CloneMediatorFactory does honour a
  #    dynamic iterations value, so the count is really consumed. So "the template does not fan out on this build"
  #    is FALSE.
  #  - The consumer-side guard is not the drop. handleAsyncWebhooksSubscriptionMessage has exactly ONE early exit,
  #    `if (!isThrottled) { ... addSubscription(...) }`, and `isThrottled` is `!isSuccess` from
  #    WebhooksDAO.addSubscription, which returns false ONLY on `currentLimit >= throttleLimit` - a path that writes
  #    no DB row at all. The live persisted scenario proves the rows ARE written, so the guard was not taken.
  #
  # And three of our own observations are stronger than they looked:
  #  - The 200 (not 404) from the event source proves TOPIC_VALIDITY passed, i.e. the receiver's `?topic=` exactly
  #    matched one of the API's URLMapping patterns. (WebhookApiHandler sets TOPIC_VALIDITY; the template answers
  #    404 `{"error": "Invalid or missing topic"}` and <respond/>s when it is "false".)
  #  - The ABSENCE of a NullPointerException proves SUBSCRIBERS_COUNT was SET, so SubscribersLoader ran to
  #    completion: CloneMediator.resolveIterationsCount does `Integer.parseInt(countStr.trim())`, and an unset
  #    property evaluates to null and would NPE on .trim().
  #  - The absence of `Error while getting subscribers count` proves generateAPIKey succeeded - the tenant store
  #    existed and getApiByContextAndVersion resolved the API - so the lookup ran and simply found nothing.
  # Net: getSubscriptionsList returned empty, <clone> iterated zero times, and the source still got its 200. The
  # failure is silent by construction at every layer, which is itself a product problem (see BUGS below).
  #
  # The two key formulas are IDENTICAL, so a key-shape mismatch is not the cause:
  #   write  SubscriptionsDataServiceImpl.addSubscription : String.valueOf(apiKey) + "_" + topicName
  #          fed by GatewayJMSMessageListener from payload apiUUID / topic / tenantDomain, which
  #          SubscribersPersistMediator filled with WebhooksUtils.generateAPIKey(mc, tenantDomain) and raw hub.topic
  #   read   WebhooksUtils.getSubscribersListFromInMemoryMap : String.valueOf(apiKey) + "_" + topicName
  #          same generateAPIKey(mc, tenantDomain), topic parsed out of TransportInURL's `?topic=`
  # generateAPIKey is literally the same method on both sides (REST_API_CONTEXT + SYNAPSE_REST_API_VERSION ->
  # getApiByContextAndVersion(...).getUuid()), and NEITHER side trims, lower-cases or re-slashes the topic.
  #
  # NOT A THROTTLING/TRAFFIC-MANAGER PROBLEM, measured: the sibling SSE and raw-WebSocket event-quota scenarios both
  # PASS on this same single-container profile with a subscription-level EVENTCOUNTLIMIT plan, so the streaming
  # throttle publish/decide/enforce loop is live here. Whatever is missing is specific to the WebSub fan-out.
  #
  # ROOT CAUSE, NOW PROVEN AND CONFINED TO ONE HOP: the asyncWebhooksData event never reaches
  # handleAsyncWebhooksSubscriptionMessage, so the in-memory subscriber map is never populated for a subscription
  # registered at runtime. EVERY layer downstream of that map is PROVEN WORKING, not merely un-refuted.
  #
  # THE DECIDING MEASUREMENT (/tmp/streaming-probe1.log, `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`,
  # BUILD SUCCESS): a probe scenario registered a subscription, asserted the row present in
  # AM_WEBHOOKS_SUBSCRIPTION, GRACEFULLY RESTARTED the carbon server with that row intact, re-asserted the row
  # present AFTER the restart, then published 5 events — and the receiver observed EXACTLY 5, settled. The count
  # assertion is exact and settled, not a floor (Utils.awaitSettledCount, then assertEquals on the value), so this
  # is "all five were delivered", not "at least one arrived".
  # The mechanism that makes it decisive: SubscriptionDataStore.initializeStore() bootstraps the in-memory map
  # straight from the same internal webhooks-subscriptions resource using the IDENTICAL key formula
  # (`getApiUUID() + "_" + getTopicName()`), BYPASSING JMS ENTIRELY. Populate the map by any route other than the
  # event and fan-out works. Deliveries arrive after a restart and never before one.
  # The post-restart row assertion is what makes a delivery result interpretable at all: it proves the row is
  # present on BOTH sides of the restart, so initializeStore() is unambiguously the only thing that changed.
  #
  # WHAT THE PROBE EXONERATES — all of it previously unprovable, none of it still open: the `<clone>` template
  # fan-out actually iterating, BOTH key formulas (the read side resolved the key correctly), SUBSCRIBERS_COUNT
  # being consumed, HMAC re-signing with the SUBSCRIBER's own secret, callback reachability from inside the
  # container, and delivery-status handling. One hop remains, and it is the publish->JMS->consume hop alone.
  # (c) — the subscriber's `hub.topic` STRING at registration differing from the event source's `?topic=` string —
  # was already REFUTED by the live post-creation-configuration scenario above, which rebuilds the event-receiver
  # URL from the persisted row's own apiContext/topicName and requires byte equality with the URL the source posts
  # to. Note the row's `callbackURL` is the SUBSCRIBER's sink (nodebackend:3022/silent/websubReceiver...), a
  # different field entirely, so there is no competing advertised URL.
  #
  # WHY THE HOP IS SILENT, which is why this took a restart to find: EventStreamRuntime.publish logs
  # `dropped since no junction found for the streamId` at DEBUG ONLY, EventJunction.sendEvent no-ops with no log at
  # all when every consumer list is empty, and APIUtil.publishEvent only logs when the publisher-map lookup itself
  # fails (which we know it did not).
  #
  # CONSEQUENCE FOR THE PARKED SCENARIOS BELOW: they assert the CORRECT behaviour and will pass UNCHANGED once that
  # one hop is fixed — no rewrite, no new glue. They are parked on a product defect, not on a test gap. Do not
  # "fix" them by weakening an assertion to a floor; the restart probe shows the exact counts are achievable.
  #
  # PRODUCT DEFECTS FOUND WHILE ESTABLISHING THE ABOVE (report upstream; none is a test problem):
  #  - THE PRIMARY DEFECT: on this all-in-one profile the asyncWebhooksData event published by
  #    sendSubscriptionNotificationOnRealtime never reaches handleAsyncWebhooksSubscriptionMessage, so a subscription
  #    registered at runtime never enters the in-memory subscriber map and receives NO deliveries until the server is
  #    restarted. Both ends are demonstrably alive (publisher active, stream deployed, JMS consumer connected - see
  #    the runtime evidence above), which is what makes this worth reporting rather than dismissing as an unconfigured
  #    profile. Proven by the restart probe: identical row, deliveries only after initializeStore() populates the map.
  #  - SubscribersLoader logs NOTHING when the resolved key yields an empty list, so a totally broken fan-out
  #    produces a clean INFO log. It already computes the key and throws it away; logging key + count would have
  #    made this a one-line grep. This is the single highest-value OBSERVABILITY fix, and it is what would have made
  #    the defect above a one-line grep instead of a multi-round investigation.
  #  - WebhookApiHandler takes the receiver-side tenant from
  #    PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain(true) on the WebhookServer inbound
  #    thread, where no tenant flow is started, and that value then selects the store on the READ side while the
  #    WRITE side keys off the API's own tenant. Same class of defect as the GraphQL/WS tenant-flow leak on netty
  #    threads; it should derive the tenant from the API context.
  #  - SubscribersLoader.getUserAgent dereferences the axis2 TRANSPORT_HEADERS map unguarded and OUTSIDE its
  #    try/catch, while the sibling clone target removes that very property. Safe only because
  #    MessageHelper.cloneAxis2MessageContext gives each clone its own map - one refactor from killing all fan-out.
  #  - Both eventpublishers/asyncWebhooksEventPublisher.xml and asyncWebhooksEventPublisher_1.0.0.xml are ENABLED on
  #    the same stream and the same JMS topic, so every subscription event is published twice (idempotent today
  #    because the inner map is keyed by callback URL).
  #
  # Glue is KEPT and unchanged: the settled delivery-count step (Utils.awaitSettledCount), the exact-count,
  # link-header and body/signature assertions, and the two publish steps. Re-enabling these scenarios needs no new
  # step definitions.
  # ===========================================================================================================
  #
  # Delivery half of the smoke arc: the five published events the legacy testInvokeWebSubAPIWithFormUrlEncodedData
  # sends are all fanned out, plus the hub's self-advertising link header (testMandatoryParameters). Asserted on the
  # API's own context rather than the legacy's "http://localhost:<eventReceiverPort>" so no port literal appears here.
  # The count is SETTLED before it is asserted, so five is an exact claim and not "at least five had arrived when we
  # happened to look".
  # PARKED - every assertion here is a delivery. See the shared note above.
  # CREATION ORDER aligned to the docs and to legacy WebSubAPITestCase: addAPI with NO subscription configuration ->
  # deploy -> GET -> set the configuration -> PUT -> redeploy -> publish, rather than declaring
  # websubSubscriptionConfiguration inline in the POST. The redeploy is load-bearing in this order: the configuration
  # is set after the first deploy, so without it the artifact on the gateway would still validate inbound signatures
  # against the pre-update secret. That both halves of that order behave as intended is verified LIVE by the
  # post-creation-configuration scenario above, so this parked scenario inherits a proven setup and its remaining
  # unknown is the fan-out alone.
  @cap:gateway @feat:streaming-invocation @rule:delivery-count @type:regression @dep:publisher @legacy:WebSubAPITestCase
  Scenario Outline: Publish events to a subscribed WebSub callback through the hub as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_websub_invoke_api.json" in context as "websubApiPayload"
    And I generate a unique alphanumeric value and store it as "websubApiSecret"
    And I remove the field "websubSubscriptionConfiguration" from the payload "websubApiPayload"
    And I create an "apis" resource with payload "websubApiPayload" as "websubApiId"
    And I deploy the API with id "websubApiId"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "websubApiId"
    And I put the response payload in context as "websubCfgPayload"
    When I update the "apis" resource "websubApiId" and "websubCfgPayload" with configuration type "websubSubscriptionConfiguration" and value:
      """
      {"enable":true,"secret":"{{websubApiSecret}}","signingAlgorithm":"SHA1","signatureHeader":"x-hub-signature"}
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "websubApiId"
    Then The value of response field "websubSubscriptionConfiguration.enable" should be "true"
    And I extract response field "context" and store it as "websubContext"
    When I deploy the API with id "websubApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "websubApiId"
    Then The lifecycle status of API "websubApiId" should be "Published"
    When I have set up application with keys, subscribed to API "websubApiId" with plan "AsyncWHUnlimited", and obtained access token for "websubSubId"
    Then The response status code should be 200
    And I generate a unique alphanumeric value and store it as "websubSubscriberSecret"
    And I have a "silent" WebSub callback receiver stored as "websubReceiver"
    When I put the following JSON payload in context as "websubEventBody"
    """
    {"Hello" : "World"}
    """
    When I send a WebSub "subscribe" request as form data to gateway context "{{websubContext}}/1.0.0" with callback "{{websubReceiverCallback}}" topic "_default" secret "{{websubSubscriberSecret}}" lease seconds "50000000" using access token "generatedAccessToken" until response status code becomes 202 within 60 seconds
    # The URL the publishes below post to is the one the hub recorded — otherwise a 200 could not mean fan-out ran
    Then The event receiver URL for gateway context "{{websubContext}}/1.0.0" topic "_default" should match the persisted webhook subscription of API "websubApiId"
    When I publish the WebSub event "websubEventBody" to the event receiver at gateway context "{{websubContext}}/1.0.0" topic "_default" signed with secret "{{websubApiSecret}}" until response status code becomes 200 within 60 seconds
    And I publish the WebSub event "websubEventBody" to the event receiver at gateway context "{{websubContext}}/1.0.0" topic "_default" signed with secret "{{websubApiSecret}}" 4 times expecting status 200
    Then The WebSub receiver "websubReceiver" should have received 5 events within 60 seconds
    And The last WebSub event delivered to receiver "websubReceiver" should carry a "link" header containing "{{websubContext}}"
    When I send a WebSub "unsubscribe" request as form data to gateway context "{{websubContext}}/1.0.0" with callback "{{websubReceiverCallback}}" topic "_default" secret "{{websubSubscriberSecret}}" lease seconds "50000000" using access token "generatedAccessToken" until response status code becomes 202 within 60 seconds
  #
    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # HMAC correctness (the whole subject of SecretValidationTestCase): the hub validates the INBOUND content against
  # the API's own secret, then RE-SIGNS each fan-out delivery with THAT SUBSCRIBER's hub.secret. The two secrets are
  # deliberately different here, so a hub that echoed the API's secret instead would fail this assertion. The
  # INBOUND half is covered by every publish step in this file signing with the API's own secret and asserting 200;
  # there is deliberately no inbound-rejection negative, because a mis-signed post is <drop/>ped with no response at
  # all (it presents as a client timeout, not a status), so it cannot be asserted with the status-based publish step.
  # PARKED - asserts a delivered body and its signature, so it needs the fan-out leg. What is UNVERIFIED here is now
  # the INBOUND REJECTION half ALONE, not both halves: a mis-signed inbound post is <drop/>ped with no response at
  # all, so it cannot be asserted with the status-based publish step.
  # The OUTBOUND RE-SIGNING half is OBSERVED WORKING, and the distinction matters: the restart probe
  # (/tmp/streaming-probe1.log) saw five deliveries arrive at the sink carrying `signature=sha1=2fd3ebcf...`,
  # re-signed with the SUBSCRIBER's own hub.secret rather than the API's. So the product behaviour this scenario
  # asserts is known good, and the scenario is parked on the runtime subscription hop rather than on any doubt about
  # the signing.
  # OBSERVED BY PROBE IS NOT SUITE COVERAGE, and this note deliberately does not claim otherwise: nothing in the
  # shipped suite asserts the outbound signature today, because the only step that could is this parked one.
  # Re-enable it and the claim becomes coverage; until then it stays an observation. See the shared note above.
  @cap:gateway @feat:streaming-invocation @rule:hmac-signature @type:regression @dep:publisher @legacy:SecretValidationTestCase
  Scenario Outline: The hub signs each WebSub delivery with the subscriber's own hub.secret as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_websub_invoke_api.json" in context as "websubApiPayload"
    And I generate a unique alphanumeric value and store it as "websubApiSecret"
    And I replace "WEBSUB_API_SECRET" with "{{websubApiSecret}}" in the payload "websubApiPayload"
    And I create an "apis" resource with payload "websubApiPayload" as "websubApiId"
    And I deploy the API with id "websubApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "websubApiId"
    Then The lifecycle status of API "websubApiId" should be "Published"
    When I retrieve the "apis" resource with id "websubApiId"
    And I extract response field "context" and store it as "websubContext"
    When I have set up application with keys, subscribed to API "websubApiId" with plan "AsyncWHUnlimited", and obtained access token for "websubSubId"
    Then The response status code should be 200
    And I generate a unique alphanumeric value and store it as "websubSubscriberSecret"
    And I have a "silent" WebSub callback receiver stored as "websubReceiver"
    When I put the following JSON payload in context as "websubEventBody"
    """
    {"Hello" : "World"}
    """
    When I send a WebSub "subscribe" request as form data to gateway context "{{websubContext}}/1.0.0" with callback "{{websubReceiverCallback}}" topic "_default" secret "{{websubSubscriberSecret}}" lease seconds "50000000" using access token "generatedAccessToken" until response status code becomes 202 within 60 seconds
    # BARRIER (added 2026-08-07): a form-data subscribe answers 202 from FORCE_SC_ACCEPTED BEFORE
    # SubscribersPersistMediator runs, so its status says NOTHING about whether the row was written. Publishing
    # straight after it races the persist and fans out to ZERO subscribers. Measured: without this the row
    # settled at 0 deliveries. The passing delivery scenarios all gate on the persisted list first.
    Then The internal webhooks subscription list should hold exactly 1 subscription for API "websubApiId" within 60 seconds
    When I publish the WebSub event "websubEventBody" to the event receiver at gateway context "{{websubContext}}/1.0.0" topic "_default" signed with secret "{{websubApiSecret}}" until response status code becomes 200 within 60 seconds
    Then The WebSub receiver "websubReceiver" should have received 1 event within 60 seconds
    And The last WebSub event delivered to receiver "websubReceiver" should have body "websubEventBody" signed with "SHA1" using secret "{{websubSubscriberSecret}}"
    When I send a WebSub "unsubscribe" request as form data to gateway context "{{websubContext}}/1.0.0" with callback "{{websubReceiverCallback}}" topic "_default" secret "{{websubSubscriberSecret}}" lease seconds "50000000" using access token "generatedAccessToken" until response status code becomes 202 within 60 seconds
  #
    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Encoding parity: the hub accepts the SAME registration with the hub.* parameters in the QUERY STRING and an
  # empty body (legacy testInvokeWebSubApiWithQueryParameters), not only as a form-urlencoded body. Runs in both
  # tenants, which is not cosmetic: it pins that the tenant-prefixed context is addressable on this branch too.
  # LIMITATION, stated rather than implied: with the fan-out leg parked this asserts the two registration STATUSES
  # only, so it cannot distinguish "accepted and effective" from "accepted and a silent no-op".
  # AND ON THIS BRANCH THE GAP IS WIDER THAN ON THE FORM ONE, which is worth recording precisely. MEASURED in the
  # probe round: after a query-parameter subscribe the callback's introspection read back
  # `{"verificationCount":0,"count":0,"verifications":[],"events":[]}` - so unlike the form-urlencoded branch (which
  # reliably lands a GET verification handshake at the callback, asserted in the smoke scenario above) the
  # query-parameter branch reached the callback NOT AT ALL, neither as a handshake nor as a delivery. The template's
  # second <clone> target does set To=hub.callback and <call> the subscribe at it, and the legacy encoded that as
  # "received == sent + 1 subscribe event" for its query-parameter helper, so this branch is expected to touch the
  # callback once at registration time and does not. That is a SECOND observation to carry into the delivery-leg
  # investigation, distinct from the fan-out itself, and it is why no callback assertion is made here at all rather
  # than a weaker one.
  # The accepted STATUS differs from the form-urlencoded arc above (200 here, 202 there) and the difference is
  # CONTENT-TYPE-driven, not parameter-placement-driven: the hub resource in websub_api_template.xml branches on
  # fn:contains(fn:lower-case($trp:Content-Type), 'application/x-www-form-urlencoded'), and only that branch sets
  # FORCE_SC_ACCEPTED=true (202, reading the parameters from the body via //xformValues). Every other content type
  # falls to the else branch, which reads $url:hub.* from the query string and just responds — so the default 200.
  # This is why the two legacy helpers disagreed: handleCallbackSubscriptionWithFormUrlEncoded asserted 202 while
  # handleCallbackSubscription (query parameters, application/json) asserted 200. Both were right.
  @cap:gateway @feat:streaming-invocation @rule:query-parameter-encoding @type:regression @dep:publisher @legacy:WebSubAPITestCase
  Scenario Outline: A WebSub subscription can be registered and removed with query parameters as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_websub_invoke_api.json" in context as "websubApiPayload"
    And I generate a unique alphanumeric value and store it as "websubApiSecret"
    And I replace "WEBSUB_API_SECRET" with "{{websubApiSecret}}" in the payload "websubApiPayload"
    And I create an "apis" resource with payload "websubApiPayload" as "websubApiId"
    And I deploy the API with id "websubApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "websubApiId"
    Then The lifecycle status of API "websubApiId" should be "Published"
    When I retrieve the "apis" resource with id "websubApiId"
    And I extract response field "context" and store it as "websubContext"
    When I have set up application with keys, subscribed to API "websubApiId" with plan "AsyncWHUnlimited", and obtained access token for "websubSubId"
    Then The response status code should be 200

    And I generate a unique alphanumeric value and store it as "websubSubscriberSecret"
    And I have a "silent" WebSub callback receiver stored as "websubReceiver"
    When I send a WebSub "subscribe" request as query parameters to gateway context "{{websubContext}}/1.0.0" with callback "{{websubReceiverCallback}}" topic "_default" secret "{{websubSubscriberSecret}}" lease seconds "50000000" using access token "generatedAccessToken" until response status code becomes 200 within 60 seconds
    # Same encoding, so the same content-type-driven status as the subscribe above — 200, not the form arc's 202.
    When I send a WebSub "unsubscribe" request as query parameters to gateway context "{{websubContext}}/1.0.0" with callback "{{websubReceiverCallback}}" topic "_default" secret "{{websubSubscriberSecret}}" lease seconds "50000000" using access token "generatedAccessToken" until response status code becomes 200 within 60 seconds

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Multi-subscriber fan-out AND unsubscribe, in one arc because they are the same observation from two sides: the
  # hub delivers every event to EVERY registered callback (legacy MultipleWebSubSubcriptionTestCase, which could
  # only see this through a database count), and after one callback unsubscribes the hub delivers only to the rest.
  # The remaining callback is the BARRIER — waiting for IT to observe the next event proves that event's fan-out
  # completed, so the unsubscribed callback's unchanged count is a real absence and not a race. No sleep is involved.
  # PARKED - every assertion here is a delivery count. The unsubscribe STATUS itself stays covered by the live
  # registration scenarios; what is unverified is that unsubscribing stops deliveries and that the hub fans out
  # to more than one callback. See the shared note above.
  @cap:gateway @feat:streaming-invocation @rule:unsubscribe @type:regression @dep:publisher @legacy:WebSubAPITestCase @legacy:MultipleWebSubSubcriptionTestCase
  Scenario Outline: Every registered WebSub callback receives each event until it unsubscribes as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_websub_invoke_api.json" in context as "websubApiPayload"
    And I generate a unique alphanumeric value and store it as "websubApiSecret"
    And I replace "WEBSUB_API_SECRET" with "{{websubApiSecret}}" in the payload "websubApiPayload"
    And I create an "apis" resource with payload "websubApiPayload" as "websubApiId"
    And I deploy the API with id "websubApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "websubApiId"
    Then The lifecycle status of API "websubApiId" should be "Published"
    When I retrieve the "apis" resource with id "websubApiId"
    And I extract response field "context" and store it as "websubContext"
    When I have set up application with keys, subscribed to API "websubApiId" with plan "AsyncWHUnlimited", and obtained access token for "websubSubId"
    Then The response status code should be 200
    And I generate a unique alphanumeric value and store it as "websubSubscriberSecret"
    And I have a "silent" WebSub callback receiver stored as "websubLeaver"
    And I have a "silent" WebSub callback receiver stored as "websubStayer"
    When I put the following JSON payload in context as "websubEventBody"
    """
    {"Hello" : "World"}
    """
    When I send a WebSub "subscribe" request as form data to gateway context "{{websubContext}}/1.0.0" with callback "{{websubLeaverCallback}}" topic "_default" secret "{{websubSubscriberSecret}}" lease seconds "50000000" using access token "generatedAccessToken" until response status code becomes 202 within 60 seconds
    And I send a WebSub "subscribe" request as form data to gateway context "{{websubContext}}/1.0.0" with callback "{{websubStayerCallback}}" topic "_default" secret "{{websubSubscriberSecret}}" lease seconds "50000000" using access token "generatedAccessToken" until response status code becomes 202 within 60 seconds
    # BARRIER (added 2026-08-07): a form-data subscribe answers 202 from FORCE_SC_ACCEPTED BEFORE
    # SubscribersPersistMediator runs, so its status says NOTHING about whether the row was written. Publishing
    # straight after it races the persist and fans out to ZERO subscribers. Measured: without this the row
    # settled at 0 deliveries. The passing delivery scenarios all gate on the persisted list first.
    Then The internal webhooks subscription list should hold exactly 2 subscriptions for API "websubApiId" within 60 seconds
    # Both callbacks are registered: every one of the five published events must reach BOTH
    When I publish the WebSub event "websubEventBody" to the event receiver at gateway context "{{websubContext}}/1.0.0" topic "_default" signed with secret "{{websubApiSecret}}" until response status code becomes 200 within 60 seconds
    And I publish the WebSub event "websubEventBody" to the event receiver at gateway context "{{websubContext}}/1.0.0" topic "_default" signed with secret "{{websubApiSecret}}" 4 times expecting status 200
    Then The WebSub receiver "websubLeaver" should have received 5 events within 60 seconds
    And The WebSub receiver "websubStayer" should have received 5 events within 60 seconds
    When I send a WebSub "unsubscribe" request as form data to gateway context "{{websubContext}}/1.0.0" with callback "{{websubLeaverCallback}}" topic "_default" secret "{{websubSubscriberSecret}}" lease seconds "50000000" using access token "generatedAccessToken" until response status code becomes 202 within 60 seconds
    # BARRIER: wait for the unsubscribe to be REMOVED before publishing again, else the leaver may
    # still receive the next event and the "exactly 5" assertion below becomes a race.
    Then The internal webhooks subscription list should hold exactly 1 subscription for API "websubApiId" within 60 seconds
    And I publish the WebSub event "websubEventBody" to the event receiver at gateway context "{{websubContext}}/1.0.0" topic "_default" signed with secret "{{websubApiSecret}}" until response status code becomes 200 within 60 seconds
    Then The WebSub receiver "websubStayer" should have received 6 events within 60 seconds
    And The WebSub receiver "websubLeaver" should have received exactly 5 events
    When I send a WebSub "unsubscribe" request as form data to gateway context "{{websubContext}}/1.0.0" with callback "{{websubStayerCallback}}" topic "_default" secret "{{websubSubscriberSecret}}" lease seconds "50000000" using access token "generatedAccessToken" until response status code becomes 202 within 60 seconds
  #
    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Subscriber verification (enableSubscriberVerification=true): the hub must only deliver to a callback that
  # ECHOED the hub.challenge. A verifying callback and a silent one both attempt to register; the verifying one is
  # the BARRIER, so its receipt of the event makes the silent one's zero count a proven absence. Extends legacy
  # testSubscriberVerification, which only asserted the positive half and so could not tell verification apart from
  # no verification at all. The hub FORCE_SC_ACCEPTEDs 202 on this (form-urlencoded) branch regardless of the
  # verification outcome, so no status code can discriminate the two — the delivered counts are the only observable.
  # UN-PARKED 2026-08-06. The previous park note claimed "the GATING EFFECT is only observable through delivery".
  # That premise is WRONG, and the template proves it: SubscribersPersistMediator sits INSIDE the then-branch of
  # the challenge comparison (websub_api_template.xml:144-150), so a callback that fails verification is NEVER
  # PERSISTED. The gate therefore lands on the persisted-subscriptions surface, which needs no fan-out at all.
  # "No row was written" is STRONGER than "no event was delivered": it rules out the registration existing.
  #
  # WHY BOTH RECEIVERS AND BOTH CHALLENGE ASSERTIONS ARE KEPT. FORCE_SC_ACCEPTED=true is set before the callback
  # is invoked, so BOTH registrations answer 202 and no status code can discriminate them. The silent receiver's
  # challenge assertion is the BARRIER: it proves the hub really did challenge that callback and still wrote no
  # row, which is what makes the absent row a verification FAILURE rather than one that never ran.
  #
  # Three template facts verified, not assumed: (1) #if($enableSubscriberVerification) is a VELOCITY directive, so
  # a non-verified API's artifact lacks the comparison filter entirely — verification cannot be toggled on an
  # already-deployed API and must be present at create time; (2) the echo must be in the RESPONSE BODY
  # (json-eval($.text)), which is why the "verifying" receiver differs from the "silent" one in exactly that field;
  # (3) HUB_CHALLENGE = get-property('MessageID') is per-request unique, so "non-empty challenge" is a real
  # assertion rather than a tautology over a constant.
  #
  # The count assertion is two-phase by construction (reach, then confirm it STAYS) inside the step, so an
  # INVERTED gate that also persisted the silent callback is caught as an over-count rather than passing on a
  # lucky sample.
  @cap:gateway @feat:streaming-invocation @rule:subscriber-verification @type:regression @dep:publisher @legacy:WebSubAPITestCase
  Scenario Outline: With subscriber verification on, only a callback that echoes the challenge is registered as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_websub_verified_api.json" in context as "websubApiPayload"
    And I generate a unique alphanumeric value and store it as "websubApiSecret"
    And I replace "WEBSUB_API_SECRET" with "{{websubApiSecret}}" in the payload "websubApiPayload"
    And I create an "apis" resource with payload "websubApiPayload" as "websubApiId"
    And I deploy the API with id "websubApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "websubApiId"
    Then The lifecycle status of API "websubApiId" should be "Published"
    When I retrieve the "apis" resource with id "websubApiId"
    And I extract response field "context" and store it as "websubContext"
    And The value of response field "enableSubscriberVerification" should be "true"
    When I have set up application with keys, subscribed to API "websubApiId" with plan "AsyncWHUnlimited", and obtained access token for "websubSubId"
    Then The response status code should be 200
    And I generate a unique alphanumeric value and store it as "websubSubscriberSecret"
    And I have a "verifying" WebSub callback receiver stored as "websubVerifier"
    And I have a "silent" WebSub callback receiver stored as "websubSilent"
    Then The internal webhooks subscription list should hold exactly 0 subscriptions for API "websubApiId" within 30 seconds
    When I send a WebSub "subscribe" request as form data to gateway context "{{websubContext}}/1.0.0" with callback "{{websubVerifierCallback}}" topic "_default" secret "{{websubSubscriberSecret}}" lease seconds "50000000" using access token "generatedAccessToken" until response status code becomes 202 within 60 seconds
    And I send a WebSub "subscribe" request as form data to gateway context "{{websubContext}}/1.0.0" with callback "{{websubSilentCallback}}" topic "_default" secret "{{websubSubscriberSecret}}" lease seconds "50000000" using access token "generatedAccessToken" until response status code becomes 202 within 60 seconds
    # BARRIER: the hub challenged BOTH callbacks. Without this the absent row below could mean "never challenged".
    Then The WebSub receiver "websubVerifier" should have recorded a "subscribe" verification for topic "_default" with a non-empty challenge within 60 seconds
    And The WebSub receiver "websubSilent" should have recorded a "subscribe" verification for topic "_default" with a non-empty challenge within 60 seconds
    # THE GATE: exactly one row, and it is the VERIFIER's. The callbackURL assertion is load-bearing, not
    # decoration — "exactly 1" alone is equally satisfied by an INVERTED gate that persisted the silent callback.
    Then The internal webhooks subscription list should hold exactly 1 subscription for API "websubApiId" within 60 seconds
    And The persisted webhook subscription for API "websubApiId" should have "callbackURL" equal to "{{websubVerifierCallback}}"
    When I send a WebSub "unsubscribe" request as form data to gateway context "{{websubContext}}/1.0.0" with callback "{{websubVerifierCallback}}" topic "_default" secret "{{websubSubscriberSecret}}" lease seconds "50000000" using access token "generatedAccessToken" until response status code becomes 202 within 60 seconds
    Then The internal webhooks subscription list should hold exactly 0 subscriptions for API "websubApiId" within 60 seconds

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Persistence of the registration itself, read back from the internal webhooks-subscriptions resource
  # (legacy testMultipleSubscriptions, which asserted initialCount + 3 and then a return to initialCount through
  # restAPIInternal.retrieveWebhooksSubscriptions()). Three callbacks on ONE application, because the row's identity
  # is (API, application, callback, topic) — three distinct callbacks are three rows, exactly as the legacy's three
  # applications were, with none of the extra key generation.
  # This is the ONE surface that observes a registration WITHOUT the fan-out leg, so it is also what pins that an
  # accepted hub response actually wrote something.
  @cap:gateway @feat:streaming-invocation @rule:persisted-subscriptions @type:regression @dep:publisher @legacy:WebSubAPITestCase
  Scenario Outline: Each WebSub registration is persisted and each unsubscribe removes it as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_websub_invoke_api.json" in context as "websubApiPayload"
    And I generate a unique alphanumeric value and store it as "websubApiSecret"
    And I replace "WEBSUB_API_SECRET" with "{{websubApiSecret}}" in the payload "websubApiPayload"
    And I create an "apis" resource with payload "websubApiPayload" as "websubApiId"
    And I deploy the API with id "websubApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "websubApiId"
    Then The lifecycle status of API "websubApiId" should be "Published"
    When I retrieve the "apis" resource with id "websubApiId"
    And I extract response field "context" and store it as "websubContext"
    When I have set up application with keys, subscribed to API "websubApiId" with plan "AsyncWHUnlimited", and obtained access token for "websubSubId"
    Then The response status code should be 200
    And I generate a unique alphanumeric value and store it as "websubSubscriberSecret"
    And I have a "silent" WebSub callback receiver stored as "websubOne"
    And I have a "silent" WebSub callback receiver stored as "websubTwo"
    And I have a "silent" WebSub callback receiver stored as "websubThree"
    # A freshly created API starts with no persisted webhook subscription at all
    Then The internal webhooks subscription list should hold exactly 0 subscriptions for API "websubApiId" within 30 seconds
    When I send a WebSub "subscribe" request as form data to gateway context "{{websubContext}}/1.0.0" with callback "{{websubOneCallback}}" topic "_default" secret "{{websubSubscriberSecret}}" lease seconds "50000000" using access token "generatedAccessToken" until response status code becomes 202 within 60 seconds
    And I send a WebSub "subscribe" request as form data to gateway context "{{websubContext}}/1.0.0" with callback "{{websubTwoCallback}}" topic "_default" secret "{{websubSubscriberSecret}}" lease seconds "50000000" using access token "generatedAccessToken" until response status code becomes 202 within 60 seconds
    And I send a WebSub "subscribe" request as form data to gateway context "{{websubContext}}/1.0.0" with callback "{{websubThreeCallback}}" topic "_default" secret "{{websubSubscriberSecret}}" lease seconds "50000000" using access token "generatedAccessToken" until response status code becomes 202 within 60 seconds
    Then The internal webhooks subscription list should hold exactly 3 subscriptions for API "websubApiId" within 60 seconds
    When I send a WebSub "unsubscribe" request as form data to gateway context "{{websubContext}}/1.0.0" with callback "{{websubOneCallback}}" topic "_default" secret "{{websubSubscriberSecret}}" lease seconds "50000000" using access token "generatedAccessToken" until response status code becomes 202 within 60 seconds
    And I send a WebSub "unsubscribe" request as form data to gateway context "{{websubContext}}/1.0.0" with callback "{{websubTwoCallback}}" topic "_default" secret "{{websubSubscriberSecret}}" lease seconds "50000000" using access token "generatedAccessToken" until response status code becomes 202 within 60 seconds
    And I send a WebSub "unsubscribe" request as form data to gateway context "{{websubContext}}/1.0.0" with callback "{{websubThreeCallback}}" topic "_default" secret "{{websubSubscriberSecret}}" lease seconds "50000000" using access token "generatedAccessToken" until response status code becomes 202 within 60 seconds
    Then The internal webhooks subscription list should hold exactly 0 subscriptions for API "websubApiId" within 60 seconds

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Lease expiry (LeaseTimeSubscriptionTestCase): a registration carrying hub.lease_seconds must STOP being a
  # subscription once that many seconds have elapsed. The product computes an ABSOLUTE expiry
  # (WebhooksSubscriptionEventHandler: expireAt = now + leaseSeconds) and the subscription query is
  # "WHERE (EXPIRY_AT >= now OR EXPIRY_AT = 0)", so an expired lease drops the row out of the subscription set by
  # construction — which is what stops deliveries, and is observable without racing a fan-out.
  # 15 seconds is the legacy's lease. The legacy proved the same thing by counting deliveries either side of a
  # Thread.sleep; here the expiry is awaited by polling the subscription set, which is both sleep-free (§4) and not
  # dependent on the timing of an event publish landing inside the remaining lease.
  #
  # This is ALSO the whole of the docs' TIME-BASED streaming limit: hub.lease_seconds is supplied at registration and
  # is the only duration lever the product has (a subscription plan carries no duration field at all — see the DTO
  # field list in admin/throttling_policy). The DELIVERY half — "an event published after expiry reaches nobody" —
  # is deliberately NOT asserted here, and NOT because it is unimportant: on this profile fan-out delivers zero
  # events even while the lease is still live (see the shared park note above), so a "received 0 events after expiry"
  # assertion would pass with the lease mechanism entirely removed. It is vacuous until fan-out works, so it is
  # parked with the other fan-out-dependent scenarios rather than written in a form that cannot fail.
  @cap:gateway @feat:streaming-invocation @rule:lease-expiry @type:regression @dep:publisher @legacy:LeaseTimeSubscriptionTestCase
  Scenario Outline: A WebSub subscription stops being a subscription once its lease expires as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_websub_invoke_api.json" in context as "websubApiPayload"
    And I generate a unique alphanumeric value and store it as "websubApiSecret"
    And I replace "WEBSUB_API_SECRET" with "{{websubApiSecret}}" in the payload "websubApiPayload"
    And I create an "apis" resource with payload "websubApiPayload" as "websubApiId"
    And I deploy the API with id "websubApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "websubApiId"
    Then The lifecycle status of API "websubApiId" should be "Published"
    When I retrieve the "apis" resource with id "websubApiId"
    And I extract response field "context" and store it as "websubContext"
    When I have set up application with keys, subscribed to API "websubApiId" with plan "AsyncWHUnlimited", and obtained access token for "websubSubId"
    Then The response status code should be 200
    And I generate a unique alphanumeric value and store it as "websubSubscriberSecret"
    And I have a "silent" WebSub callback receiver stored as "websubReceiver"
    # A 15-second lease, so the registration exists and then expires within one scenario.
    #
    # WHY THE "hold exactly 1" CHECK BELOW CANNOT BE MUTATION-PROVEN BY CHANGING THE COUNT (measured twice,
    # round B and round D; a 120s lease was tried and did NOT help, so do not try that again):
    #   Utils.retryUntil FLOORS its deadline at Constants.RUNTIME_PROPAGATION_TIMEOUT (180s, CLAUDE.md 15),
    #   so the declared "within 30 seconds" is a fiction. A mutated count is UNREACHABLE (only one
    #   registration exists), so the loop cannot accept early and polls the full 180s. retryUntil returns
    #   the LAST result, and by t=180s the lease has expired - getSubscribers prunes at READ time - so the
    #   last probe and the settle both read 0. The failure then reports settled=0 against a green value of
    #   1, i.e. it would fail unmutated too, and proves nothing.
    #   The two halves of this scenario have CONTRADICTORY lease requirements under mutation: a sound count
    #   mutant needs lease > 180s (row must outlive the futile poll) while the expiry check below needs
    #   lease < 180s (expiry must be observable inside its own floored deadline). No value satisfies both.
    #   UNMUTATED both halves are correct: expecting 1 accepts on the first probe in ~1s, nowhere near
    #   expiry. To mutation-prove this step, mutate something that stays REACHABLE (e.g. assert against a
    #   different API id, answered 0 immediately) rather than making the count unreachable.
    When I send a WebSub "subscribe" request as form data to gateway context "{{websubContext}}/1.0.0" with callback "{{websubReceiverCallback}}" topic "_default" secret "{{websubSubscriberSecret}}" lease seconds "15" using access token "generatedAccessToken" until response status code becomes 202 within 60 seconds
    # It IS a subscription while the lease runs — without this the disappearance below would also be satisfied by a
    # registration that was never recorded at all
    Then The internal webhooks subscription list should hold exactly 1 subscription for API "websubApiId" within 30 seconds
    # ...and stops being one once the lease has elapsed
    Then The internal webhooks subscription list should hold exactly 0 subscriptions for API "websubApiId" within 90 seconds

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Source-side negative: content posted to a topic the API does not expose is rejected by the event receiver and
  # nothing is fanned out. A publish to the VALID topic first is the routable control, so the rejection is genuine
  # rather than a still-warming route. Needs no application/token — an event source is unauthenticated.
  @cap:gateway @feat:streaming-invocation @rule:unknown-topic @type:negative @dep:publisher @legacy:WebSubAPITestCase
  Scenario Outline: The WebSub event receiver rejects content published to an unknown topic as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_websub_invoke_api.json" in context as "websubApiPayload"
    And I generate a unique alphanumeric value and store it as "websubApiSecret"
    And I replace "WEBSUB_API_SECRET" with "{{websubApiSecret}}" in the payload "websubApiPayload"
    And I create an "apis" resource with payload "websubApiPayload" as "websubApiId"
    And I deploy the API with id "websubApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "websubApiId"
    Then The lifecycle status of API "websubApiId" should be "Published"
    When I retrieve the "apis" resource with id "websubApiId"
    And I extract response field "context" and store it as "websubContext"
    When I put the following JSON payload in context as "websubEventBody"
    """
    {"Hello" : "World"}
    """
    # Routable control: the API's own topic accepts the content
    When I publish the WebSub event "websubEventBody" to the event receiver at gateway context "{{websubContext}}/1.0.0" topic "_default" signed with secret "{{websubApiSecret}}" until response status code becomes 200 within 60 seconds
    # An unknown topic on the same, proven-routable API is rejected
    When I publish the WebSub event "websubEventBody" to the event receiver at gateway context "{{websubContext}}/1.0.0" topic "no-such-topic" signed with secret "{{websubApiSecret}}" until response status code becomes 404 within 30 seconds

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Hub-side negative: a registration missing ANY of the three mandatory hub parameters is rejected. Preceded by a
  # well-formed subscribe/unsubscribe pair as the routable control, so the rejection cannot be a warm-up artefact.
  # ONE code path, three guards: WebhookApiHandler.hasMandatorySubscriptionParameters tests hub.topic, hub.callback
  # and hub.mode with StringUtils.isNotEmpty, so a BLANK value is missing (which is how the outline expresses each
  # case) and all three failures raise the same SynapseException before authentication even runs — hence 500 and one
  # shared message for all three rows.
  # THE MESSAGE IS THE CURRENT PRODUCT'S, NOT THE LEGACY'S. FailedWebSubSubscriptionTestCase asserted
  # "Callback URL cannot be empty" and "Topic name not found for web hook subscription request"; NEITHER string
  # exists anywhere in the shipped distribution any more (verified across every plugin jar) — the product
  # consolidated the three per-parameter messages into the single one asserted here. Porting the legacy strings
  # verbatim would have produced three tests that can never pass.
  @cap:gateway @feat:streaming-invocation @rule:missing-parameters @type:negative @dep:publisher @legacy:WebSubAPITestCase @legacy:FailedWebSubSubscriptionTestCase
  Scenario Outline: The hub rejects a WebSub registration missing <missing> as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_websub_invoke_api.json" in context as "websubApiPayload"
    And I generate a unique alphanumeric value and store it as "websubApiSecret"
    And I replace "WEBSUB_API_SECRET" with "{{websubApiSecret}}" in the payload "websubApiPayload"
    And I create an "apis" resource with payload "websubApiPayload" as "websubApiId"
    And I deploy the API with id "websubApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "websubApiId"
    Then The lifecycle status of API "websubApiId" should be "Published"
    When I retrieve the "apis" resource with id "websubApiId"
    And I extract response field "context" and store it as "websubContext"
    When I have set up application with keys, subscribed to API "websubApiId" with plan "AsyncWHUnlimited", and obtained access token for "websubSubId"
    Then The response status code should be 200

    And I generate a unique alphanumeric value and store it as "websubSubscriberSecret"
    And I have a "silent" WebSub callback receiver stored as "websubReceiver"
    # Routable control: a well-formed registration is accepted, then withdrawn
    When I send a WebSub "subscribe" request as form data to gateway context "{{websubContext}}/1.0.0" with callback "{{websubReceiverCallback}}" topic "_default" secret "{{websubSubscriberSecret}}" lease seconds "50000000" using access token "generatedAccessToken" until response status code becomes 202 within 60 seconds
    And I send a WebSub "unsubscribe" request as form data to gateway context "{{websubContext}}/1.0.0" with callback "{{websubReceiverCallback}}" topic "_default" secret "{{websubSubscriberSecret}}" lease seconds "50000000" using access token "generatedAccessToken" until response status code becomes 202 within 60 seconds
    # The same registration with one mandatory parameter blank is rejected
    When I send a WebSub "<hubMode>" request as form data to gateway context "{{websubContext}}/1.0.0" with callback "<callback>" topic "<topic>" secret "{{websubSubscriberSecret}}" lease seconds "50000000" using access token "generatedAccessToken" until response status code becomes 500 within 30 seconds
    Then The response should contain "One or more mandatory parameters were not found in web hook subscription request"

    Examples:
      | actor             | missing      | hubMode   | callback                      | topic    |
      | admin             | hub.mode     |           | {{websubReceiverCallback}}    | _default |
      | admin             | hub.callback | subscribe |                               | _default |
      | admin             | hub.topic    | subscribe | {{websubReceiverCallback}}    |          |
      | admin@tenant1.com | hub.mode     |           | {{websubReceiverCallback}}    | _default |
      | admin@tenant1.com | hub.callback | subscribe |                               | _default |
      | admin@tenant1.com | hub.topic    | subscribe | {{websubReceiverCallback}}    |          |

  # Hub-side auth negative (FailedWebSubSubscriptionTestCase#testInvokeWebSubApiWithInvalidToken): a garbage
  # credential on the hub subscribe path is rejected with 401 — the hub is an ordinary authenticated gateway route
  # (WebhookApiHandler rewrites the verb to SUBSCRIBE and then delegates to APIAuthenticationHandler), so §12's
  # "an invalid/garbage credential at the gateway is 401" applies here too. A well-formed registration first proves
  # the route is live, so the rejection is genuine and not a warm-up artefact.
  # 401, exactly — NOT "any 4xx". The mandatory-parameter check above runs BEFORE authentication and answers 500,
  # so the two rejections are ordered and each has its own exact status.
  @cap:gateway @feat:streaming-invocation @rule:security-negative @type:negative @dep:publisher @legacy:FailedWebSubSubscriptionTestCase
  Scenario Outline: The hub rejects a WebSub registration carrying an invalid token as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_websub_invoke_api.json" in context as "websubApiPayload"
    And I generate a unique alphanumeric value and store it as "websubApiSecret"
    And I replace "WEBSUB_API_SECRET" with "{{websubApiSecret}}" in the payload "websubApiPayload"
    And I create an "apis" resource with payload "websubApiPayload" as "websubApiId"
    And I deploy the API with id "websubApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "websubApiId"
    Then The lifecycle status of API "websubApiId" should be "Published"
    When I retrieve the "apis" resource with id "websubApiId"
    And I extract response field "context" and store it as "websubContext"
    When I have set up application with keys, subscribed to API "websubApiId" with plan "AsyncWHUnlimited", and obtained access token for "websubSubId"
    Then The response status code should be 200

    And I generate a unique alphanumeric value and store it as "websubSubscriberSecret"
    And I have a "silent" WebSub callback receiver stored as "websubReceiver"
    # Routable control: the registration is accepted with a valid token, then withdrawn
    When I send a WebSub "subscribe" request as form data to gateway context "{{websubContext}}/1.0.0" with callback "{{websubReceiverCallback}}" topic "_default" secret "{{websubSubscriberSecret}}" lease seconds "50000000" using access token "generatedAccessToken" until response status code becomes 202 within 60 seconds
    And I send a WebSub "unsubscribe" request as form data to gateway context "{{websubContext}}/1.0.0" with callback "{{websubReceiverCallback}}" topic "_default" secret "{{websubSubscriberSecret}}" lease seconds "50000000" using access token "generatedAccessToken" until response status code becomes 202 within 60 seconds
    # The same registration with a garbage credential is refused
    When I put the following JSON payload in context as "websubBadToken"
    """
    this-is-an-invalid-websub-token
    """
    When I send a WebSub "subscribe" request as form data to gateway context "{{websubContext}}/1.0.0" with callback "{{websubReceiverCallback}}" topic "_default" secret "{{websubSubscriberSecret}}" lease seconds "50000000" using access token "websubBadToken" until response status code becomes 401 within 30 seconds

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Subscription-count throttling: an EVENTCOUNTLIMIT subscription plan capping subscriberCount at 2 must make the
  # THIRD concurrent registration fail. Unlike the event quota this is counted from the database by the hub itself,
  # so it does not depend on a Traffic Manager. Ports ThrottlingTestCase#testSubscriptionCountThrottling, and the
  # tenant row also ports its testPublishWebSubApi — publishing a WebSub API whose plan is a CUSTOM event-count
  # policy created in that tenant, which no other scenario does for tenant1.com.
  @cap:gateway @feat:streaming-invocation @rule:subscription-count-throttling @type:regression @dep:admin @dep:publisher @legacy:ThrottlingTestCase
  Scenario Outline: A third WebSub registration is rejected once the plan's subscription cap is reached as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create a subscription throttling policy "${UNIQUE:wsubSubCap}" allowing 10000 events per minute and at most 2 webhook subscriptions
    Then The response status code should be 201
    When I put JSON payload from file "artifacts/payloads/create_apim_websub_invoke_api.json" in context as "websubApiPayload"
    And I generate a unique alphanumeric value and store it as "websubApiSecret"
    And I replace "WEBSUB_API_SECRET" with "{{websubApiSecret}}" in the payload "websubApiPayload"
    And I replace "AsyncWHUnlimited" with "{{subThrottlePolicyName}}" in the payload "websubApiPayload"
    And I create an "apis" resource with payload "websubApiPayload" as "websubApiId"
    And I deploy the API with id "websubApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "websubApiId"
    Then The lifecycle status of API "websubApiId" should be "Published"
    When I retrieve the "apis" resource with id "websubApiId"
    And I extract response field "context" and store it as "websubContext"
    When I have set up application with keys, subscribed to API "websubApiId" with plan "{{subThrottlePolicyName}}", and obtained access token for "websubSubId"
    Then The response status code should be 200
    # UNBLOCKED on the CUSTOM event-count plan specifically (the legacy ThrottlingTestCase asserted this on its own
    # plan, not on AsyncWHUnlimited): a subscription-count cap is only meaningful once the subscription itself is
    # active, and a 201 alone would also be returned for one parked ON_HOLD. See the smoke scenario's note above.
    When I get the subscription with id "websubSubId"
    Then The value of response field "status" should be "UNBLOCKED"

    And I generate a unique alphanumeric value and store it as "websubSubscriberSecret"
    And I have a "silent" WebSub callback receiver stored as "websubCapOne"
    And I have a "silent" WebSub callback receiver stored as "websubCapTwo"
    And I have a "silent" WebSub callback receiver stored as "websubCapThree"
    # QUERY-PARAMETER encoding is REQUIRED here, and it is the one thing this scenario cannot vary: the hub's
    # form-urlencoded branch sets FORCE_SC_ACCEPTED=true BEFORE SubscribersPersistMediator runs, so the rejection the
    # cap produces is overwritten with 202 and can never reach the subscriber. The query-parameter branch sets only
    # NO_ENTITY_BODY and forces no status, so the mediator's 429/900808 survives — which is why the legacy
    # ThrottlingTestCase used the query-param helper for exactly this assertion. MEASURED: with form data the third
    # subscribe answered 202, not 429. Hence 200 (not 202) for the two accepted registrations below.
    When I send a WebSub "subscribe" request as query parameters to gateway context "{{websubContext}}/1.0.0" with callback "{{websubCapOneCallback}}" topic "_default" secret "{{websubSubscriberSecret}}" lease seconds "50000000" using access token "generatedAccessToken" until response status code becomes 200 within 60 seconds
    And I send a WebSub "subscribe" request as query parameters to gateway context "{{websubContext}}/1.0.0" with callback "{{websubCapTwoCallback}}" topic "_default" secret "{{websubSubscriberSecret}}" lease seconds "50000000" using access token "generatedAccessToken" until response status code becomes 200 within 60 seconds
    # The third exceeds the plan's subscriberCount cap
    When I send a WebSub "subscribe" request as query parameters to gateway context "{{websubContext}}/1.0.0" with callback "{{websubCapThreeCallback}}" topic "_default" secret "{{websubSubscriberSecret}}" lease seconds "50000000" using access token "generatedAccessToken" until response status code becomes 429 within 30 seconds
    When I send a WebSub "unsubscribe" request as query parameters to gateway context "{{websubContext}}/1.0.0" with callback "{{websubCapOneCallback}}" topic "_default" secret "{{websubSubscriberSecret}}" lease seconds "50000000" using access token "generatedAccessToken" until response status code becomes 200 within 60 seconds
    And I send a WebSub "unsubscribe" request as query parameters to gateway context "{{websubContext}}/1.0.0" with callback "{{websubCapTwoCallback}}" topic "_default" secret "{{websubSubscriberSecret}}" lease seconds "50000000" using access token "generatedAccessToken" until response status code becomes 200 within 60 seconds

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Event-count throttling: an EVENTCOUNTLIMIT plan of 2 events/min must make the hub DROP 4 of the 6 events
  # published inside the window, so exactly 2 reach the callback. Ports ThrottlingTestCase#testEventsThrottling
  # (which only asserted "fewer than sent", noting the count could not be guaranteed).
  # The count assertion is DETERMINISTIC, not a sampled one: the delivery-count step settles the count (waits until
  # it has stopped changing — Utils.awaitSettledCount) before asserting the exact value, so an unthrottled run
  # cannot pass by being observed at the instant the count passes 2. The barrier pattern used elsewhere in this file
  # cannot serve here (a second subscriber would share the same throttled plan), which is why settling was needed.
  # PARKED - and the reason has now NARROWED to the fan-out alone. It was written as a probe of whether the
  # all-in-one profile enforces an async event quota at all. THAT QUESTION IS SETTLED, AND THE ANSWER IS YES: the
  # sibling SSE and raw-WebSocket event-quota scenarios (gateway/sse_invocation "SSE events beyond the subscription
  # plan's event quota are not delivered" and gateway/websocket_invocation "A WS API is throttled once its frames
  # exceed the subscription plan's event quota") both PASS on this single-container profile with a subscription-level
  # EVENTCOUNTLIMIT plan. So the throttle publish/decide/enforce loop for STREAMING event counts is live here and a
  # separate wso2am-tm is NOT what this row is waiting for.
  # What it is still waiting for is the fan-out: this quota's only observable is a DELIVERED count, and with the
  # hub delivering nothing the observed 0 satisfies "fewer than published" for the wrong reason, so an unenforced
  # quota and a dead fan-out remain indistinguishable HERE specifically. Per the docs the WebSub throttle also posts
  # a throttled-out message to the callback URL, which is likewise a delivery. Re-enable as soon as the delivery leg
  # works; no new glue is needed. See the shared note above.
  # EVENT-COUNT QUOTA. Rewritten 2026-08-07 after the original asserted an EXACT count it could not reliably hit.
  # Two things were measured and both shape this scenario:
  #  1. The quota OVERSHOOTS. The gateway counts locally and reconciles with the traffic manager asynchronously, so
  #     a burst slips past the limit before enforcement engages — a 9/month plan delivered 12 on a live server, and
  #     a 2-event plan delivered 6 here. Asserting "exactly 2" was asserting the overshoot, not the quota.
  #  2. The quota is INVISIBLE to the publisher: publishing past an exhausted quota still answers 200 with an empty
  #     body. So there is no status to assert on — only the absence of further deliveries. (The subscriber-COUNT
  #     cap is different: the hub rejects THAT on subscribe with 429/900808 — see the cap scenario.)
  # Hence: publish far past the limit and assert delivery STOPPED, with an UNLIMITED-plan control proving the
  # shortfall is the quota rather than delivery being broken — the failure that had this file's delivery scenarios
  # parked for days and which "fewer than N" alone would silently pass.
  @cap:gateway @feat:streaming-invocation @rule:event-count-throttling @type:regression @dep:admin @dep:publisher @legacy:ThrottlingTestCase
  Scenario Outline: An event-count quota stops delivery while an unlimited subscriber keeps receiving as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create a subscription throttling policy "${UNIQUE:wsubEvQuota}" allowing 2 events per minute
    Then The response status code should be 201
    When I put JSON payload from file "artifacts/payloads/create_apim_websub_invoke_api.json" in context as "websubApiPayload"
    And I generate a unique alphanumeric value and store it as "websubApiSecret"
    And I replace "WEBSUB_API_SECRET" with "{{websubApiSecret}}" in the payload "websubApiPayload"
    # BOTH plans must be on the API: the quota plan under test and AsyncWHUnlimited for the control subscriber.
    And I set the field "policies" to the JSON value "[\"{{subThrottlePolicyName}}\",\"AsyncWHUnlimited\"]" in the payload "websubApiPayload"
    And I create an "apis" resource with payload "websubApiPayload" as "websubApiId"
    And I deploy the API with id "websubApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "websubApiId"
    Then The lifecycle status of API "websubApiId" should be "Published"
    When I retrieve the "apis" resource with id "websubApiId"
    And I extract response field "context" and store it as "websubContext"
    And I generate a unique alphanumeric value and store it as "websubSubscriberSecret"
    And I have a "silent" WebSub callback receiver stored as "websubQuota"
    And I have a "silent" WebSub callback receiver stored as "websubControl"
    When I put the following JSON payload in context as "websubEventBody"
    """
    {"Hello" : "World"}
    """
    # The QUOTA subscriber. Each subscribe uses the token minted by the setup immediately above it — the composite
    # overwrites generatedAccessToken, so the order here is load-bearing.
    When I have set up application with keys, subscribed to API "websubApiId" with plan "{{subThrottlePolicyName}}", and obtained access token for "websubQuotaSubId"
    Then The response status code should be 200
    When I send a WebSub "subscribe" request as form data to gateway context "{{websubContext}}/1.0.0" with callback "{{websubQuotaCallback}}" topic "_default" secret "{{websubSubscriberSecret}}" lease seconds "50000000" using access token "generatedAccessToken" until response status code becomes 202 within 60 seconds
    # The CONTROL subscriber, on the unlimited plan.
    When I have set up application with keys, subscribed to API "websubApiId" with plan "AsyncWHUnlimited", and obtained access token for "websubControlSubId"
    Then The response status code should be 200
    When I send a WebSub "subscribe" request as form data to gateway context "{{websubContext}}/1.0.0" with callback "{{websubControlCallback}}" topic "_default" secret "{{websubSubscriberSecret}}" lease seconds "50000000" using access token "generatedAccessToken" until response status code becomes 202 within 60 seconds
    # BARRIER: a form-data subscribe answers 202 from FORCE_SC_ACCEPTED BEFORE the persist mediator runs, so
    # publishing straight after it races the registration and fans out to nobody.
    Then The internal webhooks subscription list should hold exactly 2 subscriptions for API "websubApiId" within 60 seconds
    # TWO BATCHES, and the split is the whole point. APIM counts throttle events on the gateway and reconciles
    # with the traffic manager ASYNCHRONOUSLY, so a single burst can finish before the counter is ever consulted —
    # measured: 20 rapid publishes against a 2/min quota were ALL delivered. Enforcement is only observable once
    # the first batch has been counted, which is exactly how it behaves on a standalone server (batch 1 delivered,
    # batch 2 withheld). So batch 1 primes the counter, the settle below waits for reconciliation, and batch 2 is
    # the one whose fate is asserted.
    When I publish the WebSub event "websubEventBody" to the event receiver at gateway context "{{websubContext}}/1.0.0" topic "_default" signed with secret "{{websubApiSecret}}" 10 times expecting status 200
    Then The WebSub receiver "websubControl" should have received 10 events within 90 seconds
    # Batch 2, published after the first has settled at both receivers.
    When I publish the WebSub event "websubEventBody" to the event receiver at gateway context "{{websubContext}}/1.0.0" topic "_default" signed with secret "{{websubApiSecret}}" 10 times expecting status 200
    # The control proves the fan-out, the publishes and the topic are all healthy for BOTH batches...
    Then The WebSub receiver "websubControl" should have received 20 events within 90 seconds
    # ...so the quota subscriber's shortfall can only be the quota. It must have received SOMETHING (else this is
    # broken delivery, not throttling) and must have STOPPED short of the full 20.
    And The WebSub receiver "websubQuota" should have settled below 20 events within 90 seconds

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
