@cleanup
Feature: Publisher Endpoint Certificates

  Publisher-plane management of endpoint (backend TLS) certificates via the /endpoint-certificates REST API:
  upload a certificate against a backend endpoint URL, search the uploaded certificates by endpoint and by alias,
  read a certificate's information (status / subject DN / version / validity), reject a duplicate alias and an
  expired certificate, delete a certificate, and query which APIs use a given certificate (usage) with pagination.
  Ports the management surface of APIEndpointCertificateTestCase and APIEndpointCertificateUsageTestCase. The
  RUNTIME half of the legacy cert test — an API pointed at an HTTPS backend the gateway does not trust, invoked
  500, then 200 once the certificate is uploaded, then 500 again once it is deleted — is a gateway concern and
  lives in features/gateway/endpoint_certificate_invocation.feature (it needs the tls-backend HTTPS app and a
  shortened SSL-profile / certificate-reloader interval, so it runs in its own container block).

  Certificates are tenant-global config, so each scenario uses a scenario-unique alias and its OWN endpoint HOST,
  and uploaded certificates are torn down by the per-scenario cleanup hook. The distinct host matters for the
  search assertions specifically: the product normalizes a search endpoint to scheme://host (the PORT and PATH are
  discarded) and matches stored endpoints with a prefix LIKE, so two scenarios sharing a host would see each
  other's certificates in a by-endpoint search and break the exact counts.

  # Upload two certificates for one endpoint, search by endpoint (2) and by alias (1), and confirm a non-existent
  # alias returns none. Then the search variants that pin the scheme://host normalization: an unrelated endpoint
  # returns 0, the bare host prefix returns both, and the endpoint plus an /api/v1 path suffix also returns both.
  # Then read each certificate's information (status / subject DN / version / validity) — read back out of the
  # gateway trust store, so it also proves the upload landed there. Finally the negatives: re-uploading an existing
  # alias is 409, and an expired certificate is 400. Ports testUploadEndpointCertificate +
  # testSearchEndpointCertificates (counts AND the per-alias certificate content) + testUploadSameEndpointCertifica
  # teInSameAlias + testUploadExpiredCert.
  @cap:publisher @feat:api-config @rule:endpoint-certificates @type:regression @legacy:APIEndpointCertificateTestCase
  Scenario Outline: Upload, search, read and validate endpoint certificates as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I generate a unique value and store it as "certEndpoint"
    And I generate a unique value and store it as "certAlias1"
    And I generate a unique value and store it as "certAlias2"

    # Upload two distinct certificates for the same backend endpoint URL.
    When I upload endpoint certificate "artifacts/certs/endpoint/endpoint.cer" with alias "{{certAlias1}}" for endpoint "https://certsearch.example.com/{{certEndpoint}}"
    Then The response status code should be 201
    And The value of response field "alias" should be "{{certAlias1}}"
    When I upload endpoint certificate "artifacts/certs/endpoint/endpoint2.cer" with alias "{{certAlias2}}" for endpoint "https://certsearch.example.com/{{certEndpoint}}"
    Then The response status code should be 201

    # Search by endpoint returns both; search by one alias returns exactly that one; an unknown alias returns none.
    When I search endpoint certificates by endpoint "https://certsearch.example.com/{{certEndpoint}}"
    Then The response status code should be 200
    And The endpoint certificate search should return 2 certificates
    When I search endpoint certificates by alias "{{certAlias1}}"
    Then The response status code should be 200
    And The endpoint certificate search should return 1 certificates
    When I search endpoint certificates by alias "{{certAlias1}}-none"
    Then The response status code should be 200
    And The endpoint certificate search should return 0 certificates

    # An UNRELATED endpoint matches nothing — the positive control for the three matching searches around it
    # (without it, a search that returned everything would satisfy every count above).
    When I search endpoint certificates by endpoint "https://certunrelated.example.com/{{certEndpoint}}"
    Then The response status code should be 200
    And The endpoint certificate search should return 0 certificates

    # The bare HOST PREFIX (no path) still matches both: the search endpoint is reduced to scheme://host and
    # prefix-matched against the stored endpoints.
    When I search endpoint certificates by endpoint "https://certsearch.example.com"
    Then The response status code should be 200
    And The endpoint certificate search should return 2 certificates

    # A LONGER endpoint (the stored one plus an /api/v1 path suffix) also matches both — the path is discarded by
    # the same normalization, so a search for a sub-resource finds the host's certificates.
    When I search endpoint certificates by endpoint "https://certsearch.example.com/{{certEndpoint}}/api/v1"
    Then The response status code should be 200
    And The endpoint certificate search should return 2 certificates

    # Certificate INFORMATION per alias: Active, the exact subject DN of each fixture (rendered by
    # X509Certificate.getSubjectDN(), i.e. RDNs in reverse order of the PEM), version 3, and the exact validity.
    When I retrieve the content of endpoint certificate "{{certAlias1}}"
    Then The response status code should be 200
    And The endpoint certificate content should have status "Active", subject "CN=localhost, OU=localhost, C=LK" and version "3"
    And The endpoint certificate validity should be from "Fri May 06 18:11:14 UTC 2022" to "Thu May 06 18:11:14 UTC 2032"
    When I retrieve the content of endpoint certificate "{{certAlias2}}"
    Then The response status code should be 200
    And The endpoint certificate content should have status "Active", subject "CN=wso2apim, OU=integration, O=WSO2, ST=Colombo, C=LK" and version "3"
    And The endpoint certificate validity should be from "Fri May 06 19:01:00 UTC 2022" to "Thu May 06 19:01:00 UTC 2032"

    # Re-uploading the same alias is a 409 conflict.
    When I attempt to upload endpoint certificate "artifacts/certs/endpoint/endpoint.cer" with alias "{{certAlias1}}" for endpoint "https://certsearch.example.com/{{certEndpoint}}"
    Then The response status code should be 409

    # An expired certificate is rejected with 400.
    When I attempt to upload endpoint certificate "artifacts/certs/endpoint/expired.cer" with alias "{{certAlias1}}-exp" for endpoint "https://certsearch.example.com/{{certEndpoint}}"
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
    When I upload endpoint certificate "artifacts/certs/endpoint/endpoint.cer" with alias "{{delAlias}}" for endpoint "https://certdelete.example.com/{{delEndpoint}}"
    Then The response status code should be 201
    When I delete the endpoint certificate with alias "{{delAlias}}"
    Then The response status code should be 200
    # The deleted certificate is really gone, not just unreachable by alias: a by-endpoint search now matches none.
    When I search endpoint certificates by endpoint "https://certdelete.example.com/{{delEndpoint}}"
    Then The response status code should be 200
    And The endpoint certificate search should return 0 certificates
    # Deleting the now-removed (i.e. non-existent) alias is 404.
    When I delete the endpoint certificate with alias "{{delAlias}}"
    Then The response status code should be 404

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # Certificate usage: upload a certificate for an endpoint used by exactly 3 APIs; usage-by-alias lists exactly
  # THOSE 3 APIs (by id, not just by count), an incorrect alias lists 0, and the full legacy limit/offset matrix
  # caps and offsets the list. Ports APIEndpointCertificateUsageTestCase (reduced from its random 20-30 API sprawl
  # to a deterministic 3-API set, but keeping all seven pagination cases and adding the identity assertion legacy
  # only made as a containsAll).
  @cap:publisher @feat:api-config @rule:endpoint-certificates @type:regression @legacy:APIEndpointCertificateUsageTestCase
  Scenario Outline: Query endpoint certificate usage with pagination as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I generate a unique value and store it as "useEndpoint"
    And I generate a unique value and store it as "useAlias"
    # Create 3 APIs whose production endpoint is the certificate's endpoint URL (no publish needed — usage is by
    # endpoint config, not deployment). Their ids are handed on as "epUsageApiIds" for the identity assertion.
    And I create 3 APIs with production endpoint "https://certusage.example.com/{{useEndpoint}}/resource" named "{{useEndpoint}}"
    When I upload endpoint certificate "artifacts/certs/endpoint/endpoint.cer" with alias "{{useAlias}}" for endpoint "https://certusage.example.com/{{useEndpoint}}/resource"
    Then The response status code should be 201

    # Usage by the correct alias lists all 3 APIs; an incorrect alias lists 0. Usage is eventually consistent
    # (freshly-created APIs + freshly-uploaded cert are not matched immediately), so the first query polls until
    # the index settles; the pagination queries below are then consistent.
    When I retrieve the usage of endpoint certificate "{{useAlias}}" with limit 10 and offset 0 until it lists 3 APIs within 60 seconds
    # WHICH APIs, not just how many — a count-only assertion passes on three unrelated APIs.
    Then The endpoint certificate usage should list exactly the APIs in "epUsageApiIds"
    When I retrieve the usage of endpoint certificate "{{useAlias}}-wrong" with limit 10 and offset 0
    Then The response status code should be 200
    And The endpoint certificate usage should list 0 APIs

    # Pagination matrix over the 3-API set, mirroring legacy's seven cases:
    #   limit + offset <  count, offset 0     -> limit
    When I retrieve the usage of endpoint certificate "{{useAlias}}" with limit 2 and offset 0
    Then The endpoint certificate usage should list 2 APIs
    #   limit == count, offset 0              -> count
    When I retrieve the usage of endpoint certificate "{{useAlias}}" with limit 3 and offset 0
    Then The endpoint certificate usage should list 3 APIs
    #   limit >  count, offset 0              -> count (the limit does not invent rows)
    When I retrieve the usage of endpoint certificate "{{useAlias}}" with limit 8 and offset 0
    Then The endpoint certificate usage should list 3 APIs
    #   limit + offset <  count, offset > 0   -> limit
    When I retrieve the usage of endpoint certificate "{{useAlias}}" with limit 1 and offset 1
    Then The endpoint certificate usage should list 1 APIs
    #   limit + offset == count, offset > 0   -> limit
    When I retrieve the usage of endpoint certificate "{{useAlias}}" with limit 2 and offset 1
    Then The endpoint certificate usage should list 2 APIs
    #   limit + offset >  count, offset > 0   -> count - offset (the remainder, not the limit)
    When I retrieve the usage of endpoint certificate "{{useAlias}}" with limit 10 and offset 2
    Then The endpoint certificate usage should list 1 APIs
    #   offset >  count                       -> 0
    When I retrieve the usage of endpoint certificate "{{useAlias}}" with limit 10 and offset 5
    Then The endpoint certificate usage should list 0 APIs

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |
