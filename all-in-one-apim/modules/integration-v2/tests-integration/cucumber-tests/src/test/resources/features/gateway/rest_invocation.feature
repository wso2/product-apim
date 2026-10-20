@cleanup
Feature: Gateway REST API Invocation

  Gateway-plane runtime invocation of a published REST API: subscribe an application, obtain an access
  token, and invoke the API through the gateway, checking both a normal 200 response and backend 204
  propagation. This is the gateway counterpart of the publisher-plane create/publish features (which assert
  only publisher outcomes). Runs in both the super tenant and tenant1.com as the tenant admin (the flow spans
  publish + subscribe + invoke). Teardown via the per-scenario cleanup hook.

  @cap:gateway @feat:rest-invocation @type:smoke @dep:publisher @legacy:APIMANAGERInvocationTestCase
  Scenario Outline: Invoke a published REST API through the gateway as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"

    # Capture the API's full gateway context (already carries /t/<tenant> for tenant APIs)
    When I retrieve the "apis" resource with id "createdApiId"
    And I extract response field "context" and store it as "apiContext"

    # Subscribe an application and obtain an access token
    When I have set up application with keys, subscribed to API "createdApiId", and obtained access token for "subscriptionId"
    Then The response status code should be 200

    # Invoke through the gateway by full context path (retry while the gateway becomes eventually consistent).
    # The BACKEND payload is pinned, not just the status: the API also exposes DELETE /customers/{id}, which the
    # backend answers 200 with an EMPTY body — so a status-only assertion cannot tell "GET reached
    # node-customer-service" from "the gateway dispatched to the wrong operation".
    When I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

  Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # The shared Node backend returns 204 with no body for process-instance DELETE and HEAD. Exercising HEAD here
  # reproduces GIT2231's gateway path: the original failure was triggered when the backend's HEAD response had no
  # content. Keep the existing DELETE assertion as well, so this extends rather than replaces the 204 coverage.
  @cap:gateway @feat:rest-invocation @type:regression @dep:publisher @legacy:APIMANAGER4464BackendReturningStatusCode204TestCase @legacy:GIT2231HeadRequestNPEErrorTestCase
  Scenario Outline: The gateway preserves a backend 204 response for DELETE and HEAD as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_204_api.json" as "api204Id" and deployed it
    When I publish the "apis" resource with id "api204Id"
    Then The lifecycle status of API "api204Id" should be "Published"
    When I retrieve the "apis" resource with id "api204Id"
    Then The response status code should be 200
    And I extract response field "context" and store it as "api204Context"

    # The standard key setup subscribes the app and waits for the API to become available through the gateway.
    When I have set up application with keys, subscribed to API "api204Id" with plan "Unlimited", and obtained access token for "api204SubscriptionId"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{api204Context}}/1.0.0/process-instances/123" with method "DELETE" using access token "generatedAccessToken" and payload "" until response status code becomes 204 within 60 seconds
    Then The response status code should be 204
    And The response body should be empty

    When I invoke the API at gateway context "{{api204Context}}/1.0.0/process-instances/123" with method "HEAD" using access token "generatedAccessToken" and payload "" until response status code becomes 204 within 60 seconds
    Then The response status code should be 204
    And The response body should be empty

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Non-admin ROLE-BASED scope enforcement at the gateway (ports the invoke facet of
  # APIImportExportTestCase#testNewAPIInvoke, whose scope-protected API was invoked by a non-admin store user).
  # Every other scope-enforcement scenario mints its token as the ADMIN, whose role owns the binding, so the token
  # ALWAYS carries the scope — that never exercises the role->scope grant path. Here two least-privilege
  # (Internal/subscriber) users are provisioned; only one also holds the role bound to the operation scope. The
  # resident KM grants a requested scope only to a user holding a bound role, so:
  #   * the role-HOLDER's password-grant token carries the scope -> the gateway allows the operation -> 200 + backend body;
  #   * the role-LACKER's token is issued (200) but WITHOUT the scope -> the gateway refuses the operation.
  # The refusal is 403 (a valid, subscribed token that merely lacks the required scope), asserted EXACTLY — matching
  # gateway/allowed_scopes and the IS7 role-enforcement negative; never widened to 401||403 (§12). The API is
  # authored/published/subscribed as the tenant admin; only the two invocations act as the provisioned non-admins.
  @cap:gateway @feat:security-enforcement @type:regression @dep:publisher @legacy:APIImportExportTestCase
  Scenario Outline: A non-admin holding the bound role can invoke a scope-protected API while one without it is refused as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I provision role "c14scoperole" in tenant "<tenant>"
    And I provision user "c14with" with roles "Internal/subscriber,c14scoperole" in tenant "<tenant>"
    And I provision user "c14without" with roles "Internal/subscriber" in tenant "<tenant>"

    # Author the scope-protected API (GET /customers/{id} requires the scope bound to c14scoperole), deploy + publish.
    When I put JSON payload from file "artifacts/payloads/create_apim_scope_protected_role_api.json" in context as "spCreatePayload"
    And I create an "apis" resource with payload "spCreatePayload" as "spApiId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "spRevPayload"
    """
    {"description":"scope-protected revision"}
    """
    And I make a request to create a revision for "apis" resource "spApiId" with payload "spRevPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "spDeployPayload"
    """
    [{"name":"{{gatewayEnvironment}}","vhost":"localhost","displayOnDevportal":true}]
    """
    And I make a request to deploy revision "revisionId" of "apis" resource "spApiId" with payload "spDeployPayload"
    Then The response status code should be 201
    When I publish the "apis" resource with id "spApiId"
    Then The lifecycle status of API "spApiId" should be "Published"
    When I retrieve the "apis" resource with id "spApiId"
    And I extract response field "context" and store it as "spContext"

    # Subscribe an application keyed for the password grant so scope-specific tokens can be minted for each user.
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "spAppPayload"
    And I create an application with payload "spAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "spKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "spKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "spSubPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "spApiId" using application "createdAppId" with payload "spSubPayload" as "spSubId"
    Then The response status code should be 201

    # Role HOLDER: token carries the scope -> gateway allows -> 200 carrying the backend body (a bare 200 would not
    # prove the call reached node-customer-service). Both users are minted as the actors provisioned above, so the
    # password grant uses each user's physical (email-mode-aware) username.
    When I request an OAuth access token using password grant as "<withUser>" with scope "c14scope"
    Then The response status code should be 200
    And I invoke the API at gateway context "{{spContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""

    # Role LACKER: token issued (200) but WITHOUT the scope -> gateway refuses the operation with 403 (exact).
    When I request an OAuth access token using password grant as "<withoutUser>" with scope "c14scope"
    Then The response status code should be 200
    And I invoke the API at gateway context "{{spContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403

    Examples:
      | actor             | tenant       | withUser            | withoutUser            |
      | admin             | carbon.super | c14with             | c14without             |
      | admin@tenant1.com | tenant1.com  | c14with@tenant1.com | c14without@tenant1.com |

  # Complete the API-local scope × principal-role × requested/omitted-scope × HTTP-method matrix from
  # APIScopeTestCase. The GET scope belongs to Internal/subscriber; the POST scope belongs only to admin. Each
  # principal requests both scopes or neither, and both methods are invoked once after explicit gateway readiness.
  # This complements (and does not replace) the separate overlapping-path method-scope scenarios in
  # gateway/rest_request_dispatch.feature.
  @cap:gateway @feat:security-enforcement @rule:local-scope-role-matrix @type:regression @dep:publisher @legacy:APIScopeTestCase
  Scenario Outline: API-local scopes enforce the role and method matrix as <principal> under <ownerActor>
    Given The system is ready
    And I have valid access tokens as "<ownerActor>"
    And I have created an api from "artifacts/payloads/create_apim_local_scope_role_matrix_api.json" as "lsrmApiId" and deployed it
    And the "apis" resource "lsrmApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "lsrmApiId"
    Then The lifecycle status of API "lsrmApiId" should be "Published"
    When I retrieve the "apis" resource with id "lsrmApiId"
    Then The response status code should be 200
    And The response array field "operations" should have exactly 2 entries
    And The response field "scopes[?(@.scope.name=='local_get_scope')].scope.bindings[*]" should be exactly the list "Internal/subscriber"
    And The response field "scopes[?(@.scope.name=='local_post_scope')].scope.bindings[*]" should be exactly the list "admin"
    And The response field "operations[?(@.verb=='GET')].scopes[*]" should be exactly the list "local_get_scope"
    And The response field "operations[?(@.verb=='POST')].scopes[*]" should be exactly the list "local_post_scope"
    And I extract response field "context" and store it as "lsrmContext"

    And I use DevPortal consumer "<applicationActor>", preparing credentials if needed
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "lsrmAppPayload"
    And I create an application with payload "lsrmAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "lsrmKeysPayload"
    """
    {"keyType":"PRODUCTION","grantTypesToBeSupported":["client_credentials","password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "lsrmKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "lsrmSubscriptionPayload"
    """
    {"applicationId":"{{applicationId}}","apiId":"{{apiId}}","throttlingPolicy":"Unlimited"}
    """
    And I subscribe to API "lsrmApiId" using application "createdAppId" with payload "lsrmSubscriptionPayload" as "lsrmSubscriptionId"
    Then The response status code should be 201

    When I request an OAuth access token using password grant as "<principal>" with scope "<requestedScopes>"
    Then The response status code should be 200
    And The issued token scope list should include "<includedScopes>" and exclude "<excludedScopes>"
    And I copy context value "generatedAccessToken" to "lsrmAccessToken"
    When I invoke the API at gateway context "{{lsrmContext}}/1.0.0/comp/cartes/op/123" once with method "GET" using access token "lsrmAccessToken" and payload ""
    Then The gateway invocation should return HTTP <getStatus> and, when successful, identify method "GET" and path "/jaxrs_basic/services/customers/customerservice/verb-echo/comp/cartes/op/123"
    When I invoke the API at gateway context "{{lsrmContext}}/1.0.0/comp/cartes/op/123" once with method "POST" using access token "lsrmAccessToken" and payload ""
    Then The gateway invocation should return HTTP <postStatus> and, when successful, identify method "POST" and path "/jaxrs_basic/services/customers/customerservice/verb-echo/comp/cartes/op/123"

    # In addition to exercising SECONDARY.COM subscribers directly, these rows make a secondary-store publisher
    # the API owner so the same scope grant/denial assertions run across a store boundary. The same runner also
    # executes this matrix in email-user mode.
    Examples:
      | ownerActor                                  | applicationActor                             | principal                                     | requestedScopes                  | includedScopes                  | excludedScopes                   | getStatus | postStatus |
      | admin                                       | admin                                        | admin                                         | local_get_scope local_post_scope | local_get_scope,local_post_scope |                                  | 200       | 200        |
      | admin                                       | admin                                        | admin                                         |                                 | default                          | local_get_scope,local_post_scope | 403       | 403        |
      | admin                                       | admin                                        | subscriberUser                                | local_get_scope local_post_scope | local_get_scope                  | local_post_scope                 | 200       | 403        |
      | admin                                       | admin                                        | subscriberUser                                |                                 | default                          | local_get_scope,local_post_scope | 403       | 403        |
      | admin                                       | admin                                        | SECONDARY.COM/subscriberUser1                 | local_get_scope local_post_scope | local_get_scope                  | local_post_scope                 | 200       | 403        |
      | admin                                       | admin                                        | SECONDARY.COM/subscriberUser1                 |                                 | default                          | local_get_scope,local_post_scope | 403       | 403        |
      | admin@tenant1.com                           | admin@tenant1.com                            | admin@tenant1.com                             | local_get_scope local_post_scope | local_get_scope,local_post_scope |                                  | 200       | 200        |
      | admin@tenant1.com                           | admin@tenant1.com                            | admin@tenant1.com                             |                                 | default                          | local_get_scope,local_post_scope | 403       | 403        |
      | admin@tenant1.com                           | admin@tenant1.com                            | subscriberUser@tenant1.com                    | local_get_scope local_post_scope | local_get_scope                  | local_post_scope                 | 200       | 403        |
      | admin@tenant1.com                           | admin@tenant1.com                            | subscriberUser@tenant1.com                    |                                 | default                          | local_get_scope,local_post_scope | 403       | 403        |
      | admin@tenant1.com                           | admin@tenant1.com                            | SECONDARY.COM/subscriberUser1@tenant1.com      | local_get_scope local_post_scope | local_get_scope                  | local_post_scope                 | 200       | 403        |
      | admin@tenant1.com                           | admin@tenant1.com                            | SECONDARY.COM/subscriberUser1@tenant1.com      |                                 | default                          | local_get_scope,local_post_scope | 403       | 403        |
      | SECONDARY.COM/publisherUser1                | SECONDARY.COM/subscriberUser1                | SECONDARY.COM/subscriberUser1                 | local_get_scope local_post_scope | local_get_scope                  | local_post_scope                 | 200       | 403        |
      | SECONDARY.COM/publisherUser1                | SECONDARY.COM/subscriberUser1                | SECONDARY.COM/subscriberUser1                 |                                 | default                          | local_get_scope,local_post_scope | 403       | 403        |
      | SECONDARY.COM/publisherUser1@tenant1.com    | SECONDARY.COM/subscriberUser1@tenant1.com    | SECONDARY.COM/subscriberUser1@tenant1.com     | local_get_scope local_post_scope | local_get_scope                  | local_post_scope                 | 200       | 403        |
      | SECONDARY.COM/publisherUser1@tenant1.com    | SECONDARY.COM/subscriberUser1@tenant1.com    | SECONDARY.COM/subscriberUser1@tenant1.com     |                                 | default                          | local_get_scope,local_post_scope | 403       | 403        |

  # Re-imported API is functionally invocable end-to-end: create an API, subscribe and INVOKE it (200) through
  # the gateway BEFORE export; then export -> delete -> re-import; deploy + publish the re-imported API,
  # re-subscribe, and INVOKE AGAIN (200) — proving the re-imported API is still routable/invocable through the
  # gateway, not merely recreated as a publisher artifact. Runs in the gateway block (backend up) as admin
  # (import needs apim:api_import_export). Ports APIImportExportTestCase#testNewAPIInvoke +
  # testNewAPIInvokeAfterImport.
 @cap:gateway @feat:rest-invocation @rule:import-export @type:regression @dep:publisher @legacy:APIImportExportTestCase
  Scenario Outline: A re-imported API is invocable through the gateway before and after import as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "ieInvApiId" and deployed it
    When I publish the "apis" resource with id "ieInvApiId"
    Then The lifecycle status of API "ieInvApiId" should be "Published"
    When I retrieve the "apis" resource with id "ieInvApiId"
    And I extract response field "name" and store it as "ieInvApiName"
    And I extract response field "context" and store it as "ieInvContext"

    # INVOKE BEFORE export → 200 carrying the BACKEND payload (the original API is functionally invocable, i.e.
    # it actually reaches node-customer-service — a bare 200 would not distinguish that from a wrong dispatch).
    When I have set up application with keys, subscribed to API "ieInvApiId", and obtained access token for "ieInvSubId"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{ieInvContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    # Export → delete → re-import the archive. The subscription must be removed first — an API with an active
    # subscription cannot be deleted (409 "active subscriptions exist").
    When I export the API "ieInvApiId" to an archive as "ieInvArchive"
    When I delete the subscription with id "ieInvSubId"
    Then The response status code should be 200
    When I delete the "apis" resource with id "ieInvApiId"
    Then The response status code should be 200
    When I import the exported archive "ieInvArchive" with additional properties "{}" as "ieInvImportResult"
    Then The response status code should be 200
    When I find the Publisher API named "{{ieInvApiName}}" and store its id as "ieInvImportedApiId"
    Then The response status code should be 200

    # Import preserves the Published lifecycle state. Deploy the RE-IMPORTED API, verify that state, re-subscribe,
    # and INVOKE AGAIN → 200 (still invocable after import). Do not send a second Publish action: Published APIs
    # reject that lifecycle transition with 903234.
    When I deploy the API with id "ieInvImportedApiId"
    Then The response status code should be 201
    Then The lifecycle status of API "ieInvImportedApiId" should be "Published"
    When I retrieve the "apis" resource with id "ieInvImportedApiId"
    And I extract response field "context" and store it as "ieInvImportedContext"
    When I have set up application with keys, subscribed to API "ieInvImportedApiId", and obtained access token for "ieInvImportedSubId"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{ieInvImportedContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports APIResourceWithTemplate #3 — an encoded URI path segment must be routed to the backend. FAITHFUL to
  # legacy: uri-template resource /{val} + a {uri.var.val} TEMPLATED ENDPOINT (…/echo/sub{uri.var.val}). The
  # gateway substitutes the path var into the endpoint AND appends the postfix → doubled backend path
  # /echo/sub<val>/<val> (exactly what legacy's bespoke Synapse backend hardcoded). The encoded segment must be
  # sent RAW (%28/%29 preserved) — the default invoke's HTTP client would decode it, which a uri-template
  # resource then 404s; the raw invoke sends it verbatim so the gateway routes it (as legacy's client did).
  @cap:gateway @feat:rest-invocation @type:regression @dep:publisher @legacy:APIResourceWithTemplateTestCase @legacy:UriTemplateReservedCharacterEncodingTest
  Scenario Outline: An encoded URI path segment is routed through a templated endpoint to the backend as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_uritemplate_api.json" as "uriApiId" and deployed it
    When I publish the "apis" resource with id "uriApiId"
    Then The lifecycle status of API "uriApiId" should be "Published"
    When I retrieve the "apis" resource with id "uriApiId"
    And I extract response field "context" and store it as "uriApiContext"
    When I have set up application with keys, subscribed to API "uriApiId", and obtained access token for "uriSubscriptionId"
    Then The response status code should be 200
    # Plain segment: routes through the {uri.var.val} endpoint; backend gets the doubled path /echo/subplainseg/plainseg.
    When I invoke the API at gateway context "{{uriApiContext}}/1.0.0/plainseg" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "echo/subplainseg/plainseg"
    # Encoded segment: sent RAW so %28/%29 reach the gateway verbatim; it routes (legacy uri-template + uri-var
    # endpoint) and the backend receives the encoding intact.
    When I invoke the API at raw gateway context "{{uriApiContext}}/1.0.0/S2222-0496%2815%2927436-0" using access token "generatedAccessToken" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "S2222-0496%2815%2927436-0"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # L5: an API whose context uses a version-FIRST template ({version}/<ctx>) deploys to /{version}/<ctx> and is
  # invocable there — the version sits first in the URL, not appended. Ports PluggableVersioningStrategyTestCase
  # (the API is created with context "{version}/api"). The deployed context already carries the version, so it
  # is invoked directly (no appended /1.0.0) — that's what proves the version-first routing.
  @cap:gateway @feat:rest-invocation @rule:version-first @type:regression @dep:publisher @legacy:PluggableVersioningStrategyTestCase
  Scenario Outline: An API with a version-first context template routes at /{version}/context as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_version_first_api.json" as "vfApiId" and deployed it
    And the "apis" resource "vfApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "vfApiId"
    Then The lifecycle status of API "vfApiId" should be "Published"
    When I retrieve the "apis" resource with id "vfApiId"
    And I extract response field "context" and store it as "vfContext"
    # The publisher returns the context template verbatim (/{version}/ctx); the gateway resolves {version} at
    # deploy, so substitute it for the invocation URL.
    And I replace "{version}" with "1.0.0" in context "vfContext"
    When I have set up application with keys, subscribed to API "vfApiId", and obtained access token for "subscriptionId"
    Then The response status code should be 200
    # Keep the existing JSON body assertions, then exercise the legacy XML Accept negotiation contract without
    # replacing or weakening the original version-first routing assertion.
    When I invoke the API at gateway context "{{vfContext}}/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    When I invoke the API at gateway context "{{vfContext}}/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" with request header "Accept" set to "text/xml" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "<id>123</id><name>John</name></Customer>"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports the invocation half of DynamicAPIContextTestCase — an API whose context carries {version} in the MIDDLE
  # of the path (api/developer/{version}) deploys with the version resolved into that position and is invocable at
  # api/developer/1.0.0/... . Distinct from the version-FIRST case above (where {version} leads); here the template
  # variable sits mid-path. The publisher returns the template verbatim; the gateway resolves {version} at deploy,
  # so it is substituted for the invocation URL. The publisher search facet is covered in api_config.feature; this
  # scenario verifies both the current request shape and the legacy XML-negotiated shape against the same API.
  @cap:gateway @feat:rest-invocation @rule:dynamic-context @type:regression @dep:publisher @legacy:DynamicAPIContextTestCase
  Scenario Outline: An API with a mid-path {version} context template is invocable at the resolved context as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_dynamic_context_api.json" as "dcApiId" and deployed it
    When I publish the "apis" resource with id "dcApiId"
    Then The lifecycle status of API "dcApiId" should be "Published"
    When I retrieve the "apis" resource with id "dcApiId"
    And I extract response field "context" and store it as "dcContext"
    And I replace "{version}" with "1.0.0" in context "dcContext"
    When I have set up application with keys, subscribed to API "dcApiId", and obtained access token for "dcSub"
    Then The response status code should be 200
    # Both requests must reach the backend and return the expected customer body.
    When I invoke the API at gateway context "{{dcContext}}" with access token "generatedAccessToken" using these request variants:
      | pathSuffix       | accept   | responseFormat | id  | name |
      | /customers/123/  |          | json           | 123 | John |
      | /customers/123   | text/xml | xml            | 123 | John |

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Wave B-1: a REST resource ADDED to a deployed API becomes invocable at the gateway after redeploy; an
  # undefined path is refused. Ports AddEditRemoveRESTResourceTestCase.
  @cap:gateway @feat:rest-invocation @rule:dynamic-resource @type:regression @dep:publisher @legacy:AddEditRemoveRESTResourceTestCase
  Scenario Outline: A REST resource added to a deployed API becomes invocable at the gateway as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "resApiId" and deployed it
    And the "apis" resource "resApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "resApiId"
    Then The lifecycle status of API "resApiId" should be "Published"
    When I retrieve the "apis" resource with id "resApiId"
    And I extract response field "context" and store it as "resContext"
    When I have set up application with keys, subscribed to API "resApiId", and obtained access token for "resSubId"
    Then The response status code should be 200
    # Baseline: the existing GET resource is invocable AND serves the backend payload. The API's other operation
    # (DELETE /customers/{id}) answers 200 with an EMPTY body, so the body is what separates "GET matched" from
    # "some other operation on the same template matched".
    When I invoke the API at gateway context "{{resContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    # Legacy AddEditRemoveRESTResourceTestCase explicitly requested XML and checked the XML response before
    # changing the API. Keep that representation contract in addition to the JSON field assertions above.
    When I invoke the API at gateway context "{{resContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" with request header "Accept" set to "text/xml" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "<id>123</id><name>John</name></Customer>"
    # POST to an as-yet-undefined resource is refused (405 — path matches GET /customers/{id}, POST not allowed).
    # Keep the trailing slash used by the legacy invocation; the API context is resolved from this scenario's own API.
    # Body pinned too: the gateway distinguishes "URI matched, verb did not" from "no URI matched" with two
    # different messages, so status alone cannot tell the two dispatch outcomes apart (CORSRequestHandler
    # #handleResourceNotFound → METHOD_NOT_FOUND_ERROR_MSG here, RESOURCE_NOT_FOUND_ERROR_MSG below).
    When I invoke the API at gateway context "{{resContext}}/1.0.0/customers/name/" with method "POST" using access token "generatedAccessToken" and payload "" until response status code becomes 405 within 60 seconds
    Then The response status code should be 405
    And The response should contain "Method not allowed for given API resource"
    # Add a POST /customers/name operation and redeploy.
    When I retrieve the "apis" resource with id "resApiId"
    And I put the response payload in context as "resApiPayload"
    When I update the "apis" resource "resApiId" and "resApiPayload" with configuration type "operations" and value:
      """
      [{"target":"/customers/{id}","verb":"GET"},{"target":"/customers/{id}","verb":"DELETE"},{"target":"/customers/name/","verb":"POST"}]
      """
    Then The response status code should be 200
    And The value of response field "id" should be "{{resApiId}}"
    When I deploy the API with id "resApiId"
    Then The response status code should be 201
    And I wait until "apis" "resApiId" revision is deployed in the gateway
    # The original GET must remain available after the resource update and deployment.
    When I invoke the API at gateway context "{{resContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    When I invoke the API at gateway context "{{resContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" with request header "Accept" set to "text/xml" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "<id>123</id><name>John</name></Customer>"
    # The newly added POST resource is now invocable and routes to the backend.
    When I invoke the API at gateway context "{{resContext}}/1.0.0/customers/name/" with method "POST" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "Tom"
    # An undefined path is refused by the gateway — status AND the gateway's own message (legacy's
    # INVALID_RESOURCE_INVOCATION constant, which is APIMgtGatewayConstants.RESOURCE_NOT_FOUND_ERROR_MSG). A bare
    # 404 would also be produced by the BACKEND for an unmapped upstream path, so the message is what proves the
    # refusal came from the gateway's resource dispatcher rather than from node-customer-service.
    When I invoke the API at gateway context "{{resContext}}/1.0.0/customers/123/invalid" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 404 within 60 seconds
    Then The response status code should be 404
    And The response should contain "No matching resource found for given API Request"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # The legacy AddEditRemoveRESTResourceTestCase uses the version-first {version}/api route over the HTTP
  # gateway and sends text/plain requests. Keep that specific wire/path contract alongside the context-first
  # resource-mutation scenario above; a similar GET or POST on a different route is not equivalent parity.
  @cap:gateway @feat:rest-invocation @rule:version-first-resource-mutation @type:regression @dep:publisher @legacy:AddEditRemoveRESTResourceTestCase
  Scenario Outline: Version-first API resource mutation preserves the legacy HTTP contract as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_version_first_api.json" as "aerApiId" and deployed it
    And the "apis" resource "aerApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "aerApiId"
    Then The lifecycle status of API "aerApiId" should be "Published"
    When I retrieve the "apis" resource with id "aerApiId"
    And I extract response field "context" and store it as "aerContext"
    And I replace "{version}" with "1.0.0" in context "aerContext"
    And I have set up application with keys, subscribed to API "aerApiId", and obtained access token for "aerSubscriptionId"
    Then The response status code should be 200

    # Baseline GET: exact HTTP listener, version-first path, Accept header, and XML body contract from legacy.
    When I invoke the API at HTTP gateway context "{{aerContext}}/customers/123" with method "GET" using access token "generatedAccessToken" and payload "" with content type "application/json" and request header "Accept" set to "text/xml" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "<id>123</id><name>John</name></Customer>"

    # Before the operation is added, POST must be rejected as a method/path mismatch, not served by the backend.
    When I invoke the API at HTTP gateway context "{{aerContext}}/customers/name/" with method "POST" using access token "generatedAccessToken" and payload "id=25" with content type "text/plain" and request header "Accept" set to "text/plain" until response status code becomes 405 within 60 seconds
    Then The response status code should be 405
    And The response should contain "Method not allowed for given API resource"

    When I retrieve the "apis" resource with id "aerApiId"
    And I put the response payload in context as "aerApiPayload"
    And I update the "apis" resource "aerApiId" and "aerApiPayload" with configuration type "operations" and value:
      """
      [{"target":"/customers/{id}","verb":"GET"},{"target":"/customers/name/","verb":"POST"}]
      """
    Then The response status code should be 200
    And The value of response field "id" should be "{{aerApiId}}"
    When I deploy the API with id "aerApiId"
    Then The response status code should be 201
    And I wait until "apis" "aerApiId" revision is deployed in the gateway

    When I invoke the API at HTTP gateway context "{{aerContext}}/customers/123" with method "GET" using access token "generatedAccessToken" and payload "" with content type "application/json" and request header "Accept" set to "text/xml" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "<id>123</id><name>John</name></Customer>"
    When I invoke the API at HTTP gateway context "{{aerContext}}/customers/name/" with method "POST" using access token "generatedAccessToken" and payload "id=25" with content type "text/plain" and request header "Accept" set to "text/plain" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "Tom"
    When I invoke the API at HTTP gateway context "{{aerContext}}/customers/123/invalid" with method "GET" using access token "generatedAccessToken" and payload "" with content type "application/json" and request header "Accept" set to "text/xml" until response status code becomes 404 within 60 seconds
    Then The response status code should be 404
    And The response should contain "No matching resource found for given API Request"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # HTTP PATCH and HEAD method routing through the gateway. PATCH forwards to the backend echo route → 200. HEAD
  # on a GET-backed resource routes cleanly (no body, no historical NPE). Ports HttpPATCHSupportTestCase and
  # GIT2231HeadRequestNPEErrorTestCase. HEAD status is asserted at whatever the backend returns (verify-first).
  @cap:gateway @feat:rest-invocation @type:regression @dep:publisher @legacy:HttpPATCHSupportTestCase @legacy:GIT2231HeadRequestNPEErrorTestCase
  Scenario Outline: PATCH and HEAD requests are routed through the gateway as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_patchhead_api.json" as "phApiId" and deployed it
    When I publish the "apis" resource with id "phApiId"
    Then The lifecycle status of API "phApiId" should be "Published"
    When I retrieve the "apis" resource with id "phApiId"
    And I extract response field "context" and store it as "phContext"
    When I have set up application with keys, subscribed to API "phApiId", and obtained access token for "phSubId"
    Then The response status code should be 200

    # PATCH is forwarded to the backend echo route → 200 whose body is the PATCH body echoed back. A marker body
    # is sent (rather than the empty one a status-only check allowed) because that is what makes the echo
    # observable: the API's other operations are GET/HEAD /hello, which answer 200 with "Hello World", so an empty
    # PATCH could not distinguish "the PATCH operation matched and its body was forwarded" from a wrong dispatch.
    When I put the following JSON payload in context as "phPatchBody"
    """
    {"patched":"by-gateway"}
    """
    When I invoke the API at gateway context "{{phContext}}/1.0.0/reflect-body" with method "PATCH" using access token "generatedAccessToken" and payload "phPatchBody" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "{\"patched\":\"by-gateway\"}"

    # HEAD on a GET-backed resource routes without the historical NPE. No body assertion is possible or wanted
    # here — a HEAD response carries no entity body by definition, so the status IS the whole observable.
    When I invoke the API at gateway context "{{phContext}}/1.0.0/hello" with method "HEAD" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Legacy HttpPATCHSupportTestCase maps PATCH to the wildcard/root path "/*" and invokes the API without a
  # resource suffix. Keep this separate from the named /reflect-body PATCH above: it verifies wildcard dispatch at
  # the API root, then confirms the request body reaches the backend echo route.
  @cap:gateway @feat:rest-invocation @type:regression @dep:publisher @legacy:HttpPATCHSupportTestCase
  Scenario Outline: A PATCH request to an API root wildcard is routed to the backend as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_wildcard_patch_api.json" as "wpApiId" and deployed it
    When I publish the "apis" resource with id "wpApiId"
    Then The lifecycle status of API "wpApiId" should be "Published"
    When I retrieve the "apis" resource with id "wpApiId"
    Then The response status code should be 200
    And The response array field "operations" should have exactly 1 entries
    And The response field "operations[?(@.verb=='PATCH')].target" should be exactly the list "/*"
    And I extract response field "context" and store it as "wpContext"
    When I have set up application with keys, subscribed to API "wpApiId", and obtained access token for "wpSubId"
    Then The response status code should be 200

    When I put the following JSON payload in context as "wpPatchBody"
    """
    {"first":"Greg"}
    """
    When I invoke the API at gateway context "{{wpContext}}/1.0.0" with method "PATCH" using access token "generatedAccessToken" and payload "wpPatchBody" with request header "Accept" set to "application/json" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "{\"first\":\"Greg\"}"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # The legacy YouTube sample sends GET /youtube/1.0.0/most_popular to an external Atom feed and checks the feed's
  # XML markers. Use a deterministic local feed fixture at the same upstream path so this covers the gateway's
  # feed request and XML response without depending on the retired public YouTube Data API.
  @cap:gateway @feat:rest-invocation @type:regression @dep:publisher @legacy:YouTubeAPITestCase
  Scenario Outline: A published YouTube-feed API returns its Atom XML markers as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_youtube_feed_api.json" as "ytApiId" and deployed it
    When I publish the "apis" resource with id "ytApiId"
    Then The lifecycle status of API "ytApiId" should be "Published"
    When I retrieve the "apis" resource with id "ytApiId"
    Then The response status code should be 200
    And The response array field "operations" should have exactly 1 entries
    And The response field "operations[?(@.verb=='GET')].target" should be exactly the list "/*"
    And I extract response field "context" and store it as "ytContext"
    When I have set up application with keys, subscribed to API "ytApiId", and obtained access token for "ytSubId"
    Then The response status code should be 200

    When I invoke the API at gateway context "{{ytContext}}/1.0.0/most_popular" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "<feed"
    And The response should contain "<category"
    And The response should contain "<entry>"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
