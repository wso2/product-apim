@cleanup
Feature: Gateway API Product Invocation

  Ports the invocation + lifecycle-stage behaviour of the legacy APIProductCreationTestCase / APIProductLifecycleTest:
  an API Product, once deployed + published + subscribed, is invocable at the gateway through its own context
  (routing each aggregated resource to ITS source API's backend) with every credential type — PRODUCTION and
  SANDBOX application keys and password-grant USER tokens against both — and the gateway response tracks the
  product's lifecycle state (PUBLISHED → 200, BLOCKED → 503, Re-Publish → 200, DEPRECATED → 200) — the product
  analogue of gateway/lifecycle_stage_invocation. Also covers what a product does with CHANGES to a member API:
  an endpoint switch and an operation-policy change must both surface through an already-deployed product once it
  is re-saved and redeployed. Products aggregate TWO APIs (with disjoint resource sets) where the legacy did.
  Runs in the concurrent IntegrationV2-Gateway block (backend started). Teardown via @cleanup.

  @cap:gateway @feat:rest-invocation @type:smoke @dep:publisher @legacy:APIProductCreationTestCase
  Scenario Outline: Invoke a published API product over two APIs with all four credential types as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "prodApiId" and deployed it
    And I have created an api from "artifacts/payloads/create_apim_test_api_two.json" as "prodApiTwoId" and deployed it
    When I create an API product "${UNIQUE:InvokeProduct}" with context "${UNIQUE:invokeProductCtx}" from APIs "prodApiId,prodApiTwoId" as "productId"
    Then The response status code should be 201
    # Deploy a product revision, publish, and capture the product's gateway context.
    When I put the following JSON payload in context as "prodRev"
    """
    {"description":"initial product revision"}
    """
    And I make a request to create a revision for "api-products" resource "productId" with payload "prodRev"
    When I put the following JSON payload in context as "prodDeploy"
    """
    [{"name":"{{gatewayEnvironment}}","vhost":"localhost","displayOnDevportal":true}]
    """
    And I make a request to deploy revision "revisionId" of "api-products" resource "productId" with payload "prodDeploy"
    Then The response status code should be 201
    When I publish the "api-products" resource with id "productId"
    Then The response status code should be 200
    When I retrieve the "api-products" resource with id "productId"
    And I extract response field "context" and store it as "productContext"
    # Subscribe ONE application and take all four credentials legacy invoked a product with.
    When I have set up application with production and sandbox keys, subscribed to API "productId" with plan "Unlimited", and obtained the four credentials as "prodSubId"
    # The production application token invokes the first member API's resource. Each invocation pins the SOURCE
    # API's own backend signature and the OTHER member's absent: member API one points at node-customer-service
    # ({"id":123,"name":"John"}) and member API two at the wildcard backend ("Hello World"), so the pair of
    # assertions is what proves the product resolved the resource to the RIGHT member — a bare 200 would be
    # satisfied equally by the wrong member answering.
    When I invoke the API at gateway context "{{productContext}}/1.0.0/customers/123/" with method "GET" using access token "productionAppToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""
    And The response should not contain "Hello World"
    # …and so do the SANDBOX application token and the password-grant user tokens of both key mappings.
    When I invoke the API at gateway context "{{productContext}}/1.0.0/customers/123/" with method "GET" using access token "sandboxAppToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    And The response should not contain "Hello World"
    When I invoke the API at gateway context "{{productContext}}/1.0.0/customers/123/" with method "GET" using access token "productionUserToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    And The response should not contain "Hello World"
    When I invoke the API at gateway context "{{productContext}}/1.0.0/customers/123/" with method "GET" using access token "sandboxUserToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    And The response should not contain "Hello World"
    # The SECOND member API's resource is reachable through the same product context, i.e. the product really
    # aggregates both APIs and routes each resource to its own source backend — the mirrored assertion pair.
    When I invoke the API at gateway context "{{productContext}}/1.0.0/assets" with method "GET" using access token "productionAppToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "Hello World"
    And The response should not contain "\"name\":\"John\""

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # An endpoint change on a MEMBER API must surface through an already-deployed product once the product is
  # re-saved and redeployed.
  #
  # DELIBERATE DEVIATION from legacy's observation mechanism: legacy proved which upstream served the call with
  # a `Version: v2` RESPONSE HEADER emitted by a second Synapse API. No backend under tests-common/nodeapps
  # emits such a header, and one was deliberately NOT added — a header invented for a single test is weaker
  # evidence than a whole-backend difference. Instead the propagation is observed as a BACKEND IDENTITY change
  # between two EXISTING backends:
  #   * node-customer-service (nodebackend:3001, /jaxrs_basic/services/customers/customerservice/) answers
  #     GET /customers/123 with {"id":123,"name":"John"};
  #   * wildcard            (nodebackend:3017) answers ANY path/method with the plain text "Hello World".
  # Each phase asserts its own backend's signature present AND the other backend's signature absent, so neither
  # phase can pass against the wrong upstream, and the "after" state is unreachable from the "before" state.
  # Ports APIProductCreationTestCase#testCreateAndInvokeApiProduct steps 9–10.
  @cap:gateway @feat:rest-invocation @rule:product @type:regression @dep:publisher @legacy:APIProductCreationTestCase
  Scenario Outline: An endpoint change on a member API surfaces through an existing API product as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "epApiId" and deployed it
    When I create an API product "${UNIQUE:EndpointProduct}" with context "${UNIQUE:endpointProductCtx}" from API "epApiId" as "epProductId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "epRev"
    """
    {"description":"initial product revision"}
    """
    And I make a request to create a revision for "api-products" resource "epProductId" with payload "epRev"
    When I deploy revision "revisionId" of "api-products" resource "epProductId"
    Then The response status code should be 201
    When I publish the "api-products" resource with id "epProductId"
    Then The response status code should be 200
    When I retrieve the "api-products" resource with id "epProductId"
    And I extract response field "context" and store it as "epProductContext"
    When I have set up application with keys, subscribed to API "epProductId", and obtained access token for "epSubId"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{epProductContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""
    And The response should not contain "Hello World"
    # Switch the member API's PRODUCTION endpoint to the wildcard backend (sandbox stays on the original).
    When I retrieve the "apis" resource with id "epApiId"
    And I put the response payload in context as "epApiPayload"
    When I put the following JSON payload in context as "epV2Endpoint"
    """
    {"endpoint_type":"http","production_endpoints":{"url":"http://nodebackend:3017/"},"sandbox_endpoints":{"url":"http://nodebackend:3001/jaxrs_basic/services/customers/customerservice/"}}
    """
    When I update the "apis" resource "epApiId" and "epApiPayload" with configuration type "endpointConfig" and value:
      """
      epV2Endpoint
      """
    Then The response status code should be 200
    # Re-save the product (legacy PUT the fetched product DTO back) and redeploy it so the change reaches the
    # gateway through the product.
    When I retrieve the "api-products" resource with id "epProductId"
    And I put the response payload in context as "epProductPayload"
    When I update "api-products" resource of id "epProductId" with payload "epProductPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "epRev2"
    """
    {"description":"product revision after the endpoint change"}
    """
    And I make a request to create a revision for "api-products" resource "epProductId" with payload "epRev2"
    Then The response status code should be 201
    When I deploy revision "revisionId" of "api-products" resource "epProductId"
    Then The response status code should be 201
    # The product now routes to the wildcard upstream: 200, ITS body, and the customer service's body gone.
    When I invoke the API at gateway context "{{epProductContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response body contains "Hello World" within 120 seconds
    Then The response status code should be 200
    And The response should contain "Hello World"
    And The response should not contain "\"name\":\"John\""

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # A publisher INTERNAL API key invokes a deployed but not-yet-published product (the publisher try-out path) —
  # ports the internal-key half of APIProductLifecycleTest#testCreateAPIProduct, which generated the key while the
  # product was still in CREATED.
  @cap:gateway @feat:rest-invocation @rule:product @type:regression @dep:publisher @legacy:APIProductLifecycleTest
  Scenario Outline: A deployed API product in CREATED state is invocable with a publisher internal API key as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "ikApiId" and deployed it
    And I have created an api from "artifacts/payloads/create_apim_test_api_two.json" as "ikApiTwoId" and deployed it
    When I create an API product "${UNIQUE:InternalKeyProduct}" with context "${UNIQUE:internalKeyProductCtx}" from APIs "ikApiId,ikApiTwoId" as "ikProductId"
    Then The response status code should be 201
    And The value of response field "state" should be "CREATED"
    When I put the following JSON payload in context as "ikRev"
    """
    {"description":"internal-key product revision"}
    """
    And I make a request to create a revision for "api-products" resource "ikProductId" with payload "ikRev"
    When I deploy revision "revisionId" of "api-products" resource "ikProductId"
    Then The response status code should be 201
    When I retrieve the "api-products" resource with id "ikProductId"
    And I extract response field "context" and store it as "ikProductContext"
    # The product is still in CREATED (never published), so only the internal key can invoke it.
    When I generate an internal API key for API "ikProductId" and store it as "ikInternalKey"
    Then The response status code should be 200
    # The internal key is the credential under test, so the call must be shown to have reached the member API's
    # OWN backend — the product also aggregates a second API on the wildcard backend, whose "Hello World" would
    # satisfy a bare 200 just as well.
    When I invoke the API at gateway context "{{ikProductContext}}/1.0.0/customers/123/" with method "GET" using internal key "ikInternalKey" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""
    And The response should not contain "Hello World"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  @cap:gateway @feat:rest-invocation @type:regression @dep:publisher @legacy:APIProductLifecycleTest
  Scenario Outline: The gateway response to an API product invocation tracks its lifecycle state as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "lcApiId" and deployed it
    When I create an API product "${UNIQUE:LcProduct}" with context "${UNIQUE:lcProductCtx}" from API "lcApiId" as "lcProductId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "lcRev"
    """
    {"description":"initial product revision"}
    """
    And I make a request to create a revision for "api-products" resource "lcProductId" with payload "lcRev"
    When I put the following JSON payload in context as "lcDeploy"
    """
    [{"name":"{{gatewayEnvironment}}","vhost":"localhost","displayOnDevportal":true}]
    """
    And I make a request to deploy revision "revisionId" of "api-products" resource "lcProductId" with payload "lcDeploy"
    Then The response status code should be 201
    When I publish the "api-products" resource with id "lcProductId"
    Then The response status code should be 200
    When I retrieve the "api-products" resource with id "lcProductId"
    And I extract response field "context" and store it as "lcProductContext"
    When I have set up application with keys, subscribed to API "lcProductId", and obtained access token for "lcSubId"
    Then The response status code should be 200

    # PUBLISHED → invocable.
    When I invoke the API at gateway context "{{lcProductContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    # BLOCKED → gateway refuses (503). The transition itself is auto-approved.
    When I change the lifecycle of "api-products" resource "lcProductId" with action "Block"
    Then The response status code should be 200
    And The value of response field "workflowStatus" should be "APPROVED"
    And The value of response field "lifecycleState.state" should be "Blocked"
    When I invoke the API at gateway context "{{lcProductContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 503 within 60 seconds
    Then The response status code should be 503

    # Re-Publish from BLOCKED → PUBLISHED and the SAME credential is served again (the recovery transition).
    When I change the lifecycle of "api-products" resource "lcProductId" with action "Re-Publish"
    Then The response status code should be 200
    And The value of response field "workflowStatus" should be "APPROVED"
    And The value of response field "lifecycleState.state" should be "Published"
    When I invoke the API at gateway context "{{lcProductContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    # DEPRECATED → still invocable (200).
    When I change the lifecycle of "api-products" resource "lcProductId" with action "Deprecate"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{lcProductContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    # (RETIRED is a publisher/delete concern for products — see publisher/api_products "lifecycle … deleted when
    #  retired". Unlike a retired API (404), a retired product's key validation fails with 900900/500, which the
    #  legacy never asserted, so it is deliberately not asserted at the gateway here.)

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # D2: a scope gated on the source API's operation is enforced when the operation is invoked through a product
  # — a token WITH the scope succeeds (200), one WITHOUT it is refused (403). Ports
  # APIProductCreationTestCase#testCreateAndInvokeApiProductWithScopes.
  @cap:gateway @feat:security-enforcement @rule:product @type:regression @dep:publisher @legacy:APIProductCreationTestCase
  Scenario Outline: A scope-gated operation is enforced when invoked through an API product as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "scopedApiId" and deployed it
    When I create a new shared scope as "prodScopeEnf"
    Then The response status code should be 201
    And I extract response field "name" and store it as "prodScopeName"
    # Register the scope on the API and gate the GET /customers/{id} operation with it.
    When I retrieve the "apis" resource with id "scopedApiId"
    And I put the response payload in context as "scopedApiPayload"
    When I update the "apis" resource "scopedApiId" and "scopedApiPayload" with configuration type "scopes" and value:
      """
      [{"shared":true,"scope":{"name":"{{prodScopeName}}","displayName":"{{prodScopeName}}","description":"product scope enforcement","bindings":["admin"]}}]
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "scopedApiId"
    And I put the response payload in context as "scopedApiPayload"
    When I update the "apis" resource "scopedApiId" and "scopedApiPayload" with configuration type "operations" and value:
      """
      [{"target":"/customers/{id}","verb":"GET","authType":"Application & Application User","throttlingPolicy":"Unlimited","scopes":["{{prodScopeName}}"],"operationPolicies":{"request":[],"response":[],"fault":[]}},{"target":"/customers/{id}","verb":"DELETE","authType":"Application & Application User","throttlingPolicy":"Unlimited","scopes":[],"operationPolicies":{"request":[],"response":[],"fault":[]}}]
      """
    Then The response status code should be 200
    # Aggregate the scoped API into a product (the product inherits the gated operation), deploy and publish.
    When I create an API product "${UNIQUE:ScopeProduct}" with context "${UNIQUE:scopeProductCtx}" from API "scopedApiId" as "scopeProductId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "scopeRev"
    """
    {"description":"scoped product revision"}
    """
    And I make a request to create a revision for "api-products" resource "scopeProductId" with payload "scopeRev"
    When I put the following JSON payload in context as "scopeDeploy"
    """
    [{"name":"{{gatewayEnvironment}}","vhost":"localhost","displayOnDevportal":true}]
    """
    And I make a request to deploy revision "revisionId" of "api-products" resource "scopeProductId" with payload "scopeDeploy"
    Then The response status code should be 201
    When I publish the "api-products" resource with id "scopeProductId"
    Then The response status code should be 200
    When I retrieve the "api-products" resource with id "scopeProductId"
    And I extract response field "context" and store it as "scopeProductContext"
    # Subscribe an application and key it.
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "generateApplicationKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateApplicationKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "apiSubscriptionPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "scopeProductId" using application "createdAppId" with payload "apiSubscriptionPayload" as "scopeSubId"
    Then The response status code should be 201
    # A token WITH the scope invokes the gated operation (200); one WITHOUT it (a different scope) is refused (403).
    When I request an OAuth access token for the current user using password grant with scope "{{prodScopeName}}"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{scopeProductContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    # The scoped token is the subject: pin the backend payload so "the scope let the call THROUGH to the gated
    # operation" is distinguishable from any other 200 (the API's DELETE /customers/{id} is ungated and answers
    # 200 with an empty body).
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    When I request an OAuth access token for the current user using password grant with scope "openid"
    Then The response status code should be 200
    And I invoke the API at gateway context "{{scopeProductContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # D1: a product that aggregates an API whose devportal visibility is RESTRICTED (visibleRoles) is still
  # invocable through the product (the source API's visibility restriction does not block product invocation),
  # with every credential type. Ports
  # APIProductCreationTestCase#testCreateAndInvokeApiProductWithVisibilityRestrictedApi (whose
  # invocationStatusCodes is empty — i.e. all operations expected to return 200, no 403), which aggregated the
  # restricted API together with a second, unrestricted one.
  @cap:gateway @feat:rest-invocation @rule:product @type:regression @dep:publisher @legacy:APIProductCreationTestCase
  Scenario Outline: A product aggregating a visibility-restricted API is invocable as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_restricted_visibility_api.json" as "restrictedApiId" and deployed it
    And I have created an api from "artifacts/payloads/create_apim_test_api_two.json" as "restrictedApiTwoId" and deployed it
    When I create an API product "${UNIQUE:RestrictedProduct}" with context "${UNIQUE:restrictedProductCtx}" from APIs "restrictedApiId,restrictedApiTwoId" as "restrictedProductId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "restrictedRev"
    """
    {"description":"restricted-visibility product revision"}
    """
    And I make a request to create a revision for "api-products" resource "restrictedProductId" with payload "restrictedRev"
    When I put the following JSON payload in context as "restrictedDeploy"
    """
    [{"name":"{{gatewayEnvironment}}","vhost":"localhost","displayOnDevportal":true}]
    """
    And I make a request to deploy revision "revisionId" of "api-products" resource "restrictedProductId" with payload "restrictedDeploy"
    Then The response status code should be 201
    When I publish the "api-products" resource with id "restrictedProductId"
    Then The response status code should be 200
    When I retrieve the "api-products" resource with id "restrictedProductId"
    And I extract response field "context" and store it as "restrictedProductContext"
    When I have set up application with production and sandbox keys, subscribed to API "restrictedProductId" with plan "Unlimited", and obtained the four credentials as "restrictedSubId"
    # The RESTRICTED member API is the one under test, so each credential must be shown to reach ITS backend
    # (node-customer-service). The product's other member sits on the wildcard backend ("Hello World"), which a
    # bare 200 could not rule out — that is precisely the "restriction silently diverted the call" failure.
    When I invoke the API at gateway context "{{restrictedProductContext}}/1.0.0/customers/123/" with method "GET" using access token "productionAppToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""
    And The response should not contain "Hello World"
    When I invoke the API at gateway context "{{restrictedProductContext}}/1.0.0/customers/123/" with method "GET" using access token "sandboxAppToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    And The response should not contain "Hello World"
    When I invoke the API at gateway context "{{restrictedProductContext}}/1.0.0/customers/123/" with method "GET" using access token "productionUserToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    And The response should not contain "Hello World"
    When I invoke the API at gateway context "{{restrictedProductContext}}/1.0.0/customers/123/" with method "GET" using access token "sandboxUserToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    And The response should not contain "Hello World"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # D4: a product that aggregates an advertise-only API is invocable through the product — the product provides
  # the gateway routing (to the advertised API's external endpoint, here the node backend), even though an
  # advertise-only API is not itself gateway-deployed — with every credential type. Ports
  # APIProductCreationTestCase#testCreateApiProductWithAdvertiseOnlyApi (which aggregated the advertise-only API
  # ALONE — one member API is faithful here). The advertised API is only CREATED (not deployed); only the product
  # is deployed.
  @cap:gateway @feat:rest-invocation @rule:product @type:regression @dep:publisher @legacy:APIProductCreationTestCase
  Scenario Outline: A product aggregating an advertise-only API is invocable as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_advertise_api.json" in context as "advertisePayload"
    And I create an "apis" resource with payload "advertisePayload" as "advertiseApiId"
    Then The response status code should be 201
    When I create an API product "${UNIQUE:AdvertiseProduct}" with context "${UNIQUE:advertiseProductCtx}" from API "advertiseApiId" as "advertiseProductId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "advertiseRev"
    """
    {"description":"advertise product revision"}
    """
    And I make a request to create a revision for "api-products" resource "advertiseProductId" with payload "advertiseRev"
    When I put the following JSON payload in context as "advertiseDeploy"
    """
    [{"name":"{{gatewayEnvironment}}","vhost":"localhost","displayOnDevportal":true}]
    """
    And I make a request to deploy revision "revisionId" of "api-products" resource "advertiseProductId" with payload "advertiseDeploy"
    Then The response status code should be 201
    When I publish the "api-products" resource with id "advertiseProductId"
    Then The response status code should be 200
    When I retrieve the "api-products" resource with id "advertiseProductId"
    And I extract response field "context" and store it as "advertiseProductContext"
    When I have set up application with production and sandbox keys, subscribed to API "advertiseProductId" with plan "Unlimited", and obtained the four credentials as "advertiseSubId"
    # The claim is that the product routes to the ADVERTISED API's EXTERNAL endpoint even though that API is not
    # itself gateway-deployed. Only the backend payload shows the call actually got there, so each credential
    # asserts it.
    When I invoke the API at gateway context "{{advertiseProductContext}}/1.0.0/customers/123/" with method "GET" using access token "productionAppToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""
    When I invoke the API at gateway context "{{advertiseProductContext}}/1.0.0/customers/123/" with method "GET" using access token "sandboxAppToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    When I invoke the API at gateway context "{{advertiseProductContext}}/1.0.0/customers/123/" with method "GET" using access token "productionUserToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    When I invoke the API at gateway context "{{advertiseProductContext}}/1.0.0/customers/123/" with method "GET" using access token "sandboxUserToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # D3: a request-flow operation policy (jsonToXML) on the source API's operation is applied when the operation
  # is invoked through a product — a JSON request body is transformed to XML before reaching the backend (which
  # echoes the body it received). Ports
  # APIProductCreationTestCase#testCreateAndInvokeApiProductWithOperationPoliciesInRequestApi.
  @cap:gateway @feat:mediation-policies @rule:product @type:regression @dep:publisher @legacy:APIProductCreationTestCase
  Scenario Outline: A request-transformation operation policy is applied when invoked through an API product as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_optransform_api.json" as "opPolicyApiId" and deployed it
    When I create an API product "${UNIQUE:OpPolicyProduct}" with context "${UNIQUE:opPolicyProductCtx}" from API "opPolicyApiId" as "opPolicyProductId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "opPolicyRev"
    """
    {"description":"operation-policy product revision"}
    """
    And I make a request to create a revision for "api-products" resource "opPolicyProductId" with payload "opPolicyRev"
    When I put the following JSON payload in context as "opPolicyDeploy"
    """
    [{"name":"{{gatewayEnvironment}}","vhost":"localhost","displayOnDevportal":true}]
    """
    And I make a request to deploy revision "revisionId" of "api-products" resource "opPolicyProductId" with payload "opPolicyDeploy"
    Then The response status code should be 201
    When I publish the "api-products" resource with id "opPolicyProductId"
    Then The response status code should be 200
    When I retrieve the "api-products" resource with id "opPolicyProductId"
    And I extract response field "context" and store it as "opPolicyProductContext"
    When I have set up application with keys, subscribed to API "opPolicyProductId", and obtained access token for "opPolicySubId"
    Then The response status code should be 200
    # A JSON request body is converted to XML by the jsonToXML request policy before reaching the reflecting backend.
    When I put the following JSON payload in context as "opPolicyBody"
    """
    {"foo":"bar"}
    """
    And I invoke the API at gateway context "{{opPolicyProductContext}}/1.0.0/reflect-body" with method "POST" using access token "generatedAccessToken" and payload "opPolicyBody" until response body contains "<jsonObject>" within 60 seconds
    Then The response should contain "<foo>bar</foo>"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # The RESPONSE-flow counterpart of D3, plus a base-API policy CHANGE surfacing through an existing product.
  # The member API carries an xmlToJson RESPONSE policy and a jsonFault fault policy; the backend echoes the body
  # with the content type it received, so an XML request comes back as an XML response and the response policy
  # converts it to JSON. Then the member API's policies are CHANGED (a jsonToXML request policy added, the
  # response policy cleared) and, after the product is re-saved and redeployed, a JSON request is converted to XML
  # inbound and echoed back as XML — proving the change surfaced through the existing product. Ports
  # APIProductCreationTestCase#testCreateAndInvokeApiProductWithOperationPoliciesInResponseApi.
  @cap:gateway @feat:mediation-policies @rule:product @type:regression @dep:publisher @legacy:APIProductCreationTestCase
  Scenario Outline: A response-transformation operation policy is applied when invoked through an API product as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_optransform_response_api.json" as "respPolicyApiId" and deployed it
    When I create an API product "${UNIQUE:RespPolicyProduct}" with context "${UNIQUE:respPolicyProductCtx}" from API "respPolicyApiId" as "respPolicyProductId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "respPolicyRev"
    """
    {"description":"response-policy product revision"}
    """
    And I make a request to create a revision for "api-products" resource "respPolicyProductId" with payload "respPolicyRev"
    When I deploy revision "revisionId" of "api-products" resource "respPolicyProductId"
    Then The response status code should be 201
    When I publish the "api-products" resource with id "respPolicyProductId"
    Then The response status code should be 200
    When I retrieve the "api-products" resource with id "respPolicyProductId"
    And I extract response field "context" and store it as "respPolicyProductContext"
    When I have set up application with keys, subscribed to API "respPolicyProductId", and obtained access token for "respPolicySubId"
    Then The response status code should be 200
    # An XML request body is echoed back as XML and the xmlToJson RESPONSE policy converts it to JSON.
    When I put the following JSON payload in context as "respPolicyXmlBody"
    """
    <foo>bar</foo>
    """
    And I invoke the API at gateway context "{{respPolicyProductContext}}/1.0.0/reflect-body-typed" with method "POST" using access token "generatedAccessToken" and payload "respPolicyXmlBody" with content type "application/xml" until response status code becomes 200 within 60 seconds
    Then The response should contain "{\"foo\":\"bar\"}"
    # CHANGE the MEMBER API's operation policies — add a jsonToXML REQUEST policy (as the legacy did) and clear
    # the response policy — then re-save + redeploy the product so the change reaches the gateway through it.
    # The response policy is cleared in the same update deliberately: with BOTH policies in place the
    # JSON→XML→JSON round trip is invisible at the client (Synapse's XML/JSON conversion uses `jsonObject` as the
    # anonymous-object root element, so xmlToJson strips the very wrapper jsonToXML added and the body reads
    # {"foo":"bar"} either way) — the assertion could then not tell "the change reached the gateway" from "it did
    # not". With the response policy gone, the jsonObject wrapper the request policy produced is visible verbatim,
    # which no pre-change state can produce.
    When I retrieve the "apis" resource with id "respPolicyApiId"
    And I put the response payload in context as "respPolicyApiPayload"
    When I update the "apis" resource "respPolicyApiId" and "respPolicyApiPayload" with configuration type "operations" and value:
      """
      [{"target":"/reflect-body-typed","verb":"POST","authType":"Application & Application User","throttlingPolicy":"Unlimited","scopes":[],"operationPolicies":{"request":[{"policyName":"jsonToXML","policyVersion":"v1","parameters":{}}],"response":[],"fault":[{"policyName":"jsonFault","policyVersion":"v1","parameters":{}}]}}]
      """
    Then The response status code should be 200
    When I retrieve the "api-products" resource with id "respPolicyProductId"
    And I put the response payload in context as "respPolicyProductPayload"
    When I update "api-products" resource of id "respPolicyProductId" with payload "respPolicyProductPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "respPolicyRev2"
    """
    {"description":"response-policy product revision after the base API policy change"}
    """
    And I make a request to create a revision for "api-products" resource "respPolicyProductId" with payload "respPolicyRev2"
    Then The response status code should be 201
    When I deploy revision "revisionId" of "api-products" resource "respPolicyProductId"
    Then The response status code should be 201
    # A JSON request body is now converted to XML inbound by the newly-added request policy, and the backend's
    # echo of that XML reaches the client untouched — proof the member API's policy change took effect through
    # the already-deployed product.
    When I put the following JSON payload in context as "respPolicyJsonBody"
    """
    {"foo":"bar"}
    """
    And I invoke the API at gateway context "{{respPolicyProductContext}}/1.0.0/reflect-body-typed" with method "POST" using access token "generatedAccessToken" and payload "respPolicyJsonBody" with content type "application/json" until response body contains "<jsonObject>" within 120 seconds
    Then The response should contain "<jsonObject><foo>bar</foo></jsonObject>"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
