@cleanup
Feature: API invocation analytics events

  Cover the invocation telemetry behavior from APIInvocationStatPublisherTestCase using the documented
  analytics event contract. The ELK reporter represents successful calls as response events, with request,
  backend, and response timing carried as fields on that event. Failed calls are emitted as fault events.
  This intentionally checks the supported analytics contract rather than the legacy internal Thrift stream names.

  @cap:analytics @feat:analytics-events @rule:successful-invocation @type:smoke @dep:gateway @legacy:APIInvocationStatPublisherTestCase
  Scenario Outline: A successful authenticated invocation publishes identity, response and latency fields as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "statsApiId" and deployed it
    And the "apis" resource "statsApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "statsApiId"
    Then The lifecycle status of API "statsApiId" should be "Published"
    When I retrieve the "apis" resource with id "statsApiId"
    And I extract response field "context" and store it as "statsApiContext"
    And I extract response field "name" and store it as "statsApiName"
    And I have set up application with keys, subscribed to API "statsApiId", and obtained access token for "statsSubId"
    Then The response status code should be 200

    When I mark the current end of the server log file "apim_metrics.log"
    And I invoke the API at gateway context "{{statsApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The server log file "apim_metrics.log" should gain event "apim:response" for API "{{statsApiName}}" with these fields within 60 seconds
      | field                    | expected      |
      | eventType                | response      |
      | apiId                    | {{statsApiId}} |
      | apiContext               | {{statsApiContext}}/1.0.0 |
      | apiVersion               | 1.0.0         |
      | apiMethod                | GET           |
      | apiResourceTemplate      | /customers/{id} |
      | proxyResponseCode        | 200           |
      | targetResponseCode       | 200           |
      | responseCacheHit         | false         |
      | applicationName          | <present>     |
      | applicationId            | <present>     |
      | applicationOwner         | <present>     |
      | userName                 | <invocationUser> |
      | backendLatency           | <number>      |
      | requestMediationLatency  | <number>      |
      | responseMediationLatency | <number>      |
      | responseLatency          | <number>      |
      | requestTimestamp         | <present>     |
      | destination              | http://nodebackend:3001/jaxrs_basic/services/customers/customerservice/ |
      | apiCreator               | <present>     |
      | apiCreatorTenantDomain   | <tenantDomain> |

    Examples:
      | actor             | tenantDomain | invocationUser       |
      | admin             | carbon.super | admin@carbon.super   |
      | admin@tenant1.com | tenant1.com  | admin@tenant1.com    |

  @cap:analytics @feat:analytics-events @rule:anonymous-invocation @type:smoke @dep:gateway @legacy:APIInvocationStatPublisherTestCase
  Scenario Outline: A successful anonymous invocation publishes the response and timing fields as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_authtype_none_api.json" as "statsAnonymousApiId" and deployed it
    And the "apis" resource "statsAnonymousApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "statsAnonymousApiId"
    Then The lifecycle status of API "statsAnonymousApiId" should be "Published"
    When I retrieve the "apis" resource with id "statsAnonymousApiId"
    And I extract response field "context" and store it as "statsAnonymousContext"
    And I extract response field "name" and store it as "statsAnonymousName"

    When I mark the current end of the server log file "apim_metrics.log"
    And I invoke the API at gateway context "{{statsAnonymousContext}}/1.0.0/customers/123/" with method "GET" without authentication until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The server log file "apim_metrics.log" should gain event "apim:response" for API "{{statsAnonymousName}}" with these fields within 60 seconds
      | field                    | expected      |
      | eventType                | response      |
      | apiId                    | {{statsAnonymousApiId}} |
      | apiContext               | {{statsAnonymousContext}}/1.0.0 |
      | apiVersion               | 1.0.0         |
      | apiMethod                | GET           |
      | apiResourceTemplate      | /customers/{id} |
      | proxyResponseCode        | 200           |
      | userName                 | anonymous     |
      | backendLatency           | <number>      |
      | requestMediationLatency  | <number>      |
      | responseMediationLatency | <number>      |
      | responseLatency          | <number>      |
      | requestTimestamp         | <present>     |
      | destination              | http://nodebackend:3001/jaxrs_basic/services/customers/customerservice/ |
      | apiCreator               | <present>     |
      | apiCreatorTenantDomain   | <tenantDomain> |

    Examples:
      | actor             | tenantDomain |
      | admin             | carbon.super |
      | admin@tenant1.com | tenant1.com  |

  @cap:analytics @feat:analytics-events @rule:failed-invocation @type:regression @dep:gateway @legacy:APIInvocationStatPublisherTestCase
  Scenario Outline: Failed authenticated and anonymous invocations publish a fault event as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_analytics_fault_api.json" as "statsFaultApiId" and deployed it
    And the "apis" resource "statsFaultApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "statsFaultApiId"
    Then The lifecycle status of API "statsFaultApiId" should be "Published"
    When I retrieve the "apis" resource with id "statsFaultApiId"
    And I extract response field "context" and store it as "statsFaultContext"
    And I extract response field "name" and store it as "statsFaultName"
    And I have set up application with keys, subscribed to API "statsFaultApiId", and obtained access token for "statsFaultSubId"
    Then The response status code should be 200

    When I mark the current end of the server log file "apim_metrics.log"
    And I invoke the API at gateway context "{{statsFaultContext}}/1.0.0/secured/123" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 500 within 60 seconds
    Then The response status code should be 500
    And The server log file "apim_metrics.log" should gain event "apim:faulty" for API "{{statsFaultName}}" with these fields within 60 seconds
      | field                  | expected      |
      | eventType              | fault         |
      | apiId                  | {{statsFaultApiId}} |
      | proxyResponseCode      | 500           |
      | errorCode              | <present>     |
      | errorMessage           | <present>     |
      | errorType              | <present>     |
      | apiVersion             | 1.0.0         |
      | applicationName        | <present>     |
      | applicationOwner       | <present>     |
      | properties.apiContext  | {{statsFaultContext}}/1.0.0 |
      | properties.userName    | <invocationUser> |
      | apiCreatorTenantDomain | <tenantDomain> |

    When I mark the current end of the server log file "apim_metrics.log"
    And I invoke the API at gateway context "{{statsFaultContext}}/1.0.0/anonymous/123" with method "GET" without authentication until response status code becomes 500 within 60 seconds
    Then The response status code should be 500
    And The server log file "apim_metrics.log" should gain event "apim:faulty" for API "{{statsFaultName}}" with these fields within 60 seconds
      | field                  | expected      |
      | eventType              | fault         |
      | apiId                  | {{statsFaultApiId}} |
      | proxyResponseCode      | 500           |
      | errorCode              | <present>     |
      | errorMessage           | <present>     |
      | errorType              | <present>     |
      | apiVersion             | 1.0.0         |
      | properties.apiContext  | {{statsFaultContext}}/1.0.0 |
      | properties.userName    | anonymous     |
      | apiCreatorTenantDomain | <tenantDomain> |

    Examples:
      | actor             | tenantDomain | invocationUser       |
      | admin             | carbon.super | admin@carbon.super   |
      | admin@tenant1.com | tenant1.com  | admin@tenant1.com    |

  # The legacy throttle-stream test was disabled because its 1-TPS API burst was intermittent. The documented
  # event contract explicitly includes throttled invocations, so use a dedicated 1-request/min application
  # policy with a fresh application counter to test that event deterministically.
  @cap:analytics @feat:analytics-events @rule:throttle-fault @type:regression @dep:gateway @dep:admin @legacy:APIInvocationStatPublisherTestCase
  Scenario Outline: Exceeding an isolated application request limit publishes a throttling fault event as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And the Traffic Manager throttleData publisher and Gateway throttleData consumer are ready within 180 seconds
    When I create an application throttling policy "${UNIQUE:invocationStatsThrottle}" allowing 1 requests per minute
    Then The response status code should be 201
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "statsThrottleApiId" and deployed it
    And the "apis" resource "statsThrottleApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "statsThrottleApiId"
    Then The lifecycle status of API "statsThrottleApiId" should be "Published"
    When I retrieve the "apis" resource with id "statsThrottleApiId"
    And I extract response field "context" and store it as "statsThrottleContext"
    And I extract response field "name" and store it as "statsThrottleName"
    When I create an application "${UNIQUE:InvocationStatsThrottleApp}" with throttling policy from "appThrottlePolicyName"
    Then The response status code should be 201
    When I put the following JSON payload in context as "statsThrottleKeyPayload"
    """
    {"keyType":"PRODUCTION","grantTypesToBeSupported":["client_credentials","password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "statsThrottleKeyPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "statsThrottleSubscriptionPayload"
    """
    {"applicationId":"{{createdAppId}}","apiId":"{{statsThrottleApiId}}","throttlingPolicy":"Unlimited"}
    """
    And I subscribe to API "statsThrottleApiId" using application "createdAppId" with payload "statsThrottleSubscriptionPayload" as "statsThrottleSubscriptionId"
    Then The response status code should be 201
    When I request an OAuth access token for the current user using password grant with scope "PRODUCTION"
    Then The response status code should be 200

    When I invoke the API at gateway context "{{statsThrottleContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I mark the current end of the server log file "apim_metrics.log"
    And I invoke the API at gateway context "{{statsThrottleContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 429 within 60 seconds
    Then The response status code should be 429
    And The value of error response field "code" should be "900803"
    And The server log file "apim_metrics.log" should gain event "apim:faulty" for API "{{statsThrottleName}}" with these fields within 60 seconds
      | field                  | expected      |
      | eventType              | fault         |
      | apiId                  | {{statsThrottleApiId}} |
      | proxyResponseCode      | 429           |
      | errorCode              | 900803        |
      | errorType              | THROTTLED     |
      | applicationName        | <present>     |
      | properties.apiContext  | {{statsThrottleContext}}/1.0.0 |
      | properties.userName    | <present>     |
      | apiCreatorTenantDomain | <tenantDomain> |

    Examples:
      | actor             | tenantDomain |
      | admin             | carbon.super |
      | admin@tenant1.com | tenant1.com  |
