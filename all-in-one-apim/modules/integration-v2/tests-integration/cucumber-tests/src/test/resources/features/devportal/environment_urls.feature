@cleanup
Feature: DevPortal Environment URLs

  How the DevPortal view of an API reflects the gateway environments the API is actually deployed to — the
  endpoint URLs listed for the API, and the servers URL stamped into its OpenAPI definition (the URL the try-out
  console calls). Creating the gateway environment and publishing the API are admin/publisher prerequisites
  (@dep), not the subject: every assertion here is on what the DevPortal returns. Store-side visibility is in
  visibility.feature and search/browse in search.feature — neither is exercised here. ×2 tenant.

  # An API deployed to a custom gateway environment reflects that environment's vhost in its Developer Portal
  # endpoint URLs. NEW verified coverage (legacy-disabled testValidateDevportalAPIAndSwaggerResponse).
  @cap:devportal @feat:discovery @rule:environment @type:regression @dep:publisher @dep:admin @legacy:EnvironmentTestCase
  Scenario Outline: A devportal API reflects the custom environment vhost it is deployed to as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "e5ApiPayload"
    And I create an "apis" resource with payload "e5ApiPayload" as "e5ApiId"
    Then The response status code should be 201
    When I create a gateway environment "${UNIQUE:e5Env}" with vhost hosts "e5.gw.example.com"
    Then The response status code should be 201
    And I extract response field "name" and store it as "e5EnvName"
    When I put the following JSON payload in context as "e5RevPayload"
    """
    {"description":"revision for devportal env check"}
    """
    And I make a request to create a revision for "apis" resource "e5ApiId" with payload "e5RevPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "e5DeployPayload"
    """
    [{"name":"{{e5EnvName}}","vhost":"e5.gw.example.com","displayOnDevportal":true}]
    """
    And I make a request to deploy revision "revisionId" of "apis" resource "e5ApiId" with payload "e5DeployPayload"
    Then The response status code should be 201
    When I publish the "apis" resource with id "e5ApiId"
    Then The lifecycle status of API "e5ApiId" should be "Published"
    # The devportal view of the API carries the custom environment's vhost among its endpoint URLs.
    When I retrieve the devportal API "e5ApiId" until it contains "e5.gw.example.com" within 60 seconds
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # DevPortal Swagger `servers` URL resolution with MULTIPLE gateway environments in the tenant. The DevPortal
  # fetches GET /apis/{id}/swagger with NO environmentName, so the server resolves the environment itself — and it
  # must consider only environments the API is actually deployed to. Ports the three swagger-server-URL tests of
  # EnvironmentTestCase, which walk ONE API through the same three deployment states, so they stay one scenario
  # here too: deployed to the second gateway only, moved to Default, then deployed to both.
  @cap:devportal @feat:discovery @rule:environment @type:regression @dep:publisher @dep:admin @legacy:EnvironmentTestCase
  Scenario Outline: The devportal swagger server URL resolves to a gateway the API is deployed to as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    # A second gateway environment alongside the built-in Default (whose vhost is localhost).
    When I create a gateway environment "${UNIQUE:swEnv}" with vhost hosts "swagger.gw.example.com"
    Then The response status code should be 201
    And I extract response field "name" and store it as "swEnvName"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "swApiPayload"
    And I create an "apis" resource with payload "swApiPayload" as "swApiId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "swRevPayload"
    """
    {"description":"revision for devportal swagger server URL checks"}
    """
    And I make a request to create a revision for "apis" resource "swApiId" with payload "swRevPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "swSecondGatewayPayload"
    """
    [{"name":"{{swEnvName}}","vhost":"swagger.gw.example.com","displayOnDevportal":true}]
    """
    # (a) Deployed ONLY to the second gateway → the resolved server host is that gateway's vhost.
    And I make a request to deploy revision "revisionId" of "apis" resource "swApiId" with payload "swSecondGatewayPayload"
    Then The response status code should be 201
    When I publish the "apis" resource with id "swApiId"
    Then The lifecycle status of API "swApiId" should be "Published"
    And the devportal swagger of API "swApiId" should resolve its server host to "swagger.gw.example.com" within 60 seconds
    # (b) Undeployed from the second gateway and deployed to Default → the resolved host follows to Default's vhost.
    When I undeploy revision "revisionId" of "apis" resource "swApiId" with payload "swSecondGatewayPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "swDefaultGatewayPayload"
    """
    [{"name":"{{gatewayEnvironment}}","vhost":"localhost","displayOnDevportal":true}]
    """
    And I make a request to deploy revision "revisionId" of "apis" resource "swApiId" with payload "swDefaultGatewayPayload"
    Then The response status code should be 201
    And the devportal swagger of API "swApiId" should resolve its server host to "localhost" within 60 seconds
    # (c) Deployed to BOTH gateways → the host must be a vhost of one of them, never the regression's empty host.
    When I make a request to deploy revision "revisionId" of "apis" resource "swApiId" with payload "swSecondGatewayPayload"
    Then The response status code should be 201
    And the devportal swagger of API "swApiId" should resolve its server host to one of "localhost,swagger.gw.example.com" within 60 seconds
    # Undeploy from the custom environment so it can be deleted
    When I undeploy revision "revisionId" of "apis" resource "swApiId" with payload "swSecondGatewayPayload"
    Then The response status code should be 201
    When I delete the gateway environment with id "environmentId"
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # The devportal endpointURLs advertise a versionless URL surface (defaultVersionURLs) ONLY once the API is the
  # default version. Ports DefaultVersionAPITestCase#testDefaultVersionAPI's store-side assertions, which are
  # exercised NOWHERE else. Two things are pinned by contrast that either alone would understate:
  #   - BEFORE vs AFTER: before the API is default, the defaultVersionURLs object is PRESENT but every member is
  #     null (observed — the object is NOT absent, correcting legacy's "null before" wording); after, http/https
  #     populate while ws/wss stay null for a REST API. Asserting all four at both states catches a regression that
  #     either stopped populating http/https or started emitting ws/wss.
  #   - defaultVersionURLs vs URLs: defaultVersionURLs.http carries NO /1.0.0 version suffix, whereas the sibling
  #     URLs.http (same payload) does. That missing version segment IS the versionless semantic the field exists
  #     for, so both are pinned here to make the contrast explicit. ×2 tenant.
  @cap:devportal @feat:discovery @rule:default-version @type:regression @dep:publisher @legacy:DefaultVersionAPITestCase
  Scenario Outline: The devportal endpointURLs advertise defaultVersionURLs only once the API is the default version as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"

    # A versioned API created WITHOUT a default version, deployed and published. The context is a controlled unique
    # base so the versionless defaultVersionURLs can be asserted exactly: a non-default API's stored context keeps
    # its /1.0.0 suffix (verified: it stays /<base>/1.0.0 even after the API is later made default), so the versionless
    # URL cannot be derived from the extracted context — it is <base> without the version.
    When I generate a unique alphanumeric value and store it as "ndvCtxBase"
    And I put JSON payload from file "artifacts/payloads/create_non_default_version_api.json" in context as "ndvPayload"
    And I set the field "context" to "{{ndvCtxBase}}" in the payload "ndvPayload"
    When I create an "apis" resource with payload "ndvPayload" as "ndvApiId"
    Then The response status code should be 201
    When I deploy the API with id "ndvApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "ndvApiId"
    Then The lifecycle status of API "ndvApiId" should be "Published"

    # BEFORE it is default: the defaultVersionURLs object is present but every member is null.
    When I retrieve the devportal API "ndvApiId"
    Then The response status code should be 200
    And The response field "endpointURLs[0].defaultVersionURLs.http" should be null
    And The response field "endpointURLs[0].defaultVersionURLs.https" should be null
    And The response field "endpointURLs[0].defaultVersionURLs.ws" should be null
    And The response field "endpointURLs[0].defaultVersionURLs.wss" should be null

    # Make it the default version and redeploy so the store reflects the change.
    When I retrieve the "apis" resource with id "ndvApiId"
    And I put the response payload in context as "ndvUpdatePayload"
    When I update the "apis" resource "ndvApiId" and "ndvUpdatePayload" with configuration type "isDefaultVersion" and value:
    """
    true
    """
    Then The response status code should be 200
    When I deploy the API with id "ndvApiId"
    Then The response status code should be 201
    When I wait until "apis" "ndvApiId" revision is deployed in the gateway

    # AFTER: http/https carry the VERSIONLESS URL — note it is the sibling URLs.http (asserted alongside) MINUS the
    # /1.0.0 version segment; that missing segment IS the whole point of the field. ws/wss stay null for a REST API.
    When I retrieve the devportal API "ndvApiId"
    Then The response status code should be 200
    And The value of response field "endpointURLs[0].URLs.http" should be "http://localhost:8280<tenantPrefix>/{{ndvCtxBase}}/1.0.0"
    And The value of response field "endpointURLs[0].defaultVersionURLs.http" should be "http://localhost:8280<tenantPrefix>/{{ndvCtxBase}}"
    And The value of response field "endpointURLs[0].defaultVersionURLs.https" should be "https://localhost:8243<tenantPrefix>/{{ndvCtxBase}}"
    And The response field "endpointURLs[0].defaultVersionURLs.ws" should be null
    And The response field "endpointURLs[0].defaultVersionURLs.wss" should be null

    Examples:
      | actor             | tenantPrefix   |
      | admin             |                |
      | admin@tenant1.com | /t/tenant1.com |

  # The advertised gateway host AND PORT an API is deployed behind are what the DevPortal tells a consumer to
  # call. Ports APIMANAGER5869WSGayewatURLTestCase.testApiGatewayUrlsAfterConfigChangeTest — the legacy-disabled
  # config-override case — and asserts the URLs EXACTLY (scheme, host, port and the full /<context>/<version>
  # path, tenant prefix included) rather than by regex shape.
  #
  # The override is applied through the ADMIN API, not a deployment.toml overlay, because on this product version
  # the advertised URL is built from the deployed environment's VHOST: APIMappingUtil#fromAPIRevisionToEndpoints
  # resolves VHostUtils.getVhostFromEnvironment(environment, host) and formats VHost#getHttpUrl / #getWsUrl. The
  # toml route legacy used (an [[apim.gateway.environment]] http_endpoint/https_endpoint pair fed through
  # APIUtils#extractEndpointURLs) is DEAD CODE in 4.7.0 — that method has no callers. Driving it through the admin
  # API therefore exercises the live mechanism, needs no extra container, and perturbs nothing co-hosted: the
  # override lives entirely inside a scenario-owned environment and the built-in Default environment is untouched.
  #
  # Both API types are covered because they take different branches of the mapper: a REST API gets http+https from
  # the vhost's httpPort/httpsPort, a WS API gets ws+wss from wsPort/wssPort.
  @cap:devportal @feat:discovery @rule:environment @type:regression @dep:publisher @dep:admin @legacy:APIMANAGER5869WSGayewatURLTestCase
  Scenario Outline: The devportal advertises the deployed environment's custom gateway host and ports as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create a gateway environment "${UNIQUE:advEnv}" with vhost host "advertised.gw.example.com" and ports http 9797 https 9898 ws 9199 wss 8199
    Then The response status code should be 201
    And I extract response field "name" and store it as "advEnvName"
    When I put the following JSON payload in context as "advDeployPayload"
    """
    [{"name":"{{advEnvName}}","vhost":"advertised.gw.example.com","displayOnDevportal":true}]
    """

    # --- A REST API: http and https are advertised on the vhost's http/https ports. ---
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "advRestPayload"
    And I create an "apis" resource with payload "advRestPayload" as "advRestApiId"
    Then The response status code should be 201
    And I extract response field "context" and store it as "advRestContext"
    When I put the following JSON payload in context as "advRestRevPayload"
    """
    {"description":"revision for advertised gateway URL checks"}
    """
    And I make a request to create a revision for "apis" resource "advRestApiId" with payload "advRestRevPayload"
    Then The response status code should be 201
    And I copy context value "revisionId" to "advRestRevisionId"
    When I make a request to deploy revision "advRestRevisionId" of "apis" resource "advRestApiId" with payload "advDeployPayload"
    Then The response status code should be 201
    When I publish the "apis" resource with id "advRestApiId"
    Then The lifecycle status of API "advRestApiId" should be "Published"
    When I retrieve the devportal API "advRestApiId" until it contains "advertised.gw.example.com" within 60 seconds
    Then The response status code should be 200
    And The value of response field "endpointURLs[0].environmentName" should be "{{advEnvName}}"
    # The advertised URL is <vhost><api context>/<version>. The publisher's `context` field carries the tenant
    # prefix but NOT the version (verified live: '/t/tenant1.com/<ctx>'), so the version is appended here — which
    # is also what makes this the CONTEXT/VERSION assertion the legacy regex was reaching for.
    And The value of response field "endpointURLs[0].URLs.http" should be "http://advertised.gw.example.com:9797{{advRestContext}}/1.0.0"
    And The value of response field "endpointURLs[0].URLs.https" should be "https://advertised.gw.example.com:9898{{advRestContext}}/1.0.0"

    # --- A WebSocket API: ws and wss are advertised on the vhost's ws/wss ports. ---
    When I put JSON payload from file "artifacts/payloads/create_apim_test_websocket_api.json" in context as "advWsPayload"
    And I create an "apis" resource with payload "advWsPayload" as "advWsApiId"
    Then The response status code should be 201
    And I extract response field "context" and store it as "advWsContext"
    When I put the following JSON payload in context as "advWsRevPayload"
    """
    {"description":"revision for advertised websocket gateway URL checks"}
    """
    And I make a request to create a revision for "apis" resource "advWsApiId" with payload "advWsRevPayload"
    Then The response status code should be 201
    And I copy context value "revisionId" to "advWsRevisionId"
    When I make a request to deploy revision "advWsRevisionId" of "apis" resource "advWsApiId" with payload "advDeployPayload"
    Then The response status code should be 201
    When I publish the "apis" resource with id "advWsApiId"
    Then The lifecycle status of API "advWsApiId" should be "Published"
    When I retrieve the devportal API "advWsApiId" until it contains "advertised.gw.example.com" within 60 seconds
    Then The response status code should be 200
    And The value of response field "endpointURLs[0].environmentName" should be "{{advEnvName}}"
    And The value of response field "endpointURLs[0].URLs.ws" should be "ws://advertised.gw.example.com:9199{{advWsContext}}/1.0.0"
    And The value of response field "endpointURLs[0].URLs.wss" should be "wss://advertised.gw.example.com:8199{{advWsContext}}/1.0.0"

    # Undeploy both revisions so the scenario-owned environment can be deleted by the teardown sweep.
    When I undeploy revision "advRestRevisionId" of "apis" resource "advRestApiId" with payload "advDeployPayload"
    Then The response status code should be 201
    When I undeploy revision "advWsRevisionId" of "apis" resource "advWsApiId" with payload "advDeployPayload"
    Then The response status code should be 201

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
