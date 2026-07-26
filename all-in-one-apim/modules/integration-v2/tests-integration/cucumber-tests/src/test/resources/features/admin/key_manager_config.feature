@cleanup
Feature: Admin Key Manager Configuration

  Ports the legacy KeyManagersTestCase (38 methods): admin-plane key-manager registration CRUD over
  /api/am/admin/v4/key-managers, across the six connector types {Auth0, WSO2-IS, KeyCloak, Okta, PingFederate,
  Forgerock}. The legacy per-type {add / add-missing-mandatory / add-optional / get / update / delete} matrix is
  consolidated into: one CRUD arc as a Scenario Outline over the six type payloads (3 payloads carry only the
  mandatory fields, 3 also carry optional description + JWKS certificates, so both paths are covered by the
  outline); a missing-connector-config negative (400) over all six; and a duplicate-name negative (409). This is
  pure config CRUD — the connector endpoints are stored, not contacted, so no external connectivity is needed
  (verified). Runs ×2 tenant (super + tenant) for parity with the legacy Factory; KM registration is a
  per-tenant admin registry. Teardown via @cleanup deletes any created key manager with the admin token.
  Deferred to increment 2: key-manager permissions (a DENY-role KM → a user in that role is refused key
  generation with 403 — needs a second role/user and the store key-generation-with-key-manager flow).

  @cap:admin @feat:key-manager-config @type:regression @legacy:KeyManagersTestCase
  Scenario Outline: Key manager CRUD for the <type> connector type as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create a key manager from payload "<payload>" as "kmId"
    Then The response status code should be 201
    When I retrieve the key manager "kmId"
    Then The response status code should be 200
    And The response should contain "<type>"
    When I update the key manager "kmId" setting its description to "Updated KM description"
    Then The response status code should be 200
    And The response should contain "Updated KM description"
    When I delete the key manager "kmId"
    Then The response status code should be 200
    When I retrieve the key manager "kmId"
    Then The response status code should be 404

    Examples: Super tenant
      | type         | payload                                          | actor |
      | Auth0        | artifacts/payloads/keymanagers/auth0.json        | admin |
      | WSO2-IS      | artifacts/payloads/keymanagers/wso2is.json       | admin |
      | KeyCloak     | artifacts/payloads/keymanagers/keycloak.json     | admin |
      | Okta         | artifacts/payloads/keymanagers/okta.json         | admin |
      | PingFederate | artifacts/payloads/keymanagers/pingfederate.json | admin |
      | Forgerock    | artifacts/payloads/keymanagers/forgerock.json    | admin |

    Examples: Tenant
      | type         | payload                                          | actor             |
      | Auth0        | artifacts/payloads/keymanagers/auth0.json        | admin@tenant1.com |
      | WSO2-IS      | artifacts/payloads/keymanagers/wso2is.json       | admin@tenant1.com |
      | KeyCloak     | artifacts/payloads/keymanagers/keycloak.json     | admin@tenant1.com |
      | Okta         | artifacts/payloads/keymanagers/okta.json         | admin@tenant1.com |
      | PingFederate | artifacts/payloads/keymanagers/pingfederate.json | admin@tenant1.com |
      | Forgerock    | artifacts/payloads/keymanagers/forgerock.json    | admin@tenant1.com |

  @cap:admin @feat:key-manager-config @type:negative @legacy:KeyManagersTestCase
  Scenario Outline: Creating a <type> key manager without its connector config is rejected as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I attempt to create a key manager from payload "<payload>" without connector config
    Then The response status code should be 400

    Examples: Super tenant
      | type         | payload                                          | actor |
      | Auth0        | artifacts/payloads/keymanagers/auth0.json        | admin |
      | WSO2-IS      | artifacts/payloads/keymanagers/wso2is.json       | admin |
      | KeyCloak     | artifacts/payloads/keymanagers/keycloak.json     | admin |
      | Okta         | artifacts/payloads/keymanagers/okta.json         | admin |
      | PingFederate | artifacts/payloads/keymanagers/pingfederate.json | admin |
      | Forgerock    | artifacts/payloads/keymanagers/forgerock.json    | admin |

    Examples: Tenant
      | type         | payload                                          | actor             |
      | Auth0        | artifacts/payloads/keymanagers/auth0.json        | admin@tenant1.com |
      | WSO2-IS      | artifacts/payloads/keymanagers/wso2is.json       | admin@tenant1.com |
      | KeyCloak     | artifacts/payloads/keymanagers/keycloak.json     | admin@tenant1.com |
      | Okta         | artifacts/payloads/keymanagers/okta.json         | admin@tenant1.com |
      | PingFederate | artifacts/payloads/keymanagers/pingfederate.json | admin@tenant1.com |
      | Forgerock    | artifacts/payloads/keymanagers/forgerock.json    | admin@tenant1.com |

  @cap:admin @feat:key-manager-config @type:negative @legacy:KeyManagersTestCase
  Scenario Outline: Creating a key manager with an existing name is rejected as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create a key manager from payload "artifacts/payloads/keymanagers/auth0.json" as "existKm"
    Then The response status code should be 201
    When I attempt to create a key manager from payload "artifacts/payloads/keymanagers/auth0.json" with name "{{existKmName}}"
    Then The response status code should be 409

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
