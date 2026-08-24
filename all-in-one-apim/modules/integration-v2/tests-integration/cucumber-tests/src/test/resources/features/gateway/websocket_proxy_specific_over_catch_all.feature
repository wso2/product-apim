@cleanup
Feature: Gateway WebSocket API — Specific Proxy Profile Precedence over Catch-All

  Verifies that a specific proxy profile takes precedence over a catch-all profile when both match
  the backend hostname, and that this holds regardless of declaration order. Runs in a container
  whose deployment.toml carries two profiles (wsProxySpecificOverCatchAll overlay):
    Profile 1 — catch-all: target_hosts: ["*"], bypass_hosts: ["nodebackend"] — declared FIRST.
                A first-match implementation would select this and bypass the proxy (CONNECT = 0).
    Profile 2 — specific: target_hosts: ["nodebackend"], no bypass_hosts — declared SECOND.
                A most-specific-match implementation selects this regardless of order (CONNECT = 1).
  The CONNECT count is the decisive assertion: count = 1 proves the specific profile overrides the
  catch-all even though the catch-all is listed first. A first-match implementation would produce
  count = 0 (catch-all bypass suppresses the CONNECT) and fail the test.

  @cap:gateway @feat:streaming-invocation @rule:proxy-routing @type:smoke @dep:publisher @legacy:WebSocketProxyProfileTestCase
  Scenario Outline: specific profile beats catch-all — gateway routes through proxy rather than bypassing as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And the proxy access logs are cleared
    And I have created an api from "artifacts/payloads/create_apim_ws_echo_api.json" as "wsApiId" and deployed it
    When I publish the "apis" resource with id "wsApiId"
    Then The lifecycle status of API "wsApiId" should be "Published"
    When I retrieve the "apis" resource with id "wsApiId"
    And I extract response field "context" and store it as "wsContext"
    When I have set up application with keys, subscribed to API "wsApiId" with plan "AsyncUnlimited", and obtained access token for "wsSubId"
    Then The response status code should be 200
    # Both profiles match "nodebackend". The catch-all (bypass_hosts = ["nodebackend"]) is listed
    # FIRST; the specific profile (no bypass_hosts, routes through proxy) is listed SECOND.
    # A first-match implementation selects the catch-all → direct connection → CONNECT = 0 → FAIL.
    # A most-specific-match implementation selects the specific profile → proxy → CONNECT = 1 → PASS.
    When I invoke the WebSocket API at gateway ws context "{{wsContext}}/1.0.0" with message "specific-wins" using access token "generatedAccessToken" expecting echo "SPECIFIC-WINS" within 60 seconds
    # count = 1  → specific profile selected regardless of declaration order (correct behaviour)
    # count = 0  → catch-all selected due to first-match (profile precedence bug)
    Then the anonymous proxy should have received exactly 1 CONNECT request(s)
    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
