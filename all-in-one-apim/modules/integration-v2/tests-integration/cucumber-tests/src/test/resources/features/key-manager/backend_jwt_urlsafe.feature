@cleanup
Feature: Backend JWT URL-safe encoding

  Key-manager plane: with backend JWT generation set to URL-SAFE base64 encoding ([apim.jwt] encoding =
  base64url), the gateway emits the X-JWT-Assertion header base64url-encoded. The API routes to the
  header-reflecting backend so the assertion can be decoded and its claims verified — proving the url-safe
  encoding round-trips. It also pins the legacy required application-attribute defaults and confirms that the
  optional empty attribute is omitted with enable_empty_values_in_application_attributes disabled. Ports
  URLSafeJWTTestCase.

  @cap:key-manager @feat:backend-jwt @rule:url-safe-jwt @type:regression @dep:gateway @legacy:URLSafeJWTTestCase
  Scenario Outline: The url-safe-encoded backend JWT decodes and carries the standard claims as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I provision a user with exact name "subscriberUser3" password "password@123" and roles "Internal/subscriber" storing the username as "usjResourceOwner"
    And I have created an api from "artifacts/payloads/create_apim_reflect_api.json" as "usjApiId" and deployed it
    When I publish the "apis" resource with id "usjApiId"
    Then The lifecycle status of API "usjApiId" should be "Published"
    When I retrieve the "apis" resource with id "usjApiId"
    And I extract response field "context" and store it as "usjApiContext"
    When I put JSON payload from file "artifacts/payloads/create_apim_urlsafe_attribute_app.json" in context as "usjAppPayload"
    And I set the field "tokenType" to "JWT" in the payload "usjAppPayload"
    And I create an application with payload "usjAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "usjKeys"
    """
    {"keyType":"PRODUCTION","grantTypesToBeSupported":["client_credentials","password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "usjKeys"
    Then The response status code should be 200
    When I put the following JSON payload in context as "usjSub"
    """
    {"applicationId":"{{applicationId}}","apiId":"{{apiId}}","throttlingPolicy":"Gold"}
    """
    And I subscribe to API "usjApiId" using application "createdAppId" with payload "usjSub" as "usjSubId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "usjToken"
    """
    {"consumerSecret":"{{appConsumerSecret}}","validityPeriod":3600}
    """
    And I request an access token for application id "createdAppId" using payload "usjToken"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{usjApiContext}}/1.0.0/reflect-headers" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The reflected backend JWT should contain claim "keytype" with value "PRODUCTION"
    And The reflected backend JWT should contain claim "subscriber" with value "<actor>"
    # Same generator invariants as the standard-encoding feature — proving the claims survive the url-safe round trip.
    And The reflected backend JWT should contain claim "iss" with value "wso2.org/products/am"
    And The reflected backend JWT should contain claim "applicationtier" with value "Unlimited"
    And The reflected backend JWT applicationAttributes claim should exactly equal
    """
    {"Production access required":"Yes","Sandbox access required":"Yes"}
    """
    And The reflected backend JWT applicationAttributes claim should not contain attribute "Optional attribute"
    And The reflected backend JWT header should contain "typ" with value "JWT"
    # Same JOSE-header invariants as the standard-encoding feature: RS256 signing and the kid must survive the
    # url-safe round trip too (the kid string value itself is not affected by the segment encoding). The kid is
    # base64(DN+serial) of the shipped wso2carbon signing certificate — deterministic and identical across tenants.
    And The reflected backend JWT header should contain "alg" with value "RS256"
    And The reflected backend JWT header should contain "kid" with value "Q049bG9jYWxob3N0LCBPVT1XU08yLCBPPVdTTzIsIEw9TW91bnRhaW4gVmlldywgU1Q9Q0EsIEM9VVMjMjExMjc5NDc5Njg2NTExMjgzMzcxNzY2ODEwMjc1MjAyMjU0ODQ5MzE4NzgwMzI3"
    # Pin cryptographic validity as well as the header values: a matching alg and kid alone do not prove this
    # URL-safe assertion's signature verifies against the key the gateway publishes.
    And The reflected backend JWT should be verifiably signed by the key the gateway JWKS endpoint publishes
    # THE assertion this feature exists for: the raw X-JWT-Assertion segments must be base64url (no '+', '/', '=').
    # The claim assertions above decode through a helper that falls back to standard base64, so they would stay
    # green even if encoding reverted to base64 — this raw, no-fallback check is what makes the feature able to fail.
    And The reflected backend JWT should be URL-safe base64 encoded

    # Repeat the invocation with the resource-owner password grant used by legacy URLSafeJWTTestCase. The gateway
    # must preserve the URL-safe assertion encoding independently of how the caller's OAuth token was obtained.
    When I request an OAuth access token using password grant as user "<loginName>" with password "password@123"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{usjApiContext}}/1.0.0/reflect-headers" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The reflected backend JWT should contain claim "keytype" with value "PRODUCTION"
    And The reflected backend JWT should contain claim "subscriber" with value "<actor>"
    And The reflected backend JWT should contain claim "iss" with value "wso2.org/products/am"
    And The reflected backend JWT should contain claim "applicationtier" with value "Unlimited"
    And The reflected backend JWT applicationAttributes claim should exactly equal
    """
    {"Production access required":"Yes","Sandbox access required":"Yes"}
    """
    And The reflected backend JWT applicationAttributes claim should not contain attribute "Optional attribute"
    And The reflected backend JWT header should contain "typ" with value "JWT"
    And The reflected backend JWT header should contain "alg" with value "RS256"
    And The reflected backend JWT header should contain "kid" with value "Q049bG9jYWxob3N0LCBPVT1XU08yLCBPPVdTTzIsIEw9TW91bnRhaW4gVmlldywgU1Q9Q0EsIEM9VVMjMjExMjc5NDc5Njg2NTExMjgzMzcxNzY2ODEwMjc1MjAyMjU0ODQ5MzE4NzgwMzI3"
    And The reflected backend JWT should be verifiably signed by the key the gateway JWKS endpoint publishes
    And The reflected backend JWT should be URL-safe base64 encoded

    Examples:
      | actor             | loginName                     |
      | admin             | subscriberUser3                |
      | admin@tenant1.com | subscriberUser3@tenant1.com   |
