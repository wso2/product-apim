@cleanup
Feature: Publisher Network Access Control - URL-gate entry points under a tenant allow policy

  The outbound host-validation policy can be driven per tenant via tenant-conf.json (NetworkSecurityAccessControl),
  independently of the platform deployment.toml source. Under a tenant allow policy that allow-lists only one host
  pattern, every publisher/admin entry point that resolves a user-supplied URL rejects a non-allow-listed host
  before any outbound fetch: endpoint validation reports the block, and Key Manager / GraphQL / WSDL / OpenAPI /
  MCP / import entry points fail with HTTP 400 and a "could not be resolved" error. An allow-listed host still
  passes, and a second tenant with no policy is unaffected. Ports HostValidationTenantAllowModeTestCase. Runs in
  the network-access-control-tenant-allow container (permissive platform; the tenant policy is the sole gate).

  Background:
    Given The system is ready and I have valid publisher access tokens as "publisherUser"
    And I have valid access tokens as "admin"
    And I apply a tenant allow policy for hosts "*.allowed.example.com" as "admin"
    And I act as "publisherUser"

  @cap:publisher @feat:network-access-control @rule:key-manager @type:negative @dep:admin @legacy:HostValidationTenantAllowModeTestCase
  Scenario: Key Manager creation with a non-allow-listed URL is rejected under the tenant allow policy
    Given I act as "admin"
    When I create a key manager with endpoints on host "evil.attacker.com"
    Then The response status code should be 400
    And The response should contain "could not be resolved"

  @cap:publisher @feat:network-access-control @rule:key-manager-jwks @type:negative @dep:admin @legacy:HostValidationTenantAllowModeTestCase
  Scenario: Key Manager creation with a non-allow-listed JWKS certificate URL is rejected
    Given I act as "admin"
    When I create a key manager with a blocked jwks certificate url "https://evil.attacker.com/.well-known/jwks.json"
    Then The response status code should be 400
    And The response should contain "could not be resolved"

  @cap:publisher @feat:network-access-control @rule:key-manager-update @type:negative @dep:admin @legacy:HostValidationTenantAllowModeTestCase
  Scenario: Key Manager update to a non-allow-listed token endpoint is rejected
    Given I act as "admin"
    When I create a key manager on host "api.allowed.example.com" then update its token endpoint to "https://evil.attacker.com/oauth2/token"
    Then The response status code should be 400
    And The response should contain "could not be resolved"

  @cap:publisher @feat:network-access-control @rule:key-manager-discover @type:negative @dep:admin @legacy:HostValidationTenantAllowModeTestCase
  Scenario: Key Manager discovery from a non-allow-listed URL is rejected
    Given I act as "admin"
    When I discover key manager configuration from url "https://evil.attacker.com/.well-known/openid-configuration" of type "OIDC"
    Then The response status code should be 400
    And The response should contain "could not be resolved"

  # GraphQL schema validation returns 200 with isValid=false and an error message when the URL is blocked.
  @cap:publisher @feat:network-access-control @rule:graphql-validate @type:negative @legacy:HostValidationTenantAllowModeTestCase
  Scenario: GraphQL schema validation from a non-allow-listed URL reports a block
    When I validate the graphql schema from url "http://evil.attacker.com/graphql"
    Then The response status code should be 200
    And The response should contain "could not be resolved"

  @cap:publisher @feat:network-access-control @rule:graphql-import @type:negative @legacy:HostValidationTenantAllowModeTestCase
  Scenario: GraphQL schema import from a non-allow-listed URL is rejected
    When I import a graphql api from url "http://evil.attacker.com/graphql" with properties file "artifacts/payloads/networkAccessControl/graphql_import_props.json"
    Then The response status code should be 400
    And The response should contain "could not be resolved"

  @cap:publisher @feat:network-access-control @rule:wsdl-url @type:negative @legacy:HostValidationTenantAllowModeTestCase
  Scenario: WSDL import from a non-allow-listed URL is rejected
    When I import a wsdl api from url "http://evil.attacker.com/service?wsdl" with properties file "artifacts/payloads/networkAccessControl/wsdl_additional_properties.json"
    Then The response status code should be 400
    And The response should contain "could not be resolved"

  @cap:publisher @feat:network-access-control @rule:oas-validate @type:negative @legacy:HostValidationTenantAllowModeTestCase
  Scenario: OpenAPI definition validation from a non-allow-listed URL is rejected
    When I validate the openapi definition from url "http://evil.attacker.com/openapi.json"
    Then The response status code should be 400
    And The response should contain "could not be resolved"

  @cap:publisher @feat:network-access-control @rule:mcp-proxy @type:negative @legacy:HostValidationTenantAllowModeTestCase
  Scenario: MCP server proxy creation to a non-allow-listed URL is rejected
    When I create an mcp server proxy to url "http://evil.attacker.com/endpoint"
    Then The response status code should be 400
    And The response should contain "could not be resolved"

  @cap:publisher @feat:network-access-control @rule:api-backend @type:negative @legacy:HostValidationTenantAllowModeTestCase
  Scenario: API creation with a non-allow-listed backend is rejected
    When I create an api with a blocked backend "http://evil.attacker.com/endpoint"
    Then The response status code should be 400
    And The response should contain "could not be resolved"

  # The API is seeded with an allow-listed backend (passes), then updated to a blocked one (the asserted call).
  @cap:publisher @feat:network-access-control @rule:api-backend-update @type:negative @legacy:HostValidationTenantAllowModeTestCase
  Scenario: API update to a non-allow-listed backend is rejected
    When I import open api definition from "artifacts/payloads/networkAccessControl/oas30_seed_clean.json" , additional properties from "artifacts/payloads/networkAccessControl/props_allowlisted_http.json" and create api as "nacUpdateApiId"
    And I set the production endpoint of api "nacUpdateApiId" to "http://evil.attacker.com/endpoint"
    Then The response status code should be 400
    And The response should contain "could not be resolved"

  @cap:publisher @feat:network-access-control @rule:mcp-from-api @type:negative @legacy:HostValidationTenantAllowModeTestCase
  Scenario: MCP server creation from an API with a non-allow-listed backend is rejected
    When I create an mcp server from api with a blocked backend "http://evil.attacker.com/endpoint"
    Then The response status code should be 400
    And The response should contain "could not be resolved"

  @cap:publisher @feat:network-access-control @rule:openapi-import @type:negative @legacy:HostValidationTenantAllowModeTestCase
  Scenario: OpenAPI import with a non-allow-listed endpoint is rejected
    When I attempt to import openapi definition from "artifacts/payloads/networkAccessControl/hv-test-openapi.yaml" with additional properties from "artifacts/payloads/networkAccessControl/props_blocked_http.json"
    Then The response status code should be 400
    And The response should contain "could not be resolved"

  @cap:publisher @feat:network-access-control @rule:asyncapi-import @type:negative @legacy:HostValidationTenantAllowModeTestCase
  Scenario: AsyncAPI import with a non-allow-listed endpoint is rejected
    When I attempt to import asyncapi definition from "artifacts/payloads/networkAccessControl/hv-test-asyncapi.yaml" with additional properties "artifacts/payloads/networkAccessControl/props_blocked_ws.json"
    Then The response status code should be 400
    And The response should contain "could not be resolved"

  # Positive control: a host that IS allow-listed passes endpoint validation (not blocked).
  @cap:publisher @feat:network-access-control @rule:allow-listed @type:regression @legacy:HostValidationTenantAllowModeTestCase
  Scenario: Endpoint validation of an allow-listed URL passes under the tenant allow policy
    When I validate the endpoint url "http://api.allowed.example.com/endpoint"
    Then The response status code should be 200
    And The response should not contain "could not be resolved"

  # Tenant scoping: the super-tenant allow policy must not affect a second tenant whose config is at default.
  @cap:publisher @feat:network-access-control @rule:tenant-isolation @type:negative @dep:admin @legacy:HostValidationTenantAllowModeTestCase
  Scenario: A second tenant with no policy is unaffected by the super-tenant allow policy
    Given The system is ready and I have valid publisher access tokens as "publisherUser@tenant1.com"
    And I act as "publisherUser"
    When I validate the endpoint url "http://evil.attacker.com/endpoint"
    Then The response status code should be 200
    And The response should contain "could not be resolved"
    When I act as "publisherUser@tenant1.com"
    And I validate the endpoint url "http://evil.attacker.com/endpoint"
    Then The response status code should be 200
    And The response should not contain "could not be resolved"
