@cleanup
Feature: Gateway Mutual-SSL (mTLS) API Invocation

  Mutual-SSL API security: an API declared with securityScheme mutualssl / mutualssl_mandatory authenticates the
  CLIENT by its TLS certificate. The publisher uploads the accepted certificate to the API; a client presenting
  the matching certificate on the gateway HTTPS handshake is authorised (200), while a client presenting NO
  certificate is rejected (401). The gateway HTTPS listener already ships SSLVerifyClient=optional (default 4.7.0
  pack) and the container exposes 8243; the block's TOML overlay shrinks the SSL-profile read interval so an
  uploaded client certificate is picked up within seconds instead of the 10-minute default. Ports
  APISecurityMutualSSLCertificateChainValidationTestCase. Teardown via the per-scenario hook.

  @cap:gateway @feat:security-enforcement @rule:mutual-ssl @type:regression @dep:publisher @legacy:APISecurityMutualSSLCertificateChainValidationTestCase
  Scenario Outline: Invoke a mutual-SSL API with and without a client certificate as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_mutualssl_api.json" in context as "mtlsPayload"
    And I create an "apis" resource with payload "mtlsPayload" as "mtlsApiId"
    Then The response status code should be 201
    # Upload the accepted client certificate to the API
    When I upload client certificate "artifacts/certs/mutualssl/cert_chain_root.cer" with alias "certchainroot" to API "mtlsApiId" for tier "Unlimited"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "mtlsApiId"
    And I extract response field "context" and store it as "mtlsContext"
    When I deploy the API with id "mtlsApiId"
    When I publish the "apis" resource with id "mtlsApiId"
    Then The lifecycle status of API "mtlsApiId" should be "Published"
    # Presenting the matching client certificate authenticates → 200 (the uploaded cert becomes active once the
    # gateway's SSL-profile read interval — shrunk to 10s by the overlay — picks it up)
    When I invoke the API at gateway context "{{mtlsContext}}/1.0.0/customers/123" presenting client certificate "artifacts/certs/mutualssl/cert_chain_root.p12" until response status code becomes 200 within 150 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    # No client certificate on a mutualssl_mandatory API → rejected with 401
    When I invoke the API at gateway context "{{mtlsContext}}/1.0.0/customers/123" with no client certificate until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    # A NON-matching client certificate (not the one uploaded to the API) → rejected with 401
    When I invoke the API at gateway context "{{mtlsContext}}/1.0.0/customers/123" presenting client certificate "artifacts/certs/mutualssl/test.p12" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401

    # The DEFAULT-VERSION (versionless) context enforces mutual SSL identically. The API is created with
    # isDefaultVersion true, so ".../<context>/customers/123" — no version segment — routes to it through the
    # gateway's default-version dispatcher, which is a DIFFERENT routing path from the versioned one and therefore
    # has its own chance to skip the client-certificate check. Legacy asserted both contexts on every mutual-SSL
    # case (testAPIInvocationWithMutualSSLOnlyAPI / ...Negative); v2 previously asserted only the versioned one.
    When I invoke the API at gateway context "{{mtlsContext}}/customers/123" presenting client certificate "artifacts/certs/mutualssl/cert_chain_root.p12" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{mtlsContext}}/customers/123" presenting client certificate "artifacts/certs/mutualssl/test.p12" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401

    # A leaf certificate merely SIGNED BY the uploaded root is refused here (401) — certificate-chain validation
    # is opt-in and this block runs with it OFF, so the authenticator matches the presented certificate by
    # serial+subjectDN against the API's uploaded list and a chain-signed leaf never matches. The chain feature
    # is covered in gateway/mutual_ssl_chain_validation.feature, which runs in a block that enables it; this
    # row pins the DEFAULT-configuration behaviour, which that block can no longer observe.
    When I invoke the API at gateway context "{{mtlsContext}}/1.0.0/customers/123" presenting client certificate "artifacts/certs/mutualssl/cert_chain_client.p12" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # The MATCHED PAIR from APISecurityTestCase#testCreateAndPublishAPIWithOAuth2 — neither half means anything alone.
  # An OAuth2 bearer token is presented, with NO client certificate, to TWO APIs:
  #   * a mutualssl-ONLY API (mutualssl + mutualssl_mandatory, no application security at all) REFUSES it with
  #     error code 900901 in the body;
  #   * the SAME token on a mutualssl + oauth2 API returns 200.
  # The second is the positive control that makes the first a statement about the SECURITY SCHEME rather than about
  # a bad token: the very same credential is accepted next door. Legacy asserted ONLY the body code on the refusal
  # and never its HTTP status (a 200 carrying a 900901 body would have passed); this pins BOTH.
  @cap:gateway @feat:security-enforcement @rule:mutual-ssl @type:negative @dep:publisher @legacy:APISecurityTestCase
  Scenario Outline: An OAuth2 token is refused on a mutualssl-only API but accepted on a mutualssl+oauth2 API as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    # The mutualssl-ONLY API (no application security): a bearer token is not a credential it accepts.
    When I put JSON payload from file "artifacts/payloads/create_apim_mutualssl_api.json" in context as "moPayload"
    And I create an "apis" resource with payload "moPayload" as "moApiId"
    Then The response status code should be 201
    When I upload client certificate "artifacts/certs/mutualssl/cert_chain_root.cer" with alias "moCertRoot" to API "moApiId" for tier "Unlimited"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "moApiId"
    And I extract response field "context" and store it as "moContext"
    When I deploy the API with id "moApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "moApiId"
    Then The lifecycle status of API "moApiId" should be "Published"

    # The mutualssl + oauth2 API (mutual SSL OPTIONAL, application security MANDATORY) — the positive control's target.
    When I have created an api from "artifacts/payloads/create_apim_mutualssl_oauth_api.json" as "mxApiId" and deployed it
    When I publish the "apis" resource with id "mxApiId"
    Then The lifecycle status of API "mxApiId" should be "Published"
    When I retrieve the "apis" resource with id "mxApiId"
    And I extract response field "context" and store it as "mxContext"
    # One application subscribed to the oauth2-permitting API yields the token used against BOTH APIs.
    When I have set up application with keys, subscribed to API "mxApiId", and obtained access token for "mxSubId"
    Then The response status code should be 200

    # POSITIVE CONTROL: the token is valid and authorises the mutualssl + oauth2 API with no client certificate.
    When I invoke the API at gateway context "{{mxContext}}/1.0.0/customers/123" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    # The SAME token on the mutualssl-ONLY API → refused. Status AND body error code both pinned.
    When I invoke the API at gateway context "{{moContext}}/1.0.0/customers/123" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    And The response should contain "900901"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Mutual SSL MANDATORY *and* application security MANDATORY: BOTH credentials are required, and neither one alone
  # nor a forwarded-certificate header substitutes for the other. Ports testAPIInvocationWithMutualSSLMandatory
  # (cert + token → 200 on the versioned AND default-version contexts), testAPIInvocationWithMutualSSLMandatoryNeagative1
  # (a valid cert but NO token → 401), testAPIInvocationWithMutualSSLMandatoryNegative2 (a VALID token but NO
  # certificate → 401 — the strong form: an OAuth token must not stand in for the mandatory cert; v2's existing
  # mTLS scenario only sent NO credential at all, which cannot distinguish the two) and
  # testAPIInvocationWithMutualSSLHeader (a base64 certificate in X-WSO2-CLIENT-CERTIFICATE plus a valid token →
  # 401: that header is what a TLS-terminating load balancer sets, so a direct client must never pass mandatory
  # mutual SSL by setting it itself). Every case is asserted on the versioned AND the default-version context,
  # because those are different gateway routing paths.
  @cap:gateway @feat:security-enforcement @rule:mutual-ssl @type:regression @dep:publisher @legacy:APISecurityTestCase
  Scenario Outline: A mandatory-mTLS API with mandatory application security requires BOTH credentials as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I have created an api from "artifacts/payloads/create_apim_mutualssl_oauth_mandatory_api.json" as "mmApiId" and deployed it
    # Upload the accepted certificate under the SANDBOX key type — legacy uploads its mutual-SSL certificates
    # per key type via the current /client-certs/{keyType} endpoint (the un-typed POST is deprecated).
    When I upload client certificate "artifacts/certs/mutualssl/cert_chain_root.cer" with alias "mmCertRoot" and key type "SANDBOX" to API "mmApiId" for tier "Unlimited"
    Then The response status code should be 201
    When I deploy the API with id "mmApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "mmApiId"
    Then The lifecycle status of API "mmApiId" should be "Published"
    When I retrieve the "apis" resource with id "mmApiId"
    And I extract response field "context" and store it as "mmContext"
    When I have set up application with keys, subscribed to API "mmApiId", and obtained access token for "mmSubId"
    Then The response status code should be 200

    # BOTH credentials → 200, on the versioned and the default-version context.
    When I invoke the API at gateway context "{{mmContext}}/1.0.0/customers/123" presenting client certificate "artifacts/certs/mutualssl/cert_chain_root.p12" and access token "generatedAccessToken" until response status code becomes 200 within 150 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{mmContext}}/customers/123" presenting client certificate "artifacts/certs/mutualssl/cert_chain_root.p12" and access token "generatedAccessToken" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    # The accepted certificate but NO application credential → 401 (a cert alone is not enough).
    When I invoke the API at gateway context "{{mmContext}}/1.0.0/customers/123" presenting client certificate "artifacts/certs/mutualssl/cert_chain_root.p12" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401

    # A VALID OAuth token but NO client certificate → 401, on both contexts (the token does not substitute).
    When I invoke the API at gateway context "{{mmContext}}/1.0.0/customers/123" with no client certificate and access token "generatedAccessToken" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    When I invoke the API at gateway context "{{mmContext}}/customers/123" with no client certificate and access token "generatedAccessToken" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401

    # The accepted certificate's PEM base64url-encoded into X-WSO2-CLIENT-CERTIFICATE, with a valid token but NO
    # handshake certificate → 401 on both contexts. The header must never substitute for the handshake.
    When I invoke the API at gateway context "{{mmContext}}/1.0.0/customers/123" with no client certificate but certificate "artifacts/certs/mutualssl/cert_chain_root.cer" in the X-WSO2-CLIENT-CERTIFICATE header and access token "generatedAccessToken" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    When I invoke the API at gateway context "{{mmContext}}/customers/123" with no client certificate but certificate "artifacts/certs/mutualssl/cert_chain_root.cer" in the X-WSO2-CLIENT-CERTIFICATE header and access token "generatedAccessToken" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Mutual SSL OPTIONAL with application security MANDATORY — the mode where a failed/absent client certificate
  # FALLS BACK to the application credential, and the application credential is the one that must hold. Ports
  # testAPIInvocationWithMutualSSLWithOauthMandatory, ...Negative1, ...Negative2 and ...Negative3.
  #
  # FINDING — the legacy cert/keystore mapping makes two of those methods the opposite of what their names and the
  # audit rows say. Legacy uploads example.crt to this API and its own comment records "(production) example.crt ->
  # test.jks", so test.jks is the MATCHING keystore and new-keystore.jks is NOT. Yet the method named
  # "...WithOauthMandatory" (the "success scenario") presents new-keystore.jks — the NON-matching cert — so what it
  # actually proves is the mTLS-failure fallback, while "...Negative2", described as the NON-uploaded-cert case,
  # presents test.jks, the matching one, and is really the both-credentials-valid positive. v2 covers both
  # combinations explicitly and labels each by what it presents, so neither is mistaken for the other again.
  @cap:gateway @feat:security-enforcement @rule:mutual-ssl @type:regression @dep:publisher @legacy:APISecurityTestCase
  Scenario Outline: An optional-mTLS API with mandatory application security falls back to the token as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I have created an api from "artifacts/payloads/create_apim_mutualssl_oauth_api.json" as "mfApiId2" and deployed it
    When I upload client certificate "artifacts/certs/mutualssl/cert_chain_root.cer" with alias "mfCertRoot" and key type "SANDBOX" to API "mfApiId2" for tier "Unlimited"
    Then The response status code should be 201
    When I deploy the API with id "mfApiId2"
    Then The response status code should be 201
    When I publish the "apis" resource with id "mfApiId2"
    Then The lifecycle status of API "mfApiId2" should be "Published"
    When I retrieve the "apis" resource with id "mfApiId2"
    And I extract response field "context" and store it as "mfContext2"
    When I have set up application with keys, subscribed to API "mfApiId2", and obtained access token for "mfSubId2"
    Then The response status code should be 200

    # The ACCEPTED certificate plus a valid token → 200 on both contexts (both credentials good).
    When I invoke the API at gateway context "{{mfContext2}}/1.0.0/customers/123" presenting client certificate "artifacts/certs/mutualssl/cert_chain_root.p12" and access token "generatedAccessToken" until response status code becomes 200 within 150 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{mfContext2}}/customers/123" presenting client certificate "artifacts/certs/mutualssl/cert_chain_root.p12" and access token "generatedAccessToken" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    # A NON-uploaded certificate plus a valid token → 200 on both contexts: because mutual SSL is OPTIONAL the
    # failed certificate is not fatal, and the request is authorised on the OAuth token instead.
    When I invoke the API at gateway context "{{mfContext2}}/1.0.0/customers/123" presenting client certificate "artifacts/certs/mutualssl/test.p12" and access token "generatedAccessToken" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{mfContext2}}/customers/123" presenting client certificate "artifacts/certs/mutualssl/test.p12" and access token "generatedAccessToken" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    # The ACCEPTED certificate but NO application-security header → 401: application security is MANDATORY, so a
    # successful client certificate does not exempt the request from presenting an application credential.
    When I invoke the API at gateway context "{{mfContext2}}/1.0.0/customers/123" presenting client certificate "artifacts/certs/mutualssl/cert_chain_root.p12" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401

    # The ACCEPTED certificate plus an INVALID bearer token → 401 on both contexts: a valid certificate does not
    # rescue a bad token when the fallback credential is the one being checked.
    When I put the following JSON payload in context as "mfBadToken"
    """
    b0a3f9c1-4e2d-4c7a-9f11-not-a-real-token
    """
    And I invoke the API at gateway context "{{mfContext2}}/1.0.0/customers/123" presenting client certificate "artifacts/certs/mutualssl/cert_chain_root.p12" and access token "mfBadToken" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    When I invoke the API at gateway context "{{mfContext2}}/customers/123" presenting client certificate "artifacts/certs/mutualssl/cert_chain_root.p12" and access token "mfBadToken" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
