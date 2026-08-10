@cleanup
Feature: Gateway WebSocket API — Specific Proxy Profile Precedence over Catch-All

  Verifies that a specific proxy profile takes precedence over a catch-all profile when both match
  the backend hostname. Runs in a container whose deployment.toml carries two profiles
  (wsProxySpecificOverCatchAll overlay):
    Profile 1 — specific: target_hosts: ["nodebackend"], no bypass_hosts, proxy_port: 3128.
                Routes nodebackend through the anonymous Squid proxy.
    Profile 2 — catch-all: target_hosts: ["*"], bypass_hosts: ["nodebackend"], proxy_port: 3128.
                Would bypass the proxy for nodebackend if selected.
  "nodebackend" satisfies both profiles. The CONNECT count is the decisive assertion: count = 1
  means the specific profile was selected and the gateway routed through Squid; count = 0 would
  mean the catch-all's bypass_hosts suppressed the CONNECT, revealing that the catch-all won.
  This test documents the profile-selection semantics: a non-wildcard target_hosts match beats
  a wildcard match regardless of declaration order in the TOML.

  @cap:gateway @feat:streaming-invocation @rule:proxy-routing @type:smoke @dep:publisher @legacy:WebSocketProxyProfileTestCase
  Scenario: specific profile beats catch-all — gateway routes through proxy rather than bypassing
    Given The system is ready
    And I have valid access tokens as "admin"
    And the proxy access logs are cleared
    And I have created an api from "artifacts/payloads/create_apim_ws_echo_api.json" as "wsApiId" and deployed it
    When I publish the "apis" resource with id "wsApiId"
    Then The lifecycle status of API "wsApiId" should be "Published"
    When I retrieve the "apis" resource with id "wsApiId"
    And I extract response field "context" and store it as "wsContext"
    When I have set up application with keys, subscribed to API "wsApiId" with plan "AsyncUnlimited", and obtained access token for "wsSubId"
    Then The response status code should be 200
    # Both profiles match "nodebackend". Profile 1 (specific) has no bypass_hosts — it routes
    # through squid-proxy:3128. Profile 2 (catch-all) has bypass_hosts = ["nodebackend"] — it
    # would connect directly. The expected outcome is that Profile 1 wins: echo succeeds through
    # the proxy and the CONNECT count is 1.
    When I invoke the WebSocket API at gateway ws context "{{wsContext}}/1.0.0" with message "specific-wins" using access token "generatedAccessToken" expecting echo "SPECIFIC-WINS" within 60 seconds
    # count = 1  → specific profile selected, gateway routed through Squid (correct behaviour)
    # count = 0  → catch-all selected, bypass suppressed the CONNECT (profile precedence bug)
    Then the anonymous proxy should have received exactly 1 CONNECT request(s)
