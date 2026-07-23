@cleanup
Feature: Admin Key Manager Configuration

  Ports the legacy KeyManagersTestCase (38 methods): admin-plane key-manager registration CRUD over
  /api/am/admin/v4/key-managers, across the connector types {Auth0, WSO2-IS, KeyCloak, Okta, PingFederate,
  Forgerock, WSO2-IS-7}. The legacy per-type {add / add-missing-mandatory / add-optional / get / update / delete}
  matrix is consolidated into: one CRUD arc as a Scenario Outline over the type payloads (some carry only the
  mandatory fields, some also carry optional description + JWKS certificates, so both paths are covered by the
  outline); a missing-connector-config negative (400) over all types; and a duplicate-name negative (409). This is
  pure config CRUD — the connector endpoints are stored, not contacted, so no external connectivity is needed
  (verified). Runs ×2 tenant (super + tenant) for parity with the legacy Factory; KM registration is a
  per-tenant admin registry. Teardown via @cleanup deletes any created key manager with the admin token.
  The WSO2 Identity Server 7 connector (type WSO2-IS-7) is a distinct type with mandatory IS7-specific connector
  config (api_resource_management_endpoint, is7_roles_endpoint) and Basic-vs-Mutual-TLS auth; its type-specific
  config paths are covered by the three IS7 scenarios below in addition to the shared CRUD/negative outlines.
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
      | type         | payload                                            | actor |
      | Auth0        | artifacts/payloads/keymanagers/auth0.json          | admin |
      | WSO2-IS      | artifacts/payloads/keymanagers/wso2is.json         | admin |
      | KeyCloak     | artifacts/payloads/keymanagers/keycloak.json       | admin |
      | Okta         | artifacts/payloads/keymanagers/okta.json           | admin |
      | PingFederate | artifacts/payloads/keymanagers/pingfederate.json   | admin |
      | Forgerock    | artifacts/payloads/keymanagers/forgerock.json      | admin |
      | WSO2-IS-7    | artifacts/payloads/keymanagers/wso2is7-config.json | admin |

    Examples: Tenant
      | type         | payload                                            | actor             |
      | Auth0        | artifacts/payloads/keymanagers/auth0.json          | admin@tenant1.com |
      | WSO2-IS      | artifacts/payloads/keymanagers/wso2is.json         | admin@tenant1.com |
      | KeyCloak     | artifacts/payloads/keymanagers/keycloak.json       | admin@tenant1.com |
      | Okta         | artifacts/payloads/keymanagers/okta.json           | admin@tenant1.com |
      | PingFederate | artifacts/payloads/keymanagers/pingfederate.json   | admin@tenant1.com |
      | Forgerock    | artifacts/payloads/keymanagers/forgerock.json      | admin@tenant1.com |
      | WSO2-IS-7    | artifacts/payloads/keymanagers/wso2is7-config.json | admin@tenant1.com |

  @cap:admin @feat:key-manager-config @type:negative @legacy:KeyManagersTestCase
  Scenario Outline: Creating a <type> key manager without its connector config is rejected as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I attempt to create a key manager from payload "<payload>" without connector config
    Then The response status code should be 400

    Examples: Super tenant
      | type         | payload                                            | actor |
      | Auth0        | artifacts/payloads/keymanagers/auth0.json          | admin |
      | WSO2-IS      | artifacts/payloads/keymanagers/wso2is.json         | admin |
      | KeyCloak     | artifacts/payloads/keymanagers/keycloak.json       | admin |
      | Okta         | artifacts/payloads/keymanagers/okta.json           | admin |
      | PingFederate | artifacts/payloads/keymanagers/pingfederate.json   | admin |
      | Forgerock    | artifacts/payloads/keymanagers/forgerock.json      | admin |
      | WSO2-IS-7    | artifacts/payloads/keymanagers/wso2is7-config.json | admin |

    Examples: Tenant
      | type         | payload                                            | actor             |
      | Auth0        | artifacts/payloads/keymanagers/auth0.json          | admin@tenant1.com |
      | WSO2-IS      | artifacts/payloads/keymanagers/wso2is.json         | admin@tenant1.com |
      | KeyCloak     | artifacts/payloads/keymanagers/keycloak.json       | admin@tenant1.com |
      | Okta         | artifacts/payloads/keymanagers/okta.json           | admin@tenant1.com |
      | PingFederate | artifacts/payloads/keymanagers/pingfederate.json   | admin@tenant1.com |
      | Forgerock    | artifacts/payloads/keymanagers/forgerock.json      | admin@tenant1.com |
      | WSO2-IS-7    | artifacts/payloads/keymanagers/wso2is7-config.json | admin@tenant1.com |

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

  # WSO2-IS-7-specific connector config: the IS7 connector requires two IS7-only endpoints
  # (api_resource_management_endpoint, is7_roles_endpoint). Omitting them is rejected with 400 (901401).
  @cap:admin @feat:key-manager-config @type:negative @legacy:KeyManagersTestCase
  Scenario Outline: Creating a WSO2-IS-7 key manager without its IS7 role/resource endpoints is rejected as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I attempt to create a key manager from payload "artifacts/payloads/keymanagers/wso2is7-config-missing-is7-endpoints.json"
    Then The response status code should be 400

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # The IS7 connector can create APIM roles in IS7 (enable_roles_creation); the setting must round-trip.
  @cap:admin @feat:key-manager-config @type:regression @legacy:KeyManagersTestCase
  Scenario Outline: A WSO2-IS-7 key manager persists the create-roles-in-IS7 setting as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create a key manager from payload "artifacts/payloads/keymanagers/wso2is7-config-roles-creation.json" as "rolesKmId"
    Then The response status code should be 201
    When I retrieve the key manager "rolesKmId"
    Then The response status code should be 200
    And The response should contain "enable_roles_creation"
    When I delete the key manager "rolesKmId"
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # The IS7 connector authenticates its management calls to IS via Basic auth (Username/Password, covered by the
  # base CRUD payload) OR Mutual TLS. The MTLS config requires the discriminator Authentication=MutualTLS plus
  # MutualTLSOptions (ServerWide/TenantWide) and IdentityUser (NOT Username/Password); this must round-trip.
  @cap:admin @feat:key-manager-config @type:regression @legacy:KeyManagersTestCase
  Scenario Outline: A WSO2-IS-7 key manager persists its Mutual-TLS authentication config as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create a key manager from payload "artifacts/payloads/keymanagers/wso2is7-config-mtls.json" as "mtlsKmId"
    Then The response status code should be 201
    When I retrieve the key manager "mtlsKmId"
    Then The response status code should be 200
    And The response should contain "MutualTLS"
    When I delete the key manager "mtlsKmId"
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Mutual-TLS auth requires the IdentityUser (the tenant-qualified IS user the mTLS calls act as). Selecting
  # Authentication=MutualTLS without IdentityUser is an invalid connector config and must be rejected with 400.
  @cap:admin @feat:key-manager-config @type:negative @legacy:KeyManagersTestCase
  Scenario Outline: Creating a WSO2-IS-7 key manager with Mutual-TLS but no IdentityUser is rejected as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I attempt to create a key manager from payload "artifacts/payloads/keymanagers/wso2is7-config-mtls-no-identity-user.json"
    Then The response status code should be 400

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # The IS7 connector accepts a mandatory-only config (its required endpoints + IS7 connector endpoints + auth),
  # with the optional description / extra endpoints / certificates / flags omitted; it must still persist.
  @cap:admin @feat:key-manager-config @type:regression @legacy:KeyManagersTestCase
  Scenario Outline: A WSO2-IS-7 key manager with only the mandatory config persists as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create a key manager from payload "artifacts/payloads/keymanagers/wso2is7-config-minimal.json" as "minimalKmId"
    Then The response status code should be 201
    When I retrieve the key manager "minimalKmId"
    Then The response status code should be 200
    And The response should contain "WSO2-IS-7"
    When I delete the key manager "minimalKmId"
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # The full IS7 grant-type set (incl. saml2-bearer, device_code, jwt-bearer, token-exchange) must be stored and
  # returned intact - a token-exchange KM relies on that grant surviving the round-trip.
  @cap:admin @feat:key-manager-config @type:regression @legacy:KeyManagersTestCase
  Scenario Outline: A WSO2-IS-7 key manager persists the full IS7 grant-type set as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create a key manager from payload "artifacts/payloads/keymanagers/wso2is7-config-grant-types.json" as "grantTypesKmId"
    Then The response status code should be 201
    When I retrieve the key manager "grantTypesKmId"
    Then The response status code should be 200
    And The response should contain "urn:ietf:params:oauth:grant-type:token-exchange"
    And The response should contain "urn:ietf:params:oauth:grant-type:saml2-bearer"
    When I delete the key manager "grantTypesKmId"
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # The IS7 connector's API-invocation method (KM tokenType) can be Token Exchange (EXCHANGED). Selecting it, with
  # a signature-validation certificate (JWKS here) and the alias, registers a trusted token issuer and the
  # EXCHANGED tokenType must round-trip. (The DIRECT default is exercised by the base CRUD scenarios.)
  @cap:admin @feat:key-manager-config @type:regression @legacy:KeyManagersTestCase
  Scenario Outline: A WSO2-IS-7 key manager persists the token-exchange (EXCHANGED) invocation method as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create a key manager from payload "artifacts/payloads/keymanagers/wso2is7-config-token-exchange.json" as "teKmId"
    Then The response status code should be 201
    When I retrieve the key manager "teKmId"
    Then The response status code should be 200
    And The value of response field "tokenType" should be "EXCHANGED"
    When I delete the key manager "teKmId"
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Signature-validation config is NOT enforced at create time for a Token-Exchange (EXCHANGED) key manager:
  # selecting EXCHANGED with neither a PEM certificate nor a JWKS endpoint is accepted and the config persists
  # (201). The missing signer surfaces only at runtime, when a subject token cannot be verified - so this pins
  # that the neither-PEM-nor-JWKS case is a permissive create, not a create-time rejection.
  @cap:admin @feat:key-manager-config @type:regression @legacy:KeyManagersTestCase
  Scenario Outline: A WSO2-IS-7 token-exchange key manager without a signature certificate is accepted at create time as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create a key manager from payload "artifacts/payloads/keymanagers/wso2is7-config-te-no-cert.json" as "teNoCertKmId"
    Then The response status code should be 201
    And The value of response field "tokenType" should be "EXCHANGED"
    When I delete the key manager "teNoCertKmId"
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # The created IS7 key manager appears in the key-manager list with its WSO2-IS-7 type.
  @cap:admin @feat:key-manager-config @type:regression @legacy:KeyManagersTestCase
  Scenario Outline: The key manager list includes a created WSO2-IS-7 key manager as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create a key manager from payload "artifacts/payloads/keymanagers/wso2is7-config.json" as "listKmId"
    Then The response status code should be 201
    When I retrieve all key managers
    Then The response status code should be 200
    And The response should contain "WSO2-IS-7"
    When I delete the key manager "listKmId"
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # JWT signature validation can be configured with a JWKS URL (covered by the base CRUD payload) OR a PEM
  # certificate; the PEM-certificate variant must also persist.
  @cap:admin @feat:key-manager-config @type:regression @legacy:KeyManagersTestCase
  Scenario Outline: A WSO2-IS-7 key manager persists a PEM certificate configuration as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create a key manager from payload "artifacts/payloads/keymanagers/wso2is7-config-pem.json" as "pemKmId"
    Then The response status code should be 201
    When I retrieve the key manager "pemKmId"
    Then The response status code should be 200
    And The response should contain "PEM"
    When I delete the key manager "pemKmId"
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # The enabled flag is a control-plane toggle that must round-trip: disabling then re-enabling the KM persists
  # the value each way (asserted as an exact field equality, not a substring).
  @cap:admin @feat:key-manager-config @type:regression @legacy:KeyManagersTestCase
  Scenario Outline: A WSO2-IS-7 key manager's enabled state can be toggled off and on as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create a key manager from payload "artifacts/payloads/keymanagers/wso2is7-config.json" as "toggleKmId"
    Then The response status code should be 201
    When I update the key manager "toggleKmId" setting its enabled state to "false"
    Then The response status code should be 200
    When I retrieve the key manager "toggleKmId"
    Then The response status code should be 200
    And The value of response field "enabled" should be "false"
    When I update the key manager "toggleKmId" setting its enabled state to "true"
    Then The response status code should be 200
    When I retrieve the key manager "toggleKmId"
    Then The response status code should be 200
    And The value of response field "enabled" should be "true"
    When I delete the key manager "toggleKmId"
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Key manager names must be unique within a tenant; creating a second WSO2-IS-7 KM whose name collides with an
  # existing one is rejected with 409 (the IS7-typed instance of the duplicate-name rule).
  @cap:admin @feat:key-manager-config @type:negative @legacy:KeyManagersTestCase
  Scenario Outline: Creating a WSO2-IS-7 key manager with a name that already exists is rejected as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create a key manager from payload "artifacts/payloads/keymanagers/wso2is7-config.json" as "dupIs7Km"
    Then The response status code should be 201
    When I attempt to create a key manager from payload "artifacts/payloads/keymanagers/wso2is7-config.json" with name "{{dupIs7KmName}}"
    Then The response status code should be 409

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Pins actual behavior: the admin API does NOT validate the connector endpoint URL format at create. Malformed /
  # non-tenant-qualified URLs (no scheme, "not-a-valid-url") are accepted with 201 and stored verbatim - validation
  # (if any) is deferred to runtime use, not enforced at config time.
  @cap:admin @feat:key-manager-config @type:regression @legacy:KeyManagersTestCase
  Scenario Outline: A WSO2-IS-7 key manager with malformed endpoint URLs is accepted and stored verbatim as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create a key manager from payload "artifacts/payloads/keymanagers/wso2is7-config-malformed-endpoints.json" as "malformedKmId"
    Then The response status code should be 201
    When I retrieve the key manager "malformedKmId"
    Then The response status code should be 200
    And The value of response field "tokenEndpoint" should be "is7.example.com:9444/oauth2/token"
    And The value of response field "issuer" should be "not-a-valid-url"
    When I delete the key manager "malformedKmId"
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # The connector type is immutable: updating an existing WSO2-IS-7 key manager to a different type is rejected
  # with 400 (pins the actual behavior - rejected, not silently ignored).
  @cap:admin @feat:key-manager-config @type:negative @legacy:KeyManagersTestCase
  Scenario Outline: Updating a WSO2-IS-7 key manager's connector type is rejected as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create a key manager from payload "artifacts/payloads/keymanagers/wso2is7-config.json" as "typeKmId"
    Then The response status code should be 201
    When I update the key manager "typeKmId" setting its type to "Okta"
    Then The response status code should be 400
    When I delete the key manager "typeKmId"
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Tenant isolation: a WSO2-IS-7 key manager created in tenant1's org is not visible to the super-tenant admin -
  # fetching it by id from carbon.super returns 404 (KM config is org-scoped). Created as admin@tenant1.com and
  # (per ResourceCleanup) torn down as that same actor, so ending the scenario as the super admin is safe.
  @cap:admin @feat:key-manager-config @type:negative @legacy:KeyManagersTestCase
  Scenario: A WSO2-IS-7 key manager created in a tenant is not visible from another tenant
    Given The system is ready
    And I have valid access tokens as "admin@tenant1.com"
    When I create a key manager from payload "artifacts/payloads/keymanagers/wso2is7-config.json" as "tenantKmId"
    Then The response status code should be 201
    And I have valid access tokens as "admin"
    When I retrieve the key manager "tenantKmId"
    Then The response status code should be 404

  # Authorization: the key-manager admin API requires apim:admin. A non-admin actor (publisherUser, whose token
  # carries publisher scopes only) attempting to create a KM is rejected with 401 ("Unauthenticated request") - the
  # scope gate fires before any connector logic, so create is representative of update/delete too.
  @cap:admin @feat:key-manager-config @type:negative @legacy:KeyManagersTestCase
  Scenario Outline: A non-admin actor cannot create a WSO2-IS-7 key manager as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I attempt to create a key manager from payload "artifacts/payloads/keymanagers/wso2is7-config.json" using the publisher token
    Then The response status code should be 401

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |
