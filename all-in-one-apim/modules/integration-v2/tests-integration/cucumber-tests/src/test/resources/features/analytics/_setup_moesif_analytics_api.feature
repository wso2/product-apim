@setup
Feature: Setup Moesif analytics reflecting API

  Builds the fixture every Moesif analytics scenario invokes through: in EACH tenant, one published, deployed API
  whose operations all target a node-customer-service reflect route, plus one application subscribed to it on the
  Unlimited policy and an access token for it. The tenant dimension is built here rather than per scenario so the
  scenarios that are genuinely tenant-sensitive can run in both without paying for a second fixture, while the
  rest use the super-tenant one.

  WHY ONE SHARED API PER TENANT RATHER THAN ONE PER SCENARIO. Turning analytics on is container-global, so the
  mock collector receives an event for every invocation in this block whatever API produced it. A per-scenario API
  would therefore not scope the event assertions anyway: the published event identifies the API and resource
  template, not the individual call. Scoping is done instead by a unique trace header per scenario, which the
  block publishes because it runs with header capture on. One API per tenant is consequently both cheaper and no
  less isolated.

  WHY THE BACKEND REFLECTS. Every body assertion has two halves: what Moesif was told, and what the backend
  actually received. Capture forces the gateway to build the message, which re-serializes it on the way out, so
  "the analytics copy was dropped" has to be distinguishable from "the proxied body was damaged". A reflecting
  backend makes the second half readable from the gateway response itself:

    * /reflect-body-typed     echoes the body back with the SAME Content-Type, so a JSON request produces a JSON
                              response and both directions of one event carry a structured body.
    * /reflect-body-sensitive echoes typed AND sets Set-Cookie plus X-Account-Number, the response headers the
                              credential strip and the configured mask have to act on.
    * /reflect-body           echoes the raw body for any content type, answering as text/html.
    * /reflect-headers        echoes the request headers the backend received, which is how "the real value still
                              reached the backend" is asserted for a masked header.

  Unlimited is the deliberate subscription policy, stated rather than defaulted: several scenarios invoke a fixed
  number of times and assert an exact event count, so a throttle tripping mid-scenario would present as a lost
  event.

  Asserts nothing about product behaviour; the status checks are fail-fast gates so a fixture failure surfaces
  here rather than as a "No value found in context" cascade later. Resources are registered for the runner's
  AfterClass sweep, NOT per-scenario cleanup, because the scenarios consume this fixture.

  Scenario Outline: Publish the reflecting API, subscribe an application and mint an access token as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I have created an api from "artifacts/payloads/create_apim_moesif_analytics_api.json" as "moesifApiId<suffix>" and deployed it
    And I retrieve the "apis" resource with id "moesifApiId<suffix>"
    Then The response status code should be 200
    And I extract response field "context" and store it as "moesifApiContext<suffix>"
    When I publish the "apis" resource with id "moesifApiId<suffix>"
    Then The lifecycle status of API "moesifApiId<suffix>" should be "Published"
    When I have set up application with keys, subscribed to API "moesifApiId<suffix>" with plan "Unlimited", and obtained access token for "moesifSubId<suffix>"
    Then The response status code should be 200
    And I put value "generatedAccessToken" in context as "moesifAccessToken<suffix>"

    Examples:
      | actor             | suffix       |
      | admin             |              |
      | admin@tenant1.com | @tenant1.com |
