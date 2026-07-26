@cleanup
Feature: Key Manager PKCE-Enabled Keys

  Ports PkceEnabledApplicationTestCase — generating application OAuth keys with PKCE additional properties
  (pkceMandatory, pkceSupportPlain, bypassClientCredentials) persists and reflects those flags in the key
  response. Three combinations: PKCE mandatory only; mandatory + plain-text challenge support; mandatory +
  plain + bypass-client-secret. Each key is generated for the authorization_code grant with a callback URL.
  Runs in the standard key-manager block; ×2 tenant. Teardown via @cleanup removes the application (and keys).

  @cap:key-manager @feat:oauth-keys @rule:pkce @type:regression @legacy:PkceEnabledApplicationTestCase
  Scenario Outline: Generate application keys with PKCE additional properties (<case>) as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "pkceAppPayload"
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

    Examples:
      | case                       | additionalProperties                                                             | pkceMandatory | pkceSupportPlain | bypassClientCredentials | actor             |
      | mandatory                  | {"pkceMandatory":"true"}                                                          | true          | false            | false                   | admin             |
      | mandatory                  | {"pkceMandatory":"true"}                                                          | true          | false            | false                   | admin@tenant1.com |
      | mandatory+plain            | {"pkceMandatory":"true","pkceSupportPlain":"true"}                                | true          | true             | false                   | admin             |
      | mandatory+plain            | {"pkceMandatory":"true","pkceSupportPlain":"true"}                                | true          | true             | false                   | admin@tenant1.com |
      | mandatory+plain+bypass     | {"pkceMandatory":"true","pkceSupportPlain":"true","bypassClientCredentials":"true"} | true        | true             | true                    | admin             |
      | mandatory+plain+bypass     | {"pkceMandatory":"true","pkceSupportPlain":"true","bypassClientCredentials":"true"} | true        | true             | true                    | admin@tenant1.com |
