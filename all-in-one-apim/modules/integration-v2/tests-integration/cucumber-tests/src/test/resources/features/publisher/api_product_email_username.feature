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

  No gateway invocation and no backend: the gateway's answer to a BLOCKED / DEPRECATED product is
  gateway/api_product_invocation's subject and needs that block's backend. What is pinned here is the
  publisher-plane half — the transitions themselves, and that an email-form provider owns them.
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

    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "emailLcApiId" and deployed it
    When I create an API product "${UNIQUE:EmailLcProduct}" with context "${UNIQUE:emailLcProductCtx}" from API "emailLcApiId" as "emailLcProductId"
    Then The response status code should be 201

    # The assertion the dimension exists for: the product is attributed to the EMAIL principal.
    And The provider of "api-products" resource "emailLcProductId" should match actor "<actor>"

    # Publish — auto-approved, and the onward transition set is pinned exactly (legacy asserted the count).
    When I publish the "api-products" resource with id "emailLcProductId"
    Then The response status code should be 200
    And The value of response field "workflowStatus" should be "APPROVED"
    And The value of response field "lifecycleState.state" should be "Published"
    And The response array field "lifecycleState.availableTransitions" should have exactly 4 entries
    When I retrieve the "api-products" resource with id "emailLcProductId"
    Then The response should contain "PUBLISHED"
    # A published product reaches the devportal plane under the same email-form provider.
    Then The devportal should report API product "emailLcProductId" exactly once with the same fields

    # BLOCKED, then recovered — the reversible transition.
    When I change the lifecycle of "api-products" resource "emailLcProductId" with action "Block"
    Then The response status code should be 200
    When I retrieve the "api-products" resource with id "emailLcProductId"
    Then The response should contain "BLOCKED"
    When I change the lifecycle of "api-products" resource "emailLcProductId" with action "Re-Publish"
    Then The response status code should be 200
    When I retrieve the "api-products" resource with id "emailLcProductId"
    Then The response should contain "PUBLISHED"

    # Deprecate -> Retire -> delete. A retired product is deletable (legacy testDeleteRetiredAPIProducts).
    When I change the lifecycle of "api-products" resource "emailLcProductId" with action "Deprecate"
    Then The response status code should be 200
    When I retrieve the "api-products" resource with id "emailLcProductId"
    Then The response should contain "DEPRECATED"
    When I change the lifecycle of "api-products" resource "emailLcProductId" with action "Retire"
    Then The response status code should be 200
    When I retrieve the "api-products" resource with id "emailLcProductId"
    Then The response should contain "RETIRED"
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
    When I create an API product "${UNIQUE:EmailSubProduct}" with context "${UNIQUE:emailSubProductCtx}" from API "emailSubApiId" as "emailSubProductId"
    Then The response status code should be 201
    And The provider of "api-products" resource "emailSubProductId" should match actor "<actor>"
    When I publish the "api-products" resource with id "emailSubProductId"
    Then The response status code should be 200

    # An application owned by the same email-form principal subscribes to the product.
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "emailSubAppPayload"
    And I create an application with payload "emailSubAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "emailProductSubPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{emailSubProductId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "emailSubProductId" using application "createdAppId" with payload "emailProductSubPayload" as "emailProductSubId"
    Then The response status code should be 201

    # The subject: deletion is refused while the subscription is live.
    When I delete the "api-products" resource with id "emailSubProductId"
    Then The response status code should be 409

    # Retire first, then the same delete succeeds — so the 409 above was about the subscription, not about
    # an email-form provider being unable to delete its own product.
    When I change the lifecycle of "api-products" resource "emailSubProductId" with action "Deprecate"
    Then The response status code should be 200
    When I change the lifecycle of "api-products" resource "emailSubProductId" with action "Retire"
    Then The response status code should be 200
    When I delete the "api-products" resource with id "emailSubProductId"
    Then The response status code should be 200

    Examples:
      | actor                  | expectedUsername                  |
      | emailAdmin             | emailAdmin@email.com@carbon.super |
      | emailAdmin@tenant1.com | emailAdmin@email.com@tenant1.com  |
