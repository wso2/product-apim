@cap:admin @feat:external-key-manager @rule:grant-types @type:regression @dep:gateway
Feature: External Key Manager OAuth Grant Types

  Verifies that every IS7-supported OAuth grant issues an access token that passes the gateway (invocation 200).
  Reuses the API / application / IS client credentials provisioned by _setup_is7_grant_app in the same runner.
  The gateway validates the token, not the grant that minted it, so each scenario proves the IS7 token-acquisition
  flow end to end. The authoritative grant set comes from IS 7.3.0's discovery document (grant_types_supported).
  client_credentials and password are already covered by is7_key_manager_boot.feature. The only parked grant is
  iwa:ntlm (Integrated Windows Auth - not feasible headless on Linux containers). token-exchange is covered
  separately in the token-exchange feature.

  Scenario Outline: The refresh_token grant issues a working access token as <actor>
    Given I act as "<actor>"
    And I use the token-exchange fixture for the acting tenant
    # Obtain an initial token (with a refresh token) from IS via the password grant, then exchange the refresh
    # token for a fresh access token and prove that new token is accepted at the gateway.
    When I request an OAuth access token from the external key manager using password grant as "admin" with password "admin" requesting scope "openid"
    Then The response status code should be 200
    When I request a new OAuth access token from the external key manager using the stored refresh token
    Then The response status code should be 200
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  Scenario Outline: The authorization_code grant issues a working access token as <actor>
    Given I act as "<actor>"
    And I use the token-exchange fixture for the acting tenant
    # Headless authorization_code flow against IS (authorize -> login at /commonauth -> code -> token exchange);
    # IS skips consent for the DCR-registered client, so no browser is needed.
    When I request an OAuth access token from the external key manager using authorization code grant as "admin" with password "admin"
    Then The response status code should be 200
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  Scenario Outline: The authorization_code grant with PKCE issues a working access token as <actor>
    Given I act as "<actor>"
    And I use the token-exchange fixture for the acting tenant
    # Same flow as above with PKCE (S256): the authorize request carries a code_challenge and the token exchange
    # presents the matching code_verifier.
    When I request an OAuth access token from the external key manager using authorization code grant with PKCE as "admin" with password "admin"
    Then The response status code should be 200
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  Scenario Outline: The device_code grant issues a working access token as <actor>
    Given I act as "<actor>"
    And I use the token-exchange fixture for the acting tenant
    # RFC 8628 device flow, headless: request a device/user code, approve it by authenticating at IS (login
    # commits the approval since IS skips consent for the DCR client), then exchange the device_code for a token.
    When I request an OAuth access token from the external key manager using device code grant as "admin" with password "admin"
    Then The response status code should be 200
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  Scenario Outline: The jwt-bearer grant issues a working access token as <actor>
    Given I act as "<actor>"
    And I use the token-exchange fixture for the acting tenant
    # RFC 7523 flow: register a trusted IdP in IS via its IdP REST API (name = assertion issuer, the committed
    # test cert to validate the signature, token-endpoint alias to validate the audience), then exchange a
    # self-signed RS256 assertion at the token endpoint. IS 7.x has no SOAP admin services, so REST is the only
    # IdP registration path; without the IdP, IS rejects the assertion with "No registered identity provider
    # found for the JWT with issuer name".
    When I register a JWT bearer identity provider in the external key manager storing its name as "jwtTrustedIdpName"
    Then The response status code should be 201
    When I request an OAuth access token from the external key manager using JWT bearer grant with issuer stored as "jwtTrustedIdpName"
    Then The response status code should be 200
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  Scenario Outline: The saml2-bearer grant issues a working access token as <actor>
    Given I act as "<actor>"
    And I use the token-exchange fixture for the acting tenant
    # RFC 7522 flow: register a trusted SAML IdP in IS via its IdP REST API (SAML authenticator IdPEntityId =
    # assertion issuer - unlike jwt-bearer, name-only resolution fails with "Identity provider is null" - plus
    # the committed test cert and token-endpoint alias), then exchange an enveloped-signed SAML 2.0 assertion
    # (RSA-SHA256, bearer subject confirmation, audience = the alias) at the token endpoint.
    When I register a SAML bearer identity provider in the external key manager storing its name as "samlTrustedIdpName"
    Then The response status code should be 201
    When I request an OAuth access token from the external key manager using SAML bearer grant with issuer stored as "samlTrustedIdpName"
    Then The response status code should be 200
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
