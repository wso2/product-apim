@cleanup
Feature: Key Manager Application Revocation

  Ports ConsumerAppBasedJWTRevocation — revoking a consumer application invalidates its issued tokens. A JWT
  application obtains an access token; introspecting it shows active with the application's client_id; after the
  application's OAuth state is set to REVOKED (via the OAuthAdminService admin operation), introspecting the same
  token shows inactive. Runs in the standard key-manager block; ×2 tenant. Teardown via @cleanup removes the app.

  @cap:key-manager @feat:token-revocation @rule:app-revocation @type:regression @legacy:ConsumerAppBasedJWTRevocation
  Scenario Outline: Revoking a consumer application invalidates its tokens as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"

    # A JWT-token-type application with client_credentials keys.
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "revAppPayload"
    And I set the field "tokenType" to "JWT" in the payload "revAppPayload"
    And I create an application with payload "revAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "revKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "revKeysPayload"
    Then The response status code should be 200

    # Obtain an access token with the generated credentials.
    When I request a client-credentials token using consumer key "consumerKey" and secret "consumerSecret"
    Then The response status code should be 200
    And I extract response field "access_token" and store it as "revToken"

    # Introspect: the token is active and carries the application's client_id (== consumerKey).
    When I introspect the access token "revToken"
    Then The response status code should be 200
    And The response should contain "\"active\":true"
    And The response should contain "{{consumerKey}}"

    # Revoke the application's OAuth state.
    When I revoke the OAuth application with consumer key "consumerKey" by setting its state to "REVOKED"

    # Introspect again: the token is now inactive.
    When I introspect the access token "revToken"
    Then The response status code should be 200
    And The response should contain "\"active\":false"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
