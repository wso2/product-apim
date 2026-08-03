@cap:admin @feat:external-key-manager @rule:discovery @type:smoke
Feature: External Key Manager Well-Known Discovery

  Verifies the admin key-manager discovery endpoint (POST /key-managers/discover) against the external IS 7.3.0's
  OIDC well-known document: APIM fetches the document server-side and maps it to a KeyManager configuration, so
  the populated endpoints, grant types and JWKS certificate are pinned here exactly as IS advertises them.
  Deliberately also pins the documented UserInfo auto-populate gotcha: discovery fills userInfoEndpoint with IS's
  oauth2/userinfo, while the WSO2-IS-7 connector actually needs scim2/Me - an admin importing via well-known must
  correct that field by hand, and this scenario is the regression canary that the gotcha still exists.

  Scenario: Discovery from the IS well-known document populates the key manager configuration
    Given The system is ready
    And I have valid access tokens as "admin"
    When I discover key manager configuration from the external key manager well-known endpoint
    Then The response status code should be 200
    And The value of response field "valid" should be "true"
    And The value of response field "value.type" should be "WSO2-IS-7"
    And The value of response field "value.wellKnownEndpoint" should be "https://wso2is:9443/oauth2/token/.well-known/openid-configuration"
    And The value of response field "value.issuer" should be "https://wso2is:9443/oauth2/token"
    And The value of response field "value.tokenEndpoint" should be "https://wso2is:9443/oauth2/token"
    And The value of response field "value.authorizeEndpoint" should be "https://wso2is:9443/oauth2/authorize"
    And The value of response field "value.revokeEndpoint" should be "https://wso2is:9443/oauth2/revoke"
    And The value of response field "value.introspectionEndpoint" should be "https://wso2is:9443/oauth2/introspect"
    And The value of response field "value.clientRegistrationEndpoint" should be "https://wso2is:9443/api/identity/oauth2/dcr/v1.1/register"
    And The value of response field "value.certificates.type" should be "JWKS"
    And The value of response field "value.certificates.value" should be "https://wso2is:9443/oauth2/jwks"
    # The UserInfo auto-populate gotcha, pinned: oauth2/userinfo from discovery, NOT the scim2/Me the connector needs.
    And The value of response field "value.userInfoEndpoint" should be "https://wso2is:9443/oauth2/userinfo"
    # Grant list is populated from IS's grant_types_supported (membership of the key grants).
    And The response should contain "urn:ietf:params:oauth:grant-type:jwt-bearer"
    And The response should contain "urn:ietf:params:oauth:grant-type:saml2-bearer"
    And The response should contain "urn:ietf:params:oauth:grant-type:token-exchange"
    And The response should contain "urn:ietf:params:oauth:grant-type:device_code"
