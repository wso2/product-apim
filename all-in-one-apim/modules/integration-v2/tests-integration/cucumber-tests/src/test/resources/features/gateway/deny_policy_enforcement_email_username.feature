@cleanup
Feature: Deny Policy Enforcement For Email-Form Principals

  The same API-context deny enforcement as gateway/deny_policy_enforcement.feature, but driven by a principal
  whose USERNAME IS AN EMAIL ADDRESS, in the IntegrationV2-EmailUserName block (emailUserMode=true,
  enable_email_domain=true, initBackend=true).

  WHY THE DIMENSION IS NOT REDUNDANT. Blocking is evaluated on the DATA plane, where the gateway first resolves
  the caller and then checks the blocking conditions in the same pass. An "@" in the principal's name changes
  how that resolution splits the name (see gateway/basic_auth_email_username.feature), so this pins that a deny
  still applies when the caller resolution is the awkward case. A plain-username run cannot show that.

  The deny used is API-CONTEXT, which keys on the API and not on the caller — deliberately, so this scenario
  fails only if BLOCKING breaks, not if caller resolution changes. USER-type deny is excluded for the reason
  recorded in gateway/deny_policy_enforcement.feature and docs/devs/email-username-findings.md: on 4.7.0 it does
  not block for ANY username form, plain or email, so it is a candidate product defect rather than coverage.

  @cap:gateway @feat:throttling-enforcement @rule:deny-enforcement @type:regression @dep:admin @dep:publisher
  Scenario Outline: An API-context deny refuses an email-form principal's invocation as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"

    # Pin the acting principal's physical username first — if the block ever provisioned plain usernames this
    # scenario would pass while testing the wrong thing.
    When I store the acting actor credentials as "denyEmailUser" and "denyEmailPass"
    Then the actual value of "denyEmailUser" should match the expected value:
      """
      <expectedUsername>
      """

    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "denyEmApiId" and deployed it
    When I publish the "apis" resource with id "denyEmApiId"
    Then The lifecycle status of API "denyEmApiId" should be "Published"
    When I retrieve the "apis" resource with id "denyEmApiId"
    And I extract response field "context" and store it as "denyEmCtx"
    And the "apis" resource "denyEmApiId" should be live on the gateway, redeploying if propagation is lost
    And I wait until "apis" "denyEmApiId" revision is deployed in the gateway

    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "denyEmApp"
    And I create an application with payload "denyEmApp"
    Then The response status code should be 201
    When I put the following JSON payload in context as "denyEmKeys"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "denyEmKeys"
    Then The response status code should be 200
    When I put the following JSON payload in context as "denyEmSub"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "denyEmApiId" using application "createdAppId" with payload "denyEmSub" as "denyEmSubId"
    Then The response status code should be 201
    When I request an OAuth access token for the current user using password grant with scope "PRODUCTION"
    Then The response status code should be 200

    # Reachable BEFORE the deny, as the email-form principal.
    When I invoke the API at gateway context "{{denyEmCtx}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""

    When I create a deny policy of type "API" with value "{{denyEmCtx}}/1.0.0" as "denyEmPolicyId"
    Then The response status code should be 201

    When I invoke the API at gateway context "{{denyEmCtx}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403
    And The response should not contain "\"name\":\"John\""

    Examples:
      | actor                  | expectedUsername                  |
      | emailAdmin             | emailAdmin@email.com@carbon.super |
      | emailAdmin@tenant1.com | emailAdmin@email.com@tenant1.com  |
