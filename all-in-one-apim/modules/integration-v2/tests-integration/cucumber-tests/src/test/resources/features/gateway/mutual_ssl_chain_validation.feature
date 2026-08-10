@cleanup
Feature: Gateway Mutual-SSL Certificate Chain Validation

  Mutual-SSL certificate CHAIN validation — the opt-in mode in which a client is authorised by the ISSUER PATH
  of the certificate it presents rather than by the certificate itself. Ports
  APISecurityMutualSSLCertificateChainValidationTestCase.

  Only the ROOT (cert_chain_root.cer) is uploaded to the API. The certificates presented below are NOT that
  certificate: they are a LEAF (CN=cert-chain-client) issued by an INTERMEDIATE (CN=cert-chain-intermediate)
  which is itself issued by the uploaded root. Authorising them is therefore only possible by walking the
  issuer chain up to the uploaded trust anchor — which is exactly what the sibling feature
  gateway/mutual_ssl_invocation.feature shows does NOT happen by default, where the same leaf is refused 401.

    cert_chain_client.jks            chain length 3 — leaf + intermediate + root presented on the handshake
    cert_chain_client_head_only.jks  chain length 1 — the SAME leaf exported ALONE, no chain

  This block therefore exists solely to carry `[apimgt.mutual_ssl] enable_certificate_chain_validation = true`,
  which selects a different authenticator code path and so cannot share a container with the default-mode
  mutual-SSL feature without destroying that feature's meaning.

  About the legacy source: the class is COMMENTED OUT of the legacy testng.xml (line 411), so none of this was
  ever actually verified upstream.

  Its second API ("intermediateCertAPI") reads like dead setup — it is never invoked, and no test method
  references it — but it is LOAD-BEARING, and only the head-only case reveals why. Uploading a client
  certificate to ANY API adds it to the gateway's shared listener trust store, so that API exists purely to put
  the INTERMEDIATE certificate there. When the leaf arrives alone, the authenticator completes the path by
  looking each issuer up in that trust store: leaf -> intermediate (found only because of the second API) ->
  root (which the API under test holds). Established here empirically — with the intermediate absent, the
  full-chain leaf is authorised (200) while the identical head-only leaf is refused (401), because the gateway
  has no way to bridge leaf to root. Removing that API would silently turn the head-only assertion into a
  failure, which is why it is ported rather than dropped.

  @cap:gateway @feat:security-enforcement @rule:mutual-ssl-chain @type:regression @dep:publisher @legacy:APISecurityMutualSSLCertificateChainValidationTestCase
  Scenario Outline: A certificate signed by the uploaded root authenticates by its chain as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_mutualssl_api.json" in context as "chainPayload"
    And I create an "apis" resource with payload "chainPayload" as "chainApiId"
    Then The response status code should be 201
    # Only the ROOT is uploaded — under the SANDBOX key type, as legacy does.
    When I upload client certificate "artifacts/certs/mutualssl/cert_chain_root.cer" with alias "chainroot" and key type "SANDBOX" to API "chainApiId" for tier "Unlimited"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "chainApiId"
    And I extract response field "context" and store it as "chainContext"
    When I deploy the API with id "chainApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "chainApiId"
    Then The lifecycle status of API "chainApiId" should be "Published"

    # A SECOND API carrying the INTERMEDIATE certificate. It is never invoked; its only purpose is that
    # uploading a client certificate to any API also adds it to the gateway's shared listener trust store, which
    # is where the authenticator looks up each issuer when completing an incomplete chain. Without it the
    # head-only leaf below cannot be bridged to the root and is refused — verified.
    When I put JSON payload from file "artifacts/payloads/create_apim_mutualssl_api.json" in context as "intermediatePayload"
    And I create an "apis" resource with payload "intermediatePayload" as "intermediateApiId"
    Then The response status code should be 201
    When I upload client certificate "artifacts/certs/mutualssl/cert_chain_intermediate.cer" with alias "chainintermediate" and key type "SANDBOX" to API "intermediateApiId" for tier "Unlimited"
    Then The response status code should be 201
    When I deploy the API with id "intermediateApiId"
    Then The response status code should be 201

    # Baseline: the uploaded certificate itself still authenticates with chain validation on, so a failure
    # below is about the CHAIN and not about the certificate having failed to reach the gateway at all.
    When I invoke the API at gateway context "{{chainContext}}/1.0.0/customers/123" presenting client certificate "artifacts/certs/mutualssl/cert_chain_root.jks" until response status code becomes 200 within 150 seconds
    Then The response status code should be 200

    # The leaf, presented WITH its full chain: authorised via the uploaded root.
    When I invoke the API at gateway context "{{chainContext}}/1.0.0/customers/123" presenting client certificate "artifacts/certs/mutualssl/cert_chain_client.jks" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    # The SAME leaf exported ALONE (head-only) — the sharper case: the intermediate is neither presented nor
    # uploaded, so the gateway must complete the path from its own trust material rather than from what the
    # client supplied.
    When I invoke the API at gateway context "{{chainContext}}/1.0.0/customers/123" presenting client certificate "artifacts/certs/mutualssl/cert_chain_client_head_only.jks" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    # The DEFAULT-VERSION (versionless) context dispatches through a different gateway routing path, so it gets
    # its own chance to skip the check — asserted for the chain case too.
    When I invoke the API at gateway context "{{chainContext}}/customers/123" presenting client certificate "artifacts/certs/mutualssl/cert_chain_client.jks" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    # Chain validation must not degrade into "any certificate is accepted": an unrelated self-signed
    # certificate, whose issuer path terminates at itself and never reaches the uploaded root, is still
    # refused. Without this the positives above would also pass on a gateway that had stopped checking.
    When I invoke the API at gateway context "{{chainContext}}/1.0.0/customers/123" presenting client certificate "artifacts/certs/mutualssl/test.jks" until response status code becomes 401 within 60 seconds
    Then The response status code should be 401
    # And no certificate at all is still refused on a mutualssl_mandatory API.
    When I invoke the API at gateway context "{{chainContext}}/1.0.0/customers/123" with no client certificate until response status code becomes 401 within 60 seconds
    Then The response status code should be 401

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |
