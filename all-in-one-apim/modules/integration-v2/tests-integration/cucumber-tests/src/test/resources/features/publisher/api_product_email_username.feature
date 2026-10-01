@cleanup
Feature: API Product Lifecycle Under an Email-Form Username Provider

  The API-product lifecycle arc driven by a provider whose USERNAME IS AN EMAIL ADDRESS. Closes the
  SUPER_TENANT_EMAIL_USER fan-out of APIProductLifecycleTest (testCreateAPIProduct, testPublishAPIProduct,
  testChangeAPIProductLifecycleStateToBlockedState, testDeleteDeprecatedAPIProductsWithSubscription,
  testDeleteRetiredAPIProducts), which the group-1 port left as the one open dimension: the SUBSTANCE of those
  rows is already covered in publisher/api_products.feature by plain and secondary-store actors, so what is
  added here is only the email-username dimension — the code paths that split a principal on "@" (user-store
  lookup, tenant-domain extraction, token subject resolution) when the principal itself contains one.

  WHY THIS LIVES IN ITS OWN BLOCK RATHER THAN AS EXAMPLES ROWS ON api_products.feature. The dimension is not an
  actor you can add to an existing outline: it is a CONTAINER MODE. The block sets emailUserMode=true, which
  changes the physical username of every provisioned user, and its overlay sets enable_email_domain, which
  changes how the whole container resolves an "@" in ANY username. A sibling runner expecting plain actor
  usernames could not survive either. Hence a separate feature in IntegrationV2-EmailUserName.

  WHAT MAKES THESE ASSERTIONS DISCRIMINATING. A 201/200 walk alone would pass even if the email principal were
  silently mangled or truncated, because the product would still be created — by SOMEONE. Two things are pinned
  instead:
    * the acting actor's EXACT physical username, before anything else runs, so a block that quietly provisioned
      plain usernames cannot make this feature pass; and
    * the product's stored PROVIDER, read back from the publisher plane, which must equal that same email-form
      principal (with the "@carbon.super" suffix stripped for a super-tenant user, as the publisher API does for
      every provider).
  Without the second assertion this would prove the lifecycle works, not that it works FOR AN EMAIL PROVIDER.

  The gateway half is pinned too (the block starts the node backend): an email-form principal is also the
  SUBJECT of the password-grant tokens the gateway validates, and key validation and subscriber lookup split
  that subject on "@" — so the product is invoked at every lifecycle stage with those tokens.
  Teardown via the per-scenario cleanup hook.

  # testCreateAPIProduct + testPublishAPIProduct + testChangeAPIProductLifecycleStateToBlockedState +
  # testDeleteRetiredAPIProducts, in one arc, as the email-form admin. Block -> Re-Publish is included because
  # BLOCKED is the one transition that is reversible, and the recovery is what proves the block did not strand
  # the product under an email-form owner.
  @cap:publisher @feat:products @rule:email-username @type:regression @dep:publisher @dep:devportal @legacy:APIProductLifecycleTest
  Scenario Outline: An API product owned by an email-form username provider completes its lifecycle as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"

    # Pin the physical username FIRST — if the block ever provisioned plain usernames, everything below would
    # still pass, and this feature would be silently testing nothing.
    When I store the acting actor credentials as "emailProductOwner" and "emailProductOwnerPassword"
    Then the actual value of "emailProductOwner" should match the expected value:
      """
      <expectedUsername>
      """

    And I have created an api from "artifacts/payloads/create_apim_product_lifecycle_api.json" as "emailLcApiId" and deployed it
    And the "apis" resource "emailLcApiId" should be live on the gateway, redeploying if propagation is lost
    And I have created an api from "artifacts/payloads/create_apim_product_leasing_api.json" as "emailLcApiTwoId" and deployed it
    And the "apis" resource "emailLcApiTwoId" should be live on the gateway, redeploying if propagation is lost
    When I create an API product "${UNIQUE:EmailLcProduct}" with context "${UNIQUE:emailLcProductCtx}" from APIs "emailLcApiId,emailLcApiTwoId" as "emailLcProductId"
    Then The response status code should be 201
    And The response array field "apis" should have exactly 2 entries
    And The response field "apis[*].apiId" should be exactly the list "{{emailLcApiId}},{{emailLcApiTwoId}}"
    And The value of response field "state" should be "CREATED"

    # The assertion the dimension exists for: the product is attributed to the EMAIL principal.
    And The provider of "api-products" resource "emailLcProductId" should match actor "<actor>"
    And The create response of API product "emailLcProductId" should echo its name, context, version and the provider of actor "<actor>"

    # Revision + deploy, then — still CREATED — the email-form publisher's internal key invokes the product on the
    # versioned and the versionless route.
    When I put the following JSON payload in context as "emailLcRev"
    """
    {"description":"email-provider product revision"}
    """
    And I make a request to create a revision for "api-products" resource "emailLcProductId" with payload "emailLcRev"
    Then The response status code should be 201
    When I deploy revision "revisionId" of "api-products" resource "emailLcProductId"
    Then The response status code should be 201
    And the "api-products" resource "emailLcProductId" should be live on the gateway, redeploying if propagation is lost
    Then The publisher product list should report API product "emailLcProductId" exactly once with the same info fields
    And The API product "emailLcProductId" read from the publisher should match its create response
    When I retrieve the "api-products" resource with id "emailLcProductId"
    And I extract response field "context" and store it as "emailLcProductContext"
    When I generate an internal API key for API "emailLcProductId" and store it as "emailLcInternalKey"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{emailLcProductContext}}/1.0.0/customers/123/" with method "GET" using internal key "emailLcInternalKey" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""
    When I invoke the API at gateway context "{{emailLcProductContext}}/customers/123/" with method "GET" using internal key "emailLcInternalKey" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""
    When I invoke the API at gateway context "{{emailLcProductContext}}/customers" with method "GET" using internal key "emailLcInternalKey" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    # Publish — auto-approved, and the onward transition set is pinned exactly (legacy asserted the count).
    When I publish the "api-products" resource with id "emailLcProductId"
    Then The response status code should be 200
    And The value of response field "workflowStatus" should be "APPROVED"
    And The value of response field "lifecycleState.state" should be "Published"
    And The response array field "lifecycleState.availableTransitions" should have exactly 4 entries
    # WHICH four — the transition set is a property of the api-products lifecycle, so it is identical to the
    # plain-username scenario's; an email-form provider must not change it.
    And The response field "lifecycleState.availableTransitions[*].event" should be exactly the list "Block,Deploy as a Prototype,Demote to Created,Deprecate"
    When I retrieve the "api-products" resource with id "emailLcProductId"
    Then The response should contain "PUBLISHED"
    # A published product reaches the devportal plane under the same email-form provider.
    Then The devportal should report API product "emailLcProductId" exactly once with the same fields
    And The devportal should advertise gateway endpoint URLs for API product "emailLcProductId"
    # Subscribe and take legacy's four credentials; the user tokens' subject is the email-form principal.
    When I have set up application with production and sandbox keys, subscribed to API "emailLcProductId" with plan "Unlimited", and obtained the four credentials as "emailLcSubId"
    When I invoke the API at gateway context "{{emailLcProductContext}}/customers" with method "GET" using access token "productionUserToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{emailLcProductContext}}/customers" with method "POST" using access token "productionUserToken" and payload "" with content type "text/plain" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{emailLcProductContext}}/customers/123" with method "PUT" using access token "productionUserToken" and payload "" with content type "text/plain" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{emailLcProductContext}}/customers/123" with method "DELETE" using access token "productionUserToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{emailLcProductContext}}/1.0.0/customers/123/" with method "GET" using access token "productionUserToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "name" should be "John"
    When I invoke the API at gateway context "{{emailLcProductContext}}/1.0.0/customers/123/" with method "GET" using access token "sandboxUserToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "name" should be "John"
    When I invoke the API at gateway context "{{emailLcProductContext}}/1.0.0/assets" with method "GET" using access token "productionAppToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "Hello World"
    When I invoke the API at gateway context "{{emailLcProductContext}}/1.0.0/assets" with method "POST" using access token "sandboxAppToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "Hello World"
    When I invoke every operation of the product leasing member at "{{emailLcProductContext}}/1.0.0" with access tokens "productionUserToken,sandboxUserToken" expecting status 200 and body "Hello World"

    # BLOCKED, then recovered — the reversible transition.
    When I change the lifecycle of "api-products" resource "emailLcProductId" with action "Block"
    Then The response status code should be 200
    And The value of response field "workflowStatus" should be "APPROVED"
    And The value of response field "lifecycleState.state" should be "Blocked"
    And The response field "lifecycleState.availableTransitions[*].event" should be exactly the list "Deprecate,Re-Publish"
    When I retrieve the "api-products" resource with id "emailLcProductId"
    Then The response should contain "BLOCKED"
    When I invoke the API at gateway context "{{emailLcProductContext}}/1.0.0/customers/123/" with method "GET" using access token "productionUserToken" and payload "" until response status code becomes 503 within 60 seconds
    Then The response status code should be 503
    When I invoke the API at gateway context "{{emailLcProductContext}}/customers/123/" with method "GET" using access token "productionUserToken" and payload "" until response status code becomes 503 within 60 seconds
    Then The response status code should be 503
    When I invoke the API at gateway context "{{emailLcProductContext}}/customers" with method "GET" using access token "productionUserToken" and payload "" until response status code becomes 503 within 60 seconds
    Then The response status code should be 503
    When I invoke the API at gateway context "{{emailLcProductContext}}/customers" with method "GET" without authentication until response status code becomes 503 within 60 seconds
    Then The response status code should be 503
    When I invoke the API at gateway context "{{emailLcProductContext}}/customers/123/" with method "GET" without authentication until response status code becomes 503 within 60 seconds
    Then The response status code should be 503
    When I change the lifecycle of "api-products" resource "emailLcProductId" with action "Re-Publish"
    Then The response status code should be 200
    And The value of response field "workflowStatus" should be "APPROVED"
    And The value of response field "lifecycleState.state" should be "Published"
    When I retrieve the "api-products" resource with id "emailLcProductId"
    Then The response should contain "PUBLISHED"
    Then The devportal should report API product "emailLcProductId" exactly once with the same fields
    When I invoke the API at gateway context "{{emailLcProductContext}}/customers" with method "GET" using access token "productionUserToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{emailLcProductContext}}/customers" with method "POST" using access token "productionUserToken" and payload "" with content type "text/plain" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{emailLcProductContext}}/customers/123" with method "PUT" using access token "productionUserToken" and payload "" with content type "text/plain" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{emailLcProductContext}}/customers/123" with method "DELETE" using access token "productionUserToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{emailLcProductContext}}/1.0.0/customers/123/" with method "GET" using access token "productionUserToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "name" should be "John"
    When I invoke every operation of the product leasing member at "{{emailLcProductContext}}/1.0.0" with access tokens "productionUserToken,sandboxUserToken" expecting status 200 and body "Hello World"

    # Deprecate -> Retire -> delete. A retired product is deletable (legacy testDeleteRetiredAPIProducts).
    When I change the lifecycle of "api-products" resource "emailLcProductId" with action "Deprecate"
    Then The response status code should be 200
    And The value of response field "workflowStatus" should be "APPROVED"
    When I retrieve the "api-products" resource with id "emailLcProductId"
    Then The response should contain "DEPRECATED"
    Then The devportal should report API product "emailLcProductId" with lifecycle status "DEPRECATED"
    When I invoke the API at gateway context "{{emailLcProductContext}}/customers" with method "GET" using access token "productionUserToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{emailLcProductContext}}/customers" with method "POST" using access token "productionUserToken" and payload "" with content type "text/plain" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{emailLcProductContext}}/customers/123" with method "PUT" using access token "productionUserToken" and payload "" with content type "text/plain" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{emailLcProductContext}}/customers/123" with method "DELETE" using access token "productionUserToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{emailLcProductContext}}/1.0.0/customers/123/" with method "GET" using access token "sandboxUserToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "name" should be "John"
    When I invoke every operation of the product leasing member at "{{emailLcProductContext}}/1.0.0" with access tokens "productionUserToken" expecting status 200 and body "Hello World"
    When I change the lifecycle of "api-products" resource "emailLcProductId" with action "Retire"
    Then The response status code should be 200
    And The value of response field "workflowStatus" should be "APPROVED"
    And The value of response field "lifecycleState.state" should be "Retired"
    When I retrieve the "api-products" resource with id "emailLcProductId"
    Then The response should contain "RETIRED"
    When I retrieve the subscriptions of API "emailLcProductId"
    Then The response status code should be 200
    And The subscription list should contain exactly 0 subscriptions
    # The provider survives the whole lifecycle — a transition must not rewrite ownership.
    And The provider of "api-products" resource "emailLcProductId" should match actor "<actor>"
    When I delete the "api-products" resource with id "emailLcProductId"
    Then The response status code should be 200

    Examples:
      | actor                  | expectedUsername                  |
      | emailAdmin             | emailAdmin@email.com@carbon.super |
      | emailAdmin@tenant1.com | emailAdmin@email.com@tenant1.com  |

  # testDeleteDeprecatedAPIProductsWithSubscription: a PUBLISHED product carrying an active subscription is
  # refused deletion (409) and only becomes deletable once RETIRED. Driven here by the email-form admin on both
  # planes — it creates the product AND subscribes to it — so the refusal is pinned for a subscription whose
  # owner and provider are both email-form principals.
  @cap:publisher @feat:products @rule:email-username @type:negative @dep:publisher @dep:devportal @legacy:APIProductLifecycleTest
  Scenario Outline: A subscribed API product owned by an email-form provider cannot be deleted until retired as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I store the acting actor credentials as "emailSubProductOwner" and "emailSubProductOwnerPassword"
    Then the actual value of "emailSubProductOwner" should match the expected value:
      """
      <expectedUsername>
      """

    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "emailSubApiId" and deployed it
    And I have created an api from "artifacts/payloads/create_apim_test_api_two.json" as "emailSubApiTwoId" and deployed it
    When I create an API product "${UNIQUE:EmailSubProduct}" with context "${UNIQUE:emailSubProductCtx}" from APIs "emailSubApiId,emailSubApiTwoId" as "emailSubProductId"
    Then The response status code should be 201
    And The provider of "api-products" resource "emailSubProductId" should match actor "<actor>"
    When I put the following JSON payload in context as "emailSubRev"
    """
    {"description":"email-provider subscribed product revision"}
    """
    And I make a request to create a revision for "api-products" resource "emailSubProductId" with payload "emailSubRev"
    Then The response status code should be 201
    When I deploy revision "revisionId" of "api-products" resource "emailSubProductId"
    Then The response status code should be 201
    When I publish the "api-products" resource with id "emailSubProductId"
    Then The response status code should be 200
    And The value of response field "workflowStatus" should be "APPROVED"

    # An application owned by the same email-form principal subscribes to the product.
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "emailSubAppPayload"
    And I create an application with payload "emailSubAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "emailProductSubPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "emailSubProductId" using application "createdAppId" with payload "emailProductSubPayload" as "emailProductSubId"
    Then The response status code should be 201

    # The subject: deletion is refused while the subscription is live.
    When I delete the "api-products" resource with id "emailSubProductId"
    Then The response status code should be 409
    And The response should contain "active subscriptions exist"

    # Retire first, then the same delete succeeds — so the 409 above was about the subscription, not about
    # an email-form provider being unable to delete its own product.
    When I change the lifecycle of "api-products" resource "emailSubProductId" with action "Deprecate"
    Then The response status code should be 200
    And The value of response field "workflowStatus" should be "APPROVED"
    When I change the lifecycle of "api-products" resource "emailSubProductId" with action "Retire"
    Then The response status code should be 200
    And The value of response field "workflowStatus" should be "APPROVED"
    And The value of response field "lifecycleState.state" should be "Retired"
    # Retiring removed the subscription.
    When I retrieve the subscriptions of API "emailSubProductId"
    Then The response status code should be 200
    And The subscription list should contain exactly 0 subscriptions
    When I delete the "api-products" resource with id "emailSubProductId"
    Then The response status code should be 200

    Examples:
      | actor                  | expectedUsername                  |
      | emailAdmin             | emailAdmin@email.com@carbon.super |
      | emailAdmin@tenant1.com | emailAdmin@email.com@tenant1.com  |
