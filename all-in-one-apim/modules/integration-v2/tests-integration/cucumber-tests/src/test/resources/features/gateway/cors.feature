@cleanup
Feature: Gateway CORS

  Gateway-plane CORS handling: an API with CORS enabled (a specific allowed origin plus allow-credentials)
  returns the Access-Control-Allow-Origin and Access-Control-Allow-Credentials response headers when invoked
  with a matching Origin. Runs in the gateway block (backend + runtime invocation), in both tenants. Ports
  CORSAccessControlAllowCredentialsHeaderTestCase (the CORS-header case; the SDK-generation case is separate).

  @cap:gateway @feat:cors @type:regression @dep:publisher @dep:devportal @legacy:CORSAccessControlAllowCredentialsHeaderTestCase
  Scenario Outline: CORS allow-origin and allow-credentials headers are returned for a matching origin as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_cors_api.json" as "corsApiId" and deployed it
    When I publish the "apis" resource with id "corsApiId"
    Then The lifecycle status of API "corsApiId" should be "Published"
    When I retrieve the "apis" resource with id "corsApiId"
    And I extract response field "context" and store it as "corsContext"
    When I have set up application with keys, subscribed to API "corsApiId", and obtained access token for "corsSubId"
    Then The response status code should be 200

    # Invoke with a matching Origin — the gateway echoes it in Access-Control-Allow-Origin and, because the API
    # enables allow-credentials, returns Access-Control-Allow-Credentials: true.
    When I invoke the API at gateway context "{{corsContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" with request header "Origin" set to "http://localhost" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain the header "Access-Control-Allow-Origin" with value "http://localhost"
    And The response should contain the header "Access-Control-Allow-Credentials" with value "true"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
