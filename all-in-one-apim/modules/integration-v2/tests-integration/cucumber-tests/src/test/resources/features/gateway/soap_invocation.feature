@cleanup
Feature: Gateway SOAP API Invocation

  Gateway-plane runtime invocation of a published SOAP (passthrough) API: subscribe an application, obtain
  an access token, and invoke the SOAP API through the gateway expecting a 200. The backend is the in-network
  soap-stub (nodebackend:3019), which returns a fixed SOAP envelope — so this proves gateway SOAP routing
  without depending on an external service. Runs in both the super tenant and tenant1.com as the tenant admin.
  Teardown via the per-scenario cleanup hook.

  @cap:gateway @feat:soap-invocation @type:smoke @dep:publisher @legacy:APIMANAGERInvocationTestCase
  Scenario Outline: Invoke a published SOAP API through the gateway as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_soap_api.json" as "soapApiId" and deployed it
    When I publish the "apis" resource with id "soapApiId"
    Then The lifecycle status of API "soapApiId" should be "Published"

    When I retrieve the "apis" resource with id "soapApiId"
    And I extract response field "context" and store it as "soapApiContext"

    When I have set up application with keys, subscribed to API "soapApiId", and obtained access token for "soapSubscriptionId"
    Then The response status code should be 200

    # Warm-up: wait for the gateway route to come up (full context path, no tenant re-prefix)
    When I invoke the API at gateway context "{{soapApiContext}}/1.0.0" with method "POST" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "CheckPhoneNumberResponse"

    # Invoke the SOAP operation with a SOAP envelope
    When I put the following JSON payload in context as "soapRequest"
    """
    <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ns="http://ws.cdyne.com/PhoneVerify/query">
      <soapenv:Body>
        <ns:CheckPhoneNumber>
          <ns:PhoneNumber>18006785432</ns:PhoneNumber>
          <ns:LicenseKey>0</ns:LicenseKey>
        </ns:CheckPhoneNumber>
      </soapenv:Body>
    </soapenv:Envelope>
    """
    And I invoke the SOAP API at gateway context "{{soapApiContext}}/1.0.0" using access token "generatedAccessToken" and payload "soapRequest" and soap action "http://ws.cdyne.com/PhoneVerify/query/CheckPhoneNumber"
    Then The response status code should be 200
    And The response should contain "CheckPhoneNumberResponse"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # SOAP-to-REST runtime: import hello.wsdl as SOAPTOREST (APIM generates a REST resource per WSDL operation,
  # here POST /sayHello, with in/out sequences that convert REST<->SOAP). Invoking the generated REST resource
  # with a JSON body drives the in-sequence (JSON->SOAP) to the soap-stub backend and the out-sequence
  # (SOAP->JSON) back to the client (200). This is the invocation counterpart of the publisher-plane
  # "WSDL imported as SOAP-to-REST generates REST resources" scenario. Ports the invoke arc of SoapToRestTestCase.
  @cap:gateway @feat:soap-invocation @rule:soap-to-rest @type:regression @dep:publisher @legacy:SoapToRestTestCase
  Scenario Outline: A SOAP-to-REST API converts a REST call to the SOAP backend and back as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I generate a unique value and store it as "srApiName"
    And I generate a unique value and store it as "srApiCtx"
    When I put the following JSON payload in context as "srAddProps"
    """
    {"name":"{{srApiName}}","context":"{{srApiCtx}}","version":"1.0.0","policies":["Unlimited","Bronze"],"endpointConfig":{"endpoint_type":"http","production_endpoints":{"url":"http://nodebackend:3019/service"},"sandbox_endpoints":{"url":"http://nodebackend:3019/service"}}}
    """
    And I import a WSDL API from file "artifacts/wsdl/hello.wsdl" with additional properties "srAddProps" and implementation type "SOAPTOREST" as "srApiId"
    Then The response status code should be 201
    When I deploy the API with id "srApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "srApiId"
    Then The lifecycle status of API "srApiId" should be "Published"
    When I retrieve the "apis" resource with id "srApiId"
    And I extract response field "context" and store it as "srContext"
    When I have set up application with keys, subscribed to API "srApiId", and obtained access token for "srSub"
    Then The response status code should be 200
    When I put the following JSON payload in context as "srBody"
    """
    {"name":"WSO2"}
    """
    When I invoke the API at gateway context "{{srContext}}/1.0.0/sayHello" with method "POST" using access token "generatedAccessToken" and payload "srBody" with content type "application/json" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    # The conversion IS the subject, so the SOAP backend's own payload must reach the client. "SOAP Stub" is the
    # soap-stub's distinctive response value; a 200 alone would be satisfied by an empty body, i.e. by the
    # out-sequence dropping the backend response entirely — which is the exact failure this scenario exists to
    # catch.
    And The response should contain "SOAP Stub"

    # An invalid content type on the SAME resource is rejected with 400 (the 200 arc above is the control that
    # makes this meaningful). Ports SoapToRestTestCase#testDefaultAPIInvocationWithInvalidContentType.
    # application/xml is deliberately NOT pinned: it currently returns 500 (601000), a suspected product defect
    # (an unsupported content type should be 4xx, as text/plain correctly is); legacy never tested it, so it is
    # not a parity gap and pinning 500 would enshrine the bug.
    When I invoke the API at gateway context "{{srContext}}/1.0.0/sayHello" with method "POST" using access token "generatedAccessToken" and payload "srBody" with content type "text/plain" until response status code becomes 400 within 60 seconds
    Then The response status code should be 400
    And The response should contain "Invalid Content-Type detected"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # authType None on a SOAP-to-REST generated resource disables gateway security: the same S2R API is built,
  # its default-secured resource rejects a token-less invoke (401), then the operation is switched to authType
  # None and redeployed, after which the token-less invoke succeeds (200) with the backend's real body.
  # Ports SoapToRestTestCase#testOperationalLevelSecurityForSoapToRest.
  @cap:gateway @feat:soap-invocation @rule:soap-to-rest @type:regression @dep:publisher @legacy:SoapToRestTestCase
  Scenario Outline: authType None disables security on a SOAP-to-REST resource as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I generate a unique value and store it as "snApiName"
    And I generate a unique value and store it as "snApiCtx"
    When I put the following JSON payload in context as "snAddProps"
    """
    {"name":"{{snApiName}}","context":"{{snApiCtx}}","version":"1.0.0","policies":["Unlimited","Bronze"],"endpointConfig":{"endpoint_type":"http","production_endpoints":{"url":"http://nodebackend:3019/service"},"sandbox_endpoints":{"url":"http://nodebackend:3019/service"}}}
    """
    And I import a WSDL API from file "artifacts/wsdl/hello.wsdl" with additional properties "snAddProps" and implementation type "SOAPTOREST" as "snApiId"
    Then The response status code should be 201
    When I deploy the API with id "snApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "snApiId"
    Then The lifecycle status of API "snApiId" should be "Published"
    When I retrieve the "apis" resource with id "snApiId"
    And I extract response field "context" and store it as "snContext"
    And the "apis" resource "snApiId" should be live on the gateway, redeploying if propagation is lost

    # Baseline: with the default authType the generated resource is secured — a token-less invoke is rejected
    # (401). Without this discriminating baseline a later 200 could just mean the resource was never secured.
    When I invoke the API at gateway context "{{snContext}}/1.0.0/sayHello" with method "POST" without authentication until response status code becomes 401 within 60 seconds
    Then The response status code should be 401

    # Switch the generated operation to authType "None" and redeploy.
    When I retrieve the "apis" resource with id "snApiId"
    And I put the response payload in context as "snPayload"
    When I update the "apis" resource "snApiId" and "snPayload" with configuration type "operations" and value:
      """
      [{"target":"/sayHello","verb":"POST","authType":"None","throttlingPolicy":"Unlimited"}]
      """
    Then The response status code should be 200
    When I deploy the API with id "snApiId"
    Then The response status code should be 201
    And the "apis" resource "snApiId" should be live on the gateway, redeploying if propagation is lost
    Then Every operation of API "snApiId" should declare authType "None"

    # Security disabled: the resource is now invocable WITHOUT a token (200) and the backend really served it
    # ("SOAP Stub" — a bare 200 could be satisfied by an empty body).
    When I invoke the API at gateway context "{{snContext}}/1.0.0/sayHello" with method "POST" without authentication until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "SOAP Stub"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Import an API from a WSDL URL — APIM fetches the WSDL over HTTP from the in-network soap-stub at
  # nodebackend:3019/wsdl (the equivalent of the legacy WireMock-hosted WSDL). This WSDL-import variant needs
  # the node backend reachable (initBackend). The subject is now runtime invocation of the URL-imported API
  # (the whole WSDL-import-to-gateway path was previously unproven — no WSDL-imported API was ever invoked), so
  # @cap is gateway and the publisher-plane import is a @dep:publisher prerequisite. Ports the WSDL-URL import +
  # served-WSDL + invoke arcs of WSDLImportTestCase.
  @cap:gateway @feat:soap-invocation @rule:wsdl-import @type:regression @dep:publisher @legacy:WSDLImportTestCase
  Scenario Outline: A WSDL-URL-imported SOAP API is invoked through the gateway as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I generate a unique value and store it as "wsdlUrlName"
    And I generate a unique value and store it as "wsdlUrlCtx"
    When I put the following JSON payload in context as "wsdlUrlProps"
    """
    {"name":"{{wsdlUrlName}}","context":"{{wsdlUrlCtx}}","version":"1.0.0","policies":["Unlimited","Bronze"],"endpointConfig":{"endpoint_type":"http","production_endpoints":{"url":"http://nodebackend:3019/service"},"sandbox_endpoints":{"url":"http://nodebackend:3019/service"}}}
    """
    And I import a WSDL API from URL "http://nodebackend:3019/wsdl" with additional properties "wsdlUrlProps" and implementation type "SOAP" as "wsdlUrlId"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "wsdlUrlId"
    Then The response status code should be 200
    And The value of response field "name" should be "{{wsdlUrlName}}"
    And The value of response field "type" should be "SOAP"

    # Serving the WSDL definition of the URL-imported API must succeed (closes the URL-imported arm of
    # WSDLImportTestCase#testGetWsdlDefinitions).
    When I retrieve the WSDL definition of API "wsdlUrlId"
    Then The response status code should be 200

    # Deploy + publish so the URL-imported WSDL API is actually invoked at the gateway.
    When I deploy the API with id "wsdlUrlId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "wsdlUrlId"
    Then The lifecycle status of API "wsdlUrlId" should be "Published"
    When I retrieve the "apis" resource with id "wsdlUrlId"
    And I extract response field "context" and store it as "wsdlUrlContext"

    And the "apis" resource "wsdlUrlId" should be live on the gateway, redeploying if propagation is lost

    # The URL-imported arm of WSDLImportTestCase#testDownloadWsdlDefinitionsFromStore. It is asserted here rather
    # than in publisher/soap_design.feature (which owns the file and archive arms) because importing from a WSDL
    # URL needs the in-network soap-stub, and only this block starts a backend. The store copy must carry the
    # GATEWAY address, not the stub's — the same null-hostname rewrite guard the file arm pins.
    When I download the WSDL definition of API "wsdlUrlId" from the devportal store
    Then The response status code should be 200
    And The response should contain "<soap:address location=\"http://localhost:8280{{wsdlUrlContext}}/1.0.0\"/>"

    When I have set up application with keys, subscribed to API "wsdlUrlId", and obtained access token for "wsdlUrlSub"
    Then The response status code should be 200

    # Invoke the SOAP operation with a SOAP envelope; the soap-stub returns CheckPhoneNumberResponse (200).
    When I put the following JSON payload in context as "wsdlUrlSoapRequest"
    """
    <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ns="http://ws.cdyne.com/PhoneVerify/query">
      <soapenv:Body>
        <ns:CheckPhoneNumber>
          <ns:PhoneNumber>18006785432</ns:PhoneNumber>
          <ns:LicenseKey>0</ns:LicenseKey>
        </ns:CheckPhoneNumber>
      </soapenv:Body>
    </soapenv:Envelope>
    """
    And I invoke the SOAP API at gateway context "{{wsdlUrlContext}}/1.0.0" using access token "generatedAccessToken" and payload "wsdlUrlSoapRequest" and soap action "http://ws.cdyne.com/PhoneVerify/query/CheckPhoneNumber"
    Then The response status code should be 200
    And The response should contain "CheckPhoneNumberResponse"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # The FILE- and ARCHIVE-imported SOAP arms carried through to a real gateway invocation. The publisher-plane
  # halves (name/type echoed, GET 200, WSDL served back) already live in publisher/soap_design.feature; what was
  # unproven there — and what legacy's testCreateSOAPAPIFromFile / testCreateSOAPAPIFromArchive assert — is that
  # such an API deploys, publishes, takes a subscription and answers a real SOAP call (200 carrying the backend's
  # CheckPhoneNumberResponse). @cap is gateway because the invocation is the subject; the import is @dep:publisher.
  # The wsdlSource column IS the difference between the two legacy arms (one import endpoint, archive detected by
  # extension), so both sources run in both tenants. Legacy's PhoneVerification.wsdl/.zip pointed at the public
  # ws.cdyne.com; the in-network equivalents (hello.wsdl/hello.zip against the soap-stub) are used instead — a
  # SOAP passthrough API routes by context, not by the WSDL, so the stub answers the CheckPhoneNumber envelope.
  @cap:gateway @feat:soap-invocation @rule:wsdl-import @type:regression @dep:publisher @legacy:WSDLImportTestCase
  Scenario Outline: A WSDL-<sourceKind>-imported SOAP API is invoked through the gateway as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I generate a unique value and store it as "wsdlFsName"
    And I generate a unique value and store it as "wsdlFsCtx"
    When I put the following JSON payload in context as "wsdlFsProps"
    """
    {"name":"{{wsdlFsName}}","context":"{{wsdlFsCtx}}","version":"1.0.0","policies":["Unlimited","Bronze"],"endpointConfig":{"endpoint_type":"http","production_endpoints":{"url":"http://nodebackend:3019/service"},"sandbox_endpoints":{"url":"http://nodebackend:3019/service"}}}
    """
    And I import a WSDL API from file "<wsdlSource>" with additional properties "wsdlFsProps" and implementation type "SOAP" as "wsdlFsId"
    Then The response status code should be 201
    # Legacy's import assertions: the requested name is echoed back and a GET of the created API returns 200.
    When I retrieve the "apis" resource with id "wsdlFsId"
    Then The response status code should be 200
    And The value of response field "name" should be "{{wsdlFsName}}"
    And The value of response field "type" should be "SOAP"

    # Deploy + publish + subscribe so the imported API is actually reachable at the gateway.
    When I deploy the API with id "wsdlFsId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "wsdlFsId"
    Then The lifecycle status of API "wsdlFsId" should be "Published"
    When I retrieve the "apis" resource with id "wsdlFsId"
    And I extract response field "context" and store it as "wsdlFsContext"
    And the "apis" resource "wsdlFsId" should be live on the gateway, redeploying if propagation is lost
    When I have set up application with keys, subscribed to API "wsdlFsId", and obtained access token for "wsdlFsSub"
    Then The response status code should be 200

    # Invoke the SOAP operation with a SOAP envelope; the soap-stub returns CheckPhoneNumberResponse (200).
    When I put the following JSON payload in context as "wsdlFsSoapRequest"
    """
    <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ns="http://ws.cdyne.com/PhoneVerify/query">
      <soapenv:Body>
        <ns:CheckPhoneNumber>
          <ns:PhoneNumber>18006785432</ns:PhoneNumber>
          <ns:LicenseKey>0</ns:LicenseKey>
        </ns:CheckPhoneNumber>
      </soapenv:Body>
    </soapenv:Envelope>
    """
    And I invoke the SOAP API at gateway context "{{wsdlFsContext}}/1.0.0" using access token "generatedAccessToken" and payload "wsdlFsSoapRequest" and soap action "http://ws.cdyne.com/PhoneVerify/query/CheckPhoneNumber"
    Then The response status code should be 200
    And The response should contain "CheckPhoneNumberResponse"

    Examples:
      | actor             | sourceKind | wsdlSource                |
      | admin             | file       | artifacts/wsdl/hello.wsdl |
      | admin             | archive    | artifacts/wsdl/hello.zip  |
      | admin@tenant1.com | file       | artifacts/wsdl/hello.wsdl |
      | admin@tenant1.com | archive    | artifacts/wsdl/hello.zip  |

  # An operation-level OAuth scope is enforced on a SOAP-to-REST generated resource: a shared scope bound to the
  # "admin" role is put on the generated POST /sayHello operation, so a token carrying that scope is let through
  # to the SOAP backend (200 + "SOAP Stub") while a token lacking it is refused at the gateway (403). Both arcs
  # are asserted — the 403 arc is what makes this enforcement rather than mere configuration.
  # Ports SoapToRestTestCase#testOperationalLevelOAuthScopesForSoapToRest.
  @cap:gateway @feat:soap-invocation @rule:soap-to-rest @type:regression @dep:publisher @legacy:SoapToRestTestCase
  Scenario Outline: An operation-level OAuth scope is enforced on a SOAP-to-REST resource as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I generate a unique value and store it as "soApiName"
    And I generate a unique value and store it as "soApiCtx"
    When I put the following JSON payload in context as "soAddProps"
    """
    {"name":"{{soApiName}}","context":"{{soApiCtx}}","version":"1.0.0","policies":["Unlimited","Bronze"],"endpointConfig":{"endpoint_type":"http","production_endpoints":{"url":"http://nodebackend:3019/service"},"sandbox_endpoints":{"url":"http://nodebackend:3019/service"}}}
    """
    And I import a WSDL API from file "artifacts/wsdl/hello.wsdl" with additional properties "soAddProps" and implementation type "SOAPTOREST" as "soApiId"
    Then The response status code should be 201
    # Register a shared scope bound to the "admin" role (which both actors have) and gate the generated /sayHello
    # POST operation with it.
    When I create a new shared scope as "s2rScopeEnf"
    Then The response status code should be 201
    And I extract response field "name" and store it as "s2rScopeName"
    When I retrieve the "apis" resource with id "soApiId"
    And I put the response payload in context as "soPayload"
    When I update the "apis" resource "soApiId" and "soPayload" with configuration type "scopes" and value:
      """
      [{"shared":true,"scope":{"name":"{{s2rScopeName}}","displayName":"{{s2rScopeName}}","description":"s2r operation scope enforcement","bindings":["admin"]}}]
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "soApiId"
    And I put the response payload in context as "soPayload"
    When I update the "apis" resource "soApiId" and "soPayload" with configuration type "operations" and value:
      """
      [{"target":"/sayHello","verb":"POST","authType":"Application & Application User","throttlingPolicy":"Unlimited","scopes":["{{s2rScopeName}}"],"operationPolicies":{"request":[],"response":[],"fault":[]}}]
      """
    Then The response status code should be 200
    When I deploy the API with id "soApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "soApiId"
    Then The lifecycle status of API "soApiId" should be "Published"
    # Deploy-readiness gate before the first gateway read: the runtime deploy event is at-most-once, so if the
    # gateway dropped it no amount of until-200 polling can recover — this re-deploys the revision instead.
    And the "apis" resource "soApiId" should be live on the gateway, redeploying if propagation is lost
    When I retrieve the "apis" resource with id "soApiId"
    And I extract response field "context" and store it as "soContext"
    # Subscribe an application keyed for the password grant so a scoped user token can be requested.
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
    And I subscribe to API "soApiId" using application "createdAppId" with payload "apiSubscriptionPayload" as "soSub"
    Then The response status code should be 201
    When I put the following JSON payload in context as "soBody"
    """
    {"name":"WSO2"}
    """
    # A token WITH the scope is let through to the SOAP backend (200 + the stub's real body).
    When I request an OAuth access token for the current user using password grant with scope "{{s2rScopeName}}"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{soContext}}/1.0.0/sayHello" with method "POST" using access token "generatedAccessToken" and payload "soBody" with content type "application/json" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "SOAP Stub"
    # A token WITHOUT the scope (openid only) is refused at the gateway (403). The 200 arc above is the control,
    # and 403 is unreachable from the served-200 state, so this gate reads the enforcement, not stale behaviour.
    When I request an OAuth access token for the current user using password grant with scope "openid"
    Then The response status code should be 200
    And I invoke the API at gateway context "{{soContext}}/1.0.0/sayHello" with method "POST" using access token "generatedAccessToken" and payload "soBody" with content type "application/json" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # A SOAP-to-REST API made the DEFAULT version is invocable through its VERSIONLESS context (no /1.0.0 segment):
  # the gateway resolves the default version and drives the JSON->SOAP conversion to the backend (200 + "SOAP
  # Stub"). The versioned invoke is asserted first as a control, so the versionless 200 proves default-version
  # routing rather than an unrelated fallback. Ports SoapToRestTestCase#testDefaultAPIInvocation.
  @cap:gateway @feat:soap-invocation @rule:soap-to-rest @type:regression @dep:publisher @legacy:SoapToRestTestCase
  Scenario Outline: A default-version SOAP-to-REST API is invocable through its versionless context as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I generate a unique value and store it as "dvApiName"
    And I generate a unique value and store it as "dvApiCtx"
    When I put the following JSON payload in context as "dvAddProps"
    """
    {"name":"{{dvApiName}}","context":"{{dvApiCtx}}","version":"1.0.0","policies":["Unlimited","Bronze"],"endpointConfig":{"endpoint_type":"http","production_endpoints":{"url":"http://nodebackend:3019/service"},"sandbox_endpoints":{"url":"http://nodebackend:3019/service"}}}
    """
    And I import a WSDL API from file "artifacts/wsdl/hello.wsdl" with additional properties "dvAddProps" and implementation type "SOAPTOREST" as "dvApiId"
    Then The response status code should be 201
    # Mark this (single-version) API the default version, then deploy + publish so the versionless route is live.
    When I retrieve the "apis" resource with id "dvApiId"
    And I put the response payload in context as "dvPayload"
    When I update the "apis" resource "dvApiId" and "dvPayload" with configuration type "isDefaultVersion" and value:
      """
      true
      """
    Then The response status code should be 200
    When I deploy the API with id "dvApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "dvApiId"
    Then The lifecycle status of API "dvApiId" should be "Published"
    # Deploy-readiness gate before the first gateway read (see the scope-enforcement scenario above for why a
    # dropped at-most-once deploy event cannot be polled away).
    And the "apis" resource "dvApiId" should be live on the gateway, redeploying if propagation is lost
    When I retrieve the "apis" resource with id "dvApiId"
    And I extract response field "context" and store it as "dvContext"
    When I have set up application with keys, subscribed to API "dvApiId", and obtained access token for "dvSub"
    Then The response status code should be 200
    When I put the following JSON payload in context as "dvBody"
    """
    {"name":"WSO2"}
    """
    # Control: the VERSIONED context invokes and converts to the backend (200 + "SOAP Stub").
    When I invoke the API at gateway context "{{dvContext}}/1.0.0/sayHello" with method "POST" using access token "generatedAccessToken" and payload "dvBody" with content type "application/json" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "SOAP Stub"
    # The VERSIONLESS context (no /1.0.0 segment) routes to the default version and converts identically.
    When I invoke the API at gateway context "{{dvContext}}/sayHello" with method "POST" using access token "generatedAccessToken" and payload "dvBody" with content type "application/json" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "SOAP Stub"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # A SOAP-to-REST API is invocable with an OPAQUE (tokenType OAUTH) application token: the gateway accepts a
  # non-JWT, reference-style token for an S2R resource and drives the JSON->SOAP conversion (200 + "SOAP Stub").
  # The invoke itself is identical to the JWT case, so the discriminator is the application fixture's tokenType
  # ("OAUTH" in create_apim_test_app_oauth.json → opaque token). Ports
  # SoapToRestTestCase#testInvokeSoapToRestAPIUsingOAuthApplication.
  @cap:gateway @feat:soap-invocation @rule:soap-to-rest @type:regression @dep:publisher @legacy:SoapToRestTestCase
  Scenario Outline: A SOAP-to-REST API is invocable with an opaque application token as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I generate a unique value and store it as "opApiName"
    And I generate a unique value and store it as "opApiCtx"
    When I put the following JSON payload in context as "opAddProps"
    """
    {"name":"{{opApiName}}","context":"{{opApiCtx}}","version":"1.0.0","policies":["Unlimited","Bronze"],"endpointConfig":{"endpoint_type":"http","production_endpoints":{"url":"http://nodebackend:3019/service"},"sandbox_endpoints":{"url":"http://nodebackend:3019/service"}}}
    """
    And I import a WSDL API from file "artifacts/wsdl/hello.wsdl" with additional properties "opAddProps" and implementation type "SOAPTOREST" as "opApiId"
    Then The response status code should be 201
    When I deploy the API with id "opApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "opApiId"
    Then The lifecycle status of API "opApiId" should be "Published"
    # Deploy-readiness gate before the first gateway read (see the scope-enforcement scenario above for why a
    # dropped at-most-once deploy event cannot be polled away).
    And the "apis" resource "opApiId" should be live on the gateway, redeploying if propagation is lost
    When I retrieve the "apis" resource with id "opApiId"
    And I extract response field "context" and store it as "opContext"
    # The OAUTH-tokenType application fixture is the discriminator — its generated token is opaque, not a JWT.
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app_oauth.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "generateApplicationKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateApplicationKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "apiSubscriptionPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "opApiId" using application "createdAppId" with payload "apiSubscriptionPayload" as "opSub"
    Then The response status code should be 201
    When I put the following JSON payload in context as "createApplicationAccessTokenPayload"
    """
    {"consumerSecret": "{{appConsumerSecret}}", "validityPeriod": 3600}
    """
    And I request an access token for application id "createdAppId" using payload "createApplicationAccessTokenPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "opBody"
    """
    {"name":"WSO2"}
    """
    When I invoke the API at gateway context "{{opContext}}/1.0.0/sayHello" with method "POST" using access token "generatedAccessToken" and payload "opBody" with content type "application/json" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "SOAP Stub"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
