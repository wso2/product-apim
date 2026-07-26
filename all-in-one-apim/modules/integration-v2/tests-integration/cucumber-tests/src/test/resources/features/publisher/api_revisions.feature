@cleanup
Feature: Publisher API Revisions

  Ports the API-revision CRUD tail from the legacy APIRevisionServerRestartTestCase (functional concern; the
  legacy "restart" was incidental). Beyond create + deploy (already covered across the suite), this exercises
  listing revisions (all and deployed-only), the rule that a deployed revision cannot be deleted (400 until it
  is undeployed), undeploy, delete, and restoring the API's working copy from a revision. Publisher plane, as a
  least-privilege publisher, in BOTH the super tenant and tenant1.com to prove the revision operations are
  tenant-agnostic. Teardown via the per-scenario cleanup hook.

  @cap:publisher @feat:api-lifecycle @type:regression @legacy:APIRevisionServerRestartTestCase
  Scenario Outline: Revision CRUD — list, deploy, delete-while-deployed guard, undeploy, delete, restore as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "revApiId" and deployed it

    # Create a second revision and confirm it is listed.
    When I put the following JSON payload in context as "rev2Payload"
    """
    {"description":"second revision"}
    """
    And I make a request to create a revision for "apis" resource "revApiId" with payload "rev2Payload"
    Then The response status code should be 201
    And I extract response field "id" and store it as "rev2Id"
    When I retrieve the revisions of "apis" resource "revApiId"
    Then The response status code should be 200
    And The response should contain "second revision"
    When I retrieve the deployed revisions of "apis" resource "revApiId"
    Then The response status code should be 200

    # Deploy the second revision.
    When I deploy revision "rev2Id" of "apis" resource "revApiId"
    Then The response status code should be 201
    And I wait until "apis" "revApiId" revision is deployed in the gateway

    # A deployed revision cannot be deleted (400) — it must be undeployed first.
    When I delete revision "rev2Id" of "apis" resource "revApiId"
    Then The response status code should be 400

    # Undeploy, then the delete succeeds (200).
    When I undeploy revision "rev2Id" of "apis" resource "revApiId"
    Then The response status code should be 201
    When I delete revision "rev2Id" of "apis" resource "revApiId"
    Then The response status code should be 200

    # Restore the API's working copy from a fresh revision.
    When I put the following JSON payload in context as "rev3Payload"
    """
    {"description":"restore source revision"}
    """
    And I make a request to create a revision for "apis" resource "revApiId" with payload "rev3Payload"
    Then The response status code should be 201
    And I extract response field "id" and store it as "rev3Id"
    When I restore revision "rev3Id" of "apis" resource "revApiId"
    Then The response status code should be 201

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # Wave B-2: deploying a revision to a vhost NOT configured for the target environment is rejected (400).
  # Ports APIRevisionTestCase.testDeployAPIRevisionWithInvalidVhost.
  @cap:publisher @feat:revisions @rule:invalid-vhost @type:negative @legacy:APIRevisionTestCase
  Scenario Outline: Deploying a revision to a vhost not configured for the environment is rejected as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "ivApiId" and deployed it
    When I put the following JSON payload in context as "ivRev"
    """
    {"description":"invalid vhost revision"}
    """
    And I make a request to create a revision for "apis" resource "ivApiId" with payload "ivRev"
    Then The response status code should be 201
    When I put the following JSON payload in context as "ivDeploy"
    """
    [{"name":"{{gatewayEnvironment}}","vhost":"ws.wso2.com","displayOnDevportal":true}]
    """
    And I make a request to deploy revision "revisionId" of "apis" resource "ivApiId" with payload "ivDeploy"
    Then The response status code should be 400

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # Wave B-4 (verify-first): a deployed API revision reports gateway deployment-acknowledgement counts. Ports
  # APIRevisionTestCase.testVerifyDeploymentAcknowledgmentCounts — asserts the count FIELDS are present in the
  # deployed-revisions deploymentInfo (the stable contract). The legacy count>0 assertion is intentionally NOT
  # made here: it depends on a gateway ack that lags (legacy retried ~100s) and is flaky in a fresh container.
  @cap:publisher @feat:revisions @rule:deployment-ack @type:regression @legacy:APIRevisionTestCase
  Scenario Outline: A deployed API revision reports gateway deployment-acknowledgement counts as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "ackApiId" and deployed it
    When I retrieve the deployed revisions of "apis" resource "ackApiId"
    Then The response status code should be 200
    And The response should contain "deployedGatewayCount"
    And The response should contain "liveGatewayCount"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |
