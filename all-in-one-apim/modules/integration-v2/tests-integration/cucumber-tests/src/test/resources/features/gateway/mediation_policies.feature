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
    # Deploy-readiness gate (self-healing): the JMS deploy event is at-most-once, so if the gateway dropped
    # it, waiting alone can NEVER succeed — this re-emits the deploy after an exhausted window. Without it a
    # lost event surfaces as a 404 polled to the deadline, which is what made this runner intermittently red.
    And the "apis" resource "medApiId" should be live on the gateway, redeploying if propagation is lost
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
    # Deploy-readiness gate (self-healing): the JMS deploy event is at-most-once, so if the gateway dropped
    # it, waiting alone can NEVER succeed — this re-emits the deploy after an exhausted window. Without it a
    # lost event surfaces as a 404 polled to the deadline, which is what made this runner intermittently red.
    And the "apis" resource "rootApiId" should be live on the gateway, redeploying if propagation is lost
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
    # Deploy-readiness gate (self-healing): the JMS deploy event is at-most-once, so if the gateway dropped
    # it, waiting alone can NEVER succeed — this re-emits the deploy after an exhausted window. Without it a
    # lost event surfaces as a 404 polled to the deadline, which is what made this runner intermittently red.
    And the "apis" resource "cvMatchApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "cvMatchApiId"
    Then The lifecycle status of API "cvMatchApiId" should be "Published"
    When I retrieve the "apis" resource with id "cvMatchApiId"
    And I extract response field "context" and store it as "cvMatchContext"
    When I have set up application with keys, subscribed to API "cvMatchApiId", and obtained access token for "cvMatchSubId"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{cvMatchContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

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
    # Deploy-readiness gate (self-healing): the JMS deploy event is at-most-once, so if the gateway dropped
    # it, waiting alone can NEVER succeed — this re-emits the deploy after an exhausted window. Without it a
    # lost event surfaces as a 404 polled to the deadline, which is what made this runner intermittently red.
    And the "apis" resource "cvMissApiId" should be live on the gateway, redeploying if propagation is lost
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

  # The same policy configured with the claim PRESENT in the token but a NON-MATCHING value blocks the
  # invocation (403). Distinct from the missing-claim deny above: here accessVerificationClaim=aut IS carried by
  # the client-credentials token (aut=APPLICATION), but the policy requires aut=NOTAPPLICATION, so the value
  # comparison fails. Discriminating: a validator that checked only for the claim's PRESENCE and ignored its
  # VALUE would let this through and pass every other scenario. Ports
  # JWTClaimBasedAccessValidatorPolicyTestCase#...WithInvalidClaimValue.
  @cap:gateway @feat:mediation-policies @rule:claim-access-validator @type:negative @dep:publisher @legacy:JWTClaimBasedAccessValidatorPolicyTestCase
  Scenario Outline: A JWT-claim access-validator whose claim is present but value mismatches blocks the invocation as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_claimvalidator_valuemismatch_api.json" as "cvValMissApiId" and deployed it
    When I publish the "apis" resource with id "cvValMissApiId"
    Then The lifecycle status of API "cvValMissApiId" should be "Published"
    When I retrieve the "apis" resource with id "cvValMissApiId"
    And I extract response field "context" and store it as "cvValMissContext"
    When I have set up application with keys, subscribed to API "cvValMissApiId", and obtained access token for "cvValMissSubId"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{cvValMissContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 403 within 60 seconds
    Then The response status code should be 403
    # The value-mismatch deny carries error code 900912 "Claim Mismatch" — a DIFFERENT code from the missing-claim
    # deny above (which the validator reports when the claim is absent). Pinning it proves the validator distinguished
    # a present-but-wrong-value claim from an absent one, the exact behaviour a presence-only check would get wrong.
    And The response should contain "900912"
    And The response should contain "Claim Mismatch"

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
    # Deploy-readiness gate (self-healing): the JMS deploy event is at-most-once, so if the gateway dropped
    # it, waiting alone can NEVER succeed — this re-emits the deploy after an exhausted window. Without it a
    # lost event surfaces as a 404 polled to the deadline, which is what made this runner intermittently red.
    And the "apis" resource "cvRxMatchApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "cvRxMatchApiId"
    Then The lifecycle status of API "cvRxMatchApiId" should be "Published"
    When I retrieve the "apis" resource with id "cvRxMatchApiId"
    And I extract response field "context" and store it as "cvRxMatchContext"
    When I have set up application with keys, subscribed to API "cvRxMatchApiId", and obtained access token for "cvRxMatchSubId"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{cvRxMatchContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

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
    # Deploy-readiness gate (self-healing): the JMS deploy event is at-most-once, so if the gateway dropped
    # it, waiting alone can NEVER succeed — this re-emits the deploy after an exhausted window. Without it a
    # lost event surfaces as a 404 polled to the deadline, which is what made this runner intermittently red.
    And the "apis" resource "cvRxMissApiId" should be live on the gateway, redeploying if propagation is lost
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
    # Deploy-readiness gate (self-healing): the JMS deploy event is at-most-once, so if the gateway dropped
    # it, waiting alone can NEVER succeed — this re-emits the deploy after an exhausted window. Without it a
    # lost event surfaces as a 404 polled to the deadline, which is what made this runner intermittently red.
    And the "apis" resource "cvInvApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "cvInvApiId"
    Then The lifecycle status of API "cvInvApiId" should be "Published"
    When I retrieve the "apis" resource with id "cvInvApiId"
    And I extract response field "context" and store it as "cvInvContext"
    When I have set up application with keys, subscribed to API "cvInvApiId", and obtained access token for "cvInvSubId"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{cvInvContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Secret-attribute operation policy: a policy declaring "Secret"-type attributes (apiKey mandatory, token
  # optional) injects those values as headers towards the backend. The secret value is observed on the backend
  # request (via /reflect-headers) but is MASKED (not returned) in the publisher representation of the API.
  # F3 (the load-bearing extension): after proving injection, an EMPTY-value update must PRESERVE the secret, not
  # wipe it — this is the ONLY assertion that separates "preserved" from "silently cleared" at runtime, since the
  # publisher plane masks a preserved secret to "" (indistinguishable from a wipe there). Ports the
  # injection + testUpdatePolicyWithSecretAttributes slices of OperationPolicyTestCase.
  @cap:gateway @feat:mediation-policies @rule:secret-attributes @type:regression @dep:publisher @legacy:OperationPolicyTestCase
  Scenario Outline: A secret-attribute operation policy injects the secret, masks it on retrieval, and preserves it on an empty-value update as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I create a new common policy with spec "artifacts/payloads/policySpecFiles/add_secret_headers.j2" and "artifacts/payloads/policySpecFiles/add_secret_headers.yaml" as "secretPolicyId"
    And I have created an api from "artifacts/payloads/create_apim_secretpolicy_api.json" as "secretApiId" and deployed it
    # Deploy-readiness gate (self-healing): the JMS deploy event is at-most-once, so if the gateway dropped
    # it, waiting alone can NEVER succeed — this re-emits the deploy after an exhausted window. Without it a
    # lost event surfaces as a 404 polled to the deadline, which is what made this runner intermittently red.
    And the "apis" resource "secretApiId" should be live on the gateway, redeploying if propagation is lost
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

    # F3: update the policy parameters supplying an EMPTY apiKey (the same update the publisher scenario does),
    # redeploy, wait for propagation, and confirm the REAL value STILL reaches the backend — the end-to-end proof
    # that preserve-on-empty is genuine and not a silent wipe the masked publisher view would hide.
    When I update the parameters of the operation policy in flow "request" of operation 0 of API "secretApiId" to "{\"apiKey\":\"\",\"token\":\"\"}"
    Then The response status code should be 200
    When I deploy the API with id "secretApiId"
    And the "apis" resource "secretApiId" should be live on the gateway, redeploying if propagation is lost
    When I invoke the API at gateway context "{{secretContext}}/1.0.0/reflect-headers" with method "GET" using access token "generatedAccessToken" and payload "" until response body contains "test-api-key-123" within 60 seconds
    Then The response status code should be 200
    And The response should contain "test-api-key-123"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Attach baseline + optional-secret measurement. A plain reflect-headers API (no policy) is invoked FIRST to
  # establish that neither injected secret header is present, then the add_secret_headers policy is attached to the
  # GET operation (mandatory apiKey set, optional token empty) and redeployed. The mandatory apiKey then reaches
  # the backend with its real value; the optional token's on-the-wire shape is measured (its own last assertion,
  # so a wrong guess costs only that line). The injected header names arrive lowercased by the node backend
  # (req.headers). Ports testAPIBeforeAttachingPolicyWithSecretAttributes + the after-attach header assertions of
  # testAPIAfterAttachingPolicyWithSecretAttributes.
  @cap:gateway @feat:mediation-policies @rule:secret-attributes @type:regression @dep:publisher @legacy:OperationPolicyTestCase
  Scenario Outline: A secret-attribute policy is absent before attach and injects the secret headers after attach as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I create a new common policy with spec "artifacts/payloads/policySpecFiles/add_secret_headers.j2" and "artifacts/payloads/policySpecFiles/add_secret_headers.yaml" as "baseSecretPolicyId"
    And I have created an api from "artifacts/payloads/create_apim_reflect_api.json" as "baseSecretApiId" and deployed it
    When I publish the "apis" resource with id "baseSecretApiId"
    Then The lifecycle status of API "baseSecretApiId" should be "Published"
    When I retrieve the "apis" resource with id "baseSecretApiId"
    And I extract response field "context" and store it as "baseSecretContext"
    When I have set up application with keys, subscribed to API "baseSecretApiId", and obtained access token for "baseSecretSubId"
    Then The response status code should be 200

    # BASELINE: with no policy attached, neither the secret value nor the injected header names reach the backend.
    When I invoke the API at gateway context "{{baseSecretContext}}/1.0.0/reflect-headers" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should not contain "test-api-key-123"
    And The response should not contain "\"apikey\""

    # Attach the secret policy to the GET operation: mandatory apiKey set, optional token left empty. Redeploy.
    When I attach the common operation policy "add_secret_headers" to operation 0 of API "baseSecretApiId" in flows "request" with parameters "{\"apiKey\":\"test-api-key-123\",\"token\":\"\"}"
    Then The response status code should be 200
    When I deploy the API with id "baseSecretApiId"
    And the "apis" resource "baseSecretApiId" should be live on the gateway, redeploying if propagation is lost

    # AFTER attach: the mandatory apiKey header now carries its real value at the backend.
    When I invoke the API at gateway context "{{baseSecretContext}}/1.0.0/reflect-headers" with method "GET" using access token "generatedAccessToken" and payload "" until response body contains "test-api-key-123" within 60 seconds
    Then The response status code should be 200
    And The response should contain "test-api-key-123"
    And The response should contain "\"apikey\""
    # The OPTIONAL token secret was attached empty — measured shape: header present with an empty value.
    And The response should contain "\"token\":\"\""

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Detach stops injection. After the secret policy injects the header at the backend, its operation policies are
  # removed and the API is redeployed; the secret header (and its value) must no longer reach the backend. Ports
  # the clear-then-verify half of testDeleteAPISpecificOperationPolicyWithSecrets.
  @cap:gateway @feat:mediation-policies @rule:secret-attributes @type:regression @dep:publisher @legacy:OperationPolicyTestCase
  Scenario Outline: Detaching a secret-attribute operation policy stops the header injection as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I create a new common policy with spec "artifacts/payloads/policySpecFiles/add_secret_headers.j2" and "artifacts/payloads/policySpecFiles/add_secret_headers.yaml" as "detachSecretPolicyId"
    And I have created an api from "artifacts/payloads/create_apim_secretpolicy_api.json" as "detachSecretApiId" and deployed it
    When I publish the "apis" resource with id "detachSecretApiId"
    Then The lifecycle status of API "detachSecretApiId" should be "Published"
    When I retrieve the "apis" resource with id "detachSecretApiId"
    And I extract response field "context" and store it as "detachSecretContext"
    When I have set up application with keys, subscribed to API "detachSecretApiId", and obtained access token for "detachSecretSubId"
    Then The response status code should be 200

    # The secret is injected while the policy is attached.
    When I invoke the API at gateway context "{{detachSecretContext}}/1.0.0/reflect-headers" with method "GET" using access token "generatedAccessToken" and payload "" until response body contains "test-api-key-123" within 60 seconds
    Then The response status code should be 200
    And The response should contain "test-api-key-123"

    # Detach the operation policies by clearing all flows via the generic operations-update step (same inline
    # operations-replace pattern as api_product_invocation.feature:266) and redeploy — the injection must stop.
    When I retrieve the "apis" resource with id "detachSecretApiId"
    And I put the response payload in context as "detachApiPayload"
    When I update the "apis" resource "detachSecretApiId" and "detachApiPayload" with configuration type "operations" and value:
      """
      [{"target":"/reflect-headers","verb":"GET","authType":"Application & Application User","throttlingPolicy":"Unlimited","scopes":[],"operationPolicies":{"request":[],"response":[],"fault":[]}}]
      """
    Then The response status code should be 200
    # (a) Management plane: confirm the detach actually emptied the operation's request-flow policy list.
    When I retrieve the "apis" resource with id "detachSecretApiId"
    Then The response status code should be 200
    And The response field "operations[0].operationPolicies.request" should be exactly the list ""
    # (b) A policy change reaches the gateway only on redeploy.
    When I deploy the API with id "detachSecretApiId"
    And the "apis" resource "detachSecretApiId" should be live on the gateway, redeploying if propagation is lost
    # (c) Wait for the NEW policy-free revision to be the deployed one (it settles the synapse hot-swap) — otherwise
    # the invoke races the redeploy and reads the still-live old sequence, which returns 200 with the header intact.
    And I wait until "apis" "detachSecretApiId" revision is deployed in the gateway
    When I invoke the API at gateway context "{{detachSecretContext}}/1.0.0/reflect-headers" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should not contain "test-api-key-123"
    And The response should not contain "\"apikey\""

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
    # Deploy-readiness gate (self-healing): the JMS deploy event is at-most-once, so if the gateway dropped
    # it, waiting alone can NEVER succeed — this re-emits the deploy after an exhausted window. Without it a
    # lost event surfaces as a 404 polled to the deadline, which is what made this runner intermittently red.
    And the "apis" resource "jxApiId" should be live on the gateway, redeploying if propagation is lost
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
    # Deploy-readiness gate (self-healing): the JMS deploy event is at-most-once, so if the gateway dropped
    # it, waiting alone can NEVER succeed — this re-emits the deploy after an exhausted window. Without it a
    # lost event surfaces as a 404 polled to the deadline, which is what made this runner intermittently red.
    And the "apis" resource "jsApiId" should be live on the gateway, redeploying if propagation is lost
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
    # Deploy-readiness gate (self-healing): the JMS deploy event is at-most-once, so if the gateway dropped
    # it, waiting alone can NEVER succeed — this re-emits the deploy after an exhausted window. Without it a
    # lost event surfaces as a 404 polled to the deadline, which is what made this runner intermittently red.
    And the "apis" resource "verV2Id" should be live on the gateway, redeploying if propagation is lost
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
    # Deploy-readiness gate (self-healing): the JMS deploy event is at-most-once, so if the gateway dropped
    # it, waiting alone can NEVER succeed — this re-emits the deploy after an exhausted window. Without it a
    # lost event surfaces as a 404 polled to the deadline, which is what made this runner intermittently red.
    And the "apis" resource "secVerV3Id" should be live on the gateway, redeploying if propagation is lost
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
    # Deploy-readiness gate (self-healing): the JMS deploy event is at-most-once, so if the gateway dropped
    # it, waiting alone can NEVER succeed — this re-emits the deploy after an exhausted window. Without it a
    # lost event surfaces as a 404 polled to the deadline, which is what made this runner intermittently red.
    And the "apis" resource "multiApiId" should be live on the gateway, redeploying if propagation is lost
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
    # Deploy-readiness gate (self-healing): the JMS deploy event is at-most-once, so if the gateway dropped
    # it, waiting alone can NEVER succeed — this re-emits the deploy after an exhausted window. Without it a
    # lost event surfaces as a 404 polled to the deadline, which is what made this runner intermittently red.
    And the "apis" resource "caApiId" should be live on the gateway, redeploying if propagation is lost
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

  # THE CONTENT-TYPE HALF of a request-flow transformation — what legacy's APIInvocationWithMessageTypeProperty
  # set out to prove and never asserted. Its point was that a messageType/content-type change in the in-sequence
  # reaches the BACKEND: it started a WireMonitorServer to capture the backend-bound message, then asserted only
  # that the HTTP reason phrase contained "Accepted". Delete the property from the sequence and that test still
  # passes, so it protected nothing. Ported here as the assertion it should have made.
  #
  # The scenario above already pins that the converted BODY survives to the backend. This pins the other half:
  # the backend receives it as application/xml, not as the JSON that was sent. /reflect-body-typed echoes the
  # body back with the SAME Content-Type it received, so the RESPONSE Content-Type is a faithful readout of what
  # the backend actually got — no wire-capture server needed.
  #
  # Without this, a policy that mangled the content type while still producing XML-shaped text would pass the
  # body assertion above unnoticed.
  @cap:gateway @feat:mediation-policies @rule:json-to-xml @type:regression @dep:publisher @legacy:APIInvocationWithMessageTypeProperty
  Scenario Outline: The jsonToXML policy makes the backend receive the request as XML as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_jsontoxml_typed_api.json" as "mtApiId" and deployed it
    When I publish the "apis" resource with id "mtApiId"
    Then The lifecycle status of API "mtApiId" should be "Published"
    When I retrieve the "apis" resource with id "mtApiId"
    And I extract response field "context" and store it as "mtContext"
    When I have set up application with keys, subscribed to API "mtApiId", and obtained access token for "mtSubId"
    Then The response status code should be 200

    # A JSON request. The request-flow policy must convert it before it leaves the gateway.
    When I put the following JSON payload in context as "mtPayload"
    """
    { "name" : "messageTypeProbe" }
    """
    When I invoke the API at gateway context "{{mtContext}}/1.0.0/reflect-body-typed" with method "POST" using access token "generatedAccessToken" and payload "mtPayload" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    # The echo carries back the Content-Type the BACKEND saw — the transformation is proven at the far end,
    # not merely at the gateway. This is the assertion legacy's wire server existed for.
    And The response header "Content-Type" should contain "xml"
    # ...and the payload really is XML-serialised, not JSON that merely arrived under an XML content type.
    And The response should contain "<name>messageTypeProbe</name>"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # By default the gateway streams a POST body on to the backend with Transfer-Encoding: chunked and no
  # Content-Length. A request-flow operation policy setting the axis2-scope properties the legacy
  # FORCE_HTTP_CONTENT_LENGTH sequence set makes the passthru target send an explicit Content-Length instead.
  # Legacy captured the backend-bound bytes with a WireMonitorServer; v2 has no wire monitor, so
  # /reflect-headers is the observation point (made POST-capable, echoing the received headers AND the body).
  #
  # MEASURED on 4.7.0, do not trim the policy back: FORCE_HTTP_CONTENT_LENGTH +
  # COPY_CONTENT_LENGTH_FROM_INCOMING ALONE leave the wire chunked. That was verified with a witness property
  # in the same sequence (a transport header carrying get-property('axis2','FORCE_HTTP_CONTENT_LENGTH')), which
  # came back as "true" on a still-chunked request — so the sequence ran and the property was set, and the
  # passthru simply did not act on it. Adding the axis2-scope DISABLE_CHUNKING is what makes the backend receive
  # Content-Length; the policy therefore carries all three, as the legacy sequence did.
  #
  # The BEFORE half is what makes this prove something: it measures the gateway DEFAULT (chunked, no
  # content-length) on the very same API, so the after-attach assertion cannot be satisfied by behaviour the
  # gateway would have had anyway. The absence of transfer-encoding after the attach is gated on positive
  # evidence the call really reached the backend — the exact content-length AND the intact echoed body — so a
  # request that never arrived can never pass it. Ports ContentLengthHeaderTestCase.
  @cap:gateway @feat:mediation-policies @rule:force-content-length @type:regression @dep:publisher @legacy:ContentLengthHeaderTestCase
  Scenario Outline: A FORCE_HTTP_CONTENT_LENGTH policy sends the backend a Content-Length instead of chunking as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I create a new common policy with spec "artifacts/payloads/policySpecFiles/force_content_length.j2" and "artifacts/payloads/policySpecFiles/force_content_length.yaml" as "fclPolicyId"
    And I have created an api from "artifacts/payloads/create_apim_force_content_length_api.json" as "fclApiId" and deployed it
    When I publish the "apis" resource with id "fclApiId"
    Then The lifecycle status of API "fclApiId" should be "Published"
    When I retrieve the "apis" resource with id "fclApiId"
    And I extract response field "context" and store it as "fclContext"
    When I have set up application with keys, subscribed to API "fclApiId", and obtained access token for "fclSubId"
    Then The response status code should be 200

    # A body of KNOWN size: {"probe":"forceContentLength"} is exactly 30 bytes (no trailing newline — a Gherkin
    # docstring carries neither the indentation nor a trailing newline).
    When I put the following JSON payload in context as "fclPayload"
    """
    {"probe":"forceContentLength"}
    """

    # BASELINE — no policy attached: the gateway chunks the body on to the backend and sends no Content-Length.
    When I invoke the API at gateway context "{{fclContext}}/1.0.0/reflect-headers" with method "POST" using access token "generatedAccessToken" and payload "fclPayload" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "$.body" should be "{\"probe\":\"forceContentLength\"}"
    And The response should contain "\"transfer-encoding\":\"chunked\""
    And The response should not contain "content-length"

    # Attach the axis2-scope property policy to the POST operation; a policy change reaches the gateway only on
    # redeploy, and the new revision must be the deployed one before the effect is observable.
    When I attach the common operation policy "force_content_length" to operation 0 of API "fclApiId" in flows "request" with parameters "{}"
    Then The response status code should be 200
    When I deploy the API with id "fclApiId"
    And the "apis" resource "fclApiId" should be live on the gateway, redeploying if propagation is lost
    And I wait until "apis" "fclApiId" revision is deployed in the gateway

    # AFTER attach: the entity is buffered and sent with an explicit Content-Length of exactly the 30 bytes posted...
    When I invoke the API at gateway context "{{fclContext}}/1.0.0/reflect-headers" with method "POST" using access token "generatedAccessToken" and payload "fclPayload" until response body contains "content-length" within 60 seconds
    Then The response status code should be 200
    And The value of response field "$.headers['content-length']" should be "30"
    # ...the body still arrived intact (with the exact length above, the discriminating gate that the request
    # really reached the backend — so the absence assertion below cannot pass on a call that never happened)...
    And The value of response field "$.body" should be "{\"probe\":\"forceContentLength\"}"
    # ...and the chunked encoding is gone.
    And The response should not contain "transfer-encoding"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

