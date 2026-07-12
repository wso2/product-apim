@cleanup
Feature: Gateway Mediation Policies

  Gateway-plane operation-policy runtime: a request-flow operation policy attached to an API operation injects a
  header towards the backend. The policy (custom_add_common_header) is registered first as a common policy, then
  referenced by the API's GET operation; the API routes to the header-reflecting backend (/reflect-headers) so
  the injected header is observed on the backend request. Runs in the gateway block (backend + invocation), in
  both tenants. Ports the attach-and-invoke runtime slice of OperationPolicyTestCase.

  @cap:gateway @feat:mediation-policies @rule:add-header @type:regression @dep:publisher @legacy:OperationPolicyTestCase
  Scenario Outline: A request-flow operation policy injects a header towards the backend as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    # Register the common operation policy first so the API can reference it by name.
    And I create a new common policy with spec "artifacts/payloads/policySpecFiles/custom_add_common_header.j2" and "artifacts/payloads/policySpecFiles/custom_add_common_header.yaml" as "medCommonPolicyId"
    And I have created an api from "artifacts/payloads/create_apim_oppolicy_api.json" as "medApiId" and deployed it
    When I publish the "apis" resource with id "medApiId"
    Then The lifecycle status of API "medApiId" should be "Published"
    When I retrieve the "apis" resource with id "medApiId"
    And I extract response field "context" and store it as "medContext"
    When I have set up application with keys, subscribed to API "medApiId", and obtained access token for "medSubId"
    Then The response status code should be 200

    # The reflecting backend echoes the headers it received; the operation policy must have injected ours.
    When I invoke the API at gateway context "{{medContext}}/1.0.0/reflect-headers" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "x-common-value"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # jwtClaimBasedAccessValidator (a shipped common policy) permits the call when the token carries the configured
  # claim=value and blocks it (403) otherwise. A client-credentials token carries aut=APPLICATION, so a matching
  # policy lets the invocation through. Ports JWTClaimBasedAccessValidatorPolicyTestCase (allow case).
  @cap:gateway @feat:mediation-policies @rule:claim-access-validator @type:regression @dep:publisher @legacy:JWTClaimBasedAccessValidatorPolicyTestCase
  Scenario Outline: A matching JWT-claim access-validator policy permits the invocation as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_claimvalidator_match_api.json" as "cvMatchApiId" and deployed it
    When I publish the "apis" resource with id "cvMatchApiId"
    Then The lifecycle status of API "cvMatchApiId" should be "Published"
    When I retrieve the "apis" resource with id "cvMatchApiId"
    And I extract response field "context" and store it as "cvMatchContext"
    When I have set up application with keys, subscribed to API "cvMatchApiId", and obtained access token for "cvMatchSubId"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{cvMatchContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # The same policy configured against a claim the token does NOT carry blocks the invocation with 403.
  # Ports JWTClaimBasedAccessValidatorPolicyTestCase (deny case).
  @cap:gateway @feat:mediation-policies @rule:claim-access-validator @type:negative @dep:publisher @legacy:JWTClaimBasedAccessValidatorPolicyTestCase
  Scenario Outline: A non-matching JWT-claim access-validator policy blocks the invocation as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_claimvalidator_mismatch_api.json" as "cvMissApiId" and deployed it
    When I publish the "apis" resource with id "cvMissApiId"
    Then The lifecycle status of API "cvMissApiId" should be "Published"
    When I retrieve the "apis" resource with id "cvMissApiId"
    And I extract response field "context" and store it as "cvMissContext"
    When I have set up application with keys, subscribed to API "cvMissApiId", and obtained access token for "cvMissSubId"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{cvMissContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Secret-attribute operation policy: a policy declaring "Secret"-type attributes (apiKey mandatory, token
  # optional) injects those values as headers towards the backend. The secret value is observed on the backend
  # request (via /reflect-headers) but is MASKED (not returned) in the publisher representation of the API.
  # Ports the secret-attributes slice of OperationPolicyTestCase.
  @cap:gateway @feat:mediation-policies @rule:secret-attributes @type:regression @dep:publisher @legacy:OperationPolicyTestCase
  Scenario Outline: A secret-attribute operation policy injects the secret header but masks it on retrieval as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I create a new common policy with spec "artifacts/payloads/policySpecFiles/add_secret_headers.j2" and "artifacts/payloads/policySpecFiles/add_secret_headers.yaml" as "secretPolicyId"
    And I have created an api from "artifacts/payloads/create_apim_secretpolicy_api.json" as "secretApiId" and deployed it
    When I publish the "apis" resource with id "secretApiId"
    Then The lifecycle status of API "secretApiId" should be "Published"
    When I retrieve the "apis" resource with id "secretApiId"
    And I extract response field "context" and store it as "secretContext"
    # The secret value must NOT be returned in the publisher representation of the API (it is masked).
    And The response should not contain "test-api-key-123"
    When I have set up application with keys, subscribed to API "secretApiId", and obtained access token for "secretSubId"
    Then The response status code should be 200

    # The reflecting backend shows the injected secret header carried the configured value.
    When I invoke the API at gateway context "{{secretContext}}/1.0.0/reflect-headers" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "test-api-key-123"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # JSON-to-XML conversion of a request body whose JSON KEY contains a reserved colon (a URI like
  # http://purl.org/dc/elements/1.1/creator): the shipped jsonToXML operation policy converts the JSON request
  # to XML before it reaches the backend; the colon-containing key must NOT break the conversion (200, not a 500).
  # The /reflect-body backend echoes the converted body so the value is observed to survive. Ports
  # ESBJAVA3380TestCase (the colon-key json_to_xml gateway-parser regression) using the shipped jsonToXML policy.
  @cap:gateway @feat:mediation-policies @rule:json-to-xml @type:regression @dep:publisher @legacy:ESBJAVA3380TestCase
  Scenario Outline: The jsonToXML policy converts a colon-keyed JSON request body without failing as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_jsontoxml_api.json" as "jxApiId" and deployed it
    When I publish the "apis" resource with id "jxApiId"
    Then The lifecycle status of API "jxApiId" should be "Published"
    When I retrieve the "apis" resource with id "jxApiId"
    And I extract response field "context" and store it as "jxContext"
    When I have set up application with keys, subscribed to API "jxApiId", and obtained access token for "jxSubId"
    Then The response status code should be 200

    # A JSON body whose key is a colon-containing URI — the conversion to XML must not choke on the colon.
    When I put the following JSON payload in context as "jxPayload"
    """
    { "http://purl.org/dc/elements/1.1/creator" : "url" }
    """
    When I invoke the API at gateway context "{{jxContext}}/1.0.0/reflect-body" with method "POST" using access token "generatedAccessToken" and payload "jxPayload" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "url"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # JS script mediator: a request-flow operation policy runs a <script language="js"> (executed by the gateway's
  # bundled Rhino engine) that builds an object with a NULL field, reads it back, and emits the result as a
  # transport header. Verifies the script executes AND handles a null object in the JSON without breaking — the
  # concern of ScriptMediatorTestCase (which asserted a null in the script's JSON is handled). The reflect-headers
  # backend echoes the header the script produced.
  @cap:gateway @feat:mediation-policies @rule:script-mediator @type:regression @dep:publisher @legacy:ScriptMediatorTestCase
  Scenario Outline: A JS script mediator executes and handles a null object field as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I create a new common policy with spec "artifacts/payloads/policySpecFiles/js_script_header.j2" and "artifacts/payloads/policySpecFiles/js_script_header.yaml" as "jsPolicyId"
    And I have created an api from "artifacts/payloads/create_apim_jsscript_api.json" as "jsApiId" and deployed it
    When I publish the "apis" resource with id "jsApiId"
    Then The lifecycle status of API "jsApiId" should be "Published"
    When I retrieve the "apis" resource with id "jsApiId"
    And I extract response field "context" and store it as "jsContext"
    When I have set up application with keys, subscribed to API "jsApiId", and obtained access token for "jsSubId"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{jsContext}}/1.0.0/reflect-headers" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    # The script ran (testName) and correctly evaluated the null field (null) -> "testName-null".
    And The response should contain "testName-null"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
