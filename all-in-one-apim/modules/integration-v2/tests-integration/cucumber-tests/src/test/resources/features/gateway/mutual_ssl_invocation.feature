@cleanup
Feature: Gateway Mutual-SSL (mTLS) API Invocation

  Mutual-SSL API security: an API declared with securityScheme mutualssl / mutualssl_mandatory authenticates the
  CLIENT by its TLS certificate. The publisher uploads the accepted certificate to the API; a client presenting
  the matching certificate on the gateway HTTPS handshake is authorised (200), while a client presenting NO
  certificate is rejected (401). The gateway HTTPS listener already ships SSLVerifyClient=optional (default 4.7.0
  pack) and the container exposes 8243, so no config overlay is needed. Ports
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
    When I invoke the API at gateway context "{{mtlsContext}}/1.0.0/customers/123" presenting client certificate "artifacts/certs/mutualssl/cert_chain_root.jks" until response status code becomes 200 within 150 seconds
    Then The response status code should be 200
    # No client certificate on a mutualssl_mandatory API → rejected with 401
    When I invoke the API at gateway context "{{mtlsContext}}/1.0.0/customers/123" with no client certificate until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    # A NON-matching client certificate (not the one uploaded to the API) → rejected with 401
    When I invoke the API at gateway context "{{mtlsContext}}/1.0.0/customers/123" presenting client certificate "artifacts/certs/mutualssl/test.jks" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |
