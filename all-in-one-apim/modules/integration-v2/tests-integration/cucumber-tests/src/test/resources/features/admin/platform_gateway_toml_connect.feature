@cleanup
Feature: Platform Gateway TOML Connect-With-Token

  P0 coverage for [[apim.platform_gateway.connect]] in deployment.toml (carbon-apimgt#13882): a platform
  gateway declared in deployment.toml — rather than dynamically registered via POST /gateways — does NOT exist
  until its first WebSocket connect, at which point it is lazily bootstrapped into the DB, becomes visible as a
  GET /environments entry, and starts receiving Publisher deploys. Needs its own container (a
  tomlExtraOverlayPath declaring two connect-with-token entries, one per organization this block provisions),
  hence its own feature file/block rather than folding into platform_gateway.feature's default-config block.
  One ordered scenario per organization — Gherkin runs a scenario's steps top-to-bottom, so the sequential
  bootstrap→connect→deploy flow needs no separate dependency chain between steps. The bootstrapped gateway's id
  is DB-assigned at connect time (not known up front), so it is looked up by name via GET /gateways rather than
  asserted against a hardcoded UUID.

  @cap:admin @feat:environments @type:regression @dep:publisher
  Scenario Outline: A TOML-declared platform gateway bootstraps lazily on WebSocket connect for org <organization>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I retrieve all platform gateways
    Then The response status code should be 200
    And The response should not contain "<gatewayName>"
    When I connect to the internal gateway control-plane WebSocket using api key "<registrationToken>"
    Then The internal gateway WebSocket connection should be open
    And I should receive an internal websocket event of type "connection.ack" within 30 seconds
    When I look up the platform gateway id for name "<gatewayName>" and store it as "tomlGatewayId"
    And I register the platform gateway "tomlGatewayId" for cleanup
    Then The platform gateway "tomlGatewayId" should become active within 30 seconds
    When I retrieve the gateway environment with id "tomlGatewayId"
    Then The response status code should be 200
    And The response should contain "APIPlatform"
    And The response should contain "<gatewayName>"
    When I retrieve internal deployments using api key "<registrationToken>"
    Then The response status code should be 200
    When I close the internal gateway WebSocket connection
    Then The platform gateway "tomlGatewayId" should become inactive within 15 seconds
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "tomlPgwApiPayload"
    And I create an "apis" resource with payload "tomlPgwApiPayload" as "tomlPgwApiId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "tomlPgwRevPayload"
    """
    {"description":"toml connect gateway deploy"}
    """
    And I make a request to create a revision for "apis" resource "tomlPgwApiId" with payload "tomlPgwRevPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "tomlPgwDeployPayload"
    """
    [{"name":"<gatewayName>","vhost":"localhost","displayOnDevportal":true}]
    """
    And I make a request to deploy revision "revisionId" of "apis" resource "tomlPgwApiId" with payload "tomlPgwDeployPayload"
    Then The response status code should be 201
    And I wait until internal deployments list "tomlPgwApiId" using api key "<registrationToken>" within 120 seconds
    When I fetch internal deployment batch as tar for deployment id "platformGatewayDeploymentId" using api key "<registrationToken>"
    Then The fetched deployment archive should contain "ustar"

    Examples:
      | organization | actor             | gatewayName       | registrationToken                                                |
      | carbon.super | admin             | toml-pgw-it        | 01900000-0000-7000-8000-000000000001.TomlConnectIntegrationPlainToken |
      | tenant1.com  | admin@tenant1.com | toml-pgw-tenant1   | 01900000-0000-7000-8000-000000000002.TomlConnectTenantPlainToken      |
