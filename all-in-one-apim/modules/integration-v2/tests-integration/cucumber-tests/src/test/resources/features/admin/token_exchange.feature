@cap:admin @feat:external-key-manager @rule:token-exchange
Feature: External Key Manager Token Exchange

  Verifies the token-exchange grant end to end, x2 tenant (carbon.super and tenant1.com): a JWT subject token
  issued by an external WSO2 IS 7.x is exchanged at API Manager's own token endpoint (authenticated with the
  application's Resident-Key-Manager credentials) for an at+jwt access token that then invokes a gateway-deployed
  API. Each scenario acts as its tenant's admin and selects that tenant's fixture (API, application, keys)
  provisioned by _setup_token_exchange; the trusted identity provider that validates the subject token is
  registered in the acting tenant. The subject token itself is tenant-agnostic (one external IS subject app). The
  two signature-validation shapes are both covered - a static PEM certificate (from IS's live JWKS x5c) and IS's
  JWKS endpoint.

  This block also runs with enforce_type_header_validation on, so it additionally pins that only at+jwt access
  tokens invoke gateway APIs: the exchanged token (at+jwt) is accepted, an OIDC id_token (no at+jwt type header)
  is refused.

  @type:regression @dep:gateway
  Scenario Outline: Exchange succeeds and invokes the gateway - PEM certificate validation as <actor>
    Given I act as "<actor>"
    And I use the token-exchange fixture for the acting tenant
    When I register the token-exchange trusted identity provider "TxTrustedIdp" using PEM certificate validation
    And I obtain a subject JWT from the identity server
    And I exchange the subject token at the API Manager token endpoint
    Then The response status code should be 200
    And the generated access token should have the "at+jwt" type header
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @type:regression @dep:gateway
  Scenario Outline: Exchange succeeds and invokes the gateway - JWKS validation as <actor>
    Given I act as "<actor>"
    And I use the token-exchange fixture for the acting tenant
    When I register the token-exchange trusted identity provider "TxTrustedIdp" using JWKS validation
    And I obtain a subject JWT from the identity server
    And I exchange the subject token at the API Manager token endpoint
    Then The response status code should be 200
    And the generated access token should have the "at+jwt" type header
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @type:negative @dep:gateway
  Scenario Outline: An id_token cannot invoke a gateway API when type-header validation is enforced as <actor>
    # enforce_type_header_validation is on for this block; an OIDC id_token (no at+jwt type header) must be
    # refused at the gateway even though it is a valid Resident-KM-signed JWT for a subscribed application.
    Given I act as "<actor>"
    And I use the token-exchange fixture for the acting tenant
    When I obtain an id_token for the application as the acting user
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @type:regression
  Scenario Outline: The exchanged token carries the subject's federated identity as <actor>
    Given I act as "<actor>"
    And I use the token-exchange fixture for the acting tenant
    When I register the token-exchange trusted identity provider "TxTrustedIdp" using JWKS validation
    And I obtain a subject JWT from the identity server
    And I exchange the subject token at the API Manager token endpoint
    Then The response status code should be 200
    And the exchanged access token subject should match the identity server subject application

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @type:negative
  Scenario Outline: An expired subject token is rejected as <actor>
    Given I act as "<actor>"
    And I use the token-exchange fixture for the acting tenant
    When I register the token-exchange trusted identity provider "TxTrustedIdp" using JWKS validation
    And I obtain an expired subject JWT from the identity server
    And I exchange the subject token at the API Manager token endpoint
    Then The response status code should be 400

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @type:regression
  Scenario Outline: Key-rotation canary - JWKS follows the live signing key, a stale PEM certificate does not as <actor>
    # A PEM certificate pinned to a key other than IS's current signer (the post-key-rotation state) rejects a
    # live subject token, while JWKS validation - which re-fetches IS's keys - accepts it. Re-uploading the
    # current certificate restores PEM validation: JWKS auto-recovers from a signing-key rotation, PEM must be
    # re-uploaded.
    Given I act as "<actor>"
    And I use the token-exchange fixture for the acting tenant
    When I register the token-exchange trusted identity provider "TxTrustedIdp" using a stale PEM certificate
    And I obtain a subject JWT from the identity server
    And I exchange the subject token at the API Manager token endpoint
    Then The response status code should be 400
    When I register the token-exchange trusted identity provider "TxTrustedIdp" using JWKS validation
    And I obtain a subject JWT from the identity server
    And I exchange the subject token at the API Manager token endpoint
    Then The response status code should be 200
    When I register the token-exchange trusted identity provider "TxTrustedIdp" using PEM certificate validation
    And I obtain a subject JWT from the identity server
    And I exchange the subject token at the API Manager token endpoint
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @type:negative
  Scenario Outline: A tampered subject token is rejected as <actor>
    Given I act as "<actor>"
    And I use the token-exchange fixture for the acting tenant
    When I register the token-exchange trusted identity provider "TxTrustedIdp" using JWKS validation
    And I obtain a subject JWT from the identity server
    And I exchange a tampered subject token at the API Manager token endpoint
    Then The response status code should be 400

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @type:negative
  Scenario Outline: A token exchange with no subject token is rejected as <actor>
    Given I act as "<actor>"
    And I use the token-exchange fixture for the acting tenant
    When I register the token-exchange trusted identity provider "TxTrustedIdp" using JWKS validation
    And I attempt a token exchange with no subject token
    Then The response status code should be 400

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @type:negative
  Scenario Outline: An application whose keys lack the token-exchange grant is refused as <actor>
    Given I act as "<actor>"
    And I use the token-exchange fixture for the acting tenant
    When I register the token-exchange trusted identity provider "TxTrustedIdp" using JWKS validation
    And I obtain a subject JWT from the identity server
    And I attempt a token exchange using the grantless application credentials
    Then The response status code should be 400

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @type:negative
  Scenario Outline: A trusted identity provider with a malformed certificate is refused at registration as <actor>
    # The wrong-format-certificate pitfall (e.g. a keystore blob pasted where the X.509 cert belongs) is caught
    # at CONFIGURATION time: addIdP validates the certificate and refuses the registration, so the broken IdP
    # never exists and a subsequent exchange fails as untrusted-issuer (400) - no hang, no 500.
    Given I act as "<actor>"
    And I use the token-exchange fixture for the acting tenant
    When I attempt to register the token-exchange trusted identity provider "TxTrustedIdp" using a malformed certificate
    Then the token-exchange trusted identity provider "TxTrustedIdp" should not exist
    When I obtain a subject JWT from the identity server
    And I exchange the subject token at the API Manager token endpoint
    Then The response status code should be 400

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @type:negative
  Scenario Outline: A subject token from an untrusted issuer is rejected as <actor>
    # With no trusted IdP whose idpIssuerName matches the subject token's iss, the handler has nothing to
    # validate against and refuses the exchange.
    Given I act as "<actor>"
    And I use the token-exchange fixture for the acting tenant
    When I remove the token-exchange trusted identity provider "TxTrustedIdp"
    And I obtain a subject JWT from the identity server
    And I exchange the subject token at the API Manager token endpoint
    Then The response status code should be 400

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
