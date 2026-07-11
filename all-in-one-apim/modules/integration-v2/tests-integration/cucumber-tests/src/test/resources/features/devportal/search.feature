@cleanup
Feature: DevPortal Search & Discovery

  DevPortal-plane discovery: a published API is findable in the DevPortal store by name and by context.
  Runs in both the super tenant and tenant1.com. The publishing actor is the least-privilege publisher;
  search is performed with the consumer (devportal) token of the same actor. Search is backed by an
  asynchronous index, so the search step polls until the result appears (no fixed sleep). Self-contained
  scenario, torn down by the per-scenario cleanup hook.

  @cap:devportal @feat:discovery @rule:search @type:smoke @legacy:DevPortalSearchVisibilityTestCase
  Scenario Outline: Search a newly published API by name and context as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"

    # Capture the uniquely-generated name and context to search for
    When I retrieve the "apis" resource with id "createdApiId"
    And I extract response field "name" and store it as "createdApiName"
    And I extract response field "context" and store it as "createdApiContext"

    When I search DevPortal APIs with query "name:{{createdApiName}}"
    Then The response status code should be 200
    And The response should contain "{{createdApiName}}"

    When I search DevPortal APIs with query "context:{{createdApiContext}}"
    Then The response status code should be 200
    And The response should contain "{{createdApiName}}"

    Examples:
      | actor                      |
      | publisherUser              |
      | publisherUser@tenant1.com  |

  # An API deployed to a custom gateway environment reflects that environment's vhost in its Developer Portal
  # endpoint URLs. NEW verified coverage (legacy-disabled testValidateDevportalAPIAndSwaggerResponse). Creating
  # the custom environment and publishing the API are admin/publisher prerequisites (@dep), not the subject —
  # the assertion is the DevPortal view reflecting the environment's vhost.
  @cap:devportal @feat:discovery @rule:environment @type:regression @dep:publisher @dep:admin @legacy:EnvironmentTestCase
  Scenario Outline: A devportal API reflects the custom environment vhost it is deployed to as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "e5ApiPayload"
    And I create an "apis" resource with payload "e5ApiPayload" as "e5ApiId"
    Then The response status code should be 201
    When I create a gateway environment "${UNIQUE:e5Env}" with vhost hosts "e5.gw.example.com"
    Then The response status code should be 201
    And I extract response field "name" and store it as "e5EnvName"
    When I put the following JSON payload in context as "e5RevPayload"
    """
    {"description":"revision for devportal env check"}
    """
    And I make a request to create a revision for "apis" resource "e5ApiId" with payload "e5RevPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "e5DeployPayload"
    """
    [{"name":"{{e5EnvName}}","vhost":"e5.gw.example.com","displayOnDevportal":true}]
    """
    And I make a request to deploy revision "revisionId" of "apis" resource "e5ApiId" with payload "e5DeployPayload"
    Then The response status code should be 201
    When I publish the "apis" resource with id "e5ApiId"
    Then The lifecycle status of API "e5ApiId" should be "Published"
    # The devportal view of the API carries the custom environment's vhost among its endpoint URLs.
    When I retrieve the devportal API "e5ApiId" until it contains "e5.gw.example.com" within 60 seconds
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
