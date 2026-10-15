@cleanup
Feature: Key Manager PKCE-Enabled Keys

  Ports PkceEnabledApplicationTestCase — generating application OAuth keys with PKCE additional properties
  (pkceMandatory, pkceSupportPlain, bypassClientCredentials), then completing the actual authorization-code flow.
  The tests verify mandatory-challenge rejection, S256 and plain challenge behavior, public-client exchange without
  a client secret, and the returned access token's 3600-second lifetime. Three configurations run as both tenants.
  Runs in the standard key-manager block; ×2 tenant. Teardown via @cleanup removes the application (and keys).

  @cap:key-manager @feat:oauth-keys @rule:pkce @type:regression @legacy:PkceEnabledApplicationTestCase
  Scenario Outline: Complete the authorization-code flow with PKCE configuration <case> as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "pkceAppPayload"
    And I set the field "tokenType" to "JWT" in the payload "pkceAppPayload"
    And I create an application with payload "pkceAppPayload"
    Then The response status code should be 201

    # Generate PRODUCTION keys with the authorization_code grant, a callback URL, and the PKCE additionalProperties.
    When I put the following JSON payload in context as "pkceKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["authorization_code"], "callbackUrl": "https://localhost:9443/store/", "additionalProperties": <additionalProperties>}
    """
    And I generate client credentials for application id "createdAppId" with payload "pkceKeysPayload"
    Then The response status code should be 200
    # Assert the persisted PKCE flags structurally (immune to the keygen response's compact JSON formatting).
    And The value of response field "additionalProperties.pkceMandatory" should be "<pkceMandatory>"
    And The value of response field "additionalProperties.pkceSupportPlain" should be "<pkceSupportPlain>"
    And The value of response field "additionalProperties.bypassClientCredentials" should be "<bypassClientCredentials>"
    # Mandatory PKCE must reject an authorization request that omits the challenge.
    When I send a resident authorization request without a PKCE challenge
    Then the actual value of "authorizeErrorCode" should match the expected value:
      """
      invalid_request
      """
    # This completes authorize -> resource-owner login/consent -> code exchange; only the public-client case omits
    # HTTP Basic client authentication, while still identifying the client and presenting the PKCE verifier.
    When I complete the resident authorization-code flow with PKCE method "<pkceMethod>" and client authentication "<clientAuthentication>"
    Then The response status code should be 200
    And The response field "access_token" should be non-blank
    And The JWT stored as "generatedAccessToken" should have a validity of 3600 seconds

    Examples:
      | case                   | additionalProperties                                                                                                                     | pkceMandatory | pkceSupportPlain | bypassClientCredentials | pkceMethod | clientAuthentication | actor             |
      | mandatory              | {"pkceMandatory":"true","application_access_token_expiry_time":"3600"}                                                           | true          | false            | false                   | S256       | secret               | admin             |
      | mandatory              | {"pkceMandatory":"true","application_access_token_expiry_time":"3600"}                                                           | true          | false            | false                   | S256       | secret               | admin@tenant1.com |
      | mandatory+plain        | {"pkceMandatory":"true","pkceSupportPlain":"true","application_access_token_expiry_time":"3600"}                                | true          | true             | false                   | plain      | secret               | admin             |
      | mandatory+plain        | {"pkceMandatory":"true","pkceSupportPlain":"true","application_access_token_expiry_time":"3600"}                                | true          | true             | false                   | plain      | secret               | admin@tenant1.com |
      | mandatory+plain+bypass | {"pkceMandatory":"true","pkceSupportPlain":"true","bypassClientCredentials":"true","application_access_token_expiry_time":"3600"} | true        | true             | true                    | plain      | public               | admin             |
      | mandatory+plain+bypass | {"pkceMandatory":"true","pkceSupportPlain":"true","bypassClientCredentials":"true","application_access_token_expiry_time":"3600"} | true        | true             | true                    | plain      | public               | admin@tenant1.com |

  @cap:key-manager @feat:oauth-keys @rule:pkce @type:negative @legacy:PkceEnabledApplicationTestCase
  Scenario Outline: Reject plain PKCE when plain support is disabled as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "pkceAppPayload"
    And I set the field "tokenType" to "JWT" in the payload "pkceAppPayload"
    And I create an application with payload "pkceAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "pkceKeysPayload"
      """
      {"keyType":"PRODUCTION","grantTypesToBeSupported":["authorization_code"],"callbackUrl":"https://localhost:9443/store/","additionalProperties":{"pkceMandatory":"true","application_access_token_expiry_time":"3600"}}
      """
    And I generate client credentials for application id "createdAppId" with payload "pkceKeysPayload"
    Then The response status code should be 200
    And The value of response field "additionalProperties.pkceMandatory" should be "true"
    And The value of response field "additionalProperties.pkceSupportPlain" should be "false"
    When I send a resident authorization request with PKCE method "plain"
    Then the actual value of "authorizeErrorCode" should match the expected value:
      """
      invalid_request
      """

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
