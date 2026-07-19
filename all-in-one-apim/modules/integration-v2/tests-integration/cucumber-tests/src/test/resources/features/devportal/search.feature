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

  # DevPortal reflects the latest PUBLISHED version of an API: v1 is shown; a newly-created but unpublished v2 is
  # NOT shown; once v2 is published it is shown. Ports the latest-version essence of APIMANAGER4081 (the heavy
  # own-tenant 20-version pagination sprawl is intentionally reduced to this 2-version slice).
  @cap:devportal @feat:discovery @rule:latest-version @type:regression @dep:publisher @legacy:APIMANAGER4081PaginationCountTestCase
  Scenario Outline: DevPortal shows the latest published version of an API as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "lvApiId" and deployed it
    When I publish the "apis" resource with id "lvApiId"
    Then The lifecycle status of API "lvApiId" should be "Published"
    When I retrieve the "apis" resource with id "lvApiId"
    And I extract response field "name" and store it as "lvApiName"

    # v1.0.0 is the latest published version -> DevPortal lists it.
    When I search DevPortal APIs with query "name:{{lvApiName}}" until it contains "{{lvApiName}}" within 60 seconds
    Then The response status code should be 200
    And The response should contain "1.0.0"

    # Create v2.0.0 but do NOT publish it -> DevPortal still shows only the published v1 (2.0.0 not present).
    When I create a new version "2.0.0" of "apis" resource "lvApiId" with default version "false" as "lvApiV2Id"
    Then The response status code should be 201
    When I search DevPortal APIs once with query "name:{{lvApiName}}"
    Then The response status code should be 200
    And The response should not contain "2.0.0"

    # Publish v2.0.0 -> DevPortal now shows 2.0.0.
    When I publish the "apis" resource with id "lvApiV2Id"
    Then The lifecycle status of API "lvApiV2Id" should be "Published"
    When I search DevPortal APIs with query "name:{{lvApiName}}" until it contains "2.0.0" within 60 seconds
    Then The response status code should be 200
    And The response should contain "2.0.0"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # Tag cloud lists case- and space-distinct tags as separate entries: four tags differing only by case
  # (API_ vs api_) or by an embedded space are four distinct tags, each used by exactly one API. Uniquely
  # generated so parallel scenarios never collide. Ports APISearchAPIByTagTestCase.testAPISearchByTagGroup.
  @cap:devportal @feat:discovery @rule:tag-cloud @type:regression @dep:publisher @legacy:APISearchAPIByTagTestCase
  Scenario Outline: DevPortal tag cloud lists case- and space-distinct tags as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I generate a unique value and store it as "grp"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" with tags "API_{{grp}}" as "apiUpper" and deployed it
    When I publish the "apis" resource with id "apiUpper"
    Then The lifecycle status of API "apiUpper" should be "Published"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" with tags "api_{{grp}}" as "apiLower" and deployed it
    When I publish the "apis" resource with id "apiLower"
    Then The lifecycle status of API "apiLower" should be "Published"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" with tags "api {{grp}}" as "apiSpace" and deployed it
    When I publish the "apis" resource with id "apiSpace"
    Then The lifecycle status of API "apiSpace" should be "Published"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" with tags "apiTag_{{grp}}" as "apiCamel" and deployed it
    When I publish the "apis" resource with id "apiCamel"
    Then The lifecycle status of API "apiCamel" should be "Published"

    # Poll the tag cloud until the last-published tag is indexed, then assert all four distinct tags, each count 1.
    When I retrieve the DevPortal tag cloud until it contains "apiTag_{{grp}}" within 60 seconds
    Then The response status code should be 200
    And the DevPortal tag cloud should contain tag "API_{{grp}}" with count 1
    And the DevPortal tag cloud should contain tag "api_{{grp}}" with count 1
    And the DevPortal tag cloud should contain tag "api {{grp}}" with count 1
    And the DevPortal tag cloud should contain tag "apiTag_{{grp}}" with count 1

    Examples:
      | actor                      |
      | publisherUser              |
      | publisherUser@tenant1.com  |

  # Search DevPortal APIs by tag: a common tag matches both APIs, a distinct tag only its own, a non-existent tag
  # matches nothing. Ports APISearchAPIByTagTestCase (tag search).
  @cap:devportal @feat:discovery @rule:tag-search @type:regression @dep:publisher @legacy:APISearchAPIByTagTestCase
  Scenario Outline: Search DevPortal APIs by tag as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I generate a unique value and store it as "commonTag"
    And I generate a unique value and store it as "tag1"
    And I generate a unique value and store it as "tag2"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" with tags "{{commonTag}},{{tag1}}" as "api1" and deployed it
    When I publish the "apis" resource with id "api1"
    Then The lifecycle status of API "api1" should be "Published"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" with tags "{{commonTag}},{{tag2}}" as "api2" and deployed it
    When I publish the "apis" resource with id "api2"
    Then The lifecycle status of API "api2" should be "Published"

    When I retrieve the "apis" resource with id "api1"
    And I extract response field "name" and store it as "api1Name"
    When I retrieve the "apis" resource with id "api2"
    And I extract response field "name" and store it as "api2Name"

    # Common tag → both APIs. Poll until the later-published api2 is indexed, then assert both are present.
    When I search DevPortal APIs with query "tags:{{commonTag}}" until it contains "{{api2Name}}" within 60 seconds
    Then The response status code should be 200
    And The response should contain "{{api1Name}}"
    And The response should contain "{{api2Name}}"

    # Distinct tag → only api1, not api2.
    When I search DevPortal APIs with query "tags:{{tag1}}" until it contains "{{api1Name}}" within 60 seconds
    Then The response status code should be 200
    And The response should contain "{{api1Name}}"
    And The response should not contain "{{api2Name}}"

    # Non-existent tag → no matching APIs (empty result, still 200). Single-shot: the tag can never appear, so
    # polling would only burn the timeout. The tag concatenates both unique tags so it is guaranteed absent.
    When I search DevPortal APIs once with query "tags:{{tag1}}-none-{{tag2}}"
    Then The response status code should be 200
    And The response should not contain "{{api1Name}}"
    And The response should not contain "{{api2Name}}"

    Examples:
      | actor                      |
      | publisherUser              |
      | publisherUser@tenant1.com  |

  # Paginated search caps a page at the requested limit: with more matching published APIs than the limit,
  # the returned page count saturates at the limit. Ports SearchPaginatedAPIsWithMultipleStatusTestCase.
  @cap:devportal @feat:discovery @rule:pagination @type:regression @dep:publisher @legacy:SearchPaginatedAPIsWithMultipleStatusTestCase
  Scenario Outline: DevPortal paginated search caps the page at the requested limit as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I generate a unique value and store it as "pfx"
    And I create and publish 12 APIs from "artifacts/payloads/create_apim_test_api.json" named "{{pfx}}"
    When I search DevPortal APIs with query "name:{{pfx}}" and limit 10 until the result count is 10 within 90 seconds
    Then The response status code should be 200

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |
