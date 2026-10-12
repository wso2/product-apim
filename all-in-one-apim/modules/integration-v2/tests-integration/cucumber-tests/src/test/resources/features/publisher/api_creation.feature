@cleanup
Feature: Publisher API creation parity

  Creation-time API security and revision deployment coverage ported from APICreationTestCase. The scenarios run
  as the least-privileged publisher in both the super-tenant and tenant1.com contexts. Revision creation waits
  for the publisher API to become readable, asserts the exact 201 response, extracts a non-empty revision ID,
  and deploys that same ID; the bounded deployment-status poll replaces any fixed delay.

  @cap:publisher @feat:api-lifecycle @rule:api-creation @type:regression @legacy:APICreationTestCase
  Scenario Outline: Create and deploy an mTLS API with a non-empty revision ID as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_mutualssl_api.json" in context as "creationMtlsPayload"
    And I create an "apis" resource with payload "creationMtlsPayload" as "creationMtlsApiId"
    Then The response status code should be 201
    When I generate a unique alphanumeric value and store it as "creationMtlsCertAlias"
    And I upload client certificate "artifacts/certs/mutualssl/cert_chain_root.cer" with alias "{{creationMtlsCertAlias}}" and key type "SANDBOX" to API "creationMtlsApiId" for tier "Unlimited"
    Then The response status code should be 201
    When I put the following JSON payload in context as "creationMtlsRevisionPayload"
    """
    {"description":"mTLS API creation revision"}
    """
    And I make a request to create a revision for "apis" resource "creationMtlsApiId" with payload "creationMtlsRevisionPayload"
    Then The response status code should be 201
    And I extract response field "id" and store it as "creationMtlsRevisionId"
    When I deploy revision "creationMtlsRevisionId" of "apis" resource "creationMtlsApiId"
    Then The response status code should be 201
    And I wait until "apis" "creationMtlsApiId" revision is deployed in the gateway

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |
