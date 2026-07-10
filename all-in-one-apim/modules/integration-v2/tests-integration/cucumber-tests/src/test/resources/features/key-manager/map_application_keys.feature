@cleanup
Feature: Key Manager Map Application Keys (BYO OAuth client)

  Ports the map-application-keys arc of the legacy ApplicationTestCase (mapApplicationKeys /
  mapApplicationKeysNegative): binding a pre-existing ("bring your own") OAuth client's consumer key/secret
  to a Developer Portal application via /devportal/applications/{id}/map-keys against the resident Key
  Manager. The legacy test registered the BYO client through the SOAP OAuthAdminService + a ServiceProvider;
  here the v2-native DCR endpoint produces the equivalent real OAuth2 consumer app in the resident IS, so no
  external Key Manager backend is required (the resident KM ships in every pack). Covers: the positive map
  (a fresh application + a BYO client → mapped keys, 200) and the negative (an application that already has
  generated PRODUCTION keys rejects a second mapping for the same key type with 409 "Key Mappings already
  exists"). Runs x2 tenant (super + tenant) — the resident KM is a per-tenant registry — even though the
  legacy Factory only exercised the super tenant. Teardown via @cleanup removes the application (and its
  keys/mappings); the BYO DCR clients are self-contained OAuth apps left to the disposable container.

  @cap:key-manager @feat:map-application-keys @type:regression @legacy:ApplicationTestCase
  Scenario Outline: Map a pre-existing OAuth client to an application via the resident key manager as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I register an OAuth client "byoMapClient" as "byo"
    Then The response status code should be 200
    When I map OAuth client "byo" to application "createdAppId" via key manager "Resident Key Manager"
    Then The response status code should be 200
    And The response should contain "consumerKey"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:key-manager @feat:map-application-keys @type:negative @legacy:ApplicationTestCase
  Scenario Outline: Mapping keys onto an application that already has generated keys is rejected as <actor>
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
    When I register an OAuth client "byoMapClientNeg" as "byoNeg"
    Then The response status code should be 200
    When I map OAuth client "byoNeg" to application "createdAppId" via key manager "Resident Key Manager"
    Then The response status code should be 409
    And The response should contain "Key Mappings already exists"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
