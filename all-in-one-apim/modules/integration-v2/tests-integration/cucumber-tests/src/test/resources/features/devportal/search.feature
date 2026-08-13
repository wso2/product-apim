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

    # Poll for EACH tag in turn before asserting the counts. Polling only the last-published tag assumes Solr
    # indexes in publish order -- it does not (the same non-order-preserving behaviour the tag-search and
    # search-syntax scenarios below wait on a COUNT to avoid). apiTag_ can land first, leaving the other three
    # unindexed when the count assertions run. Four sequential waits admit the assertions only once all four
    # documents are indexed; each is a no-op once its tag is present, so this costs nothing in the settled case.
    When I retrieve the DevPortal tag cloud until it contains "API_{{grp}}" within 60 seconds
    And I retrieve the DevPortal tag cloud until it contains "api_{{grp}}" within 60 seconds
    And I retrieve the DevPortal tag cloud until it contains "api {{grp}}" within 60 seconds
    And I retrieve the DevPortal tag cloud until it contains "apiTag_{{grp}}" within 60 seconds
    Then The response status code should be 200
    And the DevPortal tag cloud should contain tag "API_{{grp}}" with count 1
    And the DevPortal tag cloud should contain tag "api_{{grp}}" with count 1
    And the DevPortal tag cloud should contain tag "api {{grp}}" with count 1
    And the DevPortal tag cloud should contain tag "apiTag_{{grp}}" with count 1

    Examples:
      | actor                      |
      | publisherUser              |
      | publisherUser@tenant1.com  |

  # A PROTOTYPED (pre-published) API's tag DOES reach the DevPortal tag cloud — verified live on 4.7.0 (GET /tags
  # answered {"count":1,"list":[{"value":"<the tag>","count":1}],...}). This is the OPPOSITE of what legacy
  # APIM24VisibilityOfPrototypedAPIOfDifferentViewInStoreTestCase#testTagsOfPrototypedAPIVisibilityInTagList claimed,
  # and legacy could never have detected the difference: its assertion was
  # assertFalse(allTags.getList().contains(apiTags)), comparing a List<TagDTO> against the raw CSV String
  # "pizza, order, pizza-menu" — a value no TagDTO list can ever contain — so it passed unconditionally and verified
  # nothing. Consistent with the API itself being discoverable while prototyped (publisher/prototype_api.feature).
  @cap:devportal @feat:discovery @rule:tag-cloud @type:regression @dep:publisher @legacy:APIM24VisibilityOfPrototypedAPIOfDifferentViewInStoreTestCase
  Scenario Outline: A prototyped API's tag appears in the DevPortal tag cloud as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I generate a unique value and store it as "protoTag"
    When I put JSON payload from file "artifacts/payloads/create_apim_prototype_api.json" in context as "protoTagPayload"
    And I create an "apis" resource with payload "protoTagPayload" as "protoTagApiId"
    Then The response status code should be 201
    # A uniquely-generated tag: the shipped payload's literal "prototype" tag is shared with other scenarios and
    # could not be attributed to this API.
    When I set the tags of API "protoTagApiId" to "{{protoTag}}"
    Then The response status code should be 200
    When I change the lifecycle of API "protoTagApiId" with action "Deploy as a Prototype"
    Then The response status code should be 200
    And The lifecycle status of API "protoTagApiId" should be "Prototyped"
    When I deploy the API with id "protoTagApiId"
    Then The response status code should be 201
    # Wait for the API itself to be indexed in the DevPortal FIRST, so a slow index cannot be mistaken for the tag
    # being withheld.
    When I retrieve the devportal API "protoTagApiId" until it contains "PROTOTYPED" within 60 seconds
    Then The response status code should be 200
    # The tag is present, attributed to exactly this one API.
    When I retrieve the DevPortal tag cloud until it contains "{{protoTag}}" within 60 seconds
    Then The response status code should be 200
    And the DevPortal tag cloud should contain tag "{{protoTag}}" with count 1

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

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

    # Common tag -> both APIs, and only those two. Waiting on the COUNT rather than on api2's name: Solr indexing is
    # asynchronous and not order-preserving, so waiting for the later-published api2 can exit while api1 is still
    # unindexed, failing the next line against a one-API response. The count admits neither until both are indexed.
    When I search DevPortal APIs with query "tags:{{commonTag}}" and limit 25 until the result count is 2 within 60 seconds
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

  # A restricted-visibility API's tag is hidden from an unauthorised tag cloud but present for a role-bearing user.
  # Ports APITagVisibilityByRoleTestCase: a public API's tag is always visible; a restricted API's tag appears in
  # the tag cloud only for a user carrying the visibility role. Provisions the role + user inline per scenario.
  @cap:devportal @feat:discovery @rule:tag-visibility @type:regression @dep:publisher @legacy:APITagVisibilityByRoleTestCase
  Scenario Outline: A restricted API's tag is hidden from unauthorised users' tag cloud in <tenant>
    Given The system is ready
    And I have valid access tokens as "admin<suffix>"
    And I generate a unique value and store it as "tvrole"
    And I generate a unique value and store it as "tvPublicTag"
    And I generate a unique value and store it as "tvRestrictedTag"
    And I provision store-visibility role "{{tvrole}}" in tenant "<tenant>"
    And I provision user "tvUser" with roles "Internal/subscriber,{{tvrole}}" in tenant "<tenant>"
    And The system is ready and I have valid devportal access token as "tvUser<suffix>"

    Given I act as "admin<suffix>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" with tags "{{tvPublicTag}}" as "tvPublicApi" and deployed it
    When I publish the "apis" resource with id "tvPublicApi"
    Then The lifecycle status of API "tvPublicApi" should be "Published"
    When I have created an api from "artifacts/payloads/create_apim_test_api.json" with restricted visibility for roles "{{tvrole}}" as "tvRestrictedApi" and deployed it
    And I set the tags of API "tvRestrictedApi" to "{{tvRestrictedTag}}"
    And I publish the "apis" resource with id "tvRestrictedApi"
    Then The lifecycle status of API "tvRestrictedApi" should be "Published"

    # The role-bearing user's tag cloud FIRST: polling until the restricted tag appears proves the restricted
    # API's document has landed in the (shared) index — the barrier the unauthorised check below needs. Gating the
    # unauthorised check on the PUBLIC tag alone raced: the public API indexes first, so that gate passes while the
    # restricted API's visibility filter is still converging, and its tag transiently leaks into every viewer's
    # cloud (observed under CI load).
    When I act as "tvUser<suffix>"
    # Both tags must be indexed before the counts are asserted. Waiting only on the restricted tag assumes the
    # public API (published first) is already indexed -- the same order assumption that is not safe here.
    And I retrieve the DevPortal tag cloud until it contains "{{tvPublicTag}}" within 60 seconds
    And I retrieve the DevPortal tag cloud until it contains "{{tvRestrictedTag}}" within 60 seconds
    Then The response status code should be 200
    And the DevPortal tag cloud should contain tag "{{tvPublicTag}}" with count 1
    And the DevPortal tag cloud should contain tag "{{tvRestrictedTag}}" with count 1

    # Unauthorised tag cloud: a roleless SUBSCRIBER (not admin — the legacy test's unauthorised viewer was
    # role-less; admin is the API's creator/administrator and DOES see the restricted tag, verified live). With
    # the restricted doc known-indexed, poll until the tag disappears for this viewer — a persistent leak (the
    # real regression) times out and fails with the cloud body. The public tag in the same settled response
    # proves the check is not vacuous.
    Given The system is ready and I have valid devportal access token as "subscriberUser<suffix>"
    When I retrieve the DevPortal tag cloud until it does not contain "{{tvRestrictedTag}}" within 60 seconds
    Then the DevPortal tag cloud should contain tag "{{tvPublicTag}}" with count 1
    And the DevPortal tag cloud should not contain tag "{{tvRestrictedTag}}"

    Examples:
      | tenant       | suffix       |
      | carbon.super |              |
      | tenant1.com  | @tenant1.com |

  # Content search by description — a unique word planted in the API's description surfaces the API in both the
  # publisher and the store content search. Ports the description half of ContentSearchTestCase
  # testBasicContentSearch. A published API matching the description word returns exactly one result (pinned live:
  # a bare word query matches the API's description; the count is 1, not the definition-inflated 2 an older build
  # returned). Document-content search is intentionally omitted here — its indexing is minutes-slow and unreliable
  # to pin as an exact count (legacy retried it for ~5 minutes); description search covers the content-search arc.
  @cap:devportal @feat:discovery @rule:content-search @type:regression @dep:publisher @legacy:ContentSearchTestCase
  Scenario Outline: An API is found by its description in content search as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I generate a unique value and store it as "csDesc"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "csApiPayload"
    And I set the description of context payload "csApiPayload" to "Unified search feature {{csDesc}}"
    And I create an "apis" resource with payload "csApiPayload" as "csApiId"
    And I deploy the API with id "csApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "csApiId"
    Then The lifecycle status of API "csApiId" should be "Published"

    # Search by description in the publisher and the store. The description: field query matches the unique word
    # exactly (pinned live: a bare content query does not match an underscore-joined token, but description: does).
    When I search Publisher APIs with content query "description:{{csDesc}}" until the result count is 1 within 60 seconds
    When I search DevPortal APIs with content query "description:{{csDesc}}" until the result count is 1 within 60 seconds

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # Content search respects store visibility: a restricted-visibility API is found (count 1) by a store searcher
  # carrying the visibility role and NOT found (count 0) by one who does not. Ports ContentSearchTestCase
  # testContentSearchWithStoreVisibility.
  @cap:devportal @feat:discovery @rule:content-search @type:regression @dep:publisher @legacy:ContentSearchTestCase
  Scenario Outline: Content search respects store visibility restrictions in <tenant>
    Given The system is ready
    And I have valid access tokens as "admin<suffix>"
    And I generate a unique value and store it as "csvrole"
    And I generate a unique value and store it as "csvDesc"
    And I provision store-visibility role "{{csvrole}}" in tenant "<tenant>"
    And I provision user "csvIn" with roles "Internal/subscriber,{{csvrole}}" in tenant "<tenant>"
    And I provision user "csvOut" with roles "Internal/subscriber" in tenant "<tenant>"
    And The system is ready and I have valid devportal access token as "csvIn<suffix>"
    And The system is ready and I have valid devportal access token as "csvOut<suffix>"

    Given I act as "admin<suffix>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "csvApiPayload"
    And I set the description of context payload "csvApiPayload" to "Visibility filtered search {{csvDesc}}"
    When I have created an api from context payload "csvApiPayload" with restricted visibility for roles "{{csvrole}}" as "csvApiId" and deployed it
    And I publish the "apis" resource with id "csvApiId"
    Then The lifecycle status of API "csvApiId" should be "Published"

    # The role-bearing store user finds it (1 hit); the user without the role finds nothing (0).
    When I act as "csvIn<suffix>"
    And I search DevPortal APIs with content query "description:{{csvDesc}}" until the result count is 1 within 60 seconds
    When I act as "csvOut<suffix>"
    And I search DevPortal APIs with content query "description:{{csvDesc}}" until the result count is 0 within 60 seconds

    Examples:
      | tenant       | suffix       |
      | carbon.super |              |
      | tenant1.com  | @tenant1.com |

  # Solr query syntax edge-cases: multi-name OR, multi-tag OR, combined name+tags, and the tag:/tags: alias
  # equivalence. Ports the distinct query-syntax assertions of DevPortalSearchTest (single-tag / non-existent-tag
  # cases already covered by the tag-search scenario above). Runs x2-tenant (super + tenant1): each row publishes
  # its own uniquely-named/tagged APIs and searches its own tenant store.
  @cap:devportal @feat:discovery @rule:search-syntax @type:regression @dep:publisher @legacy:DevPortalSearchTest
  Scenario Outline: DevPortal search honours multi-filter and tag alias Solr syntax as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I generate a unique value and store it as "ssTagA"
    And I generate a unique value and store it as "ssTagB"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" with tags "{{ssTagA}}" as "ssApi1" and deployed it
    When I publish the "apis" resource with id "ssApi1"
    Then The lifecycle status of API "ssApi1" should be "Published"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" with tags "{{ssTagB}}" as "ssApi2" and deployed it
    When I publish the "apis" resource with id "ssApi2"
    Then The lifecycle status of API "ssApi2" should be "Published"
    When I retrieve the "apis" resource with id "ssApi1"
    And I extract response field "name" and store it as "ssApi1Name"
    When I retrieve the "apis" resource with id "ssApi2"
    And I extract response field "name" and store it as "ssApi2Name"

    # Multi-tag OR: both distinct tags in one query returns both APIs.
    # WAIT ON THE COUNT, NOT ON ONE NAME. An earlier revision polled "until it contains <api2>" on the theory that
    # api2 publishing last meant api1 was already indexed. Solr indexing is asynchronous and NOT order-preserving,
    # so api2 can land first; the wait then exits with api1 still unindexed and the next line fails on a response
    # holding exactly one API. Waiting for the count admits neither document until BOTH are indexed. It is also a
    # strictly stronger assertion: the two "should contain" lines only prove the query returns AT LEAST these APIs,
    # so an over-broad query matching the whole tenant would pass -- pinning 2 proves the "and nothing else" half.
    # Both tags are ${UNIQUE}, so exactly 2 can ever match. limit 25 is the store default, not a new constraint.
    When I search DevPortal APIs with query "tags:{{ssTagA}} tags:{{ssTagB}}" and limit 25 until the result count is 2 within 60 seconds
    Then The response status code should be 200
    And The response should contain "{{ssApi1Name}}"
    And The response should contain "{{ssApi2Name}}"

    # Multi-name OR returns both named APIs.
    # Multi-name OR returns both named APIs, and only those two. Same count-based wait, same reason as above.
    When I search DevPortal APIs with query "name:{{ssApi1Name}} name:{{ssApi2Name}}" and limit 25 until the result count is 2 within 60 seconds
    Then The response status code should be 200
    And The response should contain "{{ssApi1Name}}"
    And The response should contain "{{ssApi2Name}}"

    # Combined name + tag: name of api1 AND tag of api1 returns api1, not api2.
    When I search DevPortal APIs with query "name:{{ssApi1Name}} tags:{{ssTagA}}" until it contains "{{ssApi1Name}}" within 60 seconds
    Then The response status code should be 200
    And The response should contain "{{ssApi1Name}}"
    And The response should not contain "{{ssApi2Name}}"

    # The singular tag: alias is equivalent to tags:.
    When I search DevPortal APIs with query "tag:{{ssTagA}}" until it contains "{{ssApi1Name}}" within 60 seconds
    Then The response status code should be 200
    And The response should contain "{{ssApi1Name}}"
    And The response should not contain "{{ssApi2Name}}"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # Tag update round-trip: an API found by a tag is no longer found by that tag after the tag is removed. Ports
  # ChangeAPITagsTestCase. The removal poll waits on the DISTINGUISHING new state (the API absent from the old-tag
  # result), never on a pre-satisfied condition.
  @cap:devportal @feat:discovery @rule:tag-search @type:regression @dep:publisher @legacy:ChangeAPITagsTestCase
  Scenario Outline: Removing a tag drops the API from that tag's search results as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I generate a unique value and store it as "catKeepTag"
    And I generate a unique value and store it as "catDropTag"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" with tags "{{catKeepTag}},{{catDropTag}}" as "catApiId" and deployed it
    When I publish the "apis" resource with id "catApiId"
    Then The lifecycle status of API "catApiId" should be "Published"
    When I retrieve the "apis" resource with id "catApiId"
    And I extract response field "name" and store it as "catApiName"

    # Initially the API is found by the drop-tag.
    When I search DevPortal APIs with query "tags:{{catDropTag}}" until it contains "{{catApiName}}" within 60 seconds
    Then The response status code should be 200
    And The response should contain "{{catApiName}}"

    # Remove the drop-tag (keep only the keep-tag), then poll the drop-tag search until the API disappears.
    When I set the tags of API "catApiId" to "{{catKeepTag}}"
    And I search DevPortal APIs with query "tags:{{catDropTag}}" until it does not contain "{{catApiName}}" within 60 seconds
    Then The response status code should be 200
    And The response should not contain "{{catApiName}}"
    # Sanity: the kept tag still finds it.
    When I search DevPortal APIs with query "tags:{{catKeepTag}}" until it contains "{{catApiName}}" within 60 seconds
    Then The response should contain "{{catApiName}}"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

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
