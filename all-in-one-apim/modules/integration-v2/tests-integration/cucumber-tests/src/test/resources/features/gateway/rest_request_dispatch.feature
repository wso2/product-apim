@cleanup
Feature: Gateway REST Request Dispatch

  Gateway-plane dispatch of REST requests and relay of backend responses, in both the super tenant and
  tenant1.com: reserved characters, comma-separated values and full URLs are expanded into templated endpoints
  as configured, overlapping resource paths are dispatched by verb and authorized per method scope, special
  characters in resource paths reach the backend verbatim, load-balanced endpoint groups round-robin across their
  members, duplicate and non-standard backend headers and status lines are relayed as the gateway contract
  requires, and a mis-cased tenant domain in the path is rejected. Teardown via the per-scenario cleanup hook.

  # Reserved-character encoding during uri-template endpoint expansion. Ports the path-param cases of
  # UriTemplateReservedCharacterEncodingTest. The resource uses RFC-6570 reserved expansion (target "/{+val}"),
  # which the v4 publisher API accepts and which lets the gateway MATCH a path segment carrying a reserved char
  # (":"). The BACKEND ENDPOINT template then controls encoding: a plain {uri.var.val} percent-encodes the ":" to
  # %3A on expansion, while a reserved-expansion {+uri.var.val} preserves it. The reflect backend echoes the
  # expanded outbound path (verified live). Sent RAW (the ":" verbatim) since a normalizing client would alter it.
  @cap:gateway @feat:rest-invocation @rule:reserved-char-encoding @type:regression @dep:publisher @legacy:UriTemplateReservedCharacterEncodingTest
  Scenario Outline: A reserved character in a templated endpoint is <encoding> by the <mode> expansion as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "<payload>" as "rcApiId" and deployed it
    When I publish the "apis" resource with id "rcApiId"
    Then The lifecycle status of API "rcApiId" should be "Published"
    When I retrieve the "apis" resource with id "rcApiId"
    And I extract response field "context" and store it as "rcCtx"
    When I have set up application with keys, subscribed to API "rcApiId", and obtained access token for "rcSub"
    Then The response status code should be 200
    # Invoke a path segment carrying the reserved ':' — the endpoint expansion is echoed by the backend.
    When I invoke the API at raw gateway context "{{rcCtx}}/1.0.0/AAA:BBB" using access token "generatedAccessToken" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "<expected>"

    Examples:
      | mode    | encoding         | payload                                                    | expected     | actor             |
      | default | percent-encoded  | artifacts/payloads/create_apim_reservedchar_default_api.json | subAAA%3ABBB | admin             |
      | default | percent-encoded  | artifacts/payloads/create_apim_reservedchar_default_api.json | subAAA%3ABBB | admin@tenant1.com |
      | escape  | preserved        | artifacts/payloads/create_apim_reservedchar_escape_api.json  | subAAA:BBB   | admin             |
      | escape  | preserved        | artifacts/payloads/create_apim_reservedchar_escape_api.json  | subAAA:BBB   | admin@tenant1.com |

  # Comma-separated values route to the backend, as a PATH segment AND as a QUERY parameter. Ports
  # URLMappingRESTTestCase's comma case, which the legacy COMMENTED OUT "until synapse is upgraded". The fix is a
  # reserved-expansion resource template — /courses/{+courseInfo} for the path, /qenc?queryParam={+value} for the
  # query — which the v4 publisher API accepts and which lets the gateway MATCH a comma-bearing value (a plain
  # template returns "No matching resource", verified live). The {+} endpoint preserves the commas to the backend.
  # Sent RAW (commas verbatim).
  @cap:gateway @feat:rest-invocation @rule:comma-params @type:regression @dep:publisher @legacy:URLMappingRESTTestCase
  Scenario Outline: A comma-separated value is routed to the backend via a <location> as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "<payload>" as "commaApiId" and deployed it
    When I publish the "apis" resource with id "commaApiId"
    Then The lifecycle status of API "commaApiId" should be "Published"
    When I retrieve the "apis" resource with id "commaApiId"
    And I extract response field "context" and store it as "commaCtx"
    When I have set up application with keys, subscribed to API "commaApiId", and obtained access token for "commaSub"
    Then The response status code should be 200
    When I invoke the API at raw gateway context "{{commaCtx}}/1.0.0/<invokePath>" using access token "generatedAccessToken" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "<expected>"

    Examples:
      | location        | payload                                                | invokePath                          | expected                       | actor             |
      | path segment    | artifacts/payloads/create_apim_comma_params_api.json   | courses/course1,CE001,sem1          | course1,CE001,sem1             | admin             |
      | path segment    | artifacts/payloads/create_apim_comma_params_api.json   | courses/course1,CE001,sem1          | course1,CE001,sem1             | admin@tenant1.com |
      | query parameter | artifacts/payloads/create_apim_query_escape_api.json   | qenc?queryParam=course1,CE001,sem1  | queryParam=course1,CE001,sem1  | admin             |
      | query parameter | artifacts/payloads/create_apim_query_escape_api.json   | qenc?queryParam=course1,CE001,sem1  | queryParam=course1,CE001,sem1  | admin@tenant1.com |

  # Reserved-character encoding for a QUERY parameter (companion to the path-param scenario above). The resource
  # template binds the query param via reserved expansion (target "/qenc?queryParam={+value}", accepted by the v4
  # publisher API); the BACKEND ENDPOINT template then controls encoding of the bound value: a plain
  # {uri.var.value} percent-encodes ':' to %3A, while {+uri.var.value} preserves it. Ports the query-param cases of
  # UriTemplateReservedCharacterEncodingTest. The 'sub?queryParam=' prefix isolates the endpoint-expanded value from
  # the raw postfix the gateway also appends.
  @cap:gateway @feat:rest-invocation @rule:reserved-char-encoding @type:regression @dep:publisher @legacy:UriTemplateReservedCharacterEncodingTest
  Scenario Outline: A reserved character in a query parameter is <encoding> by the <mode> expansion as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "<payload>" as "qcApiId" and deployed it
    When I publish the "apis" resource with id "qcApiId"
    Then The lifecycle status of API "qcApiId" should be "Published"
    When I retrieve the "apis" resource with id "qcApiId"
    And I extract response field "context" and store it as "qcCtx"
    When I have set up application with keys, subscribed to API "qcApiId", and obtained access token for "qcSub"
    Then The response status code should be 200
    When I invoke the API at raw gateway context "{{qcCtx}}/1.0.0/qenc?queryParam=<queryValue>" using access token "generatedAccessToken" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "<expected>"

    # The queryValue column is threaded verbatim into the raw invoke so each row can send a different
    # reserved/special character through the same endpoint-expansion machinery. The existing colon rows keep
    # their exact expected values (APIM:WSO2); the added rows cover the plus / encoded-space / trailing-percent
    # cases from UriTemplateReservedCharacterEncodingTest (legacy's query-param decoding tests). The added-row
    # expected values are what THIS build produced (reflect backend echoes the expanded outbound query verbatim).
    Examples:
      | mode    | encoding        | payload                                                   | queryValue      | expected                     | actor             |
      | default | percent-encoded | artifacts/payloads/create_apim_query_default_api.json     | APIM:WSO2       | sub?queryParam=APIM%3AWSO2   | admin             |
      | default | percent-encoded | artifacts/payloads/create_apim_query_default_api.json     | APIM:WSO2       | sub?queryParam=APIM%3AWSO2   | admin@tenant1.com |
      | escape  | preserved       | artifacts/payloads/create_apim_query_escape_api.json      | APIM:WSO2       | sub?queryParam=APIM:WSO2     | admin             |
      | escape  | preserved       | artifacts/payloads/create_apim_query_escape_api.json      | APIM:WSO2       | sub?queryParam=APIM:WSO2     | admin@tenant1.com |
      # Plus character: default expansion percent-encodes the literal '+' to %2B (legacy assertTrue APIM%2BWSO2).
      | default | percent-encoded | artifacts/payloads/create_apim_query_default_api.json     | APIM+WSO2       | sub?queryParam=APIM%2BWSO2   | admin             |
      # Encoded space %20: preserved through the default expansion (legacy assertTrue APIM%20WSO2).
      | default | percent-encoded | artifacts/payloads/create_apim_query_default_api.json     | APIM%20WSO2     | sub?queryParam=APIM%20WSO2   | admin             |
      # Trailing/multi-escaped percent (aaa!@!% encoded): re-encoded intact (legacy assertTrue aaa%21%40%21%25).
      | default | percent-encoded | artifacts/payloads/create_apim_query_default_api.json     | aaa%21%40%21%25 | sub?queryParam=aaa%21%40%21%25 | admin           |
      # Plus character under escape (reserved) expansion: the '+' is preserved, NOT percent-encoded (legacy assertFalse APIM%2BWSO2).
      | escape  | preserved       | artifacts/payloads/create_apim_query_escape_api.json      | APIM+WSO2       | sub?queryParam=APIM+WSO2     | admin             |

  # Ports UriTemplateReservedCharacterEncodingTest#testURITemplateSpecialCaseVariableWithFullURL — a path
  # parameter whose VALUE is itself a full URL (protocol://host:port/path, carrying ':' and '//') is captured by a
  # reserved-expansion resource (/special_case/{+val}) and preserved through the {+uri.var.val} endpoint expansion.
  # The reflect backend (/echo/*) echoes the expanded outbound path verbatim, so the exact expected value below is
  # what THIS build produced (unknown ahead of the run — legacy asserted only the outbound gateway "To:" URL, which
  # v2 has no equivalent log for). Sent RAW so the ':' and '//' reach the gateway verbatim. Single super-tenant row.
  @cap:gateway @feat:rest-invocation @rule:full-url-path-param @type:regression @dep:publisher @legacy:UriTemplateReservedCharacterEncodingTest
  Scenario Outline: A full URL passed as a path parameter is preserved through the templated endpoint as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_fullurl_pathparam_api.json" as "fuApiId" and deployed it
    When I publish the "apis" resource with id "fuApiId"
    Then The lifecycle status of API "fuApiId" should be "Published"
    When I retrieve the "apis" resource with id "fuApiId"
    And I extract response field "context" and store it as "fuCtx"
    When I have set up application with keys, subscribed to API "fuApiId", and obtained access token for "fuSub"
    Then The response status code should be 200
    When I invoke the API at raw gateway context "{{fuCtx}}/1.0.0/special_case/http://nodebackend:9999/services/test_2" using access token "generatedAccessToken" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "echo/subhttp://nodebackend:9999/services/test_2"

    Examples:
      | actor |
      | admin |

  # Ports APIInvocationWithSimilarResourcesAndDifferentVerbsTestCase — an API with TWO overlapping resource paths
  # distinguished only by HTTP verb (GET /comp/cartes/* and POST /comp/cartes/op/*) routes each verb to the correct
  # operation. A path that matches BOTH templates (/comp/cartes/op/123) resolves by verb: GET → the GET operation,
  # POST → the POST operation, each returning 200 from the wildcard backend. (Legacy asserted verb-specific echo
  # bodies against a mock backend; the v2 wildcard backend does not echo the verb/path, so the subject — correct
  # verb-based routing of overlapping paths — is asserted via the 200 on each verb.)
  @cap:gateway @feat:rest-invocation @rule:verb-routing @type:regression @dep:publisher @legacy:APIInvocationWithSimilarResourcesAndDifferentVerbsTestCase
  Scenario Outline: Overlapping resource paths are routed by HTTP verb as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_similar_resources_api.json" as "svApiId" and deployed it
    When I publish the "apis" resource with id "svApiId"
    Then The lifecycle status of API "svApiId" should be "Published"
    When I retrieve the "apis" resource with id "svApiId"
    And I extract response field "context" and store it as "svContext"
    When I have set up application with keys, subscribed to API "svApiId", and obtained access token for "svSub"
    Then The response status code should be 200

    # GET on the shared path resolves to the GET operation.
    When I invoke the API at gateway context "{{svContext}}/1.0.0/comp/cartes/op/123" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "Hello World"
    # POST on the same path resolves to the POST operation.
    When I invoke the API at gateway context "{{svContext}}/1.0.0/comp/cartes/op/123" with method "POST" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "Hello World"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # The same verb dispatch of overlapping resources, against a backend that echoes the method and path it received
  # (/verb-echo), so each invocation proves the gateway forwarded the ORIGINAL verb and the full request path. Two
  # dispatch variants: targets ending in "/*" are url-mapping resources, while the legacy targets with the wildcard
  # stuck onto the segment (/comp/cartes*, /comp/cartes/op*) are uri-template resources (velocity_template.xml).
  # The publisher readback pins each target verbatim, so a target normalised into the other form fails the row.
  # The echoed path carries no /t/<tenant> prefix: the backend path is tenant-agnostic.
  @cap:gateway @feat:rest-invocation @rule:verb-routing @type:regression @dep:publisher @legacy:APIInvocationWithSimilarResourcesAndDifferentVerbsTestCase
  Scenario Outline: Overlapping <dispatch> resource paths forward the original verb and path to the backend as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "<payload>" as "sveApiId" and deployed it
    When I publish the "apis" resource with id "sveApiId"
    Then The lifecycle status of API "sveApiId" should be "Published"
    When I retrieve the "apis" resource with id "sveApiId"
    Then The response status code should be 200
    And The response array field "operations" should have exactly 2 entries
    And The response field "operations[?(@.verb=='GET')].target" should be exactly the list "<getTarget>"
    And The response field "operations[?(@.verb=='POST')].target" should be exactly the list "<postTarget>"
    And I extract response field "context" and store it as "sveContext"
    When I have set up application with keys, subscribed to API "sveApiId", and obtained access token for "sveSub"
    Then The response status code should be 200

    When I invoke the API at gateway context "{{sveContext}}/1.0.0/comp/cartes/op/123" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "method" should be "GET"
    And The value of response field "path" should be "/jaxrs_basic/services/customers/customerservice/verb-echo/comp/cartes/op/123"
    When I invoke the API at gateway context "{{sveContext}}/1.0.0/comp/cartes/op/123" with method "POST" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "method" should be "POST"
    And The value of response field "path" should be "/jaxrs_basic/services/customers/customerservice/verb-echo/comp/cartes/op/123"

    Examples:
      | dispatch     | payload                                                              | getTarget      | postTarget        | actor             |
      | url-mapping  | artifacts/payloads/create_apim_similar_resources_verb_echo_api.json  | /comp/cartes/* | /comp/cartes/op/* | admin             |
      | url-mapping  | artifacts/payloads/create_apim_similar_resources_verb_echo_api.json  | /comp/cartes/* | /comp/cartes/op/* | admin@tenant1.com |
      | uri-template | artifacts/payloads/create_apim_similar_resources_uritemplate_api.json | /comp/cartes*  | /comp/cartes/op*  | admin             |
      | uri-template | artifacts/payloads/create_apim_similar_resources_uritemplate_api.json | /comp/cartes*  | /comp/cartes/op*  | admin@tenant1.com |

  # An operation-specific scope is the authorization discriminator for the overlapping path: GET and POST address
  # /comp/cartes/op/123, but each method must resolve to its own operation and accept only that operation's scope.
  # The URL-mapping and URI-template dispatch forms are both exercised, as are super-tenant and tenant admins. Each
  # denial is a one-shot exact 403 after the gateway-live gate; polling could hide an initial cross-scope success.
  @cap:gateway @feat:security-enforcement @rule:verb-routing @type:regression @dep:publisher @legacy:APIInvocationWithSimilarResourcesAndDifferentVerbsTestCase
  Scenario Outline: Overlapping <dispatch> operations authorize only their matching method scope as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "<payload>" as "sveScopeApiId" and deployed it
    And the "apis" resource "sveScopeApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "sveScopeApiId"
    Then The lifecycle status of API "sveScopeApiId" should be "Published"
    When I retrieve the "apis" resource with id "sveScopeApiId"
    Then The response status code should be 200
    And The response array field "operations" should have exactly 2 entries
    And The response field "operations[?(@.verb=='GET')].target" should be exactly the list "<getTarget>"
    And The response field "operations[?(@.verb=='POST')].target" should be exactly the list "<postTarget>"
    And The response field "operations[?(@.verb=='GET')].scopes[*]" should be exactly the list "sve_get_scope"
    And The response field "operations[?(@.verb=='POST')].scopes[*]" should be exactly the list "sve_post_scope"
    And I extract response field "context" and store it as "sveScopeContext"

    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "sveScopeAppPayload"
    And I create an application with payload "sveScopeAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "sveScopeKeysPayload"
    """
    {"keyType":"PRODUCTION","grantTypesToBeSupported":["client_credentials","password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "sveScopeKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "sveScopeSubPayload"
    """
    {"applicationId":"{{applicationId}}","apiId":"{{apiId}}","throttlingPolicy":"Unlimited"}
    """
    And I subscribe to API "sveScopeApiId" using application "createdAppId" with payload "sveScopeSubPayload" as "sveScopeSubId"
    Then The response status code should be 201

    # A token with only the GET scope reaches GET and is denied on POST at the same overlapping path.
    When I request an OAuth access token for the current user using password grant with scope "sve_get_scope"
    Then The response status code should be 200
    And The issued token scope list should include "sve_get_scope" and exclude "sve_post_scope"
    And I copy context value "generatedAccessToken" to "sveGetScopeToken"
    When I invoke the API at gateway context "{{sveScopeContext}}/1.0.0/comp/cartes/op/123" once with method "GET" using access token "sveGetScopeToken" and payload ""
    Then The response status code should be 200
    And The value of response field "method" should be "GET"
    And The value of response field "path" should be "/jaxrs_basic/services/customers/customerservice/verb-echo/comp/cartes/op/123"
    When I invoke the API at gateway context "{{sveScopeContext}}/1.0.0/comp/cartes/op/123" once with method "POST" using access token "sveGetScopeToken" and payload ""
    Then The response status code should be 403

    # Conversely, the POST-only scope reaches POST and is denied on GET at that same path.
    When I request an OAuth access token for the current user using password grant with scope "sve_post_scope"
    Then The response status code should be 200
    And The issued token scope list should include "sve_post_scope" and exclude "sve_get_scope"
    And I copy context value "generatedAccessToken" to "svePostScopeToken"
    When I invoke the API at gateway context "{{sveScopeContext}}/1.0.0/comp/cartes/op/123" once with method "POST" using access token "svePostScopeToken" and payload ""
    Then The response status code should be 200
    And The value of response field "method" should be "POST"
    And The value of response field "path" should be "/jaxrs_basic/services/customers/customerservice/verb-echo/comp/cartes/op/123"
    When I invoke the API at gateway context "{{sveScopeContext}}/1.0.0/comp/cartes/op/123" once with method "GET" using access token "svePostScopeToken" and payload ""
    Then The response status code should be 403

    Examples:
      | dispatch     | payload                                                                    | getTarget      | postTarget        | actor             |
      | url-mapping  | artifacts/payloads/create_apim_similar_resources_scoped_verb_echo_api.json  | /comp/cartes/* | /comp/cartes/op/* | admin             |
      | url-mapping  | artifacts/payloads/create_apim_similar_resources_scoped_verb_echo_api.json  | /comp/cartes/* | /comp/cartes/op/* | admin@tenant1.com |
      | uri-template | artifacts/payloads/create_apim_similar_resources_scoped_uritemplate_api.json | /comp/cartes*  | /comp/cartes/op*  | admin             |
      | uri-template | artifacts/payloads/create_apim_similar_resources_scoped_uritemplate_api.json | /comp/cartes*  | /comp/cartes/op*  | admin@tenant1.com |

  # Ports APIResourceWithSpecialCharactersInvocation — a resource whose path NAME contains non-reserved special
  # characters (comma, hyphen, period, underscore, tilde per RFC 3986 §2.2: /special,-._~resource) is routed to the
  # backend and invoked successfully (200). The comma is the crux: it must NOT be treated as a delimiter (GraphQL
  # operations use a comma delimiter, but a plain REST resource name containing a comma must be used verbatim).
  @cap:gateway @feat:rest-invocation @rule:special-char-resource @type:regression @dep:publisher @legacy:APIResourceWithSpecialCharactersInvocation
  Scenario Outline: A resource path with non-reserved special characters is invocable as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_special_char_resource_api.json" as "scApiId" and deployed it
    When I publish the "apis" resource with id "scApiId"
    Then The lifecycle status of API "scApiId" should be "Published"
    When I retrieve the "apis" resource with id "scApiId"
    And I extract response field "context" and store it as "scContext"
    When I have set up application with keys, subscribed to API "scApiId", and obtained access token for "scSub"
    Then The response status code should be 200

    When I invoke the API at gateway context "{{scContext}}/1.0.0/special,-._~resource" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "Hello World"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # The same special-character resource against the path echo backend (/echo), which returns the raw path it
  # received: the non-reserved characters ,-._~ reach the backend verbatim, not percent-encoded or split.
  @cap:gateway @feat:rest-invocation @rule:special-char-resource @type:regression @dep:publisher @legacy:APIResourceWithSpecialCharactersInvocation
  Scenario Outline: A resource path with non-reserved special characters reaches the backend verbatim as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_special_char_resource_echo_api.json" as "sceApiId" and deployed it
    When I publish the "apis" resource with id "sceApiId"
    Then The lifecycle status of API "sceApiId" should be "Published"
    When I retrieve the "apis" resource with id "sceApiId"
    And I extract response field "context" and store it as "sceContext"
    When I have set up application with keys, subscribed to API "sceApiId", and obtained access token for "sceSub"
    Then The response status code should be 200

    When I invoke the API at gateway context "{{sceContext}}/1.0.0/special,-._~resource" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "echo/special,-._~resource"
    And The value of response field "received" should be "/jaxrs_basic/services/customers/customerservice/echo/special,-._~resource"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports LoadBalancedEndPointTestCase (commented-out in the legacy suite) — an API with a load-balanced production
  # endpoint (endpoint_type "load_balance", RoundRobin) distributes requests across THREE distinguishable backends
  # (File 1 / File 2 / File 3). Uses name-checkOne/Two/Three (ports 3014/3015/3016).
  #
  # Two assertions of increasing strength. First, reachability: each "until response body contains File N" poll
  # drives the round-robin until that backend answers, so all three are shown to serve traffic. Second — and this is
  # what closes the legacy's ORDER assertion — nine consecutive counted calls must round-robin, i.e. EVERY window of
  # three consecutive responses hits three DISTINCT members. That is strictly stronger than reachability (which a
  # degenerate 7:1:1 split would satisfy) while remaining start-offset independent, so it does not inherit the
  # flakiness of the legacy's fixed 1,2,3,1,2,3 sequence: the preceding polls have already moved the RR cursor to an
  # unknown position, and the sliding-window property holds for every rotation. The API is unique to this scenario,
  # so no sibling scenario shares its RR cursor.
  @cap:gateway @feat:rest-invocation @rule:load-balance @type:regression @dep:publisher @legacy:LoadBalancedEndPointTestCase
  Scenario Outline: A load-balanced endpoint distributes requests across all backends as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_loadbalanced_api.json" as "lbApiId" and deployed it
    When I publish the "apis" resource with id "lbApiId"
    Then The lifecycle status of API "lbApiId" should be "Published"
    When I retrieve the "apis" resource with id "lbApiId"
    And I extract response field "context" and store it as "lbContext"
    When I have set up application with keys, subscribed to API "lbApiId", and obtained access token for "lbSub"
    Then The response status code should be 200

    # Repeated invocations round-robin across the three backends — each distinct body is reached within a few calls.
    When I invoke the API at gateway context "{{lbContext}}/1.0.0/name" with method "GET" using access token "generatedAccessToken" and payload "" until response body contains "File 1" within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{lbContext}}/1.0.0/name" with method "GET" using access token "generatedAccessToken" and payload "" until response body contains "File 2" within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{lbContext}}/1.0.0/name" with method "GET" using access token "generatedAccessToken" and payload "" until response body contains "File 3" within 60 seconds
    Then The response status code should be 200

    # ORDER: over nine consecutive calls every window of three hits three DISTINCT members — real round-robin, not
    # merely "all three are reachable".
    When I invoke the API at gateway context "{{lbContext}}/1.0.0/name" with method "GET" using access token "generatedAccessToken" 9 times round-robin across backend markers "File 1,File 2,File 3"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # The SANDBOX axis of LoadBalancedEndPointTestCase#testRoundRobinAlgorithmInProductionAndSandboxEndpoints, which
  # the corpus never touched — the three `_SB` load-balance members (ports 3009/3011/3013) existed but no feature
  # referenced them, so nothing proved a load-balanced SANDBOX endpoint group is round-robinned at all. The API
  # carries BOTH groups with per-member AND per-key-type distinguishable bodies (production answers "File N",
  # sandbox "File N_Sandbox"), so a SANDBOX-key token asserted against the "_Sandbox" markers proves two things at
  # once: the key type selected the sandbox group, and that group is round-robinned across all three of its members.
  # The same nine-call sliding-window property as the production axis pins the ORDER.
  @cap:gateway @feat:rest-invocation @rule:load-balance @type:regression @dep:publisher @legacy:LoadBalancedEndPointTestCase
  Scenario Outline: A load-balanced SANDBOX endpoint group round-robins across all sandbox members as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_loadbalanced_sandbox_api.json" as "lbsApiId" and deployed it
    When I publish the "apis" resource with id "lbsApiId"
    Then The lifecycle status of API "lbsApiId" should be "Published"
    When I retrieve the "apis" resource with id "lbsApiId"
    And I extract response field "context" and store it as "lbsContext"

    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "lbsApp"
    And I create an application with payload "lbsApp"
    Then The response status code should be 201
    When I put the following JSON payload in context as "lbsSub"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "lbsApiId" using application "createdAppId" with payload "lbsSub" as "lbsSubId"
    Then The response status code should be 201

    # A SANDBOX key + token — the key type is what routes to the sandbox endpoint group.
    When I put the following JSON payload in context as "lbsSandboxKeys"
    """
    {"keyType": "SANDBOX", "grantTypesToBeSupported": ["client_credentials"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "lbsSandboxKeys"
    Then The response status code should be 200
    When I put the following JSON payload in context as "lbsSandboxToken"
    """
    {"consumerSecret": "{{appConsumerSecret}}", "validityPeriod": 3600}
    """
    And I request an access token for application id "createdAppId" using payload "lbsSandboxToken"
    Then The response status code should be 200

    # Each of the three SANDBOX members is reached (and the "_Sandbox" suffix proves it is the sandbox group, not
    # the production one, that answered).
    When I invoke the API at gateway context "{{lbsContext}}/1.0.0/name" with method "GET" using access token "generatedAccessToken" and payload "" until response body contains "File 1_Sandbox" within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{lbsContext}}/1.0.0/name" with method "GET" using access token "generatedAccessToken" and payload "" until response body contains "File 2_Sandbox" within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{lbsContext}}/1.0.0/name" with method "GET" using access token "generatedAccessToken" and payload "" until response body contains "File 3_Sandbox" within 60 seconds
    Then The response status code should be 200

    # ORDER across the sandbox group: nine consecutive calls, every window of three distinct.
    When I invoke the API at gateway context "{{lbsContext}}/1.0.0/name" with method "GET" using access token "generatedAccessToken" 9 times round-robin across backend markers "File 1_Sandbox,File 2_Sandbox,File 3_Sandbox"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Additional duplicate-header regression: the gateway must NOT collapse duplicate response headers coming from
  # the backend. The node duplicate-header-backend (port 3005, GET /duplicate) emits TWO Set-Cookie headers
  # ("12wesdsfdffdsfff" and "3456wesfdsfdsfdf"); both must reach the client, since collapsing them silently
  # breaks backend session handling. This assertion cannot be expressed by the generic header steps: the shared
  # HttpResponse.getHeaders() is a Map<String,String>, which cannot hold two values for one key, so a raw
  # java.net.http.HttpClient is used (see APIInvocationSteps#invokeAndAssertMultiValuedHeader) to read
  # allValues("Set-Cookie") and assert the EXACT count AND both values — a count-only check would pass if the
  # gateway duplicated one value twice, a values-only check if it emitted three.
  # The legacy test method remains in source but its TestNG registration is commented out. V2 restores the
  # intended duplicate-header behavior with a raw HTTP client, without depending on that dormant registration.
  @cap:gateway @feat:header-transformation @rule:duplicate-headers @type:regression @dep:publisher
  Scenario Outline: The gateway preserves duplicate Set-Cookie response headers from the backend as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_duplicate_header_api.json" as "dupApiId" and deployed it
    And the "apis" resource "dupApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "dupApiId"
    Then The lifecycle status of API "dupApiId" should be "Published"
    When I retrieve the "apis" resource with id "dupApiId"
    And I extract response field "context" and store it as "dupContext"
    When I have set up application with keys, subscribed to API "dupApiId", and obtained access token for "dupSubId"
    Then The response status code should be 200

    # Both Set-Cookie headers the backend emits must survive to the client — exact count AND exact values.
    When I invoke the API at gateway context "{{dupContext}}/1.0.0/" with method "GET" using access token "generatedAccessToken" and assert the response carries exactly 2 "Set-Cookie" headers with values "12wesdsfdffdsfff, 3456wesfdsfdsfdf" within 60 seconds

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Exact parity for the legacy DuplicateHeaderTestCase: its WAR returns two response headers named "Cookie"
  # (not Set-Cookie), and the test asserts that both survive the APIM gateway. This is a distinct header contract;
  # keep it separate from the Set-Cookie scenario above. The raw backend response avoids Node's header normalization.
  @cap:gateway @feat:header-transformation @rule:duplicate-headers @type:regression @dep:publisher @legacy:DuplicateHeaderTestCase
  Scenario Outline: The gateway preserves duplicate Cookie response headers from the backend as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_duplicate_cookie_header_api.json" as "dupCookieApiId" and deployed it
    And the "apis" resource "dupCookieApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "dupCookieApiId"
    Then The lifecycle status of API "dupCookieApiId" should be "Published"
    When I retrieve the "apis" resource with id "dupCookieApiId"
    And I extract response field "context" and store it as "dupCookieContext"
    When I have set up application with keys, subscribed to API "dupCookieApiId", and obtained access token for "dupCookieSubId"
    Then The response status code should be 200

    # Match legacy's exact count and improve it by checking both original values, not just count=2.
    When I invoke the API at gateway context "{{dupCookieContext}}/1.0.0/" with method "GET" using access token "generatedAccessToken" and assert the response carries exactly 2 "Cookie" headers with values "12wesdsfdffdsfff, 3456wesfdsfdsfdf" within 60 seconds

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports APIMANAGER5326CustomStatusMsgTestCase — the gateway must relay the BACKEND's HTTP status line verbatim,
  # including a NON-STANDARD reason phrase, rather than regenerating the canonical phrase for the status code. The
  # upstream is the raw-socket backend on :3024, which answers every request with the literal status line
  # "HTTP/1.1 400 Custom response".
  #
  # The reason phrase is the whole point: the status code alone (400) is what a gateway that had normalised the
  # phrase to "Bad Request" would also return, so a code-only assertion is blind to exactly the regression this
  # test exists for. The body is asserted too, proving the backend's response reached the client intact rather than
  # being replaced by a gateway-generated 400 fault.
  @cap:gateway @feat:rest-invocation @rule:status-line @type:regression @dep:publisher @legacy:APIMANAGER5326CustomStatusMsgTestCase
  Scenario Outline: The gateway relays a backend's non-standard status reason phrase as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_custom_status_api.json" as "csApiId" and deployed it
    When I publish the "apis" resource with id "csApiId"
    Then The lifecycle status of API "csApiId" should be "Published"
    When I retrieve the "apis" resource with id "csApiId"
    And I extract response field "context" and store it as "csContext"
    When I have set up application with keys, subscribed to API "csApiId", and obtained access token for "csSub"
    Then The response status code should be 200

    When I invoke the API at gateway context "{{csContext}}/1.0.0/custom-status" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 400 within 60 seconds
    Then The response status code should be 400
    # The backend's own reason phrase, NOT the canonical "Bad Request".
    And The response reason phrase should be "Custom response"
    # The backend's body arrived intact — this is not a gateway-generated fault.
    And The response should contain "<?xml version=\"1.0\" encoding=\"UTF-8\"?><test></test>"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports APIMANAGER3614DuplicateTransferEncodingHeaderTestCase (commented out in the legacy suite, so it never
  # actually ran there). Transfer-Encoding is HOP-BY-HOP: unlike Set-Cookie above, a duplicated one must NOT be
  # propagated — two Transfer-Encoding headers on one response is a request-smuggling / response-splitting vector,
  # so the gateway must re-frame the response with exactly ONE. The backend is a raw-socket route on the node
  # duplicate-header-backend (GET /duplicate/transfer-encoding, port 3005) that writes the status line + header
  # block as raw bytes — express normalises hop-by-hop headers, so the duplicate cannot be produced with res.append.
  # Verified on the wire against the container: the backend really does emit two "Transfer-Encoding: chunked".
  # The multi-valued-header step is used for the same reason as the Set-Cookie row (the shared HttpResponse's
  # Map<String,String> headers cannot represent a duplicate), and a second invocation pins the BODY: a gateway that
  # mangled the duplicate-header response into an empty body would otherwise pass a headers-only assertion.
  @cap:gateway @feat:header-transformation @rule:duplicate-headers @type:regression @dep:publisher @legacy:APIMANAGER3614DuplicateTransferEncodingHeaderTestCase
  Scenario Outline: The gateway collapses duplicate Transfer-Encoding response headers from the backend as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_duplicate_transfer_encoding_api.json" as "teApiId" and deployed it
    And the "apis" resource "teApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "teApiId"
    Then The lifecycle status of API "teApiId" should be "Published"
    When I retrieve the "apis" resource with id "teApiId"
    And I extract response field "context" and store it as "teContext"
    When I have set up application with keys, subscribed to API "teApiId", and obtained access token for "teSubId"
    Then The response status code should be 200

    # Exactly ONE Transfer-Encoding header reaches the client, even though the backend emitted two.
    When I invoke the API at gateway context "{{teContext}}/1.0.0/" with method "GET" using access token "generatedAccessToken" and assert the response carries exactly 1 "Transfer-Encoding" headers with values "chunked" within 60 seconds

    # The body survives the re-framing intact.
    When I invoke the API at gateway context "{{teContext}}/1.0.0/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"RestResponse\":\"duplicateTransferEncoding\""

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # A tenant API is addressed at the gateway as /t/<domain>/<context>, and the domain is matched EXACTLY — so a
  # mis-cased variant ("/t/Tenant1.com/") is rejected by the gateway with a 404 and a generic message, so that a
  # probe cannot tell an existing tenant from a non-existent one.
  # The third leg is the actual subject: a failed tenant resolution must not poison tenant routing, so the
  # correctly-cased path must still serve the backend afterwards. Legs 1 and 2 are the control and the trigger.
  # Ports TenantDomainValidationTestCase#testAPIInvokeWithTenants (valid 200, mis-cased 404, valid 200 again).
  # Tenant-only, so a plain Scenario rather than the actor outline used above: a super-tenant API's context
  # carries no /t/ segment at all, so there is nothing to mis-case in the carbon.super row.
  # Subscribed on Unlimited (as the legacy did) so the three back-to-back invocations cannot hit a tier limit.
  @cap:gateway @feat:rest-invocation @rule:tenant-domain @type:regression @dep:publisher @legacy:TenantDomainValidationTestCase
  Scenario: A mis-cased tenant domain in the gateway path is rejected and leaves the tenant's routing intact
    Given The system is ready
    And I have valid access tokens as "admin@tenant1.com"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "tdvApiId" and deployed it
    When I publish the "apis" resource with id "tdvApiId"
    Then The lifecycle status of API "tdvApiId" should be "Published"

    # Capture the tenant API's full gateway context — it already carries the /t/tenant1.com prefix
    When I retrieve the "apis" resource with id "tdvApiId"
    And I extract response field "context" and store it as "tdvContext"
    When I have set up application with keys, subscribed to API "tdvApiId" with plan "Unlimited", and obtained access token for "tdvSub"
    Then The response status code should be 200

    # Leg 1 (control) — the correctly-cased tenant domain routes through to the backend
    When I invoke the API at gateway context "{{tdvContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""

    # Leg 2 (trigger) — the same resource with only the tenant domain's case changed is rejected. The context is
    # copied first so the correctly-cased value survives for leg 3 (the replace step rewrites in place).
    When I copy context value "tdvContext" to "tdvMisCasedContext"
    And I replace "/t/tenant1.com/" with "/t/Tenant1.com/" in context "tdvMisCasedContext"
    And I invoke the API at gateway context "{{tdvMisCasedContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 404 within 60 seconds
    Then The response status code should be 404
    And The response should contain "The requested resource is not available"

    # Leg 3 (the point) — the correctly-cased path still reaches the backend, so tenant routing was not poisoned
    When I invoke the API at gateway context "{{tdvContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""
