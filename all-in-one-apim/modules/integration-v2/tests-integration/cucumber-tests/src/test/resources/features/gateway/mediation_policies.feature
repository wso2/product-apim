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

  # Root-path operation edge: a FRESH API whose ONLY operation is the root path "/" GET carrying the
  # custom_add_common_header request-flow policy. The root-path resource is the known path-matching edge case —
  # deploy/publish/subscribe/invoke the root "/" and the injected header must still reach the backend (observed
  # via /reflect-headers). The API's endpoint routes the root operation straight to the reflecting backend route.
  # Ports OperationPolicyTestCase#testFreshAPIWithRootPathOperationAndOperationPolicy.
 @cap:gateway @feat:mediation-policies @rule:add-header @type:regression @dep:publisher @legacy:OperationPolicyTestCase
  Scenario Outline: A root-path operation with an operation policy injects the header towards the backend as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    # Register the common operation policy first so the API can reference it by name.
    And I create a new common policy with spec "artifacts/payloads/policySpecFiles/custom_add_common_header.j2" and "artifacts/payloads/policySpecFiles/custom_add_common_header.yaml" as "rootCommonPolicyId"
    And I have created an api from "artifacts/payloads/create_apim_rootpath_oppolicy_api.json" as "rootApiId" and deployed it
    When I publish the "apis" resource with id "rootApiId"
    Then The lifecycle status of API "rootApiId" should be "Published"
    When I retrieve the "apis" resource with id "rootApiId"
    And I extract response field "context" and store it as "rootContext"
    When I have set up application with keys, subscribed to API "rootApiId", and obtained access token for "rootSubId"
    Then The response status code should be 200

    # Invoke the ROOT path "/" — the root-path operation's policy must have injected our header on the backend
    # request that /reflect-headers (the routed endpoint) echoes.
    When I invoke the API at gateway context "{{rootContext}}/1.0.0/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
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

  # accessVerificationClaimValueRegex: the claim value (aut=APPLICATION) is additionally validated against a
  # regex. ^[A-Z]+$ MATCHES the uppercase APPLICATION claim, so the invocation is permitted (200). Ports
  # JWTClaimBasedAccessValidatorPolicyTestCase#...WithValidRegex.
  @cap:gateway @feat:mediation-policies @rule:claim-access-validator @type:regression @dep:publisher @legacy:JWTClaimBasedAccessValidatorPolicyTestCase
  Scenario Outline: A JWT-claim access-validator with a matching regex permits the invocation as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_claimvalidator_regexmatch_api.json" as "cvRxMatchApiId" and deployed it
    When I publish the "apis" resource with id "cvRxMatchApiId"
    Then The lifecycle status of API "cvRxMatchApiId" should be "Published"
    When I retrieve the "apis" resource with id "cvRxMatchApiId"
    And I extract response field "context" and store it as "cvRxMatchContext"
    When I have set up application with keys, subscribed to API "cvRxMatchApiId", and obtained access token for "cvRxMatchSubId"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{cvRxMatchContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # accessVerificationClaimValueRegex that does NOT match: ^[0-9]+$ does not match the alphabetic APPLICATION
  # claim value, so the validator blocks the invocation (403). Ports
  # JWTClaimBasedAccessValidatorPolicyTestCase#...WithInvalidRegex.
  @cap:gateway @feat:mediation-policies @rule:claim-access-validator @type:negative @dep:publisher @legacy:JWTClaimBasedAccessValidatorPolicyTestCase
  Scenario Outline: A JWT-claim access-validator with a non-matching regex blocks the invocation as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_claimvalidator_regexmiss_api.json" as "cvRxMissApiId" and deployed it
    When I publish the "apis" resource with id "cvRxMissApiId"
    Then The lifecycle status of API "cvRxMissApiId" should be "Published"
    When I retrieve the "apis" resource with id "cvRxMissApiId"
    And I extract response field "context" and store it as "cvRxMissContext"
    When I have set up application with keys, subscribed to API "cvRxMissApiId", and obtained access token for "cvRxMissSubId"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{cvRxMissContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # shouldAllowValidation (inverted logic): the configured value NON_MATCHING does NOT equal the token's
  # aut=APPLICATION claim, but with the validation INVERTED a non-match is what PERMITS the call (200). Ports
  # JWTClaimBasedAccessValidatorPolicyTestCase#...WithInvertedValidation.
  @cap:gateway @feat:mediation-policies @rule:claim-access-validator @type:regression @dep:publisher @legacy:JWTClaimBasedAccessValidatorPolicyTestCase
  Scenario Outline: An inverted JWT-claim access-validator permits a non-matching value as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_claimvalidator_inverted_api.json" as "cvInvApiId" and deployed it
    When I publish the "apis" resource with id "cvInvApiId"
    Then The lifecycle status of API "cvInvApiId" should be "Published"
    When I retrieve the "apis" resource with id "cvInvApiId"
    And I extract response field "context" and store it as "cvInvContext"
    When I have set up application with keys, subscribed to API "cvInvApiId", and obtained access token for "cvInvSubId"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{cvInvContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

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

  # Version copy carries operation policies: attach the custom_add_common_header request-flow policy to an API,
  # deploy, then COPY the API to a new version (2.0.0). The clone MUST carry the operation policies, so the NEW
  # VERSION — deployed, published, subscribed and invoked in its own right — still injects the header towards the
  # backend (observed via /reflect-headers). Ports OperationPolicyTestCase#testCreateNewVersionAfterAddingOperationPolicy.
  @cap:gateway @feat:mediation-policies @rule:add-header @type:regression @dep:publisher @legacy:OperationPolicyTestCase
  Scenario Outline: Operation policies carry over to a copied API version and still inject the header as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    # Register the common operation policy first so the base API can reference it by name.
    And I create a new common policy with spec "artifacts/payloads/policySpecFiles/custom_add_common_header.j2" and "artifacts/payloads/policySpecFiles/custom_add_common_header.yaml" as "verCommonPolicyId"
    And I have created an api from "artifacts/payloads/create_apim_oppolicy_api.json" as "verApiId" and deployed it
    When I publish the "apis" resource with id "verApiId"
    Then The lifecycle status of API "verApiId" should be "Published"

    # Copy the API to a NEW VERSION — the clone must carry the operation policies attached to v1.0.0.
    When I create a new version "2.0.0" of "apis" resource "verApiId" with default version "false" as "verV2Id"
    Then The response status code should be 201
    # Deploy + publish the NEW VERSION in its own right.
    When I deploy the API with id "verV2Id"
    Then The response status code should be 201
    When I publish the "apis" resource with id "verV2Id"
    Then The lifecycle status of API "verV2Id" should be "Published"
    When I retrieve the "apis" resource with id "verV2Id"
    And I extract response field "context" and store it as "verV2Context"
    When I have set up application with keys, subscribed to API "verV2Id", and obtained access token for "verSubId"
    Then The response status code should be 200

    # The carried-over policy still injects the header on the NEW VERSION's request to the backend.
    When I invoke the API at gateway context "{{verV2Context}}/2.0.0/reflect-headers" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "x-common-value"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Secret-attribute policy carries through a version copy: an API with the add_secret_headers policy
  # (apiKey=test-api-key-123, a Secret attribute) is COPIED to a new version (3.0.0). The clone must carry the
  # SECRET operation policy with its value intact, so the NEW VERSION — deployed, published, subscribed and
  # invoked in its own right — still injects the secret header towards the backend (observed via
  # /reflect-headers). This is distinct from the add-header version-copy (no secret) and the single-version
  # secret-attributes scenario (no version copy) above. Ports
  # OperationPolicyTestCase#testVersionCreationWithPolicyWithSecretAttributes.
 @cap:gateway @feat:mediation-policies @rule:secret-attributes @type:regression @dep:publisher @legacy:OperationPolicyTestCase
  Scenario Outline: A secret-attribute operation policy carries through a version copy and still injects the secret as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I create a new common policy with spec "artifacts/payloads/policySpecFiles/add_secret_headers.j2" and "artifacts/payloads/policySpecFiles/add_secret_headers.yaml" as "secVerPolicyId"
    And I have created an api from "artifacts/payloads/create_apim_secretpolicy_api.json" as "secVerApiId" and deployed it
    When I publish the "apis" resource with id "secVerApiId"
    Then The lifecycle status of API "secVerApiId" should be "Published"

    # Copy the API to a NEW VERSION — the clone must carry the SECRET operation policy attached to v1.0.0.
    When I create a new version "3.0.0" of "apis" resource "secVerApiId" with default version "false" as "secVerV3Id"
    Then The response status code should be 201
    # Deploy + publish the NEW VERSION in its own right.
    When I deploy the API with id "secVerV3Id"
    Then The response status code should be 201
    When I publish the "apis" resource with id "secVerV3Id"
    Then The lifecycle status of API "secVerV3Id" should be "Published"
    When I retrieve the "apis" resource with id "secVerV3Id"
    And I extract response field "context" and store it as "secVerV3Context"
    When I have set up application with keys, subscribed to API "secVerV3Id", and obtained access token for "secVerSubId"
    Then The response status code should be 200

    # The carried-over secret policy still injects the secret header on the NEW VERSION's backend request.
    When I invoke the API at gateway context "{{secVerV3Context}}/3.0.0/reflect-headers" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "test-api-key-123"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Multiple operation policies chained on one operation: the shipped addHeader policy (TestHeader=TestValue) is
  # attached BEFORE the custom_add_common_header policy (x-common-header=x-common-value) in the request flow.
  # Invoking the operation must run BOTH in order, so both injected headers reach the backend (observed via
  # /reflect-headers). Ports OperationPolicyTestCase#testAPIInvocationAfterAddingNewMultipleOperationPolicies.
  @cap:gateway @feat:mediation-policies @rule:add-header @type:regression @dep:publisher @legacy:OperationPolicyTestCase
  Scenario Outline: Multiple chained operation policies each inject their header towards the backend as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    # The shipped addHeader policy is referenced inline; register only the custom one first.
    And I create a new common policy with spec "artifacts/payloads/policySpecFiles/custom_add_common_header.j2" and "artifacts/payloads/policySpecFiles/custom_add_common_header.yaml" as "multiCommonPolicyId"
    And I have created an api from "artifacts/payloads/create_apim_multipolicy_api.json" as "multiApiId" and deployed it
    When I publish the "apis" resource with id "multiApiId"
    Then The lifecycle status of API "multiApiId" should be "Published"
    When I retrieve the "apis" resource with id "multiApiId"
    And I extract response field "context" and store it as "multiContext"
    When I have set up application with keys, subscribed to API "multiApiId", and obtained access token for "multiSubId"
    Then The response status code should be 200

    # BOTH policies in the chain ran: the shipped addHeader's value AND the custom policy's value are on the
    # backend request that /reflect-headers echoes.
    When I invoke the API at gateway context "{{multiContext}}/1.0.0/reflect-headers" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "TestValue"
    And The response should contain "x-common-value"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Content-aware json-eval mediation: a request-flow operation policy that reads the JSON body must not produce a
  # "Could not write JSON stream" gateway error when invoked with Content-Type: application/json and an empty
  # request body (no entity) on GET, POST, PUT, PATCH, and DELETE. The /reflect-body backend echoes the request
  # body so a gateway-side failure is observable in the response. Ports ContentAwareMediationPolicyEmptyBodyTestCase.
  @cap:gateway @feat:mediation-policies @rule:content-aware-empty-body @type:regression @dep:publisher @legacy:ContentAwareMediationPolicyEmptyBodyTestCase
  Scenario Outline: A content-aware json-eval policy does not error on empty-body <method> as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I create a new common policy with spec "artifacts/payloads/policySpecFiles/content_aware_property_policy.j2" and "artifacts/payloads/policySpecFiles/content_aware_property_policy.yaml" as "caPolicyId"
    And I have created an api from "artifacts/payloads/create_content_aware_empty_body_api.json" as "caApiId" and deployed it
    When I publish the "apis" resource with id "caApiId"
    Then The lifecycle status of API "caApiId" should be "Published"
    When I retrieve the "apis" resource with id "caApiId"
    And I extract response field "context" and store it as "caContext"
    When I have set up application with keys, subscribed to API "caApiId", and obtained access token for "caSubId"
    Then The response status code should be 200

    # Content-Type: application/json + empty body (no entity) — matches ContentAwareMediationPolicyEmptyBodyTestCase
    When I invoke the API at gateway context "{{caContext}}/1.0.0/reflect-body" with method "<method>" using access token "generatedAccessToken" and payload "" with request header "Content-Type" set to "application/json" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should not contain "Could not write JSON stream"
    And The response should not contain "Runtime Error"

    Examples:
      | actor             | method |
      | admin             | GET    |
      | admin             | POST   |
      | admin             | PUT    |
      | admin             | PATCH  |
      | admin             | DELETE |
      | admin@tenant1.com | GET    |
      | admin@tenant1.com | POST   |
      | admin@tenant1.com | PUT    |
      | admin@tenant1.com | PATCH  |
      | admin@tenant1.com | DELETE |
