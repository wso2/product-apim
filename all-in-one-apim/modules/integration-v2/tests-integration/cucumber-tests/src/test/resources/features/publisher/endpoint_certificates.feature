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

  Certificates are tenant-global config, so each scenario uses scenario-unique aliases and its OWN endpoint HOST,
  and uploaded certificates are torn down by the per-scenario cleanup hook. The host is a UUID-derived DNS label:
  the product normalizes a search endpoint to scheme://host (the PORT and PATH are discarded) and matches stored
  endpoints with a prefix LIKE, so a fixed/shared host would let concurrent scenarios contaminate exact search
  counts and certificate identity assertions.

  # Upload two certificates for one endpoint, search by endpoint (2) and by each alias (1), and confirm a non-existent
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
    And I generate a random UUID and store it as "certHost"
    And I generate a unique value and store it as "certAlias1"
    And I generate a unique value and store it as "certAlias2"

    # Upload two distinct certificates for the same backend endpoint URL.
    When I upload endpoint certificate "artifacts/certs/endpoint/endpoint.cer" with alias "{{certAlias1}}" for endpoint "https://{{certHost}}.certsearch.example.com/{{certEndpoint}}"
    Then The response status code should be 201
    And The value of response field "alias" should be "{{certAlias1}}"
    And The value of response field "endpoint" should be "https://{{certHost}}.certsearch.example.com/{{certEndpoint}}"
    When I upload endpoint certificate "artifacts/certs/endpoint/endpoint2.cer" with alias "{{certAlias2}}" for endpoint "https://{{certHost}}.certsearch.example.com/{{certEndpoint}}"
    Then The response status code should be 201
    And The value of response field "alias" should be "{{certAlias2}}"
    And The value of response field "endpoint" should be "https://{{certHost}}.certsearch.example.com/{{certEndpoint}}"

    # Verify both count and identity: unique host/aliases isolate parallel scenarios, and the returned metadata
    # must be exactly the certificates uploaded above rather than any same-count unrelated result.
    When I search endpoint certificates by endpoint "https://{{certHost}}.certsearch.example.com/{{certEndpoint}}"
    Then The response status code should be 200
    And The endpoint certificate search should return 2 certificates
    And The endpoint certificate search should list exactly aliases "{{certAlias1}},{{certAlias2}}" for endpoint "https://{{certHost}}.certsearch.example.com/{{certEndpoint}}"
    When I search endpoint certificates by alias "{{certAlias1}}"
    Then The response status code should be 200
    And The endpoint certificate search should return 1 certificates
    And The endpoint certificate search should list exactly aliases "{{certAlias1}}" for endpoint "https://{{certHost}}.certsearch.example.com/{{certEndpoint}}"
    When I search endpoint certificates by alias "{{certAlias2}}"
    Then The response status code should be 200
    And The endpoint certificate search should return 1 certificates
    And The endpoint certificate search should list exactly aliases "{{certAlias2}}" for endpoint "https://{{certHost}}.certsearch.example.com/{{certEndpoint}}"
    When I search endpoint certificates by alias "{{certAlias1}}-none"
    Then The response status code should be 200
    And The endpoint certificate search should return 0 certificates

    # An UNRELATED endpoint matches nothing — the positive control for the three matching searches around it
    # (without it, a search that returned everything would satisfy every count above).
    When I search endpoint certificates by endpoint "https://{{certHost}}.certunrelated.example.com/{{certEndpoint}}"
    Then The response status code should be 200
    And The endpoint certificate search should return 0 certificates

    # The bare HOST PREFIX (no path) still matches both: the search endpoint is reduced to scheme://host and
    # prefix-matched against the stored endpoints.
    When I search endpoint certificates by endpoint "https://{{certHost}}.certsearch.example.com"
    Then The response status code should be 200
    And The endpoint certificate search should return 2 certificates
    And The endpoint certificate search should list exactly aliases "{{certAlias1}},{{certAlias2}}" for endpoint "https://{{certHost}}.certsearch.example.com/{{certEndpoint}}"

    # A LONGER endpoint (the stored one plus an /api/v1 path suffix) also matches both — the path is discarded by
    # the same normalization, so a search for a sub-resource finds the host's certificates.
    When I search endpoint certificates by endpoint "https://{{certHost}}.certsearch.example.com/{{certEndpoint}}/api/v1"
    Then The response status code should be 200
    And The endpoint certificate search should return 2 certificates
    And The endpoint certificate search should list exactly aliases "{{certAlias1}},{{certAlias2}}" for endpoint "https://{{certHost}}.certsearch.example.com/{{certEndpoint}}"

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
    When I attempt to upload endpoint certificate "artifacts/certs/endpoint/endpoint.cer" with alias "{{certAlias1}}" for endpoint "https://{{certHost}}.certsearch.example.com/{{certEndpoint}}"
    Then The response status code should be 409

    # An expired certificate is rejected with 400.
    When I attempt to upload endpoint certificate "artifacts/certs/endpoint/expired.cer" with alias "{{certAlias1}}-exp" for endpoint "https://{{certHost}}.certsearch.example.com/{{certEndpoint}}"
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
    And I generate a random UUID and store it as "delHost"
    And I generate a unique value and store it as "delAlias"
    When I upload endpoint certificate "artifacts/certs/endpoint/endpoint.cer" with alias "{{delAlias}}" for endpoint "https://{{delHost}}.certdelete.example.com/{{delEndpoint}}"
    Then The response status code should be 201
    When I delete the endpoint certificate with alias "{{delAlias}}"
    Then The response status code should be 200
    # The deleted certificate is really gone, not just unreachable by alias: a by-endpoint search now matches none.
    When I search endpoint certificates by endpoint "https://{{delHost}}.certdelete.example.com/{{delEndpoint}}"
    Then The response status code should be 200
    And The endpoint certificate search should return 0 certificates
    # Deleting the now-removed (i.e. non-existent) alias is 404.
    When I delete the endpoint certificate with alias "{{delAlias}}"
    Then The response status code should be 404

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # Certificate usage: upload a certificate for one endpoint on a scenario-unique host that 3 APIs share (each on a
  # different path, one of them the certificate's own path), plus a decoy API on another host. Usage-by-alias lists
  # exactly THOSE 3 APIs (by id, not just by count), so usage matches by HOST, not by exact URL, and excludes the
  # decoy; an incorrect alias lists 0, and the full legacy limit/offset matrix caps and offsets the list. Ports
  # APIEndpointCertificateUsageTestCase (reduced from its random 20-30 API sprawl to a deterministic 3-API set, but
  # keeping all seven pagination cases and adding the identity assertion legacy only made as a containsAll).
  # Host labels come from a UUID (a hostname cannot carry the underscore of a generated unique value), and no URL
  # contains "localhost" (the CN of endpoint.cer, itself a usage search term) or the other host as a substring.
  @cap:publisher @feat:api-config @rule:endpoint-certificates @type:regression @legacy:APIEndpointCertificateUsageTestCase
  Scenario Outline: Query endpoint certificate usage with pagination as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I generate a unique value and store it as "useEndpoint"
    And I generate a unique value and store it as "useAlias"
    And I generate a unique value and store it as "useDecoyAlias"
    And I generate a random UUID and store it as "useHost"
    # A decoy API on a DIFFERENT host with the certificate's own path (no publish needed — usage is by endpoint
    # config, not deployment). Its id is kept apart as "epDecoyApiIds".
    And I create 1 APIs with per-index production endpoint "https://{{useHost}}.certdecoy.example.org/resource2" named "{{useEndpoint}}d" storing their ids as "epDecoyApiIds"
    # 3 APIs on the certificate's host at /resource0, /resource1 and /resource2; the certificate is uploaded for
    # /resource2 only. Their ids are handed on as "epUsageApiIds" for the identity assertion.
    And I create 3 APIs with per-index production endpoint "https://{{useHost}}.certusage.example.com/resource{index}" named "{{useEndpoint}}" storing their ids as "epUsageApiIds"
    When I upload endpoint certificate "artifacts/certs/endpoint/endpoint.cer" with alias "{{useAlias}}" for endpoint "https://{{useHost}}.certusage.example.com/resource2"
    Then The response status code should be 201
    # A second certificate (CN=wso2apim, no SANs) for the decoy's endpoint: its usage listing the decoy is what
    # shows the decoy is indexed, so its absence from the first certificate's usage is an exclusion.
    When I upload endpoint certificate "artifacts/certs/endpoint/endpoint2.cer" with alias "{{useDecoyAlias}}" for endpoint "https://{{useHost}}.certdecoy.example.org/resource2"
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

    # The decoy is indexed (the decoy certificate's usage lists exactly it), and the first certificate's usage
    # still lists exactly the 3 same-host APIs, without the decoy.
    When I retrieve the usage of endpoint certificate "{{useDecoyAlias}}" with limit 10 and offset 0 until it lists 1 APIs within 60 seconds
    Then The endpoint certificate usage should list exactly the APIs in "epDecoyApiIds"
    When I retrieve the usage of endpoint certificate "{{useAlias}}" with limit 10 and offset 0
    Then The response status code should be 200
    And The endpoint certificate usage should list exactly the APIs in "epUsageApiIds"

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
