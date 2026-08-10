Feature: Gateway Mutual-SSL and Application-Security Matrix

  The mutual-SSL x application-security enforcement matrix of APISecurityTestCase. An API declares mutual SSL as
  optional or mandatory (securityScheme mutualssl / mutualssl_mandatory) INDEPENDENTLY of whether application
  security is mandatory (oauth_basic_auth_api_key_mandatory), and the gateway enforces each gate separately: a
  bearer token cannot stand in for a mandatory client certificate, a client certificate cannot stand in for
  mandatory application security, and a valid certificate must not rescue an invalid token. Each cell asserts its
  own exact status (and, where the legacy asserts one, the exact error code) — never a widened "4xx".

  Every case is asserted at BOTH the versioned context (/ctx/1.0.0) and the versionless one (/ctx), which the
  default-version route serves — the legacy class asserts both for every case, and a security gate that held on
  the versioned path but not on the default-version path would be a bypass.

  The three APIs, the accepted client certificate on each, and the application subscribed to the two
  OAuth-bearing ones come from _setup_mutual_ssl_security_matrix (listed first in the runner). Teardown is the
  runner's AfterClass sweep, so this feature is deliberately NOT tagged @cleanup — a per-scenario sweep would
  delete the shared fixture out from under the scenarios that follow.

  Scenario ORDER is load-bearing for the mandatory-mTLS API: an uploaded client certificate reaches the gateway
  HTTPS listener only on its next dynamic SSL-profile read (shrunk to 10s by the block's overlay), so the
  positive "certificate + token -> 200" case runs BEFORE that API's negatives. Without it a negative could pass
  for the wrong reason — 401 because the certificate was not live yet rather than because the gate held.

  Runs x2 tenants so every security-scheme combination is verified independently per tenant —
  mutual_ssl_invocation.feature already proves mTLS enforcement is tenant-agnostic by running x2 tenants.

  # --- mutual SSL MANDATORY, no application security -----------------------------------------------------------
  # Ports testCreateAndPublishAPIWithOAuth2 (first half): a bearer token is not a substitute for the client
  # certificate the API mandates. The legacy asserts the error CODE, not just the status: 900901 = Invalid
  # Credentials. The token is a valid, live token for OTHER APIs in the same application, which is what makes
  # this a real refusal rather than a bad-token rejection. The code is pinned as the exact JSON field the
  # gateway's _auth_failure_handler_ emits ({"code":"$ERROR_CODE","message":...}), not a bare digit match.
  @cap:gateway @feat:security-enforcement @rule:mutual-ssl @type:negative @dep:publisher @legacy:APISecurityTestCase
  Scenario Outline: A mutual-SSL-mandatory-only API refuses a valid OAuth2 bearer token
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I invoke the API at gateway context "{{mtlsOnlyContext<suffix>}}/1.0.0/customers/123" with method "GET" using access token "mtlsAccessToken<suffix>" and payload "" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    And The response should contain "\"code\":\"900901\""
    When I invoke the API at gateway context "{{mtlsOnlyContext<suffix>}}/customers/123" with method "GET" using access token "mtlsAccessToken<suffix>" and payload "" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    And The response should contain "\"code\":\"900901\""

  # --- mutual SSL OPTIONAL + application security MANDATORY ----------------------------------------------------
  # Ports testCreateAndPublishAPIWithOAuth2 (second half): the SAME token the mandatory-mTLS API refuses above is
  # accepted here with no client certificate at all, because mutual SSL is only OPTIONAL on this API.

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |
  @cap:gateway @feat:security-enforcement @rule:mutual-ssl @type:regression @dep:publisher @legacy:APISecurityTestCase
  Scenario Outline: A mutual-SSL-optional OAuth2 API accepts a bearer token with no client certificate
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I invoke the API at gateway context "{{mtlsOptionalContext<suffix>}}/1.0.0/customers/123" with method "GET" using access token "mtlsAccessToken<suffix>" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "{\"id\":123,\"name\":\"John\"}"
    When I invoke the API at gateway context "{{mtlsOptionalContext<suffix>}}/customers/123" with method "GET" using access token "mtlsAccessToken<suffix>" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "{\"id\":123,\"name\":\"John\"}"

  # Ports testAPIInvocationWithMutualSSLWithOauthMandatory: both credentials presented together are accepted.
  # What this adds over the previous scenario is that presenting a certificate does not BREAK the OAuth path —
  # the 200 here is attributable to the token alone, because mutual SSL is optional on this API. The certificate
  # gate itself is proven live further down, on the API that mandates it.

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |
  @cap:gateway @feat:security-enforcement @rule:mutual-ssl @type:regression @dep:publisher @legacy:APISecurityTestCase
  Scenario Outline: A mutual-SSL-optional OAuth2 API accepts the accepted client certificate together with a bearer token
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I invoke the API at gateway context "{{mtlsOptionalContext<suffix>}}/1.0.0/customers/123" presenting client certificate "artifacts/certs/mutualssl/cert_chain_root.p12" and access token "mtlsAccessToken<suffix>" until response status code becomes 200 within 150 seconds
    Then The response status code should be 200
    And The response should contain "{\"id\":123,\"name\":\"John\"}"
    When I invoke the API at gateway context "{{mtlsOptionalContext<suffix>}}/customers/123" presenting client certificate "artifacts/certs/mutualssl/cert_chain_root.p12" and access token "mtlsAccessToken<suffix>" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "{\"id\":123,\"name\":\"John\"}"

  # Ports testAPIInvocationWithMutualSSLWithOauthMandatoryNegative1: application security is MANDATORY on this
  # API, so the accepted client certificate alone cannot authorise the call — it is refused with 401 even though
  # mutual SSL succeeded.

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |
  @cap:gateway @feat:security-enforcement @rule:mutual-ssl @type:negative @dep:publisher @legacy:APISecurityTestCase
  Scenario Outline: A mutual-SSL-optional API with mandatory application security refuses a client certificate with no Authorization header
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I invoke the API at gateway context "{{mtlsOptionalContext<suffix>}}/1.0.0/customers/123" presenting client certificate "artifacts/certs/mutualssl/cert_chain_root.p12" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    When I invoke the API at gateway context "{{mtlsOptionalContext<suffix>}}/customers/123" presenting client certificate "artifacts/certs/mutualssl/cert_chain_root.p12" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401

  # Ports testAPIInvocationWithMutualSSLWithOauthMandatoryNegative2: presenting a certificate that is NOT the one
  # uploaded to the API is not an error when mutual SSL is optional — the OAuth token alone still authorises the
  # call (200). The same certificate is refused (401) by the mandatory-mTLS API in mutual_ssl_invocation.feature,
  # which is what makes "optional" observable here.

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |
  @cap:gateway @feat:security-enforcement @rule:mutual-ssl @type:regression @dep:publisher @legacy:APISecurityTestCase
  Scenario Outline: A mutual-SSL-optional API accepts a bearer token presented with a non-matching client certificate
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I invoke the API at gateway context "{{mtlsOptionalContext<suffix>}}/1.0.0/customers/123" presenting client certificate "artifacts/certs/mutualssl/test.p12" and access token "mtlsAccessToken<suffix>" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "{\"id\":123,\"name\":\"John\"}"
    When I invoke the API at gateway context "{{mtlsOptionalContext<suffix>}}/customers/123" presenting client certificate "artifacts/certs/mutualssl/test.p12" and access token "mtlsAccessToken<suffix>" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "{\"id\":123,\"name\":\"John\"}"

  # Ports testAPIInvocationWithMutualSSLWithOauthMandatoryNegative3: the accepted client certificate must NOT
  # rescue a garbage bearer token — a successful mutual-SSL handshake does not downgrade mandatory application
  # security to "any credential will do". 401, not 200.

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |
  @cap:gateway @feat:security-enforcement @rule:mutual-ssl @type:negative @dep:publisher @legacy:APISecurityTestCase
  Scenario Outline: The accepted client certificate does not rescue an invalid bearer token on a mutual-SSL-optional API
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I generate a unique value and store it as "mtlsInvalidToken<suffix>"
    And I invoke the API at gateway context "{{mtlsOptionalContext<suffix>}}/1.0.0/customers/123" presenting client certificate "artifacts/certs/mutualssl/cert_chain_root.p12" and access token "mtlsInvalidToken<suffix>" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    When I invoke the API at gateway context "{{mtlsOptionalContext<suffix>}}/customers/123" presenting client certificate "artifacts/certs/mutualssl/cert_chain_root.p12" and access token "mtlsInvalidToken<suffix>" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401

  # --- mutual SSL MANDATORY + application security MANDATORY --------------------------------------------------
  # Ports testAPIInvocationWithMutualSSLMandatory. FIRST of this API's scenarios on purpose: it is the only one
  # whose 200 proves the uploaded certificate is live at the listener, so the negatives that follow cannot pass
  # for the wrong reason.

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |
  @cap:gateway @feat:security-enforcement @rule:mutual-ssl @type:regression @dep:publisher @legacy:APISecurityTestCase
  Scenario Outline: An API with mutual SSL and application security both mandatory accepts a certificate together with a bearer token
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I invoke the API at gateway context "{{mtlsBothContext<suffix>}}/1.0.0/customers/123" presenting client certificate "artifacts/certs/mutualssl/cert_chain_root.p12" and access token "mtlsAccessToken<suffix>" until response status code becomes 200 within 150 seconds
    Then The response status code should be 200
    And The response should contain "{\"id\":123,\"name\":\"John\"}"
    When I invoke the API at gateway context "{{mtlsBothContext<suffix>}}/customers/123" presenting client certificate "artifacts/certs/mutualssl/cert_chain_root.p12" and access token "mtlsAccessToken<suffix>" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "{\"id\":123,\"name\":\"John\"}"

  # Ports testAPIInvocationWithMutualSSLMandatoryNeagative1: the certificate gate is satisfied but the
  # application-security gate is not. Verified live to be exactly that and not a certificate failure — the
  # gateway logs "Missing Credentials userName=cert-chain-root", i.e. mutual SSL authenticated the client as the
  # uploaded certificate's subject and the 401 came from the absent application credential.

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |
  @cap:gateway @feat:security-enforcement @rule:mutual-ssl @type:negative @dep:publisher @legacy:APISecurityTestCase
  Scenario Outline: An API with both mandatory refuses the accepted client certificate with no Authorization header
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I invoke the API at gateway context "{{mtlsBothContext<suffix>}}/1.0.0/customers/123" presenting client certificate "artifacts/certs/mutualssl/cert_chain_root.p12" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    When I invoke the API at gateway context "{{mtlsBothContext<suffix>}}/customers/123" presenting client certificate "artifacts/certs/mutualssl/cert_chain_root.p12" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401

  # Ports testAPIInvocationWithMutualSSLMandatoryNegative2: the mirror image — the application-security gate is
  # satisfied by a valid subscribed token, but no client certificate is offered on the handshake.

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |
  @cap:gateway @feat:security-enforcement @rule:mutual-ssl @type:negative @dep:publisher @legacy:APISecurityTestCase
  Scenario Outline: An API with both mandatory refuses a valid bearer token presented with no client certificate
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I invoke the API at gateway context "{{mtlsBothContext<suffix>}}/1.0.0/customers/123" with method "GET" using access token "mtlsAccessToken<suffix>" and payload "" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    When I invoke the API at gateway context "{{mtlsBothContext<suffix>}}/customers/123" with method "GET" using access token "mtlsAccessToken<suffix>" and payload "" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401

  # Ports testAPIInvocationWithMutualSSLHeader — a security assertion with no other v2 replacement. The gateway
  # reads a forwarded client certificate from apimgt.mutual_ssl.certificate_header (shipped default
  # X-WSO2-CLIENT-CERTIFICATE) but ONLY from a trusted proxy: with the shipped defaults
  # (enable_client_validation = True, forward_client_certificate_header = False) a client that sets the header
  # itself must NOT authenticate. The certificate sent is the ACCEPTED one, base64-encoded exactly as the legacy
  # encodes it, so the only thing missing is the real TLS handshake.

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |
  @cap:gateway @feat:security-enforcement @rule:mutual-ssl @type:negative @dep:publisher @legacy:APISecurityTestCase
  Scenario Outline: A base64 certificate in the client-certificate header does not substitute for a mutual-SSL handshake
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I store the base64-encoded certificate "artifacts/certs/mutualssl/cert_chain_root.cer" in context as "spoofedClientCert"
    And I invoke the API at gateway context "{{mtlsBothContext<suffix>}}/1.0.0/customers/123" with method "GET" using access token "mtlsAccessToken<suffix>" and payload "" with request header "X-WSO2-CLIENT-CERTIFICATE" set to "{{spoofedClientCert}}" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    When I invoke the API at gateway context "{{mtlsBothContext<suffix>}}/customers/123" with method "GET" using access token "mtlsAccessToken<suffix>" and payload "" with request header "X-WSO2-CLIENT-CERTIFICATE" set to "{{spoofedClientCert}}" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401

  # --- internal key scoping ------------------------------------------------------------------------------------
  # Ports the remainder of testCreateAndDeployRevisionWithInternalKeyTesting: a publisher internal API key is
  # scoped to the API it was issued for. Its own API answers 200 (the control — and, since this API is already
  # PUBLISHED, also the legacy "internal key still authenticates after publish" leg), while the SAME key on a
  # different deployed API is refused with 403, not 401: the credential is valid, it just does not carry that API.
  # The 200 leg needs no client certificate on either API because an internal-key call is not mutual-SSL
  # authenticated — mtlsOptionalApi<suffix> declares mutual SSL optional, so the key alone suffices.

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |
  @cap:gateway @feat:security-enforcement @rule:internal-key @type:negative @dep:publisher @legacy:APISecurityTestCase
  Scenario Outline: A publisher internal API key is scoped to the API it was issued for
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I generate an internal API key for API "mtlsOptionalApiId<suffix>" and store it as "mtlsOptionalInternalKey<suffix>"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{mtlsOptionalContext<suffix>}}/1.0.0/customers/123" with method "GET" using internal key "mtlsOptionalInternalKey<suffix>" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "{\"id\":123,\"name\":\"John\"}"
    When I generate an internal API key for API "mtlsBothApiId<suffix>" and store it as "mtlsBothInternalKey<suffix>"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{mtlsOptionalContext<suffix>}}/1.0.0/customers/123" with method "GET" using internal key "mtlsBothInternalKey<suffix>" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403

  # Also from testCreateAndDeployRevisionWithInternalKeyTesting (its httpResponse4 leg): the publisher internal key
  # authenticates a mutualssl_mandatory API over HTTPS with NO client certificate on the handshake — the
  # publisher try-out path is not subject to the API's mutual-SSL mandate. Asserted separately from the scoping
  # scenario because it is a different property, and it mints its own key rather than relying on another
  # scenario's artifact.

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |
  @cap:gateway @feat:security-enforcement @rule:internal-key @type:regression @dep:publisher @legacy:APISecurityTestCase
  Scenario Outline: A publisher internal API key authenticates a mutual-SSL-mandatory API without a client certificate
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I generate an internal API key for API "mtlsBothApiId<suffix>" and store it as "mtlsBypassInternalKey<suffix>"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{mtlsBothContext<suffix>}}/1.0.0/customers/123" with method "GET" using internal key "mtlsBypassInternalKey<suffix>" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "{\"id\":123,\"name\":\"John\"}"

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |
