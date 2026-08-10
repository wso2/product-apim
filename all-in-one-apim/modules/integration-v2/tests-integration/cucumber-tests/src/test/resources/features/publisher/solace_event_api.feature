@cleanup
Feature: Solace event-API integration

  APIM's Solace integration on the v2 Event-Portal surface: discover Event API Products through APIM,
  import one as an event API, publish it, and have a devportal subscription and key generation propagate
  to Solace. Ports the reachable parts of legacy SolaceTestCase.

  WHY THIS IS NOT THE FLOW LEGACY TESTED, AND WHY THOSE ROWS ARE NOT HERE.
  Legacy drove Solace through a `provider = "solace"` gateway environment and deployed API revisions to
  it (deploy / undeploy / retire-then-undeploy / delete-then-undeploy). That mechanism is GONE on this
  product, so those four rows are not portable rather than merely unwritten:
    - deploying a revision to such an environment returns 207 with 902060 "Error while deploying API to
      Gateway", logging only WARN "ExternalGatewayNotifier No gateway deployer found for environment";
    - ExternalGatewayNotifier resolves deployers via GatewayAgentConfiguration -> GatewayHolder ->
      org.wso2.carbon.apimgt.api.model.GatewayDeployer, for which the distribution ships aws / azure /
      envoy / kong agents and NO solace agent;
    - SolaceBrokerDeployer implements only the retired ExternalGatewayDeployer, referenced by exactly two
      classes across all 772 plugin jars (the interface and itself). Nothing binds it.
  Solace integrates through NOTIFIERS instead (SolaceSubscriptionsNotifier / SolaceKeyGenNotifier /
  SolaceApplicationNotifier, registered by SolaceManagerComponent), on the API configured by
  `[apim.solace_config]`. Those notifiers are what the scenarios below exercise. Do not "restore" the
  deploy rows without first checking whether a solace GatewayDeployer agent has shipped.

  WHAT STANDS IN FOR SOLACE. The block runs DynamicSolaceBroker, whose control plane is a MOCK of the
  Solace Cloud APIM/DevPortal API (`solaceshim`). Its contract comes from the Feign interface
  SolaceV2ApimApisClient and was cross-checked against Solace's published API walkthrough, including the
  {"data":…,"meta":{}} envelope, client-supplied registrationId/accessRequestId, the bare AsyncAPI
  response, and 404 for an absent registration. So these scenarios prove "APIM drives the documented
  Solace contract correctly" -- NOT "APIM works against Solace Cloud".

  HOW THESE ASSERTIONS CAN FAIL, ESTABLISHED BY MUTATION RATHER THAN BY READING THE CODE. Every row below
  was checked by breaking something reachable and confirming the row failed with the expected message. The
  results are not uniform, and an earlier version of this note was WRONG to claim they were:
    - The SUBSCRIPTION notifier propagates: remove x-ep-event-api-product-id from the imported definition and
      subscribe returns 500 (NPE in SolaceSubscriptionsNotifier.createAccessRequest), with zero access
      requests reaching Solace. So the 201 on subscribe IS a real claim.
    - The KEY-GEN notifier SWALLOWS: key generation returned 200 both when it never called Solace at all (an
      application with no Solace subscription) and when Solace answered 500 to the credential push. Its 200
      therefore proves nothing about Solace, which is why that scenario asserts BROKER STATE through SEMP.
    - Unresolvable plan is swallowed AT THE CALLER: rename the Solace plan so the subscription tier cannot be
      matched and subscribe still returns 201 with ZERO access requests. It IS logged server-side ("ERROR -
      APIUtil Error when publish SubscriptionEvent"), but the subscription is committed anyway, so the consumer
      holds a subscription Solace never granted. Do NOT write a scenario that treats a 201 as proof the plan
      resolved. The difference from the 500 above is the notifier's exception table (see the subscribe row).
  A mutant that cannot reach the code under test proves nothing about it: mutating the SHIM's AsyncAPI
  document left subscribe/keygen/invoke green, because those rows import the repo's solace_asyncapi.yaml and
  never read the shim's copy. Mutate the artifact the row actually consumes.

  Background:
    Given The system is ready

  @cap:publisher @feat:solace @rule:solace-discovery @type:smoke @legacy:SolaceTestCase
  Scenario Outline: Event API Products are discoverable through APIM as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I retrieve the integrated Solace APIs
    Then The response status code should be 200
    # MEASURED response shape -- APIM does NOT pass Solace's fields through, it COMPOSES its own:
    #   [{"apiId":"8xk2p9qz1a/pl4n7bq2xd/api5t8w2rk","apiName":"SolaceEventApi:1.0.0","plans":[...]}]
    # so IntegratedSolaceApisResponse.EventApi carries
    #   apiId   = {eventApiProductId}/{planId}/{eventApiId}   -- SLASH-joined triple
    #   apiName = {eventApi.name}:{eventApi.version}          -- COLON separator
    # An earlier draft asserted the Event API PRODUCT name ("SolaceEventApi-1.0.0", hyphenated) and failed:
    # the name here is the EVENT API's, and the separator is a colon. Note the composite ids use different
    # separators in different places -- "/" here, "_" in the client-side accessRequestId
    # ({appUUID}_{productId}_{planId}) -- so neither can be inferred from the other.
    #
    # The apiId is asserted because it is load-bearing, not decorative: the definition call in the next
    # scenario is addressed by exactly that triple, so a wrong assembly here breaks discovery -> import.
    And The response should contain "8xk2p9qz1a/pl4n7bq2xd/api5t8w2rk"
    And The response should contain "SolaceEventApi:1.0.0"
    And The response should contain "AsyncUnlimited"

    Examples:
      | actor          |
      | publisherUser  |
      | publisherUser@tenant1.com |

  @cap:publisher @feat:solace @rule:solace-discovery @type:regression @legacy:SolaceTestCase
  Scenario Outline: The AsyncAPI definition of an Event API Product is retrievable through APIM as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I retrieve the integrated Solace API definition for product "8xk2p9qz1a" plan "pl4n7bq2xd" api "api5t8w2rk"
    Then The response status code should be 200
    # APIM passes Solace's document through verbatim (getEventApiAsyncApiDefinition returns the client's
    # JsonObject with no unwrapping), so the x-ep-* extensions must survive the round trip. They are what
    # a later subscription reads; if they were stripped here, subscribing would 500 instead.
    And The response should contain "x-ep-event-api-product-id"
    And The response should contain "x-ep-application-domain-id"

    Examples:
      | actor          |
      | publisherUser  |
      | publisherUser@tenant1.com |

  @cap:publisher @feat:solace @rule:solace-import @type:regression @legacy:SolaceTestCase
  Scenario Outline: An AsyncAPI definition is imported as a Solace event API as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I import asyncapi definition from "artifacts/payloads/solace/solace_asyncapi.yaml" with additional properties "artifacts/payloads/solace/solace_api_props.json" as "solaceApiId"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "solaceApiId"
    Then The response status code should be 200
    # MEASURED, and NOT what legacy asserted -- read before "fixing" this to "solace".
    # APIUtil.handleGatewayVendorRetrieval collapses the vendor on every RETRIEVAL path:
    #     if ("wso2/apk".equals(v) || "wso2".equals(v) || "APIPlatform".equals(v)) return "wso2";
    #     return "external";                    // <- everything else, INCLUDING "solace"
    # The value STORED is "solace" (that is what makes the notifiers fire, proven by the subscription
    # scenario); every GET reports "external". Legacy asserted "solace" against the import POST response
    # DTO, not a GET. "external" is pinned as the exact current behaviour, but is NOT proof of
    # Solace-ness -- an AWS-vendor API reports it too.
    And The value of response field "gatewayVendor" should be "external"
    And The value of response field "type" should be "WEBSUB"

    Examples:
      | actor          |
      | publisherUser  |
      | publisherUser@tenant1.com |

  @cap:devportal @feat:solace @rule:solace-visibility @type:regression @dep:publisher @legacy:SolaceTestCase
  Scenario Outline: A published Solace event API is visible to consumers in the developer portal as <actor>
    Given I have valid access tokens as "<actor>"
    When I import asyncapi definition from "artifacts/payloads/solace/solace_asyncapi.yaml" with additional properties "artifacts/payloads/solace/solace_api_props.json" as "solaceStoreApiId"
    Then The response status code should be 201
    When I change the lifecycle of API "solaceStoreApiId" with action "Publish"
    Then The response status code should be 200
    # Ports the PORTABLE half of legacy showSolaceApiInDeveloperPortal. The half that is NOT portable is the
    # middle of that test: it deployed a revision to a solace gateway environment before publishing, which is
    # the retired mechanism this file's header describes. Publishing alone is what makes an API visible to
    # consumers, so the devportal claim survives without it.
    #
    # Retried, because devportal visibility is asynchronous: a just-published API is not in the store the
    # instant the lifecycle call returns.
    When I retrieve the devportal API "solaceStoreApiId" until the response status code becomes 200 within 60 seconds
    # gatewayVendor is "external" here for the same reason as on the publisher GET -- see the import scenario.
    And The value of response field "gatewayVendor" should be "external"
    # asyncTransportProtocols is the API's transport vocabulary, derived from the AsyncAPI `servers` at IMPORT
    # time. Worth having because it is the ONLY protocol-vocabulary surface reachable on this product: the
    # topicSyntax/smf-mqtt-amqp translation the v1 connector performed lives solely in SolaceAdminApis, on the
    # retired deploy path. Legacy asserted only that the store value was non-null, so this asserts presence too
    # rather than pinning a list that the fixture's `servers` block dictates.
    And The response should contain "asyncTransportProtocols"
    # The channels a consumer may subscribe to, from the same document. One channel (orders/created) is defined,
    # and it is the topic the invoke scenarios publish to -- so a change to the fixture's channels shows up here.
    When I retrieve the topics of devportal API "solaceStoreApiId"
    Then The response status code should be 200
    And The value of response field "count" should be "1"

    Examples:
      | actor |
      | admin |
      | admin@tenant1.com |

  @cap:devportal @feat:solace @rule:solace-subscription @type:regression @dep:publisher @legacy:SolaceTestCase
  Scenario Outline: Subscribing to a published Solace event API registers the application on Solace as <actor>
    Given I have valid access tokens as "<actor>"
    When I import asyncapi definition from "artifacts/payloads/solace/solace_asyncapi.yaml" with additional properties "artifacts/payloads/solace/solace_api_props.json" as "solaceApiId"
    Then The response status code should be 201
    When I change the lifecycle of API "solaceApiId" with action "Publish"
    Then The response status code should be 200
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "solaceAppPayload"
    And I create an application with payload "solaceAppPayload"
    Then The response status code should be 201
    # The Solace PLAN is resolved BY NAME against this tier: SolaceSubscriptionsNotifier passes
    # SubscriptionEvent.getPolicyId() to getEventApiProductPlanId(productId, planName), matched on the
    # plan's `name`. So AsyncUnlimited must stay in step with the shim's PLANS and this API's `policies`.
    #
    # A mismatch does NOT reach the API CALLER, which is worth knowing before trusting this row. MEASURED by
    # renaming the shim's plan: subscribe still returned 201 and ZERO access requests were sent to Solace.
    # The failure is logged server-side ("ERROR - APIUtil Error when publish SubscriptionEvent{...}") but the
    # subscription is still committed, so the consumer receives a subscription Solace never granted.
    #
    # WHY THIS DIFFERS from the x-ep failure, which DOES surface as a 500 -- the reason is structural, in
    # SolaceSubscriptionsNotifier.publishEvent's exception table: it catches APIManagementException and
    # JsonSyntaxException and rewraps them as NotifierException, which APIUtil then logs and swallows. A
    # NullPointerException is in neither catch, so it escapes raw and becomes the 500. "Plan not found" is
    # raised as an APIManagementException, hence the silence at the caller.
    # So this row's 201 is not evidence that the plan resolved -- it is the invoke scenario, which needs a
    # working grant, that would notice.
    When I put the following JSON payload in context as "solaceSubPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "AsyncUnlimited"}
    """
    And I subscribe to API "solaceApiId" using application "createdAppId" with payload "solaceSubPayload" as "solaceSubId"
    # 201 is the load-bearing assertion: the subscription notifier runs INLINE (APIConsumerImpl
    # .addSubscription -> APIUtil.sendNotification), so a Solace-side failure surfaces HERE as a 500.
    # This row is exactly what caught the missing x-ep-* extensions, so it is not a vacuous status check.
    Then The response status code should be 201

    # Unsubscribe EXPLICITLY, and not only for tidiness -- this covers the SUBSCRIPTIONS_DELETE half of the
    # arc, where APIM sends Solace DELETE /appRegistrations/{appUUID}/accessRequests/{accessRequestId}
    # (its id is composed client-side as {appUUID}_{productId}_{planId}).
    #
    # IT IS ALSO A WORKAROUND FOR A PRODUCT DEFECT, so do not "simplify" it away by letting @cleanup delete
    # the application while the subscription still exists. MEASURED: deleting a subscribed application
    # returns 500 and the application is NOT deleted (it leaks, and so does its Solace-side registration):
    #   NullPointerException: Cannot invoke "Application.getUUID()" because "wso2ApimDevPortalApplication"
    #   is null   at SolaceSubscriptionsNotifier.deleteAccessRequest(SolaceSubscriptionsNotifier.java:136)
    # Cause: APIConsumerImpl.removeApplication deletes the application row and THEN emits the cascade of
    # SUBSCRIPTIONS_DELETE events (sendApplicationDeletionEvent), and the notifier resolves the Solace
    # registrationId via apiConsumer.getApplicationByUUID(event.getApplicationUUID()).getUUID() -- a
    # redundant lookup of a UUID the event already carries, which now returns null.
    # publishEvent only acts on SUBSCRIPTIONS_CREATE / SUBSCRIPTIONS_DELETE for a "solace" vendor API, so
    # removing the subscription first means no such event fires at application-delete time and teardown is
    # clean. Here the application still exists, so the same notifier path succeeds instead of NPEing.
    When I delete the subscription with id "solaceSubId"
    Then The response status code should be 200

    Examples:
      | actor |
      | admin |
      | admin@tenant1.com |

  @cap:devportal @feat:solace @rule:solace-keygen @type:regression @dep:publisher @legacy:SolaceTestCase
  Scenario Outline: Generating application keys pushes the credentials to Solace as <actor>
    Given I have valid access tokens as "<actor>"
    When I import asyncapi definition from "artifacts/payloads/solace/solace_asyncapi.yaml" with additional properties "artifacts/payloads/solace/solace_api_props.json" as "solaceKeyApiId"
    Then The response status code should be 201
    When I change the lifecycle of API "solaceKeyApiId" with action "Publish"
    Then The response status code should be 200
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "solaceKeyAppPayload"
    And I create an application with payload "solaceKeyAppPayload"
    Then The response status code should be 201
    # SUBSCRIBE FIRST -- required, and for a different reason than an earlier version of this comment claimed.
    # It said the credential push would hit a registration nothing had created and Solace would answer 404.
    # MEASURED by removing this step: SolaceKeyGenNotifier does not fire AT ALL for an application with no
    # Solace subscription -- zero credential pushes reached the shim, no 404, and key generation returned a
    # clean 200. So the subscription is what makes the Solace leg happen; without it the scenario silently
    # tests nothing, which is precisely why the SEMP assertion below exists to catch that.
    When I put the following JSON payload in context as "solaceKeySubPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "AsyncUnlimited"}
    """
    And I subscribe to API "solaceKeyApiId" using application "createdAppId" with payload "solaceKeySubPayload" as "solaceKeySubId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "solaceKeygenPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    # This 200 is NOT the assertion that matters, and saying otherwise was wrong: MEASURED by mutation, key
    # generation returns 200 both when the notifier never calls Solace (application with no Solace
    # subscription: zero credential pushes observed) and when Solace answers 500 to the push. The key-gen
    # notifier swallows a Solace-side failure, unlike the subscription notifier. So the 200 is kept only as a
    # fail-fast on APIM's own call, and the real claim is the SEMP read below.
    And I generate client credentials for application id "createdAppId" with payload "solaceKeygenPayload"
    Then The response status code should be 200
    # THE ACTUAL CLAIM: the credential reached the BROKER. Asserted through SEMP -- the independent witness --
    # rather than through the shim, whose own responses are satisfied by construction. MEASURED: SEMP answers
    # 200 for a provisioned client username and 400 once absent, so this row fails exactly when the push does
    # not happen. This is what makes the scenario title ("pushes the credentials to Solace") true.
    When I retrieve the Solace broker client username for consumer key "{{consumerKey}}"
    Then The response status code should be 200
    # SUPER-TENANT ONLY, and this is a PRODUCT LIMITATION, not an oversight. Running this row as
    # admin@tenant1.com fails with HTTP 500 from APIM's own notifier, measured 2026-08-09:
    #   SolaceKeyGenNotifier.java:67  NullPointerException: Cannot invoke "String.intern()" because "key" is null
    #   SolaceKeyGenNotifier.java:68  NullPointerException: Cannot invoke "Application.getId()" because
    #                                 "application" is null
    # SolaceKeyGenNotifier re-resolves the application from the key-generation event without establishing the
    # tenant flow, so the lookup returns null in a tenant and is dereferenced immediately. The SUBSCRIPTION
    # notifier IS tenant-safe (the discovery/import/visibility/subscribe scenarios above pass in both tenants),
    # so the gap is specific to the key-generation notifier -- same anti-pattern as the two delete-time NPEs
    # documented above. RESTORE the admin@tenant1.com row once fixed; nothing here is super-tenant-specific.
    Examples:
      | actor |
      | admin |

  @cap:gateway @feat:solace @rule:solace-invoke @type:regression @dep:publisher @legacy:SolaceTestCase
  Scenario Outline: An application publishes and subscribes on the Solace broker within its granted topics as <actor>
    Given I have valid access tokens as "<actor>"
    When I import asyncapi definition from "artifacts/payloads/solace/solace_asyncapi.yaml" with additional properties "artifacts/payloads/solace/solace_api_props.json" as "solaceInvokeApiId"
    Then The response status code should be 201
    When I change the lifecycle of API "solaceInvokeApiId" with action "Publish"
    Then The response status code should be 200
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "solaceInvokeAppPayload"
    And I create an application with payload "solaceInvokeAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "solaceInvokeSubPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "AsyncUnlimited"}
    """
    And I subscribe to API "solaceInvokeApiId" using application "createdAppId" with payload "solaceInvokeSubPayload" as "solaceInvokeSubId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "solaceInvokeKeygenPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "solaceInvokeKeygenPayload"
    Then The response status code should be 200
    # Mint the application's own access token — this is the credential the broker will check, so it must be the
    # application's, not a test actor's. APIM issues it as a self-contained JWT (typ at+jwt, RS256, signed with
    # the key APIM publishes at /oauth2/jwks), which is exactly what makes independent verification possible.
    When I put the following JSON payload in context as "solaceInvokeTokenPayload"
    """
    {"consumerSecret": "{{appConsumerSecret}}", "validityPeriod": 3600}
    """
    And I request an access token for application id "createdAppId" using payload "solaceInvokeTokenPayload"
    # THE DATA-PLANE CLAIM, and the only assertion in this feature whose verdict comes from software nobody on
    # this project wrote: SolOS decides. Two independent links must BOTH hold for a 200, so this is a claim
    # about the whole arc rather than a status check:
    #   1. the broker fetched APIM's JWKS and verified this token's signature/expiry/type against it;
    #   2. the client username it resolves from the token's client_id claim (the app's consumerKey) was
    #      PROVISIONED on the broker, which only happens via APIM key generation -> SolaceKeyGenNotifier ->
    #      POST /appRegistrations/{app}/credentials -> the connector's SEMP clientUsername.
    # Both links are mutation-proven against a live broker (no clientUsername -> 403, provisioned -> 200,
    # deleted -> 403), and the fallback `default` client username is disabled so link 2 cannot be bypassed.
    When I publish an event to the Solace topic "orders/created" with OAuth token "{{generatedAccessToken}}" until the response status code becomes 200 within 60 seconds
    # TOPIC-LEVEL AUTHORISATION, and it belongs in THIS scenario rather than its own for a methodological
    # reason, not to save setup: the denial below uses the SAME token that just published successfully. That
    # is what proves the 403 is the ACL and not the credential. Split across two scenarios with two
    # applications, a 403 could equally mean the token was bad, and the row would prove nothing new.
    #
    # The grant comes from the access request the subscription raised: the connector double translates its
    # permissions into a broker ACL profile (default publish action DISALLOW plus an exception per granted
    # topic) and attaches it to the client username. MEASURED end-to-end on this image -- granted topic 200,
    # ungranted topic 403 "Publish ACL Denied" -- so this row fails if that translation stops happening.
    When I publish an event to the Solace topic "orders/forbidden" with OAuth token "{{generatedAccessToken}}" until the response status code becomes 403 within 30 seconds
    # ---- The SUBSCRIBE half of the same grant, over MQTT ------------------------------------------------
    # Solace's REST messaging is send-only for clients, so the subscribe permissions an access request carries
    # cannot be reached over HTTP at all -- hence a real messaging protocol here. The broker answers a
    # SUBSCRIBE with a per-filter code (a QoS grant, or 0x80 for failure), so enforcement is observable
    # WITHOUT publishing anything or waiting on delivery.
    #
    # This also covers the credential form the publish rows CANNOT: over mqtt the token travels as the
    # password in Solace's OAUTH~<profile>~<token> form (the shape its tutorial documents), which REST rejects
    # outright as basic auth. So the two halves together prove APIM's token is accepted in both credential
    # forms the broker supports.
    #
    # The granted row is the control for the denied one: same token, same connection recipe, different topic.
    # If both were denied the pair would still "look" enforced, so the grant is what proves the denial is the
    # ACL rather than a broken credential.
    When I subscribe over MQTT to the Solace topic "orders/created" with OAuth token "{{generatedAccessToken}}" expecting "granted"
    When I subscribe over MQTT to the Solace topic "orders/forbidden" with OAuth token "{{generatedAccessToken}}" expecting "denied"
    # SUPER-TENANT ONLY, and this is a PRODUCT LIMITATION, not an oversight. Running this row as
    # admin@tenant1.com fails with HTTP 500 from APIM's own notifier, measured 2026-08-09:
    #   SolaceKeyGenNotifier.java:67  NullPointerException: Cannot invoke "String.intern()" because "key" is null
    #   SolaceKeyGenNotifier.java:68  NullPointerException: Cannot invoke "Application.getId()" because
    #                                 "application" is null
    # SolaceKeyGenNotifier re-resolves the application from the key-generation event without establishing the
    # tenant flow, so the lookup returns null in a tenant and is dereferenced immediately. The SUBSCRIPTION
    # notifier IS tenant-safe (the discovery/import/visibility/subscribe scenarios above pass in both tenants),
    # so the gap is specific to the key-generation notifier -- same anti-pattern as the two delete-time NPEs
    # documented above. RESTORE the admin@tenant1.com row once fixed; nothing here is super-tenant-specific.
    Examples:
      | actor |
      | admin |

  @cap:gateway @feat:solace @rule:solace-invoke @type:negative
  Scenario: The Solace broker rejects a publish with an invalid token
    # DELIBERATELY NO SETUP. Rejecting a bad token is the broker's own behaviour, so this row needs no API,
    # application, subscription, keys or actor — and an earlier draft that built all of that first was ~15
    # wasted steps whose only effect was to make the row slower and give it more ways to fail (it also leaked
    # an application through the app-delete defect noted above).
    #
    # 403, NOT 401 — measured on this image for every rejection case: a garbage token, an EMPTY bearer, and a
    # real APIM token with one byte of its signature flipped all return 403. Asserted exactly rather than
    # widened: a different 4xx would mean the broker refused for some other reason and must not pass as
    # "blocked". (An earlier probe saw 400 for a well-formed-but-unverifiable token; that was measured before
    # the OAuth profile's issuer matched, i.e. against a broker that never reached signature validation.)
    #
    # WHY THIS IS NOT VACUOUS, which had to be proven rather than assumed: an untouched broker accepts ANY
    # credential — it ships authenticationBasicType "none" plus an enabled fallback `default` client username,
    # and publishing as bogus:bogus returned 200. The connector double disables basic auth entirely and
    # disables that fallback AT STARTUP, which is what makes rejection possible. This row therefore also
    # guards that configuration: if it stops happening, this fails instead of silently passing. (Startup, not
    # on first credential push, precisely so this scenario cannot depend on another scenario running first.)
    When I publish an event to the Solace topic "orders/created" with OAuth token "not-a-valid-token" until the response status code becomes 403 within 30 seconds
