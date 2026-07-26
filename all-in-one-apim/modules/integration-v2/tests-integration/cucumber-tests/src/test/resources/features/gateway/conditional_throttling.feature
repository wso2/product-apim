@cleanup
Feature: Gateway Conditional-Group Advanced Throttling Enforcement

  Runtime enforcement of ADVANCED (API-level) throttling policies that carry a CONDITIONAL GROUP — a lower
  request-count limit that applies only to requests matching a condition (IP / header / query-param /
  JWT-claim), while non-matching requests use the higher default limit. Each scenario creates a policy whose
  default limit is high (100/min) and whose conditional group is low (3/min), attaches it at the API level,
  redeploys, then: warms the route with a NON-matching request (served by the default limit → 200) and drives
  a MATCHING request until the group's lower limit trips a 429. This proves the condition is evaluated at the
  gateway — which requires the header/query/JWT-claim throttling flags enabled by this block's overlay (they
  are off in the distribution default; IP throttling is on by default). Runs in both tenants as the tenant
  admin. Ports the enforcement arc of JWTRequestCountThrottlingTestCase. Teardown via the per-scenario hook.

  @cap:gateway @feat:throttling-enforcement @rule:conditional-ip @type:regression @dep:admin @dep:publisher @dep:devportal @legacy:JWTRequestCountThrottlingTestCase
  Scenario Outline: An IP conditional group throttles a matching client IP at its lower limit as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create an advanced throttling policy "${UNIQUE:condIp}" allowing 100 requests per minute with an IP conditional group of 3 requests per minute for IP "10.100.7.99"
    Then The response status code should be 201
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "condIpApiId" and deployed it
    When I retrieve the "apis" resource with id "condIpApiId"
    And I put the response payload in context as "condIpPayload"
    When I update the "apis" resource "condIpApiId" and "condIpPayload" with configuration type "apiThrottlingPolicy" and value:
    """
    {{advThrottlePolicyName}}
    """
    Then The response status code should be 200
    When I deploy the API with id "condIpApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "condIpApiId"
    Then The lifecycle status of API "condIpApiId" should be "Published"
    When I retrieve the "apis" resource with id "condIpApiId"
    And I extract response field "context" and store it as "condIpContext"
    When I have set up application with keys, subscribed to API "condIpApiId", and obtained access token for "condIpSub"
    Then The response status code should be 200
    # Warm up with a non-matching request (uses the 100/min default) so the route is confirmed up.
    When I invoke the API at gateway context "{{condIpContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    # Drive a request whose X-Forwarded-For matches the IP condition → the 3/min group limit trips a 429.
    When I invoke the API at gateway context "{{condIpContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" with request header "X-Forwarded-For" set to "10.100.7.99" until response status code becomes 429 within 60 seconds
    Then The response status code should be 429

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:gateway @feat:throttling-enforcement @rule:conditional-header @type:regression @dep:admin @dep:publisher @dep:devportal @legacy:JWTRequestCountThrottlingTestCase
  Scenario Outline: A header conditional group throttles a matching header at its lower limit as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create an advanced throttling policy "${UNIQUE:condHdr}" allowing 100 requests per minute with a header conditional group of 3 requests per minute for header "X-Tier" value "gold"
    Then The response status code should be 201
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "condHdrApiId" and deployed it
    When I retrieve the "apis" resource with id "condHdrApiId"
    And I put the response payload in context as "condHdrPayload"
    When I update the "apis" resource "condHdrApiId" and "condHdrPayload" with configuration type "apiThrottlingPolicy" and value:
    """
    {{advThrottlePolicyName}}
    """
    Then The response status code should be 200
    When I deploy the API with id "condHdrApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "condHdrApiId"
    Then The lifecycle status of API "condHdrApiId" should be "Published"
    When I retrieve the "apis" resource with id "condHdrApiId"
    And I extract response field "context" and store it as "condHdrContext"
    When I have set up application with keys, subscribed to API "condHdrApiId", and obtained access token for "condHdrSub"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{condHdrContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    # Drive a request carrying the X-Tier: gold header → the 3/min group limit trips a 429.
    When I invoke the API at gateway context "{{condHdrContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" with request header "X-Tier" set to "gold" until response status code becomes 429 within 60 seconds
    Then The response status code should be 429

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:gateway @feat:throttling-enforcement @rule:conditional-query @type:regression @dep:admin @dep:publisher @dep:devportal @legacy:JWTRequestCountThrottlingTestCase
  Scenario Outline: A query-parameter conditional group throttles a matching query at its lower limit as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create an advanced throttling policy "${UNIQUE:condQry}" allowing 100 requests per minute with a query conditional group of 3 requests per minute for query "name" value "admin"
    Then The response status code should be 201
    # A resource whose URI template DECLARES the ?name query parameter, so a raw invoke carrying ?name=<v> routes
    # to the gateway (an undeclared query on a plain resource is treated as path and 404s in this harness).
    And I have created an api from "artifacts/payloads/create_apim_query_throttle_api.json" as "condQryApiId" and deployed it
    When I retrieve the "apis" resource with id "condQryApiId"
    And I put the response payload in context as "condQryPayload"
    When I update the "apis" resource "condQryApiId" and "condQryPayload" with configuration type "apiThrottlingPolicy" and value:
    """
    {{advThrottlePolicyName}}
    """
    Then The response status code should be 200
    When I deploy the API with id "condQryApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "condQryApiId"
    Then The lifecycle status of API "condQryApiId" should be "Published"
    When I retrieve the "apis" resource with id "condQryApiId"
    And I extract response field "context" and store it as "condQryContext"
    When I have set up application with keys, subscribed to API "condQryApiId", and obtained access token for "condQrySub"
    Then The response status code should be 200
    # Warm up with a NON-matching query value (?name=warmup) → served by the 100/min default → 200.
    When I invoke the API at raw gateway context "{{condQryContext}}/1.0.0/qthrottle?name=warmup" using access token "generatedAccessToken" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    # Drive ?name=admin (the matching value) → the 3/min query conditional group trips a 429.
    When I invoke the API at raw gateway context "{{condQryContext}}/1.0.0/qthrottle?name=admin" using access token "generatedAccessToken" until response status code becomes 429 within 60 seconds
    Then The response status code should be 429

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # JWT-claim group: the backend JWT (which throttling evaluates via
  # ThrottleConditionEvaluator#isJWTClaimPresent -> JWTUtil.getJWTClaims(callerToken)) carries the
  # applicationname claim, so an application whose NAME equals the claim value has every request governed by the
  # 3/min group. Needs [apim.jwt] enable = true (backend JWT) — set by this block's overlay.
  @cap:gateway @feat:throttling-enforcement @rule:conditional-jwt @type:regression @dep:admin @dep:publisher @dep:devportal @legacy:JWTRequestCountThrottlingTestCase
  Scenario Outline: A JWT-claim conditional group throttles a matching application at its lower limit as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I generate a unique value and store it as "jwtClaimApp"
    When I create an advanced throttling policy "${UNIQUE:condJwt}" allowing 100 requests per minute with a JWT claim conditional group of 3 requests per minute for claim "http://wso2.org/claims/applicationname" value "{{jwtClaimApp}}"
    Then The response status code should be 201
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "condJwtApiId" and deployed it
    When I retrieve the "apis" resource with id "condJwtApiId"
    And I put the response payload in context as "condJwtPayload"
    When I update the "apis" resource "condJwtApiId" and "condJwtPayload" with configuration type "apiThrottlingPolicy" and value:
    """
    {{advThrottlePolicyName}}
    """
    Then The response status code should be 200
    When I deploy the API with id "condJwtApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "condJwtApiId"
    Then The lifecycle status of API "condJwtApiId" should be "Published"
    When I retrieve the "apis" resource with id "condJwtApiId"
    And I extract response field "context" and store it as "condJwtContext"
    # An application whose NAME equals the claim value → its backend JWT carries applicationname={{jwtClaimApp}},
    # so every request from it matches the JWT-claim condition and is governed by the 3/min group limit.
    When I put the following JSON payload in context as "jwtApp"
    """
    {"name":"{{jwtClaimApp}}","throttlingPolicy":"Unlimited","description":"jwt-claim match"}
    """
    And I create an application with payload "jwtApp"
    Then The response status code should be 201
    When I put the following JSON payload in context as "jwtSub"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "condJwtApiId" using application "createdAppId" with payload "jwtSub" as "jwtSubId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "jwtKeys"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "jwtKeys"
    Then The response status code should be 200
    When I put the following JSON payload in context as "jwtToken"
    """
    {"consumerSecret": "{{appConsumerSecret}}", "validityPeriod": 3600}
    """
    And I request an access token for application id "createdAppId" using payload "jwtToken"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{condJwtContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 429 within 90 seconds
    Then The response status code should be 429

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
