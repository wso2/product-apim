@cleanup
Feature: Admin Throttling Policy Export / Import

  Ports ThrottlePolicyExportImportTestCase: exporting a throttling policy and re-importing it. For each policy
  type the arc is the same — create the policy, export it (200), then import the exported artifact three ways:
  without overwrite while it still exists (409 conflict), with overwrite (200 update), and — after deleting the
  policy — without overwrite again (201 new). subscription/application/advanced run ×2 tenant; custom (Siddhi)
  is super-tenant only (custom-policy creation is a 403 in a sub-tenant — an admin-global concern). The final
  re-imported (same-named, unique) policy is left to the block's container teardown.

  @cap:admin @feat:throttling-policies @type:regression @legacy:ThrottlePolicyExportImportTestCase
  Scenario Outline: Export and re-import a subscription throttling policy as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create a subscription throttling policy "eiSub${UNIQUE:Pol}" allowing 1000 requests per minute
    Then The response status code should be 201
    When I export the "subscription" throttling policy named "subThrottlePolicyName" as "eiSubExport"
    Then The response status code should be 200
    When I import the throttling policy from "eiSubExport" with overwrite "false"
    Then The response status code should be 409
    When I import the throttling policy from "eiSubExport" with overwrite "true"
    Then The response status code should be 200
    When I delete the "subscription" throttling policy with id "subThrottlePolicyId"
    Then The response status code should be 200
    When I import the throttling policy from "eiSubExport" with overwrite "false"
    Then The response status code should be 201

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:admin @feat:throttling-policies @type:regression @legacy:ThrottlePolicyExportImportTestCase
  Scenario Outline: Export and re-import an application throttling policy as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create an application throttling policy "eiApp${UNIQUE:Pol}" allowing 1000 requests per minute
    Then The response status code should be 201
    When I export the "application" throttling policy named "appThrottlePolicyName" as "eiAppExport"
    Then The response status code should be 200
    When I import the throttling policy from "eiAppExport" with overwrite "false"
    Then The response status code should be 409
    When I import the throttling policy from "eiAppExport" with overwrite "true"
    Then The response status code should be 200
    When I delete the "application" throttling policy with id "appThrottlePolicyId"
    Then The response status code should be 200
    When I import the throttling policy from "eiAppExport" with overwrite "false"
    Then The response status code should be 201

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:admin @feat:throttling-policies @type:regression @legacy:ThrottlePolicyExportImportTestCase
  Scenario Outline: Export and re-import an advanced throttling policy as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create an advanced throttling policy "eiAdv${UNIQUE:Pol}" allowing 1000 requests per minute
    Then The response status code should be 201
    When I export the "advanced" throttling policy named "advThrottlePolicyName" as "eiAdvExport"
    Then The response status code should be 200
    When I import the throttling policy from "eiAdvExport" with overwrite "false"
    Then The response status code should be 409
    When I import the throttling policy from "eiAdvExport" with overwrite "true"
    Then The response status code should be 200
    When I delete the "advanced" throttling policy with id "advThrottlePolicyId"
    Then The response status code should be 200
    When I import the throttling policy from "eiAdvExport" with overwrite "false"
    Then The response status code should be 201

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Custom (Siddhi) throttling policies are an admin-global concern — creation in a sub-tenant is 403 — so this
  # is super-tenant only (×1), consistent with the custom-policy CRUD coverage.
  @cap:admin @feat:throttling-policies @type:regression @legacy:ThrottlePolicyExportImportTestCase
  Scenario: Export and re-import a custom throttling policy
    Given The system is ready
    And I have valid access tokens as "admin"
    When I create a custom throttling policy "eiCustom${UNIQUE:Pol}" throttling API context "/eiCtx${UNIQUE:C}" after 1000 requests per minute
    Then The response status code should be 201
    When I export the "custom" throttling policy named "customThrottlePolicyName" as "eiCustomExport"
    Then The response status code should be 200
    When I import the throttling policy from "eiCustomExport" with overwrite "false"
    Then The response status code should be 409
    When I import the throttling policy from "eiCustomExport" with overwrite "true"
    Then The response status code should be 200
    When I delete the "custom" throttling policy with id "customThrottlePolicyId"
    Then The response status code should be 200
    When I import the throttling policy from "eiCustomExport" with overwrite "false"
    Then The response status code should be 201
