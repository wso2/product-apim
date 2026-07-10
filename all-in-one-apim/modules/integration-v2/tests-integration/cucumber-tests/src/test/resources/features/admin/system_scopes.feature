Feature: Admin System Scope Role-Alias Mapping

  Ports APISystemScopesTestCase: the admin REST API for system-scope role-alias mappings
  (/api/am/admin/v4/role-aliases). Adds an alias to a role, confirms it is listed, then clears the mappings.
  Runs ×2 tenant (each tenant maintains its own role-alias mappings). The clear step is the teardown, so no
  mapping is left mutated on the shared container.

  @cap:admin @feat:role-scope-mapping @type:regression @legacy:APISystemScopesTestCase
  Scenario Outline: A role-alias scope mapping can be added, retrieved and cleared as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I set the role alias "testRole" for role "admin"
    Then The response status code should be 200
    When I retrieve the role aliases
    Then The response status code should be 200
    And The response should contain "testRole"
    When I clear all role aliases
    Then The response status code should be 200
    When I retrieve the role aliases
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
