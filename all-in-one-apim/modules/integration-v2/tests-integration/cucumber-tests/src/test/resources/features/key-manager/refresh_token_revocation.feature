@cleanup
Feature: Key Manager Refresh Token Revocation

  Revoking with a REFRESH token at the revocation endpoint also revokes the access token issued with it, so the
  gateway must reject that JWT afterwards. The Resident Key Manager's token revocation listener
  (wso2is.notification.event.handlers) has to notify the gateway in this case too, not only when the access token
  itself is revoked. Without that notification a revoked JWT keeps working at the gateway until it expires.

  This only holds with OAuth token persistence enabled. With persistence disabled (the product default), revoking a
  refresh token does not revoke the access token issued with it, so there is nothing for the gateway to reject.
  The feature is therefore run only by KeyManagerTokenPersistenceRunner in the token-persistence block, and is kept
  out of token_revocation.feature, whose runner uses the default configuration.

  # Ports JWTRefreshTokenRevocationTestCase (product-apim 4.7.0): a JWT application, a password-grant token with a
  # refresh token, an exact XML invocation, revocation with the refresh token (with and without token_type_hint),
  # then an exact 401 for the access token within the same 19-attempt bound as the legacy test. The openid scope
  # keeps the user token distinct from the application token issued at key generation, which has no refresh token.
  @cap:key-manager @feat:token-revocation @rule:refresh-token @type:regression @dep:gateway
  Scenario Outline: Revoking with a refresh token blocks the JWT issued with it as <actor> (token_type_hint "<hint>")
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_jaxrs_xml_api.json" as "rtRevApiId" and deployed it
    And the "apis" resource "rtRevApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "rtRevApiId"
    Then The lifecycle status of API "rtRevApiId" should be "Published"
    When I retrieve the "apis" resource with id "rtRevApiId"
    And I extract response field "context" and store it as "rtRevApiContext"

    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "rtRevAppPayload"
    And I set the field "tokenType" to "JWT" in the payload "rtRevAppPayload"
    And I create an application with payload "rtRevAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "rtRevKeysPayload"
    """
    {"keyType":"PRODUCTION","grantTypesToBeSupported":["client_credentials","password","refresh_token"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "rtRevKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "rtRevSubPayload"
    """
    {"applicationId":"{{applicationId}}","apiId":"{{apiId}}","throttlingPolicy":"Unlimited"}
    """
    And I subscribe to API "rtRevApiId" using application "createdAppId" with payload "rtRevSubPayload" as "rtRevSubId"
    Then The response status code should be 201

    # Extract both tokens from THIS response, so a value left in context by an earlier row can never be reused.
    When I request an OAuth access token for the current user using password grant with scope "openid"
    Then The response status code should be 200
    And I extract response field "access_token" and store it as "rtRevAccessToken"
    And I extract response field "refresh_token" and store it as "rtRevRefreshToken"

    When I invoke the API once at gateway context "{{rtRevApiContext}}/1.0.0/customers/123/" with method "GET" using access token "rtRevAccessToken" and payload "" with request header "Accept" set to "text/xml"
    Then The response status code should be 200
    And The response should contain "<id>123</id><name>John</name></Customer>"

    When I revoke the OAuth token "rtRevRefreshToken" with token type hint "<hint>"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{rtRevApiContext}}/1.0.0/customers/123/" with method "GET" using access token "rtRevAccessToken" and payload "" with request header "Accept" set to "text/xml" until the revoked token is rejected within 19 seconds
    Then The response status code should be 401

    Examples:
      | actor             | hint          |
      | admin             | refresh_token |
      | admin             |               |
      | admin@tenant1.com | refresh_token |
      | admin@tenant1.com |               |
