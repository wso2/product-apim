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

  @cap:admin @feat:tenants-orgs @type:regression @legacy:AdvancedConfigurationsTestCase
  Scenario Outline: Update the tenant configuration and restore it as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I capture the tenant configuration as "tcOriginal"
    And I capture the tenant configuration as "tcModified"
    And I set the boolean field "EnableMonetization" to "true" in the payload "tcModified"
    And I update the tenant configuration from "tcModified"
    Then The response status code should be 200
    When I retrieve the tenant configuration
    Then The response status code should be 200
    When I update the tenant configuration from "tcOriginal"
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
