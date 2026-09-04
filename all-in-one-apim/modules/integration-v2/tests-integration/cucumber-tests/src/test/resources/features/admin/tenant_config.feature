Feature: Admin Tenant Configuration

  Ports the legacy AdvancedConfigurationsTestCase: the admin tenant-configuration API
  (/api/am/admin/v4/tenant-config and /tenant-config-schema). Extends the legacy "assert not-null" checks into a
  real round-trip — capture the current config, update it with a modified copy, then restore the original — so
  the update path is exercised without leaving the shared container's tenant config mutated. Adds negatives the
  legacy only partly covered: an invalid-signature JWT and a non-admin (publisher-scope) token are both rejected
  with 401. All scenarios run ×2 tenant (each tenant has its own tenant config).

  @cap:admin @feat:tenants-orgs @type:smoke @legacy:AdvancedConfigurationsTestCase
  Scenario Outline: Retrieve the tenant configuration and its schema as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I retrieve the tenant configuration
    Then The response status code should be 200
    And The response should contain "RESTAPIScopes"
    When I retrieve the tenant configuration schema
    Then The response status code should be 200
    And The response should contain "properties"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cleanup @cap:admin @feat:tenants-orgs @type:regression @legacy:AdvancedConfigurationsTestCase
  Scenario Outline: Update the tenant configuration and restore it as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I capture the tenant configuration as "tcOriginal"
    And I register tenant configuration "tcOriginal" for cleanup
    And I capture the tenant configuration as "tcModified"
    And I set the boolean field "EnableMonetization" to "true" in the payload "tcModified"
    And I update the tenant configuration from "tcModified"
    Then The response status code should be 200
    When I retrieve the tenant configuration
    Then The response status code should be 200
    And The response should contain "EnableMonetization"
    When I update the tenant configuration from "tcOriginal"
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports APIMANAGER5417PrototypedAPIsInMonetizedTestCase — its INTENT, not its mechanism. Legacy drove the removed
  # jaggery designAPI/implement flow and flipped an api-manager.xml throttling switch; neither is reachable now. The
  # portable observable is: with MONETIZATION ENABLED on the tenant, a PROTOTYPED API is still discoverable — listed
  # in the publisher and visible in the devportal. (Monetization gates commercial subscription plans; a prototyped
  # API is consumed without a subscription, so turning monetization on must not withdraw it.)
  #
  # Co-located in this feature deliberately, against §2's "move a scenario to its @cap folder" rule: the tenant
  # config is a container-GLOBAL mutable resource and the update scenario above already toggles the very same
  # EnableMonetization field. Scenarios inside one runner run sequentially, so keeping both toggles in THIS file
  # serialises them; a separate feature file would be a separate runner in the same concurrent block, and the two
  # toggles would race — one restoring the config while the other depends on it being enabled. The config is
  # restored at the end of the scenario, as above.
  @cap:devportal @feat:discovery @rule:prototype @type:regression @dep:admin @dep:publisher @legacy:APIMANAGER5417PrototypedAPIsInMonetizedTestCase
  Scenario Outline: A prototyped API stays discoverable while monetization is enabled as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    # Enable monetization on the tenant configuration (the original is captured first so it can be restored).
    When I capture the tenant configuration as "monetOriginal"
    And I capture the tenant configuration as "monetEnabled"
    And I set the boolean field "EnableMonetization" to "true" in the payload "monetEnabled"
    And I update the tenant configuration from "monetEnabled"
    Then The response status code should be 200
    When I retrieve the tenant configuration
    Then The response status code should be 200
    And The value of response field "EnableMonetization" should be "true"

    # Deploy an API as a PROTOTYPE while monetization is on.
    When I put JSON payload from file "artifacts/payloads/create_apim_prototype_api.json" in context as "monetProtoPayload"
    And I create an "apis" resource with payload "monetProtoPayload" as "monetProtoApiId"
    Then The response status code should be 201
    And I extract response field "name" and store it as "monetProtoName"
    When I change the lifecycle of API "monetProtoApiId" with action "Deploy as a Prototype"
    Then The response status code should be 200
    And The lifecycle status of API "monetProtoApiId" should be "Prototyped"
    When I deploy the API with id "monetProtoApiId"
    Then The response status code should be 201

    # Publisher: the prototyped API is listed — exactly one hit for its uniquely-generated name.
    When I search Publisher APIs with content query "name:{{monetProtoName}}" until the result count is 1 within 60 seconds
    Then The response should contain "{{monetProtoName}}"

    # DevPortal: the prototyped API is visible to consumers, carrying PROTOTYPED.
    When I retrieve the devportal API "monetProtoApiId" until it contains "PROTOTYPED" within 60 seconds
    Then The response status code should be 200
    And The value of response field "lifeCycleStatus" should be "PROTOTYPED"

    # Restore the tenant configuration so the container is left as found.
    When I update the tenant configuration from "monetOriginal"
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:admin @feat:tenants-orgs @type:negative @legacy:AdvancedConfigurationsTestCase
  Scenario Outline: A non-admin token cannot update the tenant configuration as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I capture the tenant configuration as "tcForNonAdmin"
    And I attempt to update the tenant configuration from "tcForNonAdmin" without admin scope
    Then The response status code should be 401

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:admin @feat:tenants-orgs @type:negative @legacy:AdvancedConfigurationsTestCase
  Scenario Outline: An invalid JWT cannot update the tenant configuration as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I capture the tenant configuration as "tcForInvalidJwt"
    And I attempt to update the tenant configuration from "tcForInvalidJwt" with an invalid token
    Then The response status code should be 401

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
