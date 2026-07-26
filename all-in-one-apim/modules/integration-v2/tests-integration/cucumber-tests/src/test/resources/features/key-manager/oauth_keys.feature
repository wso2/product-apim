@cleanup
Feature: Key Manager OAuth Application Keys

  Key-manager-plane OAuth key generation: generate production consumer credentials for an application and
  exchange them for an application access token. This is the key-generation arc factored out of
  devportal/applications. Runs as admin in both the super tenant and tenant1.com. Teardown via the
  per-scenario cleanup hook.

  @cap:key-manager @feat:oauth-keys @type:smoke @legacy:ApplicationKeyGenerationTestCase
  Scenario Outline: Generate OAuth keys for an application and obtain an access token as <actor>
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
    And The response should contain "consumerKey"
    And The response should contain "consumerSecret"

    When I put the following JSON payload in context as "createApplicationAccessTokenPayload"
    """
    {"consumerSecret": "{{appConsumerSecret}}", "validityPeriod": 3600}
    """
    And I request an access token for application id "createdAppId" using payload "createApplicationAccessTokenPayload"
    Then The response status code should be 200
    And The response should contain "accessToken"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # @legacy:APIMANAGER5327... — the PGSQL partial-key-cleanup regression (APIMANAGER-5327) is the SAME behaviour:
  # generate keys, then clean up the registration without error. The legacy test switched to a live PostgreSQL
  # datasource and hit a removed Jaggery endpoint (cleanUpApplicationRegistration.jag); this modern REST scenario
  # is DB-agnostic, so when the suite is matrixed onto PostgreSQL it exercises the same cleanup path on PGSQL.
  @cap:key-manager @feat:oauth-keys @type:regression @legacy:ApplicationTestCase @legacy:APIMANAGER5327KeyGenerationWithPGSQLTestCase
  Scenario Outline: Clean up an application's key registration as <actor>
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

    When I clean up the key registration for application "createdAppId" with key mapping "keyMappingId"
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
