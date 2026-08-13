@cap:admin @feat:external-key-manager @rule:roles @dep:gateway
Feature: External Key Manager Role-Based Authorization Enforcement

  Verifies that a scope-protected API operation is accessible only with the mapped IS7 role. The scope is bound to
  a plain APIM role; the WSO2-IS-7 connector creates the derived IS role (system_primary_<role>) and its scope
  binding in IS. IS grants the scope in a token only to a user holding that role, and the gateway enforces the
  operation's scope. Reuses the scope-protected API / application / IS users provisioned by
  _setup_is7_role_enforcement in the same runner.

  @type:regression
  Scenario: A user with the mapped IS7 role can invoke the scope-protected operation
    # is7roleuser holds system_primary_<role>, so IS issues a token carrying the scope -> gateway allows (200).
    When I request an OAuth access token from the external key manager using password grant as "{{is7roleuser}}" with password "Wso2Test123!" requesting scope "openid {{is7ScopeName}}"
    Then The response status code should be 200
    And I invoke the API at gateway context "{{scopedApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

  @type:negative
  Scenario: A user without the mapped IS7 role is forbidden from the scope-protected operation
    # is7noroleuser lacks the role, so IS issues a token WITHOUT the scope -> gateway forbids the operation (403).
    When I request an OAuth access token from the external key manager using password grant as "{{is7noroleuser}}" with password "Wso2Test123!" requesting scope "openid {{is7ScopeName}}"
    Then The response status code should be 200
    And I invoke the API at gateway context "{{scopedApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403
