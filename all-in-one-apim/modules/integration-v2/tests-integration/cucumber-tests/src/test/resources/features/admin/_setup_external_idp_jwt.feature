@setup
Feature: Setup - external-IdP self-validated JWT fixture

  Provisions the external-IdP fixture in BOTH tenants (carbon.super and tenant1.com) so the scenarios run x2
  tenant: per tenant TWO external key managers (each self-validating JWTs against a committed PEM certificate of
  its own key pair, with its own issuer and its own remote claim namespace), an API deployed to the
  header-reflecting backend and published, a Developer Portal application whose Resident-KM keys carry the
  token-exchange grant (so the same external JWT can also be EXCHANGED at API Manager's token endpoint), a
  subscription, and one mapped consumer key per external key manager - the value the external JWT's azp claim
  carries, which is how the gateway resolves the application for a token API Manager did not issue.
  Each tenant's fixture is stashed under a tenant-suffixed key (reusing the token-exchange stash/select steps,
  whose fixture key list covers these keys too) so a scenario selects its acting tenant's fixture.
  Asserts only create success; created ids are registered for the runner's AfterClass cleanup as the creating
  actor (applications sweep before key managers, so the key-manager delete is FK-safe).

  Both key managers are created with tokenType BOTH and enableProvisionedAppValidation=false, and both matter:
   * BOTH (rather than the DIRECT default) so the trusted IdP - which is where the ALIAS lives, since the alias is
     not a column of AM_KEY_MANAGER - exists from the start; the scenarios then set whatever tokenType they need.
   * enableProvisionedAppValidation=false so map-keys does NOT try to look the consumer key up at the IdP. That
     is a first-class field, not an additionalProperties entry: the admin mapping OVERWRITES
     additionalProperties.provisionedAppValidation with the field's value, which defaults to true - and with the
     lookup on, map-keys 500s with UnknownHostException against these deliberately non-existent IdPs.

  ORDERING IS DELIBERATE: both key managers are registered FIRST and the key MAPPINGS are made LAST. Mapping a
  consumer key needs the key manager to have reached the runtime key-manager HOLDER, and that propagation is
  asynchronous - inside the window map-keys answers 400 / 901403 "Key Manager not Registered".
  The ordering alone is NOT sufficient, which was MEASURED rather than assumed: an earlier revision relied on the
  multi-second API create/deploy/publish arc in between to cover the window, and the tenant fixture - provisioned
  straight after the super-tenant one, so every warm-up cost was already paid - lost the race anyway with ~5s
  between register and map, taking all ten of that tenant's scenarios down with it
  (/tmp/w7-run8-baseline.log: 10 failures, one root cause). So the mappings use the map-keys variant that RETRIES
  until the key manager is operational. The wait-until-operational CREATE variant is still unusable here - its
  probe is a key generation, which these key managers refuse by design (enableOAuthAppCreation=false).

  Scenario: Provision the external-IdP fixtures for both tenants
    Given The system is ready

    # --- super tenant fixture ---
    And I have valid access tokens as "admin"
    When I create a key manager from payload "artifacts/payloads/keymanagers/external-idp-1.json" as "extIdpKm1"
    Then The response status code should be 201
    # The signing certificate is installed after create rather than inlined in the payload: the payload would have
    # to carry an opaque base64 blob, while this step reads the committed PEM whose private key signs the tokens.
    When I update the key manager "extIdpKm1" setting its PEM certificate from file "artifacts/certs/externalidp1/idp1-cert.pem"
    Then The response status code should be 200
    When I create a key manager from payload "artifacts/payloads/keymanagers/external-idp-2.json" as "extIdpKm2"
    Then The response status code should be 201
    When I update the key manager "extIdpKm2" setting its PEM certificate from file "artifacts/certs/externalidp2/idp2-cert.pem"
    Then The response status code should be 200
    And I have created an api from "artifacts/payloads/create_apim_reflect_api.json" as "extIdpApiId" and deployed it
    When I publish the "apis" resource with id "extIdpApiId"
    Then The lifecycle status of API "extIdpApiId" should be "Published"
    When I retrieve the "apis" resource with id "extIdpApiId"
    And I extract response field "context" and store it as "apiContext"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app_oauth.json" in context as "extIdpAppPayload"
    And I create an application with payload "extIdpAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "extIdpKeysPayload"
    """
    {"keyType": "PRODUCTION", "keyManager": "Resident Key Manager", "grantTypesToBeSupported": ["client_credentials", "urn:ietf:params:oauth:grant-type:token-exchange"], "validityTime": 3600}
    """
    And I generate client credentials for application id "createdAppId" with payload "extIdpKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "extIdpSubPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "extIdpApiId" using application "createdAppId" with payload "extIdpSubPayload" as "extIdpSubId"
    Then The response status code should be 201
    When I mint an external IdP consumer key as "extIdpAzp1"
    And I map OAuth client "extIdpAzp1" to application "createdAppId" via key manager "{{extIdpKm1Name}}" once the key manager is operational
    Then The response status code should be 200
    When I mint an external IdP consumer key as "extIdpAzp2"
    And I map OAuth client "extIdpAzp2" to application "createdAppId" via key manager "{{extIdpKm2Name}}" once the key manager is operational
    Then The response status code should be 200
    And I stash the token-exchange fixture for the acting tenant

    # --- tenant1.com fixture (same arc, acting as the tenant admin) ---
    And I have valid access tokens as "admin@tenant1.com"
    When I create a key manager from payload "artifacts/payloads/keymanagers/external-idp-1.json" as "extIdpKm1"
    Then The response status code should be 201
    When I update the key manager "extIdpKm1" setting its PEM certificate from file "artifacts/certs/externalidp1/idp1-cert.pem"
    Then The response status code should be 200
    When I create a key manager from payload "artifacts/payloads/keymanagers/external-idp-2.json" as "extIdpKm2"
    Then The response status code should be 201
    When I update the key manager "extIdpKm2" setting its PEM certificate from file "artifacts/certs/externalidp2/idp2-cert.pem"
    Then The response status code should be 200
    And I have created an api from "artifacts/payloads/create_apim_reflect_api.json" as "extIdpApiId" and deployed it
    When I publish the "apis" resource with id "extIdpApiId"
    Then The lifecycle status of API "extIdpApiId" should be "Published"
    When I retrieve the "apis" resource with id "extIdpApiId"
    And I extract response field "context" and store it as "apiContext"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app_oauth.json" in context as "extIdpAppPayload"
    And I create an application with payload "extIdpAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "extIdpKeysPayload"
    """
    {"keyType": "PRODUCTION", "keyManager": "Resident Key Manager", "grantTypesToBeSupported": ["client_credentials", "urn:ietf:params:oauth:grant-type:token-exchange"], "validityTime": 3600}
    """
    And I generate client credentials for application id "createdAppId" with payload "extIdpKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "extIdpSubPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "extIdpApiId" using application "createdAppId" with payload "extIdpSubPayload" as "extIdpSubId"
    Then The response status code should be 201
    When I mint an external IdP consumer key as "extIdpAzp1"
    And I map OAuth client "extIdpAzp1" to application "createdAppId" via key manager "{{extIdpKm1Name}}" once the key manager is operational
    Then The response status code should be 200
    When I mint an external IdP consumer key as "extIdpAzp2"
    And I map OAuth client "extIdpAzp2" to application "createdAppId" via key manager "{{extIdpKm2Name}}" once the key manager is operational
    Then The response status code should be 200
    And I stash the token-exchange fixture for the acting tenant
