@cap:admin @feat:external-key-manager
Feature: External Key Manager Token Validation and UserInfo

  Runtime validation facets of the IS7 external key manager, reusing the API / application / IS client
  credentials provisioned by _setup_is7_grant_app in the same runner. Covers that the gateway rejects a JWT whose
  signature no longer matches its payload (JWKS validation), and that the IS7 UserInfo endpoint (SCIM2 /scim2/Me,
  the documented gotcha vs /oauth2/userinfo) returns the token owner's profile.

  @rule:token-validation @type:negative @dep:gateway
  Scenario: A tampered IS7 JWT is rejected at the gateway
    # Obtain a valid IS JWT, alter a payload claim while keeping the original signature, then confirm the gateway
    # rejects it (signature no longer validates against IS's JWKS).
    When I request an OAuth access token from the external key manager using client credentials grant
    Then The response status code should be 200
    When I tamper a claim in the generated access token
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 401 within 30 seconds
    Then The response status code should be 401

  @rule:token-validation @type:regression
  Scenario: The IS7 UserInfo endpoint (SCIM2 Me) returns the token owner profile
    # A password-grant token with the openid scope resolves the token owner's SCIM2 profile at UserInfo.
    When I request an OAuth access token from the external key manager using password grant as "admin" with password "admin" requesting scope "openid"
    Then The response status code should be 200
    When I retrieve the current user profile from the external key manager userinfo endpoint
    Then The response status code should be 200
    And The response should contain "admin"

  @rule:grant-types @type:negative
  Scenario: A grant the application is not authorized for is rejected at the token endpoint
    # The setup app is registered for client_credentials/password/refresh_token/authorization_code/device_code but
    # NOT token-exchange; requesting that grant is rejected with 400 (disallowed-grant negative).
    When I attempt an OAuth token from the external key manager using the unsupported grant "urn:ietf:params:oauth:grant-type:token-exchange"
    Then The response status code should be 400
