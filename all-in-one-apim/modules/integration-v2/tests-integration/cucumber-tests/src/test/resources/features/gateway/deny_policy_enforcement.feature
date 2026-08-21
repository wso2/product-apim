@cleanup
Feature: Deny Policy Enforcement At The Gateway

  A deny policy must actually REFUSE traffic, not merely exist. This is the first coverage of deny ENFORCEMENT
  in either suite: v2's admin/deny_policies.feature (13 scenarios) and legacy APIDenyPolicyTestCase (14 tests)
  both assert only that a policy is CREATED (201) and then delete it — neither ever invokes the gateway. So a
  deny that silently failed to apply would have gone unnoticed, which is the regression this closes.

  NEW COVERAGE, deliberately untagged with @legacy: nothing in the legacy suite exercised this, so tagging it
  as parity would misreport what was ported.

  WHY API-CONTEXT AND NOT USER. The API-context condition is the one that is independent of how the gateway
  resolves the CALLER — it keys on the API alone. A USER-type deny is NOT covered here on purpose: probing on
  4.7.0 showed a USER deny does not block, in either the bare or the tenant-qualified value form, while this
  API-context control blocked correctly in the same container. That is recorded as a candidate product defect
  in docs/devs/email-username-findings.md; pinning it either way would enshrine a suspected bug or fail on a
  correct fix. Add the USER rows here once the product side is settled.

  The pre-deny 200 matters: without it a later 403 could mean the API was never reachable at all.

  @cap:gateway @feat:throttling @rule:deny-enforcement @type:regression @dep:admin @dep:publisher
  Scenario Outline: An API-context deny policy refuses invocation of that API as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "denyEnfApiId" and deployed it
    When I publish the "apis" resource with id "denyEnfApiId"
    Then The lifecycle status of API "denyEnfApiId" should be "Published"
    When I retrieve the "apis" resource with id "denyEnfApiId"
    And I extract response field "context" and store it as "denyEnfCtx"
    And the "apis" resource "denyEnfApiId" should be live on the gateway, redeploying if propagation is lost
    And I wait until "apis" "denyEnfApiId" revision is deployed in the gateway

    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "denyEnfApp"
    And I create an application with payload "denyEnfApp"
    Then The response status code should be 201
    When I put the following JSON payload in context as "denyEnfKeys"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "denyEnfKeys"
    Then The response status code should be 200
    When I put the following JSON payload in context as "denyEnfSub"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "denyEnfApiId" using application "createdAppId" with payload "denyEnfSub" as "denyEnfSubId"
    Then The response status code should be 201
    When I request an OAuth access token for the current user using password grant with scope "PRODUCTION"
    Then The response status code should be 200

    # Reachable BEFORE the deny — otherwise the refusal below proves nothing.
    When I invoke the API at gateway context "{{denyEnfCtx}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""

    # The deny value is the API context WITH version, as the Admin Portal documents.
    When I create a deny policy of type "API" with value "{{denyEnfCtx}}/1.0.0" as "denyEnfPolicyId"
    Then The response status code should be 201

    # The subject: the SAME credential that just succeeded is now refused. Exactly 403, never widened — a
    # different 4xx here would be a real change in how a blocked API is reported.
    When I invoke the API at gateway context "{{denyEnfCtx}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403
    # And the backend was never reached.
    And The response should not contain "\"name\":\"John\""

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
