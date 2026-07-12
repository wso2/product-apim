@cleanup
Feature: Gateway Default Version Routing

  Ports the default-version routing checks from the legacy DefaultVersionAPIServerRestartTestCase (functional
  concern; the legacy "restart" was incidental). A versionless gateway context routes to whichever version is
  marked default: it follows the default from v1 to v2 when a new default version is deployed, and returns 404
  once no version is default. The two versions are pointed at distinct backends ("File 1" from name-checkOne,
  "File 2" from name-checkTwo) so the version that served the request is observable in the response body.
  Run in BOTH the super tenant and tenant1.com to prove default-version routing is tenant-agnostic (the tenant
  API is addressed by its full /t/<tenant> context). Runs in the concurrent IntegrationV2-Gateway block
  (backend started). Teardown via the per-scenario cleanup hook.

  @cap:gateway @feat:rest-invocation @type:regression @dep:publisher @legacy:DefaultVersionAPIServerRestartTestCase @legacy:DefaultVersionAPITestCase
  Scenario Outline: A versionless context routes to the default version and follows default-version changes as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"

    # v1.0.0 → "File 1" backend, created as the default version, then published.
    And I have created an api from "artifacts/payloads/create_default_version_api.json" as "dvV1Id" and deployed it
    When I publish the "apis" resource with id "dvV1Id"
    Then The lifecycle status of API "dvV1Id" should be "Published"
    When I retrieve the "apis" resource with id "dvV1Id"
    And I extract response field "context" and store it as "dvContext"

    # An application subscribed to v1, with an access token.
    When I have set up application with keys, subscribed to API "dvV1Id", and obtained access token for "dvSubV1"
    Then The response status code should be 200

    # G1: the versionless context (no /1.0.0 segment) routes to the default version → the File 1 backend.
    When I invoke the API at gateway context "{{dvContext}}/name" with method "GET" using access token "generatedAccessToken" and payload "" until response body contains "File 1" within 60 seconds
    Then The response status code should be 200
    And The response should contain "File 1"

    # Create v2.0.0 as the new default version, repoint it at the "File 2" backend, then deploy + publish it.
    When I create a new version "2.0.0" of "apis" resource "dvV1Id" with default version "true" as "dvV2Id"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "dvV2Id"
    And I put the response payload in context as "dvV2Payload"
    When I put the following JSON payload in context as "dvV2Endpoint"
    """
    {"endpoint_type":"http","production_endpoints":{"url":"http://nodebackend:3015/"},"sandbox_endpoints":{"url":"http://nodebackend:3015/"}}
    """
    When I update the "apis" resource "dvV2Id" and "dvV2Payload" with configuration type "endpointConfig" and value:
    """
    dvV2Endpoint
    """
    Then The response status code should be 200
    When I deploy the API with id "dvV2Id"
    Then The response status code should be 201
    When I publish the "apis" resource with id "dvV2Id"
    Then The lifecycle status of API "dvV2Id" should be "Published"
    # No new subscription needed: creating v2 via "create new version" copies v1's subscription onto v2, so the
    # app is already subscribed (an explicit re-subscribe returns 409). This matches the legacy test, which also
    # subscribed only to v1 and relied on that single subscription for the versionless-routed-to-v2 invocation.

    # G2: the SAME versionless context now routes to the new default (v2) → the File 2 backend.
    When I invoke the API at gateway context "{{dvContext}}/name" with method "GET" using access token "generatedAccessToken" and payload "" until response body contains "File 2" within 60 seconds
    Then The response status code should be 200
    And The response should contain "File 2"

    # Clear the default entirely (no version is default) and redeploy so the gateway drops the default route.
    When I retrieve the "apis" resource with id "dvV2Id"
    And I put the response payload in context as "dvV2Payload2"
    When I update the "apis" resource "dvV2Id" and "dvV2Payload2" with configuration type "isDefaultVersion" and value:
    """
    false
    """
    Then The response status code should be 200
    When I deploy the API with id "dvV2Id"
    Then The response status code should be 201

    # G3: with no default version, the versionless context is not routable → 404.
    When I invoke the API at gateway context "{{dvContext}}/name" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 404 within 60 seconds
    Then The response status code should be 404

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
