@cleanup
Feature: DevPortal Environment URLs

  How the DevPortal view of an API reflects the gateway environments the API is actually deployed to — the
  endpoint URLs listed for the API, and the servers URL stamped into its OpenAPI definition (the URL the try-out
  console calls). Creating the gateway environment and publishing the API are admin/publisher prerequisites
  (@dep), not the subject: every assertion here is on what the DevPortal returns. Store-side visibility is in
  visibility.feature and search/browse in search.feature — neither is exercised here. ×2 tenant.

  # An API deployed to a custom gateway environment reflects that environment's vhost in its Developer Portal
  # endpoint URLs. NEW verified coverage (legacy-disabled testValidateDevportalAPIAndSwaggerResponse).
  @cap:devportal @feat:discovery @rule:environment @type:regression @dep:publisher @dep:admin @legacy:EnvironmentTestCase
  Scenario Outline: A devportal API reflects the custom environment vhost it is deployed to as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "e5ApiPayload"
    And I create an "apis" resource with payload "e5ApiPayload" as "e5ApiId"
    Then The response status code should be 201
    When I create a gateway environment "${UNIQUE:e5Env}" with vhost hosts "e5.gw.example.com"
    Then The response status code should be 201
    And I extract response field "name" and store it as "e5EnvName"
    When I put the following JSON payload in context as "e5RevPayload"
    """
    {"description":"revision for devportal env check"}
    """
    And I make a request to create a revision for "apis" resource "e5ApiId" with payload "e5RevPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "e5DeployPayload"
    """
    [{"name":"{{e5EnvName}}","vhost":"e5.gw.example.com","displayOnDevportal":true}]
    """
    And I make a request to deploy revision "revisionId" of "apis" resource "e5ApiId" with payload "e5DeployPayload"
    Then The response status code should be 201
    When I publish the "apis" resource with id "e5ApiId"
    Then The lifecycle status of API "e5ApiId" should be "Published"
    # The devportal view of the API carries the custom environment's vhost among its endpoint URLs.
    When I retrieve the devportal API "e5ApiId" until it contains "e5.gw.example.com" within 60 seconds
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # DevPortal Swagger `servers` URL resolution with MULTIPLE gateway environments in the tenant. The DevPortal
  # fetches GET /apis/{id}/swagger with NO environmentName, so the server resolves the environment itself — and it
  # must consider only environments the API is actually deployed to. Ports the three swagger-server-URL tests of
  # EnvironmentTestCase, which walk ONE API through the same three deployment states, so they stay one scenario
  # here too: deployed to the second gateway only, moved to Default, then deployed to both.
  @cap:devportal @feat:discovery @rule:environment @type:regression @dep:publisher @dep:admin @legacy:EnvironmentTestCase
  Scenario Outline: The devportal swagger server URL resolves to a gateway the API is deployed to as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    # A second gateway environment alongside the built-in Default (whose vhost is localhost).
    When I create a gateway environment "${UNIQUE:swEnv}" with vhost hosts "swagger.gw.example.com"
    Then The response status code should be 201
    And I extract response field "name" and store it as "swEnvName"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "swApiPayload"
    And I create an "apis" resource with payload "swApiPayload" as "swApiId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "swRevPayload"
    """
    {"description":"revision for devportal swagger server URL checks"}
    """
    And I make a request to create a revision for "apis" resource "swApiId" with payload "swRevPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "swSecondGatewayPayload"
    """
    [{"name":"{{swEnvName}}","vhost":"swagger.gw.example.com","displayOnDevportal":true}]
    """
    # (a) Deployed ONLY to the second gateway → the resolved server host is that gateway's vhost.
    And I make a request to deploy revision "revisionId" of "apis" resource "swApiId" with payload "swSecondGatewayPayload"
    Then The response status code should be 201
    When I publish the "apis" resource with id "swApiId"
    Then The lifecycle status of API "swApiId" should be "Published"
    And the devportal swagger of API "swApiId" should resolve its server host to "swagger.gw.example.com" within 60 seconds
    # (b) Undeployed from the second gateway and deployed to Default → the resolved host follows to Default's vhost.
    When I undeploy revision "revisionId" of "apis" resource "swApiId" with payload "swSecondGatewayPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "swDefaultGatewayPayload"
    """
    [{"name":"{{gatewayEnvironment}}","vhost":"localhost","displayOnDevportal":true}]
    """
    And I make a request to deploy revision "revisionId" of "apis" resource "swApiId" with payload "swDefaultGatewayPayload"
    Then The response status code should be 201
    And the devportal swagger of API "swApiId" should resolve its server host to "localhost" within 60 seconds
    # (c) Deployed to BOTH gateways → the host must be a vhost of one of them, never the regression's empty host.
    When I make a request to deploy revision "revisionId" of "apis" resource "swApiId" with payload "swSecondGatewayPayload"
    Then The response status code should be 201
    And the devportal swagger of API "swApiId" should resolve its server host to one of "localhost,swagger.gw.example.com" within 60 seconds
    # Undeploy from the custom environment so it can be deleted
    When I undeploy revision "revisionId" of "apis" resource "swApiId" with payload "swSecondGatewayPayload"
    Then The response status code should be 201
    When I delete the gateway environment with id "environmentId"
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
