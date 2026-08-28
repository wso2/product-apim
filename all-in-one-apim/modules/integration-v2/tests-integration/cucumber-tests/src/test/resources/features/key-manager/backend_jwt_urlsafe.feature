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
    And The reflected backend JWT should contain claim "subscriber" with value "<actor>"
    # Same generator invariants as the standard-encoding feature — proving the claims survive the url-safe round trip.
    And The reflected backend JWT should contain claim "iss" with value "wso2.org/products/am"
    And The reflected backend JWT should contain claim "applicationtier" with value "Unlimited"
    And The reflected backend JWT header should contain "typ" with value "JWT"
    # Same JOSE-header invariants as the standard-encoding feature: RS256 signing and the kid must survive the
    # url-safe round trip too (the kid string value itself is not affected by the segment encoding). The kid is
    # base64(DN+serial) of the shipped wso2carbon signing certificate — deterministic and identical across tenants.
    And The reflected backend JWT header should contain "alg" with value "RS256"
    And The reflected backend JWT header should contain "kid" with value "Q049bG9jYWxob3N0LCBPVT1XU08yLCBPPVdTTzIsIEw9TW91bnRhaW4gVmlldywgU1Q9Q0EsIEM9VVMjMjExMjc5NDc5Njg2NTExMjgzMzcxNzY2ODEwMjc1MjAyMjU0ODQ5MzE4NzgwMzI3"
    # THE assertion this feature exists for: the raw X-JWT-Assertion segments must be base64url (no '+', '/', '=').
    # The claim assertions above decode through a helper that falls back to standard base64, so they would stay
    # green even if encoding reverted to base64 — this raw, no-fallback check is what makes the feature able to fail.
    And The reflected backend JWT should be URL-safe base64 encoded

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
