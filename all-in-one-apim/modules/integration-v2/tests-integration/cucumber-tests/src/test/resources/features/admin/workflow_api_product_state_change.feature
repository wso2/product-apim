@cleanup
Feature: Approval workflow - API product state change

  API products have their OWN approval workflow type, AM_API_PRODUCT_STATE, driven by the same
  APIStateChange Approval executor: a publish lifecycle-change request on an API product is accepted (the
  workflow is created) but the product stays in CREATED - both its state and its workflowStatus - until an
  admin approves the AM_API_PRODUCT_STATE task, after which it becomes PUBLISHED. Ports
  WorkflowApprovalExecutorTest#testAPIProductWorkflowProcess, which had no equivalent in integration-v2 at all.

  The pending task is keyed by apiName carrying the PRODUCT's name (not the underlying API's), which is what
  the capture asserts. Runs twice over the REQUESTER axis (the legacy SUPER_TENANT_ADMIN vs SUPER_TENANT_USER
  factory).

  @cap:admin @feat:workflows @dep:publisher @legacy:WorkflowApprovalExecutorTest @type:regression
  Scenario Outline: API product stays CREATED after a publish request until the state change is approved as requester <requester>
    Given The system is ready with an admin approver and "<requester>" as the requester

    # The underlying API is a prerequisite, not the subject: publish it through the approval workflow as the
    # admin (that composite approves the parked deploy + API-state tasks with the admin token).
    Given I act as the tenant admin for "<requester>"
    When I publish API from "artifacts/payloads/create_apim_test_api.json" through the approval workflow as "prodApiId"

    # The requester aggregates it into an API product and requests publish - accepted (200) but held.
    Given I act as "<requester>"
    When I create an API product "${UNIQUE:WfProduct}" with context "${UNIQUE:wfProductCtx}" from API "prodApiId" as "wfProductId"
    Then The response status code should be 201
    And I extract response field "name" and store it as "wfProductName"
    When I publish the "api-products" resource with id "wfProductId"
    Then The response status code should be 200
    When I retrieve the "api-products" resource with id "wfProductId"
    Then The response status code should be 200
    And The value of response field "state" should be "CREATED"
    And The value of response field "workflowStatus" should be "CREATED"

    # Admin sees the pending AM_API_PRODUCT_STATE task keyed by the product's name, and approves it.
    Given I act as the tenant admin for "<requester>"
    When I capture the pending "AM_API_PRODUCT_STATE" workflow reference where "apiName" is "{{wfProductName}}" as "productWfRef"
    And I get the workflow with reference "productWfRef"
    Then The response status code should be 200
    And The workflow property "apiName" should be "{{wfProductName}}"
    When I "APPROVED" the workflow with reference "productWfRef"
    Then The response status code should be 200

    # The product is now PUBLISHED (the poll rides out the lifecycle propagation; the field assertion pins it).
    Given I act as "<requester>"
    When I retrieve the "api-products" resource with id "wfProductId" until it contains "PUBLISHED" within 60 seconds
    Then The value of response field "state" should be "PUBLISHED"

    Examples:
      | requester     |
      | admin         |
      | admin@tenant1.com |
      | publisherUser |
      | publisherUser@tenant1.com |
