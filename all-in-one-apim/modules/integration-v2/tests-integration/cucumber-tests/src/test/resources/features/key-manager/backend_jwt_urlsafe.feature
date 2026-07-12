@cleanup
Feature: Backend JWT URL-safe encoding

  Key-manager plane: with backend JWT generation set to URL-SAFE base64 encoding ([apim.jwt] encoding =
  base64url), the gateway emits the X-JWT-Assertion header base64url-encoded. The API routes to the
  header-reflecting backend so the assertion can be decoded and its claims verified — proving the url-safe
  encoding round-trips. Ports URLSafeJWTTestCase.

  @cap:key-manager @feat:backend-jwt @rule:url-safe-jwt @type:regression @dep:gateway @legacy:URLSafeJWTTestCase
  Scenario Outline: The url-safe-encoded backend JWT decodes and carries the standard claims as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_reflect_api.json" as "usjApiId" and deployed it
    When I publish the "apis" resource with id "usjApiId"
    Then The lifecycle status of API "usjApiId" should be "Published"
    When I retrieve the "apis" resource with id "usjApiId"
    And I extract response field "context" and store it as "usjApiContext"
    When I have set up application with keys, subscribed to API "usjApiId", and obtained access token for "usjSubId"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{usjApiContext}}/1.0.0/reflect-headers" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The reflected backend JWT should contain claim "keytype" with value "PRODUCTION"
    And The reflected backend JWT should contain claim "subscriber" with value "admin"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
