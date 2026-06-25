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
