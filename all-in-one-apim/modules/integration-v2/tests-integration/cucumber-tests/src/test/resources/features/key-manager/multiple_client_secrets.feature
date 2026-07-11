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
