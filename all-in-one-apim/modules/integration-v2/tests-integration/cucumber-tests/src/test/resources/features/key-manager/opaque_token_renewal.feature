@cleanup
Feature: Key Manager Opaque Token Renewal

  Key-manager-plane renewal of OPAQUE access tokens without revoking the tokens already issued, driven by
  oauth.opaque.renew_token_without_revoking_existing in deployment.toml. Runs in the
  IntegrationV2-OpaqueTokenRenewal block, whose overlay enables the setting for the client_credentials grant
  ONLY. The application's OAuth client is switched to opaque issuance first, because the setting governs
  opaque tokens and has no effect on JWT-format tokens.
  Two halves make the assertion meaningful. The allowed grant must hand out a DIFFERENT token on each
  request while every earlier token stays active on introspection, which is the feature itself. The password
  grant, deliberately left out of allowed_grant_types, must hand back the SAME token twice: without that
  control a per-request token binding would also produce distinct tokens and the first half alone would pass
  whether or not the setting took effect.
  It also guards the configuration mapping. The setting only reaches the server when identity.xml.j2 maps it
  into identity.xml, and the opaque mapping was missing while the JWT mapping was present, so the config was
  accepted and silently ignored. Unit tests in the OAuth component mock the config parser and cannot see that.
  Teardown via the per-scenario cleanup hook.

  @cap:key-manager @feat:token-issuance @rule:opaque-renewal @type:regression
  Scenario Outline: An allowed grant issues a new opaque token and leaves earlier ones active as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "generateApplicationKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateApplicationKeysPayload"
    Then The response status code should be 200
    When I switch the OAuth client with consumer key "consumerKey" to opaque token issuance

    When I request a client-credentials token using consumer key "consumerKey" and secret "consumerSecret"
    Then The response status code should be 200
    And I extract response field "access_token" and store it as "opaqueTokenOne"
    And The stored access token "opaqueTokenOne" should be opaque, not a JWT

    When I request a client-credentials token using consumer key "consumerKey" and secret "consumerSecret"
    Then The response status code should be 200
    And I extract response field "access_token" and store it as "opaqueTokenTwo"

    When I request a client-credentials token using consumer key "consumerKey" and secret "consumerSecret"
    Then The response status code should be 200
    And I extract response field "access_token" and store it as "opaqueTokenThree"

    # Renewal: each request must mint a distinct token rather than replay the cached one.
    Then The stored value "opaqueTokenTwo" should not equal "opaqueTokenOne"
    And The stored value "opaqueTokenThree" should not equal "opaqueTokenOne"
    And The stored value "opaqueTokenThree" should not equal "opaqueTokenTwo"

    # Without revoking existing: the earlier tokens must survive the later issuances.
    When I introspect the access token "opaqueTokenOne"
    Then The response status code should be 200
    And The value of response field "active" should be "true"
    When I introspect the access token "opaqueTokenTwo"
    Then The response status code should be 200
    And The value of response field "active" should be "true"
    When I introspect the access token "opaqueTokenThree"
    Then The response status code should be 200
    And The value of response field "active" should be "true"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # The control. password is NOT in allowed_grant_types, so the key manager must answer a repeat request with
  # the token it already holds. This is what distinguishes the renewal feature from a per-request token
  # binding, which would mint a new token for every grant and make the scenario above pass vacuously.
  @cap:key-manager @feat:token-issuance @rule:opaque-renewal @type:negative
  Scenario Outline: A grant outside the allowed list reuses the existing opaque token as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "generateApplicationKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateApplicationKeysPayload"
    Then The response status code should be 200
    When I switch the OAuth client with consumer key "consumerKey" to opaque token issuance

    When I request an OAuth access token for the current user using password grant with scope "PRODUCTION"
    Then The response status code should be 200
    And I extract response field "access_token" and store it as "passwordTokenOne"
    And The stored access token "passwordTokenOne" should be opaque, not a JWT

    When I request an OAuth access token for the current user using password grant with scope "PRODUCTION"
    Then The response status code should be 200
    And I extract response field "access_token" and store it as "passwordTokenTwo"

    Then The stored value "passwordTokenTwo" should equal "passwordTokenOne"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
