@cleanup
Feature: DevPortal deprecated-API discoverability under DisplayAllAPIs

  Closes the TRUE direction of the config gate that governs whether a DEPRECATED API remains discoverable in
  the Developer Portal. The FALSE direction — the shipped default, which is what users actually get — is
  asserted in devportal/subscribe.feature; the two together pin BOTH sides of the switch, so neither a change
  of the default nor a change of the filter can pass unnoticed.

  MECHANISM (verified against the shipped artifacts, not inferred).
  RegistrySearchUtil.getDevPortalSearchQuery appends `lcState=(PUBLISHED OR PROTOTYPED)` to EVERY devportal
  query, and includes DEPRECATED in that list only when APIUtil.isAllowDisplayAPIsWithMultipleStatus() is
  true. That method reads api-manager.xml <APIStore><DisplayAllAPIs>, which
  repository/resources/conf/templates/repository/conf/api-manager.xml.j2 line 640 renders from
  `apim.devportal.display_deprecated_apis`. The distribution's default.json ships that key as false, so this
  feature only means anything inside the block that supplies the overlay turning it on.

  WHY THIS EXISTS AT ALL. Legacy AccessibilityOfDeprecatedOldAPIAndPublishedCopyAPITestCase had its
  store-visibility assertion commented out entirely. The reason was long assumed to be DisplayMultipleVersions
  grouping; it is in fact this lcState filter. So the facet legacy abandoned is config-gated rather than
  unconditional, and closing it honestly requires asserting each side under the configuration that produces it
  — which is why the true side needs its own container and could not simply be added to the existing scenario.

  @cap:devportal @feat:discovery @type:regression @dep:publisher @legacy:AccessibilityOfDeprecatedOldAPIAndPublishedCopyAPITestCase
  Scenario Outline: A deprecated API stays discoverable in the devportal search when DisplayAllAPIs is enabled (<tenant>)
    Given The system is ready
    And I have valid access tokens as "admin<tenantSuffix>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "depDiscApiId" and deployed it
    When I retrieve the "apis" resource with id "depDiscApiId"
    Then The response status code should be 200
    And I extract response field "name" and store it as "depDiscApiName"
    When I publish the "apis" resource with id "depDiscApiId"
    Then The lifecycle status of API "depDiscApiId" should be "Published"
    # BASELINE while PUBLISHED. Also the control that proves the API was indexed at all, so the post-deprecation
    # count-1 below cannot be satisfied by a stale index entry or misread as an accident of timing.
    When I search DevPortal APIs with query "{{depDiscApiName}}" and limit 25 until the result count is 1 within 60 seconds

    When I change the lifecycle of API "depDiscApiId" with action "Deprecate"
    Then The response status code should be 200
    And The lifecycle status of API "depDiscApiId" should be "Deprecated"

    # THE ASSERTION THIS BLOCK EXISTS FOR: with DisplayAllAPIs on, the DEPRECATED API is STILL returned by the
    # devportal search — the exact opposite of the count-0 asserted under the shipped default in
    # devportal/subscribe.feature. The API name is unique by construction, so count 1 identifies this API.
    When I search DevPortal APIs with query "{{depDiscApiName}}" and limit 25 until the result count is 1 within 60 seconds
    # Not merely "something came back": the entry returned is the DEPRECATED one, which is what proves the
    # lcState filter admitted DEPRECATED rather than the search having matched some other lifecycle state.
    Then The response should contain "DEPRECATED"

    Examples:
      | tenant       | tenantSuffix |
      | carbon.super |              |
      | tenant1.com  | @tenant1.com |
