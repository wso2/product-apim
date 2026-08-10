@cleanup
Feature: Approval workflow - pending task cleanup on resource delete

  Deleting a resource that has pending workflow tasks cleans those tasks up, so a get-by-reference on each
  returns 404. All four task types the legacy test asserts are covered: deleting an application clears its
  pending AM_APPLICATION_CREATION, AM_SUBSCRIPTION_CREATION and AM_APPLICATION_REGISTRATION_PRODUCTION tasks,
  and deleting an API clears its pending AM_API_STATE task. Ports the cleanup arc of
  WorkflowApprovalExecutorTest#testCleanUpWorkflowProcess.

  Each scenario reads the reference back for a 200 BEFORE the delete: that is what separates "the task was
  cleaned up" from "the reference was never resolvable in the first place", which a bare 404 cannot distinguish.

  Every scenario runs twice over the REQUESTER axis (the legacy SUPER_TENANT_ADMIN vs SUPER_TENANT_USER
  factory): the admin decides in both rows, while the actor that owns the resource is the admin itself in one
  row and a non-admin in the other.

  @cap:admin @feat:workflows @dep:devportal @legacy:WorkflowApprovalExecutorTest @type:regression
  Scenario Outline: Deleting an application with a pending creation task cleans up the task as requester <requester>
    Given The system is ready with an admin approver and "<requester>" as the requester

    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "wfApp"
    And I create an application with payload "wfApp"
    Then The response status code should be 201
    And I extract response field "name" and store it as "createdAppName"

    # The creation task is pending and retrievable by reference.
    Given I act as the tenant admin for "<requester>"
    When I capture the pending "AM_APPLICATION_CREATION" workflow reference where "applicationName" is "{{createdAppName}}" as "appWfRef"
    And I get the workflow with reference "appWfRef"
    Then The response status code should be 200

    # Delete the application (accepted, 201 → DELETE_PENDING) and approve the deletion; completing the delete
    # cleans up the still-pending creation task.
    Given I act as "<requester>"
    When I delete the application with id "createdAppId"
    Then The response status code should be 201
    Given I act as the tenant admin for "<requester>"
    When I capture the pending "AM_APPLICATION_DELETION" workflow reference where "applicationName" is "{{createdAppName}}" as "appDelWfRef"
    And I "APPROVED" the workflow with reference "appDelWfRef"
    Then The response status code should be 200

    # The pending creation task no longer exists.
    When I get the workflow with reference "appWfRef"
    Then The response status code should be 404

    Examples:
      | requester      |
      | admin          |
      | admin@tenant1.com |
      | subscriberUser |
      | subscriberUser@tenant1.com |

  @cap:admin @feat:workflows @dep:publisher @legacy:WorkflowApprovalExecutorTest @type:regression
  Scenario Outline: Deleting an API with a pending state-change task cleans up the task as requester <requester>
    Given The system is ready with an admin approver and "<requester>" as the requester

    # An API whose revision deployment is approved, then a publish request that parks an AM_API_STATE task.
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "cleanupApi"
    And I create an "apis" resource with payload "cleanupApi" as "cleanupApiId"
    Then The response status code should be 201
    And I extract response field "name" and store it as "cleanupApiName"
    When I put the following JSON payload in context as "cleanupRevision"
    """
    {"description": "Initial Revision"}
    """
    And I make a request to create a revision for "apis" resource "cleanupApiId" with payload "cleanupRevision"
    Then The response status code should be 201
    And I extract response field "id" and store it as "cleanupRevId"
    When I put the following JSON payload in context as "cleanupDeploy"
    """
    [{"name": "{{gatewayEnvironment}}", "vhost": "localhost", "displayOnDevportal": true}]
    """
    And I make a request to deploy revision "cleanupRevId" of "apis" resource "cleanupApiId" with payload "cleanupDeploy"
    Given I act as the tenant admin for "<requester>"
    When I capture the pending "AM_REVISION_DEPLOYMENT" workflow reference where "apiName" is "{{cleanupApiName}}" as "cleanupDepWfRef"
    And I "APPROVED" the workflow with reference "cleanupDepWfRef"
    Then The response status code should be 200
    Given I act as "<requester>"
    When I publish the "apis" resource with id "cleanupApiId"
    Then The response status code should be 200

    # The API-state task is pending and retrievable by reference.
    Given I act as the tenant admin for "<requester>"
    When I capture the pending "AM_API_STATE" workflow reference where "apiName" is "{{cleanupApiName}}" as "cleanupStateWfRef"
    And I get the workflow with reference "cleanupStateWfRef"
    Then The response status code should be 200

    # Undeploy + delete the revision, then delete the API — which cleans up its pending state-change task.
    Given I act as "<requester>"
    When I undeploy revision "cleanupRevId" of "apis" resource "cleanupApiId"
    Then The response status code should be 201
    When I delete revision "cleanupRevId" of "apis" resource "cleanupApiId"
    Then The response status code should be 200
    When I delete the "apis" resource with id "cleanupApiId"
    Then The response status code should be 200
    Given I act as the tenant admin for "<requester>"
    When I get the workflow with reference "cleanupStateWfRef"
    Then The response status code should be 404

    Examples:
      | requester     |
      | admin         |
      | admin@tenant1.com |
      | publisherUser |
      | publisherUser@tenant1.com |

  @cap:admin @feat:workflows @dep:devportal @legacy:WorkflowApprovalExecutorTest @type:regression
  Scenario Outline: Deleting an application cleans up its pending subscription and key-registration tasks as requester <requester>
    Given The system is ready with an admin approver and "<requester>" as the requester

    # A published API to subscribe to — a prerequisite, so the admin publishes it.
    Given I act as the tenant admin for "<requester>"
    When I publish API from "artifacts/payloads/create_apim_test_api.json" through the approval workflow as "subCleanupApiId"

    # An approved application owned by the requester.
    Given I act as "<requester>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "wfApp"
    And I create an application with payload "wfApp"
    Then The response status code should be 201
    And I extract response field "name" and store it as "createdAppName"
    Given I act as the tenant admin for "<requester>"
    When I capture the pending "AM_APPLICATION_CREATION" workflow reference where "applicationName" is "{{createdAppName}}" as "appWfRef"
    And I "APPROVED" the workflow with reference "appWfRef"
    Then The response status code should be 200

    # Subscribe and request production keys, leaving BOTH tasks pending (neither is approved).
    Given I act as "<requester>"
    When I subscribe application "createdAppId" to API "subCleanupApiId" retrying transient errors as "wfSubId"
    Then The response status code should be 201
    When I generate pending production keys for application "createdAppId"
    Then The response status code should be 200
    And The response should contain "CREATED"

    # Both tasks are pending and retrievable by reference.
    Given I act as the tenant admin for "<requester>"
    When I capture the pending "AM_SUBSCRIPTION_CREATION" workflow reference where "applicationName" is "{{createdAppName}}" as "subCleanupWfRef"
    And I get the workflow with reference "subCleanupWfRef"
    Then The response status code should be 200
    When I capture the pending "AM_APPLICATION_REGISTRATION_PRODUCTION" workflow reference where "applicationName" is "{{createdAppName}}" as "keyCleanupWfRef"
    And I get the workflow with reference "keyCleanupWfRef"
    Then The response status code should be 200

    # Delete the application and approve the deletion; both of its pending tasks are cleaned up.
    Given I act as "<requester>"
    When I delete the application with id "createdAppId"
    Then The response status code should be 201
    Given I act as the tenant admin for "<requester>"
    When I capture the pending "AM_APPLICATION_DELETION" workflow reference where "applicationName" is "{{createdAppName}}" as "appDelWfRef"
    And I "APPROVED" the workflow with reference "appDelWfRef"
    Then The response status code should be 200
    When I get the workflow with reference "subCleanupWfRef"
    Then The response status code should be 404
    When I get the workflow with reference "keyCleanupWfRef"
    Then The response status code should be 404

    Examples:
      | requester      |
      | admin          |
      | admin@tenant1.com |
      | subscriberUser |
      | subscriberUser@tenant1.com |
