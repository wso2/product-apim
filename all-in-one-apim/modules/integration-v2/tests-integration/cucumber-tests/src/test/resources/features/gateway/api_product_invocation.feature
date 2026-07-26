@cleanup
Feature: Gateway API Product Invocation

  Ports the invocation + lifecycle-stage behaviour of the legacy APIProductCreationTestCase / APIProductLifecycleTest:
  an API Product, once deployed + published + subscribed, is invocable at the gateway through its own context
  (routing to the aggregated API's backend), and the gateway response tracks the product's lifecycle state
  (PUBLISHED → 200, BLOCKED → 503, DEPRECATED → 200, RETIRED → 404) — the product analogue of
  gateway/lifecycle_stage_invocation. Single-tenant (super) for the lifecycle arc; the create/invoke smoke runs
  ×2. Runs in the concurrent IntegrationV2-Gateway block (backend started). Teardown via @cleanup.

  @cap:gateway @feat:rest-invocation @type:smoke @dep:publisher @legacy:APIProductCreationTestCase
  Scenario Outline: Invoke a published API product through the gateway as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "prodApiId" and deployed it
    When I create an API product "${UNIQUE:InvokeProduct}" with context "${UNIQUE:invokeProductCtx}" from API "prodApiId" as "productId"
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
    # Subscribe an application and invoke the product's aggregated resource.
    When I have set up application with keys, subscribed to API "productId", and obtained access token for "prodSubId"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{productContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

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

    # BLOCKED → gateway refuses (503).
    When I change the lifecycle of "api-products" resource "lcProductId" with action "Block"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{lcProductContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 503 within 60 seconds
    Then The response status code should be 503

    # DEPRECATED → still invocable (200).
    When I change the lifecycle of "api-products" resource "lcProductId" with action "Deprecate"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{lcProductContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
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
    # Register the scope on the API and gate the GET /customers/{id} operation with it.
    When I retrieve the "apis" resource with id "scopedApiId"
    And I put the response payload in context as "scopedApiPayload"
    When I update the "apis" resource "scopedApiId" and "scopedApiPayload" with configuration type "scopes" and value:
      """
      [{"shared":true,"scope":{"name":"prodScopeEnf","displayName":"prodScopeEnf","description":"product scope enforcement","bindings":["admin"]}}]
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "scopedApiId"
    And I put the response payload in context as "scopedApiPayload"
    When I update the "apis" resource "scopedApiId" and "scopedApiPayload" with configuration type "operations" and value:
      """
      [{"target":"/customers/{id}","verb":"GET","authType":"Application & Application User","throttlingPolicy":"Unlimited","scopes":["prodScopeEnf"],"operationPolicies":{"request":[],"response":[],"fault":[]}},{"target":"/customers/{id}","verb":"DELETE","authType":"Application & Application User","throttlingPolicy":"Unlimited","scopes":[],"operationPolicies":{"request":[],"response":[],"fault":[]}}]
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
    When I request an OAuth access token for the current user using password grant with scope "prodScopeEnf"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{scopeProductContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I request an OAuth access token for the current user using password grant with scope "openid"
    Then The response status code should be 200
    And I invoke the API at gateway context "{{scopeProductContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # D1: a product that aggregates an API whose devportal visibility is RESTRICTED (visibleRoles) is still
  # invocable through the product (the source API's visibility restriction does not block product invocation).
  # Ports APIProductCreationTestCase#testCreateAndInvokeApiProductWithVisibilityRestrictedApi (whose
  # invocationStatusCodes is empty — i.e. all operations expected to return 200, no 403).
  @cap:gateway @feat:rest-invocation @rule:product @type:regression @dep:publisher @legacy:APIProductCreationTestCase
  Scenario Outline: A product aggregating a visibility-restricted API is invocable as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_restricted_visibility_api.json" as "restrictedApiId" and deployed it
    When I create an API product "${UNIQUE:RestrictedProduct}" with context "${UNIQUE:restrictedProductCtx}" from API "restrictedApiId" as "restrictedProductId"
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
    When I have set up application with keys, subscribed to API "restrictedProductId", and obtained access token for "restrictedSubId"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{restrictedProductContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # D4: a product that aggregates an advertise-only API is invocable through the product — the product provides
  # the gateway routing (to the advertised API's external endpoint, here the node backend), even though an
  # advertise-only API is not itself gateway-deployed. Ports
  # APIProductCreationTestCase#testCreateApiProductWithAdvertiseOnlyApi. The advertised API is only CREATED
  # (not deployed); only the product is deployed.
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
    When I have set up application with keys, subscribed to API "advertiseProductId", and obtained access token for "advertiseSubId"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{advertiseProductContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

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
