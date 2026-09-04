@cleanup
Feature: Gateway Endpoint Certificate TLS Invocation

  The RUNTIME half of the endpoint-certificate feature: whether an uploaded endpoint certificate actually changes
  what the GATEWAY trusts when it calls a backend over TLS. The backend is the tls-backend node app on
  https://nodebackend:3023, which presents a self-signed certificate the gateway does not trust out of the box, and
  which mirrors node-customer-service so GET /customers/123 answers {"id":123,"name":"John"}.

  The whole arc is ONE scenario on purpose: each leg is the control for the next, and the ASSERTION IS THE
  TRANSITION. A post-upload 200 on its own proves nothing (the gateway might have trusted the backend all along);
  a post-delete 500 on its own proves nothing (the API might simply be broken). Only 500 -> upload -> 200 ->
  delete -> 500, observed in that order against one API, shows the certificate is what carries the trust. The final
  500 is additionally held across a settle window, because a single 500 could be a transient rather than
  enforcement — legacy re-probed 3 times, 2s apart, for exactly this reason.

  Runs x2 tenants sequentially because an endpoint certificate is written into the gateway's SHARED trust store;
  each row completes its upload/delete arc before the next tenant starts.

  Ports the invocation methods of APIEndpointCertificateTestCase: testInvokeAPIWithoutUploadingEndpointCertificate,
  testInvokeAPI and testInvokeAPIAfterRemovingCertificate.

  @cap:gateway @feat:security-enforcement @rule:endpoint-certificates @type:regression @dep:publisher @legacy:APIEndpointCertificateTestCase
  Scenario Outline: An endpoint certificate upload and delete flip gateway trust of a TLS backend as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I generate a unique value and store it as "tlsCertAlias"
    And I have created an api from "artifacts/payloads/create_apim_tls_endpoint_api.json" as "tlsCertApiId" and deployed it
    When I publish the "apis" resource with id "tlsCertApiId"
    Then The lifecycle status of API "tlsCertApiId" should be "Published"
    When I retrieve the "apis" resource with id "tlsCertApiId"
    And I extract response field "context" and store it as "tlsCertApiContext"
    When I have set up application with keys, subscribed to API "tlsCertApiId" with plan "Unlimited", and obtained access token for "tlsCertSubId"
    Then The response status code should be 200

    # LEG 1 (the control) — no certificate uploaded: the gateway cannot complete the TLS handshake with the
    # self-signed backend, so the request never reaches it and the gateway answers 500.
    When I invoke the API at gateway context "{{tlsCertApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 500 within 60 seconds
    Then The response status code should be 500

    # LEG 2 — upload the backend's certificate for that endpoint. Propagation is two-staged (the certificate
    # reloader writes it into the gateway trust store, then the HTTPS sender re-reads its SSL profile), which the
    # block's overlay shortens to about a minute; hence the longer window here.
    When I upload endpoint certificate "artifacts/certs/endpoint/nodebackend.cer" with alias "{{tlsCertAlias}}" for endpoint "https://nodebackend:3023"
    Then The response status code should be 201
    When I invoke the API at gateway context "{{tlsCertApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 240 seconds
    Then The response status code should be 200
    # The BACKEND's body, so a gateway-generated 200 (a CORS/fault response, a cached error page) cannot pass.
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    # LEG 3 — delete the certificate: trust is withdrawn and the handshake fails again.
    When I delete the endpoint certificate with alias "{{tlsCertAlias}}"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{tlsCertApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 500 within 240 seconds
    Then The response status code should be 500
    # ...and it STAYS 500: enforcement, not a transient that the until-status poll happened to catch.
    When I invoke the API at gateway context "{{tlsCertApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and the response status code should remain 500 for 10 seconds

    Examples:
      | actor |
      | admin |
      | admin@tenant1.com |
