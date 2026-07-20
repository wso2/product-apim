@cleanup
Feature: Publisher Deploy To Platform Gateway

  Publisher revision-deploy to a platform gateway by environment name, and how that deploy surfaces on the
  internal gateway sync APIs (GET
  /deployments, POST /deployments/fetch-batch) that a connected platform gateway polls/pulls from. Stays under
  the admin/environments capability (like platform_gateway.feature) tagged as depending on publisher, since the
  subject under test is the gateway's internal sync surface, not the publisher revision-deploy mechanics
  itself — the same placement precedent as the deploy-to-vhost scenario in gateway_environments.feature.

  @cap:admin @feat:environments @type:regression @dep:publisher
  Scenario Outline: A revision deployed to a platform gateway syncs internal deployments and fetch-batch as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I register a platform gateway named "${UNIQUE:pgwdeploysync}"
    Then The response status code should be 201
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "pgwDeploySyncApiPayload"
    And I create an "apis" resource with payload "pgwDeploySyncApiPayload" as "pgwDeploySyncApiId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "pgwDeploySyncRevPayload"
    """
    {"description":"platform universal deploy sync"}
    """
    And I make a request to create a revision for "apis" resource "pgwDeploySyncApiId" with payload "pgwDeploySyncRevPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "pgwDeploySyncDeployPayload"
    """
    [{"name":"{{platformGatewayName}}","vhost":"localhost","displayOnDevportal":true}]
    """
    And I make a request to deploy revision "revisionId" of "apis" resource "pgwDeploySyncApiId" with payload "pgwDeploySyncDeployPayload"
    Then The response status code should be 201
    And I wait until internal deployments list "pgwDeploySyncApiId" using api key "platformGatewayToken" within 120 seconds
    When I fetch internal deployment batch as tar for deployment id "platformGatewayDeploymentId" using api key "platformGatewayToken"
    Then The fetched deployment archive should contain "ustar"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:admin @feat:environments @type:negative @dep:publisher
  Scenario Outline: Deleting a platform gateway with a deployed revision is rejected as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I register a platform gateway named "${UNIQUE:pgwdelconflict}"
    Then The response status code should be 201
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "pgwDelConflictApiPayload"
    And I create an "apis" resource with payload "pgwDelConflictApiPayload" as "pgwDelConflictApiId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "pgwDelConflictRevPayload"
    """
    {"description":"block gateway delete while a revision is deployed"}
    """
    And I make a request to create a revision for "apis" resource "pgwDelConflictApiId" with payload "pgwDelConflictRevPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "pgwDelConflictDeployPayload"
    """
    [{"name":"{{platformGatewayName}}","vhost":"localhost","displayOnDevportal":true}]
    """
    And I make a request to deploy revision "revisionId" of "apis" resource "pgwDelConflictApiId" with payload "pgwDelConflictDeployPayload"
    Then The response status code should be 201
    When I delete the platform gateway with id "platformGatewayId"
    Then The response status code should be 409
    When I undeploy revision "revisionId" of "apis" resource "pgwDelConflictApiId" with payload "pgwDelConflictDeployPayload"
    Then The response status code should be 201

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:admin @feat:environments @type:regression @dep:publisher
  Scenario Outline: Undeploying a revision removes it from internal deployments as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I register a platform gateway named "${UNIQUE:pgwundeploy}"
    Then The response status code should be 201
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "pgwUndeployApiPayload"
    And I create an "apis" resource with payload "pgwUndeployApiPayload" as "pgwUndeployApiId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "pgwUndeployRevPayload"
    """
    {"description":"undeploy removes internal deployment row"}
    """
    And I make a request to create a revision for "apis" resource "pgwUndeployApiId" with payload "pgwUndeployRevPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "pgwUndeployDeployPayload"
    """
    [{"name":"{{platformGatewayName}}","vhost":"localhost","displayOnDevportal":true}]
    """
    And I make a request to deploy revision "revisionId" of "apis" resource "pgwUndeployApiId" with payload "pgwUndeployDeployPayload"
    Then The response status code should be 201
    And I wait until internal deployments list "pgwUndeployApiId" using api key "platformGatewayToken" within 120 seconds
    When I undeploy revision "revisionId" of "apis" resource "pgwUndeployApiId" with payload "pgwUndeployDeployPayload"
    Then The response status code should be 201
    And Internal deployments should stop listing "pgwUndeployApiId" using api key "platformGatewayToken" within 120 seconds

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Expected in the generated platform-gateway API YAML (PlatformGatewayAPIYamlConverter in carbon-apimgt).
  @cap:admin @feat:environments @type:regression @dep:publisher
  Scenario Outline: The fetch-batch archive contains the platform-gateway YAML apiVersion marker as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I register a platform gateway named "${UNIQUE:pgwyamlmarker}"
    Then The response status code should be 201
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "pgwYamlMarkerApiPayload"
    And I create an "apis" resource with payload "pgwYamlMarkerApiPayload" as "pgwYamlMarkerApiId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "pgwYamlMarkerRevPayload"
    """
    {"description":"artifact yaml marker"}
    """
    And I make a request to create a revision for "apis" resource "pgwYamlMarkerApiId" with payload "pgwYamlMarkerRevPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "pgwYamlMarkerDeployPayload"
    """
    [{"name":"{{platformGatewayName}}","vhost":"localhost","displayOnDevportal":true}]
    """
    And I make a request to deploy revision "revisionId" of "apis" resource "pgwYamlMarkerApiId" with payload "pgwYamlMarkerDeployPayload"
    Then The response status code should be 201
    And I wait until internal deployments list "pgwYamlMarkerApiId" using api key "platformGatewayToken" within 120 seconds
    When I fetch internal deployment batch as tar for deployment id "platformGatewayDeploymentId" using api key "platformGatewayToken"
    Then The fetched deployment archive should contain "ustar"
    And The fetched deployment archive should contain "gateway.api-platform.wso2.com"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cleanup
  @cap:admin @feat:environments @type:regression @dep:publisher @dep:key-manager @dep:devportal
  Scenario Outline: API key lifecycle events are broadcast over the internal WebSocket as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I register a platform gateway named "${UNIQUE:pgwapikeyevt}"
    Then The response status code should be 201
    When I connect to the internal gateway control-plane WebSocket using api key "platformGatewayToken"
    Then The internal gateway WebSocket connection should be open
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "pgwApiKeyEvtApiPayload"
    And I create an "apis" resource with payload "pgwApiKeyEvtApiPayload" as "pgwApiKeyEvtApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "pgwApiKeyEvtApiId"
    Then The response status code should be 200
    When I put the following JSON payload in context as "pgwApiKeyEvtRevPayload"
    """
    {"description":"apikey websocket event validation"}
    """
    And I make a request to create a revision for "apis" resource "pgwApiKeyEvtApiId" with payload "pgwApiKeyEvtRevPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "pgwApiKeyEvtDeployPayload"
    """
    [{"name":"{{platformGatewayName}}","vhost":"localhost","displayOnDevportal":true}]
    """
    And I make a request to deploy revision "revisionId" of "apis" resource "pgwApiKeyEvtApiId" with payload "pgwApiKeyEvtDeployPayload"
    Then The response status code should be 201
    And I wait until internal deployments list "pgwApiKeyEvtApiId" using api key "platformGatewayToken" within 120 seconds
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "pgwApiKeyEvtAppPayload"
    And I create an application with payload "pgwApiKeyEvtAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "pgwApiKeyEvtSubPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "pgwApiKeyEvtApiId" using application "createdAppId" with payload "pgwApiKeyEvtSubPayload" as "pgwApiKeyEvtSubscriptionId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "pgwApiKeyEvtKeyPayload"
    """
    {"keyName": "pgw-key-lifecycle", "validityPeriod": 3600, "additionalProperties": {"permittedIP": "", "permittedReferer": ""}}
    """
    And I request an api key for application id "createdAppId" using payload "pgwApiKeyEvtKeyPayload"
    Then The response status code should be 200
    And I should receive an internal websocket event of type "apikey.created" within 30 seconds
    And The last internal websocket event field "apiId" should equal "{{pgwApiKeyEvtApiId}}"
    And The last internal websocket event field "name" should equal "pgw-key-lifecycle"
    And The last internal websocket event field "maskedApiKey" should start with "****"
    When I request an api key for application id "createdAppId" using payload "pgwApiKeyEvtKeyPayload"
    Then The response status code should be 200
    And I should receive an internal websocket event of type "apikey.updated" or "apikey.created" within 30 seconds
    And The last internal websocket event's key-name field should equal "pgw-key-lifecycle"
    And The last internal websocket event field "maskedApiKey" should start with "****"
    When I retrieve the api key UUID for application id "createdAppId" as "pgwApiKeyEvtKeyUuid"
    And I revoke the api key with UUID "pgwApiKeyEvtKeyUuid" for application id "createdAppId"
    Then I should receive an internal websocket event of type "apikey.revoked" within 30 seconds
    And The last internal websocket event field "apiId" should equal "{{pgwApiKeyEvtApiId}}"
    And The last internal websocket event field "keyName" should equal "pgw-key-lifecycle"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
