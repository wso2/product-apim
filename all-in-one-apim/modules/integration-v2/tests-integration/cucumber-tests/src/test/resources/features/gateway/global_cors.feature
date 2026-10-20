@cleanup
Feature: Gateway-global REST CORS

  Verify the gateway-wide REST CORS configuration independently of per-API corsConfiguration. This feature runs
  only in the isolated mtlsWsCors block, whose overlay sets http://localhost and credentials=true globally; the
  default-CORS behavior remains covered in the unmodified default-config GatewayCors block.

  @cap:gateway @feat:cors @rule:allow-credentials @type:regression @dep:publisher @dep:devportal @legacy:CORSAccessControlAllowCredentialsHeaderTestCase
  Scenario Outline: Gateway-global REST CORS returns localhost and credentials headers as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_default_cors_api.json" as "globalCorsApiId" and deployed it
    And The "apis" resource "globalCorsApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "globalCorsApiId"
    Then The lifecycle status of API "globalCorsApiId" should be "Published"
    When I retrieve the "apis" resource with id "globalCorsApiId"
    And I extract response field "context" and store it as "globalCorsContext"
    When I have set up application with keys, subscribed to API "globalCorsApiId", and obtained access token for "globalCorsSubId"
    Then The response status code should be 200

    # No per-API corsConfiguration is present on this API: these headers must come from the gateway-wide overlay.
    When I invoke the API at gateway context "{{globalCorsContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" with request header "Origin" set to "http://localhost" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    And The response should contain the header "Access-Control-Allow-Origin" with value "http://localhost"
    And The response should contain the header "Access-Control-Allow-Credentials" with value "true"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
