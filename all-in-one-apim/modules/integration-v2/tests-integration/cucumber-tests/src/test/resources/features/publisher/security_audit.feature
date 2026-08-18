@cleanup
Feature: Publisher API Security Audit

  The publisher's external security-audit integration — `GET /apis/{apiId}/auditapi`. Ports
  APISecurityAuditTestCase.

  This block exists to carry `[security_audit]`. Without that table the product's
  getSecurityAuditAttributesFromConfig() returns null and the resource NPEs into a raw 500, so the feature is
  untestable on default config. The table points at the `am-auditApi-sample` Express mock already baked into
  the node-app-server image (pm2 app on port 3002, network alias `nodebackend`) — legacy used a .war deployed
  into a second APIM instance on https://localhost:9943.

  One request drives a THREE-leg exchange with the audit service, and which legs run depends on the
  AM_SECURITY_AUDIT_UUID_MAPPING table:

    first audit of an API   POST {base_url}                              -> new audit id read from desc.id
    later audits of it      PUT  {base_url}/{auditUuid}                  -> definition re-uploaded
    always                  GET  {base_url}/{auditUuid}/assessmentreport -> the report

  The two scenarios below are deliberately split along that branch: the first API is audited once (create
  leg), the second is audited twice (create, then update). Legacy only ever hit the create leg, so the update
  leg was never exercised upstream at all.

  On assertions: legacy asserted only `assertNotNull(response)` and status 200 — it would have passed against
  a server that returned an empty body, and it never checked that the report itself arrived. Here every field
  of the AuditReport is pinned instead, which is what makes the response evidence that the exchange really
  happened: `externalApiId` can only hold that value if the product read `desc.id` off the create leg and
  persisted it, and `grade`/`numErrors`/`report` can only hold theirs if it fetched, parsed and base64-decoded
  the assessment report. The values come from the mock's fixed fixture
  (nodeapps/am-auditApi-sample/data/*.json), so they are exact, not ranges.

  Both organizations exercise the configured audit-service integration; the external service is shared, but
  the API and its audit-id mapping are organization-scoped.

  @cap:publisher @feat:security-audit @rule:audit-report @type:smoke @legacy:APISecurityAuditTestCase
  Scenario Outline: The security audit report for a newly published API is retrieved from the audit service as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_security_audit_api.json" in context as "auditPayload"
    And I create an "apis" resource with payload "auditPayload" as "auditApiId"
    Then The response status code should be 201
    When I deploy the API with id "auditApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "auditApiId"
    Then The lifecycle status of API "auditApiId" should be "Published"

    # No mapping row exists yet, so the server takes the CREATE leg (multipart POST) and stores desc.id.
    When I retrieve the security audit report for API "auditApiId"
    Then The response status code should be 200
    # externalApiId is desc.id from the mock's test-new-audit-api.json — proves the create leg ran and the
    # returned id was read back out of the response rather than invented.
    And The value of response field "externalApiId" should be "03530124-cfc3-470d-8640-65cc6a05ec6f"
    # grade and numErrors are lifted from attr.data of the mock's test-audit-report.json. The product hard-casts
    # both to String and then parses numErrors to an Integer, so these pin the parse as well as the transport.
    And The value of response field "grade" should be "2.5321499999999997e+01"
    And The value of response field "numErrors" should be "28"
    # `report` is the mock's base64 `data` field DECODED by the server. These are issue descriptions carried
    # inside that report, so they prove the decode step happened — none of them appears anywhere in the base64
    # form. They are matched as bare phrases because `report` is embedded in the response as an ESCAPED JSON
    # string (every inner quote arrives as \"), which makes any assertion spanning a quote character brittle.
    And The response should contain "API accepts HTTP requests in the clear"
    And The response should contain "Access tokens transported as cleartext"
    And The response should contain "Numeric schema has no maximum defined"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:publisher @feat:security-audit @rule:audit-report @type:regression @legacy:APISecurityAuditTestCase
  Scenario Outline: Re-auditing an API reuses its stored audit id and takes the update leg as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_security_audit_api.json" in context as "reAuditPayload"
    And I create an "apis" resource with payload "reAuditPayload" as "reAuditApiId"
    Then The response status code should be 201
    When I deploy the API with id "reAuditApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "reAuditApiId"
    Then The lifecycle status of API "reAuditApiId" should be "Published"

    # First audit: create leg, which writes the AM_SECURITY_AUDIT_UUID_MAPPING row.
    When I retrieve the security audit report for API "reAuditApiId"
    Then The response status code should be 200
    And The value of response field "externalApiId" should be "03530124-cfc3-470d-8640-65cc6a05ec6f"

    # Second audit of the SAME API. The row now exists, so the server must take the UPDATE leg
    # (PUT {base_url}/{auditUuid}) instead of creating a second audit api. A failure here means either the
    # mapping was not persisted (the product would POST again) or the PUT leg is broken — the PUT is
    # unconditional and any non-200 from it aborts the request before the report is fetched, so a 200 with the
    # same id below is only reachable via a successful update.
    When I retrieve the security audit report for API "reAuditApiId"
    Then The response status code should be 200
    And The value of response field "externalApiId" should be "03530124-cfc3-470d-8640-65cc6a05ec6f"
    And The value of response field "numErrors" should be "28"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
