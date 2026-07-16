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

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Import an API from a WSDL URL — APIM fetches the WSDL over HTTP from the in-network soap-stub at
  # nodebackend:3019/wsdl (the equivalent of the legacy WireMock-hosted WSDL). This WSDL-import variant needs
  # the node backend reachable (initBackend), so it lives in the gateway block rather than the publisher block.
  # The assertion is publisher-plane (create + retrieve). Ports the WSDL-URL import arc of WSDLImportTestCase.
  @cap:publisher @feat:soap-design @rule:wsdl-import @type:regression @dep:gateway @legacy:WSDLImportTestCase
  Scenario Outline: An API can be imported from a WSDL URL as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I generate a unique value and store it as "wsdlUrlName"
    And I generate a unique value and store it as "wsdlUrlCtx"
    When I put the following JSON payload in context as "wsdlUrlProps"
    """
    {"name":"{{wsdlUrlName}}","context":"{{wsdlUrlCtx}}","version":"1.0.0","policies":["Unlimited"],"endpointConfig":{"endpoint_type":"http","production_endpoints":{"url":"http://nodebackend:3019/service"},"sandbox_endpoints":{"url":"http://nodebackend:3019/service"}}}
    """
    And I import a WSDL API from URL "http://nodebackend:3019/wsdl" with additional properties "wsdlUrlProps" and implementation type "SOAP" as "wsdlUrlId"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "wsdlUrlId"
    Then The response status code should be 200
    And The response should contain "{{wsdlUrlName}}"
    And The response should contain "SOAP"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
