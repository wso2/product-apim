@cap:admin @feat:external-key-manager @rule:token-exchange @type:negative
Feature: External Key Manager Token Exchange Disabled

  Verifies that when the token-exchange grant is disabled in server config
  ([oauth.grant_type.token_exchange] enable=false), the token endpoint refuses a token-exchange request. The
  token endpoint reports the disabled grant as invalid_request / "Unsupported grant_type value" (HTTP 400). Runs
  in its own block (testng-is7txdisabled.xml) because the main token-exchange block needs the grant enabled.

  Scenario Outline: A token-exchange request is refused when the grant is disabled as <actor>
    Given I act as "<actor>"
    And I use the token-exchange fixture for the acting tenant
    When I attempt a token exchange with a placeholder subject token
    Then The response status code should be 400
    And The response should contain "Unsupported grant_type value"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
