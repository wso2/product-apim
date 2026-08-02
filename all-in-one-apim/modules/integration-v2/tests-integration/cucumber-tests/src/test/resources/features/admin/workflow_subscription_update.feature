@cleanup
Feature: Approval workflow - subscription update

  With the SubscriptionUpdate Approval executor active, changing a subscription's tier parks an
  AM_SUBSCRIPTION_UPDATE workflow; the new tier is applied only after an admin approves it. Ports the
  subscription-update arc of WorkflowApprovalExecutorTest#testSubscriptionUpdateWorkflowProcess.

  @cap:admin @feat:workflows @dep:devportal @legacy:WorkflowApprovalExecutorTest @type:regression
  Scenario: Subscription tier change is applied only after approval
    Given The system is ready
    And I have valid access tokens as "admin"

    # This scenario owns its API (a runner-scoped shared one cannot survive the per-scenario @cleanup sweep).
    When I publish API from "artifacts/payloads/create_apim_test_api.json" through the approval workflow as "wfApiId"

    # An approved application, subscribed (and approved) to the published API at Unlimited.
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "wfApp"
    And I create an application with payload "wfApp"
    Then The response status code should be 201
    And I extract response field "name" and store it as "createdAppName"
    When I capture the pending "AM_APPLICATION_CREATION" workflow reference where "applicationName" is "{{createdAppName}}" as "appWfRef"
    And I "APPROVED" the workflow with reference "appWfRef"
    Then The response status code should be 200
    When I subscribe application "createdAppId" to API "wfApiId" retrying transient errors as "wfSubId"
    Then The response status code should be 201
    When I capture the pending "AM_SUBSCRIPTION_CREATION" workflow reference where "applicationName" is "{{createdAppName}}" as "subWfRef"
    And I "APPROVED" the workflow with reference "subWfRef"
    Then The response status code should be 200

    # Change the tier to Gold — parks an update workflow; the effective tier stays Unlimited. Capture the real
    # subscription as the update payload so it carries the actual application/api ids.
    When I get the subscription with id "wfSubId"
    Then The response status code should be 200
    And I put the response payload in context as "subscriptionPayload"
    When I update the subscription "wfSubId" with subscription plan "Gold"
    Then The response status code should be 200

    # Approve the update workflow; the tier is now Gold.
    When I capture the pending "AM_SUBSCRIPTION_UPDATE" workflow reference where "applicationName" is "{{createdAppName}}" as "subUpdWfRef"
    And I "APPROVED" the workflow with reference "subUpdWfRef"
    Then The response status code should be 200
    When I retrieve the subscription for Api "wfApiId" by Application "createdAppId"
    Then The response status code should be 200
    And The response should contain "Gold"
