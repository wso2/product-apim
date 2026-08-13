@cleanup
Feature: Publisher Endpoint Certificates

  Publisher-plane management of endpoint (backend TLS) certificates via the /endpoint-certificates REST API:
  upload a certificate against a backend endpoint URL, search the uploaded certificates by endpoint and by alias,
  reject a duplicate alias and an expired certificate, delete a certificate, and query which APIs use a given
  certificate (usage) with pagination. Ports the management surface of APIEndpointCertificateTestCase and
  APIEndpointCertificateUsageTestCase. The invocation half of the legacy test (a WireMock HTTPS backend + SSL
  profile reload) is out of scope — it needs custom TLS backend infra; the REST management/usage behaviour is the
  portable subject.

  Certificates are tenant-global config, so each scenario uses a scenario-unique endpoint URL and unique aliases to
  stay isolated under parallel execution; uploaded certificates are torn down by the per-scenario cleanup hook.

  # Upload two certificates for one endpoint, search by endpoint (2) and by alias (1), and confirm a non-existent
  # alias returns none. Then the negatives: re-uploading an existing alias is 409, and an expired certificate is
  # 400. Ports testUploadEndpointCertificate + testSearchEndpointCertificates + testUploadSameEndpointCertificate
  # InSameAlias + testUploadExpiredCert.
  @cap:publisher @feat:api-config @rule:endpoint-certificates @type:regression @legacy:APIEndpointCertificateTestCase
  Scenario Outline: Upload, search and validate endpoint certificates as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I generate a unique value and store it as "certEndpoint"
    And I generate a unique value and store it as "certAlias1"
    And I generate a unique value and store it as "certAlias2"

    # Upload two distinct certificates for the same backend endpoint URL.
    When I upload endpoint certificate "artifacts/certs/endpoint/endpoint.cer" with alias "{{certAlias1}}" for endpoint "https://certfixed.example.com/{{certEndpoint}}"
    Then The response status code should be 201
    And The response should contain "{{certAlias1}}"
    When I upload endpoint certificate "artifacts/certs/endpoint/endpoint2.cer" with alias "{{certAlias2}}" for endpoint "https://certfixed.example.com/{{certEndpoint}}"
    Then The response status code should be 201

    # Search by endpoint returns both; search by one alias returns exactly that one; an unknown alias returns none.
    When I search endpoint certificates by endpoint "https://certfixed.example.com/{{certEndpoint}}"
    Then The response status code should be 200
    And The endpoint certificate search should return 2 certificates
    When I search endpoint certificates by alias "{{certAlias1}}"
    Then The response status code should be 200
    And The endpoint certificate search should return 1 certificates
    When I search endpoint certificates by alias "{{certAlias1}}-none"
    Then The response status code should be 200
    And The endpoint certificate search should return 0 certificates

    # Re-uploading the same alias is a 409 conflict.
    When I attempt to upload endpoint certificate "artifacts/certs/endpoint/endpoint.cer" with alias "{{certAlias1}}" for endpoint "https://certfixed.example.com/{{certEndpoint}}"
    Then The response status code should be 409

    # An expired certificate is rejected with 400.
    When I attempt to upload endpoint certificate "artifacts/certs/endpoint/expired.cer" with alias "{{certAlias1}}-exp" for endpoint "https://certfixed.example.com/{{certEndpoint}}"
    Then The response status code should be 400
    And The response should contain "Certificate Expired"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # Delete an uploaded certificate (200), then deleting a non-existent alias is 404. Ports the delete assertions of
  # testInvokeAPIAfterRemovingCertificate (200 on delete) + testDeleteNotAvailableCert (404).
  @cap:publisher @feat:api-config @rule:endpoint-certificates @type:regression @legacy:APIEndpointCertificateTestCase
  Scenario Outline: Delete an endpoint certificate and reject deleting a missing one as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I generate a unique value and store it as "delEndpoint"
    And I generate a unique value and store it as "delAlias"
    When I upload endpoint certificate "artifacts/certs/endpoint/endpoint.cer" with alias "{{delAlias}}" for endpoint "https://certfixed.example.com/{{delEndpoint}}"
    Then The response status code should be 201
    When I delete the endpoint certificate with alias "{{delAlias}}"
    Then The response status code should be 200
    # Deleting the now-removed (i.e. non-existent) alias is 404.
    When I delete the endpoint certificate with alias "{{delAlias}}"
    Then The response status code should be 404

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # Certificate usage: upload a certificate for an endpoint used by exactly 3 APIs; usage-by-alias lists those 3,
  # an incorrect alias lists 0, and pagination (limit/offset) caps and offsets the list. Ports
  # APIEndpointCertificateUsageTestCase (reduced from its 20-30 API sprawl to a deterministic 3-API set, and the
  # 7-case pagination matrix to a representative limit-cap + offset + past-the-end trio).
  @cap:publisher @feat:api-config @rule:endpoint-certificates @type:regression @legacy:APIEndpointCertificateUsageTestCase
  Scenario Outline: Query endpoint certificate usage with pagination as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I generate a unique value and store it as "useEndpoint"
    And I generate a unique value and store it as "useAlias"
    # Create 3 APIs whose production endpoint is the certificate's endpoint URL (no publish needed — usage is by
    # endpoint config, not deployment).
    And I create 3 APIs with production endpoint "https://certfixed.example.com/{{useEndpoint}}/resource" named "{{useEndpoint}}"
    When I upload endpoint certificate "artifacts/certs/endpoint/endpoint.cer" with alias "{{useAlias}}" for endpoint "https://certfixed.example.com/{{useEndpoint}}/resource"
    Then The response status code should be 201

    # Usage by the correct alias lists all 3 APIs; an incorrect alias lists 0. Usage is eventually consistent
    # (freshly-created APIs + freshly-uploaded cert are not matched immediately), so the first query polls until
    # the index settles; the pagination queries below are then consistent.
    When I retrieve the usage of endpoint certificate "{{useAlias}}" with limit 10 and offset 0 until it lists 3 APIs within 60 seconds
    When I retrieve the usage of endpoint certificate "{{useAlias}}-wrong" with limit 10 and offset 0
    Then The response status code should be 200
    And The endpoint certificate usage should list 0 APIs

    # Pagination: limit caps the page (limit 2 -> 2); offset skips (offset 2, limit 10 -> 1); offset past the end -> 0.
    When I retrieve the usage of endpoint certificate "{{useAlias}}" with limit 2 and offset 0
    Then The endpoint certificate usage should list 2 APIs
    When I retrieve the usage of endpoint certificate "{{useAlias}}" with limit 10 and offset 2
    Then The endpoint certificate usage should list 1 APIs
    When I retrieve the usage of endpoint certificate "{{useAlias}}" with limit 10 and offset 5
    Then The endpoint certificate usage should list 0 APIs

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |
