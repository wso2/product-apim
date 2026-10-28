Feature: Platform Health Check

  Confirms GET /api/health-check/v1.0/health - the carbon-health-check framework's REST endpoint - is present
  in the default product pack, and that the TM health check probe (TMHealthChecker) reports a real reading for
  the Traffic Manager's throttle event receiver (9611/9711) and message broker (5672) connections once the
  framework is switched on (it ships disabled; this block's healthCheck TOML overlay enables it). This is smoke
  coverage that both org.wso2.carbon.healthcheck.server.feature and org.wso2.carbon.apimgt.tm.healthcheck.feature
  are installed and wired into the pack by default - if either were missing, this endpoint would 404, or the
  tm.* keys would be absent from its response. Deep fault-injection coverage (broker down, TLS failures, auth
  failures, degraded states) is intentionally out of scope here; see specs/001-tm-health-check/manual-test-plan.md
  for that.

  @cap:platform @feat:health-check @type:smoke
  Scenario: The health check endpoint reports the Traffic Manager as healthy by default
    Given The system is ready
    When I call the health check endpoint until key "tm.health.status" becomes "UP" within 120 seconds
    Then The response status code should be 200
    And the health check response key "tm.health.status" should have value "UP"
    And the health check response key "tm.throttle.receiver.status" should have value "UP"
    And the health check response key "tm.message.broker.status" should have value "UP"

  @cap:platform @feat:health-check @type:smoke
  Scenario: The health check response reports detailed throttle-receiver and message-broker probe results
    Given The system is ready
    When I call the health check endpoint until key "tm.health.status" becomes "UP" within 120 seconds
    Then The response status code should be 200
    And the health check response key "tm.throttle.receiver.endpoint.9611.status" should have value "UP"
    And the health check response key "tm.throttle.receiver.endpoint.9611.auth.status" should have value "UP"
    And the health check response key "tm.message.broker.endpoint.5672.status" should have value "UP"
    And the health check response should contain no key matching "error.code"
    And the health check response should contain no key matching "error.message"
