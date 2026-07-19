@cleanup
Feature: Key Manager Multiple Client Secrets

  Ports the consumer-secret management delta of the legacy ApplicationTestCase (10 methods): additional client
  secrets per application key mapping, over /devportal/applications/{id}/oauth-keys/{keyMappingId}/{generate-,
  revoke-}secret and /secrets. Multiple-client-secrets mode is ENABLED BY DEFAULT in the 4.7.0 pack
  (default.json: oauth.multiple_client_secrets.enable = true — verified live: the feature passes on the default
  config with no overlay), so this runs in the standard key-manager block with no special config. (The legacy
  tests skip on 900916, which only occurs when the mode is disabled — not the default here.) Covers: fetch key
  details by key-mapping id, then generate → list → revoke additional secrets (a helper secret is generated
  before revoking the target because the IS refuses to revoke the most-recently-added secret), and generation
  with a minimal (empty) payload. ×2 tenant. Teardown via @cleanup removes the application (and its keys/secrets).

  @cap:key-manager @feat:multiple-client-secrets @type:regression @legacy:ApplicationTestCase
  Scenario Outline: Generate, list and revoke additional consumer secrets for a key mapping as <actor>
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

    When I fetch the oauth key details for application "createdAppId" with key mapping "keyMappingId"
    Then The response status code should be 200
    And The response should contain "consumerKey"

    When I generate a consumer secret with description "primary extra secret" for application "createdAppId" with key mapping "keyMappingId" as "secretA"
    Then The response status code should be 201
    And The response should contain "secretValue"

    When I retrieve the consumer secrets for application "createdAppId" with key mapping "keyMappingId"
    Then The response status code should be 200
    And The response should contain "{{secretA}}"

    When I generate a consumer secret with description "helper secret" for application "createdAppId" with key mapping "keyMappingId" as "secretB"
    Then The response status code should be 201

    When I revoke the consumer secret "secretA" for application "createdAppId" with key mapping "keyMappingId"
    Then The response status code should be 204

    When I retrieve the consumer secrets for application "createdAppId" with key mapping "keyMappingId"
    Then The response status code should be 200
    And The response should not contain "{{secretA}}"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:key-manager @feat:multiple-client-secrets @type:regression @legacy:ApplicationTestCase
  Scenario Outline: Generate a consumer secret with a minimal payload as <actor>
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
    When I generate a consumer secret with description "" for application "createdAppId" with key mapping "keyMappingId" as "minSecret"
    Then The response status code should be 201
    And The response should contain "secretValue"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports the SANDBOX-key gap of ApplicationTestCase — additional consumer secrets can be generated, listed and
  # revoked for a SANDBOX key mapping exactly as for PRODUCTION. (The PRODUCTION path is covered above; this pins
  # the same lifecycle on the sandbox key type.)
  @cap:key-manager @feat:multiple-client-secrets @rule:sandbox-key @type:regression @legacy:ApplicationTestCase
  Scenario Outline: Generate, list and revoke additional consumer secrets for a SANDBOX key mapping as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "sbAppPayload"
    And I create an application with payload "sbAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "sbKeysPayload"
    """
    {"keyType": "SANDBOX", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "sbKeysPayload"
    Then The response status code should be 200

    When I generate a consumer secret with description "sandbox extra secret" for application "createdAppId" with key mapping "keyMappingId" as "sbSecretA"
    Then The response status code should be 201
    When I retrieve the consumer secrets for application "createdAppId" with key mapping "keyMappingId"
    Then The response status code should be 200
    And The response should contain "{{sbSecretA}}"
    When I generate a consumer secret with description "sandbox helper secret" for application "createdAppId" with key mapping "keyMappingId" as "sbSecretB"
    Then The response status code should be 201
    When I revoke the consumer secret "sbSecretA" for application "createdAppId" with key mapping "keyMappingId"
    Then The response status code should be 204
    When I retrieve the consumer secrets for application "createdAppId" with key mapping "keyMappingId"
    Then The response status code should be 200
    And The response should not contain "{{sbSecretA}}"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports MultipleClientSecretsTokenTestCase — two ADDITIONAL secrets on the same key mapping BOTH yield valid
  # tokens, and tokens minted with different secrets carry the SAME application identity (equal JWT sub claim). A
  # JWT-token-type application is used so the issued token is a decodable JWT (an opaque token has no sub). Proves
  # additional secrets are alternative credentials for one application, not separate identities.
  @cap:key-manager @feat:multiple-client-secrets @rule:token-identity @type:regression @legacy:MultipleClientSecretsTokenTestCase
  Scenario Outline: Tokens from two additional secrets are both valid and share the application identity as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "tsAppPayload"
    And I set the field "tokenType" to "JWT" in the payload "tsAppPayload"
    And I create an application with payload "tsAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "tsKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "tsKeysPayload"
    Then The response status code should be 200

    # First additional secret → capture its value.
    When I generate a consumer secret with description "token-secret-1" for application "createdAppId" with key mapping "keyMappingId" as "tsSecret1"
    Then The response status code should be 201
    And I extract response field "secretValue" and store it as "tsSecret1Value"
    # Second additional secret → capture its value.
    When I generate a consumer secret with description "token-secret-2" for application "createdAppId" with key mapping "keyMappingId" as "tsSecret2"
    Then The response status code should be 201
    And I extract response field "secretValue" and store it as "tsSecret2Value"

    # Both secrets yield valid tokens.
    When I request a client-credentials token using consumer key "consumerKey" and secret "tsSecret1Value"
    Then The response status code should be 200
    And I extract response field "access_token" and store it as "tsToken1"
    When I request a client-credentials token using consumer key "consumerKey" and secret "tsSecret2Value"
    Then The response status code should be 200
    And I extract response field "access_token" and store it as "tsToken2"

    # Both tokens carry the same application identity (equal sub claim).
    When I extract JWT claim "sub" from access token "tsToken1" and store it as "tsSub1"
    And I extract JWT claim "sub" from access token "tsToken2" and store it as "tsSub2"
    Then The stored value "tsSub1" should equal "tsSub2"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
