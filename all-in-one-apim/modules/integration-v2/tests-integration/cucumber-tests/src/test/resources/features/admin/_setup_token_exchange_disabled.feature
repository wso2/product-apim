@setup
Feature: Setup - token-exchange disabled negative fixture

  Provisions the minimal fixture for the grant-disabled negative in BOTH tenants: a Developer Portal application
  with Resident-Key-Manager keys. No API, subscription or external IS is needed - the token endpoint refuses the
  disabled grant before any subject token is processed, so the scenario only needs application credentials to
  authenticate the (rejected) token request. Each tenant's credentials are stashed under a tenant-suffixed key.

  Scenario: Provision the disabled-grant application in both tenants
    Given The system is ready

    And I have valid access tokens as "admin"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app_oauth.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "generateApplicationKeysPayload"
    """
    {"keyType": "PRODUCTION", "keyManager": "Resident Key Manager", "grantTypesToBeSupported": ["client_credentials"], "validityTime": 3600}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateApplicationKeysPayload"
    Then The response status code should be 200
    And I stash the token-exchange fixture for the acting tenant

    And I have valid access tokens as "admin@tenant1.com"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app_oauth.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "generateApplicationKeysPayload"
    """
    {"keyType": "PRODUCTION", "keyManager": "Resident Key Manager", "grantTypesToBeSupported": ["client_credentials"], "validityTime": 3600}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateApplicationKeysPayload"
    Then The response status code should be 200
    And I stash the token-exchange fixture for the acting tenant
